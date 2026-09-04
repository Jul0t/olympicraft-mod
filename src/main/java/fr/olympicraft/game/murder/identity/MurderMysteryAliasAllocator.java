package fr.olympicraft.game.murder.identity;

import fr.olympicraft.game.murder.MurderMysteryParticipant;
import fr.olympicraft.game.murder.MurderMysterySettings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public final class MurderMysteryAliasAllocator {

    private final Random random =
            new Random();

    public void allocate(
            List<MurderMysteryParticipant> participants,
            MurderMysterySettings settings
    ) {
        if (participants == null
                || settings == null) {
            return;
        }

        List<String> availableAliases =
                new ArrayList<>(
                        settings.config()
                                .anonymity
                                .aliases
                );

        Collections.shuffle(
                availableAliases,
                random
        );

        for (int index = 0;
             index < participants.size();
             index++) {
            MurderMysteryParticipant participant =
                    participants.get(index);

            String alias;

            if (index < availableAliases.size()) {
                alias =
                        availableAliases.get(index);
            } else {
                alias =
                        "Personne "
                                + (index + 1);
            }

            participant.alias(alias);
        }
    }
}