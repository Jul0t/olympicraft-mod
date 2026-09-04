package fr.olympicraft.match;

public enum GameState {

    INACTIVE("Inactive"),
    WAITING("En attente"),
    STARTING("Démarrage"),
    RUNNING("En cours"),
    ENDING("Fin"),
    RESETTING("Réinitialisation");

    private final String displayName;

    GameState(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }

    public boolean acceptsPlayers() {
        return this == WAITING;
    }

    public boolean active() {
        return this != INACTIVE;
    }
}
