package li.cil.oc.common.gametest;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.api.API;
import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.DriverBlock;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.driver.item.Chargeable;
import li.cil.oc.api.driver.item.Container;
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
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.ComponentConnector;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.common.ItemRegistry;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModEeproms;
import li.cil.oc.common.ModItems;
import li.cil.oc.api.Network;
import li.cil.oc.common.blockentity.CableBlockEntity;
import li.cil.oc.common.blockentity.AdapterBlockEntity;
import li.cil.oc.common.blockentity.ComputerCaseBlockEntity;
import li.cil.oc.common.blockentity.DisassemblerBlockEntity;
import li.cil.oc.common.blockentity.DiskDriveBlockEntity;
import li.cil.oc.common.blockentity.GeolyzerBlockEntity;
import li.cil.oc.common.blockentity.KeyboardBlockEntity;
import li.cil.oc.common.blockentity.ScreenBlockEntity;
import li.cil.oc.common.blockentity.AssemblerBlockEntity;
import li.cil.oc.common.blockentity.TransposerBlockEntity;
import li.cil.oc.common.block.ComputerCaseBlock;
import li.cil.oc.common.item.TabletItem;
import li.cil.oc.common.template.AssemblerTemplate;
import li.cil.oc.common.template.AssemblerTemplateImc;
import li.cil.oc.common.template.AssemblerTemplates;
import li.cil.oc.common.template.DisassemblerTemplate;
import li.cil.oc.common.template.DisassemblerTemplateImc;
import li.cil.oc.common.template.DisassemblerTemplates;
import net.neoforged.fml.InterModComms;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.IFluidTank;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

@GameTestHolder(NeoOpenComputers.MODID)
@PrefixGameTestTemplate(false)
public final class NeoOpenComputersGameTests {
    @GameTest(template = "empty")
    public static void registeredContentAvailable(final GameTestHelper helper) {
        ModBlocks.ADAPTER.get();
        ModBlocks.ASSEMBLER.get();
        ModBlocks.CABLE.get();
        ModBlocks.COMPUTER_CASE_TIER1.get();
        ModBlocks.COMPUTER_CASE_TIER2.get();
        ModBlocks.COMPUTER_CASE_TIER3.get();
        ModBlocks.DISASSEMBLER.get();
        ModBlocks.DISK_DRIVE.get();
        ModBlocks.GEOLYZER.get();
        ModBlocks.HOLOGRAM_TIER1.get();
        ModBlocks.HOLOGRAM_TIER2.get();
        ModBlocks.SCREEN_TIER1.get();
        ModBlocks.SCREEN_TIER2.get();
        ModBlocks.SCREEN_TIER3.get();
        ModBlocks.KEYBOARD.get();
        ModBlocks.MOTION_SENSOR.get();
        ModBlocks.REDSTONE_IO.get();
        ModBlocks.TRANSPOSER.get();
        ModBlocks.WAYPOINT.get();
        ModItems.ADAPTER.get();
        ModItems.ANALYZER.get();
        ModItems.ASSEMBLER.get();
        ModItems.BATTERY_UPGRADE_TIER1.get();
        ModItems.BATTERY_UPGRADE_TIER2.get();
        ModItems.BATTERY_UPGRADE_TIER3.get();
        ModItems.CUTTING_WIRE.get();
        ModItems.ACID.get();
        ModItems.RAW_CIRCUIT_BOARD.get();
        ModItems.CIRCUIT_BOARD.get();
        ModItems.PRINTED_CIRCUIT_BOARD.get();
        ModItems.CARD.get();
        ModItems.TRANSISTOR.get();
        ModItems.MICROCHIP_TIER1.get();
        ModItems.MICROCHIP_TIER2.get();
        ModItems.MICROCHIP_TIER3.get();
        ModItems.ALU.get();
        ModItems.CONTROL_UNIT.get();
        ModItems.DISK_PLATTER.get();
        ModItems.INTERWEB.get();
        ModItems.BUTTON_GROUP.get();
        ModItems.ARROW_KEYS.get();
        ModItems.NUM_PAD.get();
        ModItems.CABLE.get();
        ModItems.CARD_CONTAINER_TIER1.get();
        ModItems.CARD_CONTAINER_TIER2.get();
        ModItems.CARD_CONTAINER_TIER3.get();
        ModItems.TABLET_CASE_TIER1.get();
        ModItems.TABLET_CASE_TIER2.get();
        ModItems.TABLET_CASE_CREATIVE.get();
        ModItems.TABLET.get();
        ModItems.CPU_TIER1.get();
        ModItems.CPU_TIER2.get();
        ModItems.CPU_TIER3.get();
        ModItems.DATA_CARD_TIER1.get();
        ModItems.DATA_CARD_TIER2.get();
        ModItems.DATA_CARD_TIER3.get();
        ModItems.DATABASE_UPGRADE_TIER1.get();
        ModItems.DATABASE_UPGRADE_TIER2.get();
        ModItems.DATABASE_UPGRADE_TIER3.get();
        ModItems.EEPROM.get();
        ModItems.FLOPPY.get();
        ModItems.GEOLYZER.get();
        ModItems.HOLOGRAM_TIER1.get();
        ModItems.HOLOGRAM_TIER2.get();
        ModItems.GRAPHICS_CARD_TIER1.get();
        ModItems.GRAPHICS_CARD_TIER2.get();
        ModItems.GRAPHICS_CARD_TIER3.get();
        ModItems.HOVER_UPGRADE_TIER1.get();
        ModItems.HOVER_UPGRADE_TIER2.get();
        ModItems.HDD_TIER1.get();
        ModItems.HDD_TIER2.get();
        ModItems.HDD_TIER3.get();
        ModItems.INVENTORY_CONTROLLER_UPGRADE.get();
        ModItems.INVENTORY_UPGRADE.get();
        ModItems.CRAFTING_UPGRADE.get();
        ModItems.EXPERIENCE_UPGRADE.get();
        ModItems.INTERNET_CARD.get();
        ModItems.LINKED_CARD.get();
        ModItems.MEMORY_TIER1.get();
        ModItems.MEMORY_TIER2.get();
        ModItems.MEMORY_TIER3.get();
        ModItems.MOTION_SENSOR.get();
        ModItems.NAVIGATION_UPGRADE.get();
        ModItems.NETWORK_CARD.get();
        ModItems.PISTON_UPGRADE.get();
        ModItems.REDSTONE_IO.get();
        ModItems.SOLAR_GENERATOR_UPGRADE.get();
        ModItems.STICKY_PISTON_UPGRADE.get();
        ModItems.SIGN_UPGRADE.get();
        ModItems.TANK_UPGRADE.get();
        ModItems.TANK_CONTROLLER_UPGRADE.get();
        ModItems.TRADING_UPGRADE.get();
        ModItems.TRANSPOSER.get();
        ModItems.UPGRADE_CONTAINER_TIER1.get();
        ModItems.UPGRADE_CONTAINER_TIER2.get();
        ModItems.UPGRADE_CONTAINER_TIER3.get();
        ModItems.WAYPOINT.get();
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
        assertItemTier(helper, new ItemStack(ModItems.DATA_CARD_TIER3.get()), 2);
        assertItemTier(helper, new ItemStack(ModItems.DATABASE_UPGRADE_TIER1.get()), 0);
        assertItemTier(helper, new ItemStack(ModItems.DATABASE_UPGRADE_TIER2.get()), 1);
        assertItemTier(helper, new ItemStack(ModItems.DATABASE_UPGRADE_TIER3.get()), 2);
        assertItemTier(helper, new ItemStack(ModItems.BATTERY_UPGRADE_TIER1.get()), 0);
        assertItemTier(helper, new ItemStack(ModItems.BATTERY_UPGRADE_TIER2.get()), 1);
        assertItemTier(helper, new ItemStack(ModItems.BATTERY_UPGRADE_TIER3.get()), 2);
        assertItemTier(helper, new ItemStack(ModItems.CARD_CONTAINER_TIER1.get()), 0);
        assertItemTier(helper, new ItemStack(ModItems.CARD_CONTAINER_TIER2.get()), 1);
        assertItemTier(helper, new ItemStack(ModItems.CARD_CONTAINER_TIER3.get()), 2);
        assertItemTier(helper, new ItemStack(ModItems.MEMORY_TIER1.get()), 0);
        assertItemTier(helper, new ItemStack(ModItems.MEMORY_TIER2.get()), 1);
        assertItemTier(helper, new ItemStack(ModItems.MEMORY_TIER3.get()), 2);
        assertItemTier(helper, new ItemStack(ModItems.GRAPHICS_CARD_TIER1.get()), 0);
        assertItemTier(helper, new ItemStack(ModItems.GRAPHICS_CARD_TIER2.get()), 1);
        assertItemTier(helper, new ItemStack(ModItems.GRAPHICS_CARD_TIER3.get()), 2);
        assertItemTier(helper, new ItemStack(ModItems.HOVER_UPGRADE_TIER1.get()), 0);
        assertItemTier(helper, new ItemStack(ModItems.HOVER_UPGRADE_TIER2.get()), 1);
        assertItemTier(helper, new ItemStack(ModItems.HDD_TIER1.get()), 0);
        assertItemTier(helper, new ItemStack(ModItems.HDD_TIER2.get()), 1);
        assertItemTier(helper, new ItemStack(ModItems.HDD_TIER3.get()), 2);
        assertItemTier(helper, new ItemStack(ModItems.INVENTORY_UPGRADE.get()), 0);
        assertItemTier(helper, new ItemStack(ModItems.INVENTORY_CONTROLLER_UPGRADE.get()), 1);
        assertItemTier(helper, new ItemStack(ModItems.CRAFTING_UPGRADE.get()), 0);
        assertItemTier(helper, new ItemStack(ModItems.EXPERIENCE_UPGRADE.get()), 2);
        assertItemTier(helper, new ItemStack(ModItems.INTERNET_CARD.get()), 1);
        assertItemTier(helper, new ItemStack(ModItems.PISTON_UPGRADE.get()), 0);
        assertItemTier(helper, new ItemStack(ModItems.STICKY_PISTON_UPGRADE.get()), 1);
        assertItemTier(helper, new ItemStack(ModItems.SIGN_UPGRADE.get()), 0);
        assertItemTier(helper, new ItemStack(ModItems.TRADING_UPGRADE.get()), 1);
        assertItemTier(helper, new ItemStack(ModItems.SOLAR_GENERATOR_UPGRADE.get()), 1);
        assertItemTier(helper, new ItemStack(ModItems.TANK_UPGRADE.get()), 0);
        assertItemTier(helper, new ItemStack(ModItems.TANK_CONTROLLER_UPGRADE.get()), 1);
        assertItemTier(helper, new ItemStack(ModItems.UPGRADE_CONTAINER_TIER1.get()), 0);
        assertItemTier(helper, new ItemStack(ModItems.UPGRADE_CONTAINER_TIER2.get()), 1);
        assertItemTier(helper, new ItemStack(ModItems.UPGRADE_CONTAINER_TIER3.get()), 2);
        assertItemTier(helper, new ItemStack(ModItems.WIRELESS_NETWORK_CARD_TIER1.get()), 0);
        assertItemTier(helper, new ItemStack(ModItems.WIRELESS_NETWORK_CARD_TIER2.get()), 1);
        assertItemTier(helper, new ItemStack(ModItems.LINKED_CARD.get()), 1);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void tabletCaseItemsReportAssemblyTier(final GameTestHelper helper) {
        helper.assertTrue(ModItems.TABLET_CASE_TIER1.get().tier() == 0, "Tier 1 tablet case did not report tier 0");
        helper.assertTrue(ModItems.TABLET_CASE_TIER2.get().tier() == 1, "Tier 2 tablet case did not report tier 1");
        helper.assertTrue(ModItems.TABLET_CASE_CREATIVE.get().tier() == 3, "Creative tablet case did not report tier 3");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void tabletItemStoresCharge(final GameTestHelper helper) {
        final ItemStack stack = new ItemStack(ModItems.TABLET.get());
        final TabletItem tablet = (TabletItem) stack.getItem();

        tablet.setMaxCharge(stack, 1000D);
        helper.assertTrue(stack.getMaxStackSize() == 1, "Tablet item should not stack");
        helper.assertTrue(tablet.maxCharge(stack) == 1000D, "Tablet max charge did not persist");
        helper.assertTrue(tablet.charge(stack, 250D, false) == 250D, "Tablet did not accept initial charge");
        helper.assertTrue(tablet.getCharge(stack) == 250D, "Tablet charge did not persist");
        helper.assertTrue(tablet.charge(stack, 1000D, true) == 750D, "Tablet simulated charge did not cap to remaining capacity");
        helper.assertTrue(tablet.getCharge(stack) == 250D, "Tablet simulated charge mutated stored charge");
        helper.assertTrue(tablet.charge(stack, 1000D, false) == 750D, "Tablet did not cap accepted charge");
        helper.assertTrue(tablet.getCharge(stack) == 1000D, "Tablet charge did not cap at max");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void tabletItemShowsEnergyBar(final GameTestHelper helper) {
        final ItemStack stack = new ItemStack(ModItems.TABLET.get());
        final TabletItem tablet = (TabletItem) stack.getItem();

        tablet.setMaxCharge(stack, 1000D);
        tablet.setCharge(stack, 250D);
        helper.assertTrue(tablet.isBarVisible(stack), "Tablet energy bar should be visible");
        helper.assertTrue(tablet.getBarWidth(stack) == 3, "Tablet energy bar should show current charge ratio");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void tabletItemPersistsAssemblyData(final GameTestHelper helper) {
        final ItemStack stack = new ItemStack(ModItems.TABLET.get());
        final TabletItem tablet = (TabletItem) stack.getItem();
        final ItemStack container = new ItemStack(ModItems.CARD_CONTAINER_TIER1.get());
        final ItemStack component = new ItemStack(ModItems.NETWORK_CARD.get());

        tablet.setTier(stack, 1);
        tablet.setRunning(stack, true);
        tablet.setContainer(stack, container);
        tablet.setComponent(stack, 2, component);

        helper.assertTrue(tablet.tier(stack) == 1, "Tablet tier did not persist");
        helper.assertTrue(tablet.isRunning(stack), "Tablet running state did not persist");
        helper.assertTrue(ItemStack.isSameItemSameComponents(container, tablet.getContainer(stack)), "Tablet container did not persist");
        helper.assertTrue(ItemStack.isSameItemSameComponents(component, tablet.getComponent(stack, 2)), "Tablet component did not persist");
        helper.assertTrue(tablet.getComponent(stack, 32).isEmpty(), "Tablet out-of-range component read should be empty");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void tabletItemAssemblesFromCaseAndComponents(final GameTestHelper helper) {
        final TabletItem tablet = ModItems.TABLET.get();
        final ItemStack container = new ItemStack(ModItems.CARD_CONTAINER_TIER1.get());
        final ItemStack cpu = new ItemStack(ModItems.CPU_TIER1.get());
        final ItemStack memory = new ItemStack(ModItems.MEMORY_TIER1.get());

        final ItemStack stack = tablet.assembleFromCase(
            new ItemStack(ModItems.TABLET_CASE_TIER2.get()),
            container,
            cpu,
            memory);

        helper.assertTrue(stack.is(ModItems.TABLET.get()), "Assembler did not create tablet item");
        helper.assertTrue(tablet.tier(stack) == 1, "Assembled tablet tier did not match case");
        helper.assertTrue(tablet.maxCharge(stack) > 0D, "Assembled tablet has no energy capacity");
        helper.assertTrue(tablet.getCharge(stack) == tablet.maxCharge(stack), "Assembled tablet should start fully charged");
        helper.assertTrue(ItemStack.isSameItemSameComponents(container, tablet.getContainer(stack)), "Assembled tablet container missing");
        helper.assertTrue(tablet.getComponent(stack, 0).is(ModItems.SCREEN_TIER1.get()), "Assembled tablet screen missing");
        helper.assertTrue(ItemStack.isSameItemSameComponents(cpu, tablet.getComponent(stack, 1)), "Assembled tablet CPU missing");
        helper.assertTrue(ItemStack.isSameItemSameComponents(memory, tablet.getComponent(stack, 2)), "Assembled tablet memory missing");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void tabletItemDisassemblesToIngredients(final GameTestHelper helper) {
        final TabletItem tablet = ModItems.TABLET.get();
        final ItemStack container = new ItemStack(ModItems.CARD_CONTAINER_TIER1.get());
        final ItemStack cpu = new ItemStack(ModItems.CPU_TIER1.get());
        final ItemStack memory = new ItemStack(ModItems.MEMORY_TIER1.get());
        final ItemStack stack = tablet.assembleFromCase(
            new ItemStack(ModItems.TABLET_CASE_TIER2.get()),
            container,
            cpu,
            memory);

        final ItemStack[] ingredients = tablet.disassembleToIngredients(stack);

        helper.assertTrue(ingredients.length == 4, "Disassembled tablet returned wrong ingredient count");
        helper.assertTrue(ingredients[0].is(ModItems.TABLET_CASE_TIER2.get()), "Disassembled tablet case did not match tier");
        helper.assertTrue(ItemStack.isSameItemSameComponents(container, ingredients[1]), "Disassembled tablet container missing");
        helper.assertTrue(ItemStack.isSameItemSameComponents(cpu, ingredients[2]), "Disassembled tablet CPU missing");
        helper.assertTrue(ItemStack.isSameItemSameComponents(memory, ingredients[3]), "Disassembled tablet memory missing");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void transposerInspectsAdjacentFluidTanks(final GameTestHelper helper) {
        final BlockPos pos = new BlockPos(1, 1, 1);
        final int side = Direction.WEST.get3DDataValue();
        helper.setBlock(pos, ModBlocks.TRANSPOSER.get());
        helper.setBlock(pos.relative(Direction.WEST), Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
        final TransposerBlockEntity transposer = helper.getBlockEntity(pos);
        final li.cil.oc.api.network.Component component = (li.cil.oc.api.network.Component) transposer.node();

        final Object[] count = invokeComponent(helper, component, "getTankCount", side);
        final Object[] level = invokeComponent(helper, component, "getTankLevel", side, 1);
        final Object[] capacity = invokeComponent(helper, component, "getTankCapacity", side, 1);
        final Object[] fluid = invokeComponent(helper, component, "getFluidInTank", side, 1);
        final Object[] allFluids = invokeComponent(helper, component, "getFluidInTank", side);

        helper.assertTrue(Integer.valueOf(1).equals(count[0]), "Transposer did not see adjacent tank");
        helper.assertTrue(Integer.valueOf(1000).equals(level[0]), "Transposer did not read tank level");
        helper.assertTrue(Integer.valueOf(1000).equals(capacity[0]), "Transposer did not read tank capacity");
        helper.assertTrue("minecraft:water".equals(fluid[0]), "Transposer did not report water fluid id");
        helper.assertTrue(Integer.valueOf(1000).equals(fluid[1]), "Transposer did not report fluid amount");
        helper.assertTrue(Integer.valueOf(1000).equals(fluid[2]), "Transposer did not report fluid capacity");
        assertSingleWaterTankDescription(helper, allFluids, "Transposer");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void transposerTransfersFluidBetweenAdjacentTanks(final GameTestHelper helper) {
        final BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.TRANSPOSER.get());
        helper.setBlock(pos.relative(Direction.WEST), Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
        helper.setBlock(pos.relative(Direction.EAST), Blocks.CAULDRON);
        final TransposerBlockEntity transposer = helper.getBlockEntity(pos);
        final li.cil.oc.api.network.Component component = (li.cil.oc.api.network.Component) transposer.node();

        final Object[] result = invokeComponent(
            helper,
            component,
            "transferFluid",
            Direction.WEST.get3DDataValue(),
            Direction.EAST.get3DDataValue(),
            1000);

        helper.assertTrue(Boolean.TRUE.equals(result[0]), "Transposer did not transfer fluid");
        helper.assertTrue(Integer.valueOf(1000).equals(result[1]), "Transposer reported wrong transferred amount");
        helper.assertTrue(helper.getBlockState(pos.relative(Direction.WEST)).is(Blocks.CAULDRON), "Source cauldron was not drained");
        helper.assertTrue(helper.getBlockState(pos.relative(Direction.EAST)).is(Blocks.WATER_CAULDRON), "Sink cauldron was not filled");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void transposerPausesAfterFluidTransfer(final GameTestHelper helper) {
        final BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.TRANSPOSER.get());
        helper.setBlock(pos.relative(Direction.WEST), Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
        helper.setBlock(pos.relative(Direction.EAST), Blocks.CAULDRON);
        final TransposerBlockEntity transposer = helper.getBlockEntity(pos);
        final li.cil.oc.api.network.Component component = (li.cil.oc.api.network.Component) transposer.node();
        final double[] pauseSeconds = new double[]{-1D};
        final Context context = new RecordingContext(component, pauseSeconds);

        try {
            final Object[] result = component.invoke(
                "transferFluid",
                context,
                Direction.WEST.get3DDataValue(),
                Direction.EAST.get3DDataValue(),
                1000);
            helper.assertTrue(Boolean.TRUE.equals(result[0]), "Transposer did not transfer fluid");
            helper.assertTrue(Double.compare(0.25D, pauseSeconds[0]) == 0, "Transposer did not pause for moved fluid amount");
            helper.succeed();
        } catch (Exception e) {
            helper.fail("Component invocation failed: transferFluid " + e.getMessage());
        }
    }

    @GameTest(template = "empty")
    public static void transposerStoresStacksInDatabase(final GameTestHelper helper) {
        final BlockPos pos = new BlockPos(1, 1, 1);
        final int side = Direction.WEST.get3DDataValue();
        helper.setBlock(pos, ModBlocks.TRANSPOSER.get());
        helper.setBlock(pos.relative(Direction.WEST), Blocks.CHEST);
        final TransposerBlockEntity transposer = helper.getBlockEntity(pos);
        final li.cil.oc.api.network.Component component = (li.cil.oc.api.network.Component) transposer.node();

        final net.minecraft.world.Container chest = helper.getBlockEntity(pos.relative(Direction.WEST));
        chest.setItem(0, new ItemStack(Items.DIAMOND, 5));

        final DriverItem databaseDriver = Driver.driverFor(new ItemStack(ModItems.DATABASE_UPGRADE_TIER1.get()));
        helper.assertTrue(databaseDriver != null, "No driver for database upgrade");
        final ManagedEnvironment databaseEnvironment = databaseDriver.createEnvironment(new ItemStack(ModItems.DATABASE_UPGRADE_TIER1.get()), null);
        helper.assertTrue(databaseEnvironment instanceof li.cil.oc.api.internal.Database, "Database upgrade did not create database environment");
        final li.cil.oc.api.internal.Database database = (li.cil.oc.api.internal.Database) databaseEnvironment;
        database.setStackInSlot(0, new ItemStack(Items.GOLD_INGOT));
        Network.joinNewNetwork(databaseEnvironment.node());
        databaseEnvironment.node().connect(transposer.node());

        final String databaseAddress = databaseEnvironment.node().address();
        final Object[] store = invokeComponent(helper, component, "store", side, 1, databaseAddress, 1);
        final ItemStack stored = database.getStackInSlot(0);
        final Object[] matching = invokeComponent(helper, component, "compareStackToDatabase", side, 1, databaseAddress, 1);
        database.setStackInSlot(0, new ItemStack(Items.DIRT));
        final Object[] mismatching = invokeComponent(helper, component, "compareStackToDatabase", side, 1, databaseAddress, 1);

        helper.assertTrue(Boolean.TRUE.equals(store[0]), "Transposer store did not report overwritten database slot");
        helper.assertTrue(stored.is(Items.DIAMOND) && stored.getCount() == 5, "Transposer did not store stack in database");
        helper.assertTrue(Boolean.TRUE.equals(matching[0]), "Transposer did not match stored database stack");
        helper.assertTrue(Boolean.FALSE.equals(mismatching[0]), "Transposer matched different database stack");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void geolyzerScanConsumesEnergy(final GameTestHelper helper) {
        final BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.GEOLYZER.get());
        helper.setBlock(pos.relative(Direction.WEST), Blocks.STONE);
        final GeolyzerBlockEntity geolyzer = helper.getBlockEntity(pos);
        final ComponentConnector component = (ComponentConnector) geolyzer.node();
        component.setLocalBufferSize(10D);
        component.changeBuffer(10D);

        final Object[] result = invokeComponent(helper, component, "scan", -1, 0, 0, 1, 1, 1);

        helper.assertTrue(result.length == 1 && result[0] instanceof float[], "Geolyzer scan did not return data");
        helper.assertTrue(Double.compare(0D, component.localBuffer()) == 0, "Geolyzer scan did not consume energy");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void geolyzerOperationsReturnNoEnergyWhenUnderpowered(final GameTestHelper helper) {
        final BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.GEOLYZER.get());
        helper.setBlock(pos.relative(Direction.WEST), Blocks.STONE);
        final GeolyzerBlockEntity geolyzer = helper.getBlockEntity(pos);
        final ComponentConnector component = (ComponentConnector) geolyzer.node();

        final Object[] scan = invokeComponent(helper, component, "scan", -1, 0, 0, 1, 1, 1);
        final Object[] analyze = invokeComponent(helper, component, "analyze", Direction.WEST.get3DDataValue());
        final Object[] store = invokeComponent(helper, component, "store", Direction.WEST.get3DDataValue(), "missing", 1);

        assertNoEnergy(helper, scan, "Geolyzer scan");
        assertNoEnergy(helper, analyze, "Geolyzer analyze");
        assertNoEnergy(helper, store, "Geolyzer store");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void tankControllerInspectsAdjacentFluidTanks(final GameTestHelper helper) {
        final BlockPos pos = new BlockPos(1, 1, 1);
        final int side = Direction.WEST.get3DDataValue();
        helper.setBlock(pos, ModBlocks.ADAPTER.get());
        helper.setBlock(pos.relative(Direction.WEST), Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
        final DriverItem driver = Driver.driverFor(new ItemStack(ModItems.TANK_CONTROLLER_UPGRADE.get()), AdapterBlockEntity.class);
        final ManagedEnvironment environment = driver.createEnvironment(new ItemStack(ModItems.TANK_CONTROLLER_UPGRADE.get()), new StaticPositionEnvironmentHost(helper, pos));
        final li.cil.oc.api.network.Component component = (li.cil.oc.api.network.Component) environment.node();

        final Object[] count = invokeComponent(helper, component, "getTankCount", side);
        final Object[] level = invokeComponent(helper, component, "getTankLevel", side);
        final Object[] capacity = invokeComponent(helper, component, "getTankCapacity", side);
        final Object[] fluid = invokeComponent(helper, component, "getFluidInTank", side, 1);
        final Object[] allFluids = invokeComponent(helper, component, "getFluidInTank", side);

        helper.assertTrue(Integer.valueOf(1).equals(count[0]), "Tank controller did not see adjacent tank");
        helper.assertTrue(Integer.valueOf(1000).equals(level[0]), "Tank controller did not sum tank level");
        helper.assertTrue(Integer.valueOf(1000).equals(capacity[0]), "Tank controller did not report max tank capacity");
        helper.assertTrue("minecraft:water".equals(fluid[0]), "Tank controller did not report water fluid id");
        helper.assertTrue(Integer.valueOf(1000).equals(fluid[1]), "Tank controller did not report fluid amount");
        helper.assertTrue(Integer.valueOf(1000).equals(fluid[2]), "Tank controller did not report fluid capacity");
        assertSingleWaterTankDescription(helper, allFluids, "Tank controller");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void assemblerBlockAssemblesTabletFromInputs(final GameTestHelper helper) {
        final BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.ASSEMBLER.get());
        final AssemblerBlockEntity assembler = helper.getBlockEntity(pos);

        final ItemStack container = new ItemStack(ModItems.CARD_CONTAINER_TIER1.get());
        final ItemStack cpu = new ItemStack(ModItems.CPU_TIER1.get());
        final ItemStack memory = new ItemStack(ModItems.MEMORY_TIER1.get());

        assembler.setItem(AssemblerBlockEntity.SLOT_TEMPLATE, new ItemStack(ModItems.TABLET_CASE_TIER2.get()));
        assembler.setItem(AssemblerBlockEntity.SLOT_CONTAINER_START, container.copy());
        assembler.setItem(AssemblerBlockEntity.SLOT_COMPONENT_START, cpu.copy());
        assembler.setItem(AssemblerBlockEntity.SLOT_COMPONENT_START + 1, memory.copy());

        helper.assertTrue(assembler.canAssemble(), "Assembler did not accept tablet recipe inputs");
        helper.assertTrue(assembler.start(true), "Assembler did not start tablet assembly");
        final ItemStack output = assembler.getItem(AssemblerBlockEntity.SLOT_TEMPLATE);

        helper.assertTrue(output.is(ModItems.TABLET.get()), "Assembler did not output a tablet");
        final TabletItem tablet = (TabletItem) output.getItem();
        helper.assertTrue(tablet.tier(output) == 1, "Output tablet tier did not match tablet case");
        helper.assertTrue(ItemStack.isSameItemSameComponents(container, tablet.getContainer(output)), "Output tablet container missing");
        helper.assertTrue(ItemStack.isSameItemSameComponents(cpu, tablet.getComponent(output, 1)), "Output tablet CPU missing");
        helper.assertTrue(ItemStack.isSameItemSameComponents(memory, tablet.getComponent(output, 2)), "Output tablet memory missing");
        helper.assertTrue(assembler.getItem(AssemblerBlockEntity.SLOT_CONTAINER_START).isEmpty(), "Assembler did not consume container slot");
        helper.assertTrue(assembler.getItem(AssemblerBlockEntity.SLOT_COMPONENT_START).isEmpty(), "Assembler did not consume component slot");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void assemblerBlockUsesRegisteredTemplates(final GameTestHelper helper) {
        try (AssemblerTemplates.Registration ignored = AssemblerTemplates.register(new AssemblerTemplate() {
            @Override
            public String name() {
                return "test";
            }

            @Override
            public boolean matches(final ItemStack stack) {
                return stack.is(Items.DIAMOND);
            }

            @Override
            public boolean validate(final AssemblerBlockEntity assembler) {
                return true;
            }

            @Override
            public boolean canPlaceItem(final AssemblerBlockEntity assembler, final int slot, final ItemStack stack) {
                return slot == AssemblerBlockEntity.SLOT_TEMPLATE && matches(stack);
            }

            @Override
            public ItemStack assemble(final AssemblerBlockEntity assembler) {
                return new ItemStack(Items.EMERALD);
            }
        })) {
            final BlockPos pos = new BlockPos(1, 1, 1);
            helper.setBlock(pos, ModBlocks.ASSEMBLER.get());
            final AssemblerBlockEntity assembler = helper.getBlockEntity(pos);

            assembler.setItem(AssemblerBlockEntity.SLOT_TEMPLATE, new ItemStack(Items.DIAMOND));

            helper.assertTrue(assembler.canAssemble(), "Custom assembler template did not validate");
            helper.assertTrue(assembler.start(true), "Custom assembler template did not start");
            helper.assertTrue(assembler.getItem(AssemblerBlockEntity.SLOT_TEMPLATE).is(Items.EMERALD), "Custom assembler template did not produce output");
            helper.succeed();
        }
    }

    @GameTest(template = "empty")
    public static void assemblerComponentAssemblyProgressesOverTicks(final GameTestHelper helper) {
        try (AssemblerTemplates.Registration ignored = AssemblerTemplates.register(new AssemblerTemplate() {
            @Override
            public String name() {
                return "slow_test";
            }

            @Override
            public boolean matches(final ItemStack stack) {
                return stack.is(Items.DIAMOND);
            }

            @Override
            public boolean validate(final AssemblerBlockEntity assembler) {
                return true;
            }

            @Override
            public boolean canPlaceItem(final AssemblerBlockEntity assembler, final int slot, final ItemStack stack) {
                return slot == AssemblerBlockEntity.SLOT_TEMPLATE && matches(stack);
            }

            @Override
            public ItemStack assemble(final AssemblerBlockEntity assembler) {
                return new ItemStack(Items.EMERALD);
            }

            @Override
            public double energyRequired(final AssemblerBlockEntity assembler) {
                return 2D;
            }
        })) {
            final BlockPos pos = new BlockPos(1, 1, 1);
            helper.setBlock(pos, ModBlocks.ASSEMBLER.get());
            final AssemblerBlockEntity assembler = helper.getBlockEntity(pos);
            assembler.setItem(AssemblerBlockEntity.SLOT_TEMPLATE, new ItemStack(Items.DIAMOND));

            final Object[] startResult = assembler.start(null, null);
            helper.assertTrue(startResult.length == 1 && Boolean.TRUE.equals(startResult[0]), "Assembler component start returned false");
            helper.assertTrue(assembler.isAssembling(), "Assembler did not enter busy state");
            helper.assertTrue(assembler.getItem(AssemblerBlockEntity.SLOT_TEMPLATE).isEmpty(), "Assembler output appeared before work completed");
            helper.assertTrue(assembler.node() instanceof ComponentConnector, "Assembler node is not a component connector");

            final ComponentConnector connector = (ComponentConnector) assembler.node();
            connector.changeBuffer(2D);
            AssemblerBlockEntity.serverTick(helper.getLevel(), helper.absolutePos(pos), helper.getBlockState(pos), assembler);
            helper.assertTrue(assembler.isAssembling(), "Assembler finished after partial energy");
            AssemblerBlockEntity.serverTick(helper.getLevel(), helper.absolutePos(pos), helper.getBlockState(pos), assembler);

            helper.assertTrue(!assembler.isAssembling(), "Assembler did not finish after required energy");
            helper.assertTrue(assembler.getItem(AssemblerBlockEntity.SLOT_TEMPLATE).is(Items.EMERALD), "Assembler did not install pending output");
            helper.succeed();
        }
    }

    @GameTest(template = "empty")
    public static void assemblerProcessesImcTemplates(final GameTestHelper helper) {
        final CompoundTag payload = new CompoundTag();
        payload.putString("name", "imc_test");
        payload.putString("select", NeoOpenComputersGameTests.class.getName() + ".selectDiamondTemplate");
        payload.putString("validate", NeoOpenComputersGameTests.class.getName() + ".validateImcAssemblerTemplate");
        payload.putString("assemble", NeoOpenComputersGameTests.class.getName() + ".assembleEmeraldTemplate");
        final InterModComms.IMCMessage message = new InterModComms.IMCMessage(
            "addon",
            NeoOpenComputers.MODID,
            li.cil.oc.api.IMC.REGISTER_ASSEMBLER_TEMPLATE,
            () -> payload);

        try (AssemblerTemplates.Registration ignored = AssemblerTemplateImc.process(java.util.stream.Stream.of(message)).getFirst()) {
            final BlockPos pos = new BlockPos(1, 1, 1);
            helper.setBlock(pos, ModBlocks.ASSEMBLER.get());
            final AssemblerBlockEntity assembler = helper.getBlockEntity(pos);
            assembler.setItem(AssemblerBlockEntity.SLOT_TEMPLATE, new ItemStack(Items.DIAMOND));

            helper.assertTrue(assembler.canAssemble(), "IMC assembler template did not validate");
            helper.assertTrue(assembler.start(true), "IMC assembler template did not start");
            helper.assertTrue(assembler.getItem(AssemblerBlockEntity.SLOT_TEMPLATE).is(Items.EMERALD), "IMC assembler template did not produce output");
            helper.succeed();
        }
    }

    @GameTest(template = "empty")
    public static void registeredDisassemblerTemplatesDisassembleStacks(final GameTestHelper helper) {
        final DisassemblerTemplate template = new DisassemblerTemplate() {
            @Override
            public String name() {
                return "diamond";
            }

            @Override
            public boolean matches(final ItemStack stack) {
                return stack.is(Items.DIAMOND);
            }

            @Override
            public ItemStack[] disassemble(final ItemStack stack) {
                return new ItemStack[]{new ItemStack(Items.EMERALD)};
            }
        };

        try (DisassemblerTemplates.Registration ignored = DisassemblerTemplates.register(template)) {
            final ItemStack[] result = DisassemblerTemplates.disassemble(new ItemStack(Items.DIAMOND));
            helper.assertTrue(result.length == 1 && result[0].is(Items.EMERALD), "Custom disassembler template did not produce output");
            helper.succeed();
        }
    }

    @GameTest(template = "empty")
    public static void disassemblerProcessesImcTemplates(final GameTestHelper helper) {
        final CompoundTag payload = new CompoundTag();
        payload.putString("name", "imc_test");
        payload.putString("select", NeoOpenComputersGameTests.class.getName() + ".selectDiamondTemplate");
        payload.putString("disassemble", NeoOpenComputersGameTests.class.getName() + ".disassembleEmeraldTemplate");
        final InterModComms.IMCMessage message = new InterModComms.IMCMessage(
            "addon",
            NeoOpenComputers.MODID,
            li.cil.oc.api.IMC.REGISTER_DISASSEMBLER_TEMPLATE,
            () -> payload);

        try (DisassemblerTemplates.Registration ignored = DisassemblerTemplateImc.process(java.util.stream.Stream.of(message)).getFirst()) {
            final ItemStack[] result = DisassemblerTemplates.disassemble(new ItemStack(Items.DIAMOND));
            helper.assertTrue(result.length == 1 && result[0].is(Items.EMERALD), "IMC disassembler template did not produce output");
            helper.succeed();
        }
    }

    @GameTest(template = "empty")
    public static void disassemblerBlockDisassemblesTablet(final GameTestHelper helper) {
        final ItemStack cpu = new ItemStack(ModItems.CPU_TIER1.get());
        final ItemStack memory = new ItemStack(ModItems.MEMORY_TIER1.get());
        final ItemStack tablet = ModItems.TABLET.get().assembleFromCase(
            new ItemStack(ModItems.TABLET_CASE_TIER2.get()),
            ItemStack.EMPTY,
            cpu.copy(),
            memory.copy());
        final BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.DISASSEMBLER.get());
        final DisassemblerBlockEntity disassembler = helper.getBlockEntity(pos);
        disassembler.setItem(DisassemblerBlockEntity.SLOT_INPUT, tablet);

        helper.assertTrue(disassembler.canDisassemble(), "Disassembler did not accept tablet");
        helper.assertTrue(disassembler.disassemble(RandomSource.create(0L), 0D), "Disassembler did not start disassembly");
        helper.assertTrue(disassembler.getItem(DisassemblerBlockEntity.SLOT_INPUT).isEmpty(), "Disassembler did not clear input");
        helper.assertTrue(disassembler.containsOutput(ModItems.TABLET_CASE_TIER2.get()), "Disassembler did not output tablet case");
        helper.assertTrue(disassembler.containsOutput(ModItems.CPU_TIER1.get()), "Disassembler did not output CPU");
        helper.assertTrue(disassembler.containsOutput(ModItems.MEMORY_TIER1.get()), "Disassembler did not output memory");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void disassemblerOutputsToAdjacentInventory(final GameTestHelper helper) {
        final ItemStack cpu = new ItemStack(ModItems.CPU_TIER1.get());
        final ItemStack memory = new ItemStack(ModItems.MEMORY_TIER1.get());
        final ItemStack tablet = ModItems.TABLET.get().assembleFromCase(
            new ItemStack(ModItems.TABLET_CASE_TIER2.get()),
            ItemStack.EMPTY,
            cpu.copy(),
            memory.copy());
        final BlockPos disassemblerPos = new BlockPos(1, 1, 1);
        final BlockPos chestPos = new BlockPos(2, 1, 1);
        helper.setBlock(disassemblerPos, ModBlocks.DISASSEMBLER.get());
        helper.setBlock(chestPos, Blocks.CHEST);
        final DisassemblerBlockEntity disassembler = helper.getBlockEntity(disassemblerPos);
        final ChestBlockEntity chest = helper.getBlockEntity(chestPos);
        disassembler.setItem(DisassemblerBlockEntity.SLOT_INPUT, tablet);

        helper.assertTrue(disassembler.disassemble(RandomSource.create(0L), 0D), "Disassembler did not route output");
        helper.assertTrue(disassembler.getItem(DisassemblerBlockEntity.SLOT_INPUT).isEmpty(), "Disassembler did not clear input");
        helper.assertTrue(chestContains(chest, ModItems.TABLET_CASE_TIER2.get()), "Disassembler did not output tablet case to chest");
        helper.assertTrue(chestContains(chest, ModItems.CPU_TIER1.get()), "Disassembler did not output CPU to chest");
        helper.assertTrue(chestContains(chest, ModItems.MEMORY_TIER1.get()), "Disassembler did not output memory to chest");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void disassemblerBreakChanceCanDestroyOutputs(final GameTestHelper helper) {
        try (DisassemblerTemplates.Registration ignored = DisassemblerTemplates.register(new DisassemblerTemplate() {
            @Override
            public String name() {
                return "loss_test";
            }

            @Override
            public boolean matches(final ItemStack stack) {
                return stack.is(Items.DIAMOND);
            }

            @Override
            public ItemStack[] disassemble(final ItemStack stack) {
                return new ItemStack[]{new ItemStack(Items.EMERALD)};
            }
        })) {
            final BlockPos pos = new BlockPos(1, 1, 1);
            helper.setBlock(pos, ModBlocks.DISASSEMBLER.get());
            final DisassemblerBlockEntity disassembler = helper.getBlockEntity(pos);
            disassembler.setItem(DisassemblerBlockEntity.SLOT_INPUT, new ItemStack(Items.DIAMOND));

            helper.assertTrue(disassembler.disassemble(RandomSource.create(0L), 1D), "Disassembler did not process lossy output");
            helper.assertTrue(disassembler.getItem(DisassemblerBlockEntity.SLOT_INPUT).isEmpty(), "Disassembler did not clear input after lossy output");
            helper.assertTrue(!disassembler.containsOutput(Items.EMERALD), "Disassembler kept an output despite 100% break chance");
            helper.succeed();
        }
    }

    @GameTest(template = "empty")
    public static void disassemblerRoutesOverflowOutputsToAdjacentInventory(final GameTestHelper helper) {
        try (DisassemblerTemplates.Registration ignored = DisassemblerTemplates.register(new DisassemblerTemplate() {
            @Override
            public String name() {
                return "overflow_test";
            }

            @Override
            public boolean matches(final ItemStack stack) {
                return stack.is(Items.DIAMOND);
            }

            @Override
            public ItemStack[] disassemble(final ItemStack stack) {
                final ItemStack[] outputs = new ItemStack[10];
                for (int index = 0; index < outputs.length; index++) {
                    outputs[index] = new ItemStack(Items.EMERALD);
                }
                return outputs;
            }
        })) {
            final BlockPos disassemblerPos = new BlockPos(1, 1, 1);
            final BlockPos chestPos = new BlockPos(2, 1, 1);
            helper.setBlock(disassemblerPos, ModBlocks.DISASSEMBLER.get());
            helper.setBlock(chestPos, Blocks.CHEST);
            final DisassemblerBlockEntity disassembler = helper.getBlockEntity(disassemblerPos);
            final ChestBlockEntity chest = helper.getBlockEntity(chestPos);
            disassembler.setItem(DisassemblerBlockEntity.SLOT_INPUT, new ItemStack(Items.DIAMOND));

            helper.assertTrue(disassembler.disassemble(RandomSource.create(0L), 0D), "Disassembler rejected overflow output with adjacent chest");
            helper.assertTrue(chestItemCount(chest, Items.EMERALD) == 10, "Disassembler did not move all overflow outputs to chest");
            helper.succeed();
        }
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
        assertContainerCapability(helper, new ItemStack(ModItems.CARD_CONTAINER_TIER1.get()), li.cil.oc.api.driver.item.Slot.Card, 0);
        assertContainerCapability(helper, new ItemStack(ModItems.CARD_CONTAINER_TIER2.get()), li.cil.oc.api.driver.item.Slot.Card, 1);
        assertContainerCapability(helper, new ItemStack(ModItems.CARD_CONTAINER_TIER3.get()), li.cil.oc.api.driver.item.Slot.Card, 2);
        assertContainerCapability(helper, new ItemStack(ModItems.DISK_DRIVE.get()), li.cil.oc.api.driver.item.Slot.Floppy, Integer.MAX_VALUE);
        assertContainerCapability(helper, new ItemStack(ModItems.UPGRADE_CONTAINER_TIER1.get()), li.cil.oc.api.driver.item.Slot.Upgrade, 0);
        assertContainerCapability(helper, new ItemStack(ModItems.UPGRADE_CONTAINER_TIER2.get()), li.cil.oc.api.driver.item.Slot.Upgrade, 1);
        assertContainerCapability(helper, new ItemStack(ModItems.UPGRADE_CONTAINER_TIER3.get()), li.cil.oc.api.driver.item.Slot.Upgrade, 2);
        assertDatabaseCapacity(helper, new ItemStack(ModItems.DATABASE_UPGRADE_TIER1.get()), 9);
        assertDatabaseCapacity(helper, new ItemStack(ModItems.DATABASE_UPGRADE_TIER2.get()), 25);
        assertDatabaseCapacity(helper, new ItemStack(ModItems.DATABASE_UPGRADE_TIER3.get()), 81);
        assertBatteryCharge(helper, new ItemStack(ModItems.BATTERY_UPGRADE_TIER1.get()), 10000D);
        assertBatteryCharge(helper, new ItemStack(ModItems.BATTERY_UPGRADE_TIER2.get()), 15000D);
        assertBatteryCharge(helper, new ItemStack(ModItems.BATTERY_UPGRADE_TIER3.get()), 20000D);
        assertInventoryCapacity(helper, new ItemStack(ModItems.INVENTORY_UPGRADE.get()), 16);
        assertSolarGenerator(helper, new ItemStack(ModItems.SOLAR_GENERATOR_UPGRADE.get()));
        assertTankCapacity(helper, new ItemStack(ModItems.TANK_UPGRADE.get()), 16000);
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
    public static void generatorUpgradeBurnsQueuedFuel(final GameTestHelper helper) throws Exception {
        final DriverItem driver = Driver.driverFor(new ItemStack(ModItems.GENERATOR_UPGRADE.get()));
        helper.assertTrue(driver != null, "No driver for generator upgrade");

        final AgentTestHost host = new AgentTestHost(helper);
        host.mainInventory().setItem(0, new ItemStack(Items.COAL, 2));
        final ManagedEnvironment environment = driver.createEnvironment(new ItemStack(ModItems.GENERATOR_UPGRADE.get()), host);
        helper.assertTrue(environment != null, "Generator upgrade did not create generator environment");
        helper.assertTrue(environment.node() instanceof ComponentConnector, "Generator node is not a component connector");
        final ComponentConnector connector = (ComponentConnector) environment.node();
        helper.assertTrue("generator".equals(connector.name()), "Generator component name mismatch");

        final Object[] insert = connector.invoke("insert", null, 1);
        helper.assertTrue(Boolean.TRUE.equals(insert[0]) && Integer.valueOf(1).equals(insert[1]), "Generator did not queue one fuel item");
        helper.assertTrue(host.mainInventory().getItem(0).getCount() == 1, "Generator did not consume selected fuel");
        final Object[] count = connector.invoke("count", null);
        helper.assertTrue(Integer.valueOf(1).equals(count[0]), "Generator queue count mismatch");

        environment.update();
        helper.assertTrue(connector.localBuffer() > 0D, "Generator did not produce energy while burning");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void craftingUpgradeCraftsTopLeftInventoryGrid(final GameTestHelper helper) throws Exception {
        final DriverItem driver = Driver.driverFor(new ItemStack(ModItems.CRAFTING_UPGRADE.get()));
        helper.assertTrue(driver != null, "No driver for crafting upgrade");

        final AgentTestHost host = new AgentTestHost(helper);
        host.mainInventory().setItem(0, new ItemStack(Items.OAK_LOG));
        final ManagedEnvironment environment = driver.createEnvironment(new ItemStack(ModItems.CRAFTING_UPGRADE.get()), host);
        helper.assertTrue(environment != null, "Crafting upgrade did not create crafting environment");
        helper.assertTrue(environment.node() instanceof li.cil.oc.api.network.Component, "Crafting node is not a component");
        final li.cil.oc.api.network.Component component = (li.cil.oc.api.network.Component) environment.node();
        helper.assertTrue("crafting".equals(component.name()), "Crafting component name mismatch");

        final Object[] craft = component.invoke("craft", null, 4);
        helper.assertTrue(Boolean.TRUE.equals(craft[0]) && Integer.valueOf(4).equals(craft[1]), "Crafting upgrade did not craft four planks");
        helper.assertTrue(!host.mainInventory().getItem(0).is(Items.OAK_LOG), "Crafting upgrade did not consume input log");
        helper.assertTrue(containsStack(host.mainInventory(), Items.OAK_PLANKS, 4), "Crafting upgrade did not store crafted planks");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void experienceUpgradeConsumesExperienceBottle(final GameTestHelper helper) throws Exception {
        final DriverItem driver = Driver.driverFor(new ItemStack(ModItems.EXPERIENCE_UPGRADE.get()));
        helper.assertTrue(driver != null, "No driver for experience upgrade");

        final AgentTestHost host = new AgentTestHost(helper);
        host.mainInventory().setItem(0, new ItemStack(Items.EXPERIENCE_BOTTLE));
        final ManagedEnvironment environment = driver.createEnvironment(new ItemStack(ModItems.EXPERIENCE_UPGRADE.get()), host);
        helper.assertTrue(environment != null, "Experience upgrade did not create experience environment");
        helper.assertTrue(environment.node() instanceof li.cil.oc.api.network.Component, "Experience node is not a component");
        final li.cil.oc.api.network.Component component = (li.cil.oc.api.network.Component) environment.node();

        final Object[] consume = component.invoke("consume", null);
        helper.assertTrue(Boolean.TRUE.equals(consume[0]), "Experience upgrade did not consume bottle");
        helper.assertTrue(host.mainInventory().getItem(0).isEmpty(), "Experience upgrade did not remove consumed bottle");
        final Object[] level = component.invoke("level", null);
        helper.assertTrue(level.length == 1 && level[0] instanceof Number value && value.doubleValue() > 0D, "Experience upgrade did not gain experience");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void experienceUpgradeConsumesEnchantedItem(final GameTestHelper helper) throws Exception {
        final DriverItem driver = Driver.driverFor(new ItemStack(ModItems.EXPERIENCE_UPGRADE.get()));
        helper.assertTrue(driver != null, "No driver for experience upgrade");

        final AgentTestHost host = new AgentTestHost(helper);
        final ItemStack sword = new ItemStack(Items.IRON_SWORD);
        sword.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SHARPNESS), 1);
        host.mainInventory().setItem(0, sword);
        final ManagedEnvironment environment = driver.createEnvironment(new ItemStack(ModItems.EXPERIENCE_UPGRADE.get()), host);
        helper.assertTrue(environment != null, "Experience upgrade did not create experience environment");
        helper.assertTrue(environment.node() instanceof li.cil.oc.api.network.Component, "Experience node is not a component");
        final li.cil.oc.api.network.Component component = (li.cil.oc.api.network.Component) environment.node();

        final Object[] consume = component.invoke("consume", null);
        helper.assertTrue(Boolean.TRUE.equals(consume[0]), "Experience upgrade did not consume enchanted item");
        helper.assertTrue(host.mainInventory().getItem(0).isEmpty(), "Experience upgrade did not remove enchanted item");
        final Object[] level = component.invoke("level", null);
        helper.assertTrue(level.length == 1 && level[0] instanceof Number value && value.doubleValue() > 0D, "Experience upgrade did not gain enchantment experience");
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
    public static void inventoryControllerStoresStacksInDatabase(final GameTestHelper helper) {
        final BlockPos computerPos = new BlockPos(0, 1, 1);
        final BlockPos adapterPos = new BlockPos(1, 1, 1);
        final BlockPos chestPos = new BlockPos(2, 1, 1);

        helper.setBlock(computerPos, ModBlocks.COMPUTER_CASE_TIER1.get());
        helper.setBlock(adapterPos, ModBlocks.ADAPTER.get());
        helper.setBlock(chestPos, Blocks.CHEST);

        final li.cil.oc.common.blockentity.AdapterBlockEntity adapter = helper.getBlockEntity(adapterPos);
        adapter.setItem(0, new ItemStack(ModItems.INVENTORY_CONTROLLER_UPGRADE.get()));

        final net.minecraft.world.Container chest = helper.getBlockEntity(chestPos);
        chest.setItem(0, new ItemStack(Items.DIAMOND, 4));

        final DriverItem databaseDriver = Driver.driverFor(new ItemStack(ModItems.DATABASE_UPGRADE_TIER1.get()));
        helper.assertTrue(databaseDriver != null, "No driver for database upgrade");
        final ManagedEnvironment databaseEnvironment = databaseDriver.createEnvironment(new ItemStack(ModItems.DATABASE_UPGRADE_TIER1.get()), null);
        helper.assertTrue(databaseEnvironment instanceof li.cil.oc.api.internal.Database, "Database upgrade did not create database environment");
        final li.cil.oc.api.internal.Database database = (li.cil.oc.api.internal.Database) databaseEnvironment;
        database.setStackInSlot(0, new ItemStack(Items.GOLD_INGOT));
        Network.joinNewNetwork(databaseEnvironment.node());

        helper.succeedWhen(() -> {
            final ComputerCaseBlockEntity computer = helper.getBlockEntity(computerPos);
            databaseEnvironment.node().connect(computer.node());
            final String address = componentAddress(computer, "inventory_controller");
            helper.assertTrue(address != null, "Adapter did not expose inventory controller upgrade: " + computer.machine().components());
            try {
                final int east = Direction.EAST.get3DDataValue();
                final String databaseAddress = databaseEnvironment.node().address();
                assertInvokeResult(helper, computer, address, "store", new Object[]{east, 1, databaseAddress, 1}, true);
                final ItemStack stored = database.getStackInSlot(0);
                helper.assertTrue(stored.is(Items.DIAMOND) && stored.getCount() == 4, "Inventory controller did not store stack in database");
                assertInvokeResult(helper, computer, address, "compareStackToDatabase", new Object[]{east, 1, databaseAddress, 1}, true);
                database.setStackInSlot(0, new ItemStack(Items.DIRT));
                assertInvokeResult(helper, computer, address, "compareStackToDatabase", new Object[]{east, 1, databaseAddress, 1}, false);
            } catch (Exception e) {
                helper.fail("Inventory controller database invocation failed: " + e.getMessage());
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

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void adapterInventoryComponentStoresStacksInDatabase(final GameTestHelper helper) {
        final BlockPos computerPos = new BlockPos(0, 1, 1);
        final BlockPos adapterPos = new BlockPos(1, 1, 1);
        final BlockPos chestPos = new BlockPos(2, 1, 1);

        helper.setBlock(computerPos, ModBlocks.COMPUTER_CASE_TIER1.get());
        helper.setBlock(adapterPos, ModBlocks.ADAPTER.get());
        helper.setBlock(chestPos, Blocks.CHEST);

        final net.minecraft.world.Container chest = helper.getBlockEntity(chestPos);
        chest.setItem(0, new ItemStack(Items.DIAMOND, 6));

        final DriverItem databaseDriver = Driver.driverFor(new ItemStack(ModItems.DATABASE_UPGRADE_TIER1.get()));
        helper.assertTrue(databaseDriver != null, "No driver for database upgrade");
        final ManagedEnvironment databaseEnvironment = databaseDriver.createEnvironment(new ItemStack(ModItems.DATABASE_UPGRADE_TIER1.get()), null);
        helper.assertTrue(databaseEnvironment instanceof li.cil.oc.api.internal.Database, "Database upgrade did not create database environment");
        final li.cil.oc.api.internal.Database database = (li.cil.oc.api.internal.Database) databaseEnvironment;
        database.setStackInSlot(0, new ItemStack(Items.GOLD_INGOT));
        Network.joinNewNetwork(databaseEnvironment.node());

        helper.succeedWhen(() -> {
            final ComputerCaseBlockEntity computer = helper.getBlockEntity(computerPos);
            databaseEnvironment.node().connect(computer.node());
            final String address = componentAddress(computer, "inventory");
            helper.assertTrue(address != null, "Adapter did not expose chest inventory: " + computer.machine().components());
            try {
                final String databaseAddress = databaseEnvironment.node().address();
                assertInvokeResult(helper, computer, address, "store", new Object[]{1, databaseAddress, 1}, true);
                final ItemStack stored = database.getStackInSlot(0);
                helper.assertTrue(stored.is(Items.DIAMOND) && stored.getCount() == 6, "Inventory component did not store stack in database");
                assertInvokeResult(helper, computer, address, "compareStackToDatabase", new Object[]{1, databaseAddress, 1}, true);
                database.setStackInSlot(0, new ItemStack(Items.DIRT));
                assertInvokeResult(helper, computer, address, "compareStackToDatabase", new Object[]{1, databaseAddress, 1}, false);
            } catch (Exception e) {
                helper.fail("Inventory component database invocation failed: " + e.getMessage());
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

    private static boolean containsStack(final net.minecraft.world.Container inventory, final Item item, final int count) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            final ItemStack stack = inventory.getItem(slot);
            if (stack.is(item) && stack.getCount() >= count) {
                return true;
            }
        }
        return false;
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

    private static void assertContainerCapability(final GameTestHelper helper, final ItemStack stack, final String providedSlot, final int tier) {
        final DriverItem driver = Driver.driverFor(stack);
        helper.assertTrue(driver instanceof Container, "Expected container driver for " + stack);
        final Container container = (Container) driver;
        helper.assertTrue(li.cil.oc.api.driver.item.Slot.Container.equals(container.slot(stack)), "Expected container slot for " + stack);
        helper.assertTrue(providedSlot.equals(container.providedSlot(stack)), "Expected " + stack + " to provide " + providedSlot);
        helper.assertTrue(container.providedTier(stack) == tier, "Expected " + stack + " to provide tier " + tier + " but got " + container.providedTier(stack));
    }

    private static void assertDatabaseCapacity(final GameTestHelper helper, final ItemStack stack, final int capacity) {
        final DriverItem driver = Driver.driverFor(stack);
        helper.assertTrue(driver != null, "No driver for " + stack);
        final ManagedEnvironment environment = driver.createEnvironment(stack, null);
        helper.assertTrue(environment instanceof li.cil.oc.api.internal.Database, "No database environment for " + stack);
        final li.cil.oc.api.internal.Database database = (li.cil.oc.api.internal.Database) environment;
        helper.assertTrue(database.size() == capacity, "Expected " + stack + " to have " + capacity + " database slots but got " + database.size());
    }

    private static void assertBatteryCharge(final GameTestHelper helper, final ItemStack stack, final double capacity) {
        final DriverItem driver = Driver.driverFor(stack);
        helper.assertTrue(driver instanceof Chargeable, "Expected chargeable driver for " + stack);
        final Chargeable chargeable = (Chargeable) driver;
        helper.assertTrue(chargeable.charge(stack, capacity * 0.75D, false) == capacity * 0.75D, "Battery did not accept initial charge");
        helper.assertTrue(chargeable.charge(stack, capacity, true) == capacity * 0.25D, "Battery simulation did not report remaining capacity");
        helper.assertTrue(chargeable.charge(stack, capacity, false) == capacity * 0.25D, "Battery did not cap at max charge");
        helper.assertTrue(chargeable.charge(stack, 1D, false) == 0D, "Full battery accepted extra charge");
    }

    private static void assertInventoryCapacity(final GameTestHelper helper, final ItemStack stack, final int capacity) {
        final DriverItem driver = Driver.driverFor(stack);
        helper.assertTrue(driver instanceof li.cil.oc.api.driver.item.Inventory, "Expected inventory driver for " + stack);
        final li.cil.oc.api.driver.item.Inventory inventory = (li.cil.oc.api.driver.item.Inventory) driver;
        helper.assertTrue(inventory.inventoryCapacity(stack) == capacity, "Expected " + stack + " inventory capacity " + capacity + " but got " + inventory.inventoryCapacity(stack));
    }

    private static void assertSolarGenerator(final GameTestHelper helper, final ItemStack stack) {
        final DriverItem driver = Driver.driverFor(stack);
        helper.assertTrue(driver != null, "No driver for " + stack);
        final ManagedEnvironment environment = driver.createEnvironment(stack, new StaticEnvironmentHost(helper));
        helper.assertTrue(environment != null, "No solar generator environment for " + stack);
        helper.assertTrue(environment.canUpdate(), "Solar generator cannot update");
        helper.assertTrue(environment.node() instanceof Connector, "Solar generator has no connector");
        final Connector connector = (Connector) environment.node();
        helper.assertTrue(connector.localBufferSize() == 1D, "Solar generator connector capacity mismatch");
        helper.getLevel().setDayTime(6000);
        environment.update();
        helper.assertTrue(connector.localBuffer() > 0D, "Solar generator did not produce energy in daylight");
    }

    private static void assertTankCapacity(final GameTestHelper helper, final ItemStack stack, final int capacity) {
        final DriverItem driver = Driver.driverFor(stack);
        helper.assertTrue(driver != null, "No driver for " + stack);
        final ManagedEnvironment environment = driver.createEnvironment(stack, null);
        helper.assertTrue(environment instanceof IFluidTank, "Expected tank environment for " + stack);
        final IFluidTank tank = (IFluidTank) environment;
        helper.assertTrue(tank.getCapacity() == capacity, "Expected " + stack + " tank capacity " + capacity + " but got " + tank.getCapacity());
        helper.assertTrue(tank.fill(new FluidStack(Fluids.WATER, 1000), FluidAction.EXECUTE) == 1000, "Tank did not accept water");
        helper.assertTrue(tank.getFluidAmount() == 1000, "Tank did not store water");
        helper.assertTrue(tank.drain(250, FluidAction.EXECUTE).getAmount() == 250, "Tank did not drain water");
        helper.assertTrue(tank.getFluidAmount() == 750, "Tank did not retain remaining water");
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

    private static Object[] invokeComponent(final GameTestHelper helper, final li.cil.oc.api.network.Component component, final String method, final Object... args) {
        try {
            return component.invoke(method, null, args);
        } catch (Exception e) {
            helper.fail("Component invocation failed: " + method + " " + e.getMessage());
            return new Object[0];
        }
    }

    private static void assertSingleWaterTankDescription(final GameTestHelper helper, final Object[] result, final String name) {
        helper.assertTrue(result.length == 1 && result[0] instanceof Object[], name + " did not return a tank description list");
        final Object[] tanks = (Object[]) result[0];
        helper.assertTrue(tanks.length == 1 && tanks[0] instanceof Object[], name + " did not return one tank description");
        final Object[] tank = (Object[]) tanks[0];
        helper.assertTrue("minecraft:water".equals(tank[0]), name + " did not report water in all tank descriptions");
        helper.assertTrue(Integer.valueOf(1000).equals(tank[1]), name + " did not report water amount in all tank descriptions");
        helper.assertTrue(Integer.valueOf(1000).equals(tank[2]), name + " did not report water capacity in all tank descriptions");
    }

    private static void assertNoEnergy(final GameTestHelper helper, final Object[] result, final String name) {
        helper.assertTrue(result.length == 2 && result[0] == null && "not enough energy".equals(result[1]), name + " did not report missing energy");
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

    private static boolean chestContains(final ChestBlockEntity chest, final Item item) {
        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            if (chest.getItem(slot).is(item)) {
                return true;
            }
        }
        return false;
    }

    private static int chestItemCount(final ChestBlockEntity chest, final Item item) {
        int count = 0;
        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            final ItemStack stack = chest.getItem(slot);
            if (stack.is(item)) {
                count += stack.getCount();
            }
        }
        return count;
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

    public static boolean selectDiamondTemplate(final ItemStack stack) {
        return stack.is(Items.DIAMOND);
    }

    public static Object[] validateImcAssemblerTemplate(final net.minecraft.world.Container inventory) {
        return new Object[]{true};
    }

    public static Object[] assembleEmeraldTemplate(final net.minecraft.world.Container inventory) {
        return new Object[]{new ItemStack(Items.EMERALD), 1D};
    }

    public static ItemStack[] disassembleEmeraldTemplate(final ItemStack stack) {
        return new ItemStack[]{new ItemStack(Items.EMERALD)};
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

    private record RecordingContext(Node node, double[] pauseSeconds) implements Context {
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

    private record StaticEnvironmentHost(GameTestHelper helper) implements li.cil.oc.api.network.EnvironmentHost {
        @Override
        public net.minecraft.world.level.Level world() {
            return helper.getLevel();
        }

        @Override
        public double xPosition() {
            return helper.absolutePos(BlockPos.ZERO).getX();
        }

        @Override
        public double yPosition() {
            return helper.absolutePos(BlockPos.ZERO).getY();
        }

        @Override
        public double zPosition() {
            return helper.absolutePos(BlockPos.ZERO).getZ();
        }

        @Override
        public void markChanged() {
        }
    }

    private record StaticPositionEnvironmentHost(GameTestHelper helper, BlockPos pos) implements li.cil.oc.api.network.EnvironmentHost {
        @Override
        public net.minecraft.world.level.Level world() {
            return helper.getLevel();
        }

        @Override
        public double xPosition() {
            return helper.absolutePos(pos).getX() + 0.5D;
        }

        @Override
        public double yPosition() {
            return helper.absolutePos(pos).getY() + 0.5D;
        }

        @Override
        public double zPosition() {
            return helper.absolutePos(pos).getZ() + 0.5D;
        }

        @Override
        public void markChanged() {
        }
    }

    private static final class AgentTestHost implements li.cil.oc.api.internal.Agent {
        private final GameTestHelper helper;
        private final SimpleContainer mainInventory = new SimpleContainer(9);
        private final SimpleContainer equipmentInventory = new SimpleContainer(4);
        private int selectedSlot;

        private AgentTestHost(final GameTestHelper helper) {
            this.helper = helper;
        }

        @Override
        public SimpleContainer equipmentInventory() {
            return equipmentInventory;
        }

        @Override
        public SimpleContainer mainInventory() {
            return mainInventory;
        }

        @Override
        public li.cil.oc.api.internal.MultiTank tank() {
            return null;
        }

        @Override
        public int selectedSlot() {
            return selectedSlot;
        }

        @Override
        public void setSelectedSlot(final int index) {
            selectedSlot = index;
        }

        @Override
        public int selectedTank() {
            return 0;
        }

        @Override
        public void setSelectedTank(final int index) {
        }

        @Override
        public Player player() {
            return null;
        }

        @Override
        public String name() {
            return "test";
        }

        @Override
        public void setName(final String name) {
        }

        @Override
        public String ownerName() {
            return "test";
        }

        @Override
        public UUID ownerUUID() {
            return new UUID(0L, 0L);
        }

        @Override
        public li.cil.oc.api.machine.Machine machine() {
            return null;
        }

        @Override
        public Iterable<ItemStack> internalComponents() {
            return java.util.List.of();
        }

        @Override
        public int componentSlot(final String address) {
            return -1;
        }

        @Override
        public void onMachineConnect(final Node node) {
        }

        @Override
        public void onMachineDisconnect(final Node node) {
        }

        @Override
        public net.minecraft.world.level.Level world() {
            return helper.getLevel();
        }

        @Override
        public double xPosition() {
            return helper.absolutePos(BlockPos.ZERO).getX();
        }

        @Override
        public double yPosition() {
            return helper.absolutePos(BlockPos.ZERO).getY();
        }

        @Override
        public double zPosition() {
            return helper.absolutePos(BlockPos.ZERO).getZ();
        }

        @Override
        public void markChanged() {
        }

        @Override
        public Direction facing() {
            return Direction.NORTH;
        }

        @Override
        public Direction toGlobal(final Direction value) {
            return value;
        }

        @Override
        public Direction toLocal(final Direction value) {
            return value;
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
