package li.cil.oc.common.gametest;

import li.cil.oc.api.Driver;
import li.cil.oc.api.Network;
import li.cil.oc.api.internal.Database;
import li.cil.oc.api.network.Node;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.blockentity.AdapterBlockEntity;
import li.cil.oc.common.item.ItemDriverData;
import li.cil.oc.common.menu.AdapterMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("neoopencomputers")
@PrefixGameTestTemplate(false)
public final class AdapterUpgradePersistenceGameTests {
    private static AdapterBlockEntity adapter(GameTestHelper helper) {
        final var pos = helper.absolutePos(new BlockPos(2, 2, 2));
        helper.getLevel().setBlockAndUpdate(pos, ModBlocks.ADAPTER.get().defaultBlockState());
        final var adapter = (AdapterBlockEntity) helper.getLevel().getBlockEntity(pos);
        adapter.onLoad(); Network.joinOrCreateNetwork(adapter); return adapter;
    }
    private static Node databaseNode(GameTestHelper helper, AdapterBlockEntity adapter) {
        for (var node : adapter.onAnalyze(null, Direction.NORTH, 0, 0, 0)) if (node.host() instanceof Database) return node;
        helper.fail("Adapter has no database upgrade"); return null;
    }
    private static ItemStack database(AdapterBlockEntity adapter, net.minecraft.world.item.Item content) {
        final var stack = new ItemStack(ModItems.DATABASE_UPGRADE_TIER1.get());
        final var environment = Driver.driverFor(stack, adapter.getClass()).createEnvironment(stack, adapter);
        Network.joinNewNetwork(environment.node());
        ((Database) environment).setStackInSlot(0, new ItemStack(content)); environment.save(new CompoundTag()); environment.node().remove();
        return stack;
    }
    private static void verify(GameTestHelper helper, AdapterBlockEntity adapter, ItemStack stack, String address) {
        final var environment = Driver.driverFor(stack, adapter.getClass()).createEnvironment(stack, adapter);
        try {
            helper.assertTrue(address.equals(environment.node().address()), "Transferred adapter upgrade lost its node identity");
            helper.assertTrue(((Database) environment).getStackInSlot(0).is(Items.IRON_INGOT), "Transferred adapter database lost current catalogue");
        } finally { environment.node().remove(); }
    }
    private static void remove(GameTestHelper helper, int path) {
        final var adapter = adapter(helper); adapter.setItem(0, database(adapter, Items.DIAMOND));
        final var old = databaseNode(helper, adapter); ((Database) old.host()).setStackInSlot(0, new ItemStack(Items.IRON_INGOT));
        final ItemStack removed;
        if (path == 0) removed = adapter.removeItem(0, 1);
        else if (path == 1) removed = adapter.removeItemNoUpdate(0);
        else { removed = adapter.getItem(0); adapter.clearContent(); }
        helper.assertTrue(old.network() == null, "Removed adapter database remained connected");
        verify(helper, adapter, removed, old.address());
        adapter.setItem(0, database(adapter, Items.GOLD_INGOT));
        helper.assertTrue(((Database) databaseNode(helper, adapter).host()).getStackInSlot(0).is(Items.GOLD_INGOT), "Previous adapter upgrade contaminated newly inserted database");
        adapter.clearContent(); helper.succeed();
    }
    @GameTest(template = "ae2_export") public static void adapterSplitKeepsCurrentUpgrade(GameTestHelper helper) { remove(helper, 0); }
    @GameTest(template = "ae2_export") public static void adapterTakeKeepsCurrentUpgrade(GameTestHelper helper) { remove(helper, 1); }
    @GameTest(template = "ae2_export") public static void adapterClearDoesNotLeakUpgradeData(GameTestHelper helper) { remove(helper, 2); }

    @GameTest(template = "ae2_export")
    public static void sameDriverReplacementUsesActualNewUpgrade(GameTestHelper helper) {
        final var adapter = adapter(helper); adapter.setItem(0, database(adapter, Items.DIAMOND));
        final var old = databaseNode(helper, adapter); final var removed = adapter.getItem(0);
        ((Database) old.host()).setStackInSlot(0, new ItemStack(Items.IRON_INGOT));
        final var replacement = database(adapter, Items.GOLD_INGOT); final var address = ItemDriverData.dataTag(replacement).getCompound("node").getString("address");
        adapter.setItem(0, replacement); final var current = databaseNode(helper, adapter);
        helper.assertTrue(current != old && old.network() == null, "Replacing an upgrade with the same driver retained the previous environment");
        helper.assertTrue(address.equals(current.address()) && ((Database) current.host()).getStackInSlot(0).is(Items.GOLD_INGOT), "Same-driver replacement lost new upgrade identity/catalogue");
        verify(helper, adapter, removed, old.address()); adapter.clearContent(); helper.succeed();
    }

    @GameTest(template = "ae2_export")
    public static void differentDriverReplacementDoesNotReuseOldNode(GameTestHelper helper) {
        final var adapter = adapter(helper); adapter.setItem(0, database(adapter, Items.DIAMOND)); final var old = databaseNode(helper, adapter);
        adapter.setItem(0, new ItemStack(ModItems.INVENTORY_CONTROLLER_UPGRADE.get()));
        final var nodes = adapter.onAnalyze(null, Direction.NORTH, 0, 0, 0);
        helper.assertTrue(old.network() == null && nodes.length == 1 && !old.address().equals(nodes[0].address()), "Different adapter upgrade inherited old database node identity");
        adapter.clearContent(); helper.succeed();
    }

    @GameTest(template = "ae2_export")
    public static void adapterShiftMoveKeepsBothCopies(GameTestHelper helper) {
        final var adapter = adapter(helper); adapter.setItem(0, database(adapter, Items.DIAMOND)); final var node = databaseNode(helper, adapter);
        ((Database) node.host()).setStackInSlot(0, new ItemStack(Items.IRON_INGOT)); final var player = helper.makeMockServerPlayerInLevel();
        try {
            final var moved = new AdapterMenu(1, player.getInventory(), adapter).quickMoveStack(player, 0);
            helper.assertTrue(moved.getCount() == 1 && adapter.isEmpty(), "Adapter menu did not transfer exactly one upgrade");
            verify(helper, adapter, moved, node.address()); int count = 0;
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                final var destination = player.getInventory().getItem(slot);
                if (destination.is(ModItems.DATABASE_UPGRADE_TIER1.get())) { count += destination.getCount(); verify(helper, adapter, destination, node.address()); }
            }
            helper.assertTrue(count == 1 && node.network() == null, "Adapter menu duplicated the upgrade or left an active node");
        } finally { helper.getLevel().getServer().getPlayerList().remove(player); }
        helper.succeed();
    }

    @GameTest(template = "ae2_export")
    public static void adapterSaveEncodesCurrentItemBeforeSnapshot(GameTestHelper helper) {
        final var adapter = adapter(helper); adapter.setItem(0, database(adapter, Items.DIAMOND)); final var node = databaseNode(helper, adapter);
        ((Database) node.host()).setStackInSlot(0, new ItemStack(Items.IRON_INGOT));
        final var saved = adapter.saveWithFullMetadata(helper.getLevel().registryAccess());
        final var decoded = NonNullList.withSize(1, ItemStack.EMPTY); ContainerHelper.loadAllItems(saved.getCompound("oc:items"), decoded, helper.getLevel().registryAccess());
        verify(helper, adapter, decoded.get(0), node.address()); adapter.clearContent(); helper.succeed();
    }

    private static void reload(GameTestHelper helper, boolean legacy) {
        final var adapter = adapter(helper); adapter.setItem(0, database(adapter, Items.DIAMOND)); final var old = databaseNode(helper, adapter);
        ((Database) old.host()).setStackInSlot(0, new ItemStack(Items.IRON_INGOT));
        final var saved = adapter.saveWithFullMetadata(helper.getLevel().registryAccess());
        if (legacy) {
            // Older adapter saves kept the newest state only in oc:itemComponent.
            final var decoded = NonNullList.withSize(1, ItemStack.EMPTY);
            ContainerHelper.loadAllItems(saved.getCompound("oc:items"), decoded, helper.getLevel().registryAccess());
            ItemDriverData.writeDataTag(decoded.get(0), new CompoundTag());
            final var items = new CompoundTag(); ContainerHelper.saveAllItems(items, decoded, helper.getLevel().registryAccess()); saved.put("oc:items", items);
        } else saved.remove("oc:itemComponent");
        final var pos = adapter.getBlockPos(); final var state = adapter.getBlockState(); helper.getLevel().removeBlockEntity(pos);
        final var restored = new AdapterBlockEntity(pos, state);
        // Actual chunk deserialization reads NBT before assigning a world.
        restored.loadWithComponents(saved, helper.getLevel().registryAccess());
        restored.setLevel(helper.getLevel()); helper.getLevel().setBlockEntity(restored); restored.onLoad();
        helper.runAfterDelay(2, () -> {
            final var current = databaseNode(helper, restored);
            helper.assertTrue(old.address().equals(current.address()) && ((Database) current.host()).getStackInSlot(0).is(Items.IRON_INGOT), "Adapter deferred reload lost its actual upgrade state");
            verify(helper, restored, restored.removeItem(0, 1), old.address());
            restored.setItem(0, database(restored, Items.GOLD_INGOT));
            helper.assertTrue(((Database) databaseNode(helper, restored).host()).getStackInSlot(0).is(Items.GOLD_INGOT), "Loaded legacy adapter snapshot leaked into a new upgrade");
            restored.clearContent(); helper.succeed();
        });
    }
    @GameTest(template = "ae2_export") public static void adapterWorldlessLoadRestoresItemState(GameTestHelper helper) { reload(helper, false); }
    @GameTest(template = "ae2_export") public static void adapterLegacyHostDataMigratesOnce(GameTestHelper helper) { reload(helper, true); }

    @GameTest(template = "ae2_export")
    public static void adapterAutomationExtractionKeepsUpgradeState(GameTestHelper helper) {
        final var adapter = adapter(helper); adapter.setItem(0, database(adapter, Items.DIAMOND)); final var node = databaseNode(helper, adapter);
        ((Database) node.host()).setStackInSlot(0, new ItemStack(Items.IRON_INGOT));
        final var automation = new net.neoforged.neoforge.items.wrapper.InvWrapper(adapter);
        helper.assertTrue(automation.extractItem(0, 1, true).getCount() == 1 && adapter.getItem(0).getCount() == 1
            && databaseNode(helper, adapter) == node, "Simulated adapter extraction changed live ownership");
        final var removed = automation.extractItem(0, 1, false);
        verify(helper, adapter, removed, node.address());
        helper.assertTrue(adapter.isEmpty() && node.network() == null && automation.extractItem(0, 1, false).isEmpty(), "Adapter automation duplicated upgrade or retained old node");
        helper.succeed();
    }
}
