package factorization.api;

import java.io.DataInput;
import java.io.IOException;
import java.util.ArrayList;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

public class VectorUV {
    public double x, y, z, u, v;
    
    public VectorUV() {
        this(0, 0, 0);
    }

    public VectorUV(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.u = 0;
        this.v = 0;
    }

    public VectorUV(double x, double y, double z, double u, double v) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.u = u;
        this.v = v;
    }

    public boolean equals(VectorUV other) {
        return this.x == other.x && this.y == other.y && this.z == other.z && this.u == other.u && this.v == other.v;
    }

    public void rotate(double a, double b, double c, double argtheta) {
        //Thanks to http://inside.mines.edu/~gmurray/ArbitraryAxisRotation/
        //Be sure to double-check the signs!
        double theta = Math.toRadians(argtheta);
        double ox = this.x, oy = this.y, oz = this.z;

        double cos_theta = (double) Math.cos(theta);
        double sin_theta = (double) Math.sin(theta);
        double product = (a * ox + b * oy + c * oz) * (1 - cos_theta);
        this.x = a * product + ox * cos_theta + (-c * oy + b * oz) * sin_theta;
        this.y = b * product + oy * cos_theta + (+c * ox - a * oz) * sin_theta;
        this.z = c * product + oz * cos_theta + (-b * ox + a * oy) * sin_theta;
    }

    public VectorUV add(int dx, int dy, int dz) {
        return new VectorUV(x + dx, y + dy, z + dz, u, v);
    }
    
    public VectorUV add(VectorUV o) {
        return new VectorUV(x + o.x, y + o.y, z + o.z, u, v);
    }
    
    public VectorUV subtract(VectorUV o) {
        return new VectorUV(x - o.x, y - o.y, z - o.z, u, v);
    }

    public void scale(double d) {
        x *= d;
        y *= d;
        z *= d;
    }

    public void incr(VectorUV d) {
        x += d.x;
        y += d.y;
        z += d.z;
    }

    public VectorUV copy() {
        return new VectorUV(x, y, z, u, v);
    }
    
    public VectorUV negate() {
        return new VectorUV(-x, -y, -z, u, v);
    }

    @Override
    public String toString() {
        return "<" + x + ", " + y + ", " + z + ": " + u + ", " + v + ">";
    }
    
    public void writeToTag(NBTTagCompound tag, String prefix) {
        tag.setFloat(prefix + "x", (float) x);
        tag.setFloat(prefix + "y", (float) y);
        tag.setFloat(prefix + "z", (float) z);
    }
    
    public static VectorUV readFromTag(NBTTagCompound tag, String prefix) {
        double x = tag.getFloat(prefix+"x");
        double y = tag.getFloat(prefix+"y");
        double z = tag.getFloat(prefix+"z");
        return new VectorUV(x, y, z);
    }
    
    public static VectorUV readFromDataInput(DataInput input) throws IOException {
        return new VectorUV(input.readFloat(), input.readFloat(), input.readFloat());
    }
    
    public void addInfoToArray(ArrayList<Object> args) {
        args.add((float)x);
        args.add((float)y);
        args.add((float)z);
    }
    

    public double get(int axis) {
        switch (axis) {
        case 0: return x;
        case 1: return y;
        case 2: return z;
        default: throw new RuntimeException("Invalid argument");
        }
    }
}
