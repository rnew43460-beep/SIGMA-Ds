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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.data.model.CanvasStroke
import com.example.data.model.CustomPack
import com.example.data.model.DefaultPartyContent
import com.example.data.model.GameType
import com.example.data.model.MatchMode
import com.example.data.model.MessageType
import com.example.data.model.PartyRoom
import com.example.data.model.RoomMessage
import com.example.data.model.RoomStatus
import com.example.ui.theme.ArcadeBg
import com.example.ui.theme.ArcadeSurface
import com.example.ui.theme.ArcadeSurfaceHigh
import com.example.ui.theme.CanvasPalette
import com.example.ui.theme.CyberMint
import com.example.ui.theme.NeonCoral
import com.example.ui.theme.SkyPulse
import com.example.ui.theme.SunnyGold

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RoomScreen(
  room: PartyRoom?,
  currentUserId: String,
  messages: List<RoomMessage>,
  strokes: List<CanvasStroke>,
  customPacks: List<CustomPack>,
  onLeaveRoom: (PartyRoom) -> Unit,
  onUpdateHostState: (roomId: String, gameType: GameType, status: RoomStatus, round: Long, prompt: String) -> Unit,
  onSendMessage: (PartyRoom, String, MessageType) -> Unit,
  onAddStroke: (PartyRoom, String, Long, String) -> Unit,
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
  onNotify: (String) -> Unit,
  onBack: () -> Unit,
) {
  val clipboardManager = LocalClipboardManager.current
  var chatInput by remember { mutableStateOf("") }
  var selectedColorPair by remember { mutableStateOf(CanvasPalette.first()) }
  var canvasSize by remember { mutableStateOf(IntSize(1, 1)) }
  val activeDragPoints = remember { mutableStateListOf<Offset>() }

  Scaffold(
    containerColor = ArcadeBg,
    topBar = {
      TopAppBar(
        title = {
          Column {
            Text(room?.title ?: "Party Room", style = MaterialTheme.typography.titleLarge)
            if (room != null) {
              Text(
                text = "Code: ${room.id} · ${room.memberUids.size}/${room.maxPlayers} players",
                style = MaterialTheme.typography.labelSmall,
                color = CyberMint,
              )
            }
          }
        },
        navigationIcon = {
          IconButton(onClick = onBack, modifier = Modifier.testTag("room_back_button")) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to lobby")
          }
        },
        actions = {
          if (room != null) {
            IconButton(
              onClick = {
                clipboardManager.setText(AnnotatedString(room.id))
                onNotify("Room code ${room.id} copied!")
              },
              modifier = Modifier.testTag("room_copy_code_button"),
            ) {
              Icon(Icons.Filled.ContentCopy, contentDescription = "Copy room code", tint = SunnyGold)
            }
            OutlinedButton(
              onClick = { onLeaveRoom(room) },
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.padding(end = 8.dp).testTag("room_leave_button"),
            ) {
              Text(if (room.hostId == currentUserId) "Close" else "Leave")
            }
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = ArcadeSurface),
      )
    },
  ) { innerPadding ->
    if (room == null) {
      Box(
        modifier = Modifier.fillMaxSize().padding(innerPadding),
        contentAlignment = Alignment.Center,
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
          CircularProgressIndicator()
          Text("Connecting to live party room...")
        }
      }
      return@Scaffold
    }

    val isHost = room.hostId == currentUserId
    val game = GameType.fromId(room.gameType)
    val status = RoomStatus.fromId(room.status)

    Box(
      modifier = Modifier.fillMaxSize().padding(innerPadding).imePadding(),
      contentAlignment = Alignment.TopCenter,
    ) {
      Column(
        modifier = Modifier.widthIn(max = 680.dp).fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        // Player Roster Strip
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          room.memberNames.forEach { memberTag ->
            val cleanName = memberTag.substringBefore("#")
            Surface(
              color = ArcadeSurfaceHigh,
              shape = RoundedCornerShape(12.dp),
              border = BorderStroke(1.dp, SkyPulse.copy(alpha = 0.4f)),
            ) {
              Text(
                text = cleanName,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
              )
            }
          }
        }

        // Host Control Bar
        if (isHost && status != RoomStatus.FINISHED) {
          Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = ArcadeSurfaceHigh),
            border = BorderStroke(1.dp, SunnyGold.copy(alpha = 0.45f)),
          ) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(12.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Text("Host Controls", style = MaterialTheme.typography.titleSmall, color = SunnyGold)
                Text("Round ${room.currentRound}", style = MaterialTheme.typography.labelMedium, color = CyberMint)
              }

              FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                GameType.entries.forEach { option ->
                  FilterChip(
                    selected = game == option,
                    onClick = {
                      onUpdateHostState(room.id, option, status, room.currentRound, room.activePrompt)
                    },
                    label = { Text(option.displayName) },
                  )
                }
              }

              Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                  onClick = {
                    val prompt = pickRoomPrompt(game, customPacks)
                    val nextRound = room.currentRound + 1L
                    onUpdateHostState(room.id, game, RoomStatus.PLAYING, nextRound, prompt)
                    onSendMessage(
                      room,
                      "Round $nextRound started (${game.displayName})!",
                      MessageType.SYSTEM,
                    )
                  },
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier.weight(1f).testTag("room_start_round_button"),
                ) {
                  Icon(
                    imageVector = if (status == RoomStatus.WAITING) Icons.Filled.PlayArrow else Icons.Filled.SkipNext,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                  )
                  Spacer(Modifier.width(4.dp))
                  Text(if (status == RoomStatus.WAITING) "Start Match" else "Next Round")
                }

                OutlinedButton(
                  onClick = {
                    onUpdateHostState(room.id, game, RoomStatus.FINISHED, room.currentRound, room.activePrompt)
                    onSendMessage(room, "Match finished! Great party everyone!", MessageType.SYSTEM)
                    onRecordMatch(
                      game,
                      MatchMode.MULTIPLAYER_ROOM,
                      room.title,
                      250L + (room.currentRound * 50L),
                      50L,
                      90L,
                      true,
                      room.memberUids.size.toLong(),
                    )
                  },
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier.testTag("room_finish_match_button"),
                ) {
                  Icon(Icons.Filled.Flag, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(Modifier.width(4.dp))
                  Text("Finish")
                }
              }
            }
          }
        }

        // Live Game Stage
        if (status == RoomStatus.PLAYING) {
          when (game) {
            GameType.DRAW_GUESS -> {
              Surface(
                color = ArcadeSurfaceHigh,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, NeonCoral.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Row(
                  modifier = Modifier.padding(12.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Text(
                    text =
                      if (isHost) "Your secret word: ${room.activePrompt}"
                      else "Guess the host's live drawing in chat below!",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isHost) SunnyGold else CyberMint,
                  )
                }
              }

              // Synced Live Canvas
              Surface(
                color = Color(0xFF18122E),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth().height(210.dp),
              ) {
                val roundStrokes = strokes.filter { it.roundNumber == room.currentRound }
                Canvas(
                  modifier =
                    Modifier.fillMaxSize()
                      .onSizeChanged { canvasSize = it }
                      .then(
                        if (isHost) {
                          Modifier.pointerInput(room.id, room.currentRound) {
                            detectDragGestures(
                              onDragStart = { offset ->
                                activeDragPoints.clear()
                                activeDragPoints.add(
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
                                activeDragPoints.add(Offset(nx, ny))
                              },
                              onDragEnd = {
                                if (activeDragPoints.isNotEmpty()) {
                                  val encoded =
                                    activeDragPoints
                                      .take(120)
                                      .joinToString(";") { p ->
                                        "${(p.x * 1000).toInt()},${(p.y * 1000).toInt()}"
                                      }
                                  onAddStroke(room, selectedColorPair.first, 8L, encoded)
                                  activeDragPoints.clear()
                                }
                              },
                            )
                          }
                        } else Modifier
                      ),
                ) {
                  fun drawEncoded(encoded: String, color: Color, width: Float) {
                    val pts =
                      encoded.split(";").mapNotNull { token ->
                        val coords = token.split(",")
                        if (coords.size == 2) {
                          val x = coords[0].toFloatOrNull()?.div(1000f) ?: return@mapNotNull null
                          val y = coords[1].toFloatOrNull()?.div(1000f) ?: return@mapNotNull null
                          Offset(x * size.width, y * size.height)
                        } else null
                      }
                    if (pts.isEmpty()) return
                    if (pts.size == 1) {
                      drawCircle(color = color, radius = width / 2f, center = pts[0])
                    } else {
                      val path = Path()
                      pts.forEachIndexed { idx, p -> if (idx == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y) }
                      drawPath(path, color = color, style = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                  }

                  roundStrokes.forEach { s ->
                    val parsedColor = CanvasPalette.firstOrNull { it.first == s.colorHex }?.second ?: NeonCoral
                    drawEncoded(s.pointsEncoded, parsedColor, s.strokeWidth.toFloat() * density)
                  }
                  if (activeDragPoints.size > 1) {
                    val path = Path()
                    activeDragPoints.forEachIndexed { idx, p ->
                      val sx = p.x * size.width
                      val sy = p.y * size.height
                      if (idx == 0) path.moveTo(sx, sy) else path.lineTo(sx, sy)
                    }
                    drawPath(
                      path,
                      color = selectedColorPair.second,
                      style = Stroke(width = 8f * density, cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                  }
                }
              }

              if (isHost) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  CanvasPalette.forEach { pair ->
                    Box(
                      modifier =
                        Modifier.size(30.dp)
                          .clip(CircleShape)
                          .background(pair.second)
                          .border(if (selectedColorPair == pair) 2.5.dp else 1.dp, Color.White, CircleShape)
                          .clickable { selectedColorPair = pair }
                    )
                  }
                }
              }
            }
            GameType.WHO_IS_SPY -> {
              val parts = room.activePrompt.split("|")
              val civilianWord = parts.getOrElse(0) { "Coffee" }
              val spyWord = parts.getOrElse(1) { "Espresso" }
              val isPlayerSpy = currentUserId.hashCode() % 2 != 0 && !isHost
              Surface(
                color = ArcadeSurfaceHigh,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, CyberMint.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                  Text("WHO'S THE SPY · YOUR WORD", style = MaterialTheme.typography.labelSmall, color = CyberMint)
                  Text(
                    text = if (isPlayerSpy) spyWord else civilianWord,
                    style = MaterialTheme.typography.headlineSmall,
                    color = SunnyGold,
                  )
                  Text(
                    "Describe your word in chat without giving it away, then vote!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
            GameType.WORD_BOMB -> {
              val parts = room.activePrompt.split("|")
              val question = parts.getOrElse(0) { "Name a red planet!" }
              Surface(
                color = ArcadeSurfaceHigh,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, SunnyGold.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                  Text("WORD BOMB BLITZ · FIRST TO ANSWER WINS", style = MaterialTheme.typography.labelSmall, color = SunnyGold)
                  Text(question, style = MaterialTheme.typography.titleLarge, color = Color.White)
                }
              }
            }
          }
        } else if (status == RoomStatus.FINISHED) {
          Surface(
            color = CyberMint.copy(alpha = 0.16f),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, CyberMint),
            modifier = Modifier.fillMaxWidth(),
          ) {
            Row(
              modifier = Modifier.padding(14.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = SunnyGold)
              Text("This party match has finished! Thanks for playing.", style = MaterialTheme.typography.titleSmall)
            }
          }
        }

        // Live Room Chat & Guesses
        LazyColumn(
          modifier = Modifier.weight(1f).fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          items(messages, key = { it.id }) { msg ->
            val isSystem = msg.msgType == MessageType.SYSTEM.id
            val isGuess = msg.msgType == MessageType.GUESS.id
            Surface(
              color =
                when {
                  isSystem -> CyberMint.copy(alpha = 0.14f)
                  isGuess -> SunnyGold.copy(alpha = 0.14f)
                  else -> ArcadeSurfaceHigh
                },
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
              ) {
                if (!isSystem) {
                  Text(
                    text = "${msg.senderName}:",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (msg.senderId == currentUserId) NeonCoral else SkyPulse,
                  )
                }
                Text(
                  text = msg.text,
                  style = MaterialTheme.typography.bodyMedium,
                  color = if (isSystem) CyberMint else Color.White,
                )
              }
            }
          }
        }

        // Chat / Guess Input Bar
        if (status != RoomStatus.FINISHED) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            OutlinedTextField(
              value = chatInput,
              onValueChange = { if (it.length <= 240) chatInput = it },
              placeholder = {
                Text(if (status == RoomStatus.PLAYING) "Type guess or chat..." else "Say hi to the party...")
              },
              singleLine = true,
              modifier = Modifier.weight(1f).testTag("room_chat_input"),
            )
            IconButton(
              onClick = {
                val msg = chatInput.trim()
                if (msg.isNotEmpty()) {
                  val isExactMatch =
                    status == RoomStatus.PLAYING &&
                      room.activePrompt.isNotBlank() &&
                      (msg.equals(room.activePrompt.substringBefore("|"), ignoreCase = true) ||
                        msg.equals(room.activePrompt.substringAfter("|"), ignoreCase = true))
                  val type = if (status == RoomStatus.PLAYING) MessageType.GUESS else MessageType.CHAT
                  onSendMessage(
                    room,
                    if (isExactMatch) "$msg (CORRECT!)" else msg,
                    type,
                  )
                  chatInput = ""
                }
              },
              modifier =
                Modifier.size(48.dp)
                  .clip(CircleShape)
                  .background(MaterialTheme.colorScheme.primary)
                  .testTag("room_send_button"),
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send",
                tint = MaterialTheme.colorScheme.onPrimary,
              )
            }
          }
        }
      }
    }
  }
}

private fun pickRoomPrompt(gameType: GameType, customPacks: List<CustomPack>): String {
  val matchingPack = customPacks.filter { it.gameType == gameType.id }.randomOrNull()
  if (matchingPack != null && matchingPack.items.isNotEmpty()) {
    return matchingPack.items.random()
  }
  return when (gameType) {
    GameType.DRAW_GUESS -> DefaultPartyContent.drawWords.random()
    GameType.WHO_IS_SPY -> {
      val pair = DefaultPartyContent.spyPairs.random()
      "${pair.civilianWord}|${pair.spyWord}"
    }
    GameType.WORD_BOMB -> {
      val c = DefaultPartyContent.wordBombChallenges.random()
      "${c.prompt}|${c.options[c.correctOptionIndex]}"
    }
  }
}
