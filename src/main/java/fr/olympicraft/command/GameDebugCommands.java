package fr.olympicraft.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import fr.olympicraft.match.player.PlayerMatchService;
import fr.olympicraft.match.player.PlayerSnapshot;
import fr.olympicraft.match.player.PlayerSnapshotManager;
import fr.olympicraft.message.MessageService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

public final class GameDebugCommands {

    private GameDebugCommands() {
    }

    public static LiteralArgumentBuilder<CommandSourceStack> create(
            PlayerMatchService players,
            MessageService messages
    ) {
        return Commands.literal("debug")
                .requires(source ->
                        source.hasPermission(2)
                )

                .then(Commands.literal("save")
                        .executes(context ->
                                save(
                                        context.getSource(),
                                        players,
                                        messages
                                )
                        ))

                .then(Commands.literal("status")
                        .executes(context ->
                                status(
                                        context.getSource(),
                                        players,
                                        messages
                                )
                        ))

                .then(Commands.literal("restore")
                        .executes(context ->
                                restore(
                                        context.getSource(),
                                        players,
                                        messages
                                )
                        ))

                .then(Commands.literal("discard")
                        .executes(context ->
                                discard(
                                        context.getSource(),
                                        players,
                                        messages
                                )
                        ));
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

    private static int save(
            CommandSourceStack source,
            PlayerMatchService players,
            MessageService messages
    ) {
        ServerPlayer player =
                requirePlayer(source, messages);

        if (player == null) {
            return 0;
        }

        PlayerSnapshotManager.SaveResult result =
                players.save(player);

        if (!result.successful()) {
            messages.sendError(
                    source,
                    result.error()
            );

            return 0;
        }

        PlayerSnapshot snapshot =
                result.snapshot();

        messages.sendSuccess(
                source,
                "État sauvegardé en mémoire."
        );

        messages.sendInfo(
                source,
                "Dimension : "
                        + snapshot.dimension().location()
        );

        messages.sendInfo(
                source,
                "Position : "
                        + format(snapshot.x())
                        + ", "
                        + format(snapshot.y())
                        + ", "
                        + format(snapshot.z())
        );

        messages.sendInfo(
                source,
                "Objets sauvegardés : "
                        + snapshot.itemCount()
        );

        return 1;
    }

    private static int status(
            CommandSourceStack source,
            PlayerMatchService players,
            MessageService messages
    ) {
        ServerPlayer player =
                requirePlayer(source, messages);

        if (player == null) {
            return 0;
        }

        PlayerSnapshot snapshot =
                players.snapshots()
                        .find(player.getUUID())
                        .orElse(null);

        if (snapshot == null) {
            messages.sendWarning(
                    source,
                    "Aucune sauvegarde en mémoire."
            );

            return 0;
        }

        messages.sendInfo(
                source,
                "Une sauvegarde existe."
        );

        messages.sendInfo(
                source,
                "Dimension d'origine : "
                        + snapshot.dimension().location()
        );

        messages.sendInfo(
                source,
                "Position d'origine : "
                        + format(snapshot.x())
                        + ", "
                        + format(snapshot.y())
                        + ", "
                        + format(snapshot.z())
        );

        messages.sendInfo(
                source,
                "Objets sauvegardés : "
                        + snapshot.itemCount()
        );

        return 1;
    }

    private static int restore(
            CommandSourceStack source,
            PlayerMatchService players,
            MessageService messages
    ) {
        ServerPlayer player =
                requirePlayer(source, messages);

        if (player == null) {
            return 0;
        }

        /*
         * On vérifie d'abord qu'une sauvegarde existe afin de pouvoir
         * répondre immédiatement si ce n'est pas le cas.
         */
        if (!players.snapshots().contains(
                player.getUUID()
        )) {
            messages.sendError(
                    source,
                    "Aucune sauvegarde n'existe pour ce joueur."
            );

            return 0;
        }

        /*
         * La restauration est reportée au tick serveur suivant.
         *
         * Cela évite de téléporter le joueur et de synchroniser
         * son inventaire pendant l'exécution de la commande Brigadier.
         */
        player.getServer().execute(() -> {
            try {
                PlayerSnapshotManager.RestoreResult result =
                        players.restore(player);

                if (!result.successful()) {
                    messages.sendError(
                            player.createCommandSourceStack(),
                            result.error()
                    );

                    return;
                }

                messages.sendSuccess(
                        player.createCommandSourceStack(),
                        "Ton état a été restauré."
                );
            } catch (Exception exception) {
                /*
                 * Contrairement au traitement précédent, l'exception
                 * sera écrite dans la console.
                 */
                fr.olympicraft.Olympicraft.LOGGER.error(
                        "Impossible de restaurer l'état du joueur {}.",
                        player.getGameProfile().getName(),
                        exception
                );

                messages.sendError(
                        player.createCommandSourceStack(),
                        "La restauration a échoué. "
                                + "La sauvegarde a été conservée."
                );
            }
        });

        messages.sendInfo(
                source,
                "Restauration programmée."
        );

        return 1;
    }

    private static int discard(
            CommandSourceStack source,
            PlayerMatchService players,
            MessageService messages
    ) {
        ServerPlayer player =
                requirePlayer(source, messages);

        if (player == null) {
            return 0;
        }

        if (!players.snapshots().discard(
                player.getUUID()
        )) {
            messages.sendWarning(
                    source,
                    "Aucune sauvegarde à supprimer."
            );

            return 0;
        }

        messages.sendSuccess(
                source,
                "La sauvegarde en mémoire a été supprimée."
        );

        return 1;
    }

    private static String format(double value) {
        return String.format("%.2f", value);
    }
}
