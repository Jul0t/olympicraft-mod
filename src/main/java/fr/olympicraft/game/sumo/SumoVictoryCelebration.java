package fr.olympicraft.game.sumo;

import fr.olympicraft.Olympicraft;
import fr.olympicraft.config.model.game.SumoConfig;
import fr.olympicraft.match.GameInstance;
import fr.olympicraft.message.MessageService;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.decoration.ArmorStand;

import org.joml.Vector3f;

public final class SumoVictoryCelebration {

    private SumoVictoryCelebration() {
    }

    public static void play(
            GameInstance instance,
            SumoFighter winner,
            SumoFighter loser
    ) {
        if (instance == null || winner == null) {
            return;
        }

        SumoConfig.Victory config =
                Olympicraft.configs()
                        .sumo()
                        .victory;

        MessageService messages =
                Olympicraft.messages();

        Component victoryTitle =
                messages.render(
                        "sumo.victory.title",
                        MessageService.placeholders(
                                "winner",
                                winner.displayName()
                        ),
                        false
                );

        Component victorySubtitle =
                messages.render(
                        "sumo.victory.subtitle",
                        MessageService.placeholders(
                                "winner",
                                winner.displayName()
                        ),
                        false
                );

        Component defeatTitle =
                messages.render(
                        "sumo.defeat.title",
                        MessageService.placeholders(
                                "winner",
                                winner.displayName()
                        ),
                        false
                );

        Component defeatSubtitle =
                messages.render(
                        "sumo.defeat.subtitle",
                        MessageService.placeholders(
                                "winner",
                                winner.displayName()
                        ),
                        false
                );

        for (ServerPlayer viewer :
                instance.onlineParticipants()) {
            boolean viewerIsWinner =
                    winner.player() != null
                            && winner.player()
                            .getUUID()
                            .equals(viewer.getUUID());

            boolean viewerIsLoser =
                    loser != null
                            && loser.player() != null
                            && loser.player()
                            .getUUID()
                            .equals(viewer.getUUID());

            if (viewerIsLoser
                    && config.defeatTitleEnabled) {
                sendTitle(
                        viewer,
                        defeatTitle,
                        defeatSubtitle,
                        config
                );

                if (config.defeatSoundEnabled) {
                    playPersonalSound(
                            viewer,
                            config.enhancedClientDefeatMusicEnabled,
                            config.enhancedClientDefeatSound,
                            config.defeatSound,
                            config.defeatSoundVolume,
                            config.defeatSoundPitch
                    );
                }

                continue;
            }

            if (config.titleEnabled) {
                sendTitle(
                        viewer,
                        victoryTitle,
                        victorySubtitle,
                        config
                );
            }

            if (config.soundsEnabled) {
                playPersonalSound(
                        viewer,
                        config.enhancedClientMusicEnabled,
                        config.enhancedClientSound,
                        config.victorySound,
                        config.victorySoundVolume,
                        config.victorySoundPitch
                );
            }
        }

        if (config.particlesEnabled) {
            spawnParticles(winner);
        }
    }

    private static void sendTitle(
            ServerPlayer player,
            Component title,
            Component subtitle,
            SumoConfig.Victory config
    ) {
        player.connection.send(
                new ClientboundSetTitlesAnimationPacket(
                        config.fadeInTicks,
                        config.stayTicks,
                        config.fadeOutTicks
                )
        );

        player.connection.send(
                new ClientboundSetSubtitleTextPacket(
                        subtitle
                )
        );

        player.connection.send(
                new ClientboundSetTitleTextPacket(
                        title
                )
        );
    }

    private static void playPersonalSound(
            ServerPlayer player,
            boolean enhancedSoundEnabled,
            String enhancedSound,
            String vanillaSound,
            float volume,
            float pitch
    ) {
        boolean enhancedSoundPlayed =
                enhancedSoundEnabled
                        && Olympicraft
                        .enhancedClients()
                        .playSound(
                                player,
                                enhancedSound,
                                volume,
                                pitch
                        );

        /*
         * Si le joueur n'a pas le client Enhanced ou si le paquet
         * ne peut pas être envoyé, le son vanilla est utilisé.
         */
        if (!enhancedSoundPlayed) {
            playVanillaSound(
                    player,
                    vanillaSound,
                    volume,
                    pitch
            );
        }
    }

    private static void playVanillaSound(
            ServerPlayer player,
            String requestedSound,
            float volume,
            float pitch
    ) {
        ResourceLocation identifier =
                ResourceLocation.tryParse(
                        requestedSound
                );

        if (identifier == null) {
            return;
        }

        SoundEvent sound =
                BuiltInRegistries.SOUND_EVENT
                        .get(identifier);

        if (sound == null) {
            return;
        }

        player.playNotifySound(
                sound,
                SoundSource.MASTER,
                volume,
                pitch
        );
    }

    private static void spawnParticles(
            SumoFighter winner
    ) {
        ServerPlayer player =
                winner.player();

        if (player != null) {
            spawnParticles(
                    player.serverLevel(),
                    player.getX(),
                    player.getY() + 1.0D,
                    player.getZ()
            );

            return;
        }

        ArmorStand dummy =
                winner.dummyEntity();

        if (dummy == null
                || !(dummy.level()
                instanceof ServerLevel level)) {
            return;
        }

        spawnParticles(
                level,
                dummy.getX(),
                dummy.getY() + 1.0D,
                dummy.getZ()
        );
    }

    private static void spawnParticles(
            ServerLevel level,
            double x,
            double y,
            double z
    ) {
        DustParticleOptions gold =
                new DustParticleOptions(
                        new Vector3f(
                                1.0F,
                                0.72F,
                                0.0F
                        ),
                        1.25F
                );

        DustParticleOptions cyan =
                new DustParticleOptions(
                        new Vector3f(
                                0.0F,
                                0.85F,
                                1.0F
                        ),
                        1.0F
                );

        level.sendParticles(
                gold,
                x,
                y,
                z,
                35,
                0.8D,
                1.0D,
                0.8D,
                0.05D
        );

        level.sendParticles(
                cyan,
                x,
                y + 0.4D,
                z,
                25,
                0.7D,
                0.8D,
                0.7D,
                0.04D
        );
    }
}