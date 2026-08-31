package com.sunjk.sunjktool.feature.learninglog.edit

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope

import android.Manifest
import android.content.pm.PackageManager
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.sunjk.sunjktool.feature.notebook.picker.NotebookPickerDialog
import com.sunjk.sunjktool.ui.components.ConfirmDialog
import com.sunjk.sunjktool.ui.components.LoadingIndicator
import com.sunjk.sunjktool.util.ImageUtil
import java.io.File
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogEditScreen(
    viewModel: LogEditViewModel,
    onNavigateBack: () -> Unit,
    animatedVisibilityScope: AnimatedVisibilityScope? = null,
    sharedTransitionScope: SharedTransitionScope? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Multi-select gallery picker
    val multiImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris: List<Uri> ->
        for (uri in uris) {
            val path = ImageUtil.copyToInternal(context, uri)
            if (path != null) viewModel.addImagePath(path)
        }
    }

    // Single image picker (fallback for API < 33)
    val singleImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val path = ImageUtil.copyToInternal(context, uri)
            if (path != null) viewModel.addImagePath(path)
        }
    }


    // Attachment picker (Word / PDF)
    val attachmentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        val paths = uris.mapNotNull { uri -> copyAttachmentToInternal(context, uri) }
        if (paths.isNotEmpty()) viewModel.addAttachmentPaths(context, paths)
    }
    // Camera: generate temp file URI via FileProvider → crop → add
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    var cropFile by remember { mutableStateOf<String?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val uri = cameraUri
        if (success && uri != null) {
            val file = File(context.filesDir, "images/${uri.lastPathSegment}")
            if (file.exists()) {
                cropFile = file.absolutePath
            }
        }
    }

    // Camera permission + launch helper
    fun launchCamera() {
        val fileName = "camera_${UUID.randomUUID()}.jpg"
        val file = File(context.filesDir, "images/$fileName")
        file.parentFile?.mkdirs()
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        cameraUri = uri
        cameraLauncher.launch(uri)
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchCamera()
    }

    // Navigate back on save
    LaunchedEffect(uiState.saveComplete) {
        if (uiState.saveComplete) onNavigateBack()
    }

    var showDiscardDialog by remember { mutableStateOf(false) }
    var showNotebookPicker by remember { mutableStateOf(false) }

    // Notebook picker dialog
    if (showNotebookPicker) {
        NotebookPickerDialog(
            allNotebooks = uiState.allNotebooks,
            selectedNotebookId = uiState.notebookId,
            excludeIds = emptySet(),
            onSelected = { id, name -> viewModel.selectNotebook(id, name) },
            onDismiss = { showNotebookPicker = false }
        )
    }

    if (showDiscardDialog) {
        ConfirmDialog(
            title = "放弃编辑？",
            message = "当前修改尚未保存，确定要放弃吗？",
            confirmText = "放弃",
            onConfirm = {
                showDiscardDialog = false
                onNavigateBack()
            },
            onDismiss = { showDiscardDialog = false }
        )
    }

    // Crop screen after photo capture
    cropFile?.let { path ->
        CropScreen(
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
            val fabShared = sharedTransitionScope?.let { s ->
                with(s) {
                    animatedVisibilityScope?.let { scope ->
                        Modifier.sharedBounds(rememberSharedContentState("home_add_fab"), scope)
                    } ?: Modifier
                }
            } ?: Modifier
            TopAppBar(
                modifier = fabShared,
                windowInsets = WindowInsets(0.dp),
                title = {
                    Text(if (uiState.isEditMode) "编辑记录" else "新建记录")
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (viewModel.hasUnsavedChanges()) {
                            showDiscardDialog = true
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Box(modifier = modifier.fillMaxSize().padding(innerPadding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                if (uiState.isSaving) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Title (required)
                OutlinedTextField(
                    value = uiState.title,
                    onValueChange = viewModel::updateTitle,
                    label = { Text("标题 *") },
                    isError = uiState.titleError != null,
                    supportingText = uiState.titleError?.let { err ->
                        { Text(err, color = MaterialTheme.colorScheme.error) }
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Subject (optional)
                OutlinedTextField(
                    value = uiState.subject,
                    onValueChange = viewModel::updateSubject,
                    label = { Text("科目") },
                    placeholder = { Text("例如: 数学, Kotlin, 英语") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Description (optional)
                OutlinedTextField(
                    value = uiState.description,
                    onValueChange = viewModel::updateDescription,
                    label = { Text("描述（可选）") },
                    placeholder = { Text("记录学习内容、心得等，AI 总结时会参考") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Time spent (optional)
                OutlinedTextField(
                    value = uiState.timeSpent,
                    onValueChange = viewModel::updateTimeSpent,
                    label = { Text("花费时间 (分钟)") },
                    placeholder = { Text("例如: 45") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Notebook selector
                Text(
                    text = "笔记本",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showNotebookPicker = true },
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceContainerLow
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Folder,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = uiState.notebookName ?: "无（不归属）",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                            color = if (uiState.notebookName != null)
                                MaterialTheme.colorScheme.onSurface
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Image section
                Text(
                    text = "图片",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Thumbnail row
                if (uiState.imagePaths.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        itemsIndexed(uiState.imagePaths) { index, path ->
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(MaterialTheme.shapes.small)
                            ) {
                                AsyncImage(
                                    model = File(path),
                                    contentDescription = "图片 ${index + 1}",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(MaterialTheme.shapes.small),
                                    contentScale = ContentScale.Crop
                                )
                                Surface(
                                    modifier = Modifier.align(Alignment.TopEnd),
                                    shape = RoundedCornerShape(bottomStart = 10.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.9f),
                                    tonalElevation = 2.dp,
                                    shadowElevation = 2.dp
                                ) {
                                    IconButton(
                                        onClick = {
                                            ImageUtil.deleteInternal(path)
                                            viewModel.removeImagePath(index)
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "移除",
                                            tint = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Add image buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= 33) {
                                multiImagePicker.launch(
                                    PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            } else {
                                singleImagePicker.launch(
                                    PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("从相册选择")
                    }
                    OutlinedButton(
                        onClick = {
                            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                                == PackageManager.PERMISSION_GRANTED
                            ) {
                                launchCamera()
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("拍照")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))


                // Attachments (Word / PDF)
                Text(
                    "附件（Word / PDF）",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (uiState.attachmentPaths.isNotEmpty()) {
                    uiState.attachmentPaths.forEachIndexed { index, path ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                        ) {
                            Text(
                                File(path).name,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.weight(1f),
                                maxLines = 1
                            )
                            IconButton(onClick = { viewModel.removeAttachmentPath(index) }) {
                                Icon(Icons.Default.Close, "移除附件", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                OutlinedButton(
                    onClick = { attachmentPicker.launch(arrayOf("application/pdf", "application/msword", "application/vnd.openxmlformats-officedocument.wordprocessingml.document")) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AttachFile, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("选择 Word / PDF 文件")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "保存后 AI 总结会自动提取文件文本并加入上下文。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // Save button
                Button(
                    onClick = viewModel::save,
                    enabled = !uiState.isSaving,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (uiState.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("保存")
                }
            }
        }
    }
}

private fun copyAttachmentToInternal(context: Context, uri: Uri): String? {
    return try {
        val fileName = queryAttachmentName(context, uri) ?: "attachment_${System.currentTimeMillis()}"
        val dir = File(context.filesDir, "attachments").apply { mkdirs() }
        val dest = File(dir, fileName)
        context.contentResolver.openInputStream(uri)?.use { input ->
            dest.outputStream().use { output -> input.copyTo(output) }
        }
        dest.absolutePath
    } catch (_: Exception) {
        null
    }
}

private fun queryAttachmentName(context: Context, uri: Uri): String? {
    return try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
        }
    } catch (_: Exception) {
        null
    }
}