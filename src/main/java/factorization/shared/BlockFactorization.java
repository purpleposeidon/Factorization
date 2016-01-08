package factorization.shared;

import factorization.api.Coord;
import factorization.api.FzColor;
import factorization.common.FactoryType;
import factorization.common.Registry;
import factorization.net.StandardMessageType;
import factorization.notify.Notice;
import factorization.util.FzUtil;
import factorization.weird.barrel.TileEntityDayBarrel;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.IWorldAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;

import java.util.*;

public class BlockFactorization extends BlockContainer {
    public static final IProperty<BlockClass> BLOCK_CLASS = PropertyEnum.create("blockclass", BlockClass.class);

    public BlockFactorization(Material material) {
        super(material);
        setHardness(2.0F);
        setResistance(5);
        setLightOpacity(0);
        translucent = true;
        setTickRandomly(false);
    }

    public FactoryType getFactoryType(int md) {
        return FactoryType.fromMd(md);
    }

    @Override
    protected BlockState createBlockState() {
        return new BlockState(this, BLOCK_CLASS);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(BLOCK_CLASS, BlockClass.get(meta));
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(BLOCK_CLASS).md;
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return true;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int metadata) {
        //The TileEntity needs to be set by the item when the block is placed.
        //Originally I returned null here, but we're now returning this handy generic TE.
        //This is because portalgun relies on this to make a TE that won't drop anything when it's moving it.
        //But when this returned null, it wouldn't remove the real TE. So, the tile entity was both having its block broken, and being moved.
        //Returning a generic TE won't be an issue for us as we always use coord.getTE, and never assume, right?
        //We could possibly have our null TE remove itself.
        TileEntityFzNull nuller = new TileEntityFzNull();
        nuller.setWorldObj(world);
        return nuller;
    }

    private static TileEntityCommon get(IBlockAccess w, BlockPos pos) {
        TileEntity te = w.getTileEntity(pos);
        if (te instanceof TileEntityCommon) return (TileEntityCommon) te;
        return null;
    }

    @Override
    public ItemStack getPickBlock(MovingObjectPosition target, World world, BlockPos pos, EntityPlayer player) {
        TileEntityCommon tec = get(world, pos);
        if (tec == null) return null;
        return tec.getPickedBlock();
    }

    @Override
    public boolean isBlockSolid(IBlockAccess world, BlockPos pos, EnumFacing side) {
        TileEntityCommon tec = get(world, pos);
        if (tec == null) return false;
        return tec.isBlockSolidOnSide(side);
    }

    @Override
    @Deprecated
    public boolean isSideSolid(IBlockAccess world, BlockPos pos, EnumFacing side) {
        return isBlockSolid(world, pos, side);
    }

    @Override
    public void onNeighborChange(IBlockAccess world, BlockPos pos, BlockPos neighbor) {
        TileEntityCommon tec = get(world, pos);
        if (tec == null) return;
        tec.neighborChanged();
    }

    @Override
    public void onNeighborBlockChange(World world, BlockPos pos, IBlockState state, Block neighborBlock) {
        TileEntityCommon tec = get(world, pos);
        if (tec == null) return;
        tec.neighborChanged(neighborBlock);
    }

    @Override
    public boolean onBlockActivated(World w, BlockPos pos, IBlockState state, EntityPlayer player, EnumFacing side, float vecx, float vecy, float vecz) {
        // right click
        Coord here = new Coord(w, pos);
        TileEntityCommon t = here.getTE(TileEntityCommon.class);
        if (t == null && w.isRemote) {
            Core.network.broadcastMessageToBlock(null, here, StandardMessageType.DescriptionRequest);
            return false;
        }
        if (player.isSneaking()) {
            ItemStack cur = player.getCurrentEquippedItem();
            if (cur == null || cur.getItem() != Core.registry.logicMatrixProgrammer) {
                return false;
            }
        }

        if (t != null) {
            return t.activate(player, side);
        }
        player.addChatMessage(new ChatComponentText("This block is missing its TileEntity, possibly due to a bug in Factorization."));
        player.addChatMessage(new ChatComponentText("The block and its contents can not be recovered without cheating."));
        return true;
    }

    @Override
    public void onBlockClicked(World w, BlockPos pos, EntityPlayer player) {
        // left click
        if (w.isRemote) return;
        TileEntityCommon tec = get(w, pos);
        if (tec == null) return;
        tec.click(player);
    }

    @Override
    public int damageDropped(IBlockState state) {
        return getMetaFromState(state);
    }

    @Override
    public int quantityDropped(IBlockState state, int fortune, Random random) {
        return 1;
    }

    LinkedList<TileEntityCommon> destroyed_tes = new LinkedList<TileEntityCommon>();

    @Override
    public void breakBlock(World w, BlockPos pos, IBlockState state) {
        TileEntityCommon tec = get(w, pos);
        if (tec != null) {
            tec.onRemove();
            destroyed_tes.add(tec);
        }
        super.breakBlock(w, pos, state); // Just removes the TE; does nothing else.
    }

    @Override
    public boolean removedByPlayer(World world, BlockPos pos, EntityPlayer player, boolean willHarvest) {
        Coord here = new Coord(world, pos);
        TileEntityCommon tec = here.getTE(TileEntityCommon.class);
        if (tec == null) {
            if (!world.isRemote) {
                new Notice(here, "There was no TileEntity!").send(player);
            }
            return world.setBlockToAir(pos);
        }
        boolean ret = tec.removedByPlayer(player, willHarvest);
        if (!world.isRemote && !ret) {
            FMLProxyPacket description = tec.getDescriptionPacket();
            Core.network.broadcastPacket(player, here, description);
            here.sendRedraw();
        }
        return ret;
    }

    @Override
    public List<ItemStack> getDrops(IBlockAccess world, BlockPos pos, IBlockState state, int fortune) {
        ArrayList<ItemStack> ret = new ArrayList<ItemStack>();
        TileEntityCommon f = get(world, pos);
        if (f == null) {
            Iterator<TileEntityCommon> it = destroyed_tes.iterator();
            TileEntityCommon destroyedTE = null;
            while (it.hasNext()) {
                TileEntityCommon tec = it.next();
                if (tec.getPos().equals(pos)) {
                    destroyedTE  = tec;
                    it.remove();
                }
            }
            if (destroyedTE == null) {
                Core.logWarning("No IFactoryType TE behind block that was destroyed, and nothing saved!");
                return ret;
            }
            if (!destroyedTE.getPos().equals(pos)) {
                Core.logWarning("Last saved destroyed TE wasn't for this location");
                return ret;
            }
            f = destroyedTE;
        }
        ItemStack is = f.getDroppedBlock();
        if (is != null) ret.add(is);
        return ret;
    }

    private static void put(List<ItemStack> itemList, ItemStack item) {
        if (item == null) return;
        itemList.add(item);
    }
    
    @Override
    public void getSubBlocks(Item me, CreativeTabs tab, List<ItemStack> itemList) {
        if (this != Core.registry.legacy_factory_block) {
            return;
        }
        Registry reg = Core.registry;
        //electric
        //put(itemList, reg.battery_item_hidden);
        if (reg.battery != null) {
            //These checks are for buildcraft, which is hatin'.
            put(itemList, new ItemStack(reg.battery, 1, 2));
        }
        put(itemList, reg.leydenjar_item);
        put(itemList, reg.leydenjar_item_full);
        put(itemList, reg.sap_generator_item);
        put(itemList, reg.anthro_generator_item);
        put(itemList, reg.solarboiler_item);
        put(itemList, reg.wooden_shaft);
        put(itemList, reg.bibliogen);
        if (reg.mirror != null) {
            put(itemList, new ItemStack(reg.mirror));
        }
        put(itemList, reg.shaft_generator_item);
        put(itemList, reg.wind_mill);
        put(itemList, reg.water_wheel);
        put(itemList, reg.steam_to_shaft);

        put(itemList, reg.greenware_item);

        if (reg.rocket_engine != null) {
            put(itemList, new ItemStack(reg.rocket_engine));
        }

        //dark
        put(itemList, reg.empty_socket_item);
        put(itemList, reg.servorail_item);
        put(itemList, reg.lamp_item);
        put(itemList, reg.compression_crafter_item);

        //mechanics
        put(itemList, reg.hinge);

        put(itemList, reg.legendarium);
    }


    @Override
    public boolean canConnectRedstone(IBlockAccess world, BlockPos pos, EnumFacing side) {
        TileEntityCommon tec = get(world, pos);
        if (tec == null) return false;
        return tec.getFactoryType().connectRedstone();
    }

    @Override
    public boolean isNormalCube(IBlockAccess world, BlockPos pos) {
        return getClass(world, pos).isNormal();
    }

    public BlockClass getClass(IBlockAccess world, BlockPos pos) {
        IBlockState bs = world.getBlockState(pos);
        if (bs.getBlock() != this) return BlockClass.Default;
        return bs.getValue(BLOCK_CLASS);
    }

    @Override
    public int getFlammability(IBlockAccess world, BlockPos pos, EnumFacing face) {
        if (BlockClass.Barrel == getClass(world, pos)) {
            TileEntityCommon te = get(world, pos);
            if (te instanceof TileEntityDayBarrel) {
                return ((TileEntityDayBarrel) te).getFlamability();
            }
        }
        return 0;
    }

    @Override
    public boolean isFlammable(IBlockAccess world, BlockPos pos, EnumFacing face) {
        //Not really. But this keeps fire rendering.
        return getFlammability(world, pos, face) > 0;
    }

    @Override
    public int getLightValue(IBlockAccess world, BlockPos pos) {
        BlockClass c = getClass(world, pos);
        if (c == null) return 0;
        if (c == BlockClass.MachineDynamicLightable || c == BlockClass.MachineLightable) {
            TileEntityCommon tec = get(world, pos);
            if (tec != null) return tec.getDynamicLight();
        }
        return c.lightValue;
    }

    @Override
    public float getBlockHardness(World world, BlockPos pos) {
        return getClass(world, pos).hardness;
    }


    @Override
    public MovingObjectPosition collisionRayTrace(World world, BlockPos pos, Vec3 start, Vec3 end) {
        TileEntityCommon tec = get(world, pos);
        if (tec == null) return super.collisionRayTrace(world, pos, start, end);
        return tec.collisionRayTrace(start, end);
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBox(World w, BlockPos pos, IBlockState state) {
        // NORELEASE: Check state first. *most* things are just cubes.
        TileEntityCommon tec = get(w, pos);
        if (tec == null) return super.getCollisionBoundingBox(w, pos, state);
        return tec.getCollisionBoundingBox();
    }

    @Override
    public void addCollisionBoxesToList(World w, BlockPos pos, IBlockState state, AxisAlignedBB mask, List<AxisAlignedBB> list, Entity collidingEntity) {
        // NORELEASE: Check state first. *most* things are just cubes.
        TileEntityCommon tec = get(w, pos);
        Block test = FzUtil.getTraceHelper();
        if (tec == null || !tec.addCollisionBoxesToList(test, mask, list, collidingEntity)) {
            super.addCollisionBoxesToList(w, pos, state, mask, list, collidingEntity);
        }
    }

    @Override
    public AxisAlignedBB getSelectedBoundingBox(World world, BlockPos pos) {
        TileEntityCommon tec = get(world, pos);
        if (tec != null && tec.getFactoryType() == FactoryType.EXTENDED) {
            AxisAlignedBB ret = tec.getCollisionBoundingBox();
            if (ret != null) {
                return ret;
            }
        }
        return super.getSelectedBoundingBox(world, pos);
    }

    @Override
    public void setBlockBoundsBasedOnState(IBlockAccess world, BlockPos pos) {
        // NORELEASE: This could also be entirely state-based
        TileEntityCommon tec = get(world, pos);
        if (tec == null) {
            setBlockBounds(0, 0, 0, 1, 1, 1);
            return;
        }
        tec.setBlockBounds(this);
    }

    @Override
    public boolean isNormalCube() {
        return false;
    }

    @Override
    public boolean isOpaqueCube() {
        return false;
    }

    @Override
    public boolean isVisuallyOpaque() {
        return false;
    }

    @Override
    public boolean isPassable(IBlockAccess world, BlockPos pos) {
        return getClass(world, pos).passable;
    }

    @Override
    public int getRenderType() {
        return 3;
    }

    public static final float lamp_pad = 1F / 16F;

    @Override
    public boolean canProvidePower() {
        return true;
    }

    @Override
    public void updateTick(World world, BlockPos pos, IBlockState state, Random rand) {
        TileEntityCommon tec = get(world, pos);
        if (tec != null) tec.blockUpdateTick(this);
    }

    //Maybe we should only give weak power?


    @Override
    public int getStrongPower(IBlockAccess worldIn, BlockPos pos, IBlockState state, EnumFacing side) {
        return 0;
    }

    @Override
    public int getWeakPower(IBlockAccess world, BlockPos pos, IBlockState state, EnumFacing side) {
        TileEntityCommon tec = get(world, pos);
        if (tec != null) {
            return tec.power() ? 15 : 0;
        }
        return 0;
    }

    @Override
    public boolean hasComparatorInputOverride() {
        return true;
    }

    @Override
    public int getComparatorInputOverride(World world, BlockPos pos) {
        TileEntityCommon tec = get(world, pos);
        if (tec == null) return 0;
        return tec.getComparatorValue(EnumFacing.UP /* Delete parameter? Nah, let's just keep it. */);
    }

    @Override
    public void randomDisplayTick(World world, BlockPos pos, IBlockState state, Random rand) {
        TileEntityCommon tec = get(world, pos);
        if (tec != null) {
            tec.spawnDisplayTickParticles(rand);
        }
    }

    @Override
    public boolean shouldSideBeRendered(IBlockAccess world, BlockPos pos, EnumFacing side) {
        BlockClass bc = getClass(world, pos.offset(side.getOpposite()));
        return !bc.isNormal() || super.shouldSideBeRendered(world, pos, side);
    }

    @Override
    public boolean rotateBlock(World world, BlockPos pos, EnumFacing axis) {
        final Coord at = new Coord(world, pos);
        TileEntityCommon tec = at.getTE(TileEntityCommon.class);
        if (tec == null) {
            return false;
        }
        boolean suc = tec.rotate(axis);
        if (suc) {
            at.markBlockForUpdate();
        }
        return suc;
    }

    @Override
    public EnumFacing[] getValidRotations(World world, BlockPos pos) {
        TileEntityCommon tec = get(world, pos);
        if (tec == null) {
            return TileEntityCommon.empty_rotation_array;
        }
        return tec.getValidRotations();
    }

    @Override
    public boolean recolorBlock(World world, BlockPos pos, EnumFacing side, EnumDyeColor color) {
        TileEntityCommon tec = get(world, pos);
        if (tec == null) return false;
        return tec.recolourBlock(side, FzColor.fromVanilla(color));
    }

    @Override
    public float getPlayerRelativeBlockHardness(EntityPlayer player, World world, BlockPos pos) {
        if (!player.capabilities.allowEdit && this == Core.registry.factory_block_barrel) {
            return 0;
        }
        return super.getPlayerRelativeBlockHardness(player, world, pos);
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase player, ItemStack stack) {
        TileEntityCommon tec = get(world, pos);
        if (tec == null) return;
        tec.loadFromStack(stack);
    }
}
