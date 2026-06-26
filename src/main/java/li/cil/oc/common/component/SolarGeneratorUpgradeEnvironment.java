package li.cil.oc.common.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.common.ModSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.Map;

public class SolarGeneratorUpgradeEnvironment extends AbstractManagedEnvironment implements DeviceInfo {
    private static final double BUFFER_SIZE = 1D;
    private static final int CHECK_INTERVAL = 100;

    private final EnvironmentHost host;
    private int ticksUntilCheck;
    private boolean sunVisible;

    public SolarGeneratorUpgradeEnvironment(final EnvironmentHost host) {
        this.host = host;
        final var builder = Network.newNode(this, Visibility.Network);
        if (builder != null) {
            setNode(builder.withConnector(BUFFER_SIZE).create());
        }
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return Map.of(
            DeviceInfo.DeviceAttribute.Class, DeviceInfo.DeviceClass.Power,
            DeviceInfo.DeviceAttribute.Description, "Solar panel",
            DeviceInfo.DeviceAttribute.Vendor, "MightyPirates GmbH & Co. KG",
            DeviceInfo.DeviceAttribute.Product, "Enligh10"
        );
    }

    @Override
    public boolean canUpdate() {
        return true;
    }

    @Override
    public void update() {
        ticksUntilCheck--;
        if (ticksUntilCheck <= 0) {
            ticksUntilCheck = CHECK_INTERVAL;
            sunVisible = isSunVisible();
        }
        if (sunVisible && node() instanceof Connector connector) {
            connector.changeBuffer(ModSettings.solarGeneratorEfficiency());
        }
    }

    private boolean isSunVisible() {
        if (host == null || host.world() == null) {
            return false;
        }
        final Level level = host.world();
        final BlockPos pos = BlockPos.containing(host.xPosition(), host.yPosition(), host.zPosition()).above();
        final boolean biomeHasPrecipitation = level.getBiome(pos).value().hasPrecipitation();
        return level.isDay()
            && !Level.NETHER.equals(level.dimension())
            && level.canSeeSky(pos)
            && weatherAllowsSun(biomeHasPrecipitation, level.isRaining(), level.isThundering());
    }

    static boolean weatherAllowsSun(final boolean biomeHasPrecipitation, final boolean raining, final boolean thundering) {
        return !biomeHasPrecipitation || (!raining && !thundering);
    }
}
