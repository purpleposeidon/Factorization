package factorization.colossi;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.MapColor;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IIcon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.WeightedRandomChestContent;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraftforge.common.ChestGenHooks;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.ForgeDirection;
import factorization.api.Coord;
import factorization.common.BlockIcons;
import factorization.fzds.Hammer;
import factorization.oreprocessing.ItemOreProcessing;
import factorization.shared.Core;
import factorization.shared.Core.TabType;

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
    
    static final byte MD_BODY = 0, MD_BODY_CRACKED = 1, MD_ARM = 2, MD_LEG = 3, MD_MASK = 4, MD_EYE = 5, MD_CORE = 6;
    
    @Override
    public IIcon getIcon(int side, int md) {
        switch (md) {
        case MD_BODY: return BlockIcons.colossi$body;
        case MD_BODY_CRACKED: return BlockIcons.colossi$body_cracked;
        case MD_ARM: return BlockIcons.colossi$arm;
        case MD_LEG: return BlockIcons.colossi$leg;
        case MD_MASK: return BlockIcons.colossi$mask;
        case MD_EYE: return BlockIcons.colossi$eye;
            // if (side == 5 /* EAST*/) 
            // return BlockIcons.colossi$mask;
        case MD_CORE: return BlockIcons.colossi$core;
        default: return super.getIcon(side, md);
        } 
    }
    
    @Override
    public float getBlockHardness(World world, int x, int y, int z) {
        if (world.getBlockMetadata(x, y, z) == MD_BODY_CRACKED) {
            return 10;
        }
        if (world.getBlockMetadata(x, y, z) == MD_MASK) {
            for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
                if (isSupportive(world, x + dir.offsetX, y + dir.offsetY, z + dir.offsetZ)) {
                    return super.getBlockHardness(world, x, y, z);
                }
            }
            return 100;
        }
        return super.getBlockHardness(world, x, y, z);
    }
    
    boolean isSupportive(World world, int x, int y, int z) {
        if (world.getBlock(x, y, z) != this) return false;
        int md = world.getBlockMetadata(x, y, z);
        return md == MD_BODY || md == MD_BODY_CRACKED || md == MD_EYE || md == MD_CORE;
        
    }
    
    @Override
    public void registerBlockIcons(IIconRegister iconRegistry) { }
    
    @Override
    public void getSubBlocks(Item item, CreativeTabs tab, List list) {
        for (byte md = MD_BODY; md <= MD_CORE; md++) {
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
        coreChest.addItem(new WeightedRandomChestContent(new ItemStack(Core.registry.diamond_shard), 2, 4, 1));
        coreChest.addItem(new WeightedRandomChestContent(new ItemStack(Core.registry.ore_reduced, 1, ItemOreProcessing.OreType.DARKIRON.ID), 1, 7, 10));
        coreChest.addItem(new WeightedRandomChestContent(new ItemStack(Core.registry.motor), 1, 1, 8));
        coreChest.addItem(new WeightedRandomChestContent(Core.registry.servo_motor.copy(), 1, 1, 3));
        coreChest.addItem(new WeightedRandomChestContent(Core.registry.dark_iron_sprocket.copy(), 2, 4, 2));
        coreChest.addItem(new WeightedRandomChestContent(Core.registry.servorail_item.copy(), 4, 10, 1));
        // TODO NORELEASE: It'd be better to drop a srapbox item instead!
        // What about lead & silver?
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
    
    double getSensitivity(int md) {
        switch (md) {
        case MD_BODY:
        case MD_BODY_CRACKED:
            return 0.2;
        case MD_EYE:
        case MD_CORE:
            return 1;
        case MD_ARM:
        case MD_LEG:
            return 0.1;
        default: return 0;
        }
    }
    
    boolean maybeAwaken(Coord at, double boldNess) {
        if (at.w.isRemote) return false;
        if (at.getDimensionID() == Hammer.dimensionID) return false;
        if (getSensitivity(at.getMd()) * boldNess < at.w.rand.nextDouble()) {
            return false;
        }
        Awakener.awaken(at);
        return true;
    }
    
    boolean playerImmune(Entity ent) {
        if (ent instanceof EntityPlayer) {
            if (ent instanceof FakePlayer) {
                return false;
            }
            return ((EntityPlayer) ent).capabilities.isCreativeMode;
        }
        return true;
    }
    
    boolean nearbyNoisyPlayer(World world, int x, int y, int z) {
        double distSquared = 8*8;
        for (Entity player : (Iterable<Entity>) world.playerEntities) {
            if (player.getDistanceSq(x, y, z) < distSquared && !playerImmune(player)) return true;
        }
        return false;
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
            player.addChatComponentMessage(new ChatComponentTranslation("tile.factorization:colossalBlock." + md + ".click"));
            return true;
        }
        if (playerImmune(player)) {
            TileEntityColossalHeart heart = at.getTE(TileEntityColossalHeart.class);
            if (heart != null) {
                if (player.isSneaking()) {
                    Awakener.awaken(at);
                    return true;
                }
                heart.showInfo(player);
            }
            return false;
        }
        
        if (world.isRemote) return false;
        maybeAwaken(at, 10);
        return false;
    }
    
    @Override
    public void onNeighborBlockChange(World world, int x, int y, int z, Block block) {
        if (!nearbyNoisyPlayer(world, x, y, z)) return;
        maybeAwaken(new Coord(world, x, y, z), 1);
    }
    
    @Override
    public void onEntityCollidedWithBlock(World world, int x, int y, int z, Entity ent) {
        if (!playerImmune(ent)) {
            maybeAwaken(new Coord(world, x, y, z), sneakiness(ent));
        }
    }
    
    @Override
    public void onBlockDestroyedByExplosion(World world, int x, int y, int z, Explosion boomBoom) {
        if (nearbyNoisyPlayer(world, x, y, z)) {
            maybeAwaken(new Coord(world, x, y, z), 10);
        }
    }
    
    @Override
    public void onBlockClicked(World world, int x, int y, int z, EntityPlayer player) {
        if (!playerImmune(player)) {
            maybeAwaken(new Coord(world, x, y, z), 2);
        }
    }
    
    @Override
    public void onEntityWalking(World world, int x, int y, int z, Entity ent) {
        if (!playerImmune(ent)) {
            maybeAwaken(new Coord(world, x, y, z), sneakiness(ent));
        }
    }
    
    double sneakiness(Entity ent) {
        if (ent.isSprinting()) return 7;
        if (ent.isSneaking()) return 0.05;
        return 1;
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
}
