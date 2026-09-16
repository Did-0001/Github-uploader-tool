package com.example.ui.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GitHubRepo

@Composable
fun RepoPickerDialog(
  repos: List<GitHubRepo>,
  selectedRepo: GitHubRepo?,
  isLoading: Boolean,
  onSelectRepo: (GitHubRepo) -> Unit,
  onCreateNewClick: () -> Unit,
  onDismiss: () -> Unit
) {
  var searchQuery by remember { mutableStateOf("") }
  val filteredRepos = remember(repos, searchQuery) {
    if (searchQuery.isBlank()) repos
    else repos.filter { it.fullName.contains(searchQuery, ignoreCase = true) }
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("Select Repository", style = MaterialTheme.typography.titleLarge)
        TextButton(onClick = onCreateNewClick) {
          Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("New Repo")
        }
      }
    },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          modifier = Modifier.fillMaxWidth(),
          placeholder = { Text("Search repositories...") },
          leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
          singleLine = true,
          shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(24.dp),
            horizontalArrangement = Arrangement.Center
          ) {
            CircularProgressIndicator(modifier = Modifier.size(32.dp))
          }
        } else if (filteredRepos.isEmpty()) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = if (repos.isEmpty()) "No repositories found." else "No matching repositories.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        } else {
          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(max = 350.dp)
          ) {
            items(filteredRepos, key = { it.id }) { repo ->
              val isSelected = repo.id == selectedRepo?.id
              Card(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 4.dp)
                  .clickable {
                    onSelectRepo(repo)
                    onDismiss()
                  },
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                  containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                  else MaterialTheme.colorScheme.surfaceVariant
                )
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Icon(
                    imageVector = if (repo.isPrivate) Icons.Default.Lock else Icons.Default.Public,
                    contentDescription = if (repo.isPrivate) "Private" else "Public",
                    tint = if (repo.isPrivate) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                  )

                  Spacer(modifier = Modifier.width(12.dp))

                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = repo.name,
                      style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                      color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                      text = repo.fullName,
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }

                  if (repo.starsCount > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Stars",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(16.dp)
                      )
                      Spacer(modifier = Modifier.width(2.dp))
                      Text(
                        text = "${repo.starsCount}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                      )
                    }
                  }

                  if (isSelected) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                      imageVector = Icons.Default.Check,
                      contentDescription = "Selected",
                      tint = MaterialTheme.colorScheme.primary,
                      modifier = Modifier.size(20.dp)
                    )
                  }
                }
              }
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("Close")
      }
    }
  )
}

@Composable
fun CreateRepoDialog(
  onDismiss: () -> Unit,
  onCreate: (name: String, description: String?, isPrivate: Boolean) -> Unit
) {
  var name by remember { mutableStateOf("") }
  var description by remember { mutableStateOf("") }
  var isPrivate by remember { mutableStateOf(false) }
  var isCreating by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = { if (!isCreating) onDismiss() },
    title = { Text("Create New Repository", fontWeight = FontWeight.Bold) },
    text = {
      Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it.trim().replace(" ", "-") },
          label = { Text("Repository Name *") },
          placeholder = { Text("my-new-repo") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text("Description (Optional)") },
          placeholder = { Text("Brief overview of the project") },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(10.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = if (isPrivate) Icons.Default.Lock else Icons.Default.Public,
              contentDescription = null,
              tint = if (isPrivate) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                text = if (isPrivate) "Private Repository" else "Public Repository",
                fontWeight = FontWeight.Medium
              )
              Text(
                text = if (isPrivate) "Only you and collaborators can view" else "Visible to everyone on GitHub",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          Switch(
            checked = isPrivate,
            onCheckedChange = { isPrivate = it },
            colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary)
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (name.isNotBlank()) {
            isCreating = true
            onCreate(name, description.ifBlank { null }, isPrivate)
          }
        },
        enabled = name.isNotBlank() && !isCreating,
        shape = RoundedCornerShape(10.dp)
      ) {
        if (isCreating) {
          CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary)
          Spacer(modifier = Modifier.width(6.dp))
          Text("Creating...")
        } else {
          Text("Create")
        }
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss, enabled = !isCreating) {
        Text("Cancel")
      }
    }
  )
}
