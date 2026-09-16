package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UploadStatus
import com.example.ui.MainViewModel
import com.example.ui.dialogs.CreateRepoDialog
import com.example.ui.dialogs.RepoPickerDialog
import com.example.util.FolderScanner

@Composable
fun UploadScreen(
  viewModel: MainViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val clipboardManager = LocalClipboardManager.current
  val scrollState = rememberScrollState()

  val repos by viewModel.repos.collectAsState()
  val isLoadingRepos by viewModel.isLoadingRepos.collectAsState()
  val selectedRepo by viewModel.selectedRepo.collectAsState()
  val branches by viewModel.branches.collectAsState()
  val selectedBranch by viewModel.selectedBranch.collectAsState()
  val targetDirectory by viewModel.targetDirectory.collectAsState()
  val commitMessage by viewModel.commitMessage.collectAsState()
  val uploadStatus by viewModel.uploadStatus.collectAsState()
  val scannedFiles by viewModel.scannedFiles.collectAsState()
  val selectedFolderName by viewModel.selectedFolderName.collectAsState()
  val filterGit by viewModel.filterGit.collectAsState()
  val filterHidden by viewModel.filterHidden.collectAsState()

  var showRepoPicker by remember { mutableStateOf(false) }
  var showCreateRepoDialog by remember { mutableStateOf(false) }
  var showBranchDropdown by remember { mutableStateOf(false) }
  var isFileListExpanded by remember { mutableStateOf(false) }

  // Storage Access Framework folder picker launcher
  val folderPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocumentTree()
  ) { uri: Uri? ->
    if (uri != null) {
      viewModel.scanFolder(uri)
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .verticalScroll(scrollState)
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {

    // 1. Target Repository & Branch Card
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = CardDefaults.outlinedCardBorder()
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Destination Repository",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )
          Row {
            TextButton(onClick = { viewModel.loadUserRepos() }) {
              Icon(Icons.Default.Refresh, contentDescription = "Refresh repos", modifier = Modifier.size(16.dp))
            }
            TextButton(onClick = { showCreateRepoDialog = true }) {
              Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("New")
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Selected repo box
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .clickable { showRepoPicker = true }
            .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = if (selectedRepo?.isPrivate == true) Icons.Default.Lock else Icons.Default.Public,
              contentDescription = null,
              tint = if (selectedRepo?.isPrivate == true) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = selectedRepo?.fullName ?: "Select a repository...",
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              if (selectedRepo?.description != null) {
                Text(
                  text = selectedRepo!!.description!!,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
              }
            }
            Icon(
              imageVector = Icons.Default.ArrowDropDown,
              contentDescription = "Select repo",
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Branch and Target Folder inside repo
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          // Branch selector
          Box(modifier = Modifier.weight(0.45f)) {
            OutlinedTextField(
              value = selectedBranch,
              onValueChange = {},
              readOnly = true,
              label = { Text("Branch") },
              modifier = Modifier
                .fillMaxWidth()
                .clickable { showBranchDropdown = true },
              trailingIcon = {
                IconButton(onClick = { showBranchDropdown = true }) {
                  Icon(Icons.Default.ArrowDropDown, contentDescription = "Select branch")
                }
              },
              singleLine = true,
              shape = RoundedCornerShape(10.dp)
            )

            DropdownMenu(
              expanded = showBranchDropdown,
              onDismissRequest = { showBranchDropdown = false }
            ) {
              if (branches.isEmpty()) {
                DropdownMenuItem(
                  text = { Text(selectedBranch) },
                  onClick = { showBranchDropdown = false }
                )
              } else {
                branches.forEach { branch ->
                  DropdownMenuItem(
                    text = { Text(branch.name) },
                    onClick = {
                      viewModel.setSelectedBranch(branch.name)
                      showBranchDropdown = false
                    }
                  )
                }
              }
            }
          }

          // Sub-path inside repo
          OutlinedTextField(
            value = targetDirectory,
            onValueChange = { viewModel.setTargetDirectory(it) },
            label = { Text("Repo Path") },
            placeholder = { Text("/ (Root)") },
            modifier = Modifier.weight(0.55f),
            singleLine = true,
            shape = RoundedCornerShape(10.dp)
          )
        }
      }
    }

    // 2. Select Folder Card
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = CardDefaults.outlinedCardBorder()
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Text(
          text = "Select Device Folder",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface
        )
        Text(
          text = "Pick any directory from storage. All subfolders and nested files will be auto-created on GitHub.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
        )

        Button(
          onClick = { folderPickerLauncher.launch(null) },
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("select_folder_button"),
          shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
          Icon(Icons.Default.FolderOpen, contentDescription = null)
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = if (selectedFolderName.isNotBlank()) "Change Selected Folder" else "Browse & Pick Folder",
            fontWeight = FontWeight.SemiBold
          )
        }

        // Scanning indicator
        if (uploadStatus is UploadStatus.Scanning) {
          val scanMsg = (uploadStatus as UploadStatus.Scanning).progressMessage
          Spacer(modifier = Modifier.height(12.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
              text = scanMsg,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.primary
            )
          }
        }

        // Selected Folder Summary
        if (scannedFiles.isNotEmpty() && selectedFolderName.isNotBlank()) {
          Spacer(modifier = Modifier.height(14.dp))
          val totalBytes = scannedFiles.sumOf { it.sizeBytes }

          Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(12.dp)
          ) {
            Column(modifier = Modifier.padding(14.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Column {
                    Text(
                      text = selectedFolderName,
                      fontWeight = FontWeight.Bold,
                      style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                      text = "${scannedFiles.size} files • ${FolderScanner.formatFileSize(totalBytes)}",
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }

                IconButton(onClick = { isFileListExpanded = !isFileListExpanded }) {
                  Icon(
                    imageVector = if (isFileListExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = "Expand files list"
                  )
                }
              }

              // Expandable File preview list
              AnimatedVisibility(visible = isFileListExpanded) {
                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                ) {
                  HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))
                  LazyColumn(
                    modifier = Modifier
                      .fillMaxWidth()
                      .heightIn(max = 240.dp)
                  ) {
                    items(scannedFiles) { file ->
                      Row(
                        modifier = Modifier
                          .fillMaxWidth()
                          .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Icon(
                          imageVector = Icons.Default.Description,
                          contentDescription = null,
                          modifier = Modifier.size(16.dp),
                          tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                          text = file.relativePath,
                          style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                          color = MaterialTheme.colorScheme.onSurface,
                          modifier = Modifier.weight(1f),
                          maxLines = 1,
                          overflow = TextOverflow.Ellipsis
                        )
                        Text(
                          text = FolderScanner.formatFileSize(file.sizeBytes),
                          style = MaterialTheme.typography.labelSmall,
                          color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                      }
                    }
                  }
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Filter toggles
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Switch(
                checked = filterGit,
                onCheckedChange = { viewModel.setFilterGit(it) },
                modifier = Modifier.size(36.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text("Skip .git folder", style = MaterialTheme.typography.bodySmall)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
              Switch(
                checked = filterHidden,
                onCheckedChange = { viewModel.setFilterHidden(it) },
                modifier = Modifier.size(36.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text("Skip hidden files", style = MaterialTheme.typography.bodySmall)
            }
          }
        }
      }
    }

    // 3. Commit Message Card (if folder is selected)
    if (scannedFiles.isNotEmpty()) {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = "Commit Message",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = commitMessage,
            onValueChange = { viewModel.setCommitMessage(it) },
            placeholder = { Text("e.g. Upload $selectedFolderName files") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(10.dp)
          )

          Spacer(modifier = Modifier.height(8.dp))

          // Quick prefix chips
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            FilterChip(
              selected = commitMessage.startsWith("feat:"),
              onClick = {
                viewModel.setCommitMessage("feat: upload $selectedFolderName files")
              },
              label = { Text("feat:") }
            )
            FilterChip(
              selected = commitMessage.startsWith("chore:"),
              onClick = {
                viewModel.setCommitMessage("chore: upload $selectedFolderName directory")
              },
              label = { Text("chore:") }
            )
            FilterChip(
              selected = commitMessage.startsWith("init:"),
              onClick = {
                viewModel.setCommitMessage("init: project $selectedFolderName")
              },
              label = { Text("init:") }
            )
          }
        }
      }
    }

    // 4. Live Upload Progress / Status Card
    when (val status = uploadStatus) {
      is UploadStatus.Uploading -> {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
          border = CardDefaults.outlinedCardBorder()
        ) {
          Column(
            modifier = Modifier
              .padding(20.dp)
              .animateContentSize()
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "Uploading to GitHub...",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
              )
              Text(
                text = "${(status.progress * 100).toInt()}%",
                style = MaterialTheme.typography.titleMedium.copy(
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.Monospace
                ),
                color = MaterialTheme.colorScheme.primary
              )
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
              progress = { status.progress },
              modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
              color = MaterialTheme.colorScheme.primary,
              trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
              text = status.stepDescription,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (status.currentFileName.isNotBlank()) {
              Text(
                text = status.currentFileName,
                style = MaterialTheme.typography.bodySmall.copy(
                  fontFamily = FontFamily.Monospace,
                  fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }
        }
      }

      is UploadStatus.Success -> {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)),
          border = CardDefaults.outlinedCardBorder()
        ) {
          Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Icon(
              imageVector = Icons.Default.CheckCircle,
              contentDescription = "Success",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
              text = "Upload Completed Successfully!",
              style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onSurface
            )

            Text(
              text = "${status.filesUploaded} files uploaded to ${status.repoFullName} (${status.branch})",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )

            // Commit SHA badge
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .clickable {
                  clipboardManager.setText(AnnotatedString(status.commitSha))
                }
                .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
              Text(
                text = "Commit: ${status.commitSha.take(7)}",
                style = MaterialTheme.typography.bodySmall.copy(
                  fontFamily = FontFamily.Monospace,
                  fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.primary
              )
              Spacer(modifier = Modifier.width(6.dp))
              Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy Commit SHA",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              OutlinedButton(
                onClick = {
                  val intent = Intent(Intent.ACTION_VIEW, Uri.parse(status.commitUrl))
                  context.startActivity(intent)
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
              ) {
                Icon(Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("View Commit")
              }

              Button(
                onClick = { viewModel.resetUpload() },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
              ) {
                Text("Upload Another")
              }
            }
          }
        }
      }

      is UploadStatus.Failure -> {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
          border = CardDefaults.outlinedCardBorder()
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = "Error",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Upload Failed",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onErrorContainer
              )
            }

            Text(
              text = status.errorMessage,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onErrorContainer,
              modifier = Modifier.padding(vertical = 8.dp)
            )

            if (status.canRetry) {
              Button(
                onClick = { viewModel.startUpload() },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
              ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Retry Upload")
              }
            }
          }
        }
      }

      else -> {}
    }

    // 5. Main Action Button: Start Upload
    val isUploading = uploadStatus is UploadStatus.Uploading
    Button(
      onClick = { viewModel.startUpload() },
      modifier = Modifier
        .fillMaxWidth()
        .height(52.dp)
        .testTag("start_upload_button"),
      enabled = selectedRepo != null && scannedFiles.isNotEmpty() && !isUploading,
      shape = RoundedCornerShape(12.dp),
      colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
    ) {
      if (isUploading) {
        CircularProgressIndicator(
          modifier = Modifier.size(22.dp),
          color = MaterialTheme.colorScheme.onPrimary,
          strokeWidth = 2.5.dp
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text("Uploading Folder...", fontWeight = FontWeight.Bold, fontSize = 16.sp)
      } else {
        Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = if (scannedFiles.isEmpty()) "Select Folder to Start" else "Start Upload (${scannedFiles.size} files)",
          fontWeight = FontWeight.Bold,
          fontSize = 16.sp
        )
      }
    }
  }

  // Dialogs
  if (showRepoPicker) {
    RepoPickerDialog(
      repos = repos,
      selectedRepo = selectedRepo,
      isLoading = isLoadingRepos,
      onSelectRepo = { viewModel.selectRepo(it) },
      onCreateNewClick = {
        showRepoPicker = false
        showCreateRepoDialog = true
      },
      onDismiss = { showRepoPicker = false }
    )
  }

  if (showCreateRepoDialog) {
    CreateRepoDialog(
      onDismiss = { showCreateRepoDialog = false },
      onCreate = { name, desc, priv ->
        viewModel.createRepository(name, desc, priv) { success ->
          if (success) {
            showCreateRepoDialog = false
          }
        }
      }
    )
  }
}
