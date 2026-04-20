package com.omerkaya.sperrmuellfinder.data.repository

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.data.manager.RevenueCatManager
import com.omerkaya.sperrmuellfinder.domain.model.premium.PremiumEntitlement
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PremiumRepositoryImplTest {

    private lateinit var revenueCatManager: RevenueCatManager
    private lateinit var firestore: FirebaseFirestore
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var logger: Logger
    private lateinit var user: FirebaseUser
    private lateinit var usersCollection: CollectionReference
    private lateinit var userDocument: DocumentReference
    private lateinit var repository: PremiumRepositoryImpl

    @Before
    fun setup() {
        revenueCatManager = mockk(relaxed = true)
        firestore = mockk(relaxed = true)
        firebaseAuth = mockk(relaxed = true)
        logger = mockk(relaxed = true)
        user = mockk(relaxed = true)
        usersCollection = mockk(relaxed = true)
        userDocument = mockk(relaxed = true)
        every { firebaseAuth.currentUser } returns user
        every { user.uid } returns "uid-123"
        every { firestore.collection("users") } returns usersCollection
        every { usersCollection.document("uid-123") } returns userDocument
        every { userDocument.update(any<Map<String, Any?>>()) } returns Tasks.forResult(null)

        repository = PremiumRepositoryImpl(
            revenueCatManager = revenueCatManager,
            firestore = firestore,
            firebaseAuth = firebaseAuth,
            logger = logger
        )
    }

    @Test
    fun `syncPremiumStatusWithFirestore writes lowercase premium fields`() = runTest {
        val updateSlot = slot<Map<String, Any?>>()
        every { userDocument.update(capture(updateSlot)) } returns Tasks.forResult(null)

        val entitlement = PremiumEntitlement(
            isActive = true
        )

        val result = repository.syncPremiumStatusWithFirestore(entitlement)

        assertTrue(result is Result.Success)
        verify { userDocument.update(any<Map<String, Any?>>()) }
        assertTrue(updateSlot.captured.containsKey("ispremium"))
        assertTrue(updateSlot.captured.containsKey("premiumuntil"))
    }
}
