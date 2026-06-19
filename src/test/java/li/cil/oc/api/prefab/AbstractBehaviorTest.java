package li.cil.oc.api.prefab;

import li.cil.oc.api.nanomachines.Behavior;
import li.cil.oc.api.nanomachines.DisableReason;
import net.minecraft.world.entity.player.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

final class AbstractBehaviorTest {
    @Test
    void storesPlayerAndProvidesNoopBehaviorDefaults() {
        Player player = null;
        TestBehavior behavior = new TestBehavior(player);

        assertInstanceOf(Behavior.class, behavior);
        assertSame(player, behavior.player);
        assertNull(behavior.getNameHint());
        assertDoesNotThrow(behavior::onEnable);
        assertDoesNotThrow(() -> behavior.onDisable(DisableReason.Default));
        assertDoesNotThrow(behavior::update);
    }

    private static final class TestBehavior extends AbstractBehavior {
        private TestBehavior(final Player player) {
            super(player);
        }
    }
}
