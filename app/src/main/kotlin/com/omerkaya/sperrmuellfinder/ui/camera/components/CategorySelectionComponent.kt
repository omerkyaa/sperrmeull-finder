package com.omerkaya.sperrmuellfinder.ui.camera.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.omerkaya.sperrmuellfinder.R
import com.omerkaya.sperrmuellfinder.data.util.FirestoreConstants

/**
 * 🔥 CATEGORY SELECTION COMPONENT - SperrmüllFinder
 * Rules.md compliant - Manual category selection with max 3 categories limit
 * 
 * Features:
 * - Manual category selection (max 3 categories per post)
 * - German UI display with English internal storage
 * - Professional Material3 chip design
 * - Real-time selection count and validation
 * - Accessibility support with content descriptions
 * - Constants-based category mapping
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategorySelectionComponent(
    selectedCategories: List<String>,
    onCategoryToggle: (categoryEn: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with title and selection count
            CategorySelectionHeader(
                selectedCount = selectedCategories.size,
                maxCount = FirestoreConstants.MAX_CATEGORIES_PER_POST
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Category chips in flow layout
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FirestoreConstants.CATEGORIES_EN.forEachIndexed { index, categoryEn ->
                    val categoryDe = FirestoreConstants.CATEGORIES_DE[index]
                    val isSelected = selectedCategories.contains(categoryEn)
                    val canSelect = selectedCategories.size < FirestoreConstants.MAX_CATEGORIES_PER_POST || isSelected
                    
                    CategoryChip(
                        categoryEn = categoryEn,
                        categoryDe = categoryDe,
                        isSelected = isSelected,
                        isEnabled = canSelect,
                        onClick = { onCategoryToggle(categoryEn) }
                    )
                }
            }
            
            // Warning message if limit reached
            if (selectedCategories.size >= FirestoreConstants.MAX_CATEGORIES_PER_POST) {
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = stringResource(R.string.category_limit_warning),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun CategorySelectionHeader(
    selectedCount: Int,
    maxCount: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.select_category_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Text(
            text = stringResource(R.string.categories_selected, selectedCount, maxCount),
            style = MaterialTheme.typography.bodyMedium,
            color = if (selectedCount >= maxCount) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

@Composable
private fun CategoryChip(
    categoryEn: String,
    categoryDe: String,
    isSelected: Boolean,
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.primary
            isEnabled -> MaterialTheme.colorScheme.surface
            else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
        },
        label = "chip_background"
    )
    
    val contentColor by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.onPrimary
            isEnabled -> MaterialTheme.colorScheme.onSurface
            else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        },
        label = "chip_content"
    )
    
    val borderColor by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.primary
            isEnabled -> MaterialTheme.colorScheme.outline
            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        },
        label = "chip_border"
    )
    
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable(enabled = isEnabled) { onClick() }
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(20.dp)
            ),
        color = backgroundColor,
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(16.dp)
                )
                
                Spacer(modifier = Modifier.width(4.dp))
            }
            
            Text(
                text = categoryDe, // Display German name
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal
            )
        }
    }
}

// Preview functions
@Preview(showBackground = true)
@Composable
private fun CategorySelectionComponentPreview() {
    CategorySelectionComponent(
        selectedCategories = listOf("furniture", "electronics"),
        onCategoryToggle = { }
    )
}

@Preview(showBackground = true)
@Composable
private fun CategorySelectionComponentMaxSelectedPreview() {
    CategorySelectionComponent(
        selectedCategories = listOf("furniture", "electronics", "clothing"),
        onCategoryToggle = { }
    )
}

@Preview(showBackground = true)
@Composable
private fun CategoryChipPreview() {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(16.dp)
    ) {
        CategoryChip(
            categoryEn = "furniture",
            categoryDe = "Möbel",
            isSelected = false,
            isEnabled = true,
            onClick = { }
        )
        
        CategoryChip(
            categoryEn = "electronics",
            categoryDe = "Elektronik",
            isSelected = true,
            isEnabled = true,
            onClick = { }
        )
        
        CategoryChip(
            categoryEn = "clothing",
            categoryDe = "Kleidung",
            isSelected = false,
            isEnabled = false,
            onClick = { }
        )
    }
}
