package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CanvasStroke
import com.example.data.model.CustomPack
import com.example.data.model.GameType
import com.example.data.model.MatchMode
import com.example.data.model.MatchRecord
import com.example.data.model.MessageType
import com.example.data.model.PartyRoom
import com.example.data.model.RoomMessage
import com.example.data.model.RoomStatus
import com.example.data.model.UserProfile
import com.example.data.repository.WePlayRepository
import com.example.data.repository.unwrapFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
  data object Loading : UiState<Nothing>

  data class Success<T>(val data: T) : UiState<T>

  data class Error(val message: String) : UiState<Nothing>
}

class WePlayViewModel(
  private val repository: WePlayRepository,
  val currentUserId: String,
  private val defaultDisplayName: String?,
) : ViewModel() {

  private val _bannerMessage = MutableStateFlow<String?>(null)
  val bannerMessage: StateFlow<String?> = _bannerMessage.asStateFlow()

  private val _isBusy = MutableStateFlow(false)
  val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

  private val _activeRoomId = MutableStateFlow<String?>(null)

  init {
    viewModelScope.launch {
      repository.ensureUserProfile(defaultDisplayName).onFailure { err ->
        _bannerMessage.value = err.localizedMessage ?: "Failed to initialize profile"
      }
    }
  }

  val userProfile: StateFlow<UiState<UserProfile>> =
    repository
      .observeUserProfile(currentUserId)
      .map<UserProfile?, UiState<UserProfile>> { profile ->
        if (profile != null) UiState.Success(profile) else UiState.Loading
      }
      .catch { error ->
        val firestoreError = error.unwrapFirestoreException()
        if (firestoreError == null && error is CancellationException) throw error
        val root = firestoreError ?: error
        Log.w(TAG, "Error observing userProfile", root)
        emit(UiState.Error(root.message ?: "Failed to load profile"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = UiState.Loading,
      )

  val matchHistory: StateFlow<UiState<List<MatchRecord>>> =
    repository
      .observeMatchHistory(currentUserId)
      .map<List<MatchRecord>, UiState<List<MatchRecord>>> { UiState.Success(it) }
      .catch { error ->
        val firestoreError = error.unwrapFirestoreException()
        if (firestoreError == null && error is CancellationException) throw error
        val root = firestoreError ?: error
        Log.w(TAG, "Error observing matchHistory", root)
        emit(UiState.Error(root.message ?: "Failed to load matches"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = UiState.Loading,
      )

  val customPacks: StateFlow<UiState<List<CustomPack>>> =
    repository
      .observeCustomPacks(currentUserId)
      .map<List<CustomPack>, UiState<List<CustomPack>>> { UiState.Success(it) }
      .catch { error ->
        val firestoreError = error.unwrapFirestoreException()
        if (firestoreError == null && error is CancellationException) throw error
        val root = firestoreError ?: error
        Log.w(TAG, "Error observing customPacks", root)
        emit(UiState.Error(root.message ?: "Failed to load custom packs"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = UiState.Loading,
      )

  val publicRooms: StateFlow<UiState<List<PartyRoom>>> =
    repository
      .observePublicRooms()
      .map<List<PartyRoom>, UiState<List<PartyRoom>>> { UiState.Success(it) }
      .catch { error ->
        val firestoreError = error.unwrapFirestoreException()
        if (firestoreError == null && error is CancellationException) throw error
        val root = firestoreError ?: error
        Log.w(TAG, "Error observing publicRooms", root)
        emit(UiState.Error(root.message ?: "Failed to load party rooms"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = UiState.Loading,
      )

  val joinedRooms: StateFlow<UiState<List<PartyRoom>>> =
    repository
      .observeJoinedRooms(currentUserId)
      .map<List<PartyRoom>, UiState<List<PartyRoom>>> { UiState.Success(it) }
      .catch { error ->
        val firestoreError = error.unwrapFirestoreException()
        if (firestoreError == null && error is CancellationException) throw error
        val root = firestoreError ?: error
        Log.w(TAG, "Error observing joinedRooms", root)
        emit(UiState.Error(root.message ?: "Failed to load joined rooms"))
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = UiState.Loading,
      )

  @OptIn(ExperimentalCoroutinesApi::class)
  val activeRoom: StateFlow<PartyRoom?> =
    _activeRoomId
      .flatMapLatest { roomId ->
        if (roomId == null) flowOf(null)
        else
          repository.observeRoom(roomId).catch { error ->
            val firestoreError = error.unwrapFirestoreException()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing activeRoom", firestoreError ?: error)
            emit(null)
          }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = null,
      )

  @OptIn(ExperimentalCoroutinesApi::class)
  val activeRoomMessages: StateFlow<List<RoomMessage>> =
    combine(_activeRoomId, activeRoom) { roomId, room -> roomId to room }
      .flatMapLatest { (roomId, room) ->
        if (roomId == null || room == null) flowOf(emptyList())
        else
          repository.observeRoomMessages(roomId, room.isPublic, currentUserId).catch { error ->
            val firestoreError = error.unwrapFirestoreException()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing roomMessages", firestoreError ?: error)
            emit(emptyList())
          }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = emptyList(),
      )

  @OptIn(ExperimentalCoroutinesApi::class)
  val activeRoomStrokes: StateFlow<List<CanvasStroke>> =
    combine(_activeRoomId, activeRoom) { roomId, room -> roomId to room }
      .flatMapLatest { (roomId, room) ->
        if (roomId == null || room == null) flowOf(emptyList())
        else
          repository.observeRoomStrokes(roomId, room.isPublic, currentUserId).catch { error ->
            val firestoreError = error.unwrapFirestoreException()
            if (firestoreError == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing roomStrokes", firestoreError ?: error)
            emit(emptyList())
          }
      }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = emptyList(),
      )

  fun setActiveRoom(roomId: String?) {
    _activeRoomId.value = roomId
  }

  fun clearBanner() {
    _bannerMessage.value = null
  }

  fun showBanner(message: String) {
    _bannerMessage.value = message
  }

  fun updateProfile(
    displayName: String,
    bio: String,
    avatarIndex: Long,
    accentIndex: Long,
    favoriteGame: GameType,
    defaultRoundSeconds: Long,
    defaultMaxPlayers: Long,
  ) {
    viewModelScope.launch {
      _isBusy.value = true
      repository
        .updateUserProfile(
          displayName = displayName,
          bio = bio,
          avatarIndex = avatarIndex,
          accentIndex = accentIndex,
          favoriteGame = favoriteGame,
          defaultRoundSeconds = defaultRoundSeconds,
          defaultMaxPlayers = defaultMaxPlayers,
        )
        .onSuccess { _bannerMessage.value = "Club profile & house rules saved!" }
        .onFailure { _bannerMessage.value = it.localizedMessage ?: "Failed to save profile" }
      _isBusy.value = false
    }
  }

  fun saveCustomPack(
    packId: String? = null,
    title: String,
    gameType: GameType,
    items: List<String>,
  ) {
    viewModelScope.launch {
      _isBusy.value = true
      repository
        .saveCustomPack(packId, title, gameType, items)
        .onSuccess { _bannerMessage.value = "Custom pack \"$title\" saved!" }
        .onFailure { _bannerMessage.value = it.localizedMessage ?: "Failed to save pack" }
      _isBusy.value = false
    }
  }

  fun deleteCustomPack(packId: String) {
    viewModelScope.launch {
      repository
        .deleteCustomPack(packId)
        .onSuccess { _bannerMessage.value = "Custom pack deleted." }
        .onFailure { _bannerMessage.value = it.localizedMessage ?: "Failed to delete pack" }
    }
  }

  fun createRoom(
    title: String,
    gameType: GameType,
    isPublic: Boolean,
    roundSeconds: Long,
    maxPlayers: Long,
    onRoomCreated: (String) -> Unit,
  ) {
    val profileName =
      (userProfile.value as? UiState.Success)?.data?.displayName ?: defaultDisplayName ?: "Host"
    viewModelScope.launch {
      _isBusy.value = true
      repository
        .createRoom(
          title = title,
          gameType = gameType,
          isPublic = isPublic,
          roundSeconds = roundSeconds,
          maxPlayers = maxPlayers,
          hostDisplayName = profileName,
        )
        .onSuccess { code ->
          _activeRoomId.value = code
          onRoomCreated(code)
        }
        .onFailure { _bannerMessage.value = it.localizedMessage ?: "Could not create room" }
      _isBusy.value = false
    }
  }

  fun joinRoom(roomCode: String, onJoined: (String) -> Unit) {
    val profileName =
      (userProfile.value as? UiState.Success)?.data?.displayName ?: defaultDisplayName ?: "Player"
    val cleanCode = roomCode.trim().uppercase()
    val alreadyMember =
      (joinedRooms.value as? UiState.Success)?.data?.any { it.id == cleanCode } == true

    if (alreadyMember) {
      _activeRoomId.value = cleanCode
      onJoined(cleanCode)
      return
    }

    viewModelScope.launch {
      _isBusy.value = true
      repository
        .joinRoomByCode(cleanCode, profileName)
        .onSuccess { code ->
          _activeRoomId.value = code
          onJoined(code)
        }
        .onFailure { err ->
          _bannerMessage.value =
            if (err.message?.contains("NOT_FOUND") == true) {
              "Room code $cleanCode not found."
            } else {
              "Unable to join room $cleanCode (full, started, or invalid code)."
            }
        }
      _isBusy.value = false
    }
  }

  fun leaveRoom(room: PartyRoom, onLeft: () -> Unit) {
    val profileName =
      (userProfile.value as? UiState.Success)?.data?.displayName ?: defaultDisplayName ?: "Player"
    viewModelScope.launch {
      repository.leaveRoom(room, profileName).onFailure {
        _bannerMessage.value = it.localizedMessage ?: "Failed to leave room"
      }
      _activeRoomId.value = null
      onLeft()
    }
  }

  fun updateRoomHostState(
    roomId: String,
    gameType: GameType,
    status: RoomStatus,
    currentRound: Long,
    activePrompt: String,
  ) {
    viewModelScope.launch {
      repository
        .updateRoomHostState(roomId, gameType, status, currentRound, activePrompt)
        .onFailure { _bannerMessage.value = it.localizedMessage ?: "Failed to update room" }
    }
  }

  fun sendRoomMessage(room: PartyRoom, text: String, msgType: MessageType) {
    val clean = text.trim()
    if (clean.isBlank()) return
    val profileName =
      (userProfile.value as? UiState.Success)?.data?.displayName ?: defaultDisplayName ?: "Player"
    viewModelScope.launch {
      repository
        .sendRoomMessage(
          roomId = room.id,
          senderName = profileName,
          text = clean,
          msgType = msgType,
          isPublicRoom = room.isPublic,
          memberUids = room.memberUids,
        )
        .onFailure { _bannerMessage.value = it.localizedMessage ?: "Failed to send message" }
    }
  }

  fun addRoomStroke(
    room: PartyRoom,
    colorHex: String,
    strokeWidth: Long,
    pointsEncoded: String,
  ) {
    if (pointsEncoded.isBlank()) return
    viewModelScope.launch {
      repository
        .addCanvasStroke(
          roomId = room.id,
          colorHex = colorHex,
          strokeWidth = strokeWidth,
          pointsEncoded = pointsEncoded,
          roundNumber = room.currentRound,
          isPublicRoom = room.isPublic,
          memberUids = room.memberUids,
        )
        .onFailure { _bannerMessage.value = it.localizedMessage ?: "Failed to sync stroke" }
    }
  }

  fun recordMatch(
    gameType: GameType,
    mode: MatchMode,
    roomTitle: String,
    score: Long,
    coinsEarned: Long,
    xpEarned: Long,
    won: Boolean,
    playersCount: Long,
  ) {
    viewModelScope.launch {
      repository
        .recordMatchResult(
          gameType = gameType,
          mode = mode,
          roomTitle = roomTitle,
          score = score,
          coinsEarned = coinsEarned,
          xpEarned = xpEarned,
          won = won,
          playersCount = playersCount,
        )
        .onFailure { _bannerMessage.value = it.localizedMessage ?: "Failed to save match stats" }
    }
  }

  private companion object {
    const val TAG = "WePlayVM"
    const val STOP_TIMEOUT_MS = 5000L
  }
}
