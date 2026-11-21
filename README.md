# FractalCosmetics

Collectibles and Mystery Boxes for Fractal Network's lobby: hats, suits, gadgets, cloaks, emotes, gestures,
the Mystery Vault and Mystery Dust. Built by Sasha Wildfang.

Runs on Paper 1.21.11+ next to [FractalLobby](https://github.com/SashaWildfang/FractalLobby), which it needs
(`depend: [FractalLobby]`). It uses FractalLobby's menus, holograms, trails, chat colours and scoreboard, and plugs
back in for the Collectibles hotbar item, `%lobby_dust%` / `%lobby_boxes%`, box-found trails and chat colours, and
parkour rewards.

The first start copies `mystery-boxes.yml`, `mystery-vault.yml` and the `mystery-boxes` / `collectibles` settings
over from the Lobby plugin's folder.

## Commands
`/mysterybox`, `/collectibles` (`/cosmetics`, `/wardrobe`), `/emote`, `/gesture`, `/dust`; admins:
`/mysterybox give|dust|reset|reload|vault`. Permissions are unchanged (`lobby.admin`, `lobby.mystery.*`,
`lobby.collectibles.*`).

## Building
Clone FractalLobby next to this repo (as `FractalLobby` or `LobbyPlugin`, or pass `-PfractalLobby=<path>`), then:

    ./gradlew build

The jar is `build/libs/FractalCosmetics-<version>.jar`. Needs JDK 21.
