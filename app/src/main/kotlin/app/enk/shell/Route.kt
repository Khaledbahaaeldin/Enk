package app.enk.shell

enum class Dest { Write, Box, Find, Setup }

sealed interface Route {
    object Writer : Route
    object Box : Route
    object Find : Route
    object Setup : Route
    object Trash : Route
    object Credits : Route
    data class Note(val id: String, val page: Int) : Route
}

fun navSelection(route: Route): Dest? = when (route) {
    Route.Writer, is Route.Note -> Dest.Write
    Route.Box, Route.Trash -> Dest.Box
    Route.Find -> Dest.Find
    Route.Setup, Route.Credits -> Dest.Setup
}

fun routeBack(current: Route): Route? = when (current) {
    Route.Box, Route.Find, Route.Setup -> Route.Writer
    Route.Credits -> Route.Setup
    Route.Trash, is Route.Note -> Route.Box
    Route.Writer -> null
}

/** The route a nav bar destination opens. */
fun Dest.toRoute(): Route = when (this) {
    Dest.Write -> Route.Writer
    Dest.Box -> Route.Box
    Dest.Find -> Route.Find
    Dest.Setup -> Route.Setup
}
