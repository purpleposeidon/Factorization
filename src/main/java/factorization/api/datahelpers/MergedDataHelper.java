package factorization.api.datahelpers;

import factorization.api.FzOrientation;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraftforge.fluids.FluidTank;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

/**
 * An abstract base class for DataHelpers that aren't fussy about types.
 * Often used in more inventive contexts.
 * You probably shouldn't be using huge long chains of 'instanceof' in putImplementation.
 */
public abstract class MergedDataHelper extends DataHelper {
    protected abstract <E> E putImplementation(E o) throws IOException;

    @Override
    public final boolean putBoolean(boolean value) throws IOException {
        return putImplementation(value);
    }

    @Override
    public final byte putByte(byte value) throws IOException {
        return putImplementation(value);
    }

    @Override
    public final short putShort(short value) throws IOException {
        return putImplementation(value);
    }

    @Override
    public final int putInt(int value) throws IOException {
        return putImplementation(value);
    }

    @Override
    public final long putLong(long value) throws IOException {
        return putImplementation(value);
    }

    @Override
    public final float putFloat(float value) throws IOException {
        return putImplementation(value);
    }

    @Override
    public final double putDouble(double value) throws IOException {
        return putImplementation(value);
    }

    @Override
    public final String putString(String value) throws IOException {
        return putImplementation(value);
    }

    @Override
    public final FzOrientation putFzOrientation(FzOrientation value) throws IOException {
        return putImplementation(value);
    }

    @Override
    public final int[] putIntArray(int[] value) throws IOException {
        return putImplementation(value);
    }

    @Override
    public final NBTTagCompound putTag(NBTTagCompound value) throws IOException {
        return putImplementation(value);
    }

    @Override
    public final ItemStack[] putItemArray(ItemStack[] value) throws IOException {
        return putImplementation(value);
    }

    @Override
    public Vec3 putVec3(Vec3 val) throws IOException {
        return putImplementation(val);
    }

    @Override
    public ArrayList<ItemStack> putItemList(ArrayList<ItemStack> value) throws IOException {
        return putImplementation(value);
    }

    @Override
    public AxisAlignedBB putBox(AxisAlignedBB box) throws IOException {
        return putImplementation(box);
    }

    @Override
    public FluidTank putTank(FluidTank tank) throws IOException {
        return putImplementation(tank);
    }

    @Override
    public ItemStack putItemStack(ItemStack value) throws IOException {
        return putImplementation(value);
    }

    @Override
    public UUID putUUID(UUID uuid) throws IOException {
        return putImplementation(uuid);
    }

    @Override
    public Object putUnion(UnionEnumeration classes, Object val) throws IOException {
        return putImplementation(val);
    }

    @Override
    public <E extends Enum> E putEnum(E value) throws IOException {
        return putImplementation(value);
    }

    @Override
    public <E extends IDataSerializable> E putIDS(E val) throws IOException {
        return putImplementation(val);
    }
}
