package factorization.common;

import java.io.DataInput;
import java.io.IOException;

import net.minecraft.src.Block;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.ItemStack;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.Packet;
import factorization.api.Coord;
import factorization.api.DeltaCoord;
import factorization.api.IReflectionTarget;
import factorization.common.NetworkFactorization.MessageType;

public class TileEntityMirror extends TileEntityCommon {
    Coord reflection_target = null;

    //don't save
    public int rotation = 0;
    public boolean is_lit = false;
    int search_delay = 0;
    int trace_check = 0;
    //don't save, but *do* share w/ client
    int target_rotation = 0;


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
            rotation = target_rotation;
        }
        else {
            reflection_target = null;
        }
    }
    
    @Override
    public void neighborChanged() {
        if (Core.instance.isCannonical(worldObj)) {
            search_delay = trace_check = 1;
        }
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
        boolean raining = worldObj.isRaining() && worldObj.getBiomeGenForCoords(xCoord, yCoord).rainfall > 0;
        return getCoord().canSeeSky() && worldObj.isDaytime() && !raining;
    }
    
    void setRotation(int rotation) {
        if (this.rotation != rotation) {
            this.rotation = rotation;
        }
    }

    int last_shared = -1;

    void broadcastTargetInfo() {
        if (getTargetInfo() != last_shared) {
            broadcastMessage(null, MessageType.MirrorTargetRotation, getTargetInfo());
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
    public Packet getDescriptionPacket() {
        return super.getDescriptionPacketWith(MessageType.MirrorDescription, rotation, getTargetInfo());
    }

    @Override
    public boolean handleMessageFromServer(int messageType, DataInput input) throws IOException {
        if (super.handleMessageFromServer(messageType, input)) {
            return true;
        }
        switch (messageType) {
        case MessageType.MirrorDescription:
            rotation = input.readInt();
            //FALL THROUGH
        case MessageType.MirrorTargetRotation:
            target_rotation = input.readInt();
            getCoord().dirty();
            return true;
        }
        return false;
    }

    @Override
    void onPlacedBy(EntityPlayer player, ItemStack is, int side) {
        if (player == null) {
            return;
        }
        setRotation(clipAngle(-clipAngle((int) player.rotationYaw + 270)));
        //		if (Core.instance.isCannonical(worldObj)) {
        //			broadcastMessage(null, getDescriptionPacket());
        //		}
    }

    @Override
    void onRemove() {
        if (reflection_target == null) {
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
    public void updateEntity() {
        //		if we don't have a target, spin about
        boolean cannon = Core.instance.isCannonical(worldObj);
        if (!cannon) {
            if (target_rotation == -99) {
                rotation++;
            } else if (target_rotation != rotation) {
                int dist = target_rotation - rotation;
                if (dist > 180 || (dist < 0 && dist > -180)) {
                    rotation--;
                } else {
                    rotation++;
                }
                rotation = clipAngle(rotation);
            }
            return;
        }
        broadcastTargetInfo();
        if (reflection_target == null) {
            rotation++;
            if (search_delay > 0) {
                search_delay--;
                return;
            }
            findTarget();
            if (reflection_target == null) {
                search_delay = 60;
                return;
            }
            trace_check = 20 * 15;
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
        rotation = clipAngle(rotation);
        if (rotation != target_rotation) {
            int dist = target_rotation - rotation;
            if (dist > 180 || (dist < 0 && dist > -180)) {
                rotation--;
            } else {
                rotation++;
            }
            rotation = clipAngle(rotation);
            if (rotation == target_rotation) {
                trace_check = 1;
            }
        }
        if (trace_check == 0) {
            trace_check = 20 * 30 + rand.nextInt(20);
            if (!myTrace(reflection_target.x, reflection_target.z)) {
                if (is_lit) {
                    is_lit = false;
                    target.addReflector(-getPower());
                    reflection_target = null;
                    setRotationTarget(-99);
                    return;
                }
            }
        } else {
            trace_check--;
        }

        if (hasSun() != is_lit && rotation == target_rotation) {
            is_lit = hasSun();
            target.addReflector(is_lit ? getPower() : -getPower());
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

        int search_distance = 8;
        IReflectionTarget closest = null;
        int last_dist = Integer.MAX_VALUE;
        Coord me = getCoord();
        for (int x = xCoord - search_distance; x < xCoord + search_distance; x++) {
            for (int z = zCoord - search_distance; z < zCoord + search_distance; z++) {
                Coord here = new Coord(worldObj, x, yCoord, z);
                IReflectionTarget target = here.getTE(IReflectionTarget.class);
                if (target == null) {
                    continue;
                }
                if (!myTrace(x, z)) {
                    continue;
                }
                int new_dist = me.distanceSq(here);
                if (new_dist < last_dist) {
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

    //XXX NOTE: Stolen from TileEntityWrathLamp. Ah hah hah hah...
    float div(int a, int b) {
        if (b == 0) {
            return Math.signum(a) * 0xFFF;
        }
        return a / b;
    }

    boolean myTrace(int x, int z) {
        int dx = x - xCoord, dz = z - zCoord;
        float idealm = div(dz, dx);

        float old_dist = Float.MAX_VALUE;
        boolean first = true;
        while (true) {
            if (x == xCoord && z == zCoord) {
                return true;
            }
            int id = worldObj.getBlockId(x, yCoord, z);
            Block b = Block.blocksList[id];
            if (b != null && !first) {
                if (!b.isAirBlock(worldObj, x, yCoord, z) || b.isOpaqueCube() || Block.lightOpacity[id] != 0) {
                    return false;
                }
            }
            first = false;
            dx = x - xCoord;
            dz = z - zCoord;
            float m = div(dz, dx);
            int addx = (int) -Math.signum(dx), addz = (int) -Math.signum(dz);
            if (addx == 0 && addz == 0) {
                return true;
            }
            if (addx == 0) {
                z += addz;
                continue;
            }
            if (addz == 0) {
                x += addx;
                continue;
            }
            float m_x = div(dz, dx + addx);
            float m_z = div(dz + addz, dx);
            if (Math.abs(idealm - m_x) <= Math.abs(idealm - m_z)) {
                x += addx;
            }
            else {
                z += addz;
            }
        }

    }

    @Override
    public boolean isBlockSolidOnSide(int side) {
        return false;
    }
}
