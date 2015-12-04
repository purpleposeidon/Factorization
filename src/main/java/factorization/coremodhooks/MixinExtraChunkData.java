package factorization.coremodhooks;

import net.minecraft.entity.Entity;
import net.minecraft.world.chunk.Chunk;

public class MixinExtraChunkData extends Chunk implements IExtraChunkData {
    public MixinExtraChunkData() {
        super(null, 0, 0);
    }

    Entity[] constant_colliders;
    
    @Override
    public Entity[] getConstantColliders() {
        return constant_colliders;
    }

    @Override
    public void setConstantColliders(Entity[] constants) {
        constant_colliders = constants;
    }

}
