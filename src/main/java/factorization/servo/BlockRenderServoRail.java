package factorization.servo;

import java.util.Arrays;
import java.util.Locale;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.IIcon;
import net.minecraftforge.common.util.ForgeDirection;
import factorization.api.FzColor;
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
        if (world_mode) {
            if (te instanceof TileEntityServoRail) {
                rail = (TileEntityServoRail) te;
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
        
        if (world_mode && rail != null) {
            boolean has_comment = rail.comment != null && rail.comment.length() > 0;
            drawWithTexture(rb, has_comment ? BlockIcons.servo$rail_comment : BlockIcons.servo$rail);
            if (rail.color != FzColor.NO_COLOR) {
                drawWithTexture(rb, coloredRails[rail.color.toVanillaColorIndex()]);
            }
            Decorator dec = rail.decoration;
            if (dec != null) {
                dec.renderStatic(rail.getCoord(), rb);
            }
        } else {
            final float fL = TileEntityServoRail.width;
            final float fH = 1 - fL;
            block.useTexture(BlockIcons.servo$rail);
            block.setBlockBounds(0, fL, fL, 1, fH, fH);
            block.renderForInventory(rb);
            block.setBlockBounds(fL, 0, fL, fH, 1, fH);
            block.renderForInventory(rb);
            block.setBlockBounds(fL, fL, 0, fH, fH, 1);
            block.renderForInventory(rb);
        }
        return true;
    }
    
    void drawWithTexture(RenderBlocks rb, IIcon icon) {
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

            block.beginWithHipsterUVs();
            block.renderRotated(Tessellator.instance, x, y, z);
            
            Arrays.fill(extend, false);
            block.setTexture(i, null);
        }
    }

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.SERVORAIL;
    }

    public static IIcon[] coloredRails = new IIcon[FzColor.VALID_COLORS.length];
    public static void registerColoredIcons(IIconRegister reg) {
        for (int i = 0; i < FzColor.VALID_COLORS.length; i++) {
            FzColor color = FzColor.VALID_COLORS[i];
            coloredRails[i] = reg.registerIcon("factorization:servo/colored_rails/" + color.toString().toLowerCase(Locale.ROOT));
        }
    }
}
