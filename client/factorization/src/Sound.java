package factorization.src;

import java.io.DataInput;
import java.io.IOException;
import java.util.ArrayList;

import net.minecraft.src.Entity;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.TileEntity;
import net.minecraft.src.World;
import factorization.src.NetworkFactorization.MessageType;

public enum Sound {
	// it might be kinda cool to have this be configable?
	rightClick("random.click", 1.0, 1.25),
	leftClick("random.click", 1.0, 0.75),
	routerCluck("mob.chicken", 0.5, 0.66, false),
	stamperUse("tile.piston.in", 0.1, 1.1, false),
	makerUse("tile.piston.out", 0.08, 0.5 * 0.3 + 1.1, false),
	bagSlurp("random.drink", 0.6, 8.0, false),
	demonSqueek("mob.enderman.scream", 0.9, 8, true),
	wandCool("random.fizz", .2, 0.5, true),
	shardMake("random.old_explode", 0.7, 1, true),
	wrathLight("mob.zombie.metal", 1, 3),
	wrathForge("ambient.cave.cave", 1, 1), ;
	String src;
	float volume, pitch;
	int index;
	boolean share;

	static class sound {
		static ArrayList<Sound> list = new ArrayList();
	}

	void init(String src, double volume, double pitch) {
		this.src = src;
		this.volume = (float) volume;
		this.pitch = (float) pitch;
		this.index = sound.list.size();
		sound.list.add(this);
	}

	Sound(String src, double volume, double pitch) {
		init(src, volume, pitch);
		this.share = false;
	}

	Sound(String src, double volume, double pitch, boolean share) {
		init(src, volume, pitch);
		this.share = share;
	}

	void share(World w, int x, int y, int z) {
		if (!share) {
			return;
		}
		if (!Core.instance.isCannonical(w)) {
			return;
		}
		Core.network.broadcastMessage(null, new Coord(w, x, y, z), MessageType.PlaySound, index, x, y, z);
	}

	static void receive(DataInput input) {
		try {
			int index = input.readInt(), x = input.readInt(), y = input.readInt(), z = input.readInt();
			EntityPlayer player = Core.instance.getClientPlayer();
			if (player == null) {
				return;
			}
			sound.list.get(index).playAt(player.worldObj, x, y, z);
		} catch (IOException e) {
			return;
		}
	}

	public void playAt(Entity ent) {
		ent.worldObj.playSoundAtEntity(ent, src, volume, pitch);
		share(ent.worldObj, (int) ent.posX, (int) (ent.posY - ent.yOffset), (int) ent.posZ);
	}

	public void playAt(World world, double x, double y, double z) {
		world.playSoundEffect(x, y, z, src, volume, pitch);
		share(world, (int) x, (int) y, (int) z);
	}

	public void playAt(TileEntity ent) {
		playAt(ent.worldObj, ent.xCoord, ent.yCoord, ent.zCoord);
	}

	public void play() {
		Core.instance.playSoundFX(src, volume, pitch);
		if (share) {
			throw new RuntimeException("sharing simple play() not implemented");
		}
	}
}