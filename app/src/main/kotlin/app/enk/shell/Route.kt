package app.enk.shell

enum class Dest { Write, Box, Find, Setup }

sealed interface Route {
    object Writer : Route
    object Box : Route
    object Find : Route
    object Setup : Route
    object Trash : Route
    data class Note(val id: String, val page: Int) : Route
}

fun navSelection(route: Route): Dest? = when (route) {
    Route.Writer, is Route.Note -> Dest.Write
    Route.Box, Route.Trash -> Dest.Box
    Route.Find -> Dest.Find
    Route.Setup -> Dest.Setup
}

fun routeBack(current: Route): Route? = when (current) {
    Route.Box, Route.Find, Route.Setup -> Route.Writer
    Route.Trash, is Route.Note -> Route.Box
    Route.Writer -> null
}
