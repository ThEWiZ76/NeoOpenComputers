package li.cil.oc.api.event;

import li.cil.oc.api.network.EnvironmentHost;
import net.minecraft.core.BlockPos;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

import java.util.HashMap;
import java.util.Map;

public abstract class GeolyzerEvent extends Event implements ICancellableEvent {
    public final EnvironmentHost host;
    public final Map<?, ?> options;

    protected GeolyzerEvent(final EnvironmentHost host, final Map<?, ?> options) {
        this.host = host;
        this.options = options;
    }

    public static class Scan extends GeolyzerEvent {
        public final int minX;
        public final int minY;
        public final int minZ;
        public final int maxX;
        public final int maxY;
        public final int maxZ;
        public final float[] data = new float[64];

        public Scan(final EnvironmentHost host, final Map<?, ?> options, final int minX, final int minY, final int minZ, final int maxX, final int maxY, final int maxZ) {
            super(host, options);
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
        }
    }

    public static class Analyze extends GeolyzerEvent {
        public final BlockPos pos;
        public final Map<String, Object> data = new HashMap<>();

        public Analyze(final EnvironmentHost host, final Map<?, ?> options, final BlockPos pos) {
            super(host, options);
            this.pos = pos;
        }
    }
}
