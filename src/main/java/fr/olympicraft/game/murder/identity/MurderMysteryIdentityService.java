package fr.olympicraft.game.murder.identity;

import fr.olympicraft.Olympicraft;
import fr.olympicraft.game.murder.MurderMysteryParticipant;
import fr.olympicraft.test.dummy.DummyManager;
import fr.olympicraft.test.dummy.DummyParticipant;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.decoration.ArmorStand;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public final class MurderMysteryIdentityService {

    private final MinecraftServer server;
    private final String arenaId;
    private final DummyManager dummies;

    /*
     * Valeurs à restaurer après la partie.
     */
    private final Map<UUID, OriginalPlayerIdentity>
            originalPlayerIdentities =
            new LinkedHashMap<>();

    private final Map<UUID, OriginalDummyIdentity>
            originalDummyIdentities =
            new LinkedHashMap<>();

    public MurderMysteryIdentityService(
            MinecraftServer server,
            String arenaId
    ) {
        this.server = server;
        this.arenaId = arenaId == null
                ? ""
                : arenaId;

        this.dummies =
                Olympicraft.dummies();
    }

    public void applyAll(
            Collection<MurderMysteryParticipant> participants
    ) {
        if (participants == null) {
            return;
        }

        for (MurderMysteryParticipant participant :
                participants) {
            apply(participant);
        }
    }

    public void apply(
            MurderMysteryParticipant participant
    ) {
        if (participant == null) {
            return;
        }

        if (participant.dummy()) {
            applyDummyIdentity(participant);
            return;
        }

        applyPlayerIdentity(participant);
    }

    public void restoreAll() {
        restorePlayers();
        restoreDummies();

        originalPlayerIdentities.clear();
        originalDummyIdentities.clear();
    }

    private void applyPlayerIdentity(
            MurderMysteryParticipant participant
    ) {
        ServerPlayer player =
                server.getPlayerList()
                        .getPlayer(
                                participant.participantId()
                        );

        if (player == null) {
            return;
        }

        originalPlayerIdentities.putIfAbsent(
                player.getUUID(),
                new OriginalPlayerIdentity(
                        player.getCustomName(),
                        player.isCustomNameVisible()
                )
        );

        Component alias =
                Component.literal(
                        participant.alias()
                );

        /*
         * Première étape de l'anonymisation.
         *
         * Le nom personnalisé pourra être utilisé par certaines
         * interfaces et par nos futurs messages personnalisés.
         *
         * Le TAB sera anonymisé séparément avec des paquets réseau,
         * car ServerPlayer ne fournit pas de méthode permettant de
         * modifier directement son nom dans cette version.
         */
        player.setCustomName(alias);
        player.setCustomNameVisible(true);
    }

    private void applyDummyIdentity(
            MurderMysteryParticipant participant
    ) {
        DummyParticipant dummy =
                dummies.findByParticipantId(
                        arenaId,
                        participant.participantId()
                );

        if (dummy == null) {
            return;
        }

        ArmorStand entity =
                dummies.entity(dummy);

        if (entity == null) {
            return;
        }

        originalDummyIdentities.putIfAbsent(
                participant.participantId(),
                new OriginalDummyIdentity(
                        entity.getCustomName(),
                        entity.isCustomNameVisible()
                )
        );

        entity.setCustomName(
                Component.literal(
                        participant.alias()
                )
        );

        entity.setCustomNameVisible(true);
    }

    private void restorePlayers() {
        for (Map.Entry<UUID, OriginalPlayerIdentity> entry :
                originalPlayerIdentities.entrySet()) {
            ServerPlayer player =
                    server.getPlayerList()
                            .getPlayer(
                                    entry.getKey()
                            );

            if (player == null) {
                continue;
            }

            OriginalPlayerIdentity identity =
                    entry.getValue();

            player.setCustomName(
                    identity.customName()
            );

            player.setCustomNameVisible(
                    identity.customNameVisible()
            );
        }
    }

    private void restoreDummies() {
        for (Map.Entry<UUID, OriginalDummyIdentity> entry :
                originalDummyIdentities.entrySet()) {
            DummyParticipant dummy =
                    dummies.findByParticipantId(
                            arenaId,
                            entry.getKey()
                    );

            if (dummy == null) {
                continue;
            }

            ArmorStand entity =
                    dummies.entity(dummy);

            if (entity == null) {
                continue;
            }

            OriginalDummyIdentity identity =
                    entry.getValue();

            entity.setCustomName(
                    identity.customName()
            );

            entity.setCustomNameVisible(
                    identity.customNameVisible()
            );
        }
    }

    private record OriginalPlayerIdentity(
            Component customName,
            boolean customNameVisible
    ) {
    }

    private record OriginalDummyIdentity(
            Component customName,
            boolean customNameVisible
    ) {
    }
}