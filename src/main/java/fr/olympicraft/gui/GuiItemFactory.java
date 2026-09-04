package fr.olympicraft.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public final class GuiItemFactory {

    private GuiItemFactory() {
    }

    public static ItemStack item(
            Item item,
            String name,
            ChatFormatting color,
            String... lore
    ) {
        ItemStack stack = new ItemStack(item);

        stack.set(
                DataComponents.CUSTOM_NAME,
                Component.literal(name)
                        .withStyle(color)
        );

        if (lore != null && lore.length > 0) {
            List<Component> lines = new ArrayList<>();

            for (String line : lore) {
                lines.add(
                        Component.literal(line)
                                .withStyle(ChatFormatting.GRAY)
                );
            }

            /*
             * En 1.21.1, le composant lore utilise ItemLore.
             */
            stack.set(
                    DataComponents.LORE,
                    new net.minecraft.world.item.component.ItemLore(
                            lines
                    )
            );
        }

        return stack;
    }

    public static ItemStack filler() {
        return item(
                net.minecraft.world.item.Items.GRAY_STAINED_GLASS_PANE,
                " ",
                ChatFormatting.DARK_GRAY
        );
    }
}
