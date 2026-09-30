package io.github.mochi.data.model

enum class MediaType(val path: String) {
    Anime("anime"),
    Manga("manga"),
}

enum class ListStatus(val apiValue: String) {
    Watching("watching"),
    Reading("reading"),
    Completed("completed"),
    OnHold("on_hold"),
    Dropped("dropped"),
    PlanToWatch("plan_to_watch"),
    PlanToRead("plan_to_read");

    companion object {
        fun from(apiValue: String?): ListStatus = entries.firstOrNull { it.apiValue == apiValue } ?: PlanToWatch

        fun planFor(type: MediaType) = if (type == MediaType.Anime) PlanToWatch else PlanToRead
    }
}

fun ListStatus.label(): String = when (this) {
    ListStatus.Watching -> "Watching"
    ListStatus.Reading -> "Reading"
    ListStatus.Completed -> "Completed"
    ListStatus.OnHold -> "On Hold"
    ListStatus.Dropped -> "Dropped"
    ListStatus.PlanToWatch -> "Plan to Watch"
    ListStatus.PlanToRead -> "Plan to Read"
}

/** The statuses selectable for this media type, in display order. */
fun MediaType.listStatuses(): List<ListStatus> = if (this == MediaType.Anime) {
    listOf(ListStatus.Watching, ListStatus.Completed, ListStatus.OnHold, ListStatus.Dropped, ListStatus.PlanToWatch)
} else {
    listOf(ListStatus.Reading, ListStatus.Completed, ListStatus.OnHold, ListStatus.Dropped, ListStatus.PlanToRead)
}
