package fr.olympicraft.arena;

import net.minecraft.server.level.ServerPlayer;

public final class ArenaRegionBounds {

    private ArenaRegionBounds() {
    }

    /**
     * Vérifie si la position précise d'un joueur se trouve
     * à l'intérieur d'une région.
     *
     * Cette vérification utilise des coordonnées décimales pour
     * éviter les imprécisions provoquées par BlockPos.
     */
    public static boolean contains(
            ArenaRegion region,
            ServerPlayer player
    ) {
        if (region == null || player == null) {
            return false;
        }

        if (!sameDimension(region, player)) {
            return false;
        }

        /*
         * Une sélection de blocs inclut entièrement les blocs min et max.
         *
         * Par exemple, un bloc situé en X = 5 occupe l'espace
         * allant de 5 inclus à 6 exclu.
         *
         * Nous ajoutons donc 1 aux limites maximales.
         */
        double minimumX = region.minX;
        double maximumX = region.maxX + 1.0D;

        double minimumY = region.minY;
        double maximumY = region.maxY + 1.0D;

        double minimumZ = region.minZ;
        double maximumZ = region.maxZ + 1.0D;

        return player.getX() >= minimumX
                && player.getX() < maximumX
                && player.getY() >= minimumY
                && player.getY() < maximumY
                && player.getZ() >= minimumZ
                && player.getZ() < maximumZ;
    }

    /**
     * Vérifie si un point précis appartient à la région.
     */
    public static boolean contains(
            ArenaRegion region,
            String dimension,
            double x,
            double y,
            double z
    ) {
        if (region == null
                || dimension == null
                || dimension.isBlank()) {
            return false;
        }

        if (region.dimension == null
                || !region.dimension.equalsIgnoreCase(
                dimension
        )) {
            return false;
        }

        return x >= region.minX
                && x < region.maxX + 1.0D
                && y >= region.minY
                && y < region.maxY + 1.0D
                && z >= region.minZ
                && z < region.maxZ + 1.0D;
    }

    /**
     * Vérifie seulement les coordonnées horizontales X/Z.
     *
     * Cette méthode sera utile pour certains mini-jeux, par exemple
     * pour détecter qu'un joueur est sorti latéralement d'une arène
     * sans tenir compte de sa hauteur.
     */
    public static boolean containsHorizontal(
            ArenaRegion region,
            ServerPlayer player
    ) {
        if (region == null || player == null) {
            return false;
        }

        if (!sameDimension(region, player)) {
            return false;
        }

        return player.getX() >= region.minX
                && player.getX() < region.maxX + 1.0D
                && player.getZ() >= region.minZ
                && player.getZ() < region.maxZ + 1.0D;
    }

    private static boolean sameDimension(
            ArenaRegion region,
            ServerPlayer player
    ) {
        if (region.dimension == null
                || region.dimension.isBlank()) {
            return false;
        }

        String playerDimension =
                player.serverLevel()
                        .dimension()
                        .location()
                        .toString();

        return region.dimension.equalsIgnoreCase(
                playerDimension
        );
    }
}