package io.github.mochi.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
object ListRoute

@Serializable
object DiscoverRoute

@Serializable
object ProfileRoute

@Serializable
object MalLoginRoute

@Serializable
data class DetailRoute(val id: Int, val type: String)
