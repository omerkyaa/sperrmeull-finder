package com.omerkaya.sperrmuellfinder.ui.postdetail.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.glide.GlideImage
import com.omerkaya.sperrmuellfinder.R
import com.omerkaya.sperrmuellfinder.core.ui.theme.SperrmullPrimary
import com.omerkaya.sperrmuellfinder.domain.model.Comment
import com.omerkaya.sperrmuellfinder.domain.model.Post
import com.omerkaya.sperrmuellfinder.domain.model.ReportTargetType
import com.omerkaya.sperrmuellfinder.domain.model.User
import com.omerkaya.sperrmuellfinder.ui.report.ReportBottomSheet
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Comments bottom sheet component for displaying and adding comments.
 * Instagram-style comments interface with user interactions and real-time updates.
 */
@Composable
fun CommentsBottomSheet(
    post: Post,
    comments: LazyPagingItems<Comment>,
    currentUser: User?,
    commentText: String,
    isCommentLoading: Boolean,
    onCommentTextChange: (String) -> Unit,
    onAddComment: (String) -> Unit,
    onUserClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Note: LocalSoftwareKeyboardController is experimental in Material3 1.1.2
    // val keyboardController = LocalSoftwareKeyboardController.current
    
    var reportCommentId by remember { mutableStateOf<String?>(null) }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
    ) {
        // Header
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Handle bar
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .background(
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        RoundedCornerShape(2.dp)
                    )
                    .align(Alignment.CenterHorizontally)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = stringResource(R.string.comments_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        Divider(
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
        )
        
        // Comments list
        Box(
            modifier = Modifier.weight(1f)
        ) {
            when {
                comments.loadState.refresh is LoadState.Loading && comments.itemCount == 0 -> {
                    LoadingCommentsState()
                }
                
                comments.loadState.refresh is LoadState.Error -> {
                    ErrorCommentsState(
                        error = (comments.loadState.refresh as LoadState.Error).error.message 
                            ?: stringResource(R.string.error_loading_comments),
                        onRetry = { comments.retry() }
                    )
                }
                
                comments.itemCount == 0 -> {
                    EmptyCommentsState()
                }
                
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(
                            count = comments.itemCount,
                            key = comments.itemKey { it.id }
                        ) { index ->
                            val comment = comments[index]
                            if (comment != null) {
                                CommentItem(
                                    comment = comment,
                                    currentUserId = currentUser?.uid,
                                    onUserClick = onUserClick,
                                    onReportClick = { reportCommentId = it }
                                )
                            }
                        }
                        
                        // Loading more indicator
                        if (comments.loadState.append is LoadState.Loading) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = SperrmullPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        Divider(
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
        )
        
        // Comment input
        if (currentUser != null) {
            CommentInput(
                user = currentUser,
                commentText = commentText,
                isLoading = isCommentLoading,
                onCommentTextChange = onCommentTextChange,
                onSendComment = {
                    if (commentText.isNotBlank()) {
                        onAddComment(commentText)
                        // Note: keyboardController?.hide() removed due to experimental API
                    }
                }
            )
        }
    }
    
    // Report bottom sheet
    if (reportCommentId != null) {
        ReportBottomSheet(
            targetId = reportCommentId!!,
            targetType = ReportTargetType.COMMENT,
            onDismiss = { reportCommentId = null }
        )
    }
}

@Composable
private fun CommentItem(
    comment: Comment,
    currentUserId: String?,
    onUserClick: (String) -> Unit,
    onReportClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // User avatar
        GlideImage(
            imageModel = { comment.authorPhotoUrl ?: R.drawable.ic_default_avatar },
            imageOptions = ImageOptions(
                contentScale = ContentScale.Crop,
                contentDescription = stringResource(R.string.cd_user_avatar)
            ),
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .clickable { onUserClick(comment.authorId) }
        )
        
        // Comment content
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = comment.authorName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.clickable { onUserClick(comment.authorId) }
                )
                
                val timeFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
                Text(
                    text = timeFormat.format(comment.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = comment.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            if (comment.likesCount > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = stringResource(R.string.comment_likes_count, comment.likesCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Medium
                )
            }
        }
        
        // Three-dot menu (only show for other users' comments)
        if (currentUserId != null && currentUserId != comment.authorId) {
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.cd_more_options),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { 
                            Text(
                                text = stringResource(R.string.report_comment),
                                color = Color(0xFFD32F2F) // Red color for report
                            )
                        },
                        onClick = {
                            showMenu = false
                            onReportClick(comment.id)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CommentInput(
    user: User,
    commentText: String,
    isLoading: Boolean,
    onCommentTextChange: (String) -> Unit,
    onSendComment: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // User avatar
        GlideImage(
            imageModel = { user.photoUrl ?: R.drawable.ic_default_avatar },
            imageOptions = ImageOptions(
                contentScale = ContentScale.Crop,
                contentDescription = stringResource(R.string.cd_user_avatar)
            ),
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
        )
        
        // Comment input field
        OutlinedTextField(
            value = commentText,
            onValueChange = onCommentTextChange,
            modifier = Modifier.weight(1f),
            placeholder = { 
                Text(
                    text = stringResource(R.string.add_comment_hint),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SperrmullPrimary,
                unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                cursorColor = SperrmullPrimary
            ),
            shape = RoundedCornerShape(20.dp),
            maxLines = 3,
            keyboardOptions = KeyboardOptions(
                imeAction = ImeAction.Send
            ),
            keyboardActions = KeyboardActions(
                onSend = { onSendComment() }
            ),
            enabled = !isLoading
        )
        
        // Send button
        IconButton(
            onClick = onSendComment,
            enabled = commentText.isNotBlank() && !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = SperrmullPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = stringResource(R.string.cd_send_comment),
                    tint = if (commentText.isNotBlank()) SperrmullPrimary 
                          else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@Composable
private fun LoadingCommentsState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator(
                color = SperrmullPrimary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = stringResource(R.string.loading_comments),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun ErrorCommentsState(
    error: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.error_loading_comments_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        androidx.compose.material3.TextButton(
            onClick = onRetry
        ) {
            Text(
                text = stringResource(R.string.retry),
                color = SperrmullPrimary
            )
        }
    }
}

@Composable
private fun EmptyCommentsState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(R.string.no_comments_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = stringResource(R.string.no_comments_description),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
    }
}
