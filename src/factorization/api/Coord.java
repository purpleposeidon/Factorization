package factorization.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.common.ForgeDirection;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.common.FactorizationUtil;

public class Coord implements IDataSerializable {
    public World w;
    public int x, y, z;
    static private Random rand = new Random();
    static private ThreadLocal<Coord> staticCoord = new ThreadLocal<Coord>();

    public Coord(World w, int x, int y, int z) {
        this.w = w;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Coord(TileEntity te) {
        this(te.worldObj, te.xCoord, te.yCoord, te.zCoord);
    }

    public Coord(Entity ent) {
        this(ent.worldObj, Math.floor(ent.posX), ent.posY + ent.yOffset, Math.floor(ent.posZ));
    }

    public Coord(World w, double x, double y, double z) {
        this(w, (int) x, (int) y, (int) z);
        //this(w, (int) Math.floor(x + 0.5), (int) Math.floor(y + 0.5), (int) Math.floor(z + 0.5));
    }
    
    public Coord(World w, MovingObjectPosition mop) {
        this(w, mop.blockX, mop.blockY, mop.blockZ);
    }
    
    public static Coord tryLoad(World world, Object o) {
        if (o instanceof Coord) {
            return (Coord) o;
        }
        if (o instanceof Vec3) {
            Vec3 vec = (Vec3) o;
            return new Coord(world, vec.xCoord, vec.yCoord, vec.zCoord);
        }
        if (o instanceof Entity) {
            Entity e = (Entity) o;
            return new Coord(e);
        }
        if (o instanceof TileEntity) {
            TileEntity te = (TileEntity) o;
            return new Coord(te);
        }
        return null;
    }
    
    public static Coord of(int x, int y, int z) {
        return of((World) null, x, y, z);
    }
    
    public static Coord of(double x, double y, double z) {
        return of((World) null, (int) x, (int) y, (int) z);
    }
    
    public static Coord of(World w, int x, int y, int z) {
        Coord ret = staticCoord.get();
        if (ret == null) {
            ret = new Coord(w, x, y, z);
            staticCoord.set(ret);
            return ret;
        }
        ret.set(w, x, y, z);
        return ret;
    }

    @Override
    public String toString() {
        String ret = "(" + x + ", " + y + ", " + z + ")";
        if (w == null) {
            ret += " null world";
        } else if (!blockExists()) {
            ret += " not loaded";
        } else if (w != null) {
            Block b = getBlock();
            if (b != null) {
                ret += " " + getBlock().getClass().getSimpleName();
            } else {
                ret += " air";
            }
            TileEntity te = getTE();
            if (te != null) {
                ret += " " + te.getClass().getSimpleName();
            }
            //Chunk chunk = getChunk();
            //ret += " " + chunk;
        }
        return ret;
    }
    
    public void set(World w, int x, int y, int z) {
        this.w = w;
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    public void set(ChunkCoordinates cc) {
        set(w, cc.posX, cc.posY, cc.posZ);
    }
    
    public void set(Coord c) {
        set(c.w, c.x, c.y, c.z);
    }
    
    public void set(Vec3 v) {
        set(w, (int)v.xCoord, (int)v.yCoord, (int)v.zCoord);
    }
    
    public void set(DeltaCoord dc) {
        set(w, dc.x, dc.y, dc.z);
    }

    @Override
    public int hashCode() {
        return (((x * 11) % 71) << 7) + ((z * 7) % 479) + y;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Coord) {
            Coord other = (Coord) obj;
            return x == other.x && y == other.y && z == other.z && w == other.w;
        }
        return false;
    }

    public Coord copy() {
        return new Coord(w, x, y, z);
    }
    
    public int get(int axis) {
        switch (axis) {
        case 0: return x;
        case 1: return y;
        case 2: return z;
        default: throw new RuntimeException("Invalid argument");
        }
    }
    
    public void set(int axis, int value) {
        switch (axis) {
        case 0: x = value; return;
        case 1: y = value; return;
        case 2: z = value; return;
        default: throw new RuntimeException("Invalid argument");
        }
    }
    
    public Vec3 createVector() {
        return Vec3.createVectorHelper(x, y, z);
    }
    
    public MovingObjectPosition createMop(ForgeDirection side, Vec3 hitVec) {
        return new MovingObjectPosition(x, y, z, side.ordinal(), hitVec);
    }

    /** @return boolean for a checkerboard pattern */
    public boolean parity() {
        return ((x + y + z) & 1) == 0;
    }

    /** @return a mostly unique integer for this location */
    public int seed() {
        return ((x << 4 + z) << 8) + y;
    }

    public DeltaCoord difference(Coord b) {
        return new DeltaCoord(x - b.x, y - b.y, z - b.z);
    }
    
    public DeltaCoord asDeltaCoord() {
        return new DeltaCoord(x, y, z);
    }

    public double distance(Coord o) {
        return Math.sqrt(distanceSq(o));
    }

    public int distanceSq(Coord o) {
        if (o == null) {
            return 0;
        }
        int dx = x - o.x, dy = y - o.y, dz = z - o.z;
        return dx * dx + dy * dy + dz * dz;
    }

    public int distanceManhatten(Coord o) {
        if (o == null) {
            return 0;
        }
        int dx = x - o.x, dy = y - o.y, dz = z - o.z;
        return Math.abs(dx) + Math.abs(dy) + Math.abs(dz);
    }

    public ArrayList<Coord> getNeighborsAdjacent() {
        ArrayList<Coord> ret = new ArrayList<Coord>(6);
        for (DeltaCoord d : DeltaCoord.directNeighbors) {
            ret.add(this.add(d));
        }
        return ret;
    }
    
    public <T> Iterable<T> getAdjacentTEs(Class<T> clazz) {
        ArrayList<T> ret = new ArrayList<T>(6);
        for (Coord n : getNeighborsAdjacent()) {
            T toAdd = n.getTE(clazz);
            if (toAdd != null) {
                ret.add(toAdd);
            }
        }
        return ret;
    }

    public ArrayList<Coord> getNeighborsDiagonal() {
        ArrayList<Coord> ret = new ArrayList<Coord>(26);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    ret.add(this.add(dx, dy, dz));
                }
            }
        }
        return ret;
    }

    public ArrayList<Coord> getRandomNeighborsAdjacent() {
        ArrayList<Coord> ret = getNeighborsAdjacent();
        Collections.shuffle(ret);
        return ret;
    }

    public ArrayList<Coord> getRandomNeighborsDiagonal() {
        ArrayList<Coord> ret = getNeighborsDiagonal();
        Collections.shuffle(ret);
        return ret;
    }

    public Coord getSingleRandomNeighborAdjacent() {
        int r = rand.nextInt(DeltaCoord.directNeighbors.length);
        return this.add(DeltaCoord.directNeighbors[r]);
    }

    public Coord[] getNeighborsInPlane(int side) {
        //For god's sake, don't change the order of these return values.
        //That would mess up wire rendering.
        switch (side) {
        case 0:
        case 1: //y
            return new Coord[] {
                    add(-1, 0, 0),
                    add(+1, 0, 0),
                    add(0, 0, -1),
                    add(0, 0, +1)
            };
        case 2:
        case 3: //z
            return new Coord[] {
                    add(-1, 0, 0),
                    add(+1, 0, 0),
                    add(0, -1, 0),
                    add(0, +1, 0)
            };
        case 4:
        case 5: //x
            return new Coord[] {
                    add(0, 0, -1),
                    add(0, 0, +1),
                    add(0, -1, 0),
                    add(0, +1, 0)
            };
        }
        return null;
    }

    public Coord[] getNeighborsOutOfPlane(int side) {
        switch (side) {
        case 0:
        case 1: //y
            return new Coord[] {
                    add(0, -1, 0),
                    add(0, +1, 0)
            };
        case 2:
        case 3: //z
            return new Coord[] {
                    add(0, 0, -1),
                    add(0, 0, +1),
            };
        case 4:
        case 5: //x
            return new Coord[] {
                    add(-1, 0, 0),
                    add(+1, 0, 0),
            };
        }
        return null;
    }

    public boolean isSubmissiveTo(Coord o) {
        return y < o.y || x < o.x || z < o.z;
    }
    
    public boolean isCompletelySubmissiveTo(Coord o) {
        return x < o.x && y < o.y && z < o.z;
    }
    
    public boolean inside(Coord lower, Coord upper) {
        return lower.lesserOrEqual(this) && lesserOrEqual(upper);
    }
    
    public boolean lesserOrEqual(Coord o) {
        return x <= o.x && y <= o.y && z <= o.z;
    }

    public void setWorld(World newWorld) {
        this.w = newWorld;
    }

    public Coord add(DeltaCoord d) {
        return add(d.x, d.y, d.z);
    }
    
    public Coord add(ForgeDirection d) {
        return add(d.offsetX, d.offsetY, d.offsetZ);
    }

    public Coord add(int x, int y, int z) {
        return new Coord(w, this.x + x, this.y + y, this.z + z);
    }
    
    public Coord center(Coord o) {
        return new Coord(w, (x + o.x)/2, (y + o.y)/2, (z + o.z)/2);
    }
    
    public Vec3 centerVec(Coord o) {
        return Vec3.createVectorHelper((x + o.x)/2.0, (y + o.y)/2.0, (z + o.z)/2.0);
    }
    
    /**
     * Adjusts position. 0, 1: y; 2, 3: z; 4, 5: x
     * 
     */
    public Coord towardSide(int side) {
        switch (side) {
        case 0:
            y -= 1;
            break;
        case 1:
            y += 1;
            break;
        case 2:
            z -= 1;
            break;
        case 3:
            z += 1;
            break;
        case 4:
            x -= 1;
            break;
        case 5:
            x += 1;
            break;
        }
        return this;
    }
    
    public void adjust(DeltaCoord dc) {
        x += dc.x;
        y += dc.y;
        z += dc.z;
    }
    
    public void adjust(ForgeDirection dc) {
        x += dc.offsetX;
        y += dc.offsetY;
        z += dc.offsetZ;
    }
    
    //Methods on the world
    
    public void markBlockForUpdate() {
        //this will re-send block values & TE description to the client, which will also do a redraw()
        w.markBlockForUpdate(x, y, z);
    }

    public void redraw() {
        if (w.isRemote) {
            w.markBlockForRenderUpdate(x, y, z);
        }
    }
    
    public void notifyNeighbors() {
        //this will probably take care of our redstone issues
        w.notifyBlocksOfNeighborChange(x, y, z, getId());
    }

    public void updateLight() {
        w.updateAllLightTypes(x, y, z);
    }
    
    public void updateBlockLight() {
        w.updateLightByType(EnumSkyBlock.Block, x, y, z);
    }

    public void setTE(TileEntity te) {
        w.setBlockTileEntity(x, y, z, te);
    }

    public TileEntity getTE() {
        if (w == null) {
            return null;
        }
        return w.getBlockTileEntity(x, y, z);
    }

    @SuppressWarnings("unchecked")
    public <T> T getTE(Class<T> clazz) {
        TileEntity te = getTE();
        if (clazz.isInstance(te)) {
            return (T) te;
        }
        return null;
    }
    
    public void rmTE() {
        w.removeBlockTileEntity(x, y, z);
    }
    
    public Chunk getChunk() {
        return w.getChunkFromBlockCoords(x, z);
    }

    public Block getBlock() {
        return Block.blocksList[getId()];
    }

    public int getId() {
        return w.getBlockId(x, y, z);
    }

    public int getMd() {
        return w.getBlockMetadata(x, y, z);
    }

    public boolean isAir() {
        return w.isAirBlock(x, y, z);
    }

    public boolean isSolid() {
        Block b = getBlock();
        if (b == null) {
            return false;
        }
        return b.isBlockNormalCube(w, x, y, z);
    }
    
    public float getHardness() {
        Block b = getBlock();
        if (b == null) {
            return 0;
        }
        return b.getBlockHardness(w, x, y, z);
    }

    /** Let's try to use Orientation */
    public boolean isSolidOnSide(int side) {
        return w.isBlockSolidOnSide(x, y, z, ForgeDirection.getOrientation(side));
    }
    
    public boolean isSolidOnSide(ForgeDirection side) {
        return w.isBlockSolidOnSide(x, y, z, side);
    }
    
    public boolean isBlockBurning() {
        Block b = getBlock();
        if (b == null) {
            return false;
        }
        return b == Block.fire || b.isBlockBurning(w, x, y, z);
    }

    public boolean blockExists() {
        return w.blockExists(x, y, z);
    }

    public boolean isReplacable() {
        Block b = getBlock();
        if (b == null) {
            return true;
        }
        if (b.blockMaterial.isReplaceable()) {
            return true;
        }
        return b.isBlockReplaceable(w, x, y, z);
    }

    public boolean isTop() {
        return w.getHeightValue(x, z) == y;
    }

    public boolean canBeSeenThrough() {
        if (w.isAirBlock(x, y, z)) {
            return true;
        }
        return Block.lightOpacity[getId()] == 0;
    }

    public boolean canSeeSky() {
        Coord skyLook = this.copy();
        for (int i = y + 1; i < w.getHeight(); i++) {
            skyLook.y = i;
            if (!skyLook.canBeSeenThrough()) {
                return false;
            }
        }
        return true;
        //...okay, so apparently this doesn't get updated or something? wtf?
        //		int top = w.getHeightValue(x, z);
        //		if (top <= y) {
        //			return true;
        //		}
        //		for (int i = top; i > y; i--) {
        //			if (!w.isAirBlock(x, i, z)) {
        //				return false;
        //			}
        //		}
        //		return true;
    }

    public boolean is(Block b) {
        return getId() == b.blockID;
    }

    public boolean is(Block b, int md) {
        return getId() == b.blockID && getMd() == md;
    }
    
    public static final int NOTIFY_NEIGHBORS = 1, UPDATE = 2, ONLY_UPDATE_SERVERSIDE = 4; //TODO, this'll end up in Forge probably

    public boolean setId(int id, boolean notify) {
        int notifyFlag = notify ? NOTIFY_NEIGHBORS | UPDATE : 0;
        return w.setBlock(x, y, z, id, 0, notifyFlag);
    }

    public boolean setMd(int md, boolean notify) {
        int notifyFlag = notify ? NOTIFY_NEIGHBORS | UPDATE : 0;
        return w.setBlockMetadataWithNotify(x, y, z, md, notifyFlag);
    }

    public boolean setIdMd(int id, int md, boolean notify) {
        int notifyFlag = notify ? NOTIFY_NEIGHBORS | UPDATE : 0;
        return w.setBlock(x, y, z, id, md, notifyFlag);
    }

    public boolean setId(int id) {
        return setId(id, true);
    }

    public boolean setMd(int md) {
        return setMd(md, true);
    }

    public boolean setIdMd(int id, int md) {
        return setIdMd(id, md, true);
    }

    public boolean setId(Block block) {
        return setId(block.blockID);
    }
    
    public void notifyBlockChange() {
        w.notifyBlockChange(x, y, z, getId());
    }

    public void writeToNBT(String prefix, NBTTagCompound tag) {
        tag.setInteger(prefix + "x", x);
        tag.setInteger(prefix + "y", y);
        tag.setInteger(prefix + "z", z);
    }

    public void readFromNBT(String prefix, NBTTagCompound tag) {
        x = tag.getInteger(prefix + "x");
        y = tag.getInteger(prefix + "y");
        z = tag.getInteger(prefix + "z");
    }
    
    public void writeToStream(ByteArrayDataOutput dos) {
        dos.writeInt(x);
        dos.writeInt(y);
        dos.writeInt(z);
    }
    
    public void readFromStream(ByteArrayDataInput dis) {
        x = dis.readInt();
        y = dis.readInt();
        z = dis.readInt();
    }
    
    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        x = data.asSameShare(prefix + "x").put(x);
        y = data.asSameShare(prefix + "y").put(y);
        z = data.asSameShare(prefix + "z").put(z);
        return this;
    }

    public void mark() {
        World use_world = w;
        use_world.spawnParticle("reddust", x + 0.5, y + 0.5, z + 0.5, 0, 0, 0);
    }
    
    public boolean remote() {
        return w.isRemote;
    }
    
    public boolean local() {
        return !w.isRemote;
    }
    
    public EntityItem spawnItem(ItemStack is) {
        EntityItem ent = new EntityItem(w, x + 0.5, y + 0.5, z + 0.5, is);
        w.spawnEntityInWorld(ent);
        return ent;
    }
    
    public AxisAlignedBB getCollisionBoundingBoxFromPool() {
        Block b = getBlock();
        if (b == null) {
            return null;
        }
        return b.getCollisionBoundingBoxFromPool(w, x, y, z);
    }
    
    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getSelectedBoundingBoxFromPool() {
        Block b = getBlock();
        if (b == null) {
            return null;
        }
        return b.getSelectedBoundingBoxFromPool(w, x, y, z);
    }
    
    public void notifyOfNeighborChange(int neighborId) {
        w.notifyBlocksOfNeighborChange(x, y, z, neighborId);
    }
    
    public void scheduleUpdate(int delay) {
        w.scheduleBlockUpdate(x, y, z, getId(), delay);
    }
    
    @Deprecated //Need to use ent.setLocationAndAngles or whatever it's called
    public void setAsEntityLocation(Entity ent) {
        ent.worldObj = w;
        ent.posX = x + 0.5;
        ent.posY = y;
        ent.posZ = z + 0.5;
    }
    
    public void setAsTileEntityLocation(TileEntity te) {
        te.worldObj = w;
        te.xCoord = x;
        te.yCoord = y;
        te.zCoord = z;
    }
    
    public void setAsVector(Vec3 vec) {
        vec.xCoord = x;
        vec.yCoord = y;
        vec.zCoord = z;
    }
    
    public static void sort(Coord lower, Coord upper) {
        Coord a = lower.copy();
        Coord b = upper.copy();
        lower.x = Math.min(a.x, b.x);
        lower.y = Math.min(a.y, b.y);
        lower.z = Math.min(a.z, b.z);
        upper.x = Math.max(a.x, b.x);
        upper.y = Math.max(a.y, b.y);
        upper.z = Math.max(a.z, b.z);
    }
    
    public void moveToTopBlock() {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                y = Math.max(y,  w.getTopSolidOrLiquidBlock(x + dx, z + dz));
            }
        }
    }

    public void removeTE() {
        w.removeBlockTileEntity(x, y, z);
    }
    
    public boolean isPowered() {
        return w.getBlockPowerInput(x, y, z) > 0;
    }
    
    public static void iterateCube(Coord a, Coord b, ICoordFunction func) {
        a = a.copy();
        b = b.copy();
        sort(a, b);
        Coord here = a.copy();
        for (int x = a.x; x <= b.x; x++) {
            for (int y = a.y; y <= b.y; y++) {
                for (int z = a.z; z <= b.z; z++) {
                    here.set(here.w, x, y, z);
                    func.handle(here);
                }
            }
        }
    }
    
    public boolean hasSimilarCoordinate(Coord other) {
        return x == other.x || y == other.y || z == other.z;
    }
    
    public int getComparatorOverride(ForgeDirection side) {
        Block b = getBlock();
        if (b == null || !b.hasComparatorInputOverride()) {
            return 0;
        }
        return b.getComparatorInputOverride(w, x, y, z, side.ordinal());
    }
    
    private static Vec3 nullVec = Vec3.createVectorHelper(0, 0, 0);
    public ItemStack getPickBlock(ForgeDirection dir) {
        Block b = getBlock();
        if (b == null) {
            return null;
        }
        MovingObjectPosition mop = createMop(dir, nullVec);
        return b.getPickBlock(mop, w, x, y, z);
    }
    
    public ItemStack getBrokenBlock() {
        Block b = getBlock();
        if (b == null) {
            return null;
        }
        ArrayList<ItemStack> dropped = b.getBlockDropped(w, x, y, z, getMd(), 0);
        if (dropped == null || dropped.isEmpty()) {
            return null;
        }
        ItemStack main = dropped.remove(0);
        for (int i = 0; i < dropped.size(); i++) {
            ItemStack other = dropped.get(i);
            if (!FactorizationUtil.couldMerge(main, other)) {
                return null;
            }
            main.stackSize += other.stackSize;
        }
        return main;
    }
}
