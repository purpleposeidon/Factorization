package factorization.sockets;

import java.io.IOException;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumMovingObjectType;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
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
import factorization.common.FactoryType;
import factorization.notify.Notify;
import factorization.servo.ServoMotor;
import factorization.shared.BlockRenderHelper;
import factorization.shared.Core;
import factorization.shared.FzUtil;
import factorization.shared.FzUtil.FzInv;

public class SocketRobotHand extends TileEntitySocketBase {
    boolean wasPowered = false;
    boolean firstTry = false;
    @Override
    public FactoryType getFactoryType() {
        return FactoryType.SOCKET_ROBOTHAND;
    }
    
    @Override
    public FactoryType getParentFactoryType() {
        return FactoryType.SOCKET_EMPTY;
    }
    
    @Override
    public ItemStack getCreatingItem() {
        return Core.registry.socket_robot_hand;
    }
    
    @Override
    public boolean canUpdate() {
        return true;
    }
    
    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        wasPowered = data.as(Share.PRIVATE, "pow").putBoolean(wasPowered);
        return this;
    }
    
    @Override
    public void genericUpdate(ISocketHolder socket, Coord coord, boolean powered) {
        if (worldObj.isRemote) {
            return;
        }
        if (wasPowered || !powered) {
            wasPowered = powered;
            return;
        }
        wasPowered = true;
        IInventory back = getBackingInventory(socket);
        if (back == null) {
            return;
        }
        firstTry = true;
        FzOrientation orientation = FzOrientation.fromDirection(facing).getSwapped();
        fakePlayer = null;
        rayTrace(socket, coord, orientation, powered, true, false);
        fakePlayer = null;
    }
    
    EntityPlayer fakePlayer;
    
    @Override
    public boolean handleRay(ISocketHolder socket, MovingObjectPosition mop, boolean mopIsThis, boolean powered) {
        if (fakePlayer == null) {
            fakePlayer = getFakePlayer();
        }
        EntityPlayer player = fakePlayer;
        FzInv inv = FzUtil.openInventory(getBackingInventory(socket), facing);
        boolean foundAny = false;
        int anEmpty = -1;
        for (int i = 0; i < inv.size(); i++) {
            ItemStack is = inv.get(i);
            if (is == null || is.stackSize <= 0 || !inv.canExtract(i, is)) {
                if (anEmpty != -1) {
                    anEmpty = i;
                }
                continue;
            }
            player.inventory.mainInventory[0] = is;
            foundAny = true;
            if (clickWithInventory(i, inv, player, is, mop)) {
                return true;
            }
        }
        if (!foundAny && anEmpty != -1) {
            return clickWithInventory(anEmpty, inv, player, null, mop);
        }
        return false;
    }
    
    boolean clickWithInventory(int i, FzInv inv, EntityPlayer player, ItemStack is, MovingObjectPosition mop) {
        ItemStack orig = is == null ? null : is.copy();
        boolean result = clickItem(player, is, mop);
        firstTry = false;
        int newSize = FzUtil.getStackSize(is);
        is = player.inventory.mainInventory[0];
        // Easiest case: the item is all used up.
        // Worst case: barrel of magic jumping beans that change color.
        // (Or more realistically, a barrel of empty buckets for milking a cow...)
        // To handle: extract the entire stack. It is lost. Attempt to stuff the rest of the inv back in.
        // Anything that can't be stuffed gets dropped on the ground.
        // This could break with funky items/inventories tho.
        if (newSize <= 0 || !FzUtil.couldMerge(orig, is)) {
            inv.set(i, null); //Bye-bye!
            if (newSize > 0) {
                is = inv.pushInto(i, is);
                if (is == null || is.stackSize <= 0) {
                    player.inventory.mainInventory[0] = null;
                }
            }
        } else {
            // We aren't calling inv.decrStackInSlot.
            inv.set(i, is);
            player.inventory.mainInventory[0] = null;
        }
        Coord here = null;
        for (int j = 0; j < player.inventory.getSizeInventory(); j++) {
            ItemStack toPush = player.inventory.getStackInSlot(j);
            ItemStack toDrop = inv.push(toPush);
            if (toDrop != null && toDrop.stackSize > 0) {
                if (here == null) here = getCoord();
                here.spawnItem(toDrop);
            }
            player.inventory.setInventorySlotContents(j, null);
        }
        inv.onInvChanged();
        return result;
    }
    
    boolean clickItem(EntityPlayer player, ItemStack is, MovingObjectPosition mop) {
        if (mop.typeOfHit == EnumMovingObjectType.TILE) {
            return mcClick(player, mop, is);
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
        return false;
    }
    
    boolean mcClick(EntityPlayer player, MovingObjectPosition mop, ItemStack itemstack) {
        //Yoinked and cleaned up from Minecraft.clickMouse and PlayerControllerMP.onPlayerRightClick
        final World world = player.worldObj;
        final int x = mop.blockX;
        final int y = mop.blockY;
        final int z = mop.blockZ;
        final int side = mop.sideHit;
        final Vec3 hitVec = mop.hitVec;
        final float dx = (float)hitVec.xCoord - (float)x;
        final float dy = (float)hitVec.yCoord - (float)y;
        final float dz = (float)hitVec.zCoord - (float)z;
        final Item item = itemstack == null ? null : itemstack.getItem();
        
        boolean ret = false;
        do {
            //PlayerControllerMP.onPlayerRightClick
            if (firstTry && itemstack != null) {
                ItemStack orig = itemstack.copy();
                if (item.onItemUseFirst(itemstack, player, world, x, y, z, side, dx, dy, dz)) {
                    ret = true;
                    break;
                }
                if (!FzUtil.identical(itemstack, orig)) {
                    ret = true;
                    break;
                }
            }
            
            if (!player.isSneaking() || itemstack == null || item.shouldPassSneakingClickToBlock(world, x, y, z)) {
                int blockId = world.getBlockId(x, y, z);
            
                if (blockId > 0 && Block.blocksList[blockId].onBlockActivated(world, x, y, z, player, side, dx, dy, dz)) {
                    ret = true;
                    break;
                }
            }
            if (itemstack == null) {
                ret = false;
                break;
            }
            if (item instanceof ItemBlock) {
                ItemBlock itemblock = (ItemBlock)item;
                if (!itemblock.canPlaceItemBlockOnSide(world, x, y, z, side, player, itemstack)) {
                    ret = false;
                    break;
                }
            }
            ret = itemstack.tryPlaceItemIntoWorld(player, world, x, y, z, side, dx, dy, dz);
            break;
        } while (false);
        int origSize = itemstack.stackSize;
        ItemStack mutatedItem = itemstack.useItemRightClick(world, player);
        if (!ret && !FzUtil.identical(mutatedItem, itemstack)) {
            ret = true;
        }
        if (mutatedItem == itemstack && (mutatedItem == null || mutatedItem.stackSize == origSize)) {
            return ret;
        }
        player.inventory.mainInventory[player.inventory.currentItem] = mutatedItem;

        return true;
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public void renderStatic(ServoMotor motor, Tessellator tess) {
        BlockRenderHelper block = BlockRenderHelper.instance;
        float w = 6F/16F;
        block.setBlockBoundsOffset(w, 0, w);
        block.useTextures(BlockIcons.socket$hand, null,
                BlockIcons.socket$arm0, BlockIcons.socket$arm1, 
                BlockIcons.socket$arm2, BlockIcons.socket$arm3);
        block.begin();
        block.rotateCenter(Quaternion.fromOrientation(FzOrientation.fromDirection(facing.getOpposite())));
        if (motor != null) {
            block.translate(0, -2F/16F, 0);
        }
        block.renderRotated(tess, xCoord, yCoord, zCoord);
    }
    
    @Override
    public boolean activate(EntityPlayer entityplayer, ForgeDirection side) {
        if (worldObj.isRemote) {
            return false;
        }
        if (getBackingInventory(this) == null) {
            Notify.send(this, "Missing inventory block");
        }
        return false;
    }
}
