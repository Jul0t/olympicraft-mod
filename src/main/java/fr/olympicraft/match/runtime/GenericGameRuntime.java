package fr.olympicraft.match.runtime;

import fr.olympicraft.match.GameInstance;

public final class GenericGameRuntime
        implements GameRuntime {

    @Override
    public boolean canStart(
            GameInstance instance
    ) {
        return instance.playerCount() > 0;
    }
}