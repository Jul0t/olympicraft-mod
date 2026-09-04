package fr.olympicraft.match.runtime;

import fr.olympicraft.match.GameInstance;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;

import java.util.UUID;

public interface GameRuntime {

    default void onCreated(
            GameInstance instance
    ) {
    }

    default void onPlayerJoined(
            GameInstance instance,
            ServerPlayer player,
            boolean spectator
    ) {
    }

    default void onPlayerLeft(
            GameInstance instance,
            UUID playerId
    ) {
    }

    default boolean canStart(
            GameInstance instance
    ) {
        return instance.playerCount() > 0;
    }

    /*
     * Durée du compte à rebours propre au mini-jeu.
     * Les runtimes qui ne redéfinissent pas cette méthode
     * utilisent dix secondes.
     */
    default int countdownSeconds(
            GameInstance instance
    ) {
        return 10;
    }

    /*
     * Appelée avant que GameInstance passe à STARTING.
     *
     * Un runtime peut résoudre les participants, les préparer
     * et les téléporter ici. Retourner false annule le démarrage.
     */
    default boolean prepareCountdown(
            GameInstance instance
    ) {
        return true;
    }

    /*
     * Appelée une fois lorsque le décompte est officiellement
     * démarré.
     */
    default void onCountdownStarted(
            GameInstance instance
    ) {
    }

    /*
     * Appelée à chaque tick pendant STARTING.
     *
     * Elle permet notamment au Sumo de maintenir les joueurs
     * immobiles sur leur position de départ.
     */
    default void tickCountdown(
            GameInstance instance,
            int remainingTicks
    ) {
    }

    default void onCountdownCancelled(
            GameInstance instance
    ) {
    }

    /*
     * Appelée lorsque le décompte arrive à zéro et que l'état
     * passe à RUNNING.
     */
    default void onStarted(
            GameInstance instance
    ) {
    }

    default void tick(
            GameInstance instance
    ) {
    }

    default void onEnding(
            GameInstance instance
    ) {
    }

    default void onReset(
            GameInstance instance
    ) {
    }

    default void onShutdown(
            GameInstance instance
    ) {
    }

    default boolean preventsDamage(
            GameInstance instance,
            ServerPlayer player,
            DamageSource source,
            float amount
    ) {
        return false;
    }
}