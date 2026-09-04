package fr.olympicraft.match.player;

import fr.olympicraft.Olympicraft;
import fr.olympicraft.arena.ArenaDefinition;
import fr.olympicraft.arena.ArenaPosition;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public final class PlayerMatchService {

    private final PlayerSnapshotManager snapshots;

    /*
     * Joueurs dont l'état devra être restauré lors de leur
     * prochaine connexion.
     *
     * Le snapshot correspondant reste conservé dans
     * PlayerSnapshotManager jusqu'à une restauration réussie.
     */
    private final Set<UUID> pendingRestores =
            new LinkedHashSet<>();

    private MinecraftServer server;

    public PlayerMatchService(
            PlayerSnapshotManager snapshots
    ) {
        this.snapshots = snapshots;
    }

    public void attachServer(MinecraftServer server) {
        this.server = server;

        pendingRestores.clear();
        pendingRestores.addAll(
                snapshots.playerIds()
        );
    }

    public void detachServer() {
        this.server = null;
    }

    public boolean isAttached() {
        return server != null;
    }

    public PlayerSnapshotManager.SaveResult save(
            ServerPlayer player
    ) {
        return snapshots.save(player);
    }

    public PlayerSnapshotManager.RestoreResult restore(
            ServerPlayer player
    ) {
        if (server == null) {
            return PlayerSnapshotManager.RestoreResult.failure(
                    "Aucun serveur logique n'est attaché."
            );
        }

        PlayerSnapshotManager.RestoreResult result =
                snapshots.restore(
                        server,
                        player
                );

        if (result.successful()) {
            pendingRestores.remove(
                    player.getUUID()
            );
        }

        return result;
    }

    public EnterResult enter(
            ServerPlayer player,
            ArenaDefinition arena,
            boolean spectator
    ) {
        if (server == null) {
            return EnterResult.failure(
                    "Aucun serveur logique n'est attaché."
            );
        }

        if (player == null) {
            return EnterResult.failure(
                    "Le joueur est introuvable."
            );
        }

        if (arena == null) {
            return EnterResult.failure(
                    "L'arène est introuvable."
            );
        }

        if (snapshots.contains(player.getUUID())) {
            return EnterResult.failure(
                    "Une sauvegarde existe déjà pour ce joueur."
            );
        }

        /*
         * Un participant rejoint le lobby.
         * Un spectateur rejoint le point spectateur.
         */
        ArenaPosition target = spectator
                ? arena.spectator
                : arena.lobby;

        if (target == null) {
            return EnterResult.failure(
                    spectator
                            ? "Le point spectateur n'est pas défini."
                            : "Le lobby n'est pas défini."
            );
        }

        /*
         * ArenaPosition contient déjà le véritable identifiant
         * de dimension :
         *
         * minecraft:overworld
         * minecraft:the_nether
         * minecraft:the_end
         *
         * On ne doit donc pas utiliser ArenaDefinition.worldId,
         * qui peut contenir le nom de la sauvegarde solo.
         */
        ServerLevel targetLevel =
                target.resolveLevel(server);

        if (targetLevel == null) {
            Olympicraft.LOGGER.error(
                    "Impossible de résoudre la dimension '{}' "
                            + "pour la destination de l'arène '{}'. "
                            + "Dimensions chargées : {}",
                    target.dimension,
                    arena.id,
                    loadedWorldIds()
            );

            return EnterResult.failure(
                    "Le monde de destination est introuvable. "
                            + "Dimension enregistrée : "
                            + target.dimension
            );
        }

        /*
         * La sauvegarde est effectuée avant toute modification
         * de l'état du joueur.
         */
        PlayerSnapshotManager.SaveResult saved =
                snapshots.save(player);

        if (!saved.successful()) {
            return EnterResult.failure(
                    saved.error()
            );
        }

        try {
            if (spectator) {
                MatchPlayerPreparation.prepareSpectator(
                        player
                );
            } else {
                MatchPlayerPreparation.prepareParticipant(
                        player
                );
            }

            teleport(
                    player,
                    targetLevel,
                    target
            );

            return EnterResult.success();
        } catch (Exception exception) {
            Olympicraft.LOGGER.error(
                    "Impossible de préparer le joueur '{}' "
                            + "pour l'arène '{}'.",
                    player.getGameProfile().getName(),
                    arena.id,
                    exception
            );

            /*
             * Si la préparation ou la téléportation échoue,
             * on tente de rendre immédiatement son état initial
             * au joueur.
             */
            PlayerSnapshotManager.RestoreResult restored =
                    snapshots.restore(
                            server,
                            player
                    );

            if (!restored.successful()) {
                pendingRestores.add(
                        player.getUUID()
                );

                Olympicraft.LOGGER.error(
                        "La restauration de secours du joueur '{}' "
                                + "a également échoué : {}",
                        player.getGameProfile().getName(),
                        restored.error()
                );
            }

            return EnterResult.failure(
                    restored.successful()
                            ? "La préparation du joueur a échoué. "
                            + "Son état initial a été restauré."
                            : "La préparation du joueur a échoué "
                            + "et sa restauration est en attente."
            );
        }
    }

    public ExitResult exit(
            ServerPlayer player
    ) {
        if (player == null) {
            return ExitResult.failure(
                    "Le joueur est introuvable."
            );
        }

        if (!snapshots.contains(player.getUUID())) {
            return ExitResult.failure(
                    "Aucune sauvegarde n'existe pour ce joueur."
            );
        }

        PlayerSnapshotManager.RestoreResult restored =
                restore(player);

        if (!restored.successful()) {
            pendingRestores.add(
                    player.getUUID()
            );

            return ExitResult.failure(
                    restored.error()
            );
        }

        return ExitResult.success();
    }

    public void markForRestore(UUID playerId) {
        if (playerId == null) {
            return;
        }

        if (snapshots.contains(playerId)) {
            pendingRestores.add(playerId);
        }
    }

    public boolean shouldRestore(UUID playerId) {
        if (playerId == null) {
            return false;
        }

        return pendingRestores.contains(playerId)
                && snapshots.contains(playerId);
    }

    public PlayerSnapshotManager.RestoreResult
    restorePending(ServerPlayer player) {
        if (player == null) {
            return PlayerSnapshotManager.RestoreResult.failure(
                    "Le joueur est introuvable."
            );
        }

        if (!shouldRestore(player.getUUID())) {
            return PlayerSnapshotManager.RestoreResult.failure(
                    "Aucune restauration n'est en attente."
            );
        }

        return restore(player);
    }

    private static void teleport(
            ServerPlayer player,
            ServerLevel level,
            ArenaPosition position
    ) {
        player.teleportTo(
                level,
                position.x,
                position.y,
                position.z,
                position.yaw,
                position.pitch
        );
    }

    private String loadedWorldIds() {
        if (server == null) {
            return "serveur non attaché";
        }

        StringBuilder result =
                new StringBuilder();

        for (ServerLevel level :
                server.getAllLevels()) {
            if (!result.isEmpty()) {
                result.append(", ");
            }

            result.append(
                    level.dimension()
                            .location()
            );
        }

        return result.toString();
    }

    public PlayerSnapshotManager snapshots() {
        return snapshots;
    }

    public record EnterResult(
            boolean successful,
            String error
    ) {
        public static EnterResult success() {
            return new EnterResult(
                    true,
                    null
            );
        }

        public static EnterResult failure(
                String error
        ) {
            return new EnterResult(
                    false,
                    error
            );
        }
    }

    public record ExitResult(
            boolean successful,
            String error
    ) {
        public static ExitResult success() {
            return new ExitResult(
                    true,
                    null
            );
        }

        public static ExitResult failure(
                String error
        ) {
            return new ExitResult(
                    false,
                    error
            );
        }
    }
}