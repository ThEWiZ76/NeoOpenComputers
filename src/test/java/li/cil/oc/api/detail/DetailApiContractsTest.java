package li.cil.oc.api.detail;

import com.typesafe.config.Config;
import li.cil.oc.api.API;
import li.cil.oc.api.driver.Converter;
import li.cil.oc.api.driver.DriverBlock;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.driver.EnvironmentProvider;
import li.cil.oc.api.driver.InventoryProvider;
import li.cil.oc.api.machine.Machine;
import li.cil.oc.api.machine.MachineHost;
import li.cil.oc.api.manual.ImageRenderer;
import li.cil.oc.api.fs.Label;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.nanomachines.Controller;
import li.cil.oc.api.network.EnvironmentHost;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.Callable;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

@SuppressWarnings("deprecation")
final class DetailApiContractsTest {
    @Test
    void apiConfigKeepsUpstreamTypesafeConfigType() throws NoSuchFieldException {
        assertEquals(Config.class, API.class.getField("config").getType());
    }

    @Test
    void driverApiUsesModernMinecraftAndNeoForgeTypes() throws NoSuchMethodException {
        Method blockDriver = DriverAPI.class.getMethod("driverFor", Level.class, BlockPos.class, Direction.class);
        Method itemDriver = DriverAPI.class.getMethod("driverFor", ItemStack.class, Class.class);
        Method itemHandler = DriverAPI.class.getMethod("itemHandlerFor", ItemStack.class, Player.class);

        assertArrayEquals(new Class<?>[]{Level.class, BlockPos.class, Direction.class}, blockDriver.getParameterTypes());
        assertEquals(DriverBlock.class, blockDriver.getReturnType());
        assertArrayEquals(new Class<?>[]{ItemStack.class, Class.class}, itemDriver.getParameterTypes());
        assertEquals(DriverItem.class, itemDriver.getReturnType());
        assertArrayEquals(new Class<?>[]{ItemStack.class, Player.class}, itemHandler.getParameterTypes());
        assertEquals(IItemHandler.class, itemHandler.getReturnType());
    }

    @Test
    void itemApiUsesModernDyeColorAndItemStackTypes() throws NoSuchMethodException {
        Method getByStack = ItemAPI.class.getMethod("get", ItemStack.class);
        Method registerFloppy = ItemAPI.class.getMethod("registerFloppy", String.class, DyeColor.class, Callable.class, boolean.class);
        Method registerEeprom = ItemAPI.class.getMethod("registerEEPROM", String.class, byte[].class, byte[].class, boolean.class);

        assertEquals(ItemInfo.class, getByStack.getReturnType());
        assertArrayEquals(new Class<?>[]{String.class, DyeColor.class, Callable.class, boolean.class}, registerFloppy.getParameterTypes());
        assertEquals(ItemStack.class, registerFloppy.getReturnType());
        assertEquals(ItemStack.class, registerEeprom.getReturnType());
    }

    @Test
    void machineApiExposesArchitectureRegistryAndFactory() throws NoSuchMethodException {
        Method add = MachineAPI.class.getMethod("add", Class.class);
        Method create = MachineAPI.class.getMethod("create", MachineHost.class);

        assertArrayEquals(new Class<?>[]{Class.class}, add.getParameterTypes());
        assertEquals(Machine.class, create.getReturnType());
        assertEquals(Collection.class, MachineAPI.class.getMethod("architectures").getReturnType());
        assertEquals(String.class, MachineAPI.class.getMethod("getArchitectureName", Class.class).getReturnType());
    }

    @Test
    void manualApiUsesModernPlayerLevelAndBlockPosTypes() throws NoSuchMethodException {
        Method pathForBlock = ManualAPI.class.getMethod("pathFor", Level.class, BlockPos.class);
        Method openFor = ManualAPI.class.getMethod("openFor", Player.class);

        assertArrayEquals(new Class<?>[]{Level.class, BlockPos.class}, pathForBlock.getParameterTypes());
        assertArrayEquals(new Class<?>[]{Player.class}, openFor.getParameterTypes());
        assertEquals(ImageRenderer.class, ManualAPI.class.getMethod("imageFor", String.class).getReturnType());
    }

    @Test
    void nanomachinesApiUsesModernPlayerType() throws NoSuchMethodException {
        Method hasController = NanomachinesAPI.class.getMethod("hasController", Player.class);
        Method getController = NanomachinesAPI.class.getMethod("getController", Player.class);
        Method installController = NanomachinesAPI.class.getMethod("installController", Player.class);
        Method uninstallController = NanomachinesAPI.class.getMethod("uninstallController", Player.class);

        assertArrayEquals(new Class<?>[]{Player.class}, hasController.getParameterTypes());
        assertEquals(Controller.class, getController.getReturnType());
        assertEquals(Controller.class, installController.getReturnType());
        assertArrayEquals(new Class<?>[]{Player.class}, uninstallController.getParameterTypes());
    }

    @Test
    void fileSystemApiKeepsDeprecatedManagedEnvironmentConvenienceOverloads() throws NoSuchMethodException {
        assertDeprecatedManagedEnvironmentOverload(Label.class, EnvironmentHost.class, String.class);
        assertDeprecatedManagedEnvironmentOverload(String.class, EnvironmentHost.class, String.class);
        assertDeprecatedManagedEnvironmentOverload(Label.class);
        assertDeprecatedManagedEnvironmentOverload(String.class);
        assertDeprecatedManagedEnvironmentOverload();
    }

    @Test
    void driverApiCanBeImplementedByRegistries() {
        TestDriverAPI api = new TestDriverAPI();
        DriverBlock block = new TestDriverBlock();
        DriverItem item = new TestDriverItem();
        Converter converter = (value, output) -> {};
        EnvironmentProvider environmentProvider = stack -> String.class;
        InventoryProvider inventoryProvider = new TestInventoryProvider();

        api.add(block);
        api.add(item);
        api.add(converter);
        api.add(environmentProvider);
        api.add(inventoryProvider);

        assertSame(block, api.driverFor(null, BlockPos.ZERO, Direction.NORTH));
        assertSame(item, api.driverFor(null, EnvironmentHost.class));
        assertEquals(Set.of(String.class), api.environmentsFor(null));
        assertEquals(1, api.itemDrivers().size());
    }

    private static final class TestDriverAPI implements DriverAPI {
        private DriverBlock block;
        private DriverItem item;
        private EnvironmentProvider environmentProvider;

        @Override
        public void add(final DriverBlock driver) {
            block = driver;
        }

        @Override
        public void add(final DriverItem driver) {
            item = driver;
        }

        @Override
        public void add(final Converter converter) {
        }

        @Override
        public void add(final EnvironmentProvider provider) {
            environmentProvider = provider;
        }

        @Override
        public void add(final InventoryProvider provider) {
        }

        @Override
        public DriverBlock driverFor(final Level world, final BlockPos pos, final Direction side) {
            return block;
        }

        @Override
        public DriverItem driverFor(final ItemStack stack, final Class<? extends EnvironmentHost> host) {
            return item;
        }

        @Override
        public DriverItem driverFor(final ItemStack stack) {
            return item;
        }

        @Override
        public Class<?> environmentFor(final ItemStack stack) {
            return environmentProvider.getEnvironment(stack);
        }

        @Override
        public Set<Class<?>> environmentsFor(final ItemStack stack) {
            return Set.of(environmentFor(stack));
        }

        @Override
        public IItemHandler itemHandlerFor(final ItemStack stack, final Player player) {
            return null;
        }

        @Override
        public Collection<DriverItem> itemDrivers() {
            return Set.of(item);
        }
    }

    private static void assertDeprecatedManagedEnvironmentOverload(final Class<?>... tailParameters) throws NoSuchMethodException {
        final Class<?>[] parameters = new Class<?>[tailParameters.length + 1];
        parameters[0] = li.cil.oc.api.fs.FileSystem.class;
        System.arraycopy(tailParameters, 0, parameters, 1, tailParameters.length);

        final Method method = FileSystemAPI.class.getMethod("asManagedEnvironment", parameters);

        assertEquals(ManagedEnvironment.class, method.getReturnType());
        assertSame(Deprecated.class, method.getAnnotation(Deprecated.class).annotationType());
    }

    private static final class TestDriverBlock implements DriverBlock {
        @Override
        public boolean worksWith(final Level world, final BlockPos pos, final Direction side) {
            return false;
        }

        @Override
        public ManagedEnvironment createEnvironment(final Level world, final BlockPos pos, final Direction side) {
            return null;
        }
    }

    private static final class TestDriverItem implements DriverItem {
        @Override
        public boolean worksWith(final ItemStack stack) {
            return true;
        }

        @Override
        public ManagedEnvironment createEnvironment(final ItemStack stack, final EnvironmentHost host) {
            return null;
        }

        @Override
        public String slot(final ItemStack stack) {
            return "test";
        }

        @Override
        public int tier(final ItemStack stack) {
            return 0;
        }

        @Override
        public CompoundTag dataTag(final ItemStack stack) {
            return null;
        }
    }

    private static final class TestInventoryProvider implements InventoryProvider {
        @Override
        public boolean worksWith(final ItemStack stack, final Player player) {
            return false;
        }

        @Override
        public net.minecraft.world.Container getInventory(final ItemStack stack, final Player player) {
            return null;
        }
    }
}
