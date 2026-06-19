package li.cil.oc.api.machine;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public interface Architecture {
    boolean isInitialized();

    boolean recomputeMemory(Iterable<ItemStack> components);

    boolean initialize();

    void close();

    void runSynchronized();

    ExecutionResult runThreaded(boolean isSynchronizedReturn);

    void onSignal();

    void onConnect();

    void load(CompoundTag nbt);

    void save(CompoundTag nbt);

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface Name {
        String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @Inherited
    @interface NoMemoryRequirements {
    }
}
