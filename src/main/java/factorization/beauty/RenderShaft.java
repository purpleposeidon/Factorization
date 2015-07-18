package factorization.beauty;

import factorization.api.FzOrientation;
import factorization.api.Quaternion;
import factorization.common.BlockIcons;
import factorization.common.FactoryType;
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
        byte speed = BlockShaft.meta2speedNumber[metadata];
        IIcon icon = rb.overrideBlockTexture;
        if (icon == null) {
            int index = speed < 0 ? -speed : speed;
            if (index >= 0 && index < BlockIcons.beauty$shaft.length) {
                icon = BlockIcons.beauty$shaft[index];
            } else {
                icon = BlockIcons.error;
            }
        }
        if (!world_mode) {
            shaftModel.render(icon);
            return true;
        } else {
            BlockShaft block = (BlockShaft) w.getBlock(x, y, z);
            Tessellator.instance.addTranslation(0, 0.5F, 0);
            ForgeDirection spin = speed < 0 ? block.axis : block.axis.getOpposite();
            FzOrientation fzo = FzOrientation.fromDirection(spin);
            boolean ret = shaftModel.renderRotatedISBRH(rb, icon, block, x, y, z, Quaternion.fromOrientation(fzo));
            Tessellator.instance.addTranslation(0, -0.5F, 0);
            return ret;
        }
    }

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.NONE;
    }
}
