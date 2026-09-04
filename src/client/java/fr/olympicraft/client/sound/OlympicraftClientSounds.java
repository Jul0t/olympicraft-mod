package fr.olympicraft.client.sound;

import fr.olympicraft.client.config.OlympicraftClientConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

import java.util.HashMap;
import java.util.Map;

public final class OlympicraftClientSounds {

    private static final ResourceLocation ENHANCED_TEST_ID =
            ResourceLocation.fromNamespaceAndPath(
                    "olympicraft",
                    "enhanced.test"
            );

    /*
     * Musiques actuellement jouées en boucle.
     */
    private static final Map<
            ResourceLocation,
            LoopingSound
            > LOOPING_SOUNDS =
            new HashMap<>();

    private OlympicraftClientSounds() {
    }

    public static void playEnhancedTest() {
        play(
                ENHANCED_TEST_ID.toString(),
                1.0F,
                1.0F
        );
    }

    public static void play(
            String requestedSound,
            float requestedVolume,
            float requestedPitch
    ) {
        ResourceLocation soundId =
                ResourceLocation.tryParse(
                        requestedSound
                );

        if (soundId == null) {
            return;
        }

        SimpleSoundInstance sound =
                createSound(
                        soundId,
                        requestedVolume,
                        requestedPitch,
                        false
                );

        Minecraft.getInstance()
                .getSoundManager()
                .play(sound);
    }

    public static void playLoop(
            String requestedSound,
            float requestedVolume,
            float requestedPitch
    ) {
        ResourceLocation soundId =
                ResourceLocation.tryParse(
                        requestedSound
                );

        if (soundId == null) {
            return;
        }

        stopLoop(requestedSound);

        SimpleSoundInstance sound =
                createSound(
                        soundId,
                        requestedVolume,
                        requestedPitch,
                        true
                );

        LOOPING_SOUNDS.put(
                soundId,
                new LoopingSound(
                        requestedVolume,
                        requestedPitch,
                        sound
                )
        );

        Minecraft.getInstance()
                .getSoundManager()
                .play(sound);
    }

    public static void stopLoop(
            String requestedSound
    ) {
        ResourceLocation soundId =
                ResourceLocation.tryParse(
                        requestedSound
                );

        if (soundId == null) {
            return;
        }

        LoopingSound loopingSound =
                LOOPING_SOUNDS.remove(
                        soundId
                );

        if (loopingSound == null) {
            return;
        }

        Minecraft.getInstance()
                .getSoundManager()
                .stop(
                        loopingSound.instance()
                );
    }

    public static void stopAllLoops() {
        Minecraft minecraft =
                Minecraft.getInstance();

        for (LoopingSound sound :
                LOOPING_SOUNDS.values()) {
            minecraft.getSoundManager()
                    .stop(
                            sound.instance()
                    );
        }

        LOOPING_SOUNDS.clear();
    }

    /*
     * Minecraft ne permet pas simplement de modifier le volume
     * d'un SimpleSoundInstance déjà démarré.
     *
     * Les boucles actives sont donc arrêtées puis recréées
     * immédiatement avec le nouveau volume.
     */
    public static void refreshLoopVolumes() {
        if (LOOPING_SOUNDS.isEmpty()) {
            return;
        }

        Minecraft minecraft =
                Minecraft.getInstance();

        Map<ResourceLocation, LoopingSound> refreshed =
                new HashMap<>();

        for (Map.Entry<ResourceLocation, LoopingSound> entry :
                LOOPING_SOUNDS.entrySet()) {
            ResourceLocation soundId =
                    entry.getKey();

            LoopingSound previous =
                    entry.getValue();

            minecraft.getSoundManager()
                    .stop(
                            previous.instance()
                    );

            SimpleSoundInstance replacement =
                    createSound(
                            soundId,
                            previous.requestedVolume(),
                            previous.requestedPitch(),
                            true
                    );

            minecraft.getSoundManager()
                    .play(replacement);

            refreshed.put(
                    soundId,
                    new LoopingSound(
                            previous.requestedVolume(),
                            previous.requestedPitch(),
                            replacement
                    )
            );
        }

        LOOPING_SOUNDS.clear();
        LOOPING_SOUNDS.putAll(refreshed);
    }

    private static SimpleSoundInstance createSound(
            ResourceLocation soundId,
            float requestedVolume,
            float requestedPitch,
            boolean looping
    ) {
        float volume =
                OlympicraftClientConfig
                        .applySoundVolume(
                                Math.clamp(
                                        requestedVolume,
                                        0.0F,
                                        4.0F
                                )
                        );

        float pitch =
                Math.clamp(
                        requestedPitch,
                        0.1F,
                        2.0F
                );

        return new SimpleSoundInstance(
                soundId,
                SoundSource.MASTER,
                volume,
                pitch,
                RandomSource.create(),
                looping,
                0,
                SimpleSoundInstance.Attenuation.NONE,
                0.0D,
                0.0D,
                0.0D,
                true
        );
    }

    private record LoopingSound(
            float requestedVolume,
            float requestedPitch,
            SimpleSoundInstance instance
    ) {
    }
}