package fr.olympicraft.gui;

import net.minecraft.server.level.ServerPlayer;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GuiCapabilities {

    private final Set<UUID> enhancedClients =
            ConcurrentHashMap.newKeySet();

    public void markEnhanced(ServerPlayer player) {
        enhancedClients.add(player.getUUID());
    }

    public void clear(ServerPlayer player) {
        enhancedClients.remove(player.getUUID());
    }

    public void clearAll() {
        enhancedClients.clear();
    }

    public boolean supportsEnhanced(
            ServerPlayer player
    ) {
        return enhancedClients.contains(
                player.getUUID()
        );
    }
}