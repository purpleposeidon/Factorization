package factorization.fzds;

import java.util.ArrayList;
import java.util.List;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;

import cpw.mods.fml.common.registry.IEntityAdditionalSpawnData;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import factorization.api.Coord;
import factorization.common.Core;
import factorization.fzds.api.IFzdsEntryControl;

public class DimensionSliceEntity extends Entity implements IFzdsEntryControl, IEntityAdditionalSpawnData {
    int cell;
    
    public Coord hammerCell;
    Object renderInfo = null;
    AxisAlignedBB shadowArea = null, shadowCollisionArea = null, realArea = null, realCollisionArea = null;
    PacketProxyingPlayer proxy = null;
    boolean needAreaUpdate = true;
    ArrayList<DseCollider> children = null; //we don't init here so that it's easy to see if we need to init it
    
    static final double offsetXZ = Hammer.cellWidth*16/2.0;
    static final double offsetY = 0; //TODO?
    
    public DimensionSliceEntity(World world) {
        super(world);
        ignoreFrustumCheck = true; //kinda lame; we should give ourselves a proper bounding box?
        //noClip = true;
    }
    
    public DimensionSliceEntity(World world, int cell) {
        this(world);
        this.cell = cell;
        this.hammerCell = Hammer.getCellCorner(world, cell);
    }
    
    private static Vec3 buffer = Vec3.createVectorHelper(0, 0, 0);
    
    public Vec3 real2shadow(Vec3 realCoords) {
        //NOTE: This ignores transformations! Need to fix!
        double diffX = realCoords.xCoord + offsetXZ - posX;
        double diffY = realCoords.yCoord + offsetY - posY;
        double diffZ = realCoords.zCoord + offsetXZ - posZ;
        buffer.xCoord = hammerCell.x + diffX;
        buffer.yCoord = hammerCell.y + diffY;
        buffer.zCoord = hammerCell.z + diffZ;
        return buffer;
    }
    
    public Vec3 shadow2real(Vec3 shadowCoords) {
        //NOTE: This ignores transformations! Need to fix!
        //this.hammerCell = Hammer.getCellCorner(worldObj, cell);
        double diffX = shadowCoords.xCoord - hammerCell.x;
        double diffY = shadowCoords.yCoord - hammerCell.y;
        double diffZ = shadowCoords.zCoord - hammerCell.z;
        buffer.xCoord = posX + diffX - offsetXZ;
        buffer.yCoord = posY + diffY - offsetY;
        buffer.zCoord = posZ + diffZ - offsetXZ;
        return buffer;
    }
    
    @Override
    public AxisAlignedBB getBoundingBox() {
        return null;
    }
    
    @Override
    public void onCollideWithPlayer(EntityPlayer player) {
//		if (!worldObj.isRemote) {
//			//System.out.println("X");
//			return;
//		}
//		player.motionY = Math.max(player.motionY, 0);
//		player.onGround = true;
    }
    
    @Override
    protected void entityInit() {}

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        cell = tag.getInteger("cell");
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        tag.setInteger("cell", cell);
    }
    
    @Override
    public boolean canBeCollidedWith() {
        return true;
    }
    
    @Override
    public boolean canBePushed() {
        return false;
    }
    
    private AxisAlignedBB cloneAABB(AxisAlignedBB orig) {
        AxisAlignedBB ret = makeAABB();
        ret.setBB(orig);
        return ret;
    }
    
    private AxisAlignedBB makeAABB() {
        return AxisAlignedBB.getBoundingBox(0, 0, 0, 0, 0, 0);
    }
    
    private AxisAlignedBB offsetAABB(AxisAlignedBB orig, double dx, double dy, double dz) {
        return AxisAlignedBB.getBoundingBox(
                orig.minX + dx, orig.minY + dy, orig.minZ + dz,
                orig.maxX + dx, orig.maxY + dy, orig.maxZ + dz);
    }
    
    public void updateArea() {
        Coord c = this.hammerCell;
        Coord d = Hammer.getCellOppositeCorner(worldObj, this.cell);
        
        AxisAlignedBB start = null;
        for (int x = c.x; x < d.x; x++) {
            for (int y = c.y; y < d.y; y++) {
                for (int z = c.z; z < d.z; z++) {
                    Block block = Block.blocksList[c.w.getBlockId(x, y, z)];
                    if (block == null) {
                        continue;
                    }
                    AxisAlignedBB b = block.getCollisionBoundingBoxFromPool(c.w, x, y, z);
                    if (b == null) {
                        continue;
                    }
                    if (start == null) {
                        start = b;
                    } else {
                        start.minX = Math.min(start.minX, b.minX);
                        start.minY = Math.min(start.minY, b.minY);
                        start.minZ = Math.min(start.minZ, b.minZ);
                        start.maxX = Math.max(start.maxX, b.maxX);
                        start.maxY = Math.max(start.maxY, b.maxY);
                        start.maxZ = Math.max(start.maxZ, b.maxZ);
                    }
                }
            }
        }
        System.out.println("DSE area: " + start); //NORELEASE
        if (start == null) {
            if (worldObj.isRemote) {
                return;
            }
            setDead();
            shadowArea = makeAABB();
            shadowCollisionArea = makeAABB();
            realArea = makeAABB();
            realCollisionArea = makeAABB();
            return;
        }
        
        shadowArea = cloneAABB(start);
        shadowCollisionArea = shadowArea.expand(2, 2, 2);
        {
            double dx = posX - c.x - offsetXZ, dy = posY - c.y - offsetY, dz = posZ - c.z - offsetXZ;
            realArea = offsetAABB(shadowArea, dx, dy, dz); //NOTE: Will need to update realArea when we move
            realCollisionArea = offsetAABB(shadowCollisionArea, dx, dy, dz);
        }
        needAreaUpdate = false;
        //this.boundingBox.setBB(realArea);
        if (children == null && worldObj.isRemote) {
            children = new ArrayList((int) Math.pow(Hammer.cellWidth, 3));
            //The array will be filled as the server sends us children
        } else if (children == null && !worldObj.isRemote) {
            children = new ArrayList((int) Math.pow(Hammer.cellWidth, 3));
            int width = Hammer.cellWidth*16;
            int height = Hammer.cellHeight*16;
            for (int dx = -width/2; dx < width/2; dx += 16) {
                for (int dy = 0; dy < height; dy += 16) {
                    for (int dz = -width/2; dz < width/2; dz += 16) {
                        Entity e = new DseCollider(this, Vec3.createVectorHelper(dx, dy, dz));
                        e.onEntityUpdate();
                        worldObj.spawnEntityInWorld(e);
                    }
                }
            }
        }
    }
    
    void init() {
        if (hammerCell == null) {
            this.hammerCell = Hammer.getCellCorner(worldObj, cell);
        }
        Hammer.getSlices(worldObj).add(this);
    }
    
    public void blocksChanged(int x, int y, int z) {
        if (shadowArea == null) {
            needAreaUpdate = true;
            return;
        }
        needAreaUpdate |= x < shadowArea.minX || y < shadowArea.minY || z < shadowArea.minZ
                || x > shadowArea.maxX || y > shadowArea.maxY || z > shadowArea.maxZ;
    }
    
    @Override
    public void setPosition(double par1, double par3, double par5) {
        super.setPosition(par1, par3, par5);
        needAreaUpdate = true;
    }
    
    void doUpdate() {
        if (worldObj.isRemote) {
            if (hammerCell == null) {
                init();
                return;
            }
        } else if (proxy == null && !isDead) {
            init();
            proxy = new PacketProxyingPlayer(this);
            proxy.worldObj.spawnEntityInWorld(proxy);
            return;
        }
        if (needAreaUpdate) {
            Core.profileStart("updateArea");
            updateArea();
            Core.profileEnd();
        }
        
        if (!worldObj.isRemote) {
            //Do teleportations and stuff
            if (shadowArea == null) {
                updateArea();
            }
            if (shadowArea == null) {
                if (hammerCell.blockExists()) {
                    setDead();
                    Core.logFine("%s dying due to empty area", this.toString());
                } else {
                    needAreaUpdate = true; //sometime soon, please
                }
            } else {
                Coord corner = Hammer.getCellCorner(worldObj, cell);
                takeInteriorEntities();
                removeExteriorEntities();
            }
            if (isDead) {
                endSlice();
                return;
            }
        }
        //Do collisions...? :(
    }
    
    @Override
    public void onEntityUpdate() {
        //We don't want to call super, because it does a bunch of stuff that makes no sense for us.
        Core.profileStart("FZDSE");
        doUpdate();
        Core.profileEnd();
    }
    
    private void takeInteriorEntities() {
        //Move entities inside our bounds in the real world into the shadow world
        List<Entity> realEntities = worldObj.getEntitiesWithinAABB(Entity.class, realArea); //
        for (int i = 0; i < realEntities.size(); i++) {
            Entity ent = realEntities.get(i);
            if (ent == this) {
                continue;
            }
            takeEntity(ent);
        }
    }
    
    private void removeExteriorEntities() {
        //Move entities outside the bounds in the shadow world into the real world
        Chunk[] mychunks = Hammer.getChunks(worldObj, cell);
        for (int i = 0; i < mychunks.length; i++) {
            Chunk chunk = mychunks[i];
            for (int j = 0; j < chunk.entityLists.length; j++) {
                List<Entity> l = chunk.entityLists[j];
                for (int k = 0; k < l.size(); k++) {
                    Entity ent = l.get(k); //This is probably an ArrayList.
                    if (ent.posY < 0 || ent.posY > Hammer.wallHeight || ent == this /* oh god what */) {
                        continue;
                    }
                    AxisAlignedBB bb = ent.boundingBox;
                    if (bb != null && !shadowArea.intersectsWith(bb)) {
                        ejectEntity(ent);
                    }
                }
            }
        }
    }
    
    boolean forbidEntityToEnter(Entity ent) {
        if (ent.timeUntilPortal > 0) {
            return true;
        }
        return ent instanceof EntityPlayer /* Just for now */;
    }
    
    boolean forceKeepEntityInside(Entity ent) {
        if (ent.timeUntilPortal > 0) {
            return true;
        }
        return ent instanceof EntityPlayer /* Just for now */;
    }
    
    void takeEntity(Entity ent) {
        //TODO: Take transformations into account
        if (forbidEntityToEnter(ent)) {
            return;
        }
        IFzdsEntryControl ifec = null;
        if (ent instanceof IFzdsEntryControl) {
            ifec = (IFzdsEntryControl) ent;
            if (!ifec.canEnter(this)) {
                return;
            }
        }
        World shadowWorld = Hammer.getServerShadowWorld();
        Vec3 newLocation = real2shadow(Hammer.ent2vec(ent));
        transferEntity(ent, shadowWorld, newLocation);
        if (ifec != null) {
            ifec.onEnter(this);
        }
    }
    
    void ejectEntity(Entity ent) {
        //TODO: Take transformations into account
        if (forceKeepEntityInside(ent)) {
            return;
        }
        IFzdsEntryControl ifec = null;
        if (ent instanceof IFzdsEntryControl) {
            ifec = (IFzdsEntryControl) ent;
            if (!ifec.canExit(this)) {
                return;
            }
        }
        Vec3 newLocation = shadow2real(Hammer.ent2vec(ent));
        transferEntity(ent, worldObj, newLocation);
        if (ifec != null) {
            ifec.onExit(this);
        }
    }
    
    void transferEntity(Entity ent, World newWorld, Vec3 newPosition) {
        //Inspired by Entity.travelToDimension
        ent.worldObj.setEntityDead(ent);
        ent.isDead = false;
        
        Entity phoenix = EntityList.createEntityByName(EntityList.getEntityString(ent), newWorld); //Like a phoenix rising from the ashes!
        if (phoenix == null) {
            return; //Or not.
        }
        phoenix.copyDataFrom(ent, true);
        phoenix.timeUntilPortal = phoenix.getPortalCooldown();
        ent.isDead = true;
        phoenix.setPosition(newPosition.xCoord, newPosition.yCoord, newPosition.zCoord);
        newWorld.spawnEntityInWorld(phoenix);
    }
    
    void endSlice() {
        Hammer.getSlices(worldObj).remove(this);
        //TODO: teleport entities/blocks into the real world
    }
    
    @Override
    public void setDead() {
        super.setDead();
        Hammer.getSlices(worldObj).remove(this);
    }
    
    @Override
    public boolean isInRangeToRenderDist(double par1) {
        //TODO NORELEASE: Common sense
        return true;
    }
    
    @Override
    public boolean canEnter(DimensionSliceEntity dse) { return false; }
    
    @Override
    public boolean canExit(DimensionSliceEntity dse) { return true; }
    
    @Override
    public void onEnter(DimensionSliceEntity dse) { }
    
    @Override
    public void onExit(DimensionSliceEntity dse) { }

    void writeAABB(ByteArrayDataOutput data, AxisAlignedBB bb) {
        data.writeDouble(bb.maxX);
        data.writeDouble(bb.maxY);
        data.writeDouble(bb.maxZ);
        data.writeDouble(bb.minX);
        data.writeDouble(bb.minY);
        data.writeDouble(bb.minZ);
    }
    
    AxisAlignedBB readAABB(ByteArrayDataInput data) {
        double maxX = data.readDouble();
        double maxY = data.readDouble();
        double maxZ = data.readDouble();
        double minX = data.readDouble();
        double minY = data.readDouble();
        double minZ = data.readDouble();
        return AxisAlignedBB.getBoundingBox(minX, minY, minZ, maxX, maxY, maxZ);
    }
    
    
    @Override
    public void writeSpawnData(ByteArrayDataOutput data) {
        data.writeInt(cell);
    }

    @Override
    public void readSpawnData(ByteArrayDataInput data) {
        cell = data.readInt();
    }
}
