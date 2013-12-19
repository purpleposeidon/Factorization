package factorization.common.servo;

import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.util.Icon;
import factorization.common.BlockIcons;
import factorization.common.BlockRenderHelper;
import factorization.shared.FactorizationBlockRender;
import factorization.shared.FactoryType;

public class BlockRenderServoRail extends FactorizationBlockRender {

    Icon[] central = new Icon[6];
    boolean[] sides = new boolean[6];
    BlockRenderHelper block;
    
    void removeTextures(int i, int j) {
        if (!world_mode) {
            return;
        }
        if (sides[i]) {
            block.setTexture(i, null);
        }
        if (sides[j]) {
            block.setTexture(j, null);
        }
    }
    
    void restoreTextures(int a, int b) {
        Icon icon = BlockIcons.servo$rail;
        block.setTexture(a, icon);
        block.setTexture(b, icon);
    }
    
    @Override
    public void render(RenderBlocks rb) {
        TileEntityServoRail rail = null;
        if (world_mode) {
            if (te instanceof TileEntityServoRail) {
                rail = (TileEntityServoRail) te;
                Decorator dec = rail.decoration;
                if (dec != null) {
                    dec.renderStatic(rail.getCoord(), rb);
                }
            } else {
                return;
            }
        }
        
        
        Icon icon = BlockIcons.servo$rail;
        block = BlockRenderHelper.instance;
        block.useTexture(icon);
        float f = TileEntityServoRail.width;
        boolean any = false;
        if (rail != null) {
            rail.fillSideInfo(sides);
        } else {
            any = true;
            for (int i = 0; i < 6; i++) {
                sides[i] = true;
            }
        }
        for (int i = 0; i < central.length; i++) {
            central[i] = icon;
        }
        int count = 0;
        if (sides[0] || sides[1]) {
            //DOWN, UP
            central[2] = central[3] = central[4] = central[5] = null;
            count++;
            float low = sides[0] ? 0 : f;
            float high = sides[1] ? 1 : 1 - f;
            block.setBlockBounds(f, low, f, 1 - f, high, 1 - f);
            removeTextures(0, 1);
            renderBlock(rb, block);
            restoreTextures(0, 1);
        }
        if (sides[2] || sides[3]) {
            //NORTH, SOUTH
            central[0] = central[1] = central[4] = central[5] = null;
            count++;
            float low = sides[2] ? 0 : f;
            float high = sides[3] ? 1 : 1 - f;
            block.setBlockBounds(f, f, low, 1 - f, 1 - f, high);
            removeTextures(2, 3);
            renderBlock(rb, block);
            restoreTextures(2, 3);
        }
        if (sides[4] || sides[5]) {
            //WEST, EAST
            central[0] = central[1] = central[2] = central[3] = null;
            count++;
            float low = sides[4] ? 0 : f;
            float high = sides[5] ? 1 : 1 - f;
            block.setBlockBounds(low, f, f, high, 1 - f, 1 - f);
            removeTextures(4, 5);
            renderBlock(rb, block);
            restoreTextures(4, 5);
        }
        if (count == 0) {
            block.useTextures(central);
            block.setBlockBounds(f, f, f, 1 - f, 1 - f, 1 - f);
            renderBlock(rb, block);
        }
    }

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.SERVORAIL;
    }
}
