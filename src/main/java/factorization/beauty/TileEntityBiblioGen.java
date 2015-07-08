package factorization.beauty;

import factorization.api.Coord;
import factorization.api.ICoordFunction;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.Share;
import factorization.common.FactoryType;
import factorization.shared.BlockClass;
import factorization.shared.TileEntityCommon;

import java.io.IOException;

public class TileEntityBiblioGen extends TileEntityCommon {
    int bookCount = 0;

    @Override
    public BlockClass getBlockClass() {
        return BlockClass.Machine;
    }

    @Override
    public void putData(DataHelper data) throws IOException {
        bookCount = data.as(Share.PRIVATE, "bookCount").putInt(bookCount);
    }

    @Override
    public FactoryType getFactoryType() {
        return FactoryType.BIBLIO_GEN;
    }

    static int LIBRARY_RADIUS = 24;

    void countBooks() {
        bookCount = new BookCounter(this.getCoord()).count();
    }

    static class BookCounter implements ICoordFunction {
        int books = 0;
        final Coord min, max;

        BookCounter(Coord start) {
            min = start.add(-LIBRARY_RADIUS, -LIBRARY_RADIUS, -LIBRARY_RADIUS);
            max = start.add(+LIBRARY_RADIUS, +LIBRARY_RADIUS, +LIBRARY_RADIUS);
        }

        int count() {
            Coord.iterateCube(min, max, this);
            return books;
        }

        @Override
        public void handle(Coord here) {
            books += here.getBlock().getEnchantPowerBonus(here.w, here.x, here.y, here.z) > 0 ? 1 : 0;
        }
    }
}
