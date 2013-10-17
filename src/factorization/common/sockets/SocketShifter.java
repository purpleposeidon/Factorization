package factorization.common.sockets;

import java.io.IOException;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.inventory.IInventory;
import net.minecraftforge.common.ForgeDirection;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.api.Coord;
import factorization.api.FzOrientation;
import factorization.api.Quaternion;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.api.datahelpers.Share;
import factorization.common.BlockIcons;
import factorization.common.BlockRenderHelper;
import factorization.common.FactorizationUtil;
import factorization.common.FactorizationUtil.FzInv;
import factorization.common.FactoryType;
import factorization.common.ISocketHolder;
import factorization.common.TileEntitySocketBase;

public class SocketShifter extends TileEntitySocketBase {
    boolean streamMode = true; // be like a hopper or a filter
    int foreignSlot = -1;
    boolean exporting;
    byte transferLimit = 1;
    byte cooldown = 0;
    
    @Override
    public FactoryType getFactoryType() {
        return FactoryType.SOCKET_SHIFTER;
    }
    
    @Override
    public boolean canUpdate() {
        return true;
    }
    
    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        streamMode = data.as(Share.MUTABLE, "strm").putBoolean(streamMode);
        exporting = data.as(Share.MUTABLE, "exp").putBoolean(exporting);
        foreignSlot = data.as(Share.MUTABLE, "for").putInt(foreignSlot);
        transferLimit = data.as(Share.MUTABLE, "lim").putByte(transferLimit);
        cooldown = data.as(Share.VISIBLE, "wait").putByte(cooldown);
        if (data.isWriter()) {
            return this;
        }
        //Validate input
        if (streamMode) {
            transferLimit = 1;
        }
        if (foreignSlot < 0) {
            foreignSlot = 0;
            data.log("foreign slot was < 0");
        }
        if (transferLimit > 64) {
            transferLimit = 64;
            data.log("transfer limit was > 64");
        }
        if (transferLimit < 1) {
            transferLimit = 1;
            data.log("transfer limit was < 1");
        }
        return this;
    }
    
    @Override
    public void genericUpdate(ISocketHolder socket, Coord coord, boolean powered) {
        if (worldObj.isRemote) {
            return;
        }
        if (cooldown > 0) {
            cooldown--;
            return;
        }
        if (!powered) {
            return;
        }
        FzInv localInv, foreignInv;
        ForgeDirection back = facing.getOpposite();
        if (socket != this) {
            // meaning we're on a Servo
            localInv = FactorizationUtil.openInventory((IInventory) socket, facing);
        } else {
            coord.adjust(back);
            localInv = FactorizationUtil.openInventory(coord.getTE(IInventory.class), facing);
            coord.adjust(facing);
        }
        if (localInv == null) {
            return;
        }
        coord.adjust(facing);
        foreignInv = FactorizationUtil.openInventory(coord.getTE(IInventory.class), back);
        coord.adjust(back);
        if (foreignInv == null) {
            return;
        }
        if (exporting) {
            if (foreignSlot == -1) {
                localInv.transfer(foreignInv, transferLimit, null);
            } else {
                for (int localSlot = 0; localSlot < localInv.size(); localSlot++) {
                    if (localInv.transfer(localSlot, foreignInv, foreignSlot, transferLimit)) {
                        break;
                    }
                }
            }
        } else {
            if (foreignSlot == -1) {
                foreignInv.transfer(localInv, transferLimit, null);
            } else {
                for (int localSlot = 0; localSlot < localInv.size(); localSlot++) {
                    if (foreignInv.transfer(foreignSlot, localInv, localSlot, transferLimit)) {
                        break;
                    }
                }
            }
        }
        cooldown = (byte) (streamMode ? 8 : 1);
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public void renderStatic(Tessellator tess) {
        BlockRenderHelper block = BlockRenderHelper.instance;
        block.useTextures(BlockIcons.socket$shifter_front, null,
                BlockIcons.socket$shifter_side, BlockIcons.socket$shifter_side,
                BlockIcons.socket$shifter_side, BlockIcons.socket$shifter_side,
                BlockIcons.socket$shifter_side, BlockIcons.socket$shifter_side);
        final float minYs[] = new float[] { 8F/16F, 3F/16F, -2F/16F };
        final float ds[] = new float[] { 4F/16F, 5F/16F, 6F/16F };
        for (int i = 0; i < ds.length; i++) {
            float d = ds[i];
            float minY = minYs[i];
            block.setBlockBounds(d, minY, d, 1-d, 12F/16F, 1-d);
            block.begin();
            block.rotateCenter(Quaternion.fromOrientation(FzOrientation.fromDirection(facing.getOpposite())));
            block.renderRotated(tess, xCoord, yCoord, zCoord);
        }
    }

}
