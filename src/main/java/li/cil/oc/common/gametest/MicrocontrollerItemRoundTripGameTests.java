package li.cil.oc.common.gametest;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.item.MutableProcessor;
import li.cil.oc.api.network.Component;
import li.cil.oc.api.network.Connector;
import li.cil.oc.common.ItemRegistry;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.blockentity.AssemblerBlockEntity;
import li.cil.oc.common.blockentity.MicrocontrollerBlockEntity;
import li.cil.oc.common.item.ItemDriverData;
import li.cil.oc.common.machine.NativeLuaArchitecture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

@GameTestHolder(NeoOpenComputers.MODID)
@PrefixGameTestTemplate(false)
public final class MicrocontrollerItemRoundTripGameTests {
    private static final String PROGRAM = """
        local ee = component.proxy(component.list('eeprom')())
        local suffix = string.char(0,255) .. 'end'
        if ee.getData() == 'seed' .. suffix then ee.setData('first' .. suffix)
        elseif ee.getData() == 'first' .. suffix then ee.setData('second' .. suffix)
        else error('EEPROM binary data lost') end
        while true do computer.pullSignal() end
        """;

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void survivalMicrocontrollerRetainsAssemblyDataEnergyAndRebootsOnReplace(final GameTestHelper helper) {
        final var player = helper.makeMockServerPlayerInLevel();
        helper.testInfo.addListener(new net.minecraft.gametest.framework.GameTestListener() {
            @Override public void testStructureLoaded(final net.minecraft.gametest.framework.GameTestInfo test) { }
            @Override public void testAddedForRerun(final net.minecraft.gametest.framework.GameTestInfo test,
                                                   final net.minecraft.gametest.framework.GameTestInfo rerun,
                                                   final net.minecraft.gametest.framework.GameTestRunner runner) { }
            @Override public void testPassed(final net.minecraft.gametest.framework.GameTestInfo test,
                                             final net.minecraft.gametest.framework.GameTestRunner runner) { removePlayer(); }
            @Override public void testFailed(final net.minecraft.gametest.framework.GameTestInfo test,
                                             final net.minecraft.gametest.framework.GameTestRunner runner) { removePlayer(); }
            private void removePlayer() { helper.getLevel().getServer().getPlayerList().remove(player); }
        });
        player.setGameMode(GameType.SURVIVAL);
        final ItemStack assembled = assemble(helper, new ItemStack(ModItems.MICROCONTROLLER_CASE_TIER1.get()));
        final var original = place(helper, player, assembled, new BlockPos(1, 0, 1));
        final var addresses = hardwareAddresses(original);
        helper.assertTrue(addresses.size() == 2 && original.getItem(0).is(ModItems.CPU_TIER1.get())
            && original.getItem(1).getHoverName().getString().equals("roundtrip RAM"), "Assembler item placement lost hardware/data");
        ((Connector) original.machine().node()).changeBuffer(731);
        helper.assertTrue(original.toggleMachine(), "Assembled native microcontroller did not boot");
        final MicrocontrollerBlockEntity[] replacement = new MicrocontrollerBlockEntity[1];
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(Arrays.equals(data(original), binary("first")),
                "Initial native program did not write binary data: " + original.machine().lastError()))
            .thenExecute(() -> {
                original.machine().pause(1000);
                final var connector = (Connector) original.machine().node();
                connector.changeBuffer(-connector.globalBuffer());
                connector.changeBuffer(317.75);
                helper.assertTrue(player.gameMode.destroyBlock(original.getBlockPos()), "Survival break failed");
                helper.assertTrue(helper.getLevel().isEmptyBlock(original.getBlockPos()), "Broken microcontroller remained in world");
                final var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(original.getBlockPos()).inflate(1));
                helper.assertTrue(drops.size() == 1 && drops.getFirst().getItem().is(ModItems.MICROCONTROLLER_TIER1.get())
                    && drops.getFirst().getItem().getCount() == 1, "Break did not return exactly one assembled item; drops="
                        + drops.stream().map(entity -> entity.getItem().toString()).toList());
                final ItemStack dropped = drops.getFirst().getItem().copy();
                drops.forEach(Entity::discard);
                assertItemPayload(helper, dropped, 317);
                replacement[0] = place(helper, player, dropped, new BlockPos(3, 0, 1));
                final var restored = replacement[0];
                helper.assertTrue(hardwareAddresses(restored).equals(addresses), "Physical component identities changed on pickup");
                helper.assertTrue(Arrays.equals(data(restored), binary("first")), "Latest binary EEPROM data lost in dropped item");
                helper.assertTrue(((Connector) restored.machine().node()).globalBuffer() == 317,
                    "Dropped item did not retain upstream integer stored energy: " + ((Connector) restored.machine().node()).globalBuffer());
                helper.assertTrue(!restored.machine().isRunning() && !restored.machine().architecture().isInitialized(),
                    "Placed item unexpectedly restored a running VM");
                helper.assertTrue(restored.getItem(1).getHoverName().getString().equals("roundtrip RAM"), "Named component metadata lost");
                helper.assertTrue(restored.toggleMachine(), "Replaced microcontroller could not start preserved native hardware");
            })
            .thenWaitUntil(() -> helper.assertTrue(replacement[0] != null && Arrays.equals(data(replacement[0]), binary("second")),
                "Replaced item did not reboot preserved program: " + (replacement[0] == null ? "not placed" : replacement[0].machine().lastError())))
            .thenSucceed();
    }

    @GameTest(template = "empty")
    public static void nonPlayerMicrocontrollerBreakKeepsCreativeTierAndHardware(final GameTestHelper helper) {
        final var player = helper.makeMockServerPlayerInLevel();
        try {
            player.setGameMode(GameType.SURVIVAL);
            final var assembled = assemble(helper, new ItemStack(ModItems.MICROCONTROLLER_CASE_CREATIVE.get()));
            final var host = place(helper, player, assembled, new BlockPos(1, 0, 1));
            ((Connector) host.machine().node()).changeBuffer(280.75);
            helper.assertTrue(helper.getLevel().destroyBlock(host.getBlockPos(), true), "Non-player block destruction failed");
            final var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(host.getBlockPos()).inflate(1));
            helper.assertTrue(drops.size() == 1 && drops.getFirst().getItem().is(ModItems.MICROCONTROLLER_CREATIVE.get()),
                "Non-player destruction lost or duplicated creative-tier chassis/components");
            final ItemStack dropped = drops.getFirst().getItem().copy();
            assertItemPayload(helper, dropped, 280);
            drops.forEach(Entity::discard);
            final var restored = place(helper, player, dropped, new BlockPos(3, 0, 1));
            helper.assertTrue(restored.tier() == 3 && ((Connector) restored.machine().node()).globalBuffer() == 280,
                "Creative-tier replacement changed tier or energy");
            helper.assertTrue(restored.getItem(0).is(ModItems.CPU_TIER1.get()) && !restored.machine().isRunning(),
                "Creative-tier item lost hardware or unexpectedly started");
        } finally {
            helper.getLevel().getServer().getPlayerList().remove(player);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void microcontrollerPickAndCreativeBreakKeepTierAndAssembledPayload(final GameTestHelper helper) {
        final var player = helper.makeMockServerPlayerInLevel();
        try {
            player.setGameMode(GameType.SURVIVAL);
            final var assembled = assemble(helper, new ItemStack(ModItems.MICROCONTROLLER_CASE_TIER2.get()));
            final var host = place(helper, player, assembled, new BlockPos(1, 0, 1));
            ((Connector) host.machine().node()).changeBuffer(64.75);
            final ItemStack picked = host.getBlockState().getBlock().getCloneItemStack(helper.getLevel(), host.getBlockPos(), host.getBlockState());
            helper.assertTrue(picked.is(ModItems.MICROCONTROLLER_TIER2.get()), "Pick changed microcontroller tier");
            assertItemPayload(helper, picked, 64);
            helper.assertTrue(host.getItem(0).is(ModItems.CPU_TIER1.get()) && ((Connector) host.machine().node()).globalBuffer() == 64.75,
                "Picking consumed source hardware or energy");
            player.setGameMode(GameType.CREATIVE);
            helper.assertTrue(player.gameMode.destroyBlock(host.getBlockPos()), "Creative break failed");
            final var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(host.getBlockPos()).inflate(1));
            helper.assertTrue(drops.size() == 1 && drops.getFirst().getItem().is(ModItems.MICROCONTROLLER_TIER2.get()),
                "Creative break lost or duplicated assembled microcontroller");
            assertItemPayload(helper, drops.getFirst().getItem(), 64);
            drops.forEach(Entity::discard);
        } finally {
            helper.getLevel().getServer().getPlayerList().remove(player);
        }
        helper.succeed();
    }

    private static void assertItemPayload(final GameTestHelper helper, final ItemStack stack, final int energy) {
        final var component = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        helper.assertTrue(component != null, "Microcontroller item has no assembled block-entity data");
        final var tag = component.copyTag();
        helper.assertTrue(tag.getList("Items", net.minecraft.nbt.Tag.TAG_COMPOUND).size() == 4, "Item lost assembled components");
        helper.assertTrue(tag.getInt("oc:storedEnergy") == energy, "Item lost stored energy");
        helper.assertTrue(!tag.contains("oc:machine") && !tag.contains("oc:snooper") && !tag.contains("oc:plugs")
            && !tag.contains("oc:componentNodes") && !tag.contains("oc:outputs"), "Portable item leaked runtime/private network state");
    }

    private static ItemStack assemble(final GameTestHelper helper, final ItemStack template) {
        final BlockPos pos = new BlockPos(0, 1, 0);
        helper.setBlock(pos, ModBlocks.ASSEMBLER.get());
        final AssemblerBlockEntity assembler = helper.getBlockEntity(pos);
        assembler.setItem(AssemblerBlockEntity.SLOT_TEMPLATE, template);
        final var cpu = new ItemStack(ModItems.CPU_TIER1.get());
        ((MutableProcessor) Driver.driverFor(cpu)).setArchitecture(cpu, NativeLuaArchitecture.class);
        final var memory = new ItemStack(ModItems.MEMORY_TIER1.get());
        memory.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("roundtrip RAM"));
        final var eeprom = RobotMovementPersistenceGameTests.eeprom(PROGRAM);
        final var tag = eeprom.getOrDefault(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        tag.getCompound(ItemRegistry.EEPROM_DATA_TAG).putByteArray(ItemRegistry.EEPROM_DATA_SECTION_TAG, binary("seed"));
        eeprom.set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
        final var inputs = java.util.List.of(cpu, memory, eeprom, new ItemStack(ModItems.NETWORK_CARD.get()));
        for (int index = 0; index < inputs.size(); index++) assembler.setItem(AssemblerBlockEntity.SLOT_COMPONENT_START + index, inputs.get(index));
        helper.assertTrue(assembler.canAssemble() && assembler.start(true), "Assembler rejected native microcontroller fixture");
        final var output = assembler.getItem(AssemblerBlockEntity.SLOT_TEMPLATE).copy();
        assembler.removeItemNoUpdate(AssemblerBlockEntity.SLOT_TEMPLATE);
        return output;
    }

    private static MicrocontrollerBlockEntity place(final GameTestHelper helper, final ServerPlayer player, final ItemStack stack, final BlockPos support) {
        helper.setBlock(support, Blocks.STONE);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        final var absolute = helper.absolutePos(support);
        final var hit = new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false);
        helper.assertTrue(stack.useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit)).consumesAction(), "Assembled item could not be placed");
        helper.assertTrue(stack.isEmpty(), "Survival placement did not consume exactly one assembled item");
        final MicrocontrollerBlockEntity host = helper.getBlockEntity(support.above());
        host.onLoad();
        return host;
    }

    private static Map<String, String> hardwareAddresses(final MicrocontrollerBlockEntity host) {
        final var result = new LinkedHashMap<String, String>();
        for (final var entry : host.machine().components().entrySet()) {
            if (entry.getValue().equals("eeprom") || entry.getValue().equals("modem")) result.put(entry.getValue(), entry.getKey());
        }
        return result;
    }

    private static byte[] data(final MicrocontrollerBlockEntity host) {
        final int slot = host.tier() == 0 ? 2 : 3;
        return ItemDriverData.dataTag(host.getItem(slot)).getByteArray(ItemRegistry.EEPROM_DATA_SECTION_TAG);
    }

    private static byte[] binary(final String prefix) {
        final byte[] beginning = prefix.getBytes(StandardCharsets.UTF_8);
        final byte[] result = Arrays.copyOf(beginning, beginning.length + 5);
        result[beginning.length] = 0;
        result[beginning.length + 1] = (byte) 255;
        result[beginning.length + 2] = 'e'; result[beginning.length + 3] = 'n'; result[beginning.length + 4] = 'd';
        return result;
    }
}
