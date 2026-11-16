package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val WePlayShapes = Shapes(
  small = RoundedCornerShape(12.dp),
  medium = RoundedCornerShape(18.dp),
  large = RoundedCornerShape(24.dp),
  extraLarge = RoundedCornerShape(30.dp),
)

@Composable
fun WePlayClubTheme(
  accentIndex: Int = 0,
  content: @Composable () -> Unit,
) {
  val safeAccent = ClubAccents[accentIndex.coerceIn(0, ClubAccents.lastIndex)]
  val onPrimaryColor =
    if (safeAccent == CyberMint || safeAccent == SunnyGold || safeAccent == SkyPulse) {
      Color(0xFF120E24)
    } else {
      Color.White
    }

  val colorScheme =
    darkColorScheme(
      primary = safeAccent,
      onPrimary = onPrimaryColor,
      primaryContainer = safeAccent.copy(alpha = 0.20f),
      onPrimaryContainer = TextPrimary,
      secondary = CyberMint,
      onSecondary = Color(0xFF120E24),
      secondaryContainer = CyberMint.copy(alpha = 0.18f),
      onSecondaryContainer = TextPrimary,
      tertiary = SunnyGold,
      onTertiary = Color(0xFF120E24),
      tertiaryContainer = SunnyGold.copy(alpha = 0.18f),
      onTertiaryContainer = TextPrimary,
      background = ArcadeBg,
      onBackground = TextPrimary,
      surface = ArcadeSurface,
      onSurface = TextPrimary,
      surfaceVariant = ArcadeSurfaceHigh,
      onSurfaceVariant = TextSecondary,
      surfaceContainerLow = ArcadeSurface,
      surfaceContainer = ArcadeSurfaceHigh,
      surfaceContainerHigh = ArcadeSurfaceBright,
      outline = ArcadeOutline,
      outlineVariant = ArcadeOutline.copy(alpha = 0.55f),
      error = Color(0xFFFF6B81),
      onError = Color.White,
    )

  MaterialTheme(
    colorScheme = colorScheme,
    typography = WePlayTypography,
    shapes = WePlayShapes,
    content = content,
  )
}
