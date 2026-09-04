package fr.olympicraft.network;

import fr.olympicraft.network.payload.EnhancedHelloPayload;
import fr.olympicraft.network.payload.OpenEnhancedTestScreenPayload;
import fr.olympicraft.network.payload.PlayEnhancedSoundPayload;
import fr.olympicraft.network.payload.StartEnhancedLoopSoundPayload;
import fr.olympicraft.network.payload.StopEnhancedLoopSoundPayload;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public final class OlympicraftPayloads {

    private static boolean registered;

    private OlympicraftPayloads() {
    }

    public static void register() {
        if (registered) {
            return;
        }

        registered = true;

        PayloadTypeRegistry.playC2S().register(
                EnhancedHelloPayload.TYPE,
                EnhancedHelloPayload.STREAM_CODEC
        );

        PayloadTypeRegistry.playS2C().register(
                OpenEnhancedTestScreenPayload.TYPE,
                OpenEnhancedTestScreenPayload.STREAM_CODEC
        );

        PayloadTypeRegistry.playS2C().register(
                PlayEnhancedSoundPayload.TYPE,
                PlayEnhancedSoundPayload.STREAM_CODEC
        );

        PayloadTypeRegistry.playS2C().register(
                StartEnhancedLoopSoundPayload.TYPE,
                StartEnhancedLoopSoundPayload.STREAM_CODEC
        );

        PayloadTypeRegistry.playS2C().register(
                StopEnhancedLoopSoundPayload.TYPE,
                StopEnhancedLoopSoundPayload.STREAM_CODEC
        );
    }
}