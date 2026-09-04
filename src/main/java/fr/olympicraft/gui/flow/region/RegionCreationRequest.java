package fr.olympicraft.gui.flow.region;

import fr.olympicraft.arena.ArenaRegionType;
import fr.olympicraft.arena.ArenaSelectionManager;

import java.util.UUID;

public record RegionCreationRequest(
        UUID playerId,
        String arenaId,
        ArenaRegionType type,
        String requestedName,
        ArenaSelectionManager.Selection selection
) {

    public RegionCreationRequest {
        if (playerId == null) {
            throw new IllegalArgumentException(
                    "Le joueur est requis."
            );
        }

        if (arenaId == null || arenaId.isBlank()) {
            throw new IllegalArgumentException(
                    "L'arène est requise."
            );
        }

        if (type == null) {
            throw new IllegalArgumentException(
                    "Le type est requis."
            );
        }

        requestedName =
                requestedName == null
                        ? ""
                        : requestedName.trim();

        if (selection == null
                || !selection.complete()) {
            throw new IllegalArgumentException(
                    "La sélection doit être complète."
            );
        }

        selection = selection.normalized();
    }
}