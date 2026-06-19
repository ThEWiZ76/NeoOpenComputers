package li.cil.oc.api.driver.item;

import li.cil.oc.api.event.RobotRenderEvent;
import li.cil.oc.api.internal.Robot;
import net.minecraft.world.item.ItemStack;

import java.util.Set;

public interface UpgradeRenderer {
    String computePreferredMountPoint(ItemStack stack, Robot robot, Set<String> availableMountPoints);

    void render(ItemStack stack, RobotRenderEvent.MountPoint mountPoint, Robot robot, float partialTick);

    final class MountPointName {
        public static final String None = "none";
        public static final String Any = "any";

        public static final String TopLeft = "top_left";
        public static final String TopRight = "top_right";
        public static final String TopBack = "top_back";
        public static final String BottomLeft = "bottom_left";
        public static final String BottomRight = "bottom_right";
        public static final String BottomBack = "bottom_back";
        public static final String BottomFront = "bottom_front";

        private MountPointName() {
        }
    }
}
