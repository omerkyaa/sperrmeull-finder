package com.omerkaya.sperrmuellfinder.ui.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.omerkaya.sperrmuellfinder.R
import com.omerkaya.sperrmuellfinder.domain.model.AppTheme
import com.omerkaya.sperrmuellfinder.domain.model.Language

/**
 * 🎨 THEME SELECTION DIALOG - SperrmüllFinder
 * Rules.md compliant - Material3 dialog component
 */

@Composable
fun ThemeSelectionDialog(
    currentTheme: AppTheme,
    language: Language,
    onThemeSelected: (AppTheme) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = when (language) {
                    Language.GERMAN -> "Design auswählen"
                    Language.ENGLISH -> "Select Theme"
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                AppTheme.values().forEach { theme ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (theme == currentTheme),
                                onClick = { onThemeSelected(theme) },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (theme == currentTheme),
                            onClick = null // handled by selectable
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = theme.getDisplayName(language),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = when (theme) {
                                    AppTheme.LIGHT -> when (language) {
                                        Language.GERMAN -> "Heller Modus"
                                        Language.ENGLISH -> "Light mode"
                                    }
                                    AppTheme.DARK -> when (language) {
                                        Language.GERMAN -> "Dunkler Modus"
                                        Language.ENGLISH -> "Dark mode"
                                    }
                                    AppTheme.SYSTEM -> when (language) {
                                        Language.GERMAN -> "Folgt den Systemeinstellungen"
                                        Language.ENGLISH -> "Follows system settings"
                                    }
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = when (language) {
                        Language.GERMAN -> "Fertig"
                        Language.ENGLISH -> "Done"
                    }
                )
            }
        },
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
    )
}
