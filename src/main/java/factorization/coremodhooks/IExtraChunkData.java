package factorization.coremodhooks;

import net.minecraft.entity.Entity;

import javax.annotation.Nonnull;

public interface IExtraChunkData {
    Entity[] empty = new Entity[0];

    @Nonnull
    Entity[] getConstantColliders();
    void setConstantColliders(Entity[] constants);
}
