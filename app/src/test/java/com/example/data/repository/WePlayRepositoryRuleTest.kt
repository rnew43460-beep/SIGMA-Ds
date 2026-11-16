package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.GameType
import com.example.data.model.MatchMode
import com.example.data.model.MessageType
import com.example.data.model.RoomStatus
import com.google.firebase.firestore.FirebaseFirestoreException
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class WePlayRepositoryRuleTest : FirestoreEmulatorTestBase() {

  @Test
  fun ensureAndUpdateProfile_authenticatedOwner_succeeds() = runBlocking {
    val uid = signInTestUser(ALICE_EMAIL)
    val repo = WePlayRepository(firestore, auth)

    val profile = withTimeout(DEFAULT_TIMEOUT_MS) { repo.ensureUserProfile("Alice").getOrThrow() }
    assertEquals(uid, profile.userId)
    assertEquals("Alice", profile.displayName)

    val updateResult =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        repo.updateUserProfile(
          displayName = "AliceUpdated",
          bio = "Arcade champion",
          avatarIndex = 2L,
          accentIndex = 3L,
          favoriteGame = GameType.WHO_IS_SPY,
          defaultRoundSeconds = 60L,
          defaultMaxPlayers = 5L,
        )
      }
    assertTrue(updateResult.isSuccess)

    val observed = withTimeout(FLOW_TIMEOUT_MS) { repo.observeUserProfile(uid).first { it?.displayName == "AliceUpdated" } }
    assertNotNull(observed)
    assertEquals(GameType.WHO_IS_SPY.id, observed?.favoriteGame)
  }

  @Test
  fun customPacksAndMatches_ownerOnlyAndCrossUserDenied() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val aliceRepo = WePlayRepository(firestore, auth)
    withTimeout(DEFAULT_TIMEOUT_MS) { aliceRepo.ensureUserProfile("Alice").getOrThrow() }

    val packId =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        aliceRepo
          .saveCustomPack(
            title = "My Party Words",
            gameType = GameType.DRAW_GUESS,
            items = listOf("Spaceship", "Boba Tea"),
          )
          .getOrThrow()
      }
    val packs =
      withTimeout(FLOW_TIMEOUT_MS) {
        aliceRepo.observeCustomPacks(aliceUid).first { list -> list.any { it.id == packId } }
      }
    assertTrue(packs.any { it.id == packId && it.title == "My Party Words" })

    val matchId =
      withTimeout(DEFAULT_TIMEOUT_MS) {
        aliceRepo
          .recordMatchResult(
            gameType = GameType.WORD_BOMB,
            mode = MatchMode.SOLO_BOTS,
            roomTitle = "Solo Blitz",
            score = 350L,
            coinsEarned = 35L,
            xpEarned = 70L,
            won = true,
            playersCount = 4L,
          )
          .getOrThrow()
      }
    val matches =
      withTimeout(FLOW_TIMEOUT_MS) {
        aliceRepo.observeMatchHistory(aliceUid).first { list -> list.any { it.id == matchId } }
      }
    assertTrue(matches.any { it.id == matchId && it.won })

    // Cross-user read on Alice's custom packs must fail with PERMISSION_DENIED
    signInTestUser(BOB_EMAIL)
    val bobRepo = WePlayRepository(firestore, auth)
    try {
      withTimeout(FLOW_TIMEOUT_MS) { bobRepo.observeCustomPacks(aliceUid).first() }
      fail("Expected PERMISSION_DENIED when Bob reads Alice's customPacks")
    } catch (e: Exception) {
      val firestoreEx = e.unwrapFirestoreException()
      assertNotNull("Expected nested FirebaseFirestoreException, got $e", firestoreEx)
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }
  }

  @Test
  fun multiplayerRoom_createJoinMessageStrokeAndIsolation() = runBlocking {
    val aliceUid = signInTestUser(ALICE_EMAIL)
    val aliceRepo = WePlayRepository(firestore, auth)
    val roomCode = "R" + UUID.randomUUID().toString().replace("-", "").take(7).uppercase()

    withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo
        .createRoom(
          title = "Secret Lounge",
          gameType = GameType.DRAW_GUESS,
          isPublic = false,
          roundSeconds = 45L,
          maxPlayers = 4L,
          hostDisplayName = "Alice",
          explicitRoomCode = roomCode,
        )
        .getOrThrow()
    }

    // Bob (not yet a member) cannot observe private room
    val bobUid = signInTestUser(BOB_EMAIL)
    val bobRepo = WePlayRepository(firestore, auth)
    try {
      withTimeout(FLOW_TIMEOUT_MS) { bobRepo.observeRoom(roomCode).first() }
      fail("Expected PERMISSION_DENIED when non-member Bob reads private room")
    } catch (e: Exception) {
      val firestoreEx = e.unwrapFirestoreException()
      assertNotNull("Expected nested FirebaseFirestoreException, got $e", firestoreEx)
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }

    // Bob joins via room code and then observes room & messages
    withTimeout(DEFAULT_TIMEOUT_MS) { bobRepo.joinRoomByCode(roomCode, "Bob").getOrThrow() }
    val joinedRoom =
      withTimeout(FLOW_TIMEOUT_MS) {
        bobRepo.observeRoom(roomCode).first { it != null && bobUid in it.memberUids }
      }
    assertNotNull(joinedRoom)
    assertTrue(aliceUid in joinedRoom!!.memberUids)

    withTimeout(DEFAULT_TIMEOUT_MS) {
      bobRepo
        .sendRoomMessage(
          roomId = roomCode,
          senderName = "Bob",
          text = "Rocket!",
          msgType = MessageType.GUESS,
          isPublicRoom = false,
          memberUids = joinedRoom.memberUids,
        )
        .getOrThrow()
    }

    val messages =
      withTimeout(FLOW_TIMEOUT_MS) {
        bobRepo
          .observeRoomMessages(roomCode, isPublicRoom = false, userId = bobUid)
          .first { list -> list.any { it.text == "Rocket!" } }
      }
    assertTrue(messages.any { it.text == "Rocket!" })

    // Host Alice updates room to PLAYING and posts a stroke
    signInTestUser(ALICE_EMAIL)
    withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo
        .updateRoomHostState(
          roomId = roomCode,
          gameType = GameType.DRAW_GUESS,
          status = RoomStatus.PLAYING,
          currentRound = 1L,
          activePrompt = "Rocket",
        )
        .getOrThrow()
    }
    withTimeout(DEFAULT_TIMEOUT_MS) {
      aliceRepo
        .addCanvasStroke(
          roomId = roomCode,
          colorHex = "#FF4D6D",
          strokeWidth = 8L,
          pointsEncoded = "100,100;250,300",
          roundNumber = 1L,
          isPublicRoom = false,
          memberUids = joinedRoom.memberUids,
        )
        .getOrThrow()
    }

    val strokes =
      withTimeout(FLOW_TIMEOUT_MS) {
        aliceRepo
          .observeRoomStrokes(roomCode, isPublicRoom = false, userId = aliceUid)
          .first { it.isNotEmpty() }
      }
    assertTrue(strokes.isNotEmpty())
  }

  @Test
  fun observePublicRooms_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
    auth.signOut()
    val repo = WePlayRepository(firestore, auth)

    try {
      withTimeout(FLOW_TIMEOUT_MS) { repo.observePublicRooms().first() }
      fail("Expected PERMISSION_DENIED when unauthenticated user lists public rooms")
    } catch (e: Exception) {
      val firestoreEx = e.unwrapFirestoreException()
      assertNotNull("Expected nested FirebaseFirestoreException, got $e", firestoreEx)
      assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreEx?.code)
    }
  }

  private companion object {
    const val ALICE_EMAIL = "alice@weplaytest.com"
    const val BOB_EMAIL = "bob@weplaytest.com"
    const val DEFAULT_TIMEOUT_MS = 5000L
    const val FLOW_TIMEOUT_MS = 3000L
  }
}
