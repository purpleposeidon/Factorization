package factorization.coremodhooks;

import factorization.flat.FlatChunkLayer;
import net.minecraft.entity.Entity;

import javax.annotation.Nonnull;

public interface IExtraChunkData {
    Entity[] empty_array_of_entities = new Entity[0];

    @Nonnull
    Entity[] getConstantColliders();
    void setConstantColliders(Entity[] constants);

    FlatChunkLayer getFlatLayer();
}
