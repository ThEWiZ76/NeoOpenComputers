package li.cil.oc.common.component;

import li.cil.oc.api.internal.Agent;
import li.cil.oc.api.internal.Robot;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Context;
import li.cil.oc.common.ModSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

public final class AgentInventoryWorldControl {
    private AgentInventoryWorldControl() { }

    private record InventorySource(IItemHandler handler, Entity entity) { }

    private static InventorySource source(Agent agent, BlockPos pos, Direction side) {
        final var world = agent.world();
        final var handler = world.getCapability(Capabilities.ItemHandler.BLOCK, pos, side);
        if (handler != null) return new InventorySource(handler, null);
        for (final var entity : world.getEntities((Entity) null, new AABB(pos), entity -> entity.isAlive() && entity != agent.player())) {
            final var entityHandler = entity.getCapability(Capabilities.ItemHandler.ENTITY_AUTOMATION, side);
            if (entityHandler != null) return new InventorySource(entityHandler, entity);
        }
        return null;
    }

    private static boolean mayInteract(Agent agent, InventorySource source, BlockPos pos, Direction side) {
        final var player = agent.player();
        if (source.entity == null) {
            final var event = new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, pos,
                new BlockHitResult(Vec3.atCenterOf(pos), side, pos, false));
            NeoForge.EVENT_BUS.post(event);
            return !event.isCanceled() && event.getUseBlock() != TriState.FALSE;
        }
        final var event = new PlayerInteractEvent.EntityInteract(player, InteractionHand.MAIN_HAND, source.entity);
        NeoForge.EVENT_BUS.post(event);
        return !event.isCanceled();
    }

    private static ItemStack insert(Agent agent, ItemStack stack, boolean simulate) {
        final Container inventory = agent.mainInventory();
        final var handler = new InvWrapper(inventory);
        ItemStack remaining = stack.copy();
        for (int index = 0; index < inventory.getContainerSize() && !remaining.isEmpty(); index++) {
            final int slot = (agent.selectedSlot() + index) % inventory.getContainerSize();
            remaining = handler.insertItem(slot, remaining, simulate);
        }
        return remaining;
    }

    private static Direction inventorySide(Arguments args, Direction facing) {
        if (args.count() <= 3) return facing.getOpposite();
        final int side = args.checkInteger(3);
        if (side < 0 || side > 5) throw new IllegalArgumentException("invalid side");
        return Direction.from3DDataValue(side);
    }

    private static int transferCount(Arguments args) {
        return args.count() <= 2 || args.checkAny(2) == null ? 64 : Math.clamp(args.checkInteger(2), 0, 64);
    }

    private static int inventorySlot(InventorySource source, Arguments args) {
        final int slot = args.checkInteger(1) - 1;
        if (slot < 0 || slot >= source.handler.getSlots()) throw new IllegalArgumentException("invalid slot");
        return slot;
    }

    private static InventorySource accessibleSource(Agent agent, BlockPos pos, Direction side) {
        if (agent.world() == null || !agent.world().isLoaded(pos)) return null;
        final var source = source(agent, pos, side);
        if (source == null || source.handler instanceof InvWrapper wrapper && !wrapper.getInv().stillValid(agent.player())) return null;
        return mayInteract(agent, source, pos, side) ? source : null;
    }

    public static Object[] dropIntoSlot(Agent agent, Context context, Arguments args) {
        final var facing = AgentWorldControl.side(agent, args);
        final int count = transferCount(args);
        final var side = inventorySide(args, facing);
        final var inventory = agent.mainInventory();
        final int selected = agent.selectedSlot();
        if (selected < 0 || selected >= inventory.getContainerSize()) return new Object[]{false};
        final var stack = inventory.getItem(selected);
        if (stack.isEmpty()) return new Object[]{false};
        final var target = BlockPos.containing(agent.xPosition(), agent.yPosition(), agent.zPosition()).relative(facing);
        final var destination = accessibleSource(agent, target, side);
        if (destination == null) return new Object[]{null, "no inventory"};
        final int slot = inventorySlot(destination, args);
        final int amount = Math.min(count, stack.getCount());
        if (amount <= 0) return new Object[]{false, "inventory full/invalid slot"};
        final var remainder = destination.handler.insertItem(slot, stack.copyWithCount(amount), false);
        final int moved = amount - remainder.getCount();
        if (moved <= 0) return new Object[]{false, "inventory full/invalid slot"};
        inventory.removeItem(selected, moved);
        inventory.setChanged();
        if (context != null) context.pause(ModSettings.robotDropDelay());
        return new Object[]{true};
    }

    public static Object[] suckFromSlot(Agent agent, Context context, Arguments args) {
        final var facing = AgentWorldControl.side(agent, args);
        final int count = transferCount(args);
        final var side = inventorySide(args, facing);
        final var target = BlockPos.containing(agent.xPosition(), agent.yPosition(), agent.zPosition()).relative(facing);
        final var source = accessibleSource(agent, target, side);
        if (source == null) return new Object[]{null, "no inventory"};
        final int slot = inventorySlot(source, args);
        if (count <= 0) return new Object[]{false};
        final var preview = source.handler.extractItem(slot, count, true);
        final int accepted = preview.getCount() - insert(agent, preview, true).getCount();
        if (accepted <= 0) return new Object[]{false};
        final var extracted = source.handler.extractItem(slot, accepted, false);
        final var remainder = insert(agent, extracted, false);
        final int moved = extracted.getCount() - remainder.getCount();
        // A handler may change between simulation and extraction. Return any excess
        // before falling back to a world drop, so extracted items are never discarded.
        if (!remainder.isEmpty()) {
            final var returned = ItemHandlerHelper.insertItemStacked(source.handler,
                source.handler.insertItem(slot, remainder, false), false);
            if (!returned.isEmpty()) agent.world().addFreshEntity(new ItemEntity(agent.world(),
                target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5, returned));
        }
        if (moved <= 0) return new Object[]{false};
        agent.mainInventory().setChanged();
        if (context != null) context.pause(ModSettings.robotSuckDelay());
        return new Object[]{moved};
    }

    public static Object[] suck(Agent agent, Context context, Arguments args) {
        final var side = AgentWorldControl.side(agent, args);
        final int count = Math.clamp(args.optInteger(1, 64), 0, 64);
        final var world = agent.world();
        final var origin = BlockPos.containing(agent.xPosition(), agent.yPosition(), agent.zPosition());
        final var target = origin.relative(side);
        if (world == null || !world.isLoaded(target)) return new Object[]{false};
        final var source = source(agent, target, side.getOpposite());
        int moved = 0;
        if (count > 0 && source != null && mayInteract(agent, source, target, side.getOpposite())) {
            for (int slot = 0; slot < source.handler.getSlots() && moved == 0; slot++) {
                final var preview = source.handler.extractItem(slot, count, true);
                final int accepted = preview.getCount() - insert(agent, preview, true).getCount();
                if (accepted <= 0) continue;
                final var extracted = source.handler.extractItem(slot, accepted, false);
                final var remainder = insert(agent, extracted, false);
                moved = extracted.getCount() - remainder.getCount();
                if (!remainder.isEmpty()) {
                    final var returned = ItemHandlerHelper.insertItemStacked(source.handler, remainder, false);
                    if (!returned.isEmpty()) world.addFreshEntity(new ItemEntity(world, target.getX() + 0.5,
                        target.getY() + 0.5, target.getZ() + 0.5, returned));
                }
            }
        }
        if (moved == 0) {
            final var items = new java.util.ArrayList<ItemEntity>();
            if (agent instanceof li.cil.oc.api.internal.Drone) items.addAll(world.getEntitiesOfClass(ItemEntity.class, new AABB(origin)));
            items.addAll(world.getEntitiesOfClass(ItemEntity.class, new AABB(target)));
            for (final var item : items) {
                if (item.isRemoved() || item.hasPickUpDelay() || item.getItem().isEmpty()) continue;
                final var player = agent.player();
                if (agent instanceof Robot) {
                    final var event = new ItemEntityPickupEvent.Pre(player, item);
                    NeoForge.EVENT_BUS.post(event);
                    if (event.canPickup() == TriState.FALSE || event.canPickup() != TriState.TRUE
                        && item.getTarget() != null && !item.getTarget().equals(player.getUUID())) continue;
                }
                if (item.isRemoved() || item.getItem().isEmpty()) continue;
                final var original = item.getItem().copy();
                final var remainder = insert(agent, original.copyWithCount(Math.min(64, original.getCount())), false);
                moved = Math.min(64, original.getCount()) - remainder.getCount();
                if (moved <= 0) continue;
                item.getItem().shrink(moved);
                if (agent instanceof Robot) NeoForge.EVENT_BUS.post(new ItemEntityPickupEvent.Post(player, item, original));
                if (item.getItem().isEmpty()) item.discard();
                else item.setItem(item.getItem());
                break;
            }
        }
        if (moved <= 0) return new Object[]{false};
        agent.mainInventory().setChanged();
        if (context != null) context.pause(ModSettings.robotSuckDelay());
        return new Object[]{moved};
    }

    public static Object[] drop(Agent agent, Context context, Arguments args) {
        final var side = AgentWorldControl.side(agent, args);
        final int count = Math.clamp(args.optInteger(1, 64), 0, 64);
        final var inventory = agent.mainInventory();
        final int slot = agent.selectedSlot();
        if (slot < 0 || slot >= inventory.getContainerSize()) return new Object[]{false};
        final var source = inventory.getItem(slot);
        if (source.isEmpty()) return new Object[]{false};
        final var world = agent.world();
        final var origin = BlockPos.containing(agent.xPosition(), agent.yPosition(), agent.zPosition());
        final var target = origin.relative(side);
        if (world == null || !world.isLoaded(target)) return new Object[]{false, "target not loaded"};
        final int amount = Math.min(count, source.getCount());
        final var player = agent.player();
        final var destination = source(agent, target, side.getOpposite());
        if (destination != null && mayInteract(agent, destination, target, side.getOpposite())) {
            final var remainder = ItemHandlerHelper.insertItemStacked(destination.handler, source.copyWithCount(amount), false);
            final int moved = amount - remainder.getCount();
            if (moved <= 0) return new Object[]{false, "inventory full"};
            inventory.removeItem(slot, moved);
        } else if (amount > 0) {
            final var random = world.random;
            final int x = side.getStepX(), y = side.getStepY(), z = side.getStepZ();
            final var item = new ItemEntity(world,
                origin.getX() + 0.5 + 0.1 * (random.nextDouble() - 0.5) + x * 0.65,
                origin.getY() + 0.5 + 0.1 * (random.nextDouble() - 0.5) + y * 0.75 + (x + z) * 0.25,
                origin.getZ() + 0.5 + 0.1 * (random.nextDouble() - 0.5) + z * 0.65,
                source.copyWithCount(amount));
            item.setDeltaMovement(0.0125 * (random.nextDouble() - 0.5) + x * 0.03,
                0.0125 * (random.nextDouble() - 0.5) + y * 0.08 + (x + z) * 0.03,
                0.0125 * (random.nextDouble() - 0.5) + z * 0.03);
            item.setPickUpDelay(15);
            final var event = new ItemTossEvent(item, player);
            NeoForge.EVENT_BUS.post(event);
            if (!event.isCanceled() && world.addFreshEntity(item)) inventory.removeItem(slot, amount);
        }
        inventory.setChanged();
        if (context != null) context.pause(ModSettings.robotDropDelay());
        return new Object[]{true};
    }
}
