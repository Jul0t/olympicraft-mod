package fr.olympicraft.game.sumo;

import fr.olympicraft.Olympicraft;
import fr.olympicraft.arena.ArenaDefinition;
import fr.olympicraft.arena.ArenaPosition;
import fr.olympicraft.arena.ArenaRegion;
import fr.olympicraft.arena.ArenaRegionType;
import fr.olympicraft.match.GameInstance;
import fr.olympicraft.match.GameParticipant;
import fr.olympicraft.match.ParticipantRole;
import fr.olympicraft.match.runtime.GameRuntime;
import fr.olympicraft.test.dummy.DummyManager;
import fr.olympicraft.test.dummy.DummyParticipant;
import fr.olympicraft.config.model.game.SumoConfig;
import fr.olympicraft.game.sumo.gui.SumoKitSelectionMenu;
import fr.olympicraft.game.sumo.kit.SumoKitSelectionSession;
import fr.olympicraft.game.sumo.kit.SumoKitService;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.GameType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

public final class SumoRuntime implements GameRuntime {

    private final MinecraftServer server;
    private final ArenaDefinition arena;
    private final DummyManager dummyManager;

    private final SumoSettings settings;

    private SumoFighter firstFighter;
    private SumoFighter secondFighter;

    private int roundTicks;
    private int overtimeTicks;

    private boolean roundFinished;
    private boolean overtimeStarted;

    private ArenaPosition firstFrozenPosition;
    private ArenaPosition secondFrozenPosition;
    private boolean countdownPrepared;

    private final SumoKitService kitService =
            new SumoKitService();

    private SumoKitSelectionSession kitSelection;

    private boolean kitSelectionResolved;
    private boolean kitDistributed;

    public SumoRuntime(
            MinecraftServer server,
            ArenaDefinition arena
    ) {
        this.server = server;
        this.arena = arena;
        this.dummyManager =
                Olympicraft.dummies();

        this.settings =
                new SumoSettings(
                        Olympicraft.configs().sumo()
                );
    }

    @Override
    public boolean canStart(
            GameInstance instance
    ) {
        return instance.playerCount() >= 2;
    }

    @Override
    public boolean prepareCountdown(
            GameInstance instance
    ) {
        List<SumoFighter> fighters =
                resolveFighters(instance);

        if (fighters.size() < 2) {
            return false;
        }

        ArenaPosition firstSpawn =
                combatSpawn(0);

        ArenaPosition secondSpawn =
                combatSpawn(1);

        if (firstSpawn == null
                || secondSpawn == null) {
            Olympicraft.LOGGER.error(
                    "Impossible de préparer le Sumo dans "
                            + "l'arène '{}': deux spawns "
                            + "de combat sont nécessaires.",
                    arena.id
            );

            return false;
        }

        firstFighter = fighters.get(0);
        secondFighter = fighters.get(1);

        firstFrozenPosition =
                copyPosition(firstSpawn);

        secondFrozenPosition =
                copyPosition(secondSpawn);

        roundTicks = 0;
        overtimeTicks = 0;

        roundFinished = false;
        overtimeStarted = false;
        countdownPrepared = true;

        activePlayerKits.clear();
        /*
         * La session de sélection des kits doit être créée
         * avant le premier appel à usesMenu().
         */
        kitSelection =
                new SumoKitSelectionSession(
                        settings
                );

        kitSelectionResolved = false;
        kitDistributed = false;

        prepareFighter(
                firstFighter,
                firstFrozenPosition
        );

        prepareFighter(
                secondFighter,
                secondFrozenPosition
        );

        for (int index = 2;
             index < fighters.size();
             index++) {
            prepareWaitingFighter(
                    fighters.get(index)
            );
        }

        instance.broadcast(
                Component.literal(
                        firstFighter.displayName()
                                + " affronte "
                                + secondFighter.displayName()
                                + " !"
                )
        );

        /*
         * VOTE et PLAYER_CHOICE ouvrent le menu.
         * FIXED et RANDOM sont choisis immédiatement.
         */
        if (kitSelection.usesMenu()) {
            openSelectionMenus(instance);
        } else {
            resolveKitSelection(instance);
        }

        /*
         * resolveKitSelection() peut annuler la partie
         * lorsque la configuration ne permet aucun kit.
         */
        return !kitSelection.cancelled();
    }

    @Override
    public void onStarted(
            GameInstance instance
    ) {
        /*
         * La préparation et la téléportation ont déjà été réalisées
         * avant le compte à rebours.
         *
         * Le passage à RUNNING libère simplement les combattants.
         */
        if (!countdownPrepared
                || firstFighter == null
                || secondFighter == null) {
            instance.end(
                    Component.literal(
                            "La préparation du duel a échoué."
                    )
            );

            return;
        }
        if (!kitSelectionResolved) {
            resolveKitSelection(instance);
        }

        if (instance.state()
                != fr.olympicraft.match.GameState.RUNNING) {
            return;
        }

        releaseFighter(firstFighter);
        releaseFighter(secondFighter);

        countdownPrepared = false;

        roundTicks = 0;
        overtimeTicks = 0;
        roundFinished = false;
        overtimeStarted = false;
    }

    @Override
    public void onCountdownCancelled(
            GameInstance instance
    ) {
        stopOvertimeMusic(instance);
        closeSelectionMenus(instance);
        releaseFighter(firstFighter);
        releaseFighter(secondFighter);

        firstFighter = null;
        secondFighter = null;

        firstFrozenPosition = null;
        secondFrozenPosition = null;

        countdownPrepared = false;
        roundFinished = false;
        overtimeStarted = false;

        roundTicks = 0;
        overtimeTicks = 0;

        kitSelection = null;
        kitSelectionResolved = false;
        kitDistributed = false;
        activePlayerKits.clear();
    }

    @Override
    public void tick(
            GameInstance instance
    ) {
        if (roundFinished) {
            return;
        }

        if (firstFighter == null
                || secondFighter == null) {
            finishWithoutWinner(instance);
            return;
        }

        boolean firstAvailable =
                firstFighter.available();

        boolean secondAvailable =
                secondFighter.available();

        if (!firstAvailable && !secondAvailable) {
            finishWithoutWinner(instance);
            return;
        }

        if (!firstAvailable) {
            win(
                    instance,
                    secondFighter,
                    firstFighter
            );
            return;
        }

        if (!secondAvailable) {
            win(
                    instance,
                    firstFighter,
                    secondFighter
            );
            return;
        }

        keepProtected(firstFighter);
        keepProtected(secondFighter);

        if (isEliminated(firstFighter)) {
            win(
                    instance,
                    secondFighter,
                    firstFighter
            );
            return;
        }

        if (isEliminated(secondFighter)) {
            win(
                    instance,
                    firstFighter,
                    secondFighter
            );
            return;
        }

        roundTicks++;

        int normalDuration =
                settings.roundDurationSeconds() * 20;

        /*
         * La durée normale n'est pas encore terminée.
         */
        if (roundTicks < normalDuration) {
            return;
        }

        /*
         * Ce bloc n'est exécuté qu'une seule fois :
         * au moment exact où la prolongation commence.
         */
        if (!overtimeStarted) {
            overtimeStarted = true;
            overtimeTicks = 0;

            if (!settings.overtimeEnabled()) {
                finishWithoutWinner(instance);
                return;
            }

            announceOvertime(instance);
            return;
        }

        /*
         * Les ticks de prolongation commencent après l'annonce.
         */
        overtimeTicks++;

        int damageIntervalTicks =
                settings
                        .overtimeDamageIntervalSeconds()
                        * 20;

        if (damageIntervalTicks <= 0
                || overtimeTicks
                % damageIntervalTicks != 0) {
            return;
        }

        firstFighter.reduceHealth(
                settings.overtimeDamage()
        );

        secondFighter.reduceHealth(
                settings.overtimeDamage()
        );

        /*
         * Aucun message n'est envoyé ici.
         * Cela évite le spam dans le chat à chaque dégât.
         */

        float firstHealth =
                firstFighter.health();

        float secondHealth =
                secondFighter.health();

        if (firstHealth <= 1.0F
                && secondHealth <= 1.0F) {
            finishWithoutWinner(instance);
            return;
        }

        if (firstHealth <= 1.0F) {
            win(
                    instance,
                    secondFighter,
                    firstFighter
            );
            return;
        }

        if (secondHealth <= 1.0F) {
            win(
                    instance,
                    firstFighter,
                    secondFighter
            );
        }
    }

    @Override
    public boolean preventsDamage(
            GameInstance instance,
            ServerPlayer player,
            net.minecraft.world.damagesource.DamageSource source,
            float amount
    ) {
        /*
         * Les coups doivent être traités par Minecraft afin que
         * le recul et l'animation d'attaque soient appliqués.
         *
         * La perte de vie est empêchée séparément grâce à
         * l'effet Résistance.
         */
        return false;
    }

    @Override
    public void onEnding(
            GameInstance instance
    ) {
        stopOvertimeMusic(instance);
        removeResistance(instance);
        activePlayerKits.clear();
    }

    @Override
    public void onShutdown(
            GameInstance instance
    ) {
        stopOvertimeMusic(instance);
        removeResistance(instance);
        activePlayerKits.clear();
    }

    private List<SumoFighter> resolveFighters(
            GameInstance instance
    ) {
        List<SumoFighter> result =
                new ArrayList<>();

        for (GameParticipant participant :
                instance.participants()) {
            if (!participant.competitor()) {
                continue;
            }

            if (participant.role()
                    == ParticipantRole.PLAYER) {
                ServerPlayer player =
                        server.getPlayerList()
                                .getPlayer(
                                        participant.playerId()
                                );

                if (player != null) {
                    result.add(
                            SumoFighter.player(player)
                    );
                }

                continue;
            }

            if (participant.role()
                    == ParticipantRole.DUMMY) {
                DummyParticipant dummy =
                        dummyManager
                                .findByParticipantId(
                                        arena.id,
                                        participant.playerId()
                                );

                if (dummy != null) {
                    result.add(
                            SumoFighter.dummy(
                                    dummy,
                                    dummyManager
                            )
                    );
                }
            }
        }

        return result;
    }

    @Override
    public void tickCountdown(
            GameInstance instance,
            int remainingTicks
    ) {
        if (!countdownPrepared) {
            return;
        }

        freezeFighter(
                firstFighter,
                firstFrozenPosition
        );

        freezeFighter(
                secondFighter,
                secondFrozenPosition
        );

        int remainingSeconds =
                Math.max(
                        0,
                        (remainingTicks + 19) / 20
                );

        if (!kitSelectionResolved
                && remainingSeconds
                <= settings
                .closeMenuBeforeStartSeconds()) {
            resolveKitSelection(instance);
        }
    }

    @Override
    public int countdownSeconds(
            GameInstance instance
    ) {
        return settings.countdownSeconds();
    }

    private void prepareFighter(
            SumoFighter fighter,
            ArenaPosition spawn
    ) {
        if (fighter.player() != null) {
            ServerPlayer player =
                    fighter.player();

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

            keepProtected(fighter);

            player.inventoryMenu
                    .broadcastChanges();
        }

        if (!fighter.teleport(
                server,
                spawn
        )) {
            Olympicraft.LOGGER.error(
                    "Impossible de téléporter le combattant "
                            + "'{}' dans l'arène '{}'.",
                    fighter.displayName(),
                    arena.id
            );
        }

        freezeFighter(
                fighter,
                spawn
        );
    }

    private void prepareWaitingFighter(
            SumoFighter fighter
    ) {
        if (fighter.player() != null) {
            fighter.player().setGameMode(
                    GameType.SPECTATOR
            );
        }

        if (arena.spectator != null) {
            fighter.teleport(
                    server,
                    arena.spectator
            );
        }
    }

    private void keepProtected(
            SumoFighter fighter
    ) {
        ServerPlayer player =
                fighter.player();

        if (player == null) {
            return;
        }

        player.setRemainingFireTicks(0);
        player.fallDistance = 0.0F;

        if (!settings.resistanceEnabled()) {
            return;
        }

        MobEffectInstance current =
                player.getEffect(
                        MobEffects.DAMAGE_RESISTANCE
                );

        if (current != null
                && current.getDuration() > 60
                && current.getAmplifier()
                == settings.resistanceAmplifier()) {
            return;
        }

        player.addEffect(
                new MobEffectInstance(
                        MobEffects.DAMAGE_RESISTANCE,
                        100,
                        settings.resistanceAmplifier(),
                        false,
                        false,
                        false
                )
        );
    }

    private ArenaPosition combatSpawn(
            int requestedIndex
    ) {
        List<ArenaPosition> all =
                new ArrayList<>();

        if (arena.spawns != null) {
            for (List<ArenaPosition> group :
                    arena.spawns.values()) {
                if (group != null) {
                    all.addAll(group);
                }
            }
        }

        if (requestedIndex < 0
                || requestedIndex >= all.size()) {
            return null;
        }

        return all.get(requestedIndex);
    }

    private boolean isEliminated(
            SumoFighter fighter
    ) {
        if (!fighter.available()) {
            return true;
        }

        if (arena.regions == null) {
            return false;
        }

        for (ArenaRegion region :
                arena.regions.values()) {
            if (region == null
                    || !ArenaRegionType.VOID.id()
                    .equals(region.type)) {
                continue;
            }

            if (fighter.inside(region)) {
                return true;
            }
        }

        return false;
    }

    private void announceOvertime(
            GameInstance instance
    ) {
        if (settings.overtimeAnnouncementEnabled()) {
            Component title =
                    Olympicraft.messages().render(
                            "sumo.overtime.started",
                            false
                    );

            Component subtitle =
                    Olympicraft.messages().render(
                            "sumo.overtime.subtitle",
                            fr.olympicraft.message.MessageService
                                    .placeholders(
                                            "damage",
                                            formatNumber(
                                                    settings.overtimeDamage()
                                            ),
                                            "seconds",
                                            String.valueOf(
                                                    settings
                                                            .overtimeDamageIntervalSeconds()
                                            )
                                    ),
                            false
                    );

            for (ServerPlayer player :
                    instance.onlineParticipants()) {
                player.connection.send(
                        new net.minecraft.network.protocol.game
                                .ClientboundSetTitlesAnimationPacket(
                                5,
                                50,
                                15
                        )
                );

                player.connection.send(
                        new net.minecraft.network.protocol.game
                                .ClientboundSetTitleTextPacket(
                                title
                        )
                );

                player.connection.send(
                        new net.minecraft.network.protocol.game
                                .ClientboundSetSubtitleTextPacket(
                                subtitle
                        )
                );
            }
        }

        if (settings.overtimeSoundEnabled()) {
            for (ServerPlayer player :
                    instance.onlineParticipants()) {

                boolean enhancedMusicStarted =
                        settings.config()
                                .round
                                .enhancedClientOvertimeSoundEnabled
                                && Olympicraft
                                .enhancedClients()
                                .startLoopSound(
                                        player,
                                        settings.config()
                                                .round
                                                .enhancedClientOvertimeSound,
                                        settings.overtimeSoundVolume(),
                                        settings.overtimeSoundPitch()
                                );

                /*
                 * Les joueurs sans le client Enhanced reçoivent
                 * uniquement le son vanilla de début d'overtime.
                 */
                if (!enhancedMusicStarted) {
                    playConfiguredSound(
                            List.of(player),
                            settings.overtimeSound(),
                            settings.overtimeSoundVolume(),
                            settings.overtimeSoundPitch()
                    );
                }
            }
        }
    }

    private static void playConfiguredSound(
            List<ServerPlayer> players,
            String soundId,
            float volume,
            float pitch
    ) {
        ResourceLocation identifier =
                ResourceLocation.tryParse(soundId);

        if (identifier == null) {
            return;
        }

        SoundEvent sound =
                BuiltInRegistries.SOUND_EVENT
                        .get(identifier);

        if (sound == null) {
            return;
        }

        for (ServerPlayer player : players) {
            player.playNotifySound(
                    sound,
                    SoundSource.MASTER,
                    volume,
                    pitch
            );
        }
    }

    private static String formatNumber(
            float value
    ) {
        if (value == Math.round(value)) {
            return String.valueOf(
                    Math.round(value)
            );
        }

        return String.valueOf(value);
    }

    private void win(
            GameInstance instance,
            SumoFighter winner,
            SumoFighter eliminated
    ) {
        if (roundFinished) {
            return;
        }

        stopOvertimeMusic(instance);

        roundFinished = true;

        SumoVictoryCelebration.play(
                instance,
                winner,
                eliminated
        );

        if (eliminated != null) {
            eliminated.eliminate(
                    server,
                    arena.spectator
            );
        }

        instance.broadcast(
                Component.literal(
                        winner.displayName()
                                + " remporte le duel !"
                )
        );

        instance.end(
                Component.literal(
                        "Fin du duel de Sumo."
                )
        );
    }

    private void finishWithoutWinner(
            GameInstance instance
    ) {
        if (roundFinished) {
            return;
        }

        stopOvertimeMusic(instance);

        roundFinished = true;

        instance.end(
                Component.literal(
                        "Le duel se termine sans gagnant."
                )
        );
    }

    private static void removeResistance(
            GameInstance instance
    ) {
        for (ServerPlayer player :
                instance.onlineParticipants()) {
            player.removeEffect(
                    MobEffects.DAMAGE_RESISTANCE
            );
        }
    }
    private void freezeFighter(
            SumoFighter fighter,
            ArenaPosition position
    ) {
        if (fighter == null
                || position == null
                || !fighter.available()) {
            return;
        }

        ServerPlayer player =
                fighter.player();

        if (player != null) {
            /*
             * Supprime toute vélocité susceptible de faire tomber
             * le joueur juste après sa téléportation.
             */
            player.setDeltaMovement(
                    0.0D,
                    0.0D,
                    0.0D
            );

            player.fallDistance = 0.0F;

            /*
             * On maintient le joueur sur le spawn à chaque tick.
             * Il peut tourner la caméra, mais pas se déplacer.
             */
            var level =
                    position.resolveLevel(server);

            if (level == null) {
                return;
            }

            player.teleportTo(
                    level,
                    position.x,
                    position.y,
                    position.z,
                    player.getYRot(),
                    player.getXRot()
            );

            return;
        }

        /*
         * Pour un dummy, la téléportation périodique garantit que
         * l'ArmorStand ne tombe pas avant le début du duel.
         */
        fighter.teleport(
                server,
                position
        );
    }

    private static void releaseFighter(
            SumoFighter fighter
    ) {
        if (fighter == null) {
            return;
        }

        ServerPlayer player =
                fighter.player();

        if (player == null) {
            return;
        }

        player.setDeltaMovement(
                0.0D,
                0.0D,
                0.0D
        );

        player.fallDistance = 0.0F;
    }

    private static ArenaPosition copyPosition(
            ArenaPosition source
    ) {
        return new ArenaPosition(
                source.dimension,
                source.x,
                source.y,
                source.z,
                source.yaw,
                source.pitch
        );
    }
    private void openSelectionMenus(
            GameInstance instance
    ) {
        for (ServerPlayer player :
                instance.onlineParticipants()) {
            /*
             * Les vrais spectateurs ne participent pas à la sélection.
             */
            GameParticipant participant =
                    instance.participants()
                            .stream()
                            .filter(candidate ->
                                    candidate.playerId()
                                            .equals(
                                                    player.getUUID()
                                            )
                            )
                            .findFirst()
                            .orElse(null);

            if (participant == null
                    || participant.role()
                    != ParticipantRole.PLAYER) {
                continue;
            }

            Olympicraft.gui().open(
                    player,
                    new SumoKitSelectionMenu(
                            settings,
                            kitSelection,
                            server
                    )
            );
        }
    }

    private void closeSelectionMenus(
            GameInstance instance
    ) {
        for (ServerPlayer player :
                instance.onlineParticipants()) {
            if (Olympicraft.gui().isOpen(
                    player,
                    SumoKitSelectionMenu.class
            )) {
                player.closeContainer();
            }
        }
    }

    private void resolveKitSelection(
            GameInstance instance
    ) {
        if (kitSelectionResolved
                || kitSelection == null) {
            return;
        }

        kitSelectionResolved = true;

        closeSelectionMenus(instance);

        List<UUID> eligiblePlayers =
                instance.participants()
                        .stream()
                        .filter(participant ->
                                participant.role()
                                        == ParticipantRole.PLAYER
                        )
                        .map(
                                GameParticipant::playerId
                        )
                        .toList();

        SumoKitSelectionSession.ResolveResult result =
                kitSelection.resolve(
                        eligiblePlayers
                );

        if (!result.successful()) {
            instance.end(
                    Component.literal(
                            "La partie a été annulée : "
                                    + "aucun kit n'a été choisi."
                    )
            );

            return;
        }

        distributeKits(instance);

        if (kitSelection.mode().commonPreset()
                && result.commonPresetId() != null) {
            instance.broadcast(
                    Component.literal(
                            "Kit sélectionné : "
                                    + result.commonPresetId()
                    )
            );
        }
    }

    private void distributeKits(
            GameInstance instance
    ) {
        if (kitDistributed) {
            return;
        }

        kitDistributed = true;

        for (GameParticipant participant :
                instance.participants()) {
            if (!participant.competitor()) {
                continue;
            }

            String presetId;

            if (participant.role()
                    == ParticipantRole.DUMMY) {
                presetId =
                        kitSelection
                                .dummyPresetId();
            } else {
                presetId =
                        kitSelection.presetFor(
                                participant.playerId()
                        );
            }

            SumoConfig.KitPreset preset =
                    settings.findKitPreset(
                            presetId
                    );

            if (preset == null) {
                preset =
                        settings.defaultKitPreset();
            }

            if (participant.role()
                    == ParticipantRole.PLAYER) {
                ServerPlayer player =
                        server.getPlayerList()
                                .getPlayer(
                                        participant.playerId()
                                );

                if (player != null
                        && preset != null) {
                    kitService.give(
                            player,
                            preset
                    );

                    activePlayerKits.put(
                            player.getUUID(),
                            preset
                    );
                }
            }
        }
    }

    public SumoConfig.KitItem findActiveKitItem(
            ServerPlayer player,
            ItemStack stack
    ) {
        if (player == null
                || stack == null
                || stack.isEmpty()) {
            return null;
        }

        SumoConfig.KitPreset preset =
                activePlayerKits.get(
                        player.getUUID()
                );

        if (preset == null
                || preset.items == null) {
            return null;
        }

        int selectedSlot =
                player.getInventory().selected;

        for (SumoConfig.KitItem item :
                preset.items) {
            if (item == null || !item.enabled) {
                continue;
            }

            /*
             * Le slot permet de différencier deux objets identiques
             * possédant des règles différentes.
             */
            int configuredSlot =
                    Math.clamp(
                            item.slot,
                            0,
                            player.getInventory()
                                    .items.size() - 1
                    );

            if (configuredSlot != selectedSlot) {
                continue;
            }

            ResourceLocation itemId =
                    ResourceLocation.tryParse(
                            item.item
                    );

            if (itemId == null) {
                continue;
            }

            if (BuiltInRegistries.ITEM.get(itemId)
                    == stack.getItem()) {
                return item;
            }
        }

        return null;
    }

    private void stopOvertimeMusic(
            GameInstance instance
    ) {
        if (instance == null) {
            return;
        }

        String soundId =
                settings.config()
                        .round
                        .enhancedClientOvertimeSound;

        if (soundId == null
                || soundId.isBlank()) {
            return;
        }

        for (ServerPlayer player :
                instance.onlineParticipants()) {
            Olympicraft.enhancedClients()
                    .stopLoopSound(
                            player,
                            soundId
                    );
        }
    }

    /*
     * Preset réellement attribué à chaque joueur.
     *
     * Cette information permet notamment de retrouver les règles
     * preventDrop et preventMove des objets distribués.
     */
    private final Map<UUID, SumoConfig.KitPreset>
            activePlayerKits =
            new HashMap<>();

    @Override
    public void onPlayerLeft(
            GameInstance instance,
            UUID playerId
    ) {
        if (playerId != null) {
            activePlayerKits.remove(
                    playerId
            );
        }
    }
}