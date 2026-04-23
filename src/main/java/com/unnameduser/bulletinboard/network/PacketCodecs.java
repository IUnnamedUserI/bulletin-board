package com.unnameduser.bulletinboard.network;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;

public class PacketCodecs {
    public static final PacketCodec<RegistryByteBuf, Integer> INTEGER =
            new PacketCodec<>() {
                @Override
                public Integer decode(RegistryByteBuf buf) {
                    return buf.readInt();
                }

                @Override
                public void encode(RegistryByteBuf buf, Integer value) {
                    buf.writeInt(value);
                }
            };

    public static final PacketCodec<RegistryByteBuf, NbtCompound> NBT_COMPOUND =
            new PacketCodec<>() {
                @Override
                public NbtCompound decode(RegistryByteBuf buf) {
                    return buf.readNbt();
                }

                @Override
                public void encode(RegistryByteBuf buf, NbtCompound value) {
                    buf.writeNbt(value);
                }
            };
}