package factorization.shared;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.api.Coord;
import factorization.api.FzColor;
import factorization.common.BlockIcons;
import factorization.common.FactoryType;
import factorization.common.Registry;
import factorization.notify.Notice;
import factorization.shared.NetworkFactorization.MessageType;
import factorization.weird.TileEntityDayBarrel;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.particle.EntityDiggingFX;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.*;

public class BlockFactorization extends BlockContainer {
    public boolean fake_normal_render = false;
    public BlockFactorization() {
        super(Core.registry.materialMachine);
        setHardness(2.0F);
        setResistance(5);
        setLightOpacity(0);
        canBlockGrass = true;
        setTickRandomly(false);
    }

    @Override
    public TileEntity createNewTileEntity(World world, int metadata) {
        //The TileEntity needs to be set by the item when the block is placed.
        //Originally I returned null here, but we're now returning this handy generic TE.
        //This is because portalgun relies on this to make a TE that won't drop anything when it's moving it.
        //But when this returned null, it wouldn't remove the real TE. So, the tile entity was both having its block broken, and being moved.
        //Returning a generic TE won't be an issue for us as we always use coord.getTE, and never assume, right?
        //We could possibly have our null TE remove itself.
        return new TileEntityFzNull();
    }

    @Override
    public ItemStack getPickBlock(MovingObjectPosition target, World world, int x, int y, int z) {
        TileEntityCommon tec = new Coord(world, x, y, z).getTE(TileEntityCommon.class);
        if (tec == null) {
            return null;
        }
        return tec.getPickedBlock();
    }

    @Override
    public boolean hasTileEntity(int metadata) {
        return true;
    }

    @Override
    public boolean isBlockSolid(IBlockAccess world, int x, int y, int z, int side) {
        TileEntity t = world.getTileEntity(x, y, z);
        if (t == null || !(t instanceof TileEntityCommon)) {
            return false;
        }
        TileEntityCommon te = (TileEntityCommon) t;
        return te.isBlockSolidOnSide(side);
    }
    
    @Override
    public boolean isSideSolid(IBlockAccess world, int x, int y, int z, ForgeDirection side) {
        return isBlockSolid(world, x, y, z, side.ordinal());
    }
    
    @Override
    public void onNeighborBlockChange(World w, int x, int y, int z, Block neighbor) {
        int md = w.getBlockMetadata(x, y, z);
        TileEntity te = w.getTileEntity(x, y, z);
        if (te instanceof TileEntityCommon) {
            TileEntityCommon tec = (TileEntityCommon) te;
            tec.neighborChanged();
        }
    }

    //TODO: Ctrl/alt clicking!

    @Override
    public boolean onBlockActivated(World w, int x, int y, int z, EntityPlayer player, int side, float vecx, float vecy, float vecz) {
        // right click
        Coord here = new Coord(w, x, y, z);
        TileEntityCommon t = here.getTE(TileEntityCommon.class);
        if (t == null && w.isRemote) {
            Core.network.broadcastMessage(null, here, MessageType.DescriptionRequest);
        }
        if (player.isSneaking()) {
            ItemStack cur = player.getCurrentEquippedItem();
            if (cur == null || cur.getItem() != Core.registry.logicMatrixProgrammer) {
                return false;
            }
        }

        if (t != null) {
            return t.activate(player, ForgeDirection.getOrientation(side));
        }
        if (w.isRemote) {
            if (here.getTE() == null) {
                //we may be about to get a GUI, incidentally...
                Core.network.broadcastMessage(null, here, MessageType.DescriptionRequest);
                return false;
            }
            return false; //...?
        }
        player.addChatMessage(new ChatComponentText("This block is missing its TileEntity, possibly due to a bug in Factorization."));
        player.addChatMessage(new ChatComponentText("The block and its contents can not be recovered without cheating."));
        return true;
    }

    @Override
    public void onBlockClicked(World w, int x, int y, int z, EntityPlayer player) {
        // left click
        if (w.isRemote) return;

        TileEntity t = w.getTileEntity(x, y, z);
        if (t instanceof TileEntityCommon) {
            ((TileEntityCommon) t).click(player);
        }
    }
    
    @Override
    public void registerBlockIcons(IIconRegister reg) {
        FactorizationTextureLoader.register(reg, BlockIcons.class, null, "factorization:");
        Core.proxy.texturepackChanged(reg);
    }
    
    static public IIcon force_texture = null;
    
    @Override
    public IIcon getIcon(IBlockAccess w, int x, int y, int z, int side) {
        // Used for in-world rendering. Takes 'active' into consideration.
        if (force_texture != null) {
            return force_texture;
        }
        TileEntity te = w.getTileEntity(x, y, z);
        if (te instanceof TileEntityCommon) {
            TileEntityCommon tec = (TileEntityCommon) te;
            return tec.getIcon(ForgeDirection.getOrientation(side));
        }
        return BlockIcons.error;
    }

    private IIcon tempParticleIIcon = null;
    
    @Override
    public IIcon getIcon(int side, int md) {
        if (tempParticleIIcon != null) {
            IIcon ret = tempParticleIIcon;
            tempParticleIIcon = null;
            return ret;
        }
        // This shouldn't be called when rendering in the world.
        // Is used for inventory!
        FactoryType ft = FactoryType.fromMd((byte) md);
        if (ft == null) {
            return BlockIcons.default_icon;
            //return BlockIcons.error;
        }
        TileEntityCommon rep = ft.getRepresentative();
        if (rep == null) {
            return BlockIcons.error;
        }
        return rep.getIcon(ForgeDirection.getOrientation(side));
    }
    
    @Override
    public int damageDropped(int i) {
        return i;
    }

    @Override
    public int quantityDropped(int meta, int fortune, Random random) {
        return 1;
    }
    
    LinkedList<TileEntityCommon> destroyed_tes = new LinkedList<TileEntityCommon>();
    
    @Override
    public void breakBlock(World w, int x, int y, int z, Block id, int md) {
        Coord here = new Coord(w, x, y, z);
        TileEntityCommon te = here.getTE(TileEntityCommon.class);
        if (te != null) {
            te.onRemove();
            destroyed_tes.add(te);
        }
        super.breakBlock(w, x, y, z, id, md); //Just removes the TE; does nothing else.
    }
    
    @Override
    public boolean removedByPlayer(World world, EntityPlayer player, int x, int y, int z) {
        return removedByPlayer(world, player, x, y, z, true);
    }

    @Override
    public boolean removedByPlayer(World world, EntityPlayer player, int x, int y, int z, boolean willHarvest) {
        Coord here = new Coord(world, x, y, z);
        TileEntityCommon tec = here.getTE(TileEntityCommon.class);
        if (tec == null) {
            if (!world.isRemote) {
                new Notice(here, "There was no TileEntity!").send(player);
            }
            return world.setBlockToAir(x, y, z);
        }
        return tec.removedByPlayer(player, willHarvest);
    }

    @Override
    public ArrayList<ItemStack> getDrops(World world, int X, int Y, int Z, int md, int fortune) {
        ArrayList<ItemStack> ret = new ArrayList<ItemStack>();
        Coord here = new Coord(world, X, Y, Z);
        TileEntityCommon f = here.getTE(TileEntityCommon.class);
        if (f == null) {
            Iterator<TileEntityCommon> it = destroyed_tes.iterator();
            TileEntityCommon destroyedTE = null;
            while (it.hasNext()) {
                TileEntityCommon tec = it.next();
                if (tec.getCoord().equals(here)) {
                    destroyedTE  = tec;
                    it.remove();
                }
            }
            if (destroyedTE == null) {
                Core.logWarning("No IFactoryType TE behind block that was destroyed, and nothing saved!");
                return ret;
            }
            Coord destr = destroyedTE.getCoord();
            if (!destr.equals(here)) {
                Core.logWarning("Last saved destroyed TE wasn't for this location");
                destroyedTE = null;
                return ret;
            }
            f = destroyedTE;
            destroyedTE = null;
        }
        ItemStack is = f.getDroppedBlock();
        ret.add(is);
        return ret;
    }

    private static void put(List itemList, ItemStack item) {
        if (item == null) return;
        itemList.add(item);
    }
    
    @Override
    public void getSubBlocks(Item me, CreativeTabs tab, List itemList) {
        if (this != Core.registry.factory_block) {
            return;
        }
        Registry reg = Core.registry;
        //common
        
        put(itemList, reg.stamper_item);
        put(itemList, reg.packager_item);
        put(itemList, reg.slagfurnace_item);
        put(itemList, reg.parasieve_item);

        //electric
        //put(itemList, reg.battery_item_hidden);
        if (reg.battery != null) {
            //These checks are for buildcraft, which is hatin'.
            put(itemList, new ItemStack(reg.battery, 1, 2));
        }
        put(itemList, reg.leydenjar_item);
        put(itemList, reg.leydenjar_item_full);
        put(itemList, FactoryType.CREATIVE_CHARGE.itemStack());
        put(itemList, reg.caliometric_burner_item);
        put(itemList, reg.solarboiler_item);
        put(itemList, reg.steamturbine_item);
        if (reg.mirror != null) {
            put(itemList, new ItemStack(reg.mirror));
        }
        put(itemList, reg.heater_item);
        put(itemList, reg.leadwire_item);
        put(itemList, reg.mixer_item);
        put(itemList, reg.crystallizer_item);

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
        //put(itemList, reg.anchor);

        //Barrels
        if (todaysBarrels != null) {
            itemList.addAll(todaysBarrels);
        } else if (reg.daybarrel != null) {
            Calendar cal = Calendar.getInstance();
            int doy = cal.get(Calendar.DAY_OF_YEAR) - 1 /* start at 0, not 1 */;

            ReservoirSampler<ItemStack> barrelPool = new ReservoirSampler<ItemStack>(1, new Random(doy));
            todaysBarrels = new ArrayList<ItemStack>();

            for (ItemStack barrel : TileEntityDayBarrel.barrel_items) {
                TileEntityDayBarrel.Type type = TileEntityDayBarrel.getUpgrade(barrel);
                if (type == TileEntityDayBarrel.Type.NORMAL) {
                    barrelPool.give(barrel);
                } else if (type == TileEntityDayBarrel.Type.CREATIVE) {
                    todaysBarrels.add(barrel);
                }
            }

            TileEntityDayBarrel rep = new TileEntityDayBarrel();
            for (ItemStack barrel : barrelPool.getSamples()) {
                rep.loadFromStack(barrel);
                for (TileEntityDayBarrel.Type type : TileEntityDayBarrel.Type.values()) {
                    if (type == TileEntityDayBarrel.Type.CREATIVE) continue;
                    if (type == TileEntityDayBarrel.Type.LARGER) continue;
                    rep.type = type;
                    todaysBarrels.add(rep.getPickedBlock());
                }
            }
        }
    }

    ArrayList<ItemStack> todaysBarrels = null;

    @Override
    public boolean canConnectRedstone(IBlockAccess world, int x, int y, int z, int dir) {
        TileEntity te = world.getTileEntity(x, y, z);
        if (te instanceof TileEntityCommon) {
            TileEntityCommon tec = (TileEntityCommon) te;
            return tec.getFactoryType().connectRedstone();
        }
        return false;
    }

    @Override
    public boolean isNormalCube(IBlockAccess world, int i, int j, int k) {
        return BlockClass.get(world.getBlockMetadata(i, j, k)).isNormal();
    }

    @Override
    public int getFlammability(IBlockAccess world, int x, int y, int z, ForgeDirection face) {
        int md = world.getBlockMetadata(x, y, z);
        if (BlockClass.Barrel.md == md) {
            return 20;
        }
        return 0;
    }
    
    @Override
    public boolean isFlammable(IBlockAccess world, int x, int y, int z, ForgeDirection face) {
        //Not really. But this keeps fire rendering.
        return getFlammability(world, x, y, z, face) > 0;
    }

    //Lightair/lamp stuff

    @Override
    public int getLightValue(IBlockAccess world, int x, int y, int z) {
        int md = world.getBlockMetadata(x, y, z);
        BlockClass c = BlockClass.get(md);
        if (c == BlockClass.MachineDynamicLightable || c == BlockClass.MachineLightable) {
            TileEntity te = world.getTileEntity(x, y, z);
            if (te instanceof TileEntityCommon) {
                return ((TileEntityCommon) te).getDynamicLight();
            }
        }
        return c.lightValue;
    }

    @Override
    public float getBlockHardness(World w, int x, int y, int z) {
        BlockClass bc = BlockClass.get(w.getBlockMetadata(x, y, z));
        return bc.hardness;
    }

    //smack these blocks up
    @Override
    public MovingObjectPosition collisionRayTrace(World w, int x, int y, int z,
            Vec3 startVec, Vec3 endVec) {
        TileEntity te = w.getTileEntity(x, y, z);
        if (!(te instanceof TileEntityCommon)) return super.collisionRayTrace(w, x, y, z, startVec, endVec);
        TileEntityCommon tec = (TileEntityCommon) te;
        return tec.collisionRayTrace(startVec, endVec);
    }

    @Override
    public AxisAlignedBB getCollisionBoundingBoxFromPool(World w, int x, int y, int z) {
        TileEntity te = w.getTileEntity(x, y, z);
        if (!(te instanceof TileEntityCommon)) return super.getCollisionBoundingBoxFromPool(w, x, y, z);
        TileEntityCommon tec = (TileEntityCommon) te;
        return tec.getCollisionBoundingBoxFromPool();
    }
    
    @Override
    public void addCollisionBoxesToList(World w, int x, int y, int z, AxisAlignedBB aabb, List list, Entity entity) {
        TileEntity te = w.getTileEntity(x, y, z);
        TileEntityCommon tec = null;
        if (te instanceof TileEntityCommon) {
            tec = (TileEntityCommon) te;
        }
        Block test = w.isRemote ? Core.registry.clientTraceHelper : Core.registry.serverTraceHelper;
        if (tec == null || !tec.addCollisionBoxesToList(test, aabb, list, entity)) {
            super.addCollisionBoxesToList(w, x, y, z, aabb, list, entity);
        }
    }
    
    @Override
    public AxisAlignedBB getSelectedBoundingBoxFromPool(World w, int x, int y, int z) {
        TileEntity te = w.getTileEntity(x, y, z);
        if (te instanceof TileEntityCommon) {
            TileEntityCommon tec = (TileEntityCommon) te;
            if (tec.getFactoryType() == FactoryType.EXTENDED) {
                AxisAlignedBB ret = tec.getCollisionBoundingBoxFromPool();
                if (ret != null) {
                    return ret;
                }
            }
        }
        return super.getSelectedBoundingBoxFromPool(w, x, y, z);
    }

    @Override
    public void setBlockBoundsBasedOnState(IBlockAccess w, int x, int y, int z) {
        TileEntity te = w.getTileEntity(x, y, z);
        if (te == null || !(te instanceof TileEntityCommon)) {
            setBlockBounds(0, 0, 0, 1, 1, 1);
            return;
        }
        ((TileEntityCommon) te).setBlockBounds(this);
    }

    @Override
    public boolean renderAsNormalBlock() {
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
    public void updateTick(World w, int x, int y, int z, Random rand) {
        w.notifyBlockChange(x, y, z, this);
    }
    
    
    //Maybe we should only give weak power?
    @Override
    public int isProvidingStrongPower(IBlockAccess w, int x, int y, int z, int side) {
        return 0;
    }
    
    @Override
    public int isProvidingWeakPower(IBlockAccess w, int x, int y, int z, int side) {
        TileEntity te = w.getTileEntity(x, y, z);
        if (te instanceof TileEntityCommon) {
            return ((TileEntityCommon) te).power() ? 15 : 0;
        }
        return 0;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(World w, int x, int y, int z, Random rand) {
        TileEntity te = w.getTileEntity(x, y, z);
        if (te instanceof TileEntityCommon) {
            TileEntityCommon tec = (TileEntityCommon) te;
            tec.spawnDisplayTickParticles(rand);
        }
    }

    
    public static int CURRENT_PASS = 0;
    @Override
    public boolean canRenderInPass(int pass) {
        CURRENT_PASS = pass;
        return pass == 0 || pass == 1;
        // TODO: This is a bit lame. A bit of overhead just for barrels and mixers... 
    }

    @Override
    public int getRenderBlockPass() {
        return 1;
    }
    
    public static int sideDisable = 0;
    
    @Override
    public boolean shouldSideBeRendered(IBlockAccess iworld, int x, int y, int z, int side) {
        if (sideDisable != 0) {
            return (sideDisable & (1 << side)) == 0; 
        }
        return super.shouldSideBeRendered(iworld, x, y, z, side);
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public boolean addHitEffects(World worldObj, MovingObjectPosition target, EffectRenderer effectRenderer) {
        Coord here = new Coord(worldObj, target.blockX, target.blockY, target.blockZ);
        TileEntityCommon tec = here.getTE(TileEntityCommon.class);
        tempParticleIIcon = (tec == null) ? BlockIcons.default_icon : tec.getIcon(ForgeDirection.getOrientation(target.sideHit));
        return false;
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public boolean addDestroyEffects(World world, int x, int y, int z, int meta, EffectRenderer effectRenderer) {
        TileEntity te = world.getTileEntity(x, y, z);
        if (!(te instanceof TileEntityCommon)) {
            return false;
        }
        TileEntityCommon tec = (TileEntityCommon) te;
        IIcon theIIcon =  tec.getIcon(ForgeDirection.DOWN);
        
        //copied & modified from EffectRenderer.addDestroyEffects
        byte range = 4;
        for (int dx = 0; dx < range; ++dx) {
            for (int dy = 0; dy < range; ++dy) {
                for (int dz = 0; dz < range; ++dz) {
                    double px = x + (dx + 0.5) / range;
                    double py = y + (dy + 0.5) / range;
                    double pz = z + (dz + 0.5) / range;
                    EntityDiggingFX fx = new EntityDiggingFX(world, px, py, pz, px - x - 0.5, py - y - 0.5, pz - z - 0.5, this, meta);
                    fx.applyColourMultiplier(x, y, z);
                    fx.setParticleIcon(theIIcon);
                    effectRenderer.addEffect(fx);
                }
            }
        }
        
        return true;
    }
    
    @Override
    public boolean rotateBlock(World worldObj, int x, int y, int z, ForgeDirection axis) {
        final Coord at = new Coord(worldObj, x, y, z);
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
    public ForgeDirection[] getValidRotations(World worldObj, int x, int y, int z) {
        TileEntityCommon tec = new Coord(worldObj, x, y, z).getTE(TileEntityCommon.class);
        if (tec == null) {
            return TileEntityCommon.empty_rotation_array;
        }
        return tec.getValidRotations();
    }
    
    @Override
    public boolean hasComparatorInputOverride() {
        return true;
    }
    
    @Override
    public int getComparatorInputOverride(World world, int x, int y, int z, int side) {
        TileEntityCommon tec = new Coord(world, x, y, z).getTE(TileEntityCommon.class);
        if (tec == null) {
            return 0;
        }
        return tec.getComparatorValue(ForgeDirection.getOrientation(side));
    }
    
    @Override
    public boolean getBlocksMovement(IBlockAccess world, int x, int y, int z) {
        int md = world.getBlockMetadata(x, y, z);
        if (md == BlockClass.Wire.md) {
            return true;
        }
        return false;
    }
    
    @Override
    public void onNeighborChange(IBlockAccess world, int x, int y, int z, int tilex, int tiley, int tilez) {
        TileEntity te = world.getTileEntity(x, y, z);
        if (te instanceof TileEntityCommon) {
            ((TileEntityCommon) te).onNeighborTileChanged(tilex, tiley, tilez);
        }
    }
    
    @Override
    public boolean recolourBlock(World world, int x, int y, int z, ForgeDirection side, int colour) {
        TileEntity te = world.getTileEntity(x, y, z);
        if (te instanceof TileEntityCommon) {
            return ((TileEntityCommon) te).recolourBlock(side, FzColor.fromVanillaColorIndex(colour));
        }
        return false;
    }
}
