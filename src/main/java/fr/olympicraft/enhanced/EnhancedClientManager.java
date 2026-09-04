package fr.olympicraft.enhanced;

import fr.olympicraft.Olympicraft;
import fr.olympicraft.network.payload.EnhancedHelloPayload;
import fr.olympicraft.network.payload.OpenEnhancedTestScreenPayload;
import fr.olympicraft.network.payload.PlayEnhancedSoundPayload;
import fr.olympicraft.network.payload.StartEnhancedLoopSoundPayload;
import fr.olympicraft.network.payload.StopEnhancedLoopSoundPayload;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class EnhancedClientManager {

    private final Map<UUID, String> clients =
            new ConcurrentHashMap<>();

    private boolean receiverRegistered;

    public void registerReceiver() {
        if (receiverRegistered) {
            return;
        }

        receiverRegistered = true;

        ServerPlayNetworking.registerGlobalReceiver(
                EnhancedHelloPayload.TYPE,
                (payload, context) -> {
                    ServerPlayer player =
                            context.player();

                    String version =
                            sanitizeVersion(
                                    payload.clientVersion()
                            );

                    clients.put(
                            player.getUUID(),
                            version
                    );

                    Olympicraft.LOGGER.info(
                            "Client Enhanced détecté pour '{}' "
                                    + "(version {}).",
                            player.getGameProfile().getName(),
                            version
                    );
                }
        );
    }

    public boolean isEnhanced(
            ServerPlayer player
    ) {
        return player != null
                && clients.containsKey(
                player.getUUID()
        );
    }

    public String version(
            ServerPlayer player
    ) {
        if (player == null) {
            return "";
        }

        return clients.getOrDefault(
                player.getUUID(),
                ""
        );
    }

    public boolean openTestScreen(
            ServerPlayer player
    ) {
        if (!isEnhanced(player)) {
            return false;
        }

        if (!ServerPlayNetworking.canSend(
                player,
                OpenEnhancedTestScreenPayload.TYPE
        )) {
            return false;
        }

        ServerPlayNetworking.send(
                player,
                new OpenEnhancedTestScreenPayload(
                        "Olympicraft Enhanced",
                        "Le client Enhanced fonctionne correctement !"
                )
        );

        /*
         * Test temporaire du slider Sons Olympicraft.
         */
        playSound(
                player,
                "olympicraft:sumo.overtime",
                1.0F,
                1.0F
        );

        return true;
    }

    public boolean playSound(
            ServerPlayer player,
            String soundId,
            float volume,
            float pitch
    ) {
        if (!isEnhanced(player)
                || soundId == null
                || soundId.isBlank()) {
            return false;
        }

        if (!ServerPlayNetworking.canSend(
                player,
                PlayEnhancedSoundPayload.TYPE
        )) {
            return false;
        }

        ServerPlayNetworking.send(
                player,
                new PlayEnhancedSoundPayload(
                        soundId,
                        volume,
                        pitch
                )
        );

        return true;
    }

    public boolean startLoopSound(
            ServerPlayer player,
            String soundId,
            float volume,
            float pitch
    ) {
        if (!isEnhanced(player)
                || soundId == null
                || soundId.isBlank()) {
            return false;
        }

        if (!ServerPlayNetworking.canSend(
                player,
                StartEnhancedLoopSoundPayload.TYPE
        )) {
            return false;
        }

        ServerPlayNetworking.send(
                player,
                new StartEnhancedLoopSoundPayload(
                        soundId,
                        volume,
                        pitch
                )
        );

        return true;
    }

    public boolean stopLoopSound(
            ServerPlayer player,
            String soundId
    ) {
        if (!isEnhanced(player)
                || soundId == null
                || soundId.isBlank()) {
            return false;
        }

        if (!ServerPlayNetworking.canSend(
                player,
                StopEnhancedLoopSoundPayload.TYPE
        )) {
            return false;
        }

        ServerPlayNetworking.send(
                player,
                new StopEnhancedLoopSoundPayload(
                        soundId
                )
        );

        return true;
    }

    public void handleDisconnect(
            ServerPlayer player
    ) {
        if (player != null) {
            clients.remove(
                    player.getUUID()
            );
        }
    }

    public void clear() {
        clients.clear();
    }

    private static String sanitizeVersion(
            String version
    ) {
        if (version == null
                || version.isBlank()) {
            return "inconnue";
        }

        String result =
                version.trim();

        if (result.length() > 64) {
            result = result.substring(
                    0,
                    64
            );
        }

        return result;
    }
}