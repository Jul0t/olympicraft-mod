package fr.olympicraft.match;

import java.util.UUID;

public final class GameParticipant {

    private final UUID playerId;
    private final String displayName;

    private ParticipantRole role;

    public GameParticipant(
            UUID playerId,
            ParticipantRole role
    ) {
        this(
                playerId,
                role,
                playerId == null
                        ? "Inconnu"
                        : playerId.toString()
        );
    }

    public GameParticipant(
            UUID playerId,
            ParticipantRole role,
            String displayName
    ) {
        if (playerId == null) {
            throw new IllegalArgumentException(
                    "L'identifiant du participant "
                            + "ne peut pas être nul."
            );
        }

        this.playerId = playerId;
        this.role = role == null
                ? ParticipantRole.PLAYER
                : role;

        this.displayName =
                displayName == null
                        || displayName.isBlank()
                        ? playerId.toString()
                        : displayName;
    }

    public UUID playerId() {
        return playerId;
    }

    public ParticipantRole role() {
        return role;
    }

    public void setRole(ParticipantRole role) {
        if (role != null) {
            this.role = role;
        }
    }

    public String displayName() {
        return displayName;
    }

    public boolean spectator() {
        return role.spectator();
    }

    public boolean dummy() {
        return role.dummy();
    }

    public boolean competitor() {
        return role.competitor();
    }
}