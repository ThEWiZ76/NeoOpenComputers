package li.cil.oc.api;

import net.minecraft.nbt.CompoundTag;

/**
 * Object that can save and restore its state from a Minecraft NBT tag.
 */
public interface Persistable {
    void load(CompoundTag nbt);

    void save(CompoundTag nbt);
}
