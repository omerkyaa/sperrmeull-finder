package com.omerkaya.sperrmuellfinder.data.repository

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.data.util.FirestoreConstants.COLLECTION_USERS
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class UserRepositoryIntegrationTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var firestore: FirebaseFirestore

    @Inject
    lateinit var firebaseAuth: FirebaseAuth

    private lateinit var testUserId: String

    @Before
    fun setup() {
        hiltRule.inject()

        // Create test user
        firebaseAuth.signInAnonymously().await()
        testUserId = firebaseAuth.currentUser!!.uid
    }

    @After
    fun cleanup() = runTest {
        // Clean up test user data
        firestore.collection(COLLECTION_USERS)
            .document(testUserId)
            .delete()
            .await()

        firebaseAuth.signOut()
    }

    @Test
    fun ensureUserDocument_createsNewDocument() = runTest {
        // When
        val result = userRepository.ensureUserDocument("test-fcm-token")

        // Then
        assertTrue(result is Result.Success)
        val user = result.data!!

        // Verify user data
        assertEquals(testUserId, user.uid)
        assertEquals(100, user.xp) // Initial XP
        assertEquals(1, user.level) // Initial level
        assertEquals(100, user.honesty) // Initial honesty
        assertEquals(false, user.isPremium)
        assertEquals("test-fcm-token", user.fcmToken)

        // Verify Firestore document
        val doc = firestore.collection(COLLECTION_USERS)
            .document(testUserId)
            .get()
            .await()

        assertTrue(doc.exists())
        assertEquals(testUserId, doc.getString("uid"))
        assertEquals(100, doc.getLong("xp"))
        assertEquals(1, doc.getLong("level"))
        assertEquals(100, doc.getLong("honesty"))
        assertEquals(false, doc.getBoolean("isPremium"))
        assertEquals("test-fcm-token", doc.getString("fcmToken"))
        assertNotNull(doc.getTimestamp("createdAt"))
        assertNotNull(doc.getTimestamp("updatedAt"))
        assertNotNull(doc.getTimestamp("lastLoginAt"))
    }

    @Test
    fun ensureUserDocument_patchesExistingDocument() = runTest {
        // Given
        val initialResult = userRepository.ensureUserDocument("old-token")
        assertTrue(initialResult is Result.Success)

        // When
        val result = userRepository.ensureUserDocument("new-token")

        // Then
        assertTrue(result is Result.Success)
        val user = result.data!!

        // Verify user data
        assertEquals(testUserId, user.uid)
        assertEquals("new-token", user.fcmToken)

        // Verify Firestore document
        val doc = firestore.collection(COLLECTION_USERS)
            .document(testUserId)
            .get()
            .await()

        assertTrue(doc.exists())
        assertEquals("new-token", doc.getString("fcmToken"))
    }

    @Test
    fun getCurrentUser_returnsCorrectUser() = runTest {
        // Given
        val createResult = userRepository.ensureUserDocument("test-token")
        assertTrue(createResult is Result.Success)

        // When
        val user = userRepository.getCurrentUser().first()

        // Then
        assertNotNull(user)
        assertEquals(testUserId, user.uid)
        assertEquals("test-token", user.fcmToken)
    }

    @Test
    fun updateUserProfile_updatesCorrectFields() = runTest {
        // Given
        val createResult = userRepository.ensureUserDocument("test-token")
        assertTrue(createResult is Result.Success)

        // When
        val updateResult = userRepository.updateUserProfile(
            displayName = "Test User",
            city = "Test City",
            photoUrl = "https://example.com/photo.jpg"
        )

        // Then
        assertTrue(updateResult is Result.Success)

        // Verify Firestore document
        val doc = firestore.collection(COLLECTION_USERS)
            .document(testUserId)
            .get()
            .await()

        assertTrue(doc.exists())
        assertEquals("Test User", doc.getString("displayName"))
        assertEquals("Test City", doc.getString("city"))
        assertEquals("https://example.com/photo.jpg", doc.getString("photoUrl"))
    }

    @Test
    fun updateUserXp_calculatesLevelCorrectly() = runTest {
        // Given
        val createResult = userRepository.ensureUserDocument("test-token")
        assertTrue(createResult is Result.Success)

        // When
        val updateResult = userRepository.updateUserXp(xpDelta = 150, reason = "test")

        // Then
        assertTrue(updateResult is Result.Success)
        val user = updateResult.data!!

        // Initial XP (100) + delta (150) = 250
        assertEquals(250, user.xp)
        // Level 2 requires 250 XP (50 + 1 * 1 * 100)
        assertEquals(2, user.level)

        // Verify Firestore document
        val doc = firestore.collection(COLLECTION_USERS)
            .document(testUserId)
            .get()
            .await()

        assertTrue(doc.exists())
        assertEquals(250L, doc.getLong("xp"))
        assertEquals(2L, doc.getLong("level"))
    }

    @Test
    fun updateUserHonesty_respectsBounds() = runTest {
        // Given
        val createResult = userRepository.ensureUserDocument("test-token")
        assertTrue(createResult is Result.Success)

        // When - try to exceed max honesty (100)
        val exceedResult = userRepository.updateUserHonesty(honestyDelta = 50, reason = "test")
        assertTrue(exceedResult is Result.Success)
        assertEquals(100, exceedResult.data!!.honesty) // Should stay at 100

        // When - try to go below min honesty (0)
        val belowResult = userRepository.updateUserHonesty(honestyDelta = -150, reason = "test")
        assertTrue(belowResult is Result.Success)
        assertEquals(0, belowResult.data!!.honesty) // Should stay at 0
    }

    @Test
    fun deleteUser_removesAllUserData() = runTest {
        // Given
        val createResult = userRepository.ensureUserDocument("test-token")
        assertTrue(createResult is Result.Success)

        // When
        val deleteResult = userRepository.deleteUser()

        // Then
        assertTrue(deleteResult is Result.Success)

        // Verify Firestore document is gone
        val doc = firestore.collection(COLLECTION_USERS)
            .document(testUserId)
            .get()
            .await()

        assertTrue(!doc.exists())
    }
}
