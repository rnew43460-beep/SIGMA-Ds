package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.data.model.CustomPack
import com.example.data.model.DefaultPartyContent
import com.example.data.model.GameType
import com.example.data.model.MatchMode
import com.example.data.model.SpyWordPair
import com.example.data.model.WordBombChallenge
import com.example.ui.components.AvatarBadge
import com.example.ui.theme.ArcadeBg
import com.example.ui.theme.ArcadeSurface
import com.example.ui.theme.ArcadeSurfaceHigh
import com.example.ui.theme.CanvasPalette
import com.example.ui.theme.CyberMint
import com.example.ui.theme.NeonCoral
import com.example.ui.theme.SkyPulse
import com.example.ui.theme.SunnyGold
import kotlinx.coroutines.delay

data class LocalStroke(
  val color: Color,
  val widthDp: Float,
  val normalizedPoints: List<Offset>,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoloGameScreen(
  gameType: GameType,
  customPack: CustomPack?,
  roundSeconds: Int,
  playerName: String,
  onRecordMatch: (
    gameType: GameType,
    mode: MatchMode,
    roomTitle: String,
    score: Long,
    coinsEarned: Long,
    xpEarned: Long,
    won: Boolean,
    playersCount: Long,
  ) -> Unit,
  onBack: () -> Unit,
) {
  Scaffold(
    containerColor = ArcadeBg,
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(gameType.displayName, style = MaterialTheme.typography.titleLarge)
            Text(
              text = customPack?.let { "Custom Pack: ${it.title}" } ?: "Party Bots Mode",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.secondary,
            )
          }
        },
        navigationIcon = {
          IconButton(onClick = onBack, modifier = Modifier.testTag("game_back_button")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = ArcadeSurface),
      )
    },
  ) { innerPadding ->
    Box(
      modifier = Modifier.fillMaxSize().padding(innerPadding),
      contentAlignment = Alignment.TopCenter,
    ) {
      Column(
        modifier = Modifier.widthIn(max = 680.dp).fillMaxSize().padding(16.dp),
      ) {
        when (gameType) {
          GameType.DRAW_GUESS ->
            DrawGuessSoloGame(
              customPack = customPack,
              roundSeconds = roundSeconds,
              onFinish = { score, won ->
                val coins = (score / 10L).coerceAtLeast(10L)
                val xp = (score / 5L).coerceAtLeast(15L)
                onRecordMatch(
                  GameType.DRAW_GUESS,
                  MatchMode.SOLO_BOTS,
                  customPack?.title ?: "Sketch Party",
                  score,
                  coins,
                  xp,
                  won,
                  4L,
                )
              },
              onBack = onBack,
            )
          GameType.WHO_IS_SPY ->
            WhoIsSpySoloGame(
              customPack = customPack,
              playerName = playerName,
              onFinish = { score, won ->
                val coins = if (won) 45L else 15L
                val xp = if (won) 80L else 30L
                onRecordMatch(
                  GameType.WHO_IS_SPY,
                  MatchMode.SOLO_BOTS,
                  customPack?.title ?: "Undercover Party",
                  score,
                  coins,
                  xp,
                  won,
                  4L,
                )
              },
              onBack = onBack,
            )
          GameType.WORD_BOMB ->
            WordBombSoloGame(
              customPack = customPack,
              onFinish = { score, won ->
                val coins = (score / 10L).coerceAtLeast(12L)
                val xp = (score / 6L).coerceAtLeast(20L)
                onRecordMatch(
                  GameType.WORD_BOMB,
                  MatchMode.SOLO_BOTS,
                  customPack?.title ?: "Word Bomb Blitz",
                  score,
                  coins,
                  xp,
                  won,
                  4L,
                )
              },
              onBack = onBack,
            )
        }
      }
    }
  }
}

@Composable
private fun DrawGuessSoloGame(
  customPack: CustomPack?,
  roundSeconds: Int,
  onFinish: (score: Long, won: Boolean) -> Unit,
  onBack: () -> Unit,
) {
  val wordPool = remember(customPack) {
    val custom = customPack?.items?.map { it.substringBefore("|").trim() }?.filter { it.isNotBlank() }
    if (!custom.isNullOrEmpty()) custom else DefaultPartyContent.drawWords
  }

  var roundIndex by remember { mutableIntStateOf(0) }
  var currentWord by remember { mutableStateOf(wordPool.random()) }
  var timeLeft by remember { mutableIntStateOf(roundSeconds.coerceIn(20, 90)) }
  var score by remember { mutableLongStateOf(0L) }
  var selectedColor by remember { mutableStateOf(CanvasPalette.first().second) }
  var strokeWidth by remember { mutableIntStateOf(10) }
  var canvasSize by remember { mutableStateOf(IntSize(1, 1)) }
  val strokes = remember { mutableStateListOf<LocalStroke>() }
  val activePoints = remember { mutableStateListOf<Offset>() }
  val botGuesses = remember { mutableStateListOf<String>() }
  var isGameFinished by remember { mutableStateOf(false) }
  var recorded by remember { mutableStateOf(false) }

  // Countdown + simulated party bot guessers reacting to stroke count
  LaunchedEffect(roundIndex, isGameFinished) {
    if (isGameFinished) return@LaunchedEffect
    timeLeft = roundSeconds.coerceIn(20, 90)
    botGuesses.clear()
    botGuesses.add("NovaBot: Ready to guess your sketch!")

    while (timeLeft > 0 && !isGameFinished) {
      delay(1000L)
      timeLeft -= 1
      if (strokes.size >= 2 && timeLeft % 4 == 0) {
        val bot = DefaultPartyContent.botNames.random()
        if (strokes.size >= 4 && (timeLeft <= roundSeconds - 8) && (0..2).random() == 0) {
          botGuesses.add("$bot guessed \"$currentWord\"! +120 pts")
          score += 120L + timeLeft * 3L
          delay(1100L)
          if (roundIndex >= 2) {
            isGameFinished = true
          } else {
            roundIndex += 1
            currentWord = wordPool.random()
            strokes.clear()
          }
          break
        } else {
          val wrongGuess = wordPool.filter { it != currentWord }.randomOrNull() ?: "Star"
          botGuesses.add("$bot: Is it $wrongGuess?")
        }
      }
    }
    if (timeLeft == 0 && !isGameFinished) {
      if (roundIndex >= 2) {
        isGameFinished = true
      } else {
        roundIndex += 1
        currentWord = wordPool.random()
        strokes.clear()
      }
    }
  }

  LaunchedEffect(isGameFinished) {
    if (isGameFinished && !recorded) {
      recorded = true
      onFinish(score, score >= 200L)
    }
  }

  if (isGameFinished) {
    GameSummaryCard(
      title = "Sketch Party Complete!",
      subtitle = if (score >= 200L) "Party Bots loved your drawings!" else "Keep sketching to boost your streak!",
      score = score,
      won = score >= 200L,
      onPlayAgain = {
        roundIndex = 0
        score = 0L
        strokes.clear()
        currentWord = wordPool.random()
        recorded = false
        isGameFinished = false
      },
      onBack = onBack,
    )
    return
  }

  Column(
    modifier = Modifier.fillMaxSize(),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    // Round & Word Prompt Header
    Surface(
      color = ArcadeSurfaceHigh,
      shape = RoundedCornerShape(18.dp),
      border = BorderStroke(1.dp, NeonCoral.copy(alpha = 0.4f)),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text("ROUND ${roundIndex + 1}/3 · DRAW THIS", style = MaterialTheme.typography.labelSmall, color = NeonCoral)
          Text(currentWord, style = MaterialTheme.typography.headlineSmall, color = Color.White)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
          Surface(color = SunnyGold.copy(alpha = 0.16f), shape = RoundedCornerShape(10.dp)) {
            Text(
              "$score pts",
              style = MaterialTheme.typography.labelLarge,
              color = SunnyGold,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            )
          }
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Timer, contentDescription = null, tint = CyberMint, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("${timeLeft}s", style = MaterialTheme.typography.titleMedium, color = CyberMint)
          }
        }
      }
    }

    // Drawing Canvas
    Surface(
      color = Color(0xFF191330),
      shape = RoundedCornerShape(22.dp),
      border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
      modifier = Modifier.fillMaxWidth().weight(1f),
    ) {
      Canvas(
        modifier =
          Modifier.fillMaxSize()
            .onSizeChanged { canvasSize = it }
            .pointerInput(roundIndex) {
              detectDragGestures(
                onDragStart = { offset ->
                  activePoints.clear()
                  activePoints.add(
                    Offset(
                      (offset.x / canvasSize.width.coerceAtLeast(1)).coerceIn(0f, 1f),
                      (offset.y / canvasSize.height.coerceAtLeast(1)).coerceIn(0f, 1f),
                    )
                  )
                },
                onDrag = { change, _ ->
                  change.consume()
                  val nx = (change.position.x / canvasSize.width.coerceAtLeast(1)).coerceIn(0f, 1f)
                  val ny = (change.position.y / canvasSize.height.coerceAtLeast(1)).coerceIn(0f, 1f)
                  activePoints.add(Offset(nx, ny))
                },
                onDragEnd = {
                  if (activePoints.isNotEmpty()) {
                    strokes.add(
                      LocalStroke(
                        color = selectedColor,
                        widthDp = strokeWidth.toFloat(),
                        normalizedPoints = activePoints.toList(),
                      )
                    )
                    activePoints.clear()
                  }
                },
              )
            }
            .testTag("draw_guess_canvas"),
      ) {
        fun drawPolyline(points: List<Offset>, color: Color, width: Float) {
          if (points.size == 1) {
            drawCircle(
              color = color,
              radius = width / 2f,
              center = Offset(points[0].x * size.width, points[0].y * size.height),
            )
            return
          }
          val path = Path()
          points.forEachIndexed { idx, p ->
            val sx = p.x * size.width
            val sy = p.y * size.height
            if (idx == 0) path.moveTo(sx, sy) else path.lineTo(sx, sy)
          }
          drawPath(
            path = path,
            color = color,
            style = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round),
          )
        }
        strokes.forEach { s -> drawPolyline(s.normalizedPoints, s.color, s.widthDp * density) }
        if (activePoints.isNotEmpty()) {
          drawPolyline(activePoints, selectedColor, strokeWidth * density)
        }
      }
    }

    // Brush & Colors Toolbar
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CanvasPalette.forEach { (_, color) ->
          val selected = selectedColor == color
          Box(
            modifier =
              Modifier.size(34.dp)
                .clip(CircleShape)
                .background(color)
                .border(if (selected) 2.5.dp else 1.dp, Color.White, CircleShape)
                .clickable { selectedColor = color }
          )
        }
      }
      Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        IconButton(onClick = { if (strokes.isNotEmpty()) strokes.removeAt(strokes.lastIndex) }) {
          Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
        }
        IconButton(onClick = { strokes.clear() }) {
          Icon(Icons.Filled.DeleteSweep, contentDescription = "Clear canvas")
        }
        OutlinedButton(
          onClick = {
            currentWord = wordPool.random()
            strokes.clear()
          },
          shape = RoundedCornerShape(10.dp),
        ) {
          Text("Skip")
        }
      }
    }

    // Live Bot Guess Feed
    Surface(
      color = ArcadeSurfaceHigh,
      shape = RoundedCornerShape(14.dp),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        botGuesses.takeLast(2).forEach { line ->
          Text(
            text = line,
            style = MaterialTheme.typography.bodySmall,
            color = if (line.contains("guessed")) CyberMint else MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}

@Composable
private fun WhoIsSpySoloGame(
  customPack: CustomPack?,
  playerName: String,
  onFinish: (score: Long, won: Boolean) -> Unit,
  onBack: () -> Unit,
) {
  val pairPool = remember(customPack) {
    val parsed =
      customPack?.items?.mapNotNull { raw ->
        val parts = raw.split("|")
        if (parts.size >= 2) SpyWordPair(parts[0].trim(), parts[1].trim(), customPack.title) else null
      }
    if (!parsed.isNullOrEmpty()) parsed else DefaultPartyContent.spyPairs
  }

  var pair by remember { mutableStateOf(pairPool.random()) }
  var spySeatIndex by remember { mutableIntStateOf((0..3).random()) }
  var wordRevealed by remember { mutableStateOf(false) }
  var phase by remember { mutableStateOf("CLUES") } // CLUES, VOTE, RESULT
  var votedSeatIndex by remember { mutableIntStateOf(-1) }
  var recorded by remember { mutableStateOf(false) }

  val seats = remember(playerName) {
    listOf(
      playerName.ifBlank { "You" },
      DefaultPartyContent.botNames[0],
      DefaultPartyContent.botNames[1],
      DefaultPartyContent.botNames[2],
    )
  }

  val botClues = remember(pair, spySeatIndex) {
    seats.mapIndexed { idx, name ->
      val isSpy = idx == spySeatIndex
      val wordUsed = if (isSpy) pair.spyWord else pair.civilianWord
      val hint =
        if (isSpy) "It's popular and a bit different from the usual pick."
        else "Classic ${pair.category.lowercase()} favorite — everyone knows it!"
      Triple(idx, name, if (idx == 0) "Your turn to observe & vote!" else "$hint (${wordUsed.length} letters)")
    }
  }

  val playerWon = votedSeatIndex == spySeatIndex
  val finalScore = if (playerWon) 320L else 100L

  LaunchedEffect(phase) {
    if (phase == "RESULT" && !recorded) {
      recorded = true
      onFinish(finalScore, playerWon)
    }
  }

  Column(
    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    // Secret Word Card
    Card(
      shape = RoundedCornerShape(22.dp),
      colors = CardDefaults.cardColors(containerColor = ArcadeSurfaceHigh),
      border = BorderStroke(1.5.dp, CyberMint.copy(alpha = 0.5f)),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text("SECRET ROLE CARD", style = MaterialTheme.typography.labelMedium, color = CyberMint)
        val myWord = if (spySeatIndex == 0) pair.spyWord else pair.civilianWord
        Text(
          text = if (wordRevealed) myWord else "Tap below to peek your word",
          style = MaterialTheme.typography.headlineMedium,
          color = if (wordRevealed) SunnyGold else MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
        )
        OutlinedButton(
          onClick = { wordRevealed = !wordRevealed },
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.testTag("spy_reveal_card_button"),
        ) {
          Icon(
            imageVector = if (wordRevealed) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(Modifier.width(6.dp))
          Text(if (wordRevealed) "Hide Secret Word" else "Reveal Secret Word")
        }
      }
    }

    if (phase != "RESULT") {
      Text("Table Clues (Spot the Spy!)", style = MaterialTheme.typography.titleLarge)
      botClues.forEach { (idx, name, clue) ->
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = ArcadeSurfaceHigh),
          border = BorderStroke(1.dp, if (votedSeatIndex == idx) NeonCoral else MaterialTheme.colorScheme.outlineVariant),
        ) {
          Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
              AvatarBadge(avatarIndex = idx, size = 38.dp)
              Spacer(Modifier.width(10.dp))
              Column {
                Text(name, style = MaterialTheme.typography.titleSmall)
                Text(clue, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
            if (idx != 0) {
              Button(
                onClick = {
                  votedSeatIndex = idx
                  phase = "RESULT"
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("spy_vote_seat_$idx"),
              ) {
                Text("Vote Spy")
              }
            }
          }
        }
      }
    } else {
      GameSummaryCard(
        title = if (playerWon) "Spy Unmasked!" else "The Spy Escaped!",
        subtitle = "Civilian word: ${pair.civilianWord} · Spy (${seats[spySeatIndex]}) word: ${pair.spyWord}",
        score = finalScore,
        won = playerWon,
        onPlayAgain = {
          pair = pairPool.random()
          spySeatIndex = (0..3).random()
          wordRevealed = false
          votedSeatIndex = -1
          recorded = false
          phase = "CLUES"
        },
        onBack = onBack,
      )
    }
  }
}

@Composable
private fun WordBombSoloGame(
  customPack: CustomPack?,
  onFinish: (score: Long, won: Boolean) -> Unit,
  onBack: () -> Unit,
) {
  val challenges = remember(customPack) {
    val parsed =
      customPack?.items?.mapNotNull { raw ->
        val parts = raw.split("|")
        if (parts.size >= 2) {
          val question = parts[0].trim()
          val answer = parts[1].trim()
          val distractors = listOf("Alpha", "Omega", "Nova").filter { !it.equals(answer, ignoreCase = true) }
          val opts = (listOf(answer) + distractors).shuffled()
          WordBombChallenge(
            prompt = question,
            hint = customPack.title,
            sampleAnswers = listOf(answer),
            options = opts,
            correctOptionIndex = opts.indexOf(answer),
          )
        } else null
      }
    if (!parsed.isNullOrEmpty()) parsed else DefaultPartyContent.wordBombChallenges
  }

  var questionIdx by remember { mutableIntStateOf(0) }
  var fuseSeconds by remember { mutableIntStateOf(12) }
  var score by remember { mutableLongStateOf(0L) }
  var streak by remember { mutableIntStateOf(0) }
  var feedback by remember { mutableStateOf<String?>(null) }
  var finished by remember { mutableStateOf(false) }
  var recorded by remember { mutableStateOf(false) }

  val currentList = remember(challenges) { challenges.shuffled().take(5) }
  val activeChallenge = currentList.getOrNull(questionIdx)

  LaunchedEffect(questionIdx, finished) {
    if (finished || activeChallenge == null) return@LaunchedEffect
    fuseSeconds = 12
    while (fuseSeconds > 0 && !finished) {
      delay(1000L)
      fuseSeconds -= 1
    }
    if (fuseSeconds == 0 && !finished) {
      streak = 0
      feedback = "BOOM! Time ran out!"
      delay(900L)
      feedback = null
      if (questionIdx >= currentList.lastIndex) {
        finished = true
      } else {
        questionIdx += 1
      }
    }
  }

  LaunchedEffect(finished) {
    if (finished && !recorded) {
      recorded = true
      onFinish(score, score >= 250L)
    }
  }

  if (finished || activeChallenge == null) {
    GameSummaryCard(
      title = if (score >= 250L) "Bomb Defused!" else "Fizzled Out!",
      subtitle = "Final Word Bomb Blitz score across ${currentList.size} rounds.",
      score = score,
      won = score >= 250L,
      onPlayAgain = {
        questionIdx = 0
        score = 0L
        streak = 0
        feedback = null
        recorded = false
        finished = false
      },
      onBack = onBack,
    )
    return
  }

  Column(
    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    Card(
      shape = RoundedCornerShape(22.dp),
      colors = CardDefaults.cardColors(containerColor = ArcadeSurfaceHigh),
      border = BorderStroke(1.5.dp, SunnyGold.copy(alpha = 0.5f)),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.LocalFireDepartment, contentDescription = null, tint = NeonCoral)
            Spacer(Modifier.width(6.dp))
            Text("FUSE: ${fuseSeconds}s", style = MaterialTheme.typography.titleMedium, color = NeonCoral)
          }
          Text("Streak x$streak · $score pts", style = MaterialTheme.typography.titleSmall, color = SunnyGold)
        }
        LinearProgressIndicator(
          progress = { (fuseSeconds / 12f).coerceIn(0f, 1f) },
          modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(6.dp)),
          color = if (fuseSeconds <= 4) NeonCoral else SunnyGold,
          trackColor = MaterialTheme.colorScheme.surface,
        )
        Text(
          text = "ROUND ${questionIdx + 1}/${currentList.size} · ${activeChallenge.hint}",
          style = MaterialTheme.typography.labelSmall,
          color = SkyPulse,
        )
        Text(
          text = activeChallenge.prompt,
          style = MaterialTheme.typography.headlineSmall,
          color = Color.White,
        )
      }
    }

    if (feedback != null) {
      Surface(
        color = if (feedback!!.contains("BOOM") || feedback!!.contains("Oops")) NeonCoral.copy(alpha = 0.2f) else CyberMint.copy(alpha = 0.2f),
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text(
          text = feedback!!,
          style = MaterialTheme.typography.titleMedium,
          color = Color.White,
          modifier = Modifier.padding(12.dp),
          textAlign = TextAlign.Center,
        )
      }
    }

    activeChallenge.options.forEachIndexed { idx, optionText ->
      Button(
        onClick = {
          if (idx == activeChallenge.correctOptionIndex) {
            streak += 1
            score += 80L + (streak * 20L) + (fuseSeconds * 4L)
            feedback = "Passed! +${80 + streak * 20} pts"
          } else {
            streak = 0
            feedback = "Oops! Correct: ${activeChallenge.options[activeChallenge.correctOptionIndex]}"
          }
          if (questionIdx >= currentList.lastIndex) {
            finished = true
          } else {
            questionIdx += 1
          }
        },
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().height(54.dp).testTag("bomb_option_$idx"),
      ) {
        Text(optionText, style = MaterialTheme.typography.titleMedium)
      }
    }
  }
}

@Composable
private fun GameSummaryCard(
  title: String,
  subtitle: String,
  score: Long,
  won: Boolean,
  onPlayAgain: () -> Unit,
  onBack: () -> Unit,
) {
  Card(
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(containerColor = ArcadeSurfaceHigh),
    border = BorderStroke(1.5.dp, if (won) CyberMint else SunnyGold),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Icon(
        imageVector = if (won) Icons.Filled.EmojiEvents else Icons.Filled.CheckCircle,
        contentDescription = null,
        tint = if (won) SunnyGold else SkyPulse,
        modifier = Modifier.size(54.dp),
      )
      Text(title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
      Surface(color = SunnyGold.copy(alpha = 0.18f), shape = RoundedCornerShape(14.dp)) {
        Text(
          text = "Score: $score pts · Saved to Club Stats!",
          style = MaterialTheme.typography.titleMedium,
          color = SunnyGold,
          modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
      }
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        OutlinedButton(
          onClick = onBack,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.weight(1f).testTag("summary_lobby_button"),
        ) {
          Text("Lobby")
        }
        Button(
          onClick = onPlayAgain,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.weight(1f).testTag("summary_replay_button"),
        ) {
          Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(Modifier.width(6.dp))
          Text("Play Again")
        }
      }
    }
  }
}
