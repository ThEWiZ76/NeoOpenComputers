package li.cil.oc.common;

import li.cil.oc.common.blockentity.AssemblerBlockEntity;
import li.cil.oc.common.blockentity.ComputerCaseBlockEntity;
import li.cil.oc.common.blockentity.DisassemblerBlockEntity;
import li.cil.oc.common.blockentity.PowerConverterBlockEntity;
import li.cil.oc.common.blockentity.RelayBlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public final class ModCapabilities {
    private ModCapabilities() {
    }

    public static void register(final RegisterCapabilitiesEvent event) {
        event.registerItem(
            Capabilities.EnergyStorage.ITEM,
            ChargeableItemEnergyStorage::create,
            ModItems.BATTERY_UPGRADE_TIER1.get(),
            ModItems.BATTERY_UPGRADE_TIER2.get(),
            ModItems.BATTERY_UPGRADE_TIER3.get(),
            ModItems.TABLET.get());
        event.registerBlockEntity(
            Capabilities.EnergyStorage.BLOCK,
            ModBlockEntities.POWER_CONVERTER.get(),
            PowerConverterBlockEntity::energyStorage);
        event.registerBlockEntity(
            Capabilities.EnergyStorage.BLOCK,
            ModBlockEntities.COMPUTER_CASE.get(),
            ComputerCaseBlockEntity::energyStorage);
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
    }
}
