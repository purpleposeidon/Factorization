package factorization.colossi;

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
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.ChestGenHooks;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class ColossalBlock extends Block {
    public enum MD implements IStringSerializable {
        MASK, BODY, BODY_CRACKED, ARM, LEG, EYE, CORE, EYE_OPEN, BODY_COVERED, MASK_CRACKED;

        @Override
        public String getName() {
            return this.toString();
        }
    }
    public static final PropertyEnum VARIANT = PropertyEnum.create("variant", MD.class);

    static Material collosal_material = new Material(MapColor.purpleColor);

    static final int UP = EnumFacing.UP.ordinal();
    static final int DOWN = EnumFacing.DOWN.ordinal();
    static final int EAST = EnumFacing.EAST.ordinal();

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
    public IIcon getIcon(int side, int md) {
        switch (md) {
        case MD_BODY: return BlockIcons.colossi$body;
        case MD_BODY_COVERED: return BlockIcons.colossi$body;
        case MD_BODY_CRACKED: return BlockIcons.colossi$body_cracked;
        case MD_ARM: return BlockIcons.colossi$arm_side; // Item-only
        case MD_LEG: return BlockIcons.colossi$leg;
        case MD_MASK: return BlockIcons.colossi$mask;
        case MD_MASK_CRACKED: return BlockIcons.colossi$mask_cracked;
        case MD_EYE: return BlockIcons.colossi$eye; // Item-only
        case MD_CORE: {
            if (side == EAST) return BlockIcons.colossi$core;
            return BlockIcons.colossi$core_back;
        }
        case MD_EYE_OPEN: return BlockIcons.colossi$eye_open;
        default: return super.getIcon(side, md);
        }
    }
    
    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(IBlockAccess w, BlockPos pos, EnumFacing side) {
        int md = w.getBlockMetadata(pos);
        if (md == MD_EYE || md == MD_EYE_OPEN) {
            // This is here rather than up there so that the item form doesn't look lame
            if (side != EAST) return BlockIcons.colossi$mask;
            return md == MD_EYE_OPEN ? BlockIcons.colossi$eye_open : BlockIcons.colossi$eye;
        }
        if (md == MD_ARM) {
            if (side == UP) return BlockIcons.colossi$arm_top;
            if (side == DOWN) return BlockIcons.colossi$arm_bottom;
            Block downId = w.getBlock(x, y - 1, z);
            int downMd = w.getBlockMetadata(x, y - 1, z);
            Block upId = w.getBlock(x, y + 1, z);
            int upMd = w.getBlockMetadata(x, y + 1, z);

            if (downId == this && downMd == MD_ARM) {
                if (upId != this || upMd != MD_ARM) {
                    return BlockIcons.colossi$arm_side_top;
                }
                return BlockIcons.colossi$arm_side;
            }
            return BlockIcons.colossi$arm_side_bottom;
        }
        return getIcon(side, md);
    }
    
    @Override
    public float getBlockHardness(World world, BlockPos pos) {
        int md = world.getBlockMetadata(pos);
        if (md == MD_BODY_CRACKED || md == MD_MASK_CRACKED) {
            return 6; // 10
        }
        if (md == MD_MASK) {
            for (EnumFacing dir : EnumFacing.VALUES) {
                if (isSupportive(world, x + dir.getDirectionVec().getX(), y + dir.getDirectionVec().getY(), z + dir.getDirectionVec().getZ())) {
                    return super.getBlockHardness(world, pos);
                }
            }
            return 50; // 100
        }
        return super.getBlockHardness(world, pos);
    }
    
    boolean isSupportive(World world, BlockPos pos) {
        if (world.getBlock(pos) != this) return false;
        int md = world.getBlockMetadata(pos);
        return md == MD_BODY || md == MD_BODY_COVERED || md == MD_EYE || md == MD_EYE_OPEN || md == MD_CORE;
    }
    
    @Override
    public void registerBlockIcons(IIconRegister iconRegistry) { }
    
    @Override
    public void getSubBlocks(Item item, CreativeTabs tab, List list) {
        for (byte md = MD_MASK; md <= MD_MASK_CRACKED; md++) {
            if (md == MD_BODY_COVERED) continue; // Technical block; skip
            list.add(new ItemStack(this, 1, md));
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
    
    @Override
    public ArrayList<ItemStack> getDrops(World world, BlockPos pos, int md, int fortune) {
        ArrayList<ItemStack> ret = new ArrayList();
        if (md == MD_MASK) {
            ret.add(new ItemStack(this, 1, md));
        }
        if (md == MD_BODY_CRACKED || md == MD_MASK_CRACKED) {
            int count = 1 + world.rand.nextInt(1 + fortune);
            for (int i = 0; i < count; i++) {
                ret.add(getChest().getOneItem(world.rand));
            }
        }
        if (md == MD_CORE) {
            ret.add(new ItemStack(Core.registry.logicMatrixProgrammer));
        }
        if (md == MD_BODY_CRACKED || md == MD_MASK_CRACKED) {
            Coord me = new Coord(world, pos);
            Coord back = me.add(EnumFacing.WEST);
            if (back.getBlock() == this && back.getMd() == MD_CORE) {
                TransferLib.move(back, me, true, true);
                back.setIdMd(this, MD_BODY, true);
            }
            Awakener.awaken(me);
        }
        return ret;
    }
    
    @Override
    public void randomDisplayTick(World world, BlockPos pos, Random rand) {
        if (world.provider.getDimensionId() != DeltaChunk.getDimensionId()) return;
        int md = world.getBlockMetadata(pos);
        int r = md == MD_BODY_CRACKED ? 4 : 2;
        float px = x - 0.5F + rand.nextFloat()*r;
        float py = y - 0.5F + rand.nextFloat()*r;
        float pz = z - 0.5F + rand.nextFloat()*r;
        switch (md) {
        case MD_BODY_CRACKED:
        case MD_MASK_CRACKED:
            world.spawnParticle("flame", px, py, pz, 0, 0, 0);
            break;
        case MD_CORE:
            world.spawnParticle("reddust", px, py, pz, 0, 0, 0);
            break;
        case MD_BODY:
        case MD_BODY_COVERED:
            if (rand.nextInt(256) == 0) {
                world.spawnParticle("explode", px, py, pz, 0, 0, 0);
            }
            break;
        case MD_MASK:
        case MD_EYE:
        case MD_EYE_OPEN:
            world.spawnParticle("depthsuspend", px, py, pz, 0, 0, 0);
            break;
        default:
        case MD_ARM:
        case MD_LEG:
            break;
        } 
    }
    
    @Override
    public ItemStack getPickBlock(MovingObjectPosition target, World world, BlockPos pos) {
        return new ItemStack(this, 1, world.getBlockMetadata(pos));
    }
    
    @Override
    public boolean onBlockActivated(World world, BlockPos pos, EntityPlayer player, int side, float vecX, float vecY, float vecZ) {
        if (world.isRemote) return false;
        if (player == null) return false;
        Coord at = new Coord(world, pos);
        ItemStack held = player.getHeldItem();
        int md = at.getMd();
        if (md != MD_CORE) return false;
        /*if (held != null && held.getItem() == Core.registry.logicMatrixProgrammer && world == DeltaChunk.getServerShadowWorld()) {
            if (Core.registry.logicMatrixProgrammer.isAuthenticated(held)) return true;
            EntityPlayer realPlayer = DeltaChunk.getRealPlayer(player);
            if (realPlayer instanceof FakePlayer || realPlayer == null) {
                return true;
            }
            if (realPlayer.worldObj == world) return true;
            return giveUserAuthentication(held, realPlayer, at);
            NORELEASE.fixme("Wither test: player should be able to survive a wither while grabbed by a citizen");
        }*/
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
        ItemSpawnPoster.PosterPlacer placer = new ItemSpawnPoster.PosterPlacer(new ItemStack(Core.registry.spawnPoster), player, at.w, at.x, at.y, at.z, EnumFacing.EAST.ordinal());
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
    public void onBlockPlacedBy(World world, BlockPos pos, EntityLivingBase player, ItemStack is) {
        super.onBlockPlacedBy(world, pos, player, is);
        if (is.getItemDamage() != MD_CORE) {
            return;
        }
        Coord at = new Coord(world, pos);
        at.setTE(new TileEntityColossalHeart());
    }
    
    @Override
    public boolean hasTileEntity(int metadata) {
        return metadata == MD_CORE;
    }
    
    @Override
    public void breakBlock(World world, BlockPos pos, Block block, int md) {
        super.breakBlock(world, pos, block, md);
        if (world.isRemote) return;
        if (world == DeltaChunk.getServerShadowWorld()) return;
        Coord at = new Coord(world, pos);
        if (md == MD_BODY_CRACKED || md == MD_MASK_CRACKED) {
            for (Coord neighbor : at.getNeighborsAdjacent()) {
                if (neighbor.getBlock() == this && neighbor.getMd() == MD_BODY) {
                    int air = 0;
                    for (Coord n2 : neighbor.getNeighborsAdjacent()) {
                        if (n2.isAir()) air++;
                    }
                    if (air <= 1) {
                        neighbor.setMd(MD_BODY_COVERED);
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
        return 8;
    }
}
