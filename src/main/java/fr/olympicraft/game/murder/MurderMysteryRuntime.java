package fr.olympicraft.game.murder;

import fr.olympicraft.Olympicraft;
import fr.olympicraft.arena.ArenaDefinition;
import fr.olympicraft.arena.ArenaPosition;
import fr.olympicraft.game.murder.identity.MurderMysteryAliasAllocator;
import fr.olympicraft.game.murder.role.MurderMysteryRole;
import fr.olympicraft.game.murder.role.MurderMysteryRoleAllocator;
import fr.olympicraft.game.murder.scoreboard.MurderMysteryScoreboardService;
import fr.olympicraft.game.murder.reconnect.MurderMysteryDisconnectResult;
import fr.olympicraft.match.GameState;
import fr.olympicraft.match.GameInstance;
import fr.olympicraft.match.GameParticipant;
import fr.olympicraft.match.runtime.GameRuntime;
import fr.olympicraft.game.murder.identity.MurderMysteryIdentityService;
import fr.olympicraft.test.dummy.DummyManager;
import fr.olympicraft.test.dummy.DummyParticipant;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.GameType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class MurderMysteryRuntime
        implements GameRuntime {

    private final MinecraftServer server;

    private final ArenaDefinition arena;

    private final MurderMysterySettings settings;

    private final MurderMysteryRoleAllocator
            roleAllocator =
            new MurderMysteryRoleAllocator();

    private final MurderMysteryAliasAllocator
            aliasAllocator =
            new MurderMysteryAliasAllocator();

    private final DummyManager dummyManager;

    private final MurderMysteryIdentityService
            identityService;

    private final MurderMysteryScoreboardService
            scoreboardService =
            new MurderMysteryScoreboardService();

    private final Map<
            UUID,
            MurderMysteryParticipant
            > participants =
            new LinkedHashMap<>();

    private int roundTicks;

    private int preparationTicks;

    private boolean prepared;

    private boolean finished;

    private boolean troublemakerPresent;

    public MurderMysteryRuntime(
            MinecraftServer server,
            ArenaDefinition arena
    ) {
        this.server = server;
        this.arena = arena;

        this.dummyManager =
                Olympicraft.dummies();

        this.identityService =
                new MurderMysteryIdentityService(
                        server,
                        arena.id
                );

        this.settings =
                new MurderMysterySettings(
                        Olympicraft.configs()
                                .murderMystery()
                );
    }

    public boolean canReconnect(
            UUID playerId
    ) {
        MurderMysteryParticipant participant =
                participants.get(playerId);

        return participant != null
                && participant.alive()
                && participant.disconnected()
                && !participant.forfeited()
                && !participant.reconnectExpired();
    }

    public int reconnectSecondsRemaining(
            UUID playerId
    ) {
        MurderMysteryParticipant participant =
                participants.get(playerId);

        return participant == null
                ? 0
                : participant
                .reconnectSecondsRemaining();
    }

    public boolean reconnectPromptPending(
            UUID playerId
    ) {
        MurderMysteryParticipant participant =
                participants.get(playerId);

        return participant != null
                && participant.reconnectPromptPending()
                && canReconnect(playerId);
    }

    public void reconnectPromptShown(
            UUID playerId
    ) {
        MurderMysteryParticipant participant =
                participants.get(playerId);

        if (participant != null) {
            participant.reconnectPromptPending(
                    false
            );
        }
    }

    public String disconnectEndReason() {
        return disconnectEndReason;
    }

    public float disconnectRankedMultiplier() {
        return disconnectRankedMultiplier;
    }

    public UUID decisiveDisconnectedPlayerId() {
        return decisiveDisconnectedPlayerId;
    }

    public MurderMysteryRole decisiveDisconnectedRole() {
        return decisiveDisconnectedRole;
    }

    @Override
    public boolean canStart(
            GameInstance instance
    ) {
        if (!settings.enabled()) {
            return false;
        }

        if (instance == null) {
            return false;
        }

        if (instance.playerCount()
                < settings.minimumPlayers()) {
            return false;
        }

        if (instance.playerCount()
                > settings.maximumPlayers()) {
            return false;
        }

        return settings.allowDummies()
                || instance.dummyCount() == 0;
    }

    @Override
    public int countdownSeconds(
            GameInstance instance
    ) {
        return settings.countdownSeconds();
    }

    @Override
    public boolean prepareCountdown(
            GameInstance instance
    ) {
        participants.clear();

        roundTicks = 0;
        preparationTicks =
                settings.preparationSeconds()
                        * 20;

        prepared = false;
        finished = false;
        troublemakerPresent = false;

        List<MurderMysteryParticipant> resolved =
                resolveParticipants(instance);

        if (resolved.size()
                < settings.minimumPlayers()) {
            return false;
        }

        List<ArenaPosition> spawns =
                resolveSpawns();

        if (spawns.size() < resolved.size()) {
            Olympicraft.LOGGER.error(
                    "Impossible de préparer le Murder Mystery "
                            + "dans l'arène '{}': {} spawn(s) "
                            + "pour {} participant(s).",
                    arena.id,
                    spawns.size(),
                    resolved.size()
            );

            return false;
        }

        MurderMysteryRoleAllocator.AllocationResult
                allocation =
                roleAllocator.allocate(
                        resolved,
                        settings
                );

        if (!allocation.successful()) {
            Olympicraft.LOGGER.error(
                    "Attribution des rôles impossible "
                            + "dans l'arène '{}': {}",
                    arena.id,
                    allocation.error()
            );

            return false;
        }

        troublemakerPresent =
                allocation.troublemakerPresent();

        aliasAllocator.allocate(
                resolved,
                settings
        );

        for (MurderMysteryParticipant participant :
                resolved) {
            participants.put(
                    participant.participantId(),
                    participant
            );
        }

        /*
         * Chaque entrée de la liste n'est utilisée qu'une fois.
         * Les joueurs et les dummies reçoivent donc forcément
         * des spawns différents.
         */
        Collections.shuffle(spawns);

        for (int index = 0;
             index < resolved.size();
             index++) {
            MurderMysteryParticipant participant =
                    resolved.get(index);

            ArenaPosition spawn =
                    spawns.get(index);

            if (!prepareParticipant(
                    participant,
                    spawn
            )) {
                Olympicraft.LOGGER.error(
                        "La préparation du participant '{}' "
                                + "a échoué dans l'arène '{}'.",
                        participant.originalName(),
                        arena.id
                );

                identityService.restoreAll();
                participants.clear();

                return false;
            }
        }

        /*
         * Les alias sont appliqués seulement lorsque toutes
         * les téléportations ont réussi.
         */
        identityService.applyAll(
                participants.values()
        );


        prepared = true;

        instance.broadcast(
                Component.literal(
                        "Les identités et les rôles "
                                + "ont été distribués."
                ).withStyle(
                        ChatFormatting.DARK_PURPLE
                )
        );

        if (troublemakerPresent
                && settings
                .announceTroublemakerPresence()) {
            instance.broadcast(
                    Component.literal(
                            "Un Trouble-fête se cache "
                                    + "parmi les participants."
                    ).withStyle(
                            ChatFormatting.LIGHT_PURPLE
                    )
            );
        }

        return true;
    }

    @Override
    public void tickCountdown(
            GameInstance instance,
            int remainingTicks
    ) {
        /*
         * Les joueurs sont déjà en Adventure.
         * Les déplacements restent autorisés pendant cette
         * première version afin de tester facilement la carte.
         */
    }

    @Override
    public void onStarted(
            GameInstance instance
    ) {
        if (!prepared) {
            instance.end(
                    Component.literal(
                            "La préparation du Murder Mystery "
                                    + "a échoué."
                    )
            );

            return;
        }

        roundTicks = 0;

        sendPrivateRoles();

        scoreboardService.showAll(
                instance,
                participants,
                remainingSeconds(),
                preparationActive()
        );

        instance.broadcast(
                Component.literal(
                        "L'enquête commence. "
                                + "Observez les autres joueurs."
                ).withStyle(
                        ChatFormatting.GOLD
                )
        );

        if (preparationTicks > 0) {
            instance.broadcast(
                    Component.literal(
                            "Le Meurtrier ne pourra pas "
                                    + "attaquer pendant "
                                    + settings
                                    .preparationSeconds()
                                    + " seconde(s)."
                    ).withStyle(
                            ChatFormatting.YELLOW
                    )
            );
        }
    }

    @Override
    public void tick(
            GameInstance instance
    ) {
        if (finished || !prepared) {
            return;
        }

        roundTicks++;

        tickReconnectDeadlines(instance);

        if (finished) {
            return;
        }

        scoreboardService.updateIfNeeded(
                instance,
                participants,
                remainingSeconds(),
                preparationActive()
        );

        if (preparationTicks > 0) {
            preparationTicks--;

            if (preparationTicks == 0) {
                instance.broadcast(
                        Component.literal(
                                "La période de préparation "
                                        + "est terminée."
                        ).withStyle(
                                ChatFormatting.RED
                        )
                );
            }
        }

        int remainingSeconds =
                remainingSeconds();

        if (settings.config()
                .round
                .announceRemainingTime
                && roundTicks % 20 == 0
                && settings.config()
                .round
                .announcedRemainingSeconds
                .contains(remainingSeconds)) {
            instance.broadcast(
                    Component.literal(
                            "Temps restant : "
                                    + formatTime(
                                    remainingSeconds
                            )
                    ).withStyle(
                            ChatFormatting.YELLOW
                    )
            );
        }

        if (roundTicks
                < settings.roundDurationSeconds()
                * 20) {
            return;
        }

        finished = true;

        if (settings.murdererWinsAtTimeLimit()) {
            instance.end(
                    Component.literal(
                            "Le temps est écoulé : "
                                    + "le Meurtrier remporte "
                                    + "la partie."
                    ).withStyle(
                            ChatFormatting.RED
                    )
            );
        } else {
            instance.end(
                    Component.literal(
                            "Le temps est écoulé : "
                                    + "la partie se termine "
                                    + "sans vainqueur."
                    ).withStyle(
                            ChatFormatting.GRAY
                    )
            );
        }
    }

    @Override
    public boolean onPlayerDisconnected(
            GameInstance instance,
            ServerPlayer player
    ) {
        if (player == null
                || !settings.config()
                .reconnection
                .enabled) {
            return false;
        }

        if (instance.state()
                != GameState.STARTING
                && instance.state()
                != GameState.RUNNING) {
            return false;
        }

        MurderMysteryParticipant participant =
                participants.get(
                        player.getUUID()
                );

        if (participant == null
                || participant.dummy()
                || !participant.alive()) {
            return false;
        }

        participant.incrementDisconnectCount();

        if (participant.disconnectCount()
                >= settings.config()
                .reconnection
                .maximumDisconnects) {
            applyForfeit(
                    instance,
                    participant,
                    "Forfait après plusieurs déconnexions."
            );

            return true;
        }

        int graceSeconds =
                graceSeconds(
                        participant.role()
                );

        participant.disconnected(true);

        participant.reconnectDecisionPending(
                false
        );

        participant.reconnectDeadlineMillis(
                System.currentTimeMillis()
                        + graceSeconds * 1000L
        );

        participant.reconnectPromptPending(true);

        /*
         * Le scoreboard disparaît de lui-même côté client lors de
         * la déconnexion. Le participant logique reste dans la partie.
         */
        instance.broadcast(
                Component.literal(
                        participant.alias()
                                + " est temporairement absent."
                ).withStyle(
                        ChatFormatting.YELLOW
                )
        );

        return true;
    }

    @Override
    public boolean onPlayerReconnected(
            GameInstance instance,
            ServerPlayer player
    ) {
        if (player == null
                || !canReconnect(
                player.getUUID()
        )) {
            return false;
        }

        MurderMysteryParticipant participant =
                participants.get(
                        player.getUUID()
                );

        participant.clearReconnectState();

        /*
         * On réapplique la préparation de partie sans modifier
         * le rôle, l'alias ou la progression.
         */
        player.closeContainer();

        player.setGameMode(
                GameType.ADVENTURE
        );

        player.getInventory()
                .clearContent();

        player.getInventory()
                .setChanged();

        player.setHealth(
                Math.max(
                        1.0F,
                        player.getMaxHealth()
                                - participant.wounds()
                                * 4.0F
                )
        );

        player.getFoodData()
                .setFoodLevel(20);

        player.getFoodData()
                .setSaturation(5.0F);

        player.setRemainingFireTicks(0);
        player.fallDistance = 0.0F;

        /*
         * Tant que les cadavres ne sont pas implémentés, la reprise
         * se fait au point spectateur, qui sert de position sûre.
         */
        if (arena.spectator != null) {
            arena.spectator.teleport(
                    server,
                    player
            );
        }

        if (!participant.reconnectDecisionPending()) {
            return false;
        }

        identityService.apply(
                participant
        );

        player.inventoryMenu
                .broadcastChanges();

        scoreboardService.show(
                player,
                participant,
                remainingSeconds(),
                preparationActive()
        );

        player.sendSystemMessage(
                Component.literal(
                        "Tu as réintégré la partie avec "
                                + "l'identité "
                                + participant.alias()
                                + "."
                ).withStyle(
                        ChatFormatting.GREEN
                )
        );

        player.sendSystemMessage(
                Component.literal(
                        "Attention : une nouvelle déconnexion "
                                + "provoquera un forfait immédiat."
                ).withStyle(
                        ChatFormatting.RED,
                        ChatFormatting.BOLD
                )
        );

        return true;
    }

    @Override
    public boolean preventsDamage(
            GameInstance instance,
            ServerPlayer player,
            net.minecraft.world.damagesource.DamageSource source,
            float amount
    ) {
        if (instance == null
                || player == null
                || !instance.contains(
                player.getUUID()
        )) {
            return false;
        }

        /*
         * Le combat spécifique sera ajouté après les systèmes
         * d'identité et de cadavres. Pour le moment, les dégâts
         * sont bloqués afin d'éviter des morts vanilla.
         */
        return !settings.normalDamageEnabled();
    }

    @Override
    public void onPlayerLeft(
            GameInstance instance,
            UUID playerId
    ) {
        if (playerId == null) {
            return;
        }

        MurderMysteryParticipant participant =
                participants.get(
                        playerId
                );

        if (participant == null) {
            return;
        }

        ServerPlayer player =
                server.getPlayerList()
                        .getPlayer(
                                playerId
                        );

        if (player != null) {
            scoreboardService.hide(player);
        }

        /*
         * Un joueur forfait ou éliminé est retiré définitivement.
         * Il ne doit pas redevenir "déconnecté" après l'appel
         * à instance.remove().
         */
        if (participant.forfeited()
                || !participant.alive()) {
            participant.reconnectDecisionPending(
                    false
            );

            participant.reconnectPromptPending(
                    false
            );

            participant.reconnectDeadlineMillis(
                    0L
            );

            return;
        }

        participant.disconnected(true);
    }

    @Override
    public void onCountdownCancelled(
            GameInstance instance
    ) {
        scoreboardService.hideAll(instance);
        identityService.restoreAll();
        clearRuntimeState();
    }

    @Override
    public void onEnding(
            GameInstance instance
    ) {
        scoreboardService.hideAll(instance);
        revealMurderers(instance);
        identityService.restoreAll();
    }

    @Override
    public void onReset(
            GameInstance instance
    ) {
        scoreboardService.hideAll(instance);
        identityService.restoreAll();
        clearRuntimeState();
    }

    @Override
    public void onShutdown(
            GameInstance instance
    ) {
        scoreboardService.hideAll(instance);
        identityService.restoreAll();
        clearRuntimeState();
    }

    public MurderMysteryParticipant participant(
            UUID participantId
    ) {
        return participants.get(participantId);
    }

    public Map<UUID, MurderMysteryParticipant>
    participants() {
        return Map.copyOf(participants);
    }

    public boolean preparationActive() {
        return preparationTicks > 0;
    }

    public int remainingSeconds() {
        return Math.max(
                0,
                settings.roundDurationSeconds()
                        - roundTicks / 20
        );
    }

    private String disconnectEndReason;

    private float disconnectRankedMultiplier =
            1.0F;

    private UUID decisiveDisconnectedPlayerId;

    private MurderMysteryRole
            decisiveDisconnectedRole;

    private List<MurderMysteryParticipant>
    resolveParticipants(
            GameInstance instance
    ) {
        List<MurderMysteryParticipant> result =
                new ArrayList<>();

        for (GameParticipant participant :
                instance.participants()) {
            if (!participant.competitor()) {
                continue;
            }

            result.add(
                    new MurderMysteryParticipant(
                            participant.playerId(),
                            participant.dummy(),
                            participant.displayName()
                    )
            );
        }

        return result;
    }

    private int graceSeconds(
            MurderMysteryRole role
    ) {
        var config =
                settings.config()
                        .reconnection;

        if (role == null) {
            return config.innocentGraceSeconds;
        }

        return switch (role) {
            case MURDERER ->
                    config.murdererGraceSeconds;

            case DETECTIVE ->
                    config.detectiveGraceSeconds;

            case TROUBLEMAKER ->
                    config.troublemakerGraceSeconds;

            case INNOCENT ->
                    config.innocentGraceSeconds;
        };
    }

    private void tickReconnectDeadlines(
            GameInstance instance
    ) {
        List<MurderMysteryParticipant> expired =
                participants.values()
                        .stream()
                        .filter(participant ->
                                !participant.dummy()
                        )
                        .filter(
                                MurderMysteryParticipant
                                        ::disconnected
                        )
                        .filter(
                                MurderMysteryParticipant
                                        ::reconnectExpired
                        )
                        .toList();

        for (MurderMysteryParticipant participant :
                expired) {
            applyForfeit(
                    instance,
                    participant,
                    "Délai de reconnexion expiré."
            );

            if (finished) {
                return;
            }
        }
    }

    private void applyForfeit(
            GameInstance instance,
            MurderMysteryParticipant participant,
            String personalReason
    ) {
        if (participant == null
                || participant.forfeited()) {
            return;
        }

        participant.forfeited(true);
        participant.clearReconnectState();
        participant.eliminate();

        ServerPlayer player =
                server.getPlayerList()
                        .getPlayer(
                                participant.participantId()
                        );

        if (player != null) {
            player.sendSystemMessage(
                    Component.literal(
                            personalReason
                                    + " Cela compte comme "
                                    + "une défaite."
                    ).withStyle(
                            ChatFormatting.RED
                    )
            );
        }

        instance.broadcast(
                Component.literal(
                        participant.alias()
                                + " a abandonné la partie."
                ).withStyle(
                        ChatFormatting.YELLOW
                )
        );

        checkDisconnectVictory(
                instance,
                participant
        );
    }

    private void checkDisconnectVictory(
            GameInstance instance,
            MurderMysteryParticipant forfeited
    ) {
        if (forfeited.role()
                == MurderMysteryRole.MURDERER
                && !hasLivingConnectedOrReservedRole(
                MurderMysteryRole.MURDERER
        )) {
            endByDisconnect(
                    instance,
                    forfeited,
                    "Victoire par déconnexion "
                            + "du Meurtrier."
            );

            return;
        }

        if (forfeited.role()
                != MurderMysteryRole.MURDERER
                && !hasLivingConnectedOrReservedOpponent()) {
            endByDisconnect(
                    instance,
                    forfeited,
                    "Victoire du Meurtrier par "
                            + "déconnexion du dernier adversaire."
            );
        }
    }

    private boolean hasLivingConnectedOrReservedRole(
            MurderMysteryRole role
    ) {
        return participants.values()
                .stream()
                .anyMatch(participant ->
                        participant.alive()
                                && !participant.forfeited()
                                && participant.role()
                                == role
                );
    }

    private boolean hasLivingConnectedOrReservedOpponent() {
        return participants.values()
                .stream()
                .anyMatch(participant ->
                        participant.alive()
                                && !participant.forfeited()
                                && participant.role()
                                != MurderMysteryRole.MURDERER
                );
    }

    private void endByDisconnect(
            GameInstance instance,
            MurderMysteryParticipant participant,
            String reason
    ) {
        if (finished) {
            return;
        }

        finished = true;

        disconnectEndReason = reason;

        disconnectRankedMultiplier =
                settings.config()
                        .reconnection
                        .disconnectVictoryMultiplier;

        decisiveDisconnectedPlayerId =
                participant.participantId();

        decisiveDisconnectedRole =
                participant.role();

        instance.end(
                Component.literal(
                        reason
                                + " Récompense ranked : "
                                + formatMultiplier(
                                disconnectRankedMultiplier
                        )
                                + " des points."
                ).withStyle(
                        ChatFormatting.GOLD
                )
        );
    }

    private static String formatMultiplier(
            float multiplier
    ) {
        return Math.round(
                multiplier * 100.0F
        ) + " %";
    }

    private List<ArenaPosition> resolveSpawns() {
        List<ArenaPosition> result =
                new ArrayList<>();

        if (arena.spawns == null) {
            return result;
        }

        for (List<ArenaPosition> group :
                arena.spawns.values()) {
            if (group != null) {
                result.addAll(group);
            }
        }

        return result;
    }

    private boolean prepareParticipant(
            MurderMysteryParticipant participant,
            ArenaPosition spawn
    ) {
        if (participant == null || spawn == null) {
            return false;
        }

        if (participant.dummy()) {
            return prepareDummy(
                    participant,
                    spawn
            );
        }

        return preparePlayer(
                participant,
                spawn
        );
    }

    private boolean preparePlayer(
            MurderMysteryParticipant participant,
            ArenaPosition spawn
    ) {
        ServerPlayer player =
                server.getPlayerList()
                        .getPlayer(
                                participant.participantId()
                        );

        if (player == null) {
            return false;
        }

        player.closeContainer();

        player.setGameMode(
                GameType.ADVENTURE
        );

        player.getInventory()
                .clearContent();

        player.getInventory()
                .setChanged();

        player.setHealth(
                player.getMaxHealth()
        );

        player.getFoodData()
                .setFoodLevel(20);

        player.getFoodData()
                .setSaturation(5.0F);

        player.setRemainingFireTicks(0);
        player.fallDistance = 0.0F;

        player.inventoryMenu
                .broadcastChanges();

        if (!spawn.teleport(
                server,
                player
        )) {
            Olympicraft.LOGGER.error(
                    "Impossible de téléporter le joueur '{}' "
                            + "dans l'arène Murder Mystery '{}'.",
                    participant.originalName(),
                    arena.id
            );

            return false;
        }

        return true;
    }

    private boolean prepareDummy(
            MurderMysteryParticipant participant,
            ArenaPosition spawn
    ) {
        DummyParticipant dummy =
                dummyManager.findByParticipantId(
                        arena.id,
                        participant.participantId()
                );

        if (dummy == null) {
            Olympicraft.LOGGER.error(
                    "Le dummy '{}' est introuvable "
                            + "dans l'arène '{}'.",
                    participant.originalName(),
                    arena.id
            );

            return false;
        }

        if (!dummyManager.teleport(
                dummy,
                spawn
        )) {
            Olympicraft.LOGGER.error(
                    "Impossible de téléporter le dummy '{}' "
                            + "dans l'arène Murder Mystery '{}'.",
                    participant.originalName(),
                    arena.id
            );

            return false;
        }

        ArmorStand entity =
                dummyManager.entity(dummy);

        if (entity != null) {
            entity.setDeltaMovement(
                    0.0D,
                    0.0D,
                    0.0D
            );

            entity.fallDistance = 0.0F;
        }

        return true;
    }

    private void sendPrivateRoles() {
        for (MurderMysteryParticipant participant :
                participants.values()) {
            ServerPlayer player =
                    server.getPlayerList()
                            .getPlayer(
                                    participant
                                            .participantId()
                            );

            if (player == null) {
                continue;
            }

            player.sendSystemMessage(
                    Component.empty()
                            .append(
                                    Component.literal(
                                            "[Olympicraft] "
                                    ).withStyle(
                                            ChatFormatting.DARK_GRAY
                                    )
                            )
                            .append(
                                    Component.literal(
                                            "Ton identité : "
                                    ).withStyle(
                                            ChatFormatting.GRAY
                                    )
                            )
                            .append(
                                    Component.literal(
                                            participant.alias()
                                    ).withStyle(
                                            ChatFormatting.GOLD
                                    )
                            )
            );

            player.sendSystemMessage(
                    Component.empty()
                            .append(
                                    Component.literal(
                                            "[Olympicraft] "
                                    ).withStyle(
                                            ChatFormatting.DARK_GRAY
                                    )
                            )
                            .append(
                                    Component.literal(
                                            "Ton rôle : "
                                    ).withStyle(
                                            ChatFormatting.GRAY
                                    )
                            )
                            .append(
                                    participant.role()
                                            .displayComponent()
                            )
            );

            sendRoleObjective(
                    player,
                    participant.role()
            );
        }
    }

    private void sendRoleObjective(
            ServerPlayer player,
            MurderMysteryRole role
    ) {
        String objective =
                switch (role) {
                    case INNOCENT ->
                            "Enquête et découvre le Meurtrier.";

                    case DETECTIVE ->
                            "Enquête et protège les Innocents. "
                                    + "Ton rôle reste secret.";

                    case MURDERER ->
                            "Élimine les autres participants "
                                    + "sans être identifié.";

                    case TROUBLEMAKER ->
                            "Élimine un Innocent, le Détective "
                                    + "et le Meurtrier pour gagner seul.";
                };

        player.sendSystemMessage(
                Component.empty()
                        .append(
                                Component.literal(
                                        "[Olympicraft] "
                                ).withStyle(
                                        ChatFormatting.DARK_GRAY
                                )
                        )
                        .append(
                                Component.literal(
                                        objective
                                ).withStyle(
                                        ChatFormatting.YELLOW
                                )
                        )
        );
    }

    private void revealMurderers(
            GameInstance instance
    ) {
        if (instance == null) {
            return;
        }

        if (!settings.config()
                .roles
                .revealMurdererAtEnd) {
            return;
        }

        List<String> murderers =
                participants.values()
                        .stream()
                        .filter(participant ->
                                participant.role()
                                        == MurderMysteryRole
                                        .MURDERER
                        )
                        .map(
                                MurderMysteryParticipant::alias
                        )
                        .filter(alias ->
                                alias != null
                                        && !alias.isBlank()
                        )
                        .toList();

        if (murderers.isEmpty()) {
            instance.broadcast(
                    Component.literal(
                            "Aucun Meurtrier n'a pu être identifié."
                    ).withStyle(
                            ChatFormatting.GRAY
                    )
            );

            return;
        }

        String murdererNames =
                String.join(
                        ", ",
                        murderers
                );

        Component message =
                Component.empty()
                        .append(
                                Component.literal(
                                        "Le Meurtrier était : "
                                ).withStyle(
                                        ChatFormatting.GRAY
                                )
                        )
                        .append(
                                Component.literal(
                                        murdererNames
                                ).withStyle(
                                        ChatFormatting.RED
                                )
                        )
                        .append(
                                Component.literal(
                                        "."
                                ).withStyle(
                                        ChatFormatting.GRAY
                                )
                        );

        instance.broadcast(message);
    }

    /*
     * Efface les données internes de la partie.
     *
     * La restauration de l'inventaire, de la position et du mode
     * de jeu est prise en charge par PlayerMatchService.
     */
    private void clearRuntimeState() {
        participants.clear();

        roundTicks = 0;
        preparationTicks = 0;

        prepared = false;
        finished = false;
        troublemakerPresent = false;

        disconnectEndReason = null;
        disconnectRankedMultiplier = 1.0F;
        decisiveDisconnectedPlayerId = null;
        decisiveDisconnectedRole = null;
    }

    private static String formatTime(
            int totalSeconds
    ) {
        int safeSeconds =
                Math.max(
                        0,
                        totalSeconds
                );

        int minutes =
                safeSeconds / 60;

        int seconds =
                safeSeconds % 60;

        return String.format(
                "%02d:%02d",
                minutes,
                seconds
        );
    }
    public void forfeitReconnect(
            GameInstance instance,
            UUID playerId,
            String reason
    ) {
        MurderMysteryParticipant participant =
                participants.get(playerId);

        if (participant == null) {
            return;
        }

        applyForfeit(
                instance,
                participant,
                reason
        );
    }
    public boolean prepareReconnectDecision(
            GameInstance instance,
            ServerPlayer player
    ) {
        if (instance == null
                || player == null
                || !canReconnect(
                player.getUUID()
        )) {
            return false;
        }

        MurderMysteryParticipant participant =
                participants.get(
                        player.getUUID()
                );

        if (participant == null) {
            return false;
        }

        /*
         * Le joueur est connecté au serveur, mais reste absent
         * de la partie jusqu'à ce qu'il accepte le GUI.
         */
        participant.reconnectDecisionPending(
                true
        );

        scoreboardService.hide(player);

        /*
         * On restaure temporairement son état normal.
         *
         * Le snapshot reste présent et sera réutilisé si le joueur
         * accepte de reprendre. Il ne faut donc pas appeler ici
         * matchPlayers.exit(), qui archive le snapshot.
         */
        player.closeContainer();

        player.setGameMode(
                GameType.SPECTATOR
        );

        player.getInventory()
                .clearContent();

        player.getInventory()
                .setChanged();

        player.inventoryMenu
                .broadcastChanges();

        /*
         * Place temporairement le joueur au point spectateur.
         * Il ne peut pas interagir avec la partie pendant son choix.
         */
        if (arena.spectator != null) {
            arena.spectator.teleport(
                    server,
                    player
            );
        }

        return true;
    }
    public void cancelReconnect(
            GameInstance instance,
            ServerPlayer player,
            String reason
    ) {
        if (instance == null || player == null) {
            return;
        }

        MurderMysteryParticipant participant =
                participants.get(
                        player.getUUID()
                );

        if (participant == null) {
            return;
        }

        scoreboardService.hide(player);

        participant.reconnectDecisionPending(
                false
        );

        applyForfeit(
                instance,
                participant,
                reason
        );
    }
}