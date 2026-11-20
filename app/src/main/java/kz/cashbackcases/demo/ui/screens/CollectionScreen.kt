package kz.cashbackcases.demo.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kz.cashbackcases.core.Offer
import kz.cashbackcases.core.SampleCatalog
import kz.cashbackcases.demo.R
import kz.cashbackcases.demo.ui.Tags
import kz.cashbackcases.demo.ui.components.CaseArt
import kz.cashbackcases.demo.ui.components.ScreenHeader
import kz.cashbackcases.demo.ui.components.motionEnabled
import kz.cashbackcases.demo.ui.theme.Cyan
import kz.cashbackcases.demo.ui.theme.Gold
import kz.cashbackcases.demo.ui.theme.Ink
import kz.cashbackcases.demo.ui.theme.InkCard
import kz.cashbackcases.demo.ui.theme.Line
import kz.cashbackcases.demo.ui.theme.Muted
import kz.cashbackcases.demo.ui.theme.Text

@Composable
fun CollectionScreen(
    collectionId: String,
    onBack: () -> Unit,
    onOffer: (String) -> Unit,
) {
    val collection = SampleCatalog.collection(collectionId)
    if (collection == null) {
        Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            ScreenHeader(title = stringResource(R.string.missing_title), onBack = onBack)
            Text(
                text = stringResource(R.string.missing_body),
                color = Muted,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
        return
    }
    val reduceMotion = !motionEnabled()
    var open by remember(collectionId) { mutableStateOf(reduceMotion) }
    LaunchedEffect(collectionId) { open = true }
    val progress by animateFloatAsState(
        targetValue = if (open) 1f else 0f,
        animationSpec = if (reduceMotion) snap() else tween(durationMillis = 1100, easing = FastOutSlowInEasing),
        label = "caseOpen",
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        ScreenHeader(title = collection.title, onBack = onBack)
        Text(
            text = stringResource(R.string.fixed_collection),
            modifier = Modifier.padding(horizontal = 12.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = Gold,
        )
        Text(
            text = collection.subtitle,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = Muted,
        )
        CaseArt(
            accent = collection.accent,
            openFraction = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp),
        )
        if (progress < 0.45f) {
            Text(
                text = stringResource(R.string.opening),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = Muted,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
        AnimatedVisibility(
            visible = progress >= 0.98f,
            enter = if (reduceMotion) {
                EnterTransition.None
            } else {
                fadeIn(tween(280)) + slideInVertically { it / 8 }
            },
        ) {
            Column(Modifier.padding(horizontal = 8.dp)) {
                collection.offers.forEach { offer ->
                    OfferRow(offer = offer, onClick = { onOffer(offer.id) })
                }
            }
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun OfferRow(offer: Offer, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .testTag(Tags.offer(offer.id))
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(InkCard)
            .border(1.dp, Line, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = offer.merchantName, style = MaterialTheme.typography.titleMedium, color = Text)
            Text(text = offer.category, style = MaterialTheme.typography.bodyMedium, color = Muted)
            Text(
                text = stringResource(R.string.sample_rate_caption),
                style = MaterialTheme.typography.labelMedium,
                color = Gold,
            )
        }
        Text(
            text = stringResource(R.string.rate_badge, offer.sampleRatePercent),
            style = MaterialTheme.typography.headlineSmall,
            color = Cyan,
        )
    }
}
