package factorization.common;

import java.io.DataInput;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.packet.Packet;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.ForgeDirection;
import factorization.api.Coord;
import factorization.api.DeltaCoord;
import factorization.common.Core.NotifyStyle;
import factorization.common.NetworkFactorization.MessageType;
import factorization.fzds.DeltaChunk;
import factorization.fzds.TransferLib;
import factorization.fzds.api.IDeltaChunk;

public class TileEntityRocketEngine extends TileEntityCommon {
    boolean inSlice = false;
    boolean isLeaderEngine = false;
    public boolean isFiring = false;
    int availableFuel = -1;
    int nonfuelMass = 0;
    
    public boolean lastValidationStatus = false;
    private boolean ignitionRequest = false;

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.ROCKETENGINE;
    }

    @Override
    public BlockClass getBlockClass() {
        return BlockClass.DarkIron;
    }
    
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setBoolean("inSlice", inSlice);
        tag.setBoolean("isLeaderEngine", isLeaderEngine);
        tag.setBoolean("isFiring", isFiring);
        tag.setInteger("availableFuel", availableFuel);
        tag.setInteger("nonfuelMass", nonfuelMass);
        tag.setBoolean("lastValidationStatus", lastValidationStatus);
    };
    
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        inSlice = tag.getBoolean("inSlice");
        isLeaderEngine = tag.getBoolean("isLeaderEngine");
        isFiring = tag.getBoolean("isFiring");
        availableFuel = tag.getInteger("availableFuel");
        nonfuelMass = tag.getInteger("nonfuelMass");
        lastValidationStatus = tag.getBoolean("lastValidationStatus");
    };
    
    List<Coord> getArea() {
        return getArea(getCoord(), new DeltaCoord(1, 1, 1) /* this is dependent on the behavior of Coord.isSubmissive; onPlacedBy determines the lowest coord */);
    }
    
    List<Coord> getArea(Coord c, DeltaCoord dc) {
        //2x3x2
        ArrayList<Coord> ret = new ArrayList<Coord>(2*3*2);
        for (int dyc = 0; dyc < 3; dyc++) {
            int dy = dyc*dc.y;
            for (int dxc = 0; dxc < 2; dxc++) {
                int dx = dxc*dc.x;
                for (int dzc = 0; dzc < 2; dzc++) {
                    int dz = dzc*dc.z;
                    ret.add(c.add(dx, dy, dz));
                }
            }
        }
        return ret;
    }
    
    DeltaCoord getCornerDirection(EntityPlayer player, int side) {
        ForgeDirection dir = ForgeDirection.getOrientation(side);
        DeltaCoord dc = FactorizationUtil.getFlatDiagonalFacing(player);
        if (dc.isZero()) {
            return null;
        }
        ForgeDirection fside = ForgeDirection.getOrientation(side);
        if (fside.offsetY == 0) {
            dc.x *= fside.offsetX != 0 ? -1 : 1;
            dc.z *= fside.offsetZ != 0 ? -1 : 1;
        }
        if (fside == ForgeDirection.DOWN) {
            dc.y = -1;
        } else {
            dc.y = 1;
        }
        for (int i = 0; i < 3; i++) {
            if (dc.get(i) == 0) {
                dc.set(i, 1);
            }
        }
        return dc;
    }
    
    @Override
    boolean canPlaceAgainst(EntityPlayer player, Coord c, int side) {
        if (player.worldObj.isRemote) {
            return false;
        }
        if (!c.isReplacable()) {
            c = c.towardSide(side);
        }
        DeltaCoord dc = getCornerDirection(player, side);
        if (dc == null) {
            Core.notify(player, c, "Place it differently");
            return false;
        }
        boolean fail = false;
        for (Coord spot : getArea(c, dc)) {
            if (!spot.isReplacable()) {
                if (fail == false) {
                    Core.clearNotifications(player);
                    fail = true;
                }
                if (!spot.equals(c)) {
                    Core.notify(player, spot, NotifyStyle.FORCE, "X");
                }
            }
        }
        if (fail) {
            Core.notify(player, c, NotifyStyle.FORCE, "Obstructed");
            return false;
        }
        AxisAlignedBB area = AxisAlignedBB.getBoundingBox(c.x, c.y, c.z, c.x, c.y, c.z);
        area = area.addCoord(2*dc.x, 3*dc.y, 2*dc.z);
        //double ao = 0.5;
        //area = area.offset(ao, ao, ao);
        for (Object o : c.w.getEntitiesWithinAABBExcludingEntity(null, area)) {
            Entity e = (Entity) o;
            if (e.canBeCollidedWith() || e instanceof EntityLiving || true) {
                Core.notify(player, c, NotifyStyle.FORCE, "Obstructed by entity");
                Coord ec = new Coord(e);
                if (!ec.equals(c)) {
                    String it = "(this guy)";
                    if (e instanceof EntityPlayer) {
                        it = "(this player)";
                    }
                    if (e instanceof EntityCreeper) {
                        it = "(thissss guy)";
                    }
                    Core.notify(player, new Coord(e), NotifyStyle.FORCE, it);
                }
                return false;
            }
        }
        return true;
    }
    
    @Override
    void onPlacedBy(EntityPlayer player, ItemStack is, int side) {
        DeltaCoord dc = getCornerDirection(player, side);
        List<Coord> area = getArea(getCoord(), dc);
        Coord myDestination = area.get(0);
        for (Coord c : area) {
            if (c.isSubmissiveTo(myDestination)) {
                myDestination = c;
            }
        }
        
        Coord here = getCoord();
        TileEntityRocketEngine base = this;
        if (!here.equals(myDestination)) {
            here.removeTE();
            base = new TileEntityRocketEngine();
            myDestination.setId(Core.factory_block_id);
            myDestination.setTE(base);
        }
        
        for (Coord spot : area) {
            if (!spot.equals(myDestination)) {
                spot.setId(Core.factory_block_id);
                TileEntityExtension tex = new TileEntityExtension(base);
                spot.setTE(tex);
                tex.getBlockClass().enforce(spot);
            }
            spot.redraw();
        }
        ignitionRequest = true;
    }
    
    @Override
    void onRemove() {
        Coord here = getCoord();
        for (int dx = -5; dx <= 5; dx++) {
            for (int dy = -5; dy <= 5; dy++) {
                for (int dz = -5; dz <= 5; dz++) {
                    Coord c = here.add(dx, dy, dz);
                    TileEntityExtension tex = c.getTE(TileEntityExtension.class);
                    if (tex == null) {
                        continue;
                    }
                    if (tex.getParent() == this) {
                        c.setId(0);
                    }
                }
            }
        }
        here.setId(0);
    }
    
    @Override
    public void setBlockBounds(Block b) {
        b.setBlockBounds(0, 0, 0, 2, 3, 2);
    }
    
    
    //Actual rocketry functions
    private static int[][] perimDeltas = new int[][] {
        {-1, 0, -1},
        {-1, 0, 0},
        {-1, 0, 1},
        {-1, 0, 2},
        {0, 0, -1},
        {0, 0, 2},
        {1, 0, -1},
        {1, 0, 2},
        {2, 0, -1},
        {2, 0, 0},
        {2, 0, 1},
        {2, 0, 2},
    }; /*
d = """ 
####
#..#
#..#
####""".strip().split()
for x in range(0, len(d[0])):
  for y in range(0, len(d)):
      if d[x][y] == '#':
            print("{%s, 0, %s}," % (x - 1, y - 1)) 
*/
    
    Coord[] getIgnitionArea() {
        Coord[] ret = new Coord[perimDeltas.length + 4];
        Coord here = getCoord();
        for (int i = 0; i < perimDeltas.length; i++) {
            ret[i] = here.add(perimDeltas[i][0], 0, perimDeltas[i][1]);
        }
        int i = 0;
        for (int dx = 0; dx <= 1; dx++) {
            for (int dz = 0; dz <= 1; dz++) {
                ret[perimDeltas.length + i] = here.add(dx, -1, dz);
                i++;
            }
        }
        return ret;
    }
    
    @Override
    public void neighborChanged() {
        ignitionRequest = true;
    }
    
    ContiguitySolver canIgnite(EntityPlayer player) {
        int fireCount = 0;
        for (Coord n : getIgnitionArea()) {
            fireCount += n.isBlockBurning() ? 1 : 0;
        }
        if (fireCount < 4) {
            return null;
        }
        ContiguitySolver solver = new ContiguitySolver(this);
        try {
            solver.solve();
        } catch (RocketValidationException e) {
            e.notify(this, player);
            for (TileEntityRocketEngine engine : solver.engines) {
                engine.setValid(false);
            }
            return null;
        }
        for (TileEntityRocketEngine engine : solver.engines) {
            engine.setValid(true);
            if (engine == this) {
                continue;
            }
            for (Coord n : engine.getIgnitionArea()) {
                fireCount += n.isBlockBurning() ? 1 : 0;
            }
        }
        double perfect = solver.engines.size()*12;
        double score = fireCount / perfect;
        if (score >= 0.5) {
            if (solver.entireRocket.size() == 0) {
                Core.notify(null, getCoord(), "No body?\nBug!");
            } else {
                return solver;
            }
        } else {
            Core.notify(null, getCoord(), "Nope!");
        }
        return null;
    }
    
    void ignite(ContiguitySolver solver) {
        Core.notify(null, getCoord(), "Ignition");
        isLeaderEngine = true;
        for (TileEntityRocketEngine engine : solver.engines) {
            engine.isFiring = true;
            engine.inSlice = true;
        }
        
        Coord min = choose(solver.entireRocket);
        Coord max = min;
        for (Coord c : solver.entireRocket) {
            if (c.isSubmissiveTo(min)) {
                min = c;
            }
            if (max.isSubmissiveTo(c)) {
                max = c;
            }
        }
        DeltaCoord size = max.difference(min);
        DeltaCoord half = size.scale(0.5);
        Coord center = min.add(half);
        IDeltaChunk dse = DeltaChunk.allocateSlice(worldObj, -1, new DeltaCoord(0, 0, 0));
        center.setAsEntityLocation(dse);
        dse.posX += 0.5;
        dse.posY -= 5;
        dse.posZ += 0.5;
        //TODO NORELEASE Use the functional method for doing this
        
        Vec3 real = Vec3.createVectorHelper(0, 0, 0);
        Coord dest = new Coord(DeltaChunk.getServerShadowWorld(), 0, 0, 0);
        for (TileEntityRocketEngine engine : solver.engines) {
            Coord c = engine.getCoord();
            solver.entireRocket.remove(c);
            c.setAsVector(real);
            dest.set(dse.real2shadow(real));
            TransferLib.move(c, dest, true, true);
        }
        for (Coord c : solver.entireRocket) {
            c.setAsVector(real);
            dest.set(dse.real2shadow(real));
            TransferLib.move(c, dest, true, true);
        }
        worldObj.spawnEntityInWorld(dse);
    }
    
    void broadcastState(EntityPlayer who) {
        broadcastMessage(null, MessageType.RocketState, lastValidationStatus, isFiring);
    }
    
    void setValid(boolean nv) {
        if (nv != lastValidationStatus) {
            lastValidationStatus = nv;
            broadcastState(null);
        }
    }
    
    boolean isValid(EntityPlayer player) {
        setValid(calculateValidation(player));
        broadcastState(null);
        return lastValidationStatus;
    }
    
    long next_free_time = 0;
    
    boolean calculateValidation(EntityPlayer player) {
        long now = System.currentTimeMillis();
        if (now < next_free_time) {
            return lastValidationStatus;
        }
        long start = System.currentTimeMillis();
        try {
            new ContiguitySolver(this).solve();
        } catch (RocketValidationException e) {
            e.notify(this, player);
            return false;
        } finally {
            long end = System.currentTimeMillis();
            long delay = Math.max((end - start)*100, 2000);
            next_free_time = end + delay;
        }
        return true;
    }
    
    @Override
    public boolean activate(EntityPlayer entityplayer) {
        if (worldObj.isRemote) {
            return true;
        }
        if (isValid(entityplayer)) {
            Core.notify(entityplayer, getCoord(), "Rocket is valid");
        }
        return true;
    }
    
    @Override
    public Packet getAuxillaryInfoPacket() {
        return getDescriptionPacketWith(MessageType.RocketState, lastValidationStatus, isFiring);
    }
    
    @Override
    public boolean handleMessageFromServer(int messageType, DataInput input) throws IOException {
        if (super.handleMessageFromServer(messageType, input)) {
            return true;
        }
        if (messageType == MessageType.RocketState) {
            boolean v = input.readBoolean();
            boolean f = input.readBoolean();
            if (v != lastValidationStatus || isFiring != f) {
                lastValidationStatus = v;
                isFiring = f;
                getCoord().redraw();
            }
            return true;
        }
        return false;
    }
    
    @Override
    public void updateEntity() {
        if (worldObj.isRemote) {
            return;
        }
        if (inSlice && isFiring) {
            
        } else if (ignitionRequest) {
            ignitionRequest = false;
            ContiguitySolver solver = canIgnite(null);
            if (solver != null) {
                /*for (Coord c : solver.entireRocket) {
                    Core.notify(null, c, NotifyStyle.FORCE, "" + c.y);
                }*/
                ignite(solver);
            }
        }
    }
    
    public void notifyArea(EntityPlayer player) {
        ContiguitySolver solver = canIgnite(player);
        for (Coord c : solver.entireRocket) {
            Core.notify(player, c, NotifyStyle.FORCE, "" + c.y);
        }
    }
        
    //Code for rocket validation follows
    
    
    static class RocketValidationException extends Exception {
        String msg;
        Coord mark = null;
        public RocketValidationException(String msg) { this.msg = msg; }
        public RocketValidationException(String msg, Coord mark) { this.msg = msg; this.mark = mark; }
        public void notify(TileEntityRocketEngine where, EntityPlayer who) {
            if (who != null) {
                if (mark == null) {
                    Core.notify(who, where.getCoord(), msg);
                } else {
                    Core.notify(who, where.getCoord(), "Validation failed");
                    Core.notify(who, mark, msg);
                }
            }
        }
    }
    
    /**
     * Remove & return an element from Collection src. May have unusual set-theoretic implications.
     */
    static <E> E choose(HashSet<E> src) {
        Iterator<E> it = src.iterator();
        E ret = it.next();
        it.remove();
        return ret;
    }
    
    /** 
     * Predicate used for some Plane CoordSet operations
     */
    static interface Criteria<E> {
        public boolean fits(E obj);
    }
    
    /**
     * Return a set that includes all Coords that are contiguous to `seed` in the plane described by `planeNormal`, according to `criteria`
     * The set has a maximize size set in Core.config; it will throw {@link RocketValidationException} if it gets too big.
     */
    static HashSet<Coord> fillPlane(Iterable<Coord> seeds, int planeNormal, Criteria<Coord> criteria) throws RocketValidationException {
        HashSet<Coord> ret = new HashSet(9*9);
        HashSet<Coord> frontier = new HashSet();
        for (Coord seed : seeds) {
            if (criteria.fits(seed)) {
                frontier.add(seed);
            }
        }
        while (frontier.size() > 0) {
            if (ret.size() == Core.max_rocket_base_size) {
                throw new RocketValidationException("Rocket is too wide");
            }
            Coord me = choose(frontier);
            ret.add(me);
            Coord[] neighbors = me.getNeighborsInPlane(planeNormal);
            for (int i = 0; i < neighbors.length; i++) {
                Coord n = neighbors[i];
                if (ret.contains(n) || frontier.contains(n)) {
                    continue;
                }
                if (criteria.fits(n)) {
                    frontier.add(n);
                }
            }
        }
        return ret;
    }
    
    /**
     * Adjust all the Coords by dc.
     */
    static void movePlane(ArrayList<Coord> coordSet, DeltaCoord dc) {
        for (Coord c : coordSet) {
            c.adjust(dc);
        }
    }
    
    static int expandPlane(ArrayList<Coord> coordSet, ForgeDirection normal, Criteria<Coord> crit) {
        int ord = normal.ordinal();
        HashSet<Coord> toAdd = new HashSet();
        for (Coord me : coordSet) {
            Coord[] neighbors = me.getNeighborsInPlane(ord);
            for (int i = 0; i < neighbors.length; i++) {
                Coord n = neighbors[i];
                if (!coordSet.contains(n) && crit.fits(n)) {
                    toAdd.add(n);
                }
            }
        }
        int ret = toAdd.size();
        coordSet.addAll(toAdd);
        return ret;
    }
    
    static void collapsePlane(ArrayList<Coord> plane, Criteria<Coord> crit) {
        Iterator<Coord> it = plane.iterator();
        while (it.hasNext()) {
            Coord here = it.next();
            if (!crit.fits(here)) {
                it.remove();
            }
        }
    }
    
    static HashSet<Coord> cloneSet(Collection<Coord> src) {
        HashSet<Coord> ret = new HashSet(src.size());
        for (Coord c : src) {
            ret.add(c.copy());
        }
        return ret;
    }
    
    static ArrayList<Coord> cloneArray(Collection<Coord> src) {
        ArrayList<Coord> ret = new ArrayList(src.size());
        for (Coord c : src) {
            ret.add(c.copy());
        }
        return ret;
    }
    
    static class ContiguitySolver {
        TileEntityRocketEngine seed;
        HashSet<TileEntityRocketEngine> engines = new HashSet();
        HashSet<Coord> entireRocket = new HashSet();
        ArrayList<Coord> mountingPlane = new ArrayList();
        int fuel = 0;
        
        public ContiguitySolver(TileEntityRocketEngine seed) {
            this.seed = seed;
        }
        
        void addEngine(TileEntityCommon engine) {
            if (engine instanceof TileEntityExtension) {
                engine = ((TileEntityExtension)engine).getParent();
            }
            if (engine instanceof TileEntityRocketEngine) {
                engines.add((TileEntityRocketEngine) engine);
            }
        }
        
        public void solve() throws RocketValidationException {
            Coord mounting = seed.getCoord().add(0, 3, 0); //The blocks above the rocket engine
            Criteria isSolid = new Criteria<Coord>() {
                @Override
                public boolean fits(Coord coord) {
                    return coord.getHardness() > 0;
                }
            };
            List<Coord> seeds = Arrays.asList(mounting, mounting.add(1, 0, 0), mounting.add(1, 0, 1), mounting.add(0, 0, 1));
            mountingPlane.addAll(fillPlane(seeds, 0, isSolid));
            if (mountingPlane.size() == 0) {
                throw new RocketValidationException("Rocket engine is attatched to nothing");
            }
            movePlane(mountingPlane, new DeltaCoord(0, -3, 0));
            for (Coord c : mountingPlane) {
                addEngine(c.getTE(TileEntityCommon.class));
            }
            movePlane(mountingPlane, new DeltaCoord(0, +3, 0));
            
            for (TileEntityRocketEngine engine : engines) {
                entireRocket.addAll(engine.getArea());
            }
            
            ArrayList<Coord> heightScan = cloneArray(mountingPlane);
            //HashSet<Coord> shadow = new HashSet(mountingPlane.size());
            int y = 0;
            DeltaCoord upwards = new DeltaCoord(0, 1, 0);
            entireRocket.addAll(cloneArray(mountingPlane));
            while (true) {
                y++;
                if (y > Core.max_rocket_height) {
                    throw new RocketValidationException("Rocket is too tall");
                }
                movePlane(heightScan, upwards);
                collapsePlane(heightScan, isSolid);
                int last_size = heightScan.size();
                while (true) {
                    expandPlane(heightScan, ForgeDirection.UP, isSolid);
                    int new_size = heightScan.size();
                    if (new_size >= Core.max_rocket_base_size) {
                        throw new RocketValidationException("Rocket is too wide");
                    }
                    if (new_size == last_size) {
                        break;
                    }
                    last_size = new_size;
                }
                if (heightScan.size() >= Core.max_rocket_base_size) {
                    throw new RocketValidationException("Rocket is too wide");
                }
                if (heightScan.size() == 0) {
                    y--;
                    break;
                }
                entireRocket.addAll(cloneArray(heightScan));
                for (Coord c : mountingPlane) {
                    entireRocket.add(c.add(0, y, 0));
                }
                /*for (Coord c : heightScan) {
                    shadow.add(c.add(0, -y, 0));
                }*/
            }
            
            if (entireRocket.size() == 0) {
                throw new RocketValidationException("Rocket is made of nothing!?");
            }
            if (engines.size() == 0) {
                throw new RocketValidationException("Rocket has no engines!?");
            }
            Coord below = seed.getCoord();
            for (Coord c : entireRocket) {
                TileEntity te = c.getTE();
                if (te instanceof TileEntityExtension) {
                    te = ((TileEntityExtension)te).getParent();
                }
                if (te instanceof TileEntityRocketEngine && engines.contains(te)) {
                    continue;
                }
                below.set(c);
                below.y--;
                if (!isSolid.fits(below)) {
                    continue;
                }
                if (entireRocket.contains(below)) {
                    continue;
                }
                throw new RocketValidationException("Can't drag", below);
            }
        }
    
    }

}
