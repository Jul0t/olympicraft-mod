package fr.olympicraft.message;

import fr.olympicraft.config.OlympicraftConfigManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.LinkedHashMap;
import java.util.Map;

public final class MessageService {

    private final OlympicraftConfigManager configs;

    public MessageService(
            OlympicraftConfigManager configs
    ) {
        this.configs = configs;
    }

    public MutableComponent render(
            String key,
            Map<String, String> placeholders,
            boolean includePrefix
    ) {
        String message = configs.messages().value(key);

        for (Map.Entry<String, String> entry :
                placeholders.entrySet()) {
            message = message.replace(
                    "%" + entry.getKey() + "%",
                    entry.getValue()
            );
        }

        MutableComponent result = parse(message);

        if (!includePrefix) {
            return result;
        }

        return parse(configs.messages().prefix)
                .append(result);
    }

    public MutableComponent render(
            String key,
            boolean includePrefix
    ) {
        return render(key, Map.of(), includePrefix);
    }

    public void send(
            CommandSourceStack source,
            String key,
            Map<String, String> placeholders,
            boolean includePrefix
    ) {
        MutableComponent component = render(
                key,
                placeholders,
                includePrefix
        );

        source.sendSuccess(() -> component, false);
    }

    public void send(
            CommandSourceStack source,
            String key
    ) {
        send(source, key, Map.of(), true);
    }

    public MutableComponent parse(String input) {
        MutableComponent root = Component.empty();

        if (input == null || input.isEmpty()) {
            return root;
        }

        Map<String, ChatFormatting> formats =
                formattingTags();

        StringBuilder text = new StringBuilder();
        ChatFormatting current = ChatFormatting.WHITE;

        for (int index = 0; index < input.length(); index++) {
            if (input.charAt(index) == '<') {
                int closing = input.indexOf('>', index);

                if (closing >= 0) {
                    String tag = input.substring(
                            index + 1,
                            closing
                    ).toLowerCase();

                    ChatFormatting formatting =
                            formats.get(tag);

                    boolean resetTag =
                            tag.equals("reset")
                                    || tag.startsWith("/");

                    if (formatting != null || resetTag) {
                        append(root, text, current);
                        text.setLength(0);

                        current = resetTag
                                ? ChatFormatting.WHITE
                                : formatting;

                        index = closing;
                        continue;
                    }
                }
            }

            text.append(input.charAt(index));
        }

        append(root, text, current);
        return root;
    }

    private void append(
            MutableComponent root,
            StringBuilder text,
            ChatFormatting formatting
    ) {
        if (text.isEmpty()) {
            return;
        }

        root.append(
                Component.literal(text.toString())
                        .withStyle(formatting)
        );
    }

    private Map<String, ChatFormatting> formattingTags() {
        Map<String, ChatFormatting> values =
                new LinkedHashMap<>();

        values.put("black", ChatFormatting.BLACK);
        values.put("dark_blue", ChatFormatting.DARK_BLUE);
        values.put("dark_green", ChatFormatting.DARK_GREEN);
        values.put("dark_aqua", ChatFormatting.DARK_AQUA);
        values.put("dark_red", ChatFormatting.DARK_RED);
        values.put("dark_purple", ChatFormatting.DARK_PURPLE);
        values.put("gold", ChatFormatting.GOLD);
        values.put("gray", ChatFormatting.GRAY);
        values.put("dark_gray", ChatFormatting.DARK_GRAY);
        values.put("blue", ChatFormatting.BLUE);
        values.put("green", ChatFormatting.GREEN);
        values.put("aqua", ChatFormatting.AQUA);
        values.put("red", ChatFormatting.RED);
        values.put("light_purple", ChatFormatting.LIGHT_PURPLE);
        values.put("yellow", ChatFormatting.YELLOW);
        values.put("white", ChatFormatting.WHITE);

        values.put("obfuscated", ChatFormatting.OBFUSCATED);
        values.put("bold", ChatFormatting.BOLD);
        values.put("strikethrough", ChatFormatting.STRIKETHROUGH);
        values.put("underline", ChatFormatting.UNDERLINE);
        values.put("italic", ChatFormatting.ITALIC);

        return values;
    }

    public static Map<String, String> placeholders(
            String... values
    ) {
        if (values.length % 2 != 0) {
            throw new IllegalArgumentException(
                    "Les placeholders doivent être fournis "
                            + "par paires clé/valeur."
            );
        }

        Map<String, String> result = new LinkedHashMap<>();

        for (int index = 0; index < values.length; index += 2) {
            result.put(values[index], values[index + 1]);
        }

        return result;
    }

    public void sendInfo(
            CommandSourceStack source,
            String message
    ) {
        source.sendSuccess(
                () -> parse(
                        configs.messages().prefix
                                + "<gray>"
                                + message
                                + "</gray>"
                ),
                false
        );
    }

    public void sendSuccess(
            CommandSourceStack source,
            String message
    ) {
        source.sendSuccess(
                () -> parse(
                        configs.messages().prefix
                                + "<green>"
                                + message
                                + "</green>"
                ),
                false
        );
    }

    public void sendWarning(
            CommandSourceStack source,
            String message
    ) {
        source.sendSuccess(
                () -> parse(
                        configs.messages().prefix
                                + "<yellow>"
                                + message
                                + "</yellow>"
                ),
                false
        );
    }

    public void sendError(
            CommandSourceStack source,
            String message
    ) {
        source.sendFailure(
                parse(
                        configs.messages().prefix
                                + "<red>"
                                + message
                                + "</red>"
                )
        );
    }

}
