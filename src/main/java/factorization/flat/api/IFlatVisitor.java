package factorization.flat.api;

import factorization.api.Coord;
import net.minecraft.util.EnumFacing;

import javax.annotation.Nonnull;

public interface IFlatVisitor {
    void visit(Coord at, EnumFacing side, @Nonnull FlatFace face);
}
