package com.example.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.model.BackgroundType
import com.example.model.CartoonScene
import com.example.model.CharacterType
import com.example.model.Expression
import com.example.model.StageCharacter
import com.example.model.VoicePreset
import com.example.model.VoiceSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class ProjectRepository(private val context: Context) {

    private val projectFile: File
        get() = File(context.filesDir, "cartoon_story_v1.json")

    private val backgroundsDir: File
        get() = File(context.filesDir, "backgrounds").apply { mkdirs() }

    val recordingsDir: File
        get() = File(context.filesDir, "recordings").apply { mkdirs() }

    suspend fun loadScenes(): List<CartoonScene> = withContext(Dispatchers.IO) {
        if (!projectFile.exists()) {
            return@withContext createStarterStory()
        }
        runCatching {
            val raw = projectFile.readText()
            val array = JSONArray(raw)
            val result = mutableListOf<CartoonScene>()
            for (i in 0 until array.length()) {
                result.add(parseScene(array.getJSONObject(i)))
            }
            if (result.isEmpty()) createStarterStory() else result
        }.getOrElse {
            createStarterStory()
        }
    }

    suspend fun saveScenes(scenes: List<CartoonScene>) = withContext(Dispatchers.IO) {
        runCatching {
            val array = JSONArray()
            scenes.forEach { scene -> array.put(serializeScene(scene)) }
            projectFile.writeText(array.toString())
        }
    }

    suspend fun importCustomBackground(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val input = context.contentResolver.openInputStream(uri) ?: return@runCatching null
            val original = input.use { BitmapFactory.decodeStream(it) } ?: return@runCatching null
            // Scale down gracefully to max 1280px wide for fast stage rendering & HD video export
            val maxW = 1280
            val scaled = if (original.width > maxW) {
                val ratio = maxW.toFloat() / original.width.toFloat()
                val newH = (original.height * ratio).toInt().coerceAtLeast(360)
                Bitmap.createScaledBitmap(original, maxW, newH, true).also {
                    if (it != original) original.recycle()
                }
            } else {
                original
            }
            val dest = File(backgroundsDir, "bg_${System.currentTimeMillis()}.jpg")
            FileOutputStream(dest).use { out ->
                scaled.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            scaled.recycle()
            dest.absolutePath
        }.getOrNull()
    }

    fun createStarterStory(): List<CartoonScene> {
        val boyId1 = UUID.randomUUID().toString()
        val girlId1 = UUID.randomUUID().toString()
        val scene1 = CartoonScene(
            id = UUID.randomUUID().toString(),
            backgroundType = BackgroundType.VILLAGE_ROAD,
            durationSeconds = 4,
            speakingCharacterId = boyId1,
            characters = listOf(
                StageCharacter(
                    id = boyId1,
                    type = CharacterType.BOY,
                    name = "রাহুল",
                    xFraction = 0.30f,
                    yFraction = 0.88f,
                    scale = 1.0f,
                    flipped = false,
                    expression = Expression.TALKING,
                    outfitColorHex = 0xFF1E88E5L,
                    dialogue = "নমস্কার মীনা! চলো আজ মজার বাংলা কার্টুন বানাই!",
                    voicePreset = VoicePreset.CHILD,
                    pitch = VoicePreset.CHILD.pitch,
                    speed = VoicePreset.CHILD.speed,
                    zIndex = 1
                ),
                StageCharacter(
                    id = girlId1,
                    type = CharacterType.GIRL,
                    name = "মীনা",
                    xFraction = 0.70f,
                    yFraction = 0.88f,
                    scale = 0.98f,
                    flipped = true,
                    expression = Expression.HAPPY,
                    outfitColorHex = 0xFFE91E63L,
                    dialogue = "বাহ! দারুণ হবে! চলো স্কুলে গিয়ে সবাইকে দেখাই!",
                    voicePreset = VoicePreset.GIRL,
                    pitch = VoicePreset.GIRL.pitch,
                    speed = VoicePreset.GIRL.speed,
                    zIndex = 2
                )
            )
        )

        val grandpaId2 = UUID.randomUUID().toString()
        val birdId2 = UUID.randomUUID().toString()
        val scene2 = CartoonScene(
            id = UUID.randomUUID().toString(),
            backgroundType = BackgroundType.SCHOOL,
            durationSeconds = 4,
            speakingCharacterId = grandpaId2,
            characters = listOf(
                StageCharacter(
                    id = grandpaId2,
                    type = CharacterType.GRANDPA,
                    name = "দাদুভাই",
                    xFraction = 0.34f,
                    yFraction = 0.90f,
                    scale = 1.06f,
                    flipped = false,
                    expression = Expression.HAPPY,
                    outfitColorHex = 0xFFFFB300L,
                    dialogue = "খুব সুন্দর গল্প বানিয়েছো তোমরা, সাবাশ!",
                    voicePreset = VoicePreset.GRANDPA,
                    pitch = VoicePreset.GRANDPA.pitch,
                    speed = VoicePreset.GRANDPA.speed,
                    zIndex = 1
                ),
                StageCharacter(
                    id = birdId2,
                    type = CharacterType.BIRD,
                    name = "টুনটুনি",
                    xFraction = 0.72f,
                    yFraction = 0.66f,
                    scale = 0.82f,
                    flipped = true,
                    expression = Expression.HAPPY,
                    outfitColorHex = 0xFF00897BL,
                    dialogue = "কিচিরমিচির! আমিও গল্পে আছি!",
                    voicePreset = VoicePreset.CARTOON,
                    pitch = VoicePreset.CARTOON.pitch,
                    speed = VoicePreset.CARTOON.speed,
                    zIndex = 2
                )
            )
        )
        return listOf(scene1, scene2)
    }

    private fun serializeScene(scene: CartoonScene): JSONObject {
        val obj = JSONObject()
        obj.put("id", scene.id)
        obj.put("backgroundType", scene.backgroundType.name)
        obj.put("customBackgroundPath", scene.customBackgroundPath ?: JSONObject.NULL)
        obj.put("durationSeconds", scene.durationSeconds)
        obj.put("speakingCharacterId", scene.speakingCharacterId ?: JSONObject.NULL)
        val charsArr = JSONArray()
        scene.characters.forEach { c ->
            val co = JSONObject()
            co.put("id", c.id)
            co.put("type", c.type.name)
            co.put("name", c.name)
            co.put("xFraction", c.xFraction.toDouble())
            co.put("yFraction", c.yFraction.toDouble())
            co.put("scale", c.scale.toDouble())
            co.put("flipped", c.flipped)
            co.put("expression", c.expression.name)
            co.put("outfitColorHex", c.outfitColorHex)
            co.put("dialogue", c.dialogue)
            co.put("voicePreset", c.voicePreset.name)
            co.put("pitch", c.pitch.toDouble())
            co.put("speed", c.speed.toDouble())
            co.put("voiceSource", c.voiceSource.name)
            co.put("recordedAudioPath", c.recordedAudioPath ?: JSONObject.NULL)
            co.put("zIndex", c.zIndex)
            charsArr.put(co)
        }
        obj.put("characters", charsArr)
        return obj
    }

    private fun parseScene(obj: JSONObject): CartoonScene {
        val bgType = runCatching { BackgroundType.valueOf(obj.getString("backgroundType")) }
            .getOrDefault(BackgroundType.VILLAGE_ROAD)
        val customPath = if (obj.isNull("customBackgroundPath")) null else obj.optString("customBackgroundPath", null)
        val speakingId = if (obj.isNull("speakingCharacterId")) null else obj.optString("speakingCharacterId", null)
        val charsArr = obj.optJSONArray("characters") ?: JSONArray()
        val characters = mutableListOf<StageCharacter>()
        for (i in 0 until charsArr.length()) {
            val co = charsArr.getJSONObject(i)
            val charType = runCatching { CharacterType.valueOf(co.getString("type")) }
                .getOrDefault(CharacterType.BOY)
            val expr = runCatching { Expression.valueOf(co.optString("expression", "HAPPY")) }
                .getOrDefault(Expression.HAPPY)
            val preset = runCatching { VoicePreset.valueOf(co.optString("voicePreset", "NORMAL")) }
                .getOrDefault(VoicePreset.NORMAL)
            val source = runCatching { VoiceSource.valueOf(co.optString("voiceSource", "TTS")) }
                .getOrDefault(VoiceSource.TTS)
            val recPath = if (co.isNull("recordedAudioPath")) null else co.optString("recordedAudioPath", null)
            characters.add(
                StageCharacter(
                    id = co.optString("id", UUID.randomUUID().toString()),
                    type = charType,
                    name = co.optString("name", charType.shortNameBn),
                    xFraction = co.optDouble("xFraction", 0.5).toFloat(),
                    yFraction = co.optDouble("yFraction", 0.85).toFloat(),
                    scale = co.optDouble("scale", 1.0).toFloat(),
                    flipped = co.optBoolean("flipped", false),
                    expression = expr,
                    outfitColorHex = co.optLong("outfitColorHex", charType.defaultOutfitColorHex),
                    dialogue = co.optString("dialogue", ""),
                    voicePreset = preset,
                    pitch = co.optDouble("pitch", preset.pitch.toDouble()).toFloat(),
                    speed = co.optDouble("speed", preset.speed.toDouble()).toFloat(),
                    voiceSource = source,
                    recordedAudioPath = recPath,
                    zIndex = co.optInt("zIndex", i)
                )
            )
        }
        return CartoonScene(
            id = obj.optString("id", UUID.randomUUID().toString()),
            backgroundType = bgType,
            customBackgroundPath = customPath,
            characters = characters,
            durationSeconds = obj.optInt("durationSeconds", 4).coerceIn(2, 12),
            speakingCharacterId = speakingId
        )
    }
}
