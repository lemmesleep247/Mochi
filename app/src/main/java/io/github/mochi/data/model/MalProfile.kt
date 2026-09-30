package io.github.mochi.data.model

data class MalProfile(
    val name: String = "",
    val picture: String = "",
    val gender: String = "",
    val location: String = "",
    val birthday: String = "",
    val joinedAt: String = "",
    val animeDaysWatched: Double = 0.0,
    val animeMeanScore: Double = 0.0,
    val animeEpisodesWatched: Int = 0,
    val animeTotalEntries: Int = 0,
    val animeWatching: Int = 0,
    val animeCompleted: Int = 0,
    val animeOnHold: Int = 0,
    val animeDropped: Int = 0,
    val animePlanToWatch: Int = 0,
    // Manga stats have no equivalent in MAL's official API at all — only ever
    // populated by scraping the profile page (see MalProfileScrapeApi), hence
    // [hasMangaStats] to distinguish "not loaded" from "genuinely zero".
    val mangaDaysRead: Double = 0.0,
    val mangaMeanScore: Double = 0.0,
    val mangaChaptersRead: Int = 0,
    val mangaVolumesRead: Int = 0,
    val mangaReread: Int = 0,
    val mangaTotalEntries: Int = 0,
    val mangaReading: Int = 0,
    val mangaCompleted: Int = 0,
    val mangaOnHold: Int = 0,
    val mangaDropped: Int = 0,
    val mangaPlanToRead: Int = 0,
    val hasMangaStats: Boolean = false,
)
