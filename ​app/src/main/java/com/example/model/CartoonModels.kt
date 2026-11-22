package com.example.model

import androidx.annotation.DrawableRes
import com.example.R
import java.util.UUID

enum class CharacterType(
    val displayNameBn: String,
    val shortNameBn: String,
    val defaultOutfitColorHex: Long,
    val defaultVoicePreset: VoicePreset,
    val defaultScale: Float
) {
    BOY("রাহুল (ছেলে)", "রাহুল", 0xFF1E88E5L, VoicePreset.CHILD, 1.0f),
    GIRL("মীনা (মেয়ে)", "মীনা", 0xFFE91E63L, VoicePreset.GIRL, 0.98f),
    GRANDPA("দাদুভাই", "দাদুভাই", 0xFFFFB300L, VoicePreset.GRANDPA, 1.08f),
    TIGER("বাঘ মামা", "বাঘ মামা", 0xFF2E9E5BL, VoicePreset.CARTOON, 0.96f),
    BIRD("টুনটুনি পাখি", "টুনটুনি", 0xFF00897BL, VoicePreset.CARTOON, 0.78f),
    CAT("মিষ্টি বিড়াল", "বিড়াল", 0xFF8E24AAL, VoicePreset.CHILD, 0.85f)
}

enum class Expression(val labelBn: String) {
    HAPPY("খুশি"),
    SAD("দুঃখী"),
    ANGRY("রাগী"),
    TALKING("কথা বলছে")
}

enum class VoicePreset(
    val labelBn: String,
    val pitch: Float,
    val speed: Float
) {
    CHILD("শিশু কণ্ঠ", 1.55f, 1.08f),
    GIRL("মিষ্টি কণ্ঠ", 1.32f, 1.02f),
    NORMAL("স্বাভাবিক", 1.0f, 0.98f),
    GRANDPA("দাদুর কণ্ঠ", 0.62f, 0.80f),
    CARTOON("কার্টুন কণ্ঠ", 1.82f, 1.18f)
}

enum class VoiceSource {
    TTS,
    RECORDED
}

enum class BackgroundType(
    val labelBn: String,
    @DrawableRes val drawableRes: Int?
) {
    VILLAGE_ROAD("গ্রামের রাস্তা", R.drawable.img_bg_village_road),
    SCHOOL("স্কুল", R.drawable.img_bg_school),
    FOREST("সবুজ বন", R.drawable.img_bg_forest),
    HOUSE_INTERIOR("ঘরের ভেতর", R.drawable.img_bg_house_interior),
    CUSTOM("নিজের ছবি", null)
}

enum class StudioTab(val labelBn: String) {
    CHARACTERS("চরিত্র"),
    BACKGROUNDS("ব্যাকগ্রাউন্ড"),
    DIALOGUE("সংলাপ ও ভয়েস"),
    SCENES("দৃশ্যপট")
}

data class StageCharacter(
    val id: String = UUID.randomUUID().toString(),
    val type: CharacterType,
    val name: String = type.shortNameBn,
    val xFraction: Float = 0.5f,
    val yFraction: Float = 0.68f,
    val scale: Float = type.defaultScale,
    val flipped: Boolean = false,
    val expression: Expression = Expression.HAPPY,
    val outfitColorHex: Long = type.defaultOutfitColorHex,
    val dialogue: String = "",
    val voicePreset: VoicePreset = type.defaultVoicePreset,
    val pitch: Float = type.defaultVoicePreset.pitch,
    val speed: Float = type.defaultVoicePreset.speed,
    val voiceSource: VoiceSource = VoiceSource.TTS,
    val recordedAudioPath: String? = null,
    val zIndex: Int = 0
)

data class CartoonScene(
    val id: String = UUID.randomUUID().toString(),
    val backgroundType: BackgroundType = BackgroundType.VILLAGE_ROAD,
    val customBackgroundPath: String? = null,
    val characters: List<StageCharacter> = emptyList(),
    val durationSeconds: Int = 4,
    val speakingCharacterId: String? = null
) {
    fun activeSpeaker(): StageCharacter? {
        val explicit = characters.firstOrNull {
            it.id == speakingCharacterId && (it.dialogue.isNotBlank() || !it.recordedAudioPath.isNullOrBlank())
        }
        if (explicit != null) return explicit
        return characters.firstOrNull {
            it.dialogue.isNotBlank() || !it.recordedAudioPath.isNullOrBlank()
        }
    }
}

sealed interface ExportState {
    data object Idle : ExportState
    data class Exporting(val progress: Float, val statusTextBn: String) : ExportState
    data class Success(
        val shareFilePath: String,
        val galleryDisplayPath: String
    ) : ExportState
    data class Error(val messageBn: String) : ExportState
}

fun toBengaliDigits(value: Int): String {
    val bengaliDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
    return value.toString().map { ch ->
        if (ch in '0'..'9') bengaliDigits[ch - '0'] else ch
    }.joinToString("")
}

fun toBengaliDecimal(value: Float): String {
    val formatted = String.format(java.util.Locale.US, "%.1f", value)
    val bengaliDigits = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
    return formatted.map { ch ->
        if (ch in '0'..'9') bengaliDigits[ch - '0'] else ch
    }.joinToString("")
}
