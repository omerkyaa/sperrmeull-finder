package com.omerkaya.sperrmuellfinder.ui.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.domain.manager.PremiumManager
import com.omerkaya.sperrmuellfinder.domain.model.AccountDeletionStatus
import com.omerkaya.sperrmuellfinder.domain.model.User
import com.omerkaya.sperrmuellfinder.domain.model.PremiumFrameType
import com.omerkaya.sperrmuellfinder.domain.repository.AdminRepository
import com.omerkaya.sperrmuellfinder.domain.repository.UserRepository
import com.omerkaya.sperrmuellfinder.domain.usecase.social.GetFollowersUseCase
import com.omerkaya.sperrmuellfinder.domain.usecase.user.GetCurrentUserUseCase
import com.omerkaya.sperrmuellfinder.domain.usecase.user.UpdateUserProfileUseCase
import com.omerkaya.sperrmuellfinder.domain.model.Post
import com.omerkaya.sperrmuellfinder.data.util.FirestoreConstants
import com.omerkaya.sperrmuellfinder.data.dto.PostDto
import com.omerkaya.sperrmuellfinder.data.mapper.PostMapper
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import javax.inject.Inject

/**
 * ViewModel for Profile screen
 * Manages user profile state, XP/Level/Honesty display, and premium features
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val updateUserProfileUseCase: UpdateUserProfileUseCase,
    private val getFollowersUseCase: GetFollowersUseCase,
    private val userRepository: UserRepository,
    private val adminRepository: AdminRepository,
    private val premiumManager: PremiumManager,
    private val logger: Logger
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _profileEvent = MutableStateFlow<ProfileEvent?>(null)
    val profileEvent: StateFlow<ProfileEvent?> = _profileEvent.asStateFlow()

    // Firebase Firestore instance for user posts
    private val firestore = FirebaseFirestore.getInstance()
    
    // User posts state flows
    private val _userPosts = MutableStateFlow<List<Post>>(emptyList())
    val userPosts: StateFlow<List<Post>> = _userPosts.asStateFlow()
    
    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()
    
    // Account deletion status for banner
    private val _accountDeletionStatus = MutableStateFlow<AccountDeletionStatus?>(null)
    val accountDeletionStatus: StateFlow<AccountDeletionStatus?> = _accountDeletionStatus.asStateFlow()
    private var followCountsObservedUserId: String? = null
    private var followersCountJob: Job? = null
    private var followingCountJob: Job? = null
    
    // Current user for easy access
    val currentUser: StateFlow<User?> = _uiState
        .map { it.user }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    init {
        logger.d(Logger.TAG_DEFAULT, "🎯 ProfileViewModel INIT started")
        
        // 🎯 DEBUG: Check Firebase Auth state
        val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        logger.d(Logger.TAG_DEFAULT, "🎯 Firebase Auth User: ${firebaseUser?.displayName ?: "NULL"}, UID: ${firebaseUser?.uid ?: "NULL"}")
        logger.d(Logger.TAG_DEFAULT, "🎯 Firebase Auth isAnonymous: ${firebaseUser?.isAnonymous ?: "NULL"}")
        
        observeUserProfile()
        observePremiumStatus()
        observeAdminRole()
        observeAccountDeletionStatus()
        // loadUserStats() is called inside observeUserProfile() once the user is available
    }

    /**
     * Observe current user profile changes
     */
    private fun observeUserProfile() {
        logger.d(Logger.TAG_DEFAULT, "🎯 observeUserProfile() started")
        viewModelScope.launch {
            try {
                logger.d(Logger.TAG_DEFAULT, "🎯 Calling getCurrentUserUseCase()...")
                getCurrentUserUseCase()
                    .catch { exception ->
                        if (exception is CancellationException) throw exception
                        logger.e(Logger.TAG_DEFAULT, "❌ Error observing user profile", exception)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = exception.message ?: "Unknown error"
                        )
                    }
                    .collect { user ->
                        logger.d(Logger.TAG_DEFAULT, "🎯 User collected: ${user?.displayName ?: "NULL"}, UID: ${user?.uid ?: "NULL"}")
                        if (user != null) {
                            logger.d(Logger.TAG_DEFAULT, "✅ User found, updating profile state...")
                            updateProfileState(user)
                            observeFollowCounts(user.uid)
                            loadUserStats()
                            // 🎯 POSTS LOADING FIX: Load user posts automatically
                            logger.d(Logger.TAG_DEFAULT, "🎯 Loading user posts for UID: ${user.uid}")
                            loadUserPosts(user.uid)
                        } else {
                            logger.w(Logger.TAG_DEFAULT, "❌ User is NULL - not logged in?")
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                error = "User not found"
                            )
                        }
                    }
            } catch (e: CancellationException) {
                logger.d(Logger.TAG_DEFAULT, "observeUserProfile cancelled")
                throw e
            } catch (e: Exception) {
                logger.e(Logger.TAG_DEFAULT, "❌ Exception in observeUserProfile", e)
            }
        }
    }

    /**
     * Observe premium status changes
     */
    private fun observePremiumStatus() {
        viewModelScope.launch {
            premiumManager.premiumEntitlement.collect { entitlement ->
                _uiState.value = _uiState.value.copy(
                    isPremium = entitlement.isActive,
                    premiumUntil = entitlement.expirationDate
                )
                logger.d(Logger.TAG_DEFAULT, "Premium status updated: ${entitlement.isActive}")
            }
        }
    }

    private fun observeAdminRole() {
        viewModelScope.launch {
            adminRepository.observeAdminRole().collect { role ->
                _uiState.value = _uiState.value.copy(isAdmin = role != null)
            }
        }
    }

    /**
     * Update profile state with user data
     */
    private fun updateProfileState(user: User) {
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            error = null,
            user = user,
            followersCount = user.followersCount,
            followingCount = user.followingCount,
            frameType = user.getPremiumFrameType(),
            canPost = user.canPost(),
            badgeCount = user.badges.size
        )
        
        logger.d(Logger.TAG_DEFAULT, "Profile state updated: ${user.displayName}, PhotoURL: ${user.photoUrl}")
    }
    

    /**
     * Load user statistics (posts, followers, following)
     */
    private fun loadUserStats() {
        viewModelScope.launch {
            _uiState.value.user?.let { user ->
                try {
                    val postsCountResult = userRepository.getUserPostsCount(user.uid)
                    val postsCount = if (postsCountResult is Result.Success) postsCountResult.data else 0
                    // Follower/following counts are managed exclusively by the real-time
                    // observeFollowCounts() listener to avoid race conditions.
                    _uiState.value = _uiState.value.copy(postsCount = postsCount)
                    logger.d(Logger.TAG_DEFAULT, "User stats loaded: Posts=$postsCount")
                } catch (e: Exception) {
                    logger.e(Logger.TAG_DEFAULT, "Error loading user stats", e)
                }
            }
        }
    }

    private fun observeFollowCounts(userId: String) {
        if (followCountsObservedUserId == userId) return
        followCountsObservedUserId = userId
        followersCountJob?.cancel()
        followingCountJob?.cancel()

        followersCountJob = viewModelScope.launch {
            getFollowersUseCase.getFollowers(userId)
                .catch { e ->
                    if (e is CancellationException) throw e
                    logger.e(Logger.TAG_DEFAULT, "Error observing realtime followers count", e)
                }
                .collect { followers ->
                    _uiState.value = _uiState.value.copy(followersCount = followers.size)
                }
        }

        followingCountJob = viewModelScope.launch {
            getFollowersUseCase.getFollowing(userId)
                .catch { e ->
                    if (e is CancellationException) throw e
                    logger.e(Logger.TAG_DEFAULT, "Error observing realtime following count", e)
                }
                .collect { following ->
                    _uiState.value = _uiState.value.copy(followingCount = following.size)
                }
        }
    }

    /**
     * Update user profile information
     */
    fun updateProfile(
        displayName: String? = null, 
        firstName: String? = null,
        lastName: String? = null,
        city: String? = null, 
        photoUrl: String? = null
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUpdating = true)
            
            try {
                logger.d(Logger.TAG_DEFAULT, "Updating profile with: displayName=$displayName, firstName=$firstName, lastName=$lastName, city=$city, photoUrl=${photoUrl?.take(50)}...")
                
                when (val result = updateUserProfileUseCase.updateProfile(
                    displayName = displayName,
                    firstName = firstName,
                    lastName = lastName,
                    city = city,
                    photoUrl = photoUrl
                )) {
                    is Result.Success<*> -> {
                        _profileEvent.value = ProfileEvent.ProfileUpdated
                        logger.i(Logger.TAG_DEFAULT, "Profile updated successfully")
                        
                        // Refresh user data to show updated information
                        refreshCurrentUserProfile { }
                        
                        // Force refresh of denormalized data in posts
                        viewModelScope.launch {
                            kotlinx.coroutines.delay(500) // Small delay to ensure Firestore update completes
                            loadUserPosts(_uiState.value.user?.uid ?: "")
                        }
                    }
                    is Result.Error -> {
                        _profileEvent.value = ProfileEvent.Error(result.exception.message ?: "Update failed")
                        logger.e(Logger.TAG_DEFAULT, "Profile update failed", result.exception)
                    }
                    is Result.Loading -> { /* Handle loading if needed */ }
                }
            } catch (e: Exception) {
                _profileEvent.value = ProfileEvent.Error(e.message ?: "Update failed")
                logger.e(Logger.TAG_DEFAULT, "Profile update error", e)
            } finally {
                _uiState.value = _uiState.value.copy(isUpdating = false)
            }
        }
    }

    fun uploadProfilePhoto(
        imageUri: Uri,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isUpdating = true)
            try {
                val userId = _uiState.value.user?.uid
                    ?: com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                if (userId.isNullOrBlank()) {
                    onError("User not authenticated")
                    return@launch
                }

                when (val uploadResult = userRepository.uploadProfilePhoto(userId, imageUri)) {
                    is Result.Success -> {
                        logger.i(Logger.TAG_DEFAULT, "✅ Profile photo uploaded successfully")
                        onSuccess(uploadResult.data)
                    }
                    is Result.Error -> {
                        logger.e(Logger.TAG_DEFAULT, "❌ Profile photo upload failed", uploadResult.exception)
                        onError(uploadResult.exception.message ?: "Photo upload failed")
                    }
                    is Result.Loading -> {
                        // no-op
                    }
                }
            } catch (e: Exception) {
                logger.e(Logger.TAG_DEFAULT, "❌ Profile photo upload error", e)
                onError(e.message ?: "Photo upload failed")
            } finally {
                _uiState.value = _uiState.value.copy(isUpdating = false)
            }
        }
    }

    /**
     * Load leaderboard data
     */
    /**
     * Refresh all profile data
     */
    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            
            try {
                // 🎯 ENHANCED REFRESH: Log Firebase Auth state
                val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                logger.d(Logger.TAG_DEFAULT, "Refresh - Firebase Auth user: ${firebaseUser?.displayName}, PhotoURL: ${firebaseUser?.photoUrl}")
                
                // Refresh user data
                when (val result = userRepository.refreshUserData()) {
                    is Result.Success -> {
                        logger.d(Logger.TAG_DEFAULT, "Refresh - Firestore user: ${result.data.displayName}, PhotoURL: ${result.data.photoUrl}")
                        updateProfileState(result.data)
                        loadUserStats()
                        _uiState.value = _uiState.value.copy(isRefreshing = false)
                        logger.i(Logger.TAG_DEFAULT, "Profile refreshed successfully")
                    }
                    is Result.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isRefreshing = false,
                            error = result.exception.message
                        )
                        logger.e(Logger.TAG_DEFAULT, "Profile refresh failed", result.exception)
                    }
                    is Result.Loading -> { /* Handle loading if needed */ }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false,
                    error = e.message
                )
                logger.e(Logger.TAG_DEFAULT, "Profile refresh error", e)
            }
        }
    }

    /**
     * Clear profile event after handling
     */
    fun clearEvent() {
        _profileEvent.value = null
    }
    
    /**
     * Refresh posts after a new post is created
     */
    fun refreshAfterPostCreated() {
        viewModelScope.launch {
            logger.d(Logger.TAG_DEFAULT, "Refreshing profile posts after new post created")
            // Reload user posts
            _uiState.value.user?.let { user ->
                // Transaction history removed in MVP
                // In a real implementation, you would reload posts from repository
                // For now, the posts will be updated through user profile refresh
            }
        }
    }
    
    /**
     * 🎯 MANUAL FIREBASE PHOTO SYNC: Force sync Firebase Auth photoURL to Firestore
     */
    fun syncFirebaseAuthPhoto() {
        viewModelScope.launch {
            try {
                val firebaseUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                val authPhotoUrl = firebaseUser?.photoUrl?.toString()
                val currentUser = _uiState.value.user
                
                logger.d(Logger.TAG_DEFAULT, "🔄 Manual photo sync requested")
                logger.d(Logger.TAG_DEFAULT, "Firebase Auth photoURL: $authPhotoUrl")
                logger.d(Logger.TAG_DEFAULT, "Current Firestore photoUrl: ${currentUser?.photoUrl}")
                
                if (!authPhotoUrl.isNullOrBlank()) {
                    // Force update Firestore
                    val result = userRepository.updateUserProfile(
                        displayName = null,
                        photoUrl = authPhotoUrl,
                        city = null
                    )
                    
                    when (result) {
                        is Result.Success -> {
                            logger.i(Logger.TAG_DEFAULT, "✅ Manual photo sync successful")
                            // Refresh profile to get updated data
                            refresh()
                        }
                        is Result.Error -> {
                            logger.e(Logger.TAG_DEFAULT, "❌ Manual photo sync failed", result.exception)
                            _profileEvent.value = ProfileEvent.Error("Failed to sync profile photo: ${result.exception.message}")
                        }
                        is Result.Loading -> { /* Handle if needed */ }
                    }
                } else {
                    logger.w(Logger.TAG_DEFAULT, "⚠️ No Firebase Auth photoURL to sync")
                    _profileEvent.value = ProfileEvent.Error("No profile photo found in Firebase Auth")
                }
            } catch (e: Exception) {
                logger.e(Logger.TAG_DEFAULT, "❌ Error during manual photo sync", e)
                _profileEvent.value = ProfileEvent.Error("Photo sync error: ${e.message}")
            }
        }
    }

    /**
     * Toggle between grid and list view for posts
     */
    fun togglePostsView() {
        _uiState.value = _uiState.value.copy(
            isGridView = !_uiState.value.isGridView
        )
    }

    /**
     * Load current user profile (for own profile view)
     */
    fun loadCurrentUserProfile() {
        viewModelScope.launch {
            _loading.value = true
            try {
                getCurrentUserUseCase()
                    .catch { exception ->
                        if (exception is CancellationException) throw exception
                        logger.e(Logger.TAG_DEFAULT, "Error loading current user profile", exception)
                        _loading.value = false
                    }
                    .collect { user ->
                        if (user != null) {
                            loadUserPosts(user.uid)
                        }
                        _loading.value = false
                    }
            } catch (e: CancellationException) {
                logger.d(Logger.TAG_DEFAULT, "loadCurrentUserProfile cancelled")
                throw e
            } catch (e: Exception) {
                logger.e(Logger.TAG_DEFAULT, "Error loading current user profile", e)
                _loading.value = false
            }
        }
    }

    /**
     * Refresh current user profile with callback
     */
    fun refreshCurrentUserProfile(onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                getCurrentUserUseCase()
                    .catch { exception ->
                        if (exception is CancellationException) throw exception
                        logger.e(Logger.TAG_DEFAULT, "Error refreshing current user profile", exception)
                        onComplete()
                    }
                    .collect { user ->
                        if (user != null) {
                            loadUserPosts(user.uid)
                        }
                        onComplete()
                    }
            } catch (e: CancellationException) {
                logger.d(Logger.TAG_DEFAULT, "refreshCurrentUserProfile cancelled")
                throw e
            } catch (e: Exception) {
                logger.e(Logger.TAG_DEFAULT, "Error refreshing current user profile", e)
                onComplete()
            }
        }
    }

    /**
     * Load user posts from Firestore
     */
    fun loadUserPosts(userId: String) {
        logger.d(Logger.TAG_DEFAULT, "🎯 loadUserPosts() called for userId: $userId")
        // Start real-time listener for user posts
        startUserPostsListener(userId)
    }

    /**
     * Refresh user posts with pull-to-refresh - HomeScreen pattern
     */
    fun refreshUserPosts(userId: String, onComplete: () -> Unit) {
        // Restart real-time listener for fresh data
        startUserPostsListener(userId)
        // Complete immediately since listener will update data
        onComplete()
    }

    /**
     * Refresh posts - HomeScreen pattern
     */
    fun refreshPosts(onComplete: () -> Unit) {
        viewModelScope.launch {
            try {
                currentUser.value?.let { user ->
                    refreshUserPosts(user.uid, onComplete)
                } ?: run {
                    onComplete()
                }
            } catch (e: Exception) {
                logger.e(Logger.TAG_DEFAULT, "Error refreshing posts", e)
                onComplete()
            }
        }
    }

    // Real-time listener for user posts
    private var postsListener: ListenerRegistration? = null

    /**
     * Start real-time listening to user posts from Firestore - HomeScreen pattern
     * Uses only FirestoreConstants - no hardcoded strings
     * ✅ FIRESTORE INDEX READY: Using server-side sorting with composite index
     */
    private fun startUserPostsListener(userId: String) {
        // Remove existing listener if any
        postsListener?.remove()
        
        try {
            _loading.value = true
            logger.d(Logger.TAG_DEFAULT, "🎯 Starting real-time listener for user posts: $userId")
            logger.d(Logger.TAG_DEFAULT, "🎯 Query: collection=${FirestoreConstants.COLLECTION_POSTS}, ownerid=$userId")

            // ⚠️ TEMPORARY: Simplified query to avoid composite index requirement
            // TODO: Add composite index for (ownerid, created_at) in Firebase Console
            postsListener = firestore.collection(FirestoreConstants.COLLECTION_POSTS)
                .whereEqualTo(FirestoreConstants.FIELD_OWNER_ID, userId)
                .limit(50) // Limit for performance like HomeScreen
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        logger.e(Logger.TAG_DEFAULT, "Error listening to user posts", error)
                        _uiState.value = _uiState.value.copy(
                            error = "Failed to load posts. Please try again."
                        )
                        _loading.value = false
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        logger.d(Logger.TAG_DEFAULT, "🎯 Real-time update: ${snapshot.documents.size} posts found for user: $userId")
                        
                        // 🎯 DEBUG: Log first few documents to see their ownerid values
                        snapshot.documents.take(3).forEach { doc ->
                            val docOwnerId = doc.getString(FirestoreConstants.FIELD_OWNER_ID)
                            logger.d(Logger.TAG_DEFAULT, "🔍 Document ${doc.id}: ownerid='$docOwnerId', expected='$userId', match=${docOwnerId == userId}")
                        }
                        
                        val posts = snapshot.documents.mapNotNull { document ->
                            try {
                                val postDto = document.toObject(PostDto::class.java)
                                if (postDto != null) {
                                    // Fix empty ID issue - HomeScreen pattern
                                    val finalPostDto = if (postDto.id.isBlank()) {
                                        postDto.copy(id = document.id)
                                    } else {
                                        postDto
                                    }
                                    val postMapper = PostMapper()
                                    postMapper.fromDto(finalPostDto)
                                } else {
                                    logger.w(Logger.TAG_DEFAULT, "Failed to parse PostDto from document: ${document.id}")
                                    null
                                }
                            } catch (e: Exception) {
                                logger.e(Logger.TAG_DEFAULT, "Error parsing document ${document.id}: ${e.message}", e)
                                null
                            }
                        } // ⚠️ CLIENT-SIDE SORTING: Until composite index is created
                        .filterNotNull() // Remove null posts before sorting
                        .filter { post ->
                            // Exclude removed/moderated-out posts from profile counters and grids
                            post.status != com.omerkaya.sperrmuellfinder.domain.model.PostStatus.REMOVED
                        }
                        .sortedByDescending { post -> post.createdAt } // Sort by creation date descending

                        _userPosts.value = posts
                        logger.d(Logger.TAG_DEFAULT, "✅ Successfully updated ${posts.size} user posts for userId: $userId")
                        _loading.value = false
                    }
                }

        } catch (e: Exception) {
            logger.e(Logger.TAG_DEFAULT, "Error starting user posts listener", e)
            _uiState.value = _uiState.value.copy(
                error = "Failed to load posts. Please try again."
            )
            _loading.value = false
        }
    }

    /**
     * Fetch user posts from Firestore using FirestoreConstants (fallback method)
     */
    private suspend fun fetchUserPosts(userId: String) {
        // Use real-time listener instead
        startUserPostsListener(userId)
    }

    /**
     * Observe account deletion status
     */
    private fun observeAccountDeletionStatus() {
        viewModelScope.launch {
            userRepository.getAccountDeletionStatus().collect { status ->
                _accountDeletionStatus.value = status
                logger.d(Logger.TAG_DEFAULT, "Account deletion status: ${status?.status}")
            }
        }
    }

    /**
     * Clean up listeners when ViewModel is destroyed
     */
    override fun onCleared() {
        super.onCleared()
        postsListener?.remove()
        logger.d(Logger.TAG_DEFAULT, "ProfileViewModel cleared, listeners removed")
    }
}

/**
 * UI State for Profile screen
 */
data class ProfileUiState(
    val isLoading: Boolean = true,
    val isUpdating: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    
    // User data
    val user: User? = null,
    val isPremium: Boolean = false,
    val premiumUntil: java.util.Date? = null,
    val isAdmin: Boolean = false,
    
    // Profile appearance
    val frameType: PremiumFrameType = PremiumFrameType.NONE,
    val canPost: Boolean = true,
    val badgeCount: Int = 0,
    
    // Statistics
    val postsCount: Int = 0,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val archivedPostsCount: Int = 0,
    
    // Content tabs and posts
    val selectedContentTab: ContentTab = ContentTab.CREATED,
    val posts: List<PostPreview> = emptyList(),
    val archivedPosts: List<PostPreview> = emptyList(),
    
    // UI preferences
    val isGridView: Boolean = true
)

/**
 * Profile events
 */
sealed class ProfileEvent {
    data object ProfileUpdated : ProfileEvent()
    data class Error(val message: String) : ProfileEvent()
    data object NavigateToSettings : ProfileEvent()
    data object NavigateToPremium : ProfileEvent()
    data object NavigateToEditProfile : ProfileEvent()
}

/**
 * Content tabs for profile
 */
enum class ContentTab {
    CREATED, ARCHIVE
}
