package li.cil.oc.common.component;

import li.cil.oc.api.internal.Agent;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Context;
import li.cil.oc.common.ModSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.item.ItemTossEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.items.ItemHandlerHelper;

public final class AgentInventoryWorldControl {
    private AgentInventoryWorldControl() { }

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
        var handler = world.getCapability(Capabilities.ItemHandler.BLOCK, target, side.getOpposite());
        Entity inventoryEntity = null;
        if (handler == null) {
            for (final var entity : world.getEntities((Entity) null, new AABB(target), entity -> entity.isAlive() && entity != player)) {
                handler = entity.getCapability(Capabilities.ItemHandler.ENTITY_AUTOMATION, side.getOpposite());
                if (handler != null) { inventoryEntity = entity; break; }
            }
        }
        boolean allowed = true;
        if (handler != null) {
            if (inventoryEntity == null) {
                final var event = new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, target,
                    new BlockHitResult(Vec3.atCenterOf(target), side.getOpposite(), target, false));
                NeoForge.EVENT_BUS.post(event);
                allowed = !event.isCanceled() && event.getUseBlock() != TriState.FALSE;
            } else {
                final var event = new PlayerInteractEvent.EntityInteract(player, InteractionHand.MAIN_HAND, inventoryEntity);
                NeoForge.EVENT_BUS.post(event);
                allowed = !event.isCanceled();
            }
        }
        if (handler != null && allowed) {
            final var remainder = ItemHandlerHelper.insertItemStacked(handler, source.copyWithCount(amount), false);
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
