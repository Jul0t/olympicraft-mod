package fr.olympicraft.gui.flow.region;

import fr.olympicraft.arena.ArenaDefinition;
import fr.olympicraft.arena.ArenaManager;
import fr.olympicraft.arena.RegionRequirement;
import fr.olympicraft.game.GameDefinition;
import fr.olympicraft.game.GameRegistry;

public final class RegionCreationFlow {

    private final ArenaManager arenas;
    private final GameRegistry games;

    public RegionCreationFlow(
            ArenaManager arenas,
            GameRegistry games
    ) {
        this.arenas = arenas;
        this.games = games;
    }

    public RegionCreationResult create(
            RegionCreationRequest request
    ) {
        ArenaDefinition arena = arenas
                .find(request.arenaId())
                .orElse(null);

        if (arena == null) {
            return RegionCreationResult.failure(
                    "Cette arène n'existe plus."
            );
        }

        GameDefinition game = games
                .find(arena.gameType)
                .orElse(null);

        if (game == null) {
            return RegionCreationResult.failure(
                    "Le mini-jeu de cette arène "
                            + "n'est pas enregistré."
            );
        }

        if (!game.allowsRegionType(request.type())) {
            return RegionCreationResult.failure(
                    "Ce type de région n'est pas autorisé "
                            + "pour " + game.displayName() + "."
            );
        }

        if (!game.canAddRegion(
                arena,
                request.type()
        )) {
            return RegionCreationResult.failure(
                    "Le nombre maximal de régions de ce type "
                            + "est déjà atteint."
            );
        }

        RegionRequirement requirement =
                game.regionRequirement(request.type());

        String regionId = request.requestedName();

        if (regionId == null || regionId.isBlank()) {
            regionId = arenas.nextRegionId(
                    arena,
                    request.type(),
                    requirement
            );
        }

        ArenaManager.RegionResult result =
                arenas.addRegion(
                        arena,
                        regionId,
                        request.type(),
                        request.selection()
                );

        if (!result.successful()) {
            return RegionCreationResult.failure(
                    result.error()
            );
        }

        return RegionCreationResult.success(
                result.region()
        );
    }
}