package fr.olympicraft.game.sumo.kit;

import fr.olympicraft.Olympicraft;
import fr.olympicraft.config.model.game.SumoConfig;
import fr.olympicraft.game.sumo.SumoRuntime;
import fr.olympicraft.match.GameInstance;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public final class SumoKitProtectionService {

    private SumoKitProtectionService() {
    }

    public static boolean preventsDrop(
            ServerPlayer player
    ) {
        if (player == null) {
            return false;
        }

        GameInstance instance =
                Olympicraft.matches()
                        .findByPlayer(
                                player.getUUID()
                        )
                        .orElse(null);

        if (instance == null
                || !(instance.runtime()
                instanceof SumoRuntime runtime)) {
            return false;
        }

        ItemStack selectedStack =
                player.getMainHandItem();

        if (selectedStack.isEmpty()) {
            return false;
        }

        SumoConfig.KitItem kitItem =
                runtime.findActiveKitItem(
                        player,
                        selectedStack
                );

        return kitItem != null
                && kitItem.preventDrop;
    }
}