package app.sliver

import app.sliver.data.auth.AccountDeleter
import app.sliver.data.auth.AccountManager
import app.sliver.data.auth.FakeGoogleAuth
import app.sliver.data.backup.InMemoryDriveClient
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class AccountDeleterTest {
    private class Fixture {
        val repo = MemorySettings()
        val auth = FakeGoogleAuth()
        val drive = InMemoryDriveClient()
        var localDeleted = false
        val deleter = AccountDeleter(AccountManager(auth, repo), drive, repo) { localDeleted = true }
    }

    @Test
    fun revokeOnlyKeepsDriveAndLocal() = runTest {
        val f = Fixture()
        f.auth.signIn()
        val folder = f.drive.ensureFolder()
        f.drive.upload(folder, "a", emptyMap(), byteArrayOf(1))
        assertEquals(true, f.deleter.delete(false, false).isSuccess)
        assertFalse(f.localDeleted)
        assertEquals(1, f.drive.list(folder).size)
        assertNull(f.auth.accessToken())
        assertFalse(f.repo.settings.first().account.signedIn)
    }

    @Test
    fun deletesDriveAndLocalWhenChosen() = runTest {
        val f = Fixture()
        f.auth.signIn()
        val folder = f.drive.ensureFolder()
        f.drive.upload(folder, "a", emptyMap(), byteArrayOf(1))
        f.deleter.delete(true, true)
        assertEquals(true, f.localDeleted)
        assertEquals(0, f.drive.list(folder).size)
        assertNull(f.auth.accessToken())
    }
}
