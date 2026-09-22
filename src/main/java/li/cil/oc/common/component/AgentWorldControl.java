package li.cil.oc.common.component;

import li.cil.oc.api.internal.Agent;
import li.cil.oc.api.internal.Robot;
import li.cil.oc.api.machine.Arguments;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;

public final class AgentWorldControl {
    private AgentWorldControl() { }

    private static BlockPos target(Agent agent, Arguments args) {
        final int side = args.checkInteger(0);
        if (side < 0 || side > 5 || agent instanceof Robot && side != 0 && side != 1 && side != 3) {
            throw new IllegalArgumentException("invalid side");
        }
        final var direction = Direction.from3DDataValue(side);
        return BlockPos.containing(agent.xPosition(), agent.yPosition(), agent.zPosition())
            .relative(agent instanceof Robot robot ? robot.toGlobal(direction) : direction);
    }

    public static Object[] compare(Agent agent, Arguments args) {
        final var pos = target(agent, args);
        final var inventory = agent.mainInventory();
        if (agent.selectedSlot() < 0 || agent.selectedSlot() >= inventory.getContainerSize()) return new Object[]{false};
        final var stack = inventory.getItem(agent.selectedSlot());
        if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem item)) return new Object[]{false};
        // Legacy metadata variants are distinct blocks now; placement state is not an item subtype.
        args.optBoolean(1, false);
        return new Object[]{agent.world() != null && agent.world().isLoaded(pos)
            && agent.world().getBlockState(pos).is(item.getBlock())};
    }

    public static Object[] detect(Agent agent, Arguments args) {
        final var pos = target(agent, args);
        final var world = agent.world();
        if (world == null) return new Object[]{false, "no world"};
        if (!world.isLoaded(pos)) return new Object[]{false, "target not loaded"};
        final var player = agent.player();
        final var nearest = world.getEntities((Entity) null, new AABB(pos), entity -> entity != player).stream()
            .min(java.util.Comparator.comparingDouble(entity -> entity.distanceToSqr(player))).orElse(null);
        if (nearest instanceof LivingEntity || nearest instanceof AbstractMinecart) return new Object[]{true, "entity"};
        final var state = world.getBlockState(pos);
        if (state.isAir()) return new Object[]{false, "air"};
        // Waterlogged solid blocks remain solid; only a fluid block is a liquid obstacle.
        final boolean liquid = state.getBlock() instanceof LiquidBlock;
        if (liquid || state.canBeReplaced()) {
            final var event = new BlockEvent.BreakEvent(world, pos, state, player);
            NeoForge.EVENT_BUS.post(event);
            return new Object[]{event.isCanceled(), liquid ? "liquid" : "replaceable"};
        }
        return new Object[]{true, state.getCollisionShape(world, pos).isEmpty() ? "passable" : "solid"};
    }
}
