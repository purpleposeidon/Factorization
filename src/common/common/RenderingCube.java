package factorization.common;

import java.io.DataInput;
import java.io.IOException;
import java.util.ArrayList;

import net.minecraft.src.Block;
import net.minecraft.src.NBTTagCompound;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Side;

public class RenderingCube {
    public static class Vector {
        public float x, y, z, u, v;

        public Vector(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.u = 0;
            this.v = 0;
        }

        public Vector(float x, float y, float z, float u, float v) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.u = u;
            this.v = v;
        }
        
        public boolean equals(Vector other) {
            return this.x == other.x && this.y == other.y && this.z == other.z && this.u == other.u && this.v == other.v;
        }

        void rotate(float a, float b, float c, float argtheta) {
            //Thanks to http://inside.mines.edu/~gmurray/ArbitraryAxisRotation/
            //Be sure to double-check the signs!
            double theta = Math.toRadians(argtheta);
            float ox = this.x, oy = this.y, oz = this.z;

            float cos_theta = (float) Math.cos(theta);
            float sin_theta = (float) Math.sin(theta);
            float product = (a * ox + b * oy + c * oz) * (1 - cos_theta);
            this.x = a * product + ox * cos_theta + (-c * oy + b * oz) * sin_theta;
            this.y = b * product + oy * cos_theta + (+c * ox - a * oz) * sin_theta;
            this.z = c * product + oz * cos_theta + (-b * ox + a * oy) * sin_theta;
        }

        public Vector add(int dx, int dy, int dz) {
            return new Vector(x + dx, y + dy, z + dz, u, v);
        }

        void scale(float d) {
            x *= d;
            y *= d;
            z *= d;
        }

        void incr(Vector d) {
            x += d.x;
            y += d.y;
            z += d.z;
        }

        Vector copy() {
            return new Vector(x, y, z, u, v);
        }

        @Override
        public String toString() {
            return "<" + x + ", " + y + ", " + z + ">";
        }
        
        public void writeToTag(NBTTagCompound tag, String prefix) {
            tag.setFloat(prefix + "x", x);
            tag.setFloat(prefix + "y", y);
            tag.setFloat(prefix + "z", z);
        }
        
        public static Vector readFromTag(NBTTagCompound tag, String prefix) {
            float x = tag.getFloat(prefix+"x");
            float y = tag.getFloat(prefix+"y");
            float z = tag.getFloat(prefix+"z");
            return new Vector(x, y, z);
        }
        
        public static Vector readFromDataInput(DataInput input) throws IOException {
            return new Vector(input.readFloat(), input.readFloat(), input.readFloat());
        }
        
        void addInfoToArray(ArrayList<Object> args) {
            args.add(x);
            args.add(y);
            args.add(z);
        }
    }

    int icon;
    public Vector corner, origin, axis;
    public double ul, vl;
    public float theta;

    /**
     * Creates a lovely cube used to render with. The vectors are in texels with the center of the tile as the origin. The rotations will also be done around
     * the center of the tile.
     */
    public RenderingCube(int icon, Vector corner, Vector origin) {
        if (origin == null) {
            origin = new Vector(0, 0, 0, 0, 0);
        }
        this.corner = corner;
        this.origin = origin;
        this.axis = new Vector(0, 0, 0);
        this.theta = 0;

        setIcon(icon);
    }
    
    void writeToNBT(NBTTagCompound tag) {
        tag.setInteger("icon", icon);
        corner.writeToTag(tag, "c");
        origin.writeToTag(tag, "o");
        axis.writeToTag(tag, "a");
        tag.setFloat("theta", theta);
    }
    
    static RenderingCube loadFromNBT(NBTTagCompound tag) {
        int icon = tag.getInteger("icon");
        Vector c = Vector.readFromTag(tag, "c");
        Vector o = Vector.readFromTag(tag, "o");
        Vector a = Vector.readFromTag(tag, "a");
        RenderingCube rc = new RenderingCube(icon, c, o);
        rc.axis = a;
        rc.theta = tag.getFloat("theta");
        return rc;
    }
    
    void writeToArray(ArrayList<Object> args) {
        args.add(icon);
        corner.addInfoToArray(args);
        origin.addInfoToArray(args);
        axis.addInfoToArray(args);
        args.add(theta);
    }
    
    static float takeFloat(ArrayList<Object> args) {
        return (Float) args.remove(0);
    }
    
    static RenderingCube readFromArray(ArrayList<Object> args) {
        int icon = (Integer) args.remove(0);
        Vector c = new Vector(takeFloat(args), takeFloat(args), takeFloat(args));
        Vector o = new Vector(takeFloat(args), takeFloat(args), takeFloat(args));
        Vector a = new Vector(takeFloat(args), takeFloat(args), takeFloat(args));
        RenderingCube rc = new RenderingCube(icon, c, o);
        rc.axis = a;
        rc.theta = takeFloat(args);
        return rc;
    }
    
    public boolean equals(RenderingCube other) {
        return this.corner.equals(other.corner) && this.origin.equals(other.origin) && this.icon == other.icon; 
    }

    public RenderingCube copy() {
        RenderingCube ret = new RenderingCube(this.icon, this.corner.copy(), this.origin.copy());
        ret.ul = this.ul;
        ret.vl = this.vl;
        ret.axis = this.axis.copy();
        ret.theta = this.theta;
        return ret;
    }

    public RenderingCube normalize() {
        Vector newCorner = corner.copy();
        Vector newOrigin = origin.copy();
        newCorner.rotate(axis.x, axis.y, axis.z, theta);
        newOrigin.rotate(axis.x, axis.y, axis.z, theta);
        newCorner.x = Math.abs(newCorner.x);
        newCorner.y = Math.abs(newCorner.y);
        newCorner.z = Math.abs(newCorner.z);
        return new RenderingCube(icon, newCorner, newOrigin);
    }

    public void toBlockBounds(Block b) {
        RenderingCube cube = normalize();
        Vector c = cube.corner;
        Vector o = cube.origin;
        c.scale(1F / 16F);
        o = o.add(8, 8, 8);
        o.scale(1F / 16F);
        b.setBlockBounds(
                o.x - c.x, o.y - c.y, o.z - c.z,
                o.x + c.x, o.y + c.y, o.z + c.z);
    }

    public RenderingCube rotate(double ax, double ay, double az, int theta) {
        return rotate((float) ax, (float) ay, (float) az, theta);
    }

    public RenderingCube rotate(float ax, float ay, float az, int theta) {
        if (theta == 0) {
            this.axis = new Vector(0, 0, 0);
            this.theta = 0;
            return this;
        }
        this.axis = new Vector(ax, ay, az);
        this.theta = theta;
        return this;
    }
    
    public void setIcon(int newIcon) {
        icon = newIcon;
        //XXX TODO NOTE: This might not work properly with large texture packs?
        ul = ((icon & 0xf) << 4) / 256.0;
        vl = (icon & 0xf0) / 256.0;
    }

    public Vector[] faceVerts(int face) {
        Vector ret[] = new Vector[4];
        Vector v = corner;
        int c = 8;
        switch (face) {
        case 0: //-y
            ret[0] = new Vector(v.x, -v.y, v.z);
            ret[1] = new Vector(-v.x, -v.y, v.z);
            ret[2] = new Vector(-v.x, -v.y, -v.z);
            ret[3] = new Vector(v.x, -v.y, -v.z);
            break;
        case 1: //+y
            ret[0] = new Vector(v.x, v.y, -v.z);
            ret[1] = new Vector(-v.x, v.y, -v.z);
            ret[2] = new Vector(-v.x, v.y, v.z);
            ret[3] = new Vector(v.x, v.y, v.z);
            break;
        case 2: //-z
            ret[0] = new Vector(v.x, v.y, -v.z);
            ret[1] = new Vector(v.x, -v.y, -v.z);
            ret[2] = new Vector(-v.x, -v.y, -v.z);
            ret[3] = new Vector(-v.x, v.y, -v.z);
            break;
        case 3: //+z
            ret[0] = new Vector(v.x, v.y, v.z);
            ret[1] = new Vector(-v.x, v.y, v.z);
            ret[2] = new Vector(-v.x, -v.y, v.z);
            ret[3] = new Vector(v.x, -v.y, v.z);
            break;
        case 4: //-x
            ret[0] = new Vector(-v.x, v.y, v.z);
            ret[1] = new Vector(-v.x, v.y, -v.z);
            ret[2] = new Vector(-v.x, -v.y, -v.z);
            ret[3] = new Vector(-v.x, -v.y, v.z);
            break;
        case 5: //+x
            ret[0] = new Vector(v.x, v.y, v.z);
            ret[1] = new Vector(v.x, -v.y, v.z);
            ret[2] = new Vector(v.x, -v.y, -v.z);
            ret[3] = new Vector(v.x, v.y, -v.z);
            break;
        }
        for (Vector vert : ret) {
            vert.incr(origin);
        }
        if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
            switch (face) {
            case 0: //-y
            case 1: //+y
                //Mirror these like MC does.
                for (Vector vert : ret) {
                    vert.u = vert.x + 8;
                    vert.v = vert.z + 8;
                }
                break;
            case 2: //-z
                for (Vector vert : ret) {
                    vert.u = 16 - (vert.x + 8);
                    vert.v = 16 - (vert.y + 8);
                }
                break;
            case 3: //+z
                for (Vector vert : ret) {
                    vert.u = vert.x + 8;
                    vert.v = 16 - (vert.y + 8);
                }
                break;
            case 4: //-x
                for (Vector vert : ret) {
                    vert.u = 16 - (vert.y + 8);
                    vert.v = (vert.z + 8);
                }
                break;
            case 5: //+x
                for (Vector vert : ret) {
                    vert.u = 16 - (vert.y + 8);
                    vert.v = 16 - (vert.z + 8);
                }
                break;
            }
            for (Vector main : ret) {
                float udelta = 0, vdelta = 0;
                int nada = 0;
                if (main.u > 16) {
                    udelta = main.u - 16;
                } else if (main.u < 0) {
                    udelta = main.u;
                } else {
                    nada++;
                }
                if (main.v > 16) {
                    vdelta = main.v - 16;
                } else if (main.v < 0) {
                    vdelta = main.v;
                } else {
                    nada++;
                }
                if (nada == 2) {
                    continue;
                }
                for (Vector other : ret) {
                    other.u -= udelta;
                    other.v -= vdelta;
                }
                //vert.u = Math.max(0, Math.min(vert.u, 16));
                //vert.v = Math.max(0, Math.min(vert.v, 16));
            }
        }
        if (theta != 0) {
            for (Vector vert : ret) {
                vert.rotate(axis.x, axis.y, axis.z, theta);
            }
        }
        return ret;
    }

}
