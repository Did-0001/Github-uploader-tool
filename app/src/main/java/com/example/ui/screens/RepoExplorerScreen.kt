package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GitHubContentItem
import com.example.ui.MainViewModel
import com.example.util.FolderScanner

@Composable
fun RepoExplorerScreen(
  viewModel: MainViewModel,
  modifier: Modifier = Modifier
) {
  val selectedRepo by viewModel.selectedRepo.collectAsState()
  val selectedBranch by viewModel.selectedBranch.collectAsState()
  val explorerState by viewModel.explorerState.collectAsState()

  var showNewFileDialog by remember { mutableStateOf(false) }
  var showDeleteConfirmDialog by remember { mutableStateOf<GitHubContentItem?>(null) }
  var deleteCommitMsg by remember { mutableStateOf("") }

  LaunchedEffect(selectedRepo, selectedBranch) {
    if (selectedRepo != null) {
      viewModel.loadExplorerPath("")
    }
  }

  if (selectedRepo == null) {
    Box(
      modifier = modifier
        .fillMaxSize()
        .padding(24.dp),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = "Please select a repository on the Upload tab first.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
    return
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    // Top repository status bar
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
      border = CardDefaults.outlinedCardBorder()
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = selectedRepo!!.name,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "Branch: $selectedBranch",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
          )
        }

        Row {
          IconButton(onClick = { viewModel.loadExplorerPath(explorerState.currentPath) }) {
            Icon(Icons.Default.Refresh, contentDescription = "Refresh files")
          }
          Button(
            onClick = { showNewFileDialog = true },
            shape = RoundedCornerShape(8.dp),
            contentPadding = ButtonDefaults.ButtonWithIconContentPadding
          ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("New File", fontSize = 13.sp)
          }
        }
      }
    }

    // Breadcrumbs path bar
    val breadcrumbScroll = rememberScrollState()
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(breadcrumbScroll)
        .padding(horizontal = 16.dp, vertical = 6.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(
        modifier = Modifier
          .clip(RoundedCornerShape(6.dp))
          .clickable { viewModel.loadExplorerPath("") }
          .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = Icons.Default.Home,
          contentDescription = "Root",
          modifier = Modifier.size(18.dp),
          tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
          text = "root",
          fontWeight = FontWeight.SemiBold,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.primary
        )
      }

      val pathParts = explorerState.currentPath.split("/").filter { it.isNotBlank() }
      var cumulativePath = ""
      for (part in pathParts) {
        cumulativePath = if (cumulativePath.isEmpty()) part else "$cumulativePath/$part"
        val thisPath = cumulativePath
        Text(" / ", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
          text = part,
          style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable { viewModel.loadExplorerPath(thisPath) }
            .padding(horizontal = 6.dp, vertical = 4.dp)
        )
      }
    }

    HorizontalDivider(modifier = Modifier.padding(top = 6.dp))

    // File Viewing/Editing Sheet or Directory Content List
    if (explorerState.viewingFile != null) {
      FileEditorView(
        fileItem = explorerState.viewingFile!!,
        initialContent = explorerState.viewingContent ?: "",
        isSaving = explorerState.isSavingFile,
        onClose = { viewModel.closeFileViewer() },
        onSave = { updatedContent, commitMsg ->
          viewModel.saveEditedFile(explorerState.viewingFile!!, updatedContent, commitMsg) {}
        },
        onDelete = {
          showDeleteConfirmDialog = explorerState.viewingFile
          deleteCommitMsg = "Delete ${explorerState.viewingFile!!.name}"
        }
      )
    } else {
      // Content list
      if (explorerState.isLoading) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
          contentAlignment = Alignment.Center
        ) {
          CircularProgressIndicator(modifier = Modifier.size(36.dp))
        }
      } else if (explorerState.items.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .padding(32.dp),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
              imageVector = Icons.Default.Folder,
              contentDescription = null,
              modifier = Modifier.size(48.dp),
              tint = MaterialTheme.colorScheme.outline
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = "This folder is empty.",
              style = MaterialTheme.typography.bodyLarge,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      } else {
        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
            .padding(horizontal = 16.dp)
        ) {
          // Parent folder navigation row if not root
          if (explorerState.currentPath.isNotBlank()) {
            item {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(8.dp))
                  .clickable {
                    val parts = explorerState.currentPath.split("/").filter { it.isNotBlank() }
                    val parentPath = if (parts.size <= 1) "" else parts.dropLast(1).joinToString("/")
                    viewModel.loadExplorerPath(parentPath)
                  }
                  .padding(vertical = 12.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Icon(
                  imageVector = Icons.Default.ArrowBack,
                  contentDescription = "Up",
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                  text = ".. (parent directory)",
                  style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                  color = MaterialTheme.colorScheme.primary
                )
              }
              HorizontalDivider()
            }
          }

          items(explorerState.items, key = { it.path }) { item ->
            val isDir = item.type == "dir"
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable {
                  if (isDir) {
                    viewModel.loadExplorerPath(item.path)
                  } else {
                    viewModel.viewFile(item)
                  }
                }
                .padding(vertical = 12.dp, horizontal = 8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = if (isDir) Icons.Default.Folder else Icons.Default.Description,
                contentDescription = item.type,
                tint = if (isDir) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
              )

              Spacer(modifier = Modifier.width(12.dp))

              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = item.name,
                  style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isDir) FontWeight.SemiBold else FontWeight.Normal,
                    fontFamily = if (isDir) FontFamily.Default else FontFamily.Monospace
                  ),
                  color = MaterialTheme.colorScheme.onSurface,
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis
                )
                if (!isDir) {
                  Text(
                    text = FolderScanner.formatFileSize(item.size),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }

              if (!isDir) {
                IconButton(
                  onClick = {
                    showDeleteConfirmDialog = item
                    deleteCommitMsg = "Delete ${item.name}"
                  },
                  modifier = Modifier.size(32.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete file",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                  )
                }
              }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
          }
        }
      }
    }
  }

  // Create New File Dialog
  if (showNewFileDialog) {
    var newFileName by remember { mutableStateOf("") }
    var newFileContent by remember { mutableStateOf("") }
    var newFileCommitMsg by remember { mutableStateOf("") }

    AlertDialog(
      onDismissRequest = { showNewFileDialog = false },
      title = { Text("Create New File in Repo", fontWeight = FontWeight.Bold) },
      text = {
        Column(modifier = Modifier.fillMaxWidth()) {
          OutlinedTextField(
            value = newFileName,
            onValueChange = { newFileName = it },
            label = { Text("Filename *") },
            placeholder = { Text("index.html or config.json") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
          )

          Spacer(modifier = Modifier.height(10.dp))

          OutlinedTextField(
            value = newFileContent,
            onValueChange = { newFileContent = it },
            label = { Text("Content") },
            placeholder = { Text("Type code or text here...") },
            modifier = Modifier
              .fillMaxWidth()
              .height(140.dp),
            shape = RoundedCornerShape(10.dp)
          )

          Spacer(modifier = Modifier.height(10.dp))

          OutlinedTextField(
            value = newFileCommitMsg,
            onValueChange = { newFileCommitMsg = it },
            label = { Text("Commit Message") },
            placeholder = { Text("Create $newFileName") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (newFileName.isNotBlank()) {
              viewModel.createNewFileInExplorer(newFileName, newFileContent, newFileCommitMsg) {
                showNewFileDialog = false
              }
            }
          },
          enabled = newFileName.isNotBlank(),
          shape = RoundedCornerShape(10.dp)
        ) {
          Text("Commit File")
        }
      },
      dismissButton = {
        TextButton(onClick = { showNewFileDialog = false }) {
          Text("Cancel")
        }
      }
    )
  }

  // Confirm Delete Dialog
  if (showDeleteConfirmDialog != null) {
    val item = showDeleteConfirmDialog!!
    AlertDialog(
      onDismissRequest = { showDeleteConfirmDialog = null },
      title = { Text("Delete File from GitHub?") },
      text = {
        Column {
          Text(
            text = "Are you sure you want to delete '${item.name}' from branch '$selectedBranch'? This will create a delete commit on GitHub.",
            style = MaterialTheme.typography.bodyMedium
          )
          Spacer(modifier = Modifier.height(12.dp))
          OutlinedTextField(
            value = deleteCommitMsg,
            onValueChange = { deleteCommitMsg = it },
            label = { Text("Commit Message") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.deleteExplorerFile(item, deleteCommitMsg) {
              showDeleteConfirmDialog = null
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
          shape = RoundedCornerShape(10.dp)
        ) {
          Text("Delete File")
        }
      },
      dismissButton = {
        TextButton(onClick = { showDeleteConfirmDialog = null }) {
          Text("Cancel")
        }
      }
    )
  }
}

@Composable
fun FileEditorView(
  fileItem: GitHubContentItem,
  initialContent: String,
  isSaving: Boolean,
  onClose: () -> Unit,
  onSave: (String, String) -> Unit,
  onDelete: () -> Unit
) {
  var content by remember(initialContent) { mutableStateOf(initialContent) }
  var commitMsg by remember { mutableStateOf("Update ${fileItem.name}") }
  var isEditing by remember { mutableStateOf(false) }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .padding(16.dp)
  ) {
    // Editor top bar
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
        IconButton(onClick = onClose) {
          Icon(Icons.Default.Close, contentDescription = "Close editor")
        }
        Spacer(modifier = Modifier.width(4.dp))
        Column {
          Text(
            text = fileItem.name,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Text(
            text = fileItem.path,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
        }
      }

      Row {
        IconButton(onClick = onDelete) {
          Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
        }
        IconButton(onClick = { isEditing = !isEditing }) {
          Icon(
            imageVector = if (isEditing) Icons.Default.Description else Icons.Default.Edit,
            contentDescription = "Toggle edit mode"
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Commit message bar when in edit mode
    AnimatedVisibility(visible = isEditing) {
      Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        OutlinedTextField(
          value = commitMsg,
          onValueChange = { commitMsg = it },
          label = { Text("Commit Message") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(10.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(
          onClick = { onSave(content, commitMsg) },
          enabled = !isSaving,
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
          if (isSaving) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Committing changes...")
          } else {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Commit Changes to GitHub")
          }
        }
      }
    }

    // Code / Text display area
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f),
      shape = RoundedCornerShape(12.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
      border = CardDefaults.outlinedCardBorder()
    ) {
      if (isEditing) {
        OutlinedTextField(
          value = content,
          onValueChange = { content = it },
          modifier = Modifier.fillMaxSize(),
          textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
          shape = RoundedCornerShape(12.dp)
        )
      } else {
        val scrollState = rememberScrollState()
        Box(
          modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(14.dp)
        ) {
          Text(
            text = content,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurface
          )
        }
      }
    }
  }
}
