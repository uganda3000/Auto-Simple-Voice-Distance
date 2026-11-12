package com.example.autosimplevoicedistance.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record VolumePayload(float targetDistance) implements CustomPacketPayload {
    public static final Type<VolumePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath("autosimplevoicedistance", "volume_sync")
    );

    public static final StreamCodec<ByteBuf, VolumePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, VolumePayload::targetDistance,
            VolumePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
