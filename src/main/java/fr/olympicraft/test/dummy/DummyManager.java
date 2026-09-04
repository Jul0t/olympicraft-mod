package fr.olympicraft.test.dummy;

import fr.olympicraft.Olympicraft;
import fr.olympicraft.arena.ArenaDefinition;
import fr.olympicraft.arena.ArenaManager;
import fr.olympicraft.arena.ArenaPosition;
import fr.olympicraft.match.GameInstance;
import fr.olympicraft.test.TestModeManager;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class DummyManager {

    private final TestModeManager testMode;

    /*
     * Clé : identifiant normalisé de l'arène.
     * Valeur : dummies indexés par leur identifiant logique.
     */
    private final Map<String, Map<UUID, DummyParticipant>>
            dummiesByArena = new LinkedHashMap<>();

    private MinecraftServer server;

    public DummyManager(
            TestModeManager testMode
    ) {
        this.testMode = testMode;
    }

    public synchronized void attachServer(
            MinecraftServer server
    ) {
        this.server = server;

        /*
         * Des mannequins restés dans un monde après un arrêt
         * anormal ne sont pas des participants valides.
         */
        removeOrphanEntities();
    }

    public synchronized void detachServer() {
        clearAll();
        this.server = null;
    }

    public boolean isAttached() {
        return server != null;
    }

    public synchronized DummyParticipant findByParticipantId(
            String arenaId,
            UUID participantId
    ) {
        if (participantId == null) {
            return null;
        }

        return all(arenaId)
                .stream()
                .filter(dummy ->
                        dummy.participantId()
                                .equals(participantId)
                )
                .findFirst()
                .orElse(null);
    }

    public synchronized DummyParticipant findByEntityId(
            UUID entityId
    ) {
        if (entityId == null) {
            return null;
        }

        for (Map<UUID, DummyParticipant> arenaDummies :
                dummiesByArena.values()) {
            for (DummyParticipant dummy :
                    arenaDummies.values()) {
                if (dummy.entityId().equals(entityId)) {
                    return dummy;
                }
            }
        }

        return null;
    }

    public synchronized boolean teleport(
            DummyParticipant dummy,
            ArenaPosition position
    ) {
        if (server == null
                || dummy == null
                || position == null) {
            return false;
        }

        ArmorStand entity = entity(dummy);

        if (entity == null) {
            return false;
        }

        ServerLevel targetLevel =
                position.resolveLevel(server);

        if (targetLevel == null) {
            return false;
        }

        /*
         * Les dummies sont normalement déjà dans la dimension
         * de l'arène. Le changement de dimension n'est pas nécessaire
         * pour le premier prototype Sumo.
         */
        if (entity.level() != targetLevel) {
            return false;
        }

        entity.teleportTo(
                position.x,
                position.y,
                position.z
        );

        entity.setYRot(position.yaw);
        entity.setXRot(position.pitch);
        entity.setDeltaMovement(Vec3.ZERO);
        entity.fallDistance = 0.0F;

        return true;
    }

    public synchronized void applyKnockback(
            DummyParticipant dummy,
            ServerPlayer attacker,
            double horizontalStrength,
            double verticalStrength
    ) {
        if (dummy == null || attacker == null) {
            return;
        }

        ArmorStand entity = entity(dummy);

        if (entity == null) {
            return;
        }

        /*
         * Direction horizontale de l'attaquant vers le dummy.
         */
        double directionX =
                entity.getX() - attacker.getX();

        double directionZ =
                entity.getZ() - attacker.getZ();

        double length =
                Math.sqrt(
                        directionX * directionX
                                + directionZ * directionZ
                );

        if (length < 0.0001D) {
            /*
             * Si les deux entités ont exactement la même position,
             * on utilise la direction dans laquelle regarde le joueur.
             */
            Vec3 look = attacker.getLookAngle();

            directionX = look.x;
            directionZ = look.z;

            length = Math.sqrt(
                    directionX * directionX
                            + directionZ * directionZ
            );
        }

        if (length < 0.0001D) {
            return;
        }

        directionX /= length;
        directionZ /= length;

        double safeHorizontal =
                Math.max(0.0D, horizontalStrength);

        double safeVertical =
                Math.max(0.0D, verticalStrength);

        Vec3 previous =
                entity.getDeltaMovement();

        entity.setDeltaMovement(
                directionX * safeHorizontal,
                Math.max(
                        safeVertical,
                        previous.y
                ),
                directionZ * safeHorizontal
        );

        entity.hurtMarked = true;
        entity.hasImpulse = true;
    }

    public synchronized void removeEntityOnly(
            DummyParticipant dummy
    ) {
        if (dummy == null) {
            return;
        }

        String arenaId =
                normalizeArenaId(dummy.arenaId());

        Map<UUID, DummyParticipant> arenaDummies =
                dummiesByArena.get(arenaId);

        if (arenaDummies != null) {
            arenaDummies.remove(
                    dummy.participantId()
            );

            if (arenaDummies.isEmpty()) {
                dummiesByArena.remove(arenaId);
            }
        }

        discardEntity(dummy);
    }

    public synchronized CreateResult create(
            GameInstance instance,
            String requestedName
    ) {
        if (!testMode.isEnabled()) {
            return CreateResult.failure(
                    "Le mode test Olympicraft n'est pas activé."
            );
        }

        if (server == null) {
            return CreateResult.failure(
                    "Aucun serveur logique n'est attaché."
            );
        }

        if (instance == null) {
            return CreateResult.failure(
                    "Aucune partie n'existe pour cette arène."
            );
        }

        if (!instance.state().acceptsPlayers()) {
            return CreateResult.failure(
                    "La partie n'accepte plus de participants."
            );
        }

        ArenaDefinition arena =
                instance.arena();

        String arenaId =
                normalizeArenaId(arena.id);

        ArenaPosition spawn =
                nextSpawn(instance);

        if (spawn == null) {
            return CreateResult.failure(
                    "Aucun spawn disponible pour le dummy."
            );
        }

        ServerLevel level =
                spawn.resolveLevel(server);

        if (level == null) {
            return CreateResult.failure(
                    "La dimension du spawn est introuvable."
            );
        }

        String name = createUniqueName(
                arenaId,
                requestedName
        );

        UUID participantId =
                UUID.randomUUID();

        ArmorStand entity =
                EntityType.ARMOR_STAND.create(level);

        if (entity == null) {
            return CreateResult.failure(
                    "L'entité du dummy n'a pas pu être créée."
            );
        }

        configureEntity(
                entity,
                arenaId,
                participantId,
                name
        );

        entity.moveTo(
                spawn.x,
                spawn.y,
                spawn.z,
                spawn.yaw,
                spawn.pitch
        );

        /*
         * On inscrit d'abord le participant dans la partie.
         * Ainsi, une inscription refusée ne laisse aucune entrée
         * dans DummyManager.
         */
        if (!instance.addDummy(
                participantId,
                name
        )) {
            entity.discard();

            return CreateResult.failure(
                    "La partie a refusé le dummy."
            );
        }

        if (!level.addFreshEntity(entity)) {
            /*
             * L'entité n'a pas pu être créée : on retire également
             * le participant logique.
             */
            instance.remove(participantId);
            entity.discard();

            return CreateResult.failure(
                    "Le dummy n'a pas pu être ajouté au monde."
            );
        }

        DummyParticipant dummy =
                new DummyParticipant(
                        participantId,
                        entity.getUUID(),
                        arenaId,
                        name
                );

        dummiesByArena
                .computeIfAbsent(
                        arenaId,
                        ignored ->
                                new LinkedHashMap<>()
                )
                .put(
                        participantId,
                        dummy
                );

        Olympicraft.LOGGER.info(
                "Dummy '{}' ajouté à l'arène '{}'. "
                        + "Participant={}, entité={}.",
                name,
                arenaId,
                participantId,
                entity.getUUID()
        );

        return CreateResult.success(dummy);
    }

    public synchronized boolean remove(
            GameInstance instance,
            UUID participantId
    ) {
        if (instance == null
                || participantId == null) {
            return false;
        }

        String arenaId =
                normalizeArenaId(
                        instance.arena().id
                );

        Map<UUID, DummyParticipant> arenaDummies =
                dummiesByArena.get(arenaId);

        if (arenaDummies == null) {
            return false;
        }

        DummyParticipant dummy =
                arenaDummies.remove(
                        participantId
                );

        if (dummy == null) {
            return false;
        }

        instance.remove(participantId);
        discardEntity(dummy);

        if (arenaDummies.isEmpty()) {
            dummiesByArena.remove(arenaId);
        }

        Olympicraft.LOGGER.info(
                "Dummy '{}' supprimé de l'arène '{}'.",
                dummy.name(),
                arenaId
        );

        return true;
    }

    public synchronized int clear(
            GameInstance instance
    ) {
        if (instance == null) {
            return 0;
        }

        String arenaId =
                normalizeArenaId(
                        instance.arena().id
                );

        Map<UUID, DummyParticipant> arenaDummies =
                dummiesByArena.remove(arenaId);

        if (arenaDummies == null
                || arenaDummies.isEmpty()) {
            return 0;
        }

        List<DummyParticipant> copy =
                List.copyOf(
                        arenaDummies.values()
                );

        for (DummyParticipant dummy : copy) {
            instance.remove(
                    dummy.participantId()
            );

            discardEntity(dummy);
        }

        return copy.size();
    }

    public synchronized void clearArena(
            String requestedArenaId
    ) {
        String arenaId =
                normalizeArenaId(
                        requestedArenaId
                );

        Map<UUID, DummyParticipant> arenaDummies =
                dummiesByArena.remove(arenaId);

        if (arenaDummies == null) {
            return;
        }

        for (DummyParticipant dummy :
                arenaDummies.values()) {
            discardEntity(dummy);
        }
    }

    public synchronized void clearAll() {
        for (Map<UUID, DummyParticipant> arenaDummies :
                dummiesByArena.values()) {
            for (DummyParticipant dummy :
                    arenaDummies.values()) {
                discardEntity(dummy);
            }
        }

        dummiesByArena.clear();
    }

    public synchronized Collection<DummyParticipant> all(
            String requestedArenaId
    ) {
        String arenaId =
                normalizeArenaId(
                        requestedArenaId
                );

        Map<UUID, DummyParticipant> arenaDummies =
                dummiesByArena.get(arenaId);

        if (arenaDummies == null
                || arenaDummies.isEmpty()) {
            return List.of();
        }

        /*
         * Les références dont l'entité a disparu sont nettoyées
         * avant de renvoyer la liste.
         */
        List<UUID> missing =
                new ArrayList<>();

        for (DummyParticipant dummy :
                arenaDummies.values()) {
            if (entity(dummy) == null) {
                missing.add(
                        dummy.participantId()
                );
            }
        }

        for (UUID participantId : missing) {
            arenaDummies.remove(participantId);
        }

        if (arenaDummies.isEmpty()) {
            dummiesByArena.remove(arenaId);
            return List.of();
        }

        return List.copyOf(
                arenaDummies.values()
        );
    }

    public synchronized int count(
            String arenaId
    ) {
        return all(arenaId).size();
    }

    public synchronized Optional<DummyParticipant> find(
            String arenaId,
            String name
    ) {
        if (name == null || name.isBlank()) {
            return Optional.empty();
        }

        String requestedName =
                name.trim();

        return all(arenaId)
                .stream()
                .filter(dummy ->
                        dummy.name().equalsIgnoreCase(
                                requestedName
                        )
                )
                .findFirst();
    }

    public synchronized ArmorStand entity(
            DummyParticipant dummy
    ) {
        if (server == null || dummy == null) {
            return null;
        }

        for (ServerLevel level :
                server.getAllLevels()) {
            Entity entity =
                    level.getEntity(
                            dummy.entityId()
                    );

            if (entity instanceof ArmorStand armorStand
                    && !armorStand.isRemoved()) {
                return armorStand;
            }
        }

        return null;
    }

    public synchronized ArmorStand entity(
            String arenaId,
            UUID participantId
    ) {
        if (participantId == null) {
            return null;
        }

        DummyParticipant dummy =
                all(arenaId)
                        .stream()
                        .filter(candidate ->
                                candidate.participantId()
                                        .equals(participantId)
                        )
                        .findFirst()
                        .orElse(null);

        return entity(dummy);
    }

    private void configureEntity(
            ArmorStand entity,
            String arenaId,
            UUID participantId,
            String name
    ) {
        entity.setInvisible(false);
        entity.setInvulnerable(false);
        entity.setNoGravity(false);

        entity.setCustomName(
                Component.literal(name)
        );
        entity.setCustomNameVisible(true);
        entity.setShowArms(true);

        /*
         * Tags persistants permettant d'identifier et de nettoyer
         * les dummies après un arrêt anormal.
         */
        entity.addTag("olympicraft_dummy");
        entity.addTag(
                "olympicraft_arena_"
                        + safeTag(arenaId)
        );
        entity.addTag(
                "olympicraft_participant_"
                        + participantId
        );

        equipTrollOutfit(entity);
    }

    private static void equipTrollOutfit(
            ArmorStand entity
    ) {
        ItemStack head =
                new ItemStack(Items.PLAYER_HEAD);

        head.set(
                DataComponents.CUSTOM_NAME,
                Component.literal(
                        "Tête de Troll"
                )
        );

        /*
         * Première apparence amusante.
         *
         * Le véritable skin de troll sera ensuite stocké dans
         * la configuration avec une texture de profil.
         */
        ItemStack chest =
                new ItemStack(
                        Items.DIAMOND_CHESTPLATE
                );

        ItemStack legs =
                new ItemStack(
                        Items.DIAMOND_LEGGINGS
                );

        ItemStack boots =
                new ItemStack(
                        Items.DIAMOND_BOOTS
                );

        entity.setItemSlot(
                EquipmentSlot.HEAD,
                head
        );

        entity.setItemSlot(
                EquipmentSlot.CHEST,
                chest
        );

        entity.setItemSlot(
                EquipmentSlot.LEGS,
                legs
        );

        entity.setItemSlot(
                EquipmentSlot.FEET,
                boots
        );
    }

    private void discardEntity(
            DummyParticipant dummy
    ) {
        ArmorStand entity = entity(dummy);

        if (entity != null) {
            entity.discard();
        }
    }

    private ArenaPosition nextSpawn(
            GameInstance instance
    ) {
        List<ArenaPosition> positions =
                allSpawns(
                        instance.arena()
                );

        int competitorIndex =
                Math.toIntExact(
                        Math.min(
                                Integer.MAX_VALUE,
                                instance.playerCount()
                        )
                );

        if (competitorIndex < 0
                || competitorIndex >= positions.size()) {
            return null;
        }

        return positions.get(competitorIndex);
    }

    private static List<ArenaPosition> allSpawns(
            ArenaDefinition arena
    ) {
        List<ArenaPosition> positions =
                new ArrayList<>();

        if (arena.spawns == null) {
            return positions;
        }

        for (List<ArenaPosition> group :
                arena.spawns.values()) {
            if (group != null) {
                positions.addAll(group);
            }
        }

        return positions;
    }

    private String createUniqueName(
            String arenaId,
            String requestedName
    ) {
        if (requestedName != null
                && !requestedName.isBlank()) {
            String base =
                    sanitizeName(
                            requestedName
                    );

            if (!nameExists(arenaId, base)) {
                return base;
            }

            int suffix = 2;

            while (nameExists(
                    arenaId,
                    base + "_" + suffix
            )) {
                suffix++;
            }

            return base + "_" + suffix;
        }

        int index = 1;
        String candidate;

        do {
            candidate = "Dummy_" + index;
            index++;
        } while (nameExists(
                arenaId,
                candidate
        ));

        return candidate;
    }

    private boolean nameExists(
            String arenaId,
            String name
    ) {
        return all(arenaId)
                .stream()
                .anyMatch(dummy ->
                        dummy.name()
                                .equalsIgnoreCase(name)
                );
    }

    private static String sanitizeName(
            String requestedName
    ) {
        String normalized =
                requestedName.trim();

        if (normalized.length() > 24) {
            normalized =
                    normalized.substring(
                            0,
                            24
                    );
        }

        return normalized.isBlank()
                ? "Dummy"
                : normalized;
    }

    private void removeOrphanEntities() {
        if (server == null) {
            return;
        }

        int removed = 0;

        for (ServerLevel level :
                server.getAllLevels()) {
            /*
             * Les dummies sont des ArmorStand persistants.
             * On utilise la recherche générale par tag pour enlever
             * les mannequins abandonnés par une précédente session.
             */
            for (ArmorStand armorStand :
                    level.getEntities(
                            EntityType.ARMOR_STAND,
                            entity ->
                                    entity.getTags().contains(
                                            "olympicraft_dummy"
                                    )
                    )) {
                armorStand.discard();
                removed++;
            }
        }

        if (removed > 0) {
            Olympicraft.LOGGER.warn(
                    "{} dummy(s) orphelin(s) supprimé(s) "
                            + "au démarrage.",
                    removed
            );
        }
    }

    private static String normalizeArenaId(
            String value
    ) {
        if (value == null) {
            return "";
        }

        return ArenaManager.normalizeId(value);
    }

    private static String safeTag(
            String value
    ) {
        return value.toLowerCase(Locale.ROOT)
                .replaceAll(
                        "[^a-z0-9_.-]",
                        "_"
                );
    }

    public record CreateResult(
            boolean successful,
            String error,
            DummyParticipant dummy
    ) {
        public static CreateResult success(
                DummyParticipant dummy
        ) {
            return new CreateResult(
                    true,
                    null,
                    dummy
            );
        }

        public static CreateResult failure(
                String error
        ) {
            return new CreateResult(
                    false,
                    error,
                    null
            );
        }
    }
}