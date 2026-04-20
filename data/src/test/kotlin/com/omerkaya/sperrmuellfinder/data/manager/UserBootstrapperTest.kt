package com.omerkaya.sperrmuellfinder.data.manager

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.data.dto.user.UserDto
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class UserBootstrapperTest {
    
    private lateinit var firestore: FirebaseFirestore
    private lateinit var logger: Logger
    private lateinit var userBootstrapper: UserBootstrapper
    private lateinit var docRef: DocumentReference
    private lateinit var snapshot: DocumentSnapshot
    private lateinit var authUser: FirebaseUser
    
    @Before
    fun setup() {
        firestore = mockk(relaxed = true)
        logger = mockk(relaxed = true)
        docRef = mockk(relaxed = true)
        snapshot = mockk(relaxed = true)
        authUser = mockk(relaxed = true)
        
        every { firestore.collection(any()).document(any()) } returns docRef
        every { docRef.get() } returns Tasks.forResult(snapshot)
        
        every { authUser.uid } returns "test-uid"
        every { authUser.email } returns "test@example.com"
        every { authUser.displayName } returns "Test User"
        every { authUser.photoUrl } returns null
        
        userBootstrapper = UserBootstrapper(firestore, logger)
    }
    
    @Test
    fun `ensureUserDocument creates new document when it doesn't exist`() = runTest {
        // Given
        every { snapshot.exists() } returns false
        
        val dataSlot = slot<Map<String, Any?>>()
        every { docRef.set(capture(dataSlot)) } returns Tasks.forResult(null)
        
        val fcmToken = "test-token"
        
        // When
        val result = userBootstrapper.ensureUserDocument(authUser) { fcmToken }
        
        // Then
        verify { firestore.collection("users") }
        verify { docRef.get() }
        verify { docRef.set(any()) }
        
        assertNotNull(result)
        assertEquals("test-uid", dataSlot.captured["uid"])
        assertEquals("test@example.com", dataSlot.captured["email"])
        assertEquals("Test User", dataSlot.captured["displayName"])
        assertEquals(100, dataSlot.captured["xp"])
        assertEquals(1, dataSlot.captured["level"])
        assertEquals(100, dataSlot.captured["honesty"])
        assertEquals(false, dataSlot.captured["isPremium"])
        assertEquals(fcmToken, dataSlot.captured["fcmToken"])
    }
    
    @Test
    fun `ensureUserDocument patches existing document`() = runTest {
        // Given
        every { snapshot.exists() } returns true
        every { snapshot.toObject(UserDto::class.java) } returns UserDto(
            uid = "test-uid",
            email = "old@example.com",
            displayName = "Old Name",
            photoUrl = "",
            xp = 100,
            level = 1,
            honesty = 100,
            isPremium = false,
            premiumUntil = null,
            fcmToken = "old-token",
            deviceLang = "en",
            deviceModel = "old-model",
            deviceOs = "old-os"
        )
        
        val updatesSlot = slot<Map<String, Any?>>()
        every { docRef.set(capture(updatesSlot), any<SetOptions>()) } returns Tasks.forResult(null)
        
        val newFcmToken = "new-token"
        
        // When
        val result = userBootstrapper.ensureUserDocument(authUser) { newFcmToken }
        
        // Then
        verify { firestore.collection("users") }
        verify { docRef.get() }
        verify { docRef.set(any(), any()) }
        
        assertNotNull(result)
        with(updatesSlot.captured) {
            assertEquals("test@example.com", get("email"))
            assertEquals("Test User", get("displayName"))
            assertEquals("new-token", get("fcmToken"))
            assertNotNull(get("updatedAt"))
            assertNotNull(get("lastLoginAt"))
        }
    }
    
    @Test
    fun `ensureUserDocument handles missing FCM token`() = runTest {
        // Given
        every { snapshot.exists() } returns false
        
        val dataSlot = slot<Map<String, Any?>>()
        every { docRef.set(capture(dataSlot)) } returns Tasks.forResult(null)
        
        // When
        val result = userBootstrapper.ensureUserDocument(authUser) { null }
        
        // Then
        verify { firestore.collection("users") }
        verify { docRef.get() }
        verify { docRef.set(any()) }
        
        assertNotNull(result)
        assertEquals("test-uid", dataSlot.captured["uid"])
        assertEquals(null, dataSlot.captured["fcmToken"])
    }
    
    @Test
    fun `ensureUserDocument is idempotent`() = runTest {
        // Given
        every { snapshot.exists() } returns true
        val existingUser = UserDto(
            uid = "test-uid",
            email = "test@example.com",
            displayName = "Test User",
            photoUrl = "",
            xp = 100,
            level = 1,
            honesty = 100,
            isPremium = false,
            premiumUntil = null,
            fcmToken = "test-token",
            deviceLang = "en",
            deviceModel = "test-model",
            deviceOs = "test-os"
        )
        every { snapshot.toObject(UserDto::class.java) } returns existingUser
        
        val updatesSlot = slot<Map<String, Any?>>()
        every { docRef.set(capture(updatesSlot), any<SetOptions>()) } returns Tasks.forResult(null)
        
        // When
        val result = userBootstrapper.ensureUserDocument(authUser) { "test-token" }

        // Then
        verify { firestore.collection("users") }
        verify { docRef.get() }
        assertNotNull(result)

        // Only timestamps should be updated
        with(updatesSlot.captured) {
            assertNotNull(get("updatedAt"))
            assertNotNull(get("lastLoginAt"))
        }
    }
}
