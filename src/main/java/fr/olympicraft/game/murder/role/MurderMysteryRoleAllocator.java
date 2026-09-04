package fr.olympicraft.game.murder.role;

import fr.olympicraft.game.murder.MurderMysteryParticipant;
import fr.olympicraft.game.murder.MurderMysterySettings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class MurderMysteryRoleAllocator {

    private final Random random =
            new Random();

    public AllocationResult allocate(
            List<MurderMysteryParticipant> participants,
            MurderMysterySettings settings
    ) {
        if (participants == null
                || settings == null) {
            return AllocationResult.failure(
                    "Les participants ou les réglages sont absents."
            );
        }

        List<MurderMysteryParticipant> available =
                new ArrayList<>(
                        participants
                );

        if (available.size()
                < settings.minimumPlayers()) {
            return AllocationResult.failure(
                    "Pas assez de participants."
            );
        }

        Collections.shuffle(
                available,
                random
        );

        for (MurderMysteryParticipant participant :
                available) {
            participant.role(
                    MurderMysteryRole.INNOCENT
            );
        }

        int specialRoleIndex = 0;

        int murdererAmount =
                Math.min(
                        settings.murdererAmount(),
                        Math.max(
                                1,
                                available.size() - 1
                        )
                );

        for (int index = 0;
             index < murdererAmount;
             index++) {
            available.get(
                    specialRoleIndex++
            ).role(
                    MurderMysteryRole.MURDERER
            );
        }

        int remainingAfterMurderers =
                available.size()
                        - specialRoleIndex;

        int detectiveAmount =
                Math.min(
                        settings.detectiveAmount(),
                        Math.max(
                                0,
                                remainingAfterMurderers - 1
                        )
                );

        for (int index = 0;
             index < detectiveAmount;
             index++) {
            available.get(
                    specialRoleIndex++
            ).role(
                    MurderMysteryRole.DETECTIVE
            );
        }

        boolean troublemakerPresent =
                shouldCreateTroublemaker(
                        available.size(),
                        settings
                );

        if (troublemakerPresent
                && specialRoleIndex
                < available.size()) {
            available.get(
                    specialRoleIndex
            ).role(
                    MurderMysteryRole.TROUBLEMAKER
            );
        } else {
            troublemakerPresent = false;
        }

        return AllocationResult.success(
                troublemakerPresent
        );
    }

    private boolean shouldCreateTroublemaker(
            int participantAmount,
            MurderMysterySettings settings
    ) {
        if (!settings.troublemakerEnabled()) {
            return false;
        }

        if (participantAmount
                < settings
                .troublemakerMinimumPlayers()) {
            return false;
        }

        int chance =
                Math.clamp(
                        settings
                                .troublemakerChancePercent(),
                        0,
                        100
                );

        return random.nextInt(100) < chance;
    }

    public record AllocationResult(
            boolean successful,
            String error,
            boolean troublemakerPresent
    ) {
        public static AllocationResult success(
                boolean troublemakerPresent
        ) {
            return new AllocationResult(
                    true,
                    null,
                    troublemakerPresent
            );
        }

        public static AllocationResult failure(
                String error
        ) {
            return new AllocationResult(
                    false,
                    error,
                    false
            );
        }
    }
}