package factorization.servo;

import factorization.servo.stepper.StepperEngine;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import factorization.api.Coord;
import factorization.common.BlockIcons;
import factorization.shared.BlockRenderHelper;
import factorization.shared.Core;


public abstract class Decorator extends ServoComponent {
    public abstract void motorHit(ServoMotor motor);
    public boolean preMotorHit(ServoMotor motor) {
        return false;
    }

    public void stepperHit(StepperEngine engine) { }
    
    @SideOnly(Side.CLIENT)
    private static class StretchedIIcon implements IIcon {
        public IIcon under;

        @Override
        @SideOnly(Side.CLIENT)
        public int getIconWidth() {
            return under.getIconWidth();
        }

        @Override
        @SideOnly(Side.CLIENT)
        public int getIconHeight() {
            return under.getIconHeight();
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
    }
    
    @SideOnly(Side.CLIENT)
    private static StretchedIIcon[] stretcher;
    
    @Override
    @SideOnly(Side.CLIENT)
    public void renderStatic(Coord where, RenderBlocks rb) {
        if (stretcher == null) {
            stretcher = new StretchedIIcon[6];
            for (int i = 0; i < stretcher.length; i++) {
                stretcher[i] = new StretchedIIcon();
            }
        }
        BlockRenderHelper block = BlockRenderHelper.instance;
        float d = getSize();
        block.setBlockBoundsOffset(d, d, d);
        for (int i = 0; i < 6; i++) {
            EnumFacing face = SpaceUtil.getOrientation(i);
            IIcon icon = getIcon(face);
            if (icon == null) {
                icon = BlockIcons.uv_test;
            }
            if (stretchIIcon()) {
                stretcher[i].under = icon;
                block.setTexture(i, stretcher[i]);
            } else {
                block.setTexture(i, icon);
            }
        }
        if (where == null) {
            block.renderForTileEntity();
        } else {
            block.beginWithRotatedUVs();
            block.renderRotated(Tessellator.instance, where);
        }
    }
    
    public abstract IIcon getIcon(EnumFacing side);
    public float getSize() {
        return TileEntityServoRail.width - 1F/2048F;
        //return 6F/16F;
    }
    public boolean stretchIIcon() {
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
    
    public String getInfo() {
        return null;
    }
    
    public void onPlacedOnRail(TileEntityServoRail sr) {}
    
    public boolean collides() {
        return true;
    }
    
    public void afterClientLoad(TileEntityServoRail rail) { }
}
