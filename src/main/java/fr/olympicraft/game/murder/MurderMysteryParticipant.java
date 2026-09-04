package fr.olympicraft.game.murder;

import fr.olympicraft.game.murder.role.MurderMysteryRole;

import java.util.Objects;
import java.util.UUID;

public final class MurderMysteryParticipant {

    private final UUID participantId;

    private final boolean dummy;

    private final String originalName;

    private MurderMysteryRole role;

    private String alias;

    private boolean alive = true;

    private int wounds;

    private boolean poisoned;

    private boolean disconnected;

    private boolean killedInnocent;

    private boolean killedDetective;

    private boolean killedMurderer;

    public MurderMysteryParticipant(
            UUID participantId,
            boolean dummy,
            String originalName
    ) {
        this.participantId =
                Objects.requireNonNull(
                        participantId,
                        "participantId"
                );

        this.dummy = dummy;

        this.originalName =
                originalName == null
                        || originalName.isBlank()
                        ? "Inconnu"
                        : originalName;

        this.role =
                MurderMysteryRole.INNOCENT;

        this.alias =
                this.originalName;
    }

    public UUID participantId() {
        return participantId;
    }

    public boolean dummy() {
        return dummy;
    }

    public String originalName() {
        return originalName;
    }

    public MurderMysteryRole role() {
        return role;
    }

    public void role(
            MurderMysteryRole role
    ) {
        this.role = role == null
                ? MurderMysteryRole.INNOCENT
                : role;
    }

    public String alias() {
        return alias;
    }

    public void alias(
            String alias
    ) {
        if (alias != null
                && !alias.isBlank()) {
            this.alias = alias.trim();
        }
    }

    public boolean alive() {
        return alive;
    }

    public void eliminate() {
        alive = false;
    }

    public int wounds() {
        return wounds;
    }

    public void wounds(
            int wounds
    ) {
        this.wounds =
                Math.max(
                        0,
                        wounds
                );
    }

    public void addWounds(
            int amount
    ) {
        wounds(
                wounds
                        + Math.max(
                        0,
                        amount
                )
        );
    }

    public boolean poisoned() {
        return poisoned;
    }

    public void poisoned(
            boolean poisoned
    ) {
        this.poisoned = poisoned;
    }

    public boolean disconnected() {
        return disconnected;
    }

    public void disconnected(
            boolean disconnected
    ) {
        this.disconnected = disconnected;
    }

    public boolean killedInnocent() {
        return killedInnocent;
    }

    public boolean killedDetective() {
        return killedDetective;
    }

    public boolean killedMurderer() {
        return killedMurderer;
    }

    public void recordKill(
            MurderMysteryRole victimRole
    ) {
        if (victimRole == null) {
            return;
        }

        switch (victimRole) {
            case INNOCENT ->
                    killedInnocent = true;

            case DETECTIVE ->
                    killedDetective = true;

            case MURDERER ->
                    killedMurderer = true;

            case TROUBLEMAKER -> {
            }
        }
    }

    public boolean completedTroublemakerObjectives() {
        return killedInnocent
                && killedDetective
                && killedMurderer;
    }
}