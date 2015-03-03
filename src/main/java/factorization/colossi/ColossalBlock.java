package factorization.colossi;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import factorization.util.PlayerUtil;
import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.WeightedRandomChestContent;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.common.ChestGenHooks;
import net.minecraftforge.common.util.ForgeDirection;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.api.Coord;
import factorization.common.BlockIcons;
import factorization.fzds.DeltaChunk;
import factorization.fzds.Hammer;
import factorization.fzds.TransferLib;
import factorization.fzds.interfaces.IDeltaChunk;
import factorization.oreprocessing.ItemOreProcessing;
import factorization.shared.Core;
import factorization.shared.Core.TabType;

public class ColossalBlock extends Block {
    static final byte MD_MASK = 0, MD_BODY = 4, MD_BODY_CRACKED = 1, MD_ARM = 2, MD_LEG = 3, MD_EYE = 5, MD_CORE = 6, MD_EYE_OPEN = 7, MD_BODY_COVERED = 8, MD_MASK_CRACKED = 9;

    static Material collosal_material = new Material(MapColor.purpleColor);

    static final int UP = ForgeDirection.UP.ordinal();
    static final int DOWN = ForgeDirection.DOWN.ordinal();
    static final int EAST = ForgeDirection.EAST.ordinal();

    public ColossalBlock() {
        super(collosal_material);
        setHardness(-1);
        setResistance(150);
        setHarvestLevel("pickaxe", 2);
        setStepSound(soundTypePiston);
        setBlockName("factorization:colossalBlock");
        Core.tab(this, TabType.BLOCKS);
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
    public IIcon getIcon(IBlockAccess w, int x, int y, int z, int side) {
        int md = w.getBlockMetadata(x, y, z);
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
    public float getBlockHardness(World world, int x, int y, int z) {
        int md = world.getBlockMetadata(x, y, z);
        if (md == MD_BODY_CRACKED || md == MD_MASK_CRACKED) {
            return 6; // 10
        }
        if (md == MD_MASK) {
            for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
                if (isSupportive(world, x + dir.offsetX, y + dir.offsetY, z + dir.offsetZ)) {
                    return super.getBlockHardness(world, x, y, z);
                }
            }
            return 50; // 100
        }
        return super.getBlockHardness(world, x, y, z);
    }
    
    boolean isSupportive(World world, int x, int y, int z) {
        if (world.getBlock(x, y, z) != this) return false;
        int md = world.getBlockMetadata(x, y, z);
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
    public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int md, int fortune) {
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
            Coord me = new Coord(world, x, y, z);
            Coord back = me.add(ForgeDirection.WEST);
            if (back.getBlock() == this && back.getMd() == MD_CORE) {
                TransferLib.move(back, me, true, true);
                back.setIdMd(this, MD_BODY, true);
            }
            Awakener.awaken(me);
        }
        return ret;
    }
    
    @Override
    public void randomDisplayTick(World world, int x, int y, int z, Random rand) {
        if (world.provider.dimensionId != Hammer.getDimensionId()) return;
        int md = world.getBlockMetadata(x, y, z);
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
    public ItemStack getPickBlock(MovingObjectPosition target, World world, int x, int y, int z) {
        return new ItemStack(this, 1, world.getBlockMetadata(x, y, z));
    }
    
    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float vecX, float vecY, float vecZ) {
        if (world.isRemote) return false;
        if (player == null) return false;
        Coord at = new Coord(world, x, y, z);
        ItemStack held = player.getHeldItem();
        int md = at.getMd();
        if (held != null && held.getItem() == Core.registry.logicMatrixProgrammer) {
            if (held.getItemDamage() == 0) {
                if (md == ColossalBlock.MD_CORE) {
                    held.setItemDamage(1);
                }
                return true;
            }
            // player.addChatComponentMessage(new ChatComponentTranslation("tile.factorization:colossalBlock." + md + ".click"));
            return true;
        }
        if (PlayerUtil.isPlayerCreative(player) && md == MD_CORE) {
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
        if (md == MD_CORE) {
            Awakener.awaken(at);
            return true;
        }
        return false;
    }
    
    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase player, ItemStack is) {
        super.onBlockPlacedBy(world, x, y, z, player, is);
        if (is.getItemDamage() != MD_CORE) {
            return;
        }
        Coord at = new Coord(world, x, y, z);
        at.setTE(new TileEntityColossalHeart());
    }
    
    @Override
    public boolean hasTileEntity(int metadata) {
        return metadata == MD_CORE;
    }
    
    @Override
    public void breakBlock(World world, int x, int y, int z, Block block, int md) {
        super.breakBlock(world, x, y, z, block, md);
        if (world.isRemote) return;
        if (world == DeltaChunk.getServerShadowWorld()) return;
        Coord at = new Coord(world, x, y, z);
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
}
