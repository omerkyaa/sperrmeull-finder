package com.omerkaya.sperrmuellfinder.ui.comments

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.omerkaya.sperrmuellfinder.R
import com.omerkaya.sperrmuellfinder.domain.model.Comment
import com.omerkaya.sperrmuellfinder.domain.model.ReportTargetType
import com.omerkaya.sperrmuellfinder.core.util.DateTimeFormatters
import com.omerkaya.sperrmuellfinder.ui.report.ReportBottomSheet
import com.skydoves.landscapist.glide.GlideImage
import java.util.*

/**
 * Comments screen matching the design in the image
 * Dark theme with user profiles and report functionality
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsScreen(
    postId: String,
    onNavigateBack: () -> Unit,
    onNavigateToUserProfile: (String) -> Unit,
    onNavigateToProfile: () -> Unit,
    viewModel: CommentsViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentUserId by viewModel.currentUserId.collectAsStateWithLifecycle()
    var commentText by rememberSaveable { mutableStateOf("") }
    var reportCommentId by remember { mutableStateOf<String?>(null) }
    var deleteCommentId by remember { mutableStateOf<String?>(null) }

    // Load comments when screen is first displayed
    LaunchedEffect(postId) {
        viewModel.loadComments(postId)
    }

    // Light theme - white background
    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color.White
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top bar
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.comments_title),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Medium,
                            color = Color.Black
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.cd_back),
                            tint = Color.Black
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
            
            // Comments list
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when {
                    uiState.isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    
                    uiState.error != null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = uiState.error!!,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    
                    uiState.comments.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.empty_comments),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                    
                    else -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(0.dp)
                        ) {
                            items(
                                items = uiState.comments,
                                key = { it.id }
                            ) { comment ->
                                CommentItem(
                                    comment = comment,
                                    currentUserId = currentUserId,
                                    onUserClick = { 
                                        // Navigate to own profile or other user's profile
                                        if (currentUserId != null && comment.authorId == currentUserId) {
                                            onNavigateToProfile() // Own profile
                                        } else {
                                            onNavigateToUserProfile(comment.authorId) // Other user's profile
                                        }
                                    },
                                    onReportClick = { 
                                        reportCommentId = comment.id
                                    },
                                    onDeleteClick = {
                                        deleteCommentId = comment.id
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
            
            // Bottom input section
            CommentInputSection(
                commentText = commentText,
                onCommentTextChange = { commentText = it },
                onSendClick = {
                    if (commentText.trim().isNotEmpty()) {
                        viewModel.addComment(postId, commentText.trim())
                        commentText = ""
                    }
                },
                isLoading = uiState.isAddingComment,
                modifier = Modifier.fillMaxWidth()
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
            title = { Text(stringResource(R.string.delete_comment)) },
            text = { Text(stringResource(R.string.delete_comment_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteComment(deleteCommentId!!)
                        deleteCommentId = null
                    }
                ) {
                    Text(text = stringResource(R.string.delete_comment))
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
private fun CommentItem(
    comment: Comment,
    currentUserId: String?,
    onUserClick: () -> Unit,
    onReportClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDropdown by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            // User avatar - resimdeki gibi
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                if (!comment.authorPhotoUrl.isNullOrEmpty()) {
                    GlideImage(
                        imageModel = { comment.authorPhotoUrl },
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .clickable { onUserClick() },
                        failure = {
                            // Show initials if image fails to load
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(
                                        brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                            colors = listOf(
                                                Color(0xFF6366F1),
                                                Color(0xFF8B5CF6)
                                            )
                                        )
                                    )
                                    .clickable { onUserClick() },
                                contentAlignment = Alignment.Center
                            ) {
                                val initials = comment.authorName.let { name ->
                                    if (name.isBlank()) {
                                        "U"
                                    } else {
                                        name.split(" ")
                                            .take(2)
                                            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                                            .joinToString("")
                                            .takeIf { it.isNotEmpty() } ?: name.firstOrNull()?.uppercaseChar()?.toString() ?: "U"
                                    }
                                }
                                
                                Text(
                                    text = initials,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    )
                } else {
                    // Show initials when no photo URL
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(
                                brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF6366F1),
                                        Color(0xFF8B5CF6)
                                    )
                                )
                            )
                            .clickable { onUserClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        val initials = comment.authorName.let { name ->
                            if (name.isBlank()) {
                                "U"
                            } else {
                                name.split(" ")
                                    .take(2)
                                    .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                                    .joinToString("")
                                    .takeIf { it.isNotEmpty() } ?: name.firstOrNull()?.uppercaseChar()?.toString() ?: "U"
                            }
                        }
                        
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
            
            // Comment content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Username and date - resimdeki gibi
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (comment.authorName.isNotBlank()) comment.authorName else "Unknown User",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.clickable { onUserClick() }
                    )
                    
                    Text(
                        text = DateTimeFormatters.formatTimeAgo(comment.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                // Comment text
                Text(
                    text = if (comment.content.isNotBlank()) comment.content else "No content",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            
            // Three dots menu - resimdeki gibi
            Box {
                IconButton(
                    onClick = { showDropdown = true },
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
                    expanded = showDropdown,
                    onDismissRequest = { showDropdown = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                ) {
                    if (currentUserId != null && currentUserId == comment.authorId) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(R.string.delete_comment),
                                    color = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                showDropdown = false
                                onDeleteClick()
                            }
                        )
                    } else {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = stringResource(R.string.report_comment),
                                    color = MaterialTheme.colorScheme.error
                                )
                            },
                            onClick = {
                                showDropdown = false
                                onReportClick()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentInputSection(
    commentText: String,
    onCommentTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column {
            // Quick emoji reactions - en çok kullanılan iconlar
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(listOf("❤️", "😍", "🔥", "👍", "😂", "😊", "👏", "💯")) { emoji ->
                    Surface(
                        modifier = Modifier.clickable { 
                            onCommentTextChange(emoji)
                        },
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = emoji,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 20.sp)
                        )
                    }
                }
            }
            
            // Input field
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // User avatar (current user)
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF6366F1),
                                    Color(0xFF8B5CF6)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "U",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                
                // Input field
                OutlinedTextField(
                    value = commentText,
                    onValueChange = onCommentTextChange,
                    placeholder = {
                        Text(
                            text = stringResource(R.string.add_comment_hint),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    trailingIcon = if (commentText.isNotEmpty()) {
                        {
                            IconButton(
                                onClick = onSendClick,
                                enabled = !isLoading
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = MaterialTheme.colorScheme.primary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = stringResource(R.string.send_comment),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    } else null
                )
            }
        }
    }
}
