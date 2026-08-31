package com.sunjk.sunjktool.feature.lifelog.edit

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.sunjk.sunjktool.feature.lifelog.MoodConfig
import com.sunjk.sunjktool.ui.components.ConfirmDialog
import com.sunjk.sunjktool.ui.components.LoadingIndicator
import com.sunjk.sunjktool.util.ImageUtil
import java.io.File
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LifeLogEditScreen(
    viewModel: LifeLogEditViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val multiImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        for (uri in uris) {
            val path = ImageUtil.copyToInternal(context, uri)
            if (path != null) viewModel.addImagePath(path)
        }
    }

    val singleImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val path = ImageUtil.copyToInternal(context, uri)
            if (path != null) viewModel.addImagePath(path)
        }
    }

    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    var cropFile by remember { mutableStateOf<String?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = cameraUri
        if (success && uri != null) {
            val file = File(context.filesDir, "images/${uri.lastPathSegment}")
            if (file.exists()) cropFile = file.absolutePath
        }
    }

    fun launchCamera() {
        val fileName = "camera_${UUID.randomUUID()}.jpg"
        val file = File(context.filesDir, "images/$fileName")
        file.parentFile?.mkdirs()
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        cameraUri = uri
        cameraLauncher.launch(uri)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) launchCamera() }

    LaunchedEffect(uiState.saveComplete) {
        if (uiState.saveComplete) onNavigateBack()
    }

    var showDiscardDialog by remember { mutableStateOf(false) }
    if (showDiscardDialog) {
        ConfirmDialog(
            title = "放弃编辑？",
            message = "当前修改尚未保存，确定要放弃吗？",
            confirmText = "放弃",
            onConfirm = { showDiscardDialog = false; onNavigateBack() },
            onDismiss = { showDiscardDialog = false }
        )
    }

    cropFile?.let { path ->
        // Reuse CropScreen from learning log
        com.sunjk.sunjktool.feature.learninglog.edit.CropScreen(
            filePath = path,
            onDismiss = { cropFile = null },
            onCropped = { croppedPath ->
                cropFile = null
                viewModel.addImagePath(croppedPath)
            }
        )
    }

    if (uiState.isLoading) {
        LoadingIndicator()
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditMode) "编辑记录" else "新建记录") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (viewModel.hasUnsavedChanges()) showDiscardDialog = true
                        else onNavigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            if (uiState.isSaving) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(8.dp))
            }

            // Content (required)
            OutlinedTextField(
                value = uiState.content,
                onValueChange = viewModel::updateContent,
                label = { Text("内容 *") },
                isError = uiState.contentError != null,
                supportingText = uiState.contentError?.let { err ->
                    { Text(err, color = MaterialTheme.colorScheme.error) }
                },
                minLines = 4,
                maxLines = 8,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(16.dp))

            // Mood selector
            Text(
                text = "心情（可选）",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(MoodConfig.allMoods.size) { idx ->
                    val mood = MoodConfig.allMoods[idx]
                    val isSelected = mood.key in uiState.selectedMoods
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.toggleMood(mood.key) },
                        label = { Text(mood.label, style = MaterialTheme.typography.labelMedium) },
                        leadingIcon = {
                            Icon(mood.icon, null, Modifier.size(16.dp))
                        }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Image section
            Text(
                text = "图片",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))

            if (uiState.imagePaths.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(uiState.imagePaths) { index, path ->
                        Box(
                            modifier = Modifier.size(100.dp).clip(MaterialTheme.shapes.small)
                        ) {
                            AsyncImage(
                                model = File(path),
                                contentDescription = "图片 ${index + 1}",
                                modifier = Modifier.fillMaxSize().clip(MaterialTheme.shapes.small),
                                contentScale = ContentScale.Crop
                            )
                            Surface(
                                modifier = Modifier.align(Alignment.TopEnd),
                                shape = RoundedCornerShape(bottomStart = 10.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.9f)
                            ) {
                                IconButton(
                                    onClick = { ImageUtil.deleteInternal(path); viewModel.removeImagePath(index) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Close, "移除", Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = {
                        if (Build.VERSION.SDK_INT >= 33) {
                            multiImagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        } else {
                            singleImagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PhotoLibrary, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("从相册选择")
                }
                OutlinedButton(
                    onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                            launchCamera()
                        } else {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.CameraAlt, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("拍照")
                }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = viewModel::save,
                enabled = !uiState.isSaving,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text("保存")
            }
        }
    }
}
