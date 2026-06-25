package li.cil.oc.api.prefab;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class LegacyPrefabAliasesTest {
    @Test
    void deprecatedManagedEnvironmentAliasExtendsModernBaseClass() throws ClassNotFoundException {
        Class<?> managedEnvironment = Class.forName("li.cil.oc.api.prefab.ManagedEnvironment");

        assertEquals(AbstractManagedEnvironment.class, managedEnvironment.getSuperclass());
    }

    @Test
    void deprecatedDriverBlockAliasExtendsModernSidedBlockBaseClass() throws ClassNotFoundException {
        Class<?> driverBlock = Class.forName("li.cil.oc.api.prefab.DriverBlock");

        assertEquals(DriverSidedBlock.class, driverBlock.getSuperclass());
    }

    @Test
    void deprecatedDriverTileEntityAliasExtendsModernSidedTileEntityBaseClass() throws ClassNotFoundException {
        Class<?> driverTileEntity = Class.forName("li.cil.oc.api.prefab.DriverTileEntity");

        assertEquals(DriverSidedTileEntity.class, driverTileEntity.getSuperclass());
    }
}
