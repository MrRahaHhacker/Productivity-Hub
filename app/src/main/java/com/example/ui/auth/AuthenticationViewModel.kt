package com.example.ui.auth

import android.app.Activity
import android.app.Application
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.sync.GoogleAccountItem
import com.example.data.sync.GoogleSyncManager
import com.example.data.sync.UserRole
import com.example.data.sync.awaitTask
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.util.UUID

sealed interface AuthUiState {
    object Idle : AuthUiState
    data class Loading(val message: String = "Signing in with Google...") : AuthUiState
    data class Success(
        val firebaseUser: FirebaseUser?,
        val account: GoogleAccountItem,
        val message: String = "Signed in successfully"
    ) : AuthUiState
    data class Error(val message: String, val canFallback: Boolean = true) : AuthUiState
}

class AuthenticationViewModel @JvmOverloads constructor(
    application: Application,
    private val syncManager: GoogleSyncManager? = null,
    private val customFirebaseAuth: FirebaseAuth? = null,
    private val customCredentialManager: CredentialManager? = null
) : AndroidViewModel(application) {

    private val tag = "AuthenticationViewModel"

    private val firebaseAuth: FirebaseAuth? by lazy {
        customFirebaseAuth ?: try {
            if (FirebaseApp.getApps(getApplication()).isEmpty()) {
                FirebaseApp.initializeApp(getApplication())
            }
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            Log.e(tag, "FirebaseAuth initialization error", e)
            null
        }
    }

    private val credentialManager: CredentialManager by lazy {
        customCredentialManager ?: CredentialManager.create(getApplication())
    }

    private val serverClientId: String = BuildConfig.GOOGLE_OAUTH_CLIENT_ID

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    private val _selectedRole = MutableStateFlow(UserRole.STUDENT)
    val selectedRole: StateFlow<UserRole> = _selectedRole.asStateFlow()

    init {
        try {
            firebaseAuth?.let { auth ->
                _currentUser.value = auth.currentUser
                auth.addAuthStateListener { updatedAuth ->
                    _currentUser.value = updatedAuth.currentUser
                }
            }
        } catch (e: Exception) {
            Log.w(tag, "Auth state listener initialization note: ${e.message}")
        }
    }

    fun selectRole(role: UserRole) {
        _selectedRole.value = role
    }

    fun clearError() {
        if (_uiState.value is AuthUiState.Error) {
            _uiState.value = AuthUiState.Idle
        }
    }

    /**
     * Initiates Google Sign-In using Credential Manager and signs in to Firebase Auth.
     */
    fun signInWithGoogle(
        activity: Activity,
        role: UserRole = _selectedRole.value,
        onSuccess: ((GoogleAccountItem) -> Unit)? = null,
        onFailure: ((String) -> Unit)? = null
    ) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading("Opening Google Sign-In...")

            try {
                // 1. Generate cryptographic nonce
                val rawNonce = UUID.randomUUID().toString()
                val bytes = rawNonce.toByteArray()
                val md = MessageDigest.getInstance("SHA-256")
                val digest = md.digest(bytes)
                val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

                // 2. Build Credential Manager Google ID Option
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(serverClientId)
                    .setAutoSelectEnabled(false)
                    .setNonce(hashedNonce)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                // 3. Request credential
                val response = credentialManager.getCredential(
                    request = request,
                    context = activity
                )

                val credential = response.credential
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)

                val idToken = googleIdTokenCredential.idToken
                val email = googleIdTokenCredential.id
                val displayName = googleIdTokenCredential.displayName ?: email.substringBefore("@")

                _uiState.value = AuthUiState.Loading("Authenticating with Firebase...")

                // 4. Authenticate with Firebase using Google ID Token
                val auth = firebaseAuth
                val firebaseUser: FirebaseUser? = if (auth != null) {
                    try {
                        val authCredential = GoogleAuthProvider.getCredential(idToken, null)
                        val authResult = auth.signInWithCredential(authCredential).awaitTask()
                        authResult.user
                    } catch (e: Exception) {
                        Log.w(tag, "Firebase credential sign-in note: ${e.message}")
                        auth.currentUser
                    }
                } else null

                _currentUser.value = firebaseUser

                // 5. Update local session & cloud sync
                val finalEmail = firebaseUser?.email ?: email
                val finalDisplayName = firebaseUser?.displayName ?: displayName
                val account = GoogleAccountItem(
                    email = finalEmail,
                    displayName = finalDisplayName,
                    role = role
                )

                syncManager?.signInWithAccount(account)

                _uiState.value = AuthUiState.Success(
                    firebaseUser = firebaseUser,
                    account = account,
                    message = "Signed in as $finalDisplayName"
                )

                onSuccess?.invoke(account)

            } catch (e: GetCredentialCancellationException) {
                Log.w(tag, "Google Sign-In cancelled by user")
                val errorMsg = "Sign-In cancelled"
                _uiState.value = AuthUiState.Error(errorMsg, canFallback = true)
                onFailure?.invoke(errorMsg)
            } catch (e: NoCredentialException) {
                Log.w(tag, "No Google credentials found: ${e.message}")
                val errorMsg = "No Google account found on device. You can choose a profile below."
                _uiState.value = AuthUiState.Error(errorMsg, canFallback = true)
                onFailure?.invoke(errorMsg)
            } catch (e: GetCredentialException) {
                Log.e(tag, "Credential Manager error: ${e.message}", e)
                val errorMsg = e.message ?: "Google Sign-In failed"
                _uiState.value = AuthUiState.Error(errorMsg, canFallback = true)
                onFailure?.invoke(errorMsg)
            } catch (e: Exception) {
                Log.e(tag, "Unexpected sign-in error", e)
                val errorMsg = e.message ?: "Authentication failed"
                _uiState.value = AuthUiState.Error(errorMsg, canFallback = true)
                onFailure?.invoke(errorMsg)
            }
        }
    }

    /**
     * Fallback or direct sign-in with GoogleAccountItem
     */
    fun signInWithAccountDirectly(account: GoogleAccountItem, onSuccess: (() -> Unit)? = null) {
        _uiState.value = AuthUiState.Success(
            firebaseUser = firebaseAuth?.currentUser,
            account = account
        )
        syncManager?.signInWithAccount(account)
        onSuccess?.invoke()
    }

    /**
     * Signs out from Firebase Auth and clears Credential Manager state
     */
    fun signOut() {
        _uiState.value = AuthUiState.Idle
        _currentUser.value = null
        viewModelScope.launch {
            try {
                firebaseAuth?.signOut()
            } catch (e: Exception) {
                Log.e(tag, "Firebase signOut error", e)
            }

            try {
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w(tag, "Clear credential state note: ${e.message}")
            }

            syncManager?.signOutGoogle()
        }
    }
}
