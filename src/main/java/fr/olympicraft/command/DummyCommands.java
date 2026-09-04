package fr.olympicraft.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import fr.olympicraft.arena.ArenaManager;
import fr.olympicraft.match.GameInstanceManager;
import fr.olympicraft.message.MessageService;
import fr.olympicraft.test.dummy.DummyParticipant;
import fr.olympicraft.match.GameInstance;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public final class DummyCommands {

    private DummyCommands() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> create(
            GameInstanceManager matches,
            ArenaManager arenas,
            MessageService messages
    ) {
        return Commands.literal("dummy")
                .requires(source ->
                        source.hasPermission(2)
                )

                .then(
                        Commands.literal("add")

                                /*
                                 * /oc game dummy add
                                 */
                                .executes(context ->
                                        addCurrent(
                                                context.getSource(),
                                                matches,
                                                messages,
                                                ""
                                        )
                                )

                                /*
                                 * /oc game dummy add <arène> [nom]
                                 */
                                .then(
                                        arenaArgument(arenas)
                                                .executes(context ->
                                                        add(
                                                                context.getSource(),
                                                                matches,
                                                                messages,
                                                                StringArgumentType.getString(
                                                                        context,
                                                                        "arena"
                                                                ),
                                                                ""
                                                        )
                                                )
                                                .then(
                                                        Commands.argument(
                                                                        "name",
                                                                        StringArgumentType.word()
                                                                )
                                                                .executes(context ->
                                                                        add(
                                                                                context.getSource(),
                                                                                matches,
                                                                                messages,
                                                                                StringArgumentType.getString(
                                                                                        context,
                                                                                        "arena"
                                                                                ),
                                                                                StringArgumentType.getString(
                                                                                        context,
                                                                                        "name"
                                                                                )
                                                                        )
                                                                )
                                                )
                                )
                )

                .then(
                        Commands.literal("remove")
                                .then(
                                        arenaArgument(arenas)
                                                .then(
                                                        Commands.argument(
                                                                        "name",
                                                                        StringArgumentType.word()
                                                                )
                                                                .suggests((context, builder) -> {
                                                                    String arena =
                                                                            StringArgumentType.getString(
                                                                                    context,
                                                                                    "arena"
                                                                            );

                                                                    for (DummyParticipant dummy :
                                                                            matches.listDummies(arena)) {
                                                                        builder.suggest(
                                                                                dummy.name()
                                                                        );
                                                                    }

                                                                    return builder.buildFuture();
                                                                })
                                                                .executes(context ->
                                                                        remove(
                                                                                context.getSource(),
                                                                                matches,
                                                                                messages,
                                                                                StringArgumentType.getString(
                                                                                        context,
                                                                                        "arena"
                                                                                ),
                                                                                StringArgumentType.getString(
                                                                                        context,
                                                                                        "name"
                                                                                )
                                                                        )
                                                                )
                                                )
                                )
                )

                .then(
                        Commands.literal("list")

                                /*
                                 * /oc game dummy list
                                 */
                                .executes(context ->
                                        listCurrent(
                                                context.getSource(),
                                                matches,
                                                messages
                                        )
                                )

                                /*
                                 * /oc game dummy list <arène>
                                 */
                                .then(
                                        arenaArgument(arenas)
                                                .executes(context ->
                                                        list(
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
                        Commands.literal("clear")

                                /*
                                 * /oc game dummy clear
                                 */
                                .executes(context ->
                                        clearCurrent(
                                                context.getSource(),
                                                matches,
                                                messages
                                        )
                                )

                                /*
                                 * /oc game dummy clear <arène>
                                 */
                                .then(
                                        arenaArgument(arenas)
                                                .executes(context ->
                                                        clear(
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
                    for (String id :
                            arenas.idsStartingWith(
                                    builder.getRemaining()
                            )) {
                        builder.suggest(id);
                    }

                    return builder.buildFuture();
                });
    }

    private static int addCurrent(
            CommandSourceStack source,
            GameInstanceManager matches,
            MessageService messages,
            String name
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

        return add(
                source,
                matches,
                messages,
                instance.arena().id,
                name
        );
    }

    private static int listCurrent(
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

        return list(
                source,
                matches,
                messages,
                instance.arena().id
        );
    }

    private static int clearCurrent(
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

        return clear(
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
        ServerPlayer player;

        try {
            player = source.getPlayerOrException();
        } catch (Exception exception) {
            messages.sendError(
                    source,
                    "Cette commande sans nom d'arène "
                            + "doit être exécutée par un joueur."
            );

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

    private static int add(
            CommandSourceStack source,
            GameInstanceManager matches,
            MessageService messages,
            String arena,
            String name
    ) {
        GameInstanceManager.DummyAddResult result =
                matches.addDummy(arena, name);

        if (!result.successful()) {
            messages.sendError(
                    source,
                    result.error()
            );

            return 0;
        }

        messages.sendSuccess(
                source,
                "Dummy ajouté : "
                        + result.dummy().name()
                        + "."
        );

        return 1;
    }

    private static int remove(
            CommandSourceStack source,
            GameInstanceManager matches,
            MessageService messages,
            String arena,
            String name
    ) {
        GameInstanceManager.DummyRemoveResult result =
                matches.removeDummy(
                        arena,
                        name
                );

        if (!result.successful()) {
            messages.sendError(
                    source,
                    result.error()
            );

            return 0;
        }

        messages.sendSuccess(
                source,
                "Dummy supprimé : "
                        + result.dummy().name()
                        + "."
        );

        return 1;
    }

    private static int list(
            CommandSourceStack source,
            GameInstanceManager matches,
            MessageService messages,
            String arena
    ) {
        var dummies =
                matches.listDummies(arena);

        if (dummies.isEmpty()) {
            messages.sendWarning(
                    source,
                    "Aucun dummy dans cette arène."
            );

            return 0;
        }

        messages.sendInfo(
                source,
                "Dummies de l'arène " + arena + " :"
        );

        for (DummyParticipant dummy : dummies) {
            messages.sendInfo(
                    source,
                    "- " + dummy.name()
            );
        }

        return dummies.size();
    }

    private static int clear(
            CommandSourceStack source,
            GameInstanceManager matches,
            MessageService messages,
            String arena
    ) {
        int amount =
                matches.clearDummies(arena);

        if (amount == 0) {
            messages.sendWarning(
                    source,
                    "Aucun dummy à supprimer."
            );

            return 0;
        }

        messages.sendSuccess(
                source,
                amount
                        + " dummy(s) supprimé(s)."
        );

        return amount;
    }
}