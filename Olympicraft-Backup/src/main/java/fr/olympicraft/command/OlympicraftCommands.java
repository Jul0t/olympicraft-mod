package fr.olympicraft.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import fr.olympicraft.config.OlympicraftConfigManager;
import fr.olympicraft.internal.BuildDefaults;
import fr.olympicraft.message.MessageService;
import fr.olympicraft.test.TestModeManager;
import fr.olympicraft.arena.ArenaManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

import java.util.Map;

public final class OlympicraftCommands {

    private OlympicraftCommands() {
    }

    public static void register(
            CommandDispatcher<CommandSourceStack> dispatcher,
            OlympicraftConfigManager configs,
            MessageService messages,
            TestModeManager testMode,
            ArenaManager arenas
    ) {
        LiteralArgumentBuilder<CommandSourceStack> root =
                Commands.literal(BuildDefaults.ROOT_COMMAND)
                        .executes(context -> showHelp(
                                context.getSource(),
                                messages
                        ))

                        .then(Commands.literal("help")
                                .executes(context -> showHelp(
                                        context.getSource(),
                                        messages
                                )))

                        .then(Commands.literal("status")
                                .executes(context -> showStatus(
                                        context.getSource(),
                                        configs,
                                        messages,
                                        testMode
                                )))

                        .then(createConfigBranch(
                                configs,
                                messages
                        ))

                        .then(createTestBranch(
                                configs,
                                messages,
                                testMode
                        ))

                        .then(ArenaCommands.create(
                                arenas,
                                messages
                        ));

        var rootNode = dispatcher.register(root);

        for (String alias : BuildDefaults.ROOT_COMMAND_ALIASES) {
            if (alias.equalsIgnoreCase(
                    BuildDefaults.ROOT_COMMAND
            )) {
                continue;
            }

            dispatcher.register(
                    Commands.literal(alias).redirect(rootNode)
            );
        }
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

                            boolean changed = testMode.enable();

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
                            boolean changed = testMode.disable();

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

        if (source.hasPermission(2)) {
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

        }

        return 1;
    }

    private static int showStatus(
            CommandSourceStack source,
            OlympicraftConfigManager configs,
            MessageService messages,
            TestModeManager testMode
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
                                BuildDefaults.DEFAULT_GAME_NAMES.size()
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
}
