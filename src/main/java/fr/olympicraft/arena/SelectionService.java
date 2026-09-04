package fr.olympicraft.arena;

import fr.olympicraft.integration.worldedit
        .WorldEditSelectionProvider;
import net.minecraft.server.level.ServerPlayer;

public final class SelectionService {

    private final ArenaSelectionManager nativeSelections;
    private final WorldEditSelectionProvider worldEdit;

    public SelectionService(
            ArenaSelectionManager nativeSelections,
            WorldEditSelectionProvider worldEdit
    ) {
        this.nativeSelections = nativeSelections;
        this.worldEdit = worldEdit;
    }

    public SelectionResult resolve(
            ServerPlayer player
    ) {
        ArenaSelectionManager.Selection nativeSelection =
                nativeSelections.get(player).orElse(null);

        /*
         * La sélection Olympicraft est prioritaire.
         */
        if (nativeSelection != null
                && nativeSelection.complete()) {
            return SelectionResult.success(
                    nativeSelection.normalized(),
                    SelectionResult.Source.OLYMPICRAFT
            );
        }

        /*
         * Si aucune sélection native complète n'existe,
         * on essaie WorldEdit.
         */
        SelectionResult worldEditResult =
                worldEdit.selection(player);

        if (worldEditResult.successful()) {
            return SelectionResult.success(
                    worldEditResult.selection().normalized(),
                    SelectionResult.Source.WORLD_EDIT
            );
        }

        String nativeState;

        if (nativeSelection == null) {
            nativeState =
                    "aucune sélection Olympicraft";
        } else if (
                nativeSelection.hasDifferentDimensions()
        ) {
            nativeState =
                    "sélection Olympicraft dans plusieurs dimensions";
        } else {
            nativeState =
                    "sélection Olympicraft incomplète";
        }

        return SelectionResult.failure(
                "Aucune sélection utilisable : "
                        + nativeState
                        + " ; "
                        + worldEditResult.error()
        );
    }

    public SelectionResult importWorldEdit(
            ServerPlayer player
    ) {
        SelectionResult result =
                worldEdit.selection(player);

        if (!result.successful()) {
            return result;
        }

        ArenaSelectionManager.Selection selection =
                result.selection().normalized();

        nativeSelections.setFirst(
                player,
                selection.first()
        );

        nativeSelections.setSecond(
                player,
                selection.second()
        );

        return SelectionResult.success(
                selection,
                SelectionResult.Source.WORLD_EDIT
        );
    }

    public boolean worldEditAvailable() {
        return worldEdit.available();
    }

    public ArenaSelectionManager nativeSelections() {
        return nativeSelections;
    }
}