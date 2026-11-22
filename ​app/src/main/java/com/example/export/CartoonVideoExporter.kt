package com.example.export

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.model.BackgroundType
import com.example.model.CartoonScene
import com.example.model.toBengaliDigits
import com.example.render.CartoonCharacterRenderer
import com.example.render.SpeechBubbleRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sin

data class VideoExportResult(
    val shareFilePath: String,
    val galleryDisplayPath: String
)

/**
 * Renders all cartoon scenes (backgrounds, animated 2D characters with mouth movement,
 * and Bengali speech bubbles) into an H.264 MP4 video using MediaCodec InputSurface + MediaMuxer,
 * then saves it to the device's Movies gallery via MediaStore.
 */
class CartoonVideoExporter(private val context: Context) {

    suspend fun exportStoryToMp4(
        scenes: List<CartoonScene>,
        onProgress: (Float, String) -> Unit
    ): Result<VideoExportResult> = withContext(Dispatchers.Default) {
        if (scenes.isEmpty()) {
            return@withContext Result.failure(IllegalStateException("কোনো দৃশ্য পাওয়া যায়নি"))
        }

        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val fileName = "CartoonStudio_${System.currentTimeMillis()}.mp4"
        val outputFile = File(exportDir, fileName)

        // Try 1280x720 first, fall back to 960x540 if device encoder prefers lower resolution
        val primaryAttempt = runCatching {
            encodeVideo(scenes, outputFile, width = 1280, height = 720, fps = 24, onProgress = onProgress)
        }
        if (primaryAttempt.isFailure) {
            if (outputFile.exists()) outputFile.delete()
            val fallbackAttempt = runCatching {
                encodeVideo(scenes, outputFile, width = 960, height = 540, fps = 20, onProgress = onProgress)
            }
            if (fallbackAttempt.isFailure) {
                return@withContext Result.failure(
                    fallbackAttempt.exceptionOrNull()
                        ?: IllegalStateException("ভিডিও এনকোড করা যায়নি")
                )
            }
        }

        onProgress(0.95f, "গ্যালারিতে সেভ করা হচ্ছে...")
        val galleryPath = saveToMediaStoreMovies(outputFile, fileName)
        onProgress(1f, "সম্পন্ন হয়েছে!")
        Result.success(
            VideoExportResult(
                shareFilePath = outputFile.absolutePath,
                galleryDisplayPath = galleryPath
            )
        )
    }

    private fun encodeVideo(
        scenes: List<CartoonScene>,
        outputFile: File,
        width: Int,
        height: Int,
        fps: Int,
        onProgress: (Float, String) -> Unit
    ) {
        val mime = MediaFormat.MIMETYPE_VIDEO_AVC
        val format = MediaFormat.createVideoFormat(mime, width, height).apply {
            setInteger(
                MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
            )
            setInteger(MediaFormat.KEY_BIT_RATE, if (width >= 1280) 2_800_000 else 1_600_000)
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
        }

        val encoder = MediaCodec.createEncoderByType(mime)
        var muxer: MediaMuxer? = null
        var muxerStarted = false
        var trackIndex = -1
        val bufferInfo = MediaCodec.BufferInfo()
        val frameDurationUs = 1_000_000L / fps
        var encodedFrameCounter = 0L

        val bengaliTypeface = runCatching {
            ResourcesCompat.getFont(context, R.font.hind_siliguri)
        }.getOrNull()

        val badgeTypeface = runCatching {
            ResourcesCompat.getFont(context, R.font.baloo_da_2)
        }.getOrNull()

        val backgroundCache = mutableMapOf<String, Bitmap?>()

        fun drainEncoder(endOfStream: Boolean) {
            if (endOfStream) {
                encoder.signalEndOfInputStream()
            }
            while (true) {
                val outIndex = encoder.dequeueOutputBuffer(bufferInfo, 12_000L)
                when {
                    outIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> {
                        if (!endOfStream) break
                    }
                    outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        if (!muxerStarted) {
                            trackIndex = muxer!!.addTrack(encoder.outputFormat)
                            muxer!!.start()
                            muxerStarted = true
                        }
                    }
                    outIndex >= 0 -> {
                        val encodedData = encoder.getOutputBuffer(outIndex)
                        if (encodedData != null && bufferInfo.size > 0 && muxerStarted) {
                            val isConfig = (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0
                            if (!isConfig) {
                                bufferInfo.presentationTimeUs = encodedFrameCounter * frameDurationUs
                                encodedFrameCounter++
                                encodedData.position(bufferInfo.offset)
                                encodedData.limit(bufferInfo.offset + bufferInfo.size)
                                muxer!!.writeSampleData(trackIndex, encodedData, bufferInfo)
                            }
                        }
                        encoder.releaseOutputBuffer(outIndex, false)
                        if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                            break
                        }
                    }
                }
            }
        }

        try {
            encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val inputSurface = encoder.createInputSurface()
            encoder.start()
            muxer = MediaMuxer(outputFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            val totalFrames = scenes.sumOf { it.durationSeconds * fps }.coerceAtLeast(fps)
            var renderedFrames = 0

            scenes.forEachIndexed { sceneIndex, scene ->
                val bgBitmap = loadSceneBackground(scene, width, height, backgroundCache)
                val sceneFrames = scene.durationSeconds * fps
                val activeSpeaker = scene.activeSpeaker()
                val sortedChars = scene.characters.sortedBy { it.zIndex }

                for (frameInScene in 0 until sceneFrames) {
                    val sceneTimeSec = frameInScene.toFloat() / fps.toFloat()
                    val globalTimeSec = renderedFrames.toFloat() / fps.toFloat()

                    val canvas = inputSurface.lockCanvas(null)
                    try {
                        drawSceneFrame(
                            canvas = canvas,
                            width = width,
                            height = height,
                            bgBitmap = bgBitmap,
                            characters = sortedChars,
                            activeSpeakerId = activeSpeaker?.id,
                            sceneTimeSec = sceneTimeSec,
                            sceneDurationSec = scene.durationSeconds.toFloat(),
                            globalTimeSec = globalTimeSec,
                            bengaliTypeface = bengaliTypeface,
                            badgeTypeface = badgeTypeface
                        )
                    } finally {
                        inputSurface.unlockCanvasAndPost(canvas)
                    }

                    drainEncoder(endOfStream = false)
                    renderedFrames++

                    if (frameInScene % 4 == 0) {
                        val progress = (renderedFrames.toFloat() / totalFrames.toFloat()) * 0.92f
                        onProgress(
                            progress,
                            "দৃশ্য ${toBengaliDigits(sceneIndex + 1)}/${toBengaliDigits(scenes.size)} তৈরি হচ্ছে..."
                        )
                    }
                }
            }

            drainEncoder(endOfStream = true)
            inputSurface.release()
        } finally {
            backgroundCache.values.forEach { it?.recycle() }
            runCatching { encoder.stop() }
            runCatching { encoder.release() }
            runCatching {
                if (muxerStarted) muxer?.stop()
                muxer?.release()
            }
        }
    }

    private fun loadSceneBackground(
        scene: CartoonScene,
        targetW: Int,
        targetH: Int,
        cache: MutableMap<String, Bitmap?>
    ): Bitmap? {
        val key = if (scene.backgroundType == BackgroundType.CUSTOM) {
            "custom_${scene.customBackgroundPath}"
        } else {
            "res_${scene.backgroundType.name}"
        }
        if (cache.containsKey(key)) return cache[key]

        val raw = runCatching {
            if (scene.backgroundType == BackgroundType.CUSTOM && !scene.customBackgroundPath.isNullOrBlank()) {
                BitmapFactory.decodeFile(scene.customBackgroundPath)
            } else {
                val resId = scene.backgroundType.drawableRes ?: R.drawable.img_bg_village_road
                BitmapFactory.decodeResource(context.resources, resId)
            }
        }.getOrNull()

        val scaled = raw?.let { src ->
            val result = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
            val c = Canvas(result)
            // Center-crop fill
            val srcRatio = src.width.toFloat() / src.height.toFloat()
            val dstRatio = targetW.toFloat() / targetH.toFloat()
            val srcRect = if (srcRatio > dstRatio) {
                val cropW = (src.height * dstRatio).toInt()
                val left = (src.width - cropW) / 2
                Rect(left, 0, left + cropW, src.height)
            } else {
                val cropH = (src.width / dstRatio).toInt()
                val top = (src.height - cropH) / 2
                Rect(0, top, src.width, top + cropH)
            }
            c.drawBitmap(src, srcRect, Rect(0, 0, targetW, targetH), Paint(Paint.FILTER_BITMAP_FLAG))
            src.recycle()
            result
        }
        cache[key] = scaled
        return scaled
    }

    private fun drawSceneFrame(
        canvas: Canvas,
        width: Int,
        height: Int,
        bgBitmap: Bitmap?,
        characters: List<com.example.model.StageCharacter>,
        activeSpeakerId: String?,
        sceneTimeSec: Float,
        sceneDurationSec: Float,
        globalTimeSec: Float,
        bengaliTypeface: android.graphics.Typeface?,
        badgeTypeface: android.graphics.Typeface?
    ) {
        if (bgBitmap != null) {
            canvas.drawBitmap(bgBitmap, 0f, 0f, null)
        } else {
            canvas.drawColor(Color.parseColor("#B3E5FC"))
        }

        val baseUnitScale = (height / 470f)
        val isSpeakingWindow = sceneTimeSec in 0.25f..(sceneDurationSec - 0.35f)
        val animatedMouth = if (isSpeakingWindow) {
            (0.2f + 0.8f * abs(sin(globalTimeSec * 9.5f))).coerceIn(0f, 1f)
        } else {
            0f
        }

        var speakingChar: com.example.model.StageCharacter? = null
        var speakingAnchorX = width * 0.5f
        var speakingTopY = height * 0.45f

        for (char in characters) {
            val cx = char.xFraction * width
            val by = char.yFraction * height
            val charUnitScale = baseUnitScale * char.scale
            val isSpeaker = char.id == activeSpeakerId && char.dialogue.isNotBlank()
            val mouth = if (isSpeaker) animatedMouth else 0f

            CartoonCharacterRenderer.drawCharacter(
                canvas = canvas,
                type = char.type,
                expression = char.expression,
                outfitColorHex = char.outfitColorHex,
                centerX = cx,
                bottomY = by,
                unitScale = charUnitScale,
                flipped = char.flipped,
                timeSeconds = globalTimeSec,
                mouthOpen = mouth
            )

            if (isSpeaker) {
                speakingChar = char
                speakingAnchorX = cx
                speakingTopY = by - (220f * charUnitScale)
            }
        }

        if (speakingChar != null && sceneTimeSec >= 0.15f) {
            val popScale = min(1f, (sceneTimeSec - 0.15f) / 0.18f).coerceIn(0.2f, 1f)
            SpeechBubbleRenderer.drawSpeechBubble(
                canvas = canvas,
                text = speakingChar.dialogue,
                speakerName = speakingChar.name,
                anchorX = speakingAnchorX,
                anchorTopY = speakingTopY,
                stageWidth = width.toFloat(),
                stageHeight = height.toFloat(),
                typeface = bengaliTypeface,
                popScale = popScale
            )
        }

        // Subtle scene fade-in / fade-out
        val fadeAlpha = when {
            sceneTimeSec < 0.2f -> ((1f - sceneTimeSec / 0.2f) * 180f).toInt()
            sceneTimeSec > sceneDurationSec - 0.2f -> (((sceneTimeSec - (sceneDurationSec - 0.2f)) / 0.2f) * 180f).toInt()
            else -> 0
        }.coerceIn(0, 255)
        if (fadeAlpha > 0) {
            canvas.drawColor(Color.argb(fadeAlpha, 20, 18, 35))
        }

        // Clean studio watermark pill in bottom-right
        val scale = width / 1280f
        val badgeText = "কার্টুন স্টুডিও"
        val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 22f * scale
            typeface = badgeTypeface ?: bengaliTypeface
        }
        val tw = badgeTextPaint.measureText(badgeText)
        val badgeRect = RectF(
            width - tw - 44f * scale,
            height - 46f * scale,
            width - 16f * scale,
            height - 14f * scale
        )
        canvas.drawRoundRect(
            badgeRect,
            16f * scale,
            16f * scale,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(135, 30, 27, 46) }
        )
        canvas.drawText(
            badgeText,
            badgeRect.left + 14f * scale,
            badgeRect.bottom - 9f * scale,
            badgeTextPaint
        )
    }

    private fun saveToMediaStoreMovies(sourceFile: File, fileName: String): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                put(MediaStore.Video.Media.RELATIVE_PATH, "${Environment.DIRECTORY_MOVIES}/CartoonStudio")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
            if (uri != null) {
                runCatching {
                    resolver.openOutputStream(uri)?.use { out ->
                        FileInputStream(sourceFile).use { input ->
                            input.copyTo(out)
                        }
                    }
                    values.clear()
                    values.put(MediaStore.Video.Media.IS_PENDING, 0)
                    resolver.update(uri, values, null, null)
                }
                return "Movies/CartoonStudio/$fileName"
            }
        }
        return sourceFile.name
    }
}
