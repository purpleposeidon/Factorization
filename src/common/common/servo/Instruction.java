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
    boolean onClick(EntityPlayer player, Coord block, ForgeDirection side) {
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
    boolean onClick(EntityPlayer player, ServoMotor motor) {
        return false;
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public void renderStatic(Coord where, RenderBlocks rb) {
        BlockRenderHelper block = BlockRenderHelper.instance;
        for (int i = 0; i < 6; i++) {
            ForgeDirection face = ForgeDirection.getOrientation(i);
            block.setTexture(i, getIcon(face));
        }
        if (where == null) {
            block.renderForTileEntity();
        } else {
            block.render(FactorizationUtil.getRB(), where);
        }
    }
}
