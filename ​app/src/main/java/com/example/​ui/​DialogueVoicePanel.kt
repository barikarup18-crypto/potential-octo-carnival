package com.example.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.StageCharacter
import com.example.model.VoicePreset
import com.example.model.VoiceSource
import com.example.model.toBengaliDecimal
import com.example.model.toBengaliDigits

@Composable
fun DialogueVoicePanel(
    sceneCharacters: List<StageCharacter>,
    selectedCharacter: StageCharacter?,
    previewSpeakingCharId: String?,
    isRecording: Boolean,
    recordingSeconds: Int,
    recordingAmplitude: Float,
    onSelectCharacter: (String) -> Unit,
    onUpdateDialogue: (String, String) -> Unit,
    onApplyVoicePreset: (String, VoicePreset) -> Unit,
    onUpdatePitch: (String, Float) -> Unit,
    onUpdateSpeed: (String, Float) -> Unit,
    onSetVoiceSource: (String, VoiceSource) -> Unit,
    onPreviewVoice: (String) -> Unit,
    onStartRecordingClicked: (String) -> Unit,
    onStopRecordingClicked: (String) -> Unit,
    onDeleteRecording: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (sceneCharacters.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.voice_no_char_selected),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(18.dp)
                )
            }
            return
        }

        // Quick Character Selector Bar so user can pick who speaks in one tap
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(sceneCharacters, key = { it.id }) { char ->
                val active = char.id == selectedCharacter?.id
                FilterChip(
                    selected = active,
                    onClick = { onSelectCharacter(char.id) },
                    label = { Text(char.name) },
                    leadingIcon = {
                        CharacterPreviewAvatar(
                            type = char.type,
                            expression = char.expression,
                            outfitColorHex = char.outfitColorHex,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }
        }

        val char = selectedCharacter ?: sceneCharacters.first()
        val isPreviewingThis = previewSpeakingCharId == char.id
        val hasRecorded = !char.recordedAudioPath.isNullOrBlank()

        // 1. Bengali Dialogue Input Box (Speech Bubble)
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "${char.name}-এর ${stringResource(R.string.dialogue_label)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                OutlinedTextField(
                    value = char.dialogue,
                    onValueChange = { onUpdateDialogue(char.id, it) },
                    placeholder = { Text(stringResource(R.string.dialogue_hint)) },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialogue_text_input")
                )

                // Voice Mode Toggle (TTS vs My Recorded Voice)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = char.voiceSource == VoiceSource.TTS,
                        onClick = { onSetVoiceSource(char.id, VoiceSource.TTS) },
                        label = { Text(stringResource(R.string.voice_mode_tts)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.RecordVoiceOver,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = char.voiceSource == VoiceSource.RECORDED,
                        onClick = { onSetVoiceSource(char.id, VoiceSource.RECORDED) },
                        enabled = hasRecorded,
                        label = { Text(stringResource(R.string.voice_mode_recorded)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Mic,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 2. Built-in Bengali TTS Controls (Presets + Pitch & Speed Sliders)
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.voice_preset_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(VoicePreset.entries) { preset ->
                        val active = char.voicePreset == preset && char.voiceSource == VoiceSource.TTS
                        FilterChip(
                            selected = active,
                            onClick = { onApplyVoicePreset(char.id, preset) },
                            label = { Text(preset.labelBn) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                            ),
                            modifier = Modifier.testTag("voice_preset_${preset.name.lowercase()}")
                        )
                    }
                }

                // Pitch Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${stringResource(R.string.voice_pitch)}: ${toBengaliDecimal(char.pitch)}",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.width(142.dp)
                    )
                    Slider(
                        value = char.pitch,
                        onValueChange = { onUpdatePitch(char.id, it) },
                        valueRange = 0.5f..2.0f,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("voice_pitch_slider"),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.secondary,
                            activeTrackColor = MaterialTheme.colorScheme.secondary
                        )
                    )
                }

                // Speed Slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${stringResource(R.string.voice_speed)}: ${toBengaliDecimal(char.speed)}x",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.width(142.dp)
                    )
                    Slider(
                        value = char.speed,
                        onValueChange = { onUpdateSpeed(char.id, it) },
                        valueRange = 0.5f..1.6f,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("voice_speed_slider"),
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.tertiary,
                            activeTrackColor = MaterialTheme.colorScheme.tertiary
                        )
                    )
                }

                Button(
                    onClick = { onPreviewVoice(char.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("preview_tts_button"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPreviewingThis) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.tertiary
                        }
                    )
                ) {
                    Icon(
                        imageVector = if (isPreviewingThis) Icons.Filled.Stop else Icons.Filled.VolumeUp,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isPreviewingThis) {
                            stringResource(R.string.voice_stop_tts)
                        } else {
                            stringResource(R.string.voice_preview_tts)
                        }
                    )
                }
            }
        }

        // 3. Microphone Voice Recording Card
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isRecording) {
                    MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                } else {
                    MaterialTheme.colorScheme.surface
                }
            ),
            border = BorderStroke(
                width = if (isRecording) 2.dp else 1.dp,
                color = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
            )
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.voice_record_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (isRecording) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.GraphicEq,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "রেকর্ডিং ${toBengaliDigits(recordingSeconds)} সে.",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                val pulseScale by animateFloatAsState(
                    targetValue = if (isRecording) 1f + recordingAmplitude * 0.22f else 1f,
                    label = "micPulse"
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(56.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(
                                if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                            )
                            .clickable {
                                if (isRecording) onStopRecordingClicked(char.id)
                                else onStartRecordingClicked(char.id)
                            }
                            .testTag("mic_record_button")
                    ) {
                        Icon(
                            imageVector = if (isRecording) Icons.Filled.Stop else Icons.Filled.Mic,
                            contentDescription = if (isRecording) {
                                stringResource(R.string.voice_record_stop)
                            } else {
                                stringResource(R.string.voice_record_start)
                            },
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isRecording) {
                                "কথা বলুন... শেষ করতে লাল বাটনে ট্যাপ করুন"
                            } else if (hasRecorded) {
                                "আপনার ভয়েস রেকর্ড করা আছে!"
                            } else {
                                "মাইক্রোফোন বাটনে ট্যাপ করে নিজের কণ্ঠ রেকর্ড করুন"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (hasRecorded && !isRecording) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                onSetVoiceSource(char.id, VoiceSource.RECORDED)
                                onPreviewVoice(char.id)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("play_recorded_voice_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.voice_record_play))
                        }

                        OutlinedButton(
                            onClick = { onDeleteRecording(char.id) },
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("delete_recorded_voice_button"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = stringResource(R.string.voice_record_delete)
                            )
                        }
                    }
                }
            }
        }
    }
}
