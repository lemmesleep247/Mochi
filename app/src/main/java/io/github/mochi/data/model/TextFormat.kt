package io.github.mochi.data.model

/** MAL's API returns enum-ish fields as snake_case ("plan_to_watch"); this renders them for display. */
fun String.prettify(): String =
    split('_').filter { it.isNotBlank() }.joinToString(" ") { it.replaceFirstChar(Char::uppercase) }
