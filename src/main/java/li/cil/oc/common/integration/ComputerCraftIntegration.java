package li.cil.oc.common.integration;

import li.cil.oc.common.DriverRegistry;
import net.neoforged.fml.ModList;

/** Foreign runtime types are loaded only when ComputerCraft is installed. */
public final class ComputerCraftIntegration {
    private ComputerCraftIntegration() { }
    public static void register(DriverRegistry registry) {
        if (ModList.get().isLoaded("computercraft")) Loaded.register(registry);
    }
    private static final class Loaded {
        static void register(DriverRegistry registry) {
            registry.add(new li.cil.oc.common.integration.computercraft.PeripheralDriver());
            registry.add(new li.cil.oc.common.integration.computercraft.MediaDriver());
            final var objects = new li.cil.oc.common.integration.computercraft.LuaObjectConverter();
            registry.add(objects);
            objects.registerEvents();
        }
    }
}
