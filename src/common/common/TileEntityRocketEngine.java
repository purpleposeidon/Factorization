package factorization.common;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.common.ForgeDirection;
import factorization.api.Coord;
import factorization.api.DeltaCoord;
import factorization.common.Core.NotifyStyle;

public class TileEntityRocketEngine extends TileEntityCommon {

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.ROCKETENGINE;
    }

    @Override
    public BlockClass getBlockClass() {
        return BlockClass.DarkIron;
    }
    
    List<Coord> getArea() {
        return getArea(getCoord(), new DeltaCoord(1, 1, 1) /* this is dependent on the behavior of Coord.isSubmissive; onPlacedBy determines the lowest coord */);
    }
    
    List<Coord> getArea(Coord c, DeltaCoord dc) {
        //2x3x2
        ArrayList<Coord> ret = new ArrayList<Coord>(2*3*2);
        for (int dyc = 0; dyc < 3; dyc++) {
            int dy = dyc*dc.y;
            for (int dxc = 0; dxc < 2; dxc++) {
                int dx = dxc*dc.x;
                for (int dzc = 0; dzc < 2; dzc++) {
                    int dz = dxc*dc.x;
                    ret.add(c.add(dx, dy, dz));
                }
            }
        }
        return ret;
    }
    
    DeltaCoord getCornerDirection(EntityPlayer player, int side) {
        ForgeDirection dir = ForgeDirection.getOrientation(side);
        ForgeDirection facing = ForgeDirection.getOrientation(FactorizationUtil.determineOrientation(player));
        DeltaCoord dc = new DeltaCoord(dir.offsetX + facing.offsetX, dir.offsetY + facing.offsetY, dir.offsetZ + facing.offsetZ);
        if (dc.isZero()) {
            return null;
        }
        if (dc.getFaceSide() == -1) {
            return null;
        }
        return dc;
    }
    
    @Override
    boolean canPlaceAgainst(EntityPlayer player, Coord c, int side) {
        DeltaCoord dc = getCornerDirection(player, side);
        if (dc == null) {
            Core.notify(player, c, "Place it differently");
            return false;
        }
        boolean fail = false;
        for (Coord spot : getArea(c, dc)) {
            if (!spot.isReplacable()) {
                if (!spot.equals(c)) {
                    Core.notify(player, spot, NotifyStyle.FORCE, "X");
                }
                fail = true;
            }
        }
        if (fail) {
            Core.notify(player, c, NotifyStyle.FORCE, "Area blocked");
            return false;
        }
        AxisAlignedBB area = AxisAlignedBB.getBoundingBox(c.x, c.y, c.z, c.x + 2*dc.x, 3*dc.y, 2*dc.z);
        for (Object o : c.w.getEntitiesWithinAABBExcludingEntity(null, area)) {
            Entity e = (Entity) o;
            if (e.canBeCollidedWith()) {
                Core.notify(player, c, NotifyStyle.FORCE, "Area blocked by entity");
                return false;
            }
        }
        return true;
    }
    
    @Override
    void onPlacedBy(EntityPlayer player, ItemStack is, int side) {
        DeltaCoord dc = getCornerDirection(player, side);
        List<Coord> area = getArea(getCoord(), dc);
        Coord myDestination = area.get(0);
        for (Coord c : area) {
            if (c.isSubmissiveTo(myDestination)) {
                myDestination = c;
            }
        }
        
        for (Coord spot : area) {
            if (!spot.equals(myDestination)) {
                TileEntityExtension tex = new TileEntityExtension();
                tex.setParent(this);
                spot.setAsTileEntityLocation(tex);
                tex.getBlockClass().enforce(spot);
            }
        }
        myDestination.setTE(this);
    }
    
    @Override
    void onRemove() {
        for (int dx = -5; dx <= 5; dx++) {
            for (int dy = -5; dy <= 5; dy++) {
                for (int dz = -5; dz <= 5; dz++) {
                    Coord c = getCoord().add(dx, dy, dz);
                    TileEntityExtension tex = c.getTE(TileEntityExtension.class);
                    if (tex == null) {
                        continue;
                    }
                    if (tex.getParent() == this) {
                        c.setId(0);
                    }
                }
            }
        }
    }

}
