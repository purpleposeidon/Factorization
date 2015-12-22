package factorization.api.energy;

import factorization.api.adapter.InterfaceAdapter;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;

import factorization.api.annotation.Nonnull;
import factorization.api.annotation.Nullable;

/**
 * <p>An {@link IWorker} is a thing that can accept {@link WorkUnit}s. Various standard Minecraft classes may implement
 * this interface.</p>
 * <p>The energy net might not be prompt in handing the worker another WorkUnit when the worker needs it. For this reason,
 * some kinds of workers may want to buffer a single WorkUnit so that they can immediately continue working after
 * finishing a task.</p>
 * <p/>
 * This energy API does <b>NOT</b> need to be exhaustively implemented, neither by workers nor by energy nets.
 * (Eg, battery block items are not obliged to be chargable, and wires are not obliged to spark power over to adjacent
 * entities.)
 *
 * @param <T> {@link net.minecraft.tileentity.TileEntity},  {@link net.minecraft.entity.Entity},
 *            or also {@link factorization.api.ISaneCoord} (for blocks), {@link net.minecraft.item.ItemStack} (for items).
 */
public interface IWorker<T> {
    /**
     * These adapters are used by power sources to give power to things that weren't coded to receive it,
     * without using ASM.
     */
    InterfaceAdapter<TileEntity, IWorker> adaptTileEntity = InterfaceAdapter.getExtra(IWorker.class),
            adaptEntity = InterfaceAdapter.getExtra(IWorker.class),
            adaptBlock = InterfaceAdapter.getExtra(IWorker.class),
            adaptItem = InterfaceAdapter.getExtra(IWorker.class);

    /**
     * Return value for {@link IWorker#canHandle(WorkUnit, boolean, Object, EnumFacing, EnumFacing)}.
     * If some internal state change (such as, perhaps, a power source upgrade) causes a transition between
     * NEVER to LATER/NOW, or from LATER/NOW to NEVER, then the powersource will appreciate notification of this.
     * For blocks and tileentities, this is done with a block update. This API defines no mechanism for this when
     * the worker is an Entity or an Item. (Perhaps the powersource polls regularly.)
     * <p/>
     * Transitions between LATER and NOW do not require notice to the powersource; it must poll the worker as
     * WorkUnits become available.
     */
    enum Accepted {
        /**
         * The worker can not accept the unit because it is incompatible.
         */
        NEVER,

        /**
         * The worker could accept the unit, but has a full buffer or something.
         */
        LATER,

        /**
         * The worker has room in its buffer for the unit.
         */
        NOW

        // Maybe add THANKS for when simulate is true?
    }

    /**
     * Receive a {@link WorkUnit} and get one unit of work done.
     * <p/>
     * Powersources may make a call with simulate set to false, without making a simulated check.
     * For this reason, WorkUnits should be discarded if there is nowhere for them to fit.
     * <p/>
     * Returning something other than NEVER may imply unit-specific ramifications; for example, rotational power may
     * want you to send a packet to the client to keep your gears rendering in sync with the driver. Such details can be
     * made accessible to the Worker via a custom WorkUnit class.
     *
     * @param unit     The {@link WorkUnit}.
     * @param simulate if simulate is true, then return when the unit is needed. If simulate is false,
     *                 then the worker should start doing what it does best.
     *                 <p/>
     * @param self     A reference to the worker; used for less stateful things like blocks and ItemStacks. If {@link T} is
     *                 {@link net.minecraft.tileentity.TileEntity} or {@link net.minecraft.entity.Entity}, then self is this,
     *                 and should be unused.
     *                 If T is Item or Block, then self is the {@link ItemStack} or an {@link factorization.api.ISaneCoord}.
     *                 As a special consideration for ItemStacks, the unit provider should replace the stack with null if
     *                 the stacksize becomes 0.
     * @param side     The side that the Block or TileEntity is being powered from. May be null, even in blocky contexts.
     * @param edge     The edge of the side being powered. May be null even if side is not null. If it is not null,
     *                 then side is guaranteed to not be null, and edge is guaranteed to not be parallel to side.
     * @return NEVER if the worker can't handle the unit, LATER if the worker doesn't need ti right
     * now, or NOW if the worker can use the unit immediately... unless simulate is false, in which case the return
     * value shall be LATER, and the powersource ignores the return value.
     */
    @Nonnull
    Accepted canHandle(@Nonnull WorkUnit unit, boolean simulate, @Nonnull T self, @Nullable EnumFacing side, @Nullable EnumFacing edge);
}
