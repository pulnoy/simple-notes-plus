package dev.dettmer.simplenotes.sync.drive

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DriveSyncStatusStoreTest {
    private val values = mutableMapOf<String, Any?>()
    private val editor = mockk<SharedPreferences.Editor>(relaxed = true)
    private val prefs = mockk<SharedPreferences>(relaxed = true) {
        every { edit() } returns editor
        every { getLong(any(), any()) } answers { values[firstArg()] as? Long ?: secondArg<Long>() }
        every { getString(any(), any()) } answers { values[firstArg()] as? String ?: secondArg<String?>() }
    }
    private val context = mockk<Context> { every { getSharedPreferences(any(), any()) } returns prefs }

    init {
        every { editor.putLong(any(), any()) } answers { values[firstArg()] = secondArg<Long>(); editor }
        every { editor.putString(any(), any()) } answers { values[firstArg()] = secondArg<String?>(); editor }
        every { editor.remove(any()) } answers { values.remove(firstArg<String>()); editor }
    }

    @Test fun `failure preserves last successful time after store recreation and is account scoped`() = runBlocking {
        val store = DriveSyncStatusStore(context)
        store.track("A") { "ok" }
        val success = store.read("A").lastSuccess
        assertTrue(success > 0L)
        runCatching { store.track("A") { throw IOException("401 unauthorized") } }
        val recreated = DriveSyncStatusStore(context)
        assertEquals(success, recreated.read("A").lastSuccess)
        assertEquals(DriveSyncProblem.AUTHORIZATION, recreated.read("A").problem)
        assertEquals(DriveSyncRecord(), recreated.read("B"))
        recreated.track("A") { "recovered" }
        assertNull(recreated.read("A").problem)
    }

    @Test fun `cancelled work does not create a persistent error`() = runBlocking {
        val store = DriveSyncStatusStore(context)
        runCatching { store.track("A") { throw CancellationException("cancelled") } }
        assertEquals(DriveSyncRecord(), store.read("A"))
    }
}
