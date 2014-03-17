package factorization.servo;

import java.util.Arrays;

import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraftforge.common.util.ForgeDirection;
import factorization.common.BlockIcons;
import factorization.common.FactoryType;
import factorization.shared.BlockRenderHelper;
import factorization.shared.FactorizationBlockRender;

public class BlockRenderServoRail extends FactorizationBlockRender {

    IIcon[] central = new IIcon[6];
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
        IIcon icon = BlockIcons.servo$rail;
        block.setTexture(a, icon);
        block.setTexture(b, icon);
    }
    
    boolean[] extend = new boolean[6];
    @Override
    public boolean render(RenderBlocks rb) {
        TileEntityServoRail rail = null;
        boolean has_comment = false;
        if (world_mode) {
            if (te instanceof TileEntityServoRail) {
                rail = (TileEntityServoRail) te;
                Decorator dec = rail.decoration;
                if (dec != null) {
                    dec.renderStatic(rail.getCoord(), rb);
                }
                if (rail.comment != null && rail.comment.length() > 0) {
                    has_comment = true;
                }
            } else {
                return false;
            }
        }
        block = BlockRenderHelper.instance;
        if (rail != null) {
            rail.fillSideInfo(sides);
            block.setupBrightness(Tessellator.instance, w, x, y, z);
        } else {
            for (int i = 0; i < 6; i++) {
                sides[i] = true;
            }
        }
        IIcon icon = has_comment ? BlockIcons.servo$rail_comment : BlockIcons.servo$rail;
        
        final float fL = TileEntityServoRail.width;
        final float fH = 1 - fL;
        
        block.useTexture(null);
        
        for (ForgeDirection fd : ForgeDirection.VALID_DIRECTIONS) {
            int i = fd.ordinal();
            block.setTexture(i, icon);
            boolean any = false;
            for (ForgeDirection other : ForgeDirection.VALID_DIRECTIONS) {
                if (fd == other || fd == other.getOpposite()) {
                    continue;
                }
                int ord = other.ordinal();
                any |= (extend[ord] = sides[ord]);
            }
            block.setTexture(i, sides[i] && !any? null : icon);
            
            block.setBlockBounds(
                    extend[4] ? 0 : fL,
                    extend[0] ? 0 : fL,
                    extend[2] ? 0 : fL,
                    extend[5] ? 1 : fH,
                    extend[1] ? 1 : fH,
                    extend[3] ? 1 : fH);

            //renderBlock(rb, block);
            if (world_mode) {
                block.begin();
                block.renderRotated(Tessellator.instance, x, y, z);
            } else {
                block.renderForInventory(rb);
            }
            
            Arrays.fill(extend, false);
            block.setTexture(i, null);
        }
        return true;
    }

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.SERVORAIL;
    }
}
