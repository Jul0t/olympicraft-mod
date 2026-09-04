package fr.olympicraft.client;

import fr.olympicraft.client.gui.EnhancedTestScreen;
import fr.olympicraft.client.sound.OlympicraftClientSounds;
import fr.olympicraft.client.config.OlympicraftClientConfig;
import fr.olympicraft.network.OlympicraftPayloads;
import fr.olympicraft.network.payload.EnhancedHelloPayload;
import fr.olympicraft.network.payload.OpenEnhancedTestScreenPayload;
import fr.olympicraft.network.payload.PlayEnhancedSoundPayload;
import fr.olympicraft.network.payload.StartEnhancedLoopSoundPayload;
import fr.olympicraft.network.payload.StopEnhancedLoopSoundPayload;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class OlympicraftClient
        implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        OlympicraftClientConfig.load();

        OlympicraftPayloads.register();

        ClientPlayNetworking.registerGlobalReceiver(
                OpenEnhancedTestScreenPayload.TYPE,
                (payload, context) ->
                        context.client().execute(() ->
                                context.client().setScreen(
                                        new EnhancedTestScreen(
                                                payload.title(),
                                                payload.message()
                                        )
                                )
                        )
        );

        ClientPlayNetworking.registerGlobalReceiver(
                PlayEnhancedSoundPayload.TYPE,
                (payload, context) ->
                        context.client().execute(() ->
                                OlympicraftClientSounds.play(
                                        payload.soundId(),
                                        payload.volume(),
                                        payload.pitch()
                                )
                        )
        );

        ClientPlayNetworking.registerGlobalReceiver(
                StartEnhancedLoopSoundPayload.TYPE,
                (payload, context) ->
                        context.client().execute(() ->
                                OlympicraftClientSounds.playLoop(
                                        payload.soundId(),
                                        payload.volume(),
                                        payload.pitch()
                                )
                        )
        );

        ClientPlayNetworking.registerGlobalReceiver(
                StopEnhancedLoopSoundPayload.TYPE,
                (payload, context) ->
                        context.client().execute(() ->
                                OlympicraftClientSounds.stopLoop(
                                        payload.soundId()
                                )
                        )
        );

        ClientPlayConnectionEvents.JOIN.register(
                (handler, sender, client) -> {
                    if (!ClientPlayNetworking.canSend(
                            EnhancedHelloPayload.TYPE
                    )) {
                        return;
                    }

                    ClientPlayNetworking.send(
                            new EnhancedHelloPayload(
                                    "1.0.0"
                            )
                    );
                }
        );

        /*
         * Coupe les musiques si le joueur quitte le serveur
         * pendant une partie.
         */
        ClientPlayConnectionEvents.DISCONNECT.register(
                (handler, client) ->
                        OlympicraftClientSounds
                                .stopAllLoops()
        );
    }
}