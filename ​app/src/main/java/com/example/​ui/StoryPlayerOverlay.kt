package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.CartoonScene
import com.example.model.toBengaliDigits

@Composable
fun StoryPlayerOverlay(
    scenes: List<CartoonScene>,
    playbackSceneIndex: Int,
    playbackSpeakingCharId: String?,
    playbackProgressInScene: Float,
    isPlaying: Boolean,
    onTogglePlayPause: () -> Unit,
    onRestart: () -> Unit,
    onExportFromPlayer: () -> Unit,
    onClose: () -> Unit
) {
    BackHandler { onClose() }

    val currentScene = scenes.getOrNull(playbackSceneIndex) ?: return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF514121F))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
            .testTag("story_player_overlay")
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 820.dp)
                .align(Alignment.Center),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.player_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "দৃশ্য ${toBengaliDigits(playbackSceneIndex + 1)} / ${toBengaliDigits(scenes.size)} • ${currentScene.backgroundType.labelBn}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.78f)
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.16f)
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.testTag("close_story_player_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = stringResource(R.string.action_close),
                            tint = Color.White
                        )
                    }
                }
            }

            // Center 16:9 Theater Stage
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                CartoonStageCanvas(
                    scene = currentScene,
                    selectedCharacterId = null,
                    speakingCharacterId = playbackSpeakingCharId,
                    isInteractive = false,
                    modifier = Modifier.fillMaxWidth()
                )

                // Segmented Scene Progress Bars
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    scenes.forEachIndexed { i, _ ->
                        val progress = when {
                            i < playbackSceneIndex -> 1f
                            i == playbackSceneIndex -> playbackProgressInScene
                            else -> 0f
                        }
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(CircleShape),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = Color.White.copy(alpha = 0.22f)
                        )
                    }
                }
            }

            // Bottom Theater Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.15f)
                ) {
                    IconButton(
                        onClick = onRestart,
                        modifier = Modifier
                            .size(52.dp)
                            .testTag("player_restart_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Replay,
                            contentDescription = "পুনরায় শুরু",
                            tint = Color.White
                        )
                    }
                }

                Button(
                    onClick = onTogglePlayPause,
                    modifier = Modifier
                        .height(54.dp)
                        .testTag("player_play_pause_button"),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isPlaying) "বিরতি" else "চালান",
                        style = MaterialTheme.typography.titleSmall
                    )
                }

                Button(
                    onClick = {
                        onClose()
                        onExportFromPlayer()
                    },
                    modifier = Modifier
                        .height(54.dp)
                        .testTag("player_export_button"),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MovieCreation,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.action_export_video))
                }
            }
        }
    }
}
