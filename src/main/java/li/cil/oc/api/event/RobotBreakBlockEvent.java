package li.cil.oc.api.event;

import li.cil.oc.api.internal.Agent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.ICancellableEvent;

public abstract class RobotBreakBlockEvent extends RobotEvent {
    protected RobotBreakBlockEvent(final Agent agent) {
        super(agent);
    }

    public static class Pre extends RobotBreakBlockEvent implements ICancellableEvent {
        public final Level world;
        public final BlockPos pos;
        private double breakTime;

        public Pre(final Agent agent, final Level world, final BlockPos pos, final double breakTime) {
            super(agent);
            this.world = world;
            this.pos = pos;
            this.breakTime = breakTime;
        }

        public void setBreakTime(final double breakTime) {
            this.breakTime = Math.max(0.05, breakTime);
        }

        public double getBreakTime() {
            return breakTime;
        }
    }

    public static class Post extends RobotBreakBlockEvent {
        public final double experience;

        public Post(final Agent agent, final double experience) {
            super(agent);
            this.experience = experience;
        }
    }
}
