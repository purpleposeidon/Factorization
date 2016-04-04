package factorization.api.energy;

import factorization.api.adapter.Adapter;

import javax.annotation.Nonnull;

/**
 * Instances of this interface give a reference to the 'this' of {@link IWorker#accept(IWorkerContext, WorkUnit, boolean)},
 * and includes things like what side was accessed from.
 * <p/>
 * Each ContextThing has public static {@link factorization.api.adapter.InterfaceAdapter} adaptThing.
 * Use {@link factorization.api.adapter.InterfaceAdapter#register(Adapter)} to provide an implementation of an
 * interface to a class you don't control.
 *
 * <p/>
 * See {@link ContextTileEntity} for an example implementation.
 */
public interface IWorkerContext {
    IWorker.Accepted give(@Nonnull WorkUnit unit, boolean simulate);
    boolean isManageable();
    boolean isValid();
}
