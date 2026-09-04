package fr.olympicraft.mixin;

import fr.olympicraft.game.sumo.kit.SumoKitProtectionService;

import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {

    @Shadow
    public ServerPlayer player;

    @Inject(
            method = "handlePlayerAction",
            at = @At("HEAD"),
            cancellable = true
    )
    private void olympicraft$preventSumoItemDrop(
            ServerboundPlayerActionPacket packet,
            CallbackInfo callback
    ) {
        ServerboundPlayerActionPacket.Action action =
                packet.getAction();

        if (action
                != ServerboundPlayerActionPacket.Action.DROP_ITEM
                && action
                != ServerboundPlayerActionPacket.Action.DROP_ALL_ITEMS) {
            return;
        }

        if (!SumoKitProtectionService
                .preventsDrop(player)) {
            return;
        }

        /*
         * Empêche le serveur de retirer réellement
         * l'objet de l'inventaire.
         */
        callback.cancel();

        /*
         * Le client a déjà retiré visuellement l'objet lorsqu'il
         * a envoyé le paquet. On renvoie donc tout le contenu du
         * menu d'inventaire afin de restaurer immédiatement
         * l'affichage côté client.
         */
        player.connection.send(
                new ClientboundContainerSetContentPacket(
                        player.inventoryMenu.containerId,
                        player.inventoryMenu.incrementStateId(),
                        player.inventoryMenu.getItems(),
                        player.inventoryMenu.getCarried()
                )
        );
    }
}