package li.cil.oc.common.component;

import li.cil.oc.api.Network;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Connector;
import li.cil.oc.api.network.EnvironmentHost;
import li.cil.oc.api.network.Visibility;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import li.cil.oc.common.ModSettings;
import net.minecraft.nbt.CompoundTag;

public final class DebugCardEnvironment extends AbstractManagedEnvironment {
    private static final String COMPONENT_NAME = "debug";
    private static final String DATA_TAG = "oc:data";
    private static final String PLAYER_TAG = "oc:player";
    private static final String ACCESS_NONCE_TAG = "oc:accessNonce";

    private final EnvironmentHost host;
    private AccessContext access;

    public DebugCardEnvironment(final EnvironmentHost host) {
        this(host, null);
    }

    public DebugCardEnvironment(final EnvironmentHost host, final AccessContext access) {
        this.host = host;
        this.access = access;
        final var builder = Network.newNode(this, Visibility.Neighbors);
        if (builder != null) {
            setNode(builder.withComponent(COMPONENT_NAME).withConnector().create());
        }
    }

    @Callback(doc = "function(value:number):number -- Changes the component network's energy buffer by the specified delta.")
    public Object[] changeBuffer(final Context context, final Arguments args) throws Exception {
        checkAccess();
        if (node() instanceof Connector connector) {
            return new Object[]{connector.changeBuffer(args.checkDouble(0))};
        }
        return new Object[]{0D};
    }

    @Callback(doc = "function():number -- Get the container's X position in the world.")
    public Object[] getX(final Context context, final Arguments args) throws Exception {
        checkAccess();
        return new Object[]{host == null ? 0D : host.xPosition()};
    }

    @Callback(doc = "function():number -- Get the container's Y position in the world.")
    public Object[] getY(final Context context, final Arguments args) throws Exception {
        checkAccess();
        return new Object[]{host == null ? 0D : host.yPosition()};
    }

    @Callback(doc = "function():number -- Get the container's Z position in the world.")
    public Object[] getZ(final Context context, final Arguments args) throws Exception {
        checkAccess();
        return new Object[]{host == null ? 0D : host.zPosition()};
    }

    @Override
    public void load(final CompoundTag nbt) {
        super.load(nbt);
        access = loadAccess(nbt);
    }

    @Override
    public void save(final CompoundTag nbt) {
        super.save(nbt);
        saveAccess(nbt, access);
    }

    public static AccessContext loadAccess(final CompoundTag root) {
        if (root == null || !root.contains(DATA_TAG)) {
            return null;
        }
        final CompoundTag data = root.getCompound(DATA_TAG);
        if (!data.contains(PLAYER_TAG)) {
            return null;
        }
        return new AccessContext(data.getString(PLAYER_TAG), data.getString(ACCESS_NONCE_TAG));
    }

    public static void saveAccess(final CompoundTag root, final AccessContext access) {
        if (root == null) {
            return;
        }
        final CompoundTag data = root.contains(DATA_TAG) ? root.getCompound(DATA_TAG) : new CompoundTag();
        data.remove(PLAYER_TAG);
        data.remove(ACCESS_NONCE_TAG);
        if (access != null) {
            data.putString(PLAYER_TAG, access.player());
            data.putString(ACCESS_NONCE_TAG, access.nonce());
        }
        root.put(DATA_TAG, data);
    }

    private void checkAccess() throws Exception {
        switch (ModSettings.debugCardAccess()) {
            case "allow" -> {
            }
            case "whitelist" -> checkWhitelistAccess();
            default -> throw new Exception("debug card is disabled");
        }
    }

    private void checkWhitelistAccess() throws Exception {
        if (access == null) {
            throw new Exception("debug card is whitelisted, Shift+Click with it to bind card to yourself");
        }
        final var nonce = ModSettings.debugCardWhitelistNonce(access.player());
        if (nonce.isEmpty()) {
            throw new Exception("you are not whitelisted to use debug card");
        }
        if (!nonce.get().equals(access.nonce())) {
            throw new Exception("debug card is invalidated, please re-bind it to yourself");
        }
    }

    public record AccessContext(String player, String nonce) {
    }
}
