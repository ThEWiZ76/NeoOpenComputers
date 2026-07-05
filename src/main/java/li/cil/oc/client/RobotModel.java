package li.cil.oc.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc.NeoOpenComputers;
import net.minecraft.client.model.Model;
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

public final class RobotModel extends Model {
    public static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "textures/model/robot.png");
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
        ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "robot"),
        "main");

    private final ModelPart root;

    public RobotModel(final ModelPart root) {
        super(RenderType::entityCutout);
        this.root = root;
    }

    public static LayerDefinition createBodyLayer() {
        final MeshDefinition mesh = new MeshDefinition();
        final PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild(
            "top",
            CubeListBuilder.create()
                .texOffs(0, 0).addBox(-5F, -1F, -5F, 10F, 2F, 10F),
            PartPose.offsetAndRotation(0F, -5F, 0F, 0F, 45F * Mth.DEG_TO_RAD, 0F));
        root.addOrReplaceChild(
            "core",
            CubeListBuilder.create()
                .texOffs(16, 16).addBox(-3F, -3F, -3F, 6F, 6F, 6F),
            PartPose.offsetAndRotation(0F, 0F, 0F, 0F, 45F * Mth.DEG_TO_RAD, 0F));
        root.addOrReplaceChild(
            "bottom",
            CubeListBuilder.create()
                .texOffs(0, 16).addBox(-5F, -1F, -5F, 10F, 2F, 10F),
            PartPose.offsetAndRotation(0F, 5F, 0F, 0F, 45F * Mth.DEG_TO_RAD, 0F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void renderToBuffer(final PoseStack poseStack, final VertexConsumer buffer, final int packedLight, final int packedOverlay, final int color) {
        root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
