package factorization.api;

import factorization.util.SpaceUtil;
import io.netty.buffer.ByteBuf;

import java.io.DataInput;
import java.io.DataOutputStream;
import java.io.IOException;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.util.ForgeDirection;

import org.lwjgl.opengl.GL11;

import com.google.common.io.ByteArrayDataOutput;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;

public class Quaternion implements IDataSerializable {
    public double w, x, y, z;
    
    //Data functions
    public Quaternion() {
        this(1, 0, 0, 0);
    }
    
    public Quaternion(double w, double x, double y, double z) {
        this.w = w;
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    public Quaternion(Quaternion orig) {
        this.w = orig.w;
        this.x = orig.x;
        this.y = orig.y;
        this.z = orig.z;
    }
    
    public Quaternion(double[] init) {
        this(init[0], init[1], init[2], init[3]);
        assert init.length == 4;
    }
    
    public void loadFrom(VectorUV v) {
        this.w = 1;
        this.x = v.x;
        this.y = v.y;
        this.z = v.z;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Quaternion) {
            Quaternion other = (Quaternion) obj;
            return w == other.w && x == other.x && y == other.y && z == other.z;
        }
        return false;
    }
    
    @Override
    public String toString() {
        String m = "";
        double mag = this.magnitude();
        if (mag != 1.0) {
            m = " MAG=" + mag;
        }
        return "Q<w=" + w + ", " + x + ", " + y + ", " + z + ">" + m;
    }
    
    public void writeToTag(NBTTagCompound tag, String prefix) {
        tag.setDouble(prefix+"w", w);
        tag.setDouble(prefix+"x", x);
        tag.setDouble(prefix+"y", y);
        tag.setDouble(prefix+"z", z);
    }
    
    public static Quaternion loadFromTag(NBTTagCompound tag, String prefix) {
        return new Quaternion(tag.getDouble(prefix+"w"), tag.getDouble(prefix+"x"), tag.getDouble(prefix+"y"), tag.getDouble(prefix+"z"));
    }
    
    public void write(ByteArrayDataOutput out) {
        double[] d = toStaticArray();
        for (int i = 0; i < d.length; i++) {
            out.writeDouble(d[i]);
        }
    }
    
    public void write(ByteBuf out) {
        double[] d = toStaticArray();
        for (int i = 0; i < d.length; i++) {
            out.writeDouble(d[i]);
        }
    }
    
    public void write(DataOutputStream out) throws IOException {
        double[] d = toStaticArray();
        for (int i = 0; i < d.length; i++) {
            out.writeDouble(d[i]);
        }
    }
    
    public static Quaternion read(DataInput in) throws IOException {
        double[] d = localStaticArray.get();
        for (int i = 0; i < d.length; i++) {
            d[i] = in.readDouble();
        }
        return new Quaternion(d);
    }
    
    public static Quaternion read(ByteBuf in) throws IOException {
        double[] d = localStaticArray.get();
        for (int i = 0; i < d.length; i++) {
            d[i] = in.readDouble();
        }
        return new Quaternion(d);
    }
    
    @Override
    public IDataSerializable serialize(String name_prefix, DataHelper data) throws IOException {
        w = data.asSameShare(name_prefix + "w").put(w);
        x = data.asSameShare(name_prefix + "x").put(x);
        y = data.asSameShare(name_prefix + "y").put(y);
        z = data.asSameShare(name_prefix + "z").put(z);
        return this;
    }
    
    public double[] fillArray(double[] out) {
        out[0] = w;
        out[1] = x;
        out[2] = y;
        out[3] = z;
        return out;
    }
    
    public double[] toArray() {
        return fillArray(new double[4]);
    }
    
    private static ThreadLocal<double[]> localStaticArray = new ThreadLocal<double[]>() {
        @Override
        protected double[] initialValue() {
            return new double[4];
        };
    };
    
    public double[] toStaticArray() {
        return fillArray(localStaticArray.get());
    }
    
    public boolean isZero() {
        return x == 0 && y == 0 && z == 0;
    }
    
    public void update(double nw, double nx, double ny, double nz) {
        w = nw;
        x = nx;
        y = ny;
        z = nz;
    }
    
    public void update(Quaternion other) {
        update(other.w, other.x, other.y, other.z);
    }
    
    public void update(ForgeDirection dir) {
        update(w, dir.offsetX, dir.offsetY, dir.offsetZ);
    }
    
    public void update(Vec3 v) {
        update(0, v.xCoord, v.yCoord, v.zCoord);
    }
    
    public void copyToVector(Vec3 v) {
        v.xCoord = x;
        v.yCoord = y;
        v.zCoord = z;
    }
    
    public Vec3 toVector() {
        return Vec3.createVectorHelper(x, y, z);
    }
    
    /**
     * @return a vector parallel with the imaginary components with length equal to the rotation
     */
    public Vec3 toRotationVector() {
        Vec3 rotVec = toVector().normalize();
        SpaceUtil.incrScale(rotVec, getAngleRadians());
        return rotVec;
    }
    
    public double getAngleRadians() {
        return 2 * Math.acos(w);
    }
    
    //Math functions
    public void incrNormalize() {
        double normSquared = magnitudeSquared();
        if (normSquared == 1 || normSquared == 0) {
            return;
        }
        double norm = Math.sqrt(normSquared);
        w /= norm;
        x /= norm;
        y /= norm;
        z /= norm;
    }

    /**
     * I think this is broken? Use Quaternion.fromOrientation
     */
    @Deprecated
    public static Quaternion getRotationQuaternion(FzOrientation orient) {
        return getRotationQuaternionRadians(Math.toRadians(orient.getRotation()*90), orient.facing);
    }
    
    public static Quaternion getRotationQuaternionRadians(double angle, Vec3 axis) {
        double halfAngle = angle/2;
        double sin = Math.sin(halfAngle);
        return new Quaternion(Math.cos(halfAngle), axis.xCoord*sin, axis.yCoord*sin, axis.zCoord*sin);
    }
    
    public static Quaternion getRotationQuaternionRadians(double angle, ForgeDirection axis) {
        double halfAngle = angle/2;
        double sin = Math.sin(halfAngle);
        return new Quaternion(Math.cos(halfAngle), axis.offsetX*sin, axis.offsetY*sin, axis.offsetZ*sin);
    }
    
    public static Quaternion getRotationQuaternionRadians(double angle, double ax, double ay, double az) {
        double halfAngle = angle/2;
        double sin = Math.sin(halfAngle);
        return new Quaternion(Math.cos(halfAngle), ax*sin, ay*sin, az*sin);
    }
    
    private static Quaternion[] quat_cache = new Quaternion[25 /*FzOrientation.values().length recursive reference, bleh*/];
    /***
     * @param An {@link FzOrientation}
     * @return A {@link Quaternion} that should not be mutated. It 
     */
    public static Quaternion fromOrientation(final FzOrientation orient) {
        final int ord = orient.ordinal();
        if (quat_cache[ord] != null) {
            return quat_cache[ord];
        }
        if (orient == FzOrientation.UNKNOWN) {
            return quat_cache[ord] = new Quaternion();
        }
        final Quaternion q1;
        final double quart = Math.toRadians(90);
        int rotation = orient.getRotation();
        switch (orient.facing) {
        case UP: {
            q1 = Quaternion.getRotationQuaternionRadians(0*quart, ForgeDirection.WEST);
            rotation = 5 - rotation;
            break;
        }
        case DOWN: {
            q1 = Quaternion.getRotationQuaternionRadians(2*quart, ForgeDirection.WEST);
            rotation = 3 - rotation;
            break;
        }
        case NORTH: {
            q1 = Quaternion.getRotationQuaternionRadians(1*quart, ForgeDirection.WEST);
            rotation = 5 - rotation;
            break;
        }
        case SOUTH: {
            q1 = Quaternion.getRotationQuaternionRadians(-1*quart, ForgeDirection.WEST);
            rotation = 3 - rotation;
            break;
        }
        case EAST: {
            q1 = Quaternion.getRotationQuaternionRadians(1*quart, ForgeDirection.NORTH);
            //rotation = 3 - rotation;
            rotation += Math.abs(orient.top.offsetZ)*2;
            break;
        }
        case WEST: {
            q1 = Quaternion.getRotationQuaternionRadians(-1*quart, ForgeDirection.NORTH);
            rotation += Math.abs(orient.top.offsetY)*2;
            break;
        }
        default: return quat_cache[ord] = new Quaternion(); //Won't happen
        }
        final Quaternion q2 = Quaternion.getRotationQuaternionRadians(rotation*quart, orient.facing);
        q2.incrMultiply(q1);
        return quat_cache[ord] = q2;
    }
    
    /**
     * @param Vec3 that gets mutated to the axis of rotation
     * @return the rotation
     */
    public double setVector(Vec3 axis) {
        double halfAngle = Math.acos(w);
        double sin = Math.sin(halfAngle);
        axis.xCoord = x/sin;
        axis.yCoord = y/sin;
        axis.zCoord = z/sin;
        return halfAngle*2;
    }
    
    @SideOnly(Side.CLIENT)
    public void glRotate() {
        double halfAngle = Math.acos(w);
        double sin = Math.sin(halfAngle);
        GL11.glRotatef((float) Math.toDegrees(halfAngle*2), (float) (x/sin), (float) (y/sin), (float) (z/sin));
    }
    
    public double dotProduct(Quaternion other) {
        return w*other.w + x*other.x + y*other.y + z*other.z;
    }
    
    public void incrLerp(Quaternion other, double t) {
        other.incrAdd(this, -1);
        other.incrScale(t);
        this.incrAdd(other);
        this.incrNormalize();
    }
    
    public Quaternion lerp(Quaternion other, double t) {
        Quaternion ret = new Quaternion(this);
        ret.incrLerp(other, t);
        return ret;
    }
    
    /**
     * When this Quaternion is going to be interpolated to other, it can be interpolated either the long way around, or the short way.
     * This method makes sure it will be the short interpolation.
     */
    public void incrShortFor(Quaternion other) {
        double cosom = this.dotProduct(other);
        if (cosom < 0) {
            incrScale(-1);
        }
    }
    
    public void incrLongFor(Quaternion other) {
        double cosom = this.dotProduct(other);
        if (cosom > 0) {
            incrScale(-1);
        }
    }
    
    public Quaternion slerp(Quaternion other, double t) {
        // from blender/blenlib/intern/math_rotation.c interp_qt_qtqt
        double cosom = this.dotProduct(other);
        // We don't make the dot product > 0, because maybe we'd like long-ways rotation some times
        double omega, sinom, sc1, sc2;

        if ((1.0f - cosom) > 0.0001f) {
            omega = Math.acos(cosom);
            sinom = Math.sin(omega);
            sc1 = Math.sin((1 - t) * omega) / sinom;
            sc2 = Math.sin(t * omega) / sinom;
        } else {
            sc1 = 1.0f - t;
            sc2 = t;
        }
        
        return new Quaternion(
                sc1 * this.w + sc2 * other.w,
                sc1 * this.x + sc2 * other.x,
                sc1 * this.y + sc2 * other.y,
                sc1 * this.z + sc2 * other.z);
    }

    public Quaternion shortSlerp(Quaternion other, double t) {
        // See the other slerp
        double cosom = this.dotProduct(other);
        boolean rev = cosom < 0;
        if (rev) {
            cosom = -cosom;
            other.incrScale(-1);
        }
        double omega, sinom, sc1, sc2;

        if ((1.0f - cosom) > 0.0001f) {
            omega = Math.acos(cosom);
            sinom = Math.sin(omega);
            sc1 = Math.sin((1 - t) * omega) / sinom;
            sc2 = Math.sin(t * omega) / sinom;
        } else {
            sc1 = 1.0f - t;
            sc2 = t;
        }

        Quaternion ret = new Quaternion(
                sc1 * this.w + sc2 * other.w,
                sc1 * this.x + sc2 * other.x,
                sc1 * this.y + sc2 * other.y,
                sc1 * this.z + sc2 * other.z);
        if (rev) other.incrScale(-1);
        return ret;
    }
    
    
    public double getAngleBetween(Quaternion other) {
        double dot = dotProduct(other);
        dot = Math.max(-1, Math.min(1, dot));
        return Math.acos(dot);
    }
    
    /**
     * Also called the norm
     */
    public double magnitude() {
        return Math.sqrt(w*w + x*x + y*y + z*z);
    }
    
    public double magnitudeSquared() {
        return w*w + x*x + y*y + z*z;
    }
    
    public double incrDistance(Quaternion other) {
        incrAdd(other);
        return magnitude();
    }
    
    public void incrConjugate() {
        x = -x;
        y = -y;
        z = -z;
    }
    
    public void incrAdd(Quaternion other) {
        w += other.w;
        x += other.x;
        y += other.y;
        z += other.z;
    }
    
    public void incrAdd(Quaternion other, double scale) {
        w += other.w*scale;
        x += other.x*scale;
        y += other.y*scale;
        z += other.z*scale;
    }
    
    public void incrMultiply(Quaternion other) {
        double nw, nx, ny, nz;
        nw = w*other.w - x*other.x - y*other.y - z*other.z;
        nx = w*other.x + x*other.w + y*other.z - z*other.y;
        ny = w*other.y - x*other.z + y*other.w + z*other.x;
        nz = w*other.z + x*other.y - y*other.x + z*other.w;
        update(nw, nx, ny, nz);
    }
    
    /** 
     * Acts like {@link incrMultiply}, but the argument gets incremented instead of this.
     */
    public void incrToOtherMultiply(Quaternion other) {
        double nw, nx, ny, nz;
        nw = w*other.w - x*other.x - y*other.y - z*other.z;
        nx = w*other.x + x*other.w + y*other.z - z*other.y;
        ny = w*other.y - x*other.z + y*other.w + z*other.x;
        nz = w*other.z + x*other.y - y*other.x + z*other.w;
        other.update(nw, nx, ny, nz);
    }
    
    public void incrScale(double scaler) {
        this.w *= scaler;
        this.x *= scaler;
        this.y *= scaler;
        this.z *= scaler;
    }
    
    public void incrUnit() {
        incrScale(1/magnitude());
    }
    
    public void incrReciprocal() {
        double m = magnitude();
        incrConjugate();
        incrScale(1/(m*m));
    }
    
    public void incrCross(Quaternion other) {
        double X = this.y * other.z - this.z * other.y;
        double Y = this.z * other.x - this.x * other.z;
        double Z = this.x * other.y - this.y * other.x;
        this.x = X;
        this.y = Y;
        this.z = Z;
    }
    
    public Quaternion cross(Quaternion other) {
        Quaternion m = new Quaternion(this);
        m.incrCross(other);
        return m;
    }
    
    public void incrRotateBy(Quaternion rotation) {
        rotation.incrToOtherMultiply(this);
        rotation.incrConjugate();
        this.incrMultiply(rotation);
        rotation.incrConjugate();
    }
    
    /**
     * Note: This assumes that this quaternion is normal (magnitude = 1).
     * @param p
     */
    public void applyRotation(Vec3 p) {
        //return this * p * this^-1
        if (this.isZero()) {
            return;
        }
        if (_vector_conversion_cache == null) {
            _vector_conversion_cache = new Quaternion();
        }
        Quaternion point = _vector_conversion_cache;
        point.update(p);
        this.incrToOtherMultiply(point);
        this.incrConjugate();
        point.incrMultiply(this);
        this.incrConjugate();
        point.copyToVector(p);
    }
    
    private Quaternion _vector_conversion_cache = null;
    
    public void applyReverseRotation(Vec3 p) {
        incrConjugate();
        applyRotation(p);
        incrConjugate();
    }
    
    private static Vec3 uvCache = Vec3.createVectorHelper(0, 0, 0);
    public void applyRotation(VectorUV vec) {
        uvCache.xCoord = vec.x;
        uvCache.yCoord = vec.y;
        uvCache.zCoord = vec.z;
        applyRotation(uvCache);
        vec.x = uvCache.xCoord;
        vec.y = uvCache.yCoord;
        vec.z = uvCache.zCoord;
    }
    
    //Other math forms
    public double distance(Quaternion other) {
        return add(other).magnitude();
    }
    
    public Quaternion conjugate() {
        Quaternion ret = new Quaternion(this);
        ret.incrConjugate();
        return ret;
    }
    
    public Quaternion add(Quaternion other) {
        Quaternion ret = new Quaternion(this);
        ret.incrAdd(other);
        return ret;
    }
    
    public Quaternion add(Quaternion other, double scale) {
        Quaternion ret = new Quaternion(this);
        ret.incrAdd(other, scale);
        return ret;
    }
    
    public Quaternion multiply(Quaternion other) {
        Quaternion a = new Quaternion(this);
        a.incrMultiply(other);
        return a;
    }
    
    public Quaternion scale(double scaler) {
        Quaternion a = new Quaternion(this);
        a.incrScale(scaler);
        return a;
    }
    
    public Quaternion unit() {
        Quaternion r = new Quaternion(this);
        r.incrUnit();
        return r;
    }
    
    public Quaternion reciprocal() {
        Quaternion r = new Quaternion(this);
        r.incrReciprocal();
        return r;
    }
    
    public Quaternion power(double alpha) {
        // http://en.wikipedia.org/wiki/Quaternion#Exponential.2C_logarithm.2C_and_power
        double norm = this.magnitude();
        double theta = Math.acos(w / norm);
        double qa = Math.pow(norm, alpha);
        double alphaTheta = alpha * theta;
        double W = qa * Math.cos(alpha * theta);
        double sat = Math.sin(alphaTheta);
        return new Quaternion(W, x * sat, y * sat, z * sat);
    }

    public boolean hasNaN() {
        return Double.isNaN(w) || Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z);
    }

    public boolean hasInf() {
        return Double.isInfinite(w) || Double.isInfinite(x) || Double.isInfinite(y) || Double.isInfinite(z);
    }

    public Quaternion cleanAbnormalNumbers() {
        if (hasNaN() || hasInf()) return new Quaternion();
        return this;
    }
    
}
