package factorization.charge;

import java.io.DataInputStream;
import java.io.IOException;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.packet.Packet;
import net.minecraft.util.IIcon;
import net.minecraftforge.common.util.ForgeDirection;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.api.Coord;
import factorization.api.DeltaCoord;
import factorization.api.IReflectionTarget;
import factorization.common.BlockIcons;
import factorization.common.FactoryType;
import factorization.shared.BlockClass;
import factorization.shared.Core;
import factorization.shared.TileEntityCommon;
import factorization.shared.NetworkFactorization.MessageType;

public class TileEntityMirror extends TileEntityCommon {
    Coord reflection_target = null;

    //don't save
    public boolean is_lit = false;
    int next_check = 1;
    //don't save, but *do* share w/ client
    public int target_rotation = -99;

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.MIRROR;
    }

    @Override
    public BlockClass getBlockClass() {
        return BlockClass.Machine;
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        if (reflection_target != null) {
            reflection_target.writeToNBT("target", tag);
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        if (tag.hasKey("targetx")) {
            reflection_target = getCoord();
            reflection_target.readFromNBT("target", tag);
            updateRotation();
        }
        else {
            reflection_target = null;
        }
    }
    
    @Override
    public boolean activate(EntityPlayer entityplayer, ForgeDirection side) {
        neighborChanged();
        return false;
    }

    @Override
    public void neighborChanged() {
        next_check = -1;
    }

    int getPower() {
        return 1;
    }

    int clipAngle(int angle) {
        angle = angle % 360;
        if (angle < 0) {
            angle += 360;
        }
        return angle;
    }

    boolean hasSun() {
        boolean raining = getWorldObj().isRaining() && getWorldObj().getBiomeGenForCoords(xCoord, yCoord).rainfall > 0;
        return getCoord().canSeeSky() && worldObj.isDaytime() && !raining;
    }

    int last_shared = -1;

    void broadcastTargetInfoIfChanged() {
        if (getTargetInfo() != last_shared) {
            broadcastMessage(null, MessageType.MirrorDescription, getTargetInfo());
            last_shared = getTargetInfo();
        }
    }

    int getTargetInfo() {
        return reflection_target == null ? -99 : target_rotation;
    }

    void setRotationTarget(int new_target) {
        if (this.target_rotation != new_target) {
            this.target_rotation = new_target;
        }
    }

    @Override
    public Packet getAuxillaryInfoPacket() {
        return getDescriptionPacketWith(MessageType.MirrorDescription, target_rotation, getTargetInfo());
    }

    @Override
    public boolean handleMessageFromServer(int messageType, DataInputStream input) throws IOException {
        if (super.handleMessageFromServer(messageType, input)) {
            return true;
        }
        switch (messageType) {
        case MessageType.MirrorDescription:
            target_rotation = input.readInt();
            getCoord().redraw();
            gotten_info_packet = true;
            return true;
        }
        return false;
    }
    
    @Override
    protected void onRemove() {
        super.onRemove();
        if (reflection_target == null) {
            return;
        }
        reflection_target.w = worldObj;
        if (worldObj == null) {
            return;
        }
        IReflectionTarget target = reflection_target.getTE(IReflectionTarget.class);
        if (target == null) {
            return;
        }
        if (is_lit) {
            target.addReflector(-getPower());
            is_lit = false;
        }
        reflection_target = null;
    }
    
    @Override
    public void invalidate() {
        super.invalidate();
        if (worldObj != null) {
            onRemove();
        }
    }

    boolean gotten_info_packet = false;
    
    void setNextCheck() {
        next_check = 80 + rand.nextInt(20);
    }
    
    @Override
    public void updateEntity() {
        if (worldObj.isRemote) {
            return;
        }
        if (next_check-- <= 0) {
            try {
                setNextCheck();
                if (reflection_target == null) {
                    findTarget();
                    if (reflection_target == null) {
                        return;
                    }
                } else {
                    reflection_target.setWorld(worldObj);
                }
                //we *do* have a target coord by this point. Is there a TE there tho?
                IReflectionTarget target = null;
                target = reflection_target.getTE(IReflectionTarget.class);
                if (target == null) {
                    if (reflection_target.blockExists()) {
                        reflection_target = null;
                        is_lit = false;
                    }
                    return;
                }
                if (!myTrace(reflection_target.x, reflection_target.z)) {
                    if (is_lit) {
                        is_lit = false;
                        target.addReflector(-getPower());
                        reflection_target = null;
                        setRotationTarget(-99);
                        return;
                    }
                }
    
                if (hasSun() != is_lit) {
                    is_lit = hasSun();
                    target.addReflector(is_lit ? getPower() : -getPower());
                }
            } finally {
                broadcastTargetInfoIfChanged();
            }
        }
    }

    void findTarget() {
        if (reflection_target != null) {
            //make the old target forget about us
            IReflectionTarget target = reflection_target.getTE(IReflectionTarget.class);
            if (target != null) {
                if (is_lit) {
                    target.addReflector(-getPower());
                }
                reflection_target = null;
            }
            is_lit = false;
        }

        int search_distance = 11;
        IReflectionTarget closest = null;
        int last_dist = Integer.MAX_VALUE;
        Coord me = getCoord();
        double maxRadiusSq = 8.9*8.9;
        for (int x = xCoord - search_distance; x <= xCoord + search_distance; x++) {
            for (int z = zCoord - search_distance; z <= zCoord + search_distance; z++) {
                Coord here = new Coord(worldObj, x, yCoord, z);
                IReflectionTarget target = here.getTE(IReflectionTarget.class);
                if (target == null) {
                    continue;
                }
                if (!myTrace(x, z)) {
                    continue;
                }
                int new_dist = me.distanceSq(here);
                if (new_dist < last_dist && new_dist <= maxRadiusSq) {
                    last_dist = new_dist;
                    closest = target;
                }
            }
        }
        if (closest != null) {
            reflection_target = closest.getCoord();
            updateRotation();
        } else {
            setRotationTarget(-99);
        }
    }

    void updateRotation() {
        DeltaCoord dc = getCoord().difference(reflection_target);

        int new_target = clipAngle((int) Math.toDegrees(dc.getAngleHorizontal()));
        setRotationTarget(new_target);
    }

    double div(double a, double b) {
        if (b == 0) {
            return Math.signum(a) * 0xFFF;
        }
        return a / b;
    }

    boolean myTrace(double x, double z) {
        x += 0.5;
        z += 0.5;
        double offset_x = x - (xCoord + 0.5), offset_z = z - (zCoord + 0.5);
        double length = Math.hypot(offset_x, offset_z);
        double dx = offset_x / length, dz = offset_z / length;
        x -= dx;
        z -= dz;
        int bx = 0, bz = 0;
        for (int i = 0; i < length; i++) {
            bx = (int) Math.round(x + 0.5) - 1;
            bz = (int) Math.round(z + 0.5) - 1;
            if (bx == xCoord && bz == zCoord) {
                return true;
            }
            int id = worldObj.getBlock(bx, yCoord, bz);
            Block b = id;
            boolean air_like = false;
            if (b == null) {
                air_like = true;
            } else {
                air_like = b.isAir(worldObj, bx, yCoord, bz);
                air_like |= b.getCollisionBoundingBoxFromPool(worldObj, bx, yCoord, bz) == null;
            }
            if (!air_like) {
                return false;
            }
            x -= dx;
            z -= dz;
        }
        return false;
    }

    @Override
    public boolean isBlockSolidOnSide(int side) {
        return false;
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(ForgeDirection dir) {
        return BlockIcons.mirror_front;
    }
    
    @Override
    public ItemStack getDroppedBlock() {
        return new ItemStack(Core.registry.mirror);
    }
}
