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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.model.CustomPack
import com.example.data.model.GameType
import com.example.data.model.UserProfile
import com.example.ui.components.gameIcon
import com.example.ui.theme.ArcadeSurfaceHigh
import com.example.ui.theme.ClubAccentNames
import com.example.ui.theme.ClubAccents
import com.example.ui.theme.CyberMint
import com.example.ui.theme.NeonCoral
import com.example.ui.theme.SunnyGold
import com.example.ui.viewmodel.UiState
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudioScreen(
  profile: UserProfile?,
  customPacksState: UiState<List<CustomPack>>,
  onSaveHouseRules: (accentIndex: Long, roundSeconds: Long, maxPlayers: Long) -> Unit,
  onSaveCustomPack: (title: String, gameType: GameType, items: List<String>) -> Unit,
  onDeleteCustomPack: (String) -> Unit,
  onPlayWithPack: (GameType, String) -> Unit,
  modifier: Modifier = Modifier,
) {
  var selectedAccent by remember(profile?.accentIndex) {
    mutableIntStateOf((profile?.accentIndex ?: 0L).toInt().coerceIn(0, ClubAccents.lastIndex))
  }
  var roundSlider by remember(profile?.defaultRoundSeconds) {
    mutableFloatStateOf((profile?.defaultRoundSeconds ?: 45L).toFloat().coerceIn(20f, 120f))
  }
  var playersSlider by remember(profile?.defaultMaxPlayers) {
    mutableFloatStateOf((profile?.defaultMaxPlayers ?: 6L).toFloat().coerceIn(2f, 8f))
  }
  var showCreatePackDialog by remember { mutableStateOf(false) }

  Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
    LazyColumn(
      modifier = Modifier.widthIn(max = 680.dp).fillMaxSize(),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      item {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text("My WePlay Studio", style = MaterialTheme.typography.headlineMedium)
          Text(
            "Customize your club vibe, default house rules, and build your own party word & spy decks.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      // Theme Accent & House Rules Card
      item {
        Card(
          shape = RoundedCornerShape(22.dp),
          colors = CardDefaults.cardColors(containerColor = ArcadeSurfaceHigh),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Filled.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
              Spacer(Modifier.width(8.dp))
              Text("Club Theme & House Rules", style = MaterialTheme.typography.titleLarge)
            }

            Text("Club Accent Vibe", style = MaterialTheme.typography.titleSmall)
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
              ClubAccents.forEachIndexed { index, color ->
                val isSelected = selectedAccent == index
                Box(
                  modifier =
                    Modifier.size(48.dp)
                      .clip(CircleShape)
                      .background(color)
                      .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) Color.White else Color.White.copy(alpha = 0.25f),
                        shape = CircleShape,
                      )
                      .clickable { selectedAccent = index }
                      .testTag("studio_accent_$index"),
                  contentAlignment = Alignment.Center,
                ) {
                  if (isSelected) {
                    Icon(
                      imageVector = Icons.Filled.Check,
                      contentDescription = ClubAccentNames[index],
                      tint = Color(0xFF120E24),
                    )
                  }
                }
              }
            }
            Text(
              text = "Selected: ${ClubAccentNames[selectedAccent]}",
              style = MaterialTheme.typography.labelMedium,
              color = ClubAccents[selectedAccent],
            )

            Column {
              Text(
                "Default Round Timer: ${roundSlider.roundToInt()} seconds",
                style = MaterialTheme.typography.titleSmall,
              )
              Slider(
                value = roundSlider,
                onValueChange = { roundSlider = it },
                valueRange = 20f..120f,
                steps = 9,
              )
            }

            Column {
              Text(
                "Default Room Size: ${playersSlider.roundToInt()} players",
                style = MaterialTheme.typography.titleSmall,
              )
              Slider(
                value = playersSlider,
                onValueChange = { playersSlider = it },
                valueRange = 2f..8f,
                steps = 5,
              )
            }

            Button(
              onClick = {
                onSaveHouseRules(
                  selectedAccent.toLong(),
                  roundSlider.roundToInt().toLong(),
                  playersSlider.roundToInt().toLong(),
                )
              },
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.fillMaxWidth().testTag("studio_save_rules_button"),
            ) {
              Icon(Icons.Filled.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(Modifier.width(8.dp))
              Text("Apply My Version Settings", fontWeight = FontWeight.Bold)
            }
          }
        }
      }

      // Custom Decks Header
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("Custom Party Packs", style = MaterialTheme.typography.headlineSmall)
            Text(
              "Your personal words, spy pairs & trivia questions",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          Button(
            onClick = { showCreatePackDialog = true },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.testTag("studio_new_pack_button"),
          ) {
            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("New Pack")
          }
        }
      }

      when (customPacksState) {
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
            Text(
              text = customPacksState.message,
              color = MaterialTheme.colorScheme.error,
              modifier = Modifier.padding(12.dp),
            )
          }
        }
        is UiState.Success -> {
          val packs = customPacksState.data
          if (packs.isEmpty()) {
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
                  verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  Text("No custom packs yet", style = MaterialTheme.typography.titleMedium)
                  Text(
                    "Create a Draw & Guess word pack, a Who's the Spy pair list (e.g. Boba|Milkshake), or custom Word Bomb trivia!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  OutlinedButton(
                    onClick = { showCreatePackDialog = true },
                    modifier = Modifier.testTag("studio_empty_create_pack_button"),
                  ) {
                    Text("Create First Pack")
                  }
                }
              }
            }
          } else {
            items(packs, key = { it.id }) { pack ->
              val game = GameType.fromId(pack.gameType)
              val accent =
                when (game) {
                  GameType.DRAW_GUESS -> NeonCoral
                  GameType.WHO_IS_SPY -> CyberMint
                  GameType.WORD_BOMB -> SunnyGold
                }
              Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = ArcadeSurfaceHigh),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.4f)),
              ) {
                Column(
                  modifier = Modifier.fillMaxWidth().padding(14.dp),
                  verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      modifier = Modifier.weight(1f),
                    ) {
                      Icon(
                        imageVector = gameIcon(game),
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(20.dp),
                      )
                      Spacer(Modifier.width(8.dp))
                      Column {
                        Text(pack.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                          "${game.displayName} · ${pack.items.size} items",
                          style = MaterialTheme.typography.labelSmall,
                          color = accent,
                        )
                      }
                    }
                    IconButton(
                      onClick = { onDeleteCustomPack(pack.id) },
                      modifier = Modifier.testTag("delete_pack_${pack.id}"),
                    ) {
                      Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = "Delete pack",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    }
                  }

                  FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                  ) {
                    pack.items.take(6).forEach { entry ->
                      Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(10.dp),
                      ) {
                        Text(
                          text = entry.replace("|", " vs "),
                          style = MaterialTheme.typography.bodySmall,
                          modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                      }
                    }
                  }

                  Button(
                    onClick = { onPlayWithPack(game, pack.id) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("play_pack_${pack.id}"),
                  ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Play With This Pack")
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  if (showCreatePackDialog) {
    CreatePackDialog(
      onDismiss = { showCreatePackDialog = false },
      onCreate = { title, game, items ->
        showCreatePackDialog = false
        onSaveCustomPack(title, game, items)
      },
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CreatePackDialog(
  onDismiss: () -> Unit,
  onCreate: (String, GameType, List<String>) -> Unit,
) {
  var title by remember { mutableStateOf("") }
  var selectedGame by remember { mutableStateOf(GameType.DRAW_GUESS) }
  var rawItems by remember { mutableStateOf("") }

  val hintText =
    when (selectedGame) {
      GameType.DRAW_GUESS -> "One word per line, e.g.:\nBoba Tea\nSkateboard\nJetpack"
      GameType.WHO_IS_SPY -> "Civilian|Spy per line, e.g.:\nCoffee|Matcha\nGuitar|Ukulele"
      GameType.WORD_BOMB -> "Question|Answer per line, e.g.:\nCapital of France?|Paris\nLargest planet?|Jupiter"
    }

  val parsedItems =
    rawItems
      .lines()
      .map { it.trim() }
      .filter { it.isNotBlank() }
      .take(30)

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("New Custom Pack", style = MaterialTheme.typography.titleLarge) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
          value = title,
          onValueChange = { if (it.length <= 40) title = it },
          label = { Text("Pack Name") },
          placeholder = { Text("e.g. Squad Inside Jokes") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth().testTag("pack_title_input"),
        )
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          GameType.entries.forEach { game ->
            FilterChip(
              selected = selectedGame == game,
              onClick = { selectedGame = game },
              label = { Text(game.displayName) },
            )
          }
        }
        OutlinedTextField(
          value = rawItems,
          onValueChange = { rawItems = it },
          label = { Text("Items (${parsedItems.size}/30)") },
          placeholder = { Text(hintText) },
          minLines = 4,
          maxLines = 6,
          modifier = Modifier.fillMaxWidth().testTag("pack_items_input"),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onCreate(title.ifBlank { "My ${selectedGame.displayName} Pack" }, selectedGame, parsedItems)
        },
        enabled = parsedItems.isNotEmpty(),
        modifier = Modifier.testTag("pack_save_confirm_button"),
      ) {
        Text("Save Pack")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancel")
      }
    },
  )
}
