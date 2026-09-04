package fr.olympicraft.game.sumo.kit;

import fr.olympicraft.Olympicraft;
import fr.olympicraft.config.model.game.SumoConfig;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.ArrayList;
import java.util.List;

public final class SumoKitService {

    public boolean give(
            ServerPlayer player,
            SumoConfig.KitPreset preset
    ) {
        if (player == null || preset == null) {
            return false;
        }

        player.getInventory().clearContent();

        for (SumoConfig.KitItem itemConfig :
                preset.items) {
            if (itemConfig == null
                    || !itemConfig.enabled) {
                continue;
            }

            ItemStack stack =
                    createStack(
                            player,
                            itemConfig
                    );

            if (stack.isEmpty()) {
                continue;
            }

            int slot = Math.clamp(
                    itemConfig.slot,
                    0,
                    player.getInventory()
                            .items.size() - 1
            );

            player.getInventory().setItem(
                    slot,
                    stack
            );
        }

        player.getInventory().selected = 0;
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();

        return true;
    }

    private ItemStack createStack(
            ServerPlayer player,
            SumoConfig.KitItem itemConfig
    ) {
        ResourceLocation identifier =
                ResourceLocation.tryParse(
                        itemConfig.item
                );

        if (identifier == null) {
            Olympicraft.LOGGER.warn(
                    "Identifiant d'objet Sumo invalide : '{}'.",
                    itemConfig.item
            );

            return ItemStack.EMPTY;
        }

        Item item =
                BuiltInRegistries.ITEM.get(
                        identifier
                );

        if (item == null
                || item == net.minecraft.world.item.Items.AIR) {
            Olympicraft.LOGGER.warn(
                    "Objet Sumo introuvable : '{}'.",
                    itemConfig.item
            );

            return ItemStack.EMPTY;
        }

        ItemStack stack =
                new ItemStack(
                        item,
                        itemConfig.amount
                );

        if (itemConfig.nameMessageKey != null
                && !itemConfig.nameMessageKey
                .isBlank()) {
            stack.set(
                    DataComponents.CUSTOM_NAME,
                    Olympicraft.messages().render(
                            itemConfig.nameMessageKey,
                            false
                    )
            );
        }

        List<Component> lore =
                new ArrayList<>();

        if (itemConfig.loreMessageKeys != null) {
            for (String key :
                    itemConfig.loreMessageKeys) {
                if (key == null
                        || key.isBlank()) {
                    continue;
                }

                lore.add(
                        Olympicraft.messages()
                                .render(
                                        key,
                                        false
                                )
                );
            }
        }

        if (!lore.isEmpty()) {
            stack.set(
                    DataComponents.LORE,
                    new ItemLore(lore)
            );
        }

        if (itemConfig.knockbackLevel > 0) {
            Holder.Reference<Enchantment> knockback =
                    player.registryAccess()
                            .lookupOrThrow(
                                    Registries.ENCHANTMENT
                            )
                            .getOrThrow(
                                    Enchantments.KNOCKBACK
                            );

            stack.enchant(
                    knockback,
                    itemConfig.knockbackLevel
            );
        }

        if (itemConfig.unbreakable) {
            stack.set(
                    DataComponents.UNBREAKABLE,
                    new net.minecraft.world.item.component
                            .Unbreakable(false)
            );
        }

        return stack;
    }
}