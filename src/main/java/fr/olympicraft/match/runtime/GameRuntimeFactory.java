package fr.olympicraft.match.runtime;

import fr.olympicraft.arena.ArenaDefinition;
import net.minecraft.server.MinecraftServer;

@FunctionalInterface
public interface GameRuntimeFactory {

    GameRuntime create(
            MinecraftServer server,
            ArenaDefinition arena
    );
}
