rootProject.name = "FractalCosmetics"

// FractalCosmetics builds against FractalLobby's code (its menus, messages, holograms...).
// Expects the FractalLobby repo next to this one (as FractalLobby or LobbyPlugin); change with -PfractalLobby=<path>.
includeBuild(providers.gradleProperty("fractalLobby").getOrElse(
    if (file("../FractalLobby").isDirectory) "../FractalLobby" else "../LobbyPlugin"))
