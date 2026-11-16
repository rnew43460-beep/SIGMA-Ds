package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.SportsScore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.DefaultPartyContent
import com.example.data.model.GameType
import com.example.data.model.MatchMode
import com.example.data.model.MatchRecord
import com.example.data.model.UserProfile
import com.example.ui.components.AvatarBadge
import com.example.ui.components.StatTile
import com.example.ui.components.gameIcon
import com.example.ui.theme.ArcadeSurfaceHigh
import com.example.ui.theme.CyberMint
import com.example.ui.theme.NeonCoral
import com.example.ui.theme.SkyPulse
import com.example.ui.theme.SunnyGold
import com.example.ui.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun HistoryScreen(
  profile: UserProfile?,
  matchesState: UiState<List<MatchRecord>>,
  modifier: Modifier = Modifier,
) {
  val xp = profile?.xp ?: 0L
  val level = (xp / 100L) + 1L
  val levelProgress = ((xp % 100L) / 100f).coerceIn(0f, 1f)
  val played = profile?.gamesPlayed ?: 0L
  val won = profile?.gamesWon ?: 0L
  val winRate = if (played > 0L) ((won * 100L) / played) else 0L

  val dateFormat = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }

  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.widthIn(max = 680.dp).fillMaxSize(),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      item {
        Text("Club Stats & History", style = MaterialTheme.typography.headlineMedium)
      }

      item {
        Card(
          shape = RoundedCornerShape(22.dp),
          colors = CardDefaults.cardColors(containerColor = ArcadeSurfaceHigh),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text("Club Level $level", style = MaterialTheme.typography.titleLarge, color = CyberMint)
              Text("${xp % 100L}/100 XP", style = MaterialTheme.typography.labelMedium, color = SkyPulse)
            }
            LinearProgressIndicator(
              progress = { levelProgress },
              modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(6.dp)),
              color = CyberMint,
              trackColor = MaterialTheme.colorScheme.surface,
            )
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              StatTile("Played", "$played", SkyPulse, Modifier.weight(1f))
              StatTile("Wins", "$won", CyberMint, Modifier.weight(1f))
              StatTile("Win %", "$winRate%", SunnyGold, Modifier.weight(1f))
              StatTile("Coins", "${profile?.coins ?: 100}", NeonCoral, Modifier.weight(1f))
            }
          }
        }
      }

      item {
        Text("Recent Matches", style = MaterialTheme.typography.headlineSmall)
      }

      when (matchesState) {
        is UiState.Loading -> {
          item {
            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
              CircularProgressIndicator()
            }
          }
        }
        is UiState.Error -> {
          item {
            Text(matchesState.message, color = MaterialTheme.colorScheme.error)
          }
        }
        is UiState.Success -> {
          val matches = matchesState.data
          if (matches.isEmpty()) {
            item {
              Surface(
                color = ArcadeSurfaceHigh.copy(alpha = 0.7f),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth(),
              ) {
                Column(
                  modifier = Modifier.padding(22.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Icon(
                    imageVector = Icons.Filled.SportsScore,
                    contentDescription = null,
                    tint = SunnyGold,
                    modifier = Modifier.size(34.dp),
                  )
                  Text("No matches played yet", style = MaterialTheme.typography.titleMedium)
                  Text(
                    "Jump into Draw & Guess, Who's the Spy, or Word Bomb from the Lobby to earn coins and XP!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                }
              }
            }
          } else {
            items(matches, key = { it.id }) { match ->
              val game = GameType.fromId(match.gameType)
              val accent = if (match.won) CyberMint else NeonCoral
              val timeStr = match.createdAt?.toDate()?.let { dateFormat.format(it) } ?: "Just now"

              Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = ArcadeSurfaceHigh),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth().padding(14.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f),
                  ) {
                    Surface(
                      color = accent.copy(alpha = 0.16f),
                      shape = CircleShape,
                      modifier = Modifier.size(42.dp),
                    ) {
                      Box(contentAlignment = Alignment.Center) {
                        Icon(
                          imageVector = gameIcon(game),
                          contentDescription = game.displayName,
                          tint = accent,
                          modifier = Modifier.size(22.dp),
                        )
                      }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                      Text(match.roomTitle, style = MaterialTheme.typography.titleMedium)
                      Text(
                        "${game.displayName} · ${if (match.mode == MatchMode.MULTIPLAYER_ROOM.id) "Live Room" else "Party Bots"} · $timeStr",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                  }

                  Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      if (match.won) {
                        Icon(
                          imageVector = Icons.Filled.EmojiEvents,
                          contentDescription = "Won",
                          tint = SunnyGold,
                          modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                      }
                      Text(
                        text = "${match.score} pts",
                        style = MaterialTheme.typography.titleSmall,
                        color = accent,
                      )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Icon(
                        imageVector = Icons.Filled.MonetizationOn,
                        contentDescription = null,
                        tint = SunnyGold,
                        modifier = Modifier.size(13.dp),
                      )
                      Spacer(Modifier.width(3.dp))
                      Text(
                        text = "+${match.coinsEarned} · +${match.xpEarned} XP",
                        style = MaterialTheme.typography.labelSmall,
                        color = SunnyGold,
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
  profile: UserProfile?,
  onSaveProfile: (displayName: String, bio: String, avatarIndex: Long, favoriteGame: GameType) -> Unit,
  onSignOut: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var displayName by remember(profile?.displayName) {
    mutableStateOf(profile?.displayName ?: "PartyPlayer")
  }
  var bio by remember(profile?.bio) {
    mutableStateOf(profile?.bio ?: "Ready for party games!")
  }
  var avatarIndex by remember(profile?.avatarIndex) {
    mutableIntStateOf((profile?.avatarIndex ?: 0L).toInt().coerceIn(0, 7))
  }
  var favoriteGame by remember(profile?.favoriteGame) {
    mutableStateOf(GameType.fromId(profile?.favoriteGame))
  }

  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    Column(
      modifier =
        Modifier.widthIn(max = 680.dp)
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 16.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Text("Player Club Card", style = MaterialTheme.typography.headlineMedium)

      Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = ArcadeSurfaceHigh),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(18.dp),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          AvatarBadge(avatarIndex = avatarIndex, size = 82.dp)
          Text(
            text = displayName.ifBlank { "PartyPlayer" },
            style = MaterialTheme.typography.headlineSmall,
          )
          Text(
            text = DefaultPartyContent.avatarLabels[avatarIndex],
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
          )

          Text("Choose Mascot Avatar", style = MaterialTheme.typography.titleSmall)
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            for (i in 0..7) {
              val selected = avatarIndex == i
              Box(
                modifier =
                  Modifier.clip(CircleShape)
                    .border(
                      width = if (selected) 2.5.dp else 0.dp,
                      color = if (selected) Color.White else Color.Transparent,
                      shape = CircleShape,
                    )
                    .padding(3.dp)
                    .minimumInteractiveComponentSize()
                    .clickable { avatarIndex = i }
                    .testTag("profile_avatar_$i"),
                contentAlignment = Alignment.Center,
              ) {
                AvatarBadge(avatarIndex = i, size = 44.dp)
              }
            }
          }
        }
      }

      OutlinedTextField(
        value = displayName,
        onValueChange = { if (it.length <= 30) displayName = it },
        label = { Text("Club Nickname") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag("profile_name_input"),
      )

      OutlinedTextField(
        value = bio,
        onValueChange = { if (it.length <= 160) bio = it },
        label = { Text("Party Bio / Status") },
        minLines = 2,
        maxLines = 3,
        modifier = Modifier.fillMaxWidth().testTag("profile_bio_input"),
      )

      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Favorite Party Game", style = MaterialTheme.typography.titleSmall)
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          GameType.entries.forEach { game ->
            FilterChip(
              selected = favoriteGame == game,
              onClick = { favoriteGame = game },
              label = { Text(game.displayName) },
              leadingIcon = {
                Icon(gameIcon(game), contentDescription = null, modifier = Modifier.size(16.dp))
              },
            )
          }
        }
      }

      Button(
        onClick = { onSaveProfile(displayName, bio, avatarIndex.toLong(), favoriteGame) },
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().height(52.dp).testTag("profile_save_button"),
      ) {
        Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Save Club Card", fontWeight = FontWeight.Bold)
      }

      OutlinedButton(
        onClick = onSignOut,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("sign_out_button"),
      ) {
        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Sign Out")
      }

      Spacer(Modifier.height(16.dp))
    }
  }
}
