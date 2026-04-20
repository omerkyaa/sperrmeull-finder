package com.omerkaya.sperrmuellfinder.ui.settings.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.omerkaya.sperrmuellfinder.domain.model.Language

/**
 * 🌐 LANGUAGE SELECTION DIALOG - SperrmüllFinder
 * Rules.md compliant - Material3 dialog component
 */

@Composable
fun LanguageSelectionDialog(
    currentLanguage: Language,
    onLanguageSelected: (Language) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = when (currentLanguage) {
                    Language.GERMAN -> "Sprache auswählen"
                    Language.ENGLISH -> "Select Language"
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Language.values().forEach { language ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (language == currentLanguage),
                                onClick = { onLanguageSelected(language) },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (language == currentLanguage),
                            onClick = null // handled by selectable
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = language.flag,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = language.displayName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = when (currentLanguage) {
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
