package li.cil.oc.common.machine;

import li.cil.oc.api.IMC;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.neoforged.fml.InterModComms;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ProgramLocationImcTest {
    @AfterEach
    void clearMappings() {
        ProgramLocations.clear();
    }

    @Test
    void registersProgramDiskLabelsFromImcMessages() {
        CompoundTag payload = new CompoundTag();
        payload.putString("program", "dig");
        payload.putString("label", "Network");
        ListTag architectures = new ListTag();
        architectures.add(StringTag.valueOf("Lua"));
        payload.put("architectures", architectures);
        InterModComms.IMCMessage message = new InterModComms.IMCMessage(
            "addon",
            "neoopencomputers",
            IMC.REGISTER_PROGRAM_DISK_LABEL,
            () -> payload
        );

        ProgramLocationImc.process(Stream.of(message));

        List<ProgramLocations.Mapping> mappings = ProgramLocations.mappings("Lua");
        assertEquals(1, mappings.size());
        assertEquals("dig", mappings.getFirst().program());
        assertEquals("Network", mappings.getFirst().label());
    }

    @Test
    void registersUpstreamDefaultProgramDiskLabels() {
        ProgramLocations.registerDefaults();

        Map<String, String> mappings = ProgramLocations.mappingsByProgram("LuaJ");
        assertEquals("builder", mappings.get("build"));
        assertEquals("dig", mappings.get("dig"));
        assertEquals("data", mappings.get("md5sum"));
        assertEquals("data", mappings.get("sha256sum"));
        assertEquals("network", mappings.get("ping"));
        assertEquals("network", mappings.get("route"));
        assertEquals("openloader", mappings.get("opl-flash"));
        assertEquals("oppm", mappings.get("oppm"));
    }
}
