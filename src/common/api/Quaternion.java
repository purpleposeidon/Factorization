package factorization.api;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.ForgeDirection;

public class Quaternion {
    public double w, x, y, z;
    
    
    //Data functions
    public Quaternion() {
        this(0, 0, 0, 0);
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
    
    public Quaternion(double w, Vec3 v) {
        this(w, v.xCoord, v.yCoord, v.zCoord);
    }
    
    @Override
    public String toString() {
        return "Quaternion(" + w + ", " + x + ", " + y + ", " + z + ")";
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
    
    public double[] toArray() {
        return new double[] { w, x, y, z };
    }
    
    public void update(double nw, double nx, double ny, double nz) {
        w = nw;
        x = nx;
        y = ny;
        z = nz;
    }
    
    public void updateVector(Vec3 v) {
        v.xCoord = x;
        v.yCoord = y;
        v.zCoord = z;
    }
    
    public Vec3 toVector() {
        return Vec3.createVectorHelper(x, y, z);
    }
    
    //Math functions
    public static Quaternion getRotationQuaternion(double angle, Vec3 axis) {
        double halfAngle = angle/2;
        double sin = Math.sin(halfAngle);
        return new Quaternion(Math.cos(halfAngle), axis.xCoord*sin, axis.yCoord*sin, axis.zCoord*sin);
    }
    
    public static Quaternion getRotationQuaternion(double angle, ForgeDirection axis) {
        double halfAngle = angle/2;
        double sin = Math.sin(halfAngle);
        return new Quaternion(Math.cos(halfAngle), axis.offsetX*sin, axis.offsetY*sin, axis.offsetZ*sin);
    }
    
    public static Quaternion getRotationQuaternion(double angle, double ax, double ay, double az) {
        double halfAngle = angle/2;
        double sin = Math.sin(halfAngle);
        return new Quaternion(Math.cos(halfAngle), ax*sin, ay*sin, az*sin);
    }
    
    /**
     * Also called the norm
     */
    public double magnitude() {
        return Math.sqrt(w*w + x*x + y*y + z*z);
    }
    
    public double incrDistance(Quaternion other) {
        incrAdd(other);
        return magnitude();
    }
    
    public void incrConjugate() {
        x *= -1;
        y *= -1;
        z *= -1;
    }
    
    public void incrAdd(Quaternion other) {
        w += other.w;
        x += other.x;
        y += other.y;
        z += other.z;
    }
    
    public void incrSubtract(Quaternion other) {
        w -= other.w;
        x -= other.x;
        y -= other.y;
        z -= other.z;
    }
    
    public void incrMultiply(Quaternion other) {
        double nw, nx, ny, nz;
        nw = w*other.w - x*other.x - y*other.y - z*other.z;
        nx = w*other.x + x*other.w + y*other.z - z*other.y;
        ny = w*other.y - x*other.z + y*other.w + z*other.x;
        nz = w*other.z + x*other.y - y*other.x + z*other.w;
        update(nw, nx, ny, nz);
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
    
    /**
     * Note: This assumes that this quaternion is normal (magnitude = 1).
     * @param p
     */
    public void rotateIncr(Vec3 p) {
        //return this * p * this^-1
        Quaternion point = new Quaternion(0, p);
        Quaternion trans = this.multiply(point).multiply(this.conjugate());
        p.xCoord = trans.x;
        p.yCoord = trans.y;
        p.zCoord = trans.z;
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
    
    public Quaternion subtract(Quaternion other) {
        Quaternion ret = new Quaternion(this);
        ret.incrSubtract(other);
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
}
