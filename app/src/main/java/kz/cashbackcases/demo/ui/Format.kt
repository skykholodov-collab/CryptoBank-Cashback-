package kz.cashbackcases.demo.ui

import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val almatyZone = ZoneOffset.ofHours(5)
private val almatyFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", Locale.forLanguageTag("ru"))

fun formatEpoch(epochMs: Long): String {
    return Instant.ofEpochMilli(epochMs).atZone(almatyZone).format(almatyFormatter)
}
