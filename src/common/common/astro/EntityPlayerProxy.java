package factorization.common.astro;

import net.minecraft.server.MinecraftServer;
import net.minecraft.src.EntityPlayerMP;
import net.minecraft.src.ItemInWorldManager;
import net.minecraft.src.World;

public class EntityPlayerProxy extends EntityPlayerMP {

    public EntityPlayerProxy(MinecraftServer par1MinecraftServer,
            World par2World, String par3Str,
            ItemInWorldManager par4ItemInWorldManager) {
        super(par1MinecraftServer, par2World, par3Str, par4ItemInWorldManager);
    }

}
