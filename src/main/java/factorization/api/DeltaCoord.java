package factorization.api;

import com.google.common.base.Splitter;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import io.netty.buffer.ByteBuf;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3;

import java.io.IOException;

public class DeltaCoord implements IDataSerializable {
    public int x, y, z;

    public static final DeltaCoord ZERO = new DeltaCoord();
    public DeltaCoord() {
        x = y = z = 0;
    }

    public DeltaCoord(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public DeltaCoord(DeltaCoord orig) {
        this.x = orig.x;
        this.y = orig.y;
        this.z = orig.z;
    }

    public DeltaCoord(EnumFacing dir) {
        this(dir.getDirectionVec().getX(), dir.getDirectionVec().getY(), dir.getDirectionVec().getZ());
    }

    public DeltaCoord add(DeltaCoord o) {
        return new DeltaCoord(x + o.x, y + o.y, z + o.z);
    }

    public DeltaCoord add(int dx, int dy, int dz) {
        return new DeltaCoord(x + dx, y + dy, z + dz);
    }
    
    public DeltaCoord scale(double d) {
        return new DeltaCoord((int)(x*d), (int)(y*d), (int)(z*d));
    }

    public DeltaCoord incrScale(int s) {
        x *= s;
        y *= s;
        z *= s;
        return this;
    }

    public boolean isZero() {
        return x == 0 && y == 0 && z == 0;
    }

    @Override
    public String toString() {
        return "DeltaCoord(" + x + ", " + y + ", " + z + ")";
    }

    private static DeltaCoord d(int x, int y, int z) {
        return new DeltaCoord(x, y, z);
    }

    public static DeltaCoord directNeighbors[] = {
            d(+1, 0, 0),
            d(-1, 0, 0),
            d(0, -1, 0),
            d(0, +1, 0),
            d(0, 0, -1),
            d(0, 0, +1) };
    
    public static DeltaCoord flatNeighbors[] = {
        d(+1, 0, 0),
        d(-1, 0, 0),
        d(0, 0, -1),
        d(0, 0, +1) };

    public static final DeltaCoord[] directNeighborsPlusMe = new DeltaCoord[] {
            d(0, 0, 0),
            d(-1, 0, 0),
            d(+1, 0, 0),
            d(0, -1, 0),
            d(0, +1, 0),
            d(0, 0, -1),
            d(0, 0, +1),
    };


    public double getAngleHorizontal() {
        return Math.atan2(z, -x);
    }
    
    public EnumFacing getDirection() {
        EnumFacing[] values = EnumFacing.VALUES;
        for (int i = 0; i < values.length; i++) {
            EnumFacing d = values[i];
            if (d.getDirectionVec().getX() == x && d.getDirectionVec().getY() == y && d.getDirectionVec().getZ() == z) {
                return d;
            }
        }
        return null;
    }

    public int getFaceSide() {
        if (x == 0 && z == 0) {
            if (y == -1) {
                return 0;
            } else if (y == 1) {
                return 1;
            }
        } else if (y == 0 && x == 0) {
            if (z == -1) {
                return 2;
            } else if (z == 1) {
                return 3;
            }
        } else if (y == 0 && z == 0) {
            if (x == -1) {
                return 4;
            } else if (x == 1) {
                return 5;
            }
        }

        return -1;
    }

    public DeltaCoord reverse() {
        return new DeltaCoord(-x, -y, -z);
    }

    public boolean isSubmissive() {
        return x < 0 || y < 0 || z < 0;
    }

    public boolean equals(DeltaCoord o) {
        return x == o.x && y == o.y && z == o.z;
    }
    
    public void alignToAxis() {
        int ax = Math.abs(x);
        int ay = Math.abs(y);
        int az = Math.abs(z);
        if (ax >= ay && ax >= az) {
            x = (int) Math.signum(x);
            return;
        }
        if (ay >= ax && ay >= az) {
            y = (int) Math.signum(y);
            return;
        }
        if (az >= ay && az >= ax) {
            z = (int) Math.signum(z);
            return;
        }
        x = y = z = 0;
    }
    
    public int get(int id) {
        switch (id) {
        case 0: return x;
        case 1: return y;
        case 2: return z;
        default: throw new RuntimeException("not an dimension index");
        }
    }
    
    public void set(int id, int val) {
        switch (id) {
        case 0: x = val; break;
        case 1: y = val; break;
        case 2: z = val; break;
        default: throw new RuntimeException("not an dimension index");
        }
    }
    
    public void init(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    public void writeToTag(String prefix, NBTTagCompound tag) {
        tag.setInteger(prefix + "dx", x);
        tag.setInteger(prefix + "dy", y);
        tag.setInteger(prefix + "dz", z);
    }
    
    public static DeltaCoord readFromTag(String prefix, NBTTagCompound tag) {
        return new DeltaCoord(tag.getInteger(prefix + "dx"), tag.getInteger(prefix + "dy"), tag.getInteger(prefix + "dz"));
    }
    
    public static DeltaCoord read(ByteBuf di) throws IOException {
        return new DeltaCoord(di.readInt(), di.readInt(), di.readInt());
    }
    
    public void write(ByteBuf out) throws IOException {
        for (int i = 0; i < 3; i++) {
            out.writeInt(get(i));
        }
    }
    
    
    private static Splitter COMMA_SPLITTER = Splitter.on(',');
    public static DeltaCoord parse(String input) {
        DeltaCoord ret = new DeltaCoord();
        int i = 0;
        for (String s : COMMA_SPLITTER.split(input)) {
            ret.set(i, Integer.parseInt(s));
            i++;
        }
        return ret;
    }
    
    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        x = data.asSameShare(prefix + "dx").putInt(x);
        y = data.asSameShare(prefix + "dy").putInt(y);
        z = data.asSameShare(prefix + "dz").putInt(z);
        return this;
    }
    
    public double magnitude() {
        return Math.sqrt(x*x + y*y + z*z);
    }

    public Vec3 toVector() {
        return new Vec3(x, y, z);
    }

    public void move(EnumFacing dir) {
        x += dir.getDirectionVec().getX();
        y += dir.getDirectionVec().getY();
        z += dir.getDirectionVec().getZ();
    }
}
