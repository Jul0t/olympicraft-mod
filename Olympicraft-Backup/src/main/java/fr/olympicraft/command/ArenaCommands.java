package fr.olympicraft.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import fr.olympicraft.arena.ArenaDefinition;
import fr.olympicraft.arena.ArenaManager;
import fr.olympicraft.arena.ArenaValidationResult;
import fr.olympicraft.arena.GameType;
import fr.olympicraft.message.MessageService;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;
import java.util.Locale;

public final class ArenaCommands {

    private ArenaCommands() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> create(
            ArenaManager arenas,
            MessageService messages
    ) {
        /*
         * Toutes les sous-commandes sont ajoutées séparément
         * à la même racine "/oc arena".
         */
        LiteralArgumentBuilder<CommandSourceStack> arenaRoot =
                Commands.literal("arena")
                        .requires(source -> source.hasPermission(2));

        /*
         * /oc arena list
         */
        arenaRoot.then(
                Commands.literal("list")
                        .executes(context ->
                                list(
                                        context.getSource(),
                                        arenas,
                                        messages
                                )
                        )
        );

        /*
         * /oc arena create <name> <game>
         */
        arenaRoot.then(
                Commands.literal("create")
                        .then(
                                Commands.argument(
                                                "name",
                                                StringArgumentType.word()
                                        )
                                        .then(
                                                Commands.argument(
                                                                "game",
                                                                StringArgumentType.word()
                                                        )
                                                        .suggests(
                                                                (context, builder) -> {
                                                                    String remaining =
                                                                            builder.getRemaining()
                                                                                    .toLowerCase(
                                                                                            Locale.ROOT
                                                                                    );

                                                                    for (GameType type :
                                                                            GameType.values()) {
                                                                        if (type.id()
                                                                                .startsWith(
                                                                                        remaining
                                                                                )) {
                                                                            builder.suggest(
                                                                                    type.id(),
                                                                                    Component.literal(
                                                                                            type.displayName()
                                                                                    )
                                                                            );
                                                                        }
                                                                    }

                                                                    return builder.buildFuture();
                                                                }
                                                        )
                                                        .executes(context ->
                                                                createArena(
                                                                        context.getSource(),
                                                                        arenas,
                                                                        messages,
                                                                        StringArgumentType.getString(
                                                                                context,
                                                                                "name"
                                                                        ),
                                                                        StringArgumentType.getString(
                                                                                context,
                                                                                "game"
                                                                        )
                                                                )
                                                        )
                                        )
                        )
        );

        /*
         * /oc arena info <arena>
         */
        arenaRoot.then(
                Commands.literal("info")
                        .then(
                                arenaArgument(arenas)
                                        .executes(context ->
                                                info(
                                                        context.getSource(),
                                                        arenas,
                                                        messages,
                                                        StringArgumentType.getString(
                                                                context,
                                                                "arena"
                                                        )
                                                )
                                        )
                        )
        );

        /*
         * /oc arena enable <arena>
         */
        arenaRoot.then(
                Commands.literal("enable")
                        .then(
                                arenaArgument(arenas)
                                        .executes(context ->
                                                setEnabled(
                                                        context.getSource(),
                                                        arenas,
                                                        messages,
                                                        StringArgumentType.getString(
                                                                context,
                                                                "arena"
                                                        ),
                                                        true
                                                )
                                        )
                        )
        );

        /*
         * /oc arena disable <arena>
         */
        arenaRoot.then(
                Commands.literal("disable")
                        .then(
                                arenaArgument(arenas)
                                        .executes(context ->
                                                setEnabled(
                                                        context.getSource(),
                                                        arenas,
                                                        messages,
                                                        StringArgumentType.getString(
                                                                context,
                                                                "arena"
                                                        ),
                                                        false
                                                )
                                        )
                        )
        );

        /*
         * /oc arena delete <arena>
         * /oc arena delete <arena> confirm
         */
        arenaRoot.then(
                Commands.literal("delete")
                        .then(
                                arenaArgument(arenas)
                                        .executes(context -> {
                                            String arenaId =
                                                    StringArgumentType.getString(
                                                            context,
                                                            "arena"
                                                    );

                                            messages.sendWarning(
                                                    context.getSource(),
                                                    "Confirme avec /oc arena delete "
                                                            + arenaId
                                                            + " confirm"
                                            );

                                            return 0;
                                        })
                                        .then(
                                                Commands.literal("confirm")
                                                        .executes(context ->
                                                                delete(
                                                                        context.getSource(),
                                                                        arenas,
                                                                        messages,
                                                                        StringArgumentType.getString(
                                                                                context,
                                                                                "arena"
                                                                        )
                                                                )
                                                        )
                                        )
                        )
        );

        return arenaRoot;
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

    private static int list(
            CommandSourceStack source,
            ArenaManager arenas,
            MessageService messages
    ) {
        if (arenas.all().isEmpty()) {
            messages.sendWarning(
                    source,
                    "Aucune arène n'est configurée dans ce monde."
            );

            return 0;
        }

        source.sendSuccess(
                () -> Component.literal("Arènes Olympicraft")
                        .withStyle(ChatFormatting.AQUA),
                false
        );

        for (ArenaDefinition arena : arenas.all()) {
            GameType gameType = arena.resolvedGameType();

            String gameName = gameType == null
                    ? arena.gameType
                    : gameType.displayName();

            Component line = Component.empty()
                    .append(Component.literal(arena.id)
                            .withStyle(ChatFormatting.AQUA))
                    .append(Component.literal(" — ")
                            .withStyle(ChatFormatting.DARK_GRAY))
                    .append(Component.literal(gameName)
                            .withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(
                                    arena.enabled
                                            ? " [activée]"
                                            : " [désactivée]"
                            )
                            .withStyle(
                                    arena.enabled
                                            ? ChatFormatting.GREEN
                                            : ChatFormatting.RED
                            ));

            source.sendSuccess(() -> line, false);
        }

        return arenas.all().size();
    }

    private static int createArena(
            CommandSourceStack source,
            ArenaManager arenas,
            MessageService messages,
            String name,
            String gameInput
    ) {
        ServerPlayer player;

        try {
            player = source.getPlayerOrException();
        } catch (Exception exception) {
            messages.sendError(
                    source,
                    "Cette commande doit être exécutée par un joueur."
            );
            return 0;
        }

        GameType gameType = GameType.fromInput(gameInput)
                .orElse(null);

        if (gameType == null) {
            messages.sendError(
                    source,
                    "Mini-jeu inconnu. Valeurs disponibles : "
                            + String.join(
                            ", ",
                            Arrays.stream(GameType.values())
                                    .map(GameType::id)
                                    .toList()
                    )
            );

            return 0;
        }

        ArenaManager.CreateResult result =
                arenas.create(name, gameType, player);

        if (!result.successful()) {
            messages.sendError(source, result.error());
            return 0;
        }

        messages.sendSuccess(
                source,
                "Arène créée : "
                        + result.arena().displayName
                        + " (" + result.arena().id + "), jeu : "
                        + gameType.displayName()
                        + "."
        );

        messages.sendInfo(
                source,
                "Le lobby et le point spectateur ont été placés "
                        + "à ta position actuelle."
        );

        return 1;
    }

    private static int info(
            CommandSourceStack source,
            ArenaManager arenas,
            MessageService messages,
            String input
    ) {
        ArenaDefinition arena = arenas.find(input)
                .orElse(null);

        if (arena == null) {
            messages.sendError(
                    source,
                    "Cette arène n'existe pas."
            );
            return 0;
        }

        GameType type = arena.resolvedGameType();
        ArenaValidationResult validation = arena.validate();

        messages.sendInfo(
                source,
                "Identifiant : " + arena.id
        );

        messages.sendInfo(
                source,
                "Nom : " + arena.displayName
        );

        messages.sendInfo(
                source,
                "Jeu : " + (
                        type == null
                                ? arena.gameType
                                : type.displayName()
                )
        );

        messages.sendInfo(
                source,
                "État : " + (
                        arena.enabled
                                ? "activée"
                                : "désactivée"
                )
        );

        messages.sendInfo(
                source,
                "Monde : " + arena.worldId
        );

        messages.sendInfo(
                source,
                "Lobby : " + (
                        arena.lobby == null
                                ? "non défini"
                                : arena.lobby.formatted()
                )
        );

        messages.sendInfo(
                source,
                "Spectateur : " + (
                        arena.spectator == null
                                ? "non défini"
                                : arena.spectator.formatted()
                )
        );

        messages.sendInfo(
                source,
                "Validation : " + (
                        validation.valid()
                                ? "valide"
                                : "invalide"
                )
        );

        for (String error : validation.errors()) {
            messages.sendError(source, error);
        }

        for (String warning : validation.warnings()) {
            messages.sendWarning(source, warning);
        }

        return 1;
    }

    private static int setEnabled(
            CommandSourceStack source,
            ArenaManager arenas,
            MessageService messages,
            String input,
            boolean enabled
    ) {
        ArenaDefinition arena = arenas.find(input)
                .orElse(null);

        if (arena == null) {
            messages.sendError(
                    source,
                    "Cette arène n'existe pas."
            );
            return 0;
        }

        if (arena.enabled == enabled) {
            messages.sendWarning(
                    source,
                    "Cette arène est déjà "
                            + (enabled
                            ? "activée."
                            : "désactivée.")
            );

            return 0;
        }

        ArenaValidationResult validation = arena.validate();

        if (enabled && !validation.valid()) {
            messages.sendError(
                    source,
                    "L'arène ne peut pas être activée."
            );

            for (String error : validation.errors()) {
                messages.sendError(source, error);
            }

            return 0;
        }

        if (!arenas.setEnabled(arena, enabled)) {
            messages.sendError(
                    source,
                    "L'état de l'arène n'a pas pu être enregistré."
            );

            return 0;
        }

        messages.sendSuccess(
                source,
                "L'arène " + arena.id + " est maintenant "
                        + (enabled
                        ? "activée."
                        : "désactivée.")
        );

        return 1;
    }

    private static int delete(
            CommandSourceStack source,
            ArenaManager arenas,
            MessageService messages,
            String input
    ) {
        ArenaDefinition arena = arenas.find(input)
                .orElse(null);

        if (arena == null) {
            messages.sendError(
                    source,
                    "Cette arène n'existe pas."
            );
            return 0;
        }

        if (!arenas.delete(arena)) {
            messages.sendError(
                    source,
                    "L'arène n'a pas pu être supprimée."
            );
            return 0;
        }

        messages.sendSuccess(
                source,
                "L'arène " + arena.id + " a été supprimée."
        );

        return 1;
    }
}
