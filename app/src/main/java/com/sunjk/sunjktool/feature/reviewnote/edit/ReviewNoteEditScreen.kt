package com.sunjk.sunjktool.feature.reviewnote.edit

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import coil.compose.AsyncImage
import com.sunjk.sunjktool.domain.model.ReviewNoteSource
import com.sunjk.sunjktool.feature.learninglog.edit.CropScreen
import com.sunjk.sunjktool.util.ImageUtil
import java.io.File
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewNoteEditScreen(
    viewModel: ReviewNoteEditViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // --- unsaved-changes guard ---
    var hasUnsavedChanges by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.content, uiState.imagePaths.size) {
        if (!uiState.isLoading) hasUnsavedChanges = true
    }

    LaunchedEffect(uiState.saveComplete) {
        if (uiState.saveComplete) onNavigateBack()
    }

    // Back press guard
    val lifecycleOwner = LocalLifecycleOwner.current
    BackHandler(enabled = hasUnsavedChanges && !uiState.saveComplete) {
        showDiscardDialog = true
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("放弃编辑") },
            text = { Text("有未保存的内容，确定放弃吗？") },
            confirmButton = {
                TextButton(onClick = { showDiscardDialog = false; onNavigateBack() }) {
                    Text("放弃", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) { Text("继续编辑") }
            }
        )
    }

    // --- Image pickers ---
    var pendingCropPath by remember { mutableStateOf<String?>(null) }

    val multiImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        uris.forEach { uri ->
            ImageUtil.copyToInternal(context, uri)?.let { viewModel.addImagePath(it) }
        }
    }

    val singleImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { ImageUtil.copyToInternal(context, it)?.let { path -> viewModel.addImagePath(path) } }
    }

    val cameraUri = remember {
        val file = File(context.filesDir, "images/${UUID.randomUUID()}.jpg").apply { parentFile?.mkdirs() }
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            val path = File(context.filesDir, "images/${cameraUri.lastPathSegment ?: "photo.jpg"}").absolutePath
            pendingCropPath = path
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) cameraLauncher.launch(cameraUri)
    }

    fun launchCamera() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            cameraLauncher.launch(cameraUri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Crop dialog
    if (pendingCropPath != null) {
        CropScreen(
            filePath = pendingCropPath!!,
            onDismiss = { pendingCropPath = null },
            onCropped = { path ->
                pendingCropPath = null
                viewModel.addImagePath(path)
            }
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditMode) "编辑心得" else "添加心得") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (hasUnsavedChanges) showDiscardDialog = true
                        else onNavigateBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.save() },
                        enabled = !uiState.isSaving
                    ) {
                        if (uiState.isSaving) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text("保存")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Source info (read-only)
                if (uiState.sourceType == ReviewNoteSource.FLASHCARD) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.small,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f))
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.tertiary)
                            Spacer(Modifier.width(8.dp))
                            Text("来源: 闪卡错题导出", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // Content text field
                OutlinedTextField(
                    value = uiState.content,
                    onValueChange = { viewModel.updateContent(it) },
                    label = { Text("Markdown 内容") },
                    placeholder = { Text("输入复盘心得，支持 Markdown 格式...") },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 200.dp),
                    isError = uiState.contentError,
                    supportingText = if (uiState.contentError) {
                        { Text("内容不能为空") }
                    } else null
                )

                Spacer(Modifier.height(16.dp))

                // Image picker buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        if (android.os.Build.VERSION.SDK_INT >= 33) {
                            multiImagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        } else {
                            singleImagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                    }) {
                        Icon(Icons.Default.PhotoLibrary, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("从相册选择")
                    }
                    OutlinedButton(onClick = { launchCamera() }) {
                        Icon(Icons.Default.CameraAlt, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("拍照")
                    }
                }

                // Image thumbnails
                if (uiState.imagePaths.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(uiState.imagePaths, key = { _, p -> p }) { idx, path ->
                            Box(modifier = Modifier.size(80.dp)) {
                                AsyncImage(
                                    model = File(path),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                IconButton(
                                    onClick = { viewModel.removeImagePath(idx) },
                                    modifier = Modifier.align(Alignment.TopEnd).size(22.dp)
                                ) {
                                    Icon(Icons.Default.Cancel, "移除", Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
