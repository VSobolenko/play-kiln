package com.gexetr.plinthproductphotos.ui.components

import java.text.DateFormat
import java.util.Date
import java.util.Locale

internal fun projectDisplayTitle(
    name: String,
    createdAt: Long,
    locale: Locale = Locale.getDefault(),
): String {
    val title = name.trim()
    if (title.isNotEmpty() && !machineIdentifier.containsMatchIn(title)) return title
    return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale)
        .format(Date(createdAt))
}

private val machineIdentifier = Regex(
    "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}" +
        "|(?<![0-9a-f])[0-9a-f]{32}(?![0-9a-f])|^(?:job|request|output)[_-]\\S+",
    RegexOption.IGNORE_CASE,
)
