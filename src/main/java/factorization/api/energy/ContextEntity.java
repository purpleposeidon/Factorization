package factorization.api.energy;

import com.google.common.base.Objects;
import factorization.api.adapter.InterfaceAdapter;
import net.minecraft.entity.Entity;

import javax.annotation.Nonnull;

public class ContextEntity implements IWorkerContext {
    public static final InterfaceAdapter<Entity, IWorker> adaptEntity = InterfaceAdapter.makeAdapter(IWorker.class);

    @Nonnull
    public final Entity ent;

    final IWorker<ContextEntity> cast;

    public ContextEntity(@Nonnull Entity ent) {
        this.ent = ent;
        // TODO: Forge capabilities?
        cast = adaptEntity.cast(ent);
    }

    @Override
    public IWorker.Accepted give(@Nonnull WorkUnit unit, boolean simulate) {
        return cast.accept(this, unit, simulate);
    }

    @Override
    public boolean isManageable() {
        return false;
    }

    @Override
    public boolean isValid() {
        return cast != null && !ent.isDead;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContextEntity that = (ContextEntity) o;
        return Objects.equal(ent, that.ent);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(ent);
    }
}
