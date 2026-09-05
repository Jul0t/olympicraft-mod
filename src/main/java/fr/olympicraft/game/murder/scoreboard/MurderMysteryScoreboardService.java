package fr.olympicraft.game.murder.scoreboard;

import fr.olympicraft.game.murder.MurderMysteryParticipant;
import fr.olympicraft.game.murder.role.MurderMysteryRole;
import fr.olympicraft.match.GameInstance;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import net.minecraft.world.scores.criteria.ObjectiveCriteria.RenderType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MurderMysteryScoreboardService {

    private static final String OBJECTIVE_PREFIX =
            "oc_mm_";

    private static final String TEAM_PREFIX =
            "oc_mm_l_";

    /*
     * Nombre de lignes réservées au scoreboard.
     *
     * Elles sont créées une seule fois, puis leur contenu
     * est modifié sans supprimer l'objectif.
     */
    private static final int LINE_AMOUNT = 8;

    /*
     * Chaque joueur possède son propre objectif.
     *
     * Cela évite que le rôle privé d'un joueur soit affiché
     * sur le scoreboard d'un autre participant.
     */
    private final Map<UUID, BoardState> boards =
            new ConcurrentHashMap<>();

    public void showAll(
            GameInstance instance,
            Map<UUID, MurderMysteryParticipant> participants,
            int remainingSeconds,
            boolean preparationActive
    ) {
        if (instance == null || participants == null) {
            return;
        }

        for (ServerPlayer player :
                instance.onlineParticipants()) {
            MurderMysteryParticipant participant =
                    participants.get(
                            player.getUUID()
                    );

            if (participant == null) {
                continue;
            }

            show(
                    player,
                    participant,
                    remainingSeconds,
                    preparationActive
            );
        }
    }

    public void updateIfNeeded(
            GameInstance instance,
            Map<UUID, MurderMysteryParticipant> participants,
            int remainingSeconds,
            boolean preparationActive
    ) {
        if (instance == null || participants == null) {
            return;
        }

        for (ServerPlayer player :
                instance.onlineParticipants()) {
            MurderMysteryParticipant participant =
                    participants.get(
                            player.getUUID()
                    );

            if (participant == null) {
                continue;
            }

            BoardState board =
                    boards.get(
                            player.getUUID()
                    );

            /*
             * Le scoreboard n'existe pas encore, ou le joueur
             * vient de se reconnecter : on le crée.
             */
            if (board == null
                    || board.scoreboard()
                    != player.getScoreboard()) {
                show(
                        player,
                        participant,
                        remainingSeconds,
                        preparationActive
                );

                continue;
            }

            /*
             * Aucune information visible n'a changé.
             */
            if (board.remainingSeconds()
                    == remainingSeconds
                    && board.preparationActive()
                    == preparationActive
                    && board.wounds()
                    == participant.wounds()
                    && board.alive()
                    == participant.alive()
                    && board.poisoned()
                    == participant.poisoned()
                    && board.disconnected()
                    == participant.disconnected()
                    && board.role()
                    == participant.role()
                    && board.alias().equals(
                    safeAlias(participant)
            )) {
                continue;
            }

            update(
                    player,
                    participant,
                    remainingSeconds,
                    preparationActive,
                    board
            );
        }
    }

    public void show(
            ServerPlayer player,
            MurderMysteryParticipant participant,
            int remainingSeconds,
            boolean preparationActive
    ) {
        if (player == null || participant == null) {
            return;
        }

        /*
         * Supprime seulement une ancienne version appartenant
         * à ce joueur. Cette méthode n'est pas appelée lors de
         * chaque actualisation.
         */
        hide(player);

        Scoreboard scoreboard =
                player.getScoreboard();

        String objectiveName =
                objectiveName(
                        player.getUUID()
                );

        Objective objective =
                scoreboard.addObjective(
                        objectiveName,
                        ObjectiveCriteria.DUMMY,
                        Component.literal(
                                "MURDER MYSTERY"
                        ).withStyle(
                                ChatFormatting.DARK_RED,
                                ChatFormatting.BOLD
                        ),
                        RenderType.INTEGER,
                        false,
                        null
                );

        scoreboard.setDisplayObjective(
                DisplaySlot.SIDEBAR,
                objective
        );

        List<String> entries =
                new ArrayList<>();

        for (int index = 0;
             index < LINE_AMOUNT;
             index++) {
            String entry =
                    entryName(index);

            entries.add(entry);

            String teamName =
                    teamName(
                            player.getUUID(),
                            index
                    );

            PlayerTeam team =
                    scoreboard.addPlayerTeam(
                            teamName
                    );

            scoreboard.addPlayerToTeam(
                    entry,
                    team
            );

            ScoreHolder holder =
                    ScoreHolder.forNameOnly(
                            entry
                    );

            scoreboard.getOrCreatePlayerScore(
                    holder,
                    objective
            ).set(
                    LINE_AMOUNT - index
            );
        }

        BoardState board =
                new BoardState(
                        scoreboard,
                        objectiveName,
                        entries,
                        remainingSeconds,
                        preparationActive,
                        participant.wounds(),
                        participant.alive(),
                        participant.poisoned(),
                        participant.disconnected(),
                        participant.role(),
                        safeAlias(participant)
                );

        boards.put(
                player.getUUID(),
                board
        );

        update(
                player,
                participant,
                remainingSeconds,
                preparationActive,
                board
        );
    }

    public void hideAll(
            GameInstance instance
    ) {
        if (instance == null) {
            return;
        }

        for (ServerPlayer player :
                instance.onlineParticipants()) {
            hide(player);
        }

        /*
         * Retire aussi les références des joueurs qui seraient
         * actuellement déconnectés.
         */
        boards.clear();
    }

    public void hide(
            ServerPlayer player
    ) {
        if (player == null) {
            return;
        }

        BoardState board =
                boards.remove(
                        player.getUUID()
                );

        Scoreboard scoreboard =
                player.getScoreboard();

        String objectiveName =
                board == null
                        ? objectiveName(
                        player.getUUID()
                )
                        : board.objectiveName();

        Objective objective =
                scoreboard.getObjective(
                        objectiveName
                );

        if (objective != null) {
            Objective displayed =
                    scoreboard.getDisplayObjective(
                            DisplaySlot.SIDEBAR
                    );

            if (displayed == objective) {
                scoreboard.setDisplayObjective(
                        DisplaySlot.SIDEBAR,
                        null
                );
            }

            scoreboard.removeObjective(
                    objective
            );
        }

        removePlayerTeams(
                scoreboard,
                player.getUUID()
        );
    }

    private void update(
            ServerPlayer player,
            MurderMysteryParticipant participant,
            int remainingSeconds,
            boolean preparationActive,
            BoardState previous
    ) {
        Scoreboard scoreboard =
                player.getScoreboard();

        List<Component> lines =
                createLines(
                        participant,
                        remainingSeconds,
                        preparationActive
                );

        for (int index = 0;
             index < LINE_AMOUNT;
             index++) {
            PlayerTeam team =
                    scoreboard.getPlayerTeam(
                            teamName(
                                    player.getUUID(),
                                    index
                            )
                    );

            if (team == null) {
                /*
                 * Le scoreboard a été modifié ou supprimé par
                 * un autre système. Il sera recréé au prochain
                 * passage.
                 */
                boards.remove(
                        player.getUUID()
                );

                return;
            }

            Component newContent =
                    index < lines.size()
                            ? lines.get(index)
                            : Component.empty();

            /*
             * Modifie uniquement le préfixe de la ligne.
             * L'objectif reste affiché en permanence.
             */
            team.setPlayerPrefix(
                    newContent
            );
        }

        boards.put(
                player.getUUID(),
                new BoardState(
                        previous.scoreboard(),
                        previous.objectiveName(),
                        previous.entries(),
                        remainingSeconds,
                        preparationActive,
                        participant.wounds(),
                        participant.alive(),
                        participant.poisoned(),
                        participant.disconnected(),
                        participant.role(),
                        safeAlias(participant)
                )
        );
    }

    private List<Component> createLines(
            MurderMysteryParticipant participant,
            int remainingSeconds,
            boolean preparationActive
    ) {
        List<Component> lines =
                new ArrayList<>();

        lines.add(
                Component.literal(" ")
        );

        lines.add(
                Component.literal(
                        "Identité : "
                ).withStyle(
                        ChatFormatting.GRAY
                ).append(
                        Component.literal(
                                safeAlias(participant)
                        ).withStyle(
                                ChatFormatting.GOLD
                        )
                )
        );

        lines.add(
                Component.literal(
                        "Rôle : "
                ).withStyle(
                        ChatFormatting.GRAY
                ).append(
                        roleComponent(
                                participant.role()
                        )
                )
        );

        lines.add(
                Component.literal(
                        "État : "
                ).withStyle(
                        ChatFormatting.GRAY
                ).append(
                        stateComponent(
                                participant
                        )
                )
        );

        lines.add(
                Component.literal(
                        "Blessures : "
                ).withStyle(
                        ChatFormatting.GRAY
                ).append(
                        Component.literal(
                                String.valueOf(
                                        participant.wounds()
                                )
                        ).withStyle(
                                participant.wounds() > 0
                                        ? ChatFormatting.RED
                                        : ChatFormatting.GREEN
                        )
                )
        );

        lines.add(
                Component.literal("  ")
        );

        lines.add(
                Component.literal(
                        "Temps : "
                ).withStyle(
                        ChatFormatting.GRAY
                ).append(
                        Component.literal(
                                formatTime(
                                        remainingSeconds
                                )
                        ).withStyle(
                                remainingSeconds <= 60
                                        ? ChatFormatting.RED
                                        : ChatFormatting.YELLOW
                        )
                )
        );

        lines.add(
                Component.literal(
                        preparationActive
                                ? "Phase : Préparation"
                                : "Phase : Enquête"
                ).withStyle(
                        preparationActive
                                ? ChatFormatting.AQUA
                                : ChatFormatting.LIGHT_PURPLE
                )
        );

        return lines;
    }

    private Component stateComponent(
            MurderMysteryParticipant participant
    ) {
        if (participant.disconnected()) {
            return Component.literal(
                    "Déconnecté"
            ).withStyle(
                    ChatFormatting.YELLOW
            );
        }

        if (!participant.alive()) {
            return Component.literal(
                    "Mort"
            ).withStyle(
                    ChatFormatting.DARK_RED
            );
        }

        if (participant.poisoned()) {
            return Component.literal(
                    "Empoisonné"
            ).withStyle(
                    ChatFormatting.DARK_GREEN
            );
        }

        return Component.literal(
                "En vie"
        ).withStyle(
                ChatFormatting.GREEN
        );
    }

    private Component roleComponent(
            MurderMysteryRole role
    ) {
        if (role == null) {
            return Component.literal(
                    "Inconnu"
            ).withStyle(
                    ChatFormatting.GRAY
            );
        }

        return role.displayComponent();
    }

    private String safeAlias(
            MurderMysteryParticipant participant
    ) {
        String alias =
                participant.alias();

        return alias == null || alias.isBlank()
                ? "Inconnu"
                : alias;
    }

    private String objectiveName(
            UUID playerId
    ) {
        /*
         * Un nom d'objectif Minecraft ne doit pas être trop long.
         * Huit caractères de l'UUID suffisent car une seule partie
         * n'utilisera jamais deux fois le même identifiant court.
         */
        return OBJECTIVE_PREFIX
                + playerId.toString()
                .replace("-", "")
                .substring(0, 8);
    }

    private String teamName(
            UUID playerId,
            int index
    ) {
        return TEAM_PREFIX
                + playerId.toString()
                .replace("-", "")
                .substring(0, 4)
                + "_"
                + index;
    }

    private void removePlayerTeams(
            Scoreboard scoreboard,
            UUID playerId
    ) {
        String prefix =
                TEAM_PREFIX
                        + playerId.toString()
                        .replace("-", "")
                        .substring(0, 4)
                        + "_";

        List<PlayerTeam> removable =
                scoreboard.getPlayerTeams()
                        .stream()
                        .filter(team ->
                                team.getName()
                                        .startsWith(prefix)
                        )
                        .toList();

        for (PlayerTeam team : removable) {
            scoreboard.removePlayerTeam(
                    team
            );
        }
    }

    private String entryName(
            int index
    ) {
        ChatFormatting[] colors = {
                ChatFormatting.BLACK,
                ChatFormatting.DARK_BLUE,
                ChatFormatting.DARK_GREEN,
                ChatFormatting.DARK_AQUA,
                ChatFormatting.DARK_RED,
                ChatFormatting.DARK_PURPLE,
                ChatFormatting.GOLD,
                ChatFormatting.GRAY
        };

        return colors[index].toString()
                + ChatFormatting.RESET;
    }

    private String formatTime(
            int totalSeconds
    ) {
        int safeSeconds =
                Math.max(
                        0,
                        totalSeconds
                );

        return String.format(
                "%02d:%02d",
                safeSeconds / 60,
                safeSeconds % 60
        );
    }

    private record BoardState(
            Scoreboard scoreboard,
            String objectiveName,
            List<String> entries,
            int remainingSeconds,
            boolean preparationActive,
            int wounds,
            boolean alive,
            boolean poisoned,
            boolean disconnected,
            MurderMysteryRole role,
            String alias
    ) {
    }
}