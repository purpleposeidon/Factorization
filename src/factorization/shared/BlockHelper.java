package factorization.shared;

import static factorization.shared.BlockHelper.BlockStyle.BED;
import static factorization.shared.BlockHelper.BlockStyle.CAKE;
import static factorization.shared.BlockHelper.BlockStyle.CLONE_MD;
import static factorization.shared.BlockHelper.BlockStyle.CROP;
import static factorization.shared.BlockHelper.BlockStyle.DOOR;
import static factorization.shared.BlockHelper.BlockStyle.NOTHING;
import static factorization.shared.BlockHelper.BlockStyle.PISTON_EXTENSION;
import static factorization.shared.BlockHelper.BlockStyle.REDSTONE_ORE;
import static factorization.shared.BlockHelper.BlockStyle.SLAB;
import static factorization.shared.BlockHelper.BlockStyle.STEM;
import static factorization.shared.BlockHelper.BlockStyle.USE_GET_BLOCK_DROPPED;
import static factorization.shared.BlockHelper.BlockStyle.USE_GET_DAMAGE_VALUE;
import static factorization.shared.BlockHelper.BlockStyle.USE_ID_DROPPED;

import java.util.ArrayList;

import net.minecraft.block.Block;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockBrewingStand;
import net.minecraft.block.BlockCauldron;
import net.minecraft.block.BlockCocoa;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockEndPortal;
import net.minecraft.block.BlockFarmland;
import net.minecraft.block.BlockFlowerPot;
import net.minecraft.block.BlockFurnace;
import net.minecraft.block.BlockHugeMushroom;
import net.minecraft.block.BlockMobSpawner;
import net.minecraft.block.BlockNetherWart;
import net.minecraft.block.BlockOre;
import net.minecraft.block.BlockPistonMoving;
import net.minecraft.block.BlockPortal;
import net.minecraft.block.BlockRedstoneComparator;
import net.minecraft.block.BlockRedstoneLight;
import net.minecraft.block.BlockRedstoneRepeater;
import net.minecraft.block.BlockRedstoneTorch;
import net.minecraft.block.BlockRedstoneWire;
import net.minecraft.block.BlockReed;
import net.minecraft.block.BlockSign;
import net.minecraft.block.BlockSilverfish;
import net.minecraft.block.BlockSkull;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockTripWire;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

public class BlockHelper
{
    public static enum BlockStyle {
        // No value has been cached. This is the default value.
        UNDECIDED,
        // Do not return an ItemStack.
        NOTHING,
        // Call Blocks.getDamageValue to get the ItemStack
        USE_GET_DAMAGE_VALUE,
        // Call Blocks.idDropped to get the ItemStack
        USE_ID_DROPPED,
        // Return the first thing from Blocks.getDrops.
        USE_GET_BLOCK_DROPPED,
        // Return the block with its metadata
        CLONE_MD,

        // These blocks do weird things and need to be special-cased
        STEM,
        SLAB,
        CAKE,
        CROP,
        DOOR,
        REDSTONE_ORE,
        PISTON_EXTENSION,
        BED
    }

    static BlockStyle getBlockStyle(Block block)
    {
        return classifyBlock(block);
    }

    private static BlockStyle classifyBlock(Block block)
    {
        // These blocks don't have a good way of extracting the item,
        // so no instanceof.
        if (block == Blocks.cake)
        {
            return CAKE;
        }
        if (block == Blocks.redstone_ore || block == Blocks.lit_redstone_ore)
        {
            return REDSTONE_ORE;
        }
        if (block == Blocks.piston_extension)
        {
            return PISTON_EXTENSION;
        }
        if (block == Blocks.melon_stem || block == Blocks.pumpkin_stem)
        {
            return STEM;
        }
        if (block instanceof BlockSign || block instanceof BlockFlowerPot || block instanceof BlockRedstoneWire || block instanceof BlockBrewingStand
                || block instanceof BlockReed || block instanceof BlockTripWire || block instanceof BlockCauldron || block instanceof BlockRedstoneRepeater
                || block instanceof BlockRedstoneComparator || block instanceof BlockRedstoneTorch || block instanceof BlockFarmland || block instanceof BlockFurnace
                || block instanceof BlockHugeMushroom || block instanceof BlockRedstoneLight)
        {
            return USE_ID_DROPPED;
        }
        if (block instanceof BlockCocoa || block instanceof BlockNetherWart || block instanceof BlockSkull)
        {
            return USE_GET_BLOCK_DROPPED;
        }
        if (block instanceof BlockPistonMoving || block instanceof BlockPortal || block instanceof BlockEndPortal || block instanceof BlockSilverfish
                || block instanceof BlockMobSpawner)
        {
            return NOTHING;
        }
        if (block instanceof BlockOre)
        {
            return CLONE_MD;
        }
        // Special blocks
        if (block instanceof BlockSlab)
        {
            return SLAB;
        }
        if (block instanceof BlockCrops)
        {
            return CROP;
        }
        if (block instanceof BlockBed)
        {
            return BED;
        }
        if (block instanceof BlockDoor)
        {
            return DOOR;
        }
        return USE_GET_DAMAGE_VALUE;
    }

    private static ItemStack makeItemStack(Item itemId, int stackSize, int damage)
    {
        if (itemId == null)
        {
            return null;
        }
        return new ItemStack(itemId, stackSize, damage);
    }

    public static ItemStack getPlacingItem(Block block, MovingObjectPosition target, World world, int x, int y, int z)
    {
        int md;
        switch (classifyBlock(block))
        {
            default:
            case UNDECIDED:
            case NOTHING:
            case PISTON_EXTENSION:
                return null;
            case USE_GET_DAMAGE_VALUE:
                return new ItemStack(block, 1, block.getDamageValue(world, x, y, z));
            case USE_ID_DROPPED:
            case BED:
                md = world.getBlockMetadata(x, y, z);
                return makeItemStack(block.getItemDropped(md, world.rand, 0), 1, 0);
            case USE_GET_BLOCK_DROPPED:
                md = world.getBlockMetadata(x, y, z);
                ArrayList<ItemStack> drops = block.getDrops(world, x, y, z, md, 0);
                if (drops.isEmpty())
                {
                    return null;
                }
                return drops.get(0);
            case CLONE_MD:
                md = world.getBlockMetadata(x, y, z);
                return new ItemStack(block, 1, md);
            case STEM:
                if (block == Blocks.pumpkin_stem)
                {
                    return new ItemStack(Items.pumpkin_seeds);
                }
                else if (block == Blocks.melon_stem)
                {
                    return new ItemStack(Items.melon_seeds);
                }
                else
                {
                    return null;
                }
            case SLAB:
                md = world.getBlockMetadata(x, y, z);
                int dropped = block.quantityDropped(world.rand);
                return makeItemStack(block.getItemDropped(md, world.rand, 0), dropped, block.damageDropped(md));
            case CAKE:
                md = world.getBlockMetadata(x, y, z);
                return md == 0 ? new ItemStack(Items.cake) : null;
            case CROP:
                return new ItemStack(block.getItemDropped(0, world.rand, 0), 1, block.getDamageValue(world, x, y, z));
            case DOOR:
                md = world.getBlockMetadata(x, y, z);
                Item doorId = block.getItemDropped(md, world.rand, 0);
                if (doorId == null)
                {
                    return null;
                }
                return new ItemStack(doorId, 1, 0);
            case REDSTONE_ORE:
                return new ItemStack(Blocks.redstone_ore);
        }
    }
}
