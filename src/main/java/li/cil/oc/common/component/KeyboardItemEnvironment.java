package li.cil.oc.common.component;

import li.cil.oc.api.driver.DeviceInfo;
import li.cil.oc.api.internal.Keyboard;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.prefab.AbstractManagedEnvironment;
import net.minecraft.world.entity.player.Player;
import net.minecraft.nbt.CompoundTag;

import java.util.Map;
import java.util.function.Consumer;

public final class KeyboardItemEnvironment extends AbstractManagedEnvironment implements Keyboard, DeviceInfo {
    private final KeyboardInputState inputState = new KeyboardInputState();
    private final Consumer<CompoundTag> saveData;
    private UsabilityChecker usabilityOverride;

    public KeyboardItemEnvironment() {
        this(null, null);
    }

    public KeyboardItemEnvironment(final CompoundTag data, final Consumer<CompoundTag> saveData) {
        this.saveData = saveData;
        setNode(KeyboardEnvironment.createNode(this));
        if (data != null && !data.isEmpty()) {
            load(data);
        }
    }

    @Override
    public void save(final CompoundTag nbt) {
        super.save(nbt);
        if (saveData != null) {
            saveData.accept(nbt.copy());
        }
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
