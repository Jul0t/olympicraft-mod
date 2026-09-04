package fr.olympicraft.network.payload;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record EnhancedHelloPayload(
        String clientVersion
) implements CustomPacketPayload {

    public static final Type<EnhancedHelloPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            "olympicraft",
                            "enhanced_hello"
                    )
            );

    public static final StreamCodec<
            FriendlyByteBuf,
            EnhancedHelloPayload
            > STREAM_CODEC =
            StreamCodec.of(
                    EnhancedHelloPayload::write,
                    EnhancedHelloPayload::read
            );

    private static void write(
            FriendlyByteBuf buffer,
            EnhancedHelloPayload payload
    ) {
        buffer.writeUtf(
                payload.clientVersion(),
                64
        );
    }

    private static EnhancedHelloPayload read(
            FriendlyByteBuf buffer
    ) {
        return new EnhancedHelloPayload(
                buffer.readUtf(64)
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}