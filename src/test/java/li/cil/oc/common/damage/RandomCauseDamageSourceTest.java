package li.cil.oc.common.damage;

import net.minecraft.util.RandomSource;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class RandomCauseDamageSourceTest {
    @Test
    void buildsNumberedDeathMessageKeysFromSeededRandom() {
        final RandomSource random = RandomSource.create(0x0C0C0C0CL);

        final Set<String> keys = IntStream.range(0, 100)
            .mapToObj(index -> RandomCauseDamageSource.translationKey("oc.nanomachinesHungry", random, 3))
            .collect(Collectors.toSet());

        assertEquals(Set.of(
            "death.attack.oc.nanomachinesHungry.1",
            "death.attack.oc.nanomachinesHungry.2",
            "death.attack.oc.nanomachinesHungry.3"), keys);
    }

    @Test
    void rejectsNonPositiveCauseCount() {
        final IllegalArgumentException exception = org.junit.jupiter.api.Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> RandomCauseDamageSource.translationKey("oc.nanomachinesHungry", RandomSource.create(0L), 0));

        assertTrue(exception.getMessage().contains("causeCount"));
    }
}
