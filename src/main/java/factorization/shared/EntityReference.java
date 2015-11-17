package factorization.shared;

import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;

import java.io.IOException;
import java.util.UUID;

/**
 * A reference to an Entity by UUID. This allows tracking entities after deserialization.
 */
public class EntityReference<E extends Entity> implements IDataSerializable {
    public static interface OnFound<E extends Entity> {
        void found(E ent);
    }

    private World world;
    private E tracked_entity;
    private UUID entity_uuid = null_uuid;
    private OnFound<E> onFind = null;
    
    private static final UUID null_uuid = UUID.fromString("00000000-0000-0000-0000-000000000000");

    public EntityReference() { }

    public EntityReference(World world) {
        this.world = world;
    }

    public EntityReference(E ent) {
        world = ent.worldObj;
        trackEntity(ent);
    }

    public EntityReference(OnFound<E> informer) {
        whenFound(informer);
    }

    public EntityReference<E> whenFound(OnFound<E> informer) {
        this.onFind = informer;
        return this;
    }

    public void setWorld(World w) {
        world = w;
    }
    
    public void trackEntity(E ent) {
        if (world == null && ent != null) {
            world = ent.worldObj;
        }
        if (ent != null && ent.isDead) ent = null;
        tracked_entity = ent;
        if (tracked_entity == null) {
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
        return tracked_entity;
    }

    public E getLocatedEntity() {
        return tracked_entity;
    }
    
    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        UUID orig_id = entity_uuid;
        entity_uuid = data.asSameShare(prefix + "entity_uuid").putUUID(entity_uuid);
        if (data.isReader() && tracked_entity != null && !orig_id.equals(entity_uuid)) {
            tracked_entity = null;
            fails = 0;
        }
        return this;
    }

    
    protected transient int fails = 0;
    protected void fetchEntity() {
        if (world == null) return;
        if (fails++ > 4) {
            if (fails % 40 != 0) return;
        }
        for (Entity ent : (Iterable<Entity>)world.loadedEntityList) {
            if (entity_uuid.equals(ent.getUniqueID())) {
                tracked_entity = (E) ent;
                fails = 0;
                if (onFind != null) onFind.found(tracked_entity);
                break;
            }
        }
    }

    public boolean entityFound() {
        return tracked_entity != null;
    }

    public boolean trackedAndAlive() {
        if (!trackingEntity()) return false;
        if (!entityFound()) return true; // Probably still alive!
        return !tracked_entity.isDead;
    }

    public UUID getUUID() {
        return entity_uuid;
    }
}
