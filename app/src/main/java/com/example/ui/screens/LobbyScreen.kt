package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.GameType
import com.example.data.model.PartyRoom
import com.example.data.model.UserProfile
import com.example.ui.components.ClubHeaderBar
import com.example.ui.components.gameIcon
import com.example.ui.theme.ArcadeSurface
import com.example.ui.theme.ArcadeSurfaceBright
import com.example.ui.theme.ArcadeSurfaceHigh
import com.example.ui.theme.CyberMint
import com.example.ui.theme.NeonCoral
import com.example.ui.theme.SkyPulse
import com.example.ui.theme.SunnyGold
import com.example.ui.viewmodel.UiState

@Composable
fun LobbyScreen(
  profile: UserProfile?,
  publicRoomsState: UiState<List<PartyRoom>>,
  joinedRoomsState: UiState<List<PartyRoom>>,
  onPlaySolo: (GameType) -> Unit,
  onOpenHostSheet: (GameType) -> Unit,
  onOpenJoinDialog: () -> Unit,
  onJoinOrEnterRoom: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val joinedRooms = (joinedRoomsState as? UiState.Success)?.data.orEmpty()
  val publicRooms = (publicRoomsState as? UiState.Success)?.data.orEmpty()
  val joinedIds = joinedRooms.map { it.id }.toSet()
  val openPublicRooms = publicRooms.filter { it.id !in joinedIds }

  Box(
    modifier = modifier.fillMaxSize(),
    contentAlignment = Alignment.TopCenter,
  ) {
    LazyColumn(
      modifier = Modifier.widthIn(max = 680.dp).fillMaxSize(),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      item {
        ClubHeaderBar(
          profile = profile,
          onJoinCodeClick = onOpenJoinDialog,
        )
      }

      item {
        HeroPartyBanner(
          onHostClick = { onOpenHostSheet(GameType.fromId(profile?.favoriteGame)) },
          onQuickPlayClick = { onPlaySolo(GameType.fromId(profile?.favoriteGame)) },
        )
      }

      item {
        Text(
          text = "Party Games",
          style = MaterialTheme.typography.headlineSmall,
          color = MaterialTheme.colorScheme.onBackground,
        )
      }

      item {
        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          items(GameType.entries) { game ->
            GameShowcaseCard(
              gameType = game,
              onPlaySolo = { onPlaySolo(game) },
              onHostRoom = { onOpenHostSheet(game) },
            )
          }
        }
      }

      if (joinedRooms.isNotEmpty()) {
        item {
          Text(
            text = "My Active Rooms",
            style = MaterialTheme.typography.headlineSmall,
          )
        }
        items(joinedRooms, key = { "joined_${it.id}" }) { room ->
          PartyRoomCard(
            room = room,
            isMember = true,
            onEnter = { onJoinOrEnterRoom(room.id) },
          )
        }
      }

      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = "Live Open Lobbies",
            style = MaterialTheme.typography.headlineSmall,
          )
          OutlinedButton(
            onClick = { onOpenHostSheet(GameType.DRAW_GUESS) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.testTag("lobby_host_button"),
          ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text("Host")
          }
        }
      }

      when (publicRoomsState) {
        is UiState.Loading -> {
          item {
            Box(
              modifier = Modifier.fillMaxWidth().padding(24.dp),
              contentAlignment = Alignment.Center,
            ) {
              CircularProgressIndicator()
            }
          }
        }
        is UiState.Error -> {
          item {
            Surface(
              color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
              shape = RoundedCornerShape(16.dp),
              modifier = Modifier.fillMaxWidth(),
            ) {
              Text(
                text = publicRoomsState.message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(14.dp),
              )
            }
          }
        }
        is UiState.Success -> {
          if (openPublicRooms.isEmpty()) {
            item {
              Surface(
                color = ArcadeSurfaceHigh.copy(alpha = 0.7f),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Column(
                  modifier = Modifier.padding(20.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Icon(
                    imageVector = Icons.Filled.Groups,
                    contentDescription = null,
                    tint = CyberMint,
                    modifier = Modifier.size(34.dp),
                  )
                  Text(
                    text = "No open lobbies right now",
                    style = MaterialTheme.typography.titleMedium,
                  )
                  Text(
                    text = "Tap Host to open a room and share your 6-letter code, or jump straight into Solo Party Bots!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          } else {
            items(openPublicRooms, key = { "pub_${it.id}" }) { room ->
              PartyRoomCard(
                room = room,
                isMember = false,
                onEnter = { onJoinOrEnterRoom(room.id) },
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun HeroPartyBanner(
  onHostClick: () -> Unit,
  onQuickPlayClick: () -> Unit,
) {
  Card(
    shape = RoundedCornerShape(26.dp),
    colors = CardDefaults.cardColors(containerColor = ArcadeSurface),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Box(modifier = Modifier.fillMaxWidth().height(190.dp)) {
      Image(
        painter = painterResource(id = R.drawable.img_party_hero),
        contentDescription = "Party Lounge Banner",
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
      )
      Box(
        modifier =
          Modifier.fillMaxSize()
            .background(
              Brush.horizontalGradient(
                colors = listOf(
                  Color(0xEE120E24),
                  Color(0xAA120E24),
                  Color(0x33120E24),
                )
              )
            )
      )
      Column(
        modifier = Modifier.fillMaxSize().padding(18.dp),
        verticalArrangement = Arrangement.SpaceBetween,
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Surface(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
            shape = RoundedCornerShape(10.dp),
          ) {
            Text(
              text = "CUSTOM EDITION",
              style = MaterialTheme.typography.labelSmall,
              color = SunnyGold,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
          }
          Text(
            text = "Start a Party!",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
          )
          Text(
            text = "Play instant bot matches or host live Firestore rooms.",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFE2DDF8),
            modifier = Modifier.widthIn(max = 240.dp),
          )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          Button(
            onClick = onHostClick,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.testTag("hero_host_room_button"),
          ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("Host Room", fontWeight = FontWeight.Bold)
          }
          OutlinedButton(
            onClick = onQuickPlayClick,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(
              containerColor = Color(0x66120E24),
              contentColor = Color.White,
            ),
            modifier = Modifier.testTag("hero_quick_play_button"),
          ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("Quick Play")
          }
        }
      }
    }
  }
}

@Composable
private fun GameShowcaseCard(
  gameType: GameType,
  onPlaySolo: () -> Unit,
  onHostRoom: () -> Unit,
) {
  val (artRes, accent) =
    when (gameType) {
      GameType.DRAW_GUESS -> R.drawable.img_game_draw to NeonCoral
      GameType.WHO_IS_SPY -> R.drawable.img_game_spy to CyberMint
      GameType.WORD_BOMB -> R.drawable.img_game_bomb to SunnyGold
    }

  Card(
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.cardColors(containerColor = ArcadeSurfaceHigh),
    border = BorderStroke(1.dp, accent.copy(alpha = 0.4f)),
    modifier = Modifier.width(248.dp),
  ) {
    Column {
      Box(modifier = Modifier.fillMaxWidth().height(126.dp)) {
        Image(
          painter = painterResource(id = artRes),
          contentDescription = gameType.displayName,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)),
        )
        Surface(
          color = Color(0xBB120E24),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
        ) {
          Text(
            text = "${gameType.minPlayers}-${gameType.maxPlayers}P",
            style = MaterialTheme.typography.labelSmall,
            color = accent,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
          )
        }
      }

      Column(
        modifier = Modifier.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = gameIcon(gameType),
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(18.dp),
          )
          Spacer(Modifier.width(6.dp))
          Text(
            text = gameType.displayName,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        Text(
          text = gameType.tagline,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          minLines = 2,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Button(
            onClick = onPlaySolo,
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            modifier = Modifier.weight(1f).testTag("play_solo_${gameType.id}"),
          ) {
            Text("Play", style = MaterialTheme.typography.labelLarge)
          }
          OutlinedButton(
            onClick = onHostRoom,
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            modifier = Modifier.weight(1f).testTag("host_card_${gameType.id}"),
          ) {
            Text("Host", style = MaterialTheme.typography.labelLarge)
          }
        }
      }
    }
  }
}

@Composable
private fun PartyRoomCard(
  room: PartyRoom,
  isMember: Boolean,
  onEnter: () -> Unit,
) {
  val game = GameType.fromId(room.gameType)
  val accent =
    when (game) {
      GameType.DRAW_GUESS -> NeonCoral
      GameType.WHO_IS_SPY -> CyberMint
      GameType.WORD_BOMB -> SunnyGold
    }

  Card(
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = ArcadeSurfaceBright.copy(alpha = 0.72f)),
    border = BorderStroke(1.dp, if (isMember) accent else MaterialTheme.colorScheme.outlineVariant),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Icon(
            imageVector = gameIcon(game),
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(16.dp),
          )
          Text(
            text = room.title,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Surface(
            color = accent.copy(alpha = 0.18f),
            shape = RoundedCornerShape(8.dp),
          ) {
            Text(
              text = room.id,
              style = MaterialTheme.typography.labelSmall,
              color = accent,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            )
          }
          Icon(
            imageVector = if (room.isPublic) Icons.Filled.Public else Icons.Filled.Lock,
            contentDescription = if (room.isPublic) "Public" else "Private",
            tint = SkyPulse,
            modifier = Modifier.size(14.dp),
          )
          Text(
            text = "${room.memberUids.size}/${room.maxPlayers} · Host: ${room.hostName}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }

      Spacer(Modifier.width(10.dp))

      Button(
        onClick = onEnter,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.testTag("enter_room_${room.id}"),
      ) {
        Text(if (isMember) "Enter" else "Join")
      }
    }
  }
}
