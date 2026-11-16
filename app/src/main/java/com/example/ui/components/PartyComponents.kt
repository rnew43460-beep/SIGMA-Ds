package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.DefaultPartyContent
import com.example.data.model.GameType
import com.example.data.model.UserProfile
import com.example.ui.theme.ArcadeSurfaceBright
import com.example.ui.theme.ArcadeSurfaceHigh
import com.example.ui.theme.AvatarColors
import com.example.ui.theme.SunnyGold
import kotlin.math.roundToInt

private val AvatarIcons: List<ImageVector> = listOf(
  Icons.Filled.Pets,
  Icons.Filled.Face,
  Icons.Filled.AutoAwesome,
  Icons.Filled.Extension,
  Icons.Filled.RocketLaunch,
  Icons.Filled.SportsEsports,
  Icons.Filled.Bolt,
  Icons.Filled.Favorite,
)

fun gameIcon(gameType: GameType): ImageVector =
  when (gameType) {
    GameType.DRAW_GUESS -> Icons.Filled.Brush
    GameType.WHO_IS_SPY -> Icons.Filled.Visibility
    GameType.WORD_BOMB -> Icons.Filled.LocalFireDepartment
  }

@Composable
fun AvatarBadge(
  avatarIndex: Int,
  size: Dp = 44.dp,
  modifier: Modifier = Modifier,
) {
  val safeIdx = avatarIndex.coerceIn(0, AvatarColors.lastIndex)
  val bg = AvatarColors[safeIdx]
  val icon = AvatarIcons[safeIdx]
  Box(
    modifier =
      modifier
        .size(size)
        .clip(CircleShape)
        .background(
          Brush.linearGradient(
            colors = listOf(bg, bg.copy(alpha = 0.65f))
          )
        )
        .border(2.dp, Color.White.copy(alpha = 0.35f), CircleShape),
    contentAlignment = Alignment.Center,
  ) {
    Icon(
      imageVector = icon,
      contentDescription = DefaultPartyContent.avatarLabels[safeIdx],
      tint = Color.White,
      modifier = Modifier.size(size * 0.56f),
    )
  }
}

@Composable
fun ClubHeaderBar(
  profile: UserProfile?,
  onJoinCodeClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val level = ((profile?.xp ?: 0L) / 100L) + 1L
  Surface(
    modifier = modifier.fillMaxWidth(),
    color = ArcadeSurfaceHigh.copy(alpha = 0.92f),
    shape = RoundedCornerShape(22.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.weight(1f),
      ) {
        AvatarBadge(avatarIndex = (profile?.avatarIndex ?: 0L).toInt(), size = 42.dp)
        Spacer(Modifier.width(10.dp))
        Column {
          Text(
            text = profile?.displayName ?: "WePlay Club",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Filled.Star,
              contentDescription = "Level",
              tint = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
              text = "Lv.$level",
              style = MaterialTheme.typography.labelMedium,
              color = MaterialTheme.colorScheme.secondary,
            )
            Spacer(Modifier.width(10.dp))
            Icon(
              imageVector = Icons.Filled.MonetizationOn,
              contentDescription = "Coins",
              tint = SunnyGold,
              modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(3.dp))
            Text(
              text = "${profile?.coins ?: 100}",
              style = MaterialTheme.typography.labelMedium,
              color = SunnyGold,
            )
          }
        }
      }

      OutlinedButton(
        onClick = onJoinCodeClick,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.minimumInteractiveComponentSize().testTag("header_join_code_button"),
      ) {
        Icon(
          imageVector = Icons.Filled.Login,
          contentDescription = "Join by code",
          modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text("Join Code")
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HostRoomBottomSheet(
  initialGameType: GameType,
  defaultRoundSeconds: Long,
  defaultMaxPlayers: Long,
  onDismiss: () -> Unit,
  onCreateRoom: (title: String, gameType: GameType, isPublic: Boolean, roundSeconds: Long, maxPlayers: Long) -> Unit,
) {
  var title by remember { mutableStateOf("${initialGameType.displayName} Party") }
  var selectedGame by remember { mutableStateOf(initialGameType) }
  var isPublic by remember { mutableStateOf(true) }
  var roundSecSlider by remember { mutableFloatStateOf(defaultRoundSeconds.toFloat().coerceIn(20f, 120f)) }
  var maxPlayersSlider by remember { mutableFloatStateOf(defaultMaxPlayers.toFloat().coerceIn(2f, 8f)) }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surface,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Filled.Casino,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(8.dp))
        Text("Host Party Room", style = MaterialTheme.typography.headlineSmall)
      }

      OutlinedTextField(
        value = title,
        onValueChange = { if (it.length <= 50) title = it },
        label = { Text("Room Name") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("host_room_title_input"),
      )

      Text("Select Party Game", style = MaterialTheme.typography.titleSmall)
      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        GameType.entries.forEach { game ->
          FilterChip(
            selected = selectedGame == game,
            onClick = {
              selectedGame = game
              if (title.endsWith("Party")) {
                title = "${game.displayName} Party"
              }
            },
            label = { Text(game.displayName) },
            leadingIcon = {
              Icon(gameIcon(game), contentDescription = null, modifier = Modifier.size(16.dp))
            },
            modifier = Modifier.testTag("host_game_chip_${game.id}"),
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text("Public Lobby", style = MaterialTheme.typography.titleSmall)
          Text(
            text = if (isPublic) "Anyone in WePlay Club can join" else "Private — invite with 6-letter code",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        Switch(
          checked = isPublic,
          onCheckedChange = { isPublic = it },
          modifier = Modifier.testTag("host_public_switch"),
        )
      }

      Column {
        Text(
          text = "Round Timer: ${roundSecSlider.roundToInt()}s",
          style = MaterialTheme.typography.titleSmall,
        )
        Slider(
          value = roundSecSlider,
          onValueChange = { roundSecSlider = it },
          valueRange = 20f..120f,
          steps = 9,
        )
      }

      Column {
        Text(
          text = "Max Players: ${maxPlayersSlider.roundToInt()}",
          style = MaterialTheme.typography.titleSmall,
        )
        Slider(
          value = maxPlayersSlider,
          onValueChange = { maxPlayersSlider = it },
          valueRange = 2f..8f,
          steps = 5,
        )
      }

      Button(
        onClick = {
          onCreateRoom(
            title.ifBlank { "${selectedGame.displayName} Party" },
            selectedGame,
            isPublic,
            roundSecSlider.roundToInt().toLong(),
            maxPlayersSlider.roundToInt().toLong(),
          )
        },
        modifier = Modifier.fillMaxWidth().height(52.dp).testTag("create_room_submit_button"),
        shape = RoundedCornerShape(16.dp),
      ) {
        Icon(Icons.Filled.EmojiEvents, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Open Room", fontWeight = FontWeight.Bold)
      }
      Spacer(Modifier.height(16.dp))
    }
  }
}

@Composable
fun JoinRoomDialog(
  onDismiss: () -> Unit,
  onJoin: (String) -> Unit,
) {
  var code by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Join Party Room", style = MaterialTheme.typography.titleLarge) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          "Enter the 6-character room code shared by your host.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
          value = code,
          onValueChange = { input ->
            code = input.uppercase().filter { it.isLetterOrDigit() }.take(10)
          },
          label = { Text("Room Code") },
          placeholder = { Text("e.g. K7P9QM") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("join_room_code_input"),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onJoin(code) },
        enabled = code.length >= 4,
        modifier = Modifier.testTag("join_room_confirm_button"),
      ) {
        Text("Join")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    },
  )
}

@Composable
fun StatTile(
  label: String,
  value: String,
  accent: Color,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    color = ArcadeSurfaceBright.copy(alpha = 0.65f),
    shape = RoundedCornerShape(16.dp),
    border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = value,
        style = MaterialTheme.typography.titleLarge,
        color = accent,
      )
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
fun SelectablePill(
  text: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val bg = if (selected) MaterialTheme.colorScheme.primary else ArcadeSurfaceHigh
  val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
  Surface(
    modifier =
      modifier
        .clip(RoundedCornerShape(14.dp))
        .clickable(onClick = onClick)
        .minimumInteractiveComponentSize(),
    color = bg,
    shape = RoundedCornerShape(14.dp),
    border = BorderStroke(1.dp, if (selected) bg else MaterialTheme.colorScheme.outlineVariant),
  ) {
    Box(
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
      contentAlignment = Alignment.Center,
    ) {
      Text(text = text, style = MaterialTheme.typography.labelLarge, color = fg)
    }
  }
}
