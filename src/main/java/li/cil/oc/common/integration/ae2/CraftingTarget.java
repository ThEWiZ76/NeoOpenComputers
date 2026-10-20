package li.cil.oc.common.integration.ae2;

import appeng.api.AECapabilities;
import appeng.api.networking.IGridNode;
import appeng.api.stacks.AEItemKey;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/** A saved locator, resolving only already-loaded AE2 hosts on the server thread. */
final class CraftingTarget {
    private final CompoundTag data;
    private BlockEntity tile;

    CraftingTarget(final BlockEntity tile, final Direction side, final AEItemKey key) {
        this.tile = tile;
        data = new CompoundTag();
        data.putString("dimension", tile.getLevel().dimension().location().toString());
        data.putLong("position", tile.getBlockPos().asLong());
        data.putInt("side", side == null ? -1 : side.get3DDataValue());
        data.putString("block", BuiltInRegistries.BLOCK.getKey(tile.getBlockState().getBlock()).toString());
        data.put("item", key.toTag(tile.getLevel().registryAccess()));
    }

    CraftingTarget(final CompoundTag data) { this.data = data.copy(); }
    CompoundTag save() { return data.copy(); }
    CraftingTarget copy() { final var copy = new CraftingTarget(data); copy.tile = tile; return copy; }

    AEItemKey item() {
        final var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) throw new IllegalStateException("waiting for ae network to load");
        final var item = AEItemKey.fromTag(server.registryAccess(), data.getCompound("item"));
        if (item == null) throw new IllegalStateException("invalid crafting item");
        return item;
    }

    IGridNode node() {
        final var server = ServerLifecycleHooks.getCurrentServer();
        final var world = server == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION,
            ResourceLocation.parse(data.getString("dimension"))));
        final var pos = BlockPos.of(data.getLong("position"));
        if (world == null || !world.hasChunkAt(pos)) {
            tile = null;
            throw new IllegalStateException("waiting for ae network to load");
        }
        final var current = world.getBlockEntity(pos);
        if (current == null || tile != null && current != tile
            || !BuiltInRegistries.BLOCK.getKey(current.getBlockState().getBlock()).toString().equals(data.getString("block"))) {
            throw new IllegalStateException("no controller");
        }
        tile = current;
        final var host = world.getCapability(AECapabilities.IN_WORLD_GRID_NODE_HOST, pos, null);
        final var node = host == null ? null : host.getGridNode(data.getInt("side") < 0 ? null : Direction.from3DDataValue(data.getInt("side")));
        if (node == null) throw new IllegalStateException("no ae grid");
        return node;
    }

    net.minecraft.world.level.Level world() { node(); return tile.getLevel(); }
    BlockPos position() { return BlockPos.of(data.getLong("position")); }
    boolean loadedIn(final net.minecraft.server.level.ServerLevel world) {
        return world.dimension().location().toString().equals(data.getString("dimension")) && world.hasChunkAt(position());
    }
}
