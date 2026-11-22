package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.CartoonAudioManager
import com.example.data.ProjectRepository
import com.example.export.CartoonVideoExporter
import com.example.model.BackgroundType
import com.example.model.CartoonScene
import com.example.model.CharacterType
import com.example.model.ExportState
import com.example.model.Expression
import com.example.model.StageCharacter
import com.example.model.StudioTab
import com.example.model.VoicePreset
import com.example.model.VoiceSource
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

data class CartoonStudioUiState(
    val scenes: List<CartoonScene> = emptyList(),
    val currentSceneIndex: Int = 0,
    val selectedCharacterId: String? = null,
    val activeTab: StudioTab = StudioTab.CHARACTERS,

    // Character Creator state (for customizing before adding to stage)
    val creatorType: CharacterType = CharacterType.BOY,
    val creatorExpression: Expression = Expression.HAPPY,
    val creatorOutfitColorHex: Long = CharacterType.BOY.defaultOutfitColorHex,

    // Voice preview & recording state
    val previewSpeakingCharId: String? = null,
    val isRecording: Boolean = false,
    val recordingSeconds: Int = 0,
    val recordingAmplitude: Float = 0.1f,

    // Story player state
    val isStoryPlayerOpen: Boolean = false,
    val playbackSceneIndex: Int = 0,
    val playbackSpeakingCharId: String? = null,
    val playbackProgressInScene: Float = 0f,
    val isPlaybackPlaying: Boolean = false,

    // Video export state
    val showExportDialog: Boolean = false,
    val exportState: ExportState = ExportState.Idle,

    // Transient Bengali feedback banner
    val statusMessage: String? = null
) {
    val currentScene: CartoonScene?
        get() = scenes.getOrNull(currentSceneIndex)

    val selectedCharacter: StageCharacter?
        get() = currentScene?.characters?.firstOrNull { it.id == selectedCharacterId }

    val playbackScene: CartoonScene?
        get() = scenes.getOrNull(playbackSceneIndex)

    val totalDurationSeconds: Int
        get() = scenes.sumOf { it.durationSeconds }
}

class CartoonStudioViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProjectRepository(application)
    private val audioManager = CartoonAudioManager(application)
    private val videoExporter = CartoonVideoExporter(application)

    private val _uiState = MutableStateFlow(CartoonStudioUiState())
    val uiState: StateFlow<CartoonStudioUiState> = _uiState.asStateFlow()

    private var saveJob: Job? = null
    private var recordingTimerJob: Job? = null
    private var storyPlaybackJob: Job? = null

    init {
        viewModelScope.launch {
            val loaded = repository.loadScenes()
            val firstScene = loaded.firstOrNull()
            _uiState.update {
                it.copy(
                    scenes = loaded,
                    currentSceneIndex = 0,
                    selectedCharacterId = firstScene?.speakingCharacterId ?: firstScene?.characters?.firstOrNull()?.id
                )
            }
        }
    }

    fun selectTab(tab: StudioTab) {
        _uiState.update { it.copy(activeTab = tab) }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    fun showFeedback(msg: String) {
        _uiState.update { it.copy(statusMessage = msg) }
    }

    private fun showMessage(msg: String) = showFeedback(msg)

    // --- Character Creator & Library ---

    fun selectCreatorType(type: CharacterType) {
        _uiState.update {
            it.copy(
                creatorType = type,
                creatorOutfitColorHex = type.defaultOutfitColorHex
            )
        }
    }

    fun selectCreatorExpression(expression: Expression) {
        _uiState.update { it.copy(creatorExpression = expression) }
    }

    fun selectCreatorOutfitColor(colorHex: Long) {
        _uiState.update { it.copy(creatorOutfitColorHex = colorHex) }
    }

    fun addCharacterFromCreator() {
        val state = _uiState.value
        val scene = state.currentScene ?: return
        val maxZ = (scene.characters.maxOfOrNull { it.zIndex } ?: 0) + 1
        // Offset newly added characters slightly so they don't stack invisibly
        val offsetIndex = scene.characters.size % 4
        val startX = when (offsetIndex) {
            0 -> 0.32f
            1 -> 0.68f
            2 -> 0.50f
            else -> 0.22f
        }
        val startY = if (state.creatorType == CharacterType.BIRD) 0.56f else 0.88f

        val newChar = StageCharacter(
            id = UUID.randomUUID().toString(),
            type = state.creatorType,
            name = state.creatorType.shortNameBn,
            xFraction = startX,
            yFraction = startY,
            scale = state.creatorType.defaultScale,
            flipped = offsetIndex == 1,
            expression = state.creatorExpression,
            outfitColorHex = state.creatorOutfitColorHex,
            voicePreset = state.creatorType.defaultVoicePreset,
            pitch = state.creatorType.defaultVoicePreset.pitch,
            speed = state.creatorType.defaultVoicePreset.speed,
            zIndex = maxZ
        )

        updateCurrentScene { current ->
            current.copy(
                characters = current.characters + newChar,
                speakingCharacterId = current.speakingCharacterId ?: newChar.id
            )
        }
        _uiState.update { it.copy(selectedCharacterId = newChar.id) }
        showMessage("${newChar.name} মঞ্চে যোগ হয়েছে!")
    }

    // --- Stage Manipulation (Drag, Resize, Flip, Expression, Color, Delete) ---

    fun selectCharacter(characterId: String?) {
        _uiState.update { it.copy(selectedCharacterId = characterId) }
        if (characterId != null) {
            bringCharacterToFront(characterId)
        }
    }

    fun moveCharacter(characterId: String, deltaXFraction: Float, deltaYFraction: Float) {
        updateCharacter(characterId, persistImmediately = false) { char ->
            char.copy(
                xFraction = (char.xFraction + deltaXFraction).coerceIn(0.08f, 0.92f),
                yFraction = (char.yFraction + deltaYFraction).coerceIn(0.25f, 0.96f)
            )
        }
    }

    fun onStageGestureEnded() {
        scheduleSave()
    }

    fun scaleCharacterBy(characterId: String, zoomFactor: Float) {
        updateCharacter(characterId, persistImmediately = false) { char ->
            char.copy(scale = (char.scale * zoomFactor).coerceIn(0.45f, 1.85f))
        }
    }

    fun setCharacterScale(characterId: String, newScale: Float) {
        updateCharacter(characterId) { char ->
            char.copy(scale = newScale.coerceIn(0.45f, 1.85f))
        }
    }

    fun toggleFlipCharacter(characterId: String) {
        updateCharacter(characterId) { char ->
            char.copy(flipped = !char.flipped)
        }
    }

    fun setCharacterExpression(characterId: String, expression: Expression) {
        updateCharacter(characterId) { char ->
            char.copy(expression = expression)
        }
    }

    fun setCharacterOutfitColor(characterId: String, colorHex: Long) {
        updateCharacter(characterId) { char ->
            char.copy(outfitColorHex = colorHex)
        }
    }

    fun bringCharacterToFront(characterId: String) {
        val scene = _uiState.value.currentScene ?: return
        val maxZ = scene.characters.maxOfOrNull { it.zIndex } ?: 0
        val target = scene.characters.firstOrNull { it.id == characterId } ?: return
        if (target.zIndex >= maxZ && scene.characters.count { it.zIndex == maxZ } == 1) return
        updateCharacter(characterId) { it.copy(zIndex = maxZ + 1) }
    }

    fun deleteCharacter(characterId: String) {
        stopVoicePreview()
        updateCurrentScene { scene ->
            val remaining = scene.characters.filterNot { it.id == characterId }
            scene.copy(
                characters = remaining,
                speakingCharacterId = if (scene.speakingCharacterId == characterId) {
                    remaining.firstOrNull()?.id
                } else {
                    scene.speakingCharacterId
                }
            )
        }
        _uiState.update { state ->
            val nextId = state.currentScene?.characters?.lastOrNull()?.id
            state.copy(selectedCharacterId = if (state.selectedCharacterId == characterId) nextId else state.selectedCharacterId)
        }
    }

    // --- Backgrounds ---

    fun setPresetBackground(type: BackgroundType) {
        updateCurrentScene { scene ->
            scene.copy(backgroundType = type)
        }
    }

    fun importGalleryBackground(uri: Uri) {
        viewModelScope.launch {
            val savedPath = repository.importCustomBackground(uri)
            if (savedPath != null) {
                updateCurrentScene { scene ->
                    scene.copy(
                        backgroundType = BackgroundType.CUSTOM,
                        customBackgroundPath = savedPath
                    )
                }
                showMessage("গ্যালারি থেকে ব্যাকগ্রাউন্ড যুক্ত হয়েছে!")
            } else {
                showMessage("ছবি লোড করতে সমস্যা হয়েছে")
            }
        }
    }

    // --- Dialogue, TTS & Voiceover ---

    fun updateCharacterDialogue(characterId: String, dialogue: String) {
        updateCurrentScene { scene ->
            scene.copy(
                speakingCharacterId = if (dialogue.isNotBlank()) characterId else scene.speakingCharacterId,
                characters = scene.characters.map { ch ->
                    if (ch.id == characterId) ch.copy(dialogue = dialogue) else ch
                }
            )
        }
    }

    fun setCharacterAsSceneSpeaker(characterId: String) {
        updateCurrentScene { scene ->
            scene.copy(speakingCharacterId = characterId)
        }
    }

    fun applyVoicePreset(characterId: String, preset: VoicePreset) {
        updateCharacter(characterId) { char ->
            char.copy(
                voicePreset = preset,
                pitch = preset.pitch,
                speed = preset.speed,
                voiceSource = VoiceSource.TTS
            )
        }
    }

    fun setVoicePitch(characterId: String, pitch: Float) {
        updateCharacter(characterId) { char ->
            char.copy(pitch = pitch.coerceIn(0.5f, 2.0f))
        }
    }

    fun setVoiceSpeed(characterId: String, speed: Float) {
        updateCharacter(characterId) { char ->
            char.copy(speed = speed.coerceIn(0.5f, 1.6f))
        }
    }

    fun setVoiceSource(characterId: String, source: VoiceSource) {
        updateCharacter(characterId) { char ->
            char.copy(voiceSource = source)
        }
    }

    fun previewCharacterVoice(characterId: String) {
        val char = _uiState.value.currentScene?.characters?.firstOrNull { it.id == characterId } ?: return
        if (_uiState.value.previewSpeakingCharId == characterId) {
            stopVoicePreview()
            return
        }

        stopVoicePreview()
        setCharacterAsSceneSpeaker(characterId)

        if (char.voiceSource == VoiceSource.RECORDED && !char.recordedAudioPath.isNullOrBlank()) {
            _uiState.update { it.copy(previewSpeakingCharId = characterId) }
            val started = audioManager.playRecordedAudio(char.recordedAudioPath) {
                _uiState.update { it.copy(previewSpeakingCharId = null) }
            }
            if (!started) {
                _uiState.update { it.copy(previewSpeakingCharId = null) }
                showMessage("রেকর্ড করা অডিও পাওয়া যায়নি")
            }
        } else {
            val textToSpeak = char.dialogue.ifBlank { "নমস্কার! আমি ${char.name}।" }
            _uiState.update { it.copy(previewSpeakingCharId = characterId) }
            val started = audioManager.speakTts(
                text = textToSpeak,
                pitch = char.pitch,
                speed = char.speed
            ) {
                _uiState.update { it.copy(previewSpeakingCharId = null) }
            }
            if (!started) {
                // Even if device TTS engine is unavailable, animate mouth briefly so user gets feedback
                viewModelScope.launch {
                    delay(2000L)
                    _uiState.update {
                        if (it.previewSpeakingCharId == characterId) it.copy(previewSpeakingCharId = null) else it
                    }
                }
            }
        }
    }

    fun stopVoicePreview() {
        audioManager.stopPlayback()
        _uiState.update { it.copy(previewSpeakingCharId = null) }
    }

    fun startVoiceRecording(characterId: String) {
        stopVoicePreview()
        val file = audioManager.startRecording(repository.recordingsDir, characterId)
        if (file == null) {
            showMessage("মাইক্রোফোন চালু করা যায়নি")
            return
        }
        _uiState.update {
            it.copy(
                isRecording = true,
                recordingSeconds = 0,
                recordingAmplitude = 0.15f
            )
        }
        recordingTimerJob?.cancel()
        recordingTimerJob = viewModelScope.launch {
            var ticks = 0
            while (true) {
                delay(120L)
                ticks++
                val amp = audioManager.getRecordingAmplitudeNormalized()
                val sec = (ticks * 120) / 1000
                _uiState.update { it.copy(recordingSeconds = sec, recordingAmplitude = amp) }
                if (sec >= 30) {
                    stopVoiceRecording(characterId)
                    break
                }
            }
        }
    }

    fun stopVoiceRecording(characterId: String) {
        recordingTimerJob?.cancel()
        recordingTimerJob = null
        val savedFile = audioManager.stopRecording()
        _uiState.update {
            it.copy(
                isRecording = false,
                recordingSeconds = 0,
                recordingAmplitude = 0.1f
            )
        }
        if (savedFile != null) {
            updateCharacter(characterId) { char ->
                char.copy(
                    recordedAudioPath = savedFile.absolutePath,
                    voiceSource = VoiceSource.RECORDED
                )
            }
            setCharacterAsSceneSpeaker(characterId)
            showMessage("আপনার কণ্ঠ রেকর্ড ও সেভ হয়েছে!")
        } else {
            showMessage("রেকর্ডিং সংরক্ষণ করা যায়নি")
        }
    }

    fun deleteRecordedVoice(characterId: String) {
        stopVoicePreview()
        val char = _uiState.value.currentScene?.characters?.firstOrNull { it.id == characterId }
        char?.recordedAudioPath?.let { path -> runCatching { File(path).delete() } }
        updateCharacter(characterId) { c ->
            c.copy(recordedAudioPath = null, voiceSource = VoiceSource.TTS)
        }
        showMessage("রেকর্ডিং মুছে ফেলা হয়েছে")
    }

    // --- Scene Timeline Management ---

    fun selectScene(index: Int) {
        stopVoicePreview()
        val scenes = _uiState.value.scenes
        if (index !in scenes.indices) return
        val scene = scenes[index]
        _uiState.update {
            it.copy(
                currentSceneIndex = index,
                selectedCharacterId = scene.speakingCharacterId ?: scene.characters.firstOrNull()?.id
            )
        }
    }

    fun addNewScene() {
        stopVoicePreview()
        val current = _uiState.value.currentScene
        val nextBg = when (current?.backgroundType) {
            BackgroundType.VILLAGE_ROAD -> BackgroundType.SCHOOL
            BackgroundType.SCHOOL -> BackgroundType.FOREST
            BackgroundType.FOREST -> BackgroundType.HOUSE_INTERIOR
            else -> BackgroundType.VILLAGE_ROAD
        }
        val newScene = CartoonScene(
            id = UUID.randomUUID().toString(),
            backgroundType = nextBg,
            durationSeconds = 4,
            characters = emptyList()
        )
        val updated = _uiState.value.scenes + newScene
        _uiState.update {
            it.copy(
                scenes = updated,
                currentSceneIndex = updated.lastIndex,
                selectedCharacterId = null
            )
        }
        scheduleSave()
        showMessage("নতুন দৃশ্য যোগ হয়েছে!")
    }

    fun duplicateScene(index: Int) {
        val scenes = _uiState.value.scenes
        val source = scenes.getOrNull(index) ?: return
        val clonedChars = source.characters.map { it.copy(id = UUID.randomUUID().toString()) }
        val speakerIdx = source.characters.indexOfFirst { it.id == source.speakingCharacterId }
        val cloned = source.copy(
            id = UUID.randomUUID().toString(),
            characters = clonedChars,
            speakingCharacterId = clonedChars.getOrNull(speakerIdx)?.id
        )
        val updated = scenes.toMutableList().apply { add(index + 1, cloned) }
        _uiState.update {
            it.copy(
                scenes = updated,
                currentSceneIndex = index + 1,
                selectedCharacterId = cloned.speakingCharacterId ?: clonedChars.firstOrNull()?.id
            )
        }
        scheduleSave()
        showMessage("দৃশ্য কপি করা হয়েছে")
    }

    fun deleteScene(index: Int) {
        val scenes = _uiState.value.scenes
        if (scenes.size <= 1 || index !in scenes.indices) {
            showMessage("অন্তত একটি দৃশ্য থাকতে হবে")
            return
        }
        stopVoicePreview()
        val updated = scenes.toMutableList().apply { removeAt(index) }
        val newIndex = index.coerceAtMost(updated.lastIndex)
        _uiState.update {
            it.copy(
                scenes = updated,
                currentSceneIndex = newIndex,
                selectedCharacterId = updated[newIndex].characters.firstOrNull()?.id
            )
        }
        scheduleSave()
    }

    fun updateSceneDuration(index: Int, deltaSeconds: Int) {
        val scenes = _uiState.value.scenes
        if (index !in scenes.indices) return
        val updated = scenes.mapIndexed { i, scene ->
            if (i == index) {
                scene.copy(durationSeconds = (scene.durationSeconds + deltaSeconds).coerceIn(2, 12))
            } else {
                scene
            }
        }
        _uiState.update { it.copy(scenes = updated) }
        scheduleSave()
    }

    // --- Full Story Theater Playback ---

    fun openAndPlayStory() {
        stopVoicePreview()
        if (_uiState.value.scenes.isEmpty()) return
        _uiState.update {
            it.copy(
                isStoryPlayerOpen = true,
                playbackSceneIndex = 0,
                playbackProgressInScene = 0f,
                isPlaybackPlaying = true
            )
        }
        startStoryPlaybackFrom(0)
    }

    fun toggleStoryPlayPause() {
        if (_uiState.value.isPlaybackPlaying) {
            storyPlaybackJob?.cancel()
            audioManager.stopPlayback()
            _uiState.update { it.copy(isPlaybackPlaying = false, playbackSpeakingCharId = null) }
        } else {
            val currentIdx = _uiState.value.playbackSceneIndex
            val restartIdx = if (currentIdx >= _uiState.value.scenes.lastIndex && _uiState.value.playbackProgressInScene >= 0.98f) {
                0
            } else {
                currentIdx
            }
            _uiState.update { it.copy(isPlaybackPlaying = true) }
            startStoryPlaybackFrom(restartIdx)
        }
    }

    fun restartStoryPlayback() {
        _uiState.update { it.copy(isPlaybackPlaying = true) }
        startStoryPlaybackFrom(0)
    }

    fun closeStoryPlayer() {
        storyPlaybackJob?.cancel()
        audioManager.stopPlayback()
        _uiState.update {
            it.copy(
                isStoryPlayerOpen = false,
                isPlaybackPlaying = false,
                playbackSpeakingCharId = null
            )
        }
    }

    private fun startStoryPlaybackFrom(startSceneIndex: Int) {
        storyPlaybackJob?.cancel()
        audioManager.stopPlayback()

        storyPlaybackJob = viewModelScope.launch {
            val scenes = _uiState.value.scenes
            for (i in startSceneIndex until scenes.size) {
                val scene = scenes[i]
                val speaker = scene.activeSpeaker()
                _uiState.update {
                    it.copy(
                        playbackSceneIndex = i,
                        playbackProgressInScene = 0f,
                        playbackSpeakingCharId = speaker?.id
                    )
                }

                // Trigger audio for this scene's active speaker
                if (speaker != null) {
                    if (speaker.voiceSource == VoiceSource.RECORDED && !speaker.recordedAudioPath.isNullOrBlank()) {
                        audioManager.playRecordedAudio(speaker.recordedAudioPath) {
                            _uiState.update { st ->
                                if (st.playbackSceneIndex == i) st.copy(playbackSpeakingCharId = null) else st
                            }
                        }
                    } else if (speaker.dialogue.isNotBlank()) {
                        audioManager.speakTts(
                            text = speaker.dialogue,
                            pitch = speaker.pitch,
                            speed = speaker.speed
                        ) {
                            _uiState.update { st ->
                                if (st.playbackSceneIndex == i) st.copy(playbackSpeakingCharId = null) else st
                            }
                        }
                    }
                }

                val durationMs = scene.durationSeconds * 1000L
                val stepMs = 50L
                var elapsed = 0L
                while (elapsed < durationMs) {
                    delay(stepMs)
                    elapsed += stepMs
                    _uiState.update {
                        it.copy(playbackProgressInScene = (elapsed.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f))
                    }
                }
                audioManager.stopPlayback()
            }

            _uiState.update {
                it.copy(
                    isPlaybackPlaying = false,
                    playbackSpeakingCharId = null,
                    playbackProgressInScene = 1f
                )
            }
        }
    }

    // --- Video Export ---

    fun openExportDialog() {
        stopVoicePreview()
        _uiState.update {
            it.copy(
                showExportDialog = true,
                exportState = ExportState.Idle
            )
        }
    }

    fun dismissExportDialog() {
        if (_uiState.value.exportState is ExportState.Exporting) return
        _uiState.update {
            it.copy(
                showExportDialog = false,
                exportState = ExportState.Idle
            )
        }
    }

    fun startVideoExport() {
        val scenes = _uiState.value.scenes
        if (scenes.isEmpty()) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(exportState = ExportState.Exporting(0.05f, "ভিডিও এনকোডার প্রস্তুত হচ্ছে..."))
            }
            val result = videoExporter.exportStoryToMp4(scenes) { progress, status ->
                _uiState.update {
                    it.copy(exportState = ExportState.Exporting(progress, status))
                }
            }
            result.onSuccess { res ->
                _uiState.update {
                    it.copy(
                        exportState = ExportState.Success(
                            shareFilePath = res.shareFilePath,
                            galleryDisplayPath = res.galleryDisplayPath
                        )
                    )
                }
            }.onFailure { err ->
                _uiState.update {
                    it.copy(
                        exportState = ExportState.Error(
                            err.localizedMessage ?: "ভিডিও তৈরি করতে সমস্যা হয়েছে"
                        )
                    )
                }
            }
        }
    }

    // --- Helpers ---

    private fun updateCurrentScene(
        persistImmediately: Boolean = true,
        transform: (CartoonScene) -> CartoonScene
    ) {
        _uiState.update { state ->
            val idx = state.currentSceneIndex
            if (idx !in state.scenes.indices) return@update state
            val updatedScenes = state.scenes.mapIndexed { i, scene ->
                if (i == idx) transform(scene) else scene
            }
            state.copy(scenes = updatedScenes)
        }
        if (persistImmediately) scheduleSave()
    }

    private fun updateCharacter(
        characterId: String,
        persistImmediately: Boolean = true,
        transform: (StageCharacter) -> StageCharacter
    ) {
        updateCurrentScene(persistImmediately = persistImmediately) { scene ->
            scene.copy(
                characters = scene.characters.map { ch ->
                    if (ch.id == characterId) transform(ch) else ch
                }
            )
        }
    }

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(250L)
            repository.saveScenes(_uiState.value.scenes)
        }
    }

    override fun onCleared() {
        super.onCleared()
        recordingTimerJob?.cancel()
        storyPlaybackJob?.cancel()
        audioManager.release()
    }
}
