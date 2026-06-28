package li.cil.oc.client;

import li.cil.oc.common.item.MfuItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MfuTargetRendererShapeTest {
    @Test
    void clientRegistersMfuTargetRenderer() throws Exception {
        final String source = Files.readString(Path.of("src/main/java/li/cil/oc/client/NeoOpenComputersClient.java"));

        assertTrue(source.contains("NeoForge.EVENT_BUS.register(MfuTargetRenderer.class);"));
    }

    @Test
    void usesUpstreamTargetTagColorAndRange() {
        assertEquals(MfuItem.COORD_TAG, MfuTargetRenderer.COORD_TAG);
        assertEquals(0x00FF00, MfuTargetRenderer.COLOR);
        assertEquals(64D, MfuTargetRenderer.MAX_DISTANCE);

        assertTrue(MfuTargetRenderer.withinRange(64D));
        assertTrue(MfuTargetRenderer.withinRange(0D));
        assertEquals(false, MfuTargetRenderer.withinRange(64.001D));
    }

    @Test
    void growsTargetBlockBoundsLikeUpstream() {
        final AABB bounds = MfuTargetRenderer.targetBounds(new BlockPos(10, 20, 30));

        assertEquals(9.9D, bounds.minX, 0.0001D);
        assertEquals(19.9D, bounds.minY, 0.0001D);
        assertEquals(29.9D, bounds.minZ, 0.0001D);
        assertEquals(11.1D, bounds.maxX, 0.0001D);
        assertEquals(21.1D, bounds.maxY, 0.0001D);
        assertEquals(31.1D, bounds.maxZ, 0.0001D);
    }

    @Test
    void createsFourFaceVerticesForEachDirection() {
        final AABB bounds = MfuTargetRenderer.targetBounds(BlockPos.ZERO);

        for (final Direction direction : Direction.values()) {
            final List<MfuTargetRenderer.Vertex> vertices = MfuTargetRenderer.faceVertices(bounds, direction);

            assertEquals(4, vertices.size());
        }
    }
}
