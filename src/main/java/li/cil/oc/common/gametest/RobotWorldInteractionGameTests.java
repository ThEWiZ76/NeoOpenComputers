package li.cil.oc.common.gametest;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.common.ModBlocks;
import li.cil.oc.common.ModSettings;
import li.cil.oc.api.event.RobotUsedToolEvent;
import li.cil.oc.common.block.RobotBlock;
import li.cil.oc.common.blockentity.RobotBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(NeoOpenComputers.MODID)
@PrefixGameTestTemplate(false)
public final class RobotWorldInteractionGameTests {
    @GameTest(template = "empty")
    public static void robotResolvesInstalledHardwareEnvironmentsBySlot(final GameTestHelper helper) throws Exception {
        final RobotBlockEntity robot = robot(helper, "while true do computer.pullSignal() end", true);
        final var previous = new java.util.HashMap<Integer, li.cil.oc.api.network.Environment>();
        for (final var node : robot.machine().node().neighbors()) {
            final int slot = robot.componentSlot(node.address());
            if (slot < 0) continue;
            helper.assertTrue(robot.getComponentInSlot(slot) == node.host(), "Hardware slot does not resolve its connected environment");
            previous.put(slot, node.host());
        }
        helper.assertTrue(previous.size() >= 2, "Hardware fixture must contain multiple component environments");
        helper.assertTrue(robot.getComponentInSlot(-1) == null && robot.getComponentInSlot(Integer.MAX_VALUE) == null,
            "Invalid component indices resolved hardware");
        robot.machine().onHostChanged();
        for (final var entry : previous.entrySet()) {
            final var current = robot.getComponentInSlot(entry.getKey());
            helper.assertTrue(current != null && current != entry.getValue(), "Hardware rebuild retained a stale environment");
            helper.assertTrue(current.node().isNeighborOf(robot.machine().node()), "Resolved environment is no longer connected");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void robotAttackAwardsExperienceOnlyWhenTargetIsRemoved(final GameTestHelper helper) throws Exception {
        final RobotBlockEntity robot = robot(helper, "while true do computer.pullSignal() end", true);
        li.cil.oc.common.component.ExperienceUpgradeEnvironment upgrade = null;
        for (final var node : robot.machine().node().reachableNodes()) {
            if (node.host() instanceof li.cil.oc.common.component.ExperienceUpgradeEnvironment found) upgrade = found;
        }
        helper.assertTrue(upgrade != null, "Robot fixture has no experience upgrade");
        robot.setItem(RobotBlockEntity.TOOL_SLOT, new ItemStack(Items.IRON_SWORD));
        final var target = helper.spawn(net.minecraft.world.entity.EntityType.MINECART, new BlockPos(1, 1, 2));
        final var component = (li.cil.oc.api.network.Component) robot.node();
        final Object protection = new Object() {
            @SubscribeEvent
            public void attack(final li.cil.oc.api.event.RobotAttackEntityEvent.Pre event) {
                if (event.target == target) event.setCanceled(true);
            }
        };
        final var saved = new net.minecraft.nbt.CompoundTag();
        NeoForge.EVENT_BUS.register(protection);
        try {
            component.invoke("swing", null, 3);
            upgrade.save(saved);
            helper.assertTrue(!target.isRemoved() && saved.getDouble("oc:xp") == 0D, "Canceled attacks awarded experience");
        } finally {
            NeoForge.EVENT_BUS.unregister(protection);
        }
        component.invoke("swing", null, 3);
        upgrade.save(saved);
        helper.assertTrue(target.isRemoved(), "Allowed attack did not remove minecart");
        helper.assertTrue(Math.abs(saved.getDouble("oc:xp") - ModSettings.robotActionXp()) < 1e-9,
            "Removing a minecart did not award exactly one action reward");
        final var living = helper.spawn(net.minecraft.world.entity.EntityType.COW, new BlockPos(1, 1, 2));
        living.setNoAi(true);
        component.invoke("swing", null, 3);
        upgrade.save(saved);
        helper.assertTrue(living.isAlive() && Math.abs(saved.getDouble("oc:xp") - ModSettings.robotActionXp()) < 1e-9,
            "Nonfatal attack awarded removal experience");
        living.invulnerableTime = 0;
        living.setHealth(0.01F);
        component.invoke("swing", null, 3);
        upgrade.save(saved);
        helper.assertTrue(living.isDeadOrDying() && !living.isRemoved(), "Fixture did not enter normal delayed death");
        helper.assertTrue(Math.abs(saved.getDouble("oc:xp") - ModSettings.robotActionXp()) < 1e-9,
            "Death animation was incorrectly treated as immediate entity removal");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void robotInventoryControllerEquipsActualToolFromSelectedCargo(final GameTestHelper helper) throws Exception {
        final RobotBlockEntity robot = robot(helper);
        final var controller = new li.cil.oc.common.component.InventoryControllerEnvironment.RobotInventoryControllerEnvironment(robot);
        robot.setItem(RobotBlockEntity.TOOL_SLOT, new ItemStack(Items.IRON_PICKAXE));
        robot.setItem(RobotBlockEntity.CARGO_SLOT_START + 15, new ItemStack(Items.IRON_SWORD));
        robot.setItem(RobotBlockEntity.CARGO_SLOT_START, new ItemStack(Items.DIAMOND, 3));
        robot.setSelectedSlot(15);
        helper.assertTrue(Boolean.TRUE.equals(controller.equip(null, null)[0]), "Inventory-controller equip failed");
        helper.assertTrue(robot.getItem(RobotBlockEntity.TOOL_SLOT).is(Items.IRON_SWORD), "Equip did not change the actual tool slot");
        helper.assertTrue(robot.getItem(RobotBlockEntity.CARGO_SLOT_START + 15).is(Items.IRON_PICKAXE), "Equip did not return old tool to selected cargo");
        helper.assertTrue(robot.getItem(RobotBlockEntity.CARGO_SLOT_START).getCount() == 3, "Equip changed unrelated cargo");
        controller.equip(null, null);
        helper.assertTrue(robot.getItem(RobotBlockEntity.TOOL_SLOT).is(Items.IRON_PICKAXE)
            && robot.getItem(RobotBlockEntity.CARGO_SLOT_START + 15).is(Items.IRON_SWORD), "Second equip lost or duplicated tools");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void robotAgentInventoriesExposeSeparateLiveSlotRanges(final GameTestHelper helper) throws Exception {
        final RobotBlockEntity robot = robot(helper);
        final var cargo = robot.mainInventory();
        final var equipment = robot.equipmentInventory();
        helper.assertTrue(cargo.getContainerSize() == 16 && equipment.getContainerSize() == 4, "Agent views expose the wrong inventory sizes");
        equipment.setItem(0, new ItemStack(Items.IRON_PICKAXE));
        equipment.setItem(3, new ItemStack(Items.EMERALD));
        cargo.setItem(0, new ItemStack(Items.DIAMOND, 3));
        cargo.setItem(15, new ItemStack(Items.GOLD_INGOT, 2));
        helper.assertTrue(robot.getItem(0).is(Items.IRON_PICKAXE) && robot.getItem(3).is(Items.EMERALD)
            && robot.getItem(RobotBlockEntity.CARGO_SLOT_START).is(Items.DIAMOND)
            && robot.getItem(RobotBlockEntity.CARGO_SLOT_START + 15).is(Items.GOLD_INGOT), "Agent views use stale or shifted storage");
        helper.assertTrue(cargo.removeItem(0, 1).getCount() == 1 && robot.getItem(RobotBlockEntity.CARGO_SLOT_START).getCount() == 2, "Cargo removal did not mutate real storage");
        helper.assertTrue(equipment.removeItemNoUpdate(3).is(Items.EMERALD) && robot.getItem(3).isEmpty(), "Equipment removal did not mutate real storage");
        cargo.setItem(-1, new ItemStack(Items.DIRT));
        equipment.setItem(4, new ItemStack(Items.DIRT));
        helper.assertTrue(cargo.getItem(-1).isEmpty() && equipment.getItem(4).isEmpty()
            && robot.getItem(3).isEmpty() && cargo.getItem(0).is(Items.DIAMOND), "Invalid view index crossed inventory boundaries");
        cargo.clearContent();
        helper.assertTrue(cargo.isEmpty() && !equipment.isEmpty() && robot.getItem(0).is(Items.IRON_PICKAXE), "Clearing cargo erased equipment");
        equipment.clearContent();
        helper.assertTrue(equipment.isEmpty() && robot.isEmpty(), "Clearing equipment did not clear real slots");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void robotSwingBreaksMinecartAndCollectsItsDrop(final GameTestHelper helper) throws Exception {
        final RobotBlockEntity robot = robot(helper);
        final var target = helper.spawn(net.minecraft.world.entity.EntityType.MINECART, new BlockPos(1, 1, 2));
        robot.setItem(RobotBlockEntity.TOOL_SLOT, new ItemStack(Items.IRON_SWORD));
        final Object[] result = ((li.cil.oc.api.network.Component) robot.node()).invoke("swing", null, 3);
        helper.assertTrue(Boolean.TRUE.equals(result[0]) && "entity".equals(result[1]), "Robot did not target minecart");
        helper.assertTrue(target.isRemoved(), "Repeated attack did not break the minecart");
        helper.assertTrue(cargoCount(robot, Items.MINECART) == 1, "Minecart drop did not enter cargo exactly once");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void robotAttackRespectsNeoForgeProtection(final GameTestHelper helper) throws Exception {
        final RobotBlockEntity robot = robot(helper);
        final var target = helper.spawn(net.minecraft.world.entity.EntityType.COW, new BlockPos(1, 1, 2));
        target.setNoAi(true);
        robot.setItem(RobotBlockEntity.TOOL_SLOT, new ItemStack(Items.IRON_SWORD));
        final double originalDamage = robot.player().getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        final int[] attacks = {0};
        final Object protection = new Object() {
            @SubscribeEvent
            public void attack(final net.neoforged.neoforge.event.entity.player.AttackEntityEvent event) {
                if (event.getTarget() != target) return;
                attacks[0]++;
                helper.assertTrue(event.getEntity().getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) > originalDamage,
                    "Equipped sword attributes were not applied to attack");
                event.setCanceled(true);
            }
        };
        NeoForge.EVENT_BUS.register(protection);
        try {
            final float health = target.getHealth();
            ((li.cil.oc.api.network.Component) robot.node()).invoke("swing", null, 3);
            helper.assertTrue(attacks[0] == 1 && target.getHealth() == health, "Robot bypassed NeoForge attack protection");
            helper.assertTrue(robot.getItem(RobotBlockEntity.TOOL_SLOT).getDamageValue() == 0, "Canceled attack wore out the sword");
            helper.assertTrue(robot.player().getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE) == originalDamage,
                "Sword attributes leaked into borrowed player");
        } finally {
            NeoForge.EVENT_BUS.unregister(protection);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void robotCannotAttackPlayersByDefault(final GameTestHelper helper) throws Exception {
        final RobotBlockEntity robot = robot(helper);
        final var target = helper.makeMockServerPlayerInLevel();
        final var position = helper.absolutePos(new BlockPos(1, 1, 2));
        target.moveTo(position.getX() + 0.5D, position.getY(), position.getZ() + 0.5D, 0, 0);
        robot.setItem(RobotBlockEntity.TOOL_SLOT, new ItemStack(Items.IRON_SWORD));
        final int[] attacks = {0};
        final Object listener = new Object() {
            @SubscribeEvent
            public void attack(final li.cil.oc.api.event.RobotAttackEntityEvent.Pre event) {
                if (event.target == target) { attacks[0]++; event.setCanceled(true); }
            }
        };
        final boolean previous = ModSettings.ROBOT_CAN_ATTACK_PLAYERS.get();
        NeoForge.EVENT_BUS.register(listener);
        try {
            ModSettings.ROBOT_CAN_ATTACK_PLAYERS.set(false);
            final Object[] result = ((li.cil.oc.api.network.Component) robot.node()).invoke("swing", null, 3);
            helper.assertTrue(Boolean.TRUE.equals(result[0]) && "entity".equals(result[1]) && attacks[0] == 0, "Player protection did not suppress the attack");
            ModSettings.ROBOT_CAN_ATTACK_PLAYERS.set(true);
            ((li.cil.oc.api.network.Component) robot.node()).invoke("swing", null, 3);
            helper.assertTrue(attacks[0] == 1, "Enabled player attack did not reach cancellable protection hook");
        } finally {
            ModSettings.ROBOT_CAN_ATTACK_PLAYERS.set(previous);
            NeoForge.EVENT_BUS.unregister(listener);
            target.discard();
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void robotDoesNotAttackThroughAdjacentBlock(final GameTestHelper helper) throws Exception {
        final RobotBlockEntity robot = robot(helper);
        final var target = helper.spawn(net.minecraft.world.entity.EntityType.COW, new BlockPos(1, 1, 2));
        target.setNoAi(true);
        helper.setBlock(new BlockPos(1, 1, 2), Blocks.STONE);
        robot.setItem(RobotBlockEntity.TOOL_SLOT, new ItemStack(Items.IRON_PICKAXE));
        final float health = target.getHealth();
        final Object[] result = ((li.cil.oc.api.network.Component) robot.node()).invoke("swing", null, 3);
        helper.assertTrue(Boolean.TRUE.equals(result[0]) && "block".equals(result[1]) && target.getHealth() == health,
            "Robot attacked an entity behind the nearer block face");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void robotSwingAttacksAdjacentEntityWithEquippedTool(final GameTestHelper helper) throws Exception {
        final RobotBlockEntity robot = robot(helper);
        final var target = helper.spawn(net.minecraft.world.entity.EntityType.COW, new BlockPos(1, 1, 2));
        target.setNoAi(true);
        robot.setItem(RobotBlockEntity.TOOL_SLOT, new ItemStack(Items.IRON_SWORD));
        final float health = target.getHealth();
        final Object[] result = ((li.cil.oc.api.network.Component) robot.node()).invoke("swing", robot.machine(), 3);
        helper.assertTrue(Boolean.TRUE.equals(result[0]) && "entity".equals(result[1]), "Robot did not target the adjacent entity");
        helper.assertTrue(target.getHealth() < health, "Robot attack dealt no damage");
        helper.assertTrue(robot.machine().isPaused(), "Entity swing did not pause the robot");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void robotAttackRespectsCancellationAndRestoresBorrowedPlayer(final GameTestHelper helper) throws Exception {
        final RobotBlockEntity robot = robot(helper);
        final var target = helper.spawn(net.minecraft.world.entity.EntityType.COW, new BlockPos(1, 1, 2));
        target.setNoAi(true);
        final var player = robot.player();
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.APPLE));
        player.setShiftKeyDown(false);
        robot.setItem(RobotBlockEntity.TOOL_SLOT, new ItemStack(Items.IRON_SWORD));
        final int[] events = {0, 0};
        final Object protection = new Object() {
            @SubscribeEvent
            public void pre(final li.cil.oc.api.event.RobotAttackEntityEvent.Pre event) {
                if (event.target != target) return;
                events[0]++;
                helper.assertTrue(player.getMainHandItem().is(Items.IRON_SWORD) && player.isShiftKeyDown(), "Attack did not expose tool and sneak state");
                event.setCanceled(true);
            }
            @SubscribeEvent
            public void post(final li.cil.oc.api.event.RobotAttackEntityEvent.Post event) {
                if (event.target == target) events[1]++;
            }
        };
        NeoForge.EVENT_BUS.register(protection);
        try {
            final float health = target.getHealth();
            final Object[] result = ((li.cil.oc.api.network.Component) robot.node()).invoke("swing", null, 3, 3, true);
            helper.assertTrue(Boolean.TRUE.equals(result[0]) && "entity".equals(result[1]), "Canceled attack should still report the targeted entity, as upstream does");
            helper.assertTrue(target.getHealth() == health && events[0] == 1 && events[1] == 0, "Attack bypassed robot protection");
            helper.assertTrue(player.getMainHandItem().is(Items.APPLE) && !player.isShiftKeyDown(), "Attack leaked borrowed player state");
        } finally {
            NeoForge.EVENT_BUS.unregister(protection);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void robotHarvestStoresOreExperienceWithoutDuplicateOrbs(final GameTestHelper helper) throws Exception {
        verifyHarvestExperience(helper, true, false);
    }

    @GameTest(template = "empty")
    public static void robotHarvestWithoutUpgradeLeavesExperienceOrbs(final GameTestHelper helper) throws Exception {
        verifyHarvestExperience(helper, false, false);
    }

    @GameTest(template = "empty")
    public static void robotHarvestCanceledLootDoesNotAwardOreExperience(final GameTestHelper helper) throws Exception {
        verifyHarvestExperience(helper, true, true);
    }

    private static void verifyHarvestExperience(final GameTestHelper helper, final boolean upgraded, final boolean cancelLoot) throws Exception {
        final RobotBlockEntity robot = robot(helper, "while true do computer.pullSignal() end", upgraded);
        final BlockPos target = new BlockPos(1, 1, 2);
        helper.setBlock(target, Blocks.DIAMOND_ORE);
        robot.setItem(RobotBlockEntity.TOOL_SLOT, new ItemStack(Items.IRON_PICKAXE));
        li.cil.oc.common.component.ExperienceUpgradeEnvironment upgrade = null;
        for (final var node : robot.machine().node().reachableNodes()) {
            if (node.host() instanceof li.cil.oc.common.component.ExperienceUpgradeEnvironment found) upgrade = found;
        }
        helper.assertTrue(!upgraded || upgrade != null, "Tier-three robot fixture has no installed experience upgrade");
        final Object modifier = new Object() {
            @SubscribeEvent
            public void onDrops(final net.neoforged.neoforge.event.level.BlockDropsEvent event) {
                if (event.getLevel() == helper.getLevel() && event.getPos().equals(helper.absolutePos(target))) {
                    event.setDroppedExperience(7);
                    if (cancelLoot) event.setCanceled(true);
                }
            }
        };
        NeoForge.EVENT_BUS.register(modifier);
        try {
            helper.assertTrue(Boolean.TRUE.equals(((li.cil.oc.api.network.Component) robot.node()).invoke("swing", null, 3)[0]), "Experience harvest did not start");
            tickRobot(robot, 100);
            helper.assertTrue(helper.getBlockState(target).isAir(), "Experience harvest did not remove ore");
            if (upgrade != null) {
                final var saved = new net.minecraft.nbt.CompoundTag();
                upgrade.save(saved);
                final double expected = ModSettings.robotActionXp() + (cancelLoot ? 0D : 7D * ModSettings.robotOreXpRate());
                helper.assertTrue(Math.abs(saved.getDouble("oc:xp") - expected) < 1e-9, "Experience upgrade lost, duplicated or ignored modified ore XP");
            }
            final int groundExperience = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.ExperienceOrb.class,
                new net.minecraft.world.phys.AABB(helper.absolutePos(target)).inflate(1D)).stream()
                .mapToInt(net.minecraft.world.entity.ExperienceOrb::getValue).sum();
            helper.assertTrue(groundExperience == (upgraded || cancelLoot ? 0 : 7), "Harvest duplicated or lost ground XP");
            helper.assertTrue(cargoCount(robot, Items.DIAMOND) == (cancelLoot ? 0 : 1), "XP handling changed ore drops");
        } finally {
            NeoForge.EVENT_BUS.unregister(modifier);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void robotSwingFallsBackForMissedPartialBlocks(final GameTestHelper helper) throws Exception {
        final RobotBlockEntity robot = robot(helper);
        final BlockPos target = new BlockPos(1, 1, 2);
        helper.setBlock(target, Blocks.STONE_SLAB);
        robot.setItem(RobotBlockEntity.TOOL_SLOT, new ItemStack(Items.IRON_PICKAXE));
        final var component = (li.cil.oc.api.network.Component) robot.node();
        helper.assertTrue(Boolean.TRUE.equals(component.invoke("swing", null, 3, 1)[0]), "Missed calibrated ray did not fall back to the adjacent partial block");
        tickRobot(robot, 100);
        helper.assertTrue(helper.getBlockState(target).isAir(), "Fallback slab dig did not finish");
        helper.setBlock(target, Blocks.STONE_SLAB);
        helper.assertTrue(Boolean.TRUE.equals(component.invoke("swing", null, 3, 0)[0]), "Downward calibrated ray missed bottom slab");
        tickRobot(robot, 100);
        helper.assertTrue(helper.getBlockState(target).isAir(), "Calibrated slab dig did not finish");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void robotSwingExtinguishesFireWithoutToolWear(final GameTestHelper helper) throws Exception {
        final RobotBlockEntity robot = robot(helper);
        final BlockPos target = new BlockPos(1, 1, 2);
        helper.setBlock(target.below(), Blocks.NETHERRACK);
        helper.setBlock(target, Blocks.FIRE);
        final ItemStack tool = new ItemStack(Items.IRON_PICKAXE);
        tool.setDamageValue(7);
        robot.setItem(RobotBlockEntity.TOOL_SLOT, tool);
        final Object[] result = ((li.cil.oc.api.network.Component) robot.node()).invoke("swing", robot.machine(), 3);
        helper.assertTrue(Boolean.TRUE.equals(result[0]) && result.length == 2 && "fire".equals(result[1]), "Fire swing did not report extinguishing");
        helper.assertTrue(helper.getBlockState(target).isAir(), "Fire was not extinguished");
        helper.assertTrue(robot.getItem(RobotBlockEntity.TOOL_SLOT).getDamageValue() == 7, "Extinguishing fire damaged tool");
        helper.assertTrue(robot.machine().isPaused(), "Fire swing did not pause the caller");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void robotCanClearCobwebUsingMiningPick(final GameTestHelper helper) throws Exception {
        final RobotBlockEntity robot = robot(helper);
        final BlockPos target = new BlockPos(1, 1, 2);
        helper.setBlock(target, Blocks.COBWEB);
        robot.setItem(RobotBlockEntity.TOOL_SLOT, new ItemStack(Items.IRON_PICKAXE));
        final boolean override = ModSettings.robotNotAfraidOfSpiders();
        final double delay = ModSettings.ROBOT_SWING_DELAY.get();
        final double ratio = ModSettings.robotHarvestRatio();
        try {
            ModSettings.ROBOT_NOT_AFRAID_OF_SPIDERS.set(false);
            helper.assertTrue(Boolean.FALSE.equals(((li.cil.oc.api.network.Component) robot.node()).invoke("swing", null, 3)[0]),
                "Disabled cobweb override still allowed wrong harvesting tool");
            ModSettings.ROBOT_NOT_AFRAID_OF_SPIDERS.set(true);
            ModSettings.ROBOT_SWING_DELAY.set(0.56D);
            ModSettings.ROBOT_HARVEST_RATIO.set(1D);
            helper.assertTrue(Boolean.TRUE.equals(((li.cil.oc.api.network.Component) robot.node()).invoke("swing", robot.machine(), 3)[0]),
                "Robot with mining pick refused cobweb");
            tickRobot(robot, 9);
            helper.assertTrue(helper.getBlockState(target).is(Blocks.COBWEB), "Cobweb override ignored configured swing time");
            tickRobot(robot, 1);
            helper.assertTrue(helper.getBlockState(target).isAir(), "Robot did not clear cobweb after ten ticks");
            helper.assertTrue(cargoCount(robot, Items.STRING) == 0, "Wrong cobweb tool incorrectly gained harvest drops");
        } finally {
            ModSettings.ROBOT_NOT_AFRAID_OF_SPIDERS.set(override);
            ModSettings.ROBOT_SWING_DELAY.set(delay);
            ModSettings.ROBOT_HARVEST_RATIO.set(ratio);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void robotExtinguishingRespectsProtectionForBothFireTypes(final GameTestHelper helper) throws Exception {
        final RobotBlockEntity robot = robot(helper);
        final BlockPos target = new BlockPos(1, 1, 2);
        final var component = (li.cil.oc.api.network.Component) robot.node();
        for (final var fire : new net.minecraft.world.level.block.Block[]{Blocks.FIRE, Blocks.SOUL_FIRE}) {
            helper.setBlock(target.below(), Blocks.SOUL_SOIL);
            helper.setBlock(target, fire);
            final Object protection = new Object() {
                @SubscribeEvent
                public void onBreak(final BlockEvent.BreakEvent event) {
                    if (event.getLevel() == helper.getLevel() && event.getPos().equals(helper.absolutePos(target))) event.setCanceled(true);
                }
            };
            NeoForge.EVENT_BUS.register(protection);
            try {
                helper.assertTrue(Boolean.FALSE.equals(component.invoke("swing", null, 3)[0]), "Robot bypassed protected fire");
                helper.assertTrue(helper.getBlockState(target).is(fire), "Protected fire was removed");
            } finally {
                NeoForge.EVENT_BUS.unregister(protection);
            }
            final Object[] result = component.invoke("swing", null, 3);
            helper.assertTrue(Boolean.TRUE.equals(result[0]) && "fire".equals(result[1]) && helper.getBlockState(target).isAir(),
                "Robot did not extinguish allowed fire");
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void robotSwingHonorsSeparateBlockAndItemClickDenials(final GameTestHelper helper) throws Exception {
        final RobotBlockEntity robot = robot(helper);
        final BlockPos target = new BlockPos(1, 1, 2);
        helper.setBlock(target, Blocks.REDSTONE_ORE);
        robot.setItem(RobotBlockEntity.TOOL_SLOT, new ItemStack(Items.IRON_PICKAXE));
        final int[] mode = {0};
        final boolean[] sneakyHarvest = {false};
        final Object listener = new Object() {
            @SubscribeEvent
            public void onClick(final net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock event) {
                if (event.getLevel() != helper.getLevel() || !event.getPos().equals(helper.absolutePos(target))) return;
                if (mode[0] == 0) event.setUseItem(net.neoforged.neoforge.common.util.TriState.FALSE);
                if (mode[0] == 1) event.setUseBlock(net.neoforged.neoforge.common.util.TriState.FALSE);
            }
            @SubscribeEvent
            public void onHarvest(final BlockEvent.BreakEvent event) {
                if (event.getLevel() == helper.getLevel() && event.getPos().equals(helper.absolutePos(target))) {
                    sneakyHarvest[0] = event.getPlayer().isShiftKeyDown();
                }
            }
        };
        final var component = (li.cil.oc.api.network.Component) robot.node();
        NeoForge.EVENT_BUS.register(listener);
        try {
            helper.assertTrue(Boolean.FALSE.equals(component.invoke("swing", null, 3)[0]), "Use-item denial started a dig");
            helper.assertTrue(!helper.getBlockState(target).getValue(net.minecraft.world.level.block.RedStoneOreBlock.LIT), "Denied click still attacked ore");
            mode[0] = 1;
            helper.assertTrue(Boolean.TRUE.equals(component.invoke("swing", null, 3, 3, true)[0]), "Use-block denial incorrectly blocked item mining");
            helper.assertTrue(!helper.getBlockState(target).getValue(net.minecraft.world.level.block.RedStoneOreBlock.LIT), "Use-block denial still attacked ore");
            tickRobot(robot, 100);
            helper.assertTrue(helper.getBlockState(target).isAir() && sneakyHarvest[0], "Delayed harvest lost sneak state or did not finish");
            helper.assertTrue(!robot.player().isShiftKeyDown(), "Harvest leaked sneak state");
            mode[0] = 2;
            helper.setBlock(target, Blocks.REDSTONE_ORE);
            helper.assertTrue(Boolean.TRUE.equals(component.invoke("swing", null, 3)[0]), "Normal ore click failed");
            helper.assertTrue(helper.getBlockState(target).getValue(net.minecraft.world.level.block.RedStoneOreBlock.LIT), "Normal click omitted block attack behavior");
            tickRobot(robot, 100);
            helper.assertTrue(helper.getBlockState(target).isAir(), "Block attack state change canceled valid dig");
        } finally {
            NeoForge.EVENT_BUS.unregister(listener);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void robotSwingRejectsUnsupportedSidesAndOppositeCalibration(final GameTestHelper helper) throws Exception {
        final var component = (li.cil.oc.api.network.Component) robot(helper).node();
        for (final Object[] arguments : new Object[][]{{2}, {4}, {5}, {-1}, {6}, {3, 2}, {0, 1}, {1, 0}, {3, 6}}) {
            try {
                component.invoke("swing", null, arguments);
                helper.fail("Swing accepted invalid side arguments " + java.util.Arrays.toString(arguments));
            } catch (final IllegalArgumentException expected) {
                helper.assertTrue("invalid side".equals(expected.getMessage()), "Unexpected side validation error");
            }
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void robotSwingRespectsLeftClickProtectionAndSneaking(final GameTestHelper helper) throws Exception {
        final RobotBlockEntity robot = robot(helper);
        final BlockPos target = new BlockPos(1, 1, 2);
        helper.setBlock(target, Blocks.STONE);
        robot.setItem(RobotBlockEntity.TOOL_SLOT, new ItemStack(Items.IRON_PICKAXE));
        final boolean[] observed = {false};
        final Object protection = new Object() {
            @SubscribeEvent
            public void onClick(final net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.LeftClickBlock event) {
                if (event.getLevel() != helper.getLevel() || !event.getPos().equals(helper.absolutePos(target))) return;
                observed[0] = true;
                helper.assertTrue(event.getEntity().isShiftKeyDown(), "Robot omitted sneak modifier from left click");
                helper.assertTrue(event.getEntity().getMainHandItem().is(Items.IRON_PICKAXE), "Left click did not use equipped tool");
                helper.assertTrue(event.getFace() == Direction.NORTH, "Left click did not use actual hit face");
                event.setCanceled(true);
            }
        };
        NeoForge.EVENT_BUS.register(protection);
        try {
            final Object[] result = ((li.cil.oc.api.network.Component) robot.node()).invoke("swing", null, 3, 1, true);
            helper.assertTrue(observed[0], "Robot skipped left-click event");
            helper.assertTrue(Boolean.FALSE.equals(result[0]), "Robot ignored canceled left click");
            tickRobot(robot, 100);
            helper.assertTrue(helper.getBlockState(target).is(Blocks.STONE), "Canceled left click still broke block");
            helper.assertTrue(robot.getItem(RobotBlockEntity.TOOL_SLOT).getDamageValue() == 0, "Canceled left click wore tool");
            helper.assertTrue(!robot.player().isShiftKeyDown(), "Robot leaked sneak state into next interaction");
        } finally {
            NeoForge.EVENT_BUS.unregister(protection);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void robotDigSynchronizesSwingAnimationAndCancellation(final GameTestHelper helper) throws Exception {
        final RobotBlockEntity robot = robot(helper);
        final BlockPos target = new BlockPos(1, 1, 2);
        helper.setBlock(target, Blocks.STONE);
        robot.setItem(RobotBlockEntity.TOOL_SLOT, new ItemStack(Items.IRON_PICKAXE));
        final Object timing = new Object() {
            @SubscribeEvent
            public void onDig(final li.cil.oc.api.event.RobotBreakBlockEvent.Pre event) {
                if (event.agent == robot) event.setBreakTime(1D);
            }
        };
        NeoForge.EVENT_BUS.register(timing);
        try {
            helper.assertTrue(Boolean.TRUE.equals(((li.cil.oc.api.network.Component) robot.node()).invoke("swing", null, 3)[0]), "Animated dig did not start");
        } finally {
            NeoForge.EVENT_BUS.unregister(timing);
        }
        final var registries = helper.getLevel().registryAccess();
        final var packet = robot.getUpdateTag(registries);
        final var animation = packet.getCompound("oc:animation");
        helper.assertTrue(animation.getBoolean("swing") && animation.getInt("ticks") == 20, "Dig did not synchronize swing and duration");
        final RobotBlockEntity client = new RobotBlockEntity(robot.getBlockPos(), robot.getBlockState());
        client.handleUpdateTag(packet, registries);
        helper.assertTrue(client.getItem(RobotBlockEntity.TOOL_SLOT).is(Items.IRON_PICKAXE), "Swing packet lost equipped tool");
        final long start = animation.getLong("start");
        helper.assertTrue(Math.abs(client.swingRenderOffset(start)) < 1e-5
            && Math.abs(client.swingRenderOffset(start + 5D) - 45F) < 1e-5
            && Math.abs(client.swingRenderOffset(start + 10D)) < 1e-5
            && Math.abs(client.swingRenderOffset(start + 15D) - 45F) < 1e-5
            && client.swingRenderOffset(start + 20D) == 0F, "Swing did not follow upstream repeated arc");
        client.handleUpdateTag(packet, registries);
        helper.assertTrue(Math.abs(client.swingRenderOffset(start + 15D) - 45F) < 1e-5, "Repeated packet restarted swing");
        robot.machine().stop();
        tickRobot(robot, 1);
        client.handleUpdateTag(robot.getUpdateTag(registries), registries);
        helper.assertTrue(!client.getUpdateTag(registries).getCompound("oc:animation").getBoolean("swing"), "Canceled dig retained client swing animation");
        helper.assertTrue(client.swingRenderOffset(start + 5D) == 0F, "Canceled swing still renders");

        helper.assertTrue(robot.toggleMachine(), "Short-swing fixture did not restart");
        final double ratio = ModSettings.robotHarvestRatio();
        try {
            ModSettings.ROBOT_HARVEST_RATIO.set(0D);
            helper.assertTrue(Boolean.TRUE.equals(((li.cil.oc.api.network.Component) robot.node()).invoke("swing", null, 3)[0]), "Short dig did not start");
        } finally {
            ModSettings.ROBOT_HARVEST_RATIO.set(ratio);
        }
        final var shortAnimation = robot.getUpdateTag(registries).getCompound("oc:animation");
        helper.assertTrue(shortAnimation.getInt("ticks") == 5, "Short swing lost upstream minimum duration");
        tickRobot(robot, 1);
        helper.assertTrue(helper.getBlockState(target).isAir(), "Short dig did not complete");
        client.handleUpdateTag(robot.getUpdateTag(registries), registries);
        helper.assertTrue(Math.abs(client.swingRenderOffset(shortAnimation.getLong("start") + 2.5D) - 45F) < 1e-5,
            "Successful short dig ended swing before five ticks");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void robotLuaResumesAfterBlockDigCompletes(final GameTestHelper helper) {
        final RobotBlockEntity robot = robot(helper, """
            local robot = component.proxy(component.list('robot')())
            assert(robot.compare(3), 'compare did not see selected stone')
            assert(robot.swing(3))
            assert(not robot.detect(3), 'swing returned before block was removed')
            robot.setLightColor(0x349ABC)
            while true do computer.pullSignal() end
            """);
        helper.setBlock(new BlockPos(1, 1, 2), Blocks.STONE);
        robot.setItem(RobotBlockEntity.TOOL_SLOT, new ItemStack(Items.IRON_PICKAXE));
        robot.setItem(RobotBlockEntity.CARGO_SLOT_START, new ItemStack(Items.STONE));
        helper.succeedWhen(() -> helper.assertTrue(robot.lightColor() == 0x349ABC,
            "Lua did not resume after dig completion: " + robot.machine().lastError()));
    }

    @GameTest(template = "empty")
    public static void robotDigTimeUsesToolSpeedAndConfiguredRatio(final GameTestHelper helper) throws Exception {
        final RobotBlockEntity robot = robot(helper);
        final BlockPos target = new BlockPos(1, 1, 2);
        helper.setBlock(target, Blocks.STONE);
        robot.setItem(RobotBlockEntity.TOOL_SLOT, new ItemStack(Items.IRON_PICKAXE));
        final double ratio = ModSettings.robotHarvestRatio();
        try {
            ModSettings.ROBOT_HARVEST_RATIO.set(2D);
            helper.assertTrue(Boolean.TRUE.equals(((li.cil.oc.api.network.Component) robot.node()).invoke("swing", null, 3)[0]), "Ratio dig did not start");
        } finally {
            ModSettings.ROBOT_HARVEST_RATIO.set(ratio);
        }
        // Stone hardness 1.5, iron pick speed 6: 1.5 * 1.5 / 6 * 2 = .75 seconds.
        tickRobot(robot, 14);
        helper.assertTrue(helper.getBlockState(target).is(Blocks.STONE), "Dig ignored configured ratio or tool speed");
        tickRobot(robot, 1);
        helper.assertTrue(helper.getBlockState(target).isAir(), "Dig did not finish after calculated fifteen ticks");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void robotSwingWaitsForAdjustedDigTime(final GameTestHelper helper) throws Exception {
        final RobotBlockEntity robot = robot(helper);
        final BlockPos target = new BlockPos(1, 1, 2);
        helper.setBlock(target, Blocks.STONE);
        robot.setItem(RobotBlockEntity.TOOL_SLOT, new ItemStack(Items.IRON_PICKAXE));
        final Object timing = new Object() {
            @SubscribeEvent
            public void onDig(final li.cil.oc.api.event.RobotBreakBlockEvent.Pre event) {
                if (event.agent == robot) event.setBreakTime(0.5D);
            }
        };
        NeoForge.EVENT_BUS.register(timing);
        try {
            helper.assertTrue(Boolean.TRUE.equals(((li.cil.oc.api.network.Component) robot.node()).invoke("swing", robot.machine(), 3)[0]), "Timed swing did not start");
        } finally {
            NeoForge.EVENT_BUS.unregister(timing);
        }
        helper.assertTrue(helper.getBlockState(target).is(Blocks.STONE), "Robot broke block before dig time elapsed");
        helper.assertTrue(robot.machine().isPaused(), "Dig did not pause calling computer");
        tickRobot(robot, 9);
        helper.assertTrue(helper.getBlockState(target).is(Blocks.STONE), "Robot completed dig too early");
        tickRobot(robot, 1);
        helper.assertTrue(helper.getBlockState(target).isAir(), "Robot did not finish ten-tick dig");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void robotSwingCancelsWhenTargetChangesOrMachineStops(final GameTestHelper helper) throws Exception {
        final RobotBlockEntity robot = robot(helper);
        final BlockPos target = new BlockPos(1, 1, 2);
        helper.setBlock(target, Blocks.STONE);
        robot.setItem(RobotBlockEntity.TOOL_SLOT, new ItemStack(Items.IRON_PICKAXE));
        final var component = (li.cil.oc.api.network.Component) robot.node();
        helper.assertTrue(Boolean.TRUE.equals(component.invoke("swing", null, 3)[0]), "Dig did not start");
        helper.setBlock(target, Blocks.DIAMOND_BLOCK);
        tickRobot(robot, 100);
        helper.assertTrue(helper.getBlockState(target).is(Blocks.DIAMOND_BLOCK), "Dig destroyed replacement block");
        helper.assertTrue(robot.getItem(RobotBlockEntity.TOOL_SLOT).getDamageValue() == 0, "Canceled dig wore tool");
        helper.setBlock(target, Blocks.STONE);
        helper.assertTrue(Boolean.TRUE.equals(component.invoke("swing", null, 3)[0]), "Second dig did not start");
        robot.machine().stop();
        tickRobot(robot, 100);
        helper.assertTrue(helper.getBlockState(target).is(Blocks.STONE), "Stopped robot continued digging");
        helper.assertTrue(robot.getItem(RobotBlockEntity.TOOL_SLOT).getDamageValue() == 0, "Stopped robot wore tool");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void robotSwingUsesEquippedToolAndHarvestsOnce(final GameTestHelper helper) throws Exception {
        final RobotBlockEntity robot = robot(helper);
        final BlockPos target = new BlockPos(1, 1, 2);
        helper.setBlock(target, Blocks.STONE);
        final ItemStack tool = new ItemStack(Items.IRON_PICKAXE);
        tool.setDamageValue(7);
        robot.setItem(RobotBlockEntity.TOOL_SLOT, tool);
        robot.setItem(RobotBlockEntity.CARGO_SLOT_START, new ItemStack(Items.DIRT, 4));

        final double previousRate = ModSettings.robotItemDamageRate();
        final Object[] result;
        try {
            ModSettings.ROBOT_ITEM_DAMAGE_RATE.set(1D);
            result = ((li.cil.oc.api.network.Component) robot.node()).invoke("swing", null, 3);
            tickRobot(robot, 100);
        } finally {
            ModSettings.ROBOT_ITEM_DAMAGE_RATE.set(previousRate);
        }
        helper.assertTrue(Boolean.TRUE.equals(result[0]) && helper.getBlockState(target).isAir(), "Equipped pickaxe did not break stone");
        helper.assertTrue(robot.getItem(RobotBlockEntity.TOOL_SLOT).getDamageValue() == 8, "Swing did not damage equipped tool exactly once");
        helper.assertTrue(robot.getItem(RobotBlockEntity.CARGO_SLOT_START).getCount() == 4, "Swing changed selected cargo");
        helper.assertTrue(result.length == 2 && "block".equals(result[1]), "Successful swing must identify block interaction");
        final long drops = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
            new net.minecraft.world.phys.AABB(helper.absolutePos(target)).inflate(1D)).stream()
            .filter(entity -> entity.getItem().is(Items.COBBLESTONE)).mapToLong(entity -> entity.getItem().getCount()).sum();
        helper.assertTrue(drops == 0 && cargoCount(robot, Items.COBBLESTONE) == 1, "Swing did not collect harvested stone exactly once");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void robotHarvestCollectsOnlyNewDropsAndLeavesOverflow(final GameTestHelper helper) throws Exception {
        final RobotBlockEntity robot = robot(helper);
        final BlockPos target = new BlockPos(1, 1, 2);
        helper.setBlock(target, Blocks.STONE);
        robot.setItem(RobotBlockEntity.TOOL_SLOT, new ItemStack(Items.IRON_PICKAXE));
        for (int slot = 0; slot < RobotBlockEntity.CARGO_SLOT_COUNT; slot++) {
            robot.setItem(RobotBlockEntity.CARGO_SLOT_START + slot, new ItemStack(Items.DIRT, 64));
        }
        robot.setItem(RobotBlockEntity.CARGO_SLOT_START, new ItemStack(Items.COBBLESTONE, 63));
        final var pos = net.minecraft.world.phys.Vec3.atCenterOf(helper.absolutePos(target));
        final var existing = new net.minecraft.world.entity.item.ItemEntity(helper.getLevel(), pos.x, pos.y, pos.z, new ItemStack(Items.COBBLESTONE, 5));
        helper.getLevel().addFreshEntity(existing);
        final boolean[] pickupReported = {false};
        final Object drops = new Object() {
            @SubscribeEvent
            public void onDrops(final net.neoforged.neoforge.event.level.BlockDropsEvent event) {
                if (event.getLevel() == helper.getLevel() && event.getPos().equals(helper.absolutePos(target))) {
                    event.getDrops().getFirst().getItem().setCount(3);
                }
            }
            @SubscribeEvent
            public void onPickup(final net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent.Post event) {
                if (event.getPlayer() == robot.player()) {
                    helper.assertTrue(event.getOriginalStack().getCount() == 3 && event.getCurrentStack().getCount() == 2,
                        "Partial pickup event did not report original and remaining amounts");
                    pickupReported[0] = true;
                }
            }
        };
        NeoForge.EVENT_BUS.register(drops);
        try {
            helper.assertTrue(Boolean.TRUE.equals(((li.cil.oc.api.network.Component) robot.node()).invoke("swing", null, 3)[0]), "Overflow dig did not start");
            tickRobot(robot, 100);
            helper.assertTrue(cargoCount(robot, Items.COBBLESTONE) == 64, "Harvest did not fill the one free stack space");
            helper.assertTrue(!existing.isRemoved() && existing.getItem().getCount() == 5, "Harvest stole a pre-existing item entity");
            final int overflow = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                new net.minecraft.world.phys.AABB(helper.absolutePos(target)).inflate(1D)).stream()
                .filter(entity -> entity != existing && entity.getItem().is(Items.COBBLESTONE))
                .mapToInt(entity -> entity.getItem().getCount()).sum();
            helper.assertTrue(overflow == 2, "Harvest lost or duplicated overflow");
            helper.assertTrue(pickupReported[0], "Partial harvest omitted pickup event");
        } finally {
            NeoForge.EVENT_BUS.unregister(drops);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void robotHarvestHonorsPickupDenial(final GameTestHelper helper) throws Exception {
        final RobotBlockEntity robot = robot(helper);
        final BlockPos target = new BlockPos(1, 1, 2);
        helper.setBlock(target, Blocks.STONE);
        robot.setItem(RobotBlockEntity.TOOL_SLOT, new ItemStack(Items.IRON_PICKAXE));
        final boolean[] observed = {false};
        final Object protection = new Object() {
            @SubscribeEvent
            public void onPickup(final net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent.Pre event) {
                if (event.getPlayer() == robot.player()) {
                    observed[0] = true;
                    event.setCanPickup(net.neoforged.neoforge.common.util.TriState.FALSE);
                }
            }
        };
        NeoForge.EVENT_BUS.register(protection);
        try {
            helper.assertTrue(Boolean.TRUE.equals(((li.cil.oc.api.network.Component) robot.node()).invoke("swing", null, 3)[0]), "Denied-pickup dig did not start");
            tickRobot(robot, 100);
            helper.assertTrue(observed[0], "Harvest skipped pickup protection");
            helper.assertTrue(cargoCount(robot, Items.COBBLESTONE) == 0, "Robot ignored pickup denial");
            final int left = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                new net.minecraft.world.phys.AABB(helper.absolutePos(target)).inflate(1D)).stream()
                .filter(entity -> entity.getItem().is(Items.COBBLESTONE)).mapToInt(entity -> entity.getItem().getCount()).sum();
            helper.assertTrue(left == 1, "Denied pickup lost the item");
        } finally {
            NeoForge.EVENT_BUS.unregister(protection);
        }
        helper.succeed();
    }

    private static int cargoCount(final RobotBlockEntity robot, final net.minecraft.world.item.Item item) {
        int count = 0;
        for (int slot = 0; slot < RobotBlockEntity.CARGO_SLOT_COUNT; slot++) {
            final ItemStack stack = robot.getItem(RobotBlockEntity.CARGO_SLOT_START + slot);
            if (stack.is(item)) count += stack.getCount();
        }
        return count;
    }

    @GameTest(template = "empty")
    public static void robotSwingHonorsConfiguredAndEventAdjustedWear(final GameTestHelper helper) throws Exception {
        final RobotBlockEntity robot = robot(helper);
        final BlockPos target = new BlockPos(1, 1, 2);
        final double previousRate = ModSettings.robotItemDamageRate();
        final boolean[] observedDamage = {false};
        final Object modifier = new Object() {
            @SubscribeEvent
            public void onDamage(final RobotUsedToolEvent.ComputeDamageRate event) {
                if (event.agent != robot) return;
                helper.assertTrue(event.toolBeforeUse.getDamageValue() == 7 && event.toolAfterUse.getDamageValue() == 8,
                    "Damage modifier did not receive actual tool wear");
                helper.assertTrue(event.getDamageRate() == 0.1D, "Damage modifier did not receive configured base rate");
                observedDamage[0] = true;
                event.setDamageRate(0D);
            }
        };
        try {
            ModSettings.ROBOT_ITEM_DAMAGE_RATE.set(0D);
            helper.setBlock(target, Blocks.STONE);
            final ItemStack tool = new ItemStack(Items.IRON_PICKAXE);
            tool.setDamageValue(7);
            robot.setItem(RobotBlockEntity.TOOL_SLOT, tool);
            helper.assertTrue(Boolean.TRUE.equals(((li.cil.oc.api.network.Component) robot.node()).invoke("swing", null, 3)[0]), "Zero-wear robot could not harvest");
            tickRobot(robot, 100);
            helper.assertTrue(robot.getItem(RobotBlockEntity.TOOL_SLOT).getDamageValue() == 7, "Zero configured rate still damaged tool");

            ModSettings.ROBOT_ITEM_DAMAGE_RATE.set(0.1D);
            helper.setBlock(target, Blocks.STONE);
            NeoForge.EVENT_BUS.register(modifier);
            helper.assertTrue(Boolean.TRUE.equals(((li.cil.oc.api.network.Component) robot.node()).invoke("swing", null, 3)[0]), "Modified-wear robot could not harvest");
            tickRobot(robot, 100);
            helper.assertTrue(observedDamage[0], "Swing skipped damage modifier");
            helper.assertTrue(robot.getItem(RobotBlockEntity.TOOL_SLOT).getDamageValue() == 7, "Swing ignored modified damage rate");
        } finally {
            NeoForge.EVENT_BUS.unregister(modifier);
            ModSettings.ROBOT_ITEM_DAMAGE_RATE.set(previousRate);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void robotSwingRespectsHarvestToolAndBlockProtection(final GameTestHelper helper) throws Exception {
        final RobotBlockEntity robot = robot(helper);
        final BlockPos target = new BlockPos(1, 1, 2);
        helper.setBlock(target, Blocks.DIAMOND_ORE);
        robot.setItem(RobotBlockEntity.CARGO_SLOT_START, new ItemStack(Items.IRON_PICKAXE));
        helper.assertTrue(Boolean.FALSE.equals(((li.cil.oc.api.network.Component) robot.node()).invoke("swing", null, 3)[0]), "Robot harvested ore using a cargo tool instead of equipped tool");
        helper.assertTrue(helper.getBlockState(target).is(Blocks.DIAMOND_ORE), "Failed harvest removed ore");
        robot.setItem(RobotBlockEntity.TOOL_SLOT, new ItemStack(Items.IRON_PICKAXE));
        final boolean[] protectionCalled = {false};
        final Object protection = new Object() {
            @SubscribeEvent
            public void onBreak(final BlockEvent.BreakEvent event) {
                if (event.getLevel() == helper.getLevel() && event.getPos().equals(helper.absolutePos(target))) {
                    protectionCalled[0] = true;
                    event.setCanceled(true);
                }
            }
        };
        NeoForge.EVENT_BUS.register(protection);
        try {
            helper.assertTrue(Boolean.TRUE.equals(((li.cil.oc.api.network.Component) robot.node()).invoke("swing", null, 3)[0]), "Robot did not start protected-block dig");
            tickRobot(robot, 100);
            helper.assertTrue(protectionCalled[0], "Robot skipped standard block protection");
            helper.assertTrue(helper.getBlockState(target).is(Blocks.DIAMOND_ORE), "Protected ore was removed");
            helper.assertTrue(robot.getItem(RobotBlockEntity.TOOL_SLOT).getDamageValue() == 0, "Blocked swing damaged tool");
        } finally {
            NeoForge.EVENT_BUS.unregister(protection);
        }
        helper.succeed();
    }

    static RobotBlockEntity robot(final GameTestHelper helper) {
        return robot(helper, "while true do computer.pullSignal() end");
    }

    private static RobotBlockEntity robot(final GameTestHelper helper, final String code) {
        return robot(helper, code, false);
    }

    private static RobotBlockEntity robot(final GameTestHelper helper, final String code, final boolean experience) {
        final BlockPos pos = new BlockPos(1, 1, 1);
        helper.setBlock(pos, ModBlocks.ROBOT.get().defaultBlockState().setValue(RobotBlock.FACING, Direction.SOUTH));
        final RobotBlockEntity robot = helper.getBlockEntity(pos);
        final var parts = new java.util.ArrayList<>(java.util.List.of(
            new ItemStack(li.cil.oc.common.ModItems.CPU_TIER1.get()), new ItemStack(li.cil.oc.common.ModItems.MEMORY_TIER1.get()),
            RobotMovementPersistenceGameTests.eeprom(code)));
        if (experience) {
            robot.setTier(2);
            parts.add(new ItemStack(li.cil.oc.common.ModItems.EXPERIENCE_UPGRADE.get()));
        }
        RobotMovementPersistenceGameTests.installHardware(helper, robot, parts);
        robot.onLoad();
        ((li.cil.oc.api.network.Connector) robot.machine().node()).changeBuffer(10000D);
        helper.assertTrue(robot.toggleMachine(), "Interaction fixture did not start");
        return robot;
    }

    static void tickRobot(final RobotBlockEntity robot, final int ticks) {
        for (int tick = 0; tick < ticks; tick++) {
            RobotBlockEntity.serverTick(robot.getLevel(), robot.getBlockPos(), robot.getBlockState(), robot);
        }
    }
}
