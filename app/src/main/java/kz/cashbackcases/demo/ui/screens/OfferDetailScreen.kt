package kz.cashbackcases.demo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kz.cashbackcases.core.CashbackRules
import kz.cashbackcases.core.CashbackStatus
import kz.cashbackcases.core.DemoCopy
import kz.cashbackcases.core.SampleCatalog
import kz.cashbackcases.core.SamplePurchases
import kz.cashbackcases.core.formatKzt
import kz.cashbackcases.demo.R
import kz.cashbackcases.demo.ui.CashbackViewModel
import kz.cashbackcases.demo.ui.Tags
import kz.cashbackcases.demo.ui.components.ScreenHeader
import kz.cashbackcases.demo.ui.theme.Cyan
import kz.cashbackcases.demo.ui.theme.Gold
import kz.cashbackcases.demo.ui.theme.GoldBright
import kz.cashbackcases.demo.ui.theme.Ink
import kz.cashbackcases.demo.ui.theme.InkCard
import kz.cashbackcases.demo.ui.theme.InkRaised
import kz.cashbackcases.demo.ui.theme.Line
import kz.cashbackcases.demo.ui.theme.Muted
import kz.cashbackcases.demo.ui.theme.Text

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OfferDetailScreen(
    offerId: String,
    viewModel: CashbackViewModel,
    onBack: () -> Unit,
    onOpenBalance: () -> Unit,
) {
    val offer = SampleCatalog.offer(offerId)
    if (offer == null) {
        Column(Modifier.fillMaxSize().background(Ink).padding(horizontal = 12.dp, vertical = 8.dp)) {
            ScreenHeader(title = stringResource(R.string.missing_offer), onBack = onBack)
            Text(
                text = stringResource(R.string.missing_body),
                color = Muted,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        }
        return
    }
    val state by viewModel.state.collectAsState()
    val pending = state.entries.firstOrNull { it.offerId == offer.id && it.status == CashbackStatus.PENDING }
    val hasConfirmed = state.entries.any { it.offerId == offer.id && it.status == CashbackStatus.CONFIRMED }
    var selected by remember(offer.id) { mutableStateOf(SamplePurchases.presetsKzt[1]) }
    var showDialog by remember(offer.id) { mutableStateOf(false) }
    val cashback = CashbackRules.cashbackKzt(selected, offer.sampleRatePercent)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        ScreenHeader(title = offer.merchantName, onBack = onBack)
        Column(Modifier.padding(horizontal = 12.dp)) {
            Text(
                text = stringResource(R.string.fictional_merchant),
                modifier = Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .background(Gold.copy(alpha = 0.14f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                color = GoldBright,
                style = MaterialTheme.typography.labelMedium,
            )
            Spacer(Modifier.height(8.dp))
            Text(text = offer.category, style = MaterialTheme.typography.bodyMedium, color = Muted)
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.rate_badge, offer.sampleRatePercent),
                style = MaterialTheme.typography.displaySmall,
                color = GoldBright,
            )
            Text(
                text = stringResource(R.string.sample_rate_caption),
                style = MaterialTheme.typography.bodyMedium,
                color = Cyan,
            )
            Spacer(Modifier.height(10.dp))
            Text(text = offer.blurb, style = MaterialTheme.typography.bodyLarge, color = Text)
            Spacer(Modifier.height(16.dp))
            SectionCard(title = stringResource(R.string.terms_title)) {
                offer.terms.forEach { term ->
                    Text(
                        text = "• $term",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Muted,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            if (pending != null) {
                SectionCard(title = stringResource(R.string.activated_title)) {
                    Column(Modifier.testTag(Tags.ACTIVATED)) {
                        Text(
                            text = stringResource(R.string.activated_body),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Text,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.purchase_line, formatKzt(pending.samplePurchaseKzt)),
                            color = Muted,
                        )
                        Text(
                            text = "${formatKzt(pending.samplePurchaseKzt)} × ${pending.sampleRatePercent}% = ${formatKzt(pending.cashbackKzt)}",
                            style = MaterialTheme.typography.titleMedium,
                            color = GoldBright,
                        )
                        TextButton(onClick = onOpenBalance) {
                            Text(stringResource(R.string.go_balance), color = Cyan)
                        }
                    }
                }
            } else {
                if (hasConfirmed) {
                    Text(
                        text = stringResource(R.string.prior_confirmed),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Muted,
                    )
                    Spacer(Modifier.height(12.dp))
                }
                Text(
                    text = stringResource(R.string.purchase_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = Text,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = DemoCopy.PURCHASE_HINT,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Muted,
                )
                Spacer(Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SamplePurchases.presetsKzt.forEach { amount ->
                        FilterChip(
                            selected = selected == amount,
                            onClick = { selected = amount },
                            label = { Text(formatKzt(amount)) },
                            modifier = Modifier.testTag(Tags.purchase(amount)),
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = InkCard,
                                labelColor = Text,
                                selectedContainerColor = Color(0xFF3A2E18),
                                selectedLabelColor = GoldBright,
                            ),
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                SectionCard(title = stringResource(R.string.calc_title)) {
                    Text(
                        text = "${formatKzt(selected)} × ${offer.sampleRatePercent}% = ${formatKzt(cashback)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = GoldBright,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = DemoCopy.CALC_DISCLAIMER,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Muted,
                    )
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { showDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag(Tags.ACTIVATE),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Gold,
                        contentColor = Color(0xFF1A1408),
                    ),
                ) {
                    Text(stringResource(R.string.activate))
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            containerColor = InkRaised,
            titleContentColor = Text,
            textContentColor = Muted,
            title = { Text(stringResource(R.string.activate_dialog_title)) },
            text = { Text(DemoCopy.activationDialog(selected, cashback, offer.sampleRatePercent)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDialog = false
                        viewModel.activate(offer.id, selected)
                    },
                    modifier = Modifier.testTag(Tags.CONFIRM_ACTIVATE),
                ) {
                    Text(stringResource(R.string.activate_confirm), color = GoldBright)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.cancel), color = Muted)
                }
            },
        )
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(InkCard)
            .border(1.dp, Line, RoundedCornerShape(18.dp))
            .padding(16.dp),
    ) {
        Text(text = title, style = MaterialTheme.typography.titleMedium, color = Text)
        Spacer(Modifier.height(8.dp))
        content()
    }
}
