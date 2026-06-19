package li.cil.oc.api.event;

import li.cil.oc.api.internal.Agent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.ICancellableEvent;

public abstract class RobotPlaceBlockEvent extends RobotEvent {
    public final ItemStack stack;
    public final Level world;
    public final BlockPos pos;

    protected RobotPlaceBlockEvent(final Agent agent, final ItemStack stack, final Level world, final BlockPos pos) {
        super(agent);
        this.stack = stack;
        this.world = world;
        this.pos = pos;
    }

    public static class Pre extends RobotPlaceBlockEvent implements ICancellableEvent {
        public Pre(final Agent agent, final ItemStack stack, final Level world, final BlockPos pos) {
            super(agent, stack, world, pos);
        }
    }

    public static class Post extends RobotPlaceBlockEvent {
        public Post(final Agent agent, final ItemStack stack, final Level world, final BlockPos pos) {
            super(agent, stack, world, pos);
        }
    }
}
