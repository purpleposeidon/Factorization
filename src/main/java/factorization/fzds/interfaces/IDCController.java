package factorization.fzds.interfaces;

import factorization.api.Coord;
import factorization.fzds.DimensionSliceEntity;
import factorization.shared.EntityReference;
import factorization.util.NORELEASE;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;

public interface IDCController {
    boolean placeBlock(IDimensionSlice idc, EntityPlayer player, Coord at);
    boolean breakBlock(IDimensionSlice idc, EntityPlayer player, Coord at, EnumFacing sideHit);
    boolean hitBlock(IDimensionSlice idc, EntityPlayer player, Coord at, EnumFacing sideHit);
    boolean useBlock(IDimensionSlice idc, EntityPlayer player, Coord at, EnumFacing sideHit);
    void idcDied(IDimensionSlice idc);
    void beforeUpdate(IDimensionSlice idc);
    void afterUpdate(IDimensionSlice idc);
    boolean onAttacked(IDimensionSlice idc, DamageSource damageSource, float damage);
    CollisionAction collidedWithWorld(World realWorld, AxisAlignedBB realBox, World shadowWorld, AxisAlignedBB shadowBox);

    IDCController default_controller = new IDCController() {
        // Has to be a do-nothing, 'cause if it were a do-something then something might get overridden.
        @Override public boolean placeBlock(IDimensionSlice idc, EntityPlayer player, Coord at) { return false; }
        @Override public boolean breakBlock(IDimensionSlice idc, EntityPlayer player, Coord at, EnumFacing sideHit) { return false; }
        @Override public boolean hitBlock(IDimensionSlice idc, EntityPlayer player, Coord at, EnumFacing sideHit) { return false; }
        @Override public boolean useBlock(IDimensionSlice idc, EntityPlayer player, Coord at, EnumFacing sideHit) { return false; }
        @Override public void idcDied(IDimensionSlice idc) { }
        @Override public void beforeUpdate(IDimensionSlice idc) { }
        @Override public void afterUpdate(IDimensionSlice idc) { }
        @Override public boolean onAttacked(IDimensionSlice idc, DamageSource damageSource, float damage) { return false; }
        @Override public CollisionAction collidedWithWorld(World realWorld, AxisAlignedBB realBox, World shadowWorld, AxisAlignedBB shadowBox) { return CollisionAction.STOP_BEFORE; }
    };

    class AutoControl implements EntityReference.OnFound<DimensionSliceEntityBase> {
        final IDCController controller;

        public AutoControl(IDCController controller) {
            NORELEASE.fixme("Need a new dse class hierarchy");
            /**
             * net.minecraft.source.Entity
             * factorization.api.EntityFZ
             * factorization.fzds.interfaces.DeltaChunkEntityBase implements IDeltaChunk (abstract)
             * factorization.fzds.DimensionSliceEntityDetails (still abstract; does weird miscy things)
             * factorization.fzds.DimensionSliceEntity
             */
            this.controller = controller;
        }

        @Override
        public void found(DimensionSliceEntityBase ent) {
            ent.setController(controller);
        }
    }

    enum CollisionAction {
        STOP_BEFORE, STOP_INSIDE, IGNORE
    }
}
