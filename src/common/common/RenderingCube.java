package factorization.common;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Side;
import net.minecraft.src.Block;

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

        void rotate(float u, float v, float w, float argtheta) {
            //Thanks to http://inside.mines.edu/~gmurray/ArbitraryAxisRotation/
            //Be sure to double-check the signs!
            double theta = Math.toRadians(argtheta);
            float ox = this.x, oy = this.y, oz = this.z;

            float cos_theta = (float) Math.cos(theta);
            float sin_theta = (float) Math.sin(theta);
            float product = (u * ox + v * oy + w * oz) * (1 - cos_theta);
            this.x = u * product + ox * cos_theta + (-w * oy + v * oz) * sin_theta;
            this.y = v * product + oy * cos_theta + (+w * ox - u * oz) * sin_theta;
            this.z = w * product + oz * cos_theta + (-v * ox + u * oy) * sin_theta;
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
        this.icon = icon;
        if (origin == null) {
            origin = new Vector(0, 0, 0, 0, 0);
        }
        this.corner = corner;
        this.origin = origin;

        //XXX TODO NOTE: This might not work properly with large texture packs?
        ul = ((icon & 0xf) << 4) / 256.0;
        vl = (icon & 0xf0) / 256.0;
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
            for (Vector vert : ret) {
                vert.u = Math.max(0, Math.min(vert.u, 16));
                vert.v = Math.max(0, Math.min(vert.v, 16));
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
