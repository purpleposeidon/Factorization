package factorization.beauty;

import factorization.common.BlockIcons;
import factorization.common.FactoryType;
import factorization.shared.Core;
import factorization.shared.FactorizationBlockRender;
import factorization.shared.ObjectModel;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.ResourceLocation;

public class BlockRenderSteamShaft extends FactorizationBlockRender {
    static ObjectModel shaft = new ObjectModel(new ResourceLocation("factorization", "models/whirligig_shaft.obj"));
    static ObjectModel whirligig = new ObjectModel(new ResourceLocation("factorization", "models/whirligig.obj"));

    @Override
    public boolean render(RenderBlocks rb) {
        if (world_mode) {
            Tessellator.instance.addTranslation(0, 0.5F, 0);
            shaft.renderISBRH(rb, BlockIcons.dark_iron_block, Core.registry.factory_block, x, y, z);
            Tessellator.instance.addTranslation(0, -0.5F, 0);
        } else {
            shaft.render(BlockIcons.dark_iron_block);
            whirligig.render(BlockIcons.beauty$whirligig);
        }
        return true;
    }

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.STEAM_SHAFT;
    }
}
