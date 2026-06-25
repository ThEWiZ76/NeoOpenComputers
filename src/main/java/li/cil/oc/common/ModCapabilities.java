package li.cil.oc.common;

import li.cil.oc.common.blockentity.PowerConverterBlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public final class ModCapabilities {
    private ModCapabilities() {
    }

    public static void register(final RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
            Capabilities.EnergyStorage.BLOCK,
            ModBlockEntities.POWER_CONVERTER.get(),
            PowerConverterBlockEntity::energyStorage);
    }
}
