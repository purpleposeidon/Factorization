package factorization.common;

import java.io.DataInput;
import java.io.IOException;
import java.util.ArrayList;

import net.minecraft.src.Block;
import net.minecraft.src.NBTTagCompound;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Side;
import cpw.mods.fml.common.asm.SideOnly;
import factorization.api.MatrixTransform;
import factorization.api.VectorUV;

public class RenderingCube {
    int icon;
    public VectorUV corner;
    public MatrixTransform trans;
    public double ul, vl;

    /**
     * Creates a lovely cube used to render with. The vectors are in texels with the center of the tile as the origin. The rotations will also be done around
     * the center of the tile.
     */
    public RenderingCube(int icon, VectorUV corner) {
        this.corner = corner;
        this.trans = new MatrixTransform();
        setIcon(icon);
    }
    
    public RenderingCube(int icon, VectorUV corner, VectorUV offset) {
        this(icon, corner);
        if (offset != null) {
            trans.translate(offset.x, offset.y, offset.z);
        }
    }
    
    void writeToNBT(NBTTagCompound tag) {
        tag.setInteger("icon", icon);
        corner.writeToTag(tag, "c");
        trans.writeToTag(tag, "t");
    }
    
    static RenderingCube loadFromNBT(NBTTagCompound tag) {
        int icon = tag.getInteger("icon");
        VectorUV c = VectorUV.readFromTag(tag, "c");
        MatrixTransform trans = MatrixTransform.readFromTag(tag, "t");
        RenderingCube rc = new RenderingCube(icon, c);
        rc.trans = trans;
        return rc;
    }
    
    void writeToArray(ArrayList<Object> args) {
        args.add(icon);
        corner.addInfoToArray(args);
        trans.addToList(args);
    }
    
    static float takeFloat(ArrayList<Object> args) {
        return (Float) args.remove(0);
    }
    
    @SideOnly(Side.CLIENT)
    static RenderingCube readFromDataInput(DataInput input) throws IOException {
        int icon = input.readInt();
        VectorUV c = new VectorUV(input.readFloat(), input.readFloat(), input.readFloat());
        MatrixTransform trans = MatrixTransform.fromDataInput(input);
        RenderingCube ret = new RenderingCube(icon, c);
        ret.trans = trans;
        return ret;
    }
    
    public boolean equals(RenderingCube other) {
        return this.corner.equals(other.corner) && this.icon == other.icon && this.trans.equals(other.trans); 
    }

    public RenderingCube copy() {
        RenderingCube ret = new RenderingCube(this.icon, this.corner.copy());
        ret.ul = this.ul;
        ret.vl = this.vl;
        ret.trans = this.trans.copy();
        return ret;
    }
    
    public RenderingCube rotate(int x, int y, int z, int theta) {
        RenderingCube ret = copy();
        ret.trans.rotate(x, y, z, (float) Math.toRadians(theta));
        return ret;
    }
    
    public RenderingCube translate(int x, int y, int z) {
        RenderingCube ret = copy();
        ret.trans.translate(x, y, z);
        return ret;
    }
    
    public RenderingCube normalize() {
        //Can't do this anymore! Sorry. Blame the matrix.
        return this;
    }

//	public RenderingCube normalize() {
//		VectorUV newCorner = corner.copy();
//		VectorUV newOrigin = origin.copy();
//		newCorner.rotate(axis.x, axis.y, axis.z, theta);
//		newOrigin.rotate(axis.x, axis.y, axis.z, theta);
//		newCorner.x = Math.abs(newCorner.x);
//		newCorner.y = Math.abs(newCorner.y);
//		newCorner.z = Math.abs(newCorner.z);
//		return new RenderingCube(icon, newCorner, newOrigin);
//	}

//	public void toBlockBounds(Block b) {
//		float minX, maxX, minY, maxY, minZ, maxZ;
//		minX = minY = minZ = 9999;
//		maxX = maxY = maxZ = -minX;
//		for (int face = 0; face < 2; face++) {
//			//just need top & bottom face. That's probably what 0 and 1 are.
//			for (VectorUV vec : faceVerts(face)) {
//				vec = trans.apply(vec);
//				minX = Math.min(vec.x, minX);
//				minY = Math.min(vec.y, minY);
//				minZ = Math.min(vec.z, minZ);
//				
//				maxX = Math.min(vec.x, maxX);
//				maxY = Math.min(vec.y, maxY);
//				maxZ = Math.min(vec.z, maxZ);
//			}
//		}
//		b.setBlockBounds(minX / 16, minY / 16, minZ/16, maxX/16, maxY/16, maxZ/16);
//	}
    
    public void setIcon(int newIcon) {
        icon = newIcon;
        //XXX TODO NOTE: This might not work properly with large texture packs?
        ul = ((icon & 0xf) << 4) / 256.0;
        vl = (icon & 0xf0) / 256.0;
    }

    public VectorUV[] faceVerts(int face) {
        VectorUV ret[] = new VectorUV[4];
        VectorUV v = corner;
        int c = 8;
        switch (face) {
        case 0: //-y
            ret[0] = new VectorUV(v.x, -v.y, v.z);
            ret[1] = new VectorUV(-v.x, -v.y, v.z);
            ret[2] = new VectorUV(-v.x, -v.y, -v.z);
            ret[3] = new VectorUV(v.x, -v.y, -v.z);
            break;
        case 1: //+y
            ret[0] = new VectorUV(v.x, v.y, -v.z);
            ret[1] = new VectorUV(-v.x, v.y, -v.z);
            ret[2] = new VectorUV(-v.x, v.y, v.z);
            ret[3] = new VectorUV(v.x, v.y, v.z);
            break;
        case 2: //-z
            ret[0] = new VectorUV(v.x, v.y, -v.z);
            ret[1] = new VectorUV(v.x, -v.y, -v.z);
            ret[2] = new VectorUV(-v.x, -v.y, -v.z);
            ret[3] = new VectorUV(-v.x, v.y, -v.z);
            break;
        case 3: //+z
            ret[0] = new VectorUV(v.x, v.y, v.z);
            ret[1] = new VectorUV(-v.x, v.y, v.z);
            ret[2] = new VectorUV(-v.x, -v.y, v.z);
            ret[3] = new VectorUV(v.x, -v.y, v.z);
            break;
        case 4: //-x
            ret[0] = new VectorUV(-v.x, v.y, v.z);
            ret[1] = new VectorUV(-v.x, v.y, -v.z);
            ret[2] = new VectorUV(-v.x, -v.y, -v.z);
            ret[3] = new VectorUV(-v.x, -v.y, v.z);
            break;
        case 5: //+x
            ret[0] = new VectorUV(v.x, v.y, v.z);
            ret[1] = new VectorUV(v.x, -v.y, v.z);
            ret[2] = new VectorUV(v.x, -v.y, -v.z);
            ret[3] = new VectorUV(v.x, v.y, -v.z);
            break;
        }
        if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
            switch (face) {
            case 0: //-y
            case 1: //+y
                //Mirror these like MC does.
                for (VectorUV vert : ret) {
                    vert.u = vert.x + 8;
                    vert.v = vert.z + 8;
                }
                break;
            case 2: //-z
                for (VectorUV vert : ret) {
                    vert.u = 16 - (vert.x + 8);
                    vert.v = 16 - (vert.y + 8);
                }
                break;
            case 3: //+z
                for (VectorUV vert : ret) {
                    vert.u = vert.x + 8;
                    vert.v = 16 - (vert.y + 8);
                }
                break;
            case 4: //-x
                for (VectorUV vert : ret) {
                    vert.u = 16 - (vert.y + 8);
                    vert.v = (vert.z + 8);
                }
                break;
            case 5: //+x
                for (VectorUV vert : ret) {
                    vert.u = 16 - (vert.y + 8);
                    vert.v = 16 - (vert.z + 8);
                }
                break;
            }
            for (VectorUV main : ret) {
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
                for (VectorUV other : ret) {
                    other.u -= udelta;
                    other.v -= vdelta;
                }
            }
        }
        for (int i = 0; i < ret.length; i++) {
            ret[i] = trans.apply(ret[i]);
        }
        return ret;
    }

}
