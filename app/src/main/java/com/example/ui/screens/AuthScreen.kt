package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.auth.attemptAutoSignIn
import com.example.auth.onGoogleSignInClicked
import com.example.ui.theme.ArcadeBg
import com.example.ui.theme.ArcadeSurface
import com.example.ui.theme.CyberMint
import com.example.ui.theme.NeonCoral
import com.example.ui.theme.SunnyGold

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AuthScreen(
  onAuthSuccess: () -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val credentialManager = remember(context) { CredentialManager.create(context) }

  var isLoading by remember { mutableStateOf(false) }
  var isCheckingAuto by remember { mutableStateOf(true) }
  var errorMsg by remember { mutableStateOf<String?>(null) }

  LaunchedEffect(Unit) {
    attemptAutoSignIn(
      context = context,
      credentialManager = credentialManager,
      onAuthSuccess = {
        isCheckingAuto = false
        onAuthSuccess()
      },
      onUnauthenticated = {
        isCheckingAuto = false
      },
      scope = scope,
    )
  }

  Box(
    modifier =
      Modifier.fillMaxSize()
        .background(
          Brush.verticalGradient(
            colors = listOf(Color(0xFF1E1442), ArcadeBg, Color(0xFF0C0918))
          )
        )
        .systemBarsPadding(),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      modifier =
        Modifier.widthIn(max = 520.dp)
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 24.dp, vertical = 20.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
      Surface(
        shape = RoundedCornerShape(28.dp),
        color = ArcadeSurface,
        border = BorderStroke(1.5.dp, NeonCoral.copy(alpha = 0.4f)),
        shadowElevation = 12.dp,
      ) {
        Image(
          painter = painterResource(id = R.drawable.img_party_hero),
          contentDescription = "WePlay Club Arcade Lounge",
          modifier = Modifier.fillMaxWidth().height(210.dp).clip(RoundedCornerShape(28.dp)),
          contentScale = ContentScale.Crop,
        )
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Filled.SportsEsports,
          contentDescription = null,
          tint = SunnyGold,
          modifier = Modifier.size(30.dp),
        )
        Spacer(Modifier.width(8.dp))
        Text(
          text = "WePlay Club",
          style = MaterialTheme.typography.displaySmall,
          color = MaterialTheme.colorScheme.onBackground,
        )
      }

      Text(
        text = "Your own version of party gaming. Host live multiplayer rooms, build custom word decks, and play Draw & Guess, Spy, and Word Bomb.",
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )

      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        AssistChip(
          onClick = {},
          label = { Text("Draw & Guess") },
          leadingIcon = {
            Icon(Icons.Filled.Brush, contentDescription = null, tint = NeonCoral, modifier = Modifier.size(16.dp))
          },
        )
        AssistChip(
          onClick = {},
          label = { Text("Who's the Spy") },
          leadingIcon = {
            Icon(Icons.Filled.Visibility, contentDescription = null, tint = CyberMint, modifier = Modifier.size(16.dp))
          },
        )
        AssistChip(
          onClick = {},
          label = { Text("Word Bomb") },
          leadingIcon = {
            Icon(Icons.Filled.LocalFireDepartment, contentDescription = null, tint = SunnyGold, modifier = Modifier.size(16.dp))
          },
        )
      }

      if (errorMsg != null) {
        Surface(
          color = MaterialTheme.colorScheme.error.copy(alpha = 0.16f),
          shape = RoundedCornerShape(14.dp),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Text(
            text = errorMsg!!,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(12.dp),
          )
        }
      }

      Spacer(Modifier.height(4.dp))

      Button(
        onClick = {
          errorMsg = null
          isLoading = true
          onGoogleSignInClicked(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
              isLoading = false
              onAuthSuccess()
            },
            onAuthError = { msg ->
              isLoading = false
              errorMsg = msg
            },
            scope = scope,
            onAuthCancelled = {
              isLoading = false
              errorMsg = "Sign-in cancelled. Tap below to choose your Google account."
            },
          )
        },
        enabled = !isLoading && !isCheckingAuto,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth().height(56.dp).testTag("google_sign_in_button"),
      ) {
        if (isLoading || isCheckingAuto) {
          CircularProgressIndicator(
            modifier = Modifier.size(22.dp),
            color = MaterialTheme.colorScheme.onPrimary,
            strokeWidth = 2.5.dp,
          )
          Spacer(Modifier.width(10.dp))
          Text(if (isCheckingAuto) "Checking session..." else "Signing in...")
        } else {
          Text(
            text = "Sign in with Google",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
          )
        }
      }
    }
  }
}
