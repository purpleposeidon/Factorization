package factorization.src;

import java.util.ArrayList;
import java.util.HashSet;

import net.minecraft.src.NBTTagCompound;

public class Charge {
	private int charge;

	public Charge() {
		this.charge = 0;
	}

	public int getCharge() {
		return charge;
	}

	public void setCharge(int newCharge) {
		this.charge = Math.max(0, newCharge);
	}

	public int addCharge(int chargeToAdd) {
		setCharge(charge + chargeToAdd);
		return charge;
	}

	public void writeToTag(NBTTagCompound tag, String name) {
		tag.setInteger(name, charge);
	}

	static public Charge readFromTag(NBTTagCompound tag, String name) {
		Charge ret = new Charge();
		ret.setCharge(tag.getInteger(name));
		return ret;
	}

	private void swapWith(Charge other) {
		int a = this.charge, b = other.charge;
		this.setCharge(b);
		other.setCharge(a);
	}

	//These are some functions for users to make good & healthy use of
	/**
	 * This function spreads charge around with a random conducting neighbor of src. Call it every tick.
	 * 
	 * @param te
	 *            A conductive TileEntity
	 */
	public static void update(IChargeConductor te) {
		Charge me = te.getCharge();
		//This is a fine place for an error if me == null
		if (me.charge <= 0) {
			return;
		}

		for (Coord neighbor : te.getCoord().getRandomNeighborsAdjacent()) {
			IChargeConductor n = neighbor.getTE(IChargeConductor.class);
			if (n == null) {
				continue;
			}
			me.swapWith(n.getCharge());
			return;
		}
	}

	private static ArrayList<Coord> frontier = new ArrayList(5 * 5 * 4);
	private static HashSet<Coord> visited = new HashSet(5 * 5 * 5);

	/**
	 * Gets the average charge in the nearby connected network
	 * 
	 * @param start
	 *            where to measure from
	 * @param maxDistance
	 *            Only checks this range. Manhatten distance.
	 * @return
	 */
	public static double getChargeDensity(IChargeConductor start, int maxDistance) {
		int totalCharge = 0;
		frontier.clear();
		visited.clear();
		frontier.add(start.getCoord());
		while (frontier.size() > 0) {
			Coord hereCoord = frontier.remove(0);
			visited.add(hereCoord);
			IChargeConductor here = hereCoord.getTE(IChargeConductor.class); //won't be null
			totalCharge += here.getCharge().charge;
			for (Coord neighbor : hereCoord.getNeighborsAdjacent()) {
				if (neighbor.getTE(IChargeConductor.class) == null) {
					continue;
				}
				if (visited.contains(neighbor)) {
					continue;
				}
				if (neighbor.distanceManhatten(hereCoord) > maxDistance) {
					continue;
				}
				frontier.add(neighbor);
			}
		}
		return totalCharge / visited.size();
	}

}
