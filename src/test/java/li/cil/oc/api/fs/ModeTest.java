package li.cil.oc.api.fs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

final class ModeTest {
    @Test
    void keepsUpstreamFileModeNamesAndOrder() {
        assertArrayEquals(new Mode[]{Mode.Read, Mode.Write, Mode.Append}, Mode.values());
    }
}
