package factorization.api.energy;

import com.google.common.base.Objects;
import factorization.api.adapter.InterfaceAdapter;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;

public class ContextItemStack implements IWorkerContext {
    public static final InterfaceAdapter<Item, IWorker> adaptItem = InterfaceAdapter.makeAdapter(IWorker.class);

    public final ItemStack is;

    public ContextItemStack(ItemStack is) {
        this.is = is;
    }

    @Override
    public IWorker.Accepted give(@Nonnull WorkUnit unit, boolean simulate) {
        IWorker cast = adaptItem.cast(is.getItem());
        // TODO: Forge capabilities?
        return cast.accept(this, unit, simulate);
    }

    @Override
    public boolean isManageable() {
        return false;
    }

    @Override
    public boolean isValid() {
        return is.stackSize > 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ContextItemStack that = (ContextItemStack) o;
        return Objects.equal(is, that.is);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(is);
    }
}
