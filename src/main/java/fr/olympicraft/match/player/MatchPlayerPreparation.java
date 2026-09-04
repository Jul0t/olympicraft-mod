package fr.olympicraft.match.player;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.level.GameType;

import java.util.ArrayList;

public final class MatchPlayerPreparation {

    private MatchPlayerPreparation() {
    }

    public static void prepareParticipant(
            ServerPlayer player
    ) {
        closeAndClear(player);

        player.setGameMode(GameType.ADVENTURE);

        player.getAbilities().invulnerable = false;
        player.getAbilities().mayfly = false;
        player.getAbilities().flying = false;
        player.onUpdateAbilities();

        resetVitals(player);
    }

    public static void prepareSpectator(
            ServerPlayer player
    ) {
        closeAndClear(player);

        player.setGameMode(GameType.SPECTATOR);

        player.getAbilities().invulnerable = true;
        player.getAbilities().mayfly = true;
        player.getAbilities().flying = true;
        player.onUpdateAbilities();

        resetVitals(player);
    }

    private static void closeAndClear(
            ServerPlayer player
    ) {
        player.closeContainer();

        player.getInventory().clearContent();
        player.getInventory().setChanged();

        /*
         * Une copie est utilisée afin d'éviter de modifier
         * la collection pendant son parcours.
         */
        for (MobEffectInstance effect :
                new ArrayList<>(
                        player.getActiveEffects()
                )) {
            player.removeEffect(
                    effect.getEffect()
            );
        }

        player.setRemainingFireTicks(0);
        player.fallDistance = 0.0F;
    }

    private static void resetVitals(
            ServerPlayer player
    ) {
        player.setHealth(player.getMaxHealth());

        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(5.0F);

        player.setAirSupply(
                player.getMaxAirSupply()
        );

        player.experienceLevel = 0;
        player.totalExperience = 0;
        player.experienceProgress = 0.0F;

        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
    }
}