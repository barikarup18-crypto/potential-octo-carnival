package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.CharacterType
import com.example.model.Expression
import com.example.model.StageCharacter
import com.example.model.toBengaliDigits
import com.example.ui.theme.OutfitColorSwatches
import kotlin.math.roundToInt

@Composable
fun CharacterPanel(
    selectedCharacter: StageCharacter?,
    creatorType: CharacterType,
    creatorExpression: Expression,
    creatorOutfitColorHex: Long,
    onSelectCreatorType: (CharacterType) -> Unit,
    onSelectCreatorExpression: (Expression) -> Unit,
    onSelectCreatorOutfitColor: (Long) -> Unit,
    onAddFromCreator: () -> Unit,
    onUpdateSelectedExpression: (String, Expression) -> Unit,
    onUpdateSelectedOutfitColor: (String, Long) -> Unit,
    onUpdateSelectedScale: (String, Float) -> Unit,
    onFlipSelected: (String) -> Unit,
    onBringFrontSelected: (String) -> Unit,
    onDeleteSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Selected Stage Character Quick Editor
        if (selectedCharacter != null) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                ),
                border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.size(40.dp)
                            ) {
                                CharacterPreviewAvatar(
                                    type = selectedCharacter.type,
                                    expression = selectedCharacter.expression,
                                    outfitColorHex = selectedCharacter.outfitColorHex,
                                    modifier = Modifier.padding(3.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "নির্বাচিত: ${selectedCharacter.name}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "মঞ্চে টেনে সরান বা নিচে সাজান",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Quick Delete button
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .clickable { onDeleteSelected(selectedCharacter.id) }
                                .testTag("delete_selected_char_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.DeleteOutline,
                                    contentDescription = stringResource(R.string.action_delete),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    // Expression selector for selected character
                    Text(
                        text = stringResource(R.string.stage_expression),
                        style = MaterialTheme.typography.labelLarge
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(Expression.entries) { expr ->
                            val active = selectedCharacter.expression == expr
                            FilterChip(
                                selected = active,
                                onClick = { onUpdateSelectedExpression(selectedCharacter.id, expr) },
                                label = { Text(expr.labelBn) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                modifier = Modifier.testTag("selected_expr_${expr.name.lowercase()}")
                            )
                        }
                    }

                    // Size slider + Flip + Front row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${stringResource(R.string.stage_size)} (${toBengaliDigits((selectedCharacter.scale * 100).roundToInt())}%)",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Slider(
                            value = selectedCharacter.scale,
                            onValueChange = { onUpdateSelectedScale(selectedCharacter.id, it) },
                            valueRange = 0.5f..1.8f,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("selected_char_size_slider"),
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onFlipSelected(selectedCharacter.id) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("flip_selected_char_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Flip,
                                contentDescription = stringResource(R.string.stage_flip),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.stage_flip))
                        }

                        OutlinedButton(
                            onClick = { onBringFrontSelected(selectedCharacter.id) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("front_selected_char_button"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Layers,
                                contentDescription = stringResource(R.string.stage_front),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.stage_front))
                        }
                    }

                    // Outfit color dots for selected character
                    Text(
                        text = stringResource(R.string.stage_outfit_color),
                        style = MaterialTheme.typography.labelLarge
                    )
                    OutfitColorSwatchRow(
                        selectedHex = selectedCharacter.outfitColorHex,
                        onSelectHex = { onUpdateSelectedOutfitColor(selectedCharacter.id, it) }
                    )
                }
            }
        }

        // 2. Character Creator & Library Section
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.stage_character_creator),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                // Horizontal scrollable library of all 6 cute cartoon characters
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    items(CharacterType.entries) { type ->
                        val isChosen = creatorType == type
                        val cardColor = if (isChosen) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                        }
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = cardColor,
                            border = BorderStroke(
                                width = if (isChosen) 2.5.dp else 1.dp,
                                color = if (isChosen) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier
                                .width(102.dp)
                                .clickable { onSelectCreatorType(type) }
                                .testTag("library_char_${type.name.lowercase()}")
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp)
                            ) {
                                CharacterPreviewAvatar(
                                    type = type,
                                    expression = if (isChosen) creatorExpression else Expression.HAPPY,
                                    outfitColorHex = if (isChosen) creatorOutfitColorHex else type.defaultOutfitColorHex,
                                    animateTalking = isChosen && creatorExpression == Expression.TALKING,
                                    modifier = Modifier.size(72.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = type.shortNameBn,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Creator Expression & Outfit customization
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(Expression.entries) { expr ->
                        FilterChip(
                            selected = creatorExpression == expr,
                            onClick = { onSelectCreatorExpression(expr) },
                            label = { Text(expr.labelBn) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.secondary,
                                selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                            )
                        )
                    }
                }

                OutfitColorSwatchRow(
                    selectedHex = creatorOutfitColorHex,
                    onSelectHex = onSelectCreatorOutfitColor
                )

                Button(
                    onClick = onAddFromCreator,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("add_character_to_stage_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.AddCircle,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${creatorType.shortNameBn} — ${stringResource(R.string.stage_add_to_stage)}",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun OutfitColorSwatchRow(
    selectedHex: Long,
    onSelectHex: (Long) -> Unit
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(OutfitColorSwatches) { hex ->
            val selected = hex == selectedHex
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(hex))
                    .border(
                        width = if (selected) 3.dp else 1.5.dp,
                        color = if (selected) MaterialTheme.colorScheme.onSurface else Color.White.copy(alpha = 0.8f),
                        shape = CircleShape
                    )
                    .clickable { onSelectHex(hex) }
            ) {
                if (selected) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
