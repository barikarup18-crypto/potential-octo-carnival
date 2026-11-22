package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.model.BackgroundType
import java.io.File

@Composable
fun BackgroundPanel(
    selectedBackgroundType: BackgroundType,
    customBackgroundPath: String?,
    onSelectPreset: (BackgroundType) -> Unit,
    onPickFromGallery: () -> Unit,
    modifier: Modifier = Modifier
) {
    val presets = listOf(
        BackgroundType.VILLAGE_ROAD,
        BackgroundType.SCHOOL,
        BackgroundType.FOREST,
        BackgroundType.HOUSE_INTERIOR
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Gallery upload button (Android Photo Picker - zero storage permissions)
        Button(
            onClick = onPickFromGallery,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("pick_gallery_background_button"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary
            )
        ) {
            Icon(
                imageVector = Icons.Filled.AddPhotoAlternate,
                contentDescription = stringResource(R.string.bg_custom_gallery),
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.bg_custom_gallery),
                style = MaterialTheme.typography.labelLarge
            )
        }

        // Show custom gallery thumbnail if user uploaded one
        if (!customBackgroundPath.isNullOrBlank()) {
            val isCustomActive = selectedBackgroundType == BackgroundType.CUSTOM
            Surface(
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    width = if (isCustomActive) 3.dp else 1.dp,
                    color = if (isCustomActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onSelectPreset(BackgroundType.CUSTOM) }
                    .testTag("bg_option_custom")
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = File(customBackgroundPath),
                        contentDescription = BackgroundType.CUSTOM.labelBn,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    BackgroundLabelOverlay(
                        label = "আমার গ্যালারির ছবি",
                        isSelected = isCustomActive
                    )
                }
            }
        }

        // 2x2 Grid of vibrant pre-built cartoon backgrounds
        presets.chunked(2).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowItems.forEach { bgType ->
                    val isSelected = selectedBackgroundType == bgType
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            width = if (isSelected) 3.dp else 1.2.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(16f / 10f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onSelectPreset(bgType) }
                            .testTag("bg_option_${bgType.name.lowercase()}")
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            bgType.drawableRes?.let { resId ->
                                Image(
                                    painter = painterResource(id = resId),
                                    contentDescription = bgType.labelBn,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            BackgroundLabelOverlay(
                                label = bgType.labelBn,
                                isSelected = isSelected
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BackgroundLabelOverlay(
    label: String,
    isSelected: Boolean
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.68f))
                )
            )
            .padding(10.dp)
    ) {
        if (isSelected) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(28.dp)
                    .align(Alignment.TopEnd)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.align(Alignment.BottomStart)
        )
    }
}
