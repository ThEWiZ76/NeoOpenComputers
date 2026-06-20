package li.cil.oc.common.gametest;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.api.API;
import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.DriverBlock;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.driver.item.Memory;
import li.cil.oc.api.driver.item.Processor;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.internal.TextBuffer;
import li.cil.oc.api.machine.Signal;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.common.ItemRegistry;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModEeproms;
import li.cil.oc.common.ModItems;
import li.cil.oc.api.Network;
import li.cil.oc.common.blockentity.CableBlockEntity;
import li.cil.oc.common.blockentity.ComputerCaseBlockEntity;
import li.cil.oc.common.blockentity.DiskDriveBlockEntity;
import li.cil.oc.common.blockentity.KeyboardBlockEntity;
import li.cil.oc.common.blockentity.ScreenBlockEntity;
import li.cil.oc.common.block.ComputerCaseBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.Callable;

@GameTestHolder(NeoOpenComputers.MODID)
@PrefixGameTestTemplate(false)
public final class NeoOpenComputersGameTests {
    @GameTest(template = "empty")
    public static void registeredContentAvailable(final GameTestHelper helper) {
        ModBlocks.ADAPTER.get();
        ModBlocks.CABLE.get();
        ModBlocks.COMPUTER_CASE_TIER1.get();
        ModBlocks.COMPUTER_CASE_TIER2.get();
        ModBlocks.COMPUTER_CASE_TIER3.get();
        ModBlocks.DISK_DRIVE.get();
        ModBlocks.SCREEN_TIER1.get();
        ModBlocks.SCREEN_TIER2.get();
        ModBlocks.SCREEN_TIER3.get();
        ModBlocks.KEYBOARD.get();
        ModItems.ADAPTER.get();
        ModItems.CABLE.get();
        ModItems.CPU_TIER1.get();
        ModItems.CPU_TIER2.get();
        ModItems.CPU_TIER3.get();
        ModItems.DATA_CARD_TIER1.get();
        ModItems.DATA_CARD_TIER2.get();
        ModItems.DATABASE_UPGRADE_TIER1.get();
        ModItems.DATABASE_UPGRADE_TIER2.get();
        ModItems.DATABASE_UPGRADE_TIER3.get();
        ModItems.EEPROM.get();
        ModItems.FLOPPY.get();
        ModItems.GRAPHICS_CARD_TIER1.get();
        ModItems.GRAPHICS_CARD_TIER2.get();
        ModItems.GRAPHICS_CARD_TIER3.get();
        ModItems.HDD_TIER1.get();
        ModItems.HDD_TIER2.get();
        ModItems.HDD_TIER3.get();
        ModItems.INVENTORY_CONTROLLER_UPGRADE.get();
        ModItems.LINKED_CARD.get();
        ModItems.MEMORY_TIER1.get();
        ModItems.MEMORY_TIER2.get();
        ModItems.MEMORY_TIER3.get();
        ModItems.NETWORK_CARD.get();
        ModItems.WIRELESS_NETWORK_CARD_TIER1.get();
        ModItems.WIRELESS_NETWORK_CARD_TIER2.get();
        ModItems.REDSTONE_CARD.get();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void tieredComponentItemsReportTheirTier(final GameTestHelper helper) {
        assertItemTier(helper, new ItemStack(ModItems.CPU_TIER1.get()), 0);
        assertItemTier(helper, new ItemStack(ModItems.CPU_TIER2.get()), 1);
        assertItemTier(helper, new ItemStack(ModItems.CPU_TIER3.get()), 2);
        assertItemTier(helper, new ItemStack(ModItems.DATA_CARD_TIER1.get()), 0);
        assertItemTier(helper, new ItemStack(ModItems.DATA_CARD_TIER2.get()), 1);
        assertItemTier(helper, new ItemStack(ModItems.DATABASE_UPGRADE_TIER1.get()), 0);
        assertItemTier(helper, new ItemStack(ModItems.DATABASE_UPGRADE_TIER2.get()), 1);
        assertItemTier(helper, new ItemStack(ModItems.DATABASE_UPGRADE_TIER3.get()), 2);
        assertItemTier(helper, new ItemStack(ModItems.MEMORY_TIER1.get()), 0);
        assertItemTier(helper, new ItemStack(ModItems.MEMORY_TIER2.get()), 1);
        assertItemTier(helper, new ItemStack(ModItems.MEMORY_TIER3.get()), 2);
        assertItemTier(helper, new ItemStack(ModItems.GRAPHICS_CARD_TIER1.get()), 0);
        assertItemTier(helper, new ItemStack(ModItems.GRAPHICS_CARD_TIER2.get()), 1);
        assertItemTier(helper, new ItemStack(ModItems.GRAPHICS_CARD_TIER3.get()), 2);
        assertItemTier(helper, new ItemStack(ModItems.HDD_TIER1.get()), 0);
        assertItemTier(helper, new ItemStack(ModItems.HDD_TIER2.get()), 1);
        assertItemTier(helper, new ItemStack(ModItems.HDD_TIER3.get()), 2);
        assertItemTier(helper, new ItemStack(ModItems.WIRELESS_NETWORK_CARD_TIER1.get()), 0);
        assertItemTier(helper, new ItemStack(ModItems.WIRELESS_NETWORK_CARD_TIER2.get()), 1);
        assertItemTier(helper, new ItemStack(ModItems.LINKED_CARD.get()), 1);
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
        assertDatabaseCapacity(helper, new ItemStack(ModItems.DATABASE_UPGRADE_TIER1.get()), 9);
        assertDatabaseCapacity(helper, new ItemStack(ModItems.DATABASE_UPGRADE_TIER2.get()), 25);
        assertDatabaseCapacity(helper, new ItemStack(ModItems.DATABASE_UPGRADE_TIER3.get()), 81);
        assertWirelessModem(helper, new ItemStack(ModItems.WIRELESS_NETWORK_CARD_TIER1.get()), false, 16D);
        assertWirelessModem(helper, new ItemStack(ModItems.WIRELESS_NETWORK_CARD_TIER2.get()), true, 400D);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void databaseUpgradePersistsStoredStacks(final GameTestHelper helper) {
        final ItemStack databaseStack = new ItemStack(ModItems.DATABASE_UPGRADE_TIER1.get());
        final DriverItem driver = Driver.driverFor(databaseStack);
        helper.assertTrue(driver != null, "No driver for database upgrade");

        final ManagedEnvironment firstEnvironment = driver.createEnvironment(databaseStack, null);
        helper.assertTrue(firstEnvironment instanceof li.cil.oc.api.internal.Database, "Database upgrade did not create database environment");
        final li.cil.oc.api.internal.Database firstDatabase = (li.cil.oc.api.internal.Database) firstEnvironment;
        firstDatabase.setStackInSlot(0, new ItemStack(Items.DIAMOND, 3));
        firstEnvironment.save(new CompoundTag());

        final ManagedEnvironment secondEnvironment = driver.createEnvironment(databaseStack, null);
        helper.assertTrue(secondEnvironment instanceof li.cil.oc.api.internal.Database, "Database upgrade did not recreate database environment");
        final ItemStack restored = ((li.cil.oc.api.internal.Database) secondEnvironment).getStackInSlot(0);
        helper.assertTrue(restored.is(Items.DIAMOND) && restored.getCount() == 3, "Database stack did not persist");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void databaseUpgradeCopiesEntriesToAddressedDatabase(final GameTestHelper helper) {
        final DriverItem driver = Driver.driverFor(new ItemStack(ModItems.DATABASE_UPGRADE_TIER1.get()));
        helper.assertTrue(driver != null, "No driver for database upgrade");
        final ManagedEnvironment sourceEnvironment = driver.createEnvironment(new ItemStack(ModItems.DATABASE_UPGRADE_TIER1.get()), null);
        final ManagedEnvironment targetEnvironment = driver.createEnvironment(new ItemStack(ModItems.DATABASE_UPGRADE_TIER1.get()), null);
        helper.assertTrue(sourceEnvironment instanceof li.cil.oc.api.internal.Database, "Source is not a database");
        helper.assertTrue(targetEnvironment instanceof li.cil.oc.api.internal.Database, "Target is not a database");
        Network.joinNewNetwork(sourceEnvironment.node());
        sourceEnvironment.node().connect(targetEnvironment.node());

        ((li.cil.oc.api.internal.Database) sourceEnvironment).setStackInSlot(0, new ItemStack(Items.DIAMOND, 2));
        ((li.cil.oc.api.internal.Database) targetEnvironment).setStackInSlot(1, new ItemStack(Items.GOLD_INGOT));
        try {
            final Object[] result = ((li.cil.oc.api.network.Component) sourceEnvironment.node()).invoke("copy", null, 1, 2, targetEnvironment.node().address());
            helper.assertTrue(result.length == 1 && Boolean.TRUE.equals(result[0]), "Remote copy did not report overwritten slot");
        } catch (Exception e) {
            helper.fail("Remote database copy failed: " + e.getMessage());
        }

        final ItemStack copied = ((li.cil.oc.api.internal.Database) targetEnvironment).getStackInSlot(1);
        helper.assertTrue(copied.is(Items.DIAMOND) && copied.getCount() == 2, "Remote database slot did not receive copied stack");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void databaseUpgradeClonesEntriesToAddressedDatabase(final GameTestHelper helper) {
        final DriverItem driver = Driver.driverFor(new ItemStack(ModItems.DATABASE_UPGRADE_TIER1.get()));
        helper.assertTrue(driver != null, "No driver for database upgrade");
        final ManagedEnvironment sourceEnvironment = driver.createEnvironment(new ItemStack(ModItems.DATABASE_UPGRADE_TIER1.get()), null);
        final ManagedEnvironment targetEnvironment = driver.createEnvironment(new ItemStack(ModItems.DATABASE_UPGRADE_TIER1.get()), null);
        helper.assertTrue(sourceEnvironment instanceof li.cil.oc.api.internal.Database, "Source is not a database");
        helper.assertTrue(targetEnvironment instanceof li.cil.oc.api.internal.Database, "Target is not a database");
        Network.joinNewNetwork(sourceEnvironment.node());
        sourceEnvironment.node().connect(targetEnvironment.node());

        final li.cil.oc.api.internal.Database source = (li.cil.oc.api.internal.Database) sourceEnvironment;
        final li.cil.oc.api.internal.Database target = (li.cil.oc.api.internal.Database) targetEnvironment;
        source.setStackInSlot(0, new ItemStack(Items.DIAMOND, 2));
        source.setStackInSlot(1, new ItemStack(Items.EMERALD, 4));
        target.setStackInSlot(0, new ItemStack(Items.GOLD_INGOT));
        target.setStackInSlot(2, new ItemStack(Items.IRON_INGOT));
        final double[] pauseSeconds = {0D};
        final Context context = new DatabaseCloneContext(sourceEnvironment.node(), pauseSeconds);

        try {
            final Object[] result = ((li.cil.oc.api.network.Component) sourceEnvironment.node()).invoke("clone", context, targetEnvironment.node().address());
            helper.assertTrue(result.length == 1 && result[0].equals(9), "Clone did not report copied slot count");
        } catch (Exception e) {
            helper.fail("Database clone failed: " + e.getMessage());
        }

        helper.assertTrue(target.getStackInSlot(0).is(Items.DIAMOND) && target.getStackInSlot(0).getCount() == 2, "Clone did not copy first source slot");
        helper.assertTrue(target.getStackInSlot(1).is(Items.EMERALD) && target.getStackInSlot(1).getCount() == 4, "Clone did not copy second source slot");
        helper.assertTrue(target.getStackInSlot(2).isEmpty(), "Clone did not clear stale target slot");
        helper.assertTrue(pauseSeconds[0] == 0.25D, "Clone did not pause for upstream delay");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void tieredComputerCasesReportTheirTier(final GameTestHelper helper) {
        final BlockPos tier1Pos = new BlockPos(0, 1, 0);
        final BlockPos tier2Pos = new BlockPos(1, 1, 0);
        final BlockPos tier3Pos = new BlockPos(2, 1, 0);

        helper.setBlock(tier1Pos, ModBlocks.COMPUTER_CASE_TIER1.get());
        helper.setBlock(tier2Pos, ModBlocks.COMPUTER_CASE_TIER2.get());
        helper.setBlock(tier3Pos, ModBlocks.COMPUTER_CASE_TIER3.get());

        assertComputerCaseTier(helper, helper.getBlockEntity(tier1Pos), 0);
        assertComputerCaseTier(helper, helper.getBlockEntity(tier2Pos), 1);
        assertComputerCaseTier(helper, helper.getBlockEntity(tier3Pos), 2);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void tieredComputerCasesEnforceSlotTiers(final GameTestHelper helper) {
        final BlockPos tier1Pos = new BlockPos(0, 1, 0);
        final BlockPos tier2Pos = new BlockPos(1, 1, 0);
        final BlockPos tier3Pos = new BlockPos(2, 1, 0);

        helper.setBlock(tier1Pos, ModBlocks.COMPUTER_CASE_TIER1.get());
        helper.setBlock(tier2Pos, ModBlocks.COMPUTER_CASE_TIER2.get());
        helper.setBlock(tier3Pos, ModBlocks.COMPUTER_CASE_TIER3.get());

        final ComputerCaseBlockEntity tier1 = helper.getBlockEntity(tier1Pos);
        final ComputerCaseBlockEntity tier2 = helper.getBlockEntity(tier2Pos);
        final ComputerCaseBlockEntity tier3 = helper.getBlockEntity(tier3Pos);
        helper.assertTrue(tier1.getContainerSize() == 7, "Expected tier 1 case to have 7 slots");
        helper.assertTrue(tier2.getContainerSize() == 8, "Expected tier 2 case to have 8 slots");
        helper.assertTrue(tier3.getContainerSize() == 10, "Expected tier 3 case to have 10 slots");
        helper.assertTrue(!tier1.canPlaceItem(4, new ItemStack(ModItems.CPU_TIER2.get())), "Tier 1 CPU slot accepted tier 2 CPU");
        helper.assertTrue(tier2.canPlaceItem(6, new ItemStack(ModItems.CPU_TIER2.get())), "Tier 2 CPU slot rejected tier 2 CPU");
        helper.assertTrue(!tier2.canPlaceItem(6, new ItemStack(ModItems.CPU_TIER3.get())), "Tier 2 CPU slot accepted tier 3 CPU");
        helper.assertTrue(tier3.canPlaceItem(8, new ItemStack(ModItems.CPU_TIER3.get())), "Tier 3 CPU slot rejected tier 3 CPU");
        helper.assertTrue(tier3.canPlaceItem(7, new ItemStack(ModItems.FLOPPY.get())), "Tier 3 floppy slot rejected floppy");
        tier1.setItem(ComputerCaseBlockEntity.SLOT_CPU, new ItemStack(ModItems.CPU_TIER2.get()));
        tier1.setItem(ComputerCaseBlockEntity.SLOT_MEMORY_0, new ItemStack(ModItems.MEMORY_TIER1.get()));
        tier1.setItem(ComputerCaseBlockEntity.SLOT_EEPROM, luaBiosEepromStack());
        helper.assertTrue(!tier1.toggleMachine(), "Tier 1 case started with an installed tier 2 CPU");
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

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void cableConnectsSeparatedBlocks(final GameTestHelper helper) {
        final BlockPos computerPos = new BlockPos(0, 1, 1);
        final BlockPos cablePos = new BlockPos(1, 1, 1);
        final BlockPos diskDrivePos = new BlockPos(2, 1, 1);

        helper.setBlock(computerPos, ModBlocks.COMPUTER_CASE_TIER1.get());
        helper.setBlock(cablePos, ModBlocks.CABLE.get());
        helper.setBlock(diskDrivePos, ModBlocks.DISK_DRIVE.get());

        helper.succeedWhen(() -> {
            final ComputerCaseBlockEntity computer = helper.getBlockEntity(computerPos);
            final CableBlockEntity cable = helper.getBlockEntity(cablePos);
            final DiskDriveBlockEntity diskDrive = helper.getBlockEntity(diskDrivePos);
            helper.assertTrue(computer.node().network() != null, "Computer has no network");
            helper.assertTrue(cable.node().network() == computer.node().network(), "Cable is not on the computer network");
            helper.assertTrue(diskDrive.node().network() == computer.node().network(), "Disk drive is not connected through cable");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void adapterConnectsSeparatedBlocks(final GameTestHelper helper) {
        final BlockPos computerPos = new BlockPos(0, 1, 1);
        final BlockPos adapterPos = new BlockPos(1, 1, 1);
        final BlockPos diskDrivePos = new BlockPos(2, 1, 1);

        helper.setBlock(computerPos, ModBlocks.COMPUTER_CASE_TIER1.get());
        helper.setBlock(adapterPos, ModBlocks.ADAPTER.get());
        helper.setBlock(diskDrivePos, ModBlocks.DISK_DRIVE.get());

        helper.succeedWhen(() -> {
            final ComputerCaseBlockEntity computer = helper.getBlockEntity(computerPos);
            final li.cil.oc.common.blockentity.AdapterBlockEntity adapter = helper.getBlockEntity(adapterPos);
            final DiskDriveBlockEntity diskDrive = helper.getBlockEntity(diskDrivePos);
            helper.assertTrue(computer.node().network() != null, "Computer has no network");
            helper.assertTrue(adapter.node().network() == computer.node().network(), "Adapter is not on the computer network");
            helper.assertTrue(diskDrive.node().network() == computer.node().network(), "Disk drive is not connected through adapter");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void adapterExposesBlockDriverComponents(final GameTestHelper helper) {
        Driver.add(new AdapterTestBlockDriver());
        final BlockPos computerPos = new BlockPos(0, 1, 1);
        final BlockPos adapterPos = new BlockPos(1, 1, 1);
        final BlockPos targetPos = new BlockPos(2, 1, 1);

        helper.setBlock(computerPos, ModBlocks.COMPUTER_CASE_TIER1.get());
        helper.setBlock(adapterPos, ModBlocks.ADAPTER.get());
        helper.setBlock(targetPos, Blocks.EMERALD_BLOCK);

        helper.succeedWhen(() -> {
            final ComputerCaseBlockEntity computer = helper.getBlockEntity(computerPos);
            final String address = componentAddress(computer, AdapterTestEnvironment.COMPONENT_NAME);
            helper.assertTrue(address != null, "Adapter did not expose block-driver component: " + computer.machine().components());
            try {
                final Object[] result = computer.machine().invoke(address, "ping", new Object[0]);
                helper.assertTrue(result.length == 1 && "pong".equals(result[0]), "Adapter component invocation failed");
            } catch (Exception e) {
                helper.fail("Adapter component invocation failed: " + e.getMessage());
            }
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void adapterInventoryControllerUpgradeReadsChest(final GameTestHelper helper) {
        final BlockPos computerPos = new BlockPos(0, 1, 1);
        final BlockPos adapterPos = new BlockPos(1, 1, 1);
        final BlockPos chestPos = new BlockPos(2, 1, 1);

        helper.setBlock(computerPos, ModBlocks.COMPUTER_CASE_TIER1.get());
        helper.setBlock(adapterPos, ModBlocks.ADAPTER.get());
        helper.setBlock(chestPos, Blocks.CHEST);

        final li.cil.oc.common.blockentity.AdapterBlockEntity adapter = helper.getBlockEntity(adapterPos);
        helper.assertTrue(adapter.canPlaceItem(0, new ItemStack(ModItems.INVENTORY_CONTROLLER_UPGRADE.get())), "Adapter rejected inventory controller upgrade");
        adapter.setItem(0, new ItemStack(ModItems.INVENTORY_CONTROLLER_UPGRADE.get()));

        final net.minecraft.world.Container chest = helper.getBlockEntity(chestPos);
        chest.setItem(0, new ItemStack(net.minecraft.world.item.Items.DIAMOND, 4));
        chest.setItem(1, new ItemStack(net.minecraft.world.item.Items.DIAMOND, 1));
        chest.setItem(2, new ItemStack(net.minecraft.world.item.Items.DIRT, 1));
        chest.setItem(3, new ItemStack(net.minecraft.world.item.Items.OAK_LOG, 1));
        chest.setItem(4, new ItemStack(net.minecraft.world.item.Items.SPRUCE_LOG, 1));
        chest.setItem(5, namedStack(net.minecraft.world.item.Items.COBBLESTONE, "left"));
        chest.setItem(6, namedStack(net.minecraft.world.item.Items.COBBLESTONE, "right"));

        helper.succeedWhen(() -> {
            final ComputerCaseBlockEntity computer = helper.getBlockEntity(computerPos);
            final String address = componentAddress(computer, "inventory_controller");
            helper.assertTrue(address != null, "Adapter did not expose inventory controller upgrade: " + computer.machine().components());
            try {
                final int east = Direction.EAST.get3DDataValue();
                assertInvokeResult(helper, computer, address, "getInventoryName", new Object[]{east}, "minecraft:chest");
                assertInvokeResult(helper, computer, address, "getInventorySize", new Object[]{east}, 27);
                assertInvokeResult(helper, computer, address, "getSlotStackSize", new Object[]{east, 1}, 4);
                assertInvokeResult(helper, computer, address, "getSlotMaxStackSize", new Object[]{east, 1}, 64);
                assertInvokeResult(helper, computer, address, "getSlotMaxStackSize", new Object[]{east, 8}, 0);
                assertInvokeResult(helper, computer, address, "compareStacks", new Object[]{east, 1, 2}, true);
                assertInvokeResult(helper, computer, address, "compareStacks", new Object[]{east, 1, 3}, false);
                assertInvokeResult(helper, computer, address, "compareStacks", new Object[]{east, 6, 7}, true);
                assertInvokeResult(helper, computer, address, "compareStacks", new Object[]{east, 6, 7, true}, false);
                assertInvokeResult(helper, computer, address, "areStacksEquivalent", new Object[]{east, 4, 5}, true);
                assertInvokeResult(helper, computer, address, "areStacksEquivalent", new Object[]{east, 1, 3}, false);
                final Object[] stackResult = computer.machine().invoke(address, "getStackInSlot", new Object[]{east, 1});
                helper.assertTrue(stackResult.length == 1 && stackResult[0] instanceof ItemStack stack && stack.is(net.minecraft.world.item.Items.DIAMOND) && stack.getCount() == 4, "Inventory controller did not expose slot stack");
                final Object[] stacksResult = computer.machine().invoke(address, "getAllStacks", new Object[]{east});
                helper.assertTrue(stacksResult.length == 1 && stacksResult[0] instanceof ItemStack[], "Inventory controller did not expose stack array");
                final ItemStack[] stacks = (ItemStack[]) stacksResult[0];
                helper.assertTrue(stacks.length == 27 && stacks[0].getCount() == 4 && stacks[1].getCount() == 1, "Inventory controller all-stack list mismatch");
            } catch (Exception e) {
                helper.fail("Inventory controller invocation failed: " + e.getMessage());
            }
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void adapterExposesChestInventory(final GameTestHelper helper) {
        final BlockPos computerPos = new BlockPos(0, 1, 1);
        final BlockPos adapterPos = new BlockPos(1, 1, 1);
        final BlockPos chestPos = new BlockPos(2, 1, 1);

        helper.setBlock(computerPos, ModBlocks.COMPUTER_CASE_TIER1.get());
        helper.setBlock(adapterPos, ModBlocks.ADAPTER.get());
        helper.setBlock(chestPos, Blocks.CHEST);

        final net.minecraft.world.Container chest = helper.getBlockEntity(chestPos);
        chest.setItem(0, new ItemStack(net.minecraft.world.item.Items.DIAMOND, 3));
        chest.setItem(2, namedStack(net.minecraft.world.item.Items.COBBLESTONE, "left"));
        chest.setItem(3, namedStack(net.minecraft.world.item.Items.COBBLESTONE, "right"));

        helper.succeedWhen(() -> {
            final ComputerCaseBlockEntity computer = helper.getBlockEntity(computerPos);
            final String address = componentAddress(computer, "inventory");
            helper.assertTrue(address != null, "Adapter did not expose chest inventory: " + computer.machine().components());
            try {
                assertInvokeResult(helper, computer, address, "getInventorySize", new Object[]{}, 27);
                assertInvokeResult(helper, computer, address, "getSlotStackSize", new Object[]{1}, 3);
                final Object[] stacksResult = computer.machine().invoke(address, "getAllStacks", new Object[]{});
                helper.assertTrue(stacksResult.length == 1 && stacksResult[0] instanceof ItemStack[], "getAllStacks did not return an item-stack array");
                final ItemStack[] stacks = (ItemStack[]) stacksResult[0];
                helper.assertTrue(stacks.length == 27, "getAllStacks returned wrong chest size");
                helper.assertTrue(stacks[0].is(net.minecraft.world.item.Items.DIAMOND) && stacks[0].getCount() == 3, "getAllStacks did not include first slot diamonds");
                assertInvokeResult(helper, computer, address, "compareStacks", new Object[]{3, 4}, true);
                assertInvokeResult(helper, computer, address, "compareStacks", new Object[]{3, 4, true}, false);
                assertInvokeResult(helper, computer, address, "transferStack", new Object[]{1, 2, 2}, true);
                assertInvokeResult(helper, computer, address, "getSlotStackSize", new Object[]{1}, 1);
                assertInvokeResult(helper, computer, address, "getSlotStackSize", new Object[]{2}, 2);
            } catch (Exception e) {
                helper.fail("Chest inventory invocation failed: " + e.getMessage());
            }
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

    @GameTest(template = "empty", timeoutTicks = 260)
    public static void openOsTerminalEchoesKeyboardInput(final GameTestHelper helper) {
        final BlockPos screenPos = new BlockPos(0, 1, 1);
        final BlockPos keyboardPos = new BlockPos(0, 1, 2);
        final BlockPos computerPos = new BlockPos(1, 1, 1);
        final BlockPos diskDrivePos = new BlockPos(2, 1, 1);

        helper.setBlock(screenPos, ModBlocks.SCREEN_TIER1.get());
        helper.setBlock(keyboardPos, ModBlocks.KEYBOARD.get());
        helper.setBlock(computerPos, ModBlocks.COMPUTER_CASE_TIER1.get());
        helper.setBlock(diskDrivePos, ModBlocks.DISK_DRIVE.get());

        final ScreenBlockEntity screen = helper.getBlockEntity(screenPos);
        final ComputerCaseBlockEntity computer = helper.getBlockEntity(computerPos);
        final DiskDriveBlockEntity diskDrive = helper.getBlockEntity(diskDrivePos);
        diskDrive.setItem(DiskDriveBlockEntity.SLOT_FLOPPY, openOsFloppyStack());
        computer.setItem(ComputerCaseBlockEntity.SLOT_CARD_0, new ItemStack(ModItems.GRAPHICS_CARD_TIER1.get()));
        computer.setItem(ComputerCaseBlockEntity.SLOT_CPU, new ItemStack(ModItems.CPU_TIER1.get()));
        computer.setItem(ComputerCaseBlockEntity.SLOT_MEMORY_0, new ItemStack(ModItems.MEMORY_TIER1.get()));
        computer.setItem(ComputerCaseBlockEntity.SLOT_EEPROM, luaBiosEepromStack());

        helper.assertTrue(computer.toggleMachine(), "Computer case did not start with OpenOS terminal setup");
        helper.runAtTickTime(120, () -> {
            typeKey(screen, 'z', 0x2C);
            typeKey(screen, 'z', 0x2C);
        });
        helper.runAtTickTime(220, () -> {
            helper.assertTrue(screenText(screen).contains("zz"), "OpenOS terminal did not echo keyboard input:\n" + screenText(screen));
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 320)
    public static void openOsTerminalRunsTypedCommand(final GameTestHelper helper) {
        final BlockPos screenPos = new BlockPos(0, 1, 1);
        final BlockPos keyboardPos = new BlockPos(0, 1, 2);
        final BlockPos computerPos = new BlockPos(1, 1, 1);
        final BlockPos diskDrivePos = new BlockPos(2, 1, 1);

        helper.setBlock(screenPos, ModBlocks.SCREEN_TIER1.get());
        helper.setBlock(keyboardPos, ModBlocks.KEYBOARD.get());
        helper.setBlock(computerPos, ModBlocks.COMPUTER_CASE_TIER1.get());
        helper.setBlock(diskDrivePos, ModBlocks.DISK_DRIVE.get());

        final ScreenBlockEntity screen = helper.getBlockEntity(screenPos);
        final ComputerCaseBlockEntity computer = helper.getBlockEntity(computerPos);
        final DiskDriveBlockEntity diskDrive = helper.getBlockEntity(diskDrivePos);
        diskDrive.setItem(DiskDriveBlockEntity.SLOT_FLOPPY, openOsFloppyStack());
        computer.setItem(ComputerCaseBlockEntity.SLOT_CARD_0, new ItemStack(ModItems.GRAPHICS_CARD_TIER1.get()));
        computer.setItem(ComputerCaseBlockEntity.SLOT_CPU, new ItemStack(ModItems.CPU_TIER1.get()));
        computer.setItem(ComputerCaseBlockEntity.SLOT_MEMORY_0, new ItemStack(ModItems.MEMORY_TIER1.get()));
        computer.setItem(ComputerCaseBlockEntity.SLOT_EEPROM, luaBiosEepromStack());

        helper.assertTrue(computer.toggleMachine(), "Computer case did not start with OpenOS terminal command setup");
        helper.runAtTickTime(120, () -> {
            typeText(screen, "echo ocok");
            typeKey(screen, '\n', 0x1C);
        });
        helper.runAtTickTime(260, () -> {
            final String text = screenText(screen);
            helper.assertTrue(countOccurrences(text, "ocok") >= 2, "OpenOS terminal did not run typed echo command:\n" + text);
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void tier3ComputerBootsOpenOsFromInternalFloppy(final GameTestHelper helper) {
        final BlockPos screenPos = new BlockPos(0, 1, 1);
        final BlockPos computerPos = new BlockPos(1, 1, 1);

        helper.setBlock(screenPos, ModBlocks.SCREEN_TIER1.get());
        helper.setBlock(computerPos, ModBlocks.COMPUTER_CASE_TIER3.get());

        final ScreenBlockEntity screen = helper.getBlockEntity(screenPos);
        final ComputerCaseBlockEntity computer = helper.getBlockEntity(computerPos);
        final ItemStack openOsFloppy = openOsFloppyStack();
        helper.assertTrue(computer.canPlaceItem(7, openOsFloppy), "Tier 3 internal floppy slot rejected OpenOS floppy");

        computer.setItem(0, new ItemStack(ModItems.GRAPHICS_CARD_TIER3.get()));
        computer.setItem(3, new ItemStack(ModItems.MEMORY_TIER3.get()));
        computer.setItem(7, openOsFloppy);
        computer.setItem(8, new ItemStack(ModItems.CPU_TIER3.get()));
        computer.setItem(9, luaBiosEepromStack());

        helper.assertTrue(computer.machine().components().containsValue("filesystem"), "Internal OpenOS floppy filesystem is not visible before boot: " + computer.machine().components());
        helper.assertTrue(computer.toggleMachine(), "Tier 3 computer did not start with internal OpenOS floppy");
        helper.runAtTickTime(80, () -> {
            helper.assertTrue(computer.machine().isRunning(), "Computer stopped while booting from internal OpenOS floppy: " + computer.machine().lastError());
            helper.assertTrue(computer.machine().components().containsValue("filesystem"), "Internal OpenOS floppy filesystem is not visible");
            helper.assertTrue(computer.machine().components().containsValue("gpu"), "Graphics card component is not visible");
            helper.assertTrue(computer.machine().components().containsValue("screen"), "Screen component is not visible");
            helper.assertTrue(screenHasNonBlankText(screen), "OpenOS did not write visible screen text from internal floppy:\n" + screenText(screen));
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void computerBootsLuaBiosFromInternalHardDisk(final GameTestHelper helper) {
        final BlockPos computerPos = new BlockPos(1, 1, 1);

        helper.setBlock(computerPos, ModBlocks.COMPUTER_CASE_TIER1.get());

        final ComputerCaseBlockEntity computer = helper.getBlockEntity(computerPos);
        final ItemStack bootDisk = bootableHardDiskStack(helper, "computer.pushSignal('hdd_booted', 'ok')");
        helper.assertTrue(computer.canPlaceItem(ComputerCaseBlockEntity.SLOT_HDD, bootDisk), "Computer case rejected bootable hard disk");

        computer.setItem(ComputerCaseBlockEntity.SLOT_CPU, new ItemStack(ModItems.CPU_TIER1.get()));
        computer.setItem(ComputerCaseBlockEntity.SLOT_MEMORY_0, new ItemStack(ModItems.MEMORY_TIER1.get()));
        computer.setItem(ComputerCaseBlockEntity.SLOT_HDD, bootDisk);
        computer.setItem(ComputerCaseBlockEntity.SLOT_EEPROM, luaBiosEepromStack());

        helper.assertTrue(computer.machine().components().containsValue("filesystem"), "Internal hard disk filesystem is not visible before boot: " + computer.machine().components());
        helper.assertTrue(computer.toggleMachine(), "Computer did not start with bootable hard disk");
        helper.runAtTickTime(80, () -> {
            helper.assertTrue(computer.machine().isRunning(), "Computer stopped while booting from hard disk: " + computer.machine().lastError());
            assertNextSignal(helper, computer, "hdd_booted", "ok");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 160)
    public static void installedRedstoneCardEmitsWorldSignal(final GameTestHelper helper) {
        final BlockPos computerPos = new BlockPos(1, 1, 1);

        helper.setBlock(computerPos, ModBlocks.COMPUTER_CASE_TIER1.get());

        final ComputerCaseBlockEntity computer = helper.getBlockEntity(computerPos);
        final ItemStack bootDisk = bootableHardDiskStack(helper, """
            local redstone = component.proxy(component.list('redstone')())
            local previous = redstone.setOutput(5, 15)
            computer.pushSignal('redstone_set', previous, redstone.getOutput(5))
            """);

        computer.setItem(ComputerCaseBlockEntity.SLOT_CARD_0, new ItemStack(ModItems.REDSTONE_CARD.get()));
        computer.setItem(ComputerCaseBlockEntity.SLOT_CPU, new ItemStack(ModItems.CPU_TIER1.get()));
        computer.setItem(ComputerCaseBlockEntity.SLOT_MEMORY_0, new ItemStack(ModItems.MEMORY_TIER1.get()));
        computer.setItem(ComputerCaseBlockEntity.SLOT_HDD, bootDisk);
        computer.setItem(ComputerCaseBlockEntity.SLOT_EEPROM, luaBiosEepromStack());

        helper.assertTrue(computer.machine().components().containsValue("redstone"), "Redstone component is not visible: " + computer.machine().components());
        helper.assertTrue(computer.toggleMachine(), "Computer did not start with redstone card");
        helper.runAtTickTime(100, () -> {
            helper.assertTrue(computer.machine().isRunning(), "Computer stopped while setting redstone output: " + computer.machine().lastError());
            assertNextSignal(helper, computer, "redstone_set", 0D, 15D);
            helper.assertTrue(helper.getLevel().getSignal(helper.absolutePos(computerPos), Direction.EAST) == 15, "Computer case did not emit east redstone signal");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 160)
    public static void installedRedstoneCardReadsWorldSignal(final GameTestHelper helper) {
        final BlockPos computerPos = new BlockPos(1, 1, 1);

        helper.setBlock(computerPos, ModBlocks.COMPUTER_CASE_TIER1.get());
        helper.setBlock(computerPos.east(), Blocks.REDSTONE_BLOCK);

        final ComputerCaseBlockEntity computer = helper.getBlockEntity(computerPos);
        final ItemStack bootDisk = bootableHardDiskStack(helper, """
            local redstone = component.proxy(component.list('redstone')())
            computer.pushSignal('redstone_input', redstone.getInput(5))
            """);

        computer.setItem(ComputerCaseBlockEntity.SLOT_CARD_0, new ItemStack(ModItems.REDSTONE_CARD.get()));
        computer.setItem(ComputerCaseBlockEntity.SLOT_CPU, new ItemStack(ModItems.CPU_TIER1.get()));
        computer.setItem(ComputerCaseBlockEntity.SLOT_MEMORY_0, new ItemStack(ModItems.MEMORY_TIER1.get()));
        computer.setItem(ComputerCaseBlockEntity.SLOT_HDD, bootDisk);
        computer.setItem(ComputerCaseBlockEntity.SLOT_EEPROM, luaBiosEepromStack());

        helper.assertTrue(computer.machine().components().containsValue("redstone"), "Redstone component is not visible: " + computer.machine().components());
        helper.assertTrue(computer.toggleMachine(), "Computer did not start with redstone card");
        helper.runAtTickTime(100, () -> {
            helper.assertTrue(computer.machine().isRunning(), "Computer stopped while reading redstone input: " + computer.machine().lastError());
            assertNextSignal(helper, computer, "redstone_input", 15D);
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 160)
    public static void redstoneCardUsesComputerLocalSides(final GameTestHelper helper) {
        final BlockPos computerPos = new BlockPos(1, 1, 1);

        helper.setBlock(computerPos, ModBlocks.COMPUTER_CASE_TIER1.get().defaultBlockState().setValue(ComputerCaseBlock.FACING, Direction.EAST));
        helper.setBlock(computerPos.east(), Blocks.REDSTONE_BLOCK);

        final ComputerCaseBlockEntity computer = helper.getBlockEntity(computerPos);
        final ItemStack bootDisk = bootableHardDiskStack(helper, """
            local redstone = component.proxy(component.list('redstone')())
            local input = redstone.getInput(2)
            redstone.setOutput(2, 15)
            computer.pushSignal('redstone_local_side', input, redstone.getOutput(2))
            """);

        computer.setItem(ComputerCaseBlockEntity.SLOT_CARD_0, new ItemStack(ModItems.REDSTONE_CARD.get()));
        computer.setItem(ComputerCaseBlockEntity.SLOT_CPU, new ItemStack(ModItems.CPU_TIER1.get()));
        computer.setItem(ComputerCaseBlockEntity.SLOT_MEMORY_0, new ItemStack(ModItems.MEMORY_TIER1.get()));
        computer.setItem(ComputerCaseBlockEntity.SLOT_HDD, bootDisk);
        computer.setItem(ComputerCaseBlockEntity.SLOT_EEPROM, luaBiosEepromStack());

        helper.assertTrue(computer.toggleMachine(), "Computer did not start with redstone card");
        helper.runAtTickTime(100, () -> {
            helper.assertTrue(computer.machine().isRunning(), "Computer stopped while mapping redstone side: " + computer.machine().lastError());
            assertNextSignal(helper, computer, "redstone_local_side", 15D, 15D);
            helper.assertTrue(helper.getLevel().getSignal(helper.absolutePos(computerPos), Direction.EAST) == 15, "Local back side did not emit east redstone signal");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 180)
    public static void redstoneCardQueuesInputChangeSignal(final GameTestHelper helper) {
        final BlockPos computerPos = new BlockPos(1, 1, 1);

        helper.setBlock(computerPos, ModBlocks.COMPUTER_CASE_TIER1.get());

        final ComputerCaseBlockEntity computer = helper.getBlockEntity(computerPos);
        final ItemStack bootDisk = bootableHardDiskStack(helper, """
            while true do
              local event, address, side, oldValue, newValue = computer.pullSignal(1)
              if event == 'redstone_changed' then
                computer.pushSignal('redstone_change_seen', address, side, oldValue, newValue)
                break
              end
            end
            """);

        computer.setItem(ComputerCaseBlockEntity.SLOT_CARD_0, new ItemStack(ModItems.REDSTONE_CARD.get()));
        computer.setItem(ComputerCaseBlockEntity.SLOT_CPU, new ItemStack(ModItems.CPU_TIER1.get()));
        computer.setItem(ComputerCaseBlockEntity.SLOT_MEMORY_0, new ItemStack(ModItems.MEMORY_TIER1.get()));
        computer.setItem(ComputerCaseBlockEntity.SLOT_HDD, bootDisk);
        computer.setItem(ComputerCaseBlockEntity.SLOT_EEPROM, luaBiosEepromStack());

        helper.assertTrue(computer.toggleMachine(), "Computer did not start with redstone card");
        helper.runAtTickTime(40, () -> helper.setBlock(computerPos.east(), Blocks.REDSTONE_BLOCK));
        helper.runAtTickTime(120, () -> {
            helper.assertTrue(computer.machine().isRunning(), "Computer stopped while waiting for redstone change: " + computer.machine().lastError());
            assertNextSignal(helper, computer, "redstone_change_seen", componentAddress(computer, "redstone"), 5D, 0D, 15D);
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void installedNetworkCardsExchangeModemMessages(final GameTestHelper helper) {
        final BlockPos receiverPos = new BlockPos(1, 1, 1);
        final BlockPos senderPos = new BlockPos(2, 1, 1);

        helper.setBlock(receiverPos, ModBlocks.COMPUTER_CASE_TIER1.get());
        helper.setBlock(senderPos, ModBlocks.COMPUTER_CASE_TIER1.get());

        final ComputerCaseBlockEntity receiver = helper.getBlockEntity(receiverPos);
        final ComputerCaseBlockEntity sender = helper.getBlockEntity(senderPos);
        installBootComputer(receiver, bootableHardDiskStack(helper, """
            local modem = component.proxy(component.list('modem')())
            modem.open(123)
            while true do
              local event, localAddress, remoteAddress, port, distance, payload = computer.pullSignal(1)
              if event == 'modem_message' and port == 123 then
                computer.pushSignal('modem_received', payload)
                break
              end
            end
            """));
        installBootComputer(sender, bootableHardDiskStack(helper, """
            local modem = component.proxy(component.list('modem')())
            modem.broadcast(123, 'payload')
            """));

        helper.assertTrue(receiver.node().network() == sender.node().network(), "Computer cases are not on the same wired network");
        helper.assertTrue(receiver.machine().components().containsValue("modem"), "Receiver modem is not visible: " + receiver.machine().components());
        helper.assertTrue(sender.machine().components().containsValue("modem"), "Sender modem is not visible: " + sender.machine().components());
        helper.assertTrue(receiver.toggleMachine(), "Receiver computer did not start");
        helper.runAtTickTime(100, () -> {
            helper.assertTrue(modemPortOpen(helper, receiver, 123), "Receiver modem port was not open before sender broadcast");
            helper.assertTrue(sender.toggleMachine(), "Sender computer did not start");
        });
        helper.runAtTickTime(170, () -> {
            helper.assertTrue(receiver.machine().isRunning(), "Receiver stopped while waiting for modem message: " + receiver.machine().lastError());
            helper.assertTrue(sender.machine().isRunning(), "Sender stopped while broadcasting modem message: " + sender.machine().lastError());
            assertNextSignal(helper, receiver, "modem_received", "payload");
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void machineBlocksDropStoredItemsWhenBroken(final GameTestHelper helper) {
        final BlockPos computerPos = new BlockPos(1, 1, 1);
        final BlockPos diskDrivePos = new BlockPos(2, 1, 1);

        helper.killAllEntities();
        helper.setBlock(computerPos, ModBlocks.COMPUTER_CASE_TIER1.get());
        helper.setBlock(diskDrivePos, ModBlocks.DISK_DRIVE.get());

        final ComputerCaseBlockEntity computer = helper.getBlockEntity(computerPos);
        final DiskDriveBlockEntity diskDrive = helper.getBlockEntity(diskDrivePos);
        computer.setItem(ComputerCaseBlockEntity.SLOT_CPU, new ItemStack(ModItems.CPU_TIER1.get()));
        diskDrive.setItem(DiskDriveBlockEntity.SLOT_FLOPPY, openOsFloppyStack());

        helper.getLevel().destroyBlock(helper.absolutePos(computerPos), true);
        helper.getLevel().destroyBlock(helper.absolutePos(diskDrivePos), true);
        helper.runAtTickTime(1, () -> {
            assertDroppedItem(helper, ModItems.CPU_TIER1.get());
            assertDroppedItem(helper, ModItems.FLOPPY.get());
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void adapterDropsInstalledUpgradeWhenBroken(final GameTestHelper helper) {
        final BlockPos adapterPos = new BlockPos(1, 1, 1);

        helper.killAllEntities();
        helper.setBlock(adapterPos, ModBlocks.ADAPTER.get());

        final li.cil.oc.common.blockentity.AdapterBlockEntity adapter = helper.getBlockEntity(adapterPos);
        adapter.setItem(0, new ItemStack(ModItems.INVENTORY_CONTROLLER_UPGRADE.get()));

        helper.getLevel().destroyBlock(helper.absolutePos(adapterPos), true);
        helper.runAtTickTime(1, () -> {
            assertDroppedItem(helper, ModItems.INVENTORY_CONTROLLER_UPGRADE.get());
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void brokenComputerCaseDropsFlushedHardDiskData(final GameTestHelper helper) {
        final BlockPos computerPos = new BlockPos(1, 1, 1);

        helper.killAllEntities();
        helper.setBlock(computerPos, ModBlocks.COMPUTER_CASE_TIER1.get());

        final ComputerCaseBlockEntity computer = helper.getBlockEntity(computerPos);
        computer.setItem(ComputerCaseBlockEntity.SLOT_HDD, bootableHardDiskStack(helper, "computer.pushSignal('booted')"));

        final String filesystemAddress = componentAddress(computer, "filesystem");
        helper.assertTrue(filesystemAddress != null, "Computer has no filesystem component: " + computer.machine().components());
        writeFile(helper, computer, filesystemAddress, "saved.txt", "persisted");

        helper.getLevel().destroyBlock(helper.absolutePos(computerPos), true);
        helper.runAtTickTime(1, () -> {
            assertHardDiskContainsFile(helper, droppedItemStack(helper, ModItems.HDD_TIER1.get()), "saved.txt");
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
        final KeyboardBlockEntity keyboard = helper.getBlockEntity(keyboardPos);
        final ComputerCaseBlockEntity computer = helper.getBlockEntity(computerPos);
        helper.assertTrue(screen.node().network() == computer.node().network(), "Screen and computer are not on the same network");
        screen.keyDown('a', 30, null);
        screen.keyUp('a', 30, null);

        helper.runAtTickTime(5, () -> {
            assertNextSignal(helper, computer, "key_down", keyboard.node().address(), (int) 'a', 30);
            assertNextSignal(helper, computer, "key_up", keyboard.node().address(), (int) 'a', 30);
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

    private static void installBootComputer(final ComputerCaseBlockEntity computer, final ItemStack bootDisk) {
        computer.setItem(ComputerCaseBlockEntity.SLOT_CARD_0, new ItemStack(ModItems.NETWORK_CARD.get()));
        computer.setItem(ComputerCaseBlockEntity.SLOT_CPU, new ItemStack(ModItems.CPU_TIER1.get()));
        computer.setItem(ComputerCaseBlockEntity.SLOT_MEMORY_0, new ItemStack(ModItems.MEMORY_TIER1.get()));
        computer.setItem(ComputerCaseBlockEntity.SLOT_HDD, bootDisk);
        computer.setItem(ComputerCaseBlockEntity.SLOT_EEPROM, luaBiosEepromStack());
    }

    private static ItemStack bootableHardDiskStack(final GameTestHelper helper, final String initLua) {
        final ItemStack stack = new ItemStack(ModItems.HDD_TIER1.get());
        final DriverItem driver = Driver.driverFor(stack);
        helper.assertTrue(driver != null, "Hard disk has no item driver");
        final ManagedEnvironment environment = driver.createEnvironment(stack, null);
        helper.assertTrue(environment != null, "Hard disk driver did not create an environment");
        helper.assertTrue(environment.node() instanceof li.cil.oc.api.network.Component, "Hard disk environment has no filesystem component");
        final li.cil.oc.api.network.Component component = (li.cil.oc.api.network.Component) environment.node();
        try {
            final Object handle = component.invoke("open", null, "init.lua", "w")[0];
            component.invoke("write", null, handle, initLua.getBytes(StandardCharsets.UTF_8));
            component.invoke("close", null, handle);
            environment.save(new CompoundTag());
        } catch (Exception e) {
            helper.fail("Failed to prepare bootable hard disk: " + e.getMessage());
        }
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

    private static void assertDatabaseCapacity(final GameTestHelper helper, final ItemStack stack, final int capacity) {
        final DriverItem driver = Driver.driverFor(stack);
        helper.assertTrue(driver != null, "No driver for " + stack);
        final ManagedEnvironment environment = driver.createEnvironment(stack, null);
        helper.assertTrue(environment instanceof li.cil.oc.api.internal.Database, "No database environment for " + stack);
        final li.cil.oc.api.internal.Database database = (li.cil.oc.api.internal.Database) environment;
        helper.assertTrue(database.size() == capacity, "Expected " + stack + " to have " + capacity + " database slots but got " + database.size());
    }

    private static void assertWirelessModem(final GameTestHelper helper, final ItemStack stack, final boolean wired, final double strength) {
        final DriverItem driver = Driver.driverFor(stack);
        helper.assertTrue(driver != null, "No driver for " + stack);
        final ManagedEnvironment environment = driver.createEnvironment(stack, null);
        helper.assertTrue(environment != null, "No wireless modem environment for " + stack);
        helper.assertTrue(environment.node() instanceof li.cil.oc.api.network.Component, "Wireless modem has no component for " + stack);
        final li.cil.oc.api.network.Component component = (li.cil.oc.api.network.Component) environment.node();
        final Object[] wirelessResult = invokeComponent(helper, component, "isWireless");
        helper.assertTrue(wirelessResult.length == 1 && Boolean.TRUE.equals(wirelessResult[0]), "Expected wireless modem for " + stack);
        final Object[] wiredResult = invokeComponent(helper, component, "isWired");
        helper.assertTrue(wiredResult.length == 1 && Boolean.valueOf(wired).equals(wiredResult[0]), "Expected " + stack + " wired=" + wired);
        final Object[] strengthResult = invokeComponent(helper, component, "getStrength");
        helper.assertTrue(strengthResult.length == 1 && Double.valueOf(strength).equals(strengthResult[0]), "Expected " + stack + " strength " + strength);
    }

    private static void assertScreenTier(final GameTestHelper helper, final ScreenBlockEntity screen, final int tier, final int width, final int height, final TextBuffer.ColorDepth depth) {
        helper.assertTrue(screen.tier() == tier, "Expected screen tier " + tier + " but got " + screen.tier());
        helper.assertTrue(screen.getMaximumWidth() == width, "Expected screen max width " + width + " but got " + screen.getMaximumWidth());
        helper.assertTrue(screen.getMaximumHeight() == height, "Expected screen max height " + height + " but got " + screen.getMaximumHeight());
        helper.assertTrue(screen.getWidth() == width, "Expected screen width " + width + " but got " + screen.getWidth());
        helper.assertTrue(screen.getHeight() == height, "Expected screen height " + height + " but got " + screen.getHeight());
        helper.assertTrue(screen.getMaximumColorDepth() == depth, "Expected screen depth " + depth + " but got " + screen.getMaximumColorDepth());
    }

    private static void assertComputerCaseTier(final GameTestHelper helper, final ComputerCaseBlockEntity computerCase, final int tier) {
        helper.assertTrue(computerCase.tier() == tier, "Expected computer case tier " + tier + " but got " + computerCase.tier());
    }

    private static Object[] invokeComponent(final GameTestHelper helper, final li.cil.oc.api.network.Component component, final String method) {
        try {
            return component.invoke(method, null);
        } catch (Exception e) {
            helper.fail("Component invocation failed: " + method + " " + e.getMessage());
            return new Object[0];
        }
    }

    private static boolean modemPortOpen(final GameTestHelper helper, final ComputerCaseBlockEntity computer, final int port) {
        final String modemAddress = componentAddress(computer, "modem");
        helper.assertTrue(modemAddress != null, "Computer has no modem component: " + computer.machine().components());
        try {
            final Object[] result = computer.machine().invoke(modemAddress, "isOpen", new Object[]{port});
            return result.length == 1 && Boolean.TRUE.equals(result[0]);
        } catch (Exception e) {
            helper.fail("Failed to check modem port: " + e.getMessage());
            return false;
        }
    }

    private static String componentAddress(final ComputerCaseBlockEntity computer, final String componentType) {
        for (Map.Entry<String, String> entry : computer.machine().components().entrySet()) {
            if (componentType.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    private static void assertDroppedItem(final GameTestHelper helper, final Item item) {
        droppedItemStack(helper, item);
    }

    private static ItemStack namedStack(final Item item, final String name) {
        final ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        return stack;
    }

    private static void assertInvokeResult(
        final GameTestHelper helper,
        final ComputerCaseBlockEntity computer,
        final String address,
        final String method,
        final Object[] args,
        final Object expected) throws Exception {
        final Object[] result = computer.machine().invoke(address, method, args);
        helper.assertTrue(result.length == 1 && expected.equals(result[0]), "Expected " + method + " to return " + expected + " but got " + (result.length == 0 ? "<empty>" : result[0]));
    }

    private static ItemStack droppedItemStack(final GameTestHelper helper, final Item item) {
        for (ItemEntity entity : helper.getEntities(EntityType.ITEM)) {
            if (entity.getItem().is(item)) {
                return entity.getItem();
            }
        }
        helper.fail("Expected dropped item " + item);
        return ItemStack.EMPTY;
    }

    private static void writeFile(final GameTestHelper helper, final ComputerCaseBlockEntity computer, final String filesystemAddress, final String path, final String data) {
        try {
            final Object handle = computer.machine().invoke(filesystemAddress, "open", new Object[]{path, "w"})[0];
            computer.machine().invoke(filesystemAddress, "write", new Object[]{handle, data.getBytes(StandardCharsets.UTF_8)});
            computer.machine().invoke(filesystemAddress, "close", new Object[]{handle});
        } catch (Exception e) {
            helper.fail("Failed to write file " + path + ": " + e.getMessage());
        }
    }

    private static void assertHardDiskContainsFile(final GameTestHelper helper, final ItemStack stack, final String path) {
        final DriverItem driver = Driver.driverFor(stack);
        helper.assertTrue(driver != null, "Dropped hard disk has no item driver");
        final ManagedEnvironment environment = driver.createEnvironment(stack, null);
        helper.assertTrue(environment != null && environment.node() instanceof li.cil.oc.api.network.Component, "Dropped hard disk has no filesystem component");
        final li.cil.oc.api.network.Component component = (li.cil.oc.api.network.Component) environment.node();
        try {
            final Object[] result = component.invoke("exists", null, path);
            helper.assertTrue(result.length == 1 && Boolean.TRUE.equals(result[0]), "Dropped hard disk is missing " + path);
        } catch (Exception e) {
            helper.fail("Failed to inspect dropped hard disk: " + e.getMessage());
        }
    }

    private static void typeKey(final ScreenBlockEntity screen, final char character, final int code) {
        screen.keyDown(character, code, null);
        screen.keyUp(character, code, null);
    }

    private static void typeText(final ScreenBlockEntity screen, final String text) {
        for (int index = 0; index < text.length(); index++) {
            final char character = text.charAt(index);
            typeKey(screen, character, keyCode(character));
        }
    }

    private static int keyCode(final char character) {
        return switch (character) {
            case ' ' -> 0x39;
            case 'c' -> 0x2E;
            case 'e' -> 0x12;
            case 'h' -> 0x23;
            case 'k' -> 0x25;
            case 'o' -> 0x18;
            default -> throw new IllegalArgumentException("No test key code for " + character);
        };
    }

    private static int countOccurrences(final String text, final String value) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(value, index)) >= 0) {
            count++;
            index += value.length();
        }
        return count;
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

    private record DatabaseCloneContext(Node node, double[] pauseSeconds) implements Context {
        @Override
        public boolean canInteract(final String player) {
            return true;
        }

        @Override
        public boolean isRunning() {
            return true;
        }

        @Override
        public boolean isPaused() {
            return false;
        }

        @Override
        public boolean start() {
            return true;
        }

        @Override
        public boolean pause(final double seconds) {
            pauseSeconds[0] = seconds;
            return true;
        }

        @Override
        public boolean stop() {
            return true;
        }

        @Override
        public void consumeCallBudget(final double callCost) {
        }

        @Override
        public boolean signal(final String name, final Object... args) {
            return true;
        }
    }

    private static final class AdapterTestBlockDriver implements DriverBlock {
        @Override
        public boolean worksWith(final net.minecraft.world.level.Level world, final BlockPos pos, final Direction side) {
            return world != null && pos != null && world.getBlockState(pos).is(Blocks.EMERALD_BLOCK);
        }

        @Override
        public ManagedEnvironment createEnvironment(final net.minecraft.world.level.Level world, final BlockPos pos, final Direction side) {
            return new AdapterTestEnvironment();
        }
    }

    private static final class AdapterTestEnvironment extends AbstractManagedEnvironment {
        private static final String COMPONENT_NAME = "adapter_test";

        private AdapterTestEnvironment() {
            setNode(Network.newNode(this, Visibility.Network)
                .withComponent(COMPONENT_NAME, Visibility.Network)
                .create());
        }

        @Callback
        public Object[] ping(final Context context, final Arguments arguments) {
            return new Object[]{"pong"};
        }
    }

    private NeoOpenComputersGameTests() {
    }
}
