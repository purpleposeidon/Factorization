package factorization.common;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.monster.EntityCreeper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
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
                    int dz = dzc*dc.z;
                    ret.add(c.add(dx, dy, dz));
                }
            }
        }
        return ret;
    }
    
    DeltaCoord getCornerDirection(EntityPlayer player, int side) {
        ForgeDirection dir = ForgeDirection.getOrientation(side);
        DeltaCoord dc = FactorizationUtil.getFlatDiagonalFacing(player);
        if (dc.isZero()) {
            return null;
        }
        ForgeDirection fside = ForgeDirection.getOrientation(side);
        if (fside.offsetY == 0) {
            dc.x *= fside.offsetX != 0 ? -1 : 1;
            dc.z *= fside.offsetZ != 0 ? -1 : 1;
        }
        if (fside == ForgeDirection.DOWN) {
            dc.y = -1;
        } else {
            dc.y = 1;
        }
        for (int i = 0; i < 3; i++) {
            if (dc.get(i) == 0) {
                dc.set(i, 1);
            }
        }
        return dc;
    }
    
    @Override
    boolean canPlaceAgainst(EntityPlayer player, Coord c, int side) {
        if (player.worldObj.isRemote) {
            return false;
        }
        if (!c.isReplacable()) {
            c = c.towardSide(side);
        }
        DeltaCoord dc = getCornerDirection(player, side);
        if (dc == null) {
            Core.notify(player, c, "Place it differently");
            return false;
        }
        boolean fail = false;
        for (Coord spot : getArea(c, dc)) {
            if (!spot.isReplacable()) {
                if (fail == false) {
                    Core.clearNotifications(player);
                    fail = true;
                }
                if (!spot.equals(c)) {
                    Core.notify(player, spot, NotifyStyle.FORCE, "X");
                }
            }
        }
        if (fail) {
            Core.notify(player, c, NotifyStyle.FORCE, "Obstructed");
            return false;
        }
        AxisAlignedBB area = AxisAlignedBB.getBoundingBox(c.x, c.y, c.z, c.x, c.y, c.z);
        area = area.addCoord(2*dc.x, 3*dc.y, 2*dc.z);
        //double ao = 0.5;
        //area = area.offset(ao, ao, ao);
        for (Object o : c.w.getEntitiesWithinAABBExcludingEntity(null, area)) {
            Entity e = (Entity) o;
            if (e.canBeCollidedWith() || e instanceof EntityLiving || true) {
                Core.notify(player, c, NotifyStyle.FORCE, "Obstructed by entity");
                Coord ec = new Coord(e);
                if (!ec.equals(c)) {
                    String it = "(this guy)";
                    if (e instanceof EntityPlayer) {
                        it = "(this player)";
                    }
                    if (e instanceof EntityCreeper) {
                        it = "(thissss guy)";
                    }
                    Core.notify(player, new Coord(e), NotifyStyle.FORCE, it);
                }
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
        
        Coord here = getCoord();
        TileEntityRocketEngine base = this;
        if (!here.equals(myDestination)) {
            here.removeTE();
            base = new TileEntityRocketEngine();
            myDestination.setId(Core.factory_block_id);
            myDestination.setTE(base);
        }
        
        for (Coord spot : area) {
            if (!spot.equals(myDestination)) {
                spot.setId(Core.factory_block_id);
                TileEntityExtension tex = new TileEntityExtension(base);
                spot.setTE(tex);
                tex.getBlockClass().enforce(spot);
            }
            spot.redraw();
        }
    }
    
    @Override
    void onRemove() {
        Coord here = getCoord();
        for (int dx = -5; dx <= 5; dx++) {
            for (int dy = -5; dy <= 5; dy++) {
                for (int dz = -5; dz <= 5; dz++) {
                    Coord c = here.add(dx, dy, dz);
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
        here.setId(0);
    }
    
    @Override
    public void setBlockBounds(Block b) {
        b.setBlockBounds(0, 0, 0, 2, 3, 2);
    }

}
