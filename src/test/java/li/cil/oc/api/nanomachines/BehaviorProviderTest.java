package li.cil.oc.api.nanomachines;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

final class BehaviorProviderTest {
    @Test
    void exposesModernPlayerAndCompoundTagProviderMethods() throws NoSuchMethodException {
        Method createBehaviors = BehaviorProvider.class.getMethod("createBehaviors", Player.class);
        Method writeToNbt = BehaviorProvider.class.getMethod("writeToNBT", Behavior.class);
        Method readFromNbt = BehaviorProvider.class.getMethod("readFromNBT", Player.class, CompoundTag.class);

        assertArrayEquals(new Class<?>[]{Player.class}, createBehaviors.getParameterTypes());
        assertSame(CompoundTag.class, writeToNbt.getReturnType());
        assertArrayEquals(new Class<?>[]{Player.class, CompoundTag.class}, readFromNbt.getParameterTypes());
    }

    @Test
    void providerCanCreateSerializeAndRestoreBehavior() {
        Behavior behavior = new TestBehavior();
        BehaviorProvider provider = new TestProvider(behavior);
        Player player = null;

        assertSame(behavior, provider.createBehaviors(player).iterator().next());
        assertSame(behavior, provider.readFromNBT(player, provider.writeToNBT(behavior)));
    }

    private static final class TestProvider implements BehaviorProvider {
        private final Behavior behavior;

        private TestProvider(final Behavior behavior) {
            this.behavior = behavior;
        }

        @Override
        public Iterable<Behavior> createBehaviors(final Player player) {
            return List.of(behavior);
        }

        @Override
        public CompoundTag writeToNBT(final Behavior behavior) {
            CompoundTag tag = new CompoundTag();
            tag.putString("kind", "test");
            return tag;
        }

        @Override
        public Behavior readFromNBT(final Player player, final CompoundTag nbt) {
            return "test".equals(nbt.getString("kind")) ? behavior : null;
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
