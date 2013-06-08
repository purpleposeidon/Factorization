package factorization.common.servo;

import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Icon;
import net.minecraftforge.common.ForgeDirection;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.api.Coord;
import factorization.common.BlockRenderHelper;
import factorization.common.FactorizationUtil;


public abstract class Instruction extends Decorator {
    public abstract Icon getIcon(ForgeDirection side);
    
    @Override
    public boolean onClick(EntityPlayer player, Coord block, ForgeDirection side) {
        TileEntityServoRail rail = block.getTE(TileEntityServoRail.class);
        if (rail == null) {
            return false;
        }
        if (rail.decoration != null) {
            FactorizationUtil.spawnItemStack(player, rail.decoration.toItem());
        }
        rail.setDecoration(this);
        return true;
    }
    
    @Override
    public boolean onClick(EntityPlayer player, ServoMotor motor) {
        return false;
    }
    
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
    private static StretchedIcon stretcher;
    
    @Override
    @SideOnly(Side.CLIENT)
    public void renderStatic(Coord where, RenderBlocks rb) {
        if (stretcher == null) {
            stretcher = new StretchedIcon();
        }
        BlockRenderHelper block = BlockRenderHelper.instance;
        float d = 6F/16F;
        block.setBlockBoundsOffset(d, d, d);
        for (int i = 0; i < 6; i++) {
            ForgeDirection face = ForgeDirection.getOrientation(i);
            stretcher.under = getIcon(face);
            block.setTexture(i, stretcher);
        }
        if (where == null) {
            block.renderForTileEntity();
        } else {
            block.render(FactorizationUtil.getRB(), where);
        }
    }
}
