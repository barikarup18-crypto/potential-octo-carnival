package com.example.render

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import kotlin.math.max
import kotlin.math.min

/**
 * Draws a crisp cartoon speech bubble with wrapped Bengali text directly on android.graphics.Canvas.
 * Used identically on the Compose live stage and in the exported 720p MP4 video.
 */
object SpeechBubbleRenderer {

    fun drawSpeechBubble(
        canvas: Canvas,
        text: String,
        speakerName: String,
        anchorX: Float,
        anchorTopY: Float,
        stageWidth: Float,
        stageHeight: Float,
        typeface: Typeface?,
        popScale: Float = 1f
    ) {
        if (text.isBlank()) return

        val scaleFactor = stageWidth / 960f
        val textSizePx = (24f * scaleFactor).coerceIn(15f, 38f)
        val nameSizePx = (16f * scaleFactor).coerceIn(11f, 24f)
        val padH = 18f * scaleFactor
        val padV = 12f * scaleFactor
        val cornerRadius = 20f * scaleFactor
        val tailHeight = 16f * scaleFactor
        val maxBubbleWidth = (stageWidth * 0.44f).coerceAtLeast(180f * scaleFactor)

        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#1E1B2E")
            textSize = textSizePx
            this.typeface = typeface ?: Typeface.DEFAULT_BOLD
        }

        val namePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF6B2B")
            textSize = nameSizePx
            this.typeface = Typeface.create(typeface ?: Typeface.DEFAULT, Typeface.BOLD)
        }

        val innerMaxWidth = (maxBubbleWidth - padH * 2f).toInt().coerceAtLeast(80)
        val layout = StaticLayout.Builder
            .obtain(text, 0, text.length, textPaint, innerMaxWidth)
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setIncludePad(false)
            .setLineSpacing(2f * scaleFactor, 1.08f)
            .build()

        var maxLineW = 0f
        for (i in 0 until layout.lineCount) {
            maxLineW = max(maxLineW, layout.getLineWidth(i))
        }
        val nameWidth = if (speakerName.isNotBlank()) namePaint.measureText(speakerName) else 0f
        val contentWidth = max(maxLineW, nameWidth)
        val bubbleW = (contentWidth + padH * 2f).coerceIn(110f * scaleFactor, maxBubbleWidth)
        val nameBlockH = if (speakerName.isNotBlank()) nameSizePx + 6f * scaleFactor else 0f
        val bubbleH = layout.height + nameBlockH + padV * 2f

        val margin = 14f * scaleFactor
        val bubbleLeft = (anchorX - bubbleW / 2f).coerceIn(margin, max(margin, stageWidth - bubbleW - margin))
        val desiredTop = anchorTopY - bubbleH - tailHeight - 10f * scaleFactor
        val bubbleTop = desiredTop.coerceIn(margin, max(margin, stageHeight * 0.42f))
        val bubbleRight = bubbleLeft + bubbleW
        val bubbleBottom = bubbleTop + bubbleH

        val tailTipX = anchorX.coerceIn(bubbleLeft + cornerRadius, bubbleRight - cornerRadius)
        val tailBaseL = (tailTipX - 11f * scaleFactor).coerceAtLeast(bubbleLeft + cornerRadius * 0.7f)
        val tailBaseR = (tailTipX + 11f * scaleFactor).coerceAtMost(bubbleRight - cornerRadius * 0.7f)
        val tailTipY = min(bubbleBottom + tailHeight, stageHeight - margin)

        canvas.save()
        if (popScale != 1f) {
            canvas.scale(popScale, popScale, tailTipX, bubbleBottom)
        }

        val rect = RectF(bubbleLeft, bubbleTop, bubbleRight, bubbleBottom)

        // Drop shadow
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.argb(44, 30, 27, 46)
        }
        canvas.drawRoundRect(
            RectF(rect.left + 3f * scaleFactor, rect.top + 5f * scaleFactor, rect.right + 3f * scaleFactor, rect.bottom + 5f * scaleFactor),
            cornerRadius,
            cornerRadius,
            shadowPaint
        )

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#FFFDF9")
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = (3.4f * scaleFactor).coerceAtLeast(2.2f)
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            color = Color.parseColor("#1E1B2E")
        }

        // Combined rounded bubble + tail
        val tailPath = Path().apply {
            moveTo(tailBaseL, bubbleBottom - 2f)
            lineTo(tailTipX, tailTipY)
            lineTo(tailBaseR, bubbleBottom - 2f)
            close()
        }

        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, fillPaint)
        canvas.drawPath(tailPath, fillPaint)
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)

        // Clean tail border without crossing the bottom line inside bubble
        val tailOutline = Path().apply {
            moveTo(tailBaseL, bubbleBottom)
            lineTo(tailTipX, tailTipY)
            lineTo(tailBaseR, bubbleBottom)
        }
        canvas.drawPath(tailOutline, borderPaint)
        // Cover seam between bubble bottom and tail interior
        canvas.drawLine(
            tailBaseL + 2f,
            bubbleBottom,
            tailBaseR - 2f,
            bubbleBottom,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#FFFDF9")
                strokeWidth = borderPaint.strokeWidth + 2f
            }
        )

        // Speaker name pill header
        var textStartY = bubbleTop + padV
        if (speakerName.isNotBlank()) {
            val nameX = bubbleLeft + (bubbleW - nameWidth) / 2f
            canvas.drawText(speakerName, nameX, textStartY + nameSizePx * 0.85f, namePaint)
            textStartY += nameBlockH
        }

        // Draw StaticLayout centered horizontally inside bubble
        canvas.save()
        val textLeft = bubbleLeft + (bubbleW - layout.width) / 2f
        canvas.translate(textLeft, textStartY)
        layout.draw(canvas)
        canvas.restore()

        canvas.restore()
    }
}
