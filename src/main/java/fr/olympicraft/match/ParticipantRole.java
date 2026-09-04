package fr.olympicraft.match;

public enum ParticipantRole {

    PLAYER,
    DUMMY,
    SPECTATOR;

    public boolean competitor() {
        return this == PLAYER
                || this == DUMMY;
    }

    public boolean spectator() {
        return this == SPECTATOR;
    }

    public boolean dummy() {
        return this == DUMMY;
    }
}