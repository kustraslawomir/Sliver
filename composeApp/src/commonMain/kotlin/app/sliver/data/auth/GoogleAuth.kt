package app.sliver.data.auth

import app.sliver.data.settings.SettingsRepository

interface GoogleAuth {
    /** True when no real OAuth client is configured and sign-in is simulated locally. */
    val isFake: Boolean

    /** Sign in and grant the `drive.file` scope. Returns the account email. */
    suspend fun signIn(): Result<String>

    /** Silent refresh. Null when the user must sign in again. */
    suspend fun accessToken(): String?

    suspend fun signOut()

    /** Revoke the Drive grant for this app. */
    suspend fun revoke()
}

class FakeGoogleAuth : GoogleAuth {
    override val isFake = true
    private var email: String? = null

    override suspend fun signIn(): Result<String> = Result.success(FAKE_EMAIL.also { email = it })

    override suspend fun accessToken(): String? = email?.let { "fake-token" }

    override suspend fun signOut() {
        email = null
    }

    override suspend fun revoke() {
        email = null
    }

    companion object {
        const val FAKE_EMAIL = "Demo account"
    }
}

class AccountManager(private val auth: GoogleAuth, private val repo: SettingsRepository) {
    val isFake get() = auth.isFake

    suspend fun signIn(): Result<String> = auth.signIn().onSuccess { email ->
        repo.update { it.copy(account = it.account.copy(signedIn = true, email = email)) }
    }

    suspend fun signOut() {
        auth.signOut()
        repo.update { it.copy(account = it.account.copy(signedIn = false, email = null, driveFolderId = null)) }
    }

    suspend fun revoke() {
        auth.revoke()
        signOut()
    }
}
