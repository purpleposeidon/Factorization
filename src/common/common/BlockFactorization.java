package factorization.common;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.src.AxisAlignedBB;
import net.minecraft.src.Block;
import net.minecraft.src.BlockContainer;
import net.minecraft.src.CreativeTabs;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.IBlockAccess;
import net.minecraft.src.ItemStack;
import net.minecraft.src.MovingObjectPosition;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.TileEntity;
import net.minecraft.src.Vec3;
import net.minecraft.src.World;
import net.minecraftforge.common.ForgeDirection;
import factorization.api.Coord;
import factorization.api.IFactoryType;

public class BlockFactorization extends BlockContainer {
    public boolean fake_normal_render = false;

    public BlockFactorization(int id) {
        super(id, Core.registry.materialMachine);
        //		super(id, Material.air);
        setHardness(2.0F);
        setResistance(5);
        setLightOpacity(1);
        canBlockGrass[id] = true;
        //setBlockBounds(0, -1000, 0, 0, -999, 0);
    }

    @Override
    public TileEntity createNewTileEntity(World world) {
        //The TileEntity needs to be set when the block is placed.
        return null;
    }

    @Override
    public ItemStack getPickBlock(MovingObjectPosition target, World world, int x, int y, int z) {
        TileEntityCommon tec = new Coord(world, x, y, z).getTE(TileEntityCommon.class);
        if (tec == null) {
            return null;
        }
        FactoryType ft = tec.getFactoryType();
        if (ft == FactoryType.MIRROR) {
            return new ItemStack(Core.registry.mirror);
        }
        if (ft == FactoryType.BATTERY) {
            ItemStack is = new ItemStack(Core.registry.battery);
            TileEntityBattery bat = (TileEntityBattery) tec;
            NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger("storage", bat.storage.getValue());
            is.setTagCompound(tag);
            Core.registry.battery.normalizeDamage(is);
            return is;
        }
        return new ItemStack(Core.registry.item_factorization, 1, tec.getFactoryType().md);
    }

    @Override
    public boolean hasTileEntity() {
        return true;
    }

    @Override
    public boolean isBlockSolid(IBlockAccess world, int x, int y, int z, int side) {
        TileEntity t = world.getBlockTileEntity(x, y, z);
        if (t == null || !(t instanceof TileEntityCommon)) {
            return false;
        }
        TileEntityCommon te = (TileEntityCommon) t;
        return te.isBlockSolidOnSide(side);
    }

    @Override
    public void onNeighborBlockChange(World w, int x, int y, int z, int l) {
        int md = w.getBlockMetadata(x, y, z);
        TileEntity ent = w.getBlockTileEntity(x, y, z);
        if (ent == null) {
            return;
        }
        if (ent instanceof TileEntityCommon) {
            TileEntityCommon tec = (TileEntityCommon) ent;
            tec.neighborChanged();
        }
    }

    //TODO: Ctrl/alt clicking!

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer entityplayer,
            int side, float vecx, float vecy, float vecz) {
        // right click
        if (entityplayer.isSneaking()) {
            return false;
        }

        TileEntityCommon t = new Coord(world, x, y, z).getTE(TileEntityCommon.class);

        if (t != null) {
            return t.activate(entityplayer);
            //			if (!world.isRemote) {
            //				return t.activate(entityplayer);
            //			}
            //
            //			return true;
        }
        else {
            //info message
            if (world.isRemote) {
                return false; //...?
            }
            entityplayer.addChatMessage("This block is missing its TileEntity, possibly due to a bug in Factorization.");
            if (Core.proxy.isPlayerAdmin(entityplayer) || entityplayer.capabilities.isCreativeMode) {
                entityplayer.addChatMessage("The block and its contents can not be recovered.");
            } else {
                entityplayer.addChatMessage("It can not be repaired without cheating.");
            }
            return true;
        }
    }

    @Override
    public void onBlockClicked(World world, int x, int y, int z,
            EntityPlayer entityplayer) {
        // left click

        if (world.isRemote) {
            return;
        }

        TileEntity t = world.getBlockTileEntity(x, y, z);
        if (t instanceof TileEntityFactorization) {
            ((TileEntityFactorization) t).click(entityplayer);
        }
    }

    @Override
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
        if (t instanceof TileEntityBarrel) {
            //whee, hack
            active = ((TileEntityBarrel) t).upgrade != 0;
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
        return Core.texture_file_block;
    }

    @Override
    protected int damageDropped(int i) {
        return i;
    }

    @Override
    public int quantityDropped(int meta, int fortune, Random random) {
        return 1;
    }

    TileEntityCommon destroyedTE;

    @Override
    public void breakBlock(World w, int x, int y, int z, int id, int md) {
        TileEntityCommon te = new Coord(w, x, y, z).getTE(TileEntityCommon.class);
        if (te != null) {
            destroyedTE = te;
            te.onRemove();
        }
        super.breakBlock(w, x, y, z, id, md);
    }

    @Override
    public ArrayList<ItemStack> getBlockDropped(World world, int X, int Y, int Z, int md,
            int fortune) {
        ArrayList<ItemStack> ret = new ArrayList<ItemStack>();
        Coord here = new Coord(world, X, Y, Z);
        IFactoryType f = here.getTE(IFactoryType.class);
        if (f == null) {
            if (destroyedTE == null) {
                System.out.println("No IFactoryType TE behind block that was destroyed, and nothing saved!");
                return ret;
            }
            Coord destr = destroyedTE.getCoord();
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
        if (f.getFactoryType() == FactoryType.MIRROR) {
            is = new ItemStack(Core.registry.mirror);
        }
        if (f.getFactoryType() == FactoryType.BATTERY) {
            is = new ItemStack(Core.registry.battery);
            TileEntityBattery bat = (TileEntityBattery) f;
            NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger("storage", bat.storage.getValue());
            is.setTagCompound(tag);
            Core.registry.battery.normalizeDamage(is);
        }
        ret.add(is);
        return ret;
    }

    @Override
    public void addCreativeItems(ArrayList itemList) {
        if (this != Core.registry.factory_block) {
            return;
        }
        Registry reg = Core.registry;
        //common
        itemList.add(reg.barrel_item);
        itemList.add(reg.maker_item);
        itemList.add(reg.stamper_item);
        itemList.add(reg.packager_item);
        itemList.add(reg.slagfurnace_item);

        //dark
        itemList.add(reg.router_item);
        itemList.add(reg.lamp_item);
        //itemList.add(reg.sentrydemon_item);

        //electric
        //itemList.add(reg.battery_item_hidden);
        if (reg.battery != null) {
            //These checks are for buildcraft, which is hatin'.
            itemList.add(new ItemStack(reg.battery, 1, 2));
        }
        itemList.add(reg.solar_turbine_item);
        //itemList.add(reg.mirror_item_hidden);
        if (reg.mirror != null) {
            itemList.add(new ItemStack(reg.mirror));
        }
        itemList.add(reg.heater_item);
        itemList.add(reg.leadwire_item);
        itemList.add(reg.grinder_item);
        itemList.add(reg.mixer_item);
        itemList.add(reg.crystallizer_item);

        //itemList.add(core.cutter_item);
        //itemList.add(core.queue_item);
        //itemList.add(reg.greenware_item);
    }

    @Override
    public void getSubBlocks(int par1, CreativeTabs par2CreativeTabs, List par3List) {
        if (this != Core.registry.factory_block) {
            return;
        }
        Core.addBlockToCreativeList(par3List, this);
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
        return BlockClass.get(world.getBlockMetadata(i, j, k)).isNormal();
    }

    @Override
    public int getFlammability(IBlockAccess world, int x, int y, int z,
            int md, ForgeDirection face) {
        if (FactoryType.BARREL.is(md)) {
            return 25;
        }
        return 0;
    }

    @Override
    public boolean isFlammable(IBlockAccess world, int x, int y, int z, int metadata,
            ForgeDirection face) {
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
    public float getBlockHardness(World w, int x, int y, int z) {
        return BlockClass.get(w.getBlockMetadata(x, y, z)).hardness;
    }

    //smack these blocks up
    @Override
    public MovingObjectPosition collisionRayTrace(World w, int x, int y, int z,
            Vec3 startVec, Vec3 endVec) {
        TileEntityCommon tec = new Coord(w, x, y, z).getTE(TileEntityCommon.class);
        if (tec == null) {
            return super.collisionRayTrace(w, x, y, z, startVec, endVec);
        }
        return tec.collisionRayTrace(w, x, y, z, startVec, endVec);
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World w, int x, int y, int z) {
        TileEntityCommon tec = new Coord(w, x, y, z).getTE(TileEntityCommon.class);
        setBlockBounds(0, 0, 0, 1, 1, 1);
        if (tec == null) {
            return super.getCollisionBoundingBoxFromPool(w, x, y, z);
        }
        return tec.getCollisionBoundingBoxFromPool();
    }

    @Override
    public void setBlockBoundsBasedOnState(IBlockAccess w, int x, int y, int z) {
        TileEntity te = w.getBlockTileEntity(x, y, z);
        if (te == null || !(te instanceof TileEntityCommon)) {
            setBlockBounds(0, 0, 0, 1, 1, 1);
            return;
        }
        ((TileEntityCommon) te).setBlockBounds(this);
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
        return Core.factory_rendertype;
    }

    public static final float lamp_pad = 1F / 16F;

    @Override
    public boolean canProvidePower() {
        return true;
    }

    @Override
    public boolean isIndirectlyPoweringTo(World w, int x, int y, int z, int side) {
        Coord here = new Coord(w, x, y, z);
        TileEntityCommon tec = here.getTE(TileEntityCommon.class);
        return tec == null ? false : tec.power();
    }

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

    @Override
    //ser-ver ser-ver
    public void randomDisplayTick(World w, int x, int y, int z, Random rand) {
        Core.proxy.randomDisplayTickFor(w, x, y, z, rand);
    }

    @Override
    public boolean canRenderInPass(int pass) {
        return pass == 0 || pass == 1;
    }

    @Override
    // -- hello server asshole
    public int getRenderBlockPass() {
        return 1;
    }

    @Override
    public boolean isAirBlock(World world, int x, int y, int z) {
        return false;
    }
}
