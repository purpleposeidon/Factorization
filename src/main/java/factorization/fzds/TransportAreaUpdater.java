package factorization.fzds;

import com.google.common.base.Predicate;
import factorization.api.Coord;
import factorization.fzds.interfaces.DeltaCapability;
import factorization.fzds.interfaces.IFzdsCustomTeleport;
import factorization.fzds.interfaces.IFzdsEntryControl;
import factorization.shared.Core;
import factorization.util.NORELEASE;
import factorization.util.SpaceUtil;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;

import java.util.List;

public class TransportAreaUpdater {
    final DimensionSliceEntity dse;

    private TransportAreaUpdater(DimensionSliceEntity dse) {
        this.dse = dse;
    }

    public static TransportAreaUpdater create(DimensionSliceEntity dse) {
        return new TransportAreaUpdater(dse);
    }

    transient boolean needsRealAreaUpdate = true;
    transient boolean needsShadowAreaUpdate = true;

    void update() {
        if (needsShadowAreaUpdate || dse.shadowArea == null) {
            updateShadowArea();
        }
        if (needsRealAreaUpdate) {
            updateRealArea();
        }
        if (dse.worldObj.isRemote) {
            return;
        }
        //Do teleportations and stuff
        if (dse.shadowArea == null) {
            if (dse.getMinCorner().blockExists() && dse.can(DeltaCapability.DIE_WHEN_EMPTY)) {
                dse.setDead();
                Core.logFine("%s destroyed due to empty area", this.toString());
            } else {
                needsShadowAreaUpdate = true; //Hopefully it will load up soon...
            }
        } else {
            if (dse.can(DeltaCapability.TAKE_INTERIOR_ENTITIES)) {
                takeInteriorEntities();
            }
            if (dse.can(DeltaCapability.REMOVE_EXTERIOR_ENTITIES)) {
                removeExteriorEntities();
            }
            if (dse.can(DeltaCapability.REMOVE_ITEM_ENTITIES)) {
                removeItemEntities();
            }
        }
    }

    void updateShadowArea() {
        Coord s = dse.getMinCorner();
        Coord e = dse.getMaxCorner();
        s.setId(Blocks.stone);
        e.setId(Blocks.stone);
        double start_minX = 0, start_minY = 0, start_minZ = 0, start_maxX = 0, start_maxY = 0, start_maxZ = 0;
        // NORELEASE omfg slow!
        // :( Can't use chunk.heightMap here! It's actually just the non-opaque blocks,
        // which can of course still collide/be interacted with!
        // Some other optimizations tho:
        // 1. Keep track of the chunk directly (or could use an IWorldAccess cache?)
        // 2. Iterate over y first for cache efficiency
        World w = s.w;
        Chunk chunk = s.getChunk();
        boolean first = true;
        for (int y = s.y; y <= e.y; y++) {
            for (int x = s.x; x <= e.x; x++) {
                if (x >> 4 != chunk.xPosition) {
                    chunk = s.w.getChunkFromChunkCoords(x >> 4, s.z >> 4);
                }
                for (int z = s.z; z <= e.z; z++) {
                    if (z >> 4 != chunk.zPosition) {
                        chunk = s.w.getChunkFromChunkCoords(x >> 4, z >> 4);
                    }
                    Block block = chunk.getBlock(x & 15, y, z & 15);
                    if (block.getMaterial() == Material.air) {
                        continue;
                    }
                    if (first) {
                        first = false;
                        start_minX = x;
                        start_minY = y;
                        start_minZ = z;
                        start_maxX = x + 1;
                        start_maxY = y + 1;
                        start_maxZ = z + 1;
                    } else {
                        start_minX = Math.min(start_minX, x);
                        start_minY = Math.min(start_minY, y);
                        start_minZ = Math.min(start_minZ, z);
                        start_maxX = Math.max(start_maxX, x + 1);
                        start_maxY = Math.max(start_maxY, y + 1);
                        start_maxZ = Math.max(start_maxZ, z + 1);
                    }
                }
            }
        }
        if (first) {
            if (dse.worldObj.isRemote) {
                return;
            }
            if (dse.can(DeltaCapability.DIE_WHEN_EMPTY)) {
                Core.logInfo("IDC requests deletion when empty, and is empty: %s", this);
                dse.setDead();
                return;
            }
            dse.shadowArea = SpaceUtil.newBox();
            return;
        }

        AxisAlignedBB newShadowArea = new AxisAlignedBB(start_minX, start_minY, start_minZ, start_maxX, start_maxY, start_maxZ);
        needsRealAreaUpdate = !SpaceUtil.equals(dse.shadowArea, newShadowArea);
        dse.shadowArea = newShadowArea;
        needsShadowAreaUpdate = false;
    }

    private void takeInteriorEntities() {
        //Move entities inside our bounds in the real world into the shadow world
        List<Entity> realEntities = dse.worldObj.getEntitiesWithinAABB(Entity.class, dse.realArea); //
        for (int i = 0; i < realEntities.size(); i++) {
            Entity ent = realEntities.get(i);
            if (ent == dse) {
                continue;
            }
            takeEntity(ent);
        }
    }

    private void removeExteriorEntities() {
        //Move entities outside the bounds in the shadow world into the real world
        // public List<Entity> getEntitiesInAABBexcluding(Entity entityIn, AxisAlignedBB boundingBox, Predicate <? super Entity > predicate)
        Coord cornerMin = dse.getMinCorner();
        final int maxY = cornerMin.w.getActualHeight();
        List<Entity> eject = cornerMin.w.getEntitiesInAABBexcluding(dse, dse.shadowArea, new Predicate<Entity>() {
            @Override
            public boolean apply(Entity ent) {
                if (ent.posY < 0 || ent.posY > maxY) return false;
                return true;
            }
        });
        for (Entity ent : eject) ejectEntity(ent);
    }

    public void removeItemEntities() {
        //Move entities outside the bounds in the shadow world into the real world
        Coord cornerMin = dse.getMinCorner();
        final int maxY = cornerMin.w.getActualHeight();
        List<Entity> eject = cornerMin.w.getEntitiesInAABBexcluding(dse, dse.shadowArea.expand(16, 16, 16), new Predicate<Entity>() {
            @Override
            public boolean apply(Entity ent) {
                if (ent.posY < 0 || ent.posY > maxY) return false;
                return ent instanceof EntityItem;
            }
        });

        for (Entity ent : eject) ejectEntity(ent);
    }

    boolean forbidEntityTransfer(Entity ent) {
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
            if (!ifec.canEnter(dse)) {
                return;
            }
        }
        World shadowWorld = DeltaChunk.getServerShadowWorld();
        Vec3 newLocation = dse.real2shadow(SpaceUtil.fromEntPos(ent));
        transferEntity(ent, shadowWorld, newLocation);
        if (ifec != null) {
            ifec.onEnter(dse);
        }
    }

    void ejectEntity(Entity ent) {
        if (forbidEntityTransfer(ent)) {
            return;
        }
        IFzdsEntryControl ifec = null;
        if (ent instanceof IFzdsEntryControl) {
            ifec = (IFzdsEntryControl) ent;
            if (!ifec.canExit(dse)) {
                return;
            }
        }
        Vec3 newLocation = dse.shadow2real(SpaceUtil.fromEntPos(ent));
        transferEntity(ent, dse.worldObj, newLocation);
        if (ifec != null) {
            ifec.onExit(dse);
        }
    }

    void transferEntity(Entity ent, World newWorld, Vec3 newPosition) {
        if (ent instanceof IFzdsCustomTeleport) {
            ((IFzdsCustomTeleport) ent).transferEntity(dse, dse.worldObj, newPosition);
            return;
        }
        if (ent instanceof EntityPlayerMP) {
            if (!dse.can(DeltaCapability.TRANSFER_PLAYERS)) {
                return;
            }
            EntityPlayerMP player = (EntityPlayerMP) ent;
            MinecraftServer ms = MinecraftServer.getServer();
            ServerConfigurationManager manager = ms.getConfigurationManager();
            DSTeleporter tp = new DSTeleporter((WorldServer) newWorld);
            tp.preciseDestination = newPosition;
            manager.transferPlayerToDimension(player, newWorld.provider.getDimensionId(), tp);
        } else {
            //Inspired by Entity.travelToDimension
            ent.worldObj.removeEntity(ent); //setEntityDead
            ent.isDead = false;

            Entity phoenix = EntityList.createEntityByName(EntityList.getEntityString(ent), newWorld); //Like a phoenix rising from the ashes!
            if (phoenix == null) {
                return; //Or not.
            }
            phoenix.copyDataFromOld(ent);
            phoenix.timeUntilPortal = phoenix.getPortalCooldown();
            ent.isDead = true;
            phoenix.setPosition(newPosition.xCoord, newPosition.yCoord, newPosition.zCoord);
            newWorld.spawnEntityInWorld(phoenix);
        }
    }

    void updateRealArea() {
        NORELEASE.fixme("Move to MotionUpdater?");
        if (dse.shadowArea == null) {
            return;
        }
        Vec3[] corners = SpaceUtil.getCorners(dse.shadowArea);
        for (int i = 0; i < corners.length; i++) {
            corners[i] = dse.shadow2real(corners[i]);
        }
        Vec3 min = SpaceUtil.getLowest(corners);
        Vec3 max = SpaceUtil.getHighest(corners);
        dse.realArea = SpaceUtil.newBox(min, max);
        dse.setEntityBoundingBox(dse.realArea);
        dse.metaAABB = new MetaAxisAlignedBB(dse, dse.getShadowWorld(), dse.realArea);
        needsRealAreaUpdate = false;
        dse.updateUniversalCollisions();
    }
}
