package factorization.servo.instructions;

import com.google.common.collect.Maps;
import factorization.api.Coord;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.flat.api.IFlatModel;
import factorization.flat.api.IModelMaker;
import factorization.servo.rail.Instruction;
import net.minecraft.util.EnumFacing;

import java.io.IOException;
import java.util.HashMap;

/** Base class for single-icon no-state instructions. Fz-only. Make your own copy? */
abstract class SimpleInstruction extends Instruction {
    @Override
    public final IDataSerializable putData(String prefix, DataHelper data) throws IOException {
        return this;
    }

    @Override
    public final String getName() {
        return "fz.instruction." + getSimpleName();
    }

    protected abstract String getSimpleName();

    private static final HashMap<String, IFlatModel> map = Maps.newHashMap();
    @Override
    public final IFlatModel getModel(Coord at, EnumFacing side) {
        return map.get(getSimpleName());
    }

    @Override
    protected final void loadModels(IModelMaker maker) {
        map.put(getSimpleName(), reg(maker, getSimpleName()));
    }
}
