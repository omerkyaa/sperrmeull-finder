package com.omerkaya.sperrmuellfinder.ui.postdetail

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.omerkaya.sperrmuellfinder.core.navigation.TopLevelDestination
import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.domain.model.Comment
import com.omerkaya.sperrmuellfinder.domain.model.Post
import com.omerkaya.sperrmuellfinder.domain.model.ReportReason
import com.omerkaya.sperrmuellfinder.domain.model.User
import com.omerkaya.sperrmuellfinder.domain.usecase.post.AddCommentUseCase
import com.omerkaya.sperrmuellfinder.domain.usecase.post.GetCommentsUseCase
import com.omerkaya.sperrmuellfinder.domain.usecase.post.DeletePostUseCase
import com.omerkaya.sperrmuellfinder.domain.usecase.post.GetPostDetailUseCase
import com.omerkaya.sperrmuellfinder.domain.usecase.post.GetPostLikesUseCase
import com.omerkaya.sperrmuellfinder.domain.usecase.TogglePostLikeUseCase
import com.omerkaya.sperrmuellfinder.domain.usecase.GetPostLikeStatusUseCase
import com.omerkaya.sperrmuellfinder.domain.usecase.post.ReportPostUseCase
import com.omerkaya.sperrmuellfinder.domain.usecase.social.BlockUserUseCase
import com.omerkaya.sperrmuellfinder.domain.usecase.post.SharePostUseCase
import com.omerkaya.sperrmuellfinder.domain.usecase.user.GetCurrentUserUseCase
import com.omerkaya.sperrmuellfinder.domain.manager.LocationManager
import com.omerkaya.sperrmuellfinder.domain.manager.PremiumManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Post Detail Screen.
 * Manages post details, comments, likes, and user interactions.
 */
@HiltViewModel
class PostDetailViewModel @Inject constructor(
    private val getPostDetailUseCase: GetPostDetailUseCase,
    private val getCommentsUseCase: GetCommentsUseCase,
    private val getPostLikesUseCase: GetPostLikesUseCase,
    private val addCommentUseCase: AddCommentUseCase,
    private val deletePostUseCase: DeletePostUseCase,
    private val togglePostLikeUseCase: TogglePostLikeUseCase,
    private val getPostLikeStatusUseCase: GetPostLikeStatusUseCase,
    private val sharePostUseCase: SharePostUseCase,
    private val reportPostUseCase: ReportPostUseCase,
    private val blockUserUseCase: BlockUserUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val locationManager: LocationManager,
    private val premiumManager: PremiumManager,
    @ApplicationContext private val context: Context,
    private val logger: Logger,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    companion object {
        private const val BASIC_RADIUS_METERS = PremiumManager.BASIC_RADIUS_METERS
    }

    private val postId: String = savedStateHandle.get<String>(TopLevelDestination.POST_ID_ARG) ?: ""

    private val _uiState = MutableStateFlow(PostDetailUiState())
    val uiState: StateFlow<PostDetailUiState> = _uiState.asStateFlow()

    private val _events = Channel<PostDetailEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()
    
    // Real-time like status tracking (same as HomeViewModel)
    private val _realTimeLikeStatus = MutableStateFlow<Boolean?>(null)
    val realTimeLikeStatus: StateFlow<Boolean?> = _realTimeLikeStatus.asStateFlow()
    
    // Track like listener job
    private var likeListenerJob: Job? = null

    // Comments with pagination — cachedIn prevents re-fetch on recomposition
    val comments: Flow<PagingData<Comment>> = if (postId.isNotEmpty()) {
        getCommentsUseCase(postId, viewModelScope).cachedIn(viewModelScope)
    } else {
        emptyFlow()
    }

    // Post likes with pagination — cachedIn prevents re-fetch on recomposition
    val postLikes: Flow<PagingData<User>> = if (postId.isNotEmpty()) {
        getPostLikesUseCase(postId, viewModelScope).cachedIn(viewModelScope)
    } else {
        emptyFlow()
    }

    init {
        logger.d(Logger.TAG_DEFAULT, "PostDetailViewModel initialized for post: $postId")
        
        if (postId.isEmpty()) {
            logger.e(Logger.TAG_DEFAULT, "Post ID is empty")
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "Invalid post ID"
            )
        } else {
            // Load current user
            viewModelScope.launch {
                getCurrentUserUseCase().collect { user ->
                    _uiState.value = _uiState.value.copy(currentUser = user)
                    logger.d(Logger.TAG_DEFAULT, "Current user loaded: ${user?.displayName}")
                    
                    // Start real-time like status listener when user is loaded
                    user?.let { 
                        startRealTimeLikeStatusListener(user.uid)
                    }
                }
            }
            
            // Load post details
            loadPostDetail()
            
            // Increment view count
            incrementViewCount()
        }
    }
    
    /**
     * Start real-time like status listener for persistent red heart with crash protection
     */
    private fun startRealTimeLikeStatusListener(userId: String) {
        // Cancel existing job if any
        likeListenerJob?.cancel()
        
        logger.d(Logger.TAG_DEFAULT, "🔍 Setting up real-time like status for post: $postId, user: $userId")
        
        likeListenerJob = viewModelScope.launch {
            getPostLikeStatusUseCase(postId, userId)
                .catch { e ->
                    if (e is CancellationException) {
                        logger.d(Logger.TAG_DEFAULT, "💖 Like status listener cancelled for post: $postId")
                    } else {
                        logger.e(Logger.TAG_DEFAULT, "❌ Error in real-time like status listener", e)
                    }
                }
                .collect { result ->
                    when (result) {
                        is Result.Success -> {
                            val isLiked = result.data
                            _realTimeLikeStatus.value = isLiked
                            logger.d(Logger.TAG_DEFAULT, "💖 Real-time like status: post=$postId, user=$userId, liked=$isLiked")
                        }
                        is Result.Error -> {
                            logger.e(Logger.TAG_DEFAULT, "❌ Error getting like status for post: $postId", result.exception)
                        }
                        is Result.Loading -> {
                            // Handle loading state if needed
                        }
                    }
                }
        }
    }

    /**
     * Loads post details with real-time updates
     */
    private fun loadPostDetail() {
        viewModelScope.launch {
            getPostDetailUseCase(postId).collect { result ->
                when (result) {
                    is Result.Loading -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = true,
                            error = null
                        )
                    }
                    is Result.Success -> {
                        val post = result.data
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            post = post,
                            error = if (post == null) "Post not found" else null
                        )
                        logger.i(Logger.TAG_DEFAULT, "Post detail loaded: ${post?.id}")
                    }
                    is Result.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            error = result.exception.message ?: "Failed to load post"
                        )
                        logger.e(Logger.TAG_DEFAULT, "Failed to load post detail", result.exception)
                    }
                }
            }
        }
    }

    /**
     * Increments view count for the post
     */
    private fun incrementViewCount() {
        viewModelScope.launch {
            try {
                getPostDetailUseCase.incrementViewCount(postId)
            } catch (e: Exception) {
                logger.e(Logger.TAG_DEFAULT, "Failed to increment view count", e)
            }
        }
    }

    /**
     * 💖 PERSISTENT LIKE SYSTEM - Real-time heart status
     * Toggles like status with persistent red heart (no optimistic UI needed)
     */
    fun onLikeClick() {
        viewModelScope.launch {
            val currentPost = _uiState.value.post
            if (currentPost == null) {
                logger.w(Logger.TAG_DEFAULT, "Cannot like: post not loaded")
                return@launch
            }
            
            val currentlyLiked = _realTimeLikeStatus.value ?: currentPost.isLikedByCurrentUser
            logger.d(Logger.TAG_DEFAULT, "💖 Like action: post=$postId, currentlyLiked=$currentlyLiked")
            
            // Show loading state
            _uiState.value = _uiState.value.copy(isLikeLoading = true)
            
            // 🔄 BACKEND SYNC (Real-time listener will handle UI updates)
            val currentUser = _uiState.value.currentUser
            if (currentUser == null) {
                logger.w(Logger.TAG_DEFAULT, "Cannot like: user not authenticated")
                _uiState.value = _uiState.value.copy(isLikeLoading = false)
                _events.send(PostDetailEvent.ShowError("Authentication required"))
                return@launch
            }
            
            when (val result = togglePostLikeUseCase(postId, currentUser.uid)) {
                is Result.Success -> {
                    val actualLikeStatus = result.data
                    logger.i(Logger.TAG_DEFAULT, "✅ Post $postId like toggled successfully: $actualLikeStatus")
                    
                    _uiState.value = _uiState.value.copy(
                        isLikeLoading = false,
                        error = null
                    )
                    
                    // Send success event
                    _events.send(
                        if (actualLikeStatus) PostDetailEvent.PostLiked 
                        else PostDetailEvent.PostUnliked
                    )
                    
                    // Real-time listener automatically updates UI with persistent state
                }
                is Result.Error -> {
                    logger.e(Logger.TAG_DEFAULT, "❌ Failed to toggle like for post $postId", result.exception)
                    
                    _uiState.value = _uiState.value.copy(
                        isLikeLoading = false,
                        error = "Failed to update like status"
                    )
                    
                    _events.send(PostDetailEvent.ShowError("Failed to update like status"))
                }
                is Result.Loading -> {
                    // Keep loading state
                }
            }
        }
    }

    /**
     * Adds a comment to the post
     */
    fun addComment(content: String) {
        viewModelScope.launch {
            logger.d(Logger.TAG_DEFAULT, "Adding comment to post: $postId")
            
            _uiState.value = _uiState.value.copy(isCommentLoading = true)
            
            when (val result = addCommentUseCase(postId, content)) {
                is Result.Success -> {
                    logger.i(Logger.TAG_DEFAULT, "Comment added to post: $postId")
                    
                    _uiState.value = _uiState.value.copy(
                        isCommentLoading = false,
                        commentText = "", // Clear comment input
                        error = null
                    )
                    
                    _events.send(PostDetailEvent.CommentAdded)
                }
                is Result.Error -> {
                    logger.e(Logger.TAG_DEFAULT, "Failed to add comment to post $postId", result.exception)
                    _uiState.value = _uiState.value.copy(
                        isCommentLoading = false,
                        error = result.exception.message ?: "Failed to add comment"
                    )
                }
                is Result.Loading -> {
                    // Keep loading state
                }
            }
        }
    }

    /**
     * Updates comment text
     */
    fun updateCommentText(text: String) {
        _uiState.value = _uiState.value.copy(commentText = text)
    }

    /**
     * Reports the post
     */
    fun reportPost(reason: ReportReason, description: String? = null) {
        viewModelScope.launch {
            logger.d(Logger.TAG_DEFAULT, "Reporting post: $postId for reason: $reason")
            
            when (val result = reportPostUseCase.reportPost(postId, reason, description)) {
                is Result.Success -> {
                    logger.i(Logger.TAG_DEFAULT, "Post $postId reported successfully")
                    _events.send(PostDetailEvent.PostReported)
                }
                is Result.Error -> {
                    logger.e(Logger.TAG_DEFAULT, "Failed to report post $postId", result.exception)
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to report post"
                    )
                }
                is Result.Loading -> {
                    // Handle loading state if needed
                }
            }
        }
    }

    /**
     * Handles user profile click — routes to own profile or user profile
     */
    fun onUserClick(userId: String) {
        viewModelScope.launch {
            logger.d(Logger.TAG_DEFAULT, "User clicked: $userId")
            val currentUserId = _uiState.value.currentUser?.uid
            if (currentUserId != null && currentUserId == userId) {
                _events.send(PostDetailEvent.NavigateToProfile)
            } else {
                _events.send(PostDetailEvent.NavigateToUserProfile(userId))
            }
        }
    }

    /**
     * Retries loading the post after an error
     */
    fun retry() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)
        loadPostDetail()
    }

    /**
     * Handles share button click
     */
    fun onShareClick() {
        viewModelScope.launch {
            val post = _uiState.value.post ?: return@launch
            
            logger.d(Logger.TAG_DEFAULT, "Sharing post: ${post.id}")
            
            val appStoreLink = context.getString(com.omerkaya.sperrmuellfinder.R.string.share_app_link)
            val shareTitle = context.getString(com.omerkaya.sperrmuellfinder.R.string.share_chooser_title)
            
            when (val result = sharePostUseCase(context, post, appStoreLink, shareTitle)) {
                is Result.Success -> {
                    logger.i(Logger.TAG_DEFAULT, "Post shared successfully: ${post.id}")
                    _events.send(PostDetailEvent.PostShared)
                }
                is Result.Error -> {
                    logger.e(Logger.TAG_DEFAULT, "Failed to share post: ${post.id}", result.exception)
                    _uiState.value = _uiState.value.copy(
                        error = "Failed to share post"
                    )
                }
                is Result.Loading -> {
                    // Handle loading state if needed
                }
            }
        }
    }

    /**
     * Handle location click with same premium/radius gate logic as Home.
     */
    fun onPostLocationClick() {
        val post = _uiState.value.post ?: return
        val postLocation = post.location ?: return

        viewModelScope.launch {
            val isPremium = premiumManager.isPremium.value
            if (isPremium) {
                _events.send(PostDetailEvent.NavigateToMapLocation(postLocation.latitude, postLocation.longitude))
                return@launch
            }

            val resolvedUserLocation = locationManager.currentLocation.value
                ?: when (val locationResult = locationManager.getCurrentLocation()) {
                    is Result.Success -> locationResult.data
                    else -> null
                }

            val distanceMeters = when {
                resolvedUserLocation != null -> locationManager.calculateDistance(resolvedUserLocation, postLocation)
                post.distanceFromUser != null -> post.distanceFromUser
                else -> null
            }

            if (distanceMeters != null && distanceMeters > BASIC_RADIUS_METERS) {
                _events.send(PostDetailEvent.ShowPremiumRequiredForMap)
            } else {
                _events.send(PostDetailEvent.NavigateToMapLocation(postLocation.latitude, postLocation.longitude))
            }
        }
    }

    /**
     * Shows comments bottom sheet
     */
    fun showComments() {
        _uiState.value = _uiState.value.copy(showComments = true)
    }

    /**
     * Hides comments bottom sheet
     */
    fun hideComments() {
        _uiState.value = _uiState.value.copy(showComments = false)
    }

    /**
     * Shows likes bottom sheet
     */
    fun showLikes() {
        _uiState.value = _uiState.value.copy(showLikes = true)
    }

    /**
     * Hides likes bottom sheet
     */
    fun hideLikes() {
        _uiState.value = _uiState.value.copy(showLikes = false)
    }

    /**
     * Block the post owner and navigate back
     */
    fun blockPostOwner(reason: String? = null) {
        viewModelScope.launch {
            val ownerId = _uiState.value.post?.ownerId ?: return@launch
            when (val result = blockUserUseCase(ownerId, reason)) {
                is Result.Success -> {
                    logger.i(Logger.TAG_DEFAULT, "Blocked post owner: $ownerId")
                    _events.send(PostDetailEvent.PostDeleted(postId))
                }
                is Result.Error -> {
                    logger.e(Logger.TAG_DEFAULT, "Failed to block post owner", result.exception)
                    _events.send(PostDetailEvent.ShowError("Failed to block user"))
                }
                else -> Unit
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        likeListenerJob?.cancel()
        likeListenerJob = null
        logger.d(Logger.TAG_DEFAULT, "PostDetailViewModel cleared. Like listener cancelled for post: $postId")
    }

    /**
     * Updates current image index in carousel
     */
    fun updateCurrentImageIndex(index: Int) {
        _uiState.value = _uiState.value.copy(currentImageIndex = index)
    }

    /**
     * Clears error message
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Deletes current post (owner only). On success, emits PostDeleted event so UI can navigate back.
     */
    fun onDeletePost() {
        viewModelScope.launch {
            val currentPost = _uiState.value.post ?: return@launch
            _uiState.value = _uiState.value.copy(isDeleteLoading = true)

            when (val result = deletePostUseCase(currentPost.id)) {
                is Result.Success -> {
                    logger.i(Logger.TAG_DEFAULT, "Post deleted from detail: ${currentPost.id}")
                    _uiState.value = _uiState.value.copy(
                        isDeleteLoading = false,
                        error = null
                    )
                    _events.send(PostDetailEvent.PostDeleted(currentPost.id))
                }
                is Result.Error -> {
                    logger.e(Logger.TAG_DEFAULT, "Failed to delete post from detail: ${currentPost.id}", result.exception)
                    _uiState.value = _uiState.value.copy(
                        isDeleteLoading = false,
                        error = result.exception.message ?: "Failed to delete post"
                    )
                    _events.send(PostDetailEvent.ShowError("Failed to delete post"))
                }
                is Result.Loading -> {
                    // Keep loading state
                }
            }
        }
    }
    
    /**
     * Gets current user ID for permission checks
     */
    fun getCurrentUserId(): String? {
        return _uiState.value.currentUser?.uid
    }
    
}

/**
 * UI State for Post Detail Screen
 */
data class PostDetailUiState(
    val isLoading: Boolean = true,
    val isLikeLoading: Boolean = false,
    val isCommentLoading: Boolean = false,
    val isDeleteLoading: Boolean = false,
    val error: String? = null,
    val post: Post? = null,
    val currentUser: User? = null,
    val commentText: String = "",
    val showComments: Boolean = false,
    val showLikes: Boolean = false,
    val currentImageIndex: Int = 0
)

/**
 * Events for Post Detail Screen
 */
sealed class PostDetailEvent {
    data object PostLiked : PostDetailEvent()
    data object PostUnliked : PostDetailEvent()
    data object CommentAdded : PostDetailEvent()
    data object PostShared : PostDetailEvent()
    data object PostReported : PostDetailEvent()
    data class PostDeleted(val postId: String) : PostDetailEvent()
    data object NavigateToProfile : PostDetailEvent()
    data class NavigateToUserProfile(val userId: String) : PostDetailEvent()
    data class NavigateToMapLocation(val latitude: Double, val longitude: Double) : PostDetailEvent()
    data object ShowPremiumRequiredForMap : PostDetailEvent()
    data class ShowError(val message: String) : PostDetailEvent()
}
