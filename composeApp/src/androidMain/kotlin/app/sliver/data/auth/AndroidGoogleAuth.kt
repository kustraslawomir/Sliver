package app.sliver.data.auth

import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.activity.result.IntentSenderRequest
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import app.sliver.BuildConfig
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.RevokeAccessRequest
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

const val DRIVE_FILE_SCOPE = "https://www.googleapis.com/auth/drive.file"

val googleConfigured: Boolean get() = BuildConfig.GOOGLE_WEB_CLIENT_ID.isNotBlank()

/** Bridge to the resumed activity, needed for the credential sheet and the consent intent. */
object AuthHost {
    var activity: Activity? = null
    var launcher: ((IntentSenderRequest) -> Unit)? = null
    private var pending: CompletableDeferred<Intent?>? = null

    suspend fun resolve(intent: PendingIntent): Intent? {
        val launch = launcher ?: return null
        val result = CompletableDeferred<Intent?>().also { pending = it }
        launch(IntentSenderRequest.Builder(intent).build())
        return result.await()
    }

    fun onResult(data: Intent?) {
        pending?.complete(data)
        pending = null
    }
}

class AndroidGoogleAuth(private val context: Context, private val webClientId: String) : GoogleAuth {
    override val isFake = false
    private val scopes = listOf(Scope(DRIVE_FILE_SCOPE))

    override suspend fun signIn(): Result<String> = try {
        val activity = checkNotNull(AuthHost.activity) { "No activity" }
        val option = GetGoogleIdOption.Builder().setServerClientId(webClientId).setFilterByAuthorizedAccounts(false).build()
        val response = CredentialManager.create(activity).getCredential(activity, GetCredentialRequest(listOf(option)))
        val email = GoogleIdTokenCredential.createFrom(response.credential.data).id
        checkNotNull(authorize(interactive = true)?.accessToken) { "Drive access not granted" }
        Result.success(email)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun accessToken(): String? = try {
        authorize(interactive = false)?.accessToken
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }

    override suspend fun signOut() {
        runCatching { CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest()) }
    }

    override suspend fun revoke() {
        runCatching {
            val account = authorize(interactive = false)?.toGoogleSignInAccount()?.account
            if (account != null) {
                val request = RevokeAccessRequest.builder().setAccount(account).setScopes(scopes).build()
                Identity.getAuthorizationClient(context).revokeAccess(request).await()
            }
        }
        signOut()
    }

    private suspend fun authorize(interactive: Boolean): AuthorizationResult? {
        val client = Identity.getAuthorizationClient(AuthHost.activity?.takeIf { interactive } ?: context)
        val request = AuthorizationRequest.builder().setRequestedScopes(scopes).build()
        val result = client.authorize(request).await()
        if (!result.hasResolution()) return result
        if (!interactive) return null
        val data = AuthHost.resolve(checkNotNull(result.pendingIntent)) ?: return null
        return client.getAuthorizationResultFromIntent(data)
    }
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) }
    addOnFailureListener { cont.resumeWithException(it) }
    addOnCanceledListener { cont.cancel() }
}
