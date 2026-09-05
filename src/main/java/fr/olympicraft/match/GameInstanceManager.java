package fr.olympicraft.match;

import fr.olympicraft.arena.ArenaDefinition;
import fr.olympicraft.arena.ArenaManager;
import fr.olympicraft.match.player.PlayerMatchService;
import fr.olympicraft.match.runtime.GameRuntimeRegistry;
import fr.olympicraft.test.TestModeManager;
import fr.olympicraft.test.dummy.DummyManager;
import fr.olympicraft.test.dummy.DummyParticipant;
import fr.olympicraft.gui.GuiSession;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class GameInstanceManager {

    private final ArenaManager arenas;
    private final PlayerMatchService matchPlayers;
    private final GameRuntimeRegistry runtimes;
    private final TestModeManager testMode;
    private final DummyManager dummies;

    private final Map<String, GameInstance> instances =
            new LinkedHashMap<>();

    private MinecraftServer server;

    public GameInstanceManager(
            ArenaManager arenas,
            PlayerMatchService matchPlayers,
            GameRuntimeRegistry runtimes,
            TestModeManager testMode,
            DummyManager dummies
    ) {
        this.arenas = arenas;
        this.matchPlayers = matchPlayers;
        this.runtimes = runtimes;
        this.testMode = testMode;
        this.dummies = dummies;
    }

    public void attachServer(
            MinecraftServer server
    ) {
        this.server = server;
    }

    public void detachServer() {
        shutdown();
        this.server = null;
    }

    public boolean isAttached() {
        return server != null;
    }

    public Collection<GameInstance> all() {
        return List.copyOf(
                instances.values()
        );
    }

    public Optional<GameInstance> findByArena(
            String arenaInput
    ) {
        ArenaDefinition arena =
                arenas.find(arenaInput)
                        .orElse(null);

        if (arena == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(
                instances.get(arena.id)
        );
    }

    private final Map<UUID, PendingReconnectMenu>
            pendingReconnectMenus =
            new LinkedHashMap<>();

    public Optional<GameInstance> findByPlayer(
            UUID playerId
    ) {
        if (playerId == null) {
            return Optional.empty();
        }

        return instances.values()
                .stream()
                .filter(instance ->
                        instance.contains(playerId)
                )
                .findFirst();
    }

    public JoinResult join(
            ServerPlayer player,
            String arenaInput,
            boolean spectator
    ) {
        if (server == null) {
            return JoinResult.failure(
                    "Aucun serveur logique n'est attaché."
            );
        }

        if (player == null) {
            return JoinResult.failure(
                    "Le joueur est introuvable."
            );
        }

        if (findByPlayer(player.getUUID()).isPresent()) {
            return JoinResult.failure(
                    "Tu participes déjà à une partie."
            );
        }

        ArenaDefinition arena =
                arenas.find(arenaInput)
                        .orElse(null);

        if (arena == null) {
            return JoinResult.failure(
                    "Cette arène n'existe pas."
            );
        }

        if (!arena.enabled) {
            return JoinResult.failure(
                    "Cette arène est désactivée."
            );
        }

        if (arena.lobby == null) {
            return JoinResult.failure(
                    "Le lobby de cette arène n'est pas défini."
            );
        }

        if (arena.spectator == null) {
            return JoinResult.failure(
                    "Le point spectateur de cette arène "
                            + "n'est pas défini."
            );
        }

        GameInstance instance =
                getOrCreateInstance(arena);

        if (!spectator
                && !instance.state().acceptsPlayers()) {
            removeIfEmpty(instance);

            return JoinResult.failure(
                    "Cette partie n'accepte plus de joueurs."
            );
        }

        PlayerMatchService.EnterResult entered =
                matchPlayers.enter(
                        player,
                        arena,
                        spectator
                );

        if (!entered.successful()) {
            removeIfEmpty(instance);

            return JoinResult.failure(
                    entered.error()
            );
        }

        boolean joined = spectator
                ? instance.addSpectator(player)
                : instance.addPlayer(player);

        if (!joined) {
            /*
             * Le joueur a été préparé, mais la partie a finalement
             * refusé son inscription. Son état doit donc être rendu.
             */
            PlayerMatchService.ExitResult restored =
                    matchPlayers.exit(player);

            removeIfEmpty(instance);

            return JoinResult.failure(
                    restored.successful()
                            ? "La partie a refusé l'inscription."
                            : "La partie a refusé l'inscription "
                            + "et la restauration a échoué."
            );
        }

        return JoinResult.success(instance);
    }

    public LeaveResult leave(
            ServerPlayer player
    ) {
        if (player == null) {
            return LeaveResult.failure(
                    "Le joueur est introuvable."
            );
        }

        GameInstance instance =
                findByPlayer(player.getUUID())
                        .orElse(null);

        if (instance == null) {
            return LeaveResult.failure(
                    "Tu ne participes à aucune partie."
            );
        }

        if (!instance.remove(player.getUUID())) {
            return LeaveResult.failure(
                    "Ton inscription n'a pas pu être supprimée."
            );
        }

        PlayerMatchService.ExitResult restored =
                matchPlayers.exit(player);

        if (!restored.successful()) {
            matchPlayers.markForRestore(
                    player.getUUID()
            );

            return LeaveResult.failure(
                    "Tu as quitté la partie, mais ton état "
                            + "n'a pas pu être restauré : "
                            + restored.error()
            );
        }

        removeIfEmpty(instance);

        return LeaveResult.success();
    }

    public StartResult start(
            String arenaInput
    ) {
        GameInstance instance =
                findByArena(arenaInput)
                        .orElse(null);

        if (instance == null) {
            return StartResult.failure(
                    "Aucune partie n'existe pour cette arène."
            );
        }

        try {
            if (!instance.startCountdown()) {
                return StartResult.failure(
                        "La partie ne peut pas démarrer "
                                + "dans son état actuel."
                );
            }

            return StartResult.success(instance);
        } catch (Exception exception) {
            fr.olympicraft.Olympicraft.LOGGER.error(
                    "Erreur pendant le démarrage de l'arène '{}'.",
                    instance.arena().id,
                    exception
            );

            return StartResult.failure(
                    "Le démarrage a échoué. "
                            + "Consulte la console du serveur."
            );
        }
    }

    public StopResult stop(
            String arenaInput
    ) {
        GameInstance instance =
                findByArena(arenaInput)
                        .orElse(null);

        if (instance == null) {
            return StopResult.failure(
                    "Aucune partie n'existe pour cette arène."
            );
        }

        if (!instance.forceStop()) {
            return StopResult.failure(
                    "La partie est déjà arrêtée "
                            + "ou en cours de nettoyage."
            );
        }

        return StopResult.success(instance);
    }

    public void tick() {
        List<GameInstance> finishedInstances =
                new ArrayList<>();

        /*
         * Première étape :
         * faire avancer toutes les parties et repérer celles
         * qui sont réellement terminées.
         */
        for (GameInstance instance :
                List.copyOf(instances.values())) {
            instance.tick();

            if (instance.removable()) {
                finishedInstances.add(instance);
            }
        }

        tickPendingReconnectMenus();

        /*
         * Deuxième étape :
         * retirer d'abord les parties terminées de la liste.
         *
         * Ainsi, pendant la restauration, findByPlayer(...) ne
         * considère plus le joueur comme participant actif.
         */
        for (GameInstance instance :
                finishedInstances) {
            instances.remove(
                    instance.arena().id,
                    instance
            );
        }

        /*
         * Troisième étape :
         * restaurer les joueurs après le retrait de l'instance.
         */
        for (GameInstance instance :
                finishedInstances) {
            restoreInstancePlayers(instance);

            dummies.clearArena(
                    instance.arena().id
            );
        }
    }

    public void handleDisconnect(
            ServerPlayer player
    ) {
        if (player == null) {
            return;
        }

        pendingReconnectMenus.remove(
                player.getUUID()
        );

        GameInstance instance =
                findByPlayer(
                        player.getUUID()
                ).orElse(null);

        if (instance == null) {
            return;
        }

        /*
         * Certains jeux, comme le Murder Mystery, conservent
         * temporairement le participant afin de lui permettre
         * de reprendre sa partie.
         */
        boolean retained =
                instance.runtime()
                        .onPlayerDisconnected(
                                instance,
                                player
                        );

        if (retained) {
            return;
        }

        instance.remove(
                player.getUUID()
        );

        matchPlayers.markForRestore(
                player.getUUID()
        );

        removeIfEmpty(instance);
    }

    public void handleJoin(
            ServerPlayer player
    ) {
        if (player == null) {
            return;
        }

        player.getServer().execute(() -> {
            GameInstance reconnectInstance =
                    findByPlayer(
                            player.getUUID()
                    ).orElse(null);

            if (reconnectInstance != null
                    && reconnectInstance.runtime()
                    instanceof fr.olympicraft.game.murder
                    .MurderMysteryRuntime runtime
                    && runtime.canReconnect(
                    player.getUUID()
            )) {
                if (!runtime.prepareReconnectDecision(
                        reconnectInstance,
                        player
                )) {
                    player.sendSystemMessage(
                            Component.literal(
                                    "[Olympicraft] Impossible de préparer "
                                            + "la reprise de la partie."
                            )
                    );

                    return;
                }

                if (fr.olympicraft.Olympicraft
                        .configs()
                        .murderMystery()
                        .reconnection
                        .showReconnectMenu) {

                    pendingReconnectMenus.put(
                            player.getUUID(),
                            pendingReconnectMenus.put(
                                    player.getUUID(),
                                    new PendingReconnectMenu(
                                            reconnectInstance.arena().id,
                                            40,
                                            0
                                    )
                            ));
                } else {
                    acceptMurderMysteryReconnect(
                            player,
                            reconnectInstance.arena().id
                    );
                }

                return;
            }

            if (!matchPlayers.shouldRestore(
                    player.getUUID()
            )) {
                return;
            }

            var result =
                    matchPlayers.restorePending(
                            player
                    );

            if (result.successful()) {
                player.sendSystemMessage(
                        Component.literal(
                                "[Olympicraft] Ton état précédent "
                                        + "a été restauré."
                        )
                );
            } else {
                player.sendSystemMessage(
                        Component.literal(
                                "[Olympicraft] La restauration "
                                        + "automatique a échoué."
                        )
                );
            }
        });
    }

    public void shutdown() {
        /*
         * Les joueurs sont restaurés avant de supprimer les
         * informations des instances.
         */
        for (GameInstance instance :
                instances.values()) {
            restoreInstancePlayers(instance);
            instance.shutdown();
        }

        pendingReconnectMenus.clear();

        /*
         * Les dummies n'ont aucun snapshot à restaurer.
         * Leurs entités sont simplement supprimées.
         */
        dummies.clearAll();
        instances.clear();
    }

    private void restoreInstancePlayers(
            GameInstance instance
    ) {
        if (instance == null) {
            return;
        }

        if (server == null) {
            for (GameParticipant participant :
                    instance.participants()) {
                if (participant.dummy()) {
                    continue;
                }

                matchPlayers.markForRestore(
                        participant.playerId()
                );
            }

            return;
        }

        for (GameParticipant participant :
                instance.participants()) {
            /*
             * Les dummies ne possèdent aucun snapshot.
             */
            if (participant.dummy()) {
                continue;
            }

            ServerPlayer player =
                    server.getPlayerList()
                            .getPlayer(
                                    participant.playerId()
                            );

            if (player == null) {
                matchPlayers.markForRestore(
                        participant.playerId()
                );

                continue;
            }

            PlayerMatchService.ExitResult result =
                    matchPlayers.exit(player);

            if (!result.successful()) {
                matchPlayers.markForRestore(
                        participant.playerId()
                );

                fr.olympicraft.Olympicraft.LOGGER.error(
                        "Impossible de restaurer le joueur '{}' "
                                + "après la partie dans l'arène '{}': {}",
                        player.getGameProfile().getName(),
                        instance.arena().id,
                        result.error()
                );

                player.sendSystemMessage(
                        Component.literal(
                                "[Olympicraft] Ton état n'a pas "
                                        + "pu être totalement restauré. "
                                        + "Une nouvelle tentative sera "
                                        + "faite à ta prochaine connexion."
                        )
                );
            }
        }
    }

    private GameInstance getOrCreateInstance(
            ArenaDefinition arena
    ) {
        return instances.computeIfAbsent(
                arena.id,
                ignored ->
                        new GameInstance(
                                server,
                                arena,
                                runtimes.create(
                                        server,
                                        arena
                                )
                        )
        );
    }

    private void removeIfEmpty(
            GameInstance instance
    ) {
        if (instance == null
                || instance.participantCount() != 0) {
            return;
        }

        dummies.clearArena(
                instance.arena().id
        );

        instances.remove(
                instance.arena().id
        );
    }

    public DummyAddResult addDummy(
            String arenaInput,
            String requestedName
    ) {
        if (!testMode.isEnabled()) {
            return DummyAddResult.failure(
                    "Le mode test Olympicraft n'est pas activé."
            );
        }

        if (server == null) {
            return DummyAddResult.failure(
                    "Aucun serveur logique n'est attaché."
            );
        }

        ArenaDefinition arena =
                arenas.find(arenaInput)
                        .orElse(null);

        if (arena == null) {
            return DummyAddResult.failure(
                    "Cette arène n'existe pas."
            );
        }

        if (!arena.enabled) {
            return DummyAddResult.failure(
                    "Cette arène est désactivée."
            );
        }

        GameInstance instance =
                getOrCreateInstance(arena);

        DummyManager.CreateResult result =
                dummies.create(
                        instance,
                        requestedName
                );

        if (!result.successful()) {
            removeIfEmpty(instance);

            return DummyAddResult.failure(
                    result.error()
            );
        }

        return DummyAddResult.success(
                instance,
                result.dummy()
        );
    }

    public DummyRemoveResult removeDummy(
            String arenaInput,
            String name
    ) {
        ArenaDefinition arena =
                arenas.find(arenaInput)
                        .orElse(null);

        if (arena == null) {
            return DummyRemoveResult.failure(
                    "Cette arène n'existe pas."
            );
        }

        GameInstance instance =
                instances.get(arena.id);

        if (instance == null) {
            return DummyRemoveResult.failure(
                    "Aucune partie n'existe pour cette arène."
            );
        }

        DummyParticipant dummy =
                dummies.find(
                        arena.id,
                        name
                ).orElse(null);

        if (dummy == null) {
            return DummyRemoveResult.failure(
                    "Ce dummy n'existe pas."
            );
        }

        if (!dummies.remove(
                instance,
                dummy.participantId()
        )) {
            return DummyRemoveResult.failure(
                    "Le dummy n'a pas pu être supprimé."
            );
        }

        removeIfEmpty(instance);

        return DummyRemoveResult.success(
                dummy
        );
    }

    public int clearDummies(
            String arenaInput
    ) {
        ArenaDefinition arena =
                arenas.find(arenaInput)
                        .orElse(null);

        if (arena == null) {
            return 0;
        }

        GameInstance instance =
                instances.get(arena.id);

        if (instance == null) {
            return 0;
        }

        int removed =
                dummies.clear(instance);

        removeIfEmpty(instance);

        return removed;
    }

    public Collection<DummyParticipant> listDummies(
            String arenaInput
    ) {
        ArenaDefinition arena =
                arenas.find(arenaInput)
                        .orElse(null);

        if (arena != null) {
            return dummies.all(
                    arena.id
            );
        }

        /*
         * Solution de secours pour une éventuelle différence
         * entre le nom affiché et l'identifiant technique.
         */
        GameInstance instance =
                findByArena(arenaInput)
                        .orElse(null);

        if (instance == null) {
            return List.of();
        }

        return dummies.all(
                instance.arena().id
        );
    }

    public record DummyAddResult(
            boolean successful,
            String error,
            GameInstance instance,
            DummyParticipant dummy
    ) {
        public static DummyAddResult success(
                GameInstance instance,
                DummyParticipant dummy
        ) {
            return new DummyAddResult(
                    true,
                    null,
                    instance,
                    dummy
            );
        }

        public static DummyAddResult failure(
                String error
        ) {
            return new DummyAddResult(
                    false,
                    error,
                    null,
                    null
            );
        }
    }

    public record DummyRemoveResult(
            boolean successful,
            String error,
            DummyParticipant dummy
    ) {
        public static DummyRemoveResult success(
                DummyParticipant dummy
        ) {
            return new DummyRemoveResult(
                    true,
                    null,
                    dummy
            );
        }

        public static DummyRemoveResult failure(
                String error
        ) {
            return new DummyRemoveResult(
                    false,
                    error,
                    null
            );
        }
    }

    public record JoinResult(
            boolean successful,
            String error,
            GameInstance instance
    ) {
        public static JoinResult success(
                GameInstance instance
        ) {
            return new JoinResult(
                    true,
                    null,
                    instance
            );
        }

        public static JoinResult failure(
                String error
        ) {
            return new JoinResult(
                    false,
                    error,
                    null
            );
        }
    }

    public record LeaveResult(
            boolean successful,
            String error
    ) {
        public static LeaveResult success() {
            return new LeaveResult(
                    true,
                    null
            );
        }

        public static LeaveResult failure(
                String error
        ) {
            return new LeaveResult(
                    false,
                    error
            );
        }
    }

    public record StartResult(
            boolean successful,
            String error,
            GameInstance instance
    ) {
        public static StartResult success(
                GameInstance instance
        ) {
            return new StartResult(
                    true,
                    null,
                    instance
            );
        }

        public static StartResult failure(
                String error
        ) {
            return new StartResult(
                    false,
                    error,
                    null
            );
        }
    }

    public record StopResult(
            boolean successful,
            String error,
            GameInstance instance
    ) {
        public static StopResult success(
                GameInstance instance
        ) {
            return new StopResult(
                    true,
                    null,
                    instance
            );
        }

        public static StopResult failure(
                String error
        ) {
            return new StopResult(
                    false,
                    error,
                    null
            );
        }
    }
    public ReconnectResult acceptMurderMysteryReconnect(
            ServerPlayer player,
            String arenaInput
    ) {
        if (player == null) {
            return ReconnectResult.failure(
                    "Joueur introuvable."
            );
        }

        GameInstance instance =
                findByArena(
                        arenaInput
                ).orElse(null);

        if (instance == null
                || !(instance.runtime()
                instanceof fr.olympicraft.game.murder
                .MurderMysteryRuntime runtime)) {
            return ReconnectResult.failure(
                    "La partie n'existe plus."
            );
        }

        if (!runtime.canReconnect(
                player.getUUID()
        )) {
            return ReconnectResult.failure(
                    "Le délai de reconnexion est expiré."
            );
        }

        if (!runtime.onPlayerReconnected(
                instance,
                player
        )) {
            return ReconnectResult.failure(
                    "La réintégration a échoué."
            );
        }

        return ReconnectResult.success(
                "Tu as réintégré la partie."
        );
    }

    public ReconnectResult declineMurderMysteryReconnect(
            ServerPlayer player,
            String arenaInput
    ) {
        if (player == null) {
            return ReconnectResult.failure(
                    "Joueur introuvable."
            );
        }

        GameInstance instance =
                findByArena(
                        arenaInput
                ).orElse(null);

        if (instance == null
                || !(instance.runtime()
                instanceof fr.olympicraft.game.murder
                .MurderMysteryRuntime runtime)) {
            return ReconnectResult.failure(
                    "La partie n'existe plus."
            );
        }

        if (!runtime.canReconnect(
                player.getUUID()
        )) {
            return ReconnectResult.failure(
                    "Aucune reconnexion n'est en attente."
            );
        }

        /*
         * Enregistre d'abord le forfait et vérifie si celui-ci
         * provoque la fin de la partie.
         */
        runtime.cancelReconnect(
                instance,
                player,
                "Reconnexion refusée."
        );

        /*
         * Retire ensuite l'inscription générale.
         *
         * Le rôle reste dans l'historique interne du runtime,
         * mais le joueur n'est plus membre de GameInstance.
         */
        instance.remove(
                player.getUUID()
        );

        PlayerMatchService.ExitResult restored =
                matchPlayers.exit(player);

        if (!restored.successful()) {
            matchPlayers.markForRestore(
                    player.getUUID()
            );

            return ReconnectResult.failure(
                    "Forfait enregistré, mais ton état "
                            + "n'a pas pu être restauré : "
                            + restored.error()
            );
        }

        removeIfEmpty(instance);

        return ReconnectResult.success(
                "Tu as abandonné la partie."
        );
    }

    public record ReconnectResult(
            boolean successful,
            String message
    ) {
        public static ReconnectResult success(
                String message
        ) {
            return new ReconnectResult(
                    true,
                    message
            );
        }

        public static ReconnectResult failure(
                String message
        ) {
            return new ReconnectResult(
                    false,
                    message
            );
        }
    }
    public ReconnectResult simulateMurderMysteryDisconnect(
            ServerPlayer player
    ) {
        if (player == null) {
            return ReconnectResult.failure(
                    "Joueur introuvable."
            );
        }

        GameInstance instance =
                findByPlayer(
                        player.getUUID()
                ).orElse(null);

        if (instance == null
                || !(instance.runtime()
                instanceof fr.olympicraft.game.murder
                .MurderMysteryRuntime runtime)) {
            return ReconnectResult.failure(
                    "Tu n'es pas dans un Murder Mystery."
            );
        }

        if (!runtime.onPlayerDisconnected(
                instance,
                player
        )) {
            return ReconnectResult.failure(
                    "La déconnexion simulée a échoué."
            );
        }

        player.getServer().execute(() ->
                fr.olympicraft.Olympicraft
                        .gui()
                        .open(
                                player,
                                new fr.olympicraft.gui.menu.murder
                                        .MurderMysteryReconnectMenu(
                                        instance.arena().id,
                                        player.getUUID()
                                )
                        )
        );

        return ReconnectResult.success(
                "Déconnexion simulée."
        );
    }
    private void scheduleReconnectMenu(
            ServerPlayer player,
            String arenaId,
            int remainingTicks
    ) {
        if (player == null
                || player.getServer() == null) {
            return;
        }

        player.getServer().execute(() -> {
            /*
             * execute() seul reporte d'un passage dans la file,
             * mais pas forcément de 20 ticks.
             *
             * Cette méthode se rappelle donc une fois par tick.
             */
            if (remainingTicks > 0) {
                scheduleReconnectMenu(
                        player,
                        arenaId,
                        remainingTicks - 1
                );

                return;
            }

            /*
             * Le joueur peut s'être déconnecté une nouvelle fois
             * pendant le délai.
             */
            ServerPlayer onlinePlayer =
                    server == null
                            ? null
                            : server.getPlayerList()
                            .getPlayer(
                                    player.getUUID()
                            );

            if (onlinePlayer == null) {
                return;
            }

            GameInstance currentInstance =
                    findByArena(
                            arenaId
                    ).orElse(null);

            if (currentInstance == null
                    || !(currentInstance.runtime()
                    instanceof fr.olympicraft.game.murder
                    .MurderMysteryRuntime runtime)
                    || !runtime.canReconnect(
                    onlinePlayer.getUUID()
            )) {
                return;
            }

            runtime.reconnectPromptShown(
                    onlinePlayer.getUUID()
            );

            fr.olympicraft.Olympicraft
                    .gui()
                    .open(
                            onlinePlayer,
                            new fr.olympicraft.gui.menu.murder
                                    .MurderMysteryReconnectMenu(
                                    arenaId,
                                    onlinePlayer.getUUID()
                            )
                    );
        });
    }
    private record PendingReconnectMenu(
            String arenaId,
            int ticksRemaining,
            int openAttempts
    ) {
    }
    private void tickPendingReconnectMenus() {
        if (server == null
                || pendingReconnectMenus.isEmpty()) {
            return;
        }

        List<UUID> completed =
                new ArrayList<>();

        List<Map.Entry<UUID, PendingReconnectMenu>> entries =
                new ArrayList<>(
                        pendingReconnectMenus.entrySet()
                );

        for (Map.Entry<UUID, PendingReconnectMenu> entry :
                entries) {
            UUID playerId =
                    entry.getKey();

            PendingReconnectMenu pending =
                    entry.getValue();

            ServerPlayer player =
                    server.getPlayerList()
                            .getPlayer(playerId);

            if (player == null
                    || player.connection == null) {
                completed.add(playerId);
                continue;
            }

            int nextTicks =
                    pending.ticksRemaining() - 1;

            if (nextTicks > 0) {
                pendingReconnectMenus.put(
                        playerId,
                        new PendingReconnectMenu(
                                pending.arenaId(),
                                nextTicks,
                                pending.openAttempts()
                        )
                );

                continue;
            }

            GameInstance instance =
                    findByArena(
                            pending.arenaId()
                    ).orElse(null);

            if (instance == null
                    || !(instance.runtime()
                    instanceof fr.olympicraft.game.murder
                    .MurderMysteryRuntime runtime)
                    || !runtime.canReconnect(playerId)) {
                completed.add(playerId);
                continue;
            }

            /*
             * Ferme le menu d'inventaire encore présent après
             * la connexion avant d'ouvrir le nôtre.
             */
            player.closeContainer();

            fr.olympicraft.Olympicraft
                    .gui()
                    .open(
                            player,
                            new fr.olympicraft.gui.menu.murder
                                    .MurderMysteryReconnectMenu(
                                    pending.arenaId(),
                                    playerId
                            )
                    );

            GuiSession session =
                    fr.olympicraft.Olympicraft
                            .gui()
                            .session(playerId);

            boolean active =
                    fr.olympicraft.Olympicraft
                            .gui()
                            .hasActiveMenu(
                                    player,
                                    session
                            );

            /*
             * Si le menu a été remplacé par un paquet tardif de
             * connexion, nous retentons son ouverture dans 10 ticks.
             */
            if (!active
                    && pending.openAttempts() < 3) {
                pendingReconnectMenus.put(
                        playerId,
                        new PendingReconnectMenu(
                                pending.arenaId(),
                                10,
                                pending.openAttempts() + 1
                        )
                );

                continue;
            }

            if (active) {
                runtime.reconnectPromptShown(
                        playerId
                );
            }

            completed.add(playerId);
        }

        for (UUID playerId : completed) {
            pendingReconnectMenus.remove(
                    playerId
            );
        }
    }
}