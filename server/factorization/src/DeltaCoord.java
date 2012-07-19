package factorization.src;

public class DeltaCoord {
	int x, y, z;

	public DeltaCoord() {
		x = y = z = 0;
	}

	public DeltaCoord(int x, int y, int z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	static DeltaCoord c(int x, int y, int z) {
		return new DeltaCoord(x, y, z);
	}

	@Override
	public String toString() {
		return "DeltaCoord(" + x + ", " + y + ", " + z + ")";
	}

	static DeltaCoord directNeighbors[] = {
			c(+1, 0, 0),
			c(-1, 0, 0),
			c(0, -1, 0),
			c(0, +1, 0),
			c(0, 0, -1),
			c(0, 0, +1) };
}
