package li.cil.oc.common.gametest;

import li.cil.oc.api.Driver;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.blockentity.DiskDriveBlockEntity;
import li.cil.oc.common.blockentity.RackBlockEntity;
import li.cil.oc.common.item.ItemDriverData;
import li.cil.oc.common.menu.DiskDriveMenu;
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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@GameTestHolder("neoopencomputers")
public final class AddonDiskDriveGameTests {
    @GameTestGenerator
    public static Collection<TestFunction> tests() {
        final var tests = new ArrayList<TestFunction>();
        if (!Boolean.getBoolean("neoopencomputers.testApiSmokeAddon")) return tests;
        for (boolean rack : List.of(false, true)) for (boolean noNode : List.of(false, true))
            for (var mode : List.of("load", "custom", "nullable", "quick", "unload", "full"))
                tests.add(new TestFunction("addonDiskDrive", "addon_disk_" + rack + "_" + noNode + "_" + mode,
                    "neoopencomputers:empty", 100, 0, true, helper -> run(helper, rack, noNode, mode)));
        return tests;
    }

    private static void run(GameTestHelper helper, boolean rack, boolean noNode, String mode) {
        final String id = "disk-" + rack + "-" + noNode + "-" + mode;
        final var pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, rack ? ModBlocks.RACK.get() : ModBlocks.DISK_DRIVE.get());
        BlockEntity owner = helper.getBlockEntity(pos);
        if (rack) ((RackBlockEntity) owner).setItem(0, new ItemStack(ModItems.DISK_DRIVE_MOUNTABLE.get()));
        Container items = rack ? (Container) ((RackBlockEntity) owner).getMountable(0) : (Container) owner;
        final var stack = AddonLifecycleGameTests.card(id, noNode);
        final var tag = stack.get(DataComponents.CUSTOM_DATA).copyTag();
        tag.putBoolean("oc_api_smoke_floppy", true); tag.putBoolean("oc_api_smoke_dispose_increment", true);
        tag.putBoolean("oc_api_smoke_custom_tag", mode.equals("custom"));
        tag.putBoolean("oc_api_smoke_null_tag", mode.equals("nullable"));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        ItemDriverData.driverDataTag(Driver.driverFor(stack), stack).putInt("trackedValue", 123);
        helper.assertTrue(items.canPlaceItem(0, stack), "Disk owner rejected the actual API media");
        items.setItem(0, stack);
        if (mode.equals("full")) {
            invoke("setTrackedValue", new Class<?>[]{String.class, int.class}, id, 731);
            final var player = helper.makeMockServerPlayerInLevel();
            for (int slot = 0; slot < 36; slot++) player.getInventory().setItem(slot, new ItemStack(Items.STONE, 64));
            helper.assertTrue(new DiskDriveMenu(61, player.getInventory(), items).quickMoveStack(player, 0).isEmpty()
                && items.getItem(0).getCount() == 1 && value(id) == 731, "Clearly full disk transfer changed the live item");
            items.removeItemNoUpdate(0); helper.succeed(); return;
        }
        if (!mode.equals("quick")) {
            helper.assertTrue(value(id) == 123, "Disk owner did not load the API driver's item data");
            initialized(helper, id);
        }
        invoke("setTrackedValue", new Class<?>[]{String.class, int.class}, id, 731);
        if (mode.equals("unload")) {
            if (rack) ((RackBlockEntity) owner).onChunkUnloaded(); else ((DiskDriveBlockEntity) owner).onChunkUnloaded();
            disposed(helper, id);
            final var saved = owner.saveWithFullMetadata(helper.getLevel().registryAccess()); owner.setRemoved();
            owner = BlockEntity.loadStatic(owner.getBlockPos(), owner.getBlockState(), saved, helper.getLevel().registryAccess());
            helper.getLevel().setBlockEntity(owner); owner.onLoad();
            items = rack ? (Container) ((RackBlockEntity) owner).getMountable(0) : (Container) owner;
            helper.assertTrue(value(id) == 731, "Disk unload snapshot lost live state before disposal"); initialized(helper, id);
        }
        final ItemStack removed;
        if (mode.equals("quick")) {
            final var player = helper.makeMockServerPlayerInLevel();
            removed = new DiskDriveMenu(62, player.getInventory(), items).quickMoveStack(player, 0);
            final var delivered = player.getInventory().items.stream().filter(item -> item.is(Items.PAPER)).toList();
            helper.assertTrue(delivered.size() == 1 && delivered.getFirst().getCount() == 1
                && ItemDriverData.driverDataTag(Driver.driverFor(delivered.getFirst()), delivered.getFirst()).getInt("trackedValue") == 732,
                "Disk menu delivered stale component state");
        } else removed = items.removeItemNoUpdate(0);
        helper.assertTrue(ItemDriverData.driverDataTag(Driver.driverFor(removed), removed).getInt("trackedValue") == 732,
            "Disk removal did not retain final disposal state");
        disposed(helper, id); helper.succeed();
    }

    private static void initialized(GameTestHelper helper, String id) {
        helper.assertTrue(List.of("Constructing", "load", "Initializing", "Initialized").equals(observe(id).get("trace")), "Disk lifecycle not initialized: " + observe(id));
    }
    @SuppressWarnings("unchecked") private static void disposed(GameTestHelper helper, String id) {
        final var history = (List<Map<String, Object>>) invoke("history", new Class<?>[]{String.class}, id);
        for (var entry : history) helper.assertTrue(List.of("Constructing", "load", "Initializing", "Initialized", "Disposing", "Disposed").equals(entry.get("trace")), "Disk lifecycle disposal missing/duplicated: " + entry);
    }
    private static int value(String id) { return ((Number) observe(id).get("value")).intValue(); }
    @SuppressWarnings("unchecked") private static Map<String,Object> observe(String id) { return (Map<String,Object>) invoke("observe",new Class<?>[]{String.class},id); }
    private static Object invoke(String name, Class<?>[] types, Object... args) {
        try { return Class.forName("example.ocapi.ApiSmokeAddon").getMethod(name, types).invoke(null, args); }
        catch (ReflectiveOperationException error) { throw new AssertionError(error); }
    }
}
