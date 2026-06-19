package li.cil.oc.common.machine;

import li.cil.oc.api.IMC;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.neoforged.fml.InterModComms;

import java.util.stream.Stream;

public final class ProgramLocationImc {
    private ProgramLocationImc() {
    }

    public static void process(final Stream<InterModComms.IMCMessage> messages) {
        if (messages == null) {
            return;
        }
        messages
            .filter(message -> IMC.REGISTER_PROGRAM_DISK_LABEL.equals(message.method()))
            .map(message -> message.messageSupplier().get())
            .filter(CompoundTag.class::isInstance)
            .map(CompoundTag.class::cast)
            .forEach(ProgramLocationImc::registerProgramDiskLabel);
    }

    private static void registerProgramDiskLabel(final CompoundTag payload) {
        final String program = payload.getString("program");
        final String label = payload.getString("label");
        if (!payload.contains("architectures")) {
            ProgramLocations.addMapping(program, label);
            return;
        }
        final ListTag architectureTags = payload.getList("architectures", net.minecraft.nbt.Tag.TAG_STRING);
        final String[] architectures = new String[architectureTags.size()];
        for (int index = 0; index < architectureTags.size(); index++) {
            architectures[index] = architectureTags.getString(index);
        }
        ProgramLocations.addMapping(program, label, architectures);
    }
}
