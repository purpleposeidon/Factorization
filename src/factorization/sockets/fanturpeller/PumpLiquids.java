package factorization.sockets.fanturpeller;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.HashSet;
import java.util.PriorityQueue;

import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeDirection;
import net.minecraftforge.fluids.BlockFluidFinite;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidContainerRegistry;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidBlock;
import net.minecraftforge.fluids.IFluidHandler;
import factorization.api.Coord;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.common.FactoryType;
import factorization.sockets.ISocketHolder;

public class PumpLiquids extends SocketFanturpeller implements IFluidHandler {
    private static final int BUCKET = FluidContainerRegistry.BUCKET_VOLUME;
    private interface PumpAction {
        void suckIn();
        FluidStack drainBlock(PumpCoord probe, boolean doDrain);
        void pumpOut();
    }
    
    static final class PumpCoord {
        final int x, y, z;
        final PumpCoord parent;
        final short pathDistance;
        PumpCoord(Coord at, PumpCoord parent, int pathDistance) {
            x = at.x;
            y = at.y;
            z = at.z;
            this.pathDistance = (short) pathDistance;
            this.parent = parent;
        }
        
        PumpCoord(PumpCoord parent, ForgeDirection d) {
            this.x = parent.x + d.offsetX;
            this.y = parent.y + d.offsetY;
            this.z = parent.z + d.offsetZ;
            this.pathDistance = (short) (parent.pathDistance + 1);
            this.parent = parent;
        }
        
        
        
        @Override
        public boolean equals(Object obj) {
            if (obj instanceof PumpCoord) {
                PumpCoord o = (PumpCoord) obj;
                return o.x == x && o.y == y && o.z == z;
            }
            return false;
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
    
    final static int max_pool = (16*16*24)*12*12;
    
    private class Drainer implements PumpAction {
        // o <-- o <-- o <-- o <-- o
        final ArrayDeque<PumpCoord> frontier = new ArrayDeque();
        final HashSet<PumpCoord> visited = new HashSet();
        final PriorityQueue<PumpCoord> queue = new PriorityQueue<PumpCoord>(128, getComparator());
        
        Comparator<PumpCoord> getComparator() {
            return new Comparator<PumpCoord>() {
                @Override
                public int compare(PumpCoord a, PumpCoord b) {
                    // If we're draining, we want the furthest & highest liquid
                    if (a.pathDistance == b.pathDistance) {
                        return b.y - a.y;
                    }
                    return b.pathDistance - a.pathDistance;
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
            System.out.println("RESET"); //NORELEASE
            visited.clear();
            queue.clear();
            frontier.clear();
            frontier.add(new PumpCoord(start, null, 0));
            delay = 20*3;
        }
        
        @Override
        public FluidStack drainBlock(PumpCoord probe, boolean doDrain) {
            Block b = Block.blocksList[worldObj.getBlockId(probe.x, probe.y, probe.z)];
            if (!(b instanceof IFluidBlock)) {
                Fluid vanilla;
                if (b == Block.waterStill || b == Block.waterMoving) {
                    vanilla = FluidRegistry.WATER;
                } else if (b == Block.lavaStill || b == Block.lavaMoving) {
                    vanilla = FluidRegistry.LAVA;
                } else {
                    return null;
                }
                if (worldObj.getBlockMetadata(probe.x, probe.y, probe.z) != 0) {
                    return null;
                }
                if (doDrain) {
                    worldObj.setBlockToAir(probe.x, probe.y, probe.z);
                }
                return new FluidStack(vanilla, BUCKET);
            }
            IFluidBlock block = (IFluidBlock) b;
            if (!block.canDrain(worldObj, probe.x, probe.y, probe.z)) return null;
            FluidStack fs = block.drain(worldObj, probe.x, probe.y, probe.z, false);
            if (fs == null) return null;
            if (fs.getFluid() != targetFluid) return null;
            if (doDrain) {
                fs = block.drain(worldObj, probe.x, probe.y, probe.z, true);
            }
            if (fs == null || fs.amount <= 0) return null;
            return fs;
        }
        
        boolean updateFrontier() {
            if (visited.size() > max_pool) return false;
            if (frontier.isEmpty()) return false;
            Coord probe = new Coord(worldObj, 0, 0, 0);
            System.out.println("updateFrontier: " + frontier.size()); //NORELEASE
            int NORELEASE = 0;
            for (int amount = frontier.size(); amount > 0; amount--) {
                PumpCoord pc = frontier.poll();
                if (pc == null) return true;
                boolean orig_is_liquid = drainBlock(pc, false) != null;
                if (!orig_is_liquid) {
                    continue; //...oops!
                }
                for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
                    if (!isSucking && pc.y + dir.offsetY >= maxHeight) continue;
                    PumpCoord at = new PumpCoord(pc, dir);
                    probe.set(worldObj, at.x, at.y, at.z);
                    if (visited.contains(at) || !probe.blockExists()) continue;
                    boolean is_liquid = drainBlock(at, false) != null;
                    boolean replaceable = probe.isReplacable();
                    if (!is_liquid && !replaceable) continue;
                    if (!is_liquid && replaceable && isLiquid(probe)) continue; // It's a different liquid.
                    visited.add(at); // Don't revisit this place.
                    NORELEASE++;
                    found(replaceable, is_liquid, at);
                }
            }
            System.out.println("NORELEASE: Found " + NORELEASE); //NORELEASE
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
            if (buffer.getFluidAmount() > 0) return;
            if (updateFrontier()) return;
            if (delay > 0) {
                delay--;
                return;
            }
            PumpCoord pc = queue.poll();
            if (pc == null || !pc.verifyConnection(this, worldObj)) {
                reset();
                return;
            }
            buffer.setFluid(drainBlock(pc, true));
        }

        @Override
        public void pumpOut() { }
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
                    System.out.println("cm " + a.pathDistance + ":" + a.y + ", " + b.pathDistance + ":" + b.y);
                    //return (a.pathDistance + a.y * 1024) - (b.pathDistance + b.y * 1024);
                    //return b.y - a.y;
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
            if (buffer.getFluidAmount() < BUCKET) return;
            FluidStack fs = buffer.getFluid();
            if (fs == null) return;
            Fluid fluid = fs.getFluid();
            if (!fluid.canBePlacedInWorld()) {
                destinationAction = new TankPumper(); // This is okay?
                return;
            }
            if (delay > 0) {
                delay--;
                return;
            }
            if (updateFrontier()) return;
            System.out.println("Frontier: " + frontier.size()); //NORELEASE
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
            at.set(worldObj, pc.x, pc.y, pc.z);
            if (!at.isReplacable()) return false;
            Block block = Block.blocksList[fluid.getBlockID()];
            if (block == null) return false;
            if (drainBlock(pc, false) != null) return false;
            at.setId(block);
            if (block instanceof BlockFluidFinite) {
                at.setMd(0xF); //Mmm.
            }
            buffer.setFluid(null);
            return true;
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
            Coord at = new Coord(PumpLiquids.this);
            at.adjust(destinationDirection);
            IFluidHandler te = at.getTE(IFluidHandler.class);
            if (te == null) {
                return; //Really, it shoudln't happen.
            }
            int origSize = buffer.getFluidAmount();
            FluidStack offering = buffer.getFluid().copy();
            offering.amount = Math.min(10, offering.amount);
            int usage = te.fill(sourceDirection, offering, true);
            buffer.drain(usage, true);
        }

        @Override
        public FluidStack drainBlock(PumpCoord probe, boolean doDrain) { return null; }
        
    }

    FluidTank buffer = new FluidTank(BUCKET);
    transient PumpAction sourceAction, destinationAction;
    transient float maxRangeSq = 0;
    transient float maxHeight = 0;
    transient ForgeDirection sourceDirection, destinationDirection;
    
    @Override
    public FactoryType getFactoryType() {
        return FactoryType.SOCKET_PUMP;
    }
    
    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        super.serialize(prefix, data);
        if (data.isNBT()) { // Lame, but only just this once.
            if (data.isReader()) {
                buffer.readFromNBT(data.getTag());
            } else {
                buffer.writeToNBT(data.getTag());
            }
        }
        return this;
    }
    
    @Override
    protected void fanturpellerUpdate(ISocketHolder socket, Coord coord, boolean powered, boolean neighbor_changed) {
        if (worldObj.isRemote){
            return;
        }
        maxRangeSq = (getTargetSpeed()*16);
        maxRangeSq *= maxRangeSq;
        maxHeight = yCoord + 1 + getTargetSpeed()/10*3;
        if (isSucking) {
            sourceDirection = facing;
            destinationDirection = facing.getOpposite();
        } else {
            sourceDirection = facing.getOpposite();
            destinationDirection = facing;
        }
        if (neighbor_changed) {
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
        if (!shouldDoWork()) {
            updateDestination(coord);
        } else {
            updateSource(coord);
            updateDestination(coord);
        }
    }

    void updateSource(Coord coord) {
        if (sourceAction == null) {
            coord.adjust(facing.getOpposite());
            if (hasTank(coord)) {
                sourceAction = new TankPumper();
            } else if (isLiquid(coord)) {
                final Coord c = new Coord(this).add(sourceDirection);
                sourceAction = new Drainer(c, c.getFluid());
            }
            coord.adjust(facing);
        } else {
            sourceAction.suckIn();
        }
    }

    void updateDestination(Coord coord) {
        if (destinationAction == null) {
            coord.adjust(facing);
            if (hasTank(coord)) {
                destinationAction = new TankPumper();
            } else if (isLiquid(coord) || coord.isReplacable()) {
                final Coord c = new Coord(this).add(destinationDirection);
                destinationAction = new Flooder(c, c.getFluid());
            }
            coord.adjust(facing.getOpposite());
        } else {
            destinationAction.pumpOut();
        }
    }
    
    @Override
    protected boolean isSafeToDiscard() {
        return buffer.getFluidAmount() == 0;
    }
    
    String easyName(Object obj) {
        if (obj == null) return "none";
        return obj.getClass().getSimpleName();
    }
    
    @Override
    public String getInfo() {
        FluidStack fs = buffer.getFluid();
        String fluid;
        if (fs == null) {
            fluid = "empty";
        } else {
            fluid = fs.amount + "mB of " + fs.getFluid().getName();
        }
        return fluid + 
                "\nSucking: " + isSucking + 
                "\nSource action: " + easyName(sourceAction) + 
                "\nDestination action: " + easyName(destinationAction);
    }
    
    @Override
    protected boolean shouldFeedJuice() {
        return sourceAction != null && destinationAction != null;
    }

    @Override
    public int fill(ForgeDirection from, FluidStack resource, boolean doFill) {
        if (from != sourceDirection) return 0;
        return buffer.fill(resource, doFill);
    }

    @Override
    public FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain) {
        return null;
    }

    @Override
    public FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain) {
        return null;
    }

    @Override
    public boolean canFill(ForgeDirection from, Fluid fluid) {
        return from == sourceDirection;
    }

    @Override
    public boolean canDrain(ForgeDirection from, Fluid fluid) {
        return false;
    }

    static private FluidTankInfo[] no_info = new FluidTankInfo[0];
    private FluidTankInfo[] tank_info = new FluidTankInfo[] { new FluidTankInfo(buffer) };
    @Override
    public FluidTankInfo[] getTankInfo(ForgeDirection from) {
        if (from == sourceDirection) return tank_info;
        return no_info;
    }
}
