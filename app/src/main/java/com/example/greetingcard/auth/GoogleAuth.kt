package com.example.greetingcard.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import com.example.greetingcard.BuildConfig
import com.example.greetingcard.network.AuthApi
import com.example.greetingcard.network.TokenRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import java.security.MessageDigest
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

const val TAG = "GoogleAuthLogger"

class GoogleAuth(private val context: Context, private val scope: CoroutineScope) {
    private val googleSignInOption: GetSignInWithGoogleOption =
            GetSignInWithGoogleOption.Builder(serverClientId = BuildConfig.ServerClientId)
                    .setNonce(nonce = generateNonce())
                    .build()

    private val request = GetCredentialRequest(credentialOptions = listOf(googleSignInOption))

    private val credentialManager = CredentialManager.create(context)

    suspend fun signInWithGoogle() = coroutineScope {
        try {
            val res = credentialManager.getCredential(request = request, context = context)
            // handle credential…
            handleSignIn(res)
        } catch (e: Exception) {
            // handle error…
        }
    }

    private fun handleSignIn(res: GetCredentialResponse) {
        val cred = res.credential

        when (cred) {
            is CustomCredential -> {
                if (cred.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    try {
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(cred.data)
                        googleIdTokenCredential.displayName?.let { Log.i(TAG, it) }
                        val token = googleIdTokenCredential.idToken

                        scope.launch {
                            try {
                                Log.i(TAG, token)
                                val validationResult =
                                        AuthApi.retrofitService.postValidityToken(
                                                TokenRequest(token)
                                        )
                                Log.i(TAG, "Got the server validation res")
                                Log.i(TAG, validationResult.result.toString())
                            } catch (e: Exception) {
                                Log.e(TAG, "error in here for validation against the server", e)
                            }
                        }
                    } catch (e: GoogleIdTokenParsingException) {
                        Log.e(TAG, "Received an invalid google id token response", e)
                    }
                } else {
                    Log.e(TAG, "Unexpected custom cred type")
                }
            }
            else -> {
                Log.e(TAG, "Unexpected custom cred type")
            }
        }
    }

    /** Generate a secure nonce for the authentication request */
    private fun generateNonce(): String {
        val rawNonce = UUID.randomUUID().toString()
        val bytes = rawNonce.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)
        return digest.fold("") { str, it -> str + "%02x".format(it) }
    }
}
