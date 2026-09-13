package com.example.data.sync

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.BuildConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import java.security.MessageDigest
import java.util.UUID

sealed interface AuthResult {
    data class Success(val account: GoogleAccountItem, val idToken: String) : AuthResult
    data class Failure(val reason: String, val canFallback: Boolean = true) : AuthResult
}

class GoogleAuthHelper(private val context: Context) {

    private val credentialManager = CredentialManager.create(context)
    private val serverClientId = BuildConfig.GOOGLE_OAUTH_CLIENT_ID

    suspend fun launchGoogleSignIn(activity: Activity): AuthResult {
        return try {
            val rawNonce = UUID.randomUUID().toString()
            val bytes = rawNonce.toByteArray()
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(bytes)
            val hashedNonce = digest.fold("") { str, it -> str + "%02x".format(it) }

            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(serverClientId)
                .setAutoSelectEnabled(false)
                .setNonce(hashedNonce)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val response = credentialManager.getCredential(
                request = request,
                context = activity
            )

            val credential = response.credential
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)

            val email = googleIdTokenCredential.id
            val displayName = googleIdTokenCredential.displayName ?: email.substringBefore("@")
            val idToken = googleIdTokenCredential.idToken

            AuthResult.Success(
                account = GoogleAccountItem(
                    email = email,
                    displayName = displayName,
                    role = UserRole.STUDENT
                ),
                idToken = idToken
            )
        } catch (e: GetCredentialCancellationException) {
            Log.w("GoogleAuthHelper", "User cancelled Google Sign-In")
            AuthResult.Failure("Sign-In cancelled by user", canFallback = true)
        } catch (e: NoCredentialException) {
            Log.w("GoogleAuthHelper", "No Google account found on device: ${e.message}")
            AuthResult.Failure("No Google credentials found in Play Services. You can choose or enter your Google email.", canFallback = true)
        } catch (e: GetCredentialException) {
            Log.w("GoogleAuthHelper", "Credential manager error: ${e.message}")
            AuthResult.Failure("Google Sign-In: ${e.message ?: "Authentication required"}", canFallback = true)
        } catch (e: Exception) {
            Log.e("GoogleAuthHelper", "Unexpected error: ${e.message}", e)
            AuthResult.Failure(e.message ?: "Sign-In error", canFallback = true)
        }
    }
}
