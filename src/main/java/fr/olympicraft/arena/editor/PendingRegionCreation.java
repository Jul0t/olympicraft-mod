package fr.olympicraft.arena.editor;

import fr.olympicraft.arena.ArenaSelectionManager;

import java.time.Instant;
import java.util.UUID;

public record PendingRegionCreation(
        UUID playerId,
        ArenaSelectionManager.Selection selection,
        Instant createdAt
) {

    public PendingRegionCreation {
        if (playerId == null) {
            throw new IllegalArgumentException(
                    "Le joueur ne peut pas être nul."
            );
        }

        if (selection == null
                || !selection.complete()) {
            throw new IllegalArgumentException(
                    "La sélection doit être complète."
            );
        }

        if (createdAt == null) {
            createdAt = Instant.now();
        }

        selection = selection.normalized();
    }
}