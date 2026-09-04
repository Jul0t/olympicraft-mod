package fr.olympicraft.match;

import fr.olympicraft.arena.ArenaDefinition;
import fr.olympicraft.match.display.MatchCountdownDisplay;
import fr.olympicraft.match.runtime.GameRuntime;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class GameInstance {

    private static final int DEFAULT_GAME_DURATION_SECONDS = 300;

    private final MinecraftServer server;
    private final ArenaDefinition arena;
    private final GameRuntime runtime;

    private final Map<UUID, GameParticipant> participants =
            new LinkedHashMap<>();

    private final MatchCountdownDisplay countdownDisplay =
            new MatchCountdownDisplay();

    private GameState state = GameState.WAITING;

    private int countdownTicks;
    private int countdownDurationSeconds;

    private int elapsedTicks;
    private int runningDisplayTicks;

    private boolean endingNotified;
    private boolean resetNotified;
    private boolean shutdown;

    public GameInstance(
            MinecraftServer server,
            ArenaDefinition arena,
            GameRuntime runtime
    ) {
        this.server = server;
        this.arena = arena;
        this.runtime = runtime;

        runtime.onCreated(this);
    }

    public MinecraftServer server() {
        return server;
    }

    public ArenaDefinition arena() {
        return arena;
    }

    public GameRuntime runtime() {
        return runtime;
    }

    public GameState state() {
        return state;
    }

    public Collection<GameParticipant> participants() {
        return List.copyOf(
                participants.values()
        );
    }

    public int participantCount() {
        return participants.size();
    }

    public long playerCount() {
        return participants.values()
                .stream()
                .filter(GameParticipant::competitor)
                .count();
    }

    public long realPlayerCount() {
        return participants.values()
                .stream()
                .filter(participant ->
                        participant.role()
                                == ParticipantRole.PLAYER
                )
                .count();
    }

    public long dummyCount() {
        return participants.values()
                .stream()
                .filter(GameParticipant::dummy)
                .count();
    }

    public long spectatorCount() {
        return participants.values()
                .stream()
                .filter(GameParticipant::spectator)
                .count();
    }

    public boolean contains(
            UUID playerId
    ) {
        return participants.containsKey(
                playerId
        );
    }

    public boolean addPlayer(
            ServerPlayer player
    ) {
        if (!state.acceptsPlayers()) {
            return false;
        }

        if (participants.containsKey(
                player.getUUID()
        )) {
            return false;
        }

        participants.put(
                player.getUUID(),
                new GameParticipant(
                        player.getUUID(),
                        ParticipantRole.PLAYER,
                        player.getGameProfile()
                                .getName()
                )
        );

        runtime.onPlayerJoined(
                this,
                player,
                false
        );

        broadcast(
                Component.literal(
                        player.getGameProfile()
                                .getName()
                                + " a rejoint la partie."
                )
        );

        return true;
    }

    public boolean addSpectator(
            ServerPlayer player
    ) {
        if (participants.containsKey(
                player.getUUID()
        )) {
            return false;
        }

        participants.put(
                player.getUUID(),
                new GameParticipant(
                        player.getUUID(),
                        ParticipantRole.SPECTATOR,
                        player.getGameProfile()
                                .getName()
                )
        );

        runtime.onPlayerJoined(
                this,
                player,
                true
        );

        broadcast(
                Component.literal(
                        player.getGameProfile()
                                .getName()
                                + " observe la partie."
                )
        );

        return true;
    }

    public boolean addDummy(
            UUID participantId,
            String displayName
    ) {
        if (!state.acceptsPlayers()) {
            return false;
        }

        if (participantId == null
                || participants.containsKey(
                participantId
        )) {
            return false;
        }

        participants.put(
                participantId,
                new GameParticipant(
                        participantId,
                        ParticipantRole.DUMMY,
                        displayName
                )
        );

        broadcast(
                Component.literal(
                        displayName
                                + " a rejoint la partie "
                                + "comme dummy."
                )
        );

        return true;
    }

    public boolean remove(
            UUID playerId
    ) {
        GameParticipant removed =
                participants.remove(playerId);

        if (removed == null) {
            return false;
        }

        runtime.onPlayerLeft(
                this,
                playerId
        );

        if (removed.dummy()) {
            broadcast(
                    Component.literal(
                            removed.displayName()
                                    + " a quitté la partie."
                    )
            );
        } else {
            ServerPlayer player =
                    server.getPlayerList()
                            .getPlayer(playerId);

            if (player != null) {
                broadcast(
                        Component.literal(
                                player.getGameProfile()
                                        .getName()
                                        + " a quitté la partie."
                        )
                );
            }
        }

        if (playerCount() == 0
                && state != GameState.ENDING
                && state != GameState.RESETTING
                && state != GameState.INACTIVE) {
            end(
                    Component.literal(
                            "La partie est arrêtée : "
                                    + "aucun joueur restant."
                    )
            );
        }

        return true;
    }

    public boolean startCountdown() {
        if (state != GameState.WAITING) {
            return false;
        }

        if (!runtime.canStart(this)) {
            return false;
        }

        /*
         * Le runtime prépare et téléporte les combattants avant
         * que le décompte soit officiellement lancé.
         */
        if (!runtime.prepareCountdown(this)) {
            return false;
        }

        countdownDurationSeconds =
                Math.clamp(
                        runtime.countdownSeconds(this),
                        0,
                        300
                );

        if (countdownDurationSeconds <= 0) {
            beginRunning();
            return true;
        }

        state = GameState.STARTING;

        countdownTicks =
                countdownDurationSeconds * 20;

        countdownDisplay.start(
                this,
                countdownDurationSeconds
        );

        runtime.onCountdownStarted(this);

        broadcast(
                Component.literal(
                        "La partie commence dans "
                                + countdownDurationSeconds
                                + " seconde"
                                + (countdownDurationSeconds > 1
                                ? "s"
                                : "")
                                + "."
                )
        );

        return true;
    }

    public boolean forceStop() {
        if (state == GameState.INACTIVE
                || state == GameState.ENDING
                || state == GameState.RESETTING) {
            return false;
        }

        end(
                Component.literal(
                        "La partie a été arrêtée."
                )
        );

        return true;
    }

    public void tick() {
        switch (state) {
            case STARTING -> tickStarting();
            case RUNNING -> tickRunning();
            case ENDING -> tickEnding();
            case RESETTING -> tickResetting();
            default -> {
            }
        }
    }

    private void tickStarting() {
        if (!runtime.canStart(this)) {
            state = GameState.WAITING;
            countdownTicks = 0;

            runtime.onCountdownCancelled(this);
            countdownDisplay.hide();

            broadcast(
                    Component.literal(
                            "Le démarrage a été annulé."
                    )
            );

            return;
        }

        /*
         * Le runtime agit avant de décrémenter le chrono.
         * Le Sumo s'en sert pour bloquer les combattants.
         */
        runtime.tickCountdown(
                this,
                countdownTicks
        );

        countdownTicks--;

        if (countdownTicks <= 0) {
            beginRunning();
            return;
        }

        if (countdownTicks % 20 != 0) {
            return;
        }

        int seconds =
                countdownTicks / 20;

        countdownDisplay.update(
                this,
                seconds,
                countdownDurationSeconds
        );

        if (seconds <= 5
                || seconds == 10) {
            broadcast(
                    Component.literal(
                            "Démarrage dans "
                                    + seconds
                                    + " seconde"
                                    + (seconds > 1
                                    ? "s"
                                    : "")
                                    + "."
                    )
            );
        }
    }

    private void beginRunning() {
        state = GameState.RUNNING;
        elapsedTicks = 0;
        runningDisplayTicks = 40;

        broadcast(
                Component.literal(
                        "La partie commence !"
                )
        );

        countdownDisplay.finish(this);
        runtime.onStarted(this);
    }

    private void tickRunning() {
        if (runningDisplayTicks > 0) {
            runningDisplayTicks--;

            if (runningDisplayTicks == 0) {
                countdownDisplay.hide();
            }
        }

        elapsedTicks++;

        runtime.tick(this);

        /*
         * Le runtime peut avoir terminé la partie pendant son tick.
         */
        if (state != GameState.RUNNING) {
            return;
        }

        if (runtime.getClass()
                .getSimpleName()
                .equals("GenericGameRuntime")
                && elapsedTicks
                >= DEFAULT_GAME_DURATION_SECONDS * 20) {
            end(
                    Component.literal(
                            "Temps écoulé."
                    )
            );
        }
    }

    public void end(
            Component reason
    ) {
        countdownDisplay.hide();

        if (state == GameState.INACTIVE
                || state == GameState.ENDING
                || state == GameState.RESETTING) {
            return;
        }

        state = GameState.ENDING;
        elapsedTicks = 0;
        endingNotified = false;

        broadcast(reason);
    }

    private void tickEnding() {
        if (!endingNotified) {
            endingNotified = true;
            runtime.onEnding(this);
        }

        elapsedTicks++;

        if (elapsedTicks >= 60) {
            state = GameState.RESETTING;
            elapsedTicks = 0;
            resetNotified = false;
        }
    }

    private void tickResetting() {
        if (!resetNotified) {
            resetNotified = true;
            runtime.onReset(this);
        }

        /*
         * Les participants restent présents jusqu'à ce que
         * GameInstanceManager restaure leurs snapshots.
         */
        state = GameState.INACTIVE;
    }

    public boolean preventsDamage(
            ServerPlayer player,
            DamageSource source,
            float amount
    ) {
        return runtime.preventsDamage(
                this,
                player,
                source,
                amount
        );
    }

    public int countdownSeconds() {
        if (state != GameState.STARTING) {
            return 0;
        }

        return Math.max(
                0,
                (countdownTicks + 19) / 20
        );
    }

    public int elapsedSeconds() {
        return elapsedTicks / 20;
    }

    public int remainingSeconds() {
        if (state != GameState.RUNNING) {
            return 0;
        }

        return Math.max(
                0,
                DEFAULT_GAME_DURATION_SECONDS
                        - elapsedSeconds()
        );
    }

    public List<ServerPlayer> onlineParticipants() {
        List<ServerPlayer> result =
                new ArrayList<>();

        for (UUID playerId :
                participants.keySet()) {
            ServerPlayer player =
                    server.getPlayerList()
                            .getPlayer(playerId);

            if (player != null) {
                result.add(player);
            }
        }

        return result;
    }

    public void broadcast(
            Component message
    ) {
        Component formatted =
                Component.empty()
                        .append(
                                Component.literal(
                                        "[Olympicraft] "
                                )
                        )
                        .append(message);

        for (ServerPlayer player :
                onlineParticipants()) {
            player.sendSystemMessage(formatted);
        }
    }

    public void shutdown() {
        countdownDisplay.hide();

        if (!shutdown) {
            shutdown = true;
            runtime.onShutdown(this);
        }

        state = GameState.INACTIVE;
    }

    public boolean removable() {
        return state == GameState.INACTIVE;
    }
}