package li.cil.oc.common.template;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class AssemblerTemplatesTest {
    @Test
    void defaultTemplatesIncludeTabletAssembler() {
        assertTrue(AssemblerTemplates.defaultTemplateNames().contains("tablet"));
    }
}
