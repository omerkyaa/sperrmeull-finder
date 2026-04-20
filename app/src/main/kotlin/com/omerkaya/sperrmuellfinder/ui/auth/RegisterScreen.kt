package com.omerkaya.sperrmuellfinder.ui.auth

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.omerkaya.sperrmuellfinder.R
import com.omerkaya.sperrmuellfinder.domain.model.auth.AuthEvent
import com.omerkaya.sperrmuellfinder.domain.model.auth.AuthResult
import com.omerkaya.sperrmuellfinder.ui.common.SafeGlideImage
import com.omerkaya.sperrmuellfinder.ui.components.PhotoPickerHelper

@Composable
fun RegisterScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToWellDone: () -> Unit = {},
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var nickname by rememberSaveable { mutableStateOf("") }
    var firstName by rememberSaveable { mutableStateOf("") }
    var lastName by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var city by rememberSaveable { mutableStateOf("") }
    var birthDate by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }

    val isRegisterFormValid = nickname.isNotBlank() &&
        firstName.isNotBlank() &&
        lastName.isNotBlank() &&
        email.isNotBlank() &&
        city.isNotBlank() &&
        birthDate.isNotBlank() &&
        password.isNotBlank() &&
        confirmPassword.isNotBlank() &&
        uiState.nicknameError == null &&
        uiState.firstNameError == null &&
        uiState.lastNameError == null &&
        uiState.emailError == null &&
        uiState.cityError == null &&
        uiState.birthDateError == null &&
        uiState.passwordError == null &&
        uiState.confirmPasswordError == null &&
        !uiState.isCheckingNickname &&
        uiState.isNicknameAvailable != false

    fun submitRegistration() {
        if (!isRegisterFormValid || uiState.isLoading) return
        viewModel.onEvent(
            AuthEvent.Register(
                email = email.trim(),
                password = password,
                confirmPassword = confirmPassword,
                nickname = nickname.trim(),
                firstName = firstName.trim(),
                lastName = lastName.trim(),
                city = city.trim(),
                birthDate = birthDate.trim(),
                profilePhotoUrl = uiState.profilePhotoUrl
            )
        )
    }

    LaunchedEffect(Unit) {
        viewModel.authResult.collect { result ->
            when (result) {
                is AuthResult.Success -> Unit
                is AuthResult.Error -> snackbarHostState.showSnackbar(result.message)
                else -> Unit
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { navigation ->
            when (navigation) {
                is com.omerkaya.sperrmuellfinder.domain.model.auth.AuthNavigation.NavigateToLogin -> onNavigateToLogin()
                is com.omerkaya.sperrmuellfinder.domain.model.auth.AuthNavigation.NavigateToWellDone -> onNavigateToWellDone()
                else -> Unit
            }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.auth_sign_up),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = stringResource(R.string.auth_welcome_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            PhotoPickerHelper(
                onPhotoSelected = { uri -> viewModel.onPhotoSelected(context, uri) },
                onError = viewModel::onPhotoPickerError
            ) { onClick ->
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onClick),
                        contentAlignment = Alignment.Center
                    ) {
                        val imageModel = when {
                            !uiState.profilePhotoUrl.isNullOrBlank() -> uiState.profilePhotoUrl
                            !uiState.profilePhotoUri.isNullOrBlank() -> Uri.parse(uiState.profilePhotoUri)
                            else -> null
                        }

                        if (imageModel != null) {
                            SafeGlideImage(
                                imageModel = imageModel,
                                modifier = Modifier.fillMaxSize(),
                                shape = CircleShape,
                                contentDescription = stringResource(R.string.auth_profile_photo)
                            )
                        } else {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = stringResource(R.string.auth_profile_photo),
                                    modifier = Modifier
                                        .size(46.dp)
                                        .padding(10.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            RegisterInputField(
                value = nickname,
                onValueChange = { value ->
                    nickname = value
                    viewModel.updateNickname(value)
                },
                placeholderText = stringResource(R.string.auth_nickname),
                errorText = uiState.nicknameError,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    imeAction = ImeAction.Next
                ),
                enabled = !uiState.isLoading
            )
            if (uiState.isCheckingNickname) {
                Text(
                    text = stringResource(R.string.auth_nickname_checking),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (nickname.length >= 3 && uiState.isNicknameAvailable == true) {
                Text(
                    text = stringResource(R.string.auth_nickname_available),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF2E7D32)
                )
            }

            RegisterInputField(
                value = firstName,
                onValueChange = {
                    firstName = it
                    viewModel.updateFirstName(it)
                },
                placeholderText = stringResource(R.string.auth_first_name),
                errorText = uiState.firstNameError,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                enabled = !uiState.isLoading
            )

            RegisterInputField(
                value = lastName,
                onValueChange = {
                    lastName = it
                    viewModel.updateLastName(it)
                },
                placeholderText = stringResource(R.string.auth_last_name),
                errorText = uiState.lastNameError,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                enabled = !uiState.isLoading
            )

            RegisterInputField(
                value = email,
                onValueChange = {
                    email = it
                    viewModel.updateEmail(it)
                },
                placeholderText = stringResource(R.string.auth_email),
                errorText = uiState.emailError,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                enabled = !uiState.isLoading
            )

            RegisterInputField(
                value = city,
                onValueChange = {
                    city = it
                    viewModel.updateCity(it)
                },
                placeholderText = stringResource(R.string.auth_city),
                errorText = uiState.cityError,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                enabled = !uiState.isLoading
            )

            RegisterInputField(
                value = birthDate,
                onValueChange = {
                    birthDate = it
                    viewModel.updateBirthDate(it)
                },
                placeholderText = stringResource(R.string.auth_date_placeholder),
                labelText = stringResource(R.string.auth_date_of_birth),
                errorText = uiState.birthDateError,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                enabled = !uiState.isLoading
            )

            RegisterInputField(
                value = password,
                onValueChange = {
                    password = it
                    viewModel.updatePassword(it)
                },
                placeholderText = stringResource(R.string.auth_password),
                errorText = uiState.passwordError,
                trailingIcon = {
                    IconButton(onClick = viewModel::togglePasswordVisibility) {
                        Icon(
                            imageVector = if (uiState.isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (uiState.isPasswordVisible) {
                                stringResource(R.string.auth_hide_password)
                            } else {
                                stringResource(R.string.auth_show_password)
                            }
                        )
                    }
                },
                visualTransformation = if (uiState.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next
                ),
                enabled = !uiState.isLoading
            )

            RegisterInputField(
                value = confirmPassword,
                onValueChange = {
                    confirmPassword = it
                    viewModel.updateConfirmPassword(it)
                },
                placeholderText = stringResource(R.string.auth_confirm_password),
                errorText = uiState.confirmPasswordError,
                trailingIcon = {
                    IconButton(onClick = viewModel::toggleConfirmPasswordVisibility) {
                        Icon(
                            imageVector = if (uiState.isConfirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (uiState.isConfirmPasswordVisible) {
                                stringResource(R.string.auth_hide_password)
                            } else {
                                stringResource(R.string.auth_show_password)
                            }
                        )
                    }
                },
                visualTransformation = if (uiState.isConfirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { submitRegistration() }),
                enabled = !uiState.isLoading
            )

            Button(
                onClick = { submitRegistration() },
                enabled = isRegisterFormValid && !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                )
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = stringResource(R.string.auth_sign_up),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }

            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.auth_by_signing_up),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.width(4.dp))
                TextButton(onClick = { }) {
                    Text(
                        text = stringResource(R.string.auth_terms_conditions),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun RegisterInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholderText: String,
    modifier: Modifier = Modifier,
    labelText: String? = null,
    errorText: String? = null,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        label = labelText?.let { { Text(it) } },
        placeholder = { Text(placeholderText) },
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        trailingIcon = trailingIcon,
        isError = errorText != null,
        supportingText = {
            if (errorText != null) {
                Text(errorText)
            }
        },
        enabled = enabled,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            errorBorderColor = MaterialTheme.colorScheme.error
        )
    )
}
