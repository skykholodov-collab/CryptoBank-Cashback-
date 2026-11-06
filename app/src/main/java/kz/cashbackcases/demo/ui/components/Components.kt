package kz.cashbackcases.demo.ui.components

import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kz.cashbackcases.core.CaseAccent
import kz.cashbackcases.demo.R
import kz.cashbackcases.demo.ui.Tags
import kz.cashbackcases.demo.ui.theme.Cyan
import kz.cashbackcases.demo.ui.theme.Gold
import kz.cashbackcases.demo.ui.theme.GoldBright
import kz.cashbackcases.demo.ui.theme.Text

@Composable
fun motionEnabled(): Boolean {
    val context = LocalContext.current
    return remember {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) > 0.01f
        }.getOrDefault(true)
    }
}

@Composable
fun DemoBanner() {
    Text(
        text = stringResource(R.string.demo_banner),
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF2A2418))
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag(Tags.DEMO_BANNER),
        color = GoldBright,
        style = MaterialTheme.typography.labelMedium,
        textAlign = TextAlign.Center,
    )
}

@Composable
fun ScreenHeader(title: String, onBack: (() -> Unit)?) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = GoldBright,
                )
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = Text,
        )
    }
}

/**
 * Closed or opening cover for a fixed offer collection.
 * The motion reveals the list; it does not draw a random result.
 */
@Composable
fun CaseArt(
    accent: CaseAccent,
    openFraction: Float,
    modifier: Modifier = Modifier,
) {
    val fraction = openFraction.coerceIn(0f, 1f)
    val trim = when (accent) {
        CaseAccent.CYAN -> Cyan
        CaseAccent.GOLD -> Gold
        CaseAccent.SPLIT -> GoldBright
    }
    val glow = when (accent) {
        CaseAccent.CYAN -> Cyan
        CaseAccent.GOLD -> Gold
        CaseAccent.SPLIT -> Cyan
    }
    val cover = stringResource(R.string.case_cover)
    Box(
        modifier = modifier.semantics { contentDescription = cover },
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(width = 250.dp, height = 220.dp)) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
                    .size(width = 168.dp, height = 16.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.Black.copy(alpha = 0.35f)),
            )
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
                    .size(width = 214.dp, height = 132.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF303A4E), Color(0xFF141A26))))
                    .border(1.5.dp, trim.copy(alpha = 0.95f), RoundedCornerShape(22.dp))
                    .padding(14.dp),
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    glow.copy(alpha = 0.12f + 0.5f * fraction),
                                    Color.Transparent,
                                ),
                            ),
                        ),
                )
                Text(
                    text = "₸",
                    modifier = Modifier.align(Alignment.Center),
                    color = glow.copy(alpha = 0.3f + 0.65f * fraction),
                    style = MaterialTheme.typography.displaySmall,
                )
            }
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 34.dp)
                    .size(width = 228.dp, height = 74.dp)
                    .graphicsLayer {
                        rotationX = -108f * fraction
                        cameraDistance = 16f * density
                        transformOrigin = TransformOrigin(0.5f, 1f)
                    }
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 8.dp, bottomEnd = 8.dp))
                    .background(Brush.verticalGradient(listOf(Color(0xFF4C566C), Color(0xFF222A3A))))
                    .border(
                        width = 1.5.dp,
                        color = trim,
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 8.dp, bottomEnd = 8.dp),
                    ),
            ) {
                Box(
                    Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(0.42f)
                        .height(2.dp)
                        .background(glow.copy(alpha = 0.9f)),
                )
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = 4.dp)
                        .size(width = 34.dp, height = 8.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Gold),
                )
            }
        }
    }
}
