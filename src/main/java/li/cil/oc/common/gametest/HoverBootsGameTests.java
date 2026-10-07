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
