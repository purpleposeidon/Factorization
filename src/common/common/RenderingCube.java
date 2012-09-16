package factorization.common;

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
    }

    int icon;
    Vector corner, origin;
    public double ul, vl;
    float ax, ay, az, theta;

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

        setIcon(icon);
    }
    
    void writeToNBT(NBTTagCompound tag) {
        tag.setInteger("icon", icon);
        tag.setFloat("cx", corner.x);
        tag.setFloat("cy", corner.y);
        tag.setFloat("cz", corner.z);
        tag.setFloat("ox", origin.x);
        tag.setFloat("oy", origin.y);
        tag.setFloat("oz", origin.z);
    }
    
    static RenderingCube loadFromNBT(NBTTagCompound tag) {
        int icon = tag.getInteger("icon");
        Vector c = new Vector(tag.getFloat("cx"), tag.getFloat("cy"), tag.getFloat("cz"));
        Vector o = new Vector(tag.getFloat("ox"), tag.getFloat("oy"), tag.getFloat("oz"));
        return new RenderingCube(icon, c, o);
    }
    
    void writeToArray(ArrayList<Object> args) {
        args.add(icon);
        args.add(corner.x);
        args.add(corner.y);
        args.add(corner.z);
        args.add(origin.x);
        args.add(origin.y);
        args.add(origin.z);
    }
    
    static float takeFloat(ArrayList<Object> args) {
        return (Float) args.remove(0);
    }
    
    static RenderingCube readFromArray(ArrayList<Object> args) {
        int icon = (Integer) args.remove(0);
        Vector c = new Vector(takeFloat(args), takeFloat(args), takeFloat(args));
        Vector o = new Vector(takeFloat(args), takeFloat(args), takeFloat(args));
        return new RenderingCube(icon, c, o);
    }
    
    public boolean equals(RenderingCube other) {
        return this.corner.equals(other.corner) && this.origin.equals(other.origin) && this.icon == other.icon; 
    }

    public RenderingCube copy() {
        RenderingCube ret = new RenderingCube(this.icon, this.corner.copy(), this.origin.copy());
        ret.ul = this.ul;
        ret.vl = this.vl;
        ret.ax = this.ax;
        ret.ay = this.ay;
        ret.az = this.az;
        ret.theta = this.theta;
        return ret;
    }

    public RenderingCube normalize() {
        Vector newCorner = corner.copy();
        Vector newOrigin = origin.copy();
        newCorner.rotate(ax, ay, az, theta);
        newOrigin.rotate(ax, ay, az, theta);
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
            this.ax = this.ay = this.az = this.theta = 0;
            return this;
        }
        this.ax = ax;
        this.ay = ay;
        this.az = az;
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
                vert.rotate(ax, ay, az, theta);
            }
        }
        return ret;
    }

}
