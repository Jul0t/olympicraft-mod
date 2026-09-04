package fr.olympicraft.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import fr.olympicraft.arena.ArenaManager;
import fr.olympicraft.match.GameInstance;
import fr.olympicraft.match.GameInstanceManager;
import fr.olympicraft.match.GameState;
import fr.olympicraft.match.player.PlayerMatchService;
import fr.olympicraft.message.MessageService;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class GameCommands {

    private GameCommands() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> create(
            GameInstanceManager matches,
            ArenaManager arenas,
            PlayerMatchService players,
            MessageService messages
    ) {
        return Commands.literal("game")

                .then(
                        GameDebugCommands.create(
                                players,
                                messages
                        )
                )

                .then(
                        DummyCommands.create(
                                matches,
                                arenas,
                                messages
                        )
                )

                .then(
                        Commands.literal("join")
                                .then(
                                        arenaArgument(arenas)
                                                .executes(context ->
                                                        join(
                                                                context.getSource(),
                                                                matches,
                                                                messages,
                                                                StringArgumentType.getString(
                                                                        context,
                                                                        "arena"
                                                                ),
                                                                false
                                                        )
                                                )
                                )
                )

                .then(
                        Commands.literal("spectate")
                                .then(
                                        arenaArgument(arenas)
                                                .executes(context ->
                                                        join(
                                                                context.getSource(),
                                                                matches,
                                                                messages,
                                                                StringArgumentType.getString(
                                                                        context,
                                                                        "arena"
                                                                ),
                                                                true
                                                        )
                                                )
                                )
                )

                .then(
                        Commands.literal("leave")
                                .executes(context ->
                                        leave(
                                                context.getSource(),
                                                matches,
                                                messages
                                        )
                                )
                )

                .then(
                        Commands.literal("list")
                                .executes(context ->
                                        list(
                                                context.getSource(),
                                                matches,
                                                messages
                                        )
                                )
                )

                .then(
                        Commands.literal("status")

                                /*
                                 * /oc game status
                                 * Utilise automatiquement la partie du joueur.
                                 */
                                .executes(context ->
                                        statusCurrent(
                                                context.getSource(),
                                                matches,
                                                messages
                                        )
                                )

                                /*
                                 * /oc game status <arène>
                                 */
                                .then(
                                        arenaArgument(arenas)
                                                .executes(context ->
                                                        status(
                                                                context.getSource(),
                                                                matches,
                                                                messages,
                                                                StringArgumentType.getString(
                                                                        context,
                                                                        "arena"
                                                                )
                                                        )
                                                )
                                )
                )

                .then(
                        Commands.literal("start")
                                .requires(source ->
                                        source.hasPermission(2)
                                )

                                /*
                                 * /oc game start
                                 * Lance la partie dans laquelle se trouve l'exécutant.
                                 */
                                .executes(context ->
                                        startCurrent(
                                                context.getSource(),
                                                matches,
                                                messages
                                        )
                                )

                                /*
                                 * /oc game start <arène>
                                 */
                                .then(
                                        arenaArgument(arenas)
                                                .executes(context ->
                                                        start(
                                                                context.getSource(),
                                                                matches,
                                                                messages,
                                                                StringArgumentType.getString(
                                                                        context,
                                                                        "arena"
                                                                )
                                                        )
                                                )
                                )
                )

                .then(
                        Commands.literal("stop")
                                .requires(source ->
                                        source.hasPermission(2)
                                )

                                /*
                                 * /oc game stop
                                 * Arrête la partie dans laquelle se trouve l'exécutant.
                                 */
                                .executes(context ->
                                        stopCurrent(
                                                context.getSource(),
                                                matches,
                                                messages
                                        )
                                )

                                /*
                                 * /oc game stop <arène>
                                 */
                                .then(
                                        arenaArgument(arenas)
                                                .executes(context ->
                                                        stop(
                                                                context.getSource(),
                                                                matches,
                                                                messages,
                                                                StringArgumentType.getString(
                                                                        context,
                                                                        "arena"
                                                                )
                                                        )
                                                )
                                )
                );
    }

    private static com.mojang.brigadier.builder
            .RequiredArgumentBuilder<CommandSourceStack, String>
    arenaArgument(ArenaManager arenas) {
        return Commands.argument(
                        "arena",
                        StringArgumentType.word()
                )
                .suggests((context, builder) -> {
                    for (String id : arenas.idsStartingWith(
                            builder.getRemaining()
                    )) {
                        builder.suggest(id);
                    }

                    return builder.buildFuture();
                });
    }

    private static int join(
            CommandSourceStack source,
            GameInstanceManager matches,
            MessageService messages,
            String arena,
            boolean spectator
    ) {
        ServerPlayer player =
                requirePlayer(source, messages);

        if (player == null) {
            return 0;
        }

        GameInstanceManager.JoinResult result =
                matches.join(
                        player,
                        arena,
                        spectator
                );

        if (!result.successful()) {
            messages.sendError(
                    source,
                    result.error()
            );

            return 0;
        }

        if (spectator) {
            messages.sendSuccess(
                    source,
                    "Tu observes maintenant l'arène "
                            + result.instance().arena().id
                            + "."
            );
        } else {
            messages.sendSuccess(
                    source,
                    "Tu as rejoint l'arène "
                            + result.instance().arena().id
                            + "."
            );
        }

        return 1;
    }

    private static int leave(
            CommandSourceStack source,
            GameInstanceManager matches,
            MessageService messages
    ) {
        ServerPlayer player =
                requirePlayer(source, messages);

        if (player == null) {
            return 0;
        }

        GameInstanceManager.LeaveResult result =
                matches.leave(player);

        if (!result.successful()) {
            messages.sendError(
                    source,
                    result.error()
            );

            return 0;
        }

        messages.sendSuccess(
                source,
                "Tu as quitté la partie et ton état "
                        + "a été restauré."
        );

        return 1;
    }

    private static int list(
            CommandSourceStack source,
            GameInstanceManager matches,
            MessageService messages
    ) {
        if (matches.all().isEmpty()) {
            messages.sendWarning(
                    source,
                    "Aucune partie n'est actuellement active."
            );

            return 0;
        }

        source.sendSuccess(
                () -> Component.literal(
                        "Parties Olympicraft"
                ).withStyle(ChatFormatting.AQUA),
                false
        );

        for (GameInstance instance : matches.all()) {
            Component line = Component.empty()

                    .append(
                            Component.literal(
                                    instance.arena().id
                            ).withStyle(
                                    ChatFormatting.AQUA
                            )
                    )

                    .append(
                            Component.literal(" — ")
                                    .withStyle(
                                            ChatFormatting.DARK_GRAY
                                    )
                    )

                    .append(
                            Component.literal(
                                    instance.state().displayName()
                            ).withStyle(
                                    stateColor(
                                            instance.state()
                                    )
                            )
                    )

                    .append(
                            Component.literal(
                                    " — "
                                            + instance.playerCount()
                                            + " joueur(s), "
                                            + instance.spectatorCount()
                                            + " spectateur(s)"
                            ).withStyle(
                                    ChatFormatting.GRAY
                            )
                    );

            source.sendSuccess(
                    () -> line,
                    false
            );
        }

        return matches.all().size();
    }

    private static int statusCurrent(
            CommandSourceStack source,
            GameInstanceManager matches,
            MessageService messages
    ) {
        GameInstance instance =
                currentInstance(
                        source,
                        matches,
                        messages
                );

        if (instance == null) {
            return 0;
        }

        return status(
                source,
                matches,
                messages,
                instance.arena().id
        );
    }

    private static int startCurrent(
            CommandSourceStack source,
            GameInstanceManager matches,
            MessageService messages
    ) {
        GameInstance instance =
                currentInstance(
                        source,
                        matches,
                        messages
                );

        if (instance == null) {
            return 0;
        }

        return start(
                source,
                matches,
                messages,
                instance.arena().id
        );
    }

    private static int stopCurrent(
            CommandSourceStack source,
            GameInstanceManager matches,
            MessageService messages
    ) {
        GameInstance instance =
                currentInstance(
                        source,
                        matches,
                        messages
                );

        if (instance == null) {
            return 0;
        }

        return stop(
                source,
                matches,
                messages,
                instance.arena().id
        );
    }

    private static GameInstance currentInstance(
            CommandSourceStack source,
            GameInstanceManager matches,
            MessageService messages
    ) {
        ServerPlayer player =
                requirePlayer(source, messages);

        if (player == null) {
            return null;
        }

        GameInstance instance =
                matches.findByPlayer(
                        player.getUUID()
                ).orElse(null);

        if (instance == null) {
            messages.sendError(
                    source,
                    "Tu ne participes actuellement "
                            + "à aucune partie."
            );

            return null;
        }

        return instance;
    }

    private static int status(
            CommandSourceStack source,
            GameInstanceManager matches,
            MessageService messages,
            String arena
    ) {
        GameInstance instance =
                matches.findByArena(arena)
                        .orElse(null);

        if (instance == null) {
            messages.sendError(
                    source,
                    "Aucune partie n'existe pour cette arène."
            );

            return 0;
        }

        messages.sendInfo(
                source,
                "Arène : " + instance.arena().id
        );

        messages.sendInfo(
                source,
                "État : "
                        + instance.state().displayName()
        );

        messages.sendInfo(
                source,
                "Joueurs : "
                        + instance.playerCount()
        );

        messages.sendInfo(
                source,
                "Spectateurs : "
                        + instance.spectatorCount()
        );

        if (instance.state() == GameState.STARTING) {
            messages.sendInfo(
                    source,
                    "Démarrage dans : "
                            + instance.countdownSeconds()
                            + " seconde(s)"
            );
        }

        if (instance.state() == GameState.RUNNING) {
            messages.sendInfo(
                    source,
                    "Temps restant : "
                            + instance.remainingSeconds()
                            + " seconde(s)"
            );
        }

        return 1;
    }

    private static int start(
            CommandSourceStack source,
            GameInstanceManager matches,
            MessageService messages,
            String arena
    ) {
        GameInstanceManager.StartResult result =
                matches.start(arena);

        if (!result.successful()) {
            messages.sendError(
                    source,
                    result.error()
            );

            return 0;
        }

        messages.sendSuccess(
                source,
                "Décompte lancé pour l'arène "
                        + result.instance().arena().id
                        + "."
        );

        return 1;
    }

    private static int stop(
            CommandSourceStack source,
            GameInstanceManager matches,
            MessageService messages,
            String arena
    ) {
        GameInstanceManager.StopResult result =
                matches.stop(arena);

        if (!result.successful()) {
            messages.sendError(
                    source,
                    result.error()
            );

            return 0;
        }

        messages.sendSuccess(
                source,
                "Arrêt demandé pour l'arène "
                        + result.instance().arena().id
                        + "."
        );

        return 1;
    }

    private static ServerPlayer requirePlayer(
            CommandSourceStack source,
            MessageService messages
    ) {
        try {
            return source.getPlayerOrException();
        } catch (Exception exception) {
            messages.sendError(
                    source,
                    "Cette commande doit être exécutée "
                            + "par un joueur."
            );

            return null;
        }
    }

    private static ChatFormatting stateColor(
            GameState state
    ) {
        return switch (state) {
            case INACTIVE ->
                    ChatFormatting.DARK_GRAY;

            case WAITING ->
                    ChatFormatting.YELLOW;

            case STARTING ->
                    ChatFormatting.GOLD;

            case RUNNING ->
                    ChatFormatting.GREEN;

            case ENDING ->
                    ChatFormatting.RED;

            case RESETTING ->
                    ChatFormatting.LIGHT_PURPLE;
        };
    }
}