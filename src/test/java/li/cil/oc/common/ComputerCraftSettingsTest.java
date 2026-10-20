package li.cil.oc.common;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

final class ComputerCraftSettingsTest {
    @Test
    void peripheralBlacklistKeepsCommandBlocksBlockedByDefault() {
        final Object raw = ModSettings.SPEC.getValues().get("integration.peripheralBlacklist");
        final var value = assertInstanceOf(ModConfigSpec.ConfigValue.class, raw, "Missing configurable peripheral blacklist");
        assertEquals(List.of("integration", "peripheralBlacklist"), value.getPath());
        assertEquals(List.of("net.minecraft.world.level.block.entity.CommandBlockEntity"), value.getDefault());
    }
}
