package factorization.common.sockets;

import java.io.IOException;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumMovingObjectType;
import net.minecraft.util.Icon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.FakePlayer;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.api.Coord;
import factorization.api.FzOrientation;
import factorization.api.Quaternion;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.common.BlockIcons;
import factorization.common.BlockRenderHelper;
import factorization.common.FactorizationUtil;
import factorization.common.FactorizationUtil.FzInv;
import factorization.common.FactoryType;
import factorization.common.ISocketHolder;
import factorization.common.TileEntitySocketBase;

public class SocketRobotHand extends TileEntitySocketBase {
    boolean wasPowered = false;

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.SOCKET_ROBOTHAND;
    }
    
    @Override
    public boolean canUpdate() {
        return true;
    }
    
    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        wasPowered = data.asSameShare("pow").putBoolean(wasPowered);
        return this;
    }
    
    @Override
    public void genericUpdate(ISocketHolder socket, Entity ent, Coord coord, boolean powered) {
        if (wasPowered || !powered) {
            wasPowered = powered;
            return;
        }
        wasPowered = true;
        IInventory back = getBackingInventory(socket);
        if (back == null) {
            return;
        }
        FzOrientation orientation = FzOrientation.fromDirection(facing).getSwapped();
        rayTrace(socket, ent, coord, orientation, powered, false, true);
    }
    
    @Override
    public boolean handleRay(ISocketHolder socket, MovingObjectPosition mop, boolean mopIsThis, boolean powered) {
        FakePlayer player = getFakePlayer();
        FzInv inv = FactorizationUtil.openInventory(getBackingInventory(socket), facing);
        boolean foundAny = false;
        for (int i = 0; i < inv.size(); i++) {
            ItemStack is = inv.get(i);
            if (is == null || !inv.canExtract(i, is)) {
                continue;
            }
            is = inv.pull(i, 1);
            if (is == null) {
                continue; //Unexpected...
            }
            foundAny = true;
            if (clickItem(player, is, mop)) {
                if (is.stackSize >= 0) {
                    is = inv.pushInto(i, is);
                    if (is != null) {
                        is = inv.push(is);
                        if (is != null) {
                            getCoord().spawnItem(is); //Uh-oh!
                        }
                    }
                }
                return true;
            }
        }
        return foundAny;
    }
    
    boolean clickItem(FakePlayer player, ItemStack is, MovingObjectPosition mop) {
        if (mop.typeOfHit == EnumMovingObjectType.TILE) {
            Vec3 hitVec = mop.hitVec;
            int x = mop.blockX, y = mop.blockY, z = mop.blockZ;
            float dx = (float) (hitVec.xCoord - x);
            float dy = (float) (hitVec.yCoord - y);
            float dz = (float) (hitVec.zCoord - z);
            Item it = is.getItem();
            if (it.onItemUseFirst(is, player, worldObj, x, y, z, mop.sideHit, dx, dy, dz)) {
                return true;
            }
            if (it.onItemUse(is, player, worldObj, x, y, z, mop.sideHit, dx, dy, dz)) {
                return true;
            }
        } else if (mop.typeOfHit == EnumMovingObjectType.ENTITY) {
            if (mop.entityHit.interactFirst(player)) {
                return true;
            }
            if (mop.entityHit instanceof EntityLiving) {
                if (is.func_111282_a(player, (EntityLiving)mop.entityHit)) {
                    return true;
                }
            }
        }
        return true; //uh, yeah. Weird.
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public void renderStatic(Tessellator tess) {
        Icon icon = BlockIcons.dark_iron_block;
        BlockRenderHelper block = BlockRenderHelper.instance;
        float w = 5F/16F;
        block.setBlockBoundsOffset(w, 0, w);
        block.useTextures(icon, null,
                icon, icon,
                icon, icon);
        block.begin();
        block.rotateCenter(Quaternion.fromOrientation(FzOrientation.fromDirection(facing.getOpposite())));
        block.renderRotated(tess, xCoord, yCoord, zCoord);
    }
}
