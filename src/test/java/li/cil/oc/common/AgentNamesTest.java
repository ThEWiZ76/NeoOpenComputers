package li.cil.oc.common;

import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class AgentNamesTest {
    @Test
    void parsesUpstreamCommentsWhitespaceAndUnicode() throws Exception {
        assertEquals(List.of("Atlas", "Röbot", "BB-8"), AgentNames.parse(new StringReader(
            "# ignored\n Atlas # Portal\n\n Röbot \n BB-8 # Star Wars\n")));
    }

    @Test
    void seededSelectionAndEmptyListFallback() {
        final var names = List.of("Atlas", "Bender", "Marvin");
        final var actual = new Random(731);
        for (final String expected : List.of("Bender", "Bender", "Atlas", "Marvin", "Marvin", "Bender", "Atlas", "Atlas")) {
            assertEquals(expected, AgentNames.choose(names, actual));
        }
        assertEquals("Robot", AgentNames.choose(List.of(), new Random(731)));
        assertEquals("  ", AgentNames.resolve("  "));
    }

    @Test
    void loadsOriginalBundledNamesForSeededSelection() {
        assertEquals("LordFokas", AgentNames.randomName(new Random(731)));
    }
}
