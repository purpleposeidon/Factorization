package factorization.fzds;

import factorization.aabbdebug.AabbDebugger;
import factorization.algos.TortoiseAndHare;
import factorization.api.*;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.DataInByteBuf;
import factorization.api.datahelpers.DataOutByteBuf;
import factorization.api.datahelpers.Share;
import factorization.common.FzConfig;
import factorization.coremodhooks.IExtraChunkData;
import factorization.fzds.interfaces.DeltaCapability;
import factorization.fzds.interfaces.IDCController;
import factorization.fzds.interfaces.IDimensionSlice;
import factorization.fzds.interfaces.transform.Pure;
import factorization.fzds.interfaces.transform.TransformData;
import factorization.fzds.network.PacketProxyingPlayer;
import factorization.shared.Core;
import factorization.shared.EntityReference;
import factorization.util.NORELEASE;
import factorization.util.NumUtil;
import factorization.util.SpaceUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;
import net.minecraftforge.fml.relauncher.Side;
import org.apache.commons.lang3.ArrayUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

public final class DimensionSliceEntity extends DimensionSliceFussyDetails {
    //Dang, this class is a mess! Code folding, activate!
    
    private Coord cornerMin = Coord.ZERO.copy();
    private Coord cornerMax = Coord.ZERO.copy();

    private final EntityReference<DimensionSliceEntity> parent; // init in constructor ._.
    Vec3 parentShadowOrigin = new Vec3(0, 0, 0);
    transient /* Child remembers */ final ArrayList<IDimensionSlice> children = new ArrayList<IDimensionSlice>(0);
    
    private long capabilities = DeltaCapability.of(DeltaCapability.MOVE, DeltaCapability.COLLIDE, DeltaCapability.DRAG, DeltaCapability.REMOVE_ITEM_ENTITIES);
    
    AxisAlignedBB realArea = SpaceUtil.newBox();
    MetaAxisAlignedBB metaAABB = null;
    
    AxisAlignedBB shadowArea = null;
    boolean needsShadowAreaUpdate = true;

    /** The current transformation. */
    TransformData<Pure> transform = TransformData.newPure();
    /** How the transformation will be changed next tick. */
    TransformData<Pure> velocity = TransformData.newPure();
    TransformData<Pure> accumVelocity = TransformData.newPure();

    /** The last transform sent to the client */
    TransformData<Pure> transformSynced = TransformData.newPure();
    /** The last accel sent to the client */
    TransformData<Pure> velocitySynced = TransformData.newPure();

    /** The transform that the client is interpolation through while rendering */
    TransformData<Pure> transformPrevTick = TransformData.newPure();

    ITransformOrder order = new NullOrder();

    float opacity = 1;
    
    Object renderInfo = null; //Client-side
    
    Entity packetRelay = null;
    HashSet<IExtraChunkData> registered_chunks = new HashSet<IExtraChunkData>();
    UniversalCollider universalCollider;

    public final MotionUpdater motionUpdater = MotionUpdater.create(this);
    public final TransportAreaUpdater transportAreaUpdater = TransportAreaUpdater.create(this);
    
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
            if (!(can(DeltaCapability.ORACLE) && lowerCorner.w == worldObj)) {
                throw new IllegalArgumentException("My corners are not shadow!");
            }
        }
        Coord.sort(lowerCorner, upperCorner);
        this.cornerMin = lowerCorner;
        this.cornerMax = upperCorner;
        if (NORELEASE.on) {
            Chunk chunk = cornerMin.getChunk();
            int cx = cornerMin.x / 16;
            int cz = cornerMin.z / 16;
            if (cx != chunk.xPosition || cz != chunk.zPosition) {
                Core.logSevere("Chunk positioning fail. " + cx + "," + cz + " vs " + chunk);
                Core.logSevere("Chunk positioning fail. " + cornerMin.x + "," + cornerMin.z + " vs " + chunk);
            }
        }
        DeltaCoord dc = upperCorner.difference(lowerCorner);
        transform.setOffset(new Vec3(
                dc.x / 2,
                dc.y / 2,
                dc.z / 2));
    }

    transient Mat _transform_S2R = null, _transform_R2S = null;

    @Override
    public Mat getShadow2Real(float partial) {
        cleanTransforms();
        if (partial != 1) return compileTransform(partial).invert();
        if (_transform_S2R != null) return _transform_S2R;
        return _transform_S2R = getReal2Shadow(partial).invert();
    }

    @Override
    public Mat getReal2Shadow(float partial) {
        cleanTransforms();
        if (partial != 1) return compileTransform(partial);
        if (_transform_R2S != null) return _transform_R2S;
        return _transform_R2S = compileTransform(partial);
    }

    private Mat compileTransform(float partial) {
        return Mat.mul(
                Mat.trans(cornerMin.toVector()),
                Mat.trans(NumUtil.interp(transform.getOffset(), transformPrevTick.getOffset(), partial)),
                Mat.scale(NumUtil.interp(transform.getScale(), transformPrevTick.getScale(), partial)),
                Mat.rotate(slerpRotation(partial)),
                Mat.trans(NumUtil.interp(transform.getPos(), transformPrevTick.getPos(), partial)).invert()
        );
        // shadow2real more likely to be called? Worst case we just implement the other side.
        // real = rotate($shadow - corner - centerOffset) + DSE position
        // shadow = rotate⁻¹(real - DSE) + centerOffset + corner
    }

    private Quaternion slerpRotation(float partial) {
        Quaternion current = transform.getRot();
        if (partial == 1) return current;
        Quaternion prev = transformPrevTick.getRot();
        if (partial == 0) return prev;
        return prev.slerp(current, partial).incrNormalize();
    }

    private void dirty() {
        _transform_S2R = null;
        _transform_R2S = null;
    }

    private void cleanTransforms() {
        if (transform.clean()) {
            dirty();
        }
    }

    @Override
    public Coord getMinCorner() {
        return cornerMin.copy();
    }
    
    @Override
    public Coord getMaxCorner() {
        return cornerMax.copy();
    }

    @Override
    public void findAnyCollidingBox() {
        motionUpdater.collideWithWorld(metaAABB, TransformData.newPure(), transform);
    }

    String partName = "";
    @Override
    public void setPartName(String partName) {
        this.partName = partName;
    }

    transient IDCController controller = IDCController.default_controller;

    @Override
    public IDCController getController() {
        return controller;
    }

    @Override
    public void setController(IDCController controller) {
        if (controller == null) controller = IDCController.default_controller;
        this.controller = controller;
    }


    String nameOrInfo() {
        if (partName != null) return partName + "#" + getEntityId();
        return "" + getEntityId();

    }

    @Override
    public String toString() {
        NORELEASE.fixme("Update Entity.pos/motion");
        String ret = "[DSE " + nameOrInfo() + " <" + getMinCorner().toBlockPos() + " -> " + getMaxCorner().toBlockPos() + ">]";
        if (getParent() != null) {
            ret += " parent=" + ((DimensionSliceEntity) getParent()).nameOrInfo();
        }
        ret += " " + transform;
        return ret;
    }

    @Override
    protected void putData(DataHelper data) throws IOException {
        capabilities = data.as(Share.VISIBLE, "cap").putLong(capabilities);
        cornerMin = data.as(Share.VISIBLE, "min").putIDS(cornerMin);
        cornerMax = data.as(Share.VISIBLE, "max").putIDS(cornerMax);
        partName = data.as(Share.VISIBLE, "partName").putString(partName);
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
        /*parent =*/ data.as(Share.VISIBLE, "parent").putIDS(parent);
        parentShadowOrigin = data.as(Share.VISIBLE, "parentShadowOrigin").putVec3(parentShadowOrigin);
        entityUniqueID = data.as(Share.VISIBLE, "entityUUID").putUUID(entityUniqueID);

        transform = data.as(Share.VISIBLE, "s").putIDS(transform);
        velocity = data.as(Share.VISIBLE, "v").putIDS(velocity);

        order = (ITransformOrder) data.as(Share.VISIBLE, "order").putRegisteredUnion(ITransformOrder.Registry.registry, order);
    }
    
    private AxisAlignedBB offsetAABB(AxisAlignedBB orig, double dx, double dy, double dz) {
        return new AxisAlignedBB(
                orig.minX + dx, orig.minY + dy, orig.minZ + dz,
                orig.maxX + dx, orig.maxY + dy, orig.maxZ + dz);
    }
    
    @Override
    public IDimensionSlice getParent() {
        return parent.getEntity();
    }
    
    @Override
    public Vec3 getParentJoint() {
        return parentShadowOrigin;
    }
    
    @Override
    public void setParent(IDimensionSlice _parent) {
        DimensionSliceEntity oldParent = this.parent.getEntity();
        if (oldParent != null) {
            oldParent.children.remove(this);
        }
        if (null != TortoiseAndHare.race(this, new TortoiseAndHare.Advancer<IDimensionSlice>() {
            @Override
            public IDimensionSlice getNext(IDimensionSlice node) {
                return node.getParent();
            }
        })) {
            throw new IllegalArgumentException("Parenting loop!");
        }
        DimensionSliceEntity newParent = (DimensionSliceEntity) _parent;
        this.parent.trackEntity(newParent);
        this.parentShadowOrigin = _parent.real2shadow(SpaceUtil.fromEntPos(this));
        newParent.children.remove(this);
        newParent.children.add(this);
    }
    
    @Override
    public ArrayList<IDimensionSlice> getChildren() {
        return children;
    }

    @Override
    public TransformData<Pure> getTransform() {
        return transform;
    }

    @Override
    public TransformData<Pure> getVel() {
        return velocity;
    }

    void updateRealArea() {
        NORELEASE.fixme("Move to MotionUpdater?");
        Vec3[] corners = SpaceUtil.getCorners(shadowArea);
        for (int i = 0; i < corners.length; i++) {
            corners[i] = shadow2real(corners[i]);
        }
        Vec3 min = SpaceUtil.getLowest(corners);
        Vec3 max = SpaceUtil.getHighest(corners);
        realArea = SpaceUtil.newBox(min, max);
        setEntityBoundingBox(realArea);
        metaAABB = new MetaAxisAlignedBB(this, cornerMin.w, realArea);
        needsShadowAreaUpdate = false;
    }

    void updateUniversalCollisions() {
        NORELEASE.fixme("Move to MotionUpdater?");
        if (realArea == null) return;
        double d = 8;
        if (can(DeltaCapability.ROTATE)) {
            // Must enlarge by the worst-case rotation
            Coord min = getMinCorner(), max = getMaxCorner();
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
            if (colliders.length == 1) {
                colliders = null;
            } else {
                colliders = ArrayUtils.removeElement(colliders, universalCollider);
            }
            chunk.setConstantColliders(colliders);
        }
    }

    static {
        class ChunkLoadHandler {
            @SubscribeEvent
            public void load(ChunkEvent.Load event) {
                AxisAlignedBB chunkBox = null;
                for (IDimensionSlice idc : DeltaChunk.getAllSlices(event.world)) {
                    DimensionSliceEntity dse = (DimensionSliceEntity) idc;
                    if (dse.needsShadowAreaUpdate) continue;
                    if (chunkBox == null) {
                        chunkBox = SpaceUtil.getBox(event.getChunk());
                    }
                    if (chunkBox.intersectsWith(dse.realArea)) {
                        dse.needsShadowAreaUpdate = true;
                    }
                }
            }

            @SubscribeEvent
            public void unload(ChunkEvent.Unload event) {
                IExtraChunkData data = (IExtraChunkData) event.getChunk();
                for (Entity ent : data.getConstantColliders()) {
                    if (ent instanceof UniversalCollider) {
                        UniversalCollider uc = (UniversalCollider) ent;
                        uc.dimensionSliceEntity.needsShadowAreaUpdate = true;
                    }
                }
            }
        }
        Core.loadBus(new ChunkLoadHandler());
    }
    
    private void check_chunk(double x, double z, HashSet<IExtraChunkData> toDeregister) {
        if (isDead) return;
        int ix = (int) x;
        int iz = (int) z;
        BlockPos pos = new BlockPos(ix, 64, iz);
        if (!worldObj.isBlockLoaded(pos)) return;
        Chunk mc_chunk = worldObj.getChunkFromBlockCoords(pos);
        if (mc_chunk == null) {
            return;
        }
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
    
    
    public void blocksChanged(int x, int y, int z) {
        if (shadowArea == null) {
            needsShadowAreaUpdate = true;
            return;
        }
        needsShadowAreaUpdate |= x <= shadowArea.minX
                || y <= shadowArea.minY
                || z <= shadowArea.minZ
                || x >= shadowArea.maxX
                || y >= shadowArea.maxY
                || z >= shadowArea.maxZ;
    }

    public Vec3 getInstantaneousRotationalVelocityAtPointInCornerSpace(Vec3 corner) {
        // TODO: Doesn't take into account parent motion...
        NORELEASE.fixme("This isn't using a matrix.");
        Vec3 origPoint = SpaceUtil.subtract(transform.getOffset(), corner);
        origPoint = transform.getRot().applyRotation(origPoint);
        Vec3 rotatedPoint = velocity.getRot().applyRotation(origPoint);
        return SpaceUtil.subtract(origPoint, rotatedPoint);
    }

    enum SyncMessages {
        TRANSFORMS;

        static final Enum[] VALUES = values();
    }

    @Override
    public Enum[] getMessages() {
        return SyncMessages.VALUES;
    }

    void putTransforms(DataHelper data) throws IOException {
        transform = data.as(Share.VISIBLE, "s").putIDS(transform);
        velocity = data.as(Share.VISIBLE, "v").putIDS(velocity);
    }

    void shareTransforms() {
        boolean needSync = (transformSynced.getGrandUnifiedDistance(transform) != 0 || velocitySynced.getGrandUnifiedDistance(velocity) != 0) && !hasOrders();
        if (!needSync) return;
        try {
            ByteBuf output = Unpooled.buffer();
            Core.network.prefixEntityPacket(output, this, SyncMessages.TRANSFORMS);
            putTransforms(new DataOutByteBuf(output, Side.SERVER));
            FMLProxyPacket toSend = Core.network.entityPacket(output);
            Core.network.broadcastPacket(null, new Coord(this), toSend);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        transformSynced = transform.copy();
        velocitySynced = velocity.copy();
    }

    @Override
    public boolean handleMessageFromServer(Enum messageType, ByteBuf input) throws IOException {
        if (super.handleMessageFromServer(messageType, input)) return true;
        if (messageType == SyncMessages.TRANSFORMS) {
            putTransforms(new DataInByteBuf(input, Side.CLIENT));
        }
        return false;
    }

    void debugCollisions() {
        if (!FzConfig.debug_fzds_collisions) return;
        if (this.metaAABB == null) return;

        Coord.iterateCube(getMinCorner(), getMaxCorner(), new ICoordFunction() {
            @Override
            public void handle(Coord at) {
                if (at.isAir()) return;
                AxisAlignedBB box = at.getCollisionBoundingBox();
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
    public void onEntityUpdate() { // onupdateentity
        if (isDead) return;
        //We don't want to call super, because it does a bunch of stuff that makes no sense for us.
        prevPosX = posX;
        prevPosY = posY;
        prevPosZ = posZ;
        if (worldObj.isRemote) {
            rayOutOfDate = true;
            if (ticksExisted == 1) {
                NORELEASE.fixme("How about a spawn packet instead?");
                DeltaChunk.getSlices(worldObj).add(this);
            }
        }
        if (missingPacketRelay()) return;
        Core.profileStart("FzdsEntityTick");
        if (!worldObj.isRemote) controller.beforeUpdate(this);
        if (!parent.trackingEntity()) {
            Core.profileStart("motionUpdate");
            motionUpdater.updateMotion(TransformData.newPure());
            Core.profileEnd();
        } else if (!parent.entityFound()) {
            IDimensionSlice p = parent.getEntity();
            if (p == null) return;
            Vec3 real_parent_origin = parentShadowOrigin;
            setParent(p);
            if (!SpaceUtil.isZero(real_parent_origin)) {
                parentShadowOrigin = real_parent_origin;
            }
        }
        if (!worldObj.isRemote) {
            controller.afterUpdate(this);
            shareTransforms();
            if (can(DeltaCapability.ROTATE)) {
                shareTransforms();
            }
        }

        Core.profileStart("transportAreaUpdate");
        transportAreaUpdater.update();
        Core.profileEnd();

        Core.profileEnd();
    }

    private boolean missingPacketRelay() {
        if (worldObj.isRemote || packetRelay != null) {
            return false;
        }
        boolean isOracle = can(DeltaCapability.ORACLE);
        initCorners();
        if (isOracle) {
            packetRelay = this;
            return false;
        }
        DeltaChunk.getSlices(worldObj).add(this);
        World shadowWorld = DeltaChunk.getServerShadowWorld();
        PacketProxyingPlayer ppp = new PacketProxyingPlayer(this, shadowWorld);
        ppp.setCanDie(true);
        boolean success = ppp.worldObj.spawnEntityInWorld(ppp);
        if (!success || ppp.isDead) {
            Core.logSevere("Failed to spawn packetRelay");
            if (NORELEASE.on) {
                setDead();
                return true;
            }
            throw new IllegalStateException("Failed to spawn packetRelay");
        }
        ppp.setCanDie(false);
        packetRelay = ppp;
        return true;
    }

    @Override
    public void setDead() {
        super.setDead();
        DeltaChunk.getSlices(worldObj).remove(this);
        deregisterUniversalCollisionsForDeath();
        getController().idcDied(this);
        // Moving blocks/entities back to the real world is the responsibility of the caller!
    }
    
    @Override
    public boolean isInRangeToRenderDist(double distSquared) {
        //NOTE: This doesn't actually render entities as far as it should
        int s = 10*16;
        return distSquared < s*s;
    }

    @Override
    public boolean can(DeltaCapability cap) {
        return cap.in(capabilities);
    }

    @Override
    public DimensionSliceEntity permit(DeltaCapability ...caps) {
        for (DeltaCapability cap : caps) {
            capabilities |= cap.bit;
            if (cap == DeltaCapability.ORACLE) {
                forbid(
                        DeltaCapability.COLLIDE,
                        DeltaCapability.DRAG,
                        DeltaCapability.TAKE_INTERIOR_ENTITIES,
                        DeltaCapability.REMOVE_EXTERIOR_ENTITIES,
                        DeltaCapability.TRANSFER_PLAYERS,
                        DeltaCapability.INTERACT,
                        DeltaCapability.BLOCK_PLACE,
                        DeltaCapability.BLOCK_MINE,
                        DeltaCapability.REMOVE_ITEM_ENTITIES,
                        DeltaCapability.REMOVE_ALL_ENTITIES);
                permit(DeltaCapability.SCALE, DeltaCapability.TRANSPARENT);
            }
        }
        return this;
    }

    @Override
    public IDimensionSlice forbid(DeltaCapability... caps) {
        for (DeltaCapability cap : caps) {
            capabilities &= ~cap.bit;
        }
        return this;
    }

    @Override
    public boolean hasOrders() {
        NORELEASE.fixme("extract class OrderManager");
        return !order.isNull();
    }

    @Override
    public void cancelOrder() {
        if (order.isNull()) return;
        order = new NullOrder();
    }

    @Override
    public void giveOrder(ITransformOrder order) {
        if (!worldObj.isRemote) {
            this.order = order;
            syncData();
        }
    }

    @Override
    public ITransformOrder getOrder() {
        return order;
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
}
