package li.cil.oc.common.integration;

import li.cil.oc.common.DriverRegistry;
import net.neoforged.fml.ModList;

/** Published Forestry types remain behind the optional installed-mod boundary. */
public final class ForestryIntegration {
    private ForestryIntegration() { }

    public static void register(DriverRegistry registry) {
        if (ModList.get() != null && ModList.get().isLoaded("forestry")) Loaded.register(registry);
    }

    private static final class Loaded {
        static void register(DriverRegistry registry) {
            registry.add(new li.cil.oc.common.integration.forestry.ForestryBlockDriver());
            registry.add(new li.cil.oc.common.integration.forestry.ForestryConverter());
        }
    }
}
