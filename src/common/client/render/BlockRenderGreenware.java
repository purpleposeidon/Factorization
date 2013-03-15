package factorization.client.render;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.item.Item;
import factorization.api.VectorUV;
import factorization.common.BlockIcons;
import factorization.common.BlockRenderHelper;
import factorization.common.Core;
import factorization.common.FactoryType;
import factorization.common.TileEntityGreenware;
import factorization.common.TileEntityGreenware.ClayLump;
import factorization.common.TileEntityGreenware.ClayState;

public class BlockRenderGreenware extends FactorizationBlockRender {
    static BlockRenderGreenware instance;
    
    public BlockRenderGreenware() {
        instance = this;
        setup();
    }
    
    private boolean texture_init = false;
    public void setup() {
        if (texture_init) {
            return;
        }
    }
    
    @Override
    void render(RenderBlocks rb) {
        if (!world_mode) {
            Tessellator.instance.startDrawingQuads();
            setupRenderGenericLump(); //TODO: Actually showing the model would be nice.
            //But forge makes that stupidly difficult.
            setupRenderStand();
            Tessellator.instance.draw();
            return;
        }
        TileEntityGreenware gw = getCoord().getTE(TileEntityGreenware.class);
        if (gw == null) {
            return;
        }
        if (world_mode) {
            Tessellator.instance.setBrightness(Core.registry.factory_block.getMixedBrightnessForBlock(w, x, y, z));
        }
        ClayState state = gw.getState();
        if (state == ClayState.DRY || state == ClayState.WET) {
            BlockRenderHelper block = setupRenderStand();
        }
        if (!gw.canEdit()) {
            renderStatic(gw);
        }
        gw.renderedAsBlock = true;
    }
    
    void renderToTessellator(TileEntityGreenware greenware) {
        BlockRenderHelper block = BlockRenderHelper.instance;
        ClayState state = greenware.getState();
        if (state != ClayState.GLAZED) {
            switch (state) {
            case WET: block.useTexture(Block.blockClay.getBlockTextureFromSide(0)); break;
            case DRY: block.useTexture(BlockIcons.ceramics$dry); break;
            case BISQUED: block.useTexture(BlockIcons.ceramics$bisque); break;
            default: block.useTexture(BlockIcons.error); break;
            }
        }
        for (ClayLump rc : greenware.parts) {
            if (state == ClayState.GLAZED) {
                Item it = Item.itemsList[rc.icon_id];
                if (it == null) {
                    continue; //boo
                }
                block.useTexture(it.getIconFromDamage(rc.icon_md));
            }
            block.begin();
            block.rotate(rc.quat);
            block.renderRotated(Tessellator.instance, x, y, z);
        }
    }
    
    void renderDynamic(TileEntityGreenware greenware) {
        Tessellator.instance.startDrawingQuads();
        renderToTessellator(greenware);
        Tessellator.instance.draw();
    }
    
    void renderStatic(TileEntityGreenware greenware) {
        renderToTessellator(greenware);
    }
    
    /*
    static RenderingCube woodStand = new RenderingCube(16*12 + 2, new VectorUV(4, 1, 4));
    static RenderingCube genericLump = new RenderingCube(16*12, new VectorUV(3, 5, 3));
    static {
        woodStand.trans.translate(0, -6, 0);
    }
    */
    
    BlockRenderHelper setupRenderStand() {
        BlockRenderHelper block = BlockRenderHelper.instance;
        block.useTexture(BlockIcons.ceramics$stand);
        block.setBlockBoundsOffset(4F/8F, 1F/8F, 4F/8F);
        return block;
    }
    
    BlockRenderHelper setupRenderGenericLump() {
        BlockRenderHelper block = BlockRenderHelper.instance;
        block.useTexture(Block.blockClay.getBlockTextureFromSide(0));
        block.setBlockBoundsOffset(3F/8F, 5F/8F, 3F/8F);
        return block;
    }

    @Override
    FactoryType getFactoryType() {
        return FactoryType.CERAMIC;
    }

}
