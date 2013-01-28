package factorization.fzds;

import factorization.fzds.api.IFzdsEntryControl;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemInWorldManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;

public class PuppetPlayer extends EntityPlayerMP implements IFzdsEntryControl {

    public PuppetPlayer(MinecraftServer minecraftServer, World world,
            String string, ItemInWorldManager itemInWorldManager) {
        super(minecraftServer, world, string, itemInWorldManager);
        
    }
    
    public PuppetPlayer(World world) {
        super(MinecraftServer.getServer(), world, "[FZDS puppet]", new ItemInWorldManager(world));
    }

    @Override
    public boolean canEnter(DimensionSliceEntity dse) {
        return false;
    }

    @Override
    public boolean canExit(DimensionSliceEntity dse) {
        return true;
    }

    @Override
    public void onEnter(DimensionSliceEntity dse) {
        
    }

    @Override
    public void onExit(DimensionSliceEntity dse) {
        
    }

}
