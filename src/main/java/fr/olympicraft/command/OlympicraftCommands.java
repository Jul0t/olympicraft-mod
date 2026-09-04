package fr.olympicraft.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import fr.olympicraft.arena.ArenaManager;
import fr.olympicraft.arena.SelectionService;
import fr.olympicraft.arena.editor.ArenaEditorManager;
import fr.olympicraft.config.OlympicraftConfigManager;
import fr.olympicraft.game.GameRegistry;
import fr.olympicraft.gui.GuiManager;
import fr.olympicraft.gui.menu.ArenaListMenu;
import fr.olympicraft.gui.menu.OlympicraftMainMenu;
import fr.olympicraft.internal.BuildDefaults;
import fr.olympicraft.match.GameInstanceManager;
import fr.olympicraft.match.player.PlayerMatchService;
import fr.olympicraft.message.MessageService;
import fr.olympicraft.test.TestModeManager;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

public final class OlympicraftCommands {

    private OlympicraftCommands() {
    }

    public static void register(
            CommandDispatcher<CommandSourceStack> dispatcher,
            OlympicraftConfigManager configs,
            MessageService messages,
            TestModeManager testMode,
            ArenaManager arenas,
            GameRegistry games,
            GameInstanceManager matches,
            PlayerMatchService matchPlayers,
            SelectionService selectionService,
            ArenaEditorManager arenaEditor,
            GuiManager gui
    ) {
        dispatcher.register(
                createRoot(
                        BuildDefaults.ROOT_COMMAND,
                        configs,
                        messages,
                        testMode,
                        arenas,
                        games,
                        matches,
                        matchPlayers,
                        selectionService,
                        arenaEditor,
                        gui
                )
        );

        for (String alias :
                BuildDefaults.ROOT_COMMAND_ALIASES) {
            if (alias == null || alias.isBlank()) {
                continue;
            }

            if (alias.equalsIgnoreCase(
                    BuildDefaults.ROOT_COMMAND
            )) {
                continue;
            }

            dispatcher.register(
                    createRoot(
                            alias,
                            configs,
                            messages,
                            testMode,
                            arenas,
                            games,
                            matches,
                            matchPlayers,
                            selectionService,
                            arenaEditor,
                            gui
                    )
            );
        }
    }

    private static LiteralArgumentBuilder<CommandSourceStack>
    createRoot(
            String commandName,
            OlympicraftConfigManager configs,
            MessageService messages,
            TestModeManager testMode,
            ArenaManager arenas,
            GameRegistry games,
            GameInstanceManager matches,
            PlayerMatchService matchPlayers,
            SelectionService selectionService,
            ArenaEditorManager arenaEditor,
            GuiManager gui
    ) {
        return Commands.literal(commandName)

                .executes(context ->
                        showHelp(
                                context.getSource(),
                                messages
                        )
                )

                .then(
                        Commands.literal("menu")
                                .executes(context ->
                                        openMainMenu(
                                                context.getSource(),
                                                gui,
                                                messages
                                        )
                                )
                )

                .then(
                        Commands.literal("admin")
                                .requires(source ->
                                        source.hasPermission(2)
                                )
                                .executes(context ->
                                        openArenaMenu(
                                                context.getSource(),
                                                gui,
                                                messages
                                        )
                                )
                )

                .then(
                        Commands.literal("help")
                                .executes(context ->
                                        showHelp(
                                                context.getSource(),
                                                messages
                                        )
                                )
                )

                .then(
                        Commands.literal("enhanced")
                                .executes(context ->
                                        openEnhancedTest(
                                                context.getSource(),
                                                messages
                                        )
                                )
                )

                .then(
                        Commands.literal("status")
                                .executes(context ->
                                        showStatus(
                                                context.getSource(),
                                                configs,
                                                messages,
                                                testMode,
                                                games
                                        )
                                )
                )

                .then(
                        createConfigBranch(
                                configs,
                                messages
                        )
                )

                .then(
                        createTestBranch(
                                configs,
                                messages,
                                testMode
                        )
                )

                .then(
                        ArenaCommands.create(
                                arenas,
                                games,
                                selectionService,
                                arenaEditor,
                                configs,
                                gui,
                                messages
                        )
                )

                .then(
                        GameCommands.create(
                                matches,
                                arenas,
                                matchPlayers,
                                messages
                        )
                );
    }

    private static LiteralArgumentBuilder<CommandSourceStack>
    createConfigBranch(
            OlympicraftConfigManager configs,
            MessageService messages
    ) {
        return Commands.literal("config")
                .requires(source -> source.hasPermission(2))

                .then(Commands.literal("reload")
                        .executes(context -> {
                            boolean success = configs.loadAll();

                            messages.send(
                                    context.getSource(),
                                    success
                                            ? "command.config.reload.success"
                                            : "command.config.reload.failure"
                            );

                            return success ? 1 : 0;
                        }))

                .then(Commands.literal("save")
                        .executes(context -> {
                            boolean success = configs.saveAll();

                            messages.send(
                                    context.getSource(),
                                    success
                                            ? "command.config.save.success"
                                            : "command.config.save.failure"
                            );

                            return success ? 1 : 0;
                        }))

                .then(Commands.literal("status")
                        .executes(context -> {
                            messages.send(
                                    context.getSource(),
                                    "command.config.status",
                                    MessageService.placeholders(
                                            "state",
                                            state(
                                                    messages,
                                                    configs.isLoaded()
                                                            ? "loaded"
                                                            : "not_loaded"
                                            )
                                    ),
                                    true
                            );

                            return 1;
                        }));
    }

    private static LiteralArgumentBuilder<CommandSourceStack>
    createTestBranch(
            OlympicraftConfigManager configs,
            MessageService messages,
            TestModeManager testMode
    ) {
        return Commands.literal("test")
                .requires(source -> source.hasPermission(2))

                .then(Commands.literal("enable")
                        .executes(context -> {
                            if (!configs.general().testModeAllowed) {
                                messages.send(
                                        context.getSource(),
                                        "command.test.not_allowed"
                                );

                                return 0;
                            }

                            boolean changed =
                                    testMode.enable();

                            messages.send(
                                    context.getSource(),
                                    changed
                                            ? "command.test.enabled"
                                            : "command.test.already_enabled"
                            );

                            return changed ? 1 : 0;
                        }))

                .then(Commands.literal("disable")
                        .executes(context -> {
                            boolean changed =
                                    testMode.disable();

                            messages.send(
                                    context.getSource(),
                                    changed
                                            ? "command.test.disabled"
                                            : "command.test.already_disabled"
                            );

                            return changed ? 1 : 0;
                        }))

                .then(Commands.literal("status")
                        .executes(context -> {
                            messages.send(
                                    context.getSource(),
                                    "command.test.status",
                                    MessageService.placeholders(
                                            "state",
                                            state(
                                                    messages,
                                                    testMode.isEnabled()
                                                            ? "enabled"
                                                            : "disabled"
                                            )
                                    ),
                                    true
                            );

                            return 1;
                        }));
    }

    private static int showHelp(
            CommandSourceStack source,
            MessageService messages
    ) {
        messages.send(
                source,
                "command.help.header",
                Map.of(),
                false
        );

        helpLine(
                source,
                messages,
                "/oc help",
                "command.help.help"
        );

        helpLine(
                source,
                messages,
                "/oc status",
                "command.help.status"
        );

        helpLine(
                source,
                messages,
                "/oc menu",
                "command.help.menu"
        );

        helpLine(
                source,
                messages,
                "/oc game join <arène>",
                "command.help.game_join"
        );

        helpLine(
                source,
                messages,
                "/oc game leave",
                "command.help.game_leave"
        );

        helpLine(
                source,
                messages,
                "/oc game spectate <arène>",
                "command.help.game_spectate"
        );

        helpLine(
                source,
                messages,
                "/oc game list",
                "command.help.game_list"
        );

        helpLine(
                source,
                messages,
                "/oc game status <arène>",
                "command.help.game_status"
        );

        if (source.hasPermission(2)) {
            helpLine(
                    source,
                    messages,
                    "/oc admin",
                    "command.help.admin"
            );

            helpLine(
                    source,
                    messages,
                    "/oc config reload",
                    "command.help.config_reload"
            );

            helpLine(
                    source,
                    messages,
                    "/oc config save",
                    "command.help.config_save"
            );

            helpLine(
                    source,
                    messages,
                    "/oc config status",
                    "command.help.config_status"
            );

            helpLine(
                    source,
                    messages,
                    "/oc test enable",
                    "command.help.test_enable"
            );

            helpLine(
                    source,
                    messages,
                    "/oc test disable",
                    "command.help.test_disable"
            );

            helpLine(
                    source,
                    messages,
                    "/oc test status",
                    "command.help.test_status"
            );

            helpLine(
                    source,
                    messages,
                    "/oc arena list",
                    "command.help.arena_list"
            );

            helpLine(
                    source,
                    messages,
                    "/oc arena create <nom> <jeu>",
                    "command.help.arena_create"
            );

            helpLine(
                    source,
                    messages,
                    "/oc arena setlobby <arène>",
                    "command.help.arena_setlobby"
            );

            helpLine(
                    source,
                    messages,
                    "/oc arena setspectator <arène>",
                    "command.help.arena_setspectator"
            );

            helpLine(
                    source,
                    messages,
                    "/oc arena addspawn <arène> <groupe>",
                    "command.help.arena_addspawn"
            );

            helpLine(
                    source,
                    messages,
                    "/oc arena removespawn "
                            + "<arène> <groupe> <index>",
                    "command.help.arena_removespawn"
            );

            helpLine(
                    source,
                    messages,
                    "/oc arena listspawns <arène>",
                    "command.help.arena_listspawns"
            );

            helpLine(
                    source,
                    messages,
                    "/oc arena tp <arène> <position>",
                    "command.help.arena_tp"
            );

            helpLine(
                    source,
                    messages,
                    "/oc game start <arène>",
                    "command.help.game_start"
            );

            helpLine(
                    source,
                    messages,
                    "/oc game stop <arène>",
                    "command.help.game_stop"
            );
        }

        return 1;
    }

    private static int showStatus(
            CommandSourceStack source,
            OlympicraftConfigManager configs,
            MessageService messages,
            TestModeManager testMode,
            GameRegistry games
    ) {
        messages.send(
                source,
                "command.help.header",
                Map.of(),
                false
        );

        messages.send(
                source,
                "command.status.name",
                MessageService.placeholders(
                        "name",
                        BuildDefaults.MOD_NAME
                ),
                true
        );

        messages.send(
                source,
                "command.status.command",
                MessageService.placeholders(
                        "command",
                        BuildDefaults.ROOT_COMMAND
                ),
                true
        );

        messages.send(
                source,
                "command.status.server",
                MessageService.placeholders(
                        "state",
                        state(
                                messages,
                                testMode.isServerAttached()
                                        ? "attached"
                                        : "detached"
                        )
                ),
                true
        );

        messages.send(
                source,
                "command.status.test_mode",
                MessageService.placeholders(
                        "state",
                        state(
                                messages,
                                testMode.isEnabled()
                                        ? "enabled"
                                        : "disabled"
                        )
                ),
                true
        );

        messages.send(
                source,
                "command.status.games",
                MessageService.placeholders(
                        "count",
                        String.valueOf(
                                games.size()
                        )
                ),
                true
        );

        messages.send(
                source,
                "command.config.status",
                MessageService.placeholders(
                        "state",
                        state(
                                messages,
                                configs.isLoaded()
                                        ? "loaded"
                                        : "not_loaded"
                        )
                ),
                true
        );

        return 1;
    }

    private static void helpLine(
            CommandSourceStack source,
            MessageService messages,
            String command,
            String descriptionKey
    ) {
        messages.send(
                source,
                "command.help.line",
                MessageService.placeholders(
                        "command",
                        command,
                        "description",
                        messages.render(
                                descriptionKey,
                                false
                        ).getString()
                ),
                false
        );
    }

    private static String state(
            MessageService messages,
            String state
    ) {
        return messages.render(
                "state." + state,
                false
        ).getString();
    }

    private static int openMainMenu(
            CommandSourceStack source,
            GuiManager gui,
            MessageService messages
    ) {
        ServerPlayer player;

        try {
            player = source.getPlayerOrException();
        } catch (Exception exception) {
            messages.sendError(
                    source,
                    "Cette commande doit être exécutée "
                            + "par un joueur."
            );

            return 0;
        }

        gui.open(
                player,
                new OlympicraftMainMenu()
        );

        return 1;
    }

    private static int openArenaMenu(
            CommandSourceStack source,
            GuiManager gui,
            MessageService messages
    ) {
        ServerPlayer player;

        try {
            player = source.getPlayerOrException();
        } catch (Exception exception) {
            messages.sendError(
                    source,
                    "Cette commande doit être exécutée "
                            + "par un joueur."
            );

            return 0;
        }

        gui.open(
                player,
                new ArenaListMenu(0)
        );

        return 1;
    }
    private static int openEnhancedTest(
            CommandSourceStack source,
            MessageService messages
    ) {
        ServerPlayer player;

        try {
            player =
                    source.getPlayerOrException();
        } catch (Exception exception) {
            messages.sendError(
                    source,
                    "Cette commande doit être exécutée "
                            + "par un joueur."
            );

            return 0;
        }

        if (!fr.olympicraft.Olympicraft
                .enhancedClients()
                .isEnhanced(player)) {
            messages.sendWarning(
                    source,
                    "Le client Olympicraft Enhanced "
                            + "n'a pas été détecté."
            );

            return 0;
        }

        boolean opened =
                fr.olympicraft.Olympicraft
                        .enhancedClients()
                        .openTestScreen(player);

        if (!opened) {
            messages.sendError(
                    source,
                    "L'écran Enhanced n'a pas "
                            + "pu être ouvert."
            );

            return 0;
        }

        return 1;
    }
}
