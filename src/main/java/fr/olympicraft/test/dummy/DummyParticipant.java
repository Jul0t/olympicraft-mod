package fr.olympicraft.test.dummy;

import java.util.UUID;

public final class DummyParticipant {

    private final UUID participantId;
    private final UUID entityId;
    private final String arenaId;
    private final String name;

    public DummyParticipant(
            UUID participantId,
            UUID entityId,
            String arenaId,
            String name
    ) {
        if (participantId == null) {
            throw new IllegalArgumentException(
                    "L'identifiant logique du dummy "
                            + "ne peut pas être nul."
            );
        }

        if (entityId == null) {
            throw new IllegalArgumentException(
                    "L'identifiant de l'entité du dummy "
                            + "ne peut pas être nul."
            );
        }

        this.participantId = participantId;
        this.entityId = entityId;
        this.arenaId = arenaId == null
                ? ""
                : arenaId;

        this.name = name == null
                || name.isBlank()
                ? "Dummy"
                : name;
    }

    public UUID participantId() {
        return participantId;
    }

    public UUID entityId() {
        return entityId;
    }

    public String arenaId() {
        return arenaId;
    }

    public String name() {
        return name;
    }
}