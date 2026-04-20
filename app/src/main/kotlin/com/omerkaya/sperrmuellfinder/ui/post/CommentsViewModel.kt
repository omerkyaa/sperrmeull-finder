package com.omerkaya.sperrmuellfinder.ui.post

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.domain.model.Comment
import com.omerkaya.sperrmuellfinder.domain.model.ReportReason
import com.omerkaya.sperrmuellfinder.domain.usecase.post.AddCommentUseCase
import com.omerkaya.sperrmuellfinder.domain.usecase.post.DeleteCommentUseCase
import com.omerkaya.sperrmuellfinder.domain.usecase.post.GetCommentsUseCase
import com.omerkaya.sperrmuellfinder.domain.usecase.post.ReportCommentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing comments in posts.
 * Handles comment loading, adding, and reporting with real-time updates.
 * Rules.md compliant - Professional comment system with state management.
 */
@HiltViewModel
class CommentsViewModel @Inject constructor(
    private val getCommentsUseCase: GetCommentsUseCase,
    private val addCommentUseCase: AddCommentUseCase,
    private val deleteCommentUseCase: DeleteCommentUseCase,
    private val reportCommentUseCase: ReportCommentUseCase,
    private val logger: Logger
) : ViewModel() {
    private var lastSubmittedCommentText: String = ""
    private var lastSubmittedAtMs: Long = 0L

    private val _uiState = MutableStateFlow(CommentsUiState())
    val uiState: StateFlow<CommentsUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<CommentsEvent>()
    val events: Flow<CommentsEvent> = _events.asSharedFlow()

    private var currentPostId: String? = null
    private var _comments: Flow<PagingData<Comment>> = emptyFlow()
    val comments: Flow<PagingData<Comment>> get() = _comments

    /**
     * Loads comments for a specific post.
     */
    fun loadComments(postId: String) {
        logger.d(TAG, "Loading comments for post: $postId")
        
        if (currentPostId == postId) {
            // Already loaded for this post
            return
        }
        
        currentPostId = postId
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            error = null
        )
        
        try {
            _comments = getCommentsUseCase(postId, viewModelScope)
                .cachedIn(viewModelScope)
            
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                postId = postId
            )
        } catch (e: Exception) {
            logger.e(TAG, "Error loading comments for post: $postId", e)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = e.message ?: "Failed to load comments"
            )
        }
    }

    /**
     * Adds a new comment to the current post.
     */
    fun addComment(content: String) {
        val postId = currentPostId ?: return
        val normalized = content.trim()

        if (normalized.isBlank()) {
            viewModelScope.launch {
                _events.emit(CommentsEvent.ShowError("Comment cannot be empty"))
            }
            return
        }
        if (_uiState.value.isAddingComment) return
        val now = System.currentTimeMillis()
        if (normalized == lastSubmittedCommentText && now - lastSubmittedAtMs < 2_000L) return
        lastSubmittedCommentText = normalized
        lastSubmittedAtMs = now
        
        logger.d(TAG, "Adding comment to post: $postId")
        
        _uiState.value = _uiState.value.copy(
            isAddingComment = true,
            addCommentError = null
        )
        
        viewModelScope.launch {
            when (val result = addCommentUseCase(postId, normalized)) {
                is Result.Success -> {
                    logger.i(TAG, "Comment added successfully to post: $postId")
                    _uiState.value = _uiState.value.copy(
                        isAddingComment = false,
                        newCommentText = "" // Clear input
                    )
                    _events.emit(CommentsEvent.CommentAdded(result.data))
                    _events.emit(CommentsEvent.ScrollToTop)
                }
                is Result.Error -> {
                    logger.e(TAG, "Failed to add comment to post: $postId", result.exception)
                    val errorMessage = result.exception.message ?: "Failed to add comment"
                    _uiState.value = _uiState.value.copy(
                        isAddingComment = false,
                        addCommentError = errorMessage
                    )
                    _events.emit(CommentsEvent.ShowError(errorMessage))
                }
                is Result.Loading -> {
                    // Keep loading state
                }
            }
        }
    }

    /**
     * Reports a comment for inappropriate content.
     */
    fun reportComment(comment: Comment, reason: ReportReason, description: String? = null) {
        logger.d(TAG, "Reporting comment: ${comment.id} for reason: ${reason.displayName}")
        
        viewModelScope.launch {
            when (val result = reportCommentUseCase(comment.id, reason, description)) {
                is Result.Success -> {
                    logger.i(TAG, "Comment reported successfully: ${comment.id}")
                    _events.emit(CommentsEvent.CommentReported(comment))
                    _events.emit(CommentsEvent.ShowSuccess("Comment reported successfully"))
                }
                is Result.Error -> {
                    logger.e(TAG, "Failed to report comment: ${comment.id}", result.exception)
                    val errorMessage = result.exception.message ?: "Failed to report comment"
                    _events.emit(CommentsEvent.ShowError(errorMessage))
                }
                is Result.Loading -> {
                    // Show loading indicator if needed
                }
            }
        }
    }

    /**
     * Deletes user's own comment.
     */
    fun deleteComment(commentId: String) {
        if (commentId.isBlank()) return

        logger.d(TAG, "Deleting comment: $commentId")

        viewModelScope.launch {
            when (val result = deleteCommentUseCase(commentId)) {
                is Result.Success -> {
                    logger.i(TAG, "Comment deleted successfully: $commentId")
                    _events.emit(CommentsEvent.CommentDeleted(commentId))
                }
                is Result.Error -> {
                    logger.e(TAG, "Failed to delete comment: $commentId", result.exception)
                    _events.emit(CommentsEvent.ShowError(result.exception.message ?: "Failed to delete comment"))
                }
                is Result.Loading -> {
                    // no-op
                }
            }
        }
    }

    /**
     * Updates the new comment text as user types.
     */
    fun updateNewCommentText(text: String) {
        _uiState.value = _uiState.value.copy(
            newCommentText = text,
            addCommentError = null // Clear error when user starts typing
        )
    }

    /**
     * Clears any error state.
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(
            error = null,
            addCommentError = null
        )
    }

    /**
     * Refreshes comments for the current post.
     */
    fun refreshComments() {
        currentPostId?.let { postId ->
            logger.d(TAG, "Refreshing comments for post: $postId")
            loadComments(postId)
        }
    }

    companion object {
        private const val TAG = "CommentsViewModel"
    }
}

/**
 * UI state for comments screen.
 */
data class CommentsUiState(
    val postId: String? = null,
    val isLoading: Boolean = false,
    val isAddingComment: Boolean = false,
    val newCommentText: String = "",
    val error: String? = null,
    val addCommentError: String? = null
)

/**
 * Events emitted by CommentsViewModel.
 */
sealed class CommentsEvent {
    data class CommentAdded(val comment: Comment) : CommentsEvent()
    data class CommentDeleted(val commentId: String) : CommentsEvent()
    data class CommentReported(val comment: Comment) : CommentsEvent()
    data class ShowError(val message: String) : CommentsEvent()
    data class ShowSuccess(val message: String) : CommentsEvent()
    object ScrollToTop : CommentsEvent()
}
