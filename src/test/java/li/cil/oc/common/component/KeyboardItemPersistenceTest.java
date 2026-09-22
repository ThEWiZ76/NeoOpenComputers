package li.cil.oc.common.component;

import li.cil.oc.api.Network;
import li.cil.oc.common.OpenComputersApi;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

final class KeyboardItemPersistenceTest {
    @Test
    void callbackStoresIndependentNodeDataForRecreation() {
        OpenComputersApi.initialize();
        final AtomicReference<CompoundTag> saved = new AtomicReference<>();
        final KeyboardItemEnvironment original = new KeyboardItemEnvironment(null, saved::set);
        Network.joinNewNetwork(original.node());
        final String address = original.node().address();
        final CompoundTag temporary = new CompoundTag();
        original.save(temporary);
        assertNotNull(saved.get());
        temporary.remove("node");
        original.node().remove();
        final KeyboardItemEnvironment restored = new KeyboardItemEnvironment(saved.get(), null);
        Network.joinNewNetwork(restored.node());
        assertEquals(address, restored.node().address());
        restored.node().remove();
    }
}
