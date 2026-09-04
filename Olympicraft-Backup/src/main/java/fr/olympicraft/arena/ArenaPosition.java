package fr.olympicraft.arena;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

public final class ArenaPosition {

    public String dimension = "minecraft:overworld";

    public double x;
    public double y;
    public double z;

    public float yaw;
    public float pitch;

    public ArenaPosition() {
    }

    public ArenaPosition(
            String dimension,
            double x,
            double y,
            double z,
            float yaw,
            float pitch
    ) {
        this.dimension = dimension;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
    }

    public static ArenaPosition from(
            ServerLevel level,
            double x,
            double y,
            double z,
            float yaw,
            float pitch
    ) {
        return new ArenaPosition(
                level.dimension().location().toString(),
                x,
                y,
                z,
                yaw,
                pitch
        );
    }

    public ResourceKey<Level> dimensionKey() {
        ResourceLocation identifier =
                ResourceLocation.tryParse(dimension);

        if (identifier == null) {
            identifier = Level.OVERWORLD.location();
        }

        return ResourceKey.create(
                net.minecraft.core.registries.Registries.DIMENSION,
                identifier
        );
    }

    public String formatted() {
        return String.format(
                "%.2f, %.2f, %.2f (%s)",
                x,
                y,
                z,
                dimension
        );
    }
}
