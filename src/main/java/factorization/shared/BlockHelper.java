package factorization.shared;

import net.minecraft.block.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

import java.util.List;

import static factorization.shared.BlockHelper.BlockStyle.*;

public class BlockHelper {
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

    static BlockStyle getBlockStyle(Block block) {
        return classifyBlock(block);
    }

    private static BlockStyle classifyBlock(Block block) {
        // These blocks don't have a good way of extracting the item,
        // so no instanceof.
        if (block == Blocks.cake) {
            return CAKE;
        }
        if (block == Blocks.redstone_ore || block == Blocks.lit_redstone_ore) {
            return REDSTONE_ORE;
        }
        if (block == Blocks.piston_extension) {
            return PISTON_EXTENSION;
        }
        if (block == Blocks.melon_stem || block == Blocks.pumpkin_stem) {
            return STEM;
        }
        if (block instanceof BlockSign || block instanceof BlockFlowerPot || block instanceof BlockRedstoneWire || block instanceof BlockBrewingStand
                || block instanceof BlockReed || block instanceof BlockTripWire || block instanceof BlockCauldron || block instanceof BlockRedstoneRepeater
                || block instanceof BlockRedstoneComparator || block instanceof BlockRedstoneTorch || block instanceof BlockFarmland || block instanceof BlockFurnace
                || block instanceof BlockHugeMushroom || block instanceof BlockRedstoneLight) {
            return USE_ID_DROPPED;
        }
        if (block instanceof BlockCocoa || block instanceof BlockNetherWart || block instanceof BlockSkull) {
            return USE_GET_BLOCK_DROPPED;
        }
        if (block instanceof BlockPistonMoving || block instanceof BlockPortal || block instanceof BlockEndPortal || block instanceof BlockSilverfish
                || block instanceof BlockMobSpawner) {
            return NOTHING;
        }
        if (block instanceof BlockOre) {
            return CLONE_MD;
        }
        // Special blocks
        if (block instanceof BlockSlab) {
            return SLAB;
        }
        if (block instanceof BlockCrops) {
            return CROP;
        }
        if (block instanceof BlockBed) {
            return BED;
        }
        if (block instanceof BlockDoor) {
            return DOOR;
        }
        return USE_GET_DAMAGE_VALUE;
    }

    private static ItemStack makeItemStack(Item itemId, int stackSize, int damage) {
        if (itemId == null) {
            return null;
        }
        return new ItemStack(itemId, stackSize, damage);
    }

    public static ItemStack getPlacingItem(Block block, MovingObjectPosition target, World world, BlockPos pos) {
        IBlockState bs = world.getBlockState(pos);
        switch (classifyBlock(block)) {
            default:
            case UNDECIDED:
            case NOTHING:
            case PISTON_EXTENSION:
                return null;
            case USE_GET_DAMAGE_VALUE:
                return new ItemStack(block, 1, block.getDamageValue(world, pos));
            case USE_ID_DROPPED:
            case BED:
                return makeItemStack(block.getItemDropped(bs, world.rand, 0), 1, 0);
            case USE_GET_BLOCK_DROPPED:
                List<ItemStack> drops = block.getDrops(world, pos, bs, 0);
                if (drops.isEmpty()) {
                    return null;
                }
                return drops.get(0);
            case CLONE_MD:
                return new ItemStack(block, 1, block.getMetaFromState(bs));
            case STEM:
                if (block == Blocks.pumpkin_stem) {
                    return new ItemStack(Items.pumpkin_seeds);
                } else if (block == Blocks.melon_stem) {
                    return new ItemStack(Items.melon_seeds);
                } else {
                    return null;
                }
            case SLAB:
                int dropped = block.quantityDropped(world.rand);
                return makeItemStack(block.getItemDropped(bs, world.rand, 0), dropped, block.damageDropped(bs));
            case CAKE:
                int bites = bs.getValue(BlockCake.BITES);
                return bites == 0 ? new ItemStack(Items.cake) : null;
            case CROP:
                return new ItemStack(block.getItemDropped(bs, world.rand, 0), 1, block.getDamageValue(world, pos));
            case DOOR:
                Item doorId = block.getItemDropped(bs, world.rand, 0);
                if (doorId == null) {
                    return null;
                }
                return new ItemStack(doorId, 1, 0);
            case REDSTONE_ORE:
                return new ItemStack(Blocks.redstone_ore);
        }
    }
}
