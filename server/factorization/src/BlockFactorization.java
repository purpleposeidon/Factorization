package factorization.src;

import java.util.ArrayList;
import java.util.Random;

import net.minecraft.src.Block;
import net.minecraft.src.BlockContainer;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.IBlockAccess;
import net.minecraft.src.ItemStack;
import net.minecraft.src.TileEntity;
import net.minecraft.src.World;
import net.minecraft.src.mod_Factorization;
import net.minecraft.src.forge.IConnectRedstone;
import net.minecraft.src.forge.ITextureProvider;

public class BlockFactorization extends BlockContainer implements
		ITextureProvider, IConnectRedstone {
	public boolean fake_normal_render = false;

	public BlockFactorization(int id) {
		super(id, Core.registry.materialMachine);
		setHardness(2.0F);
		setResistance(5);
	}

	@Override
	public TileEntity getBlockEntity(int md) {
		//The TileEntity needs to be set when the block is placed.
		return null;
	}

	@Override
	public int idDropped(int par1, Random par2Random, int par3) {
		//If we had this behave normally, it'd just return garbage unfortunately.
		//Sooo... we return the other kind of garbage instead.
		//(This is what happens in Creative mode.)
		return Block.cobblestone.blockID;
		//return super.idDropped(par1, par2Random, par3);
	}

	@Override
	public boolean hasTileEntity() {
		return true;
	}

	@Override
	public TileEntity getBlockEntity() {
		throw new RuntimeException("This function shouldn't be called. Bad Forge install?");
	}

	@Override
	public void onNeighborBlockChange(World w, int x, int y, int z, int l) {
		int md = w.getBlockMetadata(x, y, z);
		TileEntity ent = w.getBlockTileEntity(x, y, z);
		if (ent == null) {
			return;
		}
		if (ent instanceof TileEntityFactorization) {
			TileEntityFactorization factory = (TileEntityFactorization) ent;
			factory.neighborChanged();
		}
		if (ent instanceof TileEntityWrathLamp) {
			TileEntityWrathLamp lamp = (TileEntityWrathLamp) ent;
			lamp.activate(y);
		}
	}

	//TODO: Ctrl/alt clicking!

	@Override
	public boolean blockActivated(World world, int i, int j, int k,
			EntityPlayer entityplayer) {
		// right click
		if (entityplayer.isSneaking()) {
			return false;
		}

		TileEntityFactorization t = new Coord(world, i, j, k).getTE(TileEntityFactorization.class);

		if (t != null) {
			if (Core.instance.isCannonical(world)) {
				t.activate(entityplayer);
			}

			return true;
		}
		else {
			//info message
			if (!Core.instance.isCannonical(world)) {
				return false;
			}
			entityplayer.addChatMessage("This block is missing its TileEntity, possibly due to a bug in Factorization.");
			if (Core.instance.isPlayerAdmin(entityplayer) || entityplayer.capabilities.isCreativeMode) {
				entityplayer.addChatMessage("The block and its contents can not be recovered.");
			} else {
				entityplayer.addChatMessage("It can not be repaired without cheating.");
			}
		}

		return false;
	}

	@Override
	public void onBlockClicked(World world, int x, int y, int z,
			EntityPlayer entityplayer) {
		// left click

		if (!mod_Factorization.instance.isCannonical(world)) {
			return;
		}

		TileEntity t = world.getBlockTileEntity(x, y, z);
		if (t instanceof TileEntityFactorization) {
			((TileEntityFactorization) t).click(entityplayer);
		}
	}

	// @Override -- Nope, can't do this for server
	public int getBlockTexture(IBlockAccess w, int x, int y, int z, int side) {
		// Used for in-world rendering. Takes 'active' into consideration.
		if (Texture.force_texture != -1) {
			return Texture.force_texture;
		}
		TileEntity t = w.getBlockTileEntity(x, y, z);
		boolean active = false;
		int facing_direction = 0;
		if (t instanceof TileEntityFactorization) {
			TileEntityFactorization f = (TileEntityFactorization) t;
			active = (((f).draw_active + 1) / 2) % 3 == 1;
			facing_direction = f.facing_direction;
		}

		if (t instanceof IFactoryType) {
			int md = ((IFactoryType) t).getFactoryType().md;
			return Texture.pick(md, side, active, facing_direction);
		}

		return 0;
	}

	@Override
	public int getBlockTextureFromSideAndMetadata(int side, int md) {
		// This shouldn't be called when rendering in the world.
		// Is used for inventory!
		return Texture.pick(md, side, false, 3);
	}

	@Override
	public String getTextureFile() {
		return mod_Factorization.texture_file_block;
	}

	@Override
	protected int damageDropped(int i) {
		return i;
	}

	@Override
	public int quantityDropped(int meta, int fortune, Random random) {
		return 1;
	}

	TileEntity destroyedTE;

	@Override
	public void onBlockRemoval(World w, int x, int y, int z) {
		TileEntity ent = w.getBlockTileEntity(x, y, z);
		destroyedTE = ent;
		if (ent instanceof TileEntityFactorization) {
			TileEntityFactorization factory = (TileEntityFactorization) ent;
			factory.dropContents();
		}
		if (ent instanceof TileEntityWrathLamp) {
			TileEntityWrathLamp lamp = (TileEntityWrathLamp) ent;
			lamp.onRemove();
		}
		if (ent instanceof TileEntityWatchDemon) {
			((TileEntityWatchDemon) ent).onRemove();
		}
	}

	@Override
	public ArrayList<ItemStack> getBlockDropped(World world, int X, int Y,
			int Z, int md, int fortune) {
		ArrayList<ItemStack> ret = new ArrayList<ItemStack>();
		Coord here = new Coord(world, X, Y, Z);
		IFactoryType f = here.getTE(IFactoryType.class);
		if (f == null) {
			if (destroyedTE == null) {
				System.out.println("No IFactoryType TE behind block that was destroyed, and nothing saved!");
				return ret;
			}
			Coord destr = new Coord(destroyedTE);
			if (!destr.equals(here)) {
				System.out.println("Last saved destroyed TE wasn't for this location");
				return ret;
			}
			if (!(destroyedTE instanceof IFactoryType)) {
				System.out.println("TileEntity isn't an IFT! It's " + here.getTE());
				return ret;
			}
			f = (IFactoryType) destroyedTE;
			destroyedTE = null;
		}
		ItemStack is = new ItemStack(Core.registry.item_factorization, 1, f.getFactoryType().md);
		ret.add(is);
		return ret;
	}

	@Override
	public void addCreativeItems(ArrayList itemList) {
		Registry core = Core.registry;
		//common
		itemList.add(core.barrel_item);
		itemList.add(core.maker_item);
		itemList.add(core.stamper_item);
		itemList.add(core.packager_item);
		itemList.add(core.slagfurnace_item);

		//dark
		itemList.add(core.router_item);
		itemList.add(core.lamp_item);
		itemList.add(core.sentrydemon_item);

		//itemList.add(core.cutter_item);
		//itemList.add(core.queue_item);
	}

	@Override
	public boolean canConnectRedstone(IBlockAccess world, int x, int y, int z, int dir) {
		if (FactoryType.ROUTER.is(world.getBlockMetadata(x, y, z))) {
			return true;
		}
		return false;
	}

	@Override
	public boolean isBlockNormalCube(World world, int i, int j, int k) {
		return true;
	}

	@Override
	public int getFlammability(IBlockAccess world, int x, int y, int z,
			int md, int face) {
		if (FactoryType.BARREL.is(md)) {
			return 25;
		}
		return 0;
	}

	@Override
	public boolean isFlammable(IBlockAccess world, int x, int y, int z, int metadata, int face) {
		return false;
		//Not really. But this keeps fire rendering.
		//		if (FactoryType.BARREL.is(metadata)) {
		//			return true;
		//		}
		//		return false;
	}

	//Lightair/lamp stuff

	@Override
	public int getLightValue(IBlockAccess world, int x, int y, int z) {
		int md = world.getBlockMetadata(x, y, z);
		BlockClass c = BlockClass.get(md);
		if (c == BlockClass.MachineLightable) {
			TileEntity te = world.getBlockTileEntity(x, y, z);
			if (te instanceof TileEntityFactorization) {
				if (((TileEntityFactorization) te).draw_active == 0) {
					return BlockClass.Machine.lightValue;
				}
				return c.lightValue;
			}
		}
		return BlockClass.get(md).lightValue;
	}

	@Override
	public float getHardness(int md) {
		return BlockClass.get(md).hardness;
	}

	@Override
	public void setBlockBoundsBasedOnState(IBlockAccess w, int x, int y, int z) {
		setBlockBounds(0, 0, 0, 1, 1, 1);
	}

	@Override
	public boolean renderAsNormalBlock() {
		if (fake_normal_render) {
			return true;
		}
		return false;
	}

	@Override
	public boolean isOpaqueCube() {
		return false;
	}

	@Override
	public int getRenderType() {
		if (fake_normal_render) {
			return 0;
		}
		return mod_Factorization.factory_rendertype;
	}

	public static final float lamp_pad = 1F / 16F;

	//	@Override
	//	public AxisAlignedBB getCollisionBoundingBoxFromPool(World w, int x, int y, int z) {
	//		//		int md = w.getBlockMetadata(x, y, z);
	//		//		if (FactoryType.LAMP.is(md)) {
	//		//			return AxisAlignedBB.getBoundingBoxFromPool(x + lamp_pad, y + lamp_pad, z + lamp_pad, x + 1 - lamp_pad, y + 1 - lamp_pad, z + 1 - lamp_pad);
	//		//		}
	//		return super.getCollisionBoundingBoxFromPool(w, x, y, z);
	//	}
	//
	//	@Override
	//	public AxisAlignedBB getSelectedBoundingBoxFromPool(World w, int x, int y, int z) {
	//		int md = w.getBlockMetadata(x, y, z);
	//		if (FactoryType.LAMP.is(md)) {
	//			return AxisAlignedBB.getBoundingBoxFromPool(x + lamp_pad, y + lamp_pad, z + lamp_pad, x + 1 - lamp_pad, y + 1 - lamp_pad, z + 1 - lamp_pad);
	//		}
	//		return super.getSelectedBoundingBoxFromPool(w, x, y, z);
	//	}

	//@Override ser-ver ser-ver
	public void randomDisplayTick(World w, int x, int y, int z, Random rand) {
		Core.instance.randomDisplayTickFor(w, x, y, z, rand);
	}
}
