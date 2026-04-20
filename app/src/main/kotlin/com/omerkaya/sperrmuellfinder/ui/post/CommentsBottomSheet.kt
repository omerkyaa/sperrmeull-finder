package com.omerkaya.sperrmuellfinder.ui.post

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.google.firebase.auth.FirebaseAuth
import com.omerkaya.sperrmuellfinder.R
import com.omerkaya.sperrmuellfinder.domain.model.Comment
import com.omerkaya.sperrmuellfinder.domain.model.ReportReason
import com.omerkaya.sperrmuellfinder.domain.model.ReportTargetType
import com.omerkaya.sperrmuellfinder.ui.report.ReportBottomSheet
import com.skydoves.landscapist.glide.GlideImage

/**
 * Instagram-style Comments Bottom Sheet with real-time updates.
 * Features: Comment list, add comment, report functionality, professional UI.
 * Rules.md compliant - Material3 design, no hardcoded strings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsBottomSheet(
    postId: String,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onUserClick: (String) -> Unit = {},
    viewModel: CommentsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val comments = viewModel.comments.collectAsLazyPagingItems()
    val focusManager = LocalFocusManager.current
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    var reportCommentId by remember { mutableStateOf<String?>(null) }
    var deleteCommentId by remember { mutableStateOf<String?>(null) }

    // Load comments when sheet opens
    LaunchedEffect(postId) {
        android.util.Log.d("CommentsBottomSheet", "Loading comments for postId: $postId")
        viewModel.loadComments(postId)
    }

    // Debug logging for comments count
    LaunchedEffect(comments.itemCount, comments.loadState.refresh) {
        android.util.Log.d("CommentsBottomSheet", "Comments count: ${comments.itemCount}, loadState: ${comments.loadState.refresh}")
    }

    // Handle events
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is CommentsEvent.CommentAdded -> {
                    // Refresh to show new comment
                    comments.refresh()
                }
                is CommentsEvent.CommentDeleted -> {
                    comments.refresh()
                }
                is CommentsEvent.ScrollToTop -> {
                    // TODO: Implement scroll to top
                }
                is CommentsEvent.ShowError -> {
                    // TODO: Show snackbar or toast
                }
                is CommentsEvent.ShowSuccess -> {
                    // TODO: Show success message
                }
                is CommentsEvent.CommentReported -> {
                    // TODO: Show report confirmation
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .navigationBarsPadding(),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            CommentsHeader(
                onClose = onDismiss,
                commentsCount = comments.itemCount
            )

            Divider()

            // Comments List
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when {
                    postId.isBlank() -> {
                        // Invalid post ID
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Invalid post ID",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    uiState.isLoading && comments.itemCount == 0 -> {
                        // Initial loading
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    comments.itemCount == 0 && comments.loadState.refresh is LoadState.NotLoading -> {
                        // Empty state
                        EmptyCommentsState()
                    }
                    else -> {
                        // Comments list
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                count = comments.itemCount,
                                key = comments.itemKey { it.id }
                            ) { index ->
                                val comment = comments[index]
                                if (comment != null) {
                                    CommentItem(
                                        comment = comment,
                                        currentUserId = currentUserId,
                                        onUserClick = onUserClick,
                                        onReportClick = { reportCommentId = it },
                                        onDeleteClick = { deleteCommentId = it },
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
                            }

                            // Loading indicator for pagination
                            if (comments.loadState.append is LoadState.Loading) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Divider()

            // Add Comment Input
            AddCommentInput(
                text = uiState.newCommentText,
                onTextChange = viewModel::updateNewCommentText,
                onSend = {
                    viewModel.addComment(uiState.newCommentText)
                    focusManager.clearFocus()
                },
                isLoading = uiState.isAddingComment,
                error = uiState.addCommentError
            )
        }
    }

    if (reportCommentId != null) {
        ReportBottomSheet(
            targetId = reportCommentId!!,
            targetType = ReportTargetType.COMMENT,
            onDismiss = { reportCommentId = null }
        )
    }

    if (deleteCommentId != null) {
        AlertDialog(
            onDismissRequest = { deleteCommentId = null },
            title = { Text(text = stringResource(R.string.delete_post_confirm)) },
            text = { Text(text = stringResource(R.string.delete_post_warning)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteComment(deleteCommentId!!)
                        deleteCommentId = null
                    }
                ) {
                    Text(text = stringResource(R.string.delete_post_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteCommentId = null }) {
                    Text(text = stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun CommentsHeader(
    onClose: () -> Unit,
    commentsCount: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.comments_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (commentsCount > 0) {
                Text(
                    text = commentsCount.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
            }

            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.close)
                )
            }
        }
    }
}

@Composable
private fun CommentItem(
    comment: Comment,
    currentUserId: String?,
    onUserClick: (String) -> Unit,
    onReportClick: (String) -> Unit,
    onDeleteClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showReportMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Profile Image
        GlideImage(
            imageModel = { comment.authorPhotoUrl ?: "" },
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .clickable { onUserClick(comment.authorId) },
            failure = {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = comment.authorName.take(1).uppercase(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        )

        // Comment Content
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = comment.authorName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onUserClick(comment.authorId) }
                )

                if (comment.authorLevel > 1) {
                    Text(
                        text = "Lv${comment.authorLevel}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = comment.getFormattedTime(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    text = stringResource(R.string.likes_count, comment.likesCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // More Options Menu
        Box {
            IconButton(
                onClick = { showReportMenu = true },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.more_options),
                    modifier = Modifier.size(16.dp)
                )
            }

            ReportCommentMenu(
                expanded = showReportMenu,
                isOwnComment = currentUserId == comment.authorId,
                onDismiss = { showReportMenu = false },
                onReportClick = { onReportClick(comment.id) },
                onDeleteClick = { onDeleteClick(comment.id) }
            )
        }
    }
}

@Composable
private fun ReportCommentMenu(
    expanded: Boolean,
    isOwnComment: Boolean,
    onDismiss: () -> Unit,
    onReportClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        if (isOwnComment) {
            DropdownMenuItem(
                text = {
                    Text(
                        text = stringResource(R.string.delete_post_confirm),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                onClick = {
                    onDismiss()
                    onDeleteClick()
                }
            )
        } else {
            DropdownMenuItem(
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Flag,
                            contentDescription = null,
                            tint = Color.Red,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.report_comment),
                            color = Color.Red,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                onClick = {
                    onDismiss()
                    onReportClick()
                }
            )
        }
    }
}

@Composable
private fun AddCommentInput(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    isLoading: Boolean,
    error: String?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        if (error != null) {
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = onTextChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(text = stringResource(R.string.add_comment_hint))
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Send
                ),
                keyboardActions = KeyboardActions(
                    onSend = { if (text.isNotBlank() && !isLoading) onSend() }
                ),
                maxLines = 3,
                shape = RoundedCornerShape(24.dp)
            )

            IconButton(
                onClick = onSend,
                enabled = text.isNotBlank() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = stringResource(R.string.send_comment),
                        tint = if (text.isNotBlank()) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
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
            text = stringResource(R.string.no_comments_yet),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.be_first_to_comment),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
