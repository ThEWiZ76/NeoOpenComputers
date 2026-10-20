package li.cil.oc.common.integration;

import li.cil.oc.api.network.Packet;
import li.cil.oc.common.ModBlockEntities;
import li.cil.oc.common.blockentity.RelayBlockEntity;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/** Keeps ComputerCraft types out of the relay's optional-mod boundary. */
public final class ComputerCraftRelayIntegration {
    private ComputerCraftRelayIntegration() { }

    public interface Receiver {
        void receive(Packet packet);
        void clear();
    }

    public static Receiver create(final RelayBlockEntity relay) {
        return ModList.get() != null && ModList.get().isLoaded("computercraft") ? Loaded.create(relay) : null;
    }

    public static void register(final RegisterCapabilitiesEvent event) {
        if (ModList.get() != null && ModList.get().isLoaded("computercraft")) Loaded.register(event);
    }

    private static final class Loaded {
        static Receiver create(final RelayBlockEntity relay) {
            return new li.cil.oc.common.integration.computercraft.RelayPeripheral(relay);
        }

        static void register(final RegisterCapabilitiesEvent event) {
            event.registerBlockEntity(dan200.computercraft.api.peripheral.PeripheralCapability.get(), ModBlockEntities.RELAY.get(),
                (relay, side) -> (li.cil.oc.common.integration.computercraft.RelayPeripheral) relay.computerCraftReceiver());
        }
    }
}
