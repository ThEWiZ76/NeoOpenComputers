package li.cil.oc.client;

import li.cil.oc.NeoOpenComputers;
import li.cil.oc.common.entity.DroneEntity;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class DroneEntityRenderer extends EntityRenderer<DroneEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(NeoOpenComputers.MODID, "textures/item/tablet.png");

    public DroneEntityRenderer(final EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(final DroneEntity entity) {
        return TEXTURE;
    }
}
