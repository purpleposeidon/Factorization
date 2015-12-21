package factorization.colossi;

import java.util.*;

import factorization.util.DataUtil;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.WeightedRandomChestContent;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import net.minecraftforge.common.ChestGenHooks;

import factorization.api.Coord;
import factorization.citizen.EntityCitizen;
import factorization.fzds.DeltaChunk;
import factorization.fzds.TransferLib;
import factorization.fzds.interfaces.IDeltaChunk;
import factorization.oreprocessing.ItemOreProcessing;
import factorization.servo.ItemMatrixProgrammer;
import factorization.shared.Core;
import factorization.shared.Core.TabType;
import factorization.util.PlayerUtil;
import factorization.weird.poster.EntityPoster;
import factorization.weird.poster.ItemSpawnPoster;
import static factorization.colossi.ColossalBlock.Md.ARM;
import static factorization.colossi.ColossalBlock.Md.BODY;
import static factorization.colossi.ColossalBlock.Md.BODY_COVERED;
import static factorization.colossi.ColossalBlock.Md.BODY_CRACKED;
import static factorization.colossi.ColossalBlock.Md.CORE;
import static factorization.colossi.ColossalBlock.Md.EYE;
import static factorization.colossi.ColossalBlock.Md.EYE_OPEN;
import static factorization.colossi.ColossalBlock.Md.MASK;
import static factorization.colossi.ColossalBlock.Md.MASK_CRACKED;
import static factorization.colossi.ColossalBlock.Md.values;

public class ColossalBlock extends Block {
    public enum Md implements IStringSerializable, Comparable<Md> {
        MASK, BODY, BODY_CRACKED, ARM, LEG, EYE, CORE, EYE_OPEN, BODY_COVERED, MASK_CRACKED;

        @Override
        public String getName() {
            return this.toString().toLowerCase(Locale.ROOT);
        }

        public static final Md[] values = values();
    }
    enum Capping implements IStringSerializable, Comparable<Capping> {
        NONE, UP, DOWN;

        @Override
        public String getName() {
            return this.toString().toLowerCase(Locale.ROOT);
        }
    }
    public static final PropertyEnum<Md> VARIANT = PropertyEnum.create("variant", Md.class);
    public static final PropertyEnum<Capping> CAPPING = PropertyEnum.create("capping", Capping.class);

    static Material collosal_material = new Material(MapColor.purpleColor);

    public ColossalBlock() {
        super(collosal_material);
        setHardness(-1);
        setResistance(150);
        setHarvestLevel("pickaxe", 2);
        setStepSound(soundTypePiston);
        setUnlocalizedName("factorization:colossalBlock");
        Core.tab(this, TabType.BLOCKS);
        DeltaChunk.assertEnabled();
    }

    @Override
    protected BlockState createBlockState() {
        return new BlockState(this, VARIANT, CAPPING);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        return getDefaultState().withProperty(VARIANT, Md.values[meta]);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(VARIANT).ordinal();
    }

    @Override
    public IBlockState getActualState(IBlockState bs, IBlockAccess w, BlockPos pos) {
        if (bs.getValue(VARIANT) == ARM) {
            boolean goesDown = DataUtil.getOr(w.getBlockState(pos.down()), VARIANT, null) == ARM;
            boolean goesUp = DataUtil.getOr(w.getBlockState(pos.up()), VARIANT, null) == ARM;
            if (goesUp != goesDown) {
                return bs.withProperty(CAPPING, goesDown ? Capping.UP : Capping.DOWN);
            }
        }
        return bs.withProperty(CAPPING, Capping.NONE);
    }

    @Override
    public float getBlockHardness(World world, BlockPos pos) {
        IBlockState bs = world.getBlockState(pos);
        Md md = bs.<Md>getValue(VARIANT);
        if (md == BODY_CRACKED || md == MASK_CRACKED) {
            return 6;
        }
        if (md == MASK) {
            for (EnumFacing dir : EnumFacing.VALUES) {
                if (isSupportive(world, pos.offset(dir))) {
                    return super.getBlockHardness(world, pos);
                }
            }
            return 50; // 100
        }
        return super.getBlockHardness(world, pos);
    }
    
    boolean isSupportive(World world, BlockPos pos) {
        IBlockState bs = world.getBlockState(pos);
        if (bs.getBlock() != this) return false;

        Md md = bs.<Md>getValue(VARIANT);
        return md == BODY || md == BODY_COVERED || md == EYE || md == EYE_OPEN || md == CORE;
    }

    @Override
    public void getSubBlocks(Item item, CreativeTabs tab, List<ItemStack> list) {
        for (Md md : values()) {
            if (md == BODY_COVERED) continue;
            list.add(new ItemStack(this, 1, getMetaFromState(getDefaultState().withProperty(VARIANT, md))));
        }
    }
    
    ChestGenHooks fractureChest = new ChestGenHooks("factorization:colossalFracture");
    boolean setup = false;
    ChestGenHooks getChest() {
        if (setup) return fractureChest;
        setup = true;
        // No LMP: only the core drops the LMP.
        fractureChest.addItem(new WeightedRandomChestContent(new ItemStack(Core.registry.logicMatrixIdentifier), 1, 1, 6));
        fractureChest.addItem(new WeightedRandomChestContent(new ItemStack(Core.registry.logicMatrixController), 1, 1, 6));
        fractureChest.addItem(new WeightedRandomChestContent(new ItemStack(Core.registry.ore_crystal, 1, ItemOreProcessing.OreType.DARKIRON.ID), 1, 2, 12));
        fractureChest.addItem(new WeightedRandomChestContent(Core.registry.dark_iron_sprocket.copy(), 2, 4, 1));

        return fractureChest;
    }

    Random rand = new Random();
    @Override
    public List<ItemStack> getDrops(IBlockAccess world, BlockPos pos, IBlockState bs, int fortune) {
        ArrayList<ItemStack> ret = new ArrayList<ItemStack>();
        Md md = bs.<Md>getValue(VARIANT);
        if (md == MASK) {
            ret.add(createStackedBlock(bs));
        }
        if (md == BODY_CRACKED || md == MASK_CRACKED) {
            int count = 1 + rand.nextInt(1 + fortune);
            for (int i = 0; i < count; i++) {
                ret.add(getChest().getOneItem(rand));
            }
        }
        if (md == CORE) {
            ret.add(new ItemStack(Core.registry.logicMatrixProgrammer));
        }
        if (md == BODY_CRACKED || md == MASK_CRACKED) {
            if (world instanceof World) {
                // Need World for the TransferLib call
                Coord me = new Coord((World) world, pos);
                Coord back = me.add(EnumFacing.WEST);
                if (me.has(VARIANT, CORE)) {
                    TransferLib.move(back, me, true, true);
                    back.set(getDefaultState().withProperty(VARIANT, BODY), true);
                }
                Awakener.awaken(me);
            }
        }
        return ret;
    }

    @Override
    public void randomDisplayTick(World world, BlockPos pos, IBlockState state, Random rand) {
        if (world.provider.getDimensionId() != DeltaChunk.getDimensionId()) return;
        Md md = state.<Md>getValue(VARIANT);
        int r = md == BODY_CRACKED ? 4 : 2;
        float px = pos.getX() - 0.5F + rand.nextFloat()*r;
        float py = pos.getY() - 0.5F + rand.nextFloat()*r;
        float pz = pos.getZ() - 0.5F + rand.nextFloat()*r;
        EnumParticleTypes particle;
        switch (md) {
        case BODY_CRACKED:
        case MASK_CRACKED:
            particle = EnumParticleTypes.FLAME;
            break;
        case CORE:
            particle = EnumParticleTypes.REDSTONE;
            break;
        case BODY:
        case BODY_COVERED:
            if (rand.nextInt(256) != 0) {
                return;
            }
            particle = EnumParticleTypes.EXPLOSION_NORMAL;
            break;
        case MASK:
        case EYE:
        case EYE_OPEN:
            particle = EnumParticleTypes.SUSPENDED_DEPTH;
            break;
        default:
        case ARM:
        case LEG:
            return;
        }
        world.spawnParticle(particle, px, py, pz, 0, 0, 0);
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer player, EnumFacing side, float hitX, float hitY, float hitZ) {
        if (world.isRemote) return false;
        if (player == null) return false;
        Coord at = new Coord(world, pos);
        ItemStack held = player.getHeldItem();
        Md md = state.<Md>getValue(VARIANT);
        if (md != Md.CORE) return false;
        if (held != null && held.getItem() == Core.registry.logicMatrixProgrammer && world == DeltaChunk.getServerShadowWorld()) {
            /*if (Core.registry.logicMatrixProgrammer.isAuthenticated(held)) return true;
            EntityPlayer realPlayer = DeltaChunk.getRealPlayer(player);
            if (realPlayer instanceof FakePlayer || realPlayer == null) {
                return true;
            }
            if (realPlayer.worldObj == world) return true;
            return giveUserAuthentication(held, realPlayer, at);
            NORELEASE.fixme("Wither test: player should be able to survive a wither while grabbed by a citizen"); */
        }
        if (PlayerUtil.isPlayerCreative(player)) {
            TileEntityColossalHeart heart = at.getTE(TileEntityColossalHeart.class);
            if (heart != null) {
                if (player.isSneaking()) {
                    Awakener.awaken(at);
                    return true;
                }
                heart.showInfo(player);
            }
            return true;
        }
        Awakener.awaken(at);
        return true;
    }

    private boolean giveUserAuthentication(ItemStack held, EntityPlayer player, Coord at) {
        if (player == null) return true;
        if (player.worldObj == DeltaChunk.getServerShadowWorld()) return true;
        if (!(player instanceof EntityPlayerMP)) return true;
        if (ItemMatrixProgrammer.isUserAuthenticated((EntityPlayerMP) player)) {
            Core.registry.logicMatrixProgrammer.setAuthenticated(held);
        } else {
            ColossusController controller = findController(at);
            if (controller == null) return true;
            if (controller.getHealth() <= 0) return true;
            controller.ai_controller.forceState(Technique.HACKED);
            placePoster(held, player, at);
            EntityCitizen.spawnOn((EntityPlayerMP) player);
        }
        return true;
    }

    private void placePoster(ItemStack held, EntityPlayer player, Coord at) {
        ItemSpawnPoster.PosterPlacer placer = new ItemSpawnPoster.PosterPlacer(new ItemStack(Core.registry.spawnPoster), player, at.w, at.toBlockPos(), EnumFacing.EAST);
        placer.invoke();
        final EntityPoster poster = placer.result;
        poster.locked = true;
        poster.inv = held.copy();
        poster.spin_tilt = -8;
        poster.spin_vertical = -8;
        poster.updateValues();
        poster.posX += 1.5 / 16.0;
        placer.spawn();
        poster.locked = true;
    }

    @Override
    public IBlockState onBlockPlaced(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
        IBlockState ibs =  super.onBlockPlaced(world, pos, facing, hitX, hitY, hitZ, meta, placer);
        if (ibs.getValue(VARIANT) != Md.CORE) return ibs;
        Coord at = new Coord(world, pos);
        at.setTE(new TileEntityColossalHeart());
        return ibs;
    }

    @Override
    public boolean hasTileEntity(IBlockState state) {
        return state.getValue(VARIANT) == Md.CORE;
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        super.breakBlock(world, pos, state);
        if (world.isRemote) return;
        if (world == DeltaChunk.getServerShadowWorld()) return;
        Coord at = new Coord(world, pos);
        if (at.has(VARIANT, Md.BODY_CRACKED, Md.MASK_CRACKED)) {
            for (Coord neighbor : at.getNeighborsAdjacent()) {
                if (neighbor.has(VARIANT, Md.BODY)) {
                    int air = 0;
                    for (Coord n2 : neighbor.getNeighborsAdjacent()) {
                        if (n2.isAir()) air++;
                    }
                    if (air <= 1) {
                        neighbor.set(VARIANT, Md.BODY_COVERED);
                    }
                }
            }
            Awakener.awaken(at);
        }
    }
    
    public ColossusController findController(Coord at) {
        TileEntityColossalHeart heart = Awakener.findNearestHeart(at);
        if (heart == null) return null;
        UUID controllerId = heart.controllerUuid;
        if (controllerId.equals(TileEntityColossalHeart.noController)) return null;
        for (IDeltaChunk idc : DeltaChunk.getSlicesContainingPoint(at)) {
            Object c = idc.getController();
            if (c instanceof ColossusController) {
                ColossusController controller = (ColossusController) c;
                if (controller.getUniqueID().equals(controllerId)) {
                    return controller;
                }
            }
        }
        return null;
    }

    @Override
    public int getLightValue(IBlockAccess world, BlockPos pos) {
        return 8; // FZDS has lighting glitches :(
    }
}
