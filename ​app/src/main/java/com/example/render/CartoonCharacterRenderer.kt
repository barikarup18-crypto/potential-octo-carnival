package com.example.render

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.example.model.CharacterType
import com.example.model.Expression
import kotlin.math.PI
import kotlin.math.sin

/**
 * Shared 2D cel-shaded vector cartoon renderer drawn directly on android.graphics.Canvas.
 * Used identically by both the interactive Jetpack Compose stage and the 720p MP4 video exporter.
 *
 * Normalized local coordinate space:
 *  - Origin (0, 0) is bottom-center (feet / perch point) of the character.
 *  - Nominal height is 220f units (from y = -220f at top of head to y = 0f at feet).
 *  - Nominal width is ~140f units (-70f..+70f).
 */
object CartoonCharacterRenderer {

    private val inkColor = Color.parseColor("#1E1B2E")

    private fun fillPaint(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        this.color = color
    }

    private fun strokePaint(width: Float = 4.2f, color: Int = inkColor) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = width
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            this.color = color
        }

    fun drawCharacter(
        canvas: Canvas,
        type: CharacterType,
        expression: Expression,
        outfitColorHex: Long,
        centerX: Float,
        bottomY: Float,
        unitScale: Float,
        flipped: Boolean,
        timeSeconds: Float,
        mouthOpen: Float // 0f..1f
    ) {
        val outfitColor = outfitColorHex.toInt()
        val bobY = sin(timeSeconds * 3.2f) * 2.5f
        val blinkCycle = timeSeconds % 3.8f
        val isBlinking = blinkCycle > 3.62f && expression != Expression.ANGRY
        val effectiveMouthOpen = when {
            mouthOpen > 0.05f -> mouthOpen.coerceIn(0f, 1f)
            expression == Expression.TALKING -> (0.28f + 0.65f * ((sin(timeSeconds * 13f) + 1f) * 0.5f))
            else -> 0f
        }

        canvas.save()
        canvas.translate(centerX, bottomY)
        canvas.scale(if (flipped) -unitScale else unitScale, unitScale)

        // Soft ground shadow
        val shadowPaint = fillPaint(Color.argb(46, 20, 18, 35))
        canvas.drawOval(RectF(-44f, -8f, 44f, 9f), shadowPaint)

        // Subtle breathing vertical motion
        canvas.translate(0f, bobY)

        when (type) {
            CharacterType.BOY -> drawBoy(canvas, expression, outfitColor, isBlinking, effectiveMouthOpen, timeSeconds)
            CharacterType.GIRL -> drawGirl(canvas, expression, outfitColor, isBlinking, effectiveMouthOpen, timeSeconds)
            CharacterType.GRANDPA -> drawGrandpa(canvas, expression, outfitColor, isBlinking, effectiveMouthOpen, timeSeconds)
            CharacterType.TIGER -> drawTiger(canvas, expression, outfitColor, isBlinking, effectiveMouthOpen, timeSeconds)
            CharacterType.BIRD -> drawBird(canvas, expression, outfitColor, isBlinking, effectiveMouthOpen, timeSeconds)
            CharacterType.CAT -> drawCat(canvas, expression, outfitColor, isBlinking, effectiveMouthOpen, timeSeconds)
        }

        canvas.restore()
    }

    private fun drawBoy(
        canvas: Canvas,
        expression: Expression,
        outfitColor: Int,
        isBlinking: Boolean,
        mouthOpen: Float,
        timeSeconds: Float
    ) {
        val skin = Color.parseColor("#F5C396")
        val skinShade = Color.parseColor("#E6A978")
        val hair = Color.parseColor("#241F2B")
        val shorts = Color.parseColor("#2D3B55")
        val shoe = Color.parseColor("#E53935")
        val stroke = strokePaint(3.8f)

        // Legs & shoes
        val legPaint = fillPaint(skin)
        canvas.drawRoundRect(RectF(-24f, -46f, -9f, -10f), 7f, 7f, legPaint)
        canvas.drawRoundRect(RectF(-24f, -46f, -9f, -10f), 7f, 7f, stroke)
        canvas.drawRoundRect(RectF(9f, -46f, 24f, -10f), 7f, 7f, legPaint)
        canvas.drawRoundRect(RectF(9f, -46f, 24f, -10f), 7f, 7f, stroke)

        val shoePaint = fillPaint(shoe)
        canvas.drawRoundRect(RectF(-30f, -18f, -5f, -2f), 8f, 8f, shoePaint)
        canvas.drawRoundRect(RectF(-30f, -18f, -5f, -2f), 8f, 8f, stroke)
        canvas.drawRoundRect(RectF(5f, -18f, 30f, -2f), 8f, 8f, shoePaint)
        canvas.drawRoundRect(RectF(5f, -18f, 30f, -2f), 8f, 8f, stroke)

        // Shorts
        val shortsPaint = fillPaint(shorts)
        canvas.drawRoundRect(RectF(-30f, -66f, 30f, -34f), 10f, 10f, shortsPaint)
        canvas.drawRoundRect(RectF(-30f, -66f, 30f, -34f), 10f, 10f, stroke)

        // Arms (gentle wave when talking or happy)
        val armWave = if (mouthOpen > 0.1f) sin(timeSeconds * 8f) * 9f else sin(timeSeconds * 2.4f) * 3f
        canvas.save()
        canvas.rotate(-16f + armWave, -32f, -104f)
        canvas.drawRoundRect(RectF(-46f, -110f, -28f, -62f), 10f, 10f, fillPaint(skin))
        canvas.drawRoundRect(RectF(-46f, -110f, -28f, -62f), 10f, 10f, stroke)
        canvas.drawRoundRect(RectF(-47f, -112f, -26f, -88f), 8f, 8f, fillPaint(outfitColor))
        canvas.drawRoundRect(RectF(-47f, -112f, -26f, -88f), 8f, 8f, stroke)
        canvas.restore()

        canvas.save()
        canvas.rotate(16f - armWave, 32f, -104f)
        canvas.drawRoundRect(RectF(28f, -110f, 46f, -62f), 10f, 10f, fillPaint(skin))
        canvas.drawRoundRect(RectF(28f, -110f, 46f, -62f), 10f, 10f, stroke)
        canvas.drawRoundRect(RectF(26f, -112f, 47f, -88f), 8f, 8f, fillPaint(outfitColor))
        canvas.drawRoundRect(RectF(26f, -112f, 47f, -88f), 8f, 8f, stroke)
        canvas.restore()

        // Torso / T-Shirt
        val shirtRect = RectF(-34f, -118f, 34f, -58f)
        canvas.drawRoundRect(shirtRect, 18f, 18f, fillPaint(outfitColor))
        // White pocket stripe detail
        canvas.drawRoundRect(RectF(-30f, -92f, 30f, -80f), 4f, 4f, fillPaint(Color.argb(65, 255, 255, 255)))
        canvas.drawRoundRect(shirtRect, 18f, 18f, stroke)

        // Neck
        canvas.drawRoundRect(RectF(-10f, -130f, 10f, -112f), 5f, 5f, fillPaint(skinShade))
        canvas.drawRoundRect(RectF(-10f, -130f, 10f, -112f), 5f, 5f, stroke)

        // Ears
        canvas.drawCircle(-44f, -162f, 11f, fillPaint(skin))
        canvas.drawCircle(-44f, -162f, 11f, stroke)
        canvas.drawCircle(44f, -162f, 11f, fillPaint(skin))
        canvas.drawCircle(44f, -162f, 11f, stroke)

        // Head
        canvas.drawCircle(0f, -164f, 45f, fillPaint(skin))
        canvas.drawCircle(0f, -164f, 45f, stroke)

        // Boy messy cartoon hair
        val hairPath = Path().apply {
            moveTo(-45f, -168f)
            quadTo(-48f, -214f, -10f, -215f)
            quadTo(2f, -225f, 16f, -213f)
            quadTo(48f, -212f, 45f, -168f)
            quadTo(30f, -186f, 12f, -184f)
            quadTo(0f, -196f, -14f, -182f)
            quadTo(-32f, -188f, -45f, -168f)
            close()
        }
        canvas.drawPath(hairPath, fillPaint(hair))
        canvas.drawPath(hairPath, stroke)

        // Expressive Face
        drawHumanFace(canvas, 0f, -160f, expression, isBlinking, mouthOpen, hasMustache = false, hasGlasses = false)
    }

    private fun drawGirl(
        canvas: Canvas,
        expression: Expression,
        outfitColor: Int,
        isBlinking: Boolean,
        mouthOpen: Float,
        timeSeconds: Float
    ) {
        val skin = Color.parseColor("#F7C8A0")
        val skinShade = Color.parseColor("#E6B083")
        val hair = Color.parseColor("#231D2B")
        val ribbonColor = Color.parseColor("#FFC107")
        val stroke = strokePaint(3.8f)

        // Twin ponytails behind head
        val sway = sin(timeSeconds * 3f) * 3f
        val hairPaint = fillPaint(hair)
        canvas.drawOval(RectF(-68f + sway, -186f, -34f + sway, -132f), hairPaint)
        canvas.drawOval(RectF(-68f + sway, -186f, -34f + sway, -132f), stroke)
        canvas.drawOval(RectF(34f - sway, -186f, 68f - sway, -132f), hairPaint)
        canvas.drawOval(RectF(34f - sway, -186f, 68f - sway, -132f), stroke)

        // Ribbon bows
        val ribbonPaint = fillPaint(ribbonColor)
        canvas.drawCircle(-46f, -178f, 9f, ribbonPaint)
        canvas.drawCircle(-46f, -178f, 9f, stroke)
        canvas.drawCircle(46f, -178f, 9f, ribbonPaint)
        canvas.drawCircle(46f, -178f, 9f, stroke)

        // Legs & shoes
        canvas.drawRoundRect(RectF(-20f, -44f, -7f, -10f), 6f, 6f, fillPaint(skin))
        canvas.drawRoundRect(RectF(-20f, -44f, -7f, -10f), 6f, 6f, stroke)
        canvas.drawRoundRect(RectF(7f, -44f, 20f, -10f), 6f, 6f, fillPaint(skin))
        canvas.drawRoundRect(RectF(7f, -44f, 20f, -10f), 6f, 6f, stroke)

        canvas.drawRoundRect(RectF(-26f, -17f, -4f, -2f), 8f, 8f, fillPaint(outfitColor))
        canvas.drawRoundRect(RectF(-26f, -17f, -4f, -2f), 8f, 8f, stroke)
        canvas.drawRoundRect(RectF(4f, -17f, 26f, -2f), 8f, 8f, fillPaint(outfitColor))
        canvas.drawRoundRect(RectF(4f, -17f, 26f, -2f), 8f, 8f, stroke)

        // Arms
        val armWave = if (mouthOpen > 0.1f) sin(timeSeconds * 8.5f) * 10f else sin(timeSeconds * 2.6f) * 3f
        canvas.save()
        canvas.rotate(-20f + armWave, -28f, -104f)
        canvas.drawRoundRect(RectF(-44f, -108f, -28f, -62f), 9f, 9f, fillPaint(skin))
        canvas.drawRoundRect(RectF(-44f, -108f, -28f, -62f), 9f, 9f, stroke)
        canvas.drawCircle(-35f, -104f, 12f, fillPaint(outfitColor))
        canvas.drawCircle(-35f, -104f, 12f, stroke)
        canvas.restore()

        canvas.save()
        canvas.rotate(20f - armWave, 28f, -104f)
        canvas.drawRoundRect(RectF(28f, -108f, 44f, -62f), 9f, 9f, fillPaint(skin))
        canvas.drawRoundRect(RectF(28f, -108f, 44f, -62f), 9f, 9f, stroke)
        canvas.drawCircle(35f, -104f, 12f, fillPaint(outfitColor))
        canvas.drawCircle(35f, -104f, 12f, stroke)
        canvas.restore()

        // Cute flared frock/dress
        val dressPath = Path().apply {
            moveTo(-26f, -118f)
            lineTo(26f, -118f)
            lineTo(44f, -40f)
            quadTo(0f, -30f, -44f, -40f)
            close()
        }
        canvas.drawPath(dressPath, fillPaint(outfitColor))
        // Decorative golden hem band
        val bandPath = Path().apply {
            moveTo(-39f, -54f)
            lineTo(39f, -54f)
            lineTo(43f, -42f)
            quadTo(0f, -33f, -43f, -42f)
            close()
        }
        canvas.drawPath(bandPath, fillPaint(Color.argb(120, 255, 224, 130)))
        canvas.drawPath(dressPath, stroke)

        // Neck
        canvas.drawRoundRect(RectF(-9f, -129f, 9f, -113f), 5f, 5f, fillPaint(skinShade))
        canvas.drawRoundRect(RectF(-9f, -129f, 9f, -113f), 5f, 5f, stroke)

        // Head
        canvas.drawCircle(0f, -163f, 44f, fillPaint(skin))
        canvas.drawCircle(0f, -163f, 44f, stroke)

        // Smooth parted hair bangs
        val bangsPath = Path().apply {
            moveTo(-44f, -164f)
            quadTo(-44f, -212f, 0f, -212f)
            quadTo(44f, -212f, 44f, -164f)
            quadTo(22f, -184f, 4f, -186f)
            quadTo(-16f, -184f, -44f, -164f)
            close()
        }
        canvas.drawPath(bangsPath, hairPaint)
        canvas.drawPath(bangsPath, stroke)

        // Cute tiny red bindi
        canvas.drawCircle(0f, -173f, 3.2f, fillPaint(Color.parseColor("#D50000")))

        drawHumanFace(canvas, 0f, -158f, expression, isBlinking, mouthOpen, hasMustache = false, hasGlasses = false)
    }

    private fun drawGrandpa(
        canvas: Canvas,
        expression: Expression,
        outfitColor: Int,
        isBlinking: Boolean,
        mouthOpen: Float,
        timeSeconds: Float
    ) {
        val skin = Color.parseColor("#EBB88F")
        val whiteHair = Color.parseColor("#ECEFF4")
        val dhoti = Color.parseColor("#FAF6EE")
        val shawl = Color.parseColor("#C62828")
        val stroke = strokePaint(3.8f)

        // Dhoti / Pajama legs
        canvas.drawRoundRect(RectF(-28f, -58f, 28f, -12f), 10f, 10f, fillPaint(dhoti))
        canvas.drawRoundRect(RectF(-28f, -58f, 28f, -12f), 10f, 10f, stroke)
        // Sandals
        canvas.drawRoundRect(RectF(-32f, -16f, -4f, -2f), 7f, 7f, fillPaint(Color.parseColor("#6D4C41")))
        canvas.drawRoundRect(RectF(-32f, -16f, -4f, -2f), 7f, 7f, stroke)
        canvas.drawRoundRect(RectF(4f, -16f, 32f, -2f), 7f, 7f, fillPaint(Color.parseColor("#6D4C41")))
        canvas.drawRoundRect(RectF(4f, -16f, 32f, -2f), 7f, 7f, stroke)

        // Arms
        val armWave = if (mouthOpen > 0.1f) sin(timeSeconds * 6.5f) * 7f else sin(timeSeconds * 2f) * 2f
        canvas.save()
        canvas.rotate(-14f + armWave, -36f, -106f)
        canvas.drawRoundRect(RectF(-50f, -112f, -30f, -60f), 10f, 10f, fillPaint(outfitColor))
        canvas.drawRoundRect(RectF(-50f, -112f, -30f, -60f), 10f, 10f, stroke)
        canvas.drawCircle(-40f, -58f, 9f, fillPaint(skin))
        canvas.drawCircle(-40f, -58f, 9f, stroke)
        canvas.restore()

        canvas.save()
        canvas.rotate(14f - armWave, 36f, -106f)
        canvas.drawRoundRect(RectF(30f, -112f, 50f, -60f), 10f, 10f, fillPaint(outfitColor))
        canvas.drawRoundRect(RectF(30f, -112f, 50f, -60f), 10f, 10f, stroke)
        canvas.drawCircle(40f, -58f, 9f, fillPaint(skin))
        canvas.drawCircle(40f, -58f, 9f, stroke)
        canvas.restore()

        // Bengali Panjabi / Kurta
        val kurtaRect = RectF(-36f, -120f, 36f, -44f)
        canvas.drawRoundRect(kurtaRect, 16f, 16f, fillPaint(outfitColor))
        canvas.drawRoundRect(kurtaRect, 16f, 16f, stroke)

        // Placket & buttons
        canvas.drawLine(0f, -118f, 0f, -80f, strokePaint(3f))
        canvas.drawCircle(0f, -106f, 2.5f, fillPaint(inkColor))
        canvas.drawCircle(0f, -94f, 2.5f, fillPaint(inkColor))

        // Traditional red shoulder shawl drape
        val shawlPath = Path().apply {
            moveTo(-36f, -118f)
            lineTo(-14f, -118f)
            lineTo(-6f, -56f)
            lineTo(-30f, -56f)
            close()
        }
        canvas.drawPath(shawlPath, fillPaint(shawl))
        canvas.drawPath(shawlPath, stroke)

        // Ears
        canvas.drawCircle(-45f, -164f, 11f, fillPaint(skin))
        canvas.drawCircle(-45f, -164f, 11f, stroke)
        canvas.drawCircle(45f, -164f, 11f, fillPaint(skin))
        canvas.drawCircle(45f, -164f, 11f, stroke)

        // Head
        canvas.drawCircle(0f, -166f, 45f, fillPaint(skin))
        canvas.drawCircle(0f, -166f, 45f, stroke)

        // Fluffy white side hair tufts (cute grandpa look)
        val whitePaint = fillPaint(whiteHair)
        canvas.drawOval(RectF(-50f, -196f, -26f, -166f), whitePaint)
        canvas.drawOval(RectF(-50f, -196f, -26f, -166f), stroke)
        canvas.drawOval(RectF(26f, -196f, 50f, -166f), whitePaint)
        canvas.drawOval(RectF(26f, -196f, 50f, -166f), stroke)
        // Little top curl
        val topHair = Path().apply {
            moveTo(-16f, -208f)
            quadTo(0f, -222f, 16f, -208f)
            quadTo(0f, -200f, -16f, -208f)
            close()
        }
        canvas.drawPath(topHair, whitePaint)
        canvas.drawPath(topHair, stroke)

        drawHumanFace(canvas, 0f, -162f, expression, isBlinking, mouthOpen, hasMustache = true, hasGlasses = true)
    }

    private fun drawTiger(
        canvas: Canvas,
        expression: Expression,
        outfitColor: Int,
        isBlinking: Boolean,
        mouthOpen: Float,
        timeSeconds: Float
    ) {
        val orange = Color.parseColor("#FF9800")
        val cream = Color.parseColor("#FFF3D6")
        val stripe = Color.parseColor("#2A1E17")
        val stroke = strokePaint(3.8f)

        // Swishing tail behind body
        val tailSwing = sin(timeSeconds * 4.5f) * 10f
        val tailPath = Path().apply {
            moveTo(28f, -44f)
            quadTo(66f, -52f + tailSwing, 58f, -92f + tailSwing)
        }
        canvas.drawPath(tailPath, strokePaint(14f, inkColor))
        canvas.drawPath(tailPath, strokePaint(8.5f, orange))

        // Feet / Paws
        canvas.drawOval(RectF(-34f, -24f, -6f, -2f), fillPaint(orange))
        canvas.drawOval(RectF(-34f, -24f, -6f, -2f), stroke)
        canvas.drawOval(RectF(6f, -24f, 34f, -2f), fillPaint(orange))
        canvas.drawOval(RectF(6f, -24f, 34f, -2f), stroke)

        // Plump Tiger Body
        canvas.drawOval(RectF(-40f, -112f, 40f, -18f), fillPaint(orange))
        canvas.drawOval(RectF(-24f, -94f, 24f, -28f), fillPaint(cream))
        canvas.drawOval(RectF(-40f, -112f, 40f, -18f), stroke)

        // Cute customizable scarf / bandana
        val scarfPath = Path().apply {
            moveTo(-34f, -110f)
            quadTo(0f, -92f, 34f, -110f)
            lineTo(24f, -84f)
            quadTo(0f, -74f, -24f, -84f)
            close()
        }
        canvas.drawPath(scarfPath, fillPaint(outfitColor))
        canvas.drawPath(scarfPath, stroke)

        // Arms / Paws
        val pawWave = if (mouthOpen > 0.1f) sin(timeSeconds * 8f) * 10f else 0f
        canvas.save()
        canvas.rotate(-18f + pawWave, -32f, -92f)
        canvas.drawOval(RectF(-50f, -98f, -26f, -58f), fillPaint(orange))
        canvas.drawOval(RectF(-50f, -98f, -26f, -58f), stroke)
        canvas.restore()

        canvas.save()
        canvas.rotate(18f - pawWave, 32f, -92f)
        canvas.drawOval(RectF(26f, -98f, 50f, -58f), fillPaint(orange))
        canvas.drawOval(RectF(26f, -98f, 50f, -58f), stroke)
        canvas.restore()

        // Round Tiger Ears
        canvas.drawCircle(-36f, -196f, 16f, fillPaint(orange))
        canvas.drawCircle(-36f, -196f, 16f, stroke)
        canvas.drawCircle(-36f, -196f, 8f, fillPaint(cream))

        canvas.drawCircle(36f, -196f, 16f, fillPaint(orange))
        canvas.drawCircle(36f, -196f, 16f, stroke)
        canvas.drawCircle(36f, -196f, 8f, fillPaint(cream))

        // Tiger Head
        canvas.drawOval(RectF(-49f, -202f, 49f, -114f), fillPaint(orange))
        canvas.drawOval(RectF(-49f, -202f, 49f, -114f), stroke)

        // Forehead & cheek stripes
        val stripePaint = fillPaint(stripe)
        canvas.drawRoundRect(RectF(-5f, -200f, 5f, -181f), 4f, 4f, stripePaint)
        canvas.drawRoundRect(RectF(-20f, -197f, -11f, -183f), 4f, 4f, stripePaint)
        canvas.drawRoundRect(RectF(11f, -197f, 20f, -183f), 4f, 4f, stripePaint)
        // Cheek stripes
        canvas.drawLine(-46f, -152f, -33f, -150f, strokePaint(4.5f, stripe))
        canvas.drawLine(-45f, -142f, -32f, -142f, strokePaint(4.5f, stripe))
        canvas.drawLine(46f, -152f, 33f, -150f, strokePaint(4.5f, stripe))
        canvas.drawLine(45f, -142f, 32f, -142f, strokePaint(4.5f, stripe))

        // Cream muzzle
        canvas.drawOval(RectF(-24f, -152f, 24f, -120f), fillPaint(cream))

        // Cute triangle nose
        val nosePath = Path().apply {
            moveTo(-9f, -150f)
            lineTo(9f, -150f)
            lineTo(0f, -140f)
            close()
        }
        canvas.drawPath(nosePath, fillPaint(Color.parseColor("#E91E63")))
        canvas.drawPath(nosePath, strokePaint(2.8f))

        drawAnimalEyesAndMouth(canvas, 0f, -162f, expression, isBlinking, mouthOpen, mouthY = -132f)
    }

    private fun drawBird(
        canvas: Canvas,
        expression: Expression,
        outfitColor: Int,
        isBlinking: Boolean,
        mouthOpen: Float,
        timeSeconds: Float
    ) {
        val bodyYellow = Color.parseColor("#FFD54F")
        val bellyCream = Color.parseColor("#FFFDE7")
        val beakOrange = Color.parseColor("#FF6D00")
        val stroke = strokePaint(3.8f)

        // Cute little legs
        val legStroke = strokePaint(4.5f, beakOrange)
        canvas.drawLine(-14f, -36f, -14f, -6f, legStroke)
        canvas.drawLine(-14f, -6f, -24f, -2f, legStroke)
        canvas.drawLine(-14f, -6f, -6f, -2f, legStroke)
        canvas.drawLine(14f, -36f, 14f, -6f, legStroke)
        canvas.drawLine(14f, -6f, 6f, -2f, legStroke)
        canvas.drawLine(14f, -6f, 24f, -2f, legStroke)

        // Head crest feathers
        val crestPath = Path().apply {
            moveTo(-6f, -166f)
            quadTo(-16f, -198f, 2f, -184f)
            quadTo(12f, -206f, 16f, -172f)
            close()
        }
        canvas.drawPath(crestPath, fillPaint(outfitColor))
        canvas.drawPath(crestPath, stroke)

        // Fluttering wings
        val wingFlap = if (mouthOpen > 0.1f) sin(timeSeconds * 14f) * 20f else sin(timeSeconds * 4f) * 6f
        canvas.save()
        canvas.rotate(-22f + wingFlap, -44f, -104f)
        canvas.drawOval(RectF(-72f, -120f, -34f, -70f), fillPaint(outfitColor))
        canvas.drawOval(RectF(-72f, -120f, -34f, -70f), stroke)
        canvas.restore()

        canvas.save()
        canvas.rotate(22f - wingFlap, 44f, -104f)
        canvas.drawOval(RectF(34f, -120f, 72f, -70f), fillPaint(outfitColor))
        canvas.drawOval(RectF(34f, -120f, 72f, -70f), stroke)
        canvas.restore()

        // Plump bird body
        canvas.drawOval(RectF(-50f, -168f, 50f, -34f), fillPaint(bodyYellow))
        canvas.drawOval(RectF(-32f, -110f, 32f, -44f), fillPaint(bellyCream))
        canvas.drawOval(RectF(-50f, -168f, 50f, -34f), stroke)

        // Rosy cheeks
        val cheekPaint = fillPaint(Color.argb(120, 255, 110, 128))
        canvas.drawCircle(-28f, -114f, 8f, cheekPaint)
        canvas.drawCircle(28f, -114f, 8f, cheekPaint)

        // Eyes & Brows
        drawEyes(canvas, -19f, 19f, -126f, expression, isBlinking)

        // Animated opening/closing beak!
        val beakSpread = mouthOpen * 12f
        val upperBeak = Path().apply {
            moveTo(-14f, -112f)
            lineTo(14f, -112f)
            lineTo(0f, -100f - beakSpread * 0.25f)
            close()
        }
        val lowerBeak = Path().apply {
            moveTo(-11f, -110f + beakSpread * 0.2f)
            lineTo(11f, -110f + beakSpread * 0.2f)
            lineTo(0f, -98f + beakSpread)
            close()
        }
        if (mouthOpen > 0.08f) {
            canvas.drawOval(RectF(-10f, -112f, 10f, -100f + beakSpread), fillPaint(Color.parseColor("#C62828")))
        }
        canvas.drawPath(lowerBeak, fillPaint(Color.parseColor("#E65100")))
        canvas.drawPath(lowerBeak, strokePaint(3f))
        canvas.drawPath(upperBeak, fillPaint(beakOrange))
        canvas.drawPath(upperBeak, strokePaint(3f))
    }

    private fun drawCat(
        canvas: Canvas,
        expression: Expression,
        outfitColor: Int,
        isBlinking: Boolean,
        mouthOpen: Float,
        timeSeconds: Float
    ) {
        val fur = Color.parseColor("#F5F0FA")
        val furEarInner = Color.parseColor("#F8BBD0")
        val bellGold = Color.parseColor("#FFC107")
        val stroke = strokePaint(3.8f)

        // Curved swishing cat tail
        val tailSway = sin(timeSeconds * 4f) * 12f
        val tailPath = Path().apply {
            moveTo(26f, -38f)
            quadTo(66f, -46f + tailSway, 56f, -96f + tailSway)
        }
        canvas.drawPath(tailPath, strokePaint(13f, inkColor))
        canvas.drawPath(tailPath, strokePaint(7.5f, fur))

        // Feet
        canvas.drawOval(RectF(-30f, -22f, -6f, -2f), fillPaint(fur))
        canvas.drawOval(RectF(-30f, -22f, -6f, -2f), stroke)
        canvas.drawOval(RectF(6f, -22f, 30f, -2f), fillPaint(fur))
        canvas.drawOval(RectF(6f, -22f, 30f, -2f), stroke)

        // Body wearing customizable cute sweater
        canvas.drawOval(RectF(-36f, -110f, 36f, -18f), fillPaint(outfitColor))
        canvas.drawOval(RectF(-36f, -110f, 36f, -18f), stroke)

        // Collar & Bell
        canvas.drawRoundRect(RectF(-26f, -116f, 26f, -102f), 6f, 6f, fillPaint(Color.parseColor("#E53935")))
        canvas.drawRoundRect(RectF(-26f, -116f, 26f, -102f), 6f, 6f, stroke)
        canvas.drawCircle(0f, -100f, 7f, fillPaint(bellGold))
        canvas.drawCircle(0f, -100f, 7f, strokePaint(2.6f))

        // Paws
        val pawWave = if (mouthOpen > 0.1f) sin(timeSeconds * 9f) * 9f else 0f
        canvas.save()
        canvas.rotate(-16f + pawWave, -28f, -92f)
        canvas.drawOval(RectF(-46f, -96f, -24f, -60f), fillPaint(fur))
        canvas.drawOval(RectF(-46f, -96f, -24f, -60f), stroke)
        canvas.restore()

        canvas.save()
        canvas.rotate(16f - pawWave, 28f, -92f)
        canvas.drawOval(RectF(24f, -96f, 46f, -60f), fillPaint(fur))
        canvas.drawOval(RectF(24f, -96f, 46f, -60f), stroke)
        canvas.restore()

        // Pointy cat ears
        val leftEar = Path().apply {
            moveTo(-42f, -172f)
            lineTo(-38f, -214f)
            lineTo(-10f, -196f)
            close()
        }
        val rightEar = Path().apply {
            moveTo(42f, -172f)
            lineTo(38f, -214f)
            lineTo(10f, -196f)
            close()
        }
        canvas.drawPath(leftEar, fillPaint(fur))
        canvas.drawPath(leftEar, stroke)
        canvas.drawPath(rightEar, fillPaint(fur))
        canvas.drawPath(rightEar, stroke)

        // Pink inner ears
        val innerL = Path().apply {
            moveTo(-36f, -178f)
            lineTo(-34f, -203f)
            lineTo(-17f, -193f)
            close()
        }
        val innerR = Path().apply {
            moveTo(36f, -178f)
            lineTo(34f, -203f)
            lineTo(17f, -193f)
            close()
        }
        canvas.drawPath(innerL, fillPaint(furEarInner))
        canvas.drawPath(innerR, fillPaint(furEarInner))

        // Cat Head
        canvas.drawOval(RectF(-46f, -198f, 46f, -112f), fillPaint(fur))
        canvas.drawOval(RectF(-46f, -198f, 46f, -112f), stroke)

        // Whiskers
        val whiskerPaint = strokePaint(2.6f, inkColor)
        canvas.drawLine(-54f, -148f, -32f, -146f, whiskerPaint)
        canvas.drawLine(-52f, -138f, -32f, -140f, whiskerPaint)
        canvas.drawLine(54f, -148f, 32f, -146f, whiskerPaint)
        canvas.drawLine(52f, -138f, 32f, -140f, whiskerPaint)

        // Cute pink nose
        canvas.drawCircle(0f, -145f, 5.5f, fillPaint(Color.parseColor("#FF6090")))

        drawAnimalEyesAndMouth(canvas, 0f, -160f, expression, isBlinking, mouthOpen, mouthY = -132f)
    }

    private fun drawHumanFace(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        expression: Expression,
        isBlinking: Boolean,
        mouthOpen: Float,
        hasMustache: Boolean,
        hasGlasses: Boolean
    ) {
        // Rosy cheeks
        val cheekColor = if (expression == Expression.ANGRY) {
            Color.argb(120, 239, 83, 80)
        } else {
            Color.argb(95, 255, 112, 132)
        }
        canvas.drawCircle(cx - 26f, cy + 8f, 8f, fillPaint(cheekColor))
        canvas.drawCircle(cx + 26f, cy + 8f, 8f, fillPaint(cheekColor))

        // Eyes & eyebrows
        drawEyes(canvas, cx - 16f, cx + 16f, cy - 6f, expression, isBlinking)

        // Glasses for Grandpa
        if (hasGlasses) {
            val glassFill = fillPaint(Color.argb(45, 225, 245, 254))
            val glassStroke = strokePaint(3.2f, Color.parseColor("#455A64"))
            canvas.drawCircle(cx - 16f, cy - 6f, 13f, glassFill)
            canvas.drawCircle(cx - 16f, cy - 6f, 13f, glassStroke)
            canvas.drawCircle(cx + 16f, cy - 6f, 13f, glassFill)
            canvas.drawCircle(cx + 16f, cy - 6f, 13f, glassStroke)
            canvas.drawLine(cx - 3f, cy - 6f, cx + 3f, cy - 6f, glassStroke)
        }

        // Cute button nose
        canvas.drawArc(
            RectF(cx - 4f, cy - 1f, cx + 4f, cy + 7f),
            15f,
            150f,
            false,
            strokePaint(2.8f, Color.parseColor("#B57B52"))
        )

        // Mouth (animated when speaking!)
        drawMouth(canvas, cx, cy + 20f, expression, mouthOpen)

        // Fluffy white mustache for Grandpa (sits right above mouth)
        if (hasMustache) {
            val mustachePaint = fillPaint(Color.parseColor("#F5F7FA"))
            val mustacheStroke = strokePaint(2.8f)
            val mY = cy + 12f - mouthOpen * 2f
            canvas.drawOval(RectF(cx - 24f, mY - 5f, cx + 2f, mY + 8f), mustachePaint)
            canvas.drawOval(RectF(cx - 24f, mY - 5f, cx + 2f, mY + 8f), mustacheStroke)
            canvas.drawOval(RectF(cx - 2f, mY - 5f, cx + 24f, mY + 8f), mustachePaint)
            canvas.drawOval(RectF(cx - 2f, mY - 5f, cx + 24f, mY + 8f), mustacheStroke)
        }

        // Sad tear drop
        if (expression == Expression.SAD) {
            val tearPaint = fillPaint(Color.parseColor("#4FC3F7"))
            canvas.drawOval(RectF(cx - 24f, cy + 4f, cx - 17f, cy + 14f), tearPaint)
            canvas.drawOval(RectF(cx + 17f, cy + 4f, cx + 24f, cy + 14f), tearPaint)
        }
    }

    private fun drawAnimalEyesAndMouth(
        canvas: Canvas,
        cx: Float,
        eyeY: Float,
        expression: Expression,
        isBlinking: Boolean,
        mouthOpen: Float,
        mouthY: Float
    ) {
        val cheekPaint = fillPaint(Color.argb(95, 255, 112, 132))
        canvas.drawCircle(cx - 27f, eyeY + 14f, 7.5f, cheekPaint)
        canvas.drawCircle(cx + 27f, eyeY + 14f, 7.5f, cheekPaint)

        drawEyes(canvas, cx - 17f, cx + 17f, eyeY, expression, isBlinking)
        drawMouth(canvas, cx, mouthY, expression, mouthOpen)

        if (expression == Expression.SAD) {
            val tearPaint = fillPaint(Color.parseColor("#4FC3F7"))
            canvas.drawOval(RectF(cx - 24f, eyeY + 10f, cx - 17f, eyeY + 20f), tearPaint)
        }
    }

    private fun drawEyes(
        canvas: Canvas,
        leftX: Float,
        rightX: Float,
        y: Float,
        expression: Expression,
        isBlinking: Boolean
    ) {
        val white = fillPaint(Color.WHITE)
        val pupil = fillPaint(inkColor)
        val stroke = strokePaint(3f)

        if (isBlinking) {
            canvas.drawArc(RectF(leftX - 8f, y - 4f, leftX + 8f, y + 6f), 0f, 180f, false, stroke)
            canvas.drawArc(RectF(rightX - 8f, y - 4f, rightX + 8f, y + 6f), 0f, 180f, false, stroke)
        } else {
            // Eye whites
            canvas.drawCircle(leftX, y, 8.8f, white)
            canvas.drawCircle(leftX, y, 8.8f, stroke)
            canvas.drawCircle(rightX, y, 8.8f, white)
            canvas.drawCircle(rightX, y, 8.8f, stroke)

            // Pupils
            canvas.drawCircle(leftX + 1.5f, y + 0.5f, 4.8f, pupil)
            canvas.drawCircle(rightX + 1.5f, y + 0.5f, 4.8f, pupil)

            // Glossy catchlights
            canvas.drawCircle(leftX + 3.2f, y - 1.8f, 2f, white)
            canvas.drawCircle(rightX + 3.2f, y - 1.8f, 2f, white)
        }

        // Eyebrows based on expression
        val browPaint = strokePaint(3.6f, inkColor)
        when (expression) {
            Expression.HAPPY, Expression.TALKING -> {
                canvas.drawArc(RectF(leftX - 9f, y - 19f, leftX + 9f, y - 10f), 200f, 140f, false, browPaint)
                canvas.drawArc(RectF(rightX - 9f, y - 19f, rightX + 9f, y - 10f), 200f, 140f, false, browPaint)
            }
            Expression.SAD -> {
                canvas.drawLine(leftX - 9f, y - 11f, leftX + 8f, y - 17f, browPaint)
                canvas.drawLine(rightX - 8f, y - 17f, rightX + 9f, y - 11f, browPaint)
            }
            Expression.ANGRY -> {
                canvas.drawLine(leftX - 9f, y - 18f, leftX + 9f, y - 11f, browPaint)
                canvas.drawLine(rightX - 9f, y - 11f, rightX + 9f, y - 18f, browPaint)
            }
        }
    }

    private fun drawMouth(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        expression: Expression,
        mouthOpen: Float
    ) {
        val stroke = strokePaint(3.4f)
        if (mouthOpen > 0.08f) {
            val openH = 5f + mouthOpen * 13f
            val openW = 9f + mouthOpen * 7f
            val mouthRect = RectF(cx - openW, cy - openH * 0.45f, cx + openW, cy + openH * 0.75f)
            canvas.drawOval(mouthRect, fillPaint(Color.parseColor("#8E1538")))
            // Cute pink tongue inside open mouth
            val tongueRect = RectF(cx - openW * 0.55f, cy + openH * 0.1f, cx + openW * 0.55f, cy + openH * 0.72f)
            canvas.drawOval(tongueRect, fillPaint(Color.parseColor("#FF6F91")))
            // Upper teeth bar for cheerful talking
            if (expression != Expression.SAD) {
                canvas.drawRoundRect(
                    RectF(cx - openW * 0.65f, cy - openH * 0.42f, cx + openW * 0.65f, cy - openH * 0.12f),
                    3f,
                    3f,
                    fillPaint(Color.WHITE)
                )
            }
            canvas.drawOval(mouthRect, stroke)
        } else {
            when (expression) {
                Expression.HAPPY, Expression.TALKING -> {
                    canvas.drawArc(RectF(cx - 13f, cy - 9f, cx + 13f, cy + 9f), 15f, 150f, false, stroke)
                }
                Expression.SAD -> {
                    canvas.drawArc(RectF(cx - 11f, cy - 2f, cx + 11f, cy + 14f), 205f, 130f, false, stroke)
                }
                Expression.ANGRY -> {
                    val angryMouth = Path().apply {
                        moveTo(cx - 12f, cy + 4f)
                        lineTo(cx - 4f, cy - 2f)
                        lineTo(cx + 4f, cy + 3f)
                        lineTo(cx + 12f, cy - 2f)
                    }
                    canvas.drawPath(angryMouth, stroke)
                }
            }
        }
    }
}
