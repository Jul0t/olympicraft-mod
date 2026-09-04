package fr.olympicraft.network.payload;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record StartEnhancedLoopSoundPayload(
        String soundId,
        float volume,
        float pitch
) implements CustomPacketPayload {

    public static final Type<
            StartEnhancedLoopSoundPayload
            > TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            "olympicraft",
                            "start_enhanced_loop_sound"
                    )
            );

    public static final StreamCodec<
            FriendlyByteBuf,
            StartEnhancedLoopSoundPayload
            > STREAM_CODEC =
            StreamCodec.of(
                    StartEnhancedLoopSoundPayload::write,
                    StartEnhancedLoopSoundPayload::read
            );

    private static void write(
            FriendlyByteBuf buffer,
            StartEnhancedLoopSoundPayload payload
    ) {
        buffer.writeUtf(
                payload.soundId(),
                256
        );

        buffer.writeFloat(
                payload.volume()
        );

        buffer.writeFloat(
                payload.pitch()
        );
    }

    private static StartEnhancedLoopSoundPayload read(
            FriendlyByteBuf buffer
    ) {
        return new StartEnhancedLoopSoundPayload(
                buffer.readUtf(256),
                buffer.readFloat(),
                buffer.readFloat()
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}