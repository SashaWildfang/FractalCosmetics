/*
 * FractalCosmetics - built by Sasha Wildfang for Fractal Network.
 */
package com.sasha.cosmetics;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.sasha.cosmetics.collectibles.Catalog;
import com.sasha.cosmetics.collectibles.Collectibles;
import com.sasha.cosmetics.mystery.MysteryBoxes;
import com.sasha.lobby.Lobby;
import com.sasha.lobby.api.CosmeticsHook;
import com.sasha.lobby.config.LobbyConfig;
import com.sasha.lobby.config.Messages;
import com.sasha.lobby.items.ChatColors;
import com.sasha.lobby.items.ConfirmMenu;
import com.sasha.lobby.items.MenuStyle;
import com.sasha.lobby.items.Trails;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * FractalCosmetics: the lobby's Collectibles (hats, suits, gadgets, cloaks, emotes, gestures) and Mystery Boxes
 * (the vault, Mystery Dust, crafting). It runs alongside FractalLobby, using its menus, holograms, trails, chat
 * colours and scoreboard, and plugs back into it through {@link CosmeticsHook} (the hotbar item, %lobby_dust% and
 * %lobby_boxes%, unlocked trails and chat colours, parkour rewards).
 *
 * <p>Built by Sasha Wildfang for Fractal Network.
 */
public final class FractalCosmetics extends JavaPlugin {

    private Lobby lobby;
    private Messages messages;
    private MysteryBoxes mystery;
    private Collectibles collectibles;

    @Override
    public void onEnable() {
        lobby = JavaPlugin.getPlugin(Lobby.class);
        copyFromLobby();
        saveDefaultConfig();
        loadMessages();
        mystery = new MysteryBoxes(this);
        collectibles = new Collectibles(this);
        getServer().getPluginManager().registerEvents(mystery, this);
        getServer().getPluginManager().registerEvents(collectibles, this);
        getServer().getPluginManager().registerEvents(collectibles.effectsListener(), this);
        registerCommands();
        mystery.start();
        collectibles.start();
        lobby.cosmetics(new Hook());
        for (Player player : getServer().getOnlinePlayers()) collectibles.apply(player);
    }

    @Override
    public void onDisable() {
        if (lobby != null) lobby.cosmetics(null);
        if (collectibles != null) collectibles.stop();
        if (mystery != null) mystery.stop();
    }

    /** /mysterybox reload (and FractalLobby's /lobby reload): config.yml and messages.yml again. */
    public void reload() {
        saveDefaultConfig();
        reloadConfig();
        loadMessages();
        mystery.reload();
        collectibles.start();
    }

    /**
     * messages.yml, with FractalLobby's choice of click hints: the resource pack's mouse and Shift pictures
     * (menus.custom-gui in FractalLobby's config.yml, on by default) or the words "Left-click", "Right-click".
     */
    private void loadMessages() {
        messages = Messages.load(this);
        var config = lobby.getConfig();
        messages.buttonIcons(config.getBoolean("menus.custom-gui", config.getBoolean("settings-menu.custom-gui", true)));
    }

    // ---------------------------------------------------------------- what the cosmetics use

    public Messages messages() {
        return messages;
    }

    public MysteryBoxes mystery() {
        return mystery;
    }

    public Collectibles collectibles() {
        return collectibles;
    }

    /** FractalLobby itself. */
    public Lobby lobby() {
        return lobby;
    }

    public MenuStyle menuStyle() {
        return lobby.menuStyle();
    }

    public ConfirmMenu confirm() {
        return lobby.confirm();
    }

    public LobbyConfig lobbyConfig() {
        return lobby.lobbyConfig();
    }

    public com.sasha.lobby.hologram.Holograms holograms() {
        return lobby.holograms();
    }

    public com.sasha.lobby.parkour.ParkourLobby parkour() {
        return lobby.parkour();
    }

    public com.sasha.lobby.listener.DoubleJump doubleJump() {
        return lobby.doubleJump();
    }

    public com.sasha.lobby.scoreboard.DisplayService display() {
        return lobby.display();
    }

    public Trails trails() {
        return lobby.trails();
    }

    public ChatColors chatColors() {
        return lobby.chatColors();
    }

    // ---------------------------------------------------------------- FractalLobby's side

    /** What FractalLobby asks the cosmetics for. */
    private final class Hook implements CosmeticsHook {
        @Override
        public String dust(UUID player) {
            var stash = mystery.stash(player);
            return MysteryBoxes.compact(stash == null ? 0 : stash.dust());
        }

        @Override
        public String boxes(UUID player) {
            var stash = mystery.stash(player);
            return MysteryBoxes.compact(stash == null ? 0 : stash.totalBoxes());
        }

        @Override
        public boolean unlocked(Player player, String reward) {
            return mystery.owns(player, reward);
        }

        @Override
        public boolean anyChatColor(Player player) {
            return mystery.stash(player.getUniqueId()).owned().stream().anyMatch(id -> id.startsWith("chatcolor:"));
        }

        @Override
        public boolean oldHotbar() {
            return Collectibles.oldHotbar(lobby);
        }

        @Override
        public void applyJoinItems(Player player) {
            collectibles.apply(player);
        }

        @Override
        public void swapKey(Player player) {
            collectibles.swapKey(player);
        }

        @Override
        public boolean blocksDoubleJump(Player player) {
            return collectibles.blocksDoubleJump(player);
        }

        @Override
        public void parkourBest(Player player, int score) {
            mystery.parkourBest(player, score);
        }

        @Override
        public void reload() {
            FractalCosmetics.this.reload();
        }
    }

    // ---------------------------------------------------------------- moving over from the Lobby plugin

    /**
     * The first time it starts, FractalCosmetics brings over what the Lobby plugin used to keep: the boxes, dust and
     * collectibles (mystery-boxes.yml), the vaults (mystery-vault.yml), and the server's Mystery Box and Collectibles
     * settings from its config.yml. The Lobby's own copies are left as they were.
     */
    private void copyFromLobby() {
        Path folder = getDataFolder().toPath();
        if (Files.exists(folder.resolve("config.yml"))) return;
        Path from = lobby.getDataFolder().toPath();
        if (!Files.isDirectory(from) || !Files.exists(from.resolve("mystery-boxes.yml"))) {
            from = folder.resolveSibling("Lobby"); // FractalLobby's own copy may not exist yet
        }
        try {
            Files.createDirectories(folder);
            for (String file : List.of("mystery-boxes.yml", "mystery-vault.yml")) {
                if (Files.exists(from.resolve(file))) Files.copy(from.resolve(file), folder.resolve(file), StandardCopyOption.COPY_ATTRIBUTES);
            }
            saveDefaultConfig();
            File config = folder.resolve("config.yml").toFile();
            YamlConfiguration lobbyConfig = YamlConfiguration.loadConfiguration(from.resolve("config.yml").toFile());
            YamlConfiguration ours = YamlConfiguration.loadConfiguration(config);
            boolean changed = false;
            for (String section : List.of("mystery-boxes", "collectibles")) {
                ConfigurationSection theirs = lobbyConfig.getConfigurationSection(section);
                if (theirs == null) continue;
                for (String key : theirs.getKeys(true)) {
                    if (!theirs.isConfigurationSection(key)) ours.set(section + "." + key, theirs.get(key));
                }
                changed = true;
            }
            if (changed) ours.save(config);
            getLogger().info("Brought over the Mystery Boxes, Collectibles and their settings from " + from.getFileName() + ".");
        } catch (IOException e) {
            getLogger().severe("Could not bring over the Lobby's cosmetics data: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------- commands

    private void registerCommands() {
        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Commands commands = event.registrar();

            commands.register(Commands.literal("mysterybox")
                    .executes(context -> asPlayer(context.getSource(), mystery::openMenu))
                    .then(Commands.literal("give").requires(source -> source.getSender().hasPermission("lobby.admin"))
                            .then(Commands.argument("player", StringArgumentType.word())
                                    .then(Commands.argument("stars", IntegerArgumentType.integer(1, 5))
                                            .executes(context -> {
                                                mystery.giveCommand(context.getSource().getSender(), StringArgumentType.getString(context, "player"),
                                                        IntegerArgumentType.getInteger(context, "stars"), 1);
                                                return Command.SINGLE_SUCCESS;
                                            })
                                            .then(Commands.argument("amount", IntegerArgumentType.integer(1, 1000))
                                                    .executes(context -> {
                                                        mystery.giveCommand(context.getSource().getSender(), StringArgumentType.getString(context, "player"),
                                                                IntegerArgumentType.getInteger(context, "stars"),
                                                                IntegerArgumentType.getInteger(context, "amount"));
                                                        return Command.SINGLE_SUCCESS;
                                                    })))))
                    .then(Commands.literal("dust").requires(source -> source.getSender().hasPermission("lobby.admin"))
                            .then(Commands.argument("player", StringArgumentType.word())
                                    .then(Commands.argument("amount", LongArgumentType.longArg(-1_000_000, 1_000_000))
                                            .executes(context -> {
                                                mystery.dustCommand(context.getSource().getSender(), StringArgumentType.getString(context, "player"),
                                                        LongArgumentType.getLong(context, "amount"));
                                                return Command.SINGLE_SUCCESS;
                                            }))))
                    .then(Commands.literal("reset").requires(source -> source.getSender().hasPermission("lobby.admin"))
                            .then(Commands.argument("player", StringArgumentType.word())
                                    .executes(context -> {
                                        mystery.resetCommand(context.getSource().getSender(), StringArgumentType.getString(context, "player"));
                                        return Command.SINGLE_SUCCESS;
                                    })))
                    .then(Commands.literal("reload").requires(source -> source.getSender().hasPermission("lobby.admin"))
                            .executes(context -> {
                                reload();
                                messages.send(context.getSource().getSender(), "mystery.reloaded");
                                return Command.SINGLE_SUCCESS;
                            }))
                    .then(Commands.literal("vault").requires(source -> source.getSender().hasPermission("lobby.admin"))
                            .then(Commands.literal("add").executes(context -> asPlayer(context.getSource(), player -> mystery.vault().add(player))))
                            .then(Commands.literal("set").executes(context -> asPlayer(context.getSource(), player -> mystery.vault().add(player))))
                            .then(Commands.literal("remove").executes(context -> asPlayer(context.getSource(), player -> mystery.vault().remove(player))))
                            .then(Commands.literal("list").executes(context -> {
                                mystery.vault().list(context.getSource().getSender());
                                return Command.SINGLE_SUCCESS;
                            })))
                    .build(), "Open your Mystery Boxes", List.of("mysteryboxes", "mb", "boxes"));

            commands.register(Commands.literal("collectibles")
                    .executes(context -> asPlayer(context.getSource(), collectibles::open))
                    .build(), "Open your Collectibles: hats, suits, gadgets, cloaks, emotes and gestures", List.of("cosmetics", "wardrobe"));

            for (var category : List.of(Catalog.Category.EMOTE, Catalog.Category.GESTURE)) {
                String name = category == Catalog.Category.EMOTE ? "emote" : "gesture";
                commands.register(Commands.literal(name)
                        .executes(context -> asPlayer(context.getSource(), collectibles::open))
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests((context, builder) -> {
                                    if (context.getSource().getSender() instanceof Player p) {
                                        for (String key : collectibles.owned(p, category)) if (key.startsWith(builder.getRemainingLowerCase())) builder.suggest(key);
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(context -> asPlayer(context.getSource(), player ->
                                        collectibles.use(player, category, StringArgumentType.getString(context, "name")))))
                        .build(), name.equals("emote") ? "Do an emote (a big emoji over your head)" : "Do a gesture (wave, clap, spin...)", List.of());
            }

            commands.register(Commands.literal("dust")
                    .executes(context -> asPlayer(context.getSource(), mystery::showDust))
                    .build(), "See your Mystery Dust", List.of("mysterydust"));
        });
    }

    private int asPlayer(CommandSourceStack source, Consumer<Player> action) {
        if (source.getSender() instanceof Player player) {
            action.accept(player);
        } else {
            source.getSender().sendMessage("Only players can do that.");
        }
        return Command.SINGLE_SUCCESS;
    }
}
