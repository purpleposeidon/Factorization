package factorization.fzds;

import net.minecraft.entity.Entity;
import net.minecraft.util.Vec3;
import net.minecraft.world.Teleporter;
import net.minecraft.world.WorldServer;
import factorization.api.Coord;

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
        destination.setAsEntityLocation(player);
    }
}