package li.cil.oc.api.network;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

final class VisibilityTest {
    @Test
    void keepsUpstreamVisibilityNamesAndOrder() {
        assertArrayEquals(
                new Visibility[]{Visibility.None, Visibility.Neighbors, Visibility.Network},
                Visibility.values());
    }
}
