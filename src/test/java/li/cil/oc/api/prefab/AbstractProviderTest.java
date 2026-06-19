package li.cil.oc.api.prefab;

import li.cil.oc.api.nanomachines.Behavior;
import li.cil.oc.api.nanomachines.DisableReason;
import li.cil.oc.api.nanomachines.BehaviorProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class AbstractProviderTest {
    @Test
    void writesProviderIdAndOnlyReadsOwnedTags() {
        Behavior behavior = new TestBehavior();
        TestProvider provider = new TestProvider("provider-id", behavior);
        Player player = null;

        CompoundTag tag = provider.writeToNBT(behavior);
        CompoundTag otherTag = new CompoundTag();
        otherTag.putString("provider", "other");

        assertInstanceOf(BehaviorProvider.class, provider);
        assertEquals("provider-id", tag.getString("provider"));
        assertEquals("written", tag.getString("state"));
        assertSame(behavior, provider.readFromNBT(player, tag));
        assertNull(provider.readFromNBT(player, otherTag));
    }

    @Test
    void rejectsNullProviderId() {
        assertThrows(NullPointerException.class, () -> new TestProvider(null, new TestBehavior()));
    }

    private static final class TestProvider extends AbstractProvider {
        private final Behavior behavior;

        private TestProvider(final String id, final Behavior behavior) {
            super(id);
            this.behavior = behavior;
        }

        @Override
        public Iterable<Behavior> createBehaviors(final Player player) {
            return List.of(behavior);
        }

        @Override
        protected void writeBehaviorToNBT(final Behavior behavior, final CompoundTag nbt) {
            nbt.putString("state", "written");
        }

        @Override
        protected Behavior readBehaviorFromNBT(final Player player, final CompoundTag nbt) {
            return behavior;
        }
    }

    private static final class TestBehavior implements Behavior {
        @Override
        public String getNameHint() {
            return "test";
        }

        @Override
        public void onEnable() {
        }

        @Override
        public void onDisable(final DisableReason reason) {
        }

        @Override
        public void update() {
        }
    }
}
