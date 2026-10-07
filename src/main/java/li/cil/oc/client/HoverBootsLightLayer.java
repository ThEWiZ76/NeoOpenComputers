package li.cil.oc.client;

import com.mojang.blaze3d.vertex.PoseStack;
import li.cil.oc.common.ModItems;
import li.cil.oc.common.item.HoverBootsItem;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.component.DyedItemColor;

public final class HoverBootsLightLayer<T extends LivingEntity> extends RenderLayer<T, HumanoidModel<T>> {
    private final HoverBootsModel<T> model;

    public HoverBootsLightLayer(final RenderLayerParent<T, HumanoidModel<T>> parent, final EntityModelSet models) {
        super(parent);
        model = new HoverBootsModel<>(models.bakeLayer(HoverBootsModel.LIGHT_LAYER));
    }

    @Override
    public void render(final PoseStack pose, final MultiBufferSource buffers, final int packedLight, final T entity,
            final float limbSwing, final float limbSwingAmount, final float partialTicks, final float ageInTicks,
            final float netHeadYaw, final float headPitch) {
        final var stack = entity.getItemBySlot(EquipmentSlot.FEET);
        if (!stack.is(ModItems.HOVER_BOOTS.get())) return;
        getParentModel().copyPropertiesTo(model);
        model.setAllVisible(false);
        model.leftLeg.visible = true;
        model.rightLeg.visible = true;
        final int color = 0xFF000000 | (DyedItemColor.getOrDefault(stack, 0x66DD55) & 0xFFFFFF);
        model.renderToBuffer(pose, buffers.getBuffer(RenderType.eyes(HoverBootsItem.ARMOR_TEXTURE)),
            LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, color);
    }
}
