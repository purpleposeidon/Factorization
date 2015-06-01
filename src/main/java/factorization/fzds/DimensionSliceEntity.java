package factorization.fzds;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.internal.FMLProxyPacket;
import factorization.aabbdebug.AabbDebugger;
import factorization.api.Coord;
import factorization.api.DeltaCoord;
import factorization.api.ICoordFunction;
import factorization.api.Quaternion;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.Share;
import factorization.common.FzConfig;
import factorization.coremodhooks.IExtraChunkData;
import factorization.coremodhooks.IKinematicTracker;
import factorization.fzds.interfaces.*;
import factorization.fzds.network.HammerNet;
import factorization.fzds.network.PacketProxyingPlayer;
import factorization.shared.Core;
import factorization.shared.EntityReference;
import factorization.shared.TortoiseAndHare;
import factorization.util.SpaceUtil;
import net.minecraft.block.Block;
import net.minecraft.command.IEntitySelector;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import org.apache.commons.lang3.ArrayUtils;

import java.io.IOException;
import java.util.*;

public class DimensionSliceEntity extends IDeltaChunk implements IFzdsEntryControl {
    //Dang, this class is a mess! Code folding, activate!
    
    private Coord cornerMin = Coord.ZERO.copy();
    private Coord cornerMax = Coord.ZERO.copy();
    private Vec3 centerOffset = Vec3.createVectorHelper(0, 0, 0);
    
    private final EntityReference<DimensionSliceEntity> parent; // init in constructor ._.
    private Vec3 offsetFromParent = Vec3.createVectorHelper(0, 0, 0);
    private transient ArrayList<IDeltaChunk> children = new ArrayList<IDeltaChunk>(0);
    
    private long capabilities = DeltaCapability.of(DeltaCapability.MOVE, DeltaCapability.COLLIDE, DeltaCapability.DRAG, DeltaCapability.REMOVE_ITEM_ENTITIES);
    
    AxisAlignedBB realArea = makeAABB();
    MetaAxisAlignedBB metaAABB = null;
    
    private AxisAlignedBB shadowArea = null, realDragArea = null;
    private boolean needAreaUpdate = true;
    private double last_motion_hash = Double.NaN;
    
    private Quaternion rotation = new Quaternion(), rotationalVelocity = new Quaternion();
    private Quaternion last_shared_rotation = new Quaternion(), last_shared_rotational_velocity = new Quaternion(); //used on the server
    Quaternion prevTickRotation = new Quaternion(); //Client-side
    private double last_shared_posX = -99, last_shared_posY = -99, last_shared_posZ = -99;
    private double last_shared_motionX = 0, last_shared_motionY = 0, last_shared_motionZ = 0;
    private Quaternion rotationStart = new Quaternion(), rotationEnd = new Quaternion();
    private long orderTimeStart = -1, orderTimeEnd = -1;
    private Interpolation orderInterp = Interpolation.CONSTANT;
    
    float scale = 1;
    float opacity = 1;
    
    Object renderInfo = null; //Client-side
    
    Entity packetRelay = null;
    HashSet<IExtraChunkData> registered_chunks = new HashSet<IExtraChunkData>();
    UniversalCollider universalCollider;
    
    public DimensionSliceEntity(World world) {
        super(world);
        if (world == DeltaChunk.getWorld(world)) {
            Core.logWarning("Aborting attempt to spawn DSE in Hammerspace");
            setDead();
        }
        universalCollider = new UniversalCollider(this, world);
        parent = new EntityReference<DimensionSliceEntity>(world);
    }
    
    public DimensionSliceEntity(World world, Coord lowerCorner, Coord upperCorner) {
        this(world);
        setCorners(lowerCorner, upperCorner);
    }
    
    private void setCorners(Coord lowerCorner, Coord upperCorner) {
        if (lowerCorner.w != DeltaChunk.getWorld(worldObj)) {
            if (!can(DeltaCapability.ORACLE) || lowerCorner.w != worldObj) {
                throw new IllegalArgumentException("My corners are not shadow!");
            }
        }
        Coord.sort(lowerCorner, upperCorner);
        this.cornerMin = lowerCorner;
        this.cornerMax = upperCorner;
        DeltaCoord dc = upperCorner.difference(lowerCorner);
        centerOffset = Vec3.createVectorHelper(
                dc.x/2,
                dc.y/2,
                dc.z/2);
    }
    
    @Override
    public final Vec3 real2shadow(final Vec3 realVector) {
        // rotate⁻¹(real - DSE) + centerOffset + corner = shadow
        Vec3 buffer = Vec3.createVectorHelper(0, 0, 0);
        real2shadow(realVector, buffer);
        return buffer;
    }
    
   public final void real2shadow(final Vec3 realVector, final Vec3 buffer) {
        // rotate⁻¹(real - DSE) + centerOffset + corner = shadow
        buffer.xCoord = realVector.xCoord - posX;
        buffer.yCoord = realVector.yCoord - posY;
        buffer.zCoord = realVector.zCoord - posZ;
        
        rotation.applyReverseRotation(buffer);
        
        buffer.xCoord += cornerMin.x + centerOffset.xCoord;
        buffer.yCoord += cornerMin.y + centerOffset.yCoord;
        buffer.zCoord += cornerMin.z + centerOffset.zCoord;
    }
    
    @Override
    public final Vec3 shadow2real(final Vec3 shadowVector) {
        // rotate(shadow - corner - centerOffset) + DSE = real
        Vec3 buffer = Vec3.createVectorHelper(0, 0, 0);
        buffer.xCoord = shadowVector.xCoord - cornerMin.x - centerOffset.xCoord;
        buffer.yCoord = shadowVector.yCoord - cornerMin.y - centerOffset.yCoord;
        buffer.zCoord = shadowVector.zCoord - cornerMin.z - centerOffset.zCoord;
        
        rotation.applyRotation(buffer);
        
        buffer.xCoord += posX;
        buffer.yCoord += posY;
        buffer.zCoord += posZ;
        return buffer;
    }

    @Override
    public AxisAlignedBB shadow2real(AxisAlignedBB shadowBox) {
        Vec3 min = SpaceUtil.getMin(shadowBox);
        Vec3 max = SpaceUtil.getMax(shadowBox);
        return SpaceUtil.createAABB(shadow2real(min), shadow2real(max));
    }

    private Vec3 workVec = Vec3.createVectorHelper(0, 0, 0);
    
    @Override
    public void shadow2real(Coord c) {
        double d = 1.0;
        workVec.xCoord = c.x + d;
        workVec.yCoord = c.y + d;
        workVec.zCoord = c.z + d;
        workVec = shadow2real(workVec);
        c.set(workVec);
        c.w = worldObj;
    }
    
    @Override
    public void real2shadow(Coord c) {
        c.set(real2shadow(c.createVector()));
        c.w = DeltaChunk.getWorld(worldObj);
    }

    @Override
    public AxisAlignedBB real2shadow(AxisAlignedBB shadowBox) {
        Vec3 min = SpaceUtil.getMin(shadowBox);
        Vec3 max = SpaceUtil.getMax(shadowBox);
        return SpaceUtil.createAABB(real2shadow(min), real2shadow(max));
    }
    
    @Override
    public Coord getCorner() {
        return cornerMin;
    }
    
    @Override
    public Coord getFarCorner() {
        return cornerMax;
    }
    
    @Override
    public AxisAlignedBB getBoundingBox() {
        return null; // universalCollider handles collisions.
    }
    
    @Override
    public void onCollideWithPlayer(EntityPlayer player) {
        //Maybe adjust our velocities?
    }
    
    @Override
    protected void entityInit() {}
    
    @Override
    protected void putData(DataHelper data) throws IOException {
        capabilities = data.as(Share.VISIBLE, "cap").putLong(capabilities);
        rotation = data.as(Share.VISIBLE, "r").put(rotation);
        rotationalVelocity = data.as(Share.VISIBLE, "w").put(rotationalVelocity);
        centerOffset = data.as(Share.VISIBLE, "co").putVec3(centerOffset);
        cornerMin = data.as(Share.VISIBLE, "min").put(cornerMin);
        cornerMax = data.as(Share.VISIBLE, "max").put(cornerMax);
        partName = data.as(Share.VISIBLE, "partName").putString(partName);
        if (can(DeltaCapability.SCALE)) {
            scale = data.as(Share.VISIBLE, "incrScale").putFloat(scale);
        }
        if (can(DeltaCapability.TRANSPARENT)) {
            opacity = data.as(Share.VISIBLE, "opacity").putFloat(opacity);
        }
        if (data.isReader()) {
            if (worldObj.isRemote) {
                DeltaChunk.getSlices(worldObj).add(this);
                cornerMax.w = cornerMin.w = DeltaChunk.getClientShadowWorld();
            } else if (data.isNBT()) {
                initCorners();
            }
        }
        /*parent =*/ data.as(Share.VISIBLE, "parent").put(parent);
        offsetFromParent = data.as(Share.VISIBLE, "parentOffset").putVec3(offsetFromParent);
        entityUniqueID = data.as(Share.VISIBLE, "entityUUID").putUUID(entityUniqueID);
        
        rotationStart = data.as(Share.VISIBLE, "rotStart").put(rotationStart);
        rotationEnd = data.as(Share.VISIBLE, "rotEnd").put(rotationEnd);
        orderTimeStart = data.as(Share.VISIBLE, "rotOrdStart").putLong(orderTimeStart);
        orderTimeEnd = data.as(Share.VISIBLE, "rotOrdEnd").putLong(orderTimeEnd);
        orderInterp = data.as(Share.VISIBLE, "orderInterp").putEnum(orderInterp);

        if (data.isReader() && data.isNBT()) {
            rotation = rotation.cleanAbnormalNumbers();
            rotationalVelocity = rotationalVelocity.cleanAbnormalNumbers();
        }
    }
    
    @Override
    public boolean canBeCollidedWith() {
        return false;
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
    
    @Override
    public IDeltaChunk getParent() {
        return parent.getEntity();
    }
    
    @Override
    public Vec3 getParentJoint() {
        return offsetFromParent;
    }
    
    @Override
    public void setParent(IDeltaChunk _parent, Vec3 jointPositionAtParent) {
        if (null != TortoiseAndHare.race(this, new TortoiseAndHare.Advancer<IDeltaChunk>() {
            @Override
            public IDeltaChunk getNext(IDeltaChunk node) {
                return node.getParent();
            }
        })) {
            throw new IllegalArgumentException("Parenting loop!");
        }
        DimensionSliceEntity newParent = (DimensionSliceEntity) _parent;
        if (this.parent.trackingEntity()) {
            DimensionSliceEntity oldParent = parent.getEntity();
            if (oldParent != null) {
                oldParent.children.remove(this);
            }
        }
        this.parent.trackEntity(newParent);
        this.offsetFromParent = jointPositionAtParent;
        newParent.children.remove(this);
        newParent.children.add(this);
    }
    
    @Override
    public ArrayList<IDeltaChunk> getChildren() {
        return children;
    }
    
    private void updateRealArea() {
        Vec3[] corners = SpaceUtil.getCorners(shadowArea);
        Vec3 first = shadow2real(corners[0]);
        SpaceUtil.setAABB(realArea, first, first);
        for (int i = 1; i < corners.length; i++) {
            Vec3 v = corners[i];
            v = shadow2real(v);
            if (v.xCoord < realArea.minX) realArea.minX = v.xCoord;
            if (v.yCoord < realArea.minY) realArea.minY = v.yCoord;
            if (v.zCoord < realArea.minZ) realArea.minZ = v.zCoord;
            
            if (v.xCoord > realArea.maxX) realArea.maxX = v.xCoord;
            if (v.yCoord > realArea.maxY) realArea.maxY = v.yCoord;
            if (v.zCoord > realArea.maxZ) realArea.maxZ = v.zCoord;
        }
        
        this.boundingBox.setBB(realArea);
        if (metaAABB == null) metaAABB = new MetaAxisAlignedBB(this, cornerMin.w);
        metaAABB.setUnderlying(realArea);
        needAreaUpdate = false;
    }
    
    double last_uni_x = Double.NEGATIVE_INFINITY;
    double last_uni_z = Double.NEGATIVE_INFINITY;
    Quaternion last_uni_rot = null;
    boolean need_recheck = false;
    
    boolean significantMovement() {
        if (need_recheck) {
            need_recheck = false;
            return true;
        }
        if (ticksExisted <= 2) {
            return true;
        }
        // This counter doesn't get serialized, so we'll be sure to update the colliders

        double dx = Math.abs(last_uni_x - posX);
        double dz = Math.abs(last_uni_z - posZ);
        if (dx > 8 || dz > 8) {
            last_uni_x = posX;
            last_uni_z = posZ;
            return true;
        }
        if (!can(DeltaCapability.ROTATE)) return false;
        if (last_uni_rot == null) {
            last_uni_rot = new Quaternion(rotation);
            return true;
        }
        double angle_change = rotation.getAngleBetween(last_uni_rot);
        if (angle_change < Math.PI * 2 / cornerMin.distanceSq(cornerMax)) {
            last_uni_rot = new Quaternion(last_uni_rot);
            return true;
        }
        return false;
    }
    
    private void updateUniversalCollisions() {
        if (realArea == null) return;
        double last_x = last_uni_x;
        double last_z = last_uni_z;
        if (!significantMovement()) return;
        double d = 8;
        if (can(DeltaCapability.ROTATE)) {
            // Must enlarge by the worst-case rotation
            Coord min = getCorner(), max = getFarCorner();
            Vec3 center = real2shadow(SpaceUtil.fromEntPos(this));
            double sx = Math.max(center.xCoord - min.x, max.x - center.xCoord);
            double sy = Math.max(center.yCoord - min.y, max.y - center.yCoord);
            double sz = Math.max(center.zCoord - min.z, max.z - center.zCoord);
            double r = Math.sqrt(sx * sx + sy * sy + sz * sz);
            d += r;
        }
        double minX = realArea.minX - d;
        double maxX = realArea.maxX + d;
        double minZ = realArea.minZ - d;
        double maxZ = realArea.maxZ + d;
        // Check nearby areas
        HashSet<IExtraChunkData> toDeregister = new HashSet<IExtraChunkData>(registered_chunks.size());
        toDeregister.addAll(registered_chunks);
        for (double x = minX; x <= maxX; x += 16) {
            for (double z = minZ; z <= maxZ; z += 16) {
                check_chunk(x, z, toDeregister);
            }
        }
        deregisterUCs(toDeregister);
    }
    
    private void deregisterUniversalCollisionsForDeath() {
        deregisterUCs(registered_chunks);
        registered_chunks.clear();
    }

    private void deregisterUCs(HashSet<IExtraChunkData> old) {
        for (IExtraChunkData chunk : old) {
            Entity[] colliders = chunk.getConstantColliders();
            if (colliders == null || colliders.length == 1) {
                colliders = null;
            } else {
                colliders = ArrayUtils.removeElement(colliders, universalCollider);
            }
            chunk.setConstantColliders(colliders);
        }
    }
    
    private void check_chunk(double x, double z, HashSet<IExtraChunkData> toDeregister) {
        if (isDead) return;
        int ix = (int) x;
        int iz = (int) z;
        if (!worldObj.blockExists(ix, 64, iz)) return;
        Chunk mc_chunk = worldObj.getChunkFromBlockCoords(ix, iz);
        if (mc_chunk == null) return;
        if (mc_chunk.isEmpty() && worldObj.isRemote) need_recheck = true;
        IExtraChunkData chunk = (IExtraChunkData) mc_chunk;
        Entity[] colliders = chunk.getConstantColliders();
        boolean is_registered = ArrayUtils.contains(colliders, universalCollider);

        toDeregister.remove(chunk);
        if (!is_registered) {
            colliders = ArrayUtils.add(colliders, universalCollider);
            registered_chunks.add(chunk);
            chunk.setConstantColliders(colliders);
        }
    }
    
    
    private void updateShadowArea() {
        Coord c = getCorner();
        Coord d = getFarCorner();
        AxisAlignedBB start = null;
        // NORELEASE omfg slow!
        World w = c.w;
        for (int x = c.x; x <= d.x; x++) {
            for (int y = c.y; y <= d.y; y++) {
                for (int z = c.z; z <= d.z; z++) {
                    Block block = c.w.getBlock(x, y, z);
                    if (block.isAir(c.w, x, y, z)) {
                        continue;
                    }
                    if (start == null) {
                        start = AxisAlignedBB.getBoundingBox(x, y, z, x + 1, y + 1, z + 1);
                    } else {
                        start.minX = Math.min(start.minX, x);
                        start.minY = Math.min(start.minY, y);
                        start.minZ = Math.min(start.minZ, z);
                        start.maxX = Math.max(start.maxX, x + 1);
                        start.maxY = Math.max(start.maxY, y + 1);
                        start.maxZ = Math.max(start.maxZ, z + 1);
                    }
                }
            }
        }
        if (start == null) {
            if (worldObj.isRemote) {
                return;
            }
            if (can(DeltaCapability.DIE_WHEN_EMPTY)) {
                Core.logInfo("IDC requests deletion when empty, and is empty: %s", this);
                setDead();
                return;
            }
            shadowArea = makeAABB();
            return;
        }
        
        shadowArea = cloneAABB(start);
        updateRealArea();
        updateUniversalCollisions();
    }
    
    public void blocksChanged(int x, int y, int z) {
        if (shadowArea == null) {
            needAreaUpdate = true;
            return;
        }
        needAreaUpdate |= x <= shadowArea.minX || y <= shadowArea.minY || z <= shadowArea.minZ
                || x >= shadowArea.maxX || y >= shadowArea.maxY || z >= shadowArea.maxZ;
    }
    
    @Override
    public void setPosition(double par1, double par3, double par5) {
        super.setPosition(par1, par3, par5);
        needAreaUpdate = true;
    }
    
    boolean updateHashMotion() {
        double hash = motionX*10 + motionY*1010 + motionZ*101010;
        if (hash == last_motion_hash) {
            return false;
        }
        last_motion_hash = hash;
        return true;
    }
    
    private static final IEntitySelector excludeDseRelatedEntities = new IEntitySelector() {
        @Override
        public boolean isEntityApplicable(Entity entity) {
            Class entClass = entity.getClass();
            if (entClass == DimensionSliceEntity.class) return false;
            return entClass != UniversalCollider.class;
        }
    };
    
    private static DamageSource violenceDamage = new DamageSource("dseHit");
    
    public Vec3 getInstantaneousRotationalVelocityAtPointInCornerSpace(Vec3 corner) {
        Vec3 origPoint = SpaceUtil.subtract(centerOffset, corner);
        rotation.applyRotation(origPoint);
        Vec3 rotatedPoint = SpaceUtil.copy(origPoint);
        rotationalVelocity.applyRotation(rotatedPoint);
        return SpaceUtil.subtract(origPoint, rotatedPoint);
    }
    
    private boolean hasLinearMotion() {
        return motionX != 0 || motionY != 0 || motionZ != 0;
    }
    
    private boolean hasRotationalMotion() {
        return !rotationalVelocity.isZero() || hasOrderedRotation();
    }
    
    void updateMotion(Vec3 parentTickDisp, Quaternion parentTickRotation) {
        if (metaAABB == null) {
            return;
        }
        if (hasOrderedRotation() && orderTimeEnd <= worldObj.getTotalWorldTime()) {
            if (can(DeltaCapability.SNAP_TO_EXACT_ORDERED_ROTATION)) {
                Quaternion trueRot = new Quaternion(rotationEnd);
                IDeltaChunk here = this.getParent();
                while (here != null) {
                    here.getRotation().incrToOtherMultiply(trueRot);
                    here = here.getParent();
                }
                setRotation(trueRot);
            }
            cancelOrderedRotation();
        }
        final boolean parentRotation = !parentTickRotation.isZero();
        final boolean linearMotion = parentRotation || !SpaceUtil.isZero(parentTickDisp) || hasLinearMotion();
        final boolean rotationalMotion = parentRotation || hasRotationalMotion();
        
        Vec3 mot = null;
        Quaternion rot = null;
        boolean moved = false;

        double prevX = posX, prevY = posY, prevZ = posZ;
        Quaternion prevRotation = null;

        if (linearMotion) {
            mot = parentTickDisp.addVector(motionX, motionY, motionZ);
            if (realDragArea == null || updateHashMotion() || rotationalMotion) {
                realDragArea = realArea.addCoord(mot.xCoord, mot.yCoord, mot.zCoord);
            }
            if (mot.xCoord != 0 || mot.yCoord != 0 || mot.zCoord != 0) {
                posX += mot.xCoord;
                posY += mot.yCoord;
                posZ += mot.zCoord;
                moved = true;
            }
        } else {
            mot = parentTickDisp;
        }
        
        if (rotationalMotion) {
            if (hasOrderedRotation()) {
                long now = worldObj.getTotalWorldTime();
                Quaternion r0 = getOrderedRotation(now);
                Quaternion r1 = getOrderedRotation(now + 1 /* or -1 on the other? I think this is the right way tho. */);
                r0.incrConjugate();
                r1.incrMultiply(r0);
                rot = r1;
            } else {
                prevRotation = new Quaternion(rotation);
                rot = new Quaternion(rotationalVelocity);
            }
            if (parentRotation) {
                parentTickRotation.incrToOtherMultiply(rot);
                //rot.incrMultiply(parentTickRotation);
            }
            
            if (!rot.isZero()) {
                rot.incrToOtherMultiply(rotation);
                //Or....?
                //rotation.incrMultiply(rot);
                rotation.incrNormalize(); // Prevent the accumulation of error
                moved = true;
            }
        } else {
            rot = parentTickRotation;
        }
        last_shared_rotation.incrMultiply(last_shared_rotational_velocity);

        if (moved && !noClip && can(DeltaCapability.COLLIDE_WITH_WORLD)) {
            // NORELEASE TODO This is too slow. Fuck it. Let's bust out a proper physics library.
            // Use an IWorldAccess to synchronize collision areas. We'll need to do this for both Real and Shadow, yeah?
            // MetaAABB will defer to this physics library as well
            List<AxisAlignedBB> collisions = worldObj.getCollidingBoundingBoxes(this, realArea);
            AxisAlignedBB collision = null;
            for (int i = 0; i < collisions.size(); i++) {
                AxisAlignedBB solid = collisions.get(i);
                if (solid.getClass() != AxisAlignedBB.class) continue;
                if (metaAABB.intersectsWith(solid)) {
                    collision = solid;
                    break;
                }
            }
            if (collision != null) {
                // XXX TODO: This is lame; should at least iterate closer (or do it properly)
                if (mot != null) {
                    posX = prevX;
                    posY = prevY;
                    posZ = prevZ;
                }
                if (prevRotation != null) {
                    setRotation(prevRotation);
                }
                setVelocity(0, 0, 0);
                rotationalVelocity.update(1, 0, 0, 0);
                moved = false;
            }
        }
        if (moved && can(DeltaCapability.DRAG)) {
            List ents = worldObj.getEntitiesWithinAABBExcludingEntity(this, metaAABB, excludeDseRelatedEntities);
            float dyaw = 0;
            dyaw = (float) Math.toDegrees(-rot.toRotationVector().yCoord);
            if (Float.isNaN(dyaw)) dyaw = 0;
            long now = worldObj.getTotalWorldTime() + 100 /* Hack around MixinEntityKinematicsTracker.kinematics_last_change not being initialized */;
            
            for (int i = 0; i < ents.size(); i++) {
                Entity e = (Entity) ents.get(i);
                AxisAlignedBB ebb = e.boundingBox;
                double expansion = 0;
                if (mot != null) {
                    double friction_expansion = 0.05 * mot.lengthVector();
                    if (mot.yCoord > 0) {
                        ebb = ebb.expand(-mot.xCoord, -mot.yCoord, -mot.zCoord);
                    }
                    if (mot.xCoord != 0 || mot.zCoord != 0) {
                        expansion = friction_expansion;
                    }
                }
                if (expansion < 0.1) {
                    expansion = 0.1;
                }
                if (expansion != 0) {
                    ebb = ebb.expand(expansion, expansion, expansion);
                }
                // could multiply stuff by velocity
                if (!metaAABB.intersectsWith(ebb)) {
                    // NORELEASE metaAABB.intersectsWith is very slow, especially with lots of entities
                    continue;
                }
                
                if (can(DeltaCapability.ENTITY_PHYSICS)) {
                    IKinematicTracker kine = (IKinematicTracker) e;
                    kine.reset(now);
                    double instant_scale = 1;
                    double motion_scale = 1;
                    double vel_scale = 1;
                    Vec3 entityAt = SpaceUtil.fromEntPos(e);
                    Vec3 velocity = calcInstantVelocityAtRealPoint(entityAt, mot, rot);
                    if (can(DeltaCapability.VIOLENT_COLLISIONS) && !worldObj.isRemote) {
                        double smackSpeed = velocity.lengthVector();
                        vel_scale = 1;
                        if (e instanceof EntityLivingBase) {
                            if (smackSpeed > 0.05) {
                                EntityLivingBase el = (EntityLivingBase) e;
                                el.attackEntityFrom(violenceDamage, (float) (20 * smackSpeed));
                                Vec3 emo = velocity.normalize();
                                e.motionX += emo.xCoord * vel_scale;
                                e.motionY += emo.yCoord * vel_scale;
                                e.motionZ += emo.zCoord * vel_scale;
                            }
                        }
                    }
                    velocity.xCoord *= instant_scale;
                    velocity.yCoord *= instant_scale;
                    velocity.zCoord *= instant_scale;
                    velocity.xCoord = clipVelocity(velocity.xCoord*motion_scale, e.motionX);
                    velocity.yCoord = clipVelocity(velocity.yCoord*motion_scale, e.motionY);
                    velocity.zCoord = clipVelocity(velocity.zCoord*motion_scale, e.motionZ);
                    e.moveEntity(velocity.xCoord, velocity.yCoord, velocity.zCoord);
                    // Hrm. Is it needed or not? Seems to cause jitterings with it on
                    //e.prevPosX += velocity.xCoord;
                    //e.prevPosY += velocity.yCoord;
                    //e.prevPosZ += velocity.zCoord;
                    // TODO FIXME: Jittering rotation when the player is standing on top! Argh! PLEASE FIX!
                    double origYaw = e.rotationYaw;
                    e.rotationYaw = (float) addLimitedDelta(kine.getKinematics_yaw(), e.rotationYaw, dyaw);
                    double yd = e.rotationYaw - origYaw;
                    e.prevRotationYaw += yd;
                } else if (mot != null) {
                    e.moveEntity(mot.xCoord, mot.yCoord, mot.zCoord);
                    
                    if (mot.yCoord > 0 && e.motionY < mot.yCoord) {
                        e.motionY = mot.yCoord;
                        e.fallDistance += (float) Math.abs(mot.yCoord - e.motionY);
                    }
                }
                e.onGround = true;
            }
            updateRealArea();
        }
        if (linearMotion || rotationalMotion) {
            updateUniversalCollisions();
        }
        Vec3 childAt = Vec3.createVectorHelper(0, 0, 0);
        for (Iterator<IDeltaChunk> iterator = children.iterator(); iterator.hasNext();) {
            DimensionSliceEntity child = (DimensionSliceEntity) iterator.next();
            if (child.isDead) {
                iterator.remove();
                continue;
            }
            childAt.xCoord = child.posX;
            childAt.yCoord = child.posY;
            childAt.zCoord = child.posZ;
            Vec3 inst = getInstRotVel(childAt, rot);
            SpaceUtil.incrAdd(inst, mot);
            child.updateMotion(inst, rot);
        }
    }
    
    public Vec3 getInstRotVel(Vec3 real, Quaternion rot) {
        Vec3 dse_space = real.addVector(-posX, -posY, -posZ); // Errm, center offset?
        Vec3 point_a = dse_space;
        Vec3 point_b = dse_space.addVector(0, 0, 0);
        rot.applyRotation(point_b);
        return SpaceUtil.subtract(point_b, point_a);
    }
    
    Vec3 point_a = SpaceUtil.newVec(), point_b = SpaceUtil.newVec();
    Vec3 calcInstantVelocityAtRealPoint(Vec3 realPos, Vec3 linear, Quaternion rot) {
        // FIXME center offset? See real2shadow probably
        point_a.xCoord = realPos.xCoord - posX;
        point_a.yCoord = realPos.yCoord - posY;
        point_a.zCoord = realPos.zCoord - posZ;
        point_b = SpaceUtil.set(point_b, point_a);
        rot.applyRotation(point_b);
        Vec3 rotational = SpaceUtil.incrSubtract(point_b, point_a);
        return SpaceUtil.incrAdd(rotational, linear);
    }

    public Vec3 getInstantaneousVelocity(Vec3 realPos) {
        Quaternion rot = new Quaternion(getRotationalVelocity());
        DimensionSliceEntity here = this;
        while (true) {
            DimensionSliceEntity next = (DimensionSliceEntity) here.getParent();
            if (next == null) break;
            here = next;
            rot.incrMultiply(here.getRotationalVelocity());
        }
        Vec3 linear = Vec3.createVectorHelper(here.motionX, here.motionY, here.motionZ);
        return calcInstantVelocityAtRealPoint(realPos, linear, rot);
    }
    
    /**
     * If the player is standing on two platforms moving in the same direction, then the natural behavior is for the player to move twice as fast.
     */
    double addLimitedDelta(double prevVal, double currentVal, double delta) {
        if (delta == 0) return currentVal;
        double oldDelta = currentVal - prevVal;
        if (oldDelta != 0 && Math.signum(oldDelta) != Math.signum(delta)) return currentVal; // First DSE wins
        if (delta > 0) {
            return prevVal + Math.max(delta, oldDelta);
        } else {
            return prevVal + Math.min(delta, oldDelta);
        }
    }
    
    double clipVelocity(double impulse_velocity, double current_velocity) {
        if (impulse_velocity < 0) {
            return Math.min(impulse_velocity, current_velocity);
        } else if (impulse_velocity > 0) {
            return Math.max(impulse_velocity, current_velocity);
        } else {
            return current_velocity;
        }
    }
    
    static final int force_sync_time = 20 * 4;
    
    void shareRotationInfo() {
        boolean d0 = !rotation.equals(last_shared_rotation), d1 = !rotationalVelocity.equals(last_shared_rotational_velocity);
        if (d1) d0 = true;
        if (parent.trackingEntity()) {
            d0 = false;
        }
        FMLProxyPacket toSend = null;
        if ((d0 && d1) || (ticksExisted % force_sync_time == 0)) {
            toSend = HammerNet.makePacket(HammerNet.HammerNetType.rotationBoth, getEntityId(), rotation, rotationalVelocity);
            last_shared_rotation.update(rotation);
            last_shared_rotational_velocity.update(rotationalVelocity);
        } else if (d0) {
            toSend = HammerNet.makePacket(HammerNet.HammerNetType.rotation, getEntityId(), rotation);
            last_shared_rotation.update(rotation);
        } else if (d1) {
            toSend = HammerNet.makePacket(HammerNet.HammerNetType.rotationVelocity, getEntityId(), rotationalVelocity);
            last_shared_rotational_velocity.update(rotationalVelocity);
        }
        if (toSend != null) {
            broadcastPacket(toSend);
        }
    }
    
    void shareDisplacementInfo() {
        last_shared_posX += last_shared_motionX;
        last_shared_posY += last_shared_motionY;
        last_shared_posZ += last_shared_motionZ;
        boolean share_displacement = (last_shared_posX != posX) || (last_shared_posY != posY) || (last_shared_posZ != posZ);
        boolean share_velocity = (last_shared_motionX != motionX) || (last_shared_motionY != motionY) || (last_shared_motionZ != motionZ);
        share_displacement |= ticksExisted % force_sync_time == 0;
        if (!(share_displacement || share_velocity)) {
            return;
        }
        // Vanilla's packets don't give enough precision. We need ALL of the precision.
        FMLProxyPacket toSend = HammerNet.makePacket(HammerNet.HammerNetType.exactPositionAndMotion, getEntityId(), posX, posY, posZ, motionX, motionY, motionZ);
        broadcastPacket(toSend);
        
        last_shared_posX = posX;
        last_shared_posY = posY;
        last_shared_posZ = posZ;
        last_shared_motionX = motionX;
        last_shared_motionY = motionY;
        last_shared_motionZ = motionZ;
    }
    
    void debugCollisions() {
        if (!FzConfig.debug_fzds_collisions) return;
        if (this.metaAABB == null) return;

        Coord.iterateCube(getCorner(), getFarCorner(), new ICoordFunction() {
            @Override
            public void handle(Coord at) {
                if (at.isAir()) return;
                AxisAlignedBB box = at.getCollisionBoundingBoxFromPool();
                if (box == null) return;
                AabbDebugger.addBox(DimensionSliceEntity.this.metaAABB.convertShadowBoxToRealBox(box));
            }
        });
    }

    private void initCorners() {
        World target_world = can(DeltaCapability.ORACLE) ? worldObj : DeltaChunk.getServerShadowWorld();
        cornerMin.w = cornerMax.w = target_world;
    }
    
    @Override
    public void onEntityUpdate() {
        if (isDead) return;
        //We don't want to call super, because it does a bunch of stuff that makes no sense for us.
        prevTickRotation.update(rotation);
        prevPosX = posX;
        prevPosY = posY;
        prevPosZ = posZ;
        if (worldObj.isRemote) {
            rayOutOfDate = true;
            if (ticksExisted == 1) {
                DeltaChunk.getSlices(worldObj).add(this);
            }
        } else if (packetRelay == null) {
            boolean isOracle = can(DeltaCapability.ORACLE);
            initCorners();
            if (isOracle) {
                packetRelay = this;
            } else {
                DeltaChunk.getSlices(worldObj).add(this);
                World shadowWorld = DeltaChunk.getServerShadowWorld();
                packetRelay = new PacketProxyingPlayer(this, shadowWorld);
                packetRelay.worldObj.spawnEntityInWorld(packetRelay);
            }
            return;
        }
        if (ticksExisted % 60 == 0) {
            need_recheck = true;
            updateUniversalCollisions(); // TODO: Do it properly
        }
        Core.profileStart("FzdsDseTick");
        if (!worldObj.isRemote) controller.beforeUpdate(this);
        if (!parent.trackingEntity()) {
            Core.profileStart("updateMotion");
            updateMotion(Vec3.createVectorHelper(0, 0, 0), new Quaternion());
            Core.profileEnd();
        } else if (!parent.entityFound()) {
            IDeltaChunk p = parent.getEntity();
            if (p != null) {
                setParent(p, getParentJoint());
            }
        }
        if (!worldObj.isRemote) {
            controller.afterUpdate(this);
            shareDisplacementInfo();
            if (can(DeltaCapability.ROTATE)) {
                shareRotationInfo();
            }
        }
        if (needAreaUpdate) {
            Core.profileStart("updateArea");
            updateShadowArea();
            Core.profileEnd();
        }
        
        if (!worldObj.isRemote) {
            //Do teleportations and stuff
            if (shadowArea == null) {
                updateShadowArea();
            }
            if (shadowArea == null) {
                if (cornerMin.blockExists() && can(DeltaCapability.DIE_WHEN_EMPTY)) {
                    setDead();
                    Core.logFine("%s destroyed due to empty area", this.toString());
                } else {
                    needAreaUpdate = true; //Hopefully it will load up soon...
                }
            } else {
                if (can(DeltaCapability.TAKE_INTERIOR_ENTITIES)) {
                    takeInteriorEntities();
                }
                if (can(DeltaCapability.REMOVE_EXTERIOR_ENTITIES)) {
                    removeExteriorEntities();
                }
                if (can(DeltaCapability.REMOVE_ITEM_ENTITIES)) {
                    removeItemEntities();
                }
            }
        }
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
        for (int x = cornerMin.x; x <= cornerMax.x; x += 16) {
            for (int z = cornerMin.z; z <= cornerMax.z; z += 16) {
                if (!worldObj.blockExists(x, 64, z)) {
                    continue;
                }
                Chunk chunk = worldObj.getChunkFromBlockCoords(x, z);
                for (int j = 0; j < chunk.entityLists.length; j++) {
                    List<Entity> l = chunk.entityLists[j];
                    for (int k = 0; k < l.size(); k++) {
                        Entity ent = l.get(k); //This is probably an ArrayList.
                        if (ent.posY < 0 || ent.posY > worldObj.getActualHeight() || ent == this /* oh god what */) {
                            continue;
                        }
                        if (!shadowArea.intersectsWith(ent.boundingBox)) {
                            ejectEntity(ent);
                        }
                    }
                }
                
            }
        }
        
    }
    
    private void removeItemEntities() {
        //Move entities outside the bounds in the shadow world into the real world
        World w = cornerMin.w;
        for (int x = cornerMin.x - 16; x <= cornerMax.x + 16; x += 16) {
            for (int z = cornerMin.z - 16; z <= cornerMax.z + 16; z += 16) {
                if (!w.blockExists(x, 64, z)) {
                    continue;
                }
                Chunk chunk = w.getChunkFromBlockCoords(x, z);
                for (int j = 0; j < chunk.entityLists.length; j++) {
                    List<Entity> l = chunk.entityLists[j];
                    for (int k = 0; k < l.size(); k++) {
                        Entity ent = l.get(k); //This is probably an ArrayList.
                        if (ent.posY < 0 || ent.posY > w.getActualHeight() || ent == this /* oh god what */) {
                            continue;
                        }
                        if (ent instanceof EntityItem) {
                            ejectEntity(ent);
                        }
                    }
                }
                
            }
        }
        
    }
    
    boolean forbidEntityTransfer(Entity ent) {
//		if (ent instanceof EntityPlayerMP) {
//			EntityPlayerMP player = (EntityPlayerMP) ent;
//			if (player.capabilities.isCreativeMode) {
//				return true;
//			}
//		}
        return ent.timeUntilPortal > 0;
    }
    
    void takeEntity(Entity ent) {
        //TODO: Take transformations into account
        if (forbidEntityTransfer(ent)) {
            return;
        }
        IFzdsEntryControl ifec = null;
        if (ent instanceof IFzdsEntryControl) {
            ifec = (IFzdsEntryControl) ent;
            if (!ifec.canEnter(this)) {
                return;
            }
        }
        World shadowWorld = DeltaChunk.getServerShadowWorld();
        Vec3 newLocation = real2shadow(SpaceUtil.fromEntPos(ent));
        transferEntity(ent, shadowWorld, newLocation);
        if (ifec != null) {
            ifec.onEnter(this);
        }
    }
    
    void ejectEntity(Entity ent) {
        if (forbidEntityTransfer(ent)) {
            return;
        }
        IFzdsEntryControl ifec = null;
        if (ent instanceof IFzdsEntryControl) {
            ifec = (IFzdsEntryControl) ent;
            if (!ifec.canExit(this)) {
                return;
            }
        }
        Vec3 newLocation = shadow2real(SpaceUtil.fromEntPos(ent));
        transferEntity(ent, worldObj, newLocation);
        if (ifec != null) {
            ifec.onExit(this);
        }
    }
    
    void transferEntity(Entity ent, World newWorld, Vec3 newPosition) {
        if (ent instanceof IFzdsCustomTeleport) {
            ((IFzdsCustomTeleport) ent).transferEntity(this, worldObj, newPosition);
            return;
        }
        if (ent instanceof EntityPlayerMP) {
            if (!can(DeltaCapability.TRANSFER_PLAYERS)) {
                return;
            }
            EntityPlayerMP player = (EntityPlayerMP) ent;
            MinecraftServer ms = MinecraftServer.getServer();
            ServerConfigurationManager manager = ms.getConfigurationManager();
            DSTeleporter tp = new DSTeleporter((WorldServer) newWorld);
            tp.preciseDestination = newPosition;
            manager.transferPlayerToDimension(player, newWorld.provider.dimensionId, tp);
        } else {
            //Inspired by Entity.travelToDimension
            ent.worldObj.removeEntity(ent); //setEntityDead
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
    }
    
    void endSlice() {
        DeltaChunk.getSlices(worldObj).remove(this);
        deregisterUniversalCollisionsForDeath();
        getController().idcDied(this);
        //TODO: teleport entities/blocks into the real world?
    }
    
    @Override
    public void setDead() {
        super.setDead();
        endSlice();
    }
    
    @Override
    public boolean isInRangeToRenderDist(double distSquared) {
        //NOTE: This doesn't actually render entities as far as it should
        int s = 10*16;
        return distSquared < s*s;
    }
    
    @Override
    public boolean canEnter(IDeltaChunk dse) { return false; }
    
    @Override
    public boolean canExit(IDeltaChunk dse) { return true; }
    
    @Override
    public void onEnter(IDeltaChunk dse) { }
    
    @Override
    public void onExit(IDeltaChunk dse) { }

    @Override
    public void setPositionAndRotation2(double x, double y, double z, float yaw, float pitch, int the_number_three) {
        // This function is disabled because entity position packets call it with insufficiently precise variables.
//		this.setPosition(x, y, z);
//		this.setRotation(yaw, pitch);
    }
    
    @Override
    public void addVelocity(double par1, double par3, double par5) {
        super.addVelocity(par1, par3, par5);
        isAirBorne = false; //If this is true, we get packet spam
    }
    
    @Override
    public boolean can(DeltaCapability cap) {
        return cap.in(capabilities);
    }
    
    @Override
    public DimensionSliceEntity permit(DeltaCapability cap) {
        capabilities |= cap.bit;
        if (cap == DeltaCapability.ORACLE) {
            forbid(DeltaCapability.COLLIDE);
            forbid(DeltaCapability.DRAG);
            forbid(DeltaCapability.TAKE_INTERIOR_ENTITIES);
            forbid(DeltaCapability.REMOVE_EXTERIOR_ENTITIES);
            forbid(DeltaCapability.TRANSFER_PLAYERS);
            forbid(DeltaCapability.INTERACT);
            forbid(DeltaCapability.BLOCK_PLACE);
            forbid(DeltaCapability.BLOCK_MINE);
            forbid(DeltaCapability.REMOVE_ITEM_ENTITIES);
            forbid(DeltaCapability.REMOVE_ALL_ENTITIES);
            
            permit(DeltaCapability.SCALE);
            permit(DeltaCapability.TRANSPARENT);
        }
        return this;
    }
    
    @Override
    public DimensionSliceEntity forbid(DeltaCapability cap) {
        capabilities &= ~cap.bit;
        return this;
    }

    @Override
    public Quaternion getRotation() {
        return rotation;
    }

    @Override
    public Quaternion getRotationalVelocity() {
        return rotationalVelocity;
    }

    @Override
    public void setRotation(Quaternion r) {
        rotation = r;
    }

    @Override
    public void setRotationalVelocity(Quaternion w) {
        if (hasOrderedRotation()) return; // Could throw an error?
        rotationalVelocity = w;
    }
    
    @Override
    public boolean hasOrderedRotation() {
        return orderTimeStart != -1;
    }
    
    @Override
    public void cancelOrderedRotation() {
        if (!worldObj.isRemote && orderTimeEnd > worldObj.getTotalWorldTime() /* Didn't end naturally */) {
            FMLProxyPacket toSend = HammerNet.makePacket(HammerNet.HammerNetType.orderedRotation, this.getEntityId(), getRotation(), getRotation(), -1, Interpolation.CONSTANT.ordinal());
            broadcastPacket(toSend);
        }
        orderTimeStart = orderTimeEnd = -1;
    }
    
    @Override
    public void orderTargetRotation(Quaternion target, int tickTime, Interpolation interp) {
        target.incrNormalize();
        rotationStart = new Quaternion(getRotation());
        rotationEnd = new Quaternion(target);
        orderTimeStart = worldObj.getTotalWorldTime();
        orderTimeEnd = orderTimeStart + tickTime;
        orderInterp = interp;
        setRotationalVelocity(new Quaternion());
        if (!worldObj.isRemote) {
            FMLProxyPacket toSend = HammerNet.makePacket(HammerNet.HammerNetType.orderedRotation, this.getEntityId(), rotationStart, rotationEnd, tickTime, (byte) interp.ordinal());
            broadcastPacket(toSend);
        }
    }
    
    @Override
    public int getRemainingRotationTime() {
        long now = worldObj.getTotalWorldTime();
        if (now > orderTimeEnd) return 0;
        return (int) (orderTimeEnd - now);
    }
    
    @Override
    public Quaternion getOrderedRotationTarget() {
        return rotationEnd;
    }
    
    @Override
    public Vec3 getRotationalCenterOffset() {
        return centerOffset;
    }
    
    @Override
    public void setRotationalCenterOffset(Vec3 newOffset) {
        centerOffset = newOffset;
        if (worldObj.isRemote) return;
        if (newOffset == null) throw new NullPointerException();
        FMLProxyPacket toSend = HammerNet.makePacket(HammerNet.HammerNetType.rotationCenterOffset, getEntityId(), centerOffset);
        broadcastPacket(toSend);
    }
    
    private Quaternion getOrderedRotation(long tick) {
        // !this.hasOrderedRotation() --> tick >= -1 --> return rotationEnd
        Quaternion useStart = rotationStart, useEnd = rotationEnd;
        
        IDeltaChunk parent = this.getParent();
        if (parent != null) {
            useStart = new Quaternion(useStart);
            useEnd = new Quaternion(useEnd);
            while (parent != null) {
                Quaternion pRot = parent.getRotation();
                //tpRot.incrToOtherMultiply(useStart);
                pRot.incrToOtherMultiply(useEnd);
                parent = parent.getParent();
            }
        }
        if (tick <= orderTimeStart) return new Quaternion(useStart);
        if (tick >= orderTimeEnd) return new Quaternion(useEnd);
        
        double d = orderTimeEnd - orderTimeStart;
        double t = (tick - orderTimeStart) / d;
        t = orderInterp.scale(t);
        Quaternion ret = useStart.slerp(useEnd, t);
        ret.incrNormalize();
        return ret;
    }
    
    @Override
    public float getCollisionBorderSize() {
        return 0;
    }
    
    private DseRayTarget rayTarget = null;
    private Entity[] raypart = null;
    private boolean rayOutOfDate = true;
    
    Entity[] getRayParts() {
        if (!worldObj.isRemote) {
            return null;
        }
        if (!can(DeltaCapability.INTERACT)) {
            return null;
        }
        if (rayOutOfDate) {
            if (raypart == null) {
                raypart = new Entity[1];
                raypart[0] = rayTarget = new DseRayTarget(this);
            }
            rayOutOfDate = false;
            Hammer.proxy.updateRayPosition(rayTarget);
        }
        return raypart;
    }
    
    private void broadcastPacket(FMLProxyPacket toSend) {
        HammerNet.channel.sendToAllAround(toSend, new NetworkRegistry.TargetPoint(dimension, posX, posY, posZ, 64));
    }

    static final ItemStack[] blast_protection = new ItemStack[1];
    static {
        ItemStack is = blast_protection[0] = new ItemStack(Items.diamond_chestplate, 0, 0 /* hopefully it doesn't get stolen */);
        is.addEnchantment(Enchantment.blastProtection, 88);
    }

    @Override
    public ItemStack[] getLastActiveItems() {
        return blast_protection;
    }

    @Override
    public boolean doesEntityNotTriggerPressurePlate() {
        return true;
    }
}
