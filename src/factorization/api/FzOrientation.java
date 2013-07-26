package factorization.api;

import net.minecraft.util.Vec3;
import net.minecraftforge.common.ForgeDirection;

public enum FzOrientation {
    FACE_DOWN_POINT_SOUTH(ForgeDirection.DOWN, ForgeDirection.SOUTH),
    FACE_DOWN_POINT_NORTH(ForgeDirection.DOWN, ForgeDirection.NORTH),
    FACE_DOWN_POINT_EAST(ForgeDirection.DOWN, ForgeDirection.EAST),
    FACE_DOWN_POINT_WEST(ForgeDirection.DOWN, ForgeDirection.WEST),

    FACE_UP_POINT_SOUTH(ForgeDirection.UP, ForgeDirection.SOUTH),
    FACE_UP_POINT_NORTH(ForgeDirection.UP, ForgeDirection.NORTH),
    FACE_UP_POINT_EAST(ForgeDirection.UP, ForgeDirection.EAST),
    FACE_UP_POINT_WEST(ForgeDirection.UP, ForgeDirection.WEST),

    FACE_NORTH_POINT_UP(ForgeDirection.NORTH, ForgeDirection.UP),
    FACE_NORTH_POINT_DOWN(ForgeDirection.NORTH, ForgeDirection.DOWN),
    FACE_NORTH_POINT_EAST(ForgeDirection.NORTH, ForgeDirection.EAST),
    FACE_NORTH_POINT_WEST(ForgeDirection.NORTH, ForgeDirection.WEST),

    FACE_SOUTH_POINT_UP(ForgeDirection.SOUTH, ForgeDirection.UP),
    FACE_SOUTH_POINT_DOWN(ForgeDirection.SOUTH, ForgeDirection.DOWN),
    FACE_SOUTH_POINT_EAST(ForgeDirection.SOUTH, ForgeDirection.EAST),
    FACE_SOUTH_POINT_WEST(ForgeDirection.SOUTH, ForgeDirection.WEST),

    FACE_WEST_POINT_UP(ForgeDirection.WEST, ForgeDirection.UP),
    FACE_WEST_POINT_DOWN(ForgeDirection.WEST, ForgeDirection.DOWN),
    FACE_WEST_POINT_SOUTH(ForgeDirection.WEST, ForgeDirection.SOUTH),
    FACE_WEST_POINT_NORTH(ForgeDirection.WEST, ForgeDirection.NORTH),

    FACE_EAST_POINT_UP(ForgeDirection.EAST, ForgeDirection.UP),
    FACE_EAST_POINT_DOWN(ForgeDirection.EAST, ForgeDirection.DOWN),
    FACE_EAST_POINT_SOUTH(ForgeDirection.EAST, ForgeDirection.SOUTH),
    FACE_EAST_POINT_NORTH(ForgeDirection.EAST, ForgeDirection.NORTH),


    FACE_UNKNOWN_POINT_UNKNOWN(ForgeDirection.UNKNOWN, ForgeDirection.UNKNOWN);
    
    static public final FzOrientation UNKNOWN = FACE_UNKNOWN_POINT_UNKNOWN;

    
    //#Java is an excellent language. Hence, this python script.
    //dirs = "DOWN UP NORTH SOUTH WEST EAST".split()
    //
    //RM = ( #Imported from ForgeDirection
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
    //	    if data[j] == j:
    //	      continue
    //	    face = dirs[i]
    //	    point = dirs[data[j]]
    //	    name = "FACE_{0}_POINT_{1}".format(face, point)
    //	    print("{0}(ForgeDirection.{1}, ForgeDirection.{2}),".format(name, face, point))
    //  print()
    //print("FACE_UNKNOWN_POINT_UNKNOWN(ForgeDirection.UNKNOWN, ForgeDirection.UNKNOWN);")
    
    /**
     * This value is what a Dispenser has. It can point in any of the 6 directions.
     */
    public final ForgeDirection facing;
    
    /**
     * This is what various RedPower-style machines add. It can only point in 4 directions. It can not point in the facing direction, nor in the opposite direction.
     */
    public final ForgeDirection top;
    
    private FzOrientation nextFaceRotation, prevFaceRotation;
    private int rotation;
    
    private static FzOrientation[] valuesCache = values();
    
    FzOrientation(ForgeDirection facing, ForgeDirection top) {
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
        if (valuesCache.length == 0) {
            throw new RuntimeException("lolwut");
        }
    }
    
    private void setup() {
        if (this == UNKNOWN) {
            nextFaceRotation = prevFaceRotation = this;
        }
        nextFaceRotation = find(facing, top.getRotation(facing));
        prevFaceRotation = find(facing, top.getRotation(facing).getRotation(facing).getRotation(facing));
    }
    
    private void setupRotation() {
        if (this == UNKNOWN) {
            return;
        }
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
    
    private static FzOrientation find(ForgeDirection f, ForgeDirection t) {
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
    
    public static FzOrientation getOrientation(int index) {
        if (index >= 0 && index < valuesCache.length) {
            return valuesCache[index];
        }
        return UNKNOWN;
    }
    
    public static FzOrientation fromDirection(ForgeDirection dir) {
        if (dir == ForgeDirection.UNKNOWN) {
            return UNKNOWN;
        }
        return valuesCache[dir.ordinal()*4];
    }
    
    /**
     * @param newTop
     * @return {@link FzOrientation} with the same direction, but facing newTop. If the top can't be change to that direction because it is already facing that direction, it returns UNKNOWN.
     */
    public FzOrientation pointTopTo(ForgeDirection newTop) {
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
        vec.xCoord = facing.offsetX;
        vec.yCoord = facing.offsetY;
        vec.zCoord = facing.offsetZ;
        vec.xCoord += top.offsetX;
        vec.yCoord += top.offsetY;
        vec.zCoord += top.offsetZ;
    }
    
    public Vec3 getDiagonalVector() {
        Vec3 ret = Vec3.createVectorHelper(0, 0, 0);
        setDiagonalVector(ret);
        return ret;
    }
}
