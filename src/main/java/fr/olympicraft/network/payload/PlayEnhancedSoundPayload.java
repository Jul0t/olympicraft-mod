package fr.olympicraft.network.payload;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record PlayEnhancedSoundPayload(
        String soundId,
        float volume,
        float pitch
) implements CustomPacketPayload {

    public static final Type<PlayEnhancedSoundPayload> TYPE =
            new Type<>(
                    ResourceLocation.fromNamespaceAndPath(
                            "olympicraft",
                            "play_enhanced_sound"
                    )
            );

    public static final StreamCodec<
            FriendlyByteBuf,
            PlayEnhancedSoundPayload
            > STREAM_CODEC =
            StreamCodec.of(
                    PlayEnhancedSoundPayload::write,
                    PlayEnhancedSoundPayload::read
            );

    private static void write(
            FriendlyByteBuf buffer,
            PlayEnhancedSoundPayload payload
    ) {
        buffer.writeUtf(
                payload.soundId,
                256
        );

        buffer.writeFloat(
                payload.volume
        );

        buffer.writeFloat(
                payload.pitch
        );
    }

    private static PlayEnhancedSoundPayload read(
            FriendlyByteBuf buffer
    ) {
        return new PlayEnhancedSoundPayload(
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