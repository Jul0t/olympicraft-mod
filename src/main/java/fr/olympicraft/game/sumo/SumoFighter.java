package fr.olympicraft.game.sumo;

import fr.olympicraft.arena.ArenaPosition;
import fr.olympicraft.arena.ArenaRegion;
import fr.olympicraft.arena.ArenaRegionBounds;
import fr.olympicraft.test.dummy.DummyManager;
import fr.olympicraft.test.dummy.DummyParticipant;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.GameType;

import java.util.UUID;

public final class SumoFighter {

    private final UUID participantId;
    private final String displayName;

    private final ServerPlayer player;
    private final DummyParticipant dummy;
    private final DummyManager dummyManager;

    private SumoFighter(
            UUID participantId,
            String displayName,
            ServerPlayer player,
            DummyParticipant dummy,
            DummyManager dummyManager
    ) {
        this.participantId = participantId;
        this.displayName = displayName;
        this.player = player;
        this.dummy = dummy;
        this.dummyManager = dummyManager;
    }

    public static SumoFighter player(
            ServerPlayer player
    ) {
        return new SumoFighter(
                player.getUUID(),
                player.getGameProfile().getName(),
                player,
                null,
                null
        );
    }

    public static SumoFighter dummy(
            DummyParticipant dummy,
            DummyManager manager
    ) {
        return new SumoFighter(
                dummy.participantId(),
                dummy.name(),
                null,
                dummy,
                manager
        );
    }

    public UUID participantId() {
        return participantId;
    }

    public String displayName() {
        return displayName;
    }

    public boolean dummy() {
        return dummy != null;
    }

    public ServerPlayer player() {
        return player;
    }

    public DummyParticipant dummyParticipant() {
        return dummy;
    }

    public boolean available() {
        if (player != null) {
            return !player.isRemoved()
                    && player.isAlive();
        }

        return dummyManager != null
                && dummyManager.entity(dummy) != null;
    }

    public boolean teleport(
            MinecraftServer server,
            ArenaPosition position
    ) {
        if (position == null) {
            return false;
        }

        if (player != null) {
            return position.teleport(
                    server,
                    player
            );
        }

        return dummyManager != null
                && dummyManager.teleport(
                dummy,
                position
        );
    }

    public boolean inside(
            ArenaRegion region
    ) {
        if (region == null) {
            return false;
        }

        if (player != null) {
            return ArenaRegionBounds.contains(
                    region,
                    player
            );
        }

        if (dummyManager == null) {
            return false;
        }

        ArmorStand entity =
                dummyManager.entity(dummy);

        if (entity == null) {
            return false;
        }

        String dimension =
                entity.level()
                        .dimension()
                        .location()
                        .toString();

        return ArenaRegionBounds.contains(
                region,
                dimension,
                entity.getX(),
                entity.getY(),
                entity.getZ()
        );
    }

    public float health() {
        if (player != null) {
            return player.getHealth();
        }

        ArmorStand entity =
                dummyManager == null
                        ? null
                        : dummyManager.entity(dummy);

        return entity == null
                ? 0.0F
                : entity.getHealth();
    }

    public void reduceHealth(float amount) {
        float safeAmount =
                Math.max(0.0F, amount);

        if (player != null) {
            player.setHealth(
                    Math.max(
                            1.0F,
                            player.getHealth() - safeAmount
                    )
            );

            return;
        }

        ArmorStand entity =
                dummyManager == null
                        ? null
                        : dummyManager.entity(dummy);

        if (entity != null) {
            entity.setHealth(
                    Math.max(
                            1.0F,
                            entity.getHealth() - safeAmount
                    )
            );
        }
    }

    public void eliminate(
            MinecraftServer server,
            ArenaPosition spectator
    ) {
        if (player != null) {
            player.setGameMode(
                    GameType.SPECTATOR
            );

            if (spectator != null) {
                spectator.teleport(
                        server,
                        player
                );
            }

            return;
        }

        if (dummyManager != null) {
            dummyManager.removeEntityOnly(dummy);
        }
    }

    public ArmorStand dummyEntity() {
        if (dummy == null || dummyManager == null) {
            return null;
        }

        return dummyManager.entity(dummy);
    }
}