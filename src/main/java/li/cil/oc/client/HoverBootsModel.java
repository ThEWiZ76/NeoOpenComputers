package li.cil.oc.client;

import li.cil.oc.NeoOpenComputers;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

public final class HoverBootsModel<T extends LivingEntity> extends HumanoidModel<T> {
    public static final ModelLayerLocation BODY_LAYER = new ModelLayerLocation(
        ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "hover_boots"), "body");
    public static final ModelLayerLocation LIGHT_LAYER = new ModelLayerLocation(BODY_LAYER.getModel(), "lights");

    public HoverBootsModel(final ModelPart root) {
        super(root);
    }

    public static LayerDefinition createLayer(final boolean lights) {
        final var mesh = new MeshDefinition();
        final var root = mesh.getRoot();
        for (final String part : new String[]{"head", "hat", "body", "left_arm", "right_arm"}) {
            root.addOrReplaceChild(part, CubeListBuilder.create(), PartPose.ZERO);
        }
        final var left = root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.offset(1.9F, 12, 0));
        final var right = root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.offset(-1.9F, 12, 0));
        boot(left, true, lights);
        boot(right, false, lights);
        return LayerDefinition.create(mesh, 64, 32);
    }

    private static void boot(final PartDefinition leg, final boolean left, final boolean lights) {
        final var boot = leg.addOrReplaceChild("boot", CubeListBuilder.create(), PartPose.offset(0, left ? 10.11F : 10.1F, 0));
        if (!lights) {
            boot.addOrReplaceChild("platform", CubeListBuilder.create()
                .texOffs(0, 1).addBox(-3, 1, -3, 6, 1, 6)
                .texOffs(0, 23).addBox(-1, 0, -1, 2, 1, 2)
                .texOffs(0, 17).addBox(-2, -1, -2, 4, 1, 4),
                PartPose.rotation(0, (float) (Math.PI / 4), 0));
        }
        for (int wing = 0; wing < 2; wing++) {
            final int z = wing == 0 ? -7 : 1;
            final var cubes = CubeListBuilder.create().texOffs(lights ? 24 : 0, lights ? 0 : 9)
                .addBox(left ? -1 : -5, 0, z, 6, 1, 6);
            if (!lights) cubes.texOffs(0, 27).addBox(left ? 0 : -1, -1, wing == 0 ? -3 : 2, 1, 3, 1);
            boot.addOrReplaceChild("wing" + wing, cubes, PartPose.ZERO);
        }
    }
}
