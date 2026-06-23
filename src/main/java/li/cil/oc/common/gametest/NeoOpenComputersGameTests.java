package li.cil.oc.common.gametest;

import li.cil.oc.NeoOpenComputers;
import it.unimi.dsi.fastutil.longs.LongSet;
import li.cil.oc.api.API;
import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.driver.DriverBlock;
import li.cil.oc.api.driver.DriverItem;
import li.cil.oc.api.driver.item.Chargeable;
import li.cil.oc.api.driver.item.Container;
import li.cil.oc.api.driver.item.Memory;
import li.cil.oc.api.driver.item.Processor;
import li.cil.oc.api.driver.item.Slot;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.internal.TextBuffer;
import li.cil.oc.api.machine.Signal;
import li.cil.oc.api.machine.Value;
import li.cil.oc.api.network.ManagedEnvironment;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.ComponentConnector;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.api.prefab.ItemStackArrayValue;
import li.cil.oc.common.ItemRegistry;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModEeproms;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.ModSettings;
import li.cil.oc.api.Network;
import li.cil.oc.common.blockentity.CableBlockEntity;
import li.cil.oc.common.blockentity.AdapterBlockEntity;
import li.cil.oc.common.blockentity.ComputerCaseBlockEntity;
import li.cil.oc.common.blockentity.DisassemblerBlockEntity;
import li.cil.oc.common.blockentity.DiskDriveBlockEntity;
import li.cil.oc.common.blockentity.GeolyzerBlockEntity;
import li.cil.oc.common.blockentity.HologramBlockEntity;
import li.cil.oc.common.blockentity.KeyboardBlockEntity;
import li.cil.oc.common.blockentity.MotionSensorBlockEntity;
import li.cil.oc.common.blockentity.RackBlockEntity;
import li.cil.oc.common.blockentity.RaidBlockEntity;
import li.cil.oc.common.blockentity.PowerDistributorBlockEntity;
import li.cil.oc.common.blockentity.RelayBlockEntity;
import li.cil.oc.common.blockentity.ScreenBlockEntity;
import li.cil.oc.common.blockentity.AssemblerBlockEntity;
import li.cil.oc.common.blockentity.TransposerBlockEntity;
import li.cil.oc.common.block.ComputerCaseBlock;
import li.cil.oc.common.block.DiskDriveBlock;
import li.cil.oc.common.item.AnalyzerItem;
import li.cil.oc.common.item.LinkedCardItem;
import li.cil.oc.common.item.TabletItem;
import li.cil.oc.common.item.TerminalItem;
import li.cil.oc.common.item.TexturePickerItem;
import li.cil.oc.common.item.WrenchItem;
import li.cil.oc.common.menu.AssemblerMenu;
import li.cil.oc.common.menu.ComputerCaseMenu;
import li.cil.oc.common.menu.DisassemblerMenu;
import li.cil.oc.common.menu.DiskDriveMenu;
import li.cil.oc.common.menu.RackMenu;
import li.cil.oc.common.component.LinkedCardEnvironment;
import li.cil.oc.common.component.DebugCardEnvironment;
import li.cil.oc.common.component.MfuEnvironment;
import li.cil.oc.common.component.TerminalServerRackMountableEnvironment;
import li.cil.oc.common.component.TerminalServerRegistry;
import li.cil.oc.common.nanomachines.provider.NanomachineDisintegrationProvider;
import li.cil.oc.common.nanomachines.provider.NanomachineHungryProvider;
import li.cil.oc.common.nanomachines.provider.NanomachinePotionProvider;
import li.cil.oc.common.template.AssemblerTemplate;
import li.cil.oc.common.template.AssemblerTemplateImc;
import li.cil.oc.common.template.AssemblerTemplates;
import li.cil.oc.common.template.DisassemblerTemplate;
import li.cil.oc.common.template.DisassemblerTemplateImc;
import li.cil.oc.common.template.DisassemblerTemplates;
import net.neoforged.fml.InterModComms;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ForcedChunksSavedData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.IFluidTank;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

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
        ModBlocks.POWER_DISTRIBUTOR.get();
        ModBlocks.RACK.get();
        ModBlocks.RAID.get();
        ModBlocks.REDSTONE_IO.get();
        ModBlocks.RELAY.get();
        ModBlocks.TRANSPOSER.get();
        ModBlocks.WAYPOINT.get();
        ModItems.ADAPTER.get();
        ModItems.ANALYZER.get();
        ModItems.WRENCH.get();
        ModItems.TEXTURE_PICKER.get();
        ModItems.TERMINAL.get();
        ModItems.TERMINAL_SERVER.get();
        ModItems.NANOMACHINES.get();
        ModItems.SERVER_TIER1.get();
        ModItems.SERVER_TIER2.get();
        ModItems.SERVER_TIER3.get();
        ModItems.APU_TIER1.get();
        ModItems.APU_TIER2.get();
        ModItems.ASSEMBLER.get();
        ModItems.BATTERY_UPGRADE_TIER1.get();
        ModItems.BATTERY_UPGRADE_TIER2.get();
        ModItems.BATTERY_UPGRADE_TIER3.get();
        ModItems.BARCODE_READER_UPGRADE.get();
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
        ModItems.INK_CARTRIDGE_EMPTY.get();
        ModItems.INK_CARTRIDGE.get();
        ModItems.BUTTON_GROUP.get();
        ModItems.ARROW_KEYS.get();
        ModItems.NUM_PAD.get();
        ModItems.POWER_DISTRIBUTOR.get();
        ModItems.RACK.get();
        ModItems.RAID.get();
        ModItems.CABLE.get();
        ModItems.RELAY.get();
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
        ModItems.DEBUG_CARD.get();
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
        ModItems.TRACTOR_BEAM_UPGRADE.get();
        ModItems.LEASH_UPGRADE.get();
        ModItems.ANGEL_UPGRADE.get();
        ModItems.CHUNKLOADER_UPGRADE.get();
        ModItems.MFU.get();
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
    public static void hologramExposesBottomSidedNodeAndAnalyzerNode(final GameTestHelper helper) {
        final BlockPos pos = new BlockPos(0, 1, 0);
        helper.setBlock(pos, ModBlocks.HOLOGRAM_TIER1.get());
        final HologramBlockEntity hologram = helper.getBlockEntity(pos);

        helper.assertTrue(hologram instanceof li.cil.oc.api.network.SidedEnvironment, "Hologram must expose sided networking");
        final li.cil.oc.api.network.SidedEnvironment sided = (li.cil.oc.api.network.SidedEnvironment) hologram;
        helper.assertTrue(sided.canConnect(Direction.DOWN), "Hologram should connect from bottom");
        helper.assertTrue(!sided.canConnect(Direction.NORTH), "Hologram should not connect from sides");
        helper.assertTrue(sided.sidedNode(Direction.DOWN) == hologram.node(), "Hologram bottom side did not expose component node");
        helper.assertTrue(sided.sidedNode(Direction.NORTH) == null, "Hologram side exposed component node");
        helper.assertTrue(hologram instanceof li.cil.oc.api.network.Analyzable, "Hologram must be analyzable");
        final Node[] nodes = ((li.cil.oc.api.network.Analyzable) hologram).onAnalyze(null, Direction.NORTH, 0, 0, 0);
        helper.assertTrue(nodes.length == 1 && nodes[0] == hologram.node(), "Analyzer did not report hologram node");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void wrenchRotatesComputerCase(final GameTestHelper helper) {
        final BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.COMPUTER_CASE_TIER1.get().defaultBlockState().setValue(ComputerCaseBlock.FACING, Direction.NORTH));

        final boolean simulated = WrenchItem.rotateBlock(helper.getLevel(), helper.absolutePos(pos), true);
        helper.assertTrue(simulated, "Wrench did not accept computer case in simulation");
        helper.assertTrue(helper.getBlockState(pos).getValue(ComputerCaseBlock.FACING) == Direction.NORTH, "Simulated wrench changed computer case");

        final boolean rotated = WrenchItem.rotateBlock(helper.getLevel(), helper.absolutePos(pos), false);
        helper.assertTrue(rotated, "Wrench did not rotate computer case");
        helper.assertTrue(helper.getBlockState(pos).getValue(ComputerCaseBlock.FACING) == Direction.EAST, "Wrench did not rotate computer case clockwise");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void texturePickerDescribesTargetBlock(final GameTestHelper helper) {
        final BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, Blocks.STONE.defaultBlockState());

        helper.assertTrue("minecraft:stone".equals(TexturePickerItem.describeBlockTexture(helper.getLevel(), helper.absolutePos(pos))), "Texture Picker did not describe target block");
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
        assertItemTier(helper, new ItemStack(ModItems.CRAFTING_UPGRADE.get()), 1);
        assertItemTier(helper, new ItemStack(ModItems.EXPERIENCE_UPGRADE.get()), 2);
        assertItemTier(helper, new ItemStack(ModItems.INTERNET_CARD.get()), 1);
        assertItemTier(helper, new ItemStack(ModItems.MOTION_SENSOR.get()), 1);
        assertItemTier(helper, new ItemStack(ModItems.PISTON_UPGRADE.get()), 0);
        assertItemTier(helper, new ItemStack(ModItems.STICKY_PISTON_UPGRADE.get()), 1);
        assertItemTier(helper, new ItemStack(ModItems.SIGN_UPGRADE.get()), 0);
        assertItemTier(helper, new ItemStack(ModItems.TRADING_UPGRADE.get()), 1);
        assertItemTier(helper, new ItemStack(ModItems.TRACTOR_BEAM_UPGRADE.get()), 2);
        assertItemTier(helper, new ItemStack(ModItems.LEASH_UPGRADE.get()), 0);
        assertItemTier(helper, new ItemStack(ModItems.ANGEL_UPGRADE.get()), 1);
        assertItemTier(helper, new ItemStack(ModItems.CHUNKLOADER_UPGRADE.get()), 2);
        assertItemTier(helper, new ItemStack(ModItems.MFU.get()), 2);
        assertItemTier(helper, new ItemStack(ModItems.SOLAR_GENERATOR_UPGRADE.get()), 1);
        assertItemTier(helper, new ItemStack(ModItems.TANK_UPGRADE.get()), 0);
        assertItemTier(helper, new ItemStack(ModItems.TANK_CONTROLLER_UPGRADE.get()), 1);
        assertItemTier(helper, new ItemStack(ModItems.UPGRADE_CONTAINER_TIER1.get()), 0);
        assertItemTier(helper, new ItemStack(ModItems.UPGRADE_CONTAINER_TIER2.get()), 1);
        assertItemTier(helper, new ItemStack(ModItems.UPGRADE_CONTAINER_TIER3.get()), 2);
        assertItemTier(helper, new ItemStack(ModItems.WIRELESS_NETWORK_CARD_TIER1.get()), 0);
        assertItemTier(helper, new ItemStack(ModItems.WIRELESS_NETWORK_CARD_TIER2.get()), 1);
        assertItemTier(helper, new ItemStack(ModItems.LINKED_CARD.get()), 2);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void linkedCardDriverDataTagPersistsTunnel(final GameTestHelper helper) {
        final ItemStack stack = new ItemStack(ModItems.LINKED_CARD.get());
        final DriverItem driver = Driver.driverFor(stack);

        helper.assertTrue(driver != null, "No driver for linked card");
        driver.dataTag(stack).putString(LinkedCardItem.TUNNEL_TAG, "pair");

        helper.assertTrue("pair".equals(driver.dataTag(stack).getString(LinkedCardItem.TUNNEL_TAG)), "Linked card driver data tag did not persist tunnel");
        final ManagedEnvironment environment = driver.createEnvironment(stack, null);
        helper.assertTrue(environment instanceof LinkedCardEnvironment, "Linked card did not create linked environment");
        final LinkedCardEnvironment linked = (LinkedCardEnvironment) environment;
        helper.assertTrue("pair".equals(linked.linkedChannel()), "Linked card environment did not use persisted tunnel");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void debugCardDriverDataTagCarriesAccessContext(final GameTestHelper helper) throws Exception {
        final ItemStack stack = new ItemStack(ModItems.DEBUG_CARD.get());
        final DriverItem driver = Driver.driverFor(stack);

        helper.assertTrue(driver != null, "No driver for debug card");
        DebugCardEnvironment.saveAccess(driver.dataTag(stack), new DebugCardEnvironment.AccessContext("Alice", "nonce-1"));

        withCachedConfig(ModSettings.DEBUG_CARD_ACCESS, "whitelist", () ->
            withCachedConfig(ModSettings.DEBUG_CARD_WHITELIST, List.of("alice nonce-1"), () -> {
                final ManagedEnvironment environment = driver.createEnvironment(stack, null);
                helper.assertTrue(environment instanceof DebugCardEnvironment, "Debug card did not create debug environment");
                helper.assertTrue(environment.node() instanceof li.cil.oc.api.network.Component, "Debug card did not create component node");
                final Object[] result = ((li.cil.oc.api.network.Component) environment.node()).invoke("getX", null);
                helper.assertTrue(result.length == 1 && Double.valueOf(0D).equals(result[0]), "Debug card did not use persisted access context");
            }));
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void debugCardShiftUseBindsAndUnbindsAccessContext(final GameTestHelper helper) throws Exception {
        final ItemStack stack = new ItemStack(ModItems.DEBUG_CARD.get());
        final DriverItem driver = Driver.driverFor(stack);
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        final String playerName = player.getGameProfile().getName();
        player.setShiftKeyDown(true);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);

        helper.assertTrue(driver != null, "No driver for debug card");
        withCachedConfig(ModSettings.DEBUG_CARD_ACCESS, "whitelist", () ->
            withCachedConfig(ModSettings.DEBUG_CARD_WHITELIST, List.of(playerName.toLowerCase(java.util.Locale.ROOT) + " nonce-1"), () -> {
                final ItemStack first = stack.use(helper.getLevel(), player, InteractionHand.MAIN_HAND).getObject();
                final DebugCardEnvironment.AccessContext access = DebugCardEnvironment.loadAccess(driver.dataTag(first));
                helper.assertTrue(access != null, "Debug card shift-use did not bind access context");
                helper.assertTrue(playerName.equals(access.player()), "Debug card bound wrong player");
                helper.assertTrue("nonce-1".equals(access.nonce()), "Debug card bound wrong nonce");

                final ItemStack second = first.use(helper.getLevel(), player, InteractionHand.MAIN_HAND).getObject();
                helper.assertTrue(DebugCardEnvironment.loadAccess(driver.dataTag(second)) == null, "Debug card second shift-use did not unbind access context");
            }));
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void debugCardWorldValueReadsAndSetsTime(final GameTestHelper helper) {
        final DebugCardEnvironment card = new DebugCardEnvironment(new StaticEnvironmentHost(helper));
        helper.assertTrue(card.node() instanceof li.cil.oc.api.network.Component, "Debug card did not expose component");
        final li.cil.oc.api.network.Component component = (li.cil.oc.api.network.Component) card.node();

        final Object[] worldResult = invokeComponent(helper, component, "getWorld");
        helper.assertTrue(worldResult.length == 1 && worldResult[0] instanceof Value, "Debug card getWorld did not return a value");
        final Value world = (Value) worldResult[0];

        helper.getLevel().setDayTime(1234L);
        final Object[] getTime = invokeValue(helper, world, "getTime");
        helper.assertTrue(getTime.length == 1 && Long.valueOf(1234L).equals(getTime[0]), "World value did not report current time");

        invokeValue(helper, world, "setTime", 5678L);
        helper.assertTrue(helper.getLevel().getDayTime() == 5678L, "World value did not set current time");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void debugCardWorldValueReadsAndSetsWeather(final GameTestHelper helper) {
        final DebugCardEnvironment card = new DebugCardEnvironment(new StaticEnvironmentHost(helper));
        helper.assertTrue(card.node() instanceof li.cil.oc.api.network.Component, "Debug card did not expose component");
        final li.cil.oc.api.network.Component component = (li.cil.oc.api.network.Component) card.node();

        final Object[] worldResult = invokeComponent(helper, component, "getWorld");
        helper.assertTrue(worldResult.length == 1 && worldResult[0] instanceof Value, "Debug card getWorld did not return a value");
        final Value world = (Value) worldResult[0];

        invokeValue(helper, world, "setRaining", true);
        final Object[] raining = invokeValue(helper, world, "isRaining");
        helper.assertTrue(raining.length == 1 && Boolean.TRUE.equals(raining[0]), "World value did not report rain");

        invokeValue(helper, world, "setRaining", false);
        final Object[] clear = invokeValue(helper, world, "isRaining");
        helper.assertTrue(clear.length == 1 && Boolean.FALSE.equals(clear[0]), "World value did not clear rain");

        invokeValue(helper, world, "setThundering", true);
        final Object[] thundering = invokeValue(helper, world, "isThundering");
        helper.assertTrue(thundering.length == 1 && Boolean.TRUE.equals(thundering[0]), "World value did not report thunder");

        invokeValue(helper, world, "setThundering", false);
        final Object[] quiet = invokeValue(helper, world, "isThundering");
        helper.assertTrue(quiet.length == 1 && Boolean.FALSE.equals(quiet[0]), "World value did not clear thunder");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void debugCardWorldValueReportsSeedAndDimensionName(final GameTestHelper helper) {
        final DebugCardEnvironment card = new DebugCardEnvironment(new StaticEnvironmentHost(helper));
        helper.assertTrue(card.node() instanceof li.cil.oc.api.network.Component, "Debug card did not expose component");
        final li.cil.oc.api.network.Component component = (li.cil.oc.api.network.Component) card.node();

        final Object[] worldResult = invokeComponent(helper, component, "getWorld");
        helper.assertTrue(worldResult.length == 1 && worldResult[0] instanceof Value, "Debug card getWorld did not return a value");
        final Value world = (Value) worldResult[0];

        final Object[] seed = invokeValue(helper, world, "getSeed");
        helper.assertTrue(seed.length == 1 && Long.valueOf(helper.getLevel().getSeed()).equals(seed[0]), "World value did not report seed");

        final Object[] dimensionName = invokeValue(helper, world, "getDimensionName");
        helper.assertTrue(dimensionName.length == 1 && helper.getLevel().dimension().location().toString().equals(dimensionName[0]), "World value did not report dimension name");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void debugCardWorldValueReadsAndSetsSpawnPoint(final GameTestHelper helper) {
        final DebugCardEnvironment card = new DebugCardEnvironment(new StaticEnvironmentHost(helper));
        helper.assertTrue(card.node() instanceof li.cil.oc.api.network.Component, "Debug card did not expose component");
        final li.cil.oc.api.network.Component component = (li.cil.oc.api.network.Component) card.node();

        final Object[] worldResult = invokeComponent(helper, component, "getWorld");
        helper.assertTrue(worldResult.length == 1 && worldResult[0] instanceof Value, "Debug card getWorld did not return a value");
        final Value world = (Value) worldResult[0];

        final BlockPos initial = new BlockPos(3, 65, 7);
        helper.getLevel().setDefaultSpawnPos(initial, 0F);
        final Object[] spawn = invokeValue(helper, world, "getSpawnPoint");
        helper.assertTrue(spawn.length == 3 && Integer.valueOf(3).equals(spawn[0]) && Integer.valueOf(65).equals(spawn[1]) && Integer.valueOf(7).equals(spawn[2]), "World value did not report spawn point");

        invokeValue(helper, world, "setSpawnPoint", 12, 66, 18);
        helper.assertTrue(new BlockPos(12, 66, 18).equals(helper.getLevel().getSharedSpawnPos()), "World value did not set spawn point");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void debugCardWorldValueReportsLoadedAndBlockEntityState(final GameTestHelper helper) {
        final DebugCardEnvironment card = new DebugCardEnvironment(new StaticEnvironmentHost(helper));
        helper.assertTrue(card.node() instanceof li.cil.oc.api.network.Component, "Debug card did not expose component");
        final li.cil.oc.api.network.Component component = (li.cil.oc.api.network.Component) card.node();

        final Object[] worldResult = invokeComponent(helper, component, "getWorld");
        helper.assertTrue(worldResult.length == 1 && worldResult[0] instanceof Value, "Debug card getWorld did not return a value");
        final Value world = (Value) worldResult[0];

        final BlockPos chestPos = new BlockPos(1, 1, 1);
        helper.setBlock(chestPos, Blocks.CHEST.defaultBlockState());
        final BlockPos absoluteChest = helper.absolutePos(chestPos);
        final Object[] loaded = invokeValue(helper, world, "isLoaded", absoluteChest.getX(), absoluteChest.getY(), absoluteChest.getZ());
        helper.assertTrue(loaded.length == 1 && Boolean.TRUE.equals(loaded[0]), "World value did not report loaded block");

        final Object[] hasTileEntity = invokeValue(helper, world, "hasTileEntity", absoluteChest.getX(), absoluteChest.getY(), absoluteChest.getZ());
        helper.assertTrue(hasTileEntity.length == 1 && Boolean.TRUE.equals(hasTileEntity[0]), "World value did not report block entity");

        final BlockPos absoluteAir = helper.absolutePos(chestPos.east());
        final Object[] noTileEntity = invokeValue(helper, world, "hasTileEntity", absoluteAir.getX(), absoluteAir.getY(), absoluteAir.getZ());
        helper.assertTrue(noTileEntity.length == 1 && Boolean.FALSE.equals(noTileEntity[0]), "World value reported block entity for air");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void debugCardWorldValueReportsBlockQueries(final GameTestHelper helper) {
        final DebugCardEnvironment card = new DebugCardEnvironment(new StaticEnvironmentHost(helper));
        helper.assertTrue(card.node() instanceof li.cil.oc.api.network.Component, "Debug card did not expose component");
        final li.cil.oc.api.network.Component component = (li.cil.oc.api.network.Component) card.node();

        final Object[] worldResult = invokeComponent(helper, component, "getWorld");
        helper.assertTrue(worldResult.length == 1 && worldResult[0] instanceof Value, "Debug card getWorld did not return a value");
        final Value world = (Value) worldResult[0];

        final BlockPos blockPos = new BlockPos(1, 1, 1);
        final BlockState state = Blocks.STONE.defaultBlockState();
        helper.setBlock(blockPos, state);
        final BlockPos absolute = helper.absolutePos(blockPos);

        final Object[] blockId = invokeValue(helper, world, "getBlockId", absolute.getX(), absolute.getY(), absolute.getZ());
        helper.assertTrue(blockId.length == 1 && Integer.valueOf(BuiltInRegistries.BLOCK.getId(Blocks.STONE)).equals(blockId[0]), "World value did not report block id");

        final Object[] metadata = invokeValue(helper, world, "getMetadata", absolute.getX(), absolute.getY(), absolute.getZ());
        helper.assertTrue(metadata.length == 1 && Integer.valueOf(0).equals(metadata[0]), "World value did not report metadata");

        final Object[] blockState = invokeValue(helper, world, "getBlockState", absolute.getX(), absolute.getY(), absolute.getZ());
        helper.assertTrue(blockState.length == 1 && state.equals(blockState[0]), "World value did not report block state");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void debugCardWorldValueReportsLightAndSkyQueries(final GameTestHelper helper) {
        final DebugCardEnvironment card = new DebugCardEnvironment(new StaticEnvironmentHost(helper));
        helper.assertTrue(card.node() instanceof li.cil.oc.api.network.Component, "Debug card did not expose component");
        final li.cil.oc.api.network.Component component = (li.cil.oc.api.network.Component) card.node();

        final Object[] worldResult = invokeComponent(helper, component, "getWorld");
        helper.assertTrue(worldResult.length == 1 && worldResult[0] instanceof Value, "Debug card getWorld did not return a value");
        final Value world = (Value) worldResult[0];

        final BlockPos blockPos = new BlockPos(1, 1, 1);
        final BlockState state = Blocks.GLOWSTONE.defaultBlockState();
        helper.setBlock(blockPos, state);
        final BlockPos absolute = helper.absolutePos(blockPos);
        final Object[] lightOpacity = invokeValue(helper, world, "getLightOpacity", absolute.getX(), absolute.getY(), absolute.getZ());
        helper.assertTrue(lightOpacity.length == 1 && Integer.valueOf(state.getLightBlock(helper.getLevel(), absolute)).equals(lightOpacity[0]), "World value did not report light opacity");

        final Object[] lightValue = invokeValue(helper, world, "getLightValue", absolute.getX(), absolute.getY(), absolute.getZ());
        helper.assertTrue(lightValue.length == 1 && Integer.valueOf(state.getLightEmission()).equals(lightValue[0]), "World value did not report light value");

        final BlockPos air = absolute.above();
        final Object[] canSeeSky = invokeValue(helper, world, "canSeeSky", air.getX(), air.getY(), air.getZ());
        helper.assertTrue(canSeeSky.length == 1 && Boolean.valueOf(helper.getLevel().canSeeSky(air)).equals(canSeeSky[0]), "World value did not report sky visibility");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void linkedCardRecipeAssignsSharedTunnel(final GameTestHelper helper) {
        final CraftingInput input = CraftingInput.of(3, 3, List.of(
            new ItemStack(Items.ENDER_EYE), ItemStack.EMPTY, new ItemStack(Items.ENDER_EYE),
            new ItemStack(ModItems.WIRELESS_NETWORK_CARD_TIER2.get()), new ItemStack(ModItems.INTERWEB.get()), new ItemStack(ModItems.WIRELESS_NETWORK_CARD_TIER2.get()),
            new ItemStack(ModItems.MICROCHIP_TIER3.get()), ItemStack.EMPTY, new ItemStack(ModItems.MICROCHIP_TIER3.get())
        ));
        final Optional<RecipeHolder<CraftingRecipe>> recipe = helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel());

        helper.assertTrue(recipe.isPresent(), "No linked card recipe matched");
        final ItemStack result = recipe.get().value().assemble(input, helper.getLevel().registryAccess());
        helper.assertTrue(result.is(ModItems.LINKED_CARD.get()), "Linked card recipe returned wrong item");
        helper.assertTrue(result.getCount() == 2, "Linked card recipe should craft paired cards");
        final DriverItem driver = Driver.driverFor(result);
        helper.assertTrue(driver != null, "No driver for linked card result");
        final String tunnel = driver.dataTag(result).getString(LinkedCardItem.TUNNEL_TAG);
        helper.assertTrue(!tunnel.isBlank(), "Linked card recipe did not assign a tunnel");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void inkCartridgeLeavesEmptyCraftingRemainder(final GameTestHelper helper) {
        final Item filled = ModItems.INK_CARTRIDGE.get();

        helper.assertTrue(filled.getDefaultMaxStackSize() == 1, "Ink cartridge should not stack");
        helper.assertTrue(filled.hasCraftingRemainingItem(), "Ink cartridge should have a crafting remainder");
        helper.assertTrue(filled.getCraftingRemainingItem() == ModItems.INK_CARTRIDGE_EMPTY.get(), "Ink cartridge remainder should be empty cartridge");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void mfuDriverIsAdapterOnly(final GameTestHelper helper) {
        final ItemStack stack = new ItemStack(ModItems.MFU.get());

        helper.assertTrue(Driver.driverFor(stack, AdapterBlockEntity.class) != null, "No MFU driver for adapter host");
        helper.assertTrue(Driver.driverFor(stack, AgentTestHost.class) == null, "MFU driver accepted non-adapter host");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void mfuCreatesRemoteAdapterEnvironmentForTaggedTarget(final GameTestHelper helper) {
        final BlockPos adapterPos = new BlockPos(0, 1, 0);
        final ItemStack stack = new ItemStack(ModItems.MFU.get());
        final DriverItem driver = Driver.driverFor(stack, AdapterBlockEntity.class);
        helper.assertTrue(driver != null, "No MFU driver for adapter host");

        driver.dataTag(stack).putIntArray("oc:coord", new int[]{
            helper.absolutePos(adapterPos).getX(),
            helper.absolutePos(adapterPos).getY(),
            helper.absolutePos(adapterPos).getZ(),
            Direction.NORTH.ordinal()
        });
        helper.assertTrue(driver.dataTag(stack).contains("oc:coord"), "MFU target data did not persist");

        final AdapterBlockEntity adapter = new AdapterBlockEntity(helper.absolutePos(BlockPos.ZERO), ModBlocks.ADAPTER.get().defaultBlockState());
        adapter.setLevel(helper.getLevel());
        final ManagedEnvironment environment = driver.createEnvironment(stack, adapter);
        helper.assertTrue(environment != null, "MFU did not create remote adapter environment");
        helper.assertTrue(environment.node() instanceof Connector, "MFU node is not a connector");
        helper.assertTrue(environment instanceof DeviceInfo, "MFU environment lacks device info");
        final Map<String, String> info = ((DeviceInfo) environment).getDeviceInfo();
        helper.assertTrue(DeviceInfo.DeviceClass.Bus.equals(info.get(DeviceInfo.DeviceAttribute.Class)), "MFU device class mismatch");
        helper.assertTrue("Remote Adapter".equals(info.get(DeviceInfo.DeviceAttribute.Description)), "MFU description mismatch");
        helper.assertTrue("Scummtech, Inc.".equals(info.get(DeviceInfo.DeviceAttribute.Vendor)), "MFU vendor mismatch");
        helper.assertTrue("ERR NAME NOT FOUND".equals(info.get(DeviceInfo.DeviceAttribute.Product)), "MFU product mismatch");
        helper.assertTrue(Driver.environmentFor(stack) != null, "MFU has no environment provider");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void mfuLinksRemoteBlockDriverEnvironment(final GameTestHelper helper) {
        final BlockPos adapterPos = new BlockPos(0, 1, 0);
        final BlockPos targetPos = new BlockPos(3, 1, 0);
        helper.setBlock(targetPos, Blocks.CHEST.defaultBlockState());

        final ItemStack stack = new ItemStack(ModItems.MFU.get());
        final DriverItem driver = Driver.driverFor(stack, AdapterBlockEntity.class);
        helper.assertTrue(driver != null, "No MFU driver for adapter host");
        final BlockPos absoluteTarget = helper.absolutePos(targetPos);
        driver.dataTag(stack).putIntArray("oc:coord", new int[]{
            absoluteTarget.getX(),
            absoluteTarget.getY(),
            absoluteTarget.getZ(),
            Direction.NORTH.ordinal()
        });

        final AdapterBlockEntity adapter = new AdapterBlockEntity(helper.absolutePos(adapterPos), ModBlocks.ADAPTER.get().defaultBlockState());
        adapter.setLevel(helper.getLevel());
        final ManagedEnvironment environment = driver.createEnvironment(stack, adapter);
        helper.assertTrue(environment != null, "MFU did not create remote adapter environment");
        Network.joinNewNetwork(environment.node());

        boolean foundInventory = false;
        for (final Node reachable : environment.node().reachableNodes()) {
            if (reachable instanceof li.cil.oc.api.network.Component component && "inventory".equals(component.name())) {
                foundInventory = true;
                break;
            }
        }
        helper.assertTrue(foundInventory, "MFU did not link remote inventory component");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void mfuRefreshesRemoteBlockDriverWhenTargetChanges(final GameTestHelper helper) {
        final BlockPos adapterPos = new BlockPos(0, 1, 0);
        final BlockPos targetPos = new BlockPos(3, 1, 0);
        helper.setBlock(targetPos, Blocks.CHEST.defaultBlockState());

        final ItemStack stack = new ItemStack(ModItems.MFU.get());
        final DriverItem driver = Driver.driverFor(stack, AdapterBlockEntity.class);
        helper.assertTrue(driver != null, "No MFU driver for adapter host");
        final BlockPos absoluteTarget = helper.absolutePos(targetPos);
        driver.dataTag(stack).putIntArray("oc:coord", new int[]{
            absoluteTarget.getX(),
            absoluteTarget.getY(),
            absoluteTarget.getZ(),
            Direction.NORTH.ordinal()
        });

        final AdapterBlockEntity adapter = new AdapterBlockEntity(helper.absolutePos(adapterPos), ModBlocks.ADAPTER.get().defaultBlockState());
        adapter.setLevel(helper.getLevel());
        final ManagedEnvironment environment = driver.createEnvironment(stack, adapter);
        helper.assertTrue(environment != null, "MFU did not create remote adapter environment");
        Network.joinNewNetwork(environment.node());
        helper.assertTrue(reachableComponent(environment.node(), "inventory"), "MFU did not link initial remote inventory component");

        helper.setBlock(targetPos, Blocks.AIR.defaultBlockState());
        environment.update();
        helper.assertFalse(reachableComponent(environment.node(), "inventory"), "MFU kept stale remote inventory component");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void mfuReadsUpstreamFiveIntTargetTag(final GameTestHelper helper) {
        final BlockPos adapterPos = new BlockPos(0, 1, 0);
        final BlockPos targetPos = new BlockPos(3, 1, 0);
        helper.setBlock(targetPos, Blocks.CHEST.defaultBlockState());

        final ItemStack stack = new ItemStack(ModItems.MFU.get());
        final DriverItem driver = Driver.driverFor(stack, AdapterBlockEntity.class);
        helper.assertTrue(driver != null, "No MFU driver for adapter host");
        final BlockPos absoluteTarget = helper.absolutePos(targetPos);
        driver.dataTag(stack).putIntArray("oc:coord", new int[]{
            absoluteTarget.getX(),
            absoluteTarget.getY(),
            absoluteTarget.getZ(),
            0,
            Direction.NORTH.ordinal()
        });

        final AdapterBlockEntity adapter = new AdapterBlockEntity(helper.absolutePos(adapterPos), ModBlocks.ADAPTER.get().defaultBlockState());
        adapter.setLevel(helper.getLevel());
        final ManagedEnvironment environment = driver.createEnvironment(stack, adapter);
        helper.assertTrue(environment instanceof MfuEnvironment, "MFU did not read upstream 5-int target tag");
        final MfuEnvironment mfu = (MfuEnvironment) environment;
        helper.assertTrue(mfu.side() == Direction.NORTH, "MFU read dimension as side for upstream target tag");
        Network.joinNewNetwork(mfu.node());
        helper.assertTrue(reachableComponent(mfu.node(), "inventory"), "MFU did not link remote inventory from upstream target tag");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void mfuRejectsWrongDimensionTargetTag(final GameTestHelper helper) {
        final BlockPos adapterPos = new BlockPos(0, 1, 0);
        final BlockPos targetPos = new BlockPos(3, 1, 0);
        helper.setBlock(targetPos, Blocks.CHEST.defaultBlockState());

        final ItemStack stack = new ItemStack(ModItems.MFU.get());
        final DriverItem driver = Driver.driverFor(stack, AdapterBlockEntity.class);
        helper.assertTrue(driver != null, "No MFU driver for adapter host");
        final BlockPos absoluteTarget = helper.absolutePos(targetPos);
        driver.dataTag(stack).putIntArray("oc:coord", new int[]{
            absoluteTarget.getX(),
            absoluteTarget.getY(),
            absoluteTarget.getZ(),
            -1,
            Direction.NORTH.ordinal()
        });

        final AdapterBlockEntity adapter = new AdapterBlockEntity(helper.absolutePos(adapterPos), ModBlocks.ADAPTER.get().defaultBlockState());
        adapter.setLevel(helper.getLevel());
        helper.assertTrue(driver.createEnvironment(stack, adapter) == null, "MFU accepted upstream tag for wrong dimension");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void mfuRejectsTargetsOutsideDefaultRange(final GameTestHelper helper) {
        final BlockPos adapterPos = new BlockPos(0, 1, 0);
        final BlockPos targetPos = new BlockPos(4, 1, 0);
        helper.setBlock(targetPos, Blocks.CHEST.defaultBlockState());

        final ItemStack stack = new ItemStack(ModItems.MFU.get());
        final DriverItem driver = Driver.driverFor(stack, AdapterBlockEntity.class);
        helper.assertTrue(driver != null, "No MFU driver for adapter host");
        final BlockPos absoluteTarget = helper.absolutePos(targetPos);
        driver.dataTag(stack).putIntArray("oc:coord", new int[]{
            absoluteTarget.getX(),
            absoluteTarget.getY(),
            absoluteTarget.getZ(),
            0,
            Direction.NORTH.ordinal()
        });

        final AdapterBlockEntity adapter = new AdapterBlockEntity(helper.absolutePos(adapterPos), ModBlocks.ADAPTER.get().defaultBlockState());
        adapter.setLevel(helper.getLevel());
        final ManagedEnvironment environment = driver.createEnvironment(stack, adapter);
        helper.assertTrue(environment != null, "MFU did not create environment for range check");
        Network.joinNewNetwork(environment.node());
        helper.assertFalse(reachableComponent(environment.node(), "inventory"), "MFU linked target outside upstream default range");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void mfuShiftUseStoresUpstreamTargetTag(final GameTestHelper helper) {
        final BlockPos targetPos = new BlockPos(2, 1, 0);
        helper.setBlock(targetPos, Blocks.CHEST.defaultBlockState());

        final ItemStack stack = new ItemStack(ModItems.MFU.get());
        final Player player = helper.makeMockPlayer(GameType.CREATIVE);
        player.setShiftKeyDown(true);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        final BlockPos absoluteTarget = helper.absolutePos(targetPos);
        final BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absoluteTarget), Direction.SOUTH, absoluteTarget, false);

        final InteractionResult result = stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        helper.assertTrue(result.consumesAction(), "MFU shift-use did not consume target link action");

        final int[] coord = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getIntArray("oc:coord");
        helper.assertTrue(coord.length == 5, "MFU shift-use did not write upstream 5-int target tag");
        helper.assertTrue(coord[0] == absoluteTarget.getX(), "MFU stored wrong target x");
        helper.assertTrue(coord[1] == absoluteTarget.getY(), "MFU stored wrong target y");
        helper.assertTrue(coord[2] == absoluteTarget.getZ(), "MFU stored wrong target z");
        helper.assertTrue(coord[3] == 0, "MFU stored wrong target dimension");
        helper.assertTrue(coord[4] == Direction.SOUTH.ordinal(), "MFU stored wrong target side");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void nanomachinesItemInstallsController(final GameTestHelper helper) {
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        final ItemStack stack = new ItemStack(ModItems.NANOMACHINES.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);

        helper.assertFalse(li.cil.oc.api.Nanomachines.hasController(player), "Player already had nanomachine controller");
        stack.getItem().finishUsingItem(stack, helper.getLevel(), player);

        helper.assertTrue(stack.isEmpty(), "Nanomachines item was not consumed");
        helper.assertTrue(li.cil.oc.api.Nanomachines.hasController(player), "Nanomachines item did not install controller");
        helper.assertTrue(li.cil.oc.api.Nanomachines.getController(player) != null, "Installed nanomachine controller was not available");

        li.cil.oc.api.Nanomachines.uninstallController(player);
        helper.assertFalse(li.cil.oc.api.Nanomachines.hasController(player), "Nanomachines uninstall did not clear controller state");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void nanomachinesInputChangesNotifyBehaviorLifecycle(final GameTestHelper helper) {
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        final li.cil.oc.common.NanomachinesRegistry registry = new li.cil.oc.common.NanomachinesRegistry();
        final RecordingNanomachineBehavior behavior = new RecordingNanomachineBehavior();
        registry.addProvider(new RecordingNanomachineProvider(behavior));
        final li.cil.oc.api.nanomachines.Controller controller = registry.installController(player);

        helper.assertFalse(containsBehavior(controller.getActiveBehaviors(), behavior), "Nanomachines behavior started active");
        helper.assertTrue(controller.setInput(0, true), "Nanomachines input rejected first active input");
        helper.assertTrue(containsBehavior(controller.getActiveBehaviors(), behavior), "Nanomachines behavior did not become active");
        helper.assertTrue(behavior.enableCount == 1, "Nanomachines input did not enable behavior");
        helper.assertTrue(controller.setInput(0, false), "Nanomachines input rejected deactivation");
        helper.assertFalse(containsBehavior(controller.getActiveBehaviors(), behavior), "Nanomachines behavior stayed active after input disable");
        helper.assertTrue(behavior.disableCount == 1, "Nanomachines input did not disable behavior");
        helper.assertTrue(behavior.disableReason == li.cil.oc.api.nanomachines.DisableReason.InputChanged, "Nanomachines behavior disable reason was not input changed");

        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void nanomachinesControllerStatePersistsAcrossRegistryReload(final GameTestHelper helper) {
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        final li.cil.oc.common.NanomachinesRegistry firstRegistry = new li.cil.oc.common.NanomachinesRegistry();
        final li.cil.oc.api.nanomachines.Controller firstController = firstRegistry.installController(player);
        firstController.setInput(0, true);
        firstController.changeBuffer(-1234D);

        final li.cil.oc.common.NanomachinesRegistry reloadedRegistry = new li.cil.oc.common.NanomachinesRegistry();
        final li.cil.oc.api.nanomachines.Controller reloadedController = reloadedRegistry.getController(player);

        helper.assertTrue(reloadedController != null, "Nanomachines controller did not reload");
        helper.assertTrue(reloadedController.getInput(0), "Nanomachines controller did not persist active input");
        helper.assertTrue(Math.abs(reloadedController.getLocalBuffer() - firstController.getLocalBuffer()) < 0.001D, "Nanomachines controller did not persist energy");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void nanomachinesBlankItemReinstallResetsControllerState(final GameTestHelper helper) {
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        final li.cil.oc.api.nanomachines.Controller firstController = li.cil.oc.api.Nanomachines.installController(player);
        firstController.setInput(0, true);
        firstController.changeBuffer(-1234D);
        final ItemStack stack = new ItemStack(ModItems.NANOMACHINES.get());

        stack.getItem().finishUsingItem(stack, helper.getLevel(), player);
        final li.cil.oc.api.nanomachines.Controller reinstalledController = li.cil.oc.api.Nanomachines.getController(player);

        helper.assertTrue(reinstalledController != null, "Nanomachines reinstall did not install controller");
        helper.assertFalse(reinstalledController.getInput(0), "Nanomachines blank reinstall kept stale active input");
        helper.assertTrue(reinstalledController.getLocalBuffer() > firstController.getLocalBuffer(), "Nanomachines blank reinstall kept stale energy");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void nanomachinesConfiguredItemRestoresSavedConfiguration(final GameTestHelper helper) {
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        final li.cil.oc.common.NanomachinesRegistry previous = API.nanomachines instanceof li.cil.oc.common.NanomachinesRegistry registry ? registry : null;
        final li.cil.oc.common.NanomachinesRegistry registry = new li.cil.oc.common.NanomachinesRegistry();
        final RecordingNanomachineBehavior behavior = new RecordingNanomachineBehavior();
        registry.addProvider(new RecordingNanomachineProvider(behavior));
        final ItemStack stack = new ItemStack(ModItems.NANOMACHINES.get());
        final CompoundTag root = new CompoundTag();
        root.putString("oc:uuid", "configured-controller");
        root.put("oc:configuration", nanomachineConfigurationTag(3));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        API.nanomachines = registry;
        try {
            stack.getItem().finishUsingItem(stack, helper.getLevel(), player);
            final li.cil.oc.api.nanomachines.Controller controller = registry.getController(player);

            helper.assertTrue(controller != null, "Configured nanomachines item did not install controller");
            helper.assertTrue(controller.getTotalInputCount() == 4, "Configured nanomachines item did not restore saved trigger count");
            helper.assertFalse(controller.getInput(3), "Configured nanomachines item restored active input state");
            helper.assertTrue(controller.setInput(3, true), "Configured nanomachines item rejected saved trigger input");
            helper.assertTrue(containsBehavior(controller.getActiveBehaviors(), behavior), "Configured nanomachines item did not restore saved behavior wiring");
        } finally {
            API.nanomachines = previous;
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void nanomachinesWirelessSaveConfigurationWritesBlankItem(final GameTestHelper helper) {
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        final ItemStack blank = new ItemStack(ModItems.NANOMACHINES.get());
        player.getInventory().add(blank);
        final li.cil.oc.api.nanomachines.Controller controller = li.cil.oc.api.Nanomachines.installController(player);
        final li.cil.oc.api.network.WirelessEndpoint endpoint = (li.cil.oc.api.network.WirelessEndpoint) controller;
        final RecordingWirelessEndpoint sender = new RecordingWirelessEndpoint(helper.getLevel(), player.blockPosition());
        Network.joinWirelessNetwork(sender);
        endpoint.receivePacket(Network.newPacket("sender", null, 1, new Object[]{"nanomachines", "setResponsePort", 563}), sender);
        runNanomachinesCommandDelay(player);
        sender.lastPacket = null;

        endpoint.receivePacket(Network.newPacket("sender", null, 1, new Object[]{"nanomachines", "saveConfiguration"}), sender);
        runNanomachinesCommandDelay(player);

        helper.assertTrue(sender.lastPacket != null, "Nanomachines saveConfiguration did not respond");
        helper.assertTrue(sender.lastPacket.port() == 563, "Nanomachines saveConfiguration used wrong response port");
        helper.assertTrue(java.util.Arrays.equals(new Object[]{"nanomachines", "saved", true}, sender.lastPacket.data()), "Nanomachines saveConfiguration returned wrong payload");
        final ItemStack saved = player.getInventory().items.stream()
            .filter(stack -> stack.is(ModItems.NANOMACHINES.get()))
            .filter(stack -> stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().contains("oc:configuration", CompoundTag.TAG_COMPOUND))
            .findFirst()
            .orElse(ItemStack.EMPTY);
        helper.assertFalse(saved.isEmpty(), "Nanomachines saveConfiguration did not write configuration to a blank item");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void nanomachinesWirelessHealthCommandReportsPlayerHealth(final GameTestHelper helper) {
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setHealth(7.5F);
        final li.cil.oc.api.nanomachines.Controller controller = li.cil.oc.api.Nanomachines.installController(player);
        final li.cil.oc.api.network.WirelessEndpoint endpoint = (li.cil.oc.api.network.WirelessEndpoint) controller;
        final RecordingWirelessEndpoint sender = new RecordingWirelessEndpoint(helper.getLevel(), player.blockPosition());
        Network.joinWirelessNetwork(sender);
        endpoint.receivePacket(Network.newPacket("sender", null, 1, new Object[]{"nanomachines", "setResponsePort", 558}), sender);
        runNanomachinesCommandDelay(player);
        sender.lastPacket = null;

        endpoint.receivePacket(Network.newPacket("sender", null, 1, new Object[]{"nanomachines", "getHealth"}), sender);
        runNanomachinesCommandDelay(player);

        helper.assertTrue(sender.lastPacket != null, "Nanomachines health command did not respond");
        helper.assertTrue(sender.lastPacket.port() == 558, "Nanomachines health command used wrong response port");
        helper.assertTrue(java.util.Arrays.equals(new Object[]{"nanomachines", "health", 7.5F, player.getMaxHealth()}, sender.lastPacket.data()), "Nanomachines health command returned wrong payload");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void nanomachinesWirelessHungerCommandReportsPlayerFoodState(final GameTestHelper helper) {
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getFoodData().setFoodLevel(6);
        player.getFoodData().setSaturation(2.5F);
        final li.cil.oc.api.nanomachines.Controller controller = li.cil.oc.api.Nanomachines.installController(player);
        final li.cil.oc.api.network.WirelessEndpoint endpoint = (li.cil.oc.api.network.WirelessEndpoint) controller;
        final RecordingWirelessEndpoint sender = new RecordingWirelessEndpoint(helper.getLevel(), player.blockPosition());
        Network.joinWirelessNetwork(sender);
        endpoint.receivePacket(Network.newPacket("sender", null, 1, new Object[]{"nanomachines", "setResponsePort", 559}), sender);
        runNanomachinesCommandDelay(player);
        sender.lastPacket = null;

        endpoint.receivePacket(Network.newPacket("sender", null, 1, new Object[]{"nanomachines", "getHunger"}), sender);
        runNanomachinesCommandDelay(player);

        helper.assertTrue(sender.lastPacket != null, "Nanomachines hunger command did not respond");
        helper.assertTrue(sender.lastPacket.port() == 559, "Nanomachines hunger command used wrong response port");
        helper.assertTrue(java.util.Arrays.equals(new Object[]{"nanomachines", "hunger", 6, 2.5F}, sender.lastPacket.data()), "Nanomachines hunger command returned wrong payload");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void nanomachinesWirelessAgeCommandReportsPlayerAgeSeconds(final GameTestHelper helper) {
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.tickCount = 85;
        final li.cil.oc.api.nanomachines.Controller controller = li.cil.oc.api.Nanomachines.installController(player);
        final li.cil.oc.api.network.WirelessEndpoint endpoint = (li.cil.oc.api.network.WirelessEndpoint) controller;
        final RecordingWirelessEndpoint sender = new RecordingWirelessEndpoint(helper.getLevel(), player.blockPosition());
        Network.joinWirelessNetwork(sender);
        endpoint.receivePacket(Network.newPacket("sender", null, 1, new Object[]{"nanomachines", "setResponsePort", 560}), sender);
        runNanomachinesCommandDelay(player);
        sender.lastPacket = null;

        endpoint.receivePacket(Network.newPacket("sender", null, 1, new Object[]{"nanomachines", "getAge"}), sender);
        runNanomachinesCommandDelay(player);

        helper.assertTrue(sender.lastPacket != null, "Nanomachines age command did not respond");
        helper.assertTrue(sender.lastPacket.port() == 560, "Nanomachines age command used wrong response port");
        helper.assertTrue(java.util.Arrays.equals(new Object[]{"nanomachines", "age", 4}, sender.lastPacket.data()), "Nanomachines age command returned wrong payload");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void nanomachinesWirelessNameCommandReportsPlayerDisplayName(final GameTestHelper helper) {
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        final li.cil.oc.api.nanomachines.Controller controller = li.cil.oc.api.Nanomachines.installController(player);
        final li.cil.oc.api.network.WirelessEndpoint endpoint = (li.cil.oc.api.network.WirelessEndpoint) controller;
        final RecordingWirelessEndpoint sender = new RecordingWirelessEndpoint(helper.getLevel(), player.blockPosition());
        Network.joinWirelessNetwork(sender);
        endpoint.receivePacket(Network.newPacket("sender", null, 1, new Object[]{"nanomachines", "setResponsePort", 561}), sender);
        runNanomachinesCommandDelay(player);
        sender.lastPacket = null;

        endpoint.receivePacket(Network.newPacket("sender", null, 1, new Object[]{"nanomachines", "getName"}), sender);
        runNanomachinesCommandDelay(player);

        helper.assertTrue(sender.lastPacket != null, "Nanomachines name command did not respond");
        helper.assertTrue(sender.lastPacket.port() == 561, "Nanomachines name command used wrong response port");
        helper.assertTrue(java.util.Arrays.equals(new Object[]{"nanomachines", "name", "test-mock-player"}, sender.lastPacket.data()), "Nanomachines name command returned wrong payload");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void nanomachinesWirelessExperienceCommandReportsPlayerExperienceLevel(final GameTestHelper helper) {
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.experienceLevel = 12;
        final li.cil.oc.api.nanomachines.Controller controller = li.cil.oc.api.Nanomachines.installController(player);
        final li.cil.oc.api.network.WirelessEndpoint endpoint = (li.cil.oc.api.network.WirelessEndpoint) controller;
        final RecordingWirelessEndpoint sender = new RecordingWirelessEndpoint(helper.getLevel(), player.blockPosition());
        Network.joinWirelessNetwork(sender);
        endpoint.receivePacket(Network.newPacket("sender", null, 1, new Object[]{"nanomachines", "setResponsePort", 562}), sender);
        runNanomachinesCommandDelay(player);
        sender.lastPacket = null;

        endpoint.receivePacket(Network.newPacket("sender", null, 1, new Object[]{"nanomachines", "getExperience"}), sender);
        runNanomachinesCommandDelay(player);

        helper.assertTrue(sender.lastPacket != null, "Nanomachines experience command did not respond");
        helper.assertTrue(sender.lastPacket.port() == 562, "Nanomachines experience command used wrong response port");
        helper.assertTrue(java.util.Arrays.equals(new Object[]{"nanomachines", "experience", 12}, sender.lastPacket.data()), "Nanomachines experience command returned wrong payload");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void nanomachinesWirelessCommandsIgnoreFarSenders(final GameTestHelper helper) {
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        final li.cil.oc.api.nanomachines.Controller controller = li.cil.oc.api.Nanomachines.installController(player);
        final li.cil.oc.api.network.WirelessEndpoint endpoint = (li.cil.oc.api.network.WirelessEndpoint) controller;
        final RecordingWirelessEndpoint sender = new RecordingWirelessEndpoint(helper.getLevel(), player.blockPosition().offset(6, 0, 0));
        Network.joinWirelessNetwork(sender);

        endpoint.receivePacket(Network.newPacket("sender", null, 1, new Object[]{"nanomachines", "setInput", 1, true}), sender);

        helper.assertTrue(sender.lastPacket == null, "Nanomachines command responded outside command range");
        helper.assertFalse(controller.getInput(0), "Nanomachines command changed input outside command range");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void nanomachinesOverloadDamagesPlayersAboveSafeInputCount(final GameTestHelper helper) {
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 4));
        final li.cil.oc.common.NanomachinesRegistry registry = new li.cil.oc.common.NanomachinesRegistry();
        registry.addProvider(new RecordingNanomachineProvider(List.of(
            new RecordingNanomachineBehavior(),
            new RecordingNanomachineBehavior(),
            new RecordingNanomachineBehavior(),
            new RecordingNanomachineBehavior(),
            new RecordingNanomachineBehavior(),
            new RecordingNanomachineBehavior(),
            new RecordingNanomachineBehavior(),
            new RecordingNanomachineBehavior()
        )));
        final li.cil.oc.api.nanomachines.Controller controller = registry.installController(player);
        helper.assertTrue(controller.getTotalInputCount() > controller.getSafeActiveInputs(), "Nanomachines test controller has too few inputs for overload");
        for (int input = 0; input <= controller.getSafeActiveInputs(); input++) {
            helper.assertTrue(controller.setInput(input, true), "Nanomachines rejected overload test input " + input);
        }

        final float before = player.getHealth();
        runNanomachinesTicks(registry, player, 20);

        final float expected = before - 1F;
        helper.assertTrue(Math.abs(player.getHealth() - expected) < 0.001F, "Nanomachines overload did not bypass resistance");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void nanomachinesHungryDamageBypassesResistance(final GameTestHelper helper) {
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 100, 4));
        final NanomachineHungryProvider provider = new NanomachineHungryProvider();
        final li.cil.oc.api.nanomachines.Behavior behavior = provider.createBehaviors(player).iterator().next();

        final float before = player.getHealth();
        behavior.onDisable(li.cil.oc.api.nanomachines.DisableReason.OutOfEnergy);

        final float expected = before - (float) ModSettings.nanomachinesHungryDamage();
        helper.assertTrue(Math.abs(player.getHealth() - expected) < 0.001F, "Hungry damage did not bypass resistance");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void nanomachinesPotionProviderAppliesAndRemovesEffects(final GameTestHelper helper) {
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        final NanomachinePotionProvider provider = new NanomachinePotionProvider();
        final li.cil.oc.api.nanomachines.Behavior speed = nanomachineBehaviorByName(provider.createBehaviors(player), "speed");

        helper.assertFalse(player.hasEffect(MobEffects.MOVEMENT_SPEED), "Player already had speed effect");
        speed.update();

        final MobEffectInstance effect = player.getEffect(MobEffects.MOVEMENT_SPEED);
        helper.assertTrue(effect != null, "Nanomachines potion behavior did not apply speed");
        helper.assertTrue(effect.getAmplifier() == 0, "Nanomachines potion behavior applied wrong amplifier");
        helper.assertTrue(effect.getDuration() <= 600 && effect.getDuration() > 0, "Nanomachines potion behavior applied wrong duration");

        speed.onDisable(li.cil.oc.api.nanomachines.DisableReason.InputChanged);

        helper.assertFalse(player.hasEffect(MobEffects.MOVEMENT_SPEED), "Nanomachines potion behavior did not remove speed");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 300)
    public static void nanomachinesDisintegrationBreaksNearbyBlocks(final GameTestHelper helper) {
        final BlockPos playerPos = new BlockPos(1, 1, 1);
        final BlockPos targetPos = playerPos.relative(Direction.NORTH);
        helper.setBlock(targetPos, Blocks.DIRT.defaultBlockState());
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getAbilities().mayBuild = true;
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SHOVEL));
        player.moveTo(Vec3.atBottomCenterOf(helper.absolutePos(playerPos)));
        final li.cil.oc.common.NanomachinesRegistry registry = new li.cil.oc.common.NanomachinesRegistry();
        registry.addProvider(new NanomachineDisintegrationProvider());
        final li.cil.oc.api.detail.NanomachinesAPI previous = API.nanomachines;
        API.nanomachines = registry;
        try {
            final li.cil.oc.api.nanomachines.Controller controller = registry.installController(player);
            helper.assertTrue(controller.setInput(0, true), "Nanomachines rejected disintegration input");
            helper.startSequence()
                .thenExecuteFor(200, () -> registry.update(player))
                .thenExecute(() -> {
                    try {
                        helper.assertTrue(helper.getBlockState(targetPos).isAir(), "Disintegration did not break nearby dirt");
                    } finally {
                        API.nanomachines = previous;
                    }
                })
                .thenSucceed();
        } catch (RuntimeException e) {
            API.nanomachines = previous;
            throw e;
        }
    }

    @GameTest(template = "empty", timeoutTicks = 120)
    public static void nanomachinesDisintegrationFinishesCompletedTrackedBlocksOutsideCurrentRange(final GameTestHelper helper) {
        final BlockPos playerPos = new BlockPos(1, 1, 1);
        final BlockPos targetPos = playerPos.relative(Direction.NORTH);
        final BlockPos movedPos = playerPos.offset(3, 0, 3);
        helper.setBlock(targetPos, Blocks.DIRT.defaultBlockState());
        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.getAbilities().mayBuild = true;
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SHOVEL));
        player.moveTo(Vec3.atBottomCenterOf(helper.absolutePos(playerPos)));
        final li.cil.oc.api.nanomachines.Behavior behavior = new NanomachineDisintegrationProvider().createBehaviors(player).iterator().next();
        final li.cil.oc.api.nanomachines.Controller controller = fixedInputController(behavior, 1);
        final li.cil.oc.api.detail.NanomachinesAPI nanomachines = singleControllerNanomachinesApi(player, controller);
        final li.cil.oc.api.detail.NanomachinesAPI previous = API.nanomachines;
        try {
            API.nanomachines = nanomachines;
            behavior.update();
            API.nanomachines = previous;
            helper.startSequence()
                .thenIdle(80)
                .thenExecute(() -> {
                    player.moveTo(Vec3.atBottomCenterOf(helper.absolutePos(movedPos)));
                    API.nanomachines = nanomachines;
                    behavior.update();
                    API.nanomachines = previous;
                    try {
                        helper.assertTrue(helper.getBlockState(targetPos).isAir(), "Disintegration did not finish completed tracked block outside current range");
                    } finally {
                        API.nanomachines = previous;
                    }
                })
                .thenSucceed();
        } catch (RuntimeException e) {
            API.nanomachines = previous;
            throw e;
        }
    }

    private static li.cil.oc.api.detail.NanomachinesAPI singleControllerNanomachinesApi(
        final Player owner,
        final li.cil.oc.api.nanomachines.Controller controller
    ) {
        return new li.cil.oc.api.detail.NanomachinesAPI() {
            @Override
            public void addProvider(final li.cil.oc.api.nanomachines.BehaviorProvider provider) {
            }

            @Override
            public Iterable<li.cil.oc.api.nanomachines.BehaviorProvider> getProviders() {
                return java.util.List.of();
            }

            @Override
            public boolean hasController(final Player player) {
                return player == owner;
            }

            @Override
            public li.cil.oc.api.nanomachines.Controller getController(final Player player) {
                return player == owner ? controller : null;
            }

            @Override
            public li.cil.oc.api.nanomachines.Controller installController(final Player player) {
                return player == owner ? controller : null;
            }

            @Override
            public void uninstallController(final Player player) {
            }
        };
    }

    private static li.cil.oc.api.nanomachines.Controller fixedInputController(
        final li.cil.oc.api.nanomachines.Behavior behavior,
        final int inputCount
    ) {
        return new li.cil.oc.api.nanomachines.Controller() {
            @Override
            public li.cil.oc.api.nanomachines.Controller reconfigure() {
                return this;
            }

            @Override
            public int getTotalInputCount() {
                return inputCount;
            }

            @Override
            public int getSafeActiveInputs() {
                return inputCount;
            }

            @Override
            public int getMaxActiveInputs() {
                return inputCount;
            }

            @Override
            public boolean getInput(final int index) {
                return index >= 0 && index < inputCount;
            }

            @Override
            public boolean setInput(final int index, final boolean value) {
                return index >= 0 && index < inputCount && value;
            }

            @Override
            public Iterable<li.cil.oc.api.nanomachines.Behavior> getActiveBehaviors() {
                return java.util.List.of(behavior);
            }

            @Override
            public int getInputCount(final li.cil.oc.api.nanomachines.Behavior queriedBehavior) {
                return queriedBehavior == behavior ? inputCount : 0;
            }

            @Override
            public double getLocalBuffer() {
                return 1D;
            }

            @Override
            public double getLocalBufferSize() {
                return 1D;
            }

            @Override
            public double changeBuffer(final double delta) {
                return 1D;
            }
        };
    }

    @GameTest(template = "empty")
    public static void mfuLinksRemoteSidedTileEnvironment(final GameTestHelper helper) {
        final BlockPos adapterPos = new BlockPos(0, 1, 0);
        final BlockPos targetPos = new BlockPos(2, 1, 0);
        helper.setBlock(targetPos, ModBlocks.HOLOGRAM_TIER1.get());

        final ItemStack stack = new ItemStack(ModItems.MFU.get());
        final DriverItem driver = Driver.driverFor(stack, AdapterBlockEntity.class);
        helper.assertTrue(driver != null, "No MFU driver for adapter host");
        final BlockPos absoluteTarget = helper.absolutePos(targetPos);
        driver.dataTag(stack).putIntArray("oc:coord", new int[]{
            absoluteTarget.getX(),
            absoluteTarget.getY(),
            absoluteTarget.getZ(),
            0,
            Direction.DOWN.ordinal()
        });

        final AdapterBlockEntity adapter = new AdapterBlockEntity(helper.absolutePos(adapterPos), ModBlocks.ADAPTER.get().defaultBlockState());
        adapter.setLevel(helper.getLevel());
        final ManagedEnvironment environment = driver.createEnvironment(stack, adapter);
        helper.assertTrue(environment != null, "MFU did not create remote adapter environment");
        Network.joinNewNetwork(environment.node());
        helper.assertTrue(reachableComponent(environment.node(), "hologram"), "MFU did not link remote sided tile environment component");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void mfuDisconnectsRemoteTargetWithoutEnergy(final GameTestHelper helper) {
        final BlockPos adapterPos = new BlockPos(0, 1, 0);
        final BlockPos targetPos = new BlockPos(2, 1, 0);
        helper.setBlock(targetPos, Blocks.CHEST.defaultBlockState());

        final ItemStack stack = new ItemStack(ModItems.MFU.get());
        final DriverItem driver = Driver.driverFor(stack, AdapterBlockEntity.class);
        helper.assertTrue(driver != null, "No MFU driver for adapter host");
        final BlockPos absoluteTarget = helper.absolutePos(targetPos);
        driver.dataTag(stack).putIntArray("oc:coord", new int[]{
            absoluteTarget.getX(),
            absoluteTarget.getY(),
            absoluteTarget.getZ(),
            0,
            Direction.NORTH.ordinal()
        });

        final AdapterBlockEntity adapter = new AdapterBlockEntity(helper.absolutePos(adapterPos), ModBlocks.ADAPTER.get().defaultBlockState());
        adapter.setLevel(helper.getLevel());
        final ManagedEnvironment environment = driver.createEnvironment(stack, adapter);
        helper.assertTrue(environment != null, "MFU did not create remote adapter environment");
        helper.assertTrue(environment.node() instanceof Connector, "MFU node is not a connector");
        final Connector connector = (Connector) environment.node();
        connector.setLocalBufferSize(1D);
        connector.changeBuffer(1D);
        Network.joinNewNetwork(connector);
        helper.assertTrue(reachableComponent(environment.node(), "inventory"), "MFU did not link initial remote inventory component");

        helper.succeedWhen(() -> {
            environment.update();
            helper.assertFalse(reachableComponent(environment.node(), "inventory"), "MFU kept remote target linked without energy");
        });
    }

    @GameTest(template = "empty")
    public static void mfuRefreshesRemoteTargetOnNeighborNotify(final GameTestHelper helper) {
        final BlockPos adapterPos = new BlockPos(0, 1, 0);
        final BlockPos targetPos = new BlockPos(2, 1, 0);
        helper.setBlock(targetPos, Blocks.CHEST.defaultBlockState());

        final ItemStack stack = new ItemStack(ModItems.MFU.get());
        final DriverItem driver = Driver.driverFor(stack, AdapterBlockEntity.class);
        helper.assertTrue(driver != null, "No MFU driver for adapter host");
        final BlockPos absoluteTarget = helper.absolutePos(targetPos);
        driver.dataTag(stack).putIntArray("oc:coord", new int[]{
            absoluteTarget.getX(),
            absoluteTarget.getY(),
            absoluteTarget.getZ(),
            0,
            Direction.NORTH.ordinal()
        });

        final AdapterBlockEntity adapter = new AdapterBlockEntity(helper.absolutePos(adapterPos), ModBlocks.ADAPTER.get().defaultBlockState());
        adapter.setLevel(helper.getLevel());
        final ManagedEnvironment environment = driver.createEnvironment(stack, adapter);
        helper.assertTrue(environment != null, "MFU did not create remote adapter environment");
        Network.joinNewNetwork(environment.node());
        helper.assertTrue(reachableComponent(environment.node(), "inventory"), "MFU did not link initial remote inventory component");

        helper.setBlock(targetPos, Blocks.AIR.defaultBlockState());
        NeoForge.EVENT_BUS.post(new BlockEvent.NeighborNotifyEvent(
            helper.getLevel(),
            absoluteTarget,
            Blocks.AIR.defaultBlockState(),
            EnumSet.allOf(Direction.class),
            false));
        helper.assertFalse(reachableComponent(environment.node(), "inventory"), "MFU ignored target block change event");
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
    public static void tabletItemRegistersDriverSlotAndTier(final GameTestHelper helper) {
        final ItemStack stack = new ItemStack(ModItems.TABLET.get());
        final TabletItem tablet = (TabletItem) stack.getItem();

        tablet.setTier(stack, 1);
        tablet.setRunning(stack, true);
        final DriverItem driver = Driver.driverFor(stack);

        helper.assertTrue(driver != null, "No driver registered for tablet item");
        helper.assertTrue(Slot.Tablet.equals(driver.slot(stack)), "Tablet driver slot mismatch");
        helper.assertTrue(driver.tier(stack) == 1, "Tablet driver tier mismatch");
        helper.assertTrue(driver.dataTag(stack).getBoolean("running"), "Tablet driver data tag did not expose stored data");
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
        final ComponentConnector component = (ComponentConnector) transposer.node();
        component.setLocalBufferSize(1D);
        component.changeBuffer(1D);

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
        helper.assertTrue(Double.compare(0D, component.localBuffer()) == 0, "Transposer fluid transfer did not consume energy");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void transposerPausesAfterFluidTransfer(final GameTestHelper helper) {
        final BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.TRANSPOSER.get());
        helper.setBlock(pos.relative(Direction.WEST), Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
        helper.setBlock(pos.relative(Direction.EAST), Blocks.CAULDRON);
        final TransposerBlockEntity transposer = helper.getBlockEntity(pos);
        final ComponentConnector component = (ComponentConnector) transposer.node();
        component.setLocalBufferSize(1D);
        component.changeBuffer(1D);
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
    public static void transposerTransferItemRequiresEnergy(final GameTestHelper helper) {
        final BlockPos pos = new BlockPos(1, 1, 1);
        final BlockPos sourcePos = pos.relative(Direction.WEST);
        final BlockPos sinkPos = pos.relative(Direction.EAST);
        helper.setBlock(pos, ModBlocks.TRANSPOSER.get());
        helper.setBlock(sourcePos, Blocks.CHEST);
        helper.setBlock(sinkPos, Blocks.CHEST);
        final TransposerBlockEntity transposer = helper.getBlockEntity(pos);
        final ComponentConnector component = (ComponentConnector) transposer.node();
        final net.minecraft.world.Container source = helper.getBlockEntity(sourcePos);
        final net.minecraft.world.Container sink = helper.getBlockEntity(sinkPos);
        source.setItem(0, new ItemStack(Items.DIAMOND));

        final Object[] noEnergy = invokeComponent(helper, component, "transferItem", Direction.WEST.get3DDataValue(), Direction.EAST.get3DDataValue(), 1);
        assertNoEnergy(helper, noEnergy, "Transposer item transfer");
        helper.assertTrue(source.getItem(0).is(Items.DIAMOND), "Transposer moved item without energy");
        helper.assertTrue(sink.getItem(0).isEmpty(), "Transposer inserted item without energy");

        component.setLocalBufferSize(1D);
        component.changeBuffer(1D);
        final Object[] transferred = invokeComponent(helper, component, "transferItem", Direction.WEST.get3DDataValue(), Direction.EAST.get3DDataValue(), 1);
        helper.assertTrue(Boolean.TRUE.equals(transferred[0]), "Transposer did not transfer item with energy");
        helper.assertTrue(source.getItem(0).isEmpty(), "Transposer did not remove source item");
        helper.assertTrue(sink.getItem(0).is(Items.DIAMOND), "Transposer did not insert sink item");
        helper.assertTrue(Double.compare(0D, component.localBuffer()) == 0, "Transposer item transfer did not consume energy");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void transposerTransferFluidRequiresEnergy(final GameTestHelper helper) {
        final BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.TRANSPOSER.get());
        helper.setBlock(pos.relative(Direction.WEST), Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
        helper.setBlock(pos.relative(Direction.EAST), Blocks.CAULDRON);
        final TransposerBlockEntity transposer = helper.getBlockEntity(pos);
        final ComponentConnector component = (ComponentConnector) transposer.node();

        final Object[] result = invokeComponent(
            helper,
            component,
            "transferFluid",
            Direction.WEST.get3DDataValue(),
            Direction.EAST.get3DDataValue(),
            1000);

        assertNoEnergy(helper, result, "Transposer fluid transfer");
        helper.assertTrue(helper.getBlockState(pos.relative(Direction.WEST)).is(Blocks.WATER_CAULDRON), "Transposer drained fluid without energy");
        helper.assertTrue(helper.getBlockState(pos.relative(Direction.EAST)).is(Blocks.CAULDRON), "Transposer filled tank without energy");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void transposerItemTransferRequiresEnergy(final GameTestHelper helper) {
        final DriverItem driver = Driver.driverFor(new ItemStack(ModItems.TRANSPOSER.get()));
        helper.assertTrue(driver != null, "No driver for transposer item");

        final BlockPos hostPos = new BlockPos(2, 1, 2);
        final BlockPos sourcePos = hostPos.relative(Direction.WEST);
        final BlockPos sinkPos = hostPos.relative(Direction.EAST);
        helper.setBlock(sourcePos, Blocks.CHEST);
        helper.setBlock(sinkPos, Blocks.CHEST);
        final net.minecraft.world.Container source = helper.getBlockEntity(sourcePos);
        final net.minecraft.world.Container sink = helper.getBlockEntity(sinkPos);
        source.setItem(0, new ItemStack(Items.EMERALD));
        final ManagedEnvironment environment = driver.createEnvironment(new ItemStack(ModItems.TRANSPOSER.get()), new StaticPositionEnvironmentHost(helper, hostPos));
        helper.assertTrue(environment != null, "Transposer item did not create environment");
        helper.assertTrue(environment.node() instanceof ComponentConnector, "Transposer item node is not a connector");
        final ComponentConnector component = (ComponentConnector) environment.node();

        final Object[] noEnergy = invokeComponent(helper, component, "transferItem", Direction.WEST.get3DDataValue(), Direction.EAST.get3DDataValue(), 1);
        assertNoEnergy(helper, noEnergy, "Transposer item environment transfer");
        helper.assertTrue(source.getItem(0).is(Items.EMERALD), "Transposer item environment moved item without energy");
        helper.assertTrue(sink.getItem(0).isEmpty(), "Transposer item environment inserted item without energy");

        component.setLocalBufferSize(1D);
        component.changeBuffer(1D);
        final Object[] transferred = invokeComponent(helper, component, "transferItem", Direction.WEST.get3DDataValue(), Direction.EAST.get3DDataValue(), 1);
        helper.assertTrue(Boolean.TRUE.equals(transferred[0]), "Transposer item environment did not transfer item with energy");
        helper.assertTrue(source.getItem(0).isEmpty(), "Transposer item environment did not remove source item");
        helper.assertTrue(sink.getItem(0).is(Items.EMERALD), "Transposer item environment did not insert sink item");
        helper.assertTrue(Double.compare(0D, component.localBuffer()) == 0, "Transposer item environment transfer did not consume energy");
        helper.succeed();
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
    public static void componentItemsProvideEnvironmentClasses(final GameTestHelper helper) {
        assertEnvironmentProvider(helper, new ItemStack(ModItems.DATABASE_UPGRADE_TIER1.get()), li.cil.oc.common.component.DatabaseEnvironment.class);
        assertEnvironmentProvider(helper, new ItemStack(ModItems.DATA_CARD_TIER2.get()), li.cil.oc.common.component.DataCardEnvironment.class);
        assertEnvironmentProvider(helper, new ItemStack(ModItems.EEPROM.get()), li.cil.oc.common.component.EepromEnvironment.class);
        assertEnvironmentProvider(helper, new ItemStack(ModItems.GRAPHICS_CARD_TIER3.get()), li.cil.oc.common.component.GraphicsCardEnvironment.class);
        assertEnvironmentProvider(helper, new ItemStack(ModItems.INTERNET_CARD.get()), li.cil.oc.common.component.InternetCardEnvironment.class);
        assertEnvironmentProvider(helper, new ItemStack(ModItems.NETWORK_CARD.get()), li.cil.oc.common.component.NetworkCardEnvironment.class);
        assertEnvironmentProvider(helper, new ItemStack(ModItems.WIRELESS_NETWORK_CARD_TIER2.get()), li.cil.oc.common.component.WirelessNetworkCardEnvironment.class);
        assertEnvironmentProvider(helper, new ItemStack(ModItems.REDSTONE_CARD.get()), li.cil.oc.common.component.RedstoneCardEnvironment.class);
        assertEnvironmentProvider(helper, new ItemStack(ModItems.NAVIGATION_UPGRADE.get()), li.cil.oc.common.component.NavigationUpgradeEnvironment.class);
        assertEnvironmentProvider(helper, new ItemStack(ModItems.ANGEL_UPGRADE.get()), li.cil.oc.common.component.AngelUpgradeEnvironment.class);
        assertEnvironmentProvider(helper, new ItemStack(ModItems.MOTION_SENSOR.get()), li.cil.oc.common.component.MotionSensorEnvironment.class);
        assertEnvironmentProvider(helper, new ItemStack(ModItems.TRANSPOSER.get()), li.cil.oc.common.component.TransposerEnvironment.class);
        assertEnvironmentProvider(helper, new ItemStack(ModItems.SCREEN_TIER2.get()), li.cil.oc.common.blockentity.ScreenItemEnvironment.class);
        assertEnvironmentProvider(helper, new ItemStack(ModItems.KEYBOARD.get()), li.cil.oc.common.component.KeyboardItemEnvironment.class);
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
    public static void geolyzerAddsAnalyzeDataToTabletUse(final GameTestHelper helper) {
        final BlockPos pos = new BlockPos(1, 1, 1);
        final BlockPos targetPos = pos.relative(Direction.WEST);
        helper.setBlock(pos, ModBlocks.GEOLYZER.get());
        helper.setBlock(targetPos, Blocks.STONE);
        final GeolyzerBlockEntity geolyzer = helper.getBlockEntity(pos);
        final ComponentConnector component = (ComponentConnector) geolyzer.node();
        component.setLocalBufferSize(10D);
        component.changeBuffer(10D);

        final CompoundTag tabletData = new CompoundTag();
        geolyzer.onMessage(new TestMessage(null, "tablet.use", new Object[]{
            tabletData,
            new ItemStack(ModItems.TABLET.get()),
            null,
            helper.absolutePos(targetPos),
            Direction.WEST,
            Float.valueOf(0.5F),
            Float.valueOf(0.5F),
            Float.valueOf(0.5F)
        }));

        helper.assertTrue("minecraft:stone".equals(tabletData.getString("name")), "Geolyzer tablet analysis did not collect block name");
        helper.assertTrue(tabletData.contains("hardness"), "Geolyzer tablet analysis did not collect block hardness");
        helper.assertTrue(Double.compare(0D, component.localBuffer()) == 0, "Geolyzer tablet analysis did not consume energy");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void hologramConsumesEnergyForLitVoxels(final GameTestHelper helper) {
        final BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.HOLOGRAM_TIER1.get());
        final HologramBlockEntity hologram = helper.getBlockEntity(pos);
        helper.assertTrue(hologram.node() instanceof ComponentConnector, "Hologram node is not a component connector");
        final ComponentConnector connector = (ComponentConnector) hologram.node();
        connector.setLocalBufferSize(1D);
        connector.changeBuffer(1D);
        invokeComponent(helper, connector, "set", 1, 1, 1, 1);

        helper.succeedWhen(() -> helper.assertTrue(connector.localBuffer() < 1D, "Hologram did not consume energy"));
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
    public static void geolyzerItemAnalyzesRotatedHostSide(final GameTestHelper helper) {
        final DriverItem driver = Driver.driverFor(new ItemStack(ModItems.GEOLYZER.get()));
        helper.assertTrue(driver != null, "No driver for geolyzer upgrade");

        final BlockPos hostPos = new BlockPos(2, 1, 2);
        helper.setBlock(hostPos.relative(Direction.NORTH), Blocks.STONE);
        helper.setBlock(hostPos.relative(Direction.EAST), Blocks.DIAMOND_BLOCK);
        final ManagedEnvironment environment = driver.createEnvironment(
            new ItemStack(ModItems.GEOLYZER.get()),
            new RotatedPositionEnvironmentHost(helper, hostPos, Direction.EAST)
        );
        helper.assertTrue(environment != null, "Geolyzer upgrade did not create environment");
        helper.assertTrue(environment.node() instanceof ComponentConnector, "Geolyzer upgrade node is not a connector");
        final ComponentConnector component = (ComponentConnector) environment.node();
        component.setLocalBufferSize(10D);
        component.changeBuffer(10D);

        final Object[] analyze = invokeComponent(helper, component, "analyze", Direction.NORTH.get3DDataValue());

        helper.assertTrue(analyze.length == 1 && analyze[0] instanceof Map<?, ?>, "Geolyzer analyze did not return block data");
        final Map<?, ?> data = (Map<?, ?>) analyze[0];
        helper.assertTrue("minecraft:diamond_block".equals(data.get("name")), "Geolyzer analyze did not use rotated host side");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void tankControllerInspectsAdjacentFluidTanks(final GameTestHelper helper) {
        final BlockPos pos = new BlockPos(1, 1, 1);
        final int side = Direction.WEST.get3DDataValue();
        helper.setBlock(pos, ModBlocks.ADAPTER.get());
        helper.setBlock(pos.relative(Direction.WEST), Blocks.WATER_CAULDRON.defaultBlockState().setValue(LayeredCauldronBlock.LEVEL, 3));
        final DriverItem driver = Driver.driverFor(new ItemStack(ModItems.TANK_CONTROLLER_UPGRADE.get()), AdapterBlockEntity.class);
        final AdapterBlockEntity adapter = helper.getBlockEntity(pos);
        final ManagedEnvironment environment = driver.createEnvironment(new ItemStack(ModItems.TANK_CONTROLLER_UPGRADE.get()), adapter);
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
    public static void tankControllerUpgradeCreatesRobotEnvironment(final GameTestHelper helper) {
        final ItemStack stack = new ItemStack(ModItems.TANK_CONTROLLER_UPGRADE.get());
        final DriverItem driver = Driver.driverFor(stack, RobotTestHost.class);
        helper.assertTrue(driver != null, "No tank controller driver for robot host");

        final ManagedEnvironment environment = driver.createEnvironment(stack, new RobotTestHost(helper));
        helper.assertTrue(environment != null, "Tank controller did not create environment for robot host");
        helper.assertTrue(environment.node() instanceof li.cil.oc.api.network.Component, "Tank controller node is not a component");
        final li.cil.oc.api.network.Component component = (li.cil.oc.api.network.Component) environment.node();
        helper.assertTrue("tank_controller".equals(component.name()), "Tank controller component name mismatch");
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
    public static void assemblerMenuReportsAssemblyState(final GameTestHelper helper) {
        try (AssemblerTemplates.Registration ignored = AssemblerTemplates.register(new AssemblerTemplate() {
            @Override
            public String name() {
                return "menu_status_test";
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
                return 4D;
            }
        })) {
            final BlockPos pos = new BlockPos(1, 1, 1);
            helper.setBlock(pos, ModBlocks.ASSEMBLER.get());
            final AssemblerBlockEntity assembler = helper.getBlockEntity(pos);

            helper.assertTrue(AssemblerMenu.stateFor(assembler) == AssemblerMenu.STATE_IDLE, "Empty assembler did not report idle state");
            helper.assertTrue(AssemblerMenu.progressFor(assembler) == 0, "Idle assembler reported progress");

            assembler.setItem(AssemblerBlockEntity.SLOT_TEMPLATE, new ItemStack(Items.DIAMOND));
            helper.assertTrue(AssemblerMenu.stateFor(assembler) == AssemblerMenu.STATE_READY, "Ready assembler did not report ready state");

            helper.assertTrue(assembler.start(false), "Assembler did not start menu status recipe");
            helper.assertTrue(AssemblerMenu.stateFor(assembler) == AssemblerMenu.STATE_BUSY, "Started assembler did not report busy state");

            final ComponentConnector connector = (ComponentConnector) assembler.node();
            connector.changeBuffer(2D);
            AssemblerBlockEntity.serverTick(helper.getLevel(), helper.absolutePos(pos), helper.getBlockState(pos), assembler);

            final int progress = AssemblerMenu.progressFor(assembler);
            helper.assertTrue(progress > 0 && progress < 100, "Busy assembler did not report partial progress");
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
    public static void disassemblerMenuReportsInputState(final GameTestHelper helper) {
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

        helper.assertTrue(DisassemblerMenu.stateFor(disassembler) == DisassemblerMenu.STATE_EMPTY, "Empty disassembler did not report empty state");

        disassembler.setItem(DisassemblerBlockEntity.SLOT_INPUT, tablet);
        helper.assertTrue(DisassemblerMenu.stateFor(disassembler) == DisassemblerMenu.STATE_READY, "Ready disassembler did not report ready state");
        helper.assertTrue(!disassembler.canPlaceItem(DisassemblerBlockEntity.SLOT_OUTPUT_START, tablet), "Disassembler output slot accepted player input");

        disassembler.setItem(DisassemblerBlockEntity.SLOT_OUTPUT_START, new ItemStack(Items.STONE));
        helper.assertTrue(DisassemblerMenu.stateFor(disassembler) == DisassemblerMenu.STATE_BLOCKED, "Blocked disassembler did not report blocked state");
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
        assertProcessorComponents(helper, new ItemStack(ModItems.APU_TIER1.get()), 12);
        assertProcessorComponents(helper, new ItemStack(ModItems.APU_TIER2.get()), 16);
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
        assertScreenItemDriver(helper, new ItemStack(ModItems.SCREEN_TIER1.get()), 0, 50, 16, TextBuffer.ColorDepth.OneBit);
        assertScreenItemDriver(helper, new ItemStack(ModItems.SCREEN_TIER2.get()), 1, 80, 25, TextBuffer.ColorDepth.FourBit);
        assertScreenItemDriver(helper, new ItemStack(ModItems.SCREEN_TIER3.get()), 2, 160, 50, TextBuffer.ColorDepth.EightBit);
        assertKeyboardItemDriver(helper, new ItemStack(ModItems.KEYBOARD.get()));
        assertMotionSensorItemDriver(helper, new ItemStack(ModItems.MOTION_SENSOR.get()));
        assertGeolyzerItemDriver(helper, new ItemStack(ModItems.GEOLYZER.get()));
        assertTransposerItemDriver(helper, new ItemStack(ModItems.TRANSPOSER.get()));
        assertRackMountableDriver(helper, new ItemStack(ModItems.SERVER_TIER1.get()), 0);
        assertRackMountableDriver(helper, new ItemStack(ModItems.SERVER_TIER3.get()), 2);
        assertRackMountableDriver(helper, new ItemStack(ModItems.TERMINAL_SERVER.get()), 0);
        assertComponentBusDriver(helper, new ItemStack(ModItems.COMPONENT_BUS_TIER1.get()), 0, 8);
        assertComponentBusDriver(helper, new ItemStack(ModItems.COMPONENT_BUS_TIER2.get()), 1, 12);
        assertComponentBusDriver(helper, new ItemStack(ModItems.COMPONENT_BUS_TIER3.get()), 2, 16);
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
    public static void generatorUpgradeDropsQueuedFuelOnDisconnect(final GameTestHelper helper) throws Exception {
        helper.killAllEntities();
        final DriverItem driver = Driver.driverFor(new ItemStack(ModItems.GENERATOR_UPGRADE.get()));
        helper.assertTrue(driver != null, "No driver for generator upgrade");

        final AgentTestHost host = new AgentTestHost(helper);
        host.mainInventory().setItem(0, new ItemStack(Items.COAL, 1));
        final ManagedEnvironment environment = driver.createEnvironment(new ItemStack(ModItems.GENERATOR_UPGRADE.get()), host);
        helper.assertTrue(environment != null, "Generator upgrade did not create generator environment");
        helper.assertTrue(environment.node() instanceof ComponentConnector, "Generator node is not a component connector");

        final ComponentConnector connector = (ComponentConnector) environment.node();
        final Object[] insert = connector.invoke("insert", null, 1);
        helper.assertTrue(Boolean.TRUE.equals(insert[0]) && Integer.valueOf(1).equals(insert[1]), "Generator did not queue fuel");

        environment.onDisconnect(environment.node());

        helper.succeedWhen(() -> helper.assertTrue(droppedItemCount(helper, Items.COAL, BlockPos.ZERO, 2D) == 1, "Generator did not drop queued fuel on disconnect"));
    }

    @GameTest(template = "empty")
    public static void generatorUpgradeReturnsFuelContainersOnInsert(final GameTestHelper helper) throws Exception {
        final DriverItem driver = Driver.driverFor(new ItemStack(ModItems.GENERATOR_UPGRADE.get()));
        helper.assertTrue(driver != null, "No driver for generator upgrade");

        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        final AgentTestHost host = new AgentTestHost(helper, player);
        host.mainInventory().setItem(0, new ItemStack(Items.LAVA_BUCKET, 1));
        final ManagedEnvironment environment = driver.createEnvironment(new ItemStack(ModItems.GENERATOR_UPGRADE.get()), host);
        helper.assertTrue(environment != null, "Generator upgrade did not create generator environment");
        helper.assertTrue(environment.node() instanceof ComponentConnector, "Generator node is not a component connector");

        final ComponentConnector connector = (ComponentConnector) environment.node();
        final Object[] insert = connector.invoke("insert", null, 1);

        helper.assertTrue(Boolean.TRUE.equals(insert[0]) && Integer.valueOf(1).equals(insert[1]), "Generator did not queue lava bucket fuel");
        helper.assertTrue(host.mainInventory().getItem(0).isEmpty(), "Generator did not consume lava bucket fuel");
        helper.assertTrue(containsStack(player.getInventory(), Items.BUCKET, 1), "Generator did not return fuel container");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void generatorUpgradeConsumesFuelContainerOnRemove(final GameTestHelper helper) throws Exception {
        final DriverItem driver = Driver.driverFor(new ItemStack(ModItems.GENERATOR_UPGRADE.get()));
        helper.assertTrue(driver != null, "No driver for generator upgrade");

        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        final AgentTestHost host = new AgentTestHost(helper, player);
        host.mainInventory().setItem(0, new ItemStack(Items.LAVA_BUCKET, 1));
        final ManagedEnvironment environment = driver.createEnvironment(new ItemStack(ModItems.GENERATOR_UPGRADE.get()), host);
        helper.assertTrue(environment != null, "Generator upgrade did not create generator environment");
        helper.assertTrue(environment.node() instanceof ComponentConnector, "Generator node is not a component connector");

        final ComponentConnector connector = (ComponentConnector) environment.node();
        final Object[] insert = connector.invoke("insert", null, 1);
        helper.assertTrue(Boolean.TRUE.equals(insert[0]) && Integer.valueOf(1).equals(insert[1]), "Generator did not queue lava bucket fuel");

        host.mainInventory().setItem(0, new ItemStack(Items.BUCKET, 1));
        final Object[] remove = connector.invoke("remove", null, 1);

        helper.assertTrue(Boolean.TRUE.equals(remove[0]) && Integer.valueOf(1).equals(remove[1]), "Generator did not remove lava bucket fuel");
        helper.assertTrue(host.mainInventory().getItem(0).isEmpty(), "Generator did not consume required fuel container");
        helper.assertTrue(containsStack(player.getInventory(), Items.LAVA_BUCKET, 1), "Generator did not return removed fuel");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void craftingUpgradeRejectsNonRobotAgentHost(final GameTestHelper helper) {
        final DriverItem driver = Driver.driverFor(new ItemStack(ModItems.CRAFTING_UPGRADE.get()));
        helper.assertTrue(driver != null, "No driver for crafting upgrade");

        final ManagedEnvironment environment = driver.createEnvironment(new ItemStack(ModItems.CRAFTING_UPGRADE.get()), new AgentTestHost(helper));
        helper.assertTrue(environment == null, "Crafting upgrade created environment for non-robot agent host");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void craftingUpgradeCraftsTopLeftInventoryGrid(final GameTestHelper helper) throws Exception {
        final DriverItem driver = Driver.driverFor(new ItemStack(ModItems.CRAFTING_UPGRADE.get()));
        helper.assertTrue(driver != null, "No driver for crafting upgrade");

        final RobotTestHost host = new RobotTestHost(helper);
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
    public static void angelUpgradeCreatesPassiveDeviceInfoEnvironment(final GameTestHelper helper) {
        final DriverItem driver = Driver.driverFor(new ItemStack(ModItems.ANGEL_UPGRADE.get()));
        helper.assertTrue(driver != null, "No driver for angel upgrade");

        final ManagedEnvironment environment = driver.createEnvironment(new ItemStack(ModItems.ANGEL_UPGRADE.get()), new AgentTestHost(helper));
        helper.assertTrue(environment != null, "Angel upgrade did not create environment");
        helper.assertTrue(environment.node() != null, "Angel upgrade environment has no node");
        helper.assertTrue(environment instanceof DeviceInfo, "Angel upgrade environment lacks device info");
        final Map<String, String> info = ((DeviceInfo) environment).getDeviceInfo();
        helper.assertTrue(DeviceInfo.DeviceClass.Generic.equals(info.get(DeviceInfo.DeviceAttribute.Class)), "Angel upgrade device class mismatch");
        helper.assertTrue("Angel upgrade".equals(info.get(DeviceInfo.DeviceAttribute.Description)), "Angel upgrade description mismatch");
        helper.assertTrue("FreePlacer (TM)".equals(info.get(DeviceInfo.DeviceAttribute.Product)), "Angel upgrade product mismatch");
        helper.assertTrue("8192".equals(info.get(DeviceInfo.DeviceAttribute.Capacity)), "Angel upgrade capacity mismatch");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void chunkloaderUpgradeCreatesManagedComponent(final GameTestHelper helper) throws Exception {
        final ItemStack stack = new ItemStack(ModItems.CHUNKLOADER_UPGRADE.get());
        final DriverItem driver = Driver.driverFor(stack);
        helper.assertTrue(driver != null, "No driver for chunkloader upgrade");
        helper.assertTrue(driver.tier(stack) == 2, "Chunkloader tier mismatch");

        final ManagedEnvironment environment = driver.createEnvironment(stack, new AgentTestHost(helper));
        helper.assertTrue(environment != null, "Chunkloader upgrade did not create environment");
        helper.assertTrue(environment.node() instanceof ComponentConnector, "Chunkloader node is not a component connector");
        final ComponentConnector component = (ComponentConnector) environment.node();
        helper.assertTrue("chunkloader".equals(component.name()), "Chunkloader component name mismatch");
        helper.assertTrue(environment instanceof DeviceInfo, "Chunkloader environment lacks device info");
        final Map<String, String> info = ((DeviceInfo) environment).getDeviceInfo();
        helper.assertTrue(DeviceInfo.DeviceClass.Generic.equals(info.get(DeviceInfo.DeviceAttribute.Class)), "Chunkloader device class mismatch");
        helper.assertTrue("World stabilizer".equals(info.get(DeviceInfo.DeviceAttribute.Description)), "Chunkloader description mismatch");
        helper.assertTrue("Realizer9001-CL".equals(info.get(DeviceInfo.DeviceAttribute.Product)), "Chunkloader product mismatch");
        helper.assertTrue(Driver.environmentFor(stack) != null, "Chunkloader has no environment provider");

        final Object[] inactive = component.invoke("isActive", null);
        helper.assertTrue(Boolean.FALSE.equals(inactive[0]), "Chunkloader should start inactive");
        final Object[] enabled = component.invoke("setActive", null, true);
        helper.assertTrue(Boolean.TRUE.equals(enabled[0]), "Chunkloader did not report activation change");
        final Object[] active = component.invoke("isActive", null);
        helper.assertTrue(Boolean.TRUE.equals(active[0]), "Chunkloader should be active after setActive(true)");
        environment.onMessage(new TestMessage(component, "computer.stopped", new Object[0]));
        final Object[] stopped = component.invoke("isActive", null);
        helper.assertTrue(Boolean.FALSE.equals(stopped[0]), "Chunkloader did not deactivate on computer.stopped");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void chunkloaderUpgradeForcesHostChunkWhenActive(final GameTestHelper helper) throws Exception {
        final ItemStack stack = new ItemStack(ModItems.CHUNKLOADER_UPGRADE.get());
        final DriverItem driver = Driver.driverFor(stack);
        helper.assertTrue(driver != null, "No driver for chunkloader upgrade");

        final AgentTestHost host = new AgentTestHost(helper);
        final ChunkPos ownerChunk = new ChunkPos(BlockPos.containing(host.xPosition(), host.yPosition(), host.zPosition()));
        final ManagedEnvironment environment = driver.createEnvironment(stack, host);
        helper.assertTrue(environment != null, "Chunkloader upgrade did not create environment");
        helper.assertTrue(environment.node() instanceof ComponentConnector, "Chunkloader node is not a component connector");
        final ComponentConnector component = (ComponentConnector) environment.node();

        component.invoke("setActive", null, true);
        helper.assertTrue(hasModForcedTickingChunk(helper, ownerChunk.toLong()), "Chunkloader did not add forced chunk ticket");

        component.invoke("setActive", null, false);
        helper.assertFalse(hasModForcedTickingChunk(helper, ownerChunk.toLong()), "Chunkloader did not remove forced chunk ticket");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void tractorBeamUpgradeRejectsNonRobotAgentHost(final GameTestHelper helper) {
        final DriverItem driver = Driver.driverFor(new ItemStack(ModItems.TRACTOR_BEAM_UPGRADE.get()));
        helper.assertTrue(driver != null, "No driver for tractor beam upgrade");

        final ManagedEnvironment environment = driver.createEnvironment(new ItemStack(ModItems.TRACTOR_BEAM_UPGRADE.get()), new AgentTestHost(helper));
        helper.assertTrue(environment == null, "Tractor beam upgrade created environment for non-robot agent host");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void tractorBeamUpgradeSucksNearbyItemStack(final GameTestHelper helper) throws Exception {
        final DriverItem driver = Driver.driverFor(new ItemStack(ModItems.TRACTOR_BEAM_UPGRADE.get()));
        helper.assertTrue(driver != null, "No driver for tractor beam upgrade");

        final RobotTestHost host = new RobotTestHost(helper);
        host.setSelectedSlot(3);
        final ItemEntity drop = new ItemEntity(
            helper.getLevel(),
            host.xPosition() + 0.5D,
            host.yPosition() + 0.5D,
            host.zPosition() + 0.5D,
            new ItemStack(Items.DIAMOND, 2));
        helper.getLevel().addFreshEntity(drop);

        final ManagedEnvironment environment = driver.createEnvironment(new ItemStack(ModItems.TRACTOR_BEAM_UPGRADE.get()), host);
        helper.assertTrue(environment != null, "Tractor beam upgrade did not create tractor beam environment");
        helper.assertTrue(environment.node() instanceof li.cil.oc.api.network.Component, "Tractor beam node is not a component");
        final li.cil.oc.api.network.Component component = (li.cil.oc.api.network.Component) environment.node();
        helper.assertTrue("tractor_beam".equals(component.name()), "Tractor beam component name mismatch");

        final Object[] suck = component.invoke("suck", null);
        helper.assertTrue(Boolean.TRUE.equals(suck[0]), "Tractor beam did not suck nearby item");
        helper.assertTrue(host.mainInventory().getItem(3).is(Items.DIAMOND) && host.mainInventory().getItem(3).getCount() == 2, "Tractor beam did not insert item stack into selected slot");
        helper.assertTrue(drop.isRemoved() || drop.getItem().isEmpty(), "Tractor beam left sucked item in world");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void tractorBeamUpgradeSucksNearbyItemStackIntoTabletPlayerInventory(final GameTestHelper helper) throws Exception {
        final DriverItem driver = Driver.driverFor(new ItemStack(ModItems.TRACTOR_BEAM_UPGRADE.get()));
        helper.assertTrue(driver != null, "No driver for tractor beam upgrade");

        final Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.moveTo(helper.absolutePos(BlockPos.ZERO).getX() + 0.5D, helper.absolutePos(BlockPos.ZERO).getY() + 1D, helper.absolutePos(BlockPos.ZERO).getZ() + 0.5D);
        final TabletTestHost host = new TabletTestHost(helper, player);
        final ItemEntity drop = new ItemEntity(
            helper.getLevel(),
            host.xPosition(),
            host.yPosition(),
            host.zPosition(),
            new ItemStack(Items.DIAMOND, 2));
        helper.getLevel().addFreshEntity(drop);

        final ManagedEnvironment environment = driver.createEnvironment(new ItemStack(ModItems.TRACTOR_BEAM_UPGRADE.get()), host);
        helper.assertTrue(environment != null, "Tractor beam upgrade did not create tablet environment");
        helper.assertTrue(environment.node() instanceof li.cil.oc.api.network.Component, "Tractor beam node is not a component");
        final li.cil.oc.api.network.Component component = (li.cil.oc.api.network.Component) environment.node();

        final Object[] suck = component.invoke("suck", null);
        helper.assertTrue(Boolean.TRUE.equals(suck[0]), "Tractor beam did not suck nearby item for tablet");
        helper.assertTrue(containsStack(player.getInventory(), Items.DIAMOND, 2), "Tractor beam did not insert item stack into tablet player inventory");
        helper.assertTrue(drop.isRemoved() || drop.getItem().isEmpty(), "Tractor beam left tablet-sucked item in world");
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
    public static void signUpgradeReadsAndWritesHostSign(final GameTestHelper helper) throws Exception {
        final DriverItem driver = Driver.driverFor(new ItemStack(ModItems.SIGN_UPGRADE.get()));
        helper.assertTrue(driver != null, "No driver for sign upgrade");

        final BlockPos hostPos = new BlockPos(1, 1, 1);
        helper.setBlock(hostPos, Blocks.OAK_SIGN);
        final ManagedEnvironment environment = driver.createEnvironment(
            new ItemStack(ModItems.SIGN_UPGRADE.get()),
            new StaticRotatablePositionEnvironmentHost(helper, hostPos, Direction.NORTH)
        );
        helper.assertTrue(environment != null, "Sign upgrade did not create sign environment");
        helper.assertTrue(environment.node() instanceof li.cil.oc.api.network.Component, "Sign node is not a component");
        final li.cil.oc.api.network.Component component = (li.cil.oc.api.network.Component) environment.node();
        helper.assertTrue("sign".equals(component.name()), "Sign component name mismatch");

        final Object[] set = component.invoke("setValue", null, "alpha\nbeta");
        helper.assertTrue(set.length == 1 && "alpha\nbeta\n\n".equals(set[0]), "Sign upgrade did not write normalized sign text");
        final Object[] get = component.invoke("getValue", null);
        helper.assertTrue(get.length == 1 && "alpha\nbeta\n\n".equals(get[0]), "Sign upgrade did not read sign text");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void pistonUpgradePushesBlockInFrontOfHost(final GameTestHelper helper) throws Exception {
        final DriverItem driver = Driver.driverFor(new ItemStack(ModItems.PISTON_UPGRADE.get()));
        helper.assertTrue(driver != null, "No driver for piston upgrade");

        final BlockPos hostPos = new BlockPos(2, 1, 2);
        final BlockPos sourcePos = hostPos.relative(Direction.EAST);
        final BlockPos targetPos = sourcePos.relative(Direction.EAST);
        helper.setBlock(sourcePos, Blocks.DIRT);
        helper.setBlock(targetPos, Blocks.AIR);
        final ManagedEnvironment environment = driver.createEnvironment(
            new ItemStack(ModItems.PISTON_UPGRADE.get()),
            new StaticRotatablePositionEnvironmentHost(helper, hostPos, Direction.EAST)
        );
        helper.assertTrue(environment != null, "Piston upgrade did not create piston environment");
        helper.assertTrue(environment.node() instanceof li.cil.oc.api.network.Component, "Piston node is not a component");
        final li.cil.oc.api.network.Component component = (li.cil.oc.api.network.Component) environment.node();
        helper.assertTrue("piston".equals(component.name()), "Piston component name mismatch");

        final Object[] sticky = component.invoke("isSticky", null);
        helper.assertTrue(sticky.length == 1 && Boolean.FALSE.equals(sticky[0]), "Piston upgrade reported sticky");
        final Object[] push = component.invoke("push", null);
        helper.assertTrue(push.length == 1 && Boolean.TRUE.equals(push[0]), "Piston upgrade did not push block: " + java.util.Arrays.toString(push));
        helper.assertTrue(helper.getBlockState(sourcePos).isAir(), "Piston upgrade did not clear source block");
        helper.assertTrue(helper.getBlockState(targetPos).is(Blocks.DIRT), "Piston upgrade did not move block forward");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void pistonUpgradePushesBlockChain(final GameTestHelper helper) throws Exception {
        final DriverItem driver = Driver.driverFor(new ItemStack(ModItems.PISTON_UPGRADE.get()));
        helper.assertTrue(driver != null, "No driver for piston upgrade");

        final BlockPos hostPos = new BlockPos(1, 1, 1);
        final BlockPos firstPos = hostPos.relative(Direction.EAST);
        final BlockPos secondPos = firstPos.relative(Direction.EAST);
        final BlockPos targetPos = secondPos.relative(Direction.EAST);
        helper.setBlock(firstPos, Blocks.DIRT);
        helper.setBlock(secondPos, Blocks.STONE);
        helper.setBlock(targetPos, Blocks.AIR);
        final ManagedEnvironment environment = driver.createEnvironment(
            new ItemStack(ModItems.PISTON_UPGRADE.get()),
            new StaticRotatablePositionEnvironmentHost(helper, hostPos, Direction.EAST)
        );
        helper.assertTrue(environment != null, "Piston upgrade did not create piston environment");
        final li.cil.oc.api.network.Component component = (li.cil.oc.api.network.Component) environment.node();

        final Object[] push = component.invoke("push", null);
        helper.assertTrue(push.length == 1 && Boolean.TRUE.equals(push[0]), "Piston upgrade did not push block chain: " + java.util.Arrays.toString(push));
        helper.assertTrue(helper.getBlockState(firstPos).isAir(), "Piston upgrade did not clear first chain block");
        helper.assertTrue(helper.getBlockState(secondPos).is(Blocks.DIRT), "Piston upgrade did not move first block into second position");
        helper.assertTrue(helper.getBlockState(targetPos).is(Blocks.STONE), "Piston upgrade did not move second block into target position");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void stickyPistonUpgradePullsBlockTowardHost(final GameTestHelper helper) throws Exception {
        final DriverItem driver = Driver.driverFor(new ItemStack(ModItems.STICKY_PISTON_UPGRADE.get()));
        helper.assertTrue(driver != null, "No driver for sticky piston upgrade");

        final BlockPos hostPos = new BlockPos(2, 1, 2);
        final BlockPos targetPos = hostPos.relative(Direction.EAST);
        final BlockPos sourcePos = targetPos.relative(Direction.EAST);
        helper.setBlock(targetPos, Blocks.AIR);
        helper.setBlock(sourcePos, Blocks.DIRT);
        final ManagedEnvironment environment = driver.createEnvironment(
            new ItemStack(ModItems.STICKY_PISTON_UPGRADE.get()),
            new StaticRotatablePositionEnvironmentHost(helper, hostPos, Direction.EAST)
        );
        helper.assertTrue(environment != null, "Sticky piston upgrade did not create piston environment");
        helper.assertTrue(environment.node() instanceof li.cil.oc.api.network.Component, "Sticky piston node is not a component");
        final li.cil.oc.api.network.Component component = (li.cil.oc.api.network.Component) environment.node();
        helper.assertTrue("piston".equals(component.name()), "Sticky piston component name mismatch");

        final Object[] sticky = component.invoke("isSticky", null);
        helper.assertTrue(sticky.length == 1 && Boolean.TRUE.equals(sticky[0]), "Sticky piston upgrade did not report sticky");
        final Object[] pull = component.invoke("pull", null);
        helper.assertTrue(pull.length == 1 && Boolean.TRUE.equals(pull[0]), "Sticky piston upgrade did not pull block: " + java.util.Arrays.toString(pull));
        helper.assertTrue(helper.getBlockState(sourcePos).isAir(), "Sticky piston upgrade did not clear source block");
        helper.assertTrue(helper.getBlockState(targetPos).is(Blocks.DIRT), "Sticky piston upgrade did not move block toward host");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void tradingUpgradeListsNearbyVillagerTrades(final GameTestHelper helper) throws Exception {
        final DriverItem driver = Driver.driverFor(new ItemStack(ModItems.TRADING_UPGRADE.get()));
        helper.assertTrue(driver != null, "No driver for trading upgrade");

        final BlockPos hostPos = new BlockPos(2, 1, 2);
        final BlockPos villagerPos = helper.absolutePos(hostPos.relative(Direction.EAST));
        final Villager villager = EntityType.VILLAGER.create(helper.getLevel());
        helper.assertTrue(villager != null, "Villager did not spawn");
        final MerchantOffers offers = new MerchantOffers();
        offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 1), new ItemStack(Items.BREAD, 3), 4, 1, 0.05F));
        villager.setOffers(offers);
        villager.setNoAi(true);
        villager.moveTo(villagerPos.getX() + 0.5D, villagerPos.getY(), villagerPos.getZ() + 0.5D, 0, 0);
        helper.getLevel().addFreshEntity(villager);

        final ManagedEnvironment environment = driver.createEnvironment(
            new ItemStack(ModItems.TRADING_UPGRADE.get()),
            new StaticPositionEnvironmentHost(helper, hostPos)
        );
        helper.assertTrue(environment != null, "Trading upgrade did not create trading environment");
        helper.assertTrue(environment.node() instanceof li.cil.oc.api.network.Component, "Trading node is not a component");
        final li.cil.oc.api.network.Component component = (li.cil.oc.api.network.Component) environment.node();
        helper.assertTrue("trading".equals(component.name()), "Trading component name mismatch");

        final Object[] result = component.invoke("getTrades", null);
        helper.assertTrue(result.length == 1 && result[0] instanceof java.util.List<?> trades && trades.size() == 1, "Trading upgrade did not list nearby trade");
        final Object trade = ((java.util.List<?>) result[0]).getFirst();
        final Method getMerchantId = trade.getClass().getMethod("getMerchantId", Context.class, Arguments.class);
        final Object[] merchantId = (Object[]) getMerchantId.invoke(trade, null, null);
        helper.assertTrue(merchantId.length == 1 && Integer.valueOf(1).equals(merchantId[0]), "Trade merchant id mismatch");
        final Method getInput = trade.getClass().getMethod("getInput", Context.class, Arguments.class);
        final Object[] input = (Object[]) getInput.invoke(trade, null, null);
        helper.assertTrue(input.length == 2 && input[0] instanceof ItemStack firstInput && firstInput.is(Items.EMERALD) && firstInput.getCount() == 1, "Trade input mismatch");
        final Method getOutput = trade.getClass().getMethod("getOutput", Context.class, Arguments.class);
        final Object[] output = (Object[]) getOutput.invoke(trade, null, null);
        helper.assertTrue(output.length == 1 && output[0] instanceof ItemStack firstOutput && firstOutput.is(Items.BREAD) && firstOutput.getCount() == 3, "Trade output mismatch");
        final Method isEnabled = trade.getClass().getMethod("isEnabled", Context.class, Arguments.class);
        final Object[] enabled = (Object[]) isEnabled.invoke(trade, null, null);
        helper.assertTrue(enabled.length == 1 && Boolean.TRUE.equals(enabled[0]), "Trade should be enabled");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void tradingUpgradeExecutesVillagerTrade(final GameTestHelper helper) throws Exception {
        final DriverItem driver = Driver.driverFor(new ItemStack(ModItems.TRADING_UPGRADE.get()));
        helper.assertTrue(driver != null, "No driver for trading upgrade");

        final AgentTestHost host = new AgentTestHost(helper);
        host.mainInventory().setItem(0, new ItemStack(Items.EMERALD));
        final BlockPos villagerPos = helper.absolutePos(new BlockPos(1, 0, 0));
        final Villager villager = EntityType.VILLAGER.create(helper.getLevel());
        helper.assertTrue(villager != null, "Villager did not spawn");
        final MerchantOffers offers = new MerchantOffers();
        final MerchantOffer offer = new MerchantOffer(new ItemCost(Items.EMERALD, 1), new ItemStack(Items.BREAD, 3), 4, 1, 0.05F);
        offers.add(offer);
        villager.setOffers(offers);
        villager.setNoAi(true);
        villager.moveTo(villagerPos.getX() + 0.5D, villagerPos.getY(), villagerPos.getZ() + 0.5D, 0, 0);
        helper.getLevel().addFreshEntity(villager);

        final ManagedEnvironment environment = driver.createEnvironment(new ItemStack(ModItems.TRADING_UPGRADE.get()), host);
        helper.assertTrue(environment != null, "Trading upgrade did not create trading environment");
        final li.cil.oc.api.network.Component component = (li.cil.oc.api.network.Component) environment.node();
        final Object[] result = component.invoke("getTrades", null);
        final Object trade = ((java.util.List<?>) result[0]).getFirst();
        final Method tradeMethod = trade.getClass().getMethod("trade", Context.class, Arguments.class);
        final Object[] traded = (Object[]) tradeMethod.invoke(trade, null, null);

        helper.assertTrue(traded.length == 1 && Boolean.TRUE.equals(traded[0]), "Trade did not succeed");
        helper.assertTrue(!containsStack(host.mainInventory(), Items.EMERALD, 1), "Trade did not consume emerald");
        helper.assertTrue(containsStack(host.mainInventory(), Items.BREAD, 3), "Trade did not insert bread");
        helper.assertTrue(offer.getUses() == 1, "Trade did not notify merchant");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void tradingUpgradeAcceptsMatchingItemCostComponents(final GameTestHelper helper) throws Exception {
        final DriverItem driver = Driver.driverFor(new ItemStack(ModItems.TRADING_UPGRADE.get()));
        helper.assertTrue(driver != null, "No driver for trading upgrade");

        final AgentTestHost host = new AgentTestHost(helper);
        final ItemStack renamedEmerald = new ItemStack(Items.EMERALD);
        renamedEmerald.set(DataComponents.CUSTOM_NAME, Component.literal("trade coin"));
        host.mainInventory().setItem(0, renamedEmerald);
        final BlockPos villagerPos = helper.absolutePos(new BlockPos(1, 0, 0));
        final Villager villager = EntityType.VILLAGER.create(helper.getLevel());
        helper.assertTrue(villager != null, "Villager did not spawn");
        final MerchantOffers offers = new MerchantOffers();
        offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 1), new ItemStack(Items.BREAD, 3), 4, 1, 0.05F));
        villager.setOffers(offers);
        villager.setNoAi(true);
        villager.moveTo(villagerPos.getX() + 0.5D, villagerPos.getY(), villagerPos.getZ() + 0.5D, 0, 0);
        helper.getLevel().addFreshEntity(villager);

        final ManagedEnvironment environment = driver.createEnvironment(new ItemStack(ModItems.TRADING_UPGRADE.get()), host);
        helper.assertTrue(environment != null, "Trading upgrade did not create trading environment");
        final li.cil.oc.api.network.Component component = (li.cil.oc.api.network.Component) environment.node();
        final Object[] result = component.invoke("getTrades", null);
        final Object trade = ((java.util.List<?>) result[0]).getFirst();
        final Method tradeMethod = trade.getClass().getMethod("trade", Context.class, Arguments.class);
        final Object[] traded = (Object[]) tradeMethod.invoke(trade, null, null);

        helper.assertTrue(traded.length == 1 && Boolean.TRUE.equals(traded[0]), "Trade did not accept matching item cost with extra components");
        helper.assertTrue(!containsStack(host.mainInventory(), Items.EMERALD, 1), "Trade did not consume renamed emerald");
        helper.assertTrue(containsStack(host.mainInventory(), Items.BREAD, 3), "Trade did not insert bread");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void tradingUpgradeDoesNotDoubleCountTwoCosts(final GameTestHelper helper) throws Exception {
        final DriverItem driver = Driver.driverFor(new ItemStack(ModItems.TRADING_UPGRADE.get()));
        helper.assertTrue(driver != null, "No driver for trading upgrade");

        final AgentTestHost host = new AgentTestHost(helper);
        host.mainInventory().setItem(0, new ItemStack(Items.EMERALD));
        final BlockPos villagerPos = helper.absolutePos(new BlockPos(1, 0, 0));
        final Villager villager = EntityType.VILLAGER.create(helper.getLevel());
        helper.assertTrue(villager != null, "Villager did not spawn");
        final MerchantOffers offers = new MerchantOffers();
        offers.add(new MerchantOffer(
            new ItemCost(Items.EMERALD, 1),
            Optional.of(new ItemCost(Items.EMERALD, 1)),
            new ItemStack(Items.BREAD, 3),
            4,
            1,
            0.05F));
        villager.setOffers(offers);
        villager.setNoAi(true);
        villager.moveTo(villagerPos.getX() + 0.5D, villagerPos.getY(), villagerPos.getZ() + 0.5D, 0, 0);
        helper.getLevel().addFreshEntity(villager);

        final ManagedEnvironment environment = driver.createEnvironment(new ItemStack(ModItems.TRADING_UPGRADE.get()), host);
        helper.assertTrue(environment != null, "Trading upgrade did not create trading environment");
        final li.cil.oc.api.network.Component component = (li.cil.oc.api.network.Component) environment.node();
        final Object[] result = component.invoke("getTrades", null);
        final Object trade = ((java.util.List<?>) result[0]).getFirst();
        final Method tradeMethod = trade.getClass().getMethod("trade", Context.class, Arguments.class);
        final Object[] traded = (Object[]) tradeMethod.invoke(trade, null, null);

        helper.assertTrue(traded.length >= 2 && Boolean.FALSE.equals(traded[0]) && "not enough items to trade".equals(traded[1]), "Two-cost trade should reject one emerald");
        helper.assertTrue(containsStack(host.mainInventory(), Items.EMERALD, 1), "Rejected trade consumed emerald");
        helper.assertTrue(!containsStack(host.mainInventory(), Items.BREAD, 1), "Rejected trade inserted bread");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void tradingUpgradeTradeValueReloadsMerchantOffer(final GameTestHelper helper) throws Exception {
        final DriverItem driver = Driver.driverFor(new ItemStack(ModItems.TRADING_UPGRADE.get()));
        helper.assertTrue(driver != null, "No driver for trading upgrade");

        final BlockPos hostPos = new BlockPos(2, 1, 2);
        final BlockPos villagerPos = helper.absolutePos(hostPos.relative(Direction.EAST));
        final Villager villager = EntityType.VILLAGER.create(helper.getLevel());
        helper.assertTrue(villager != null, "Villager did not spawn");
        final MerchantOffers offers = new MerchantOffers();
        offers.add(new MerchantOffer(new ItemCost(Items.EMERALD, 1), new ItemStack(Items.BREAD, 3), 4, 1, 0.05F));
        villager.setOffers(offers);
        villager.setNoAi(true);
        villager.moveTo(villagerPos.getX() + 0.5D, villagerPos.getY(), villagerPos.getZ() + 0.5D, 0, 0);
        helper.getLevel().addFreshEntity(villager);

        final StaticPositionEnvironmentHost host = new StaticPositionEnvironmentHost(helper, hostPos);
        final ManagedEnvironment environment = driver.createEnvironment(new ItemStack(ModItems.TRADING_UPGRADE.get()), host);
        final li.cil.oc.api.network.Component component = (li.cil.oc.api.network.Component) environment.node();
        final Object trade = ((java.util.List<?>) component.invoke("getTrades", null)[0]).getFirst();
        final CompoundTag saved = new CompoundTag();
        ((li.cil.oc.api.machine.Value) trade).save(saved);

        final Object loaded = trade.getClass()
            .getConstructor(li.cil.oc.api.network.EnvironmentHost.class)
            .newInstance(host);
        ((li.cil.oc.api.machine.Value) loaded).load(saved);

        final Method getMerchantId = loaded.getClass().getMethod("getMerchantId", Context.class, Arguments.class);
        final Object[] merchantId = (Object[]) getMerchantId.invoke(loaded, null, null);
        helper.assertTrue(merchantId.length == 1 && Integer.valueOf(1).equals(merchantId[0]), "Reloaded trade merchant id mismatch");
        final Method getOutput = loaded.getClass().getMethod("getOutput", Context.class, Arguments.class);
        final Object[] output = (Object[]) getOutput.invoke(loaded, null, null);
        helper.assertTrue(output.length == 1 && output[0] instanceof ItemStack stack && stack.is(Items.BREAD) && stack.getCount() == 3, "Reloaded trade output mismatch");
        final Method isEnabled = loaded.getClass().getMethod("isEnabled", Context.class, Arguments.class);
        final Object[] enabled = (Object[]) isEnabled.invoke(loaded, null, null);
        helper.assertTrue(enabled.length == 1 && Boolean.TRUE.equals(enabled[0]), "Reloaded trade should be enabled");
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
    public static void rackStoresOnlyRackMountableItems(final GameTestHelper helper) {
        final BlockPos rackPos = new BlockPos(1, 1, 1);
        helper.setBlock(rackPos, ModBlocks.RACK.get());
        final RackBlockEntity rack = helper.getBlockEntity(rackPos);

        helper.assertTrue(rack.getContainerSize() == RackBlockEntity.CONTAINER_SIZE, "Rack slot count mismatch");
        helper.assertTrue(rack.canPlaceItem(0, new ItemStack(ModItems.SERVER_TIER1.get())), "Rack rejected tier 1 server");
        helper.assertTrue(rack.canPlaceItem(1, new ItemStack(ModItems.SERVER_TIER3.get())), "Rack rejected tier 3 server");
        helper.assertTrue(rack.canPlaceItem(2, new ItemStack(ModItems.TERMINAL_SERVER.get())), "Rack rejected terminal server");
        helper.assertTrue(!rack.canPlaceItem(3, new ItemStack(ModItems.CPU_TIER1.get())), "Rack accepted CPU");
        helper.assertTrue(!rack.canPlaceItem(RackBlockEntity.CONTAINER_SIZE, new ItemStack(ModItems.SERVER_TIER1.get())), "Rack accepted invalid slot");

        final ItemStack server = new ItemStack(ModItems.SERVER_TIER2.get());
        rack.setItem(0, server.copy());
        helper.assertTrue(rack.getItem(0).is(ModItems.SERVER_TIER2.get()), "Rack did not store server");
        final li.cil.oc.api.component.RackMountable mountable = rack.getMountable(0);
        helper.assertTrue(mountable != null, "Rack did not create server mountable");
        helper.assertTrue(mountable.node() != null, "Rack mountable has no node");
        helper.assertTrue(rack.indexOfMountable(mountable) == 0, "Rack mountable index mismatch");
        helper.assertTrue(mountable instanceof li.cil.oc.api.internal.Server, "Rack server mountable is not an internal server");
        final li.cil.oc.api.internal.Server rackServer = (li.cil.oc.api.internal.Server) mountable;
        helper.assertTrue(rackServer.rack() == rack, "Rack server reported wrong rack");
        helper.assertTrue(rackServer.slot() == 0, "Rack server reported wrong slot");
        helper.assertTrue(rackServer.tier() == 1, "Rack server reported wrong tier");
        helper.assertTrue(rackServer.machine() != null, "Rack server did not create machine");
        helper.assertTrue(rackServer instanceof net.minecraft.world.Container, "Rack server did not expose component inventory");
        final net.minecraft.world.Container serverInventory = (net.minecraft.world.Container) rackServer;
        helper.assertTrue(serverInventory.getContainerSize() == 13, "Tier 2 server slot count mismatch");
        helper.assertTrue(serverInventory.canPlaceItem(0, new ItemStack(ModItems.NETWORK_CARD.get())), "Server rejected card slot item");
        helper.assertTrue(serverInventory.canPlaceItem(2, new ItemStack(ModItems.CPU_TIER3.get())), "Server rejected tier 3 CPU");
        helper.assertTrue(serverInventory.canPlaceItem(3, new ItemStack(ModItems.COMPONENT_BUS_TIER3.get())), "Server rejected tier 3 component bus");
        helper.assertTrue(serverInventory.canPlaceItem(5, new ItemStack(ModItems.MEMORY_TIER3.get())), "Server rejected tier 3 memory");
        helper.assertTrue(serverInventory.canPlaceItem(8, new ItemStack(ModItems.HDD_TIER3.get())), "Server rejected tier 3 hard disk");
        helper.assertTrue(serverInventory.canPlaceItem(12, luaBiosEepromStack()), "Server rejected EEPROM");
        helper.assertTrue(!serverInventory.canPlaceItem(2, new ItemStack(ModItems.MEMORY_TIER1.get())), "Server CPU slot accepted memory");
        serverInventory.setItem(0, new ItemStack(ModItems.NETWORK_CARD.get()));
        serverInventory.setItem(2, new ItemStack(ModItems.CPU_TIER3.get()));
        serverInventory.setItem(3, new ItemStack(ModItems.COMPONENT_BUS_TIER3.get()));
        serverInventory.setItem(5, new ItemStack(ModItems.MEMORY_TIER3.get()));
        serverInventory.setItem(8, new ItemStack(ModItems.HDD_TIER3.get()));
        serverInventory.setItem(12, luaBiosEepromStack());
        int internalComponentCount = 0;
        for (final ItemStack stack : rackServer.internalComponents()) {
            if (!stack.isEmpty()) {
                internalComponentCount++;
            }
        }
        helper.assertTrue(internalComponentCount == 6, "Rack server internal component count mismatch");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void rackServerBootsAndReportsWorkingState(final GameTestHelper helper) {
        final BlockPos rackPos = new BlockPos(1, 1, 1);
        helper.setBlock(rackPos, ModBlocks.RACK.get());
        final RackBlockEntity rack = helper.getBlockEntity(rackPos);

        rack.setItem(0, new ItemStack(ModItems.SERVER_TIER2.get()));
        final li.cil.oc.api.component.RackMountable mountable = rack.getMountable(0);
        helper.assertTrue(mountable instanceof li.cil.oc.api.internal.Server, "Rack did not create server mountable");
        final li.cil.oc.api.internal.Server rackServer = (li.cil.oc.api.internal.Server) mountable;
        final net.minecraft.world.Container serverInventory = (net.minecraft.world.Container) rackServer;
        serverInventory.setItem(2, new ItemStack(ModItems.CPU_TIER3.get()));
        serverInventory.setItem(5, new ItemStack(ModItems.MEMORY_TIER3.get()));
        serverInventory.setItem(8, bootableHardDiskStack(helper, "computer.pushSignal('rack_booted', 'ok')"));
        serverInventory.setItem(12, luaBiosEepromStack());

        helper.assertTrue(rackServer.machine().components().containsValue("filesystem"), "Rack server filesystem is not visible before boot: " + rackServer.machine().components());
        helper.assertTrue(rackServer.machine().start(), "Rack server machine did not start");
        helper.assertTrue(mountable.getCurrentState().contains(li.cil.oc.api.util.StateAware.State.IsWorking), "Rack server did not report working state");
        final CompoundTag data = mountable.getData();
        mountable.save(data);
        helper.assertTrue(data.contains("machine"), "Rack server did not persist machine state");

        helper.succeedWhen(() -> {
            RackBlockEntity.serverTick(helper.getLevel(), rackPos, helper.getBlockState(rackPos), rack);
            helper.assertTrue(rackServer.machine().isRunning(), "Rack server stopped while ticking: " + rackServer.machine().lastError());
        });
    }

    @GameTest(template = "empty")
    public static void rackMenuDistinguishesEmptyAndIncompleteServerStates(final GameTestHelper helper) {
        final BlockPos rackPos = new BlockPos(1, 1, 1);
        helper.setBlock(rackPos, ModBlocks.RACK.get());
        final RackBlockEntity rack = helper.getBlockEntity(rackPos);

        helper.assertTrue(RackMenu.rackStateFor(rack, 0) == RackMenu.STATE_EMPTY, "Empty rack slot reported non-empty state");
        helper.assertTrue(RackMenu.rackMissingRequirementsFor(rack, 0) == 0, "Empty rack slot reported missing components");

        rack.setItem(0, new ItemStack(ModItems.SERVER_TIER2.get()));

        helper.assertTrue(RackMenu.rackStateFor(rack, 0) == RackMenu.STATE_INCOMPLETE, "Incomplete server looked like an empty slot");
        helper.assertTrue(RackMenu.rackMissingRequirementsFor(rack, 0) == (RackMenu.MISSING_CPU | RackMenu.MISSING_MEMORY | RackMenu.MISSING_EEPROM), "Incomplete server did not report all missing requirements");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void rackServerMaxComponentsUsesCpuAndComponentBus(final GameTestHelper helper) {
        final BlockPos rackPos = new BlockPos(1, 1, 1);
        helper.setBlock(rackPos, ModBlocks.RACK.get());
        final RackBlockEntity rack = helper.getBlockEntity(rackPos);

        rack.setItem(0, new ItemStack(ModItems.SERVER_TIER2.get()));
        final li.cil.oc.api.internal.Server rackServer = (li.cil.oc.api.internal.Server) rack.getMountable(0);
        final net.minecraft.world.Container serverInventory = (net.minecraft.world.Container) rackServer;
        serverInventory.setItem(2, new ItemStack(ModItems.CPU_TIER3.get()));
        serverInventory.setItem(3, new ItemStack(ModItems.COMPONENT_BUS_TIER3.get()));

        helper.assertTrue(rackServer.machine().maxComponents() == 32, "Rack server max components mismatch: " + rackServer.machine().maxComponents());
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void rackServerInventoryInvalidatesWhenRackSlotIsRemoved(final GameTestHelper helper) {
        final BlockPos rackPos = new BlockPos(1, 1, 1);
        helper.setBlock(rackPos, ModBlocks.RACK.get());
        final RackBlockEntity rack = helper.getBlockEntity(rackPos);

        rack.setItem(0, new ItemStack(ModItems.SERVER_TIER2.get()));
        final li.cil.oc.api.internal.Server rackServer = (li.cil.oc.api.internal.Server) rack.getMountable(0);
        final net.minecraft.world.Container serverInventory = (net.minecraft.world.Container) rackServer;

        helper.assertTrue(serverInventory.stillValid(null), "Installed rack server inventory was not valid");
        rack.removeItemNoUpdate(0);

        helper.assertTrue(!serverInventory.stillValid(null), "Removed rack server inventory stayed valid");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void brokenRackDropsStatefulServerItem(final GameTestHelper helper) {
        final BlockPos rackPos = new BlockPos(1, 1, 1);
        helper.killAllEntities();
        helper.setBlock(rackPos, ModBlocks.RACK.get());
        final RackBlockEntity rack = helper.getBlockEntity(rackPos);
        rack.setItem(0, new ItemStack(ModItems.SERVER_TIER2.get()));
        final li.cil.oc.api.internal.Server rackServer = (li.cil.oc.api.internal.Server) rack.getMountable(0);
        final net.minecraft.world.Container serverInventory = (net.minecraft.world.Container) rackServer;
        serverInventory.setItem(2, new ItemStack(ModItems.CPU_TIER3.get()));
        serverInventory.setItem(5, new ItemStack(ModItems.MEMORY_TIER3.get()));
        serverInventory.setItem(8, new ItemStack(ModItems.HDD_TIER3.get()));
        serverInventory.setItem(12, luaBiosEepromStack());

        helper.getLevel().destroyBlock(helper.absolutePos(rackPos), true);
        helper.runAtTickTime(1, () -> {
            helper.assertTrue(droppedItemCount(helper, ModItems.SERVER_TIER2.get()) == 1, "Broken rack did not drop exactly one tier 2 server item");
            final ItemStack dropped = droppedItemStack(helper, ModItems.SERVER_TIER2.get());
            final BlockPos loadedPos = new BlockPos(3, 1, 1);
            helper.setBlock(loadedPos, ModBlocks.RACK.get());
            final RackBlockEntity loadedRack = helper.getBlockEntity(loadedPos);
            loadedRack.setItem(0, dropped);
            final li.cil.oc.api.internal.Server loadedServer = (li.cil.oc.api.internal.Server) loadedRack.getMountable(0);
            final net.minecraft.world.Container loadedInventory = (net.minecraft.world.Container) loadedServer;
            helper.assertTrue(loadedInventory.getItem(2).is(ModItems.CPU_TIER3.get()), "Dropped rack server missing CPU");
            helper.assertTrue(loadedInventory.getItem(5).is(ModItems.MEMORY_TIER3.get()), "Dropped rack server missing memory");
            helper.assertTrue(loadedInventory.getItem(8).is(ModItems.HDD_TIER3.get()), "Dropped rack server missing hard disk");
            helper.assertTrue(loadedInventory.getItem(12).is(ModItems.EEPROM.get()), "Dropped rack server missing EEPROM");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void rackBlockItemRetainsMountablesAndServerComponents(final GameTestHelper helper) {
        final BlockPos rackPos = new BlockPos(1, 1, 1);
        helper.setBlock(rackPos, ModBlocks.RACK.get());
        final RackBlockEntity rack = helper.getBlockEntity(rackPos);
        rack.setItem(0, new ItemStack(ModItems.SERVER_TIER2.get()));
        rack.setItem(1, new ItemStack(ModItems.TERMINAL_SERVER.get()));
        final li.cil.oc.api.internal.Server rackServer = (li.cil.oc.api.internal.Server) rack.getMountable(0);
        final net.minecraft.world.Container serverInventory = (net.minecraft.world.Container) rackServer;
        serverInventory.setItem(2, new ItemStack(ModItems.CPU_TIER3.get()));
        serverInventory.setItem(5, new ItemStack(ModItems.MEMORY_TIER3.get()));
        serverInventory.setItem(8, new ItemStack(ModItems.HDD_TIER3.get()));
        serverInventory.setItem(12, luaBiosEepromStack());

        final ItemStack clone = ModBlocks.RACK.get().getCloneItemStack(helper.getLevel(), helper.absolutePos(rackPos), helper.getBlockState(rackPos));
        final BlockPos loadedPos = new BlockPos(3, 1, 1);
        helper.setBlock(loadedPos, ModBlocks.RACK.get());
        ModBlocks.RACK.get().setPlacedBy(helper.getLevel(), helper.absolutePos(loadedPos), helper.getBlockState(loadedPos), null, clone);

        final RackBlockEntity loadedRack = helper.getBlockEntity(loadedPos);
        helper.assertTrue(loadedRack.getItem(0).is(ModItems.SERVER_TIER2.get()), "Loaded rack missing server");
        helper.assertTrue(loadedRack.getItem(1).is(ModItems.TERMINAL_SERVER.get()), "Loaded rack missing terminal server");
        final li.cil.oc.api.internal.Server loadedServer = (li.cil.oc.api.internal.Server) loadedRack.getMountable(0);
        final net.minecraft.world.Container loadedInventory = (net.minecraft.world.Container) loadedServer;
        helper.assertTrue(loadedInventory.getItem(2).is(ModItems.CPU_TIER3.get()), "Loaded rack server missing CPU");
        helper.assertTrue(loadedInventory.getItem(5).is(ModItems.MEMORY_TIER3.get()), "Loaded rack server missing memory");
        helper.assertTrue(loadedInventory.getItem(8).is(ModItems.HDD_TIER3.get()), "Loaded rack server missing hard disk");
        helper.assertTrue(loadedInventory.getItem(12).is(ModItems.EEPROM.get()), "Loaded rack server missing EEPROM");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void powerDistributorBalancesSidedConnectorBuffers(final GameTestHelper helper) {
        final BlockPos distributorPos = new BlockPos(1, 1, 1);
        helper.setBlock(distributorPos, ModBlocks.POWER_DISTRIBUTOR.get());
        Network.joinOrCreateNetwork(helper.getLevel(), helper.absolutePos(distributorPos));
        final PowerDistributorBlockEntity distributor = helper.getBlockEntity(distributorPos);

        final Connector east = (Connector) distributor.sidedNode(Direction.EAST);
        final Connector west = (Connector) distributor.sidedNode(Direction.WEST);
        helper.assertTrue(east != null && west != null, "Power distributor did not expose sided connectors");
        helper.assertTrue(Double.compare(PowerDistributorBlockEntity.CONNECTOR_BUFFER_SIZE, east.localBufferSize()) == 0, "Power distributor connector capacity mismatch");
        east.changeBuffer(PowerDistributorBlockEntity.CONNECTOR_BUFFER_SIZE);

        PowerDistributorBlockEntity.serverTick(helper.getLevel(), distributorPos, helper.getBlockState(distributorPos), distributor);

        final double expected = PowerDistributorBlockEntity.CONNECTOR_BUFFER_SIZE / Direction.values().length;
        assertClose(helper, east.localBuffer(), expected, "Power distributor east buffer");
        assertClose(helper, west.localBuffer(), expected, "Power distributor west buffer");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void relayForwardsPacketsBetweenSideNetworks(final GameTestHelper helper) {
        final BlockPos relayPos = new BlockPos(1, 1, 1);
        helper.setBlock(relayPos, ModBlocks.RELAY.get());
        Network.joinOrCreateNetwork(helper.getLevel(), helper.absolutePos(relayPos));
        final RelayBlockEntity relay = helper.getBlockEntity(relayPos);

        final RecordingNetworkEnvironment source = new RecordingNetworkEnvironment();
        final RecordingNetworkEnvironment receiver = new RecordingNetworkEnvironment();
        Network.joinNewNetwork(source.node());
        Network.joinNewNetwork(receiver.node());
        source.node().connect(relay.sidedNode(Direction.WEST));
        receiver.node().connect(relay.sidedNode(Direction.EAST));

        final li.cil.oc.api.network.Packet packet = Network.newPacket(source.node().address(), null, 123, new Object[]{"payload"});
        source.node().sendToReachable("network.message", packet);
        tickRelayThroughDelay(helper, relayPos, relay);

        helper.assertTrue(receiver.lastPacket != null, "Relay did not forward packet");
        helper.assertTrue(receiver.lastPacket.port() == 123, "Relay forwarded wrong port");
        helper.assertTrue(receiver.lastPacket.ttl() == packet.ttl() - 1, "Relay did not decrement packet TTL");
        helper.assertTrue("payload".equals(receiver.lastPacket.data()[0]), "Relay forwarded wrong payload");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void relayWaitsConfiguredDelayBeforeForwarding(final GameTestHelper helper) {
        final BlockPos relayPos = new BlockPos(1, 1, 1);
        helper.setBlock(relayPos, ModBlocks.RELAY.get());
        Network.joinOrCreateNetwork(helper.getLevel(), helper.absolutePos(relayPos));
        final RelayBlockEntity relay = helper.getBlockEntity(relayPos);

        final RecordingNetworkEnvironment source = new RecordingNetworkEnvironment();
        final RecordingNetworkEnvironment receiver = new RecordingNetworkEnvironment();
        Network.joinNewNetwork(source.node());
        Network.joinNewNetwork(receiver.node());
        source.node().connect(relay.sidedNode(Direction.WEST));
        receiver.node().connect(relay.sidedNode(Direction.EAST));

        source.node().sendToReachable("network.message", Network.newPacket(source.node().address(), null, 124, new Object[]{"delayed"}));
        for (int tick = 1; tick < relay.relayDelay(); tick++) {
            RelayBlockEntity.serverTick(helper.getLevel(), relayPos, helper.getBlockState(relayPos), relay);
            helper.assertTrue(receiver.lastPacket == null, "Relay ignored relay delay");
        }

        RelayBlockEntity.serverTick(helper.getLevel(), relayPos, helper.getBlockState(relayPos), relay);
        helper.assertTrue(receiver.lastPacket != null, "Relay did not forward after relay delay");
        helper.assertTrue("delayed".equals(receiver.lastPacket.data()[0]), "Relay delayed wrong payload");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void relayWirelessCardRelaysPacketsToWirelessEndpoints(final GameTestHelper helper) {
        final BlockPos relayPos = new BlockPos(1, 1, 1);
        helper.setBlock(relayPos, ModBlocks.RELAY.get());
        Network.joinOrCreateNetwork(helper.getLevel(), helper.absolutePos(relayPos));
        final RelayBlockEntity relay = helper.getBlockEntity(relayPos);
        relay.setItem(RelayBlockEntity.CARD_SLOT, new ItemStack(ModItems.WIRELESS_NETWORK_CARD_TIER1.get()));

        final RecordingNetworkEnvironment source = new RecordingNetworkEnvironment();
        final RecordingWirelessEndpoint receiver = new RecordingWirelessEndpoint(helper.getLevel(), helper.absolutePos(new BlockPos(4, 1, 1)));
        Network.joinNewNetwork(source.node());
        Network.joinWirelessNetwork(receiver);
        source.node().connect(relay.sidedNode(Direction.WEST));
        final Connector relayConnector = (Connector) relay.sidedNode(Direction.WEST);
        relayConnector.changeBuffer(10);

        final li.cil.oc.api.network.Packet packet = Network.newPacket(source.node().address(), null, 222, new Object[]{"wireless"});
        source.node().sendToReachable("network.message", packet);
        tickRelayThroughDelay(helper, relayPos, relay);

        helper.assertTrue(receiver.lastPacket != null, "Relay did not send wireless packet");
        helper.assertTrue(receiver.lastSender == relay, "Relay did not identify as wireless sender");
        helper.assertTrue(receiver.lastPacket.ttl() == packet.ttl() - 1, "Relay wireless packet did not hop");
        helper.assertTrue("wireless".equals(receiver.lastPacket.data()[0]), "Relay sent wrong wireless payload");
        helper.assertTrue(relayConnector.localBuffer() < 10, "Relay did not spend energy for wireless forwarding");
        Network.leaveWirelessNetwork(receiver);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void relayLinkedCardRelaysPacketsBetweenRelays(final GameTestHelper helper) {
        final BlockPos firstRelayPos = new BlockPos(1, 1, 1);
        final BlockPos secondRelayPos = new BlockPos(5, 1, 1);
        helper.setBlock(firstRelayPos, ModBlocks.RELAY.get());
        helper.setBlock(secondRelayPos, ModBlocks.RELAY.get());
        Network.joinOrCreateNetwork(helper.getLevel(), helper.absolutePos(firstRelayPos));
        Network.joinOrCreateNetwork(helper.getLevel(), helper.absolutePos(secondRelayPos));
        final RelayBlockEntity firstRelay = helper.getBlockEntity(firstRelayPos);
        final RelayBlockEntity secondRelay = helper.getBlockEntity(secondRelayPos);
        firstRelay.setItem(RelayBlockEntity.CARD_SLOT, new ItemStack(ModItems.LINKED_CARD.get()));
        secondRelay.setItem(RelayBlockEntity.CARD_SLOT, new ItemStack(ModItems.LINKED_CARD.get()));

        final RecordingNetworkEnvironment source = new RecordingNetworkEnvironment();
        final RecordingNetworkEnvironment receiver = new RecordingNetworkEnvironment();
        Network.joinNewNetwork(source.node());
        Network.joinNewNetwork(receiver.node());
        source.node().connect(firstRelay.sidedNode(Direction.WEST));
        receiver.node().connect(secondRelay.sidedNode(Direction.EAST));
        ((Connector) firstRelay.sidedNode(Direction.WEST)).changeBuffer(RelayBlockEntity.CONNECTOR_BUFFER_SIZE);

        final li.cil.oc.api.network.Packet packet = Network.newPacket(source.node().address(), null, 225, new Object[]{"linked"});
        source.node().sendToReachable("network.message", packet);
        tickRelayThroughDelay(helper, firstRelayPos, firstRelay);
        tickRelayThroughDelay(helper, secondRelayPos, secondRelay);

        helper.assertTrue(receiver.lastPacket != null, "Relay linked card did not forward packet");
        helper.assertTrue(receiver.lastPacket.port() == 225, "Relay linked card forwarded wrong port");
        helper.assertTrue(receiver.lastPacket.ttl() == packet.ttl() - 2, "Relay linked card did not count both relay hops");
        helper.assertTrue("linked".equals(receiver.lastPacket.data()[0]), "Relay linked card forwarded wrong payload");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void relayComponentCallbacksConfigureWirelessForwarding(final GameTestHelper helper) throws Exception {
        final BlockPos relayPos = new BlockPos(1, 1, 1);
        helper.setBlock(relayPos, ModBlocks.RELAY.get());
        Network.joinOrCreateNetwork(helper.getLevel(), helper.absolutePos(relayPos));
        final RelayBlockEntity relay = helper.getBlockEntity(relayPos);
        relay.setItem(RelayBlockEntity.CARD_SLOT, new ItemStack(ModItems.WIRELESS_NETWORK_CARD_TIER1.get()));
        helper.assertTrue(relay.sidedNode(Direction.WEST) instanceof li.cil.oc.api.network.Component, "Relay side did not expose relay component");
        final li.cil.oc.api.network.Component component = (li.cil.oc.api.network.Component) relay.sidedNode(Direction.WEST);

        helper.assertTrue(component.methods().contains("getStrength"), "Relay component missing getStrength callback");
        helper.assertTrue(component.methods().contains("setStrength"), "Relay component missing setStrength callback");
        helper.assertTrue(component.methods().contains("isRepeater"), "Relay component missing isRepeater callback");
        helper.assertTrue(component.methods().contains("setRepeater"), "Relay component missing setRepeater callback");
        helper.assertTrue((Boolean) component.invoke("isRepeater", null)[0], "Relay repeater default mismatch");
        helper.assertTrue(Double.valueOf(2D).equals(component.invoke("setStrength", null, 2D)[0]), "Relay setStrength returned wrong value");
        helper.assertTrue(Double.valueOf(2D).equals(component.invoke("getStrength", null)[0]), "Relay getStrength returned wrong value");
        helper.assertTrue(Boolean.FALSE.equals(component.invoke("setRepeater", null, false)[0]), "Relay setRepeater returned wrong value");
        helper.assertTrue(Boolean.FALSE.equals(component.invoke("isRepeater", null)[0]), "Relay repeater setting did not persist");

        final RecordingNetworkEnvironment source = new RecordingNetworkEnvironment();
        final RecordingWirelessEndpoint receiver = new RecordingWirelessEndpoint(helper.getLevel(), helper.absolutePos(new BlockPos(4, 1, 1)));
        Network.joinNewNetwork(source.node());
        Network.joinWirelessNetwork(receiver);
        source.node().connect(relay.sidedNode(Direction.WEST));
        ((Connector) relay.sidedNode(Direction.WEST)).changeBuffer(10);

        source.node().sendToReachable("network.message", Network.newPacket(source.node().address(), null, 223, new Object[]{"short"}));
        tickRelayThroughDelay(helper, relayPos, relay);
        helper.assertTrue(receiver.lastPacket == null, "Relay ignored configured low wireless strength");

        component.invoke("setStrength", null, 4D);
        source.node().sendToReachable("network.message", Network.newPacket(source.node().address(), null, 224, new Object[]{"long"}));
        tickRelayThroughDelay(helper, relayPos, relay);
        helper.assertTrue(receiver.lastPacket != null, "Relay did not use configured wireless strength");
        helper.assertTrue("long".equals(receiver.lastPacket.data()[0]), "Relay sent wrong configured wireless payload");
        Network.leaveWirelessNetwork(receiver);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void relayWirelessControlsPersistThroughNbt(final GameTestHelper helper) throws Exception {
        final BlockPos relayPos = new BlockPos(1, 1, 1);
        helper.setBlock(relayPos, ModBlocks.RELAY.get());
        final RelayBlockEntity relay = helper.getBlockEntity(relayPos);
        relay.setItem(RelayBlockEntity.CARD_SLOT, new ItemStack(ModItems.WIRELESS_NETWORK_CARD_TIER1.get()));
        final li.cil.oc.api.network.Component component = (li.cil.oc.api.network.Component) relay.sidedNode(Direction.WEST);
        component.invoke("setStrength", null, 5D);
        component.invoke("setRepeater", null, false);

        final CompoundTag saved = relay.saveWithFullMetadata(helper.getLevel().registryAccess());
        final RelayBlockEntity loaded = new RelayBlockEntity(relayPos, helper.getBlockState(relayPos));
        loaded.loadWithComponents(saved, helper.getLevel().registryAccess());
        final li.cil.oc.api.network.Component loadedComponent = (li.cil.oc.api.network.Component) loaded.sidedNode(Direction.WEST);

        helper.assertTrue(Double.valueOf(5D).equals(loadedComponent.invoke("getStrength", null)[0]), "Relay strength did not persist");
        helper.assertTrue(Boolean.FALSE.equals(loadedComponent.invoke("isRepeater", null)[0]), "Relay repeater setting did not persist");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void analyzerReportsRelayOnlyWhenWirelessEnabled(final GameTestHelper helper) {
        final BlockPos relayPos = new BlockPos(1, 1, 1);
        helper.setBlock(relayPos, ModBlocks.RELAY.get());
        Network.joinOrCreateNetwork(helper.getLevel(), helper.absolutePos(relayPos));
        final RelayBlockEntity relay = helper.getBlockEntity(relayPos);

        helper.assertTrue(AnalyzerItem.describe(relay, Direction.WEST).isEmpty(), "Analyzer reported relay without wireless card");

        relay.setItem(RelayBlockEntity.CARD_SLOT, new ItemStack(ModItems.WIRELESS_NETWORK_CARD_TIER1.get()));
        final List<Component> lines = AnalyzerItem.describe(relay, Direction.WEST);
        final String analysis = lines.stream().map(Component::getString).collect(java.util.stream.Collectors.joining("\n"));
        helper.assertTrue(analysis.contains("Component: relay"), "Analyzer did not report wireless relay component:\n" + analysis);
        helper.assertTrue(analysis.contains("Stored energy: 0.00/600.00"), "Analyzer did not report relay connector energy:\n" + analysis);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void connectedConnectorNodesShareGlobalEnergy(final GameTestHelper helper) {
        final RecordingConnectorEnvironment source = new RecordingConnectorEnvironment(10);
        final RecordingConnectorEnvironment sink = new RecordingConnectorEnvironment(10);
        Network.joinNewNetwork(source.node());
        Network.joinNewNetwork(sink.node());
        source.node().connect(sink.node());

        source.connector().changeBuffer(10);

        assertClose(helper, sink.connector().globalBuffer(), 10, "Connected connector global buffer");
        assertClose(helper, sink.connector().globalBufferSize(), 20, "Connected connector global capacity");
        helper.assertTrue(sink.connector().tryChangeBuffer(-6), "Connected connector could not drain shared energy");
        assertClose(helper, source.connector().localBuffer(), 4, "Connected connector source buffer after drain");
        assertClose(helper, sink.connector().globalBuffer(), 4, "Connected connector global buffer after drain");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void relayUpgradeInventoryUpdatesPacketLimits(final GameTestHelper helper) {
        final BlockPos relayPos = new BlockPos(1, 1, 1);
        helper.setBlock(relayPos, ModBlocks.RELAY.get());
        final RelayBlockEntity relay = helper.getBlockEntity(relayPos);

        helper.assertTrue(relay.getContainerSize() == RelayBlockEntity.CONTAINER_SIZE, "Relay inventory size mismatch");
        helper.assertTrue(relay.canPlaceItem(RelayBlockEntity.CPU_SLOT, new ItemStack(ModItems.CPU_TIER3.get())), "Relay rejected CPU slot");
        helper.assertTrue(relay.canPlaceItem(RelayBlockEntity.MEMORY_SLOT, new ItemStack(ModItems.MEMORY_TIER3.get())), "Relay rejected memory slot");
        helper.assertTrue(relay.canPlaceItem(RelayBlockEntity.HDD_SLOT, new ItemStack(ModItems.HDD_TIER3.get())), "Relay rejected HDD slot");
        helper.assertTrue(relay.canPlaceItem(RelayBlockEntity.CARD_SLOT, new ItemStack(ModItems.WIRELESS_NETWORK_CARD_TIER2.get())), "Relay rejected wireless card slot");
        helper.assertTrue(!relay.canPlaceItem(RelayBlockEntity.CPU_SLOT, new ItemStack(ModItems.MEMORY_TIER3.get())), "Relay accepted memory in CPU slot");
        helper.assertTrue(!relay.canPlaceItem(RelayBlockEntity.CARD_SLOT, new ItemStack(ModItems.INTERNET_CARD.get())), "Relay accepted unsupported card");

        final int baseDelay = relay.relayDelay();
        final int baseAmount = relay.relayAmount();
        final int baseQueueSize = relay.maxQueueSize();

        relay.setItem(RelayBlockEntity.CPU_SLOT, new ItemStack(ModItems.CPU_TIER3.get()));
        relay.setItem(RelayBlockEntity.MEMORY_SLOT, new ItemStack(ModItems.MEMORY_TIER3.get()));
        relay.setItem(RelayBlockEntity.HDD_SLOT, new ItemStack(ModItems.HDD_TIER3.get()));
        relay.setItem(RelayBlockEntity.CARD_SLOT, new ItemStack(ModItems.WIRELESS_NETWORK_CARD_TIER2.get()));

        helper.assertTrue(relay.relayDelay() < baseDelay, "Relay CPU did not reduce delay");
        helper.assertTrue(relay.relayAmount() > baseAmount, "Relay memory did not increase packet amount");
        helper.assertTrue(relay.maxQueueSize() > baseQueueSize, "Relay HDD did not increase queue size");
        helper.assertTrue(relay.isWirelessEnabled(), "Relay wireless card did not enable wireless mode");

        relay.removeItemNoUpdate(RelayBlockEntity.CPU_SLOT);
        relay.removeItemNoUpdate(RelayBlockEntity.MEMORY_SLOT);
        relay.removeItemNoUpdate(RelayBlockEntity.HDD_SLOT);
        relay.removeItemNoUpdate(RelayBlockEntity.CARD_SLOT);

        helper.assertTrue(relay.relayDelay() == baseDelay, "Relay CPU removal did not reset delay");
        helper.assertTrue(relay.relayAmount() == baseAmount, "Relay memory removal did not reset packet amount");
        helper.assertTrue(relay.maxQueueSize() == baseQueueSize, "Relay HDD removal did not reset queue size");
        helper.assertTrue(!relay.isWirelessEnabled(), "Relay card removal did not disable wireless mode");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void relayUsesConfiguredUpgradeLimits(final GameTestHelper helper) throws Exception {
        withCachedConfig(ModSettings.DEFAULT_RELAY_DELAY, 7, () ->
            withCachedConfig(ModSettings.RELAY_DELAY_UPGRADE, 2.5D, () ->
                withCachedConfig(ModSettings.DEFAULT_RELAY_AMOUNT, 3, () ->
                    withCachedConfig(ModSettings.RELAY_AMOUNT_UPGRADE, 2, () ->
                        withCachedConfig(ModSettings.DEFAULT_MAX_QUEUE_SIZE, 11, () ->
                            withCachedConfig(ModSettings.QUEUE_SIZE_UPGRADE, 4, () -> {
                                final BlockPos relayPos = new BlockPos(1, 1, 1);
                                helper.setBlock(relayPos, ModBlocks.RELAY.get());
                                final RelayBlockEntity relay = helper.getBlockEntity(relayPos);

                                helper.assertTrue(relay.relayDelay() == 7, "Relay ignored configured base delay");
                                helper.assertTrue(relay.relayAmount() == 3, "Relay ignored configured base relay amount");
                                helper.assertTrue(relay.maxQueueSize() == 11, "Relay ignored configured base queue size");

                                relay.setItem(RelayBlockEntity.CPU_SLOT, new ItemStack(ModItems.CPU_TIER3.get()));
                                relay.setItem(RelayBlockEntity.MEMORY_SLOT, new ItemStack(ModItems.MEMORY_TIER3.get()));
                                relay.setItem(RelayBlockEntity.HDD_SLOT, new ItemStack(ModItems.HDD_TIER3.get()));

                                helper.assertTrue(relay.relayDelay() == 1, "Relay ignored configured CPU delay upgrade");
                                helper.assertTrue(relay.relayAmount() == 9, "Relay ignored configured memory amount upgrade");
                                helper.assertTrue(relay.maxQueueSize() == 23, "Relay ignored configured HDD queue upgrade");
                                helper.succeed();
                            }))))));
    }

    @GameTest(template = "empty")
    public static void relayUsesConfiguredWirelessRangeAndCost(final GameTestHelper helper) throws Exception {
        withCachedConfig(ModSettings.MAX_WIRELESS_RANGE, List.of(6D, 9D), () ->
            withCachedConfig(ModSettings.WIRELESS_COST_PER_RANGE, List.of(0.25D, 0.5D), () -> {
                final BlockPos relayPos = new BlockPos(1, 1, 1);
                helper.setBlock(relayPos, ModBlocks.RELAY.get());
                Network.joinOrCreateNetwork(helper.getLevel(), helper.absolutePos(relayPos));
                final RelayBlockEntity relay = helper.getBlockEntity(relayPos);
                relay.setItem(RelayBlockEntity.CARD_SLOT, new ItemStack(ModItems.WIRELESS_NETWORK_CARD_TIER2.get()));
                helper.assertTrue(li.cil.oc.common.menu.RelayMenu.relayStrengthFor(relay) == 9, "Relay ignored configured wireless range");

                final RecordingNetworkEnvironment source = new RecordingNetworkEnvironment();
                final RecordingWirelessEndpoint receiver = new RecordingWirelessEndpoint(helper.getLevel(), helper.absolutePos(new BlockPos(9, 1, 1)));
                Network.joinNewNetwork(source.node());
                Network.joinWirelessNetwork(receiver);
                source.node().connect(relay.sidedNode(Direction.WEST));
                final Connector relayConnector = (Connector) relay.sidedNode(Direction.WEST);
                relayConnector.changeBuffer(10);

                source.node().sendToReachable("network.message", Network.newPacket(source.node().address(), null, 226, new Object[]{"configured"}));
                tickRelayThroughDelay(helper, relayPos, relay);

                helper.assertTrue(receiver.lastPacket != null, "Relay did not use configured wireless range");
                helper.assertTrue("configured".equals(receiver.lastPacket.data()[0]), "Relay sent wrong configured wireless payload");
                assertClose(helper, relayConnector.localBuffer(), 5.5D, "Relay configured wireless cost");
                Network.leaveWirelessNetwork(receiver);
                helper.succeed();
            }));
    }

    @GameTest(template = "empty")
    public static void relayMenuReportsRuntimeStatus(final GameTestHelper helper) throws Exception {
        final BlockPos relayPos = new BlockPos(1, 1, 1);
        helper.setBlock(relayPos, ModBlocks.RELAY.get());
        final RelayBlockEntity relay = helper.getBlockEntity(relayPos);

        helper.assertTrue(li.cil.oc.common.menu.RelayMenu.relayModeFor(relay) == li.cil.oc.common.menu.RelayMenu.MODE_WIRED, "Empty relay did not report wired mode");
        helper.assertTrue(li.cil.oc.common.menu.RelayMenu.relayDelayFor(relay) == relay.relayDelay(), "Relay delay status mismatch");
        helper.assertTrue(li.cil.oc.common.menu.RelayMenu.relayMaxQueueSizeFor(relay) == relay.maxQueueSize(), "Relay max queue status mismatch");
        helper.assertTrue(li.cil.oc.common.menu.RelayMenu.relayQueueSizeFor(relay) == 0, "Empty relay reported queued packets");
        helper.assertTrue(li.cil.oc.common.menu.RelayMenu.relayStrengthFor(relay) == 0, "Empty relay reported wireless strength");
        helper.assertTrue(li.cil.oc.common.menu.RelayMenu.relayRepeaterFor(relay) == 1, "Relay repeater default status mismatch");

        relay.setItem(RelayBlockEntity.CARD_SLOT, new ItemStack(ModItems.WIRELESS_NETWORK_CARD_TIER2.get()));
        helper.assertTrue(li.cil.oc.common.menu.RelayMenu.relayModeFor(relay) == li.cil.oc.common.menu.RelayMenu.MODE_WIRELESS, "Wireless relay did not report wireless mode");
        helper.assertTrue(li.cil.oc.common.menu.RelayMenu.relayStrengthFor(relay) == 400, "Wireless relay did not report max strength");
        final li.cil.oc.api.network.Component component = (li.cil.oc.api.network.Component) relay.sidedNode(Direction.WEST);
        component.invoke("setStrength", null, 12D);
        component.invoke("setRepeater", null, false);
        helper.assertTrue(li.cil.oc.common.menu.RelayMenu.relayStrengthFor(relay) == 12, "Wireless relay did not report configured strength");
        helper.assertTrue(li.cil.oc.common.menu.RelayMenu.relayRepeaterFor(relay) == 0, "Wireless relay did not report repeater state");

        relay.setItem(RelayBlockEntity.CARD_SLOT, new ItemStack(ModItems.LINKED_CARD.get()));
        helper.assertTrue(li.cil.oc.common.menu.RelayMenu.relayModeFor(relay) == li.cil.oc.common.menu.RelayMenu.MODE_LINKED, "Linked relay did not report linked mode");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void brokenRelayDropsInstalledUpgradeItems(final GameTestHelper helper) {
        final BlockPos relayPos = new BlockPos(1, 1, 1);
        helper.killAllEntities();
        helper.setBlock(relayPos, ModBlocks.RELAY.get());
        final RelayBlockEntity relay = helper.getBlockEntity(relayPos);
        relay.setItem(RelayBlockEntity.CPU_SLOT, new ItemStack(ModItems.CPU_TIER3.get()));
        relay.setItem(RelayBlockEntity.MEMORY_SLOT, new ItemStack(ModItems.MEMORY_TIER3.get()));
        relay.setItem(RelayBlockEntity.HDD_SLOT, new ItemStack(ModItems.HDD_TIER3.get()));
        relay.setItem(RelayBlockEntity.CARD_SLOT, new ItemStack(ModItems.WIRELESS_NETWORK_CARD_TIER2.get()));

        helper.getLevel().destroyBlock(helper.absolutePos(relayPos), true);
        helper.runAtTickTime(1, () -> {
            helper.assertTrue(droppedItemCount(helper, ModItems.CPU_TIER3.get()) == 1, "Broken relay did not drop installed CPU");
            helper.assertTrue(droppedItemCount(helper, ModItems.MEMORY_TIER3.get()) == 1, "Broken relay did not drop installed memory");
            helper.assertTrue(droppedItemCount(helper, ModItems.HDD_TIER3.get()) == 1, "Broken relay did not drop installed HDD");
            helper.assertTrue(droppedItemCount(helper, ModItems.WIRELESS_NETWORK_CARD_TIER2.get()) == 1, "Broken relay did not drop installed wireless card");
            helper.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void terminalServerExposesVirtualScreenAndKeyboard(final GameTestHelper helper) {
        final BlockPos rackPos = new BlockPos(1, 1, 1);
        helper.setBlock(rackPos, ModBlocks.RACK.get());
        final RackBlockEntity rack = helper.getBlockEntity(rackPos);

        rack.setItem(0, new ItemStack(ModItems.TERMINAL_SERVER.get()));
        final li.cil.oc.api.component.RackMountable terminalServer = rack.getMountable(0);
        helper.assertTrue(terminalServer != null, "Rack did not create terminal server mountable");
        boolean sawScreen = false;
        boolean sawKeyboard = false;
        for (final Node node : terminalServer.node().neighbors()) {
            if (node instanceof li.cil.oc.api.network.Component component && "screen".equals(component.name()) && node.host() instanceof li.cil.oc.api.internal.TextBuffer) {
                sawScreen = true;
            }
            if (node instanceof li.cil.oc.api.network.Component component && "keyboard".equals(component.name()) && node.host() instanceof li.cil.oc.api.internal.Keyboard) {
                sawKeyboard = true;
            }
        }

        helper.assertTrue(sawScreen, "Terminal server did not expose virtual screen");
        helper.assertTrue(sawKeyboard, "Terminal server did not expose virtual keyboard");
        final CompoundTag data = terminalServer.getData();
        terminalServer.save(data);
        helper.assertTrue(data.contains("screen") && data.contains("keyboard"), "Terminal server did not persist virtual terminal state");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void terminalItemBindsToTerminalServer(final GameTestHelper helper) {
        final BlockPos rackPos = new BlockPos(1, 1, 1);
        helper.setBlock(rackPos, ModBlocks.RACK.get());
        final RackBlockEntity rack = helper.getBlockEntity(rackPos);
        rack.setItem(0, new ItemStack(ModItems.TERMINAL_SERVER.get()));
        final ItemStack terminal = new ItemStack(ModItems.TERMINAL.get());

        helper.assertTrue(TerminalItem.bindToTerminalServer(terminal, rack, 0), "Terminal did not bind to terminal server");
        final CompoundTag data = terminal.get(DataComponents.CUSTOM_DATA).copyTag().getCompound("oc:terminal");
        helper.assertTrue(data.contains("terminalServer"), "Terminal binding missing terminal server address");
        helper.assertTrue(data.contains("screen"), "Terminal binding missing screen address");
        helper.assertTrue(data.contains("keyboard"), "Terminal binding missing keyboard address");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void terminalServerRegistryTracksLiveMountables(final GameTestHelper helper) {
        final BlockPos rackPos = new BlockPos(1, 1, 1);
        helper.setBlock(rackPos, ModBlocks.RACK.get());
        final RackBlockEntity rack = helper.getBlockEntity(rackPos);
        rack.setItem(0, new ItemStack(ModItems.TERMINAL_SERVER.get()));
        final ItemStack terminal = new ItemStack(ModItems.TERMINAL.get());

        helper.assertTrue(TerminalItem.bindToTerminalServer(terminal, rack, 0), "Terminal did not bind to terminal server");
        final CompoundTag data = terminal.get(DataComponents.CUSTOM_DATA).copyTag().getCompound(TerminalItem.DATA_TAG);
        final String address = data.getString(TerminalItem.TERMINAL_SERVER_TAG);

        helper.assertTrue(TerminalServerRegistry.find(address) == rack.getMountable(0), "Terminal server registry did not resolve live mountable");
        rack.removeItemNoUpdate(0);
        helper.assertTrue(TerminalServerRegistry.find(address) == null, "Terminal server registry kept removed mountable");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void terminalServerPersistsBoundTerminalKeys(final GameTestHelper helper) {
        final BlockPos rackPos = new BlockPos(1, 1, 1);
        helper.setBlock(rackPos, ModBlocks.RACK.get());
        final RackBlockEntity rack = helper.getBlockEntity(rackPos);
        rack.setItem(0, new ItemStack(ModItems.TERMINAL_SERVER.get()));
        final TerminalServerRackMountableEnvironment terminalServer = (TerminalServerRackMountableEnvironment) rack.getMountable(0);
        final ItemStack terminal = new ItemStack(ModItems.TERMINAL.get());

        helper.assertTrue(TerminalItem.bindToTerminalServer(terminal, rack, 0), "Terminal did not bind to terminal server");
        helper.assertTrue(terminalServer.allowsTerminal(terminal), "Terminal server did not authorize bound terminal");

        final CompoundTag saved = terminalServer.getData();
        terminalServer.save(saved);
        final TerminalServerRackMountableEnvironment loaded = new TerminalServerRackMountableEnvironment();
        loaded.load(saved);

        helper.assertTrue(loaded.allowsTerminal(terminal), "Terminal server did not persist bound terminal key");
        loaded.removeVirtualNodes();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void terminalServerKeepsOnlyLatestFourTerminalKeys(final GameTestHelper helper) {
        final BlockPos rackPos = new BlockPos(1, 1, 1);
        helper.setBlock(rackPos, ModBlocks.RACK.get());
        final RackBlockEntity rack = helper.getBlockEntity(rackPos);
        rack.setItem(0, new ItemStack(ModItems.TERMINAL_SERVER.get()));
        final TerminalServerRackMountableEnvironment terminalServer = (TerminalServerRackMountableEnvironment) rack.getMountable(0);
        final ItemStack[] terminals = new ItemStack[5];
        for (int index = 0; index < terminals.length; index++) {
            terminals[index] = new ItemStack(ModItems.TERMINAL.get());
            helper.assertTrue(TerminalItem.bindToTerminalServer(terminals[index], rack, 0), "Terminal did not bind to terminal server");
        }

        helper.assertFalse(terminalServer.allowsTerminal(terminals[0]), "Terminal server kept evicted terminal key");
        for (int index = 1; index < terminals.length; index++) {
            helper.assertTrue(terminalServer.allowsTerminal(terminals[index]), "Terminal server lost recent terminal key");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void terminalItemResolvesOnlyAuthorizedLiveTerminalServer(final GameTestHelper helper) {
        final BlockPos rackPos = new BlockPos(1, 1, 1);
        helper.setBlock(rackPos, ModBlocks.RACK.get());
        final RackBlockEntity rack = helper.getBlockEntity(rackPos);
        rack.setItem(0, new ItemStack(ModItems.TERMINAL_SERVER.get()));
        final TerminalServerRackMountableEnvironment terminalServer = (TerminalServerRackMountableEnvironment) rack.getMountable(0);
        final ItemStack terminal = new ItemStack(ModItems.TERMINAL.get());

        helper.assertTrue(TerminalItem.bindToTerminalServer(terminal, rack, 0), "Terminal did not bind to terminal server");
        helper.assertTrue(TerminalItem.findBoundTerminalServer(terminal) == terminalServer, "Terminal did not resolve authorized terminal server");
        terminalServer.removeVirtualNodes();
        helper.assertTrue(TerminalItem.findBoundTerminalServer(terminal) == null, "Terminal resolved removed terminal server");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void terminalServerSnapshotIncludesVirtualScreenText(final GameTestHelper helper) {
        final BlockPos rackPos = new BlockPos(1, 1, 1);
        helper.setBlock(rackPos, ModBlocks.RACK.get());
        final RackBlockEntity rack = helper.getBlockEntity(rackPos);
        rack.setItem(0, new ItemStack(ModItems.TERMINAL_SERVER.get()));
        final TerminalServerRackMountableEnvironment terminalServer = (TerminalServerRackMountableEnvironment) rack.getMountable(0);

        terminalServer.screen().set(0, 0, "neo", false);
        final li.cil.oc.common.component.TerminalScreenSnapshot snapshot = terminalServer.screenSnapshot();

        helper.assertTrue(snapshot.width() == terminalServer.screen().renderWidth(), "Terminal snapshot width mismatch");
        helper.assertTrue(snapshot.height() == terminalServer.screen().renderHeight(), "Terminal snapshot height mismatch");
        helper.assertTrue("neo".equals(snapshot.line(0)), "Terminal snapshot missed virtual screen text");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void terminalItemMenuUsesBoundTerminalScreenSnapshot(final GameTestHelper helper) {
        final BlockPos rackPos = new BlockPos(1, 1, 1);
        helper.setBlock(rackPos, ModBlocks.RACK.get());
        final RackBlockEntity rack = helper.getBlockEntity(rackPos);
        rack.setItem(0, new ItemStack(ModItems.TERMINAL_SERVER.get()));
        final TerminalServerRackMountableEnvironment terminalServer = (TerminalServerRackMountableEnvironment) rack.getMountable(0);
        final ItemStack terminal = new ItemStack(ModItems.TERMINAL.get());

        helper.assertTrue(TerminalItem.bindToTerminalServer(terminal, rack, 0), "Terminal did not bind to terminal server");
        terminalServer.screen().set(0, 0, "menu", false);
        final li.cil.oc.common.menu.TerminalMenu menu = TerminalItem.createMenuForBoundTerminal(1, null, terminal);

        helper.assertTrue(menu != null, "Terminal item did not create menu for bound terminal");
        helper.assertTrue("menu".equals(menu.snapshot().line(0)), "Terminal menu did not include screen snapshot");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void terminalMenuInvalidatesWhenTerminalServerIsRemoved(final GameTestHelper helper) {
        final BlockPos rackPos = new BlockPos(1, 1, 1);
        helper.setBlock(rackPos, ModBlocks.RACK.get());
        final RackBlockEntity rack = helper.getBlockEntity(rackPos);
        rack.setItem(0, new ItemStack(ModItems.TERMINAL_SERVER.get()));
        final TerminalServerRackMountableEnvironment terminalServer = (TerminalServerRackMountableEnvironment) rack.getMountable(0);
        final ItemStack terminal = new ItemStack(ModItems.TERMINAL.get());

        helper.assertTrue(TerminalItem.bindToTerminalServer(terminal, rack, 0), "Terminal did not bind to terminal server");
        final li.cil.oc.common.menu.TerminalMenu menu = TerminalItem.createMenuForBoundTerminal(1, null, terminal);
        helper.assertTrue(menu != null, "Terminal item did not create menu for bound terminal");
        helper.assertTrue(menu.stillValid(null), "Fresh terminal menu was not valid");

        terminalServer.removeVirtualNodes();
        helper.assertTrue(!menu.stillValid(null), "Removed terminal server menu stayed valid");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void terminalItemCreatesScreenSnapshotPayloadForBoundTerminal(final GameTestHelper helper) {
        final BlockPos rackPos = new BlockPos(1, 1, 1);
        helper.setBlock(rackPos, ModBlocks.RACK.get());
        final RackBlockEntity rack = helper.getBlockEntity(rackPos);
        rack.setItem(0, new ItemStack(ModItems.TERMINAL_SERVER.get()));
        final TerminalServerRackMountableEnvironment terminalServer = (TerminalServerRackMountableEnvironment) rack.getMountable(0);
        final ItemStack terminal = new ItemStack(ModItems.TERMINAL.get());

        helper.assertTrue(TerminalItem.bindToTerminalServer(terminal, rack, 0), "Terminal did not bind to terminal server");
        terminalServer.screen().set(0, 0, "packet", false);
        final li.cil.oc.common.network.TerminalScreenSnapshotPayload payload = TerminalItem.createScreenSnapshotPayloadForBoundTerminal(9, terminal);

        helper.assertTrue(payload != null, "Terminal item did not create screen snapshot payload");
        helper.assertTrue(payload.containerId() == 9, "Terminal snapshot payload used wrong container id");
        helper.assertTrue("packet".equals(payload.snapshot().line(0)), "Terminal snapshot payload missed screen text");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void analyzerReportsRackTerminalServerVirtualNodes(final GameTestHelper helper) {
        final BlockPos rackPos = new BlockPos(1, 1, 1);
        helper.setBlock(rackPos, ModBlocks.RACK.get());
        final RackBlockEntity rack = helper.getBlockEntity(rackPos);
        rack.setItem(0, new ItemStack(ModItems.TERMINAL_SERVER.get()));

        final List<Component> lines = AnalyzerItem.describe(rack, Direction.NORTH);
        final String analysis = lines.stream().map(Component::getString).collect(java.util.stream.Collectors.joining("\n"));

        helper.assertTrue(analysis.contains("Component: screen"), "Analyzer did not report rack terminal screen:\n" + analysis);
        helper.assertTrue(analysis.contains("Component: keyboard"), "Analyzer did not report rack terminal keyboard:\n" + analysis);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void analyzerReportsRackServerMachineNode(final GameTestHelper helper) {
        final BlockPos rackPos = new BlockPos(1, 1, 1);
        helper.setBlock(rackPos, ModBlocks.RACK.get());
        final RackBlockEntity rack = helper.getBlockEntity(rackPos);
        rack.setItem(0, new ItemStack(ModItems.SERVER_TIER2.get()));

        final List<Component> lines = AnalyzerItem.describe(rack, Direction.NORTH);
        final String analysis = lines.stream().map(Component::getString).collect(java.util.stream.Collectors.joining("\n"));

        helper.assertTrue(analysis.contains("Component: computer"), "Analyzer did not report rack server machine node:\n" + analysis);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void analyzerReportsRackServerInternalComponents(final GameTestHelper helper) {
        final BlockPos rackPos = new BlockPos(1, 1, 1);
        helper.setBlock(rackPos, ModBlocks.RACK.get());
        final RackBlockEntity rack = helper.getBlockEntity(rackPos);
        rack.setItem(0, new ItemStack(ModItems.SERVER_TIER2.get()));
        final li.cil.oc.api.component.RackMountable mountable = rack.getMountable(0);
        helper.assertTrue(mountable instanceof li.cil.oc.api.internal.Server, "Rack did not create server mountable");
        final net.minecraft.world.Container serverInventory = (net.minecraft.world.Container) mountable;
        serverInventory.setItem(2, new ItemStack(ModItems.CPU_TIER3.get()));
        serverInventory.setItem(5, new ItemStack(ModItems.MEMORY_TIER3.get()));
        serverInventory.setItem(8, new ItemStack(ModItems.HDD_TIER3.get()));
        serverInventory.setItem(12, luaBiosEepromStack());

        final List<Component> lines = AnalyzerItem.describe(rack, Direction.NORTH);
        final String analysis = lines.stream().map(Component::getString).collect(java.util.stream.Collectors.joining("\n"));

        helper.assertTrue(analysis.contains("Component: filesystem"), "Analyzer did not report rack server filesystem:\n" + analysis);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void raidCreatesFilesystemWhenFilledWithHardDisks(final GameTestHelper helper) {
        final BlockPos raidPos = new BlockPos(1, 1, 1);
        helper.setBlock(raidPos, ModBlocks.RAID.get());
        final RaidBlockEntity raid = helper.getBlockEntity(raidPos);

        helper.assertTrue(raid.getContainerSize() == RaidBlockEntity.CONTAINER_SIZE, "RAID slot count mismatch");
        helper.assertTrue(raid.canPlaceItem(0, new ItemStack(ModItems.HDD_TIER1.get())), "RAID rejected tier 1 hard disk");
        helper.assertTrue(raid.canPlaceItem(1, new ItemStack(ModItems.HDD_TIER2.get())), "RAID rejected tier 2 hard disk");
        helper.assertTrue(raid.canPlaceItem(2, new ItemStack(ModItems.HDD_TIER3.get())), "RAID rejected tier 3 hard disk");
        helper.assertTrue(!raid.canPlaceItem(0, new ItemStack(ModItems.FLOPPY.get())), "RAID accepted a floppy");

        raid.setItem(0, new ItemStack(ModItems.HDD_TIER1.get()));
        raid.setItem(1, new ItemStack(ModItems.HDD_TIER2.get()));
        helper.assertTrue(AnalyzerItem.describe(raid, Direction.NORTH).isEmpty(), "Partial RAID exposed a filesystem");

        raid.setItem(2, new ItemStack(ModItems.HDD_TIER3.get()));
        final Node[] nodes = raid.onAnalyze(null, Direction.NORTH, 0, 0, 0);
        helper.assertTrue(nodes.length == 1 && nodes[0] instanceof li.cil.oc.api.network.Component, "Full RAID did not expose one filesystem node");
        final li.cil.oc.api.network.Component component = (li.cil.oc.api.network.Component) nodes[0];
        helper.assertTrue("filesystem".equals(component.name()), "RAID component mismatch: " + component.name());
        assertSingleResult(helper, invokeComponent(helper, component, "spaceTotal"), 7L * 1024L * 1024L, "RAID filesystem capacity");

        final List<Component> lines = AnalyzerItem.describe(raid, Direction.NORTH);
        final String analysis = lines.stream().map(Component::getString).collect(java.util.stream.Collectors.joining("\n"));
        helper.assertTrue(analysis.contains("Component: filesystem"), "Analyzer did not report RAID filesystem:\n" + analysis);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void raidMenuReportsFilesystemStatus(final GameTestHelper helper) {
        final BlockPos raidPos = new BlockPos(1, 1, 1);
        helper.setBlock(raidPos, ModBlocks.RAID.get());
        final RaidBlockEntity raid = helper.getBlockEntity(raidPos);

        helper.assertTrue(li.cil.oc.common.menu.RaidMenu.raidStateFor(raid) == li.cil.oc.common.menu.RaidMenu.STATE_EMPTY, "Empty RAID reported non-empty status");
        helper.assertTrue(li.cil.oc.common.menu.RaidMenu.raidCapacityFor(raid) == 0, "Empty RAID reported capacity");
        helper.assertTrue(li.cil.oc.common.menu.RaidMenu.raidDiskCountFor(raid) == 0, "Empty RAID reported disks");

        raid.setItem(0, new ItemStack(ModItems.HDD_TIER1.get()));
        helper.assertTrue(li.cil.oc.common.menu.RaidMenu.raidStateFor(raid) == li.cil.oc.common.menu.RaidMenu.STATE_INCOMPLETE, "Partial RAID did not report incomplete status");
        helper.assertTrue(li.cil.oc.common.menu.RaidMenu.raidCapacityFor(raid) == 1024 * 1024, "Partial RAID capacity mismatch");
        helper.assertTrue(li.cil.oc.common.menu.RaidMenu.raidDiskCountFor(raid) == 1, "Partial RAID disk count mismatch");

        raid.setItem(1, new ItemStack(ModItems.HDD_TIER2.get()));
        raid.setItem(2, new ItemStack(ModItems.HDD_TIER3.get()));
        helper.assertTrue(li.cil.oc.common.menu.RaidMenu.raidStateFor(raid) == li.cil.oc.common.menu.RaidMenu.STATE_READY, "Full RAID did not report ready status");
        helper.assertTrue(li.cil.oc.common.menu.RaidMenu.raidCapacityFor(raid) == 7 * 1024 * 1024, "Full RAID capacity mismatch");
        helper.assertTrue(li.cil.oc.common.menu.RaidMenu.raidDiskCountFor(raid) == 3, "Full RAID disk count mismatch");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void raidBlockItemRetainsDisksAndFilesystem(final GameTestHelper helper) {
        final BlockPos raidPos = new BlockPos(1, 1, 1);
        helper.setBlock(raidPos, ModBlocks.RAID.get());
        final RaidBlockEntity raid = helper.getBlockEntity(raidPos);
        raid.setItem(0, new ItemStack(ModItems.HDD_TIER1.get()));
        raid.setItem(1, new ItemStack(ModItems.HDD_TIER2.get()));
        raid.setItem(2, new ItemStack(ModItems.HDD_TIER3.get()));

        final li.cil.oc.api.network.Component component = (li.cil.oc.api.network.Component) raid.onAnalyze(null, Direction.NORTH, 0, 0, 0)[0];
        chargeConnector(helper, component, 1D);
        final Object[] open = invokeComponent(helper, component, "open", "persisted.txt", "w");
        invokeComponent(helper, component, "write", open[0], "kept".getBytes(StandardCharsets.UTF_8));
        invokeComponent(helper, component, "close", open[0]);

        final ItemStack saved = new ItemStack(ModItems.RAID.get());
        raid.saveToStack(saved, helper.getLevel().registryAccess());

        final BlockPos loadedPos = new BlockPos(3, 1, 1);
        helper.setBlock(loadedPos, ModBlocks.RAID.get());
        final RaidBlockEntity loaded = helper.getBlockEntity(loadedPos);
        loaded.loadFromStack(saved, helper.getLevel().registryAccess());

        helper.assertTrue(loaded.getItem(0).is(ModItems.HDD_TIER1.get()), "Loaded RAID missing tier 1 disk");
        helper.assertTrue(loaded.getItem(1).is(ModItems.HDD_TIER2.get()), "Loaded RAID missing tier 2 disk");
        helper.assertTrue(loaded.getItem(2).is(ModItems.HDD_TIER3.get()), "Loaded RAID missing tier 3 disk");
        final li.cil.oc.api.network.Component loadedComponent = (li.cil.oc.api.network.Component) loaded.onAnalyze(null, Direction.NORTH, 0, 0, 0)[0];
        assertSingleResult(helper, invokeComponent(helper, loadedComponent, "exists", "persisted.txt"), Boolean.TRUE, "Loaded RAID file");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void brokenRaidDropsStatefulRaidItem(final GameTestHelper helper) {
        final BlockPos raidPos = new BlockPos(1, 1, 1);

        helper.killAllEntities();
        helper.setBlock(raidPos, ModBlocks.RAID.get());
        final RaidBlockEntity raid = helper.getBlockEntity(raidPos);
        raid.setItem(0, new ItemStack(ModItems.HDD_TIER1.get()));
        raid.setItem(1, new ItemStack(ModItems.HDD_TIER2.get()));
        raid.setItem(2, new ItemStack(ModItems.HDD_TIER3.get()));
        final li.cil.oc.api.network.Component component = (li.cil.oc.api.network.Component) raid.onAnalyze(null, Direction.NORTH, 0, 0, 0)[0];
        chargeConnector(helper, component, 1D);
        final Object[] open = invokeComponent(helper, component, "open", "survived.txt", "w");
        invokeComponent(helper, component, "write", open[0], "kept".getBytes(StandardCharsets.UTF_8));
        invokeComponent(helper, component, "close", open[0]);

        helper.getLevel().destroyBlock(helper.absolutePos(raidPos), true);
        helper.runAtTickTime(1, () -> {
            helper.assertTrue(droppedItemCount(helper, ModItems.RAID.get()) == 1, "Broken RAID did not drop exactly one RAID item");
            helper.assertTrue(droppedItemCount(helper, ModItems.HDD_TIER1.get()) == 0, "Broken RAID dropped loose tier 1 HDD");
            helper.assertTrue(droppedItemCount(helper, ModItems.HDD_TIER2.get()) == 0, "Broken RAID dropped loose tier 2 HDD");
            helper.assertTrue(droppedItemCount(helper, ModItems.HDD_TIER3.get()) == 0, "Broken RAID dropped loose tier 3 HDD");
            final ItemStack dropped = droppedItemStack(helper, ModItems.RAID.get());
            final BlockPos loadedPos = new BlockPos(3, 1, 1);
            helper.setBlock(loadedPos, ModBlocks.RAID.get());
            final RaidBlockEntity loaded = helper.getBlockEntity(loadedPos);
            loaded.loadFromStack(dropped, helper.getLevel().registryAccess());
            helper.assertTrue(loaded.getItem(0).is(ModItems.HDD_TIER1.get()), "Dropped RAID item missing tier 1 disk");
            final li.cil.oc.api.network.Component loadedComponent = (li.cil.oc.api.network.Component) loaded.onAnalyze(null, Direction.NORTH, 0, 0, 0)[0];
            assertSingleResult(helper, invokeComponent(helper, loadedComponent, "exists", "survived.txt"), Boolean.TRUE, "Dropped RAID file");
            helper.succeed();
        });
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
                helper.assertTrue(stacksResult.length == 1 && stacksResult[0] instanceof ItemStackArrayValue, "Inventory controller did not expose item-stack array value");
                final ItemStackArrayValue stacks = (ItemStackArrayValue) stacksResult[0];
                helper.assertTrue(Integer.valueOf(27).equals(stacks.count(null, null)[0]), "Inventory controller all-stack value size mismatch");
                final Map<?, ?> stackMap = (Map<?, ?>) stacks.getAll(null, null)[0];
                final Object firstStack = stackMap.get(1);
                final Object secondStack = stackMap.get(2);
                helper.assertTrue(firstStack instanceof ItemStack first && first.getCount() == 4 && secondStack instanceof ItemStack second && second.getCount() == 1, "Inventory controller all-stack value mismatch");
            } catch (Exception e) {
                helper.fail("Inventory controller invocation failed: " + e.getMessage());
            }
        });
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void adapterSignUpgradeWritesAdjacentSign(final GameTestHelper helper) {
        final BlockPos computerPos = new BlockPos(0, 1, 1);
        final BlockPos adapterPos = new BlockPos(1, 1, 1);
        final BlockPos signPos = new BlockPos(2, 1, 1);

        helper.setBlock(computerPos, ModBlocks.COMPUTER_CASE_TIER1.get());
        helper.setBlock(adapterPos, ModBlocks.ADAPTER.get());
        helper.setBlock(signPos, Blocks.OAK_SIGN);

        final li.cil.oc.common.blockentity.AdapterBlockEntity adapter = helper.getBlockEntity(adapterPos);
        helper.assertTrue(adapter.canPlaceItem(0, new ItemStack(ModItems.SIGN_UPGRADE.get())), "Adapter rejected sign upgrade");
        adapter.setItem(0, new ItemStack(ModItems.SIGN_UPGRADE.get()));

        helper.succeedWhen(() -> {
            final ComputerCaseBlockEntity computer = helper.getBlockEntity(computerPos);
            final String address = componentAddress(computer, "sign");
            helper.assertTrue(address != null, "Adapter did not expose sign upgrade: " + computer.machine().components());
            ComponentConnector signComponent = null;
            for (final Node node : adapter.onAnalyze(null, Direction.WEST, 0.5F, 0.5F, 0.5F)) {
                if (node instanceof ComponentConnector component && "sign".equals(component.name())) {
                    signComponent = component;
                }
            }
            helper.assertTrue(signComponent != null, "Analyzer did not expose sign upgrade node");
            helper.assertTrue(signComponent.visibility() == Visibility.Network, "Adapter sign upgrade component should be network-visible");
            try {
                final int east = Direction.EAST.get3DDataValue();
                assertInvokeResult(helper, computer, address, "setValue", new Object[]{east, "adapter\nsign"}, "adapter\nsign\n\n");
                assertInvokeResult(helper, computer, address, "getValue", new Object[]{east}, "adapter\nsign\n\n");
            } catch (Exception e) {
                helper.fail("Sign upgrade invocation failed: " + e.getMessage());
            }
        });
    }

    @GameTest(template = "empty")
    public static void signUpgradeAddsSignTextToTabletUseData(final GameTestHelper helper) {
        final BlockPos signPos = new BlockPos(1, 1, 1);
        helper.setBlock(signPos, Blocks.OAK_SIGN);
        final SignBlockEntity sign = helper.getBlockEntity(signPos);
        SignText text = sign.getFrontText();
        text = text.setMessage(0, Component.literal("alpha"));
        text = text.setMessage(1, Component.literal("beta"));
        sign.setText(text, true);

        final DriverItem driver = Driver.driverFor(new ItemStack(ModItems.SIGN_UPGRADE.get()));
        helper.assertTrue(driver != null, "No driver for sign upgrade");
        final ManagedEnvironment environment = driver.createEnvironment(new ItemStack(ModItems.SIGN_UPGRADE.get()), new StaticPositionEnvironmentHost(helper, signPos));
        helper.assertTrue(environment != null, "No sign upgrade environment");

        final CompoundTag tabletData = new CompoundTag();
        environment.onMessage(new TestMessage(null, "tablet.use", new Object[]{
            tabletData,
            new ItemStack(ModItems.TABLET.get()),
            null,
            helper.absolutePos(signPos),
            Direction.NORTH,
            Float.valueOf(0.5F),
            Float.valueOf(0.5F),
            Float.valueOf(0.5F)
        }));

        helper.assertTrue("alpha\nbeta\n\n".equals(tabletData.getString("signText")), "Sign upgrade did not add sign text to tablet use data");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void signUpgradeForRotatableHostIsNeighborVisible(final GameTestHelper helper) {
        final DriverItem driver = Driver.driverFor(new ItemStack(ModItems.SIGN_UPGRADE.get()));
        helper.assertTrue(driver != null, "No driver for sign upgrade");
        final ManagedEnvironment environment = driver.createEnvironment(new ItemStack(ModItems.SIGN_UPGRADE.get()), new AgentTestHost(helper));
        helper.assertTrue(environment != null, "No sign upgrade environment for rotatable host");
        helper.assertTrue(environment.node() instanceof ComponentConnector, "Sign upgrade has no connector component node");
        final ComponentConnector component = (ComponentConnector) environment.node();
        helper.assertTrue("sign".equals(component.name()), "Sign upgrade component name mismatch");
        helper.assertTrue(component.visibility() == Visibility.Neighbors, "Rotatable sign upgrade component should be neighbor-visible");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void tabletItemAnalyzesBlockWithInstalledSignUpgrade(final GameTestHelper helper) {
        final BlockPos signPos = new BlockPos(1, 1, 1);
        helper.setBlock(signPos, Blocks.OAK_SIGN);
        final SignBlockEntity sign = helper.getBlockEntity(signPos);
        SignText text = sign.getFrontText();
        text = text.setMessage(0, Component.literal("tablet"));
        text = text.setMessage(1, Component.literal("scan"));
        sign.setText(text, true);

        final TabletItem tablet = ModItems.TABLET.get();
        final ItemStack stack = new ItemStack(tablet);
        tablet.setRunning(stack, true);
        tablet.setComponent(stack, 1, new ItemStack(ModItems.SIGN_UPGRADE.get()));

        final CompoundTag result = tablet.analyzeBlock(stack, helper.getLevel(), null, helper.absolutePos(signPos), Direction.NORTH, 0.5F, 0.5F, 0.5F);
        helper.assertTrue("tablet\nscan\n\n".equals(result.getString("signText")), "Tablet analysis did not collect sign text");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void tabletItemAnalyzesBlockWithInstalledNavigationUpgrade(final GameTestHelper helper) {
        final BlockPos targetPos = new BlockPos(3, 2, 4);
        final BlockPos absoluteTargetPos = helper.absolutePos(targetPos);
        helper.setBlock(targetPos, Blocks.STONE);

        final TabletItem tablet = ModItems.TABLET.get();
        final ItemStack stack = new ItemStack(tablet);
        tablet.setRunning(stack, true);
        tablet.setComponent(stack, 1, new ItemStack(ModItems.NAVIGATION_UPGRADE.get()));

        final CompoundTag result = tablet.analyzeBlock(stack, helper.getLevel(), null, absoluteTargetPos, Direction.NORTH, 0.5F, 0.5F, 0.5F);
        helper.assertTrue(result.getInt("posX") == absoluteTargetPos.getX(), "Tablet navigation analysis did not collect target X");
        helper.assertTrue(result.getInt("posY") == absoluteTargetPos.getY(), "Tablet navigation analysis did not collect target Y");
        helper.assertTrue(result.getInt("posZ") == absoluteTargetPos.getZ(), "Tablet navigation analysis did not collect target Z");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void tabletItemAnalyzesBlockWithInstalledBarcodeReaderUpgrade(final GameTestHelper helper) {
        final BlockPos geolyzerPos = new BlockPos(1, 1, 1);
        helper.setBlock(geolyzerPos, ModBlocks.GEOLYZER.get());
        final GeolyzerBlockEntity geolyzer = helper.getBlockEntity(geolyzerPos);

        final TabletItem tablet = ModItems.TABLET.get();
        final ItemStack stack = new ItemStack(tablet);
        tablet.setRunning(stack, true);
        tablet.setComponent(stack, 1, new ItemStack(ModItems.BARCODE_READER_UPGRADE.get()));

        final CompoundTag result = tablet.analyzeBlock(stack, helper.getLevel(), null, helper.absolutePos(geolyzerPos), Direction.NORTH, 0.5F, 0.5F, 0.5F);
        final ListTag analyzed = result.getList("analyzed", Tag.TAG_COMPOUND);
        helper.assertTrue(analyzed.size() == 1, "Barcode Reader did not report exactly one analyzed node");
        final CompoundTag nodeData = analyzed.getCompound(0);
        helper.assertTrue("geolyzer".equals(nodeData.getString("type")), "Barcode Reader did not report geolyzer node type");
        helper.assertTrue(geolyzer.node().address().equals(nodeData.getString("address")), "Barcode Reader did not report geolyzer node address");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void navigationFindWaypointsReportsWaypointRedstone(final GameTestHelper helper) {
        final BlockPos hostPos = new BlockPos(1, 1, 1);
        final BlockPos waypointPos = new BlockPos(3, 1, 1);
        helper.setBlock(hostPos, Blocks.STONE);
        helper.setBlock(waypointPos, ModBlocks.WAYPOINT.get());
        helper.setBlock(waypointPos.relative(Direction.EAST), Blocks.REDSTONE_BLOCK);

        final ItemStack stack = new ItemStack(ModItems.NAVIGATION_UPGRADE.get());
        final DriverItem driver = Driver.driverFor(stack);
        helper.assertTrue(driver != null, "No driver for navigation upgrade");
        final ManagedEnvironment environment = driver.createEnvironment(stack, new StaticPositionEnvironmentHost(helper, hostPos));
        helper.assertTrue(environment.node() instanceof li.cil.oc.api.network.Component, "Navigation upgrade has no component node");
        helper.assertTrue(environment.node() instanceof ComponentConnector, "Navigation upgrade has no connector node");
        final ComponentConnector connector = (ComponentConnector) environment.node();
        connector.setLocalBufferSize(1D);
        connector.changeBuffer(1D);

        final Object[] result = invokeComponent(helper, (li.cil.oc.api.network.Component) environment.node(), "findWaypoints", 8D);
        helper.assertTrue(result.length == 1 && result[0] instanceof Map[], "Navigation upgrade did not return waypoint list");
        final Map[] waypoints = (Map[]) result[0];
        helper.assertTrue(waypoints.length == 1, "Navigation upgrade did not find exactly one waypoint");
        final Object[] position = (Object[]) waypoints[0].get("position");
        helper.assertTrue(Double.valueOf(2D).equals(position[0]), "Navigation waypoint X target mismatch: " + java.util.Arrays.toString(position));
        helper.assertTrue(Double.valueOf(0D).equals(position[1]), "Navigation waypoint Y target mismatch: " + java.util.Arrays.toString(position));
        helper.assertTrue(Double.valueOf(-1D).equals(position[2]), "Navigation waypoint Z target mismatch: " + java.util.Arrays.toString(position));
        helper.assertTrue(Integer.valueOf(15).equals(waypoints[0].get("redstone")), "Navigation waypoint redstone mismatch: " + waypoints[0]);
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void motionSensorRequiresLineOfSight(final GameTestHelper helper) {
        final BlockPos computerPos = new BlockPos(0, 1, 1);
        final BlockPos sensorPos = new BlockPos(1, 1, 1);
        final BlockPos wallBase = new BlockPos(2, 1, 1);
        final BlockPos villagerPos = new BlockPos(3, 1, 1);

        helper.setBlock(computerPos, ModBlocks.COMPUTER_CASE_TIER1.get());
        helper.setBlock(sensorPos, ModBlocks.MOTION_SENSOR.get());
        helper.setBlock(wallBase, Blocks.STONE);
        helper.setBlock(wallBase.above(), Blocks.STONE);

        final Villager villager = EntityType.VILLAGER.create(helper.getLevel());
        helper.assertTrue(villager != null, "Villager did not spawn");
        final BlockPos absoluteVillagerPos = helper.absolutePos(villagerPos);
        villager.setNoAi(true);
        villager.moveTo(absoluteVillagerPos.getX() + 0.5D, absoluteVillagerPos.getY(), absoluteVillagerPos.getZ() + 0.5D, 0, 0);
        helper.getLevel().addFreshEntity(villager);

        final ComputerCaseBlockEntity computer = helper.getBlockEntity(computerPos);
        startSignalComputer(helper, computer);

        helper.runAtTickTime(2, () ->
            helper.assertTrue(componentAddress(computer, "motion_sensor") != null, "Motion sensor component is not visible: " + computer.machine().components()));
        helper.runAtTickTime(20, () -> {
            for (int attempt = 0; attempt < 16; attempt++) {
                final Signal signal = computer.machine().popSignal();
                if (signal == null) {
                    break;
                }
                helper.assertTrue(!"motion".equals(signal.name()), "Motion sensor detected entity through wall");
            }
            helper.succeed();
        });
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void motionSensorSignalIncludesEntityName(final GameTestHelper helper) {
        final BlockPos computerPos = new BlockPos(0, 1, 1);
        final BlockPos sensorPos = new BlockPos(1, 1, 1);
        final BlockPos villagerPos = new BlockPos(3, 1, 1);

        helper.setBlock(computerPos, ModBlocks.COMPUTER_CASE_TIER1.get());
        helper.setBlock(sensorPos, ModBlocks.MOTION_SENSOR.get());

        final ComputerCaseBlockEntity computer = helper.getBlockEntity(computerPos);
        startSignalComputer(helper, computer);

        helper.runAtTickTime(2, () -> {
            helper.assertTrue(componentAddress(computer, "motion_sensor") != null, "Motion sensor component is not visible: " + computer.machine().components());
            final Villager villager = EntityType.VILLAGER.create(helper.getLevel());
            helper.assertTrue(villager != null, "Villager did not spawn");
            final BlockPos absoluteVillagerPos = helper.absolutePos(villagerPos);
            villager.setNoAi(true);
            villager.setCustomName(Component.literal("MotionTarget"));
            villager.moveTo(absoluteVillagerPos.getX() + 0.5D, absoluteVillagerPos.getY(), absoluteVillagerPos.getZ() + 0.5D, 0, 0);
            helper.getLevel().addFreshEntity(villager);
            final MotionSensorBlockEntity sensor = helper.getBlockEntity(sensorPos);
            sendMotionSignalNow(sensor, helper.absolutePos(sensorPos), villager);
        });
        helper.runAtTickTime(3, () -> {
            final String sensorAddress = componentAddress(computer, "motion_sensor");
            helper.assertTrue(sensorAddress != null, "Motion sensor component is not visible: " + computer.machine().components());
            assertNextSignal(helper, computer, "motion", sensorAddress, 2D, -0.5D, 0D, "MotionTarget");
            helper.succeed();
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
                helper.assertTrue(stacksResult.length == 1 && stacksResult[0] instanceof ItemStackArrayValue, "getAllStacks did not return an item-stack array value");
                final ItemStackArrayValue stacks = (ItemStackArrayValue) stacksResult[0];
                helper.assertTrue(Integer.valueOf(27).equals(stacks.count(null, null)[0]), "getAllStacks returned wrong chest size");
                final Map<?, ?> stackMap = (Map<?, ?>) stacks.getAll(null, null)[0];
                final Object firstStack = stackMap.get(1);
                helper.assertTrue(firstStack instanceof ItemStack stack && stack.is(net.minecraft.world.item.Items.DIAMOND) && stack.getCount() == 3, "getAllStacks did not include first slot diamonds");
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
    public static void adapterChestHotplugSignalsReachComputer(final GameTestHelper helper) {
        final BlockPos computerPos = new BlockPos(0, 1, 1);
        final BlockPos adapterPos = new BlockPos(1, 1, 1);
        final BlockPos chestPos = new BlockPos(2, 1, 1);

        helper.setBlock(computerPos, ModBlocks.COMPUTER_CASE_TIER1.get());
        helper.setBlock(adapterPos, ModBlocks.ADAPTER.get());
        final ComputerCaseBlockEntity computer = helper.getBlockEntity(computerPos);
        startSignalComputer(helper, computer);
        computer.machine().popSignal();

        final AtomicReference<String> address = new AtomicReference<>();
        helper.setBlock(chestPos, Blocks.CHEST);
        final net.minecraft.world.Container chest = helper.getBlockEntity(chestPos);
        chest.setItem(0, new ItemStack(Items.DIAMOND, 2));

        helper.runAtTickTime(2, () -> {
            address.set(componentAddress(computer, "inventory"));
            helper.assertTrue(address.get() != null, "Adapter did not expose hotplugged chest inventory: " + computer.machine().components());
            assertNextSignal(helper, computer, "component_added", address.get(), "inventory");
            helper.setBlock(chestPos, Blocks.AIR);
        });
        helper.runAtTickTime(4, () -> {
            assertNextSignal(helper, computer, "component_removed", address.get(), "inventory");
            helper.succeed();
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

    @GameTest(template = "empty")
    public static void analyzerReportsDiskDriveFilesystemNode(final GameTestHelper helper) {
        final BlockPos diskDrivePos = new BlockPos(1, 1, 1);
        helper.setBlock(diskDrivePos, ModBlocks.DISK_DRIVE.get());
        final DiskDriveBlockEntity diskDrive = helper.getBlockEntity(diskDrivePos);
        diskDrive.setItem(DiskDriveBlockEntity.SLOT_FLOPPY, openOsFloppyStack());

        final List<Component> lines = AnalyzerItem.describe(diskDrive, Direction.NORTH);
        final String analysis = lines.stream().map(Component::getString).collect(java.util.stream.Collectors.joining("\n"));

        helper.assertTrue(analysis.contains("Component: filesystem"), "Analyzer did not report inserted floppy filesystem:\n" + analysis);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void diskDriveExposesComponentCallbacks(final GameTestHelper helper) {
        final BlockPos diskDrivePos = new BlockPos(1, 1, 1);
        helper.setBlock(diskDrivePos, ModBlocks.DISK_DRIVE.get());
        final DiskDriveBlockEntity diskDrive = helper.getBlockEntity(diskDrivePos);
        final Map<String, String> metadata = diskDrive.getDeviceInfo();
        helper.assertTrue(DeviceInfo.DeviceClass.Disk.equals(metadata.get(DeviceInfo.DeviceAttribute.Class)), "Disk drive device class mismatch");
        helper.assertTrue("Floppy disk drive".equals(metadata.get(DeviceInfo.DeviceAttribute.Description)), "Disk drive description mismatch");
        helper.assertTrue("Spinner 520p1".equals(metadata.get(DeviceInfo.DeviceAttribute.Product)), "Disk drive product mismatch");

        helper.assertTrue(diskDrive.node() instanceof li.cil.oc.api.network.Component, "Disk drive has no component node");
        final li.cil.oc.api.network.Component component = (li.cil.oc.api.network.Component) diskDrive.node();
        assertSingleResult(helper, invokeComponent(helper, component, "isEmpty"), Boolean.TRUE, "Disk drive empty state before insert");

        diskDrive.setItem(DiskDriveBlockEntity.SLOT_FLOPPY, openOsFloppyStack());
        assertSingleResult(helper, invokeComponent(helper, component, "isEmpty"), Boolean.FALSE, "Disk drive empty state after insert");
        final Object[] media = invokeComponent(helper, component, "media");
        helper.assertTrue(media.length == 1 && media[0] instanceof String address && !address.isEmpty(), "Disk drive did not report media address");

        assertSingleResult(helper, invokeComponent(helper, component, "eject", 0D), Boolean.TRUE, "Disk drive eject");
        assertSingleResult(helper, invokeComponent(helper, component, "isEmpty"), Boolean.TRUE, "Disk drive empty state after eject");
        assertDroppedItem(helper, ModItems.FLOPPY.get());
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void diskDriveEjectUsesBlockFacing(final GameTestHelper helper) throws Exception {
        final BlockPos diskDrivePos = new BlockPos(1, 1, 1);
        helper.setBlock(diskDrivePos, ModBlocks.DISK_DRIVE.get().defaultBlockState().setValue(DiskDriveBlock.FACING, Direction.EAST));
        final DiskDriveBlockEntity diskDrive = helper.getBlockEntity(diskDrivePos);
        diskDrive.setItem(DiskDriveBlockEntity.SLOT_FLOPPY, openOsFloppyStack());

        final li.cil.oc.api.network.Component component = (li.cil.oc.api.network.Component) diskDrive.node();
        assertSingleResult(helper, invokeComponent(helper, component, "eject", 1D), Boolean.TRUE, "Disk drive eject");
        final ItemEntity entity = droppedItemEntity(helper, ModItems.FLOPPY.get());
        helper.assertTrue(entity.getDeltaMovement().x > 0.5D, "Ejected disk did not move along facing direction: " + entity.getDeltaMovement());
        helper.assertTrue(Math.abs(entity.getDeltaMovement().z) < 0.001D, "Ejected disk kept north/south velocity: " + entity.getDeltaMovement());
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void diskDriveMediaHotplugSignalsReachComputer(final GameTestHelper helper) {
        final BlockPos computerPos = new BlockPos(1, 1, 1);
        final BlockPos diskDrivePos = new BlockPos(2, 1, 1);
        helper.setBlock(computerPos, ModBlocks.COMPUTER_CASE_TIER1.get());
        helper.setBlock(diskDrivePos, ModBlocks.DISK_DRIVE.get());
        final ComputerCaseBlockEntity computer = helper.getBlockEntity(computerPos);
        final DiskDriveBlockEntity diskDrive = helper.getBlockEntity(diskDrivePos);
        startSignalComputer(helper, computer);
        computer.machine().popSignal();

        diskDrive.setItem(DiskDriveBlockEntity.SLOT_FLOPPY, openOsFloppyStack());
        final Object[] media = invokeComponent(helper, (li.cil.oc.api.network.Component) diskDrive.node(), "media");
        helper.assertTrue(media.length == 1 && media[0] instanceof String address && !address.isEmpty(), "Disk drive did not report inserted media address");
        final String mediaAddress = (String) media[0];

        assertNextSignal(helper, computer, "component_added", mediaAddress, "filesystem");

        invokeComponent(helper, (li.cil.oc.api.network.Component) diskDrive.node(), "eject", 0D);

        assertNextSignal(helper, computer, "component_removed", mediaAddress, "filesystem");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void diskDriveMenuReportsMediaState(final GameTestHelper helper) {
        final BlockPos diskDrivePos = new BlockPos(1, 1, 1);
        helper.setBlock(diskDrivePos, ModBlocks.DISK_DRIVE.get());
        final DiskDriveBlockEntity diskDrive = helper.getBlockEntity(diskDrivePos);

        helper.assertTrue(DiskDriveMenu.mediaStateFor(diskDrive) == DiskDriveMenu.STATE_EMPTY, "Empty disk drive did not report empty media state");

        diskDrive.setItem(DiskDriveBlockEntity.SLOT_FLOPPY, openOsFloppyStack());
        helper.assertTrue(DiskDriveMenu.mediaStateFor(diskDrive) == DiskDriveMenu.STATE_LOADED, "Loaded disk drive did not report loaded media state");

        diskDrive.removeItem(DiskDriveBlockEntity.SLOT_FLOPPY, 1);
        helper.assertTrue(DiskDriveMenu.mediaStateFor(diskDrive) == DiskDriveMenu.STATE_EMPTY, "Ejected disk drive did not report empty media state");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void analyzerReportsAdapterInstalledUpgradeNode(final GameTestHelper helper) {
        final BlockPos adapterPos = new BlockPos(1, 1, 1);
        helper.setBlock(adapterPos, ModBlocks.ADAPTER.get());
        final AdapterBlockEntity adapter = helper.getBlockEntity(adapterPos);
        adapter.setItem(0, new ItemStack(ModItems.INVENTORY_CONTROLLER_UPGRADE.get()));

        final List<Component> lines = AnalyzerItem.describe(adapter, Direction.NORTH);
        final String analysis = lines.stream().map(Component::getString).collect(java.util.stream.Collectors.joining("\n"));

        helper.assertTrue(analysis.contains("Component: inventory_controller"), "Analyzer did not report adapter installed upgrade:\n" + analysis);
        helper.succeed();
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

    @GameTest(template = "empty", timeoutTicks = 360)
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
        helper.succeedWhen(() -> {
            helper.assertTrue(screenText(screen).contains("zz"), "OpenOS terminal did not echo keyboard input:\n" + screenText(screen));
        });
    }

    @GameTest(template = "empty", timeoutTicks = 2200)
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
        final String command = "echo ocok";
        for (int index = 0; index < command.length(); index++) {
            final char character = command.charAt(index);
            helper.runAtTickTime(120L + index * 80L, () -> screen.keyDown(character, keyCode(character), null));
        }
        final AtomicBoolean submitted = new AtomicBoolean(false);
        final AtomicInteger checksAfterSubmit = new AtomicInteger(0);
        helper.succeedWhen(() -> {
            final String text = screenText(screen);
            if (!submitted.get() && text.contains("/home # " + command)) {
                submitted.set(true);
                typeKey(screen, '\r', 0x1C);
                helper.assertTrue(false, "OpenOS terminal command submitted; waiting for output:\n" + text);
            }
            if (submitted.get()) {
                final int checks = checksAfterSubmit.incrementAndGet();
                helper.assertTrue(checks >= 20, "OpenOS terminal command output not checked until terminal has advanced:\n" + text);
            }
            helper.assertTrue(countOccurrences(text, "ocok") >= 2, "OpenOS terminal did not run typed echo command:\n" + text);
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

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void computerCaseMenuReportsMachineStatus(final GameTestHelper helper) {
        final BlockPos computerPos = new BlockPos(1, 1, 1);

        helper.setBlock(computerPos, ModBlocks.COMPUTER_CASE_TIER1.get());

        final ComputerCaseBlockEntity computer = helper.getBlockEntity(computerPos);
        helper.assertTrue(ComputerCaseMenu.computerStateFor(computer) == ComputerCaseMenu.STATE_INCOMPLETE, "Empty computer case did not report incomplete status");
        helper.assertTrue(ComputerCaseMenu.missingRequirementsFor(computer) == (ComputerCaseMenu.MISSING_CPU | ComputerCaseMenu.MISSING_MEMORY | ComputerCaseMenu.MISSING_EEPROM), "Empty computer case missing mask mismatch");

        computer.setItem(ComputerCaseBlockEntity.SLOT_CPU, new ItemStack(ModItems.CPU_TIER1.get()));
        computer.setItem(ComputerCaseBlockEntity.SLOT_MEMORY_0, new ItemStack(ModItems.MEMORY_TIER1.get()));
        computer.setItem(ComputerCaseBlockEntity.SLOT_EEPROM, luaBiosEepromStack());

        helper.assertTrue(ComputerCaseMenu.missingRequirementsFor(computer) == 0, "Complete computer case reported missing requirements");
        helper.assertTrue(ComputerCaseMenu.computerStateFor(computer) == ComputerCaseMenu.STATE_READY, "Complete stopped computer case did not report ready status");
        helper.assertTrue(computer.toggleMachine(), "Computer case did not start with required components");
        helper.assertTrue(ComputerCaseMenu.computerStateFor(computer) == ComputerCaseMenu.STATE_RUNNING, "Running computer case did not report running status");
        helper.succeed();
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

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void redstoneWakeThresholdStartsComputer(final GameTestHelper helper) {
        final BlockPos computerPos = new BlockPos(1, 1, 1);

        helper.setBlock(computerPos, ModBlocks.COMPUTER_CASE_TIER1.get());

        final ComputerCaseBlockEntity computer = helper.getBlockEntity(computerPos);
        computer.setItem(ComputerCaseBlockEntity.SLOT_CARD_0, new ItemStack(ModItems.REDSTONE_CARD.get()));
        computer.setItem(ComputerCaseBlockEntity.SLOT_CPU, new ItemStack(ModItems.CPU_TIER1.get()));
        computer.setItem(ComputerCaseBlockEntity.SLOT_MEMORY_0, new ItemStack(ModItems.MEMORY_TIER1.get()));
        computer.setItem(ComputerCaseBlockEntity.SLOT_HDD, bootableHardDiskStack(helper, "while true do computer.pullSignal(1) end"));
        computer.setItem(ComputerCaseBlockEntity.SLOT_EEPROM, luaBiosEepromStack());

        final String redstoneAddress = componentAddress(computer, "redstone");
        helper.assertTrue(redstoneAddress != null, "Redstone component is not visible: " + computer.machine().components());
        try {
            assertSingleResult(helper, computer.machine().invoke(redstoneAddress, "setWakeThreshold", new Object[]{15}), 0, "setWakeThreshold");
            assertSingleResult(helper, computer.machine().invoke(redstoneAddress, "getWakeThreshold", new Object[0]), 15, "getWakeThreshold");
        } catch (Exception e) {
            helper.fail("Failed to configure redstone wake threshold: " + e.getMessage());
        }

        helper.assertTrue(!computer.machine().isRunning(), "Computer started before redstone threshold crossing");
        helper.runAtTickTime(20, () -> helper.setBlock(computerPos.east(), Blocks.REDSTONE_BLOCK));
        helper.runAtTickTime(60, () -> {
            helper.assertTrue(computer.machine().isRunning(), "Computer did not wake from redstone threshold crossing");
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
        startSignalComputer(helper, computer);
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
        startSignalComputer(helper, computer);

        final BlockState state = helper.getBlockState(screenPos);
        final BlockPos absoluteScreenPos = helper.absolutePos(screenPos);
        final Vec3 hitLocation = new Vec3(absoluteScreenPos.getX() + 0.25D, absoluteScreenPos.getY() + 0.75D, absoluteScreenPos.getZ());
        final BlockHitResult hit = new BlockHitResult(hitLocation, Direction.NORTH, absoluteScreenPos, false);
        helper.assertTrue(invokeUseWithoutItem(state, helper, screenPos, hit) == InteractionResult.CONSUME, "Screen click was not consumed");

        helper.runAtTickTime(5, () -> {
            assertNextSignal(helper, computer, "touch", screen.node().address(), 13, 5, 0);
            assertNextSignal(helper, computer, "drop", screen.node().address(), 13, 5, 0);
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

    private static void startSignalComputer(final GameTestHelper helper, final ComputerCaseBlockEntity computer) {
        computer.setItem(ComputerCaseBlockEntity.SLOT_CPU, new ItemStack(ModItems.CPU_TIER1.get()));
        computer.setItem(ComputerCaseBlockEntity.SLOT_MEMORY_0, new ItemStack(ModItems.MEMORY_TIER1.get()));
        computer.setItem(ComputerCaseBlockEntity.SLOT_EEPROM, new ItemStack(ModItems.EEPROM.get()));
        helper.assertTrue(computer.toggleMachine(), "Computer did not start with CPU, memory, and EEPROM");
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
        chargeConnector(helper, environment.node(), 1D);
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

    private static void assertEnvironmentProvider(final GameTestHelper helper, final ItemStack stack, final Class<?> expectedEnvironment) {
        final Class<?> environment = Driver.environmentFor(stack);
        helper.assertTrue(expectedEnvironment.equals(environment), "Expected " + stack + " to provide " + expectedEnvironment.getSimpleName() + " but got " + environment);
    }

    private static boolean reachableComponent(final Node node, final String name) {
        if (node == null) {
            return false;
        }
        for (final Node reachable : node.reachableNodes()) {
            if (reachable instanceof li.cil.oc.api.network.Component component && name.equals(component.name())) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsBehavior(final Iterable<li.cil.oc.api.nanomachines.Behavior> behaviors, final li.cil.oc.api.nanomachines.Behavior behavior) {
        for (final li.cil.oc.api.nanomachines.Behavior candidate : behaviors) {
            if (candidate == behavior) {
                return true;
            }
        }
        return false;
    }

    private static li.cil.oc.api.nanomachines.Behavior nanomachineBehaviorByName(final Iterable<li.cil.oc.api.nanomachines.Behavior> behaviors, final String name) {
        for (final li.cil.oc.api.nanomachines.Behavior behavior : behaviors) {
            if (name.equals(behavior.getNameHint())) {
                return behavior;
            }
        }
        throw new IllegalArgumentException("Missing nanomachine behavior " + name);
    }

    private static CompoundTag nanomachineConfigurationTag(final int triggerInput) {
        final CompoundTag configuration = new CompoundTag();
        configuration.put("connectors", new ListTag());
        final ListTag behaviors = new ListTag();
        final CompoundTag behavior = new CompoundTag();
        behavior.put("behavior", new CompoundTag());
        behavior.putIntArray("triggerInputs", new int[]{triggerInput});
        behavior.putIntArray("connectorInputs", new int[0]);
        behaviors.add(behavior);
        configuration.put("behaviors", behaviors);
        return configuration;
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

    private static boolean hasModForcedTickingChunk(final GameTestHelper helper, final long chunk) {
        final ForcedChunksSavedData data = helper.getLevel().getDataStorage().computeIfAbsent(ForcedChunksSavedData.factory(), ForcedChunksSavedData.FILE_ID);
        for (final LongSet chunks : data.getBlockForcedChunks().getTickingChunks().values()) {
            if (chunks.contains(chunk)) {
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
        helper.assertTrue(Double.compare(0.2D, connector.localBuffer()) == 0, "Solar generator did not use upstream default efficiency");
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

    private static void assertScreenItemDriver(final GameTestHelper helper, final ItemStack stack, final int tier, final int width, final int height, final TextBuffer.ColorDepth depth) {
        final DriverItem driver = Driver.driverFor(stack);
        helper.assertTrue(driver != null, "No driver for " + stack);
        helper.assertTrue(Slot.Upgrade.equals(driver.slot(stack)), "Expected screen item upgrade slot for " + stack);
        helper.assertTrue(driver.tier(stack) == tier, "Expected screen tier " + tier + " for " + stack + " but got " + driver.tier(stack));
        final ManagedEnvironment environment = driver.createEnvironment(stack, null);
        helper.assertTrue(environment instanceof TextBuffer, "Screen item did not create text buffer environment for " + stack);
        helper.assertTrue(environment.node() instanceof li.cil.oc.api.network.Component, "Screen item has no component node for " + stack);
        final li.cil.oc.api.network.Component component = (li.cil.oc.api.network.Component) environment.node();
        helper.assertTrue("screen".equals(component.name()), "Screen item component name mismatch for " + stack);
        final TextBuffer buffer = (TextBuffer) environment;
        helper.assertTrue(buffer.getMaximumWidth() == width, "Expected screen item max width " + width + " but got " + buffer.getMaximumWidth());
        helper.assertTrue(buffer.getMaximumHeight() == height, "Expected screen item max height " + height + " but got " + buffer.getMaximumHeight());
        helper.assertTrue(buffer.getMaximumColorDepth() == depth, "Expected screen item depth " + depth + " but got " + buffer.getMaximumColorDepth());
    }

    private static void assertKeyboardItemDriver(final GameTestHelper helper, final ItemStack stack) {
        final DriverItem driver = Driver.driverFor(stack);
        helper.assertTrue(driver != null, "No driver for " + stack);
        helper.assertTrue(Slot.Upgrade.equals(driver.slot(stack)), "Expected keyboard item upgrade slot for " + stack);
        final ManagedEnvironment environment = driver.createEnvironment(stack, null);
        helper.assertTrue(environment instanceof li.cil.oc.api.internal.Keyboard, "Keyboard item did not create keyboard environment for " + stack);
        helper.assertTrue(environment.node() instanceof li.cil.oc.api.network.Component, "Keyboard item has no component node for " + stack);
        final li.cil.oc.api.network.Component component = (li.cil.oc.api.network.Component) environment.node();
        helper.assertTrue("keyboard".equals(component.name()), "Keyboard item component name mismatch for " + stack);
        helper.assertTrue(environment instanceof DeviceInfo, "Keyboard item environment lacks device info");
        final DeviceInfo info = (DeviceInfo) environment;
        helper.assertTrue(DeviceInfo.DeviceClass.Input.equals(info.getDeviceInfo().get(DeviceInfo.DeviceAttribute.Class)), "Keyboard item device class mismatch");
    }

    private static void assertMotionSensorItemDriver(final GameTestHelper helper, final ItemStack stack) {
        final DriverItem driver = Driver.driverFor(stack);
        helper.assertTrue(driver != null, "No driver for " + stack);
        helper.assertTrue(Slot.Upgrade.equals(driver.slot(stack)), "Expected motion sensor item upgrade slot for " + stack);
        helper.assertTrue(driver.tier(stack) == 1, "Expected motion sensor tier 1 for " + stack + " but got " + driver.tier(stack));
        final ManagedEnvironment environment = driver.createEnvironment(stack, new StaticEnvironmentHost(helper));
        helper.assertTrue(environment != null, "Motion sensor item did not create environment for " + stack);
        helper.assertTrue(environment.node() instanceof li.cil.oc.api.network.Component, "Motion sensor item has no component node for " + stack);
        final li.cil.oc.api.network.Component component = (li.cil.oc.api.network.Component) environment.node();
        helper.assertTrue("motion_sensor".equals(component.name()), "Motion sensor item component name mismatch for " + stack);
        final Object[] sensitivity = invokeComponent(helper, component, "getSensitivity");
        helper.assertTrue(sensitivity.length == 1 && Double.valueOf(0.4D).equals(sensitivity[0]), "Motion sensor default sensitivity mismatch");
        final Object[] oldSensitivity = invokeComponent(helper, component, "setSensitivity", 0.1D);
        helper.assertTrue(oldSensitivity.length == 1 && Double.valueOf(0.4D).equals(oldSensitivity[0]), "Motion sensor did not return old sensitivity");
        final Object[] clampedSensitivity = invokeComponent(helper, component, "getSensitivity");
        helper.assertTrue(clampedSensitivity.length == 1 && Double.valueOf(0.2D).equals(clampedSensitivity[0]), "Motion sensor did not clamp sensitivity");
    }

    private static void assertGeolyzerItemDriver(final GameTestHelper helper, final ItemStack stack) {
        final DriverItem driver = Driver.driverFor(stack);
        helper.assertTrue(driver != null, "No driver for " + stack);
        helper.assertTrue(Slot.Upgrade.equals(driver.slot(stack)), "Expected geolyzer item upgrade slot for " + stack);
        helper.assertTrue(driver.tier(stack) == 0, "Expected geolyzer tier 0 for " + stack + " but got " + driver.tier(stack));
        final ManagedEnvironment environment = driver.createEnvironment(stack, new StaticEnvironmentHost(helper));
        helper.assertTrue(environment != null, "Geolyzer item did not create environment for " + stack);
        helper.assertTrue(environment.node() instanceof ComponentConnector, "Geolyzer item has no connector component node for " + stack);
        final ComponentConnector component = (ComponentConnector) environment.node();
        helper.assertTrue("geolyzer".equals(component.name()), "Geolyzer item component name mismatch for " + stack);
        final Object[] noEnergy = invokeComponent(helper, component, "scan", 0, 0, 0, 1, 1, 1);
        assertNoEnergy(helper, noEnergy, "Geolyzer item scan");
        component.setLocalBufferSize(10D);
        component.changeBuffer(10D);
        final Object[] scan = invokeComponent(helper, component, "scan", 0, 0, 0, 1, 1, 1);
        helper.assertTrue(scan.length == 1 && scan[0] instanceof float[], "Geolyzer item scan did not return data");
        helper.assertTrue(Double.compare(0D, component.localBuffer()) == 0, "Geolyzer item scan did not consume energy");
    }

    private static void assertTransposerItemDriver(final GameTestHelper helper, final ItemStack stack) {
        final DriverItem driver = Driver.driverFor(stack);
        helper.assertTrue(driver != null, "No driver for " + stack);
        helper.assertTrue(Slot.Upgrade.equals(driver.slot(stack)), "Expected transposer item upgrade slot for " + stack);
        helper.assertTrue(driver.tier(stack) == 0, "Expected transposer tier 0 for " + stack + " but got " + driver.tier(stack));
        final BlockPos hostPos = new BlockPos(2, 1, 2);
        helper.setBlock(hostPos.relative(Direction.EAST), Blocks.CHEST);
        final ManagedEnvironment environment = driver.createEnvironment(stack, new StaticPositionEnvironmentHost(helper, hostPos));
        helper.assertTrue(environment != null, "Transposer item did not create environment for " + stack);
        helper.assertTrue(environment.node() instanceof ComponentConnector, "Transposer item has no connector component node for " + stack);
        final ComponentConnector component = (ComponentConnector) environment.node();
        helper.assertTrue("transposer".equals(component.name()), "Transposer item component name mismatch for " + stack);
        helper.assertTrue(component.visibility() == Visibility.Neighbors, "Transposer item component should be neighbor-visible");
        final Object[] size = invokeComponent(helper, component, "getInventorySize", Direction.EAST.get3DDataValue());
        helper.assertTrue(size.length == 1 && Integer.valueOf(27).equals(size[0]), "Transposer item did not inspect adjacent chest");
    }

    private static void assertRackMountableDriver(final GameTestHelper helper, final ItemStack stack, final int tier) {
        final DriverItem driver = Driver.driverFor(stack);
        helper.assertTrue(driver != null, "No rack mountable driver for " + stack);
        helper.assertTrue(Slot.RackMountable.equals(driver.slot(stack)), "Expected rack mountable slot for " + stack);
        helper.assertTrue(driver.tier(stack) == tier, "Expected rack mountable tier " + tier + " for " + stack + " but got " + driver.tier(stack));
        final ManagedEnvironment environment = driver.createEnvironment(stack, new StaticEnvironmentHost(helper));
        helper.assertTrue(environment instanceof li.cil.oc.api.component.RackMountable, "Driver did not create rack mountable for " + stack);
        helper.assertTrue(environment.node() != null, "Rack mountable has no node for " + stack);
    }

    private static void assertComponentBusDriver(final GameTestHelper helper, final ItemStack stack, final int tier, final int supportedComponents) {
        final DriverItem driver = Driver.driverFor(stack);
        helper.assertTrue(driver instanceof Processor, "No component bus processor driver for " + stack);
        helper.assertTrue(Slot.ComponentBus.equals(driver.slot(stack)), "Expected component bus slot for " + stack);
        helper.assertTrue(driver.tier(stack) == tier, "Expected component bus tier " + tier + " for " + stack + " but got " + driver.tier(stack));
        helper.assertTrue(((Processor) driver).supportedComponents(stack) == supportedComponents, "Expected component bus supported component count " + supportedComponents);
        helper.assertTrue(((Processor) driver).architecture(stack) == null, "Component bus should not provide an architecture");
        helper.assertTrue(driver.createEnvironment(stack, new StaticEnvironmentHost(helper)) == null, "Component bus should not create a component environment");
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

    private static Object[] invokeValue(final GameTestHelper helper, final Value value, final String method, final Object... args) {
        try {
            return API.machine.create(null).invoke(value, method, args);
        } catch (Exception e) {
            helper.fail("Value invocation failed: " + method + " " + e.getMessage());
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

    private static void assertClose(final GameTestHelper helper, final double actual, final double expected, final String name) {
        helper.assertTrue(Math.abs(actual - expected) < 0.0001D, name + " expected " + expected + " but got " + actual);
    }

    private static <T> void withCachedConfig(final ModConfigSpec.ConfigValue<T> value, final T override, final ThrowingRunnable action) throws Exception {
        final Field cachedValue = ModConfigSpec.ConfigValue.class.getDeclaredField("cachedValue");
        cachedValue.setAccessible(true);
        final Object previous = cachedValue.get(value);
        cachedValue.set(value, override);
        try {
            action.run();
        } finally {
            cachedValue.set(value, previous);
        }
    }

    private static void assertSingleResult(final GameTestHelper helper, final Object[] result, final Object expected, final String name) {
        helper.assertTrue(result.length == 1 && expected.equals(result[0]), name + " expected " + expected + " but got " + (result.length == 0 ? "<empty>" : result[0]));
    }

    private static Direction rotateHorizontal(final Direction value, final int steps) {
        if (value.getAxis().isVertical()) {
            return value;
        }
        Direction result = value;
        for (int index = 0; index < steps; index++) {
            result = result.getClockWise();
        }
        return result;
    }

    private static int horizontalSteps(final Direction facing) {
        return switch (facing) {
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
        };
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
        return droppedItemEntity(helper, item).getItem();
    }

    private static ItemEntity droppedItemEntity(final GameTestHelper helper, final Item item) {
        for (ItemEntity entity : helper.getEntities(EntityType.ITEM)) {
            if (entity.getItem().is(item)) {
                return entity;
            }
        }
        helper.fail("Expected dropped item " + item);
        throw new IllegalStateException("Expected dropped item " + item);
    }

    private static int droppedItemCount(final GameTestHelper helper, final Item item) {
        int count = 0;
        for (ItemEntity entity : helper.getEntities(EntityType.ITEM)) {
            if (entity.getItem().is(item)) {
                count += entity.getItem().getCount();
            }
        }
        return count;
    }

    private static int droppedItemCount(final GameTestHelper helper, final Item item, final BlockPos pos, final double radius) {
        int count = 0;
        for (ItemEntity entity : helper.getEntities(EntityType.ITEM, pos, radius)) {
            if (entity.getItem().is(item)) {
                count += entity.getItem().getCount();
            }
        }
        return count;
    }

    private static int nanomachinesCommandDelayTicks() {
        return Math.max(1, (int) (ModSettings.nanomachinesCommandDelay() * 20D));
    }

    private static void runNanomachinesCommandDelay(final Player player) {
        if (API.nanomachines instanceof li.cil.oc.common.NanomachinesRegistry registry) {
            for (int i = 0; i < nanomachinesCommandDelayTicks(); i++) {
                registry.update(player);
            }
        }
    }

    private static void runNanomachinesTicks(final li.cil.oc.common.NanomachinesRegistry registry, final Player player, final int ticks) {
        for (int i = 0; i < ticks; i++) {
            registry.update(player);
        }
    }

    private static void writeFile(final GameTestHelper helper, final ComputerCaseBlockEntity computer, final String filesystemAddress, final String path, final String data) {
        try {
            chargeComponent(helper, computer, filesystemAddress, 1D);
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

    private static void chargeComponent(final GameTestHelper helper, final ComputerCaseBlockEntity computer, final String address, final double energy) {
        for (final Node reachable : computer.node().reachableNodes()) {
            if (address.equals(reachable.address())) {
                chargeConnector(helper, reachable, energy);
                return;
            }
        }
        helper.fail("Could not find component node " + address + " to charge");
    }

    private static void chargeConnector(final GameTestHelper helper, final Node node, final double energy) {
        helper.assertTrue(node instanceof Connector, "Expected connector node to charge");
        final Connector connector = (Connector) node;
        connector.setLocalBufferSize(energy);
        connector.changeBuffer(energy);
    }

    private record TestMessage(Node source, String name, Object[] data) implements Message {
        @Override
        public void cancel() {
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private static final class RecordingNetworkEnvironment implements li.cil.oc.api.network.Environment {
        private final Node node = Network.newNode(this, Visibility.Network).create();
        private li.cil.oc.api.network.Packet lastPacket;

        @Override
        public Node node() {
            return node;
        }

        @Override
        public void onConnect(final Node node) {
        }

        @Override
        public void onDisconnect(final Node node) {
        }

        @Override
        public void onMessage(final Message message) {
            if ("network.message".equals(message.name()) && message.data().length == 1 && message.data()[0] instanceof li.cil.oc.api.network.Packet packet) {
                lastPacket = packet;
            }
        }
    }

    private static final class RecordingConnectorEnvironment implements li.cil.oc.api.network.Environment {
        private final Connector node;

        private RecordingConnectorEnvironment(final double bufferSize) {
            node = (Connector) Network.newNode(this, Visibility.Network).withConnector(bufferSize).create();
        }

        @Override
        public Node node() {
            return node;
        }

        private Connector connector() {
            return node;
        }

        @Override
        public void onConnect(final Node node) {
        }

        @Override
        public void onDisconnect(final Node node) {
        }

        @Override
        public void onMessage(final Message message) {
        }
    }

    private static final class RecordingWirelessEndpoint implements li.cil.oc.api.network.WirelessEndpoint {
        private final net.minecraft.world.level.Level level;
        private final BlockPos pos;
        private li.cil.oc.api.network.Packet lastPacket;
        private li.cil.oc.api.network.WirelessEndpoint lastSender;

        private RecordingWirelessEndpoint(final net.minecraft.world.level.Level level, final BlockPos pos) {
            this.level = level;
            this.pos = pos;
        }

        @Override
        public int x() {
            return pos.getX();
        }

        @Override
        public int y() {
            return pos.getY();
        }

        @Override
        public int z() {
            return pos.getZ();
        }

        @Override
        public net.minecraft.world.level.Level world() {
            return level;
        }

        @Override
        public void receivePacket(final li.cil.oc.api.network.Packet packet, final li.cil.oc.api.network.WirelessEndpoint sender) {
            lastPacket = packet;
            lastSender = sender;
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

    private static void tickRelayThroughDelay(final GameTestHelper helper, final BlockPos pos, final RelayBlockEntity relay) {
        for (int tick = 0; tick < relay.relayDelay(); tick++) {
            RelayBlockEntity.serverTick(helper.getLevel(), pos, helper.getBlockState(pos), relay);
        }
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

    private static void sendMotionSignalNow(final MotionSensorBlockEntity sensor, final BlockPos pos, final LivingEntity entity) {
        try {
            final Method sendMotionSignal = MotionSensorBlockEntity.class.getDeclaredMethod("sendMotionSignal", Vec3.class, LivingEntity.class);
            sendMotionSignal.setAccessible(true);
            sendMotionSignal.invoke(sensor, Vec3.atCenterOf(pos), entity);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Could not invoke motion sensor signal", e);
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

    private record StaticRotatablePositionEnvironmentHost(GameTestHelper helper, BlockPos pos, Direction facing)
        implements li.cil.oc.api.network.EnvironmentHost, li.cil.oc.api.internal.Rotatable {
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

        @Override
        public Direction toGlobal(final Direction value) {
            return value;
        }

        @Override
        public Direction toLocal(final Direction value) {
            return value;
        }
    }

    private record RotatedPositionEnvironmentHost(GameTestHelper helper, BlockPos pos, Direction facing)
        implements li.cil.oc.api.network.EnvironmentHost, li.cil.oc.api.internal.Rotatable {
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

        @Override
        public Direction toGlobal(final Direction value) {
            return rotateHorizontal(value, horizontalSteps(facing));
        }

        @Override
        public Direction toLocal(final Direction value) {
            return rotateHorizontal(value, (4 - horizontalSteps(facing)) % 4);
        }
    }

    private static class AgentTestHost implements li.cil.oc.api.internal.Agent {
        private final GameTestHelper helper;
        private final Player player;
        private final SimpleContainer mainInventory = new SimpleContainer(9);
        private final SimpleContainer equipmentInventory = new SimpleContainer(4);
        private int selectedSlot;

        private AgentTestHost(final GameTestHelper helper) {
            this(helper, null);
        }

        private AgentTestHost(final GameTestHelper helper, final Player player) {
            this.helper = helper;
            this.player = player;
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
            return player;
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

    private static final class RobotTestHost extends AgentTestHost implements li.cil.oc.api.internal.Robot {
        private RobotTestHost(final GameTestHelper helper) {
            super(helper);
        }

        @Override
        public Node node() {
            return null;
        }

        @Override
        public void onConnect(final Node node) {
        }

        @Override
        public void onDisconnect(final Node node) {
        }

        @Override
        public void onMessage(final Message message) {
        }

        @Override
        public int tier() {
            return 0;
        }

        @Override
        public int componentCount() {
            return 0;
        }

        @Override
        public li.cil.oc.api.network.Environment getComponentInSlot(final int index) {
            return null;
        }

        @Override
        public void synchronizeSlot(final int slot) {
        }

        @Override
        public boolean shouldAnimate() {
            return false;
        }

        @Override
        public int getContainerSize() {
            return mainInventory().getContainerSize();
        }

        @Override
        public boolean isEmpty() {
            return mainInventory().isEmpty();
        }

        @Override
        public ItemStack getItem(final int slot) {
            return mainInventory().getItem(slot);
        }

        @Override
        public ItemStack removeItem(final int slot, final int amount) {
            return mainInventory().removeItem(slot, amount);
        }

        @Override
        public ItemStack removeItemNoUpdate(final int slot) {
            return mainInventory().removeItemNoUpdate(slot);
        }

        @Override
        public void setItem(final int slot, final ItemStack stack) {
            mainInventory().setItem(slot, stack);
        }

        @Override
        public void setChanged() {
            mainInventory().setChanged();
        }

        @Override
        public boolean stillValid(final Player player) {
            return true;
        }

        @Override
        public void clearContent() {
            mainInventory().clearContent();
        }

        @Override
        public int[] getSlotsForFace(final Direction side) {
            final int[] slots = new int[getContainerSize()];
            for (int slot = 0; slot < slots.length; slot++) {
                slots[slot] = slot;
            }
            return slots;
        }

        @Override
        public boolean canPlaceItemThroughFace(final int slot, final ItemStack stack, final Direction side) {
            return true;
        }

        @Override
        public boolean canTakeItemThroughFace(final int slot, final ItemStack stack, final Direction side) {
            return true;
        }
    }

    private static final class TabletTestHost implements li.cil.oc.api.internal.Tablet {
        private final GameTestHelper helper;
        private final Player player;

        private TabletTestHost(final GameTestHelper helper, final Player player) {
            this.helper = helper;
            this.player = player;
        }

        @Override
        public Player player() {
            return player;
        }

        @Override
        public li.cil.oc.api.machine.Machine machine() {
            return null;
        }

        @Override
        public Iterable<ItemStack> internalComponents() {
            return List.of();
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
            return player.getX();
        }

        @Override
        public double yPosition() {
            return player.getY();
        }

        @Override
        public double zPosition() {
            return player.getZ();
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

    private static final class RecordingNanomachineProvider implements li.cil.oc.api.nanomachines.BehaviorProvider {
        private final Iterable<li.cil.oc.api.nanomachines.Behavior> behaviors;

        private RecordingNanomachineProvider(final li.cil.oc.api.nanomachines.Behavior behavior) {
            this(List.of(behavior));
        }

        private RecordingNanomachineProvider(final Iterable<li.cil.oc.api.nanomachines.Behavior> behaviors) {
            this.behaviors = behaviors;
        }

        @Override
        public Iterable<li.cil.oc.api.nanomachines.Behavior> createBehaviors(final Player player) {
            return behaviors;
        }

        @Override
        public CompoundTag writeToNBT(final li.cil.oc.api.nanomachines.Behavior behavior) {
            return new CompoundTag();
        }

        @Override
        public li.cil.oc.api.nanomachines.Behavior readFromNBT(final Player player, final CompoundTag nbt) {
            return behaviors.iterator().hasNext() ? behaviors.iterator().next() : null;
        }
    }

    private static final class RecordingNanomachineBehavior implements li.cil.oc.api.nanomachines.Behavior {
        private int enableCount;
        private int disableCount;
        private li.cil.oc.api.nanomachines.DisableReason disableReason;

        @Override
        public String getNameHint() {
            return "recording";
        }

        @Override
        public void onEnable() {
            enableCount++;
        }

        @Override
        public void onDisable(final li.cil.oc.api.nanomachines.DisableReason reason) {
            disableCount++;
            disableReason = reason;
        }

        @Override
        public void update() {
        }
    }

    private NeoOpenComputersGameTests() {
    }
}
