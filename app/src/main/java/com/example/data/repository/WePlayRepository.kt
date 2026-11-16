package com.example.data.repository

import android.content.Context
import com.example.R
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
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class WePlayRepository(
  private val db: FirebaseFirestore,
  private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
) {
  constructor(
    context: Context
  ) : this(
    db =
      FirebaseFirestore.getInstance(
        context.applicationContext.getString(R.string.firestore_database_id)
      ),
    auth = FirebaseAuth.getInstance(),
  )

  private fun requireUserId(): String {
    return auth.currentUser?.uid
      ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
  }

  private fun memberTag(displayName: String, uid: String): String {
    val cleanName = displayName.trim().ifBlank { "Player" }.take(20)
    return "$cleanName#${uid.take(4)}"
  }

  // --- User Profile ---

  suspend fun ensureUserProfile(defaultDisplayName: String? = null): Result<UserProfile> {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid)
    return try {
      val snapshot = docRef.get().await()
      if (snapshot.exists()) {
        val existing =
          snapshot.toObject(
            UserProfile::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
        Result.success(existing ?: UserProfile(id = uid, userId = uid))
      } else {
        val safeName =
          (defaultDisplayName ?: auth.currentUser?.displayName ?: "PartyHost")
            .trim()
            .ifBlank { "PartyHost" }
            .take(30)
        val payload =
          mapOf(
            "userId" to uid,
            "displayName" to safeName,
            "bio" to "Ready for party games!",
            "avatarIndex" to 0L,
            "accentIndex" to 0L,
            "favoriteGame" to GameType.DRAW_GUESS.id,
            "coins" to 100L,
            "xp" to 0L,
            "gamesPlayed" to 0L,
            "gamesWon" to 0L,
            "defaultRoundSeconds" to 45L,
            "defaultMaxPlayers" to 6L,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        docRef.set(payload).await()
        val created =
          docRef
            .get()
            .await()
            .toObject(
              UserProfile::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            )
        Result.success(created ?: UserProfile(id = uid, userId = uid, displayName = safeName))
      }
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path, auth)
      Result.failure(e)
    }
  }

  fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
    val docRef = db.collection("users").document(userId)
    emitAll(
      docRef
        .snapshots()
        .map { snapshot ->
          if (snapshot.exists()) {
            snapshot.toObject(
              UserProfile::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            )
          } else {
            null
          }
        }
        .catch { error ->
          val firestoreError = error.unwrapFirestoreException()
          if (firestoreError == null && error is CancellationException) throw error
          val toLog = firestoreError ?: (error as? Exception)
          if (toLog != null && auth.currentUser?.uid == userId) {
            handleFirestoreError(toLog, OperationType.GET, docRef.path, auth)
          }
          throw error
        }
    )
  }

  suspend fun updateUserProfile(
    displayName: String,
    bio: String,
    avatarIndex: Long,
    accentIndex: Long,
    favoriteGame: GameType,
    defaultRoundSeconds: Long,
    defaultMaxPlayers: Long,
  ): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid)
    val payload =
      mapOf(
        "displayName" to displayName.trim().ifBlank { "PartyPlayer" }.take(30),
        "bio" to bio.trim().take(160),
        "avatarIndex" to avatarIndex.coerceIn(0L, 7L),
        "accentIndex" to accentIndex.coerceIn(0L, 4L),
        "favoriteGame" to favoriteGame.id,
        "defaultRoundSeconds" to defaultRoundSeconds.coerceIn(20L, 120L),
        "defaultMaxPlayers" to defaultMaxPlayers.coerceIn(2L, 8L),
        "updatedAt" to FieldValue.serverTimestamp(),
      )
    return try {
      docRef.update(payload).await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
      Result.failure(e)
    }
  }

  // --- Match History & Rewards ---

  fun observeMatchHistory(userId: String): Flow<List<MatchRecord>> = flow {
    val path = "users/$userId/matches"
    emitAll(
      db.collection("users")
        .document(userId)
        .collection("matches")
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(
              MatchRecord::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            )
            .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
        }
        .catch { error ->
          val firestoreError = error.unwrapFirestoreException()
          if (firestoreError == null && error is CancellationException) throw error
          val toLog = firestoreError ?: (error as? Exception)
          if (toLog != null && auth.currentUser?.uid == userId) {
            handleFirestoreError(toLog, OperationType.LIST, path, auth)
          }
          throw error
        }
    )
  }

  suspend fun recordMatchResult(
    gameType: GameType,
    mode: MatchMode,
    roomTitle: String,
    score: Long,
    coinsEarned: Long,
    xpEarned: Long,
    won: Boolean,
    playersCount: Long,
  ): Result<String> {
    val uid = requireUserId()
    val matchId = UUID.randomUUID().toString()
    val matchRef = db.collection("users").document(uid).collection("matches").document(matchId)
    val userRef = db.collection("users").document(uid)

    val safeScore = score.coerceIn(0L, 100000L)
    val safeCoins = coinsEarned.coerceIn(0L, 10000L)
    val safeXp = xpEarned.coerceIn(0L, 10000L)
    val safePlayers = playersCount.coerceIn(1L, 12L)

    val matchPayload =
      mapOf(
        "gameType" to gameType.id,
        "mode" to mode.id,
        "roomTitle" to roomTitle.trim().ifBlank { gameType.displayName }.take(60),
        "score" to safeScore,
        "coinsEarned" to safeCoins,
        "xpEarned" to safeXp,
        "won" to won,
        "playersCount" to safePlayers,
        "createdAt" to FieldValue.serverTimestamp(),
      )

    return try {
      matchRef.set(matchPayload).await()
      val userUpdates =
        mutableMapOf<String, Any>(
          "coins" to FieldValue.increment(safeCoins),
          "xp" to FieldValue.increment(safeXp),
          "gamesPlayed" to FieldValue.increment(1L),
          "updatedAt" to FieldValue.serverTimestamp(),
        )
      if (won) {
        userUpdates["gamesWon"] = FieldValue.increment(1L)
      }
      userRef.update(userUpdates).await()
      Result.success(matchId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, matchRef.path, auth)
      Result.failure(e)
    }
  }

  // --- Custom Party Packs ("My Own Version") ---

  fun observeCustomPacks(userId: String): Flow<List<CustomPack>> = flow {
    val path = "users/$userId/customPacks"
    emitAll(
      db.collection("users")
        .document(userId)
        .collection("customPacks")
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(
              CustomPack::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            )
            .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
        }
        .catch { error ->
          val firestoreError = error.unwrapFirestoreException()
          if (firestoreError == null && error is CancellationException) throw error
          val toLog = firestoreError ?: (error as? Exception)
          if (toLog != null && auth.currentUser?.uid == userId) {
            handleFirestoreError(toLog, OperationType.LIST, path, auth)
          }
          throw error
        }
    )
  }

  suspend fun saveCustomPack(
    packId: String? = null,
    title: String,
    gameType: GameType,
    items: List<String>,
  ): Result<String> {
    val uid = requireUserId()
    val cleanItems =
      items
        .map { it.trim().take(80) }
        .filter { it.isNotBlank() }
        .take(30)
    require(cleanItems.isNotEmpty()) { "Custom pack must include at least one item." }

    val targetId = packId?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
    val docRef = db.collection("users").document(uid).collection("customPacks").document(targetId)
    val cleanTitle = title.trim().ifBlank { "My Custom Pack" }.take(40)

    return try {
      if (packId.isNullOrBlank()) {
        val payload =
          mapOf(
            "title" to cleanTitle,
            "gameType" to gameType.id,
            "items" to cleanItems,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        docRef.set(payload).await()
      } else {
        val payload =
          mapOf(
            "title" to cleanTitle,
            "gameType" to gameType.id,
            "items" to cleanItems,
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        docRef.update(payload).await()
      }
      Result.success(targetId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.WRITE, docRef.path, auth)
      Result.failure(e)
    }
  }

  suspend fun deleteCustomPack(packId: String): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection("users").document(uid).collection("customPacks").document(packId)
    return try {
      docRef.delete().await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
      Result.failure(e)
    }
  }

  // --- Multiplayer Party Rooms ---

  fun observePublicRooms(): Flow<List<PartyRoom>> = flow {
    val path = "rooms"
    emitAll(
      db.collection("rooms")
        .whereEqualTo("isPublic", true)
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(
              PartyRoom::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            )
            .filter { it.status != RoomStatus.FINISHED.id }
            .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
        }
        .catch { error ->
          val firestoreError = error.unwrapFirestoreException()
          if (firestoreError == null && error is CancellationException) throw error
          val toLog = firestoreError ?: (error as? Exception)
          if (toLog != null && auth.currentUser != null) {
            handleFirestoreError(toLog, OperationType.LIST, path, auth)
          }
          throw error
        }
    )
  }

  fun observeJoinedRooms(userId: String): Flow<List<PartyRoom>> = flow {
    val path = "rooms"
    emitAll(
      db.collection("rooms")
        .whereArrayContains("memberUids", userId)
        .snapshots()
        .map { snapshot ->
          snapshot
            .toObjects(
              PartyRoom::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            )
            .sortedByDescending { it.updatedAt ?: it.createdAt ?: Timestamp(0, 0) }
        }
        .catch { error ->
          val firestoreError = error.unwrapFirestoreException()
          if (firestoreError == null && error is CancellationException) throw error
          val toLog = firestoreError ?: (error as? Exception)
          if (toLog != null && auth.currentUser?.uid == userId) {
            handleFirestoreError(toLog, OperationType.LIST, path, auth)
          }
          throw error
        }
    )
  }

  fun observeRoom(roomId: String): Flow<PartyRoom?> = flow {
    val docRef = db.collection("rooms").document(roomId)
    emitAll(
      docRef
        .snapshots()
        .map { snapshot ->
          if (snapshot.exists()) {
            snapshot.toObject(
              PartyRoom::class.java,
              DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            )
          } else {
            null
          }
        }
        .catch { error ->
          val firestoreError = error.unwrapFirestoreException()
          if (firestoreError == null && error is CancellationException) throw error
          val toLog = firestoreError ?: (error as? Exception)
          if (toLog != null && auth.currentUser != null) {
            handleFirestoreError(toLog, OperationType.GET, docRef.path, auth)
          }
          throw error
        }
    )
  }

  suspend fun createRoom(
    title: String,
    gameType: GameType,
    isPublic: Boolean,
    roundSeconds: Long,
    maxPlayers: Long,
    hostDisplayName: String,
    explicitRoomCode: String? = null,
  ): Result<String> {
    val uid = requireUserId()
    val roomCode = explicitRoomCode?.trim()?.uppercase() ?: generateRoomCode()
    val docRef = db.collection("rooms").document(roomCode)
    val cleanHostName = hostDisplayName.trim().ifBlank { "Host" }.take(30)
    val cleanTitle = title.trim().ifBlank { "${gameType.displayName} Party" }.take(50)

    val payload =
      mapOf(
        "hostId" to uid,
        "hostName" to cleanHostName,
        "title" to cleanTitle,
        "gameType" to gameType.id,
        "status" to RoomStatus.WAITING.id,
        "isPublic" to isPublic,
        "roundSeconds" to roundSeconds.coerceIn(20L, 120L),
        "maxPlayers" to maxPlayers.coerceIn(2L, 8L),
        "currentRound" to 0L,
        "activePrompt" to "",
        "memberUids" to listOf(uid),
        "memberNames" to listOf(memberTag(cleanHostName, uid)),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp(),
      )

    return try {
      docRef.set(payload).await()
      sendRoomMessage(
        roomId = roomCode,
        senderName = cleanHostName,
        text = "$cleanHostName opened the party room!",
        msgType = MessageType.SYSTEM,
        isPublicRoom = isPublic,
        memberUids = listOf(uid),
      )
      Result.success(roomCode)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
      Result.failure(e)
    }
  }

  suspend fun joinRoomByCode(roomCode: String, displayName: String): Result<String> {
    val uid = requireUserId()
    val cleanCode = roomCode.trim().uppercase()
    if (!cleanCode.matches(Regex("^[A-Za-z0-9_\\-]{4,36}$"))) {
      return Result.failure(IllegalArgumentException("Enter a valid 6-character room code."))
    }
    val docRef = db.collection("rooms").document(cleanCode)
    val tag = memberTag(displayName, uid)
    return try {
      docRef
        .update(
          mapOf(
            "memberUids" to FieldValue.arrayUnion(uid),
            "memberNames" to FieldValue.arrayUnion(tag),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      val room =
        docRef
          .get()
          .await()
          .toObject(
            PartyRoom::class.java,
            DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
          )
      if (room != null) {
        sendRoomMessage(
          roomId = cleanCode,
          senderName = displayName.trim().ifBlank { "Player" }.take(30),
          text = "${displayName.trim().ifBlank { "Player" }} joined the party!",
          msgType = MessageType.SYSTEM,
          isPublicRoom = room.isPublic,
          memberUids = room.memberUids,
        )
      }
      Result.success(cleanCode)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
      Result.failure(e)
    }
  }

  suspend fun leaveRoom(room: PartyRoom, displayName: String): Result<Unit> {
    val uid = requireUserId()
    val docRef = db.collection("rooms").document(room.id)
    val tag = memberTag(displayName, uid)
    return try {
      if (room.hostId == uid) {
        docRef.delete().await()
      } else {
        docRef
          .update(
            mapOf(
              "memberUids" to FieldValue.arrayRemove(uid),
              "memberNames" to FieldValue.arrayRemove(tag),
              "updatedAt" to FieldValue.serverTimestamp(),
            )
          )
          .await()
      }
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
      Result.failure(e)
    }
  }

  suspend fun updateRoomHostState(
    roomId: String,
    gameType: GameType,
    status: RoomStatus,
    currentRound: Long,
    activePrompt: String,
  ): Result<Unit> {
    requireUserId()
    val docRef = db.collection("rooms").document(roomId)
    return try {
      docRef
        .update(
          mapOf(
            "gameType" to gameType.id,
            "status" to status.id,
            "currentRound" to currentRound.coerceIn(0L, 20L),
            "activePrompt" to activePrompt.take(120),
            "updatedAt" to FieldValue.serverTimestamp(),
          )
        )
        .await()
      Result.success(Unit)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.UPDATE, docRef.path, auth)
      Result.failure(e)
    }
  }

  // --- Room Chat & Canvas Strokes ---

  fun observeRoomMessages(roomId: String, isPublicRoom: Boolean, userId: String): Flow<List<RoomMessage>> =
    flow {
      val path = "rooms/$roomId/messages"
      val colRef = db.collection("rooms").document(roomId).collection("messages")
      val query =
        if (isPublicRoom) {
          colRef.whereEqualTo("isPublicRoom", true)
        } else {
          colRef.whereArrayContains("memberUids", userId)
        }
      emitAll(
        query
          .snapshots()
          .map { snapshot ->
            snapshot
              .toObjects(
                RoomMessage::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
              )
              .sortedBy { it.createdAt ?: Timestamp(0, 0) }
          }
          .catch { error ->
            val firestoreError = error.unwrapFirestoreException()
            if (firestoreError == null && error is CancellationException) throw error
            val toLog = firestoreError ?: (error as? Exception)
            if (toLog != null && auth.currentUser != null) {
              handleFirestoreError(toLog, OperationType.LIST, path, auth)
            }
            throw error
          }
      )
    }

  suspend fun sendRoomMessage(
    roomId: String,
    senderName: String,
    text: String,
    msgType: MessageType,
    isPublicRoom: Boolean,
    memberUids: List<String>,
  ): Result<String> {
    val uid = requireUserId()
    val msgId = UUID.randomUUID().toString()
    val docRef = db.collection("rooms").document(roomId).collection("messages").document(msgId)
    val safeMembers = memberUids.ifEmpty { listOf(uid) }.take(8)
    val payload =
      mapOf(
        "senderId" to uid,
        "senderName" to senderName.trim().ifBlank { "Player" }.take(30),
        "text" to text.trim().take(240),
        "msgType" to msgType.id,
        "isPublicRoom" to isPublicRoom,
        "memberUids" to safeMembers,
        "createdAt" to FieldValue.serverTimestamp(),
      )
    return try {
      docRef.set(payload).await()
      Result.success(msgId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
      Result.failure(e)
    }
  }

  fun observeRoomStrokes(roomId: String, isPublicRoom: Boolean, userId: String): Flow<List<CanvasStroke>> =
    flow {
      val path = "rooms/$roomId/strokes"
      val colRef = db.collection("rooms").document(roomId).collection("strokes")
      val query =
        if (isPublicRoom) {
          colRef.whereEqualTo("isPublicRoom", true)
        } else {
          colRef.whereArrayContains("memberUids", userId)
        }
      emitAll(
        query
          .snapshots()
          .map { snapshot ->
            snapshot
              .toObjects(
                CanvasStroke::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
              )
              .sortedBy { it.createdAt ?: Timestamp(0, 0) }
          }
          .catch { error ->
            val firestoreError = error.unwrapFirestoreException()
            if (firestoreError == null && error is CancellationException) throw error
            val toLog = firestoreError ?: (error as? Exception)
            if (toLog != null && auth.currentUser != null) {
              handleFirestoreError(toLog, OperationType.LIST, path, auth)
            }
            throw error
          }
      )
    }

  suspend fun addCanvasStroke(
    roomId: String,
    colorHex: String,
    strokeWidth: Long,
    pointsEncoded: String,
    roundNumber: Long,
    isPublicRoom: Boolean,
    memberUids: List<String>,
  ): Result<String> {
    val uid = requireUserId()
    val strokeId = UUID.randomUUID().toString()
    val docRef = db.collection("rooms").document(roomId).collection("strokes").document(strokeId)
    val safeMembers = memberUids.ifEmpty { listOf(uid) }.take(8)
    val payload =
      mapOf(
        "artistId" to uid,
        "colorHex" to colorHex.take(10),
        "strokeWidth" to strokeWidth.coerceIn(2L, 32L),
        "pointsEncoded" to pointsEncoded.take(4000),
        "roundNumber" to roundNumber.coerceIn(0L, 20L),
        "isPublicRoom" to isPublicRoom,
        "memberUids" to safeMembers,
        "createdAt" to FieldValue.serverTimestamp(),
      )
    return try {
      docRef.set(payload).await()
      Result.success(strokeId)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
      Result.failure(e)
    }
  }

  private fun generateRoomCode(): String {
    val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    return (1..6).map { chars.random() }.joinToString("")
  }
}
