package li.cil.oc.common;

import li.cil.oc.api.network.Connector;
import net.neoforged.neoforge.energy.IEnergyStorage;

import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

public final class ForgeEnergyStorageView implements IEnergyStorage {
    private final Supplier<Connector> connectorSupplier;
    private final DoubleSupplier throughputSupplier;

    public ForgeEnergyStorageView(final Supplier<Connector> connectorSupplier, final DoubleSupplier throughputSupplier) {
        this.connectorSupplier = connectorSupplier;
        this.throughputSupplier = throughputSupplier;
    }

    @Override
    public int receiveEnergy(final int toReceive, final boolean simulate) {
        final Connector connector = connector();
        if (connector == null || toReceive <= 0 || ModSettings.ignorePower()) {
            return 0;
        }

        final double throughput = throughput();
        final double requestedEnergy = ModSettings.fromForgeEnergy(toReceive);
        final double cappedEnergy = Math.max(0D, Math.min(Math.min(throughput, requestedEnergy), globalDemand(connector, throughput)));
        if (simulate) {
            return ModSettings.toForgeEnergy(cappedEnergy);
        }
        return ModSettings.toForgeEnergy(cappedEnergy - connector.changeBuffer(cappedEnergy));
    }

    @Override
    public int extractEnergy(final int toExtract, final boolean simulate) {
        return 0;
    }

    @Override
    public int getEnergyStored() {
        final Connector connector = connector();
        return connector == null ? 0 : ModSettings.toForgeEnergy(connector.globalBuffer());
    }

    @Override
    public int getMaxEnergyStored() {
        final Connector connector = connector();
        return connector == null ? 0 : ModSettings.toForgeEnergy(connector.globalBufferSize());
    }

    @Override
    public boolean canExtract() {
        return false;
    }

    @Override
    public boolean canReceive() {
        return !ModSettings.ignorePower() && connector() != null;
    }

    private Connector connector() {
        return connectorSupplier.get();
    }

    private double throughput() {
        return Math.max(0D, throughputSupplier.getAsDouble());
    }

    private static double globalDemand(final Connector connector, final double throughput) {
        return Math.max(0D, Math.min(throughput, connector.globalBufferSize() - connector.globalBuffer()));
    }
}
