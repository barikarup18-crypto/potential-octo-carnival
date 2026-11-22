package com.example.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ViewCarousel
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material.icons.outlined.ViewCarousel
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.model.CartoonScene
import com.example.model.StageCharacter
import com.example.model.StudioTab
import com.example.model.toBengaliDigits
import com.example.viewmodel.CartoonStudioUiState
import com.example.viewmodel.CartoonStudioViewModel
import java.io.File

@Composable
fun CartoonStudioScreen(
    viewModel: CartoonStudioViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Pending character ID waiting for runtime RECORD_AUDIO permission approval
    var pendingRecordCharId by remember { mutableStateOf<String?>(null) }

    // Zero-permission Android Photo Picker for custom gallery backgrounds
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.importGalleryBackground(uri)
        }
    }

    val permissionDeniedMsg = stringResource(R.string.voice_permission_needed)
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        val charId = pendingRecordCharId
        pendingRecordCharId = null
        if (granted && charId != null) {
            viewModel.startVoiceRecording(charId)
        } else {
            viewModel.showFeedback(permissionDeniedMsg)
        }
    }

    val requestStartRecording: (String) -> Unit = { charId ->
        val hasPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPerm) {
            viewModel.startVoiceRecording(charId)
        } else {
            pendingRecordCharId = charId
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LaunchedEffect(uiState.statusMessage) {
        val msg = uiState.statusMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isExpandedScreen = maxWidth >= 720.dp

            Scaffold(
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                topBar = {
                    StudioTopBar(
                        onPlayStory = viewModel::openAndPlayStory,
                        onOpenExport = viewModel::openExportDialog
                    )
                },
                bottomBar = {
                    if (!isExpandedScreen) {
                        StudioBottomBar(
                            activeTab = uiState.activeTab,
                            onSelectTab = viewModel::selectTab
                        )
                    }
                }
            ) { innerPadding ->
                val currentScene = uiState.currentScene
                if (currentScene != null) {
                    if (isExpandedScreen) {
                        // Adaptive Tablet / Landscape Canonical Split Layout
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            StudioNavigationRail(
                                activeTab = uiState.activeTab,
                                onSelectTab = viewModel::selectTab
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1.15f)
                                    .fillMaxHeight()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                StageSection(
                                    uiState = uiState,
                                    scene = currentScene,
                                    viewModel = viewModel
                                )
                            }
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                modifier = Modifier
                                    .weight(0.95f)
                                    .fillMaxHeight()
                            ) {
                                ActiveToolPanel(
                                    uiState = uiState,
                                    scene = currentScene,
                                    viewModel = viewModel,
                                    onPickGallery = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    onRequestStartRecording = requestStartRecording
                                )
                            }
                        }
                    } else {
                        // Mobile-First Vertical Layout: Pinned 16:9 Stage + Quick Bar + Scrollable Tool Panel
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.background,
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                            )
                                        )
                                    )
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                StageSection(
                                    uiState = uiState,
                                    scene = currentScene,
                                    viewModel = viewModel
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                            ) {
                                ActiveToolPanel(
                                    uiState = uiState,
                                    scene = currentScene,
                                    viewModel = viewModel,
                                    onPickGallery = {
                                        photoPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    onRequestStartRecording = requestStartRecording
                                )
                            }
                        }
                    }
                }
            }
        }

        // Fullscreen Story Theater Overlay
        if (uiState.isStoryPlayerOpen) {
            StoryPlayerOverlay(
                scenes = uiState.scenes,
                playbackSceneIndex = uiState.playbackSceneIndex,
                playbackSpeakingCharId = uiState.playbackSpeakingCharId,
                playbackProgressInScene = uiState.playbackProgressInScene,
                isPlaying = uiState.isPlaybackPlaying,
                onTogglePlayPause = viewModel::toggleStoryPlayPause,
                onRestart = viewModel::restartStoryPlayback,
                onExportFromPlayer = viewModel::openExportDialog,
                onClose = viewModel::closeStoryPlayer
            )
        }

        // HD Video Export Dialog
        if (uiState.showExportDialog) {
            ExportVideoDialog(
                sceneCount = uiState.scenes.size,
                totalDurationSeconds = uiState.totalDurationSeconds,
                exportState = uiState.exportState,
                onStartExport = viewModel::startVideoExport,
                onShareVideo = { filePath -> shareExportedVideo(context, filePath) },
                onDismiss = viewModel::dismissExportDialog
            )
        }
    }
}

@Composable
private fun StudioTopBar(
    onPlayStory: () -> Unit,
    onOpenExport: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(38.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.studio_subtitle),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onPlayStory,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier
                        .height(42.dp)
                        .testTag("top_play_story_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "চালান", style = MaterialTheme.typography.labelMedium)
                }

                Button(
                    onClick = onOpenExport,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier
                        .height(42.dp)
                        .testTag("top_export_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Filled.MovieCreation, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "সেভ", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun StageSection(
    uiState: CartoonStudioUiState,
    scene: CartoonScene,
    viewModel: CartoonStudioViewModel
) {
    // Interactive 16:9 Cartoon Stage
    CartoonStageCanvas(
        scene = scene,
        selectedCharacterId = uiState.selectedCharacterId,
        speakingCharacterId = uiState.previewSpeakingCharId,
        isInteractive = true,
        onSelectCharacter = viewModel::selectCharacter,
        onMoveCharacter = viewModel::moveCharacter,
        onScaleCharacter = viewModel::scaleCharacterBy,
        onGestureEnded = viewModel::onStageGestureEnded,
        modifier = Modifier.fillMaxWidth()
    )

    // Compact Quick Bar: Scene Pills + Selected Character Quick Actions (Flip, Resize, Speak, Delete)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Mini Scene Switcher Pills
        LazyRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            itemsIndexed(uiState.scenes) { idx, _ ->
                val active = idx == uiState.currentSceneIndex
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(
                        1.dp,
                        if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    ),
                    modifier = Modifier
                        .height(34.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { viewModel.selectScene(idx) }
                        .testTag("mini_scene_pill_$idx")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 10.dp)
                    ) {
                        Text(
                            text = "দৃশ্য ${toBengaliDigits(idx + 1)}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (active) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            item {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .clickable { viewModel.addNewScene() }
                        .testTag("mini_add_scene_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = stringResource(R.string.action_add_scene),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Quick Stage Action Buttons when a character is selected
        val selChar = uiState.selectedCharacter
        if (selChar != null) {
            Spacer(modifier = Modifier.width(6.dp))
            StageQuickActionRow(
                character = selChar,
                onFlip = { viewModel.toggleFlipCharacter(selChar.id) },
                onShrink = { viewModel.setCharacterScale(selChar.id, selChar.scale - 0.12f) },
                onGrow = { viewModel.setCharacterScale(selChar.id, selChar.scale + 0.12f) },
                onSpeak = { viewModel.previewCharacterVoice(selChar.id) },
                onDelete = { viewModel.deleteCharacter(selChar.id) }
            )
        }
    }
}

@Composable
private fun StageQuickActionRow(
    character: StageCharacter,
    onFlip: () -> Unit,
    onShrink: () -> Unit,
    onGrow: () -> Unit,
    onSpeak: () -> Unit,
    onDelete: () -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        QuickRoundAction(
            icon = Icons.Filled.Flip,
            contentDescription = stringResource(R.string.stage_flip),
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
            onClick = onFlip,
            testTag = "quick_flip_button"
        )
        QuickRoundAction(
            icon = Icons.Filled.Remove,
            contentDescription = "ছোট করুন",
            containerColor = MaterialTheme.colorScheme.surface,
            iconTint = MaterialTheme.colorScheme.onSurface,
            onClick = onShrink,
            testTag = "quick_shrink_button"
        )
        QuickRoundAction(
            icon = Icons.Filled.Add,
            contentDescription = "বড় করুন",
            containerColor = MaterialTheme.colorScheme.surface,
            iconTint = MaterialTheme.colorScheme.onSurface,
            onClick = onGrow,
            testTag = "quick_grow_button"
        )
        QuickRoundAction(
            icon = Icons.Filled.VolumeUp,
            contentDescription = "${character.name} কথা বলবে",
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            iconTint = MaterialTheme.colorScheme.onTertiaryContainer,
            onClick = onSpeak,
            testTag = "quick_speak_button"
        )
        QuickRoundAction(
            icon = Icons.Filled.DeleteOutline,
            contentDescription = stringResource(R.string.action_delete),
            containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
            iconTint = MaterialTheme.colorScheme.error,
            onClick = onDelete,
            testTag = "quick_delete_button"
        )
    }
}

@Composable
private fun QuickRoundAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    containerColor: Color,
    iconTint: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Surface(
            shape = CircleShape,
            color = containerColor,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = iconTint,
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}

@Composable
private fun ActiveToolPanel(
    uiState: CartoonStudioUiState,
    scene: CartoonScene,
    viewModel: CartoonStudioViewModel,
    onPickGallery: () -> Unit,
    onRequestStartRecording: (String) -> Unit
) {
    when (uiState.activeTab) {
        StudioTab.CHARACTERS -> CharacterPanel(
            selectedCharacter = uiState.selectedCharacter,
            creatorType = uiState.creatorType,
            creatorExpression = uiState.creatorExpression,
            creatorOutfitColorHex = uiState.creatorOutfitColorHex,
            onSelectCreatorType = viewModel::selectCreatorType,
            onSelectCreatorExpression = viewModel::selectCreatorExpression,
            onSelectCreatorOutfitColor = viewModel::selectCreatorOutfitColor,
            onAddFromCreator = viewModel::addCharacterFromCreator,
            onUpdateSelectedExpression = viewModel::setCharacterExpression,
            onUpdateSelectedOutfitColor = viewModel::setCharacterOutfitColor,
            onUpdateSelectedScale = viewModel::setCharacterScale,
            onFlipSelected = viewModel::toggleFlipCharacter,
            onBringFrontSelected = viewModel::bringCharacterToFront,
            onDeleteSelected = viewModel::deleteCharacter
        )

        StudioTab.BACKGROUNDS -> BackgroundPanel(
            selectedBackgroundType = scene.backgroundType,
            customBackgroundPath = scene.customBackgroundPath,
            onSelectPreset = viewModel::setPresetBackground,
            onPickFromGallery = onPickGallery
        )

        StudioTab.DIALOGUE -> DialogueVoicePanel(
            sceneCharacters = scene.characters,
            selectedCharacter = uiState.selectedCharacter,
            previewSpeakingCharId = uiState.previewSpeakingCharId,
            isRecording = uiState.isRecording,
            recordingSeconds = uiState.recordingSeconds,
            recordingAmplitude = uiState.recordingAmplitude,
            onSelectCharacter = viewModel::selectCharacter,
            onUpdateDialogue = viewModel::updateCharacterDialogue,
            onApplyVoicePreset = viewModel::applyVoicePreset,
            onUpdatePitch = viewModel::setVoicePitch,
            onUpdateSpeed = viewModel::setVoiceSpeed,
            onSetVoiceSource = viewModel::setVoiceSource,
            onPreviewVoice = viewModel::previewCharacterVoice,
            onStartRecordingClicked = onRequestStartRecording,
            onStopRecordingClicked = viewModel::stopVoiceRecording,
            onDeleteRecording = viewModel::deleteRecordedVoice
        )

        StudioTab.SCENES -> SceneTimelinePanel(
            scenes = uiState.scenes,
            currentSceneIndex = uiState.currentSceneIndex,
            totalDurationSeconds = uiState.totalDurationSeconds,
            onSelectScene = viewModel::selectScene,
            onAddScene = viewModel::addNewScene,
            onDuplicateScene = viewModel::duplicateScene,
            onDeleteScene = viewModel::deleteScene,
            onChangeDuration = viewModel::updateSceneDuration,
            onPlayFullStory = viewModel::openAndPlayStory,
            onExportVideo = viewModel::openExportDialog
        )
    }
}

@Composable
private fun StudioBottomBar(
    activeTab: StudioTab,
    onSelectTab: (StudioTab) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp
    ) {
        StudioTab.entries.forEach { tab ->
            val selected = tab == activeTab
            NavigationBarItem(
                selected = selected,
                onClick = { onSelectTab(tab) },
                icon = {
                    Icon(
                        imageVector = tabIcon(tab, selected),
                        contentDescription = tab.labelBn
                    )
                },
                label = {
                    Text(
                        text = tab.labelBn,
                        maxLines = 1,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
            )
        }
    }
}

@Composable
private fun StudioNavigationRail(
    activeTab: StudioTab,
    onSelectTab: (StudioTab) -> Unit
) {
    NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
        StudioTab.entries.forEach { tab ->
            val selected = tab == activeTab
            NavigationRailItem(
                selected = selected,
                onClick = { onSelectTab(tab) },
                icon = {
                    Icon(
                        imageVector = tabIcon(tab, selected),
                        contentDescription = tab.labelBn
                    )
                },
                label = {
                    Text(
                        text = tab.labelBn,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            )
        }
    }
}

private fun tabIcon(tab: StudioTab, selected: Boolean) = when (tab) {
    StudioTab.CHARACTERS -> if (selected) Icons.Filled.Face else Icons.Outlined.Face
    StudioTab.BACKGROUNDS -> if (selected) Icons.Filled.Landscape else Icons.Outlined.Landscape
    StudioTab.DIALOGUE -> if (selected) Icons.Filled.ChatBubble else Icons.Outlined.ChatBubbleOutline
    StudioTab.SCENES -> if (selected) Icons.Filled.ViewCarousel else Icons.Outlined.ViewCarousel
}

private fun shareExportedVideo(context: Context, filePath: String) {
    runCatching {
        val file = File(filePath)
        if (!file.exists()) return
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "video/mp4"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "কার্টুন স্টুডিও ভিডিও")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "কার্টুন ভিডিও শেয়ার করুন"))
    }
}
