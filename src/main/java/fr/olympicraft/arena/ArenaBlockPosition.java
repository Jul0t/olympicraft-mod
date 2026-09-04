package fr.olympicraft.arena;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public final class ArenaBlockPosition {

    public String dimension = "minecraft:overworld";

    public int x;
    public int y;
    public int z;

    public ArenaBlockPosition() {
    }

    public ArenaBlockPosition(
            String dimension,
            int x,
            int y,
            int z
    ) {
        this.dimension = dimension;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static ArenaBlockPosition from(
            ServerLevel level,
            BlockPos position
    ) {
        return new ArenaBlockPosition(
                level.dimension().location().toString(),
                position.getX(),
                position.getY(),
                position.getZ()
        );
    }

    public BlockPos toBlockPos() {
        return new BlockPos(x, y, z);
    }

    public String formatted() {
        return x + ", " + y + ", " + z
                + " (" + dimension + ")";
    }
}
