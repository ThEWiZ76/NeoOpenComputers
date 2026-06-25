package li.cil.oc.common;

import li.cil.oc.api.IMC;
import net.neoforged.fml.InterModComms;

import java.util.stream.Stream;

public final class PeripheralBlacklistImc {
    public static void process(final Stream<InterModComms.IMCMessage> messages) {
        if (messages == null) {
            return;
        }
        messages
            .filter(message -> IMC.BLACKLIST_PERIPHERAL.equals(message.method()))
            .map(message -> message.messageSupplier().get())
            .filter(String.class::isInstance)
            .map(String.class::cast)
            .forEach(PeripheralBlacklist::add);
    }

    private PeripheralBlacklistImc() {
    }
}
