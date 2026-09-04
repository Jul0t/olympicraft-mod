package fr.olympicraft.arena;

import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ArenaSelectionManager {

    private final Map<UUID, Selection> selections =
            new ConcurrentHashMap<>();

    public void setFirst(
            ServerPlayer player,
            ArenaBlockPosition position
    ) {
        selections.compute(
                player.getUUID(),
                (uuid, current) -> new Selection(
                        position,
                        current == null
                                ? null
                                : current.second()
                )
        );
    }

    public void setSecond(
            ServerPlayer player,
            ArenaBlockPosition position
    ) {
        selections.compute(
                player.getUUID(),
                (uuid, current) -> new Selection(
                        current == null
                                ? null
                                : current.first(),
                        position
                )
        );
    }

    public Optional<Selection> get(
            ServerPlayer player
    ) {
        return Optional.ofNullable(
                selections.get(player.getUUID())
        );
    }

    public boolean clear(
            ServerPlayer player
    ) {
        return selections.remove(
                player.getUUID()
        ) != null;
    }

    public void clearAll() {
        selections.clear();
    }

    public record Selection(
            ArenaBlockPosition first,
            ArenaBlockPosition second
    ) {

        public boolean complete() {
            return first != null
                    && second != null
                    && first.dimension.equals(
                    second.dimension
            );
        }

        public boolean hasDifferentDimensions() {
            return first != null
                    && second != null
                    && !first.dimension.equals(
                    second.dimension
            );
        }

        /**
         * Retourne une sélection dont le premier coin contient
         * les coordonnées minimales et le second les maximales.
         *
         * L'ordre dans lequel les positions ont été sélectionnées
         * n'a donc aucune influence.
         */
        public Selection normalized() {
            if (!complete()) {
                return this;
            }

            ArenaBlockPosition minimum =
                    new ArenaBlockPosition(
                            first.dimension,
                            Math.min(first.x, second.x),
                            Math.min(first.y, second.y),
                            Math.min(first.z, second.z)
                    );

            ArenaBlockPosition maximum =
                    new ArenaBlockPosition(
                            first.dimension,
                            Math.max(first.x, second.x),
                            Math.max(first.y, second.y),
                            Math.max(first.z, second.z)
                    );

            return new Selection(
                    minimum,
                    maximum
            );
        }

        public long estimatedVolume() {
            if (!complete()) {
                return 0L;
            }

            Selection normalized = normalized();

            return new ArenaRegion(
                    "selection",
                    ArenaRegionType.GAME_BOUNDS,
                    normalized.first(),
                    normalized.second()
            ).volume();
        }
    }
}