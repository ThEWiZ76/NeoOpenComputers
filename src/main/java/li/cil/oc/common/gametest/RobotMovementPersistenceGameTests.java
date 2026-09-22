package li.cil.oc.common.gametest;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.Node;
import li.cil.oc.api.Network;
import li.cil.oc.api.event.RobotMoveEvent;
import li.cil.oc.common.ItemRegistry;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.block.RobotBlock;
import li.cil.oc.common.blockentity.RobotBlockEntity;
import li.cil.oc.common.blockentity.PowerConverterBlockEntity;
import li.cil.oc.common.component.TerminalScreenSnapshot;
import li.cil.oc.common.menu.RobotMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.bus.api.SubscribeEvent;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@GameTestHolder(NeoOpenComputers.MODID)
@PrefixGameTestTemplate(false)
public final class RobotMovementPersistenceGameTests {
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void robotSendsVisualStateWithoutPrivateMachineData(final GameTestHelper helper) {
        final BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.ROBOT.get());
        final RobotBlockEntity robot = helper.getBlockEntity(pos);
        installHardware(helper, robot, List.of(new ItemStack(ModItems.CPU_TIER1.get()),
            new ItemStack(ModItems.MEMORY_TIER1.get()), eeprom("""
                component.proxy(component.list("robot")()).setLightColor(0x123456)
                while true do computer.pullSignal() end
                """)));
        robot.onLoad();
        final ItemStack tool = new ItemStack(Items.DIAMOND_PICKAXE);
        tool.setDamageValue(7);
        robot.setItem(RobotBlockEntity.TOOL_SLOT, tool);
        robot.setItem(RobotBlockEntity.CARGO_SLOT_START, new ItemStack(Items.DIAMOND, 3));
        final Connector connector = (Connector) robot.machine().node();
        connector.changeBuffer(connector.localBufferSize());
        helper.assertTrue(robot.toggleMachine(), "Visual state fixture did not start");
        helper.succeedWhen(() -> {
            final CompoundTag tag = robot.getUpdateTag(helper.getLevel().registryAccess());
            helper.assertTrue(tag.getBoolean("oc:running"), "Robot update does not contain running state");
            helper.assertTrue(tag.getInt("oc:lightColor") == 0x123456, "Robot update does not contain Lua light color");
            final ItemStack receivedTool = ItemStack.parseOptional(helper.getLevel().registryAccess(), tag.getCompound("oc:tool"));
            helper.assertTrue(ItemStack.matches(tool, receivedTool), "Robot update lost tool or durability");
            helper.assertTrue(!tag.contains("oc:machine") && !tag.contains(RobotBlockEntity.TAG_HARDWARE)
                && !tag.contains("Items") && !tag.contains("oc:ownerUUID"), "Visual update leaked private machine/inventory data");
            helper.assertTrue(robot.getUpdatePacket() != null, "Robot has no blockentity update packet");
            final RobotBlockEntity client = new RobotBlockEntity(robot.getBlockPos(), robot.getBlockState());
            client.handleUpdateTag(tag, helper.getLevel().registryAccess());
            helper.assertTrue(client.isRunningForRendering() && !client.machine().isRunning(),
                "Client rendering depends on running a second Lua machine");
            helper.assertTrue(client.lightColor() == 0x123456 && ItemStack.matches(tool, client.getItem(RobotBlockEntity.TOOL_SLOT)),
                "Client did not apply light/tool state");
            helper.assertTrue(client.getItem(RobotBlockEntity.CARGO_SLOT_START).isEmpty(), "Client received robot cargo");
            robot.setItem(RobotBlockEntity.TOOL_SLOT, ItemStack.EMPTY);
            client.onDataPacket(null, robot.getUpdatePacket(), helper.getLevel().registryAccess());
            helper.assertTrue(client.getItem(RobotBlockEntity.TOOL_SLOT).isEmpty(), "Removing tool left ghost item on client");
            tag.putBoolean("oc:running", false);
            client.handleUpdateTag(tag, helper.getLevel().registryAccess());
            helper.assertTrue(!client.isRunningForRendering(), "Stopped state did not clear client running light");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void runningRobotMovesTwiceWithoutRebootAndKeepsItsOpenTerminal(final GameTestHelper helper) {
        final BlockPos start = new BlockPos(1, 1, 1);
        final BlockPos finish = start.south(2);
        for (int z = 1; z <= 4; z++) {
            helper.setBlock(new BlockPos(1, 0, z), Blocks.STONE);
            helper.setBlock(new BlockPos(1, 1, z), Blocks.AIR);
        }
        helper.setBlock(start, ModBlocks.ROBOT.get().defaultBlockState().setValue(RobotBlock.FACING, Direction.NORTH));
        final RobotBlockEntity robot = helper.getBlockEntity(start);
        final ItemStack eeprom = eeprom("""
            local robot = component.proxy(component.list("robot")())
            local eeprom = component.proxy(component.list("eeprom")())
            if eeprom.getData() == "started" then
              robot.setLightColor(0xFF0000)
              while true do computer.pullSignal() end
            end
            eeprom.setData("started")
            local sentinel = {value = 731}
            assert(robot.move(3))
            sentinel.value = sentinel.value + 1
            assert(robot.move(3))
            assert(sentinel.value == 732)
            eeprom.setData("done")
            robot.setLightColor(0x123456)
            while true do computer.pullSignal() end
            """);
        installHardware(helper, robot, List.of(new ItemStack(ModItems.SCREEN_TIER1.get()),
            new ItemStack(ModItems.KEYBOARD.get()), new ItemStack(ModItems.GRAPHICS_CARD_TIER1.get()),
            new ItemStack(ModItems.CPU_TIER1.get()), new ItemStack(ModItems.MEMORY_TIER1.get()), eeprom));
        robot.onLoad();
        helper.setBlock(start.west(), ModBlocks.POWER_CONVERTER.get());
        helper.setBlock(finish.east(), ModBlocks.POWER_CONVERTER.get());
        final PowerConverterBlockEntity oldPower = helper.getBlockEntity(start.west());
        final PowerConverterBlockEntity newPower = helper.getBlockEntity(finish.east());
        Network.joinOrCreateNetwork(robot);
        helper.assertTrue(robot.node().isNeighborOf(oldPower.node()), "Initial power connection missing");
        robot.setItem(RobotBlockEntity.CARGO_SLOT_START, new ItemStack(Items.DIAMOND, 3));
        helper.assertTrue(robot.terminalScreen() != null, "Movement fixture has no internal screen");
        robot.terminalScreen().setResolution(10, 3);
        robot.terminalScreen().setViewport(8, 2);
        robot.terminalScreen().setForegroundColor(0x000000);
        robot.terminalScreen().setBackgroundColor(0xFFFFFF);
        robot.terminalScreen().set(0, 0, "keep me", false);
        final TerminalScreenSnapshot beforeScreen = robot.terminalScreen().terminalSnapshot();
        final Map<String, String> beforeAddresses = addresses(robot);
        final var machine = robot.machine();
        final var architecture = machine.architecture();
        final var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.moveTo(robot.xPosition(), robot.yPosition(), robot.zPosition());
        final RobotMenu menu = new RobotMenu(92, player.getInventory(), robot);
        helper.assertTrue(menu.acceptsInput(player), "Movement fixture terminal is not usable");
        final Connector connector = (Connector) machine.node();
        connector.setLocalBufferSize(10000D);
        connector.changeBuffer(10000D);
        helper.assertTrue(robot.toggleMachine(), "Movement fixture could not start EEPROM program");

        helper.succeedWhen(() -> {
            final var entity = helper.getLevel().getBlockEntity(helper.absolutePos(finish));
            helper.assertTrue(entity instanceof RobotBlockEntity,
                "Running EEPROM did not complete two physical steps; first step=" + stateAt(helper, start.south()));
            final RobotBlockEntity moved = (RobotBlockEntity) entity;
            helper.assertTrue(Integer.valueOf(0x123456).equals(moved.getLightColor(null, null)[0]),
                "Lua did not continue after both moves (red light means detected reboot); state=" + stateAt(helper, finish));
            helper.assertTrue(helper.getBlockState(start).isAir() && helper.getBlockState(start.south()).isAir(), "Movement left duplicate robot blocks");
            helper.assertTrue(moved.machine() == machine && moved.machine().architecture() == architecture, "Movement replaced the live machine or Lua architecture");
            helper.assertTrue(beforeAddresses.equals(addresses(moved)), "Movement changed component addresses");
            helper.assertTrue(beforeScreen.contentEquals(moved.terminalScreen().terminalSnapshot()), "Movement lost internal screen contents or viewport");
            final ItemStack cargo = moved.getItem(RobotBlockEntity.CARGO_SLOT_START);
            helper.assertTrue(cargo.is(Items.DIAMOND) && cargo.getCount() == 3, "Movement lost robot cargo");
            helper.assertTrue(menu.stillValid(player) && menu.acceptsInput(player), "Nearby open robot terminal stopped following the robot");
            helper.assertTrue(menu.itemScreen() == moved.terminalScreen(), "Open terminal still targets an old screen");
            helper.assertTrue(!moved.node().isNeighborOf(oldPower.node()), "Robot retained power connection at old position");
            helper.assertTrue(moved.node().isNeighborOf(newPower.node()), "Robot did not connect to power at destination");
        });
    }

    @GameTest(template = "empty")
    public static void destinationChangedByPreEventPreservesRobot(final GameTestHelper helper) throws Exception {
        final BlockPos start = new BlockPos(1, 1, 1);
        final BlockPos target = start.south();
        helper.setBlock(start.below(), Blocks.STONE);
        helper.setBlock(target.below(), Blocks.STONE);
        helper.setBlock(target, Blocks.AIR);
        helper.setBlock(start, ModBlocks.ROBOT.get().defaultBlockState().setValue(RobotBlock.FACING, Direction.NORTH));
        final RobotBlockEntity robot = helper.getBlockEntity(start);
        robot.onLoad();
        robot.setItem(RobotBlockEntity.CARGO_SLOT_START, new ItemStack(Items.DIAMOND, 3));
        final var machine = robot.machine();
        final var address = robot.node().address();
        final var blockDestination = new java.util.concurrent.atomic.AtomicBoolean(true);
        final var completedMoves = new java.util.ArrayList<RobotMoveEvent.Post>();
        final Object listener = new Object() {
            @SubscribeEvent
            public void onMove(final RobotMoveEvent.Pre event) {
                if (event.agent == robot && blockDestination.get()) {
                    helper.setBlock(target, Blocks.STONE);
                }
            }

            @SubscribeEvent
            public void onMoved(final RobotMoveEvent.Post event) {
                if (event.agent == robot) {
                    completedMoves.add(event);
                }
            }
        };
        NeoForge.EVENT_BUS.register(listener);
        try {
            final Object[] result = ((Component) robot.node()).invoke("move", null, 3);
            helper.assertTrue(Boolean.FALSE.equals(result[0]), "Occupied destination was accepted");
            helper.assertTrue(helper.getBlockState(target).is(Blocks.STONE), "Move overwrote event-placed block");
            helper.assertTrue(helper.getBlockEntity(start) == robot && !robot.isRemoved(), "Blocked movement removed source robot");
            helper.assertTrue(robot.machine() == machine && address.equals(robot.node().address()), "Blocked movement replaced runtime");
            final ItemStack cargo = robot.getItem(RobotBlockEntity.CARGO_SLOT_START);
            helper.assertTrue(cargo.is(Items.DIAMOND) && cargo.getCount() == 3, "Blocked movement lost cargo");
            helper.assertTrue(completedMoves.isEmpty(), "Blocked movement posted success event");
            blockDestination.set(false);
            helper.setBlock(target, Blocks.AIR);
            final Object[] moved = ((Component) robot.node()).invoke("move", null, 3);
            helper.assertTrue(Boolean.TRUE.equals(moved[0]), "Robot could not move after destination cleared");
            helper.assertTrue(completedMoves.size() == 1 && completedMoves.getFirst().direction == Direction.SOUTH,
                "Successful movement did not post one event for the retained robot");
            helper.assertTrue(helper.getBlockEntity(target) == robot, "Success event robot is not destination robot");
        } finally {
            NeoForge.EVENT_BUS.unregister(listener);
        }
        helper.succeed();
    }

    private static String stateAt(final GameTestHelper helper, final BlockPos pos) {
        final var entity = helper.getLevel().getBlockEntity(helper.absolutePos(pos));
        return entity instanceof RobotBlockEntity robot
            ? "light=" + robot.getLightColor(null, null)[0] + ", error=" + robot.machine().lastError()
            : "no robot";
    }

    private static ItemStack eeprom(final String code) {
        final ItemStack stack = new ItemStack(ModItems.EEPROM.get());
        final CompoundTag data = new CompoundTag();
        data.putByteArray(ItemRegistry.EEPROM_CODE_TAG, code.getBytes(StandardCharsets.UTF_8));
        final CompoundTag root = new CompoundTag();
        root.put(ItemRegistry.EEPROM_DATA_TAG, data);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        return stack;
    }

    private static void installHardware(final GameTestHelper helper, final RobotBlockEntity robot, final List<ItemStack> parts) {
        final NonNullList<ItemStack> hardware = NonNullList.withSize(RobotBlockEntity.slotCount(0), ItemStack.EMPTY);
        for (final ItemStack part : parts) {
            final var driver = li.cil.oc.api.Driver.driverFor(part, li.cil.oc.api.internal.Robot.class);
            helper.assertTrue(driver != null, "Movement fixture has no driver for " + part);
            boolean placed = false;
            for (int slot = 0; slot < hardware.size(); slot++) {
                if (hardware.get(slot).isEmpty() && RobotBlockEntity.slotType(0, slot).equals(driver.slot(part))
                    && driver.tier(part) <= RobotBlockEntity.slotTier(0, slot)) {
                    hardware.set(slot, part);
                    placed = true;
                    break;
                }
            }
            helper.assertTrue(placed, "Movement fixture cannot install " + part);
        }
        final CompoundTag tag = robot.saveWithFullMetadata(helper.getLevel().registryAccess());
        tag.put(RobotBlockEntity.TAG_HARDWARE, RobotBlockEntity.saveHardwareItems(hardware));
        robot.loadWithComponents(tag, helper.getLevel().registryAccess());
    }

    private static Map<String, String> addresses(final RobotBlockEntity robot) {
        final Map<String, String> result = new LinkedHashMap<>();
        result.put("machine", robot.machine().node().address());
        result.put("robot", robot.node().address());
        for (final Node node : robot.machine().node().reachableNodes()) {
            if (node instanceof Component component) {
                result.put(component.name(), node.address());
            }
        }
        return result;
    }
}
