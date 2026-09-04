package fr.olympicraft.arena;

import fr.olympicraft.internal.BuildDefaults;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.List;

public final class ArenaDefinition {

    public int schemaVersion =
            BuildDefaults.SAVE_SCHEMA_VERSION;

    public String id = "";

    public String displayName = "";

    public String gameType = "";

    public boolean enabled;

    public String worldId = "";

    public String creatorUuid = "";

    public String createdAt = Instant.now().toString();

    public String updatedAt = Instant.now().toString();

    public ArenaPosition lobby;

    public ArenaPosition spectator;

    public Map<String, List<ArenaPosition>> spawns =
            new LinkedHashMap<>();

    public Map<String, String> properties =
            new LinkedHashMap<>();

    public ArenaDefinition() {
    }

    public ArenaDefinition(
            String id,
            String displayName,
            String gameTypeId,
            UUID creatorUuid,
            String worldId
    ) {
        this.id = id;
        this.displayName = displayName;
        this.gameType = gameTypeId;
        this.creatorUuid = creatorUuid.toString();
        this.worldId = worldId;
    }

    public void touch() {
        updatedAt = Instant.now().toString();
    }

    public ArenaValidationResult validateBase() {
        ArenaValidationResult result =
                new ArenaValidationResult();

        if (id == null || id.isBlank()) {
            result.error("L'identifiant est vide.");
        }

        if (displayName == null || displayName.isBlank()) {
            result.error("Le nom d'affichage est vide.");
        }

        if (gameType == null || gameType.isBlank()) {
            result.error(
                    "L'identifiant du mini-jeu est vide."
            );
        }

        if (worldId == null || worldId.isBlank()) {
            result.error(
                    "Le monde de l'arène est inconnu."
            );
        }

        if (lobby == null) {
            result.warning(
                    "Le point de lobby n'est pas encore défini."
            );
        }

        if (spectator == null) {
            result.warning(
                    "Le point spectateur n'est pas encore défini."
            );
        }

        return result;
    }

    public List<ArenaPosition> spawnGroup(String group) {
        return spawns.getOrDefault(group, List.of());
    }

    public int totalSpawnCount() {
        return spawns.values()
                .stream()
                .mapToInt(List::size)
                .sum();
    }
    public Map<String, ArenaRegion> regions =
            new LinkedHashMap<>();

    public ArenaRegion region(String regionId) {
        return regions.get(
                ArenaManager.normalizeId(regionId)
        );
    }

    public long totalRegionVolume() {
        return regions.values()
                .stream()
                .mapToLong(ArenaRegion::volume)
                .sum();
    }


}
