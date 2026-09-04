package fr.olympicraft.arena;

import net.minecraft.core.BlockPos;

public final class ArenaRegion {

    public String id = "";

    public String type = "";

    public String dimension = "minecraft:overworld";

    public int minX;
    public int minY;
    public int minZ;

    public int maxX;
    public int maxY;
    public int maxZ;

    public ArenaRegion() {
    }

    public ArenaRegion(
            String id,
            ArenaRegionType type,
            ArenaBlockPosition first,
            ArenaBlockPosition second
    ) {
        if (!first.dimension.equals(second.dimension)) {
            throw new IllegalArgumentException(
                    "Les deux positions doivent être "
                            + "dans la même dimension."
            );
        }

        this.id = ArenaManager.normalizeId(id);
        this.type = type.id();
        this.dimension = first.dimension;

        this.minX = Math.min(first.x, second.x);
        this.minY = Math.min(first.y, second.y);
        this.minZ = Math.min(first.z, second.z);

        this.maxX = Math.max(first.x, second.x);
        this.maxY = Math.max(first.y, second.y);
        this.maxZ = Math.max(first.z, second.z);
    }

    public ArenaRegionType resolvedType() {
        return ArenaRegionType.fromInput(type)
                .orElse(null);
    }

    public boolean contains(
            String testedDimension,
            BlockPos position
    ) {
        if (!dimension.equals(testedDimension)) {
            return false;
        }

        return position.getX() >= minX
                && position.getX() <= maxX
                && position.getY() >= minY
                && position.getY() <= maxY
                && position.getZ() >= minZ
                && position.getZ() <= maxZ;
    }

    public long volume() {
        long sizeX = (long) maxX - minX + 1L;
        long sizeY = (long) maxY - minY + 1L;
        long sizeZ = (long) maxZ - minZ + 1L;

        try {
            return Math.multiplyExact(
                    Math.multiplyExact(sizeX, sizeY),
                    sizeZ
            );
        } catch (ArithmeticException exception) {
            return Long.MAX_VALUE;
        }
    }

    public BlockPos center() {
        return new BlockPos(
                minX + (maxX - minX) / 2,
                minY + (maxY - minY) / 2,
                minZ + (maxZ - minZ) / 2
        );
    }

    public String formattedBounds() {
        return "["
                + minX + ", " + minY + ", " + minZ
                + "] → ["
                + maxX + ", " + maxY + ", " + maxZ
                + "] (" + dimension + ")";
    }
}