package li.cil.oc.common.gametest;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.entity.DroneEntity;
import li.cil.oc.common.item.DroneItem;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(NeoOpenComputers.MODID)
@PrefixGameTestTemplate(false)
public final class DroneInventoryGameTests {
    @GameTest(template = "empty")
    public static void assemblerAcceptsMixedTierUpgradesInEveryOrder(GameTestHelper helper) {
        final var pos = new net.minecraft.core.BlockPos(1, 1, 1);
        helper.setBlock(pos, li.cil.oc.common.ModBlocks.ASSEMBLER.get());
        final li.cil.oc.common.blockentity.AssemblerBlockEntity assembler = helper.getBlockEntity(pos);
        final var upgrades = new net.minecraft.world.item.Item[]{ModItems.INVENTORY_UPGRADE.get(),
            ModItems.INVENTORY_CONTROLLER_UPGRADE.get(), ModItems.TRACTOR_BEAM_UPGRADE.get()};
        for (int first = 0; first < 3; first++) {
            for (int second = 0; second < 3; second++) {
                if (first == second) continue;
                assembler.clearContent();
                assembler.setItem(assembler.SLOT_TEMPLATE, new ItemStack(ModItems.DRONE_CASE_TIER2.get()));
                assembler.setItem(assembler.SLOT_COMPONENT_START, new ItemStack(ModItems.CPU_TIER1.get()));
                assembler.setItem(assembler.SLOT_COMPONENT_START + 1, new ItemStack(ModItems.MEMORY_TIER1.get()));
                assembler.setItem(assembler.SLOT_COMPONENT_START + 2, RobotMovementPersistenceGameTests.eeprom("while true do computer.pullSignal() end"));
                assembler.setItem(assembler.SLOT_UPGRADE_START, new ItemStack(upgrades[first]));
                assembler.setItem(assembler.SLOT_UPGRADE_START + 1, new ItemStack(upgrades[second]));
                assembler.setItem(assembler.SLOT_UPGRADE_START + 2, new ItemStack(upgrades[3 - first - second]));
                helper.assertTrue(assembler.canAssemble() && assembler.start(true), "Valid upgrade order rejected by assembler: " + first + second);
                final var output = assembler.getItem(assembler.SLOT_TEMPLATE);
                helper.assertTrue(output.is(ModItems.DRONE.get()), "Assembler did not produce drone");
                final var components = ((DroneItem) output.getItem()).componentStacks(output);
                helper.assertTrue(components.size() == 6, "Assembly lost components");
                for (var upgrade : upgrades) helper.assertTrue(components.stream().filter(stack -> stack.is(upgrade)).count() == 1, "Assembly lost upgrade");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void mixedTierUpgradesLoadInEveryOrder(GameTestHelper helper) {
        final var upgrades = new net.minecraft.world.item.Item[]{ModItems.INVENTORY_UPGRADE.get(),
            ModItems.INVENTORY_CONTROLLER_UPGRADE.get(), ModItems.TRACTOR_BEAM_UPGRADE.get()};
        for (int first = 0; first < 3; first++) {
            for (int second = 0; second < 3; second++) {
                if (first == second) continue;
                final int third = 3 - first - second;
                final var drone = new DroneEntity(helper.getLevel());
                drone.loadFromItemStack(((DroneItem) ModItems.DRONE.get()).assembleFromCase(
                    new ItemStack(ModItems.DRONE_CASE_TIER2.get()), new ItemStack(upgrades[first]),
                    new ItemStack(upgrades[second]), new ItemStack(upgrades[third])), null);
                for (var expected : upgrades) {
                    int found = 0;
                    for (var component : drone.internalComponents()) if (component.is(expected)) found += component.getCount();
                    helper.assertTrue(found == 1, "Upgrade lost for permutation " + first + second + third + ": " + expected);
                }
                drone.discard();
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void removingInventoryUpgradeDropsOnlyOverflow(GameTestHelper helper) {
        final var drone = create(helper, 2);
        drone.mainInventory().setItem(1, new ItemStack(Items.DIAMOND, 3));
        drone.mainInventory().setItem(7, new ItemStack(Items.IRON_INGOT, 11));
        drone.setSelectedSlot(7);
        final ItemStack removed = drone.removeItemNoUpdate(0);
        helper.assertTrue(removed.is(ModItems.INVENTORY_UPGRADE.get()), "Fixture removed wrong upgrade");
        helper.assertTrue(drone.mainInventory().getContainerSize() == 4 && drone.selectedSlot() == 3,
            "Shrinking cargo did not update capacity and selection");
        helper.assertTrue(drone.mainInventory().getItem(1).getCount() == 3, "Shrinking cargo lost retained items");
        drone.setItem(0, removed);
        helper.assertTrue(drone.mainInventory().getContainerSize() == 8 && drone.mainInventory().getItem(7).isEmpty(),
            "Reinstalling inventory upgrade duplicated dropped cargo");
        final var drops = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
            drone.getBoundingBox().inflate(1));
        helper.assertTrue(drops.stream().filter(entity -> entity.getItem().is(Items.IRON_INGOT))
            .mapToInt(entity -> entity.getItem().getCount()).sum() == 11, "Overflow was lost or dropped more than once");
        helper.assertTrue(drops.stream().noneMatch(entity -> entity.getItem().is(Items.DIAMOND)), "Retained cargo was also dropped");
        drone.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void cargoCapacityAndSparseSlotsSurviveReload(GameTestHelper helper) {
        final var drone = create(helper, 2);
        helper.assertTrue(drone.mainInventory().getContainerSize() == 8, "Two inventory upgrades must provide eight cargo slots");
        drone.mainInventory().setItem(1, new ItemStack(Items.DIAMOND, 3));
        drone.mainInventory().setItem(7, new ItemStack(Items.IRON_INGOT, 11));
        drone.setSelectedSlot(99);
        helper.assertTrue(drone.selectedSlot() == 7, "Selected slot must be clamped to cargo, not hardware slots");
        final var saved = new CompoundTag();
        drone.saveWithoutId(saved);
        final var restored = new DroneEntity(helper.getLevel());
        restored.load(saved);
        helper.assertTrue(restored.mainInventory().getContainerSize() == 8, "Reload lost cargo capacity");
        helper.assertTrue(restored.mainInventory().getItem(0).isEmpty(), "Reload compacted empty cargo slots");
        helper.assertTrue(restored.mainInventory().getItem(1).is(Items.DIAMOND)
            && restored.mainInventory().getItem(1).getCount() == 3, "Reload lost diamonds");
        helper.assertTrue(restored.mainInventory().getItem(7).is(Items.IRON_INGOT)
            && restored.mainInventory().getItem(7).getCount() == 11, "Reload lost last-slot cargo");
        helper.assertTrue(restored.selectedSlot() == 7, "Reload lost selected cargo slot");
        drone.discard();
        restored.discard();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void cargoCapacityMatchesInstalledUpgrades(GameTestHelper helper) {
        for (int upgrades = 0; upgrades <= 3; upgrades++) {
            final var drone = create(helper, upgrades);
            helper.assertTrue(drone.mainInventory().getContainerSize() == Math.min(8, upgrades * 4),
                "Wrong cargo capacity for " + upgrades + " upgrades");
            drone.discard();
        }
        helper.succeed();
    }

    private static DroneEntity create(GameTestHelper helper, int upgrades) {
        final var components = new ItemStack[upgrades];
        for (int i = 0; i < upgrades; i++) components[i] = new ItemStack(ModItems.INVENTORY_UPGRADE.get());
        final var drone = new DroneEntity(helper.getLevel());
        final var pos = helper.absolutePos(new net.minecraft.core.BlockPos(1, 1, 1));
        drone.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
        drone.loadFromItemStack(((DroneItem) ModItems.DRONE.get()).assembleFromCase(
            new ItemStack(ModItems.DRONE_CASE_TIER2.get()), components), null);
        return drone;
    }
}
