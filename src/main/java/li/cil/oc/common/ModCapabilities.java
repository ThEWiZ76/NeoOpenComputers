package li.cil.oc.common;

import li.cil.oc.common.blockentity.AssemblerBlockEntity;
import li.cil.oc.common.blockentity.ChargerBlockEntity;
import li.cil.oc.common.blockentity.ComputerCaseBlockEntity;
import li.cil.oc.common.blockentity.DisassemblerBlockEntity;
import li.cil.oc.common.blockentity.PowerConverterBlockEntity;
import li.cil.oc.common.blockentity.RelayBlockEntity;
import li.cil.oc.common.blockentity.RobotBlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public final class ModCapabilities {
    private ModCapabilities() {
    }

    public static void register(final RegisterCapabilitiesEvent event) {
        li.cil.oc.common.integration.ComputerCraftRelayIntegration.register(event);
        if (Boolean.getBoolean("neoopencomputers.testComputerCraft") && net.neoforged.fml.ModList.get().isLoaded("computercraft")) {
            li.cil.oc.common.gametest.TestComputerCraftCapabilities.register(event);
            li.cil.oc.common.gametest.TestComputerCraftMediaCapabilities.register(event);
        }
        if (Boolean.getBoolean("neoopencomputers.testFluidCapabilities")) {
            li.cil.oc.common.gametest.TestFluidCapabilities.register(event);
        }
        event.registerItem(
            Capabilities.EnergyStorage.ITEM,
            ChargeableItemEnergyStorage::create,
            ModItems.BATTERY_UPGRADE_TIER1.get(),
            ModItems.BATTERY_UPGRADE_TIER2.get(),
            ModItems.BATTERY_UPGRADE_TIER3.get(),
            ModItems.HOVER_BOOTS.get(),
            ModItems.TABLET.get());
        event.registerBlockEntity(
            Capabilities.ItemHandler.BLOCK,
            ModBlockEntities.PRINTER.get(),
            (printer, side) -> new net.neoforged.neoforge.items.wrapper.SidedInvWrapper(printer,
                side == null ? net.minecraft.core.Direction.DOWN : side));
        event.registerBlockEntity(
            Capabilities.EnergyStorage.BLOCK,
            ModBlockEntities.POWER_CONVERTER.get(),
            PowerConverterBlockEntity::energyStorage);
        event.registerBlockEntity(
            Capabilities.EnergyStorage.BLOCK,
            ModBlockEntities.CHARGER.get(),
            ChargerBlockEntity::energyStorage);
        event.registerBlockEntity(
            Capabilities.EnergyStorage.BLOCK,
            ModBlockEntities.COMPUTER_CASE.get(),
            ComputerCaseBlockEntity::energyStorage);
        event.registerBlockEntity(
            Capabilities.EnergyStorage.BLOCK,
            ModBlockEntities.MICROCONTROLLER.get(),
            li.cil.oc.common.blockentity.MicrocontrollerBlockEntity::energyStorage);
        event.registerBlockEntity(
            Capabilities.EnergyStorage.BLOCK,
            ModBlockEntities.ASSEMBLER.get(),
            AssemblerBlockEntity::energyStorage);
        event.registerBlockEntity(
            Capabilities.EnergyStorage.BLOCK,
            ModBlockEntities.DISASSEMBLER.get(),
            DisassemblerBlockEntity::energyStorage);
        event.registerBlockEntity(
            Capabilities.EnergyStorage.BLOCK,
            ModBlockEntities.RELAY.get(),
            RelayBlockEntity::energyStorage);
        event.registerBlockEntity(
            Capabilities.EnergyStorage.BLOCK,
            ModBlockEntities.ROBOT.get(),
            RobotBlockEntity::energyStorage);
    }
}
