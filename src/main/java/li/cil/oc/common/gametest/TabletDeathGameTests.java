package li.cil.oc.common.gametest;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.api.Driver;
import li.cil.oc.api.driver.item.MutableProcessor;
import li.cil.oc.common.ItemRegistry;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.item.ItemDriverData;
import li.cil.oc.common.item.TabletRuntime;
import li.cil.oc.common.item.TabletRuntimeRegistry;
import li.cil.oc.common.machine.NativeLuaArchitecture;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.nio.charset.StandardCharsets;

@GameTestHolder(NeoOpenComputers.MODID)
@PrefixGameTestTemplate(false)
public final class TabletDeathGameTests {
    @GameTest(template = "empty", timeoutTicks = 400)
    public static void deathDropsLatestStoppedTablet(GameTestHelper helper) { death(helper, false); }

    @GameTest(template = "empty", timeoutTicks = 400)
    public static void keepInventoryRespawnRetainsTabletVm(GameTestHelper helper) { death(helper, true); }

    private static void death(GameTestHelper helper, boolean keepInventory) {
        final ServerPlayer[] holder = {helper.makeMockServerPlayerInLevel()};
        final ServerPlayer player = holder[0];
        player.setNoGravity(true);
        final var cpu = new ItemStack(ModItems.CPU_TIER1.get());
        ((MutableProcessor) Driver.driverFor(cpu)).setArchitecture(cpu, NativeLuaArchitecture.class);
        final var item = ModItems.TABLET.get();
        final ItemStack stack = item.assembleFromCase(new ItemStack(ModItems.TABLET_CASE_TIER2.get()), ItemStack.EMPTY,
            cpu, new ItemStack(ModItems.MEMORY_TIER2.get()), RobotMovementPersistenceGameTests.eeprom("""
                local retained = 731
                local eeprom = component.proxy(component.list('eeprom')())
                eeprom.setData('newest before death')
                repeat until computer.pullSignal() == 'after_death'
                eeprom.setData('resumed ' .. retained)
                while true do computer.pullSignal() end
                """));
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        final TabletRuntime runtime = TabletRuntimeRegistry.get(stack, player);
        helper.assertTrue(runtime.start(), "Tablet did not start");
        for (int tick = 1; tick < 400; tick++) helper.runAtTickTime(tick, () -> {
            if (!holder[0].isRemoved()) holder[0].doTick();
        });
        helper.startSequence()
            .thenWaitUntil(() -> helper.assertTrue(liveMarker(runtime).equals("newest before death"), "Tablet program failed: " + runtime.machine().lastError()))
            .thenExecute(() -> {
                final var rule = helper.getLevel().getGameRules().getRule(GameRules.RULE_KEEPINVENTORY);
                final boolean previous = rule.get();
                try {
                    // Scope the global rule to this synchronous death/respawn only.
                    rule.set(keepInventory, helper.getLevel().getServer());
                    player.die(player.damageSources().genericKill());
                    if (keepInventory) {
                        helper.assertTrue(!runtime.isClosed() && runtime.machine().isRunning(), "Kept tablet was stopped on death");
                        holder[0] = helper.getLevel().getServer().getPlayerList().respawn(player, false, Entity.RemovalReason.KILLED);
                        holder[0].setNoGravity(true);
                        final ItemStack kept = holder[0].getMainHandItem();
                        helper.assertTrue(kept.is(item), "Respawn lost tablet");
                        helper.assertTrue(TabletRuntimeRegistry.get(kept, holder[0]) == runtime && runtime.player() == holder[0], "Respawn did not rebind existing VM");
                        runtime.machine().signal("after_death");
                    } else {
                        helper.assertTrue(runtime.isClosed() && !runtime.machine().architecture().isInitialized(), "Death drop retained live tablet VM");
                        final var drops = helper.getLevel().getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(2), entity -> entity.getItem().is(item));
                        helper.assertTrue(drops.size() == 1, "Expected one tablet death drop");
                        final ItemStack dropped = drops.getFirst().getItem();
                        helper.assertTrue(!item.isRunning(dropped), "Death drop retained running flag");
                        helper.assertTrue(marker(item.getComponent(dropped, 3)).equals("newest before death"), "Death drop lost newest EEPROM data");
                        final ItemStack restored = ItemStack.parseOptional(helper.getLevel().registryAccess(), (net.minecraft.nbt.CompoundTag) dropped.save(helper.getLevel().registryAccess()));
                        helper.assertTrue(marker(item.getComponent(restored, 3)).equals("newest before death"), "Saved death drop lost EEPROM data");
                        drops.getFirst().discard();
                    }
                } finally {
                    rule.set(previous, helper.getLevel().getServer());
                }
            })
            .thenWaitUntil(() -> {
                if (keepInventory) helper.assertTrue(liveMarker(runtime).equals("resumed 731"), "Respawn lost Lua continuation");
            })
            .thenExecute(() -> helper.getLevel().getServer().getPlayerList().remove(holder[0]))
            .thenSucceed();
    }

    private static String liveMarker(TabletRuntime runtime) {
        for (ItemStack stack : runtime.internalComponents()) {
            final String value = marker(stack);
            if (!value.isEmpty()) return value;
        }
        return "";
    }

    private static String marker(ItemStack stack) {
        return new String(ItemDriverData.dataTag(stack).getByteArray(ItemRegistry.EEPROM_DATA_SECTION_TAG), StandardCharsets.UTF_8);
    }
}
