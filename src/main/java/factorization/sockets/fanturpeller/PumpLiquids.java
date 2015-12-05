package factorization.sockets.fanturpeller;

import factorization.api.Coord;
import factorization.api.FzOrientation;
import factorization.api.Quaternion;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.api.datahelpers.Share;
import factorization.common.FactoryType;
import factorization.servo.ServoMotor;
import factorization.shared.Core;
import factorization.sockets.ISocketHolder;
import factorization.util.FluidUtil;
import factorization.util.InvUtil;
import factorization.util.ItemUtil;
import factorization.util.NumUtil;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemBucket;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraftforge.fluids.*;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.HashSet;
import java.util.PriorityQueue;

import static org.lwjgl.opengl.GL11.*;

public class PumpLiquids extends SocketFanturpeller implements IFluidHandler {
    protected static final int BUCKET = FluidContainerRegistry.BUCKET_VOLUME;
    protected FluidTank buffer = new FluidTank(BUCKET);
    protected FluidTank auxBuffer = new FluidTank(BUCKET);
    protected boolean isFloodingTank = false;
    private static FluidTankInfo[] no_info = new FluidTankInfo[0];
    private int available_pumping_activity = 0;
    
    {
        super.isSucking = false;
    }

    public static final int CHARGE_DEPLETION = 10;

    boolean depleteCharge(boolean simulate, int fluidAmount) {
        if (simulate) {
            if (fluidAmount < available_pumping_activity) return true;
            return charge.getValue() > CHARGE_DEPLETION;
        }
        available_pumping_activity -= fluidAmount;
        if (available_pumping_activity > 0) return true;
        available_pumping_activity += BUCKET;
        return charge.tryTake(CHARGE_DEPLETION) >= CHARGE_DEPLETION;
    }

    @Override
    public int fill(EnumFacing from, FluidStack resource, boolean doFill) {
        if (from != facing.getOpposite()) return 0;
        return buffer.fill(resource, doFill);
    }

    @Override
    public FluidStack drain(EnumFacing from, FluidStack resource, boolean doDrain) {
        return null;
    }

    @Override
    public FluidStack drain(EnumFacing from, int maxDrain, boolean doDrain) {
        return null;
    }

    @Override
    public boolean canFill(EnumFacing from, Fluid fluid) {
        return from == facing.getOpposite();
    }

    @Override
    public boolean canDrain(EnumFacing from, Fluid fluid) {
        return false;
    }
    
    @Override
    public FluidTankInfo[] getTankInfo(EnumFacing from) {
        if (from == facing.getOpposite()) {
            return new FluidTankInfo[] { new FluidTankInfo(buffer), new FluidTankInfo(auxBuffer) };
        }
        return no_info;
    }
    
    
    
    
    private interface PumpAction {
        void suckIn();
        FluidStack drainBlock(PumpCoord probe, boolean doDrain);
        void pumpOut();
    }
    
    static final class PumpCoord {
        final int pos;
        final short pathDistance;
        final PumpCoord parent;
        PumpCoord(Coord at, PumpCoord parent, int pathDistance) {
            x = at.x;
            y = at.y;
            z = at.z;
            this.pathDistance = (short) pathDistance;
            this.parent = parent;
        }
        
        PumpCoord(PumpCoord parent, EnumFacing d) {
            this.x = parent.x + d.getDirectionVec().getX();
            this.y = parent.y + d.getDirectionVec().getY();
            this.z = parent.z + d.getDirectionVec().getZ();
            this.pathDistance = (short) (parent.pathDistance + 1);
            this.parent = parent;
        }
        
        
        
        @Override
        public boolean equals(Object obj) {
            // No instanceof for efficiency. Probably safe & worthwhile.
            PumpCoord o = (PumpCoord) obj;
            return o.x == x && o.y == y && o.z == z;
        }
        
        @Override
        public int hashCode() {
            return (((x * 11) % 71) << 7) + ((z * 7) % 479) + y; //TODO: This hashcode is probably terrible.
        }
        
        boolean verifyConnection(PumpAction pump, World w) {
            PumpCoord here = parent;
            while (here != null) {
                if (pump.drainBlock(here, false) == null) return false;
                here = here.parent;
            }
            return true;
        }
    }
    
    static final class FoundFluidHandler {
        final IFluidHandler te;
        final EnumFacing dir;
        public FoundFluidHandler(IFluidHandler te, EnumFacing dir) {
            this.te = te;
            this.dir = dir.getOpposite();
        }
    }
    
    final static int max_pool = (16*16*24)*12*12;
    
    private class Drainer implements PumpAction {
        // o <-- o <-- o <-- o <-- o
        final ArrayDeque<PumpCoord> frontier = new ArrayDeque();
        final HashSet<PumpCoord> visited = new HashSet();
        final PriorityQueue<PumpCoord> queue = new PriorityQueue<PumpCoord>(128, getComparator());
        final ArrayDeque<FoundFluidHandler> foundContainers = new ArrayDeque();
        /*
         * Eh. Memory inefficient.
         * Could we switch to packed arrays?
         */
        
        Comparator<PumpCoord> getComparator() {
            return new Comparator<PumpCoord>() {
                @Override
                public int compare(PumpCoord a, PumpCoord b) {
                    // If we're draining, we want the furthest & highest liquid
                    if (a.y == b.y) { 
                        return b.pathDistance - a.pathDistance;
                    }
                    return b.y - a.y;
                }
            };
        }
        
        final Coord start;
        final Fluid targetFluid;
        
        int delay; // we wait this long before reconstruction
        
        Drainer(Coord start, Fluid targetFluid) {
            this.start = start;
            this.targetFluid = targetFluid;
            reset();
        }
        
        void reset() {
            visited.clear();
            queue.clear();
            frontier.clear();
            PumpCoord seed = new PumpCoord(start, null, 0);
            frontier.add(seed);
            visited.add(seed);
            queue.add(seed);
            delay = 20*3;
            foundContainers.clear();
            if (buffer.getFluidAmount() > 0 && buffer.getFluidAmount() < buffer.getCapacity()) {
                FluidUtil.transfer(auxBuffer, buffer);
            }
        }
        
        @Override
        public FluidStack drainBlock(PumpCoord probe, boolean doDrain) {
            return FluidUtil.drainSpecificBlockFluid(worldObj, probe.x, probe.y, probe.z, doDrain, targetFluid);
        }
        
        FluidStack probeAbove(PumpCoord probe) {
            return FluidUtil.drainSpecificBlockFluid(worldObj, probe.x, probe.y + 1, probe.z, false, targetFluid);
        }
        
        boolean updateFrontier() {
            if (visited.size() > max_pool) return false;
            if (frontier.isEmpty()) return false;
            Coord probe = new Coord(worldObj, 0, 0, 0);
            int maxHeight = getMaxHeight();
            int maxDistance = getMaxDistance();
            for (int amount = Math.max(frontier.size(), 1024); amount > 0; amount--) {
                PumpCoord pc = frontier.poll();
                if (pc == null) return true;
                if (pc.y >= maxHeight) continue;
                if (pc.pathDistance >= maxDistance) continue;
                boolean orig_is_liquid = drainBlock(pc, false) != null;
                if (!orig_is_liquid) {
                    continue; //...oops!
                }
                for (EnumFacing dir : EnumFacing.VALUES) {
                    if (!isSucking && pc.y + dir.getDirectionVec().getY() >= maxHeight) continue;
                    PumpCoord at = new PumpCoord(pc, dir);
                    probe.set(worldObj, at.x, at.y, at.z);
                    if (visited.contains(at) || !probe.blockExists()) continue;
                    boolean is_liquid = drainBlock(at, false) != null;
                    boolean replaceable = probe.isReplacable();
                    if (!is_liquid && !replaceable) {
                        IFluidHandler ifh = probe.getTE(IFluidHandler.class);
                        if (ifh != null && ifh != PumpLiquids.this) {
                            foundContainers.add(new FoundFluidHandler(ifh, dir));
                        }
                        continue;
                    }
                    if (!is_liquid && replaceable && isLiquid(probe)) continue; // It's a different liquid.
                    visited.add(at); // Don't revisit this place.
                    found(replaceable, is_liquid, at);
                }
            }
            return true;
        }
        
        protected void found(boolean replaceable, boolean is_liquid, PumpCoord at) {
            if (is_liquid) {
                queue.add(at); // We can continue iteration here
                frontier.add(at); // It can be sucked up
            }
        }

        @Override
        public void suckIn() {
            if (isSucking) return; //don't run backwards
            if (auxBuffer.getFluidAmount() > 0) return;
            if (updateFrontier()) return;
            if (delay > 0) {
                delay--;
                return;
            }
            delay = 10;
            PumpCoord pc = queue.poll();
            if (pc == null) {
                FoundFluidHandler foundIfh = foundContainers.poll();
                if (foundIfh == null) {
                    reset();
                    return;
                }
                FluidStack resource = new FluidStack(targetFluid, auxBuffer.getCapacity() - auxBuffer.getFluidAmount());
                FluidStack gotten = foundIfh.te.drain(foundIfh.dir, resource, true);
                auxBuffer.fill(gotten, true);
                return;
            }
            if (!pc.verifyConnection(this, worldObj)) {
                reset();
                return;
            }
            if (probeAbove(pc) != null) {
                // Fluid (likely water) has refilled in above us
                reset();
            } else {
                auxBuffer.setFluid(drainBlock(pc, true));
            }
        }

        @Override
        public void pumpOut() { }
        
        int getMaxHeight() {
            return worldObj.getHeight();
        }
        
        int getMaxDistance() {
            return 81;
        }
    }
    
    private class Flooder extends Drainer {
        Flooder(Coord start, Fluid targetFluid) {
            super(start, targetFluid);
        }
        
        Comparator<PumpCoord> getComparator() {
            return new Comparator<PumpCoord>() {
                @Override
                public int compare(PumpCoord a, PumpCoord b) {
                    // If we're flooding, we want the furthest & lowest liquid
                    if (a.y == b.y) {
                        if (a.pathDistance == b.pathDistance) {
                            return 0;
                        } else if (a.pathDistance > b.pathDistance) {
                            return 1;
                        } else {
                            return -1;
                        }
                    } else if (a.y > b.y) {
                        return 1;
                    } else {
                        return -1;
                    }
                }
            };
        }
        
        @Override
        void reset() {
            super.reset();
            queue.add(frontier.peek());
            // We force the block in front to be valid because we absolutely need to fill it up.
            // We don't need to worry about someone changing it to stone if we check it during normal processing.
        }
        
        @Override
        protected void found(boolean replaceable, boolean is_liquid, PumpCoord at) {
            if (replaceable && !is_liquid) {
                queue.add(at);
            } else if (is_liquid) {
                frontier.add(at);
            }
        }
        
        @Override
        public void suckIn() { }
        
        @Override
        public void pumpOut() {
            if (isSucking) return; //don't run backwards
            if (!depleteCharge(false, buffer.getFluidAmount())) return;
            if (!foundContainers.isEmpty() && buffer.getFluidAmount() > 0) {
                FoundFluidHandler foundIfh = foundContainers.poll();
                FluidTank buff = auxBuffer.getFluidAmount() > 0 ? auxBuffer : buffer;
                FluidStack work = buff.getFluid().copy();
                if (work.amount > 25) {
                    work.amount = 25;
                }
                int amount = foundIfh.te.fill(foundIfh.dir, work, true);
                buff.drain(amount, true);
                if (buffer /* NOT buff; we could be using auxBuff*/.getFluidAmount() <= 0) {
                    reset();
                } else {
                    if (amount > 0) {
                        foundContainers.add(foundIfh);
                    }
                    delay = 0;
                }
                isFloodingTank = (buffer /* also could be using auxBuff? */.getFluidAmount() % 1000) != 0;
                return;
            }
            if (buffer.getFluidAmount() < BUCKET) {
                if (isFloodingTank) {
                    if (!updateFrontier()) {
                        reset();
                    }
                }
                return;
            }
            FluidStack fs = buffer.getFluid();
            if (fs == null) return;
            Fluid fluid = fs.getFluid();
            if (fluid == null) return; // Uhm, what?
            if (!fluid.canBePlacedInWorld()) {
                destinationAction = new TankPumper(); // This is okay?
                return;
            }
            if (delay > 0) {
                delay--;
                return;
            }
            delay = 10;
            if (updateFrontier()) return;
            for (int i = 0; i < 16; i++) {
                PumpCoord pc = queue.poll();
                if (pc == null || !pc.verifyConnection(this, worldObj)) {
                    reset();
                    return;
                }
                if (placeFluid(pc, fluid)) {
                    //Have we opened up new frontiers? (Hint: probably)
                    frontier.add(pc);
                    break;
                }
            }
        }
        
        private Coord at = new Coord(worldObj, 0, 0, 0);
        boolean placeFluid(PumpCoord pc, Fluid fluid) {
            at.w = worldObj;
            at.x = pc.x;
            at.y = pc.y;
            at.z = pc.z;
            if (!at.isReplacable()) return false;
            Block block = fluid.getBlock();
            if (block == null) return false;
            if (drainBlock(pc, false) != null) return false;
            if (block == Blocks.water) block = Blocks.flowing_water;
            else if (block == Blocks.lava) block = Blocks.flowing_lava;
            
            if (block == Blocks.flowing_water) {
                ((ItemBucket) Items.water_bucket).tryPlaceContainedLiquid(at.w, at.x, at.y, at.z);
            } else {
                at.setIdMd(block, block instanceof BlockFluidFinite ? 0xF : 0, true);
            }
            buffer.setFluid(null);
            at.notifyBlockChange();
            return true;
        }
        
        @Override
        int getMaxHeight() {
            return pos.getY() + 12;
        }
    }
    
    private class TankPumper implements PumpAction {
        @Override
        public void suckIn() {
            if (buffer.getFluidAmount() >= BUCKET) return;
            Coord at = new Coord(PumpLiquids.this);
            at.adjust(sourceDirection);
            IFluidHandler te = at.getTE(IFluidHandler.class);
            if (te == null) {
                return; //Shouldn't happen?
            }
            FluidStack rep = null;
            if (buffer.getFluidAmount() > 0) {
                rep = buffer.getFluid().copy();
                rep.amount = Math.min(50, BUCKET - buffer.getFluidAmount());
                buffer.fill(te.drain(destinationDirection, rep, true), true);
            } else {
                FluidTankInfo[] tanks = te.getTankInfo(destinationDirection);
                if (tanks == null || tanks.length == 0) return;
                for (FluidTankInfo tank : tanks) {
                    if (tank.fluid == null) continue;
                    FluidStack fs = te.drain(destinationDirection, BUCKET, true);
                    if (fs != null) {
                        buffer.setFluid(fs);
                        break;
                    }
                }
            }
        }

        @Override
        public void pumpOut() {
            if (buffer.getFluidAmount() <= 0) return;
            if (!depleteCharge(false, buffer.getFluidAmount())) return;
            Coord at = new Coord(PumpLiquids.this);
            at.adjust(destinationDirection);
            IFluidHandler te = at.getTE(IFluidHandler.class);
            if (te == null) {
                return; //Really, it shoudln't happen.
            }
            FluidStack offering = buffer.getFluid().copy();
            offering.amount = Math.min(10, offering.amount);
            int usage = te.fill(sourceDirection, offering, true);
            buffer.drain(usage, true);
        }

        @Override
        public FluidStack drainBlock(PumpCoord probe, boolean doDrain) { return null; }
        
    }

    transient PumpAction sourceAction, destinationAction;
    transient EnumFacing sourceDirection, destinationDirection;
    
    @Override
    public FactoryType getFactoryType() {
        return FactoryType.SOCKET_PUMP;
    }
    
    boolean dirty = true;
    
    @Override
    public void neighborChanged() {
        dirty = true;
    }
    
    @Override
    protected void fanturpellerUpdate(ISocketHolder socket, Coord coord, boolean powered) {
        if (worldObj.isRemote){
            return;
        }
        boolean onServo = socket != this;
        sourceDirection = facing.getOpposite();
        destinationDirection = facing;
        if (dirty) {
            dirty = false;
            coord.adjust(facing.getOpposite());
            if (sourceAction instanceof TankPumper ^ hasTank(coord)) {
                sourceAction = null;
            }
            if (sourceAction instanceof Drainer ^ isLiquid(coord)) {
                sourceAction = null;
            }
            coord.adjust(facing);
            coord.adjust(facing);
            if (destinationAction instanceof TankPumper ^ hasTank(coord)) {
                destinationAction = null;
            }
            if (destinationAction instanceof Flooder ^ isLiquid(coord)) {
                destinationAction = null;
            }
            coord.adjust(facing.getOpposite());
        }
        if (auxBuffer.getFluidAmount() > 0) {
            FluidUtil.transfer(buffer, auxBuffer);
        }
        if (!shouldDoWork()) {
            updateDestination(coord, onServo);
            if (sourceAction == null) {
                updateSource(coord, onServo);
            }
        } else {
            updateSource(coord, onServo);
            updateDestination(coord, onServo);
        }
    }

    void updateSource(Coord coord, boolean onServo) {
        if (sourceAction == null) {
            coord.adjust(facing.getOpposite());
            if (hasTank(coord)) {
                sourceAction = new TankPumper();
            } else if (!onServo && isLiquid(coord)) {
                final Coord c = new Coord(this).add(sourceDirection);
                sourceAction = new Drainer(c, c.getFluid());
            }
            coord.adjust(facing);
        } else if (shouldDoWork()) {
            sourceAction.suckIn();
        }
    }

    void updateDestination(Coord coord, boolean onServo) {
        if (destinationAction == null) {
            coord.adjust(facing);
            if (hasTank(coord)) {
                destinationAction = new TankPumper();
            } else if (onServo) {
                // Wrapping that line below in parens'd suck.
            } else if ((isLiquid(coord) || coord.isReplacable()) && buffer.getFluidAmount() > 0) {
                final Coord c = new Coord(this).add(destinationDirection);
                destinationAction = new Flooder(c, buffer.getFluid().getFluid());
            }
            coord.adjust(facing.getOpposite());
        } else if (shouldDoWork()) {
            destinationAction.pumpOut();
        }
    }
    
    @Override
    protected boolean isSafeToDiscard() {
        return buffer.getFluidAmount() == 0;
    }
    
    String easyName(Object obj) {
        if (obj == null) return "None";
        return obj.getClass().getSimpleName();
    }

    String nameTank(FluidTank buff) {
        FluidStack fs = buff.getFluid();
        if (fs == null || fs.amount == 0) return "";
        String unit;
        if (fs.amount % BUCKET == 0) {
            int n = (fs.amount / BUCKET);
            String bucket = n == 1 ? "bucket" : "buckets"; // >_>
            unit = n + " " + bucket + " of ";
        } else {
            unit = fs.amount + "mb of ";
        }
        return "\n" + unit + fs.getFluid().getName();
    }
    
    @Override
    public String getInfo() {
        FluidStack fs = buffer.getFluid();
        String fluid = nameTank(buffer) + nameTank(auxBuffer);
        float targetSpeed = getTargetSpeed();
        String speed = "";
        if (Math.abs(targetSpeed) > 1) {
            speed = "\nSpeed: " + (int)(100*fanω/targetSpeed) + "%";
        }
        return easyName(sourceAction) + 
                " -> " + easyName(destinationAction) +
                fluid + speed;
    }
    
    @Override
    protected boolean shouldFeedJuice() {
        return sourceAction != null || destinationAction != null;
    }

    @Override
    int getRequiredCharge() {
        return 2;
    }
    
    @SideOnly(Side.CLIENT)
    private static ObjectModel corkscrew;
    
    @Override
    @SideOnly(Side.CLIENT)
    public void representYoSelf() {
        super.representYoSelf();
        corkscrew = new ObjectModel(Core.getResource("models/corkscrew.obj"));
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public void renderTesr(ServoMotor motor, float partial) {
        float d = 0.5F;
        GL11.glTranslatef(d, d, d);
        Quaternion.fromOrientation(FzOrientation.fromDirection(facing.getOpposite())).glRotate();
        float turn = scaleRotation(NumUtil.interp(prevFanRotation, fanRotation, partial));
        GL11.glRotatef(turn, 0, 1, 0);
        float sd = motor == null ? -2F/16F : 3F/16F;
        sd += -7F/16F;
        GL11.glTranslatef(0, sd, 0);
        
        
        float s = 12F/16F;
        if (motor != null) {
            s = 10F/16F;
            GL11.glTranslatef(0, -3F/16F, 0);
        }
        GL11.glScalef(s, 1, s);
        
        TextureManager tex = Minecraft.getMinecraft().renderEngine;
        tex.bindTexture(Core.blockAtlas);
        glEnable(GL_LIGHTING);
        glDisable(GL11.GL_CULL_FACE);
        glEnable(GL12.GL_RESCALE_NORMAL);
        corkscrew.render(BlockIcons.socket$corkscrew);
        glEnable(GL11.GL_CULL_FACE);
        glEnable(GL_LIGHTING);
    }
    
    @Override
    public ItemStack getCreatingItem() {
        return new ItemStack(Core.registry.corkscrew);
    }
    
    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        super.serialize(prefix, data);
        data.as(Share.PRIVATE, "buff").putTank(buffer);
        data.as(Share.PRIVATE, "auxBuff").putTank(auxBuffer);
        isFloodingTank = data.as(Share.PRIVATE, "floodTank").putBoolean(isFloodingTank);
        available_pumping_activity = data.as(Share.PRIVATE, "pumpActivity").putInt(available_pumping_activity);
        
        target_speed = 2;
        isSucking = false;
        return this;
    }
    
    @Override
    public boolean activate(EntityPlayer player, EnumFacing side) {
        ItemStack is = ItemUtil.normalize(player.getHeldItem());
        if (is != null && is.getItem() instanceof IFluidContainerItem && buffer.getFluidAmount() > 0) {
            ItemStack use = is;
            boolean pushBack = false;
            if (use.stackSize > 1) {
                use = use.splitStack(1);
                pushBack = true;
            }
            IFluidContainerItem it = (IFluidContainerItem) use.getItem();
            int taken = it.fill(use, buffer.getFluid(), true);
            buffer.drain(taken, true);
            if (pushBack) {
                InvUtil.givePlayerItem(player, use);
            }
            return true;
        }
        return super.activate(player, side);
    }
}
