package factorization.fzds.interfaces;

import factorization.api.Coord;
import factorization.shared.EntityReference;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;

public interface IDCController {
    boolean placeBlock(IDeltaChunk idc, EntityPlayer player, Coord at);
    boolean breakBlock(IDeltaChunk idc, EntityPlayer player, Coord at, byte sideHit);
    boolean hitBlock(IDeltaChunk idc, EntityPlayer player, Coord at, byte sideHit);
    boolean useBlock(IDeltaChunk idc, EntityPlayer player, Coord at, byte sideHit);
    void idcDied(IDeltaChunk idc);
    void beforeUpdate(IDeltaChunk idc);
    void afterUpdate(IDeltaChunk idc);
    boolean onAttacked(IDeltaChunk idc, DamageSource damageSource, float damage);

    IDCController default_controller = new IDCController() {
        // Has to be a do-nothing, 'cause if it were a do-something then something might get overridden.
        @Override public boolean placeBlock(IDeltaChunk idc, EntityPlayer player, Coord at) { return false; }
        @Override public boolean breakBlock(IDeltaChunk idc, EntityPlayer player, Coord at, byte sideHit) { return false; }
        @Override public boolean hitBlock(IDeltaChunk idc, EntityPlayer player, Coord at, byte sideHit) { return false; }
        @Override public boolean useBlock(IDeltaChunk idc, EntityPlayer player, Coord at, byte sideHit) { return false; }
        @Override public void idcDied(IDeltaChunk idc) { }
        @Override public void beforeUpdate(IDeltaChunk idc) { }
        @Override public void afterUpdate(IDeltaChunk idc) { }
        @Override public boolean onAttacked(IDeltaChunk idc, DamageSource damageSource, float damage) { return false; }
    };

    class AutoControl implements EntityReference.OnFound<IDeltaChunk> {
        final IDCController controller;

        public AutoControl(IDCController controller) {
            this.controller = controller;
        }

        @Override
        public void found(IDeltaChunk ent) {
            ent.setController(controller);
        }
    }
}
