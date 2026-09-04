package fr.olympicraft.game.sumo.kit;

import fr.olympicraft.config.model.game.SumoConfig;
import fr.olympicraft.game.sumo.SumoSettings;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public final class SumoKitSelectionSession {

    private final SumoSettings settings;

    private final SumoKitSelectionMode mode;

    private final Map<UUID, String> choices =
            new LinkedHashMap<>();

    private final Random random =
            new Random();

    private boolean resolved;
    private boolean cancelled;

    private String commonPresetId;

    public SumoKitSelectionSession(
            SumoSettings settings
    ) {
        this.settings = settings;

        this.mode =
                SumoKitSelectionMode.from(
                        settings.kitSelectionMode()
                );
    }

    public SumoKitSelectionMode mode() {
        return mode;
    }

    public boolean usesMenu() {
        return mode.usesMenu();
    }

    public boolean resolved() {
        return resolved;
    }

    public boolean cancelled() {
        return cancelled;
    }

    public String commonPresetId() {
        return commonPresetId;
    }

    public String choice(
            UUID playerId
    ) {
        return choices.get(playerId);
    }

    public Map<UUID, String> choices() {
        return Map.copyOf(choices);
    }

    public boolean choose(
            UUID playerId,
            String requestedPreset
    ) {
        if (resolved
                || playerId == null
                || requestedPreset == null) {
            return false;
        }

        SumoConfig.KitPreset preset =
                settings.findKitPreset(
                        requestedPreset
                );

        if (preset == null
                || !isAllowed(preset.id)) {
            return false;
        }

        String previous =
                choices.get(playerId);

        if (previous != null
                && !settings.kitSelection()
                .allowVoteChange) {
            return false;
        }

        choices.put(
                playerId,
                preset.id
        );

        return true;
    }

    public void removeChoice(
            UUID playerId
    ) {
        if (!resolved
                && playerId != null) {
            choices.remove(playerId);
        }
    }

    public long voteCount(
            String presetId
    ) {
        if (presetId == null) {
            return 0L;
        }

        return choices.values()
                .stream()
                .filter(value ->
                        value.equalsIgnoreCase(
                                presetId
                        )
                )
                .count();
    }

    public List<UUID> voters(
            String presetId
    ) {
        if (presetId == null) {
            return List.of();
        }

        return choices.entrySet()
                .stream()
                .filter(entry ->
                        entry.getValue()
                                .equalsIgnoreCase(
                                        presetId
                                )
                )
                .map(Map.Entry::getKey)
                .toList();
    }

    public ResolveResult resolve(
            Collection<UUID> eligiblePlayers
    ) {
        if (resolved) {
            if (cancelled) {
                return ResolveResult.cancellation();
            }

            return ResolveResult.success(
                    commonPresetId
            );
        }

        removeIneligibleChoices(
                eligiblePlayers
        );

        return switch (mode) {
            case FIXED ->
                    resolveFixed();

            case RANDOM ->
                    resolveRandom();

            case VOTE ->
                    resolveVote();

            case PLAYER_CHOICE ->
                    resolvePlayerChoice(
                            eligiblePlayers
                    );
        };
    }

    public String presetFor(
            UUID playerId
    ) {
        if (!resolved || cancelled) {
            return null;
        }

        if (mode.commonPreset()) {
            return commonPresetId;
        }

        String selected =
                choices.get(playerId);

        if (selected != null
                && settings.findKitPreset(
                selected
        ) != null) {
            return selected;
        }

        SumoConfig.KitPreset fallback =
                settings.defaultKitPreset();

        return fallback == null
                ? null
                : fallback.id;
    }

    public String dummyPresetId() {
        if (!resolved || cancelled) {
            return null;
        }

        if (mode.commonPreset()) {
            return commonPresetId;
        }

        String dummyMode =
                settings.kitSelection()
                        .dummyPresetMode;

        if ("RANDOM".equalsIgnoreCase(
                dummyMode
        )) {
            return randomAllowedPreset();
        }

        if ("MOST_SELECTED".equalsIgnoreCase(
                dummyMode
        )) {
            List<String> leaders =
                    leadingPresetIds();

            if (!leaders.isEmpty()) {
                return leaders.get(
                        random.nextInt(
                                leaders.size()
                        )
                );
            }
        }

        SumoConfig.KitPreset fallback =
                settings.defaultKitPreset();

        return fallback == null
                ? null
                : fallback.id;
    }

    private ResolveResult resolveFixed() {
        SumoConfig.KitPreset preset =
                settings.fixedKitPreset();

        if (preset == null) {
            return cancel();
        }

        commonPresetId = preset.id;
        resolved = true;

        return ResolveResult.success(
                commonPresetId
        );
    }

    private ResolveResult resolveRandom() {
        String selected =
                randomAllowedPreset();

        if (selected == null) {
            return cancel();
        }

        commonPresetId = selected;
        resolved = true;

        return ResolveResult.success(
                commonPresetId
        );
    }

    private ResolveResult resolveVote() {
        if (choices.isEmpty()) {
            return resolveSpecialCase(
                    SumoKitResolution.from(
                            settings.kitSelection()
                                    .noVoteResolution
                    ),
                    allowedPresetIds()
            );
        }

        List<String> leaders =
                leadingPresetIds();

        if (leaders.isEmpty()) {
            return cancel();
        }

        if (leaders.size() == 1) {
            commonPresetId =
                    leaders.getFirst();

            resolved = true;

            return ResolveResult.success(
                    commonPresetId
            );
        }

        return resolveSpecialCase(
                SumoKitResolution.from(
                        settings.kitSelection()
                                .tieResolution
                ),
                leaders
        );
    }

    private ResolveResult resolvePlayerChoice(
            Collection<UUID> eligiblePlayers
    ) {
        SumoConfig.KitPreset fallback =
                settings.defaultKitPreset();

        if (fallback == null) {
            return cancel();
        }

        if (eligiblePlayers != null) {
            for (UUID playerId :
                    eligiblePlayers) {
                if (playerId == null) {
                    continue;
                }

                choices.putIfAbsent(
                        playerId,
                        fallback.id
                );
            }
        }

        resolved = true;

        return ResolveResult.success(null);
    }

    private ResolveResult resolveSpecialCase(
            SumoKitResolution resolution,
            List<String> candidates
    ) {
        if (resolution
                == SumoKitResolution.CANCEL) {
            return cancel();
        }

        if (resolution
                == SumoKitResolution.DEFAULT) {
            SumoConfig.KitPreset preset =
                    settings.defaultKitPreset();

            if (preset == null) {
                return cancel();
            }

            commonPresetId = preset.id;
        } else {
            if (candidates == null
                    || candidates.isEmpty()) {
                return cancel();
            }

            commonPresetId =
                    candidates.get(
                            random.nextInt(
                                    candidates.size()
                            )
                    );
        }

        resolved = true;

        return ResolveResult.success(
                commonPresetId
        );
    }

    private ResolveResult cancel() {
        cancelled = true;
        resolved = true;
        commonPresetId = null;

        return ResolveResult.cancellation();
    }

    private List<String> leadingPresetIds() {
        List<String> allowed =
                allowedPresetIds();

        long bestAmount = -1L;

        List<String> leaders =
                new ArrayList<>();

        for (String presetId : allowed) {
            long amount =
                    voteCount(presetId);

            if (amount > bestAmount) {
                leaders.clear();
                leaders.add(presetId);
                bestAmount = amount;
            } else if (amount == bestAmount) {
                leaders.add(presetId);
            }
        }

        return leaders;
    }

    private String randomAllowedPreset() {
        List<String> allowed =
                allowedPresetIds();

        if (allowed.isEmpty()) {
            return null;
        }

        return allowed.get(
                random.nextInt(
                        allowed.size()
                )
        );
    }

    private List<String> allowedPresetIds() {
        List<String> configured =
                settings.kitSelection()
                        .allowedPresets;

        if (configured == null
                || configured.isEmpty()) {
            return List.of();
        }

        return settings.enabledKitPresets()
                .stream()
                .filter(preset ->
                        configured.stream()
                                .anyMatch(value ->
                                        value != null
                                                && value.equalsIgnoreCase(
                                                preset.id
                                        )
                                )
                )
                .map(preset -> preset.id)
                .distinct()
                .sorted(
                        Comparator.naturalOrder()
                )
                .toList();
    }

    private boolean isAllowed(
            String presetId
    ) {
        return allowedPresetIds()
                .stream()
                .anyMatch(value ->
                        value.equalsIgnoreCase(
                                presetId
                        )
                );
    }

    private void removeIneligibleChoices(
            Collection<UUID> eligiblePlayers
    ) {
        if (eligiblePlayers == null) {
            choices.clear();
            return;
        }

        choices.keySet()
                .removeIf(playerId ->
                        !eligiblePlayers.contains(
                                playerId
                        )
                );
    }

    public record ResolveResult(
            boolean successful,
            boolean cancelled,
            String commonPresetId
    ) {

        public static ResolveResult success(
                String commonPresetId
        ) {
            return new ResolveResult(
                    true,
                    false,
                    commonPresetId
            );
        }

        public static ResolveResult cancellation() {
            return new ResolveResult(
                    false,
                    true,
                    null
            );
        }
    }
}