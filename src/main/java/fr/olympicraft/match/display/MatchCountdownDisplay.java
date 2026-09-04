package fr.olympicraft.match.display;

import fr.olympicraft.match.GameInstance;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public final class MatchCountdownDisplay {

    private final ServerBossEvent bossBar =
            new ServerBossEvent(
                    Component.literal(
                            "Démarrage de la partie"
                    ),
                    BossEvent.BossBarColor.BLUE,
                    BossEvent.BossBarOverlay.PROGRESS
            );

    /*
     * Évite de jouer plusieurs fois le son pendant
     * la même seconde.
     */
    private int lastDisplayedSecond = -1;

    public void start(
            GameInstance instance,
            int totalSeconds
    ) {
        lastDisplayedSecond = -1;

        bossBar.setVisible(true);
        bossBar.setProgress(1.0F);
        bossBar.setName(
                countdownTitle(totalSeconds)
        );

        synchronizePlayers(instance);
    }

    public void update(
            GameInstance instance,
            int remainingSeconds,
            int totalSeconds
    ) {
        synchronizePlayers(instance);

        int safeTotal =
                Math.max(1, totalSeconds);

        int safeRemaining =
                Math.max(0, remainingSeconds);

        float progress =
                Math.clamp(
                        (float) safeRemaining
                                / (float) safeTotal,
                        0.0F,
                        1.0F
                );

        bossBar.setProgress(progress);
        bossBar.setName(
                countdownTitle(safeRemaining)
        );

        if (lastDisplayedSecond
                == safeRemaining) {
            return;
        }

        lastDisplayedSecond = safeRemaining;

        /*
         * Son chaque seconde pendant les cinq dernières
         * secondes, ainsi qu'au début du décompte.
         */
        if (safeRemaining <= 5
                || safeRemaining == safeTotal) {
            playCountdownSound(
                    instance,
                    safeRemaining
            );
        }
    }

    public void finish(GameInstance instance) {
        synchronizePlayers(instance);

        bossBar.setName(
                Component.literal(
                        "C'est parti !"
                )
        );

        bossBar.setProgress(1.0F);
        bossBar.setColor(
                BossEvent.BossBarColor.GREEN
        );

        for (ServerPlayer player :
                instance.onlineParticipants()) {
            player.playNotifySound(
                    SoundEvents.PLAYER_LEVELUP,
                    SoundSource.MASTER,
                    0.8F,
                    1.2F
            );
        }
    }

    public void hide() {
        bossBar.removeAllPlayers();
        bossBar.setVisible(false);
        bossBar.setColor(
                BossEvent.BossBarColor.BLUE
        );

        lastDisplayedSecond = -1;
    }

    private void synchronizePlayers(
            GameInstance instance
    ) {
        Set<UUID> expected =
                new LinkedHashSet<>();

        for (ServerPlayer player :
                instance.onlineParticipants()) {
            expected.add(player.getUUID());

            if (!bossBar.getPlayers()
                    .contains(player)) {
                bossBar.addPlayer(player);
            }
        }

        for (ServerPlayer player :
                Set.copyOf(
                        bossBar.getPlayers()
                )) {
            if (!expected.contains(
                    player.getUUID()
            )) {
                bossBar.removePlayer(player);
            }
        }
    }

    private static Component countdownTitle(
            int seconds
    ) {
        return Component.literal(
                "Démarrage dans "
                        + seconds
                        + " seconde"
                        + (seconds > 1 ? "s" : "")
        );
    }

    private static void playCountdownSound(
            GameInstance instance,
            int remainingSeconds
    ) {
        float pitch;

        if (remainingSeconds <= 1) {
            pitch = 1.8F;
        } else if (remainingSeconds <= 3) {
            pitch = 1.5F;
        } else {
            pitch = 1.2F;
        }

        for (ServerPlayer player :
                instance.onlineParticipants()) {
            player.playNotifySound(
                    SoundEvents.NOTE_BLOCK_HAT.value(),
                    SoundSource.MASTER,
                    0.6F,
                    pitch
            );
        }
    }
}