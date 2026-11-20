package kz.cashbackcases.demo.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import kz.cashbackcases.core.CaseAccent
import kz.cashbackcases.core.OfferCollection
import kz.cashbackcases.core.SampleCatalog
import kz.cashbackcases.demo.R
import kz.cashbackcases.demo.ui.Tags
import kz.cashbackcases.demo.ui.components.CaseArt
import kz.cashbackcases.demo.ui.components.motionEnabled
import kz.cashbackcases.demo.ui.theme.Cyan
import kz.cashbackcases.demo.ui.theme.Gold
import kz.cashbackcases.demo.ui.theme.GoldBright
import kz.cashbackcases.demo.ui.theme.Ink
import kz.cashbackcases.demo.ui.theme.InkCard
import kz.cashbackcases.demo.ui.theme.Line
import kz.cashbackcases.demo.ui.theme.Muted
import kz.cashbackcases.demo.ui.theme.Text
import kotlinx.coroutines.launch

@Composable
fun CatalogScreen(onOpen: (String) -> Unit) {
    val collections = SampleCatalog.collections
    val pagerState = rememberPagerState(pageCount = { collections.size })
    val scope = rememberCoroutineScope()
    val motion = motionEnabled()
    val current = collections[pagerState.currentPage]
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.displaySmall,
            color = Text,
        )
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .size(width = 72.dp, height = 2.dp)
                .background(Gold),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.catalog_lead),
            style = MaterialTheme.typography.bodyLarge,
            color = Muted,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.catalog_swipe),
            style = MaterialTheme.typography.bodyMedium,
            color = Muted,
        )
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            collections.forEachIndexed { index, collection ->
                FilterChip(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        scope.launch {
                            if (motion) {
                                pagerState.animateScrollToPage(index)
                            } else {
                                pagerState.scrollToPage(index)
                            }
                        }
                    },
                    label = { Text(collection.title) },
                    modifier = Modifier.testTag(Tags.caseTab(collection.id)),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = InkCard,
                        labelColor = Text,
                        selectedContainerColor = Color(0xFF143834),
                        selectedLabelColor = Cyan,
                    ),
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        HorizontalPager(
            state = pagerState,
            contentPadding = PaddingValues(horizontal = 4.dp),
            pageSpacing = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
        ) { page ->
            val collection = collections[page]
            CaseSelectCard(
                collection = collection,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(Tags.caseCard(collection.id)),
                onOpen = { onOpen(collection.id) },
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            collections.forEachIndexed { index, _ ->
                val active = pagerState.currentPage == index
                Box(
                    Modifier
                        .padding(horizontal = 4.dp)
                        .size(width = if (active) 22.dp else 8.dp, height = 8.dp)
                        .clip(RoundedCornerShape(99.dp))
                        .background(if (active) Gold else Line),
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { onOpen(current.id) },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag(Tags.OPEN_COLLECTION),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Gold,
                contentColor = Color(0xFF1A1408),
            ),
        ) {
            Text(stringResource(R.string.open_named, current.title))
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.catalog_footnote),
            style = MaterialTheme.typography.bodyMedium,
            color = Muted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun CaseSelectCard(
    collection: OfferCollection,
    modifier: Modifier,
    onOpen: () -> Unit,
) {
    val trim = when (collection.accent) {
        CaseAccent.CYAN -> Cyan
        CaseAccent.GOLD -> Gold
        CaseAccent.SPLIT -> GoldBright
    }
    val count = collection.offers.size
    val countLabel = pluralStringResource(R.plurals.offers_in_collection, count, count)
    val rateLabel = stringResource(R.string.max_sample_rate, collection.maxSampleRatePercent)
    Surface(
        onClick = onOpen,
        modifier = modifier,
        shape = RoundedCornerShape(28.dp),
        color = InkCard,
        border = BorderStroke(1.dp, trim.copy(alpha = 0.6f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CaseArt(
                accent = collection.accent,
                openFraction = 0f,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )
            Text(
                text = collection.title,
                style = MaterialTheme.typography.headlineSmall,
                color = Text,
            )
            Text(
                text = collection.subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = Muted,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "$countLabel · $rateLabel",
                style = MaterialTheme.typography.labelMedium,
                color = GoldBright,
            )
        }
    }
}
