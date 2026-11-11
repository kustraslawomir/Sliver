package app.sliver

import app.sliver.data.auth.AccountManager
import app.sliver.data.auth.FakeGoogleAuth
import app.sliver.data.settings.AppSettings
import app.sliver.data.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class MemorySettings : SettingsRepository {
    private val state = MutableStateFlow(AppSettings())
    override val settings = state
    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        state.value = transform(state.value)
    }
}

class AccountManagerTest {
    @Test
    fun signInStoresEmailAndSignOutClearsIt() = runTest {
        val repo = MemorySettings()
        val auth = FakeGoogleAuth()
        val manager = AccountManager(auth, repo)

        assertTrue(manager.signIn().isSuccess)
        assertTrue(repo.settings.first().account.signedIn)
        assertEquals(FakeGoogleAuth.FAKE_EMAIL, repo.settings.first().account.email)
        assertEquals("fake-token", auth.accessToken())

        manager.signOut()
        assertFalse(repo.settings.first().account.signedIn)
        assertNull(repo.settings.first().account.email)
        assertNull(auth.accessToken())
    }
}
