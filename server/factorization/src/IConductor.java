package factorization.src;

import java.util.Collections;
import java.util.List;

import net.minecraft.src.TileEntity;

public interface IConductor {
	char getCharge();

	void setCharge(char charge);

	static class Update {
		static void run(Coord here) {
			IConductor me = here.getTE(IConductor.class);
			if (me == null || me.getCharge() < 1) {
				return;
			}
			List<Coord> neighbors = here.getNeighborsAdjacent();
			Collections.shuffle(neighbors);
			for (Coord neighbor : neighbors) {
				swap(me, neighbor);
			}
		}

		static private void swap(IConductor me, Coord yourCoord) {
			IConductor you = yourCoord.getTE(IConductor.class);
			if (you == null) {
				return;
			}
			char your_charge = you.getCharge();
			char my_charge = me.getCharge();
			if (your_charge < my_charge) {
				you.setCharge(my_charge);
				me.setCharge(your_charge);
			}
		}
	}

	class Implementor extends TileEntity implements IConductor {
		char charge;

		@Override
		public char getCharge() {
			return charge;
		}

		@Override
		public void setCharge(char charge) {
			this.charge = charge;
		}

		@Override
		public void updateEntity() {
			super.updateEntity();
			Update.run(new Coord(this));
		}
	}
}
