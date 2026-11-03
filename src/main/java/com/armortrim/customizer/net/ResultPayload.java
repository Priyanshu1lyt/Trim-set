package com.armortrim.customizer.net;

import com.armortrim.customizer.ArmorTrimCustomizer;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ResultPayload(String message) implements CustomPayload {
    public static final CustomPayload.Id<ResultPayload> ID =
            new CustomPayload.Id<>(Identifier.of(ArmorTrimCustomizer.MOD_ID, "result"));
    public static final PacketCodec<ByteBuf, ResultPayload> CODEC =
            PacketCodecs.STRING.xmap(ResultPayload::new, ResultPayload::message);

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
