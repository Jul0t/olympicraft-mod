package fr.olympicraft.game;

import fr.olympicraft.arena.ArenaDefinition;
import fr.olympicraft.arena.ArenaRegion;
import fr.olympicraft.arena.ArenaRegionType;
import fr.olympicraft.arena.ArenaValidationResult;
import fr.olympicraft.arena.RegionRequirement;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

public final class GameDefinition {

    private final String id;
    private final GamePresentation presentation;

    private final int minimumPlayers;
    private final int maximumPlayers;

    private final Map<ArenaRegionType, RegionRequirement>
            regionRequirements;

    private final Consumer<ArenaValidationContext>
            arenaValidator;

    public GameDefinition(
            String id,
            GamePresentation presentation,
            int minimumPlayers,
            int maximumPlayers,
            Map<ArenaRegionType, RegionRequirement>
                    regionRequirements,
            Consumer<ArenaValidationContext> arenaValidator
    ) {
        this.id = GameRegistry.normalizeId(id);

        this.presentation = Objects.requireNonNull(
                presentation,
                "presentation"
        );

        if (this.id.isBlank()) {
            throw new IllegalArgumentException(
                    "L'identifiant d'un jeu ne peut pas être vide."
            );
        }

        if (minimumPlayers < 1) {
            throw new IllegalArgumentException(
                    "Le minimum de joueurs doit être supérieur à zéro."
            );
        }

        if (maximumPlayers < minimumPlayers) {
            throw new IllegalArgumentException(
                    "Le maximum doit être supérieur ou égal au minimum."
            );
        }

        this.minimumPlayers = minimumPlayers;
        this.maximumPlayers = maximumPlayers;

        EnumMap<ArenaRegionType, RegionRequirement> requirements =
                new EnumMap<>(ArenaRegionType.class);

        if (regionRequirements != null) {
            requirements.putAll(regionRequirements);
        }

        this.regionRequirements =
                Collections.unmodifiableMap(requirements);

        this.arenaValidator = arenaValidator == null
                ? context -> {
        }
                : arenaValidator;
    }

    public String id() {
        return id;
    }

    public GamePresentation presentation() {
        return presentation;
    }

    public String displayName() {
        return presentation.displayName();
    }

    public int minimumPlayers() {
        return minimumPlayers;
    }

    public int maximumPlayers() {
        return maximumPlayers;
    }

    public Map<ArenaRegionType, RegionRequirement>
    regionRequirements() {
        return regionRequirements;
    }

    public Set<ArenaRegionType> allowedRegionTypes() {
        return regionRequirements.keySet();
    }

    public boolean allowsRegionType(
            ArenaRegionType type
    ) {
        return type != null
                && regionRequirements.containsKey(type);
    }

    public RegionRequirement regionRequirement(
            ArenaRegionType type
    ) {
        return regionRequirements.get(type);
    }

    public long regionCount(
            ArenaDefinition arena,
            ArenaRegionType type
    ) {
        if (arena == null
                || arena.regions == null
                || type == null) {
            return 0L;
        }

        return arena.regions.values()
                .stream()
                .filter(Objects::nonNull)
                .filter(region ->
                        type.id().equals(region.type)
                )
                .count();
    }

    public boolean canAddRegion(
            ArenaDefinition arena,
            ArenaRegionType type
    ) {
        RegionRequirement requirement =
                regionRequirement(type);

        if (requirement == null) {
            return false;
        }

        long count = regionCount(arena, type);

        if (count > Integer.MAX_VALUE) {
            return false;
        }

        return requirement.canAdd((int) count);
    }

    public ArenaValidationResult validateArena(
            ArenaDefinition arena
    ) {
        Objects.requireNonNull(arena, "arena");

        ArenaValidationResult result =
                arena.validateBase();

        validateRegionRequirements(
                arena,
                result
        );

        validateStoredRegionTypes(
                arena,
                result
        );

        arenaValidator.accept(
                new ArenaValidationContext(
                        arena,
                        result
                )
        );

        return result;
    }

    private void validateRegionRequirements(
            ArenaDefinition arena,
            ArenaValidationResult result
    ) {
        for (Map.Entry<
                ArenaRegionType,
                RegionRequirement
                > entry : regionRequirements.entrySet()) {

            ArenaRegionType type = entry.getKey();
            RegionRequirement requirement = entry.getValue();

            long amount = regionCount(arena, type);

            if (amount < requirement.minimum()) {
                result.error(
                        "La région '" + type.id()
                                + "' est requise au moins "
                                + requirement.minimum()
                                + " fois, actuellement "
                                + amount
                                + "."
                );
            }

            if (!requirement.unlimited()
                    && amount > requirement.maximum()) {
                result.error(
                        "La région '" + type.id()
                                + "' accepte au maximum "
                                + requirement.maximum()
                                + " occurrence(s), actuellement "
                                + amount
                                + "."
                );
            }
        }
    }

    private void validateStoredRegionTypes(
            ArenaDefinition arena,
            ArenaValidationResult result
    ) {
        if (arena.regions == null) {
            return;
        }

        for (ArenaRegion region :
                arena.regions.values()) {
            if (region == null) {
                result.error(
                        "L'arène contient une région nulle."
                );
                continue;
            }

            ArenaRegionType type =
                    region.resolvedType();

            if (type == null) {
                result.error(
                        "La région '" + region.id
                                + "' possède un type inconnu : "
                                + region.type
                );
                continue;
            }

            if (!allowsRegionType(type)) {
                result.error(
                        "Le type de région '"
                                + type.id()
                                + "' n'est pas autorisé "
                                + "pour ce jeu."
                );
            }
        }
    }

    public record ArenaValidationContext(
            ArenaDefinition arena,
            ArenaValidationResult result
    ) {

        public ArenaValidationContext {
            Objects.requireNonNull(arena, "arena");
            Objects.requireNonNull(result, "result");
        }

        public void requireLobby() {
            if (arena.lobby == null) {
                result.error(
                        "Le point de lobby n'est pas défini."
                );
            }
        }

        public void requireSpectator() {
            if (arena.spectator == null) {
                result.error(
                        "Le point spectateur n'est pas défini."
                );
            }
        }

        public void requireSpawnGroup(
                String requestedGroup,
                int minimum
        ) {
            String group = requestedGroup == null
                    ? ""
                    : requestedGroup.trim();

            int safeMinimum = Math.max(1, minimum);
            int amount = arena.spawnGroup(group).size();

            if (amount < safeMinimum) {
                result.error(
                        "Le groupe de spawns '"
                                + group
                                + "' nécessite au moins "
                                + safeMinimum
                                + " position(s), actuellement "
                                + amount
                                + "."
                );
            }
        }

        public void requireTotalSpawns(
                int minimum
        ) {
            int safeMinimum = Math.max(1, minimum);
            int amount = arena.totalSpawnCount();

            if (amount < safeMinimum) {
                result.error(
                        "L'arène nécessite au moins "
                                + safeMinimum
                                + " spawn(s), actuellement "
                                + amount
                                + "."
                );
            }
        }

        public void warning(String message) {
            result.warning(message);
        }

        public void error(String message) {
            result.error(message);
        }
    }
}