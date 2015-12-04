package factorization.sockets;

import factorization.api.FzOrientation;
import factorization.api.Quaternion;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.common.BlockIcons;
import factorization.common.FactoryType;
import factorization.servo.ServoMotor;
import factorization.shared.BlockRenderHelper;
import factorization.shared.Core;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.IOException;

public class SocketBareMotor extends TileEntitySocketBase {

    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        return this;
    }

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.SOCKET_BARE_MOTOR;
    }

    @Override
    public boolean canUpdate() {
        return false;
    }

    @Override
    public ItemStack getCreatingItem() {
        return new ItemStack(Core.registry.motor);
    }

    @Override
    public FactoryType getParentFactoryType() {
        return FactoryType.SOCKET_EMPTY;
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public void renderStatic(ServoMotor motor, Tessellator tess) {
        BlockRenderHelper block = BlockRenderHelper.instance;
        Quaternion rotation = Quaternion.fromOrientation(FzOrientation.fromDirection(facing.getOpposite()));
        {
            IIcon metal = BlockIcons.motor_texture;
            float d = 4.0F / 16.0F;
            float yd = -d + 0.003F;
    
            block.useTextures(metal, null,
                    metal, metal,
                    metal, metal);
            float yoffset = 5F/16F;
            float sd = motor == null ? 0 : 2F/16F;
            block.setBlockBounds(d, d + yd + yoffset + 2F/16F + sd, d, 1 - d, 1 - (d + 0F/16F) + yd + yoffset, 1 - d);
            block.beginWithMirroredUVs();
            block.rotateCenter(rotation);
            block.renderRotated(tess, pos.getX(), pos.getY(), pos.getZ());
        }
    }
}
