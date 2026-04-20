package com.omerkaya.sperrmuellfinder.ui.comments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omerkaya.sperrmuellfinder.core.util.Result
import com.omerkaya.sperrmuellfinder.core.util.Logger
import com.omerkaya.sperrmuellfinder.domain.model.Comment
import com.omerkaya.sperrmuellfinder.domain.repository.FirestoreRepository
import com.omerkaya.sperrmuellfinder.domain.repository.SocialRepository
import com.omerkaya.sperrmuellfinder.domain.usecase.post.DeleteCommentUseCase
import com.omerkaya.sperrmuellfinder.domain.usecase.user.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 💬 COMMENTS VIEW MODEL - SperrmüllFinder
 * Rules.md compliant - MVVM pattern with StateFlow
 * 
 * Features:
 * - Comments loading with real-time updates
 * - Add comment functionality
 * - Professional error handling
 * - Clean Architecture compliance
 */
@HiltViewModel
class CommentsViewModel @Inject constructor(
    private val firestoreRepository: FirestoreRepository,
    private val socialRepository: SocialRepository,
    private val deleteCommentUseCase: DeleteCommentUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val logger: Logger
) : ViewModel() {
    private var lastSubmittedCommentText: String = ""
    private var lastSubmittedAtMs: Long = 0L

    private val _uiState = MutableStateFlow(CommentsUiState())
    val uiState: StateFlow<CommentsUiState> = _uiState.asStateFlow()
    
    // Current user ID for navigation logic
    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()
    
    // Track jobs for proper cleanup
    private var commentsJob: Job? = null
    private var userJob: Job? = null
    
    init {
        // Load current user ID with proper cancellation handling
        userJob = viewModelScope.launch {
            try {
                getCurrentUserUseCase()
                    .catch { e ->
                        if (e is CancellationException) {
                            logger.d(Logger.TAG_DEFAULT, "💬 User loading cancelled")
                        } else {
                            logger.e(Logger.TAG_DEFAULT, "❌ Error loading current user", e)
                        }
                        throw e
                    }
                    .collect { user ->
                        _currentUserId.value = user?.uid
                        logger.d(Logger.TAG_DEFAULT, "Current user ID loaded: ${user?.uid}")
                    }
            } catch (e: CancellationException) {
                logger.d(Logger.TAG_DEFAULT, "💬 User loading job cancelled")
            } catch (e: Exception) {
                logger.e(Logger.TAG_DEFAULT, "❌ Error in user loading", e)
            }
        }
    }

    fun loadComments(postId: String) {
        // Cancel existing comments job
        commentsJob?.cancel()
        
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            
        logger.d(Logger.TAG_DEFAULT, "🎯 CommentsViewModel - Loading comments for post: $postId")
        
        commentsJob = viewModelScope.launch {
            try {
                socialRepository.getComments(postId)
                    .catch { e ->
                        if (e is CancellationException) {
                            logger.d(Logger.TAG_DEFAULT, "💬 Comments loading cancelled for post: $postId")
                        } else {
                            logger.e(Logger.TAG_DEFAULT, "❌ Error in comments flow", e)
                        }
                        throw e
                    }
                    .collect { comments ->
                                logger.d(Logger.TAG_DEFAULT, "🎯 CommentsViewModel - Loaded ${comments.size} comments")
                                
                                // Debug log for each comment's user data
                                comments.forEach { comment ->
                                    logger.d(Logger.TAG_DEFAULT, "🎯 Comment ${comment.id}: author='${comment.authorName}', photo='${comment.authorPhotoUrl}', content='${comment.content.take(30)}...'")
                                }
                                
                            _uiState.value = _uiState.value.copy(
                                isLoading = false,
                                    comments = comments,
                                error = null
                            )
                }
            } catch (e: CancellationException) {
                logger.d(Logger.TAG_DEFAULT, "💬 Comments loading job cancelled for post: $postId")
            } catch (e: Exception) {
                logger.e(Logger.TAG_DEFAULT, "🎯 CommentsViewModel - Exception in loadComments Flow collection", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error occurred"
                )
            }
        }
    }

    fun addComment(postId: String, text: String) {
        val normalized = text.trim()
        if (normalized.isBlank() || _uiState.value.isAddingComment) return
        val now = System.currentTimeMillis()
        if (normalized == lastSubmittedCommentText && now - lastSubmittedAtMs < 2_000L) {
            return
        }
        lastSubmittedCommentText = normalized
        lastSubmittedAtMs = now

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAddingComment = true)
            
            // Optimistic UI: Add temporary comment immediately
            val tempComment = Comment(
                id = "temp_${System.currentTimeMillis()}",
                postId = postId,
                authorId = "current_user", // TODO: Get from auth
                authorName = "You", // TODO: Get from user profile
                authorPhotoUrl = null, // TODO: Get from user profile
                authorLevel = 1, // TODO: Get from user profile
                content = normalized,
                likesCount = 0,
                createdAt = java.util.Date(),
                updatedAt = java.util.Date(),
                isLikedByCurrentUser = false
            )
            
            val currentComments = _uiState.value.comments.toMutableList()
            currentComments.add(0, tempComment) // Add to top
            _uiState.value = _uiState.value.copy(comments = currentComments)
            
            // Debounce rapid submissions
            kotlinx.coroutines.delay(300)
            
            try {
                when (val result = socialRepository.addComment(postId, normalized)) {
                    is com.omerkaya.sperrmuellfinder.core.util.Result.Success<*> -> {
                        _uiState.value = _uiState.value.copy(isAddingComment = false)
                        
                        // Remove temporary comment - real-time listener will add the actual one
                        val updatedComments = _uiState.value.comments.toMutableList()
                        updatedComments.removeAll { it.id == tempComment.id }
                        _uiState.value = _uiState.value.copy(comments = updatedComments)
                        
                        logger.d("CommentsViewModel", "Comment added successfully")
                    }
                    is com.omerkaya.sperrmuellfinder.core.util.Result.Error -> {
                        // Revert optimistic UI
                        val revertedComments = _uiState.value.comments.toMutableList()
                        revertedComments.removeAll { it.id == tempComment.id }
                        
                        _uiState.value = _uiState.value.copy(
                            isAddingComment = false,
                            comments = revertedComments,
                            error = result.exception.message ?: "Failed to add comment"
                        )
                        
                        logger.e("CommentsViewModel", "Failed to add comment", result.exception)
                    }
                    is com.omerkaya.sperrmuellfinder.core.util.Result.Loading -> {
                        // Keep loading state
                    }
                }
            } catch (e: Exception) {
                // Revert optimistic UI
                val revertedComments = _uiState.value.comments.toMutableList()
                revertedComments.removeAll { it.id == tempComment.id }
                
                _uiState.value = _uiState.value.copy(
                    isAddingComment = false,
                    comments = revertedComments,
                    error = e.message ?: "Failed to add comment"
                )
                
                logger.e("CommentsViewModel", "Exception adding comment", e)
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun deleteComment(commentId: String) {
        if (commentId.isBlank()) return
        viewModelScope.launch {
            when (val result = deleteCommentUseCase(commentId)) {
                is Result.Success -> {
                    _uiState.value = _uiState.value.copy(
                        comments = _uiState.value.comments.filterNot { it.id == commentId }
                    )
                }
                is Result.Error -> {
                    _uiState.value = _uiState.value.copy(
                        error = result.exception.message ?: "Failed to delete comment"
                    )
                }
                is Result.Loading -> {
                    // no-op
                }
            }
        }
    }
    
    /**
     * Clean up jobs when ViewModel is destroyed
     */
    override fun onCleared() {
        super.onCleared()
        logger.d(Logger.TAG_DEFAULT, "🧹 Cleaning up CommentsViewModel jobs")
        
        // Cancel all active jobs
        commentsJob?.cancel()
        userJob?.cancel()
        
        commentsJob = null
        userJob = null
        
        logger.d(Logger.TAG_DEFAULT, "✅ CommentsViewModel cleanup completed")
    }
}

/**
 * UI State for Comments Screen
 */
data class CommentsUiState(
    val isLoading: Boolean = false,
    val isAddingComment: Boolean = false,
    val comments: List<Comment> = emptyList(),
    val error: String? = null
)
