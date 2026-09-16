package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.MainViewModel
import com.example.util.BrowserUtils

@Composable
fun AuthScreen(
  viewModel: MainViewModel,
  modifier: Modifier = Modifier
) {
  val authState by viewModel.authUiState.collectAsState()
  val context = LocalContext.current
  val clipboardManager = LocalClipboardManager.current
  val scrollState = rememberScrollState()

  var tokenInput by remember { mutableStateOf("") }
  var isTokenVisible by remember { mutableStateOf(false) }
  var showPatSection by remember { mutableStateOf(false) }
  var showAdvancedSettings by remember { mutableStateOf(false) }
  var customClientIdInput by remember { mutableStateOf("") }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .verticalScroll(scrollState)
      .padding(horizontal = 24.dp, vertical = 28.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Top
  ) {
    // App Logo & Header
    Box(
      modifier = Modifier
        .size(68.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.primaryContainer)
        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        imageVector = Icons.Default.CloudUpload,
        contentDescription = "Git Uploader Logo",
        modifier = Modifier.size(38.dp),
        tint = MaterialTheme.colorScheme.onPrimaryContainer
      )
    }

    Spacer(modifier = Modifier.height(14.dp))

    Text(
      text = "Git Uploader",
      style = MaterialTheme.typography.headlineMedium.copy(
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp
      ),
      color = MaterialTheme.colorScheme.onBackground
    )

    Text(
      text = "Upload entire folders & nested files directly to GitHub",
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
      modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
    )

    // PRIMARY: Automatic Browser Login Card
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .testTag("github_oauth_card"),
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = CardDefaults.outlinedCardBorder()
    ) {
      Column(
        modifier = Modifier.padding(22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(44.dp)
              .clip(CircleShape)
              .background(Color(0xFF24292E)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              painter = painterResource(id = R.drawable.ic_github_logo),
              contentDescription = "GitHub",
              tint = Color.White,
              modifier = Modifier.size(24.dp)
            )
          }
          Spacer(modifier = Modifier.width(14.dp))
          Column {
            Text(
              text = "Sign in with GitHub",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "Log in in browser with zero manual setup",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
          text = "Tap below to authenticate in your browser. All required repository permissions are configured automatically.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Active Device Code Flow UI (when login is in progress)
        if (authState.deviceCodeInfo != null) {
          val codeInfo = authState.deviceCodeInfo!!

          Card(
            modifier = Modifier
              .fillMaxWidth()
              .testTag("device_code_card"),
            colors = CardDefaults.cardColors(
              containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            ),
            shape = RoundedCornerShape(16.dp),
            border = CardDefaults.outlinedCardBorder()
          ) {
            Column(
              modifier = Modifier.padding(18.dp),
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "Authorization Code",
                  style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                  color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(
                  onClick = { viewModel.cancelDeviceFlow() },
                  modifier = Modifier.size(24.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cancel",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }

              Spacer(modifier = Modifier.height(10.dp))

              // Clickable code display
              Surface(
                modifier = Modifier
                  .clip(RoundedCornerShape(12.dp))
                  .clickable {
                    clipboardManager.setText(AnnotatedString(codeInfo.userCode))
                    Toast.makeText(context, "Code ${codeInfo.userCode} copied!", Toast.LENGTH_SHORT).show()
                  },
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Text(
                    text = codeInfo.userCode,
                    style = MaterialTheme.typography.headlineMedium.copy(
                      fontFamily = FontFamily.Monospace,
                      fontWeight = FontWeight.ExtraBold,
                      letterSpacing = 3.sp
                    ),
                    color = MaterialTheme.colorScheme.primary
                  )
                  Spacer(modifier = Modifier.width(12.dp))
                  Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "Copy code",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                  )
                }
              }

              Text(
                text = "✓ Code automatically copied to clipboard",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
              )

              Spacer(modifier = Modifier.height(16.dp))

              // Steps guide
              Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
              ) {
                Column(modifier = Modifier.padding(12.dp)) {
                  Text(
                    text = "1. Browser opens GitHub verification page",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = "2. Paste the code (${codeInfo.userCode}) and tap Continue",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = "3. Tap Authorize — this app logs in automatically!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                }
              }

              Spacer(modifier = Modifier.height(16.dp))

              Button(
                onClick = {
                  clipboardManager.setText(AnnotatedString(codeInfo.userCode))
                  Toast.makeText(context, "Code copied! Opening GitHub...", Toast.LENGTH_SHORT).show()
                  BrowserUtils.openUrl(context, codeInfo.verificationUri)
                },
                modifier = Modifier
                  .fillMaxWidth()
                  .height(48.dp)
                  .testTag("reopen_browser_button"),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
              ) {
                Icon(
                  imageVector = Icons.Default.OpenInBrowser,
                  contentDescription = null,
                  modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open GitHub Verification Page", fontWeight = FontWeight.Bold)
              }

              Spacer(modifier = Modifier.height(12.dp))

              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
              ) {
                CircularProgressIndicator(
                  modifier = Modifier.size(16.dp),
                  strokeWidth = 2.dp,
                  color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                  text = "Waiting for approval on GitHub...",
                  style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        } else {
          // Standard state: Big "Log in with GitHub" button
          Button(
            onClick = {
              viewModel.startDeviceFlow(customClientIdInput) { codeInfo ->
                // Auto-copy code and launch browser immediately
                clipboardManager.setText(AnnotatedString(codeInfo.userCode))
                Toast.makeText(context, "Code ${codeInfo.userCode} copied! Opening GitHub...", Toast.LENGTH_LONG).show()
                BrowserUtils.openUrl(context, codeInfo.verificationUri)
              }
            },
            modifier = Modifier
              .fillMaxWidth()
              .height(52.dp)
              .testTag("login_with_github_button"),
            enabled = !authState.isVerifying,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = Color(0xFF24292E),
              contentColor = Color.White
            )
          ) {
            if (authState.isVerifying) {
              CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = Color.White,
                strokeWidth = 2.dp
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text("Connecting to GitHub...", fontWeight = FontWeight.SemiBold)
            } else {
              Icon(
                painter = painterResource(id = R.drawable.ic_github_logo),
                contentDescription = "GitHub",
                modifier = Modifier.size(22.dp),
                tint = Color.White
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = "Log in with GitHub",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
              )
            }
          }
        }
      }
    }

    // Error Message
    AnimatedVisibility(visible = authState.errorMessage != null) {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = RoundedCornerShape(12.dp)
      ) {
        Row(
          modifier = Modifier.padding(16.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = "Error",
            tint = MaterialTheme.colorScheme.error
          )
          Spacer(modifier = Modifier.width(12.dp))
          Text(
            text = authState.errorMessage ?: "",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(24.dp))

    // SECONDARY: Alternative Login Options (Personal Access Token & Custom Client ID)
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { showPatSection = !showPatSection },
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Key,
              contentDescription = "PAT",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = "Use Personal Access Token (PAT)",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
              color = MaterialTheme.colorScheme.onSurface
            )
          }
          Icon(
            imageVector = if (showPatSection) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = if (showPatSection) "Collapse" else "Expand",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        AnimatedVisibility(visible = showPatSection) {
          Column(modifier = Modifier.padding(top = 14.dp)) {
            Text(
              text = "For fine-grained or enterprise tokens requiring 'repo' permission.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(bottom = 12.dp)
            )

            OutlinedTextField(
              value = tokenInput,
              onValueChange = { tokenInput = it },
              modifier = Modifier
                .fillMaxWidth()
                .testTag("github_token_input"),
              label = { Text("Personal Access Token") },
              placeholder = { Text("ghp_... or github_pat_...") },
              singleLine = true,
              visualTransformation = if (isTokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
              keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
              ),
              keyboardActions = KeyboardActions(
                onDone = { viewModel.verifyAndLogin(tokenInput) }
              ),
              trailingIcon = {
                IconButton(onClick = { isTokenVisible = !isTokenVisible }) {
                  Icon(
                    imageVector = if (isTokenVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (isTokenVisible) "Hide token" else "Show token"
                  )
                }
              },
              shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Create Token link button
            OutlinedButton(
              onClick = {
                BrowserUtils.openUrl(
                  context,
                  "https://github.com/settings/tokens/new?scopes=repo,read:user,user:email&description=GitUploaderApp"
                )
              },
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(10.dp)
            ) {
              Icon(
                imageVector = Icons.Default.OpenInBrowser,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text("Generate Pre-configured Token in Browser", fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
              onClick = { viewModel.verifyAndLogin(tokenInput) },
              modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .testTag("verify_token_button"),
              enabled = tokenInput.isNotBlank() && !authState.isVerifying,
              shape = RoundedCornerShape(10.dp)
            ) {
              Text("Verify & Connect Token", fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Advanced Settings Toggle (Custom OAuth App Client ID)
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clickable { showAdvancedSettings = !showAdvancedSettings },
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Settings,
              contentDescription = "Settings",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = "Advanced OAuth Settings",
              style = MaterialTheme.typography.titleSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          Icon(
            imageVector = if (showAdvancedSettings) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = if (showAdvancedSettings) "Collapse" else "Expand",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        AnimatedVisibility(visible = showAdvancedSettings) {
          Column(modifier = Modifier.padding(top = 12.dp)) {
            Text(
              text = "By default, Git Uploader uses the verified public Client ID. If your organization requires a dedicated GitHub OAuth App, enter its Client ID below:",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(bottom = 8.dp)
            )

            OutlinedTextField(
              value = customClientIdInput,
              onValueChange = { customClientIdInput = it },
              modifier = Modifier.fillMaxWidth(),
              label = { Text("Custom Client ID (Optional)") },
              placeholder = { Text("Default: Built-in Official Client") },
              singleLine = true,
              shape = RoundedCornerShape(10.dp)
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // Security & Privacy Note
    Row(
      modifier = Modifier.padding(horizontal = 8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        imageVector = Icons.Default.Lock,
        contentDescription = "Security",
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(16.dp)
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = "Direct connection with GitHub API. Tokens stored securely on device.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}
