package li.cil.oc.common.component;

import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.internal.Keyboard;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import net.minecraft.world.entity.player.Player;

import java.util.Map;

public final class KeyboardItemEnvironment extends AbstractManagedEnvironment implements Keyboard, DeviceInfo {
    private final KeyboardInputState inputState = new KeyboardInputState();
    private UsabilityChecker usabilityOverride;

    public KeyboardItemEnvironment() {
        setNode(KeyboardEnvironment.createNode(this));
    }

    @Override
    public void setUsableOverride(final UsabilityChecker callback) {
        usabilityOverride = callback;
    }

    @Override
    public void onMessage(final Message message) {
        inputState.onMessage(node(), message, this::isUsableByPlayer);
    }

    @Override
    public Map<String, String> getDeviceInfo() {
        return KeyboardEnvironment.deviceInfo();
    }

    private boolean isUsableByPlayer(final Player player) {
        return usabilityOverride == null || usabilityOverride.isUsableByPlayer(this, player);
    }
}
