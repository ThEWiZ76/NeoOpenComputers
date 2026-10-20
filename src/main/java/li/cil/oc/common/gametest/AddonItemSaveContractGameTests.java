package li.cil.oc.common.gametest;

import li.cil.oc.api.Driver;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.blockentity.AdapterBlockEntity;
import li.cil.oc.common.blockentity.ChargerBlockEntity;
import li.cil.oc.common.blockentity.ComputerCaseBlockEntity;
import li.cil.oc.common.blockentity.RobotBlockEntity;
import li.cil.oc.common.item.ItemDriverData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.gametest.GameTestHolder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@GameTestHolder("neoopencomputers")
public final class AddonItemSaveContractGameTests {
    private record Fixture(Container inventory, int slot, Runnable save) { }

    @GameTestGenerator
    public static Collection<TestFunction> tests() {
        if (!Boolean.getBoolean("neoopencomputers.testApiSmokeAddon")) return List.of();
        final var tests = new ArrayList<TestFunction>();
        for (final var owner : List.of("case", "robot", "adapter", "charger"))
            for (final var mode : List.of("default", "null", "custom"))
                tests.add(new TestFunction("addonItemSave", "addon_cleared_item_tag_" + owner + "_" + mode,
                    "neoopencomputers:empty", 100, 0, true, helper -> run(helper, owner, mode)));
        for (final var owner : List.of("case", "robot", "adapter", "charger"))
            for (final var failure : List.of("load_exception", "save_exception", "load_linkage", "save_linkage"))
                tests.add(new TestFunction("addonItemSave", "addon_item_error_" + owner + "_" + failure,
                    "neoopencomputers:empty", 100, 0, true, helper -> runFailure(helper, owner, failure)));
        return tests;
    }

    @SuppressWarnings("unchecked")
    private static void runFailure(final GameTestHelper helper, final String owner, final String failure) {
        final var fixture = fixture(helper, owner);
        final String id = "item-error-" + owner + "-" + failure;
        final var stack = new ItemStack(Items.PAPER);
        final var root = new CompoundTag();
        root.putBoolean("oc_api_smoke", true);
        root.putBoolean("oc_api_smoke_upgrade", owner.equals("robot") || owner.equals("adapter"));
        root.putBoolean("oc_api_smoke_tablet", owner.equals("charger"));
        root.putString("oc_api_smoke_lifecycle_id", id);
        root.putString("oc_api_smoke_failure", failure);
        root.putString("saveContractSibling", "retained");
        final var state = new CompoundTag(); state.putInt("trackedValue", 731); state.putInt("retainedOnFailure", 42);
        root.put("oc:data", state);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        helper.assertTrue(fixture.inventory.canPlaceItem(fixture.slot, stack), "Throwing addon item rejected by " + owner);
        fixture.inventory.setItem(fixture.slot, stack);
        fixture.save.run();
        final Map<String, Object> observed;
        try {
            observed = (Map<String, Object>) Class.forName("example.ocapi.ApiSmokeAddon").getMethod("observe", String.class).invoke(null, id);
        } catch (ReflectiveOperationException error) { throw new IllegalStateException("Standalone error fixture unavailable", error); }
        helper.assertTrue(Boolean.TRUE.equals(observed.get("loaded")) && ((Number) observed.get("value")).intValue() == 731,
            "Host lost the addon environment after an isolated persistence error: " + observed);
        if (!owner.equals("charger")) helper.assertTrue(observed.get("phase").equals("Initialized"),
            "Item persistence error interrupted lifecycle initialization: " + observed);
        final var saved = fixture.inventory.getItem(fixture.slot);
        final var restored = ItemStack.parseOptional(helper.getLevel().registryAccess(), (CompoundTag) saved.save(helper.getLevel().registryAccess()));
        final var restoredRoot = restored.get(DataComponents.CUSTOM_DATA).copyTag();
        final var restoredState = restoredRoot.getCompound("oc:data");
        helper.assertTrue(restoredRoot.getString("saveContractSibling").equals("retained")
            && restoredState.getInt("trackedValue") == 731 && !restoredState.contains("unpublished"),
            "Failed addon persistence published partial state or lost sibling data");
        if (failure.startsWith("save")) helper.assertTrue(restoredState.equals(state), "Failed save replaced the last good item state");
        helper.succeed();
    }

    private static void run(final GameTestHelper helper, final String owner, final String mode) {
        final var fixture = fixture(helper, owner);
        final var stack = new ItemStack(Items.PAPER);
        final var root = new CompoundTag();
        root.putBoolean("oc_api_smoke", true);
        root.putBoolean("oc_api_smoke_null_tag", mode.equals("null"));
        root.putBoolean("oc_api_smoke_custom_tag", mode.equals("custom"));
        root.putBoolean("oc_api_smoke_upgrade", owner.equals("robot") || owner.equals("adapter"));
        root.putBoolean("oc_api_smoke_tablet", owner.equals("charger"));
        root.putString("saveContractSibling", "retained");
        final var state = new CompoundTag(); state.putInt("count", 731); state.putInt("obsolete", 42);
        root.put(mode.equals("custom") ? "apiSmokeData" : "oc:data", state);
        if (mode.equals("custom")) {
            final var sibling = new CompoundTag(); sibling.putString("marker", "other child"); root.put("oc:data", sibling);
        }
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        helper.assertTrue(fixture.inventory.canPlaceItem(fixture.slot, stack), "Actual addon item rejected by " + owner);
        fixture.inventory.setItem(fixture.slot, stack);
        fixture.save.run();
        final var saved = fixture.inventory.getItem(fixture.slot);
        final var driver = Driver.driverFor(saved);
        final var data = ItemDriverData.driverDataTag(driver, saved).copy();
        helper.assertTrue(data.getInt("count") == 731, owner + " failed to load/save actual addon state");
        helper.assertTrue(!data.contains("obsolete"), owner + " retained a field omitted by the environment save");
        helper.assertTrue(!data.getCompound("node").getString("address").isEmpty(), owner + " lost addon node identity");
        final var restored = ItemStack.parseOptional(helper.getLevel().registryAccess(), (CompoundTag) saved.save(helper.getLevel().registryAccess()));
        final var restoredRoot = restored.get(DataComponents.CUSTOM_DATA).copyTag();
        helper.assertTrue(restoredRoot.getBoolean("oc_api_smoke")
            && restoredRoot.getBoolean("oc_api_smoke_null_tag") == mode.equals("null")
            && restoredRoot.getBoolean("oc_api_smoke_custom_tag") == mode.equals("custom")
            && restoredRoot.getString("saveContractSibling").equals("retained")
            && ItemDriverData.driverDataTag(Driver.driverFor(restored), restored).equals(data),
            owner + " lost selected driver data or sibling metadata during ItemStack serialization");
        if (mode.equals("custom")) helper.assertTrue(restoredRoot.getCompound("oc:data").getString("marker").equals("other child"),
            owner + " overwrote a child outside the driver's selected scope");
        helper.succeed();
    }

    private static Fixture fixture(final GameTestHelper helper, final String owner) {
        final var pos = new BlockPos(1, 1, 1);
        return switch (owner) {
            case "case" -> {
                helper.setBlock(pos, ModBlocks.COMPUTER_CASE_TIER1.get());
                final ComputerCaseBlockEntity computer = helper.getBlockEntity(pos);
                yield new Fixture(computer, 0, () -> computer.machine().saveComponentData(computer.getItem(0)));
            }
            case "robot" -> {
                helper.setBlock(pos, ModBlocks.ROBOT.get());
                final RobotBlockEntity robot = helper.getBlockEntity(pos); robot.setTier(2);
                RobotMovementPersistenceGameTests.installHardware(helper, robot, List.of(new ItemStack(ModItems.UPGRADE_CONTAINER_TIER2.get())));
                robot.onLoad(); yield new Fixture(robot, 1, () -> robot.saveItemData(1));
            }
            case "adapter" -> {
                helper.setBlock(pos, ModBlocks.ADAPTER.get());
                final AdapterBlockEntity adapter = helper.getBlockEntity(pos);
                adapter.onLoad(); yield new Fixture(adapter, 0, () -> adapter.saveItemData(0));
            }
            case "charger" -> {
                helper.setBlock(pos, ModBlocks.CHARGER.get());
                final ChargerBlockEntity charger = helper.getBlockEntity(pos);
                yield new Fixture(charger, 0, () -> charger.saveWithFullMetadata(helper.getLevel().registryAccess()));
            }
            default -> throw new IllegalArgumentException(owner);
        };
    }
}
