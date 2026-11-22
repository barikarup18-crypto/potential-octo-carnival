package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import coil.compose.AsyncImage
import com.example.R
import com.example.model.BackgroundType
import com.example.model.CartoonScene
import com.example.model.CharacterType
import com.example.model.Expression
import com.example.model.StageCharacter
import com.example.render.CartoonCharacterRenderer
import com.example.render.SpeechBubbleRenderer
import com.example.ui.theme.MangoOrange
import java.io.File
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun CartoonStageCanvas(
    scene: CartoonScene,
    selectedCharacterId: String?,
    speakingCharacterId: String?,
    isInteractive: Boolean,
    modifier: Modifier = Modifier,
    onSelectCharacter: (String?) -> Unit = {},
    onMoveCharacter: (String, Float, Float) -> Unit = { _, _, _ -> },
    onScaleCharacter: (String, Float) -> Unit = { _, _ -> },
    onGestureEnded: () -> Unit = {}
) {
    val context = LocalContext.current
    val bengaliTypeface = remember(context) {
        runCatching { ResourcesCompat.getFont(context, R.font.hind_siliguri) }.getOrNull()
    }

    // Continuous smooth animation clock for blinking, breathing, and talking mouth movement
    var animTimeSec by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val startNanos = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            animTimeSec = (now - startNanos) / 1_000_000_000f
        }
    }

    val currentScene by rememberUpdatedState(scene)
    val currentSelectedId by rememberUpdatedState(selectedCharacterId)
    val sortedChars = remember(scene.characters) { scene.characters.sortedBy { it.zIndex } }

    Box(
        modifier = modifier
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(20.dp))
            .border(
                width = 2.5.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.8f),
                shape = RoundedCornerShape(20.dp)
            )
            .background(Color(0xFFB3E5FC))
            .testTag("cartoon_stage")
    ) {
        // 1. Background layer
        if (scene.backgroundType == BackgroundType.CUSTOM && !scene.customBackgroundPath.isNullOrBlank()) {
            AsyncImage(
                model = File(scene.customBackgroundPath),
                contentDescription = scene.backgroundType.labelBn,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            val resId = scene.backgroundType.drawableRes ?: R.drawable.img_bg_village_road
            Image(
                painter = painterResource(id = resId),
                contentDescription = scene.backgroundType.labelBn,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 2. Interactive Characters + Speech Bubbles Canvas
        val interactionModifier = if (isInteractive) {
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { tapOffset ->
                        val hitChar = findHitCharacter(
                            chars = currentScene.characters,
                            tap = tapOffset,
                            stageW = size.width.toFloat(),
                            stageH = size.height.toFloat()
                        )
                        onSelectCharacter(hitChar?.id)
                    }
                }
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        val targetId = currentSelectedId ?: findHitCharacter(
                            chars = currentScene.characters,
                            tap = centroid,
                            stageW = size.width.toFloat(),
                            stageH = size.height.toFloat()
                        )?.id?.also { onSelectCharacter(it) }

                        if (targetId != null && size.width > 0 && size.height > 0) {
                            if (pan != Offset.Zero) {
                                onMoveCharacter(
                                    targetId,
                                    pan.x / size.width.toFloat(),
                                    pan.y / size.height.toFloat()
                                )
                            }
                            if (abs(zoom - 1f) > 0.005f) {
                                onScaleCharacter(targetId, zoom)
                            }
                            onGestureEnded()
                        }
                    }
                }
        } else {
            Modifier.fillMaxSize()
        }

        Canvas(modifier = interactionModifier) {
            val stageW = size.width
            val stageH = size.height
            if (stageW <= 0f || stageH <= 0f) return@Canvas

            val baseUnitScale = stageH / 470f
            val talkingWave = (0.22f + 0.78f * abs(sin(animTimeSec * 10.5f))).coerceIn(0f, 1f)

            val bubbleSpeaker = if (!isInteractive) {
                scene.activeSpeaker()
            } else {
                val sel = sortedChars.firstOrNull { it.id == selectedCharacterId && it.dialogue.isNotBlank() }
                sel ?: scene.activeSpeaker()?.takeIf { it.dialogue.isNotBlank() }
            }

            var bubbleAnchorX = stageW * 0.5f
            var bubbleAnchorTopY = stageH * 0.45f

            for (char in sortedChars) {
                val cx = char.xFraction * stageW
                val by = char.yFraction * stageH
                val charUnitScale = baseUnitScale * char.scale
                val isSpeakingNow = char.id == speakingCharacterId
                val mouthVal = if (isSpeakingNow) talkingWave else 0f

                // Selection highlight frame in editor mode
                if (isInteractive && char.id == selectedCharacterId) {
                    val boxW = 144f * charUnitScale
                    val boxH = 232f * charUnitScale
                    val topLeft = Offset(cx - boxW / 2f, by - boxH + 8f * charUnitScale)
                    drawRoundRect(
                        color = MangoOrange.copy(alpha = 0.14f),
                        topLeft = topLeft,
                        size = Size(boxW, boxH),
                        cornerRadius = CornerRadius(18f, 18f)
                    )
                    drawRoundRect(
                        color = MangoOrange,
                        topLeft = topLeft,
                        size = Size(boxW, boxH),
                        cornerRadius = CornerRadius(18f, 18f),
                        style = Stroke(
                            width = 4f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                        )
                    )
                }

                drawIntoCanvas { composeCanvas ->
                    CartoonCharacterRenderer.drawCharacter(
                        canvas = composeCanvas.nativeCanvas,
                        type = char.type,
                        expression = char.expression,
                        outfitColorHex = char.outfitColorHex,
                        centerX = cx,
                        bottomY = by,
                        unitScale = charUnitScale,
                        flipped = char.flipped,
                        timeSeconds = animTimeSec,
                        mouthOpen = mouthVal
                    )
                }

                if (char.id == bubbleSpeaker?.id) {
                    bubbleAnchorX = cx
                    bubbleAnchorTopY = by - (220f * charUnitScale)
                }
            }

            // Draw Speech Bubble on top of all characters
            if (bubbleSpeaker != null && bubbleSpeaker.dialogue.isNotBlank()) {
                drawIntoCanvas { composeCanvas ->
                    SpeechBubbleRenderer.drawSpeechBubble(
                        canvas = composeCanvas.nativeCanvas,
                        text = bubbleSpeaker.dialogue,
                        speakerName = bubbleSpeaker.name,
                        anchorX = bubbleAnchorX,
                        anchorTopY = bubbleAnchorTopY,
                        stageWidth = stageW,
                        stageHeight = stageH,
                        typeface = bengaliTypeface,
                        popScale = 1f
                    )
                }
            }
        }

        // Empty stage friendly Bengali helper pill
        if (isInteractive && scene.characters.isEmpty()) {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.90f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.stage_hint_empty),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }
    }
}

private fun findHitCharacter(
    chars: List<StageCharacter>,
    tap: Offset,
    stageW: Float,
    stageH: Float
): StageCharacter? {
    val baseUnitScale = stageH / 470f
    // Check top-most (highest zIndex) character first
    return chars.sortedByDescending { it.zIndex }.firstOrNull { char ->
        val cx = char.xFraction * stageW
        val by = char.yFraction * stageH
        val unitScale = baseUnitScale * char.scale
        val halfW = (78f * unitScale).coerceAtLeast(42f)
        val height = (232f * unitScale).coerceAtLeast(80f)
        tap.x in (cx - halfW)..(cx + halfW) && tap.y in (by - height)..(by + 16f)
    }
}

/**
 * Miniature crisp 2D cartoon preview for Character Library cards & Creator preview.
 */
@Composable
fun CharacterPreviewAvatar(
    type: CharacterType,
    expression: Expression,
    outfitColorHex: Long,
    modifier: Modifier = Modifier,
    animateTalking: Boolean = false
) {
    var animSec by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(animateTalking) {
        if (!animateTalking) {
            animSec = 0.5f
            return@LaunchedEffect
        }
        val start = withFrameNanos { it }
        while (true) {
            val now = withFrameNanos { it }
            animSec = (now - start) / 1_000_000_000f
        }
    }

    Canvas(modifier = modifier) {
        val unitScale = (size.height / 245f).coerceAtLeast(0.1f)
        val mouth = if (animateTalking || expression == Expression.TALKING) {
            (0.25f + 0.65f * abs(sin(animSec * 9f)))
        } else {
            0f
        }
        drawIntoCanvas { c ->
            CartoonCharacterRenderer.drawCharacter(
                canvas = c.nativeCanvas,
                type = type,
                expression = expression,
                outfitColorHex = outfitColorHex,
                centerX = size.width / 2f,
                bottomY = size.height * 0.94f,
                unitScale = unitScale,
                flipped = false,
                timeSeconds = animSec,
                mouthOpen = mouth
            )
        }
    }
}
