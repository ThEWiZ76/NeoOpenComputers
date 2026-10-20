package li.cil.oc.common.gametest;

import li.cil.oc.common.ItemCharges;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModItemCharges;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.ModSettings;
import li.cil.oc.common.OpenComputersApi;
import li.cil.oc.common.WrenchTools;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@GameTestHolder("neoopencomputers")
public final class MekanismIntegrationGameTests {
    @GameTestGenerator
    public static Collection<TestFunction> tests() {
        if (!net.neoforged.fml.ModList.get().isLoaded("mekanism")) {
            if (Boolean.getBoolean("neoopencomputers.testMekanism")) throw new IllegalStateException("Requested Mekanism is absent");
            return List.of();
        }
        if (Boolean.getBoolean("neoopencomputers.testMekanism")) ModdedGameTestConnections.register();
        return Loaded.tests();
    }

    private static final class Loaded {
        static Collection<TestFunction> tests() {
            return List.of(test("mekanism_chemical_descriptions", Loaded::chemicals), test("mekanism_configurator_modes", Loaded::wrenches),
                test("mekanism_fe_item_charge", Loaded::itemCharge), test("mekanism_charger_conservation", Loaded::charger),
                test("mekanism_cable_oc_power", Loaded::cablePower), test("mekanism_native_chemical_bridge", Loaded::nativeChemicals),
                test("mekanism_drone_wrench_pickup", Loaded::dronePickup));
        }
        static TestFunction test(final String name, final java.util.function.Consumer<GameTestHelper> body) {
            return new TestFunction("mekanismIntegration", name, "neoopencomputers:ae2_export", 600, 0, true, body);
        }

        static void chemicals(final GameTestHelper helper) {
            final boolean previous = ModSettings.insertIdsInConverters();
            try {
                for (final String name : List.of("hydrogen", "oxygen", "red", "dirty_iron")) {
                    final var chemical = mekanism.api.MekanismAPI.CHEMICAL_REGISTRY.get(id(name));
                    helper.assertTrue(chemical != mekanism.api.MekanismAPI.EMPTY_CHEMICAL_HOLDER.value(), "Unknown test chemical: " + name);
                    final long amount = 9_007_199_254_740_993L;
                    final var stack = new mekanism.api.chemical.ChemicalStack(chemical, amount);
                    ModSettings.INSERT_IDS_IN_CONVERTERS.set(false);
                    final var result = describe(stack);
                    helper.assertTrue(result.get("amount") instanceof Long count && count == amount && result.get("name").equals("mekanism:" + name)
                        && result.get("label").equals(stack.getTextComponent().getString()) && !result.containsKey("id"), "Chemical description lost long amount/name/label or exposed debug id");
                    ModSettings.INSERT_IDS_IN_CONVERTERS.set(true);
                    helper.assertTrue(((Number) describe(stack).get("id")).intValue() == mekanism.api.MekanismAPI.CHEMICAL_REGISTRY.getId(chemical), "Chemical debug registry ID mismatch");
                    helper.assertTrue(stack.getAmount() == amount && stack.getChemical() == chemical, "Chemical conversion mutated source");
                    final var nested = (Map<?, ?>) OpenComputersApi.convert(new Object[]{Map.of("chemical", stack)})[0];
                    helper.assertTrue(((Map<?, ?>) nested.get("chemical")).get("amount").equals(amount), "Nested chemical lost numeric precision");
                }
                final var empty = describe(mekanism.api.chemical.ChemicalStack.EMPTY);
                helper.assertTrue(((Number) empty.get("amount")).longValue() == 0 && empty.get("name").equals("mekanism:empty"), "Empty chemical not normalized to actual Mekanism sentinel");
            } finally { ModSettings.INSERT_IDS_IN_CONVERTERS.set(previous); }
            helper.succeed();
        }

        static void wrenches(final GameTestHelper helper) {
            final var player = helper.makeMockServerPlayerInLevel(); player.setGameMode(GameType.SURVIVAL);
            final var adapterPos = helper.absolutePos(new BlockPos(2, 2, 2));
            final var chargerPos = adapterPos.east(2);
            helper.getLevel().setBlockAndUpdate(adapterPos, ModBlocks.ADAPTER.get().defaultBlockState());
            helper.getLevel().setBlockAndUpdate(chargerPos, ModBlocks.CHARGER.get().defaultBlockState());
            final var adapter = (li.cil.oc.common.blockentity.AdapterBlockEntity) helper.getLevel().getBlockEntity(adapterPos);
            final var charger = (li.cil.oc.common.blockentity.ChargerBlockEntity) helper.getLevel().getBlockEntity(chargerPos);
            final var tool = new ItemStack(item("configurator"));
            final var configurator = (mekanism.common.item.ItemConfigurator) tool.getItem();
            player.setItemInHand(InteractionHand.MAIN_HAND, tool);
            for (final var mode : mekanism.common.item.ItemConfigurator.ConfiguratorMode.values()) {
                configurator.setMode(tool, player, mode);
                final var original = tool.copy();
                final boolean allowed = mode == mekanism.common.item.ItemConfigurator.ConfiguratorMode.WRENCH;
                helper.assertTrue(WrenchTools.isWrench(tool), "Configurator tool recognition lost in mode " + mode);
                helper.assertTrue(WrenchTools.holdsApplicableWrench(player, adapterPos) == allowed, "Wrong configurator eligibility in mode " + mode);
                final boolean open = adapter.isSideOpen(Direction.EAST), inverted = charger.invertSignal();
                final var adapterResult = adapter.getBlockState().useItemOn(tool, helper.getLevel(), player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(adapterPos), Direction.EAST, adapterPos, false));
                final var chargerResult = charger.getBlockState().useItemOn(tool, helper.getLevel(), player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(chargerPos), Direction.EAST, chargerPos, false));
                helper.assertTrue(adapter.isSideOpen(Direction.EAST) == (allowed ? !open : open) && charger.invertSignal() == (allowed ? !inverted : inverted)
                    && adapterResult.consumesAction() == allowed && chargerResult.consumesAction() == allowed && ItemStack.matches(tool, original), "Configurator mode bypassed eligibility or consumed tool: " + mode);
            }
            player.setItemInHand(InteractionHand.OFF_HAND, tool); player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND));
            final boolean open = adapter.isSideOpen(Direction.EAST);
            adapter.getBlockState().useItemOn(tool, helper.getLevel(), player, InteractionHand.OFF_HAND,
                new BlockHitResult(Vec3.atCenterOf(adapterPos), Direction.EAST, adapterPos, false));
            helper.assertTrue(adapter.isSideOpen(Direction.EAST) == open, "Offhand configurator bypassed main-hand eligibility");
            helper.succeed();
        }

        static void itemCharge(final GameTestHelper helper) {
            final var tablet = new ItemStack(item("energy_tablet"));
            final var energy = tablet.getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.ITEM);
            final var nativeEnergy = tablet.getCapability(mekanism.common.capabilities.Capabilities.STRICT_ENERGY.item());
            helper.assertTrue(energy != null && nativeEnergy != null && ItemCharges.canCharge(tablet), "Real Mekanism energy tablet is not chargeable through OC");
            final double requested = ModSettings.fromForgeEnergy(1000);
            final int expected = energy.receiveEnergy(1000, true);
            final var original = tablet.copy();
            helper.assertTrue(ModItemCharges.charge(tablet, requested, true) == requested - ModSettings.fromForgeEnergy(expected)
                && ItemStack.matches(tablet, original), "FE charge simulation mutated item or reported wrong surplus");
            final int before = energy.getEnergyStored();
            final double surplus = ItemCharges.charge(tablet, requested);
            helper.assertTrue(energy.getEnergyStored() - before == expected && surplus == requested - ModSettings.fromForgeEnergy(expected), "Actual FE item charge lost energy or reported wrong surplus");
            final var conversion = mekanism.api.energy.IEnergyConversionHelper.INSTANCE.feConversion();
            nativeEnergy.setEnergy(0, nativeEnergy.getMaxEnergy(0) - conversion.convertFrom(2));
            helper.assertTrue(ItemCharges.charge(tablet, requested) == requested - ModSettings.fromForgeEnergy(2)
                && nativeEnergy.getEnergy(0) == nativeEnergy.getMaxEnergy(0), "Near-full tablet overcharged or failed to refund energy");
            helper.assertTrue(ItemCharges.charge(tablet, requested) == requested, "Full tablet consumed OC energy");
            helper.assertTrue(!ItemCharges.canCharge(new ItemStack(Items.DIAMOND)), "Non-energy item accepted");
            final var battery = new ItemStack(ModItems.BATTERY_UPGRADE_TIER1.get());
            helper.assertTrue(ModItemCharges.charge(battery, 0.125, false) == 0
                && ((li.cil.oc.common.item.BatteryUpgradeItem) battery.getItem()).chargeStored(battery) == 0.125, "FE fallback preempted precise OC Chargeable provider");
            helper.succeed();
        }

        static void charger(final GameTestHelper helper) {
            final var pos = helper.absolutePos(new BlockPos(3, 3, 3));
            helper.getLevel().setBlockAndUpdate(pos, ModBlocks.CHARGER.get().defaultBlockState());
            final var charger = (li.cil.oc.common.blockentity.ChargerBlockEntity) helper.getLevel().getBlockEntity(pos);
            li.cil.oc.api.Network.joinOrCreateNetwork(helper.getLevel(), pos);
            final var tablet = new ItemStack(item("energy_tablet"));
            final var nativeEnergy = tablet.getCapability(mekanism.common.capabilities.Capabilities.STRICT_ENERGY.item());
            helper.assertTrue(nativeEnergy != null && charger.canPlaceItem(0, tablet), "OC charger slot rejects real Mekanism energy tablet");
            final var conversion = mekanism.api.energy.IEnergyConversionHelper.INSTANCE.feConversion();
            nativeEnergy.setEnergy(0, nativeEnergy.getMaxEnergy(0) - conversion.convertFrom(2));
            final var node = (li.cil.oc.api.network.Connector) charger.node();
            charger.setItem(0, tablet); charger.setChargeSpeed(1);
            final boolean previous = ModSettings.ignorePower();
            ModSettings.IGNORE_POWER.set(false);
            try {
                node.changeBuffer(1000 - node.localBuffer());
                final double before = node.localBuffer();
                final boolean charged = charger.runChargeCycle();
                helper.assertTrue(charged && nativeEnergy.getEnergy(0) == nativeEnergy.getMaxEnergy(0)
                    && Math.abs(before - node.localBuffer() - ModSettings.fromForgeEnergy(2)) < 1e-9, "Charger failed actual tablet transfer or over-debited source: charged="
                    + charged + ", before=" + before + ", after=" + node.localBuffer() + ", itemJ=" + nativeEnergy.getEnergy(0));
                final double full = node.localBuffer();
                helper.assertTrue(!charger.runChargeCycle() && node.localBuffer() == full, "Full item drained charger buffer");
                nativeEnergy.setEnergy(0, 0); node.changeBuffer(-node.localBuffer());
                helper.assertTrue(!charger.runChargeCycle() && nativeEnergy.getEnergy(0) == 0, "Empty source created tablet energy");
            } finally { ModSettings.IGNORE_POWER.set(previous); }
            helper.succeed();
        }

        static void cablePower(final GameTestHelper helper) {
            final var cubePos = helper.absolutePos(new BlockPos(2, 2, 2));
            final var cablePos = cubePos.east(); final var targetPos = cubePos.east(2);
            helper.getLevel().setBlockAndUpdate(cubePos, block("basic_energy_cube").defaultBlockState());
            helper.getLevel().setBlockAndUpdate(cablePos, block("basic_universal_cable").defaultBlockState());
            helper.getLevel().setBlockAndUpdate(targetPos, ModBlocks.POWER_CONVERTER.get().defaultBlockState());
            final var cube = (mekanism.common.tile.TileEntityEnergyCube) helper.getLevel().getBlockEntity(cubePos);
            cube.setFacing(Direction.EAST);
            final var output = cube.getConfig().getConfig(mekanism.common.lib.transmitter.TransmissionType.ENERGY);
            output.setDataType(mekanism.common.tile.component.config.DataType.OUTPUT, mekanism.api.RelativeSide.FRONT);
            output.setEjecting(true);
            cube.getConfig().sideChanged(mekanism.common.lib.transmitter.TransmissionType.ENERGY, mekanism.api.RelativeSide.FRONT);
            final var cable = (mekanism.common.tile.transmitter.TileEntityUniversalCable) helper.getLevel().getBlockEntity(cablePos);
            final var target = helper.getLevel().getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK, targetPos, Direction.WEST);
            final var conversion = mekanism.api.energy.IEnergyConversionHelper.INSTANCE.feConversion();
            helper.startSequence().thenWaitUntil(() -> helper.assertTrue(cable.getTransmitter().hasTransmitterNetwork(), "Waiting for actual universal cable network"))
                .thenExecute(() -> {
                    helper.assertTrue(target != null && target.getEnergyStored() == 0, "OC FE target not empty/exposed");
                    cube.getEnergyContainer().setEnergy(conversion.convertFrom(1000));
                }).thenWaitUntil(() -> helper.assertTrue(target.getEnergyStored() == 1000, "Native Mekanism cube/cable has not delivered 1000 FE to OC: target="
                    + target.getEnergyStored() + ", cubeJ=" + cube.getEnergyContainer().getEnergy() + ", cableJ=" + cable.getTransmitter().getTransmitterNetwork().energyContainer.getEnergy()))
                .thenExecute(() -> helper.assertTrue(cube.getEnergyContainer().getEnergy() == 0 && cable.getTransmitter().getTransmitterNetwork().energyContainer.getEnergy() == 0,
                    "Cube/cable retained or duplicated the delivered energy")).thenSucceed();
        }

        static void nativeChemicals(final GameTestHelper helper) {
            final var pos = new BlockPos(3, 3, 3);
            helper.setBlock(pos, ModBlocks.COMPUTER_CASE_TIER1.get());
            final li.cil.oc.common.blockentity.ComputerCaseBlockEntity computer = helper.getBlockEntity(pos);
            final var cpu = new ItemStack(ModItems.CPU_TIER1.get());
            ((li.cil.oc.api.driver.item.MutableProcessor) li.cil.oc.api.Driver.driverFor(cpu)).setArchitecture(cpu, li.cil.oc.common.machine.NativeLua53Architecture.class);
            computer.setItem(4, cpu); computer.setItem(2, new ItemStack(ModItems.MEMORY_TIER1.get()));
            computer.setItem(6, RobotMovementPersistenceGameTests.eeprom("""
                local bridge = component.proxy(assert(component.list('chemical_fixture')()))
                local chemical = bridge.read()
                assert(chemical.name == 'mekanism:hydrogen')
                assert(math.type(chemical.amount) == 'integer' and chemical.amount == 9007199254740993)
                component.proxy(component.list('eeprom')()).setData('mek-chemical-ok')
                while true do computer.pullSignal() end
                """));
            computer.onLoad(); li.cil.oc.api.Network.joinOrCreateNetwork(computer);
            final var bridge = new ChemicalFixture(); computer.machine().node().connect(bridge.node());
            ((li.cil.oc.api.network.Connector) computer.machine().node()).changeBuffer(10000);
            helper.assertTrue(computer.machine().start(), "Native chemical fixture did not start");
            helper.startSequence().thenWaitUntil(() -> helper.assertTrue(new String(li.cil.oc.common.item.ItemDriverData.dataTag(computer.getItem(6)).getByteArray(
                li.cil.oc.common.ItemRegistry.EEPROM_DATA_SECTION_TAG), java.nio.charset.StandardCharsets.UTF_8).equals("mek-chemical-ok"), "Native chemical bridge failed: " + computer.machine().lastError()))
                .thenExecute(() -> bridge.node().remove()).thenSucceed();
        }

        public static final class ChemicalFixture extends li.cil.oc.api.prefab.AbstractManagedEnvironment {
            ChemicalFixture() { setNode(li.cil.oc.api.Network.newNode(this, li.cil.oc.api.network.Visibility.Network).withComponent("chemical_fixture").create()); }
            @li.cil.oc.api.machine.Callback
            public Object[] read(final li.cil.oc.api.machine.Context context, final li.cil.oc.api.machine.Arguments args) {
                return new Object[]{new mekanism.api.chemical.ChemicalStack(mekanism.api.MekanismAPI.CHEMICAL_REGISTRY.get(id("hydrogen")), 9_007_199_254_740_993L)};
            }
        }

        static void dronePickup(final GameTestHelper helper) {
            final var pos = helper.absolutePos(new BlockPos(3, 3, 3));
            final var drone = new li.cil.oc.common.entity.DroneEntity(helper.getLevel());
            drone.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
            drone.loadFromItemStack(((li.cil.oc.common.item.DroneItem) ModItems.DRONE.get()).assembleFromCase(new ItemStack(ModItems.DRONE_CASE_TIER1.get())), null);
            helper.getLevel().addFreshEntity(drone);
            final var player = helper.makeMockPlayer(GameType.SURVIVAL); player.setShiftKeyDown(true);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item("configurator")));
            drone.interact(player, InteractionHand.MAIN_HAND);
            helper.assertTrue(drone.isRemoved(), "Default configurator mode lost upstream type-only drone pickup");
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(1))
                .stream().filter(entity -> entity.getItem().is(ModItems.DRONE.get())).mapToInt(entity -> entity.getItem().getCount()).sum() == 1, "Configurator drone pickup lost/duplicated packed item");
            helper.succeed();
        }

        static ResourceLocation id(final String path) { return ResourceLocation.fromNamespaceAndPath("mekanism", path); }
        static net.minecraft.world.item.Item item(final String path) { return BuiltInRegistries.ITEM.getOptional(id(path)).orElseThrow(); }
        static net.minecraft.world.level.block.Block block(final String path) { return BuiltInRegistries.BLOCK.getOptional(id(path)).orElseThrow(); }
        static Map<?, ?> describe(final Object stack) { return (Map<?, ?>) OpenComputersApi.convert(new Object[]{stack})[0]; }
    }
}
