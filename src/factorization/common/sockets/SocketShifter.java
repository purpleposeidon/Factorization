package factorization.common.sockets;

import java.io.IOException;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
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
import factorization.common.FactorizationUtil;
import factorization.common.FactorizationUtil.FzInv;
import factorization.common.BlockRenderHelper;
import factorization.common.FactoryType;
import factorization.common.ISocketHolder;
import factorization.common.TileEntitySocketBase;

public class SocketShifter extends TileEntitySocketBase {
    int localSlot = -1, foreignSlot = -1;
    boolean exporting;
    byte transferLimit = 1;
    byte cooldown = 0;
    
    @Override
    public FactoryType getFactoryType() {
        return FactoryType.SOCKET_SHIFTER;
    }
    
    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        localSlot = data.as(Share.PRIVATE, "loc").putInt(localSlot);
        foreignSlot = data.as(Share.PRIVATE, "for").putInt(foreignSlot);
        exporting = data.as(Share.PRIVATE, "exp").putBoolean(exporting);
        transferLimit = data.as(Share.PRIVATE, "lim").putByte(transferLimit);
        cooldown = data.as(Share.PRIVATE, "wait").putByte(cooldown);
        return this;
    }
    
    @Override
    public boolean canUpdate() {
        return true;
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
            exportItems(localInv, foreignInv);
        } else {
            exportItems(foreignInv, localInv);
        }
    }
    
    void exportItems(FzInv local, FzInv foreign) {
        int localStart, localEnd, foreignStart, foreignEnd;
        int localSize = local.size(), foreignSize = foreign.size();
        byte COOL = 8;
        if (localSlot == -1) {
            localStart = 0;
            localEnd = localSize;
        } else {
            if (localSlot < 0 || localSlot >= localSize) {
                cooldown = COOL;
                return;
            }
            localStart = localSlot;
            localEnd = localStart + 1;
        }
        if (foreignSlot == -1) {
            foreignStart = 0;
            foreignEnd = foreignSize;
        } else {
            if (foreignSlot < 0 || foreignSlot >= foreignSize) {
                cooldown = COOL;
                return;
            }
            foreignStart = foreignSlot;
            foreignEnd = foreignSlot + 1;
        }
        
        for (int l = localStart; l < localEnd; l++) {
            ItemStack lis = local.get(l);
            if (lis == null || !local.canExtract(l, lis)) {
                continue;
            }
            int origSize = lis.stackSize;
            for (int f = foreignStart; f < foreignEnd; f++) {
                if (foreign.get(f) == null) {
                    continue;
                }
                lis = foreign.pushInto(f, lis);
                if (lis == null) {
                    local.set(l, null);
                    cooldown = COOL;
                    return;
                }
            }
            for (int f = foreignStart; f < foreignEnd; f++) {
                if (foreign.get(f) != null) {
                    continue;
                }
                lis = foreign.pushInto(f, lis);
                if (lis == null) {
                    local.set(l, null);
                    cooldown = COOL;
                    return;
                }
            }
            if (FactorizationUtil.getStackSize(lis) == origSize) {
                continue;
            }
            local.set(l, lis);
            cooldown = COOL;
        }
    }
    
    
    @Override
    @SideOnly(Side.CLIENT)
    public void renderStatic(Tessellator tess) {
        BlockRenderHelper block = BlockRenderHelper.instance;
        block.useTextures(BlockIcons.socket$shifter_front, null,
                BlockIcons.socket$shifter_side, BlockIcons.socket$shifter_side,
                BlockIcons.socket$shifter_side, BlockIcons.socket$shifter_side,
                BlockIcons.socket$shifter_side, BlockIcons.socket$shifter_side);
        float d = 4F/16F;
        block.setBlockBounds(d, 8F/16F, d, 1-d, 12F/16F, 1-d);
        block.begin();
        block.rotateCenter(Quaternion.fromOrientation(FzOrientation.fromDirection(facing.getOpposite())));
        block.renderRotated(tess, xCoord, yCoord, zCoord);
        d = 5F/16F;
        block.setBlockBounds(d, 3F/16F, d, 1-d, 12F/16F, 1-d);
        block.begin();
        block.rotateCenter(Quaternion.fromOrientation(FzOrientation.fromDirection(facing.getOpposite())));
        block.renderRotated(tess, xCoord, yCoord, zCoord);
        d = 6F/16F;
        block.setBlockBounds(d, -2F/16F, d, 1-d, 12F/16F, 1-d);
        block.begin();
        block.rotateCenter(Quaternion.fromOrientation(FzOrientation.fromDirection(facing.getOpposite())));
        block.renderRotated(tess, xCoord, yCoord, zCoord);
    }

}
