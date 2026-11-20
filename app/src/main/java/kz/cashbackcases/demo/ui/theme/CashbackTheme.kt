@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package kz.cashbackcases.demo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kz.cashbackcases.demo.R

val Ink = Color(0xFF0B0E14)
val InkRaised = Color(0xFF121722)
val InkCard = Color(0xFF1A2130)
val Line = Color(0xFF2E384A)
val Gold = Color(0xFFD7B56D)
val GoldBright = Color(0xFFF3E2B8)
val Cyan = Color(0xFF3EE0D2)
val Text = Color(0xFFF6F1E7)
val Muted = Color(0xFFA7B0C0)

private fun manrope(weight: FontWeight, axis: Int) = Font(
    resId = R.font.manrope,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(axis)),
)

private fun tenge(weight: FontWeight) = Font(
    resId = R.font.tenge,
    weight = weight,
)

private val Manrope = FontFamily(
    manrope(FontWeight.Normal, 400),
    tenge(FontWeight.Normal),
    manrope(FontWeight.Medium, 500),
    tenge(FontWeight.Medium),
    manrope(FontWeight.SemiBold, 600),
    tenge(FontWeight.SemiBold),
    manrope(FontWeight.Bold, 700),
    tenge(FontWeight.Bold),
)

private val CashbackTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.SemiBold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = Manrope,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.3.sp,
    ),
)

private val CashbackColors = darkColorScheme(
    primary = Gold,
    onPrimary = Color(0xFF1A1408),
    secondary = Cyan,
    onSecondary = Color(0xFF04221F),
    background = Ink,
    onBackground = Text,
    surface = InkRaised,
    onSurface = Text,
    surfaceVariant = InkCard,
    onSurfaceVariant = Muted,
    outline = Line,
    error = Color(0xFFE7B4B4),
    onError = Color(0xFF2A1212),
)

@Composable
fun CashbackTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CashbackColors,
        typography = CashbackTypography,
        content = content,
    )
}
