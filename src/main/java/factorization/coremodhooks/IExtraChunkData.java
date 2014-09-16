package factorization.coremodhooks;

import net.minecraft.entity.Entity;

public interface IExtraChunkData {
    Entity[] getConstantColliders();
    void setConstantColliders(Entity[] constants);
}
