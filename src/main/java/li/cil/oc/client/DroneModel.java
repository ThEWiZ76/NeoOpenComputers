package li.cil.oc.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc.NeoOpenComputers;
import li.cil.oc.common.entity.DroneEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public final class DroneModel extends EntityModel<DroneEntity> {
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "textures/model/drone.png");
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "drone"),
        "main");

    private static final float DEFAULT_FLAP_TILT = 2F * Mth.DEG_TO_RAD;
    private final ModelPart root;
    private final ModelPart[] wings;

    public DroneModel(final ModelPart root) {
        super(RenderType::entityCutout);
        this.root = root;
        this.wings = new ModelPart[]{
            root.getChild("wing0"),
            root.getChild("wing1"),
            root.getChild("wing2"),
            root.getChild("wing3")
        };
    }

    public static LayerDefinition createBodyLayer() {
        final MeshDefinition mesh = new MeshDefinition();
        final PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild(
            "body",
            CubeListBuilder.create()
                .texOffs(0, 1).addBox(-3F, 1F, -3F, 6F, 1F, 6F)
                .texOffs(0, 23).addBox(-1F, 0F, -1F, 2F, 1F, 2F)
                .texOffs(0, 17).addBox(-2F, -1F, -2F, 4F, 1F, 4F),
            PartPose.rotation(0F, 45F * Mth.DEG_TO_RAD, 0F));
        root.addOrReplaceChild(
            "wing0",
            CubeListBuilder.create()
                .texOffs(0, 9).addBox(1F, 0F, -7F, 6F, 1F, 6F)
                .texOffs(0, 27).addBox(2F, -1F, -3F, 1F, 3F, 1F),
            PartPose.ZERO);
        root.addOrReplaceChild(
            "wing1",
            CubeListBuilder.create()
                .texOffs(0, 9).addBox(1F, 0F, 1F, 6F, 1F, 6F)
                .texOffs(0, 27).addBox(2F, -1F, 2F, 1F, 3F, 1F),
            PartPose.ZERO);
        root.addOrReplaceChild(
            "wing2",
            CubeListBuilder.create()
                .texOffs(0, 9).addBox(-7F, 0F, 1F, 6F, 1F, 6F)
                .texOffs(0, 27).addBox(-3F, -1F, 2F, 1F, 3F, 1F),
            PartPose.ZERO);
        root.addOrReplaceChild(
            "wing3",
            CubeListBuilder.create()
                .texOffs(0, 9).addBox(-7F, 0F, -7F, 6F, 1F, 6F)
                .texOffs(0, 27).addBox(-3F, -1F, -3F, 1F, 3F, 1F),
            PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void setupAnim(final DroneEntity entity, final float limbSwing, final float limbSwingAmount, final float ageInTicks, final float netHeadYaw, final float headPitch) {
        root.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        final Vec3 velocity = entity.getDeltaMovement();
        final double speed = velocity.length();
        final float tilt = entity.machine().isRunning()
            ? (float) Mth.clamp(speed * 0.8D, DEFAULT_FLAP_TILT, 20F * Mth.DEG_TO_RAD)
            : DEFAULT_FLAP_TILT;
        wings[0].xRot = tilt;
        wings[0].zRot = tilt;
        wings[1].xRot = -tilt;
        wings[1].zRot = tilt;
        wings[2].xRot = -tilt;
        wings[2].zRot = -tilt;
        wings[3].xRot = tilt;
        wings[3].zRot = -tilt;
    }

    @Override
    public void renderToBuffer(final PoseStack poseStack, final VertexConsumer buffer, final int packedLight, final int packedOverlay, final int color) {
        root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
