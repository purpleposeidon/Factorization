package factorization.coremodhooks;

import factorization.flat.FlatChunkLayer;
import net.minecraft.entity.Entity;
import net.minecraft.world.chunk.Chunk;

import javax.annotation.Nonnull;

public class MixinExtraChunkData extends Chunk implements IExtraChunkData {
    public MixinExtraChunkData() {
        super(null, 0, 0);
    }

    Entity[] constant_colliders;
    
    @Override
    @Nonnull
    public Entity[] getConstantColliders() {
        if (constant_colliders == null) {
            return IExtraChunkData.empty_array_of_entities;
        }
        return constant_colliders;
    }

    @Override
    public void setConstantColliders(Entity[] constants) {
        if (constants == null || constants.length == 0) {
            constants = null;
        }
        constant_colliders = constants;
    }

    FlatChunkLayer flatLayer;

    public FlatChunkLayer getFlatLayer() {
        if (flatLayer == null) {
            flatLayer = new FlatChunkLayer();
        }
        return flatLayer;
    }

}
