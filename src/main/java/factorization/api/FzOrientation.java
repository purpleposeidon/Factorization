package factorization.api;

import factorization.util.SpaceUtil;
import net.minecraft.util.Vec3;
import net.minecraft.util.EnumFacing;
import org.omg.CORBA.UNKNOWN;

public enum FzOrientation {
    FACE_DOWN_POINT_SOUTH(EnumFacing.DOWN, EnumFacing.SOUTH),
    FACE_DOWN_POINT_NORTH(EnumFacing.DOWN, EnumFacing.NORTH),
    FACE_DOWN_POINT_EAST(EnumFacing.DOWN, EnumFacing.EAST),
    FACE_DOWN_POINT_WEST(EnumFacing.DOWN, EnumFacing.WEST),

    FACE_UP_POINT_SOUTH(EnumFacing.UP, EnumFacing.SOUTH),
    FACE_UP_POINT_NORTH(EnumFacing.UP, EnumFacing.NORTH),
    FACE_UP_POINT_EAST(EnumFacing.UP, EnumFacing.EAST),
    FACE_UP_POINT_WEST(EnumFacing.UP, EnumFacing.WEST),

    FACE_NORTH_POINT_UP(EnumFacing.NORTH, EnumFacing.UP),
    FACE_NORTH_POINT_DOWN(EnumFacing.NORTH, EnumFacing.DOWN),
    FACE_NORTH_POINT_EAST(EnumFacing.NORTH, EnumFacing.EAST),
    FACE_NORTH_POINT_WEST(EnumFacing.NORTH, EnumFacing.WEST),

    FACE_SOUTH_POINT_UP(EnumFacing.SOUTH, EnumFacing.UP),
    FACE_SOUTH_POINT_DOWN(EnumFacing.SOUTH, EnumFacing.DOWN),
    FACE_SOUTH_POINT_EAST(EnumFacing.SOUTH, EnumFacing.EAST),
    FACE_SOUTH_POINT_WEST(EnumFacing.SOUTH, EnumFacing.WEST),

    FACE_WEST_POINT_UP(EnumFacing.WEST, EnumFacing.UP),
    FACE_WEST_POINT_DOWN(EnumFacing.WEST, EnumFacing.DOWN),
    FACE_WEST_POINT_SOUTH(EnumFacing.WEST, EnumFacing.SOUTH),
    FACE_WEST_POINT_NORTH(EnumFacing.WEST, EnumFacing.NORTH),

    FACE_EAST_POINT_UP(EnumFacing.EAST, EnumFacing.UP),
    FACE_EAST_POINT_DOWN(EnumFacing.EAST, EnumFacing.DOWN),
    FACE_EAST_POINT_SOUTH(EnumFacing.EAST, EnumFacing.SOUTH),
    FACE_EAST_POINT_NORTH(EnumFacing.EAST, EnumFacing.NORTH);


    
    //#Java is an excellent language. Hence, this python script.
    //dirs = "DOWN UP NORTH SOUTH WEST EAST".split()
    //
    //RM = ( #Imported from EnumFacing
    //  (0, 1, 4, 5, 3, 2),
    //  (0, 1, 5, 4, 2, 3),
    //  (5, 4, 2, 3, 0, 1),
    //  (4, 5, 2, 3, 1, 0),
    //  (2, 3, 1, 0, 4, 5),
    //  (3, 2, 0, 1, 4, 5),
    //  (0, 1, 2, 3, 4, 5),
    //)
    //
    //for i in range(len(RM)):
    //  data = RM[i]
    //  for j in data:
    //     if data[j] == j:
    //       continue
    //     face = dirs[i]
    //     point = dirs[data[j]]
    //     name = "FACE_{0}_POINT_{1}".format(face, point)
    //       print("{0}(EnumFacing.{1}, EnumFacing.{2}),".format(name, face, point))
    //  print()
    //print("FACE_UNKNOWN_POINT_UNKNOWN(null, null);")
    
    /**
     * This value is what a Dispenser has. It can point in any of the 6 directions.
     */
    public final EnumFacing facing;
    
    /**
     * This is what various RedPower-style machines add. It can only point in 4 directions. It can not point in the facing direction, nor in the opposite direction.
     */
    public final EnumFacing top;
    
    private FzOrientation nextFaceRotation, prevFaceRotation;
    private int rotation;
    private FzOrientation swapped;
    private EnumFacing[] dirRotations = new EnumFacing[EnumFacing.values().length]; // Admitedly we could just use values() here. But that's ugly.
    
    private static FzOrientation[] valuesCache = values();
    
    FzOrientation(EnumFacing facing, EnumFacing top) {
        this.facing = facing;
        this.top = top;
    }
    
    static {
        for (FzOrientation o : values()) {
            o.setup();
        }
        for (FzOrientation o : values()) {
            o.setupRotation();
        }
        for (FzOrientation o : values()) {
            for (FzOrientation t : values()) {
                if (o.facing == t.top && o.top == t.facing) {
                    o.swapped = t;
                    break;
                }
            }
        }
        for (FzOrientation o : values()) {
            o.setupDirectionRotation();
        }
        if (valuesCache.length == 0) {
            throw new RuntimeException("lolwut");
        }
    }

    private void setup() {
        nextFaceRotation = find(facing, top.getRotation(facing));
        prevFaceRotation = find(facing, top.getRotation(facing).getRotation(facing).getRotation(facing));
    }
    
    private void setupRotation() {
        int rcount = 0;
        FzOrientation head = fromDirection(facing);
        for (int i = 0; i < 5; i++) {
            if (head == this) {
                rotation = rcount;
            }
            rcount++;
            head = head.nextFaceRotation;
        }
    }

    private void setupDirectionRotation() {
        for (EnumFacing dir : EnumFacing.values()) {
            Vec3 v = SpaceUtil.fromDirection(dir);
            Quaternion.fromOrientation(this).applyRotation(v);
            dirRotations[dir.ordinal()] = SpaceUtil.round(v, null);
        }
    }
    
    private static FzOrientation find(EnumFacing f, EnumFacing t) {
        for (FzOrientation o : values()) {
            if (o.facing == f && o.top == t) {
                return o;
            }
        }
        return UNKNOWN;
    }
    
    
    public FzOrientation rotateOnFace(int count) {
        count = count % 4;
        if (count > 0) {
            FzOrientation here = this;
            while (count > 0) {
                count--;
                here = here.nextFaceRotation;
            }
            return here;
        } else if (count < 0) {
            FzOrientation here = this;
            while (count < 0) {
                count++;
                here = here.prevFaceRotation;
            }
            return here;
        } else {
            return this;
        }
    }
    
    public FzOrientation getNextRotationOnFace() {
        return nextFaceRotation;
    }
    
    public FzOrientation getPrevRotationOnFace() {
        return prevFaceRotation;
    }
    
    public FzOrientation getNextRotationOnTop() {
        return getSwapped().getNextRotationOnFace().getSwapped();
    }
    
    public FzOrientation getPrevRotationOnTop() {
        return getSwapped().getPrevRotationOnFace().getSwapped();
    }
    
    public FzOrientation rotateOnTop(int count) {
        return getSwapped().rotateOnFace(count).getSwapped();
    }
    
    public static FzOrientation getOrientation(int index) {
        if (index >= 0 && index < valuesCache.length) {
            return valuesCache[index];
        }
        return UNKNOWN;
    }
    
    public static FzOrientation fromDirection(EnumFacing dir) {
        if (dir == null) {
            return UNKNOWN;
        }
        return valuesCache[dir.ordinal()*4];
    }
    
    /**
     * @param newTop
     * @return {@link FzOrientation} with the same direction, but facing newTop. If the top can't be change to that direction because it is already facing that direction, it returns UNKNOWN.
     */
    public FzOrientation pointTopTo(EnumFacing newTop) {
        FzOrientation fzo = this;
        for (int i = 0; i < 4; i++) {
            if (fzo.top == newTop) {
                return fzo;
            }
            fzo = fzo.nextFaceRotation;
        }
        return UNKNOWN;
    }
    
    public int getRotation() {
        return rotation;
    }
    
    public void setDiagonalVector(Vec3 vec) {
        vec.xCoord = facing.getDirectionVec().getX();
        vec.yCoord = facing.getDirectionVec().getY();
        vec.zCoord = facing.getDirectionVec().getZ();
        vec.xCoord += top.getDirectionVec().getX();
        vec.yCoord += top.getDirectionVec().getY();
        vec.zCoord += top.getDirectionVec().getZ();
    }
    
    public Vec3 getDiagonalVector() {
        Vec3 ret = new Vec3(0, 0, 0);
        setDiagonalVector(ret);
        return ret;
    }
    
    public FzOrientation getSwapped() {
        return swapped;
    }

    public EnumFacing applyRotation(EnumFacing dir) {
        return dirRotations[dir.ordinal()];
    }
}
