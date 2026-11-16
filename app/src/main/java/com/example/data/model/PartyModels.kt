package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class GameType(
  val id: String,
  val displayName: String,
  val tagline: String,
  val minPlayers: Int,
  val maxPlayers: Int,
) {
  DRAW_GUESS(
    id = "DRAW_GUESS",
    displayName = "Draw & Guess",
    tagline = "Sketch neon clues before the timer runs out",
    minPlayers = 2,
    maxPlayers = 8,
  ),
  WHO_IS_SPY(
    id = "WHO_IS_SPY",
    displayName = "Who's the Spy",
    tagline = "Spot the undercover player without blowing your cover",
    minPlayers = 3,
    maxPlayers = 8,
  ),
  WORD_BOMB(
    id = "WORD_BOMB",
    displayName = "Word Bomb",
    tagline = "Beat the ticking fuse with fast trivia & word combos",
    minPlayers = 2,
    maxPlayers = 8,
  );

  companion object {
    fun fromId(id: String?): GameType = entries.firstOrNull { it.id == id } ?: DRAW_GUESS
  }
}

enum class RoomStatus(val id: String) {
  WAITING("WAITING"),
  PLAYING("PLAYING"),
  FINISHED("FINISHED");

  companion object {
    fun fromId(id: String?): RoomStatus = entries.firstOrNull { it.id == id } ?: WAITING
  }
}

enum class MatchMode(val id: String) {
  SOLO_BOTS("SOLO_BOTS"),
  MULTIPLAYER_ROOM("MULTIPLAYER_ROOM"),
}

enum class MessageType(val id: String) {
  CHAT("CHAT"),
  GUESS("GUESS"),
  SYSTEM("SYSTEM"),
}

@IgnoreExtraProperties
data class UserProfile(
  @DocumentId val id: String = "",
  val userId: String = "",
  val displayName: String = "PartyPlayer",
  val bio: String = "Ready for party games!",
  val avatarIndex: Long = 0L,
  val accentIndex: Long = 0L,
  val favoriteGame: String = GameType.DRAW_GUESS.id,
  val coins: Long = 100L,
  val xp: Long = 0L,
  val gamesPlayed: Long = 0L,
  val gamesWon: Long = 0L,
  val defaultRoundSeconds: Long = 45L,
  val defaultMaxPlayers: Long = 6L,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class MatchRecord(
  @DocumentId val id: String = "",
  val gameType: String = GameType.DRAW_GUESS.id,
  val mode: String = MatchMode.SOLO_BOTS.id,
  val roomTitle: String = "Party Session",
  val score: Long = 0L,
  val coinsEarned: Long = 0L,
  val xpEarned: Long = 0L,
  val won: Boolean = false,
  val playersCount: Long = 1L,
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class CustomPack(
  @DocumentId val id: String = "",
  val title: String = "",
  val gameType: String = GameType.DRAW_GUESS.id,
  val items: List<String> = emptyList(),
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class PartyRoom(
  @DocumentId val id: String = "",
  val hostId: String = "",
  val hostName: String = "",
  val title: String = "",
  val gameType: String = GameType.DRAW_GUESS.id,
  val status: String = RoomStatus.WAITING.id,
  val isPublic: Boolean = true,
  val roundSeconds: Long = 45L,
  val maxPlayers: Long = 6L,
  val currentRound: Long = 0L,
  val activePrompt: String = "",
  val memberUids: List<String> = emptyList(),
  val memberNames: List<String> = emptyList(),
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class RoomMessage(
  @DocumentId val id: String = "",
  val senderId: String = "",
  val senderName: String = "",
  val text: String = "",
  val msgType: String = MessageType.CHAT.id,
  val isPublicRoom: Boolean = true,
  val memberUids: List<String> = emptyList(),
  @ServerTimestamp val createdAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class CanvasStroke(
  @DocumentId val id: String = "",
  val artistId: String = "",
  val colorHex: String = "#FF4D6D",
  val strokeWidth: Long = 8L,
  val pointsEncoded: String = "",
  val roundNumber: Long = 0L,
  val isPublicRoom: Boolean = true,
  val memberUids: List<String> = emptyList(),
  @ServerTimestamp val createdAt: Timestamp? = null,
)

data class SpyWordPair(
  val civilianWord: String,
  val spyWord: String,
  val category: String = "Classic",
)

data class WordBombChallenge(
  val prompt: String,
  val hint: String,
  val sampleAnswers: List<String>,
  val options: List<String>,
  val correctOptionIndex: Int,
)

object DefaultPartyContent {
  val drawWords = listOf(
    "Rocket", "Pizza", "Penguin", "Rainbow", "Guitar", "Lighthouse",
    "Volcano", "Octopus", "Headphones", "Cupcake", "Astronaut", "Cactus",
    "Skateboard", "Dragon", "Treasure", "Campfire", "Telescope", "Butterfly",
  )

  val spyPairs = listOf(
    SpyWordPair("Coffee", "Espresso", "Drinks"),
    SpyWordPair("Guitar", "Violin", "Music"),
    SpyWordPair("Beach", "Swimming Pool", "Places"),
    SpyWordPair("Pizza", "Burger", "Food"),
    SpyWordPair("Laptop", "Tablet", "Tech"),
    SpyWordPair("Lion", "Tiger", "Animals"),
    SpyWordPair("Sneakers", "Boots", "Fashion"),
    SpyWordPair("Movie Theater", "Concert Hall", "Venues"),
    SpyWordPair("Snowboard", "Ski", "Sports"),
    SpyWordPair("Pancake", "Waffle", "Breakfast"),
  )

  val wordBombChallenges = listOf(
    WordBombChallenge(
      prompt = "Which planet is known as the Red Planet?",
      hint = "Space Blitz",
      sampleAnswers = listOf("Mars"),
      options = listOf("Venus", "Mars", "Jupiter", "Saturn"),
      correctOptionIndex = 1,
    ),
    WordBombChallenge(
      prompt = "Which instrument has 88 black and white keys?",
      hint = "Music Blitz",
      sampleAnswers = listOf("Piano"),
      options = listOf("Piano", "Harp", "Accordion", "Xylophone"),
      correctOptionIndex = 0,
    ),
    WordBombChallenge(
      prompt = "What gas do plants absorb from the air to grow?",
      hint = "Science Blitz",
      sampleAnswers = listOf("Carbon Dioxide"),
      options = listOf("Oxygen", "Nitrogen", "Carbon Dioxide", "Helium"),
      correctOptionIndex = 2,
    ),
    WordBombChallenge(
      prompt = "Which ocean is the largest on Earth?",
      hint = "Geography Blitz",
      sampleAnswers = listOf("Pacific"),
      options = listOf("Atlantic", "Indian", "Arctic", "Pacific"),
      correctOptionIndex = 3,
    ),
    WordBombChallenge(
      prompt = "In chess, which piece moves only diagonally?",
      hint = "Games Blitz",
      sampleAnswers = listOf("Bishop"),
      options = listOf("Rook", "Knight", "Bishop", "Pawn"),
      correctOptionIndex = 2,
    ),
    WordBombChallenge(
      prompt = "Which fruit is famous for having seeds on the outside?",
      hint = "Food Blitz",
      sampleAnswers = listOf("Strawberry"),
      options = listOf("Blueberry", "Strawberry", "Kiwi", "Mango"),
      correctOptionIndex = 1,
    ),
    WordBombChallenge(
      prompt = "How many sides does a hexagon have?",
      hint = "Quick Math",
      sampleAnswers = listOf("6", "Six"),
      options = listOf("5", "6", "7", "8"),
      correctOptionIndex = 1,
    ),
    WordBombChallenge(
      prompt = "Which country is home to Mount Fuji?",
      hint = "World Blitz",
      sampleAnswers = listOf("Japan"),
      options = listOf("South Korea", "Thailand", "Japan", "Nepal"),
      correctOptionIndex = 2,
    ),
  )

  val avatarLabels = listOf(
    "Neon Fox",
    "Cyber Cat",
    "Disco Panda",
    "Pixel Frog",
    "Astro Bear",
    "Turbo Duck",
    "Laser Owl",
    "Party Bunny",
  )

  val botNames = listOf("NovaBot", "PixelBot", "MochiBot", "TurboBot", "EchoBot")
}
