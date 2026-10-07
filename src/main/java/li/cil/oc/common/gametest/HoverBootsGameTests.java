package li.cil.oc.common.gametest;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.api.driver.item.Chargeable;
import li.cil.oc.common.ModItemCharges;
import li.cil.oc.common.ModSettings;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(NeoOpenComputers.MODID)
@PrefixGameTestTemplate(false)
public final class HoverBootsGameTests {
    @GameTest(template = "empty")
    public static void hoverBootsCraftAndCreativeCharge(GameTestHelper helper) {
        final var recipes = helper.getLevel().getRecipeManager();
        final var holder = recipes.byKey(ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "hover_boots"));
        helper.assertTrue(holder.isPresent(), "Hover boots crafting recipe is missing");
        @SuppressWarnings("unchecked")
        final var recipe = (net.minecraft.world.item.crafting.Recipe<net.minecraft.world.item.crafting.CraftingInput>) holder.orElseThrow().value();
        final var grid = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, java.util.List.of(
            new ItemStack(Items.IRON_NUGGET), new ItemStack(li.cil.oc.common.ModItems.HOVER_UPGRADE_TIER2.get()), new ItemStack(Items.IRON_NUGGET),
            new ItemStack(Items.LEATHER), new ItemStack(li.cil.oc.common.ModItems.DRONE_CASE_TIER1.get()), new ItemStack(Items.LEATHER),
            new ItemStack(Items.IRON_NUGGET), new ItemStack(li.cil.oc.common.ModItems.CAPACITOR.get()), new ItemStack(Items.IRON_NUGGET)));
        helper.assertTrue(recipe.matches(grid, helper.getLevel()), "Upstream hover boots ingredients do not craft");
        final var result = recipe.assemble(grid, helper.getLevel().registryAccess());
        helper.assertTrue(result.is(li.cil.oc.common.ModItems.HOVER_BOOTS.get()) && result.getCount() == 1
            && li.cil.oc.common.ModItems.HOVER_BOOTS.get().getCharge(result) == 0, "Survival recipe produced incorrect or charged boots");
        final var wrong = new java.util.ArrayList<>(grid.items());
        wrong.set(1, new ItemStack(li.cil.oc.common.ModItems.HOVER_UPGRADE_TIER1.get()));
        helper.assertTrue(!recipe.matches(net.minecraft.world.item.crafting.CraftingInput.of(3, 3, wrong), helper.getLevel()), "Boot recipe accepted wrong hover upgrade tier");
        final var tab = li.cil.oc.common.ModCreativeTabs.MAIN.get();
        tab.buildContents(new net.minecraft.world.item.CreativeModeTab.ItemDisplayParameters(
            helper.getLevel().enabledFeatures(), true, helper.getLevel().registryAccess()));
        helper.assertTrue(tab.getDisplayItems().stream().anyMatch(stack -> stack.is(li.cil.oc.common.ModItems.HOVER_BOOTS.get())
            && li.cil.oc.common.ModItems.HOVER_BOOTS.get().getCharge(stack) == ModSettings.hoverBootsBuffer()), "Creative tab lacks charged boots");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void hoverBootsColorAndWashPreserveCharge(GameTestHelper helper) {
        final var recipes = helper.getLevel().getRecipeManager();
        final var colorHolder = recipes.byKey(ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "colorize_hover_boots"));
        final var washHolder = recipes.byKey(ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "decolorize_hover_boots"));
        helper.assertTrue(colorHolder.isPresent() && washHolder.isPresent(), "Hover boots dye/wash recipes are missing");
        @SuppressWarnings("unchecked")
        final var color = (net.minecraft.world.item.crafting.Recipe<net.minecraft.world.item.crafting.CraftingInput>) colorHolder.orElseThrow().value();
        @SuppressWarnings("unchecked")
        final var wash = (net.minecraft.world.item.crafting.Recipe<net.minecraft.world.item.crafting.CraftingInput>) washHolder.orElseThrow().value();
        final var boots = li.cil.oc.common.ModItems.HOVER_BOOTS.get();
        final var stack = new ItemStack(boots);
        boots.setCharge(stack, 123);
        stack.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("fixture-boots"));
        final var input = net.minecraft.world.item.crafting.CraftingInput.of(3, 1,
            java.util.List.of(stack, new ItemStack(Items.RED_DYE), new ItemStack(Items.BLUE_DYE)));
        helper.assertTrue(color.matches(input, helper.getLevel()), "Boots do not accept multiple dyes");
        final var dyed = color.assemble(input, helper.getLevel().registryAccess());
        helper.assertTrue(dyed.get(DataComponents.DYED_COLOR).rgb() == 0xAD5398 && boots.getCharge(dyed) == 123
            && dyed.get(DataComponents.CUSTOM_NAME).equals(stack.get(DataComponents.CUSTOM_NAME)) && !stack.has(DataComponents.DYED_COLOR),
            "Dyeing lost charge/name or mutated input");
        final var washInput = net.minecraft.world.item.crafting.CraftingInput.of(2, 1, java.util.List.of(dyed, new ItemStack(Items.WATER_BUCKET)));
        helper.assertTrue(wash.matches(washInput, helper.getLevel()), "Water bucket cannot wash boots");
        final var washed = wash.assemble(washInput, helper.getLevel().registryAccess());
        helper.assertTrue(!washed.has(DataComponents.DYED_COLOR) && boots.getCharge(washed) == 123 && dyed.has(DataComponents.DYED_COLOR)
            && wash.getRemainingItems(washInput).get(1).is(Items.BUCKET), "Bucket washing lost charge, mutated input or consumed the bucket");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void droppedHoverBootsWashInWaterCauldron(GameTestHelper helper) {
        final var world = helper.getLevel();
        final var pos = helper.absolutePos(new net.minecraft.core.BlockPos(1, 2, 1));
        final var boots = li.cil.oc.common.ModItems.HOVER_BOOTS.get();
        final var stack = new ItemStack(boots);
        boots.setCharge(stack, 123);
        stack.set(DataComponents.DYED_COLOR, new net.minecraft.world.item.component.DyedItemColor(0x123456, true));
        final var entity = new net.minecraft.world.entity.item.ItemEntity(world, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
        world.setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.WATER_CAULDRON.defaultBlockState()
            .setValue(net.minecraft.world.level.block.LayeredCauldronBlock.LEVEL, 1));
        entity.tick();
        helper.assertTrue(!entity.getItem().has(DataComponents.DYED_COLOR) && boots.getCharge(entity.getItem()) == 123
            && world.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.CAULDRON), "Dropped boots did not wash and empty a level-one water cauldron");
        world.setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.WATER_CAULDRON.defaultBlockState()
            .setValue(net.minecraft.world.level.block.LayeredCauldronBlock.LEVEL, 3));
        entity.tick();
        helper.assertTrue(world.getBlockState(pos).getValue(net.minecraft.world.level.block.LayeredCauldronBlock.LEVEL) == 3,
            "Uncolored boots consumed cauldron water");
        entity.getItem().set(DataComponents.DYED_COLOR, new net.minecraft.world.item.component.DyedItemColor(0x123456, true));
        world.setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.POWDER_SNOW_CAULDRON.defaultBlockState());
        entity.tick();
        helper.assertTrue(entity.getItem().has(DataComponents.DYED_COLOR), "Powder snow washed boots");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void hoverBootsMovementChargesOnIntervalAndRestoresOtherModifiers(GameTestHelper helper) {
        final var player = helper.makeMockPlayer(GameType.SURVIVAL);
        final var boots = li.cil.oc.common.ModItems.HOVER_BOOTS.get();
        final var stack = new ItemStack(boots);
        player.setItemSlot(EquipmentSlot.FEET, stack);
        boots.setCharge(stack, 2);
        player.setOnGround(true);
        player.setDeltaMovement(0.2, 0, 0);
        final var height = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.STEP_HEIGHT);
        final var other = new net.minecraft.world.entity.ai.attributes.AttributeModifier(
            ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "test_step"), 0.2,
            net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE);
        height.addTransientModifier(other);
        final double original = height.getValue();
        final var clock = (net.minecraft.world.level.storage.ServerLevelData) helper.getLevel().getLevelData();
        final long originalTime = clock.getGameTime();
        try {
            clock.setGameTime(0);
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Pre(player));
            helper.assertTrue(boots.getCharge(stack) == 1 && Math.abs(height.getValue() - original - 0.4) < 0.00001,
                "Moving boots did not spend interval energy or preserve other step modifiers");
            clock.setGameTime(1);
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Pre(player));
            helper.assertTrue(boots.getCharge(stack) == 1, "Movement charged outside its interval");
            clock.setGameTime(10);
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Pre(player));
            helper.assertTrue(boots.getCharge(stack) == 0 && height.getValue() == original && height.hasModifier(other.id()),
                "Depletion did not restore the previous step height");
            boots.setCharge(stack, 10);
            player.setDeltaMovement(0, 0, 0);
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Pre(player));
            helper.assertTrue(boots.getCharge(stack) == 10, "Stationary boots drained energy");
            player.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Pre(player));
            helper.assertTrue(height.getValue() == original && height.hasModifier(other.id()), "Removing boots changed another step modifier");
        } finally {
            clock.setGameTime(originalTime);
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void hoverBootsActualJumpFallAndCreativeExemption(GameTestHelper helper) {
        final var boots = li.cil.oc.common.ModItems.HOVER_BOOTS.get();
        final var stack = new ItemStack(boots);
        boots.setCharge(stack, 20);
        final var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemSlot(EquipmentSlot.FEET, stack);
        player.jumpFromGround();
        helper.assertTrue(player.getDeltaMovement().y > 0.8 && boots.getCharge(stack) == 10,
            "Actual Minecraft jump did not dispatch the boot boost");
        final float health = player.getHealth();
        player.causeFallDamage(10, 1, helper.getLevel().damageSources().fall());
        helper.assertTrue(player.getHealth() == health && boots.getCharge(stack) == 0,
            "Actual Minecraft fall did not dispatch absorption");
        final var creative = helper.makeMockPlayer(GameType.CREATIVE);
        creative.setItemSlot(EquipmentSlot.FEET, stack);
        creative.jumpFromGround();
        helper.assertTrue(creative.getDeltaMovement().y > 0.8 && boots.getCharge(stack) == 0,
            "Creative jump required or consumed charge");
        final var fake = new net.neoforged.neoforge.common.util.FakePlayer(helper.getLevel(),
            new com.mojang.authlib.GameProfile(new java.util.UUID(0, 123), "hover_fixture"));
        boots.setCharge(stack, 20);
        fake.setItemSlot(EquipmentSlot.FEET, stack);
        fake.jumpFromGround();
        helper.assertTrue(fake.getDeltaMovement().y < 0.5 && boots.getCharge(stack) == 20, "Fake player received a hover jump");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void hoverBootsJumpAndFallUseExactEnergy(GameTestHelper helper) {
        final var player = helper.makeMockPlayer(GameType.SURVIVAL);
        final var stack = new ItemStack(li.cil.oc.common.ModItems.HOVER_BOOTS.get());
        final var boots = li.cil.oc.common.ModItems.HOVER_BOOTS.get();
        player.setItemSlot(EquipmentSlot.FEET, stack);
        boots.setCharge(stack, 30);
        player.setDeltaMovement(0.2, 0.42, 0.3);
        player.setSprinting(true);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.living.LivingEvent.LivingJumpEvent(player));
        helper.assertTrue(Math.abs(player.getDeltaMovement().y - 0.82) < 0.00001
            && Math.abs(player.getDeltaMovement().x - 0.3) < 0.00001
            && Math.abs(player.getDeltaMovement().z - 0.45) < 0.00001 && boots.getCharge(stack) == 20,
            "Sprint jump boost or energy cost is missing");
        player.setShiftKeyDown(true);
        final var fall = new net.neoforged.neoforge.event.entity.living.LivingFallEvent(player, 10, 1);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(fall);
        helper.assertTrue(fall.getDistance() == 3 && boots.getCharge(stack) == 10, "Sneaking fall absorption did not match upstream");
        final var before = player.getDeltaMovement();
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.living.LivingEvent.LivingJumpEvent(player));
        helper.assertTrue(player.getDeltaMovement().equals(before) && boots.getCharge(stack) == 10, "Sneaking jump spent charge or boosted");
        player.setShiftKeyDown(false);
        boots.setCharge(stack, 9);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.living.LivingEvent.LivingJumpEvent(player));
        final var unpaidFall = new net.neoforged.neoforge.event.entity.living.LivingFallEvent(player, 10, 1);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(unpaidFall);
        helper.assertTrue(player.getDeltaMovement().equals(before) && unpaidFall.getDistance() == 10 && boots.getCharge(stack) == 9,
            "Insufficient charge gave a benefit or partially consumed energy");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void hoverBootsTickRestoresStepAndSlowsEmptyBoots(GameTestHelper helper) {
        final var player = helper.makeMockPlayer(GameType.SURVIVAL);
        final var boots = li.cil.oc.common.ModItems.HOVER_BOOTS.get();
        final var stack = new ItemStack(boots);
        player.setItemSlot(EquipmentSlot.FEET, stack);
        boots.setCharge(stack, 10);
        player.setOnGround(false);
        player.fallDistance = 4;
        player.setDeltaMovement(0.2, -1, 0.3);
        final var height = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.STEP_HEIGHT);
        final double original = height.getValue();
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Pre(player));
        helper.assertTrue(Math.abs(player.getDeltaMovement().y + 0.9) < 0.00001 && Math.abs(height.getValue() - (original + 0.4)) < 0.00001,
            "Charged boots did not hover or raise step height");
        player.setShiftKeyDown(true);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Pre(player));
        helper.assertTrue(height.getValue() == original && Math.abs(player.getDeltaMovement().y + 0.9) < 0.00001,
            "Sneaking did not restore step height and disable hover");
        boots.setCharge(stack, 0);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Pre(player));
        final var slowness = player.getEffect(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN);
        helper.assertTrue(slowness != null && slowness.getAmplifier() == 1 && slowness.getDuration() == 20,
            "Empty boots did not apply upstream slowness");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void chargerTransfersEnergyIntoHoverBoots(GameTestHelper helper) {
        final var pos = helper.absolutePos(new net.minecraft.core.BlockPos(1, 2, 1));
        final var world = helper.getLevel();
        world.setBlockAndUpdate(pos, li.cil.oc.common.ModBlocks.CHARGER.get().defaultBlockState());
        final var charger = (li.cil.oc.common.blockentity.ChargerBlockEntity) world.getBlockEntity(pos);
        li.cil.oc.api.Network.joinOrCreateNetwork(world, pos);
        final var node = (li.cil.oc.api.network.Connector) charger.node();
        node.changeBuffer(1000);
        final var stack = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "hover_boots")));
        helper.assertTrue(charger.canPlaceItem(0, stack), "Charger rejected hover boots");
        charger.setItem(0, stack);
        charger.setChargeSpeed(1);
        final double before = node.globalBuffer();
        helper.assertTrue(charger.runChargeCycle(), "Charger did not charge hover boots");
        final double used = before - node.globalBuffer();
        helper.assertTrue(used > 0 && stack.getCapability(Capabilities.EnergyStorage.ITEM).getEnergyStored() == ModSettings.toForgeEnergy(used),
            "Charging hover boots did not conserve source and destination energy");
        ((Chargeable) stack.getItem()).charge(stack, Double.MAX_VALUE, false);
        final double fullBuffer = node.globalBuffer();
        helper.assertTrue(!charger.runChargeCycle() && node.globalBuffer() == fullBuffer,
            "Full hover boots drained charger energy");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void hoverBootsConsumeChargeOnActualArmorDamage(GameTestHelper helper) {
        final var item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "hover_boots"));
        final var stack = new ItemStack(item);
        ((Chargeable) item).charge(stack, 100, false);
        final var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemSlot(EquipmentSlot.FEET, stack);
        final var attacker = net.minecraft.world.entity.EntityType.ZOMBIE.create(helper.getLevel());
        final var source = helper.getLevel().damageSources().mobAttack(attacker);
        helper.assertTrue(!source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_ARMOR), "Armor fixture source bypasses armor");
        helper.assertTrue(player.hurt(source, 8), "Player armor damage fixture did not take a hit");
        helper.assertTrue(stack.getCapability(Capabilities.EnergyStorage.ITEM).getEnergyStored() == ModSettings.toForgeEnergy(98)
            && stack.getDamageValue() == 0 && stack.getCount() == 1, "Actual armor hit did not consume energy while preserving boots: energy="
                + stack.getCapability(Capabilities.EnergyStorage.ITEM).getEnergyStored() + ", damage=" + stack.getDamageValue()
                + ", count=" + stack.getCount() + ", damageable=" + stack.isDamageableItem());
        stack.hurtAndBreak(Integer.MAX_VALUE, player, EquipmentSlot.FEET);
        helper.assertTrue(stack.getCount() == 1 && stack.getDamageValue() == 0
            && stack.getCapability(Capabilities.EnergyStorage.ITEM).getEnergyStored() == 0,
            "A large armor hit broke boots or underflowed charge");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void hoverBootsArmorChargesAndPersists(GameTestHelper helper) {
        final var registered = BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "hover_boots"));
        helper.assertTrue(registered.isPresent(), "Player hover boots are not registered");
        final var item = registered.orElseThrow();
        helper.assertTrue(li.cil.oc.api.API.items.get("hoverboots") != null
            && li.cil.oc.api.API.items.get("hoverboots").createItemStack(1).getItem() == item, "Upstream hoverboots item API alias is missing");
        helper.assertTrue(item instanceof ArmorItem && item instanceof Chargeable, "Hover boots are not chargeable armor");
        final var armor = (ArmorItem) item;
        final var chargeable = (Chargeable) item;
        final var stack = new ItemStack(item);
        helper.assertTrue(armor.getEquipmentSlot() == EquipmentSlot.FEET && armor.getDefense() == 3
            && stack.getMaxStackSize() == 1 && stack.getDamageValue() == 0, "Hover boots lack upstream armor/stack/durability behavior");
        helper.assertTrue(!armor.isRepairable(stack) && !armor.isValidRepairItem(stack, new ItemStack(Items.DIAMOND)), "Hover boots accepted repair");
        final var tag = new CompoundTag();
        tag.putString("fixture", "keep");
        tag.putDouble("oc:charge", 100);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        final var energy = stack.getCapability(Capabilities.EnergyStorage.ITEM);
        helper.assertTrue(energy != null && energy.getEnergyStored() == ModSettings.toForgeEnergy(100), "Legacy root charge was not exposed through FE");
        helper.assertTrue(chargeable.charge(stack, 50, true) == 50 && energy.getEnergyStored() == ModSettings.toForgeEnergy(100),
            "Charge simulation mutated boots");
        helper.assertTrue(ModItemCharges.charge(stack, 50, false) == 0
            && energy.getEnergyStored() == ModSettings.toForgeEnergy(150), "OC charge callback did not charge boots");
        helper.assertTrue(chargeable.charge(stack, -20, true) == -20 && energy.getEnergyStored() == ModSettings.toForgeEnergy(150),
            "Discharge simulation mutated boots");
        helper.assertTrue(chargeable.charge(stack, -20, false) == -20 && energy.getEnergyStored() == ModSettings.toForgeEnergy(130),
            "Discharging did not consume exact energy");
        stack.setDamageValue(7);
        helper.assertTrue(stack.getDamageValue() == 0 && energy.getEnergyStored() == ModSettings.toForgeEnergy(123),
            "Armor damage did not consume energy instead of durability");
        final var saved = stack.save(helper.getLevel().registryAccess());
        final var loaded = ItemStack.parseOptional(helper.getLevel().registryAccess(), (CompoundTag) saved);
        helper.assertTrue(loaded.getCapability(Capabilities.EnergyStorage.ITEM).getEnergyStored() == ModSettings.toForgeEnergy(123)
            && "keep".equals(loaded.get(DataComponents.CUSTOM_DATA).copyTag().getString("fixture")), "Boot charge/data was lost in serialization");
        helper.assertTrue(chargeable.charge(stack, -Double.MAX_VALUE, false) == -123
            && energy.getEnergyStored() == 0, "Boot energy underflowed");
        chargeable.charge(stack, Double.MAX_VALUE, false);
        helper.assertTrue(energy.getMaxEnergyStored() == ModSettings.toForgeEnergy(15000)
            && energy.getEnergyStored() == energy.getMaxEnergyStored()
            && chargeable.charge(stack, 10, false) == 0, "Boot capacity/overflow did not match upstream");
        helper.assertTrue(energy.extractEnergy(100, false) == 0 && !energy.canExtract(), "FE could extract boot energy");
        final var player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, loaded);
        helper.assertTrue(item.use(helper.getLevel(), player, InteractionHand.MAIN_HAND).getResult().consumesAction()
            && player.getItemBySlot(EquipmentSlot.FEET).getItem() == item,
            "Hover boots could not be equipped through the normal armor action");
        helper.assertTrue(player.getItemBySlot(EquipmentSlot.FEET).getCapability(Capabilities.EnergyStorage.ITEM).getEnergyStored()
            == ModSettings.toForgeEnergy(123), "Equipping boots lost their charge");
        helper.succeed();
    }
}
