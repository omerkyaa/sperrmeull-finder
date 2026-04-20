package com.omerkaya.sperrmuellfinder.ui.search.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.omerkaya.sperrmuellfinder.R
import com.omerkaya.sperrmuellfinder.core.ui.theme.SperrmullPrimary
import com.omerkaya.sperrmuellfinder.domain.repository.PostSortBy

/**
 * Sort options bottom sheet for search results.
 * Allows users to select different sorting criteria.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortBottomSheet(
    currentSortBy: PostSortBy,
    onSortSelected: (PostSortBy) -> Unit,
    onDismiss: () -> Unit,
    isPremium: Boolean,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.search_sort_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = SperrmullPrimary
                )
                
                IconButton(
                    onClick = onDismiss
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.cd_close_banner),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Sort Options
            val sortOptions = listOf(
                SortOption(
                    sortBy = PostSortBy.NEWEST,
                    titleRes = R.string.search_sort_newest,
                    icon = Icons.Default.Schedule,
                    isPremiumOnly = false
                ),
                SortOption(
                    sortBy = PostSortBy.OLDEST,
                    titleRes = R.string.search_sort_oldest,
                    icon = Icons.Default.AccessTime,
                    isPremiumOnly = false
                ),
                SortOption(
                    sortBy = PostSortBy.NEAREST,
                    titleRes = R.string.search_sort_nearest,
                    icon = Icons.Default.NearMe,
                    isPremiumOnly = true
                ),
                SortOption(
                    sortBy = PostSortBy.MOST_LIKED,
                    titleRes = R.string.search_sort_most_liked,
                    icon = Icons.Default.ThumbUp,
                    isPremiumOnly = false
                ),
                SortOption(
                    sortBy = PostSortBy.MOST_COMMENTED,
                    titleRes = R.string.search_sort_most_commented,
                    icon = Icons.Default.Comment,
                    isPremiumOnly = false
                ),
                SortOption(
                    sortBy = PostSortBy.MOST_VIEWED,
                    titleRes = R.string.search_sort_most_viewed,
                    icon = Icons.Default.Visibility,
                    isPremiumOnly = true
                ),
                SortOption(
                    sortBy = PostSortBy.EXPIRING_SOON,
                    titleRes = R.string.search_sort_expiring_soon,
                    icon = Icons.Default.Schedule,
                    isPremiumOnly = true
                )
            )
            
            sortOptions.forEach { option ->
                SortOptionItem(
                    option = option,
                    isSelected = currentSortBy == option.sortBy,
                    isEnabled = isPremium || !option.isPremiumOnly,
                    onClick = {
                        if (isPremium || !option.isPremiumOnly) {
                            onSortSelected(option.sortBy)
                            onDismiss()
                        }
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SortOptionItem(
    option: SortOption,
    isSelected: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = isEnabled) { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = option.icon,
            contentDescription = null,
            tint = when {
                !isEnabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                isSelected -> SperrmullPrimary
                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            },
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Text(
            text = stringResource(option.titleRes),
            style = MaterialTheme.typography.bodyMedium,
            color = when {
                !isEnabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                isSelected -> SperrmullPrimary
                else -> MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.weight(1f)
        )
        
        if (!isEnabled && option.isPremiumOnly) {
            Text(
                text = stringResource(R.string.premium_badge),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
        
        RadioButton(
            selected = isSelected,
            onClick = null,
            enabled = isEnabled,
            colors = RadioButtonDefaults.colors(
                selectedColor = SperrmullPrimary,
                unselectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                disabledSelectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                disabledUnselectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        )
    }
}

private data class SortOption(
    val sortBy: PostSortBy,
    val titleRes: Int,
    val icon: ImageVector,
    val isPremiumOnly: Boolean
)
