package li.cil.oc.common.gametest;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.api.API;
import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.driver.item.Memory;
import li.cil.oc.api.driver.item.Processor;
import li.cil.oc.api.internal.TextBuffer;
import li.cil.oc.api.machine.Signal;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Node;
import li.cil.oc.common.ItemRegistry;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModEeproms;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.blockentity.ComputerCaseBlockEntity;
import li.cil.oc.common.blockentity.DiskDriveBlockEntity;
import li.cil.oc.common.blockentity.ScreenBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

@GameTestHolder(NeoOpenComputers.MODID)
@PrefixGameTestTemplate(false)
public final class NeoOpenComputersGameTests {
    @GameTest(template = "empty")
    public static void registeredContentAvailable(final GameTestHelper helper) {
        ModBlocks.COMPUTER_CASE_TIER1.get();
        ModBlocks.DISK_DRIVE.get();
        ModBlocks.SCREEN_TIER1.get();
        ModBlocks.SCREEN_TIER2.get();
        ModBlocks.SCREEN_TIER3.get();
        ModBlocks.KEYBOARD.get();
        ModItems.CPU_TIER1.get();
        ModItems.CPU_TIER2.get();
        ModItems.CPU_TIER3.get();
        ModItems.EEPROM.get();
        ModItems.FLOPPY.get();
        ModItems.GRAPHICS_CARD_TIER1.get();
        ModItems.GRAPHICS_CARD_TIER2.get();
        ModItems.GRAPHICS_CARD_TIER3.get();
        ModItems.HDD_TIER1.get();
        ModItems.HDD_TIER2.get();
        ModItems.HDD_TIER3.get();
        ModItems.MEMORY_TIER1.get();
        ModItems.MEMORY_TIER2.get();
        ModItems.MEMORY_TIER3.get();
        ModItems.NETWORK_CARD.get();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void tieredComponentItemsReportTheirTier(final GameTestHelper helper) {
        assertItemTier(helper, new ItemStack(ModItems.CPU_TIER1.get()), 0);
        assertItemTier(helper, new ItemStack(ModItems.CPU_TIER2.get()), 1);
        assertItemTier(helper, new ItemStack(ModItems.CPU_TIER3.get()), 2);
        assertItemTier(helper, new ItemStack(ModItems.MEMORY_TIER1.get()), 0);
        assertItemTier(helper, new ItemStack(ModItems.MEMORY_TIER2.get()), 1);
        assertItemTier(helper, new ItemStack(ModItems.MEMORY_TIER3.get()), 2);
        assertItemTier(helper, new ItemStack(ModItems.GRAPHICS_CARD_TIER1.get()), 0);
        assertItemTier(helper, new ItemStack(ModItems.GRAPHICS_CARD_TIER2.get()), 1);
        assertItemTier(helper, new ItemStack(ModItems.GRAPHICS_CARD_TIER3.get()), 2);
        assertItemTier(helper, new ItemStack(ModItems.HDD_TIER1.get()), 0);
        assertItemTier(helper, new ItemStack(ModItems.HDD_TIER2.get()), 1);
        assertItemTier(helper, new ItemStack(ModItems.HDD_TIER3.get()), 2);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void tieredComponentItemsExposeTierCapabilities(final GameTestHelper helper) {
        assertProcessorComponents(helper, new ItemStack(ModItems.CPU_TIER1.get()), 8);
        assertProcessorComponents(helper, new ItemStack(ModItems.CPU_TIER2.get()), 12);
        assertProcessorComponents(helper, new ItemStack(ModItems.CPU_TIER3.get()), 16);
        assertMemoryAmount(helper, new ItemStack(ModItems.MEMORY_TIER1.get()), 192);
        assertMemoryAmount(helper, new ItemStack(ModItems.MEMORY_TIER2.get()), 384);
        assertMemoryAmount(helper, new ItemStack(ModItems.MEMORY_TIER3.get()), 768);
        assertHardDiskCapacity(helper, new ItemStack(ModItems.HDD_TIER1.get()), 1024L * 1024L);
        assertHardDiskCapacity(helper, new ItemStack(ModItems.HDD_TIER2.get()), 2048L * 1024L);
        assertHardDiskCapacity(helper, new ItemStack(ModItems.HDD_TIER3.get()), 4096L * 1024L);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void tieredScreensExposeTierCapabilities(final GameTestHelper helper) {
        final BlockPos tier1Pos = new BlockPos(0, 1, 0);
        final BlockPos tier2Pos = new BlockPos(1, 1, 0);
        final BlockPos tier3Pos = new BlockPos(2, 1, 0);

        helper.setBlock(tier1Pos, ModBlocks.SCREEN_TIER1.get());
        helper.setBlock(tier2Pos, ModBlocks.SCREEN_TIER2.get());
        helper.setBlock(tier3Pos, ModBlocks.SCREEN_TIER3.get());

        assertScreenTier(helper, helper.getBlockEntity(tier1Pos), 0, 50, 16, TextBuffer.ColorDepth.OneBit);
        assertScreenTier(helper, helper.getBlockEntity(tier2Pos), 1, 80, 25, TextBuffer.ColorDepth.FourBit);
        assertScreenTier(helper, helper.getBlockEntity(tier3Pos), 2, 160, 50, TextBuffer.ColorDepth.EightBit);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void adjacentBlocksJoinSameNetwork(final GameTestHelper helper) {
        final BlockPos computerPos = new BlockPos(1, 1, 1);
        final BlockPos diskDrivePos = new BlockPos(2, 1, 1);

        helper.setBlock(computerPos, ModBlocks.COMPUTER_CASE_TIER1.get());
        helper.setBlock(diskDrivePos, ModBlocks.DISK_DRIVE.get());

        helper.succeedWhen(() -> {
            final ComputerCaseBlockEntity computer = helper.getBlockEntity(computerPos);
            final DiskDriveBlockEntity diskDrive = helper.getBlockEntity(diskDrivePos);
            final Node computerNode = computer.node();
            final Node diskDriveNode = diskDrive.node();
            helper.assertTrue(computerNode != null, "Computer case has no network node");
            helper.assertTrue(diskDriveNode != null, "Disk drive has no network node");
            helper.assertTrue(computerNode.network() != null, "Computer case node is not joined to a network");
            helper.assertTrue(computerNode.network() == diskDriveNode.network(), "Adjacent block nodes are not in the same network");
        });
    }

    @GameTest(template = "empty")
    public static void computerCaseStartsWithCoreComponents(final GameTestHelper helper) {
        final BlockPos computerPos = new BlockPos(1, 1, 1);
        helper.setBlock(computerPos, ModBlocks.COMPUTER_CASE_TIER1.get());

        final ComputerCaseBlockEntity computer = helper.getBlockEntity(computerPos);
        computer.setItem(ComputerCaseBlockEntity.SLOT_CPU, new ItemStack(ModItems.CPU_TIER1.get()));
        computer.setItem(ComputerCaseBlockEntity.SLOT_MEMORY_0, new ItemStack(ModItems.MEMORY_TIER1.get()));
        computer.setItem(ComputerCaseBlockEntity.SLOT_EEPROM, new ItemStack(ModItems.EEPROM.get()));

        helper.assertTrue(computer.toggleMachine(), "Computer case did not start with CPU, memory, and EEPROM");
        helper.succeedWhen(() -> helper.assertTrue(computer.machine().isRunning(), "Computer machine is not running"));
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void computerRunsWithLuaBiosAndOpenOsFloppy(final GameTestHelper helper) {
        final BlockPos screenPos = new BlockPos(0, 1, 1);
        final BlockPos computerPos = new BlockPos(1, 1, 1);
        final BlockPos diskDrivePos = new BlockPos(2, 1, 1);

        helper.setBlock(screenPos, ModBlocks.SCREEN_TIER1.get());
        helper.setBlock(computerPos, ModBlocks.COMPUTER_CASE_TIER1.get());
        helper.setBlock(diskDrivePos, ModBlocks.DISK_DRIVE.get());

        final ScreenBlockEntity screen = helper.getBlockEntity(screenPos);
        final ComputerCaseBlockEntity computer = helper.getBlockEntity(computerPos);
        final DiskDriveBlockEntity diskDrive = helper.getBlockEntity(diskDrivePos);
        final ItemStack openOsFloppy = openOsFloppyStack();
        final DriverItem floppyDriver = Driver.driverFor(openOsFloppy);
        helper.assertTrue(floppyDriver != null, "OpenOS floppy has no item driver");
        helper.assertTrue(API.items instanceof ItemRegistry, "OpenOS item API is not an ItemRegistry");
        final Callable<li.cil.oc.api.fs.FileSystem> floppyFactory = ((ItemRegistry) API.items).floppyFactory(openOsFloppy);
        helper.assertTrue(floppyFactory != null, "OpenOS floppy has no registered filesystem factory");
        helper.assertTrue(callFactory(floppyFactory) != null, "OpenOS floppy factory did not create a filesystem");
        helper.assertTrue(diskDrive.canPlaceItem(DiskDriveBlockEntity.SLOT_FLOPPY, openOsFloppy), "Disk drive does not accept OpenOS floppy");
        diskDrive.setItem(DiskDriveBlockEntity.SLOT_FLOPPY, openOsFloppy);
        computer.setItem(ComputerCaseBlockEntity.SLOT_CARD_0, new ItemStack(ModItems.GRAPHICS_CARD_TIER1.get()));
        computer.setItem(ComputerCaseBlockEntity.SLOT_CPU, new ItemStack(ModItems.CPU_TIER1.get()));
        computer.setItem(ComputerCaseBlockEntity.SLOT_MEMORY_0, new ItemStack(ModItems.MEMORY_TIER1.get()));
        computer.setItem(ComputerCaseBlockEntity.SLOT_EEPROM, luaBiosEepromStack());

        helper.assertTrue(computer.node().network() == diskDrive.node().network(), "Computer and disk drive are not on the same network");
        helper.assertTrue(computer.machine().components().containsValue("filesystem"), "OpenOS floppy filesystem is not visible before boot: " + computer.machine().components() + " diskNeighbors=" + neighborComponents(diskDrive.node()));
        helper.assertTrue(computer.toggleMachine(), "Computer case did not start with Lua BIOS and OpenOS floppy");
        helper.runAtTickTime(80, () -> {
            helper.assertTrue(computer.machine().isRunning(), "Computer stopped while running Lua BIOS and OpenOS: " + computer.machine().lastError());
            helper.assertTrue(computer.machine().components().containsValue("filesystem"), "OpenOS floppy filesystem is not visible");
            helper.assertTrue(computer.machine().components().containsValue("gpu"), "Graphics card component is not visible");
            helper.assertTrue(computer.machine().components().containsValue("screen"), "Screen component is not visible");
            helper.assertTrue(screenHasNonBlankText(screen), "OpenOS did not write visible screen text:\n" + screenText(screen));
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void screenKeyboardSignalsReachComputer(final GameTestHelper helper) {
        final BlockPos screenPos = new BlockPos(0, 1, 1);
        final BlockPos keyboardPos = new BlockPos(0, 1, 2);
        final BlockPos computerPos = new BlockPos(1, 1, 1);

        helper.setBlock(screenPos, ModBlocks.SCREEN_TIER1.get());
        helper.setBlock(keyboardPos, ModBlocks.KEYBOARD.get());
        helper.setBlock(computerPos, ModBlocks.COMPUTER_CASE_TIER1.get());

        final ScreenBlockEntity screen = helper.getBlockEntity(screenPos);
        final ComputerCaseBlockEntity computer = helper.getBlockEntity(computerPos);
        helper.assertTrue(screen.node().network() == computer.node().network(), "Screen and computer are not on the same network");
        screen.keyDown('a', 30, null);
        screen.keyUp('a', 30, null);

        helper.runAtTickTime(5, () -> {
            assertNextSignal(helper, computer, "key_down", 'a', 30);
            assertNextSignal(helper, computer, "key_up", 'a', 30);
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void screenBlockClickSignalsReachComputer(final GameTestHelper helper) {
        final BlockPos screenPos = new BlockPos(0, 1, 1);
        final BlockPos computerPos = new BlockPos(1, 1, 1);

        helper.setBlock(screenPos, ModBlocks.SCREEN_TIER1.get());
        helper.setBlock(computerPos, ModBlocks.COMPUTER_CASE_TIER1.get());

        final ScreenBlockEntity screen = helper.getBlockEntity(screenPos);
        final ComputerCaseBlockEntity computer = helper.getBlockEntity(computerPos);
        helper.assertTrue(screen.node().network() == computer.node().network(), "Screen and computer are not on the same network");

        final BlockState state = helper.getBlockState(screenPos);
        final BlockPos absoluteScreenPos = helper.absolutePos(screenPos);
        final Vec3 hitLocation = new Vec3(absoluteScreenPos.getX() + 0.25D, absoluteScreenPos.getY() + 0.75D, absoluteScreenPos.getZ());
        final BlockHitResult hit = new BlockHitResult(hitLocation, Direction.NORTH, absoluteScreenPos, false);
        helper.assertTrue(invokeUseWithoutItem(state, helper, screenPos, hit) == InteractionResult.CONSUME, "Screen click was not consumed");

        helper.runAtTickTime(5, () -> {
            assertNextSignal(helper, computer, "touch", 13, 5, 0);
            assertNextSignal(helper, computer, "drop", 13, 5, 0);
            helper.succeed();
        });
    }

    private static ItemStack luaBiosEepromStack() {
        final ItemStack stack = new ItemStack(ModItems.EEPROM.get());
        final CompoundTag data = new CompoundTag();
        data.putString(ItemRegistry.EEPROM_LABEL_TAG, "EEPROM (Lua BIOS)");
        data.putByteArray(ItemRegistry.EEPROM_CODE_TAG, ModEeproms.luaBiosCode());
        data.putByteArray(ItemRegistry.EEPROM_DATA_SECTION_TAG, new byte[0]);
        data.putBoolean(ItemRegistry.EEPROM_READONLY_TAG, true);

        final CompoundTag root = new CompoundTag();
        root.put(ItemRegistry.EEPROM_DATA_TAG, data);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("EEPROM (Lua BIOS)"));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        return stack;
    }

    private static ItemStack openOsFloppyStack() {
        final ItemStack stack = new ItemStack(ModItems.FLOPPY.get());
        final CompoundTag data = new CompoundTag();
        data.putString(ItemRegistry.FLOPPY_LABEL_TAG, "OpenOS (Operating System)");
        data.putString(ItemRegistry.FLOPPY_COLOR_TAG, "green");
        data.putString(ItemRegistry.FLOPPY_FACTORY_ID_TAG, "neoopencomputers:loot/openos");
        data.putBoolean(ItemRegistry.FLOPPY_RECIPE_CYCLING_TAG, true);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal("OpenOS (Operating System)"));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        return stack;
    }

    private static String neighborComponents(final Node node) {
        final StringBuilder builder = new StringBuilder("[");
        if (node != null) {
            boolean first = true;
            for (Node neighbor : node.neighbors()) {
                if (!first) {
                    builder.append(", ");
                }
                first = false;
                builder.append(neighbor.address()).append('=');
                if (neighbor instanceof li.cil.oc.api.network.Component component) {
                    builder.append(component.name()).append('/').append(component.visibility());
                } else {
                    builder.append(neighbor.getClass().getSimpleName());
                }
            }
        }
        return builder.append(']').toString();
    }

    private static li.cil.oc.api.fs.FileSystem callFactory(final Callable<li.cil.oc.api.fs.FileSystem> factory) {
        try {
            return factory.call();
        } catch (Exception e) {
            return null;
        }
    }

    private static void assertItemTier(final GameTestHelper helper, final ItemStack stack, final int tier) {
        final DriverItem driver = Driver.driverFor(stack);
        helper.assertTrue(driver != null, "No driver for " + stack);
        helper.assertTrue(driver.tier(stack) == tier, "Expected " + stack + " to report tier " + tier + " but got " + driver.tier(stack));
    }

    private static void assertProcessorComponents(final GameTestHelper helper, final ItemStack stack, final int supportedComponents) {
        final DriverItem driver = Driver.driverFor(stack);
        helper.assertTrue(driver instanceof Processor, "Expected processor driver for " + stack);
        final Processor processor = (Processor) driver;
        helper.assertTrue(processor.supportedComponents(stack) == supportedComponents, "Expected " + stack + " to support " + supportedComponents + " components but got " + processor.supportedComponents(stack));
    }

    private static void assertMemoryAmount(final GameTestHelper helper, final ItemStack stack, final double amount) {
        final DriverItem driver = Driver.driverFor(stack);
        helper.assertTrue(driver instanceof Memory, "Expected memory driver for " + stack);
        final Memory memory = (Memory) driver;
        helper.assertTrue(memory.amount(stack) == amount, "Expected " + stack + " to provide " + amount + " KB RAM but got " + memory.amount(stack));
    }

    private static void assertHardDiskCapacity(final GameTestHelper helper, final ItemStack stack, final long capacity) {
        final DriverItem driver = Driver.driverFor(stack);
        helper.assertTrue(driver != null, "No driver for " + stack);
        final ManagedEnvironment environment = driver.createEnvironment(stack, null);
        helper.assertTrue(environment != null, "No HDD environment for " + stack);
        helper.assertTrue(environment.node() instanceof li.cil.oc.api.network.Component, "HDD environment has no filesystem component for " + stack);
        final li.cil.oc.api.network.Component component = (li.cil.oc.api.network.Component) environment.node();
        final Object[] result = invokeComponent(helper, component, "spaceTotal");
        helper.assertTrue(result.length == 1 && result[0].equals(capacity), "Expected " + stack + " capacity " + capacity + " but got " + (result.length == 0 ? "<empty>" : result[0]));
    }

    private static void assertScreenTier(final GameTestHelper helper, final ScreenBlockEntity screen, final int tier, final int width, final int height, final TextBuffer.ColorDepth depth) {
        helper.assertTrue(screen.tier() == tier, "Expected screen tier " + tier + " but got " + screen.tier());
        helper.assertTrue(screen.getMaximumWidth() == width, "Expected screen max width " + width + " but got " + screen.getMaximumWidth());
        helper.assertTrue(screen.getMaximumHeight() == height, "Expected screen max height " + height + " but got " + screen.getMaximumHeight());
        helper.assertTrue(screen.getWidth() == width, "Expected screen width " + width + " but got " + screen.getWidth());
        helper.assertTrue(screen.getHeight() == height, "Expected screen height " + height + " but got " + screen.getHeight());
        helper.assertTrue(screen.getMaximumColorDepth() == depth, "Expected screen depth " + depth + " but got " + screen.getMaximumColorDepth());
    }

    private static Object[] invokeComponent(final GameTestHelper helper, final li.cil.oc.api.network.Component component, final String method) {
        try {
            return component.invoke(method, null);
        } catch (Exception e) {
            helper.fail("Component invocation failed: " + method + " " + e.getMessage());
            return new Object[0];
        }
    }

    private static boolean screenHasNonBlankText(final ScreenBlockEntity screen) {
        for (int y = 0; y < screen.getHeight(); y++) {
            for (int x = 0; x < screen.getWidth(); x++) {
                if (screen.getCodePoint(x, y) != ' ') {
                    return true;
                }
            }
        }
        return false;
    }

    private static String screenText(final ScreenBlockEntity screen) {
        final StringBuilder builder = new StringBuilder();
        for (int y = 0; y < screen.getHeight(); y++) {
            if (y > 0) {
                builder.append('\n');
            }
            for (int x = 0; x < screen.getWidth(); x++) {
                builder.appendCodePoint(screen.getCodePoint(x, y));
            }
        }
        return builder.toString();
    }

    private static void assertNextSignal(final GameTestHelper helper, final ComputerCaseBlockEntity computer, final String name, final Object... args) {
        for (int attempt = 0; attempt < 16; attempt++) {
            final Signal signal = computer.machine().popSignal();
            helper.assertTrue(signal != null, "Expected signal " + name + " but queue was empty");
            if (name.equals(signal.name())) {
                assertSignal(helper, signal, name, args);
                return;
            }
        }
        helper.fail("Expected signal " + name + " but it was not in the next 16 queued signals");
    }

    private static InteractionResult invokeUseWithoutItem(final BlockState state, final GameTestHelper helper, final BlockPos pos, final BlockHitResult hit) {
        try {
            final Method useWithoutItem = state.getBlock().getClass().getDeclaredMethod(
                "useWithoutItem",
                BlockState.class,
                net.minecraft.world.level.Level.class,
                BlockPos.class,
                net.minecraft.world.entity.player.Player.class,
                BlockHitResult.class);
            useWithoutItem.setAccessible(true);
            return (InteractionResult) useWithoutItem.invoke(state.getBlock(), state, helper.getLevel(), helper.absolutePos(pos), null, hit);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Could not invoke screen useWithoutItem", e);
        }
    }

    private static void assertSignal(final GameTestHelper helper, final Signal signal, final String name, final Object... args) {
        helper.assertTrue(name.equals(signal.name()), "Expected signal " + name + " but got " + signal.name());
        helper.assertTrue(signal.args().length == args.length, "Expected signal " + name + " to have " + args.length + " arguments but got " + signal.args().length);
        for (int index = 0; index < args.length; index++) {
            helper.assertTrue(args[index].equals(signal.args()[index]), "Expected signal " + name + " argument " + index + " to be " + args[index] + " but got " + signal.args()[index]);
        }
    }

    private NeoOpenComputersGameTests() {
    }
}
