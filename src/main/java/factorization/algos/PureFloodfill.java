package factorization.algos;

import factorization.api.Coord;
import factorization.api.DeltaCoord;
import factorization.api.ICoordFunction;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.ArrayList;

public abstract class PureFloodfill extends ScanlineFloodfill implements ICoordFunction {
    private final Coord start, end;
    private final DeltaCoord size;
    private final byte buffer[];
    private Coord cursor = null, saved = null;
    private byte saved_buffer = 0;

    private static class Neighbor {
        final DeltaCoord offset;
        final int indexOffset;
        boolean isOpen = false;

        private Neighbor(ForgeDirection dir, int indexOffset) {
            this.indexOffset = indexOffset;
            this.offset = new DeltaCoord(dir.offsetX, dir.offsetY, dir.offsetZ);
        }

        public void open(boolean setOpen, Coord unoffsetCursor, FastBag<Coord> toVisit) {
            if (!isOpen && setOpen) {
                final Coord found = unoffsetCursor.add(offset);
                toVisit.add(found);
            }
            isOpen = setOpen;
        }

        public void close() {
            isOpen = false;
        }
    }

    private final Neighbor[] neighbors;
    private final FastBag<Coord> toVisit = new FastBag<Coord>();

    protected PureFloodfill(Coord start, Coord end, Coord origin) {
        Coord.sort(start, end);
        this.start = start;
        this.end = end;
        this.size = end.difference(start).add(1, 1, 1);
        int dim = size.x * size.y * size.z;

        {
            beginScan(start);
            int zero = (indexYZ + start.x);
            ArrayList<Neighbor> s = new ArrayList<Neighbor>(4);
            for (ForgeDirection fd : new ForgeDirection[] { ForgeDirection.UP, ForgeDirection.DOWN, ForgeDirection.NORTH, ForgeDirection.SOUTH }) {
                final Coord spot = start.add(fd);
                beginScan(spot);
                int d = (indexYZ + spot.x) - zero;
                s.add(new Neighbor(fd, d));
            }
            neighbors = s.toArray(new Neighbor[4]);
        }

        toVisit.add(origin);

        buffer = new byte[dim];
        Coord.iterateCube(start, end, this);
    }

    @Override
    public final void handle(Coord here) {
        final byte v = convert(here);
        beginScan(here);
        final int index = indexYZ + here.x;
        if (index < 0 || index >= buffer.length) {
            return;
        }
        buffer[index] = v;
    }

    void resetNeighbors() {
        for (Neighbor neighbor : neighbors) {
            neighbor.close();
        }
    }

    /**
     * Convert a world position to a byte
     * @param here The coordinate
     * @return 0 if the location should not be visited, or some other value if it should be.
     */
    protected abstract byte convert(Coord here);

    /**
     * Called as the scanline floodfill progresses. Will not be invoked twice for a single position.
     * @param here The location
     * @param val The value that was stored at the location by {@link factorization.algos.PureFloodfill#convert}
     */
    protected abstract void visit(Coord here, byte val);

    /**
     * @param here The data value at the current location
     * @param next The data value of a potential later location
     * @return true if 'next' is a value that can be visited from 'here'
     */
    protected abstract boolean canTransition(byte here, byte next);

    protected int indexYZ;
    private void beginScan(Coord at) {
        indexYZ = (at.z - start.z) * (size.x) + (at.y - start.y) * (size.x * size.z) - start.x;
    }

    @Override
    protected void savePlace() {
        saved = cursor.copy();
        beginScan(cursor);
        saved_buffer = buffer[indexYZ + cursor.x];
        for (Neighbor neighbor : neighbors) {
            neighbor.close();
        }
    }

    @Override
    protected void restorePlace() {
        cursor = saved;
        cursor.x--;
        beginScan(cursor);
        int index = indexYZ + cursor.x;
        byte nextValue = buffer[index];
        if (!canTransition(saved_buffer, nextValue)) {
            cursor.x = start.x - 1; // Cause early exit
        }
    }

    @Override
    protected boolean forward() {
        if (cursor.x > end.x) return false;
        int index = indexYZ + cursor.x;
        byte value = buffer[index];
        boolean next = visitIndex(index, value);
        cursor.x++;
        return next && cursor.x <= end.x && canTransition(value, buffer[index + 1]);
    }

    @Override
    protected boolean backward() {
        if (cursor.x < start.x) return false;
        int index = indexYZ + cursor.x;
        byte value = buffer[index];
        boolean next = visitIndex(index, value);
        cursor.x--;
        return next && cursor.x >= start.x && canTransition(value, buffer[index - 1]);
    }

    boolean visitIndex(int index, byte value) {
        buffer[index] = 0;
        if (value != 0) {
            visit(cursor, value);
            for (Neighbor neighbor : neighbors) {
                final int neighborIndex = index + neighbor.indexOffset;
                if (neighborIndex > 0 && neighborIndex < buffer.length) {
                    final byte neighborValue = buffer[neighborIndex];
                    if (canTransition(value, neighborValue)) {
                        neighbor.open(neighborValue > 0, cursor, toVisit);
                    }
                }
            }
            return true;
        } else {
            for (Neighbor neighbor : neighbors) {
                neighbor.close();
            }
            return false;
        }
    }

    @Override
    protected void popQueue() {
        cursor = toVisit.remove(0);
    }

    public boolean calculate(int iterationLimit) {
        while (iterationLimit-- > 0 && !toVisit.isEmpty()) {
            tick();
        }
        return !toVisit.isEmpty();
    }
}
