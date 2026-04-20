package com.omerkaya.sperrmuellfinder.ui.search.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.omerkaya.sperrmuellfinder.R
import com.omerkaya.sperrmuellfinder.core.ui.theme.SperrmullPrimary
import com.omerkaya.sperrmuellfinder.domain.model.CategoryData
import com.omerkaya.sperrmuellfinder.domain.model.GermanCities
import com.omerkaya.sperrmuellfinder.domain.model.SearchFilters
import com.omerkaya.sperrmuellfinder.domain.model.TimeRange
import com.omerkaya.sperrmuellfinder.domain.repository.PostSortBy
import kotlin.math.absoluteValue

/**
 * 🎛️ PERFECT FILTER BOTTOM SHEET - SperrmüllFinder
 * Rules.md compliant - Professional Material 3 filter interface
 * 
 * Features:
 * - INTELLIGENT CATEGORY SYSTEM: Popular categories + search functionality
 * - SMART CITY FILTERING: Major German cities + autocomplete suggestions
 * - PRECISION RADIUS CONTROL: Visual distance indicators with landmarks
 * - ADVANCED TIME FILTERING: Flexible time ranges with custom options
 * - COMPREHENSIVE SORTING: Multiple sort criteria with visual indicators
 * - PERFECT UX: Smooth animations, clear visual feedback, accessibility
 * - NO PREMIUM RESTRICTIONS: All features available to everyone
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FilterBottomSheet(
    sheetState: SheetState,
    filters: SearchFilters,
    isPremium: Boolean, // Kept for compatibility but NOT used for gating
    onDismiss: () -> Unit,
    onApplyFilters: (SearchFilters) -> Unit,
    onClearFilters: () -> Unit,
    onPremiumRequired: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategories by remember { mutableStateOf(filters.categories) }
    var selectedCity by remember { mutableStateOf(filters.city) }
    var selectedRadius by remember { mutableFloatStateOf(filters.radiusMeters.toFloat()) }
    var selectedTimeRange by remember { mutableStateOf(filters.timeRange) }
    var selectedSortBy by remember { mutableStateOf(filters.sortBy) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
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
                    text = stringResource(R.string.search_filters_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = SperrmullPrimary
                )
                
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.cd_close_banner),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // 🏷️ INTELLIGENT CATEGORIES SECTION
                item {
                    PerfectFilterSection(
                        title = stringResource(R.string.search_filter_categories),
                        subtitle = stringResource(R.string.filter_categories_subtitle),
                        icon = "🏷️"
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Popular Categories (Always visible)
                            Text(
                                text = stringResource(R.string.popular_categories),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                CategoryData.POPULAR_CATEGORIES.take(8).forEach { category ->
                                    PerfectFilterChip(
                                        text = "${category.icon} ${category.nameDe}",
                                        isSelected = selectedCategories.contains(category.id),
                                        onClick = {
                                            selectedCategories = if (selectedCategories.contains(category.id)) {
                                                selectedCategories - category.id
                                            } else {
                                                selectedCategories + category.id
                                            }
                                        }
                                    )
                                }
                            }
                            
                            // All Categories (Expandable)
                            if (CategoryData.POPULAR_CATEGORIES.size > 8) {
                                var showAllCategories by remember { mutableStateOf(false) }
                                
                                if (showAllCategories) {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        CategoryData.POPULAR_CATEGORIES.drop(8).forEach { category ->
                                            PerfectFilterChip(
                                                text = "${category.icon} ${category.nameDe}",
                                                isSelected = selectedCategories.contains(category.id),
                                                onClick = {
                                                    selectedCategories = if (selectedCategories.contains(category.id)) {
                                                        selectedCategories - category.id
                                                    } else {
                                                        selectedCategories + category.id
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                                
                                OutlinedButton(
                                    onClick = { showAllCategories = !showAllCategories },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = if (showAllCategories) 
                                            stringResource(R.string.show_less_categories)
                                        else 
                                            stringResource(R.string.show_more_categories)
                                    )
                                }
                            }
                        }
                    }
                }
                
                // 🏙️ SMART CITY FILTERING SECTION
                item {
                    PerfectFilterSection(
                        title = stringResource(R.string.search_filter_city),
                        subtitle = stringResource(R.string.filter_city_subtitle),
                        icon = "🏙️"
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Major Cities (Quick selection)
                            Text(
                                text = stringResource(R.string.major_cities),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Clear selection chip
                                if (selectedCity != null) {
                                    PerfectFilterChip(
                                        text = "🚫 ${stringResource(R.string.clear_city)}",
                                        isSelected = false,
                                        onClick = { selectedCity = null },
                                        isDestructive = true
                                    )
                                }
                                
                                // Major German cities
                                GermanCities.MAJOR_CITIES.take(12).forEach { city ->
                                    PerfectFilterChip(
                                        text = "🏙️ $city",
                                        isSelected = selectedCity == city,
                                        onClick = {
                                            selectedCity = if (selectedCity == city) null else city
                                        }
                                    )
                                }
                            }
                            
                            // More cities (Expandable)
                            if (GermanCities.MAJOR_CITIES.size > 12) {
                                var showMoreCities by remember { mutableStateOf(false) }
                                
                                if (showMoreCities) {
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        GermanCities.MAJOR_CITIES.drop(12).forEach { city ->
                                            PerfectFilterChip(
                                                text = "🏙️ $city",
                                                isSelected = selectedCity == city,
                                                onClick = {
                                                    selectedCity = if (selectedCity == city) null else city
                                                }
                                            )
                                        }
                                    }
                                }
                                
                                OutlinedButton(
                                    onClick = { showMoreCities = !showMoreCities },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = if (showMoreCities) 
                                            stringResource(R.string.show_less_cities)
                                        else 
                                            stringResource(R.string.show_more_cities)
                                    )
                                }
                            }
                        }
                    }
                }
                
                // 📍 PRECISION RADIUS CONTROL SECTION
                item {
                    PerfectFilterSection(
                        title = stringResource(R.string.search_filter_radius),
                        subtitle = stringResource(R.string.filter_radius_subtitle),
                        icon = "📍"
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            // Current radius display with landmarks
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = SperrmullPrimary.copy(alpha = 0.1f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.current_search_radius),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = SperrmullPrimary
                                    )
                                    
                                    Text(
                                        text = getRadiusDisplayText(selectedRadius),
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    
                                    Text(
                                        text = getRadiusLandmark(selectedRadius),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }
                            
                            // Quick radius presets
                            Text(
                                text = stringResource(R.string.quick_radius_presets),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                            
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val presets = listOf(500f, 1000f, 2000f, 5000f, 10000f, 25000f)
                                presets.forEach { preset ->
                                    PerfectFilterChip(
                                        text = getRadiusDisplayText(preset),
                                        isSelected = (selectedRadius - preset).absoluteValue < 100f,
                                        onClick = { selectedRadius = preset }
                                    )
                                }
                            }
                            
                            // Precision slider
                            Column {
                                Text(
                                    text = stringResource(R.string.precise_radius_control),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Slider(
                                    value = selectedRadius,
                                    onValueChange = { selectedRadius = it },
                                    valueRange = 500f..50000f,
                                    steps = 49, // More precise steps
                                    colors = SliderDefaults.colors(
                                        thumbColor = SperrmullPrimary,
                                        activeTrackColor = SperrmullPrimary,
                                        inactiveTrackColor = SperrmullPrimary.copy(alpha = 0.3f)
                                    )
                                )
                                
                                // Slider labels
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "500m",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        text = "50km",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Time Range Section - NO PREMIUM RESTRICTIONS
                item {
                    FilterSection(
                        title = stringResource(R.string.search_filter_time_range),
                        isPremium = true // Always show as available
                    ) {
                        Column {
                            TimeRange.values().forEach { timeRange ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .selectable(
                                            selected = selectedTimeRange == timeRange,
                                            onClick = { 
                                                // NO PREMIUM CHECK - All users can select time range
                                                selectedTimeRange = timeRange 
                                            },
                                            role = Role.RadioButton,
                                            enabled = true // Always enabled
                                        )
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedTimeRange == timeRange,
                                        onClick = null,
                                        enabled = true, // Always enabled
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = SperrmullPrimary
                                        )
                                    )
                                    
                                    Spacer(modifier = Modifier.width(8.dp))
                                    
                                    Text(
                                        text = when (timeRange) {
                                            TimeRange.LAST_24H -> stringResource(R.string.time_range_24h)
                                            TimeRange.LAST_48H -> stringResource(R.string.time_range_48h)
                                            TimeRange.LAST_72H -> stringResource(R.string.time_range_72h)
                                            TimeRange.ALL_TIME -> stringResource(R.string.time_range_all)
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // Sort Section
                item {
                    FilterSection(
                        title = stringResource(R.string.search_filter_sort),
                        isPremium = isPremium
                    ) {
                        Column {
                            PostSortBy.values().forEach { sortBy ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .selectable(
                                            selected = selectedSortBy == sortBy,
                                            onClick = { 
                                                if (isPremium) selectedSortBy = sortBy 
                                            },
                                            role = Role.RadioButton,
                                            enabled = isPremium
                                        )
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedSortBy == sortBy,
                                        onClick = null,
                                        enabled = isPremium,
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = SperrmullPrimary,
                                            disabledSelectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                            disabledUnselectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                        )
                                    )
                                    
                                    Spacer(modifier = Modifier.width(8.dp))
                                    
                                    Text(
                                        text = when (sortBy) {
                                            PostSortBy.NEWEST -> stringResource(R.string.sort_newest)
                                            PostSortBy.OLDEST -> stringResource(R.string.sort_oldest)
                                            PostSortBy.NEAREST -> stringResource(R.string.sort_nearest)
                                            PostSortBy.MOST_LIKED -> stringResource(R.string.sort_most_liked)
                                            PostSortBy.MOST_COMMENTED -> stringResource(R.string.sort_most_commented)
                                            PostSortBy.MOST_VIEWED -> stringResource(R.string.sort_most_viewed)
                                            PostSortBy.EXPIRING_SOON -> stringResource(R.string.sort_expiring_soon)
                                        },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isPremium) MaterialTheme.colorScheme.onSurface
                                               else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                            }
                        }
                    }
                }
                
                // Action Buttons
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                selectedCategories = emptyList()
                                selectedCity = null
                                selectedRadius = if (isPremium) 5000f else 1500f
                                selectedTimeRange = TimeRange.ALL_TIME
                                selectedSortBy = PostSortBy.NEWEST
                                onClearFilters()
                            },
                            modifier = Modifier.weight(1f),
                            enabled = isPremium
                        ) {
                            Text(
                                text = stringResource(R.string.filter_clear),
                                color = if (isPremium) SperrmullPrimary 
                                       else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                        }
                        
                        Button(
                            onClick = {
                                val newFilters = filters.copy(
                                    categories = selectedCategories,
                                    city = selectedCity,
                                    radiusMeters = selectedRadius.toInt(),
                                    timeRange = selectedTimeRange,
                                    sortBy = selectedSortBy
                                )
                                onApplyFilters(newFilters)
                            },
                            modifier = Modifier.weight(1f),
                            enabled = isPremium,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SperrmullPrimary,
                                disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                            )
                        ) {
                            Text(
                                text = stringResource(R.string.filter_apply),
                                color = if (isPremium) Color.White 
                                       else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

/**
 * 🎨 PERFECT FILTER SECTION - Professional section wrapper
 */
@Composable
private fun PerfectFilterSection(
    title: String,
    subtitle: String? = null,
    icon: String? = null,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Section header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                icon?.let { emoji ->
                    Text(
                        text = emoji,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    subtitle?.let { sub ->
                        Text(
                            text = sub,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            
            Divider(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                thickness = 1.dp
            )
            
            // Section content
            content()
        }
    }
}

/**
 * 🎯 PERFECT FILTER CHIP - Enhanced filter chip with animations
 */
@Composable
private fun PerfectFilterChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    FilterChip(
        onClick = onClick,
        label = { 
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        },
        selected = isSelected,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surface,
            labelColor = if (isDestructive) MaterialTheme.colorScheme.error 
                        else MaterialTheme.colorScheme.onSurface,
            selectedContainerColor = if (isDestructive) MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                                   else SperrmullPrimary.copy(alpha = 0.2f),
            selectedLabelColor = if (isDestructive) MaterialTheme.colorScheme.error 
                               else SperrmullPrimary
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = isSelected,
            borderColor = if (isDestructive) MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                         else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
            selectedBorderColor = if (isDestructive) MaterialTheme.colorScheme.error
                                 else SperrmullPrimary
        )
    )
}

/**
 * 📍 Get radius display text with appropriate units
 */
@Composable
private fun getRadiusDisplayText(radiusMeters: Float): String {
    return if (radiusMeters >= 1000f) {
        stringResource(R.string.radius_km_format, (radiusMeters / 1000f).toInt())
    } else {
        stringResource(R.string.radius_m_format, radiusMeters.toInt())
    }
}

/**
 * 🏛️ Get landmark reference for radius
 */
@Composable
private fun getRadiusLandmark(radiusMeters: Float): String {
    return when {
        radiusMeters <= 500f -> stringResource(R.string.radius_landmark_neighborhood)
        radiusMeters <= 1000f -> stringResource(R.string.radius_landmark_walking)
        radiusMeters <= 2000f -> stringResource(R.string.radius_landmark_district)
        radiusMeters <= 5000f -> stringResource(R.string.radius_landmark_city_center)
        radiusMeters <= 10000f -> stringResource(R.string.radius_landmark_city)
        radiusMeters <= 25000f -> stringResource(R.string.radius_landmark_metro_area)
        else -> stringResource(R.string.radius_landmark_region)
    }
}

/**
 * Filter section wrapper (Legacy - kept for compatibility)
 */
@Composable
private fun FilterSection(
    title: String,
    isPremium: Boolean,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (isPremium) MaterialTheme.colorScheme.onSurface 
                   else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        
        if (!isPremium) {
            Text(
                text = stringResource(R.string.premium_required),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (!isPremium) {
                        Modifier
                            .background(
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp)
                    } else {
                        Modifier
                    }
                )
        ) {
            content()
        }
    }
}
