package factorization.shared;

import java.io.IOException;
import java.util.UUID;

import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;

/**
 * A reference to an Entity by UUID.
 */
public class EntityReference<E extends Entity> implements IDataSerializable {
    private final World world;
    E tracked_entity;
    UUID entity_uuid = null_uuid;
    
    private static final UUID null_uuid = UUID.fromString("00000000-0000-0000-0000-000000000000");
    
    public EntityReference(World world) {
        this.world = world;
    }
    
    public void trackEntity(E ent) {
        tracked_entity = ent;
        if (ent == null) {
            entity_uuid = null_uuid;
        } else {
            entity_uuid = ent.getUniqueID();
        }
    }
    
    public boolean trackingEntity() {
        return !null_uuid.equals(entity_uuid);
    }
    
    /**
     * @return the tracked entity. May return null if the entity isn't loaded. If the entity becomes loaded, it may not return it immediately.
     */
    public E getEntity() {
        if (tracked_entity == null) {
            if (!trackingEntity()) return null;
            fetchEntity();
        }
        return (E) tracked_entity;
    }
    
    
    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        entity_uuid = data.asSameShare(prefix + "entity_uuid").putUUID(entity_uuid);
        return this;
    }

    
    protected transient int fails = 0;
    protected void fetchEntity() {
        if (fails > 4) {
            if (fails % 40 != 0) return;
        }
        for (Entity ent : (Iterable<Entity>)world.loadedEntityList) {
            if (entity_uuid == ent.getUniqueID()) {
                tracked_entity = (E) ent;
                fails = 0;
                break;
            }
        }
        fails++;
    }
    
    
}
