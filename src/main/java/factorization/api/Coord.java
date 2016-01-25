package factorization.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import io.netty.buffer.ByteBuf;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S21PacketChunkData;
import net.minecraft.server.management.PlayerManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.Chunk;

import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.IFluidBlock;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.shared.BlockHelper;
import factorization.shared.Core;
import factorization.net.FzNetDispatch;
import factorization.net.StandardMessageType;
import factorization.shared.TileEntityCommon;
import factorization.util.FzUtil;
import factorization.util.ItemUtil;

// Note: The rules for holding on to references to Coord are the same as for holding on to World.
// Don't keep references to them outside of things that are in worlds to avoid mem-leaks; or be careful about it.
public final class Coord implements IDataSerializable, ISaneCoord, Comparable<Coord> {
    // NORELEASE: Inline things that create BlockPos.
    public World w;
    public int x, y, z;
    private static final Random rand = new Random();
    private static final ThreadLocal<Coord> staticCoord = new ThreadLocal<Coord>();
    
    public static final Coord ZERO = new Coord(null, 0, 0, 0);

    public Coord(World w, int x, int y, int z) {
        this.w = w;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Coord(World w, BlockPos pos) {
        this(w, pos.getX(), pos.getY(), pos.getZ());
    }

    public Coord(TileEntity te) {
        this(te.getWorld(), te.getPos());
    }

    public Coord(Entity ent) {
        this(ent.worldObj, Math.floor(ent.posX), ent.posY + ent.getYOffset(), Math.floor(ent.posZ));
    }

    public Coord(World w, double x, double y, double z) {
        this(w, (int) x, (int) y, (int) z);
        //this(w, (int) Math.floor(x + 0.5), (int) Math.floor(y + 0.5), (int) Math.floor(z + 0.5));
    }

    public Coord(World w, Vec3 v) {
        this(w, v.xCoord, v.yCoord, v.zCoord);
    }

    @Deprecated
    public Coord(World w, MovingObjectPosition mop) {
        this(w, mop.getBlockPos());
    }

    public static Coord fromMop(World world, MovingObjectPosition mop) {
        if (mop.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY && mop.entityHit != null) return new Coord(mop.entityHit);
        return new Coord(world, mop.getBlockPos());
    }
    
    public Coord(Chunk chunk) {
        this(chunk.getWorld(), chunk.xPosition * 16, 0, chunk.zPosition * 16);
    }
    
    @Override public World w() { return w; }
    public int x() { return x; }
    public int y() { return y; }
    public int z() { return z; }
    
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
        } else {
            ret += " (Dimension " + FzUtil.getWorldDimension(w) + ")";
            if (!blockExists()) {
                ret += " not loaded";
            } else {
                Block b = getBlock();
                if (b != null) {
                    ret += " " + getBlock().getClass().getSimpleName();
                    ret += " " + b.getUnlocalizedName();
                } else {
                    ret += " null";
                }
                int md = getMd();
                ret += "#" + md;
                TileEntity te = getTE();
                if (te != null) {
                    ret += " " + te.getClass().getSimpleName();
                }
                //Chunk chunk = getChunk();
                //ret += " " + chunk;
            }
        }
        return ret;
    }

    public String toShortString() {
        String ws = w == null ? "(null)" : ("[" + FzUtil.getWorldDimension(w) + "]");
        return ws + " " + x + "," + y + "," + z;
    }
    
    public void set(World w, int x, int y, int z) {
        this.w = w;
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    public void set(Coord c) {
        set(c.w, c.x, c.y, c.z);
    }

    public void setOff(Coord c, int dx, int dy, int dz) {
        set(c.w, c.x + dx, c.y + dy, c.z + dz);
    }
    
    public void set(Vec3 v) {
        set(w, (int)v.xCoord, (int)v.yCoord, (int)v.zCoord);
    }
    
    public void set(DeltaCoord dc) {
        set(w, dc.x, dc.y, dc.z);
    }
    
    public void set(TileEntity te) {
        set(te.getWorld(), te.getPos());
    }

    public void set(World w, BlockPos pos) {
        w = w;
        set(pos);
    }

    public void set(BlockPos pos) {
        x = pos.getX();
        y = pos.getY();
        z = pos.getZ();
    }

    @Override
    public int hashCode() {
        return (((x * 11) % 71) << 7) + ((z * 7) % 479) + y; //TODO: This hashcode is probably terrible.
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

    public BlockPos.MutableBlockPos copyTo(BlockPos.MutableBlockPos pos) {
        return pos.set(x, y, z);
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
        return new Vec3(x, y, z);
    }
    
    public MovingObjectPosition createMop(EnumFacing side, Vec3 hitVec) {
        return new MovingObjectPosition(hitVec, side, toBlockPos());
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

    public double distanceSq(Entity ent) {
        double dx = x - ent.posX;
        double dy = y - ent.posY;
        double dz = z - ent.posZ;
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
    
    public <T> List<T> getAdjacentTEs(Class<T> clazz) {
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

    @Override
    public int compareTo(Coord o) {
        int d = y - o.y;
        if (d == 0) {
            d = x - o.x;
            if (d == 0) {
                d = z - o.z;
            }
        }
        return d;
    }

    public boolean isSubmissiveTo(Coord o) {
        return y < o.y || x < o.x || z < o.z;
    }
    
    public boolean inside(Coord lower, Coord upper) { // "within"
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

    public Coord add(BlockPos pos) {
        return add(pos.getX(), pos.getY(), pos.getZ());
    }
    
    public Coord add(EnumFacing d) {
        return add(d.getDirectionVec().getX(), d.getDirectionVec().getY(), d.getDirectionVec().getZ());
    }

    public Coord add(int x, int y, int z) {
        return new Coord(w, this.x + x, this.y + y, this.z + z);
    }

    public Coord add(Vec3 v) {
        return new Coord(w, this.x + v.xCoord, this.y + v.yCoord, this.z + v.zCoord);
    }
    
    public Coord center(Coord o) {
        return new Coord(w, (x + o.x)/2, (y + o.y)/2, (z + o.z)/2);
    }
    
    public Vec3 centerVec(Coord o) {
        return new Vec3((x + o.x)/2.0, (y + o.y)/2.0, (z + o.z)/2.0);
    }
    
    public Coord adjust(DeltaCoord dc) { // aka incrAdd
        x += dc.x;
        y += dc.y;
        z += dc.z;
        return this;
    }
    
    public Coord adjust(EnumFacing dc) {
        x += dc.getDirectionVec().getX();
        y += dc.getDirectionVec().getY();
        z += dc.getDirectionVec().getZ();
        return this;
    }
    
    public Coord adjust(int dx, int dy, int dz) {
        x += dx;
        y += dy;
        z += dz;
        return this;
    }
    
    //Methods on the world
    
    public void markBlockForUpdate() {
        //this will re-send block values & TE description to the client, which will also do a redraw()
        //...is what it used to do. Now you have to use syncAndDraw()?
        w.markBlockForUpdate(toBlockPos());
    }

    public void redraw() {
        if (w.isRemote) {
            w.markBlockForUpdate(toBlockPos());
        }
    }
    
    public void syncTE() {
        TileEntityCommon tec = getTE(TileEntityCommon.class);
        if (tec == null) return;
        FMLProxyPacket description = tec.getDescriptionPacket();
        Core.network.broadcastPacket(null, this, description);
    }
    
    public void sendRedraw() {
        if (w.isRemote) {
            redraw();
        } else {
            Core.network.broadcastMessageToBlock(null, this, StandardMessageType.RedrawOnClient);
        }
    }
    
    public void syncAndRedraw() {
        syncTE();
        sendRedraw();
    }
    
    public void notifyNeighbors() {
        BlockPos bp = toBlockPos();
        IBlockState bs = w.getBlockState(bp);
        w.notifyBlockOfStateChange(bp, bs.getBlock());
    }

    public void updateLight() {
        w.checkLight(toBlockPos());
    }
    
    public void updateBlockLight() {
        w.checkLightFor(EnumSkyBlock.BLOCK, toBlockPos());
    }

    public int getLightLevelBlock() {
        return w.getLight(toBlockPos());
    }
    
    public int getLightLevelSky() {
        return w.getLightFor(EnumSkyBlock.SKY, toBlockPos());
    }
    
    public void setLightLevelBlock(int light) {
        getChunk().setLightFor(EnumSkyBlock.BLOCK, toBlockPos(), light);
    }
    
    public void setLightLevelSky(int light) {
        getChunk().setLightFor(EnumSkyBlock.SKY, toBlockPos(), light);
    }

    public void setTE(TileEntity te) {
        w.setTileEntity(toBlockPos(), te);
    }
    
    public void rmTE() {
        w.removeTileEntity(toBlockPos());
    }

    public TileEntity getTE() {
        if (w == null) {
            return null;
        }
        if (!blockExists()) {
            return null;
        }
        return w.getTileEntity(toBlockPos());
        // Could check blockHasTE() first. Might only be needed for TE-scanning, which is often better done w/ hashmap iteration instead
    }

    public TileEntity forceGetTE() {
        if (w == null) return null;
        return w.getTileEntity(toBlockPos());
    }

    public boolean blockHasTE() {
        return getBlock().hasTileEntity(getState());
    }

    @SuppressWarnings("unchecked")
    public <T> T getTE(Class<T> clazz) {
        TileEntity te = getTE();
        if (clazz.isInstance(te)) {
            return (T) te;
        }
        return null;
    }
    
    public Chunk getChunk() {
        return w.getChunkFromBlockCoords(toBlockPos());
    }
    
    public BiomeGenBase getBiome() {
        return w.getBiomeGenForCoords(toBlockPos());
    }

    public void setBiome(BiomeGenBase biome) {
        Chunk chunk = getChunk();
        byte[] biomeData = chunk.getBiomeArray();
        if (biomeData == null) return;
        final int index = (z & 0xF) << 4 | (x & 0xF);
        biomeData[index] = (byte) (biome.biomeID & 0xFF);
    }

    public void resyncChunksFull() {
        if (w.isRemote) return;
        Chunk chunk = getChunk();
        final WorldServer world = (WorldServer) chunk.getWorld();
        final PlayerManager pm = world.getPlayerManager();
        PlayerManager.PlayerInstance watcher = pm.getPlayerInstance(chunk.xPosition, chunk.zPosition, false);
        if (watcher == null) return;
        ArrayList<EntityPlayerMP> players = new ArrayList<EntityPlayerMP>();
        players.addAll(watcher.playersWatchingChunk);
        for (EntityPlayerMP player : players) {
            watcher.removePlayer(player);
            watcher.addPlayer(player);
        }


        Packet packet = new S21PacketChunkData(chunk, true, -1);
        FzNetDispatch.addPacketFrom(packet, chunk);
    }

    public IBlockState getState() {
        return w.getBlockState(toBlockPos());
    }

    public Block getBlock() {
        return getState().getBlock();
    }

    public Material getMaterial() {
        return getState().getBlock().getMaterial();
    }

    @Deprecated
    public int getMd() {
        IBlockState bs = getState();
        return bs.getBlock().getMetaFromState(bs);
    }
    
    public int getRawId() {
        return Block.getIdFromBlock(getBlock());
    }

    public boolean isAir() {
        return w.isAirBlock(toBlockPos());
    }

    public boolean isSolid() {
        Block b = getBlock();
        if (b == null) {
            return false;
        }
        return b.isNormalCube(w, toBlockPos());
    }
    
    public float getHardness() {
        Block b = getBlock();
        if (b == null) {
            return 0;
        }
        return b.getBlockHardness(w, toBlockPos());
    }

    public boolean isBedrock() {
        return getHardness() < 0;
    }

    public boolean isSolidOnSide(EnumFacing side) {
        return w.isSideSolid(toBlockPos(), side);
    }

    public boolean isBlockBurning() {
        Block b = getBlock();
        if (b == null) return false;
        return b == Blocks.fire || b.isBurning(w, toBlockPos());
    }

    public boolean blockExists() {
        return w != null && w.isBlockLoaded(toBlockPos());
    }

    public boolean isReplacable() {
        Block b = getBlock();
        if (b == null) return true;
        return b.isReplaceable(w, toBlockPos());
    }

    @Deprecated // Inaccurate, ignores transparent blocks
    public boolean isTop() {
        return w.getHeight(toBlockPos()).getY() == y;
    }
    
    public int getColumnHeight() {
        return w.getHeight(toBlockPos()).getY();
    }

    public boolean canBeSeenThrough() {
        return getBlock().getLightOpacity() == 0;
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
        return getBlock() == b;
        //NORELEASE ? return b.getDefaultState().equals(getBlock().getDefaultState());
    }

    public boolean is(IBlockState state) {
        return state.equals(getState());
    }

    @Deprecated
    public boolean is(Block b, int md) {
        return getBlock() == b && getMd() == md;
    }
    
    public static final int NOTIFY_NEIGHBORS = 1, UPDATE = 2, ONLY_UPDATE_SERVERSIDE = 4; //TODO, this'll end up in Forge probably

    public boolean set(IBlockState state, boolean notify) {
        int notifyFlag = notify ? NOTIFY_NEIGHBORS | UPDATE : 0;
        return w.setBlockState(toBlockPos(), state, notifyFlag);
    }

    public boolean set(IBlockState state, int notifyFlag) {
        return w.setBlockState(toBlockPos(), state, notifyFlag);
    }

    public boolean setId(Block block, boolean notify) {
        return set(block.getDefaultState(), notify);
    }

    @Deprecated // Use IBlockState
    public boolean setMd(int md, boolean notify) {
        IBlockState bs = getBlock().getStateFromMeta(md);
        return set(bs, notify);
    }

    @Deprecated // Use IBlockState
    public boolean setIdMd(Block block, int md, boolean notify) {
        IBlockState bs = block.getStateFromMeta(md);
        return set(bs, notify);
    }
    
    public void setAir() {
        w.setBlockToAir(toBlockPos());
    }

    @Deprecated
    public boolean setId(Block id) {
        return setId(id, true);
    }

    @Deprecated
    public boolean setMd(int md) {
        return setMd(md, true);
    }
    
    public void notifyBlockChange() {
        w.notifyBlockOfStateChange(toBlockPos(), getBlock()); // NORELEASE: Check that this does what I expect. There's also another call of this in this file.
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
    
    public void writeToStream(ByteBuf dos) {
        dos.writeInt(x);
        dos.writeInt(y);
        dos.writeInt(z);
    }
    
    public void readFromStream(ByteArrayDataInput dis) {
        x = dis.readInt();
        y = dis.readInt();
        z = dis.readInt();
    }
    
    public void readFromStream(ByteBuf dis) {
        x = dis.readInt();
        y = dis.readInt();
        z = dis.readInt();
    }
    
    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        x = data.asSameShare(prefix + "x").putInt(x);
        y = data.asSameShare(prefix + "y").putInt(y);
        z = data.asSameShare(prefix + "z").putInt(z);
        return this;
    }

    public void mark() {
        // Variant of spawnParticle(); for debugging.
        World use_world = w;
        use_world.spawnParticle(EnumParticleTypes.REDSTONE, x + 0.5, y + 0.5, z + 0.5, 0, 0, 0);
    }

    public void spawnParticle(EnumParticleTypes particle) {
        w.spawnParticle(particle, x + 0.5, y + 0.5, z + 0.5, 0, 0, 0);
    }
    
    public boolean remote() {
        return w.isRemote;
    }
    
    public boolean local() {
        return !w.isRemote;
    }
    
    public Entity spawnItem(ItemStack is) {
        Entity ent = new EntityItem(w, x + 0.5, y + 0.5, z + 0.5, is);
        Item item = is.getItem();
        if (item.hasCustomEntity(is)) {
            ent = item.createEntity(w, ent, is);
        }
        w.spawnEntityInWorld(ent);
        return ent;
    }

    public Entity spawnItem(Item it) {
        return spawnItem(new ItemStack(it));
    }
    
    public AxisAlignedBB getCollisionBoundingBox() {
        BlockPos pos = toBlockPos();
        IBlockState bs = w.getBlockState(pos);
        return bs.getBlock().getCollisionBoundingBox(w, pos, bs);
    }
    
    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getSelectedBoundingBoxFromPool() {
        BlockPos pos = toBlockPos();
        IBlockState bs = w.getBlockState(pos);
        return bs.getBlock().getSelectedBoundingBox(w, pos);
    }

    public AxisAlignedBB getBlockBounds() {
        Block block = getBlock();
        double minX = block.getBlockBoundsMinX();
        double maxX = block.getBlockBoundsMaxX();
        double minY = block.getBlockBoundsMinY();
        double maxY = block.getBlockBoundsMaxY();
        double minZ = block.getBlockBoundsMinZ();
        double maxZ = block.getBlockBoundsMaxZ();
        return new AxisAlignedBB(x + minX, y + minY, z + minZ, x + maxX, y + maxY, z + maxZ);
    }
    
    public static AxisAlignedBB aabbFromRange(Coord min, Coord max) {
        Coord.sort(min, max);
        return new AxisAlignedBB(min.x, min.y, min.z, max.x, max.y, max.z);
    }

    public void scheduleUpdate(int delay, int priority) {
        w.updateBlockTick(toBlockPos(), getBlock(), delay, priority);
    }

    @Deprecated
    public void scheduleUpdate(int delay) {
        scheduleUpdate(delay, 0);
    }
    
    public void setAsEntityLocation(Entity ent) {
        ent.worldObj = w;
        ent.setLocationAndAngles(x + 0.5, y, z + 0.5, ent.rotationYaw, ent.rotationPitch);
    }
    
    public void setAsEntityLocationUnsafe(Entity ent) {
        ent.worldObj = w;
        ent.posX = x + 0.5;
        ent.posY = y;
        ent.posZ = z + 0.5;
    }
    
    public void setAsTileEntityLocation(TileEntity te) {
        if (te.getWorld() != w) {
            te.setWorldObj(w);
        }
        te.setPos(toBlockPos());
    }

    public Vec3 toVector() {
        return new Vec3(x, y, z);
    }

    public Vec3 toMiddleVector() {
        return new Vec3(x + 0.5, y + 0.5, z + 0.5);
    }

    @Override
    public BlockPos toBlockPos() {
        return new BlockPos(x, y, z);
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
        int r = 1;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                BlockPos top = w.getTopSolidOrLiquidBlock(toBlockPos());
                y = Math.max(y, top.getY());
            }
        }
    }
    
    public boolean isPowered() {
        return w.isBlockPowered(toBlockPos());
    }
    
    public boolean isWeaklyPowered() {
        return w.isBlockIndirectlyGettingPowered(toBlockPos()) > 0;
    }

    public int getPowerInput(EnumFacing face) {
        return w.getRedstonePower(toBlockPos(), face);
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
    
    public static void iterateEmptyBox(Coord min, Coord max, ICoordFunction func) {
        // Warning: Probably broken; check it before you use it
        min = min.copy();
        max = max.copy();
        sort(min, max);
        Coord here = min.copy();
        Coord food = here.copy();
        for (here.y = min.y; here.y <= max.y; here.y++) {
            if (here.y == min.y || here.y == max.y) {
                // Do the top / bottom
                for (here.x = min.x; here.x <= max.x; here.x++) {
                    for (here.z = min.z; here.z <= max.z; here.z++) {
                        food.set(here);
                        func.handle(food);
                    }
                }
                continue;
            }
            for (here.x = min.x; here.x <= max.x; here.x++) {
                if (here.x == min.x || here.x == max.x) {
                    // Fill a line between them
                    for (here.z = min.z; here.z <= max.z; here.z++) {
                        func.handle(food);
                    }
                    continue;
                }
                here.z = min.z;
                food.set(here);
                func.handle(food);
                here.z = max.z;
                food.set(here);
                func.handle(food);
            }
        }
    }

    public static void iterateChunks(Coord min, Coord max, ICoordFunction func) {
        min = min.copy();
        max = max.copy();
        sort(min, max);
        Coord here = min.copy();
        for (int x = min.x; x <= max.x; x += 16) {
            for (int z = min.z; z <= max.z; z += 16) {
                here.x = x;
                here.z = z;
                func.handle(here);
            }
        }
    }
    
    public static void drawLine(Coord start, Coord end, ICoordFunction func) {
        Coord at = start.copy();
        double len = start.distance(end);
        double t = 0;
        double dt = 1.0 / len;
        int elsewhere = (int) (len * 3);
        Coord last = end.add(elsewhere, elsewhere, elsewhere);
        DeltaCoord d = end.difference(start);
        while (t <= 1) {
            at.x = (int) (d.x * t) + start.x;
            at.y = (int) (d.y * t) + start.y;
            at.z = (int) (d.z * t) + start.z;
            if (!at.equals(last)) {
                func.handle(at);;
                last.set(at);
            }
            t += dt;
        }
    }
    
    public boolean hasSimilarCoordinate(Coord other) {
        return x == other.x || y == other.y || z == other.z;
    }
    
    public int getComparatorOverride(EnumFacing side) {
        Block b = getBlock();
        if (b == null || !b.hasComparatorInputOverride()) {
            return 0;
        }
        return b.getComparatorInputOverride(w, toBlockPos());
    }
    
    private static Vec3 nullVec = new Vec3(0, 0, 0);
    private static boolean spam = false;
    public ItemStack getPickBlock(EnumFacing dir, EntityPlayer player) {
        Block b = getBlock();
        MovingObjectPosition mop = createMop(dir, nullVec);
        try {
            return b.getPickBlock(mop, w, toBlockPos(), player);
        } catch (NoSuchMethodError t) {
            if (!spam) {
                /*Core.logWarning("Blocks.getPickBlock is unusable on the server." +
            " A workaround prevents crashes, but may possibly allow dupe bugs." +
            " The developer is no longer interested in wasting his time, energy, and vitality on this matter." +
            " This is not a bug, it is a fact of life." +
            " Do not attempt to report it. If you have happened somehow to have actually improved the situation in Forge/Vanilla," +
            " the developer would, of course, be happy to learn of it. Otherwise, if I hear about it, I will ban you or something.");
                t.printStackTrace();*/
                spam = true;
            }
            return BlockHelper.getPlacingItem(b, mop, w, toBlockPos());
        }
    }

    public ItemStack getPickBlock(EnumFacing dir) {
        return getPickBlock(dir, null);
    }
    
    public ItemStack getPickBlock(MovingObjectPosition mop, EntityPlayer player) {
        Block b = getBlock();
        return b.getPickBlock(mop, w, toBlockPos(), player);
    }
    
    public ItemStack getBrokenBlock() {
        Block b = getBlock();
        List<ItemStack> dropped = b.getDrops(w, toBlockPos(), getState(), 0);
        if (dropped == null || dropped.isEmpty()) {
            return null;
        }
        ItemStack main = null;
        for (ItemStack other : dropped) {
            if (main == null) {
                main = other.copy();
                continue;
            }
            if (!ItemUtil.couldMerge(main, other)) {
                return null;
            }
            main.stackSize += other.stackSize;
        }
        return main;
    }

    public Fluid getFluid() {
        Block b = getBlock();
        if (b instanceof IFluidBlock) {
            return ((IFluidBlock) b).getFluid();
        }
        if (b == Blocks.water || b == Blocks.flowing_water) {
            return FluidRegistry.WATER;
        }
        if (b == Blocks.lava || b == Blocks.flowing_lava) {
            return FluidRegistry.LAVA;
        }
        return null;
    }

    public int getDimensionID() {
        return w.provider.getDimensionId();
    }

    public void breakBlock() {
        Block b = getBlock();
        b.dropBlockAsItem(w, toBlockPos(), getState(), 0 /* fortune */);
    }

    public boolean isAt(TileEntity te) {
        BlockPos bp = te.getPos();
        return bp.getX() == x && bp.getY() == y && bp.getZ() == z && te.getWorld() == w;
    }

    public boolean isNormalCube() {
        return getBlock().isNormalCube(w, toBlockPos());
    }

    public boolean isInvalid() {
        return x == 0 && y == -1 && z == 0;
    }

    public static Coord getInvalid() {
        return new Coord(null, 0, -1, 0);
    }

    public <T extends Comparable<T>> T getProperty(IProperty<T> PROPERTY) {
        return getState().getValue(PROPERTY);
    }

    public <T extends Comparable<T>> T getPropertyOr(IProperty<T> PROPERTY, T or) {
        Comparable ret = getState().getProperties().get(PROPERTY);
        if (ret == null) return or;
        return (T) ret;
    }

    public <T extends Comparable<T>> void set(IProperty<T> PROPERTY, T value, boolean notify) {
        set(getState().withProperty(PROPERTY, value), notify);
    }

    public <T extends Comparable<T>> void set(IProperty<T> PROPERTY, T value) {
        set(getState().withProperty(PROPERTY, value), true);
    }

    public <T extends Comparable<T>> boolean trySet(IProperty<T> PROPERTY, T value) {
        IBlockState bs = getState();
        if (bs.getProperties().get(PROPERTY) == null) return false;
        bs = bs.withProperty(PROPERTY, value);
        return set(bs, true);
    }

    public <T extends Comparable<T>> boolean has(IProperty<T> PROPERTY, T... values) {
        T actual = getPropertyOr(PROPERTY, null);
        for (T v : values) {
            if (v == actual) return true;
        }
        return false;
    }

    public boolean stateIs(IBlockState otherState) {
        IBlockState mine = getState();
        Block myBlock = mine.getBlock();
        if (myBlock != otherState.getBlock()) return false;
        return myBlock.getMetaFromState(mine) == myBlock.getMetaFromState(otherState);
    }

}
