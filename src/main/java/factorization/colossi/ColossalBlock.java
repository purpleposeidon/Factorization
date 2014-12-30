package factorization.colossi;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
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
import factorization.shared.FzUtil;

public class ColossalBlock extends Block {
    static Material collosal_material = new Material(MapColor.purpleColor);
    
    public ColossalBlock() {
        super(collosal_material);
        setHardness(-1);
        setResistance(150);
        setHarvestLevel("pickaxe", 2);
        setStepSound(soundTypePiston);
        setBlockName("factorization:colossalBlock");
        Core.tab(this, TabType.BLOCKS);
    }
    
    static final byte MD_MASK = 0, MD_BODY = 4, MD_BODY_CRACKED = 1, MD_ARM = 2, MD_LEG = 3, MD_EYE = 5, MD_CORE = 6, MD_EYE_OPEN = 7;
    
    static final int EAST_SIDE = 5;
    
    @Override
    public IIcon getIcon(int side, int md) {
        switch (md) {
        case MD_BODY: return BlockIcons.colossi$body;
        case MD_BODY_CRACKED: return BlockIcons.colossi$body_cracked;
        case MD_ARM: return BlockIcons.colossi$arm;
        case MD_LEG: return BlockIcons.colossi$leg;
        case MD_MASK: return BlockIcons.colossi$mask;
        case MD_EYE: return BlockIcons.colossi$eye;
        case MD_CORE: return BlockIcons.colossi$core;
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
            if (side != EAST_SIDE) return BlockIcons.colossi$mask;
            return md == MD_EYE_OPEN ? BlockIcons.colossi$eye_open : BlockIcons.colossi$eye;
        }
        return getIcon(side, md);
    }
    
    @Override
    public float getBlockHardness(World world, int x, int y, int z) {
        if (world.getBlockMetadata(x, y, z) == MD_BODY_CRACKED) {
            return 6; // 10
        }
        if (world.getBlockMetadata(x, y, z) == MD_MASK) {
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
        return md == MD_BODY || md == MD_EYE || md == MD_EYE_OPEN || md == MD_CORE;
        
    }
    
    @Override
    public void registerBlockIcons(IIconRegister iconRegistry) { }
    
    @Override
    public void getSubBlocks(Item item, CreativeTabs tab, List list) {
        for (byte md = MD_MASK; md <= MD_EYE_OPEN; md++) {
            list.add(new ItemStack(this, 1, md));
        }
    }
    
    ChestGenHooks coreChest = new ChestGenHooks("factorization:colossalCore");
    boolean setup = false;
    ChestGenHooks getChest() {
        if (setup) return coreChest;
        setup = true;
        // No LMP, only the core drops the LMP.
        coreChest.addItem(new WeightedRandomChestContent(new ItemStack(Core.registry.logicMatrixIdentifier), 1, 1, 5));
        coreChest.addItem(new WeightedRandomChestContent(new ItemStack(Core.registry.logicMatrixController), 1, 1, 5));
        coreChest.addItem(new WeightedRandomChestContent(new ItemStack(Core.registry.diamond_shard), 2, 4, 1)); // Hrm. Not sure about this one.
        coreChest.addItem(new WeightedRandomChestContent(new ItemStack(Core.registry.ore_crystal, 1, ItemOreProcessing.OreType.DARKIRON.ID), 1, 7, 10));
        coreChest.addItem(new WeightedRandomChestContent(new ItemStack(Core.registry.insulated_coil), 4, 6, 8));
        coreChest.addItem(new WeightedRandomChestContent(Core.registry.dark_iron_sprocket.copy(), 2, 4, 2));
        coreChest.addItem(new WeightedRandomChestContent(Core.registry.servorail_item.copy(), 4, 10, 1));
        // TODO NORELEASE: Would it be better to drop a srapbox item instead!
        return coreChest;
    }
    
    @Override
    public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int metadata, int fortune) {
        ArrayList<ItemStack> ret = new ArrayList();
        if (metadata == MD_MASK) {
            ret.add(new ItemStack(this, 1, metadata));
        }
        if (metadata != MD_CORE && metadata != MD_BODY_CRACKED) {
            return ret;
        }
        if (metadata == MD_CORE) {
            ret.add(new ItemStack(Core.registry.logicMatrixProgrammer));
        }
        int count = 2 + world.rand.nextInt(3 + fortune);
        for (int i = 0; i < count; i++) {
            ret.add(getChest().getOneItem(world.rand));
        }
        if (metadata == MD_BODY_CRACKED) {
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
        if (world.provider.dimensionId != Hammer.dimensionID) return;
        int md = world.getBlockMetadata(x, y, z);
        int r = md == MD_BODY_CRACKED ? 4 : 2;
        float px = x - 0.5F + rand.nextFloat()*r;
        float py = y - 0.5F + rand.nextFloat()*r;
        float pz = z - 0.5F + rand.nextFloat()*r;
        switch (md) {
        case MD_BODY_CRACKED:
            world.spawnParticle("flame", px, py, pz, 0, 0, 0);
            break;
        case MD_CORE:
            world.spawnParticle("reddust", px, py, pz, 0, 0, 0);
            break;
        case MD_BODY:
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
        if (world.isRemote) return true;
        if (player == null) return false;
        Coord at = new Coord(world, x, y, z);
        ItemStack held = player.getHeldItem();
        if (held != null && held.getItem() == Core.registry.logicMatrixProgrammer) {
            int md = at.getMd();
            if (held.getItemDamage() == 0) {
                if (md == ColossalBlock.MD_CORE) {
                    held.setItemDamage(1);
                }
                return true;
            }
            // player.addChatComponentMessage(new ChatComponentTranslation("tile.factorization:colossalBlock." + md + ".click"));
            return true;
        }
        if (FzUtil.isPlayerCreative(player)) {
            TileEntityColossalHeart heart = at.getTE(TileEntityColossalHeart.class);
            if (heart != null) {
                if (player.isSneaking()) {
                    Awakener.awaken(at);
                    return true;
                }
                heart.showInfo(player);
            } else if ((at.getMd() == MD_EYE || at.getMd() == MD_EYE_OPEN) && player.isSneaking()) {
                Awakener.awaken(at);
                return true;
            }
            return false;
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
    @SideOnly(Side.CLIENT)
    public int colorMultiplier(IBlockAccess w, int x, int y, int z) {
        int ret = 0xFFFFFF;
        World mcw = Minecraft.getMinecraft().theWorld;
        if (mcw == null) return ret;
        if (mcw.provider.dimensionId == Hammer.dimensionID) return ret;
        return 0x9284B4;
    }
    
    @Override
    public void onBlockHarvested(World world, int x, int y, int z, int md, EntityPlayer player) {
        if (world.isRemote) return;
        if (world == DeltaChunk.getServerShadowWorld()) {
            if (md == MD_BODY_CRACKED) {
                Coord at = new Coord(world, x, y, z);
                ColossusController controller = findController(at);
                if (controller != null) {
                    controller.crackBroken();
                }
            }
        } else if (md == MD_EYE || md == MD_EYE_OPEN) {
            Awakener.awaken(new Coord(world, x, y, z));
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
