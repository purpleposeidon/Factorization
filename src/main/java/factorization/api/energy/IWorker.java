package factorization.api.energy;

import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * <p>An {@link IWorker} is a thing that can accept {@link WorkUnit}s. Various standard Minecraft classes may implement
 * this interface.</p>
 * <p>The energy net might not be prompt in handing the worker another WorkUnit when the worker needs it. For this reason,
 * workers should buffer a single WorkUnit so that they can immediately continue working after finish a task.</p>
 * <p/>
 * This energy API does <b>NOT</b> need to be exhaustively implemented, neither by workers nor by energy nets.
 * (Eg, battery block items are not obliged to be chargable, and wires are not obliged to spark power over to adjacent
 * entities.)
 *
 * @param <T> {@link factorization.api.ISaneCoord} (for Block), {@link net.minecraft.tileentity.TileEntity},
 *            {@link net.minecraft.item.ItemStack} (for Item), {@link net.minecraft.entity.Entity}.
 */
public interface IWorker<T> {
    enum Accepted {
        /**
         * The worker will never accept the unit.
         * The workers that are ISaneCoords and TileEntities may change their mind if they then trigger a
         * block update.
         * <p/>
         * It's not really defined what happens if an Entity or ItemStack changes its mind.
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
     * Receive a {@link WorkUnit} and get to work.
     * Machines that run continuously will need a buffer. For example, a sawmill might have a 100-tick buffer, and each
     * WorkUnit provides 50 ticks of operation.
     *
     * @param unit     The {@link WorkUnit}.
     * @param simulate if simulate is true, then return when the unit is needed. If simulate is false,
     *                 then the worker should start running.
     *                 <p/>
     * @param self     A reference to the worker; used for less stateful things like blocks and ItemStacks. If {@link T} is
     *                 {@link net.minecraft.tileentity.TileEntity} or {@link net.minecraft.entity.Entity}, then self is this.
     *                 If T is Item or Block, then self is the {@link ItemStack} or an {@link factorization.api.ISaneCoord}.
     *                 As a special consideration for ItemStacks, the unit provider should replace the stack with null if
     *                 the stacksize is null.
     * @param side     The side that the Block or TileEntity is being powered from. May be null, even in blocky contexts.
     * @param edge     The edge of the side being powered. May be null even if side is not null. If it is not null,
     *                 then side is guaranteed to not be null, and edge is guaranteed to not be parallel to side.
     * @return NEVER if the worker can't handle the unit, LATER if the worker doesn't need ti right
     * now, or NOW if the worker can use the unit immediately... unless simulate is false, in which case the return
     * value should be LATER, and any other value is ignored.
     */
    @Nonnull
    Accepted canHandle(@Nonnull WorkUnit unit, boolean simulate, @Nonnull T self, @Nullable EnumFacing side, @Nullable EnumFacing edge);
}
