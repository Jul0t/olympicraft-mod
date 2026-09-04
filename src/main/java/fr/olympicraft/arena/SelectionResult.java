package fr.olympicraft.arena;

public record SelectionResult(
        boolean successful,
        ArenaSelectionManager.Selection selection,
        Source source,
        String error
) {

    public enum Source {
        OLYMPICRAFT,
        WORLD_EDIT,
        NONE
    }

    public static SelectionResult success(
            ArenaSelectionManager.Selection selection,
            Source source
    ) {
        return new SelectionResult(
                true,
                selection,
                source,
                ""
        );
    }

    public static SelectionResult failure(String error) {
        return new SelectionResult(
                false,
                null,
                Source.NONE,
                error
        );
    }

    public String sourceDisplayName() {
        return switch (source) {
            case OLYMPICRAFT -> "Olympicraft";
            case WORLD_EDIT -> "WorldEdit";
            case NONE -> "aucune";
        };
    }
}
