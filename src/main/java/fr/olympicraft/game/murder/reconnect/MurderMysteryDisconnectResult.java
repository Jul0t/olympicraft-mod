package fr.olympicraft.game.murder.reconnect;

import fr.olympicraft.game.murder.role.MurderMysteryRole;

import java.util.UUID;

public record MurderMysteryDisconnectResult(
        boolean handled,
        boolean forfeited,
        boolean endedMatch,
        UUID playerId,
        MurderMysteryRole role,
        String reason,
        float rankedMultiplier
) {

    public static MurderMysteryDisconnectResult ignored() {
        return new MurderMysteryDisconnectResult(
                false,
                false,
                false,
                null,
                null,
                null,
                1.0F
        );
    }

    public static MurderMysteryDisconnectResult waiting(
            UUID playerId,
            MurderMysteryRole role
    ) {
        return new MurderMysteryDisconnectResult(
                true,
                false,
                false,
                playerId,
                role,
                null,
                1.0F
        );
    }

    public static MurderMysteryDisconnectResult forfeited(
            UUID playerId,
            MurderMysteryRole role,
            boolean endedMatch,
            String reason,
            float rankedMultiplier
    ) {
        return new MurderMysteryDisconnectResult(
                true,
                true,
                endedMatch,
                playerId,
                role,
                reason,
                rankedMultiplier
        );
    }
}