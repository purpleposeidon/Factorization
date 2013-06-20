package factorization.common.servo;

import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Icon;
import net.minecraftforge.common.ForgeDirection;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.api.Coord;
import factorization.common.BlockRenderHelper;
import factorization.common.Core;
import factorization.common.FactorizationUtil;


public abstract class Decorator extends ServoComponent {
    public abstract void motorHit(ServoMotor motor);
    @SideOnly(Side.CLIENT)
    private static class StretchedIcon implements Icon {
        public Icon under;

        @Override
        @SideOnly(Side.CLIENT)
        public int getOriginX() {
            return under.getOriginX();
        }

        @Override
        @SideOnly(Side.CLIENT)
        public int getOriginY() {
            return under.getOriginY();
        }

        @Override
        @SideOnly(Side.CLIENT)
        public float getMinU() {
            return under.getMinU();
        }

        @Override
        @SideOnly(Side.CLIENT)
        public float getMaxU() {
            return under.getMaxU();
        }

        @Override
        @SideOnly(Side.CLIENT)
        public float getInterpolatedU(double d0) {
            return d0 > 8 ? under.getMaxU() : under.getMinU();
        }

        @Override
        @SideOnly(Side.CLIENT)
        public float getMinV() {
            return under.getMinV();
        }

        @Override
        @SideOnly(Side.CLIENT)
        public float getMaxV() {
            return under.getMaxV();
        }

        @Override
        @SideOnly(Side.CLIENT)
        public float getInterpolatedV(double d0) {
            return d0 > 8 ? under.getMaxV() : under.getMinV();
        }

        @Override
        @SideOnly(Side.CLIENT)
        public String getIconName() {
            return under.getIconName();
        }

        @Override
        @SideOnly(Side.CLIENT)
        public int getSheetWidth() {
            return under.getSheetWidth();
        }

        @Override
        @SideOnly(Side.CLIENT)
        public int getSheetHeight() {
            return under.getSheetHeight();
        }
        
    }
    
    @SideOnly(Side.CLIENT)
    private static StretchedIcon[] stretcher;
    
    @Override
    @SideOnly(Side.CLIENT)
    public void renderStatic(Coord where, RenderBlocks rb) {
        if (stretcher == null) {
            stretcher = new StretchedIcon[6];
            for (int i = 0; i < stretcher.length; i++) {
                stretcher[i] = new StretchedIcon();
            }
        }
        BlockRenderHelper block = BlockRenderHelper.instance;
        float d = getSize();
        block.setBlockBoundsOffset(d, d, d);
        for (int i = 0; i < 6; i++) {
            ForgeDirection face = ForgeDirection.getOrientation(i);
            Icon icon = getIcon(face);
            if (stretchIcon()) {
                stretcher[i].under = icon;
                block.setTexture(i, stretcher[i]);
            } else {
                block.setTexture(i, icon);
            }
        }
        if (where == null) {
            block.renderForTileEntity();
        } else {
            block.render(FactorizationUtil.getRB(), where);
        }
    }
    
    public abstract Icon getIcon(ForgeDirection side);
    public float getSize() {
        return TileEntityServoRail.width - 1F/2048F;
        //return 6F/16F;
    }
    public boolean stretchIcon() {
        return true;
    }
    
    public static boolean playerHasProgrammer(EntityPlayer player) {
        if (player == null) {
            return false;
        }
        ItemStack cur = player.getCurrentEquippedItem();
        if (cur == null) {
            return false;
        }
        return cur.getItem() == Core.registry.logicMatrixProgrammer;
    }
    
    public boolean isFreeToPlace() {
        return false;
    }
}
