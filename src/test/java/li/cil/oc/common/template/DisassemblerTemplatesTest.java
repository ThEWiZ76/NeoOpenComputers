package li.cil.oc.common.template;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class DisassemblerTemplatesTest {
    @Test
    void defaultTemplatesIncludeTabletDisassembler() {
        assertTrue(DisassemblerTemplates.defaultTemplateNames().contains("tablet"));
    }
}
