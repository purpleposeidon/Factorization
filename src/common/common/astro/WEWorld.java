package factorization.common.astro;

import net.minecraft.src.Entity;
import net.minecraft.src.IChunkProvider;
import net.minecraft.src.World;
import net.minecraft.src.WorldInfo;
import net.minecraft.src.WorldSettings;

class WEWorld extends World {
    WorldEntity worldEntity;
    World parentWorld;
    
    public WEWorld(WorldEntity worldEntity) {
        super(
                new VoidSaveHandler(),
                "worldEntity" + worldEntity.entityId,
                new WorldSettings(new WorldInfo(worldEntity.worldObj.getWorldInfo().getNBTTagCompound())),
                new WEWorldProvider(),
                worldEntity.worldObj.theProfiler);
        this.worldEntity = worldEntity;
        this.parentWorld = worldEntity.worldObj;
        setWorldTime(20*60*5);
    }
    
    @Override
    protected IChunkProvider createChunkProvider() {
        return new WEChunkProvider(this);
    }
    
    //Some things removed
    public void tick()
    {
        super.tick();
    }

    @Override
    public Entity getEntityByID(int var1) {
        return null;
    }
}