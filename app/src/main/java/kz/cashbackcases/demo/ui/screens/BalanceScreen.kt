package kz.cashbackcases.demo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kz.cashbackcases.core.CashbackEntry
import kz.cashbackcases.core.CashbackStatus
import kz.cashbackcases.core.DemoCopy
import kz.cashbackcases.core.WithdrawalPolicy
import kz.cashbackcases.core.formatKzt
import kz.cashbackcases.demo.R
import kz.cashbackcases.demo.ui.CashbackViewModel
import kz.cashbackcases.demo.ui.Tags
import kz.cashbackcases.demo.ui.formatEpoch
import kz.cashbackcases.demo.ui.theme.Cyan
import kz.cashbackcases.demo.ui.theme.Gold
import kz.cashbackcases.demo.ui.theme.GoldBright
import kz.cashbackcases.demo.ui.theme.Ink
import kz.cashbackcases.demo.ui.theme.InkCard
import kz.cashbackcases.demo.ui.theme.InkRaised
import kz.cashbackcases.demo.ui.theme.Line
import kz.cashbackcases.demo.ui.theme.Muted
import kz.cashbackcases.demo.ui.theme.Text

@Composable
fun BalanceScreen(viewModel: CashbackViewModel) {
    val state by viewModel.state.collectAsState()
    val withdrawal = WithdrawalPolicy.request(state)
    var confirmId by remember { mutableStateOf<String?>(null) }
    var showReset by remember { mutableStateOf(false) }
    val entries = state.entries.sortedByDescending { it.createdAtEpochMs }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Text(
            text = stringResource(R.string.balance_title),
            style = MaterialTheme.typography.displaySmall,
            color = Text,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.balance_intro),
            style = MaterialTheme.typography.bodyLarge,
            color = Muted,
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                label = stringResource(R.string.pending_label),
                amount = formatKzt(state.pendingKzt),
                amountTag = Tags.BALANCE_PENDING,
                accent = GoldBright,
                modifier = Modifier.weight(1f),
            )
            StatTile(
                label = stringResource(R.string.confirmed_label),
                amount = formatKzt(state.confirmedKzt),
                amountTag = Tags.BALANCE_CONFIRMED,
                accent = Cyan,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(text = stringResource(R.string.pending_help), style = MaterialTheme.typography.bodyMedium, color = Muted)
        Spacer(Modifier.height(4.dp))
        Text(text = stringResource(R.string.confirmed_help), style = MaterialTheme.typography.bodyMedium, color = Muted)
        Spacer(Modifier.height(18.dp))
        Text(text = stringResource(R.string.history_title), style = MaterialTheme.typography.titleLarge, color = Text)
        Text(text = stringResource(R.string.history_note), style = MaterialTheme.typography.bodyMedium, color = Muted)
        Spacer(Modifier.height(10.dp))
        if (entries.isEmpty()) {
            Text(text = stringResource(R.string.empty_history), color = Muted)
        } else {
            entries.forEach { entry ->
                HistoryCard(entry = entry, onConfirm = { confirmId = entry.id })
            }
        }
        Spacer(Modifier.height(8.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF221C22))
                .border(1.dp, Color(0xFF5A3E46), RoundedCornerShape(18.dp))
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Lock, contentDescription = null, tint = Color(0xFFE7B4B4))
                Text(
                    text = stringResource(R.string.withdrawal_title),
                    modifier = Modifier.padding(start = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = Text,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(text = withdrawal.message, style = MaterialTheme.typography.bodyMedium, color = Muted)
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {},
                enabled = WithdrawalPolicy.availableInDemo,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(Tags.WITHDRAWAL),
                colors = ButtonDefaults.buttonColors(
                    disabledContainerColor = Color(0xFF3A3032),
                    disabledContentColor = Color(0xFFB7A4A6),
                ),
            ) {
                Text(stringResource(R.string.withdrawal_button))
            }
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = { showReset = true },
            modifier = Modifier
                .fillMaxWidth()
                .testTag(Tags.RESET),
        ) {
            Text(stringResource(R.string.reset_demo), color = GoldBright)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.version_line),
            style = MaterialTheme.typography.labelMedium,
            color = Muted,
        )
        Spacer(Modifier.height(12.dp))
    }
    val pendingConfirm = entries.firstOrNull { it.id == confirmId }
    if (pendingConfirm != null) {
        AlertDialog(
            onDismissRequest = { confirmId = null },
            containerColor = InkRaised,
            titleContentColor = Text,
            textContentColor = Muted,
            title = { Text(stringResource(R.string.confirm_dialog_title)) },
            text = {
                Text(
                    "${pendingConfirm.merchantName}. ${formatKzt(pendingConfirm.cashbackKzt)}. ${DemoCopy.CONFIRM_LOCAL_BODY}",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = pendingConfirm.id
                        confirmId = null
                        viewModel.confirmLocally(id)
                    },
                    modifier = Modifier.testTag(Tags.CONFIRM_STATUS),
                ) {
                    Text(stringResource(R.string.confirm_action), color = Cyan)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmId = null }) {
                    Text(stringResource(R.string.cancel), color = Muted)
                }
            },
        )
    }
    if (showReset) {
        AlertDialog(
            onDismissRequest = { showReset = false },
            containerColor = InkRaised,
            titleContentColor = Text,
            textContentColor = Muted,
            title = { Text(stringResource(R.string.reset_dialog_title)) },
            text = { Text(DemoCopy.RESET_BODY) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showReset = false
                        viewModel.reset()
                    },
                    modifier = Modifier.testTag(Tags.CONFIRM_RESET),
                ) {
                    Text(stringResource(R.string.reset_action), color = GoldBright)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReset = false }) {
                    Text(stringResource(R.string.cancel), color = Muted)
                }
            },
        )
    }
}

@Composable
private fun StatTile(
    label: String,
    amount: String,
    amountTag: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(InkCard)
            .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
            .padding(14.dp),
    ) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = Muted)
        Spacer(Modifier.height(6.dp))
        Text(
            text = amount,
            modifier = Modifier.testTag(amountTag),
            style = MaterialTheme.typography.titleLarge,
            color = accent,
        )
    }
}

@Composable
private fun HistoryCard(entry: CashbackEntry, onConfirm: () -> Unit) {
    val pending = entry.status == CashbackStatus.PENDING
    val accent = if (pending) Gold else Cyan
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(InkCard)
            .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = entry.merchantName,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                color = Text,
            )
            Text(
                text = if (pending) stringResource(R.string.pending_label) else stringResource(R.string.confirmed_label),
                modifier = Modifier
                    .clip(RoundedCornerShape(99.dp))
                    .background(accent.copy(alpha = 0.15f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                color = accent,
                style = MaterialTheme.typography.labelMedium,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.purchase_line, formatKzt(entry.samplePurchaseKzt)),
            style = MaterialTheme.typography.bodyMedium,
            color = Muted,
        )
        Text(
            text = "${formatKzt(entry.samplePurchaseKzt)} × ${entry.sampleRatePercent}% = ${formatKzt(entry.cashbackKzt)}",
            style = MaterialTheme.typography.titleMedium,
            color = Text,
        )
        Text(
            text = stringResource(R.string.created_at, formatEpoch(entry.createdAtEpochMs)),
            style = MaterialTheme.typography.labelMedium,
            color = Muted,
        )
        val confirmedAt = entry.confirmedAtEpochMs
        if (confirmedAt != null) {
            Text(
                text = stringResource(R.string.confirmed_at, formatEpoch(confirmedAt)),
                style = MaterialTheme.typography.labelMedium,
                color = Muted,
            )
        }
        if (pending) {
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = onConfirm,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(Tags.confirmOffer(entry.offerId)),
            ) {
                Text(stringResource(R.string.confirm_local), color = Cyan)
            }
        }
    }
}
