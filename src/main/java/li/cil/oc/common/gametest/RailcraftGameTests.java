package li.cil.oc.common.gametest;

import li.cil.oc.api.network.Component;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModItems;
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
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;

import java.util.Collection;
import java.util.List;

@GameTestHolder("neoopencomputers")
public final class RailcraftGameTests {
    @GameTestGenerator
    public static Collection<TestFunction> tests() {
        if (!net.neoforged.fml.ModList.get().isLoaded("railcraft")) {
            if (Boolean.getBoolean("neoopencomputers.testRailcraft")) throw new IllegalStateException("Requested Railcraft fixture is absent");
            return List.of();
        }
        return List.of(
            new TestFunction("railcraft", "railcraft_actual_worldspike_contract", "neoopencomputers:empty", 200, 0, true, Loaded::worldspikes),
            new TestFunction("railcraft", "railcraft_actual_crowbar_interaction", "neoopencomputers:empty", 200, 0, true, Loaded::crowbars),
            new TestFunction("railcraft", "railcraft_native_adapter_callbacks", "neoopencomputers:empty", 400, 0, true, Loaded::nativeAdapter));
    }

    private static final class Loaded {
        private static ResourceLocation id(String name) { return ResourceLocation.fromNamespaceAndPath("railcraft", name); }

        static void worldspikes(GameTestHelper helper) {
            final var pos = helper.absolutePos(new BlockPos(1, 1, 1));
            try {
                for (String kind : List.of("world", "personal")) {
                    final var block = BuiltInRegistries.BLOCK.get(id(kind.equals("world") ? "world_spike" : "personal_world_spike"));
                    helper.assertTrue(block != Blocks.AIR, "Missing published Railcraft worldspike");
                    helper.getLevel().setBlockAndUpdate(pos, block.defaultBlockState());
                    final var tile = (mods.railcraft.world.level.block.entity.worldspike.WorldSpikeBlockEntity) helper.getLevel().getBlockEntity(pos);
                    final var driver = li.cil.oc.api.Driver.driverFor(helper.getLevel(), pos, Direction.NORTH);
                    helper.assertTrue(driver != null, "No OC driver for actual Railcraft " + kind + " worldspike");
                    final var environment = driver.createEnvironment(helper.getLevel(), pos, Direction.NORTH);
                    try {
                        final var component = (Component) environment.node();
                        final var members = environment instanceof li.cil.oc.common.driver.CompoundBlockEnvironment compound
                            ? compound.environments().stream().map(li.cil.oc.common.driver.CompoundBlockEnvironment.Entry::environment).toList()
                            : List.of(environment);
                        helper.assertTrue(component.name().equals("worldspike") && members.stream().anyMatch(member ->
                            member instanceof li.cil.oc.api.driver.NamedBlock named && named.preferredName().equals("worldspike") && named.priority() == 5),
                            "Worldspike name/priority changed");
                        helper.assertTrue(component.methods().containsAll(List.of("getFuel", "getOwner", "getType", "getFuelSlotContents", "isDisabled")), "Missing original worldspike callbacks");
                        helper.assertTrue(component.invoke("getType", null)[0].equals(kind), "Wrong actual worldspike variant");
                        helper.assertTrue(((Number) component.invoke("getFuel", null)[0]).intValue() == 0, "Fuel-free provider reports invented fuel");
                        helper.assertTrue(component.invoke("getFuelSlotContents", null) == null, "Fuel-free provider reports a nonexistent inventory");
                        helper.assertTrue(component.invoke("isDisabled", null)[0].equals(false), "Always-enabled provider reports a redstone control");
                        helper.getLevel().setBlockAndUpdate(pos.east(), Blocks.REDSTONE_BLOCK.defaultBlockState());
                        helper.assertTrue(helper.getLevel().hasNeighborSignal(pos) && component.invoke("isDisabled", null)[0].equals(false),
                            "Current provider acquired an invented redstone disable state");
                        helper.assertTrue(component.invoke("getOwner", null) == null, "Ownerless worldspike reports a name");
                        final var profile = new com.mojang.authlib.GameProfile(java.util.UUID.fromString("20261020-0000-4000-8000-000000000f01"), "OC_Spike");
                        tile.setOwner(profile);
                        helper.assertTrue(component.invoke("getOwner", null)[0].equals("OC_Spike"), "Worldspike owner lost");
                        final var saved = tile.saveWithFullMetadata(helper.getLevel().registryAccess());
                        final var replacement = (mods.railcraft.world.level.block.entity.worldspike.WorldSpikeBlockEntity)
                            ((net.minecraft.world.level.block.EntityBlock) block).newBlockEntity(pos, tile.getBlockState());
                        replacement.loadWithComponents(saved, helper.getLevel().registryAccess());
                        helper.assertTrue(replacement.getOwner().orElseThrow().equals(profile), "Actual provider owner save/load lost");
                        tile.setOwner(new com.mojang.authlib.GameProfile(profile.getId(), "[unknown]"));
                        helper.assertTrue(component.invoke("getOwner", null) == null, "Legacy unknown owner sentinel exposed");
                        helper.getLevel().removeBlockEntity(pos);
                        helper.getLevel().setBlockEntity(replacement);
                        boolean rejected = false;
                        try { component.invoke("getType", null); } catch (Exception expected) { rejected = true; }
                        helper.assertTrue(rejected, "Stale environment accepted a same-kind replacement");
                    } finally { environment.node().remove(); }
                    helper.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                }
            } catch (Exception exception) { throw new IllegalStateException(exception); }
            finally {
                helper.getLevel().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                helper.getLevel().setBlockAndUpdate(pos.east(), Blocks.AIR.defaultBlockState());
            }
            helper.succeed();
        }

        static void crowbars(GameTestHelper helper) {
            final var player = helper.makeMockServerPlayerInLevel();
            player.setGameMode(GameType.SURVIVAL);
            final var pos = helper.absolutePos(new BlockPos(2, 1, 1));
            helper.getLevel().setBlockAndUpdate(pos, ModBlocks.CHARGER.get().defaultBlockState());
            final var charger = (li.cil.oc.common.blockentity.ChargerBlockEntity) helper.getLevel().getBlockEntity(pos);
            for (String kind : List.of("iron_crowbar", "steel_crowbar", "diamond_crowbar", "seasons_crowbar")) {
                final var stack = new ItemStack(BuiltInRegistries.ITEM.get(id(kind)));
                helper.assertTrue(!stack.isEmpty() && stack.getItem() instanceof mods.railcraft.api.item.Crowbar, "Missing actual crowbar " + kind);
                player.setItemInHand(InteractionHand.MAIN_HAND, stack);
                helper.assertTrue(WrenchTools.isWrench(stack), "Actual Railcraft crowbar not registered with OC");
                helper.assertTrue(WrenchTools.holdsApplicableWrench(player, pos) && stack.getDamageValue() == 0, "Eligibility rejects or damages crowbar");
                final boolean before = charger.invertSignal();
                final var result = charger.getBlockState().useItemOn(stack, helper.getLevel(), player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(pos), Direction.EAST, pos, false));
                helper.assertTrue(result.consumesAction() && charger.invertSignal() != before && stack.getDamageValue() == 1,
                    "Actual charger interaction must toggle once and consume exactly one crowbar durability: " + kind);
            }
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(net.minecraft.world.item.Items.DIAMOND));
            helper.assertTrue(!WrenchTools.isWrench(player.getMainHandItem()) && !WrenchTools.holdsApplicableWrench(player, pos), "Ordinary item registered as crowbar");
            final var dronePos = helper.absolutePos(new BlockPos(4, 1, 1));
            final var drone = new li.cil.oc.common.entity.DroneEntity(helper.getLevel());
            drone.moveTo(dronePos.getX() + 0.5, dronePos.getY(), dronePos.getZ() + 0.5, 0, 0);
            drone.loadFromItemStack(((li.cil.oc.common.item.DroneItem) ModItems.DRONE.get()).assembleFromCase(new ItemStack(ModItems.DRONE_CASE_TIER1.get())), null);
            helper.getLevel().addFreshEntity(drone);
            player.setShiftKeyDown(true);
            final var pickupTool = new ItemStack(BuiltInRegistries.ITEM.get(id("iron_crowbar")));
            player.setItemInHand(InteractionHand.MAIN_HAND, pickupTool);
            drone.interact(player, InteractionHand.MAIN_HAND);
            helper.assertTrue(drone.isRemoved() && pickupTool.getDamageValue() == 0, "Type-only drone pickup rejected or damaged crowbar");
            helper.assertTrue(helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(dronePos).inflate(1))
                .stream().filter(entity -> entity.getItem().is(ModItems.DRONE.get())).mapToInt(entity -> entity.getItem().getCount()).sum() == 1,
                "Crowbar drone pickup lost or duplicated packed item");
            helper.succeed();
        }

        static void nativeAdapter(GameTestHelper helper) {
            final var spikePos = new BlockPos(3, 1, 1);
            helper.setBlock(spikePos, BuiltInRegistries.BLOCK.get(id("personal_world_spike")));
            helper.runAtTickTime(390, () -> helper.setBlock(spikePos, Blocks.AIR));
            final var spike = (mods.railcraft.world.level.block.entity.worldspike.WorldSpikeBlockEntity) helper.getBlockEntity(spikePos);
            spike.setOwner(new com.mojang.authlib.GameProfile(java.util.UUID.fromString("20261020-0000-4000-8000-000000000f02"), "OC_Native"));
            helper.setBlock(new BlockPos(2, 1, 1), ModBlocks.ADAPTER.get());
            helper.setBlock(new BlockPos(1, 1, 1), ModBlocks.COMPUTER_CASE_TIER1.get());
            final li.cil.oc.common.blockentity.ComputerCaseBlockEntity computer = helper.getBlockEntity(new BlockPos(1, 1, 1));
            final var cpu = new ItemStack(ModItems.CPU_TIER1.get());
            ((li.cil.oc.api.driver.item.MutableProcessor) li.cil.oc.api.Driver.driverFor(cpu)).setArchitecture(cpu, li.cil.oc.common.machine.NativeLuaArchitecture.class);
            computer.setItem(computer.SLOT_CPU, cpu);
            computer.setItem(computer.SLOT_MEMORY_0, new ItemStack(ModItems.MEMORY_TIER1.get()));
            computer.setItem(computer.SLOT_EEPROM, RobotMovementPersistenceGameTests.eeprom("""
                local address
                repeat
                  address = component.list('worldspike')()
                  if not address then computer.pullSignal(0.05) end
                until address
                local spike = component.proxy(address)
                assert(spike.getType() == 'personal')
                assert(spike.getOwner() == 'OC_Native')
                assert(spike.getFuel() == 0)
                assert(select('#', spike.getFuelSlotContents()) == 0)
                assert(spike.isDisabled() == false)
                component.proxy(component.list('eeprom')()).setData('railcraft_ok')
                while true do computer.pullSignal() end
                """));
            helper.assertTrue(computer.toggleMachine(), "Native Railcraft computer did not start");
            helper.startSequence().thenWaitUntil(() -> {
                final var marker = new String(li.cil.oc.common.item.ItemDriverData.dataTag(computer.getItem(computer.SLOT_EEPROM))
                    .getByteArray(li.cil.oc.common.ItemRegistry.EEPROM_DATA_SECTION_TAG), java.nio.charset.StandardCharsets.UTF_8);
                helper.assertTrue(marker.equals("railcraft_ok") && computer.machine().isRunning(), "Native adapter callbacks incomplete: " + computer.machine().lastError());
            }).thenExecute(() -> {
                computer.machine().stop();
                helper.setBlock(spikePos, Blocks.AIR);
            }).thenSucceed();
        }
    }
}
