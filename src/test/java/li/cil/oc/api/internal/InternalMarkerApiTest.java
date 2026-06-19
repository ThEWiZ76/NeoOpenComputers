package li.cil.oc.api.internal;

import li.cil.oc.api.component.RackMountable;
import li.cil.oc.api.machine.MachineHost;
import li.cil.oc.api.network.Environment;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.SidedEnvironment;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class InternalMarkerApiTest {
    @Test
    void adapterAndCaseUseModernContainerMarker() {
        assertTrue(Environment.class.isAssignableFrom(Adapter.class));
        assertTrue(Container.class.isAssignableFrom(Adapter.class));

        assertTrue(Environment.class.isAssignableFrom(Case.class));
        assertTrue(EnvironmentHost.class.isAssignableFrom(Case.class));
        assertTrue(MachineHost.class.isAssignableFrom(Case.class));
        assertTrue(Colored.class.isAssignableFrom(Case.class));
        assertTrue(Rotatable.class.isAssignableFrom(Case.class));
        assertTrue(Tiered.class.isAssignableFrom(Case.class));
        assertTrue(Container.class.isAssignableFrom(Case.class));
    }

    @Test
    void microcontrollerRackAndServerExposeExpectedHostContracts() throws NoSuchMethodException {
        Method getMountableData = Rack.class.getMethod("getMountableData", int.class);

        assertTrue(Environment.class.isAssignableFrom(Microcontroller.class));
        assertTrue(EnvironmentHost.class.isAssignableFrom(Microcontroller.class));
        assertTrue(MachineHost.class.isAssignableFrom(Microcontroller.class));
        assertTrue(Rotatable.class.isAssignableFrom(Microcontroller.class));
        assertTrue(Tiered.class.isAssignableFrom(Microcontroller.class));

        assertTrue(SidedEnvironment.class.isAssignableFrom(Rack.class));
        assertTrue(EnvironmentHost.class.isAssignableFrom(Rack.class));
        assertTrue(Rotatable.class.isAssignableFrom(Rack.class));
        assertTrue(Container.class.isAssignableFrom(Rack.class));
        assertEquals(CompoundTag.class, getMountableData.getReturnType());

        assertTrue(EnvironmentHost.class.isAssignableFrom(Server.class));
        assertTrue(MachineHost.class.isAssignableFrom(Server.class));
        assertTrue(Tiered.class.isAssignableFrom(Server.class));
        assertTrue(RackMountable.class.isAssignableFrom(Server.class));
    }

    @Test
    void robotUsesModernWorldlyContainerMarkerAndMethods() throws NoSuchMethodException {
        Method getComponentInSlot = Robot.class.getMethod("getComponentInSlot", int.class);

        assertTrue(Agent.class.isAssignableFrom(Robot.class));
        assertTrue(Environment.class.isAssignableFrom(Robot.class));
        assertTrue(EnvironmentHost.class.isAssignableFrom(Robot.class));
        assertTrue(Tiered.class.isAssignableFrom(Robot.class));
        assertTrue(WorldlyContainer.class.isAssignableFrom(Robot.class));
        assertEquals(Environment.class, getComponentInSlot.getReturnType());
    }
}
