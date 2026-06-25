package li.cil.oc.common;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.api.IMC;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.InterModComms;

import java.util.stream.Stream;

public final class HostBlacklistImc {
    public static void process(final DriverRegistry registry, final Stream<InterModComms.IMCMessage> messages) {
        if (registry == null || messages == null) {
            return;
        }
        messages
            .filter(message -> IMC.BLACKLIST_HOST.equals(message.method()))
            .map(message -> message.messageSupplier().get())
            .filter(CompoundTag.class::isInstance)
            .map(CompoundTag.class::cast)
            .forEach(payload -> register(registry, payload));
    }

    private static void register(final DriverRegistry registry, final CompoundTag payload) {
        final ItemStack stack = ItemStack.OPTIONAL_CODEC.parse(NbtOps.INSTANCE, payload.get("item"))
            .result()
            .orElse(ItemStack.EMPTY);
        final String hostName = payload.getString("host");
        try {
            registry.blacklistHost(stack, Class.forName(hostName));
        } catch (ClassNotFoundException e) {
            NeoOpenComputers.LOGGER.warn("Failed blacklisting component for missing host '{}'.", hostName, e);
        }
    }

    private HostBlacklistImc() {
    }
}
