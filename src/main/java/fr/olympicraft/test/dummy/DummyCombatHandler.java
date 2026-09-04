package fr.olympicraft.test.dummy;

import fr.olympicraft.config.OlympicraftConfigManager;
import fr.olympicraft.match.GameInstance;
import fr.olympicraft.match.GameInstanceManager;
import fr.olympicraft.match.GameState;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.decoration.ArmorStand;

public final class DummyCombatHandler {

    private final DummyManager dummies;
    private final GameInstanceManager matches;
    private final OlympicraftConfigManager configs;

    public DummyCombatHandler(
            DummyManager dummies,
            GameInstanceManager matches,
            OlympicraftConfigManager configs
    ) {
        this.dummies = dummies;
        this.matches = matches;
        this.configs = configs;
    }

    public void register() {
        AttackEntityCallback.EVENT.register(
                (player, level, hand, entity, hitResult) -> {
                    if (!(player instanceof ServerPlayer attacker)) {
                        return InteractionResult.PASS;
                    }

                    if (!(entity instanceof ArmorStand)) {
                        return InteractionResult.PASS;
                    }

                    DummyParticipant dummy =
                            dummies.findByEntityId(
                                    entity.getUUID()
                            );

                    if (dummy == null) {
                        return InteractionResult.PASS;
                    }

                    GameInstance instance =
                            matches.findByPlayer(
                                    attacker.getUUID()
                            ).orElse(null);

                    if (instance == null
                            || !instance.arena().id
                            .equalsIgnoreCase(
                                    dummy.arenaId()
                            )) {
                        return InteractionResult.FAIL;
                    }

                    if (instance.state() != GameState.RUNNING) {
                        return InteractionResult.FAIL;
                    }

                    var settings =
                            configs.sumo().dummy;

                    if (!settings.enabled) {
                        return InteractionResult.FAIL;
                    }

                    double horizontal =
                            settings.horizontalKnockback;

                    if (attacker.isSprinting()) {
                        horizontal *=
                                settings.sprintMultiplier;
                    }

                    dummies.applyKnockback(
                            dummy,
                            attacker,
                            horizontal,
                            settings.verticalKnockback
                    );

                    /*
                     * SUCCESS empêche le traitement vanilla qui
                     * pourrait endommager ou détruire l'ArmorStand.
                     */
                    return InteractionResult.SUCCESS;
                }
        );
    }
}