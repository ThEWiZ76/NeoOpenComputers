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
    private appeng.api.parts.IPart part;

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
    CraftingTarget copy() { final var copy = new CraftingTarget(data); copy.tile = tile; copy.part = part; return copy; }
    void part(final String id) {
        data.putString("part", id);
        if (tile instanceof appeng.api.parts.IPartHost host) part = host.getPart(Direction.from3DDataValue(data.getInt("side")));
    }

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
            part = null;
            throw new IllegalStateException("waiting for ae network to load");
        }
        final var current = world.getBlockEntity(pos);
        if (current == null || tile != null && current != tile
            || !BuiltInRegistries.BLOCK.getKey(current.getBlockState().getBlock()).toString().equals(data.getString("block"))) {
            throw new IllegalStateException("no controller");
        }
        tile = current;
        if (data.contains("part")) {
            final var currentPart = current instanceof appeng.api.parts.IPartHost host ? host.getPart(Direction.from3DDataValue(data.getInt("side"))) : null;
            if (currentPart == null || part != null && part != currentPart
                || !BuiltInRegistries.ITEM.getKey(currentPart.getPartItem().asItem()).toString().equals(data.getString("part"))) throw new IllegalStateException("no controller");
            part = currentPart;
            if (part.getGridNode() == null) throw new IllegalStateException("no ae grid");
            return part.getGridNode();
        }
        final var host = world.getCapability(AECapabilities.IN_WORLD_GRID_NODE_HOST, pos, null);
        // AE2's null face is not an internal-node selector. IActionHost exposes that node explicitly.
        final var node = data.getInt("side") < 0 && current instanceof appeng.api.networking.security.IActionHost actionHost
            ? actionHost.getActionableNode() : host == null ? null : host.getGridNode(Direction.from3DDataValue(data.getInt("side")));
        if (node == null) throw new IllegalStateException("no ae grid");
        return node;
    }

    net.minecraft.world.level.Level world() { node(); return tile.getLevel(); }
    BlockPos position() { return BlockPos.of(data.getLong("position")); }
    boolean loadedIn(final net.minecraft.server.level.ServerLevel world) {
        return world.dimension().location().toString().equals(data.getString("dimension")) && world.hasChunkAt(position());
    }
}
