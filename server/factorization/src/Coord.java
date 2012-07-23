package factorization.src;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import net.minecraft.src.Block;
import net.minecraft.src.Entity;
import net.minecraft.src.TileEntity;
import net.minecraft.src.World;

public class Coord {
	public World w;
	public int x, y, z;
	private Random rand = new Random();

	public Coord(World w, int x, int y, int z) {
		this.w = w;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public Coord(TileEntity te) {
		this(te.worldObj, te.xCoord, te.yCoord, te.zCoord);
	}

	public Coord(Entity ent) {
		this(ent.worldObj, (int) ent.posX, (int) (ent.posY + ent.yOffset), (int) ent.posZ);
	}

	public Coord(World w, double x, double y, double z) {
		this(w, (int) x, (int) y, (int) z);
	}

	public String toString() {
		return "(" + x + ", " + y + ", " + z + ")";
	}

	@Override
	public int hashCode() {
		return (((x * 11) % 71) << 7) + ((z * 7) % 479) + y;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Coord) {
			Coord other = (Coord) obj;
			return x == other.x && y == other.y && z == other.z && w == other.w;
		}
		return false;
	}

	public Coord copy() {
		return new Coord(w, x, y, z);
	}

	public DeltaCoord difference(Coord b) {
		return new DeltaCoord(x - b.x, y - b.y, z - b.z);
	}

	public double distance(Coord o) {
		return Math.sqrt(distanceSq(o));
	}

	public int distanceSq(Coord o) {
		if (o == null) {
			return 0;
		}
		int dx = x - o.x, dy = y - o.y, dz = z - o.z;
		return dx * dx + dy * dy + dz * dz;
	}

	public int distanceManhatten(Coord o) {
		if (o == null) {
			return 0;
		}
		int dx = x - o.x, dy = y - o.y, dz = z - o.z;
		return Math.abs(dx) + Math.abs(dy) + Math.abs(dz);
	}

	public ArrayList<Coord> getNeighborsAdjacent() {
		ArrayList<Coord> ret = new ArrayList(6);
		for (DeltaCoord d : DeltaCoord.directNeighbors) {
			ret.add(this.add(d));
		}
		return ret;
	}

	public ArrayList<Coord> getNeighborsDiagonal() {
		ArrayList<Coord> ret = new ArrayList(26);
		for (int dx = -1; dx <= 1; dx++) {
			for (int dy = -1; dy <= 1; dy++) {
				for (int dz = -1; dz <= 1; dz++) {
					if (dx == 0 && dy == 0 && dz == 0) {
						continue;
					}
					ret.add(this.add(dx, dy, dz));
				}
			}
		}
		return ret;
	}

	public ArrayList<Coord> getRandomNeighborsAdjacent() {
		ArrayList<Coord> ret = getNeighborsAdjacent();
		Collections.shuffle(ret);
		return ret;
	}

	public ArrayList<Coord> getRandomNeighborsDiagonal() {
		ArrayList<Coord> ret = getNeighborsDiagonal();
		Collections.shuffle(ret);
		return ret;
	}

	public Coord getSingleRandomNeighborAdjacent() {
		int r = rand.nextInt(DeltaCoord.directNeighbors.length);
		return this.add(DeltaCoord.directNeighbors[r]);
	}

	//Impure methods
	public Coord add(DeltaCoord d) {
		return add(d.x, d.y, d.z);
	}

	public Coord add(int x, int y, int z) {
		return new Coord(w, this.x + x, this.y + y, this.z + z);
	}

	public void dirty() {
		w.markBlocksDirty(x, y, z, x, y, z);
	}

	public void updateLight() {
		w.updateAllLightTypes(x, y, z);
	}

	public void setTE(TileEntity te) {
		w.setBlockTileEntity(x, y, z, te);
	}

	/**
	 * Adjusts position. 0, 1: y; 2, 3: z; 4, 5: x
	 * 
	 */
	public Coord towardSide(int side) {
		switch (side) {
		case 0:
			y -= 1;
			break;
		case 1:
			y += 1;
			break;
		case 2:
			z -= 1;
			break;
		case 3:
			z += 1;
			break;
		case 4:
			x -= 1;
			break;
		case 5:
			x += 1;
			break;
		}
		return this;
	}

	//world gets/sets
	public TileEntity getTE() {
		return w.getBlockTileEntity(x, y, z);
	}

	public <T> T getTE(Class<T> clazz) {
		TileEntity te = getTE();
		if (clazz.isInstance(te)) {
			return (T) te;
		}
		return null;
	}

	public Block getBlock() {
		return Block.blocksList[getId()];
	}

	public int getId() {
		return w.getBlockId(x, y, z);
	}

	public int getMd() {
		return w.getBlockMetadata(x, y, z);
	}

	public boolean isAir() {
		return w.isAirBlock(x, y, z);
	}

	public boolean isSolid() {
		Block b = getBlock();
		if (b == null) {
			return false;
		}
		return getBlock().isBlockNormalCube(w, x, y, z);
	}

	public boolean is(Block b) {
		return getId() == b.blockID;
	}

	public boolean is(Block b, int md) {
		return getId() == b.blockID && getMd() == md;
	}

	public boolean setId(int id, boolean notify) {
		if (notify) {
			return w.setBlockWithNotify(x, y, z, id);
		}
		return w.setBlock(x, y, z, id);
	}

	public boolean setMd(int md, boolean notify) {
		if (notify) {
			w.setBlockMetadataWithNotify(x, y, z, md);
			return true;
		}
		return w.setBlockMetadata(x, y, z, md);
	}

	public boolean setIdMd(int id, int md, boolean notify) {
		if (notify) {
			return w.setBlockAndMetadataWithNotify(x, y, z, id, md);
		}
		return w.setBlockAndMetadata(x, y, z, id, md);
	}

	public boolean setId(int id) {
		return setId(id, true);
	}

	public boolean setMd(int md) {
		return setMd(md, true);
	}

	public boolean setIdMd(int id, int md) {
		return setIdMd(id, md, true);
	}

	public boolean setId(Block block) {
		return setId(block.blockID);
	}

}
