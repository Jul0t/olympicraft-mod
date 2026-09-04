package fr.olympicraft.network.payload;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record OpenEnhancedTestScreenPayload(
        String title,
        String message
) implements CustomPacketPayload {

    public static final Type<OpenEnhancedTestScreenPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            "olympicraft",
                            "open_enhanced_test_screen"
                    )
            );

    public static final StreamCodec<
            FriendlyByteBuf,
            OpenEnhancedTestScreenPayload
            > STREAM_CODEC =
            StreamCodec.of(
                    OpenEnhancedTestScreenPayload::write,
                    OpenEnhancedTestScreenPayload::read
            );

    private static void write(
            FriendlyByteBuf buffer,
            OpenEnhancedTestScreenPayload payload
    ) {
        buffer.writeUtf(
                payload.title(),
                128
        );

        buffer.writeUtf(
                payload.message(),
                512
        );
    }

    private static OpenEnhancedTestScreenPayload read(
            FriendlyByteBuf buffer
    ) {
        return new OpenEnhancedTestScreenPayload(
                buffer.readUtf(128),
                buffer.readUtf(512)
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}