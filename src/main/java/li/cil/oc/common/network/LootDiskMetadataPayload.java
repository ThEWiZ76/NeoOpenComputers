package li.cil.oc.common.network;

import li.cil.oc.NeoOpenComputers;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** An empty stack starts a new connection's metadata snapshot. */
public record LootDiskMetadataPayload(ItemStack stack) implements CustomPacketPayload {
    public static final Type<LootDiskMetadataPayload> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "loot_disk_metadata"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LootDiskMetadataPayload> STREAM_CODEC = StreamCodec.composite(
        ItemStack.OPTIONAL_STREAM_CODEC, LootDiskMetadataPayload::stack, LootDiskMetadataPayload::new);

    public LootDiskMetadataPayload {
        stack = stack == null ? ItemStack.EMPTY : stack.copyWithCount(1);
    }

    @Override public ItemStack stack() { return stack.copy(); }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
