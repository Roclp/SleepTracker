package com.example.SleepTracker.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.SleepTracker.viewmodel.AccountViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    viewModel: AccountViewModel = viewModel()
) {
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("个 人 中 心") },
                actions = {
                    if (!viewModel.isEditing) {
                        IconButton(onClick = { viewModel.startEditing() }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit profile")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (viewModel.isEditing) {
                EditProfileContent(viewModel)
            } else {
                DisplayProfileContent(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileContent(viewModel: AccountViewModel) {
    OutlinedTextField(
        value = viewModel.tempName,
        onValueChange = { viewModel.updateTempName(it) },
        label = { Text("昵 称") },
        modifier = Modifier.fillMaxWidth()
    )

    OutlinedTextField(
        value = viewModel.tempEmail,
        onValueChange = { viewModel.updateTempEmail(it) },
        label = { Text("性 别") },
        modifier = Modifier.fillMaxWidth()
    )

    OutlinedTextField(
        value = viewModel.tempAge,
        onValueChange = { viewModel.updateTempAge(it) },
        label = { Text("年 龄") },
        modifier = Modifier.fillMaxWidth()
    )

    OutlinedTextField(
        value = viewModel.tempWeight,
        onValueChange = { viewModel.updateTempWeight(it) },
        label = { Text("体重 (kg)") },
        modifier = Modifier.fillMaxWidth()
    )

    OutlinedTextField(
        value = viewModel.tempHeight,
        onValueChange = { viewModel.updateTempHeight(it) },
        label = { Text("身高 (cm)") },
        modifier = Modifier.fillMaxWidth()
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = { viewModel.cancelEditing() },
            modifier = Modifier.weight(1f)
        ) {
            Text("取消")
        }
        Button(
            onClick = { viewModel.saveProfile() },
            modifier = Modifier.weight(1f)
        ) {
            Text("保存")
        }
    }
}

@Composable
private fun DisplayProfileContent(viewModel: AccountViewModel) {
    val profile = viewModel.userProfile

    ProfileField("昵 称", profile.name.toString())
    ProfileField("性 别", profile.email.toString())
    ProfileField("年 龄", profile.age.toString())
    ProfileField("体 重", "${profile.weight} kg")
    ProfileField("身 高", "${profile.height} cm")
}

@Composable
private fun ProfileField(label: String, value: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value.ifEmpty { "未设置" },
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
    }
}
