package fr.olympicraft.match.player;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class PlayerSnapshot {

    private static final int DATA_VERSION = 1;

    private final UUID playerId;

    private final ResourceKey<Level> dimension;

    private final double x;
    private final double y;
    private final double z;

    private final float yaw;
    private final float pitch;

    private final GameType gameMode;

    private final List<ItemStack> inventory;
    private final List<ItemStack> armor;
    private final List<ItemStack> offhand;

    private final int selectedSlot;

    private final float health;
    private final int foodLevel;
    private final float saturation;
    private final int airSupply;

    private final int experienceLevel;
    private final int totalExperience;
    private final float experienceProgress;

    private final boolean invulnerable;
    private final boolean flying;
    private final boolean mayFly;
    private final float flyingSpeed;

    private final List<MobEffectInstance> effects;

    private PlayerSnapshot(
            UUID playerId,
            ResourceKey<Level> dimension,
            double x,
            double y,
            double z,
            float yaw,
            float pitch,
            GameType gameMode,
            List<ItemStack> inventory,
            List<ItemStack> armor,
            List<ItemStack> offhand,
            int selectedSlot,
            float health,
            int foodLevel,
            float saturation,
            int airSupply,
            int experienceLevel,
            int totalExperience,
            float experienceProgress,
            boolean invulnerable,
            boolean flying,
            boolean mayFly,
            float flyingSpeed,
            List<MobEffectInstance> effects
    ) {
        this.playerId = playerId;
        this.dimension = dimension;

        this.x = x;
        this.y = y;
        this.z = z;

        this.yaw = yaw;
        this.pitch = pitch;

        this.gameMode = gameMode;

        this.inventory = copyItems(inventory);
        this.armor = copyItems(armor);
        this.offhand = copyItems(offhand);

        this.selectedSlot = selectedSlot;

        this.health = health;
        this.foodLevel = foodLevel;
        this.saturation = saturation;
        this.airSupply = airSupply;

        this.experienceLevel = experienceLevel;
        this.totalExperience = totalExperience;
        this.experienceProgress = experienceProgress;

        this.invulnerable = invulnerable;
        this.flying = flying;
        this.mayFly = mayFly;
        this.flyingSpeed = flyingSpeed;

        this.effects = copyEffects(effects);
    }

    public static PlayerSnapshot capture(
            ServerPlayer player
    ) {
        return new PlayerSnapshot(
                player.getUUID(),
                player.serverLevel().dimension(),
                player.getX(),
                player.getY(),
                player.getZ(),
                player.getYRot(),
                player.getXRot(),
                player.gameMode.getGameModeForPlayer(),
                player.getInventory().items,
                player.getInventory().armor,
                player.getInventory().offhand,
                player.getInventory().selected,
                player.getHealth(),
                player.getFoodData().getFoodLevel(),
                player.getFoodData().getSaturationLevel(),
                player.getAirSupply(),
                player.experienceLevel,
                player.totalExperience,
                player.experienceProgress,
                player.getAbilities().invulnerable,
                player.getAbilities().flying,
                player.getAbilities().mayfly,
                player.getAbilities().getFlyingSpeed(),
                new ArrayList<>(
                        player.getActiveEffects()
                )
        );
    }

    public CompoundTag save(
            HolderLookup.Provider registries
    ) {
        CompoundTag root = new CompoundTag();

        root.putInt("DataVersion", DATA_VERSION);
        root.putUUID("PlayerId", playerId);

        root.putString(
                "Dimension",
                dimension.location().toString()
        );

        root.putDouble("X", x);
        root.putDouble("Y", y);
        root.putDouble("Z", z);

        root.putFloat("Yaw", yaw);
        root.putFloat("Pitch", pitch);

        root.putString(
                "GameMode",
                gameMode.getName()
        );

        root.put(
                "Inventory",
                saveItems(inventory, registries)
        );

        root.put(
                "Armor",
                saveItems(armor, registries)
        );

        root.put(
                "Offhand",
                saveItems(offhand, registries)
        );

        root.putInt("SelectedSlot", selectedSlot);

        root.putFloat("Health", health);
        root.putInt("FoodLevel", foodLevel);
        root.putFloat("Saturation", saturation);
        root.putInt("AirSupply", airSupply);

        root.putInt(
                "ExperienceLevel",
                experienceLevel
        );

        root.putInt(
                "TotalExperience",
                totalExperience
        );

        root.putFloat(
                "ExperienceProgress",
                experienceProgress
        );

        CompoundTag abilities =
                new CompoundTag();

        abilities.putBoolean(
                "Invulnerable",
                invulnerable
        );

        abilities.putBoolean(
                "Flying",
                flying
        );

        abilities.putBoolean(
                "MayFly",
                mayFly
        );

        abilities.putFloat(
                "FlyingSpeed",
                flyingSpeed
        );

        root.put("Abilities", abilities);

        ListTag effectList = new ListTag();

        for (MobEffectInstance effect : effects) {
            effectList.add(
                    effect.save()
            );
        }

        root.put("Effects", effectList);

        return root;
    }

    public static PlayerSnapshot load(
            CompoundTag root,
            HolderLookup.Provider registries
    ) {
        UUID playerId =
                root.getUUID("PlayerId");

        ResourceLocation dimensionLocation =
                ResourceLocation.tryParse(
                        root.getString("Dimension")
                );

        if (dimensionLocation == null) {
            dimensionLocation =
                    Level.OVERWORLD.location();
        }

        ResourceKey<Level> dimension =
                ResourceKey.create(
                        Registries.DIMENSION,
                        dimensionLocation
                );

        GameType gameMode =
                GameType.byName(
                        root.getString("GameMode"),
                        GameType.SURVIVAL
                );

        CompoundTag abilities =
                root.getCompound("Abilities");

        return new PlayerSnapshot(
                playerId,
                dimension,
                root.getDouble("X"),
                root.getDouble("Y"),
                root.getDouble("Z"),
                root.getFloat("Yaw"),
                root.getFloat("Pitch"),
                gameMode,
                loadItems(
                        root.getList(
                                "Inventory",
                                Tag.TAG_COMPOUND
                        ),
                        registries
                ),
                loadItems(
                        root.getList(
                                "Armor",
                                Tag.TAG_COMPOUND
                        ),
                        registries
                ),
                loadItems(
                        root.getList(
                                "Offhand",
                                Tag.TAG_COMPOUND
                        ),
                        registries
                ),
                root.getInt("SelectedSlot"),
                root.getFloat("Health"),
                root.getInt("FoodLevel"),
                root.getFloat("Saturation"),
                root.getInt("AirSupply"),
                root.getInt("ExperienceLevel"),
                root.getInt("TotalExperience"),
                root.getFloat("ExperienceProgress"),
                abilities.getBoolean("Invulnerable"),
                abilities.getBoolean("Flying"),
                abilities.getBoolean("MayFly"),
                abilities.getFloat("FlyingSpeed"),
                loadEffects(
                        root.getList(
                                "Effects",
                                Tag.TAG_COMPOUND
                        )
                )
        );
    }

    private static ListTag saveItems(
            List<ItemStack> items,
            HolderLookup.Provider registries
    ) {
        ListTag result = new ListTag();

        for (int slot = 0;
             slot < items.size();
             slot++) {
            ItemStack stack =
                    items.get(slot);

            CompoundTag entry =
                    new CompoundTag();

            entry.putInt("Slot", slot);

            if (!stack.isEmpty()) {
                entry.put(
                        "Item",
                        stack.save(registries)
                );
            }

            result.add(entry);
        }

        return result;
    }

    private static List<ItemStack> loadItems(
            ListTag entries,
            HolderLookup.Provider registries
    ) {
        int highestSlot = -1;

        for (int index = 0;
             index < entries.size();
             index++) {
            CompoundTag entry =
                    entries.getCompound(index);

            highestSlot = Math.max(
                    highestSlot,
                    entry.getInt("Slot")
            );
        }

        List<ItemStack> result =
                new ArrayList<>();

        for (int index = 0;
             index <= highestSlot;
             index++) {
            result.add(ItemStack.EMPTY);
        }

        for (int index = 0;
             index < entries.size();
             index++) {
            CompoundTag entry =
                    entries.getCompound(index);

            int slot = entry.getInt("Slot");

            if (slot < 0 || slot >= result.size()) {
                continue;
            }

            if (!entry.contains(
                    "Item",
                    Tag.TAG_COMPOUND
            )) {
                continue;
            }

            Optional<ItemStack> stack =
                    ItemStack.parse(
                            registries,
                            entry.getCompound("Item")
                    );

            stack.ifPresent(itemStack ->
                    result.set(
                            slot,
                            itemStack
                    )
            );
        }

        return result;
    }

    private static List<MobEffectInstance> loadEffects(
            ListTag entries
    ) {
        List<MobEffectInstance> result =
                new ArrayList<>();

        for (int index = 0;
             index < entries.size();
             index++) {
            CompoundTag entry =
                    entries.getCompound(index);

            MobEffectInstance effect =
                    MobEffectInstance.load(entry);

            if (effect != null) {
                result.add(effect);
            }
        }

        return result;
    }

    private static List<ItemStack> copyItems(
            List<ItemStack> source
    ) {
        List<ItemStack> result =
                new ArrayList<>(source.size());

        for (ItemStack stack : source) {
            result.add(stack.copy());
        }

        return result;
    }

    private static List<MobEffectInstance> copyEffects(
            List<MobEffectInstance> source
    ) {
        List<MobEffectInstance> result =
                new ArrayList<>(source.size());

        for (MobEffectInstance effect : source) {
            result.add(
                    new MobEffectInstance(effect)
            );
        }

        return result;
    }

    public UUID playerId() {
        return playerId;
    }

    public ResourceKey<Level> dimension() {
        return dimension;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double z() {
        return z;
    }

    public int itemCount() {
        return countItems(inventory)
                + countItems(armor)
                + countItems(offhand);
    }

    private static int countItems(
            List<ItemStack> stacks
    ) {
        int amount = 0;

        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) {
                amount += stack.getCount();
            }
        }

        return amount;
    }

    public boolean restore(
            MinecraftServer server,
            ServerPlayer player
    ) {
        if (server == null
                || player == null
                || !playerId.equals(
                player.getUUID()
        )) {
            return false;
        }

        ServerLevel targetLevel =
                server.getLevel(dimension);

        if (targetLevel == null) {
            return false;
        }

        /*
         * Ferme le menu Olympicraft ou tout autre conteneur
         * encore ouvert.
         */
        player.closeContainer();

        /*
         * Arrête les mouvements venant de la partie.
         */
        player.setDeltaMovement(
                0.0D,
                0.0D,
                0.0D
        );

        player.fallDistance = 0.0F;

        /*
         * Restaure le mode de jeu sauvegardé.
         */
        player.setGameMode(gameMode);

        /*
         * Restaure l'inventaire.
         */
        restoreItems(
                player.getInventory().items,
                inventory
        );

        restoreItems(
                player.getInventory().armor,
                armor
        );

        restoreItems(
                player.getInventory().offhand,
                offhand
        );

        int hotbarSize =
                Math.min(
                        9,
                        player.getInventory()
                                .items.size()
                );

        player.getInventory().selected =
                hotbarSize <= 0
                        ? 0
                        : Math.clamp(
                        selectedSlot,
                        0,
                        hotbarSize - 1
                );

        /*
         * Restaure les effets.
         */
        player.removeAllEffects();

        for (MobEffectInstance effect : effects) {
            player.addEffect(
                    new MobEffectInstance(effect)
            );
        }

        /*
         * Restaure la vie et la nourriture.
         */
        player.setHealth(
                Math.clamp(
                        health,
                        1.0F,
                        player.getMaxHealth()
                )
        );

        player.getFoodData().setFoodLevel(
                foodLevel
        );

        player.getFoodData().setSaturation(
                saturation
        );

        player.setAirSupply(
                airSupply
        );

        /*
         * Restaure l'expérience.
         */
        player.experienceLevel =
                experienceLevel;

        player.totalExperience =
                totalExperience;

        player.experienceProgress =
                experienceProgress;

        /*
         * Restaure les capacités.
         */
        player.getAbilities().invulnerable =
                invulnerable;

        player.getAbilities().mayfly =
                mayFly;

        player.getAbilities().flying =
                flying && mayFly;

        player.getAbilities().setFlyingSpeed(
                flyingSpeed
        );

        player.onUpdateAbilities();

        /*
         * Replace le joueur à sa position précédente.
         */
        player.teleportTo(
                targetLevel,
                x,
                y,
                z,
                yaw,
                pitch
        );

        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();

        /*
         * Refait la synchronisation au tick suivant.
         * Cela évite que le client reste bloqué avec les
         * restrictions du mode aventure.
         */
        server.execute(() -> {
            if (player.isRemoved()
                    || !playerId.equals(
                    player.getUUID()
            )) {
                return;
            }

            player.setGameMode(gameMode);

            player.getAbilities().invulnerable =
                    invulnerable;

            player.getAbilities().mayfly =
                    mayFly;

            player.getAbilities().flying =
                    flying && mayFly;

            player.getAbilities().setFlyingSpeed(
                    flyingSpeed
            );

            player.onUpdateAbilities();

            player.getInventory().setChanged();
            player.inventoryMenu.broadcastChanges();
        });

        return true;
    }

    private static void restoreItems(
            List<ItemStack> destination,
            List<ItemStack> source
    ) {
        if (destination == null) {
            return;
        }

        int sourceSize =
                source == null
                        ? 0
                        : source.size();

        int commonSize =
                Math.min(
                        destination.size(),
                        sourceSize
                );

        /*
         * Restaure les objets sauvegardés.
         */
        for (int index = 0;
             index < commonSize;
             index++) {
            ItemStack savedStack =
                    source.get(index);

            destination.set(
                    index,
                    savedStack == null
                            ? ItemStack.EMPTY
                            : savedStack.copy()
            );
        }

        /*
         * Supprime les objets temporaires restants.
         */
        for (int index = commonSize;
             index < destination.size();
             index++) {
            destination.set(
                    index,
                    ItemStack.EMPTY
            );
        }
    }
}