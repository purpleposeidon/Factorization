package factorization.fzds;

import factorization.api.Coord;
import net.minecraft.entity.Entity;
import net.minecraft.util.Vec3;
import net.minecraft.world.Teleporter;
import net.minecraft.world.WorldServer;

class DSTeleporter extends Teleporter {
    Coord destination;
    Vec3 preciseDestination;
    
    public DSTeleporter(WorldServer par1WorldServer) {
        super(par1WorldServer);
    }

    @Override
    public void placeInPortal(Entity player, float rotationYaw) {
        if (preciseDestination != null) {
            player.posX = preciseDestination.xCoord;
            player.posY = preciseDestination.yCoord;
            player.posZ = preciseDestination.zCoord;
            return;
        }
        destination.setAsEntityLocation(player);
    }

}