package fr.olympicraft.gui.flow.region;

import fr.olympicraft.arena.ArenaRegion;

public record RegionCreationResult(
        boolean successful,
        ArenaRegion region,
        String error
) {

    public static RegionCreationResult success(
            ArenaRegion region
    ) {
        return new RegionCreationResult(
                true,
                region,
                ""
        );
    }

    public static RegionCreationResult failure(
            String error
    ) {
        return new RegionCreationResult(
                false,
                null,
                error
        );
    }
}