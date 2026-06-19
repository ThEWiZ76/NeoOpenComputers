package li.cil.oc.api.event;

import li.cil.oc.api.driver.item.UpgradeRenderer;
import li.cil.oc.api.internal.Robot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

final class RobotRenderEventApiTest {
    @Test
    void robotRenderEventIsCancelableAndCarriesMountPoints() {
        RobotRenderEvent.MountPoint mountPoint = new RobotRenderEvent.MountPoint("top_left");
        RobotRenderEvent event = new RobotRenderEvent(null, new RobotRenderEvent.MountPoint[]{mountPoint});

        assertInstanceOf(Event.class, event);
        assertInstanceOf(ICancellableEvent.class, event);
        assertSame(mountPoint, event.mountPoints[0]);
        assertEquals("top_left", mountPoint.name);
        assertInstanceOf(Vector3f.class, mountPoint.offset);
        assertInstanceOf(Vector4f.class, mountPoint.rotation);
    }

    @Test
    void unnamedMountPointKeepsNullName() {
        RobotRenderEvent.MountPoint mountPoint = new RobotRenderEvent.MountPoint();

        assertNull(mountPoint.name);
    }

    @Test
    void upgradeRendererUsesModernItemStackAndRobotRenderMountPoint() throws NoSuchMethodException {
        Method preferred = UpgradeRenderer.class.getMethod("computePreferredMountPoint", ItemStack.class, Robot.class, Set.class);
        Method render = UpgradeRenderer.class.getMethod("render", ItemStack.class, RobotRenderEvent.MountPoint.class, Robot.class, float.class);

        assertEquals(String.class, preferred.getReturnType());
        assertEquals(void.class, render.getReturnType());
    }

    @Test
    void upgradeMountPointNamesMatchOpenComputersConstants() {
        assertEquals("none", UpgradeRenderer.MountPointName.None);
        assertEquals("any", UpgradeRenderer.MountPointName.Any);
        assertEquals("top_left", UpgradeRenderer.MountPointName.TopLeft);
        assertEquals("bottom_front", UpgradeRenderer.MountPointName.BottomFront);
    }
}
