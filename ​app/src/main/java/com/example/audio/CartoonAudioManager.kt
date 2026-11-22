package com.example.audio

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.Locale
import java.util.UUID

/**
 * Manages Bengali Text-to-Speech (with customizable pitch & speed) as well as
 * local microphone voice recording (MediaRecorder -> .m4a) and playback (MediaPlayer).
 */
class CartoonAudioManager(private val context: Context) {

    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentRecordingFile: File? = null

    private val _isTtsReady = MutableStateFlow(false)
    val isTtsReady: StateFlow<Boolean> = _isTtsReady.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private var onPlaybackCompleteCallback: (() -> Unit)? = null

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsReady = true
                _isTtsReady.value = true
                selectBestBengaliLocale()
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                        val cb = onPlaybackCompleteCallback
                        onPlaybackCompleteCallback = null
                        cb?.invoke()
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                        val cb = onPlaybackCompleteCallback
                        onPlaybackCompleteCallback = null
                        cb?.invoke()
                    }
                })
            }
        }
    }

    private fun selectBestBengaliLocale() {
        val engine = tts ?: return
        val candidates = listOf(
            Locale("bn", "BD"),
            Locale("bn", "IN"),
            Locale("bn")
        )
        for (locale in candidates) {
            val res = engine.isLanguageAvailable(locale)
            if (res >= TextToSpeech.LANG_AVAILABLE) {
                engine.language = locale
                return
            }
        }
        // Fallback: attempt bn-BD anyway; Android TTS gracefully falls back if voice pack absent
        engine.language = Locale("bn", "BD")
    }

    fun speakTts(
        text: String,
        pitch: Float,
        speed: Float,
        onComplete: (() -> Unit)? = null
    ): Boolean {
        stopPlayback()
        if (text.isBlank()) {
            onComplete?.invoke()
            return false
        }
        val engine = tts
        if (!ttsReady || engine == null) {
            onComplete?.invoke()
            return false
        }
        onPlaybackCompleteCallback = onComplete
        engine.setPitch(pitch.coerceIn(0.45f, 2.0f))
        engine.setSpeechRate(speed.coerceIn(0.45f, 1.8f))
        _isSpeaking.value = true
        val utteranceId = "cartoon_${UUID.randomUUID()}"
        val res = engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        if (res != TextToSpeech.SUCCESS) {
            _isSpeaking.value = false
            onPlaybackCompleteCallback = null
            onComplete?.invoke()
            return false
        }
        return true
    }

    fun playRecordedAudio(
        filePath: String,
        onComplete: (() -> Unit)? = null
    ): Boolean {
        stopPlayback()
        val file = File(filePath)
        if (!file.exists() || file.length() == 0L) {
            onComplete?.invoke()
            return false
        }
        return runCatching {
            val player = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnCompletionListener {
                    _isSpeaking.value = false
                    release()
                    if (mediaPlayer === this) mediaPlayer = null
                    onComplete?.invoke()
                }
                setOnErrorListener { mp, _, _ ->
                    _isSpeaking.value = false
                    mp.release()
                    if (mediaPlayer === mp) mediaPlayer = null
                    onComplete?.invoke()
                    true
                }
                prepare()
                start()
            }
            mediaPlayer = player
            _isSpeaking.value = true
            true
        }.getOrElse {
            _isSpeaking.value = false
            onComplete?.invoke()
            false
        }
    }

    fun stopPlayback() {
        onPlaybackCompleteCallback = null
        runCatching { tts?.stop() }
        runCatching {
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        }
        mediaPlayer = null
        _isSpeaking.value = false
    }

    fun startRecording(outputDir: File, characterId: String): File? {
        stopPlayback()
        stopRecording()
        outputDir.mkdirs()
        val file = File(outputDir, "voice_${characterId.take(8)}_${System.currentTimeMillis()}.m4a")
        return runCatching {
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(96000)
                setAudioSamplingRate(44100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            mediaRecorder = recorder
            currentRecordingFile = file
            file
        }.getOrNull()
    }

    fun getRecordingAmplitudeNormalized(): Float {
        val amp = runCatching { mediaRecorder?.maxAmplitude ?: 0 }.getOrDefault(0)
        return (amp / 16000f).coerceIn(0.08f, 1f)
    }

    fun stopRecording(): File? {
        val file = currentRecordingFile
        runCatching {
            mediaRecorder?.stop()
        }
        runCatching {
            mediaRecorder?.release()
        }
        mediaRecorder = null
        currentRecordingFile = null
        return if (file != null && file.exists() && file.length() > 0L) file else null
    }

    fun release() {
        stopRecording()
        stopPlayback()
        runCatching { tts?.shutdown() }
        tts = null
        ttsReady = false
    }
}
