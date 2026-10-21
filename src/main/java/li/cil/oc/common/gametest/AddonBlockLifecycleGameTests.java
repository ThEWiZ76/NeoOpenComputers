package li.cil.oc.common.gametest;

import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.blockentity.AdapterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestGenerator;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@GameTestHolder("neoopencomputers")
public final class AddonBlockLifecycleGameTests {
    private record Fixture(AdapterBlockEntity adapter, BlockPos target, String id) { }

    @GameTestGenerator
    public static Collection<TestFunction> tests() {
        if (!Boolean.getBoolean("neoopencomputers.testApiSmokeAddon")) return List.of();
        final var tests = new ArrayList<TestFunction>();
        for (boolean noNode : List.of(false, true)) {
            tests.add(new TestFunction("addonBlockLifecycle", "addon_block_refresh_" + noNode, "neoopencomputers:empty", 100, 0, true,
                helper -> refresh(helper, noNode)));
            tests.add(new TestFunction("addonBlockLifecycle", "addon_block_unload_" + noNode, "neoopencomputers:empty", 100, 0, true,
                helper -> unload(helper, noNode)));
            tests.add(new TestFunction("addonBlockLifecycle", "addon_block_target_replacement_" + noNode, "neoopencomputers:empty", 100, 0, true,
                helper -> replacement(helper, noNode)));
            tests.add(new TestFunction("addonBlockLifecycle", "addon_block_driver_change_" + noNode, "neoopencomputers:empty", 100, 0, true,
                helper -> driverChange(helper, noNode)));
        }
        return tests;
    }

    private static Fixture fixture(GameTestHelper helper, boolean noNode) {
        final var target = helper.absolutePos(new BlockPos(2, 1, 1));
        helper.getLevel().setBlockAndUpdate(target, Blocks.BARREL.defaultBlockState());
        name(helper, (BarrelBlockEntity) helper.getLevel().getBlockEntity(target), noNode, 1);
        helper.setBlock(new BlockPos(1, 1, 1), ModBlocks.ADAPTER.get());
        final AdapterBlockEntity adapter = helper.getBlockEntity(new BlockPos(1, 1, 1));
        adapter.onLoad(); adapter.refreshNeighbors();
        final var id = blockId(target, Direction.EAST);
        helper.assertTrue(observe(id).get("phase").equals("Initialized"), "Actual registered addon block environment missing");
        invoke("setTrackedValue", new Class<?>[]{String.class, int.class}, id, 731);
        return new Fixture(adapter, target, id);
    }

    static void name(GameTestHelper helper, BarrelBlockEntity barrel, boolean noNode, int marker) {
        final var tag = barrel.saveWithoutMetadata(helper.getLevel().registryAccess());
        tag.putString("CustomName", Component.Serializer.toJson(Component.literal("OC API smoke block " + (noNode ? "nodeless" : "node")), helper.getLevel().registryAccess()));
        barrel.loadWithComponents(tag, helper.getLevel().registryAccess());
        barrel.getPersistentData().putInt("oc_api_smoke_target", marker); barrel.setChanged();
    }

    static String blockId(BlockPos pos, Direction side) { return (String) invoke("blockId", new Class<?>[]{BlockPos.class, Direction.class}, pos, side); }

    private static void refresh(GameTestHelper helper, boolean noNode) {
        final var fixture = fixture(helper, noNode);
        final int instances = history(fixture.id).size();
        final var address = observe(fixture.id).get("address");
        for (int i = 0; i < 3; i++) fixture.adapter.refreshNeighbor(fixture.target);
        helper.assertTrue(history(fixture.id).size() == instances, "Unchanged neighbor refresh recreated the addon environment");
        helper.startSequence().thenIdle(3).thenExecute(() -> {
            final var state = observe(fixture.id);
            helper.assertTrue(state.get("phase").equals("Initialized") && ((Number) state.get("value")).intValue() == 731
                && state.get("address").equals(address) && Boolean.TRUE.equals(state.get("updated"))
                && !Boolean.TRUE.equals(state.get("updatedOutsideLifetime")), "Stable block addon lost live state/identity/update lifetime");
        }).thenSucceed();
    }

    private static void unload(GameTestHelper helper, boolean noNode) {
        final var fixture = fixture(helper, noNode);
        final var address = observe(fixture.id).get("address");
        fixture.adapter.onChunkUnloaded();
        helper.assertTrue(observe(fixture.id).get("phase").equals("Disposed"), "Adapter unload did not dispose addon block");
        final var saved = fixture.adapter.saveWithFullMetadata(helper.getLevel().registryAccess());
        final var block = saved.getList("oc:adapter.blocks", CompoundTag.TAG_COMPOUND).getCompound(Direction.EAST.get3DDataValue());
        helper.assertTrue(block.getCompound("data").getCompound("example.ocapi.ApiSmokeAddon$BlockDriver").getInt("trackedValue") == 731,
            "Adapter unload discarded the saved addon block state");
        fixture.adapter.setRemoved();
        final var restored = (AdapterBlockEntity) BlockEntity.loadStatic(fixture.adapter.getBlockPos(), fixture.adapter.getBlockState(), saved, helper.getLevel().registryAccess());
        helper.getLevel().setBlockEntity(restored); restored.onLoad(); restored.refreshNeighbors();
        final var state = observe(fixture.id);
        helper.assertTrue(state.get("phase").equals("Initialized") && Boolean.TRUE.equals(state.get("loaded"))
            && ((Number) state.get("value")).intValue() == 731 && state.get("address").equals(address),
            "Reloaded addon block lost state/identity/lifecycle: " + state);
        helper.startSequence().thenIdle(3).thenExecute(() ->
            helper.assertTrue(Boolean.TRUE.equals(observe(fixture.id).get("updated")), "Reloaded block addon did not update")).thenSucceed();
    }

    private static void replacement(GameTestHelper helper, boolean noNode) {
        final var fixture = fixture(helper, noNode);
        helper.getLevel().removeBlockEntity(fixture.target);
        final var replacement = new BarrelBlockEntity(fixture.target, helper.getLevel().getBlockState(fixture.target));
        name(helper, replacement, noNode, 2);
        helper.getLevel().setBlockEntity(replacement);
        fixture.adapter.refreshNeighbor(fixture.target);
        final var state = observe(fixture.id);
        helper.assertTrue(state.get("phase").equals("Initialized") && ((Number) state.get("targetMarker")).intValue() == 2,
            "Adapter kept an environment bound to the replaced target block entity");
        helper.succeed();
    }

    private static void driverChange(GameTestHelper helper, boolean noNode) {
        final var fixture = fixture(helper, noNode);
        name(helper, (BarrelBlockEntity) helper.getLevel().getBlockEntity(fixture.target), !noNode, 2);
        fixture.adapter.refreshNeighbor(fixture.target);
        final var state = observe(fixture.id);
        helper.assertTrue(state.get("phase").equals("Initialized") && ((Number) state.get("targetMarker")).intValue() == 2,
            "Changed block driver did not create the new environment");
        helper.assertTrue(((Number) state.get("value")).intValue() == 0, "Changed block driver inherited the old driver's saved state");
        helper.succeed();
    }

    @SuppressWarnings("unchecked") private static Map<String, Object> observe(String id) { return (Map<String, Object>) invoke("observe", new Class<?>[]{String.class}, id); }
    @SuppressWarnings("unchecked") private static List<Map<String, Object>> history(String id) { return (List<Map<String, Object>>) invoke("history", new Class<?>[]{String.class}, id); }
    private static Object invoke(String method, Class<?>[] types, Object... arguments) {
        try { return Class.forName("example.ocapi.ApiSmokeAddon").getMethod(method, types).invoke(null, arguments); }
        catch (ReflectiveOperationException error) { throw new IllegalStateException("Standalone block fixture unavailable", error); }
    }
}
