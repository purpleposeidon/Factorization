package net.minecraft.src;

import java.util.HashSet;

import net.minecraft.src.forge.ISaveEventHandler;
import net.minecraft.src.forge.MinecraftForge;
import net.minecraft.src.forge.NetworkMod;

public class mod_BlockRecover extends NetworkMod implements ISaveEventHandler {
	static SaveBlock sb;

	void log(String w) {
		System.out.print("BlockRecover: ");
		System.out.println(w);
	}

	class SaveBlock extends BlockContainer {
		protected SaveBlock(int id) {
			super(id, Material.cloth);
			setBlockUnbreakable();
		}

		@Override
		public int getBlockTexture(IBlockAccess world, int x, int y, int z,
				int side) {
			TileEntity t = world.getBlockTileEntity(x, y, z);
			if (t == null) {
				return 0;
			}
			return 11 * 16;
		}

		@Override
		public boolean blockActivated(World world, int x, int y, int z,
				EntityPlayer player) {
			int id = world.getBlockId(x, y, z);
			int md = world.getBlockMetadata(x, y, z);
			player.addChatMessage("Block: " + id + ":" + md);
			TileEntity t = world.getBlockTileEntity(x, y, z);
			if (t == null) {
				return true;
			}
			if (t instanceof SaveTileEntity) {
				player.addChatMessage("TileEntity: " + t);
			} else {
				player.addChatMessage("KNOWN TileEntity: " + t);
			}
			return true;
		}

		@Override
		public void dropBlockAsItemWithChance(World par1World, int par2,
				int par3, int par4, int par5, float par6, int par7) {
			return;
		}

		@Override
		public boolean hasTileEntity(int metadata) {
			return true;
		}

		@Override
		public TileEntity getBlockEntity() {
			return null;
		}
	}

	static HashSet<String> claimed_TE = new HashSet<String>();

	static class SaveTileEntity extends TileEntity {
		NBTTagCompound tag;

		public SaveTileEntity(NBTTagCompound tag) {
			setTag(tag);
		}

		void setTag(NBTTagCompound tag) {
			this.tag = tag;
			if (tag == null) {
				return;
			}
			String id = tag.getString("id");
			if (id == null) {
				return;
			}
			if (claimed_TE.contains(id)) {
				return;
			}
			// ModLoader.registerTileEntity(SaveTileEntity.class, id);
			claimed_TE.add(id);
			updateContainingBlockInfo();
		}

		@Override
		public void writeToNBT(NBTTagCompound compound) {
			if (tag == null) {
				return;
			}
			for (Object o : tag.getTags()) {
				NBTBase base = (NBTTagCompound) o;
				compound.setTag(base.getName(), base);
			}
		}

		@Override
		public void readFromNBT(NBTTagCompound tag) {
			setTag(tag);
		}

		@Override
		public String toString() {
			if (tag == null) {
				return "null NBT tag";
			}
			String id = tag.getString("id");
			if (id != null) {
				return id;
			}
			String r = "No ID. Fields: ";
			for (Object o : tag.getTags()) {
				NBTBase base = (NBTTagCompound) o;
				r += base.getName() + " ";
			}
			return super.toString();
		}

		@Override
		public boolean canUpdate() {
			return false;
		}

		@Override
		public void updateContainingBlockInfo() {
			blockType = sb;
		}
	}

	@Override
	public String getVersion() {
		return "1.0";
	}

	@Override
	public void load() {
		sb = new SaveBlock(0xff);
		MinecraftForge.registerSaveHandler(this);
		ModLoader.registerTileEntity(SaveTileEntity.class, "SaveTileEntity");
	}

	@Override
	public void onWorldLoad(World world) {
		int count = 0;
		for (int i = 1; i < 0xFF; i++) {
			if (Block.blocksList[i] == null) {
				Block.blocksList[i] = sb;
				count++;
			}
		}
		log("Keeping " + count + " block IDS");
	}

	@Override
	public void onChunkLoadData(World world, Chunk chunk, NBTTagCompound data) {
		NBTTagCompound level = data.getCompoundTag("Level");
		NBTTagList entities = (NBTTagList) level.getTag("TileEntities");
		if (entities == null) {
			return;
		}
		int count = 0;
		for (int i = 0; i < entities.tagCount(); i++) {
			NBTTagCompound tag = (NBTTagCompound) entities.tagAt(i);
			TileEntity e = TileEntity.createAndLoadEntity(tag);
			if (e == null) {
				// loading failed. Create a TE saver
				TileEntity toadd = new SaveTileEntity(tag);
				world.setBlockTileEntity(toadd.xCoord, toadd.yCoord,
						toadd.zCoord, toadd);
				count++;
				log("Loading " + toadd + " at " + toadd.xCoord + ", "
						+ toadd.yCoord + ", " + toadd.zCoord);
				assert world.getBlockTileEntity(toadd.xCoord, toadd.yCoord,
						toadd.zCoord) == toadd;
			}
		}
		if (count > 0) {
			log(count + " TileEntities saved at " + chunk.xPosition + ", "
					+ chunk.zPosition);
		}
	}

	@Override
	public void onWorldSave(World world) {
	}

	@Override
	public void onChunkLoad(World world, Chunk chunk) {
	}

	@Override
	public void onChunkUnload(World world, Chunk chunk) {
	}

	@Override
	public void onChunkSaveData(World world, Chunk chunk, NBTTagCompound data) {
	}
}
