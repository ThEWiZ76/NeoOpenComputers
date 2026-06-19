package li.cil.oc.api.network;

import net.minecraft.nbt.CompoundTag;

/**
 * Payload sent via wired or wireless network cards.
 */
public interface Packet {
    String source();

    String destination();

    int port();

    Object[] data();

    int size();

    int ttl();

    Packet hop();

    void save(CompoundTag nbt);
}
