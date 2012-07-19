package factorization.src;

import java.util.HashMap;

import net.minecraft.src.EntityPlayer;

public enum Command {
	bagShuffle(1), craftClear(2), craftMove(3), craftBalance(4), craftOpen(5), bagShuffleReverse(6);

	static class name {
		static HashMap<Byte, Command> map = new HashMap();
	}

	public byte id;

	Command(int id) {
		this.id = (byte) id;
		name.map.put(this.id, this);
	}

	static void fromNetwork(EntityPlayer player, byte s) {
		Command c = name.map.get(s);
		if (c == null) {
			System.err.println("Received invalid command #" + s);
			return;
		}
		c.call(player);
	}

	public void call(EntityPlayer player) {
		if (!Core.instance.isCannonical(player.worldObj)) {
			Core.network.sendCommand(player, this);
			if (this != craftOpen) {
				return;
			}
		}
		switch (this) {
		case bagShuffle:
			Core.registry.bag_of_holding.useBag(player, false);
			break;
		case bagShuffleReverse:
			Core.registry.bag_of_holding.useBag(player, true);
			break;
		case craftClear:
			// move items from pocket crafting area into rest of inventory,
			// or into a bag
			break;
		case craftMove:
			// do something smart with items in crafting area
			break;
		case craftBalance:
			// move as many items as we can to fill in template in crafting
			// area
			break;
		case craftOpen:
			Core.registry.pocket_table.tryOpen(player);
			break;
		default:
			throw new RuntimeException("enum lacks handler");
		}
	}

}