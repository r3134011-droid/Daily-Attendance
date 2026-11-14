package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Immutable
data class AttendanceSemanticColors(
  val headerStart: Color,
  val headerEnd: Color,
  val goldAccent: Color,
  val present: Color,
  val presentContainer: Color,
  val absent: Color,
  val absentContainer: Color,
  val holiday: Color,
  val holidayContainer: Color,
  val weeklyOff: Color,
  val weeklyOffContainer: Color,
  val todayRing: Color,
)

private val LightSemanticColors = AttendanceSemanticColors(
  headerStart = IndigoDeep,
  headerEnd = IndigoSurface,
  goldAccent = SovereignGoldSoft,
  present = PresentGreen,
  presentContainer = PresentGreenContainer,
  absent = AbsentRed,
  absentContainer = AbsentRedContainer,
  holiday = HolidayAmber,
  holidayContainer = HolidayAmberContainer,
  weeklyOff = WeeklyOffBlue,
  weeklyOffContainer = WeeklyOffBlueContainer,
  todayRing = SovereignGold,
)

private val DarkSemanticColors = AttendanceSemanticColors(
  headerStart = Color(0xFF080D20),
  headerEnd = Color(0xFF172D60),
  goldAccent = SovereignGoldSoft,
  present = Color(0xFF22B77A),
  presentContainer = Color(0xFF0E3E2C),
  absent = Color(0xFFF36B78),
  absentContainer = Color(0xFF471620),
  holiday = Color(0xFFFFC45D),
  holidayContainer = Color(0xFF47300A),
  weeklyOff = Color(0xFF5B9CF0),
  weeklyOffContainer = Color(0xFF142E5C),
  todayRing = SovereignGold,
)

val LocalAttendanceColors = staticCompositionLocalOf { LightSemanticColors }

private val LightColorScheme = lightColorScheme(
  primary = RoyalBlue,
  onPrimary = Color.White,
  primaryContainer = RoyalBlueContainer,
  onPrimaryContainer = IndigoDeep,
  secondary = IndigoSurface,
  onSecondary = Color.White,
  secondaryContainer = LightSurfaceVariant,
  onSecondaryContainer = DarkInk,
  tertiary = SovereignGoldDark,
  onTertiary = Color.White,
  tertiaryContainer = SovereignGoldSoft,
  onTertiaryContainer = IndigoDeep,
  background = LightBackground,
  onBackground = DarkInk,
  surface = LightSurface,
  onSurface = DarkInk,
  surfaceVariant = LightSurfaceVariant,
  onSurfaceVariant = MutedInk,
  outline = LightOutline,
  error = AbsentRed,
  onError = Color.White,
  errorContainer = AbsentRedContainer,
  onErrorContainer = AbsentRedDeep,
)

private val DarkColorScheme = darkColorScheme(
  primary = Color(0xFF7FA1FF),
  onPrimary = IndigoDeep,
  primaryContainer = Color(0xFF1F387E),
  onPrimaryContainer = RoyalBlueContainer,
  secondary = SovereignGold,
  onSecondary = IndigoDeep,
  secondaryContainer = DarkSurfaceVariant,
  onSecondaryContainer = LightInk,
  tertiary = SovereignGoldSoft,
  onTertiary = IndigoDeep,
  background = DarkBackground,
  onBackground = LightInk,
  surface = DarkSurface,
  onSurface = LightInk,
  surfaceVariant = DarkSurfaceVariant,
  onSurfaceVariant = DarkMutedInk,
  outline = DarkOutline,
  error = Color(0xFFFF8A95),
  onError = IndigoDeep,
  errorContainer = Color(0xFF5D1B27),
  onErrorContainer = AbsentRedContainer,
)

val AppShapes = Shapes(
  small = RoundedCornerShape(10.dp),
  medium = RoundedCornerShape(16.dp),
  large = RoundedCornerShape(22.dp),
  extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun MahadevAttendanceTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      val context = LocalContext.current
      if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    darkTheme -> DarkColorScheme
    else -> LightColorScheme
  }

  val semanticColors = if (darkTheme) DarkSemanticColors else LightSemanticColors

  CompositionLocalProvider(LocalAttendanceColors provides semanticColors) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = AppTypography,
      shapes = AppShapes,
      content = content,
    )
  }
}
