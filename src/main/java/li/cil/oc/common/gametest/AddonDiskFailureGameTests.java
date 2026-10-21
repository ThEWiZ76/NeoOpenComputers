package li.cil.oc.common.gametest;

import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.blockentity.DiskDriveBlockEntity;
import li.cil.oc.common.blockentity.RackBlockEntity;
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
public final class AddonDiskFailureGameTests {
    @GameTestGenerator
    public static Collection<TestFunction> tests() {
        final var tests = new ArrayList<TestFunction>();
        if (!Boolean.getBoolean("neoopencomputers.testApiSmokeAddon")) return tests;
        for (boolean rack : List.of(false, true)) for (boolean noNode : List.of(false, true))
            for (var failure : List.of("constructing_argument", "load_durable", "initializing_argument", "initialized_argument"))
                tests.add(new TestFunction("addonDiskFailure", "addon_disk_failure_" + rack + "_" + noNode + "_" + failure,
                    "neoopencomputers:empty", 100, 0, true, helper -> run(helper, rack, noNode, failure)));
        return tests;
    }

    private static void run(GameTestHelper helper, boolean rack, boolean noNode, String failure) {
        final String id = "disk-init-" + rack + "-" + noNode + "-" + failure;
        final var pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, rack ? ModBlocks.RACK.get() : ModBlocks.DISK_DRIVE.get());
        final Container items;
        final Runnable save;
        if (rack) {
            final RackBlockEntity owner = helper.getBlockEntity(pos);
            owner.setItem(0, new ItemStack(ModItems.DISK_DRIVE_MOUNTABLE.get()));
            final var drive = (li.cil.oc.common.component.DiskDriveMountableEnvironment) owner.getMountable(0);
            items = drive; save = () -> drive.save(new CompoundTag());
        } else {
            final DiskDriveBlockEntity drive = helper.getBlockEntity(pos);
            items = drive; save = () -> drive.save(new CompoundTag());
        }
        final var stack = AddonLifecycleGameTests.card(id, noNode);
        final var root = stack.get(DataComponents.CUSTOM_DATA).copyTag();
        root.putBoolean("oc_api_smoke_floppy", true); root.putBoolean("oc_api_smoke_dispose_increment", true);
        root.putBoolean("oc_api_smoke_reenter_cleanup", true);
        root.putString("oc_api_smoke_failure", failure); root.putString("retainedSibling", "last good");
        final var data = new CompoundTag(); data.putInt("trackedValue", 731); data.putInt("retainedOnFailure", 42);
        root.put("oc:data", data); stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        items.setItem(0, stack); save.run();
        helper.assertTrue(items.getItem(0).get(DataComponents.CUSTOM_DATA).copyTag().equals(root), "Failed initialization published partial or disposal data");
        final var expected = new ArrayList<String>(); expected.add("Constructing");
        if (!failure.startsWith("constructing")) expected.add("load");
        if (failure.startsWith("initializ")) expected.add("Initializing");
        if (failure.startsWith("initialized")) expected.add("Initialized");
        expected.add("Disposing"); expected.add("Disposed");
        helper.assertTrue(expected.equals(observe(id).get("trace")), "Failed disk initialization did not complete disposal: " + observe(id));
        helper.assertTrue(Boolean.TRUE.equals(observe(id).get("cleanupReentered")), "Failed component did not exercise reentrant host save during cleanup");
        helper.assertTrue(!Boolean.TRUE.equals(observe(id).get("networked")), "Failed media node remained connected");
        if (failure.startsWith("initialized")) helper.assertTrue(Boolean.TRUE.equals(observe(id).get("initializedNetworked")) == !noNode,
            "Post-publication failure did not exercise the intended node state");
        final String recoveryId = id + "-recovered";
        final var recovered = items.getItem(0).copy();
        final var recoveryData = recovered.get(DataComponents.CUSTOM_DATA).copyTag();
        recoveryData.remove("oc_api_smoke_failure"); recoveryData.putString("oc_api_smoke_lifecycle_id", recoveryId);
        recovered.set(DataComponents.CUSTOM_DATA, CustomData.of(recoveryData)); items.setItem(0, recovered);
        helper.assertTrue(((Number) observe(recoveryId).get("value")).intValue() == 731
            && List.of("Constructing", "load", "Initializing", "Initialized").equals(observe(recoveryId).get("trace")), "Owner could not recover using the last good item data");
        final var removed = items.removeItemNoUpdate(0);
        helper.assertTrue(ItemDriverData.dataTag(removed).getInt("trackedValue") == 732, "Recovered component did not finish its ordinary transfer");
        helper.assertTrue(expected.equals(observe(id).get("trace")), "Later removal disposed the failed instance twice");
        helper.succeed();
    }

    @SuppressWarnings("unchecked") private static Map<String,Object> observe(String id) {
        try { return (Map<String,Object>) Class.forName("example.ocapi.ApiSmokeAddon").getMethod("observe", String.class).invoke(null, id); }
        catch (ReflectiveOperationException error) { throw new AssertionError(error); }
    }
}
