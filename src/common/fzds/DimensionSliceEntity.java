package factorization.fzds;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import factorization.api.Coord;
import factorization.fzds.api.IFzdsEntryControl;

public class DimensionSliceEntity extends Entity implements IFzdsEntryControl {
    int cell;
    
    public Coord hammerCell;
    Object renderInfo = null;
    AxisAlignedBB shadowArea = null, shadowCollisionArea = null, realArea = null, realCollisionArea = null;
    PacketProxyingPlayer proxy = null;
    
    public DimensionSliceEntity(World world) {
        super(world);
        ignoreFrustumCheck = true; //kinda lame; we should give ourselves a proper bounding box?
    }
    
    public DimensionSliceEntity(World world, int cell) {
        this(world);
        this.cell = cell;
        this.hammerCell = Hammer.getCellCorner(world, cell);
    }
    
    private static Vec3 buffer = Vec3.createVectorHelper(0, 0, 0);
    
    public Vec3 real2shadow(Vec3 realCoords) {
        //NOTE: This ignores transformations! Need to fix!
        double diffX = realCoords.xCoord - posX;
        double diffY = realCoords.yCoord - posY;
        double diffZ = realCoords.zCoord - posZ;
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
        buffer.xCoord = posX + diffX;
        buffer.yCoord = posY + diffY;
        buffer.zCoord = posZ + diffZ;
        return buffer;
    }
    
    @Override
    protected void entityInit() {
        // TODO Auto-generated method stub

    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        cell = tag.getInteger("cell");
        
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        //this.ticksExisted = 1;
        tag.setInteger("cell", cell);
    }
    
    @Override
    public boolean canBeCollidedWith() {
        return !this.isDead;
    }
    
    public void updateArea() {
        Coord c = this.hammerCell;
        Coord d = Hammer.getCellOppositeCorner(worldObj, this.cell);
        AxisAlignedBB fullRange = AxisAlignedBB.getBoundingBox(c.x, c.y, c.z, d.x, d.y, d.z);
        List<AxisAlignedBB> blockBoxes = c.w.getAllCollidingBoundingBoxes(fullRange);
        if (blockBoxes.size() <= 0) {
            shadowArea = null;
            shadowCollisionArea = null;
            return;
        }
        AxisAlignedBB start = blockBoxes.get(0);
        for (int i = 1; i < blockBoxes.size(); i++) {
            AxisAlignedBB b = blockBoxes.get(i);
            start.minX = Math.min(start.minX, b.minX);
            start.minY = Math.min(start.minY, b.minY);
            start.minZ = Math.min(start.minZ, b.minZ);
            start.maxX = Math.max(start.maxX, b.maxX);
            start.maxY = Math.max(start.maxY, b.maxY);
            start.maxZ = Math.max(start.maxZ, b.maxZ);
        }
        shadowArea = start.copy();
        shadowCollisionArea = shadowArea.expand(2, 2, 2);
        realArea = shadowArea.copy().getOffsetBoundingBox(posX - c.x, posY - c.z, posZ - c.z); //NOTE: Will need to update realArea when we move
        realCollisionArea = shadowCollisionArea.copy().getOffsetBoundingBox(posX - c.x, posY - c.z, posZ - c.z);
    }
    
    void init() {
        if (hammerCell == null) {
            this.hammerCell = Hammer.getCellCorner(worldObj, cell);
        }
        Hammer.getSlices(worldObj).add(this);
        updateArea();
    }
    
    @Override
    public void onEntityUpdate() {
        //We don't want to call super, because it does a bunch of stuff that makes no sense for us.
        if (worldObj.isRemote) {
            if (hammerCell == null) {
                init();
            }
        } else {
            if (proxy == null && !isDead) {
                init();
                proxy = new PacketProxyingPlayer(this);
                proxy.worldObj.spawnEntityInWorld(proxy);
                return;
            }
        }
        
        if (!worldObj.isRemote) {
            //Do teleportations and stuff
            if (shadowArea == null) {
                updateArea();
            }
            if (shadowArea == null) {
                setDead();
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
}
