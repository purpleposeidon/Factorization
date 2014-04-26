package factorization.notify;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import factorization.api.Coord;

class RecuringNotification {
    EntityPlayer player;
    Object where;
    MessageUpdater updater;
    int age = 0;
    boolean first = true;
    
    RecuringNotification(EntityPlayer player, Object where, MessageUpdater updater) {
        this.where = where;
        this.updater = updater;
        this.player = player;
    }
    
    boolean isInvalid() {
        if (age++ > 20*11 /* See Message.lifeTime */) {
            return true;
        }
        if (where instanceof Entity) {
            Entity ent = (Entity) where;
            if (ent.isDead) {
                return false;
            }
        } else if (where instanceof TileEntity) {
            TileEntity te = (TileEntity) where;
            if (te.isInvalid()) {
                return false;
            }
        } else if (where instanceof Coord) {
            Coord coord = (Coord) where;
            if (!coord.blockExists()) {
                return false;
            }
        }
        if (player != null) {
            return player.isDead;
        }
        return false;
    }
}
