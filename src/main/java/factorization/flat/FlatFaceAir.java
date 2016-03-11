package factorization.flat;

import factorization.api.Coord;
import factorization.flat.api.FlatFace;
import factorization.flat.api.IBoxList;
import factorization.flat.api.IFlatModel;
import factorization.flat.api.IModelMaker;
import net.minecraft.entity.Entity;
import net.minecraft.util.EnumFacing;

import javax.annotation.Nullable;

public final class FlatFaceAir extends FlatFace {
    public static final FlatFace INSTANCE = new FlatFaceAir();

    @Override
    @Nullable
    public IFlatModel getModel(Coord at, EnumFacing side) {
        return null;
    }

    @Override
    public void loadModels(IModelMaker uhm) {
    }

    @Override
    public boolean isReplaceable(Coord at, EnumFacing side) {
        return true;
    }

    @Override
    public boolean isNull() {
        return true;
    }

    @Override
    public void listSelectionBounds(Coord at, EnumFacing side, Entity player, IBoxList list) {
    }

    @Override
    public void spawnParticle(Coord at, EnumFacing side) {
    }

    @Override
    public void playSound(Coord at, EnumFacing side, boolean placed) {
    }
}
