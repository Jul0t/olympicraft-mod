package fr.olympicraft.network.payload;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record StopEnhancedLoopSoundPayload(
        String soundId
) implements CustomPacketPayload {

    public static final Type<
            StopEnhancedLoopSoundPayload
            > TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            "olympicraft",
                            "stop_enhanced_loop_sound"
                    )
            );

    public static final StreamCodec<
            FriendlyByteBuf,
            StopEnhancedLoopSoundPayload
            > STREAM_CODEC =
            StreamCodec.of(
                    StopEnhancedLoopSoundPayload::write,
                    StopEnhancedLoopSoundPayload::read
            );

    private static void write(
            FriendlyByteBuf buffer,
            StopEnhancedLoopSoundPayload payload
    ) {
        buffer.writeUtf(
                payload.soundId(),
                256
        );
    }

    private static StopEnhancedLoopSoundPayload read(
            FriendlyByteBuf buffer
    ) {
        return new StopEnhancedLoopSoundPayload(
                buffer.readUtf(256)
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}