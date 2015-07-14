package factorization.beauty;

import factorization.api.FzOrientation;
import factorization.api.Quaternion;
import factorization.common.BlockIcons;
import factorization.common.FactoryType;
import factorization.shared.Core;
import factorization.shared.FactorizationBlockRender;
import factorization.shared.ObjectModel;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.ForgeDirection;

public class RenderShaft extends FactorizationBlockRender {
    ObjectModel shaftModel = new ObjectModel(new ResourceLocation("factorization", "models/shaft.obj"));

    @Override
    public boolean render(RenderBlocks rb) {
        if (metadata > 0xF || metadata < 0) return false;
        ForgeDirection dir = BlockShaft.meta2direction[metadata];
        byte speed = BlockShaft.meta2speed[metadata];
        IIcon icon = rb.overrideBlockTexture;
        if (icon == null) {
            if (speed >= 0 && speed < BlockIcons.beauty$shaft.length) {
                icon = BlockIcons.beauty$shaft[speed];
            } else {
                icon = BlockIcons.error;
            }
        }
        if (world_mode) {
            Tessellator.instance.addTranslation(0, 0.5F, 0);
        }
        if (!world_mode) {
            shaftModel.render(icon);
        } else if (dir == ForgeDirection.SOUTH) {
            shaftModel.renderRotatedISBRH(rb, icon, Core.registry.wooden_shaft, x, y, z, Quaternion.fromOrientation(FzOrientation.FACE_SOUTH_POINT_DOWN));
        } else if (dir == ForgeDirection.EAST) {
            shaftModel.renderRotatedISBRH(rb, icon, Core.registry.wooden_shaft, x, y, z, Quaternion.fromOrientation(FzOrientation.FACE_EAST_POINT_DOWN));
        } else {
            shaftModel.renderISBRH(rb, icon, Core.registry.wooden_shaft, x, y, z);
        }
        if (world_mode) {
            Tessellator.instance.addTranslation(0, -0.5F, 0);
        }
        return true;
    }

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.NONE;
    }
}
