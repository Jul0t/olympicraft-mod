package fr.olympicraft.gui.input;

import fr.olympicraft.gui.GuiManager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class AnvilTextInputHandler extends AnvilMenu {


    private final GuiManager guiManager;
    private final AnvilTextInputMenu inputMenu;

    private boolean submitted;
    private boolean reopeningMenu;

    public AnvilTextInputHandler(
            int containerId,
            Inventory inventory,
            GuiManager guiManager,
            AnvilTextInputMenu inputMenu
    ) {
        super(
                containerId,
                inventory,
                ContainerLevelAccess.create(
                        inventory.player.level(),
                        inventory.player.blockPosition()
                )
        );

        this.guiManager = guiManager;
        this.inputMenu = inputMenu;

        String initialValue = normalizedInitialValue();

        ItemStack input = new ItemStack(Items.PAPER);

        /*
         * Le papier paraît ne porter aucun nom.
         *
         * Le caractère \u200B est une espace de largeur nulle :
         * il est présent techniquement, mais invisible en jeu.
         */
        input.set(
                DataComponents.CUSTOM_NAME,
                Component.literal("")
        );

        this.inputSlots.setItem(0, input);

        /*
         * Le champ de l'enclume reçoit ensuite la véritable
         * valeur proposée, par exemple "void_2".
         */
        setItemName(initialValue);

        createResult();
        broadcastChanges();

    }

    private String normalizedInitialValue() {
        String initialValue = inputMenu.initialValue();

        if (initialValue != null && !initialValue.isBlank()) {
            return initialValue.trim();
        }

        String placeholder = inputMenu.placeholder();

        if (placeholder != null && !placeholder.isBlank()) {
            return placeholder.trim();
        }

        return "Nom";
    }

    @Override
    public boolean stillValid(Player player) {
        /*
         * Aucune véritable enclume n'est nécessaire dans le monde.
         */
        return true;
    }

    @Override
    public void createResult() {
        super.createResult();
    }

    @Override
    protected boolean mayPickup(
            Player player,
            boolean hasStack
    ) {
        return hasStack;
    }

    @Override
    public void clicked(
            int slotId,
            int button,
            ClickType clickType,
            Player player
    ) {
        /*
         * L'emplacement 2 est le résultat de l'enclume.
         *
         * Nous interceptons son clic avant que le code vanilla ne déplace
         * le papier dans l'inventaire du joueur.
         */
        if (slotId == 2 && !getSlot(2).getItem().isEmpty()) {
            submit(player);
            return;
        }

        super.clicked(
                slotId,
                button,
                clickType,
                player
        );
    }

    private void submit(Player player) {
        if (submitted) {
            return;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        /*
         * Le nom du résultat contient le texte réellement présent
         * dans le champ de l'enclume.
         */
        ItemStack result = getSlot(2).getItem();

        String submittedValue = result.isEmpty()
                ? ""
                : result.getHoverName()
                .getString()
                .replace("\u200B", "")
                .trim();

        String placeholder = inputMenu.placeholder();

        /*
         * Si le champ contient seulement le texte indicatif, il est
         * interprété comme une saisie vide.
         */
        if (placeholder != null
                && submittedValue.equals(placeholder.trim())) {
            submittedValue = "";
        }

        if (submittedValue.equalsIgnoreCase("Nom")) {
            submittedValue = "";
        }

        /*
         * Marqué avant la fermeture afin que removed() ne considère pas
         * celle-ci comme une annulation.
         */
        submitted = true;

        /*
         * Suppression explicite des objets temporaires.
         *
         * Cela empêche :
         * - le papier d'entrée de revenir dans l'inventaire ;
         * - le papier de résultat d'être donné au joueur ;
         * - une duplication lors de la fermeture du menu.
         */
        this.inputSlots.setItem(0, ItemStack.EMPTY);
        this.inputSlots.setItem(1, ItemStack.EMPTY);
        this.resultSlots.setItem(0, ItemStack.EMPTY);

        broadcastChanges();

        String finalValue = submittedValue;

        /*
         * La fenêtre est fermée avant d'exécuter le callback.
         * Le callback pourra ensuite ouvrir le menu suivant.
         */
        serverPlayer.closeContainer();

        serverPlayer.getServer().execute(() ->
                inputMenu.submit(
                        serverPlayer,
                        finalValue
                )
        );
    }

    @Override
    protected void onTake(
            Player player,
            ItemStack stack
    ) {
        /*
         * Sécurité supplémentaire.
         *
         * Le clic normal sur le résultat est déjà intercepté par
         * clicked(), mais on conserve ce traitement au cas où une autre
         * interaction vanilla déclencherait directement onTake().
         */
        submit(player);
    }

    @Override
    public void removed(Player player) {
        /*
         * Avant d'appeler super.removed(), on supprime le papier
         * temporaire. Sinon, ItemCombinerMenu pourrait le rendre au joueur.
         */
        this.inputSlots.setItem(0, ItemStack.EMPTY);
        this.inputSlots.setItem(1, ItemStack.EMPTY);
        this.resultSlots.setItem(0, ItemStack.EMPTY);

        super.removed(player);

        if (submitted || reopeningMenu) {
            return;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        reopeningMenu = true;

        inputMenu.cancel(
                guiManager,
                serverPlayer
        );
    }
}