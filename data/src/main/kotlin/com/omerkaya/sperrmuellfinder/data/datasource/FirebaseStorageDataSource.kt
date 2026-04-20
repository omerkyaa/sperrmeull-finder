package com.omerkaya.sperrmuellfinder.data.datasource

import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.omerkaya.sperrmuellfinder.core.util.Result
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseStorageDataSource @Inject constructor(
    private val storage: FirebaseStorage
) {
    private val postsRef: StorageReference = storage.reference.child("posts")

    suspend fun uploadImage(file: File): Result<String> {
        return try {
            val fileName = "${UUID.randomUUID()}.jpg"
            val imageRef = postsRef.child(fileName)
            
            imageRef.putFile(android.net.Uri.fromFile(file)).await()
            val downloadUrl = imageRef.downloadUrl.await()
            
            Result.Success(downloadUrl.toString())
        } catch (e: Exception) {
            Result.Error(e)
        }
    }
}
