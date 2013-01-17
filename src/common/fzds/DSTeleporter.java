package factorization.fzds;

import net.minecraft.entity.Entity;
import net.minecraft.util.Vec3;
import net.minecraft.world.Teleporter;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import factorization.api.Coord;
import factorization.common.Core;

class DSTeleporter extends Teleporter {
    Coord destination;
    Vec3 preciseDestination;
    
    public DSTeleporter(WorldServer par1WorldServer) {
        super(par1WorldServer);
    }
    
    @Override
    public void placeInPortal(Entity player, double par2, double par4, double par6, float par8) {
        if (preciseDestination != null) {
            player.posX = preciseDestination.xCoord;
            player.posY = preciseDestination.yCoord;
            player.posZ = preciseDestination.zCoord;
            return;
        }
        destination.x--;
        destination.moveToTopBlock();
        if (player.worldObj == DimensionManager.getWorld(Core.dimension_slice_dimid)) {
            destination.y = Math.min(Hammer.wallHeight, destination.y);
        }
        destination.setAsEntityLocation(player);
        /*Coord below = new Coord(player);
        below = below.add(0, -3, 0);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                Coord platform = below.add(dx, 0, dz);
                if (platform.isAir()) {
                    platform.setId(Block.stone);
                }
            }
        }*/
    }
}