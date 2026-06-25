package li.cil.oc.common.item.data;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.List;

public final class PrintRenderModel {
    public static final AABB UNIT_BOUNDS = new AABB(0D, 0D, 0D, 1D, 1D, 1D);
    public static final String FALLBACK_TEXTURE = "minecraft:block/white_wool";
    public static final int FALLBACK_TINT = 0x80C71F;
    public static final int DEFAULT_TINT = 0xFFFFFF;

    private PrintRenderModel() {
    }

    public static List<RenderShape> blockShapes(final PrintData data, final boolean active, final Direction facing) {
        final List<RenderShape> result = new ArrayList<>();
        final Iterable<PrintData.Shape> shapes = active ? data.stateOn() : data.stateOff();
        for (PrintData.Shape shape : shapes) {
            if (shape.texture() == null || shape.texture().isBlank()) {
                continue;
            }
            result.add(new RenderShape(rotateTowardsFacing(shape.bounds(), facing), normalizeTexture(shape.texture()), tint(shape)));
        }
        return List.copyOf(result);
    }

    public static List<RenderShape> itemShapes(final PrintData data, final boolean activePreview) {
        final Iterable<PrintData.Shape> shapes = activePreview && data.hasActiveState() ? data.stateOn() : data.stateOff();
        final List<RenderShape> result = new ArrayList<>();
        for (PrintData.Shape shape : shapes) {
            final String texture = shape.texture() == null || shape.texture().isBlank() ? FALLBACK_TEXTURE : normalizeTexture(shape.texture());
            result.add(new RenderShape(shape.bounds(), texture, tint(shape)));
        }
        if (result.isEmpty()) {
            result.add(new RenderShape(UNIT_BOUNDS, FALLBACK_TEXTURE, FALLBACK_TINT));
        }
        return List.copyOf(result);
    }

    private static int tint(final PrintData.Shape shape) {
        return shape.tint() == null ? DEFAULT_TINT : shape.tint();
    }

    private static String normalizeTexture(final String texture) {
        if (texture.contains(":blocks/")) {
            return texture.replace(":blocks/", ":block/");
        }
        if (!texture.contains(":")) {
            final String path = texture.startsWith("blocks/")
                ? "block/" + texture.substring("blocks/".length())
                : texture.startsWith("block/") ? texture : "block/" + texture;
            return "minecraft:" + path;
        }
        return texture;
    }

    private static AABB rotateTowardsFacing(final AABB bounds, final Direction facing) {
        return switch (facing) {
            case EAST -> new AABB(bounds.minZ, bounds.minY, 1D - bounds.maxX, bounds.maxZ, bounds.maxY, 1D - bounds.minX);
            case NORTH -> new AABB(1D - bounds.maxX, bounds.minY, 1D - bounds.maxZ, 1D - bounds.minX, bounds.maxY, 1D - bounds.minZ);
            case WEST -> new AABB(1D - bounds.maxZ, bounds.minY, bounds.minX, 1D - bounds.minZ, bounds.maxY, bounds.maxX);
            default -> bounds;
        };
    }

    public record RenderShape(AABB bounds, String texture, int tint) {
    }
}
