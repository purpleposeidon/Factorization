package factorization.coremod;

import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class BlockHelper {
    public static int pickBlockForServer(Block block, World world, int x, int y, int z) {
        int blockID = block.blockID;
        if (block instanceof BlockRedstoneLight) {
            return Block.redstoneLampIdle.blockID;
        }
        if (block instanceof BlockStem) {
            BlockStem these = (BlockStem) block;
            return these.fruitType == Block.pumpkin ? Item.pumpkinSeeds.itemID : (these.fruitType == Block.melon ? Item.melonSeeds.itemID : 0);
        }
        if (block instanceof BlockHalfSlab) {
            boolean singleSlab = blockID == Block.stoneSingleSlab.blockID || blockID == Block.woodSingleSlab.blockID;
            return singleSlab ? blockID : (blockID == Block.stoneDoubleSlab.blockID ? Block.stoneSingleSlab.      blockID : (blockID == Block.woodDoubleSlab.blockID ? Block.woodSingleSlab.blockID : Block.stoneSingleSlab.blockID));
        }
        if (block instanceof BlockCocoa) {
            return Item.dyePowder.itemID;
        }
        if (block instanceof BlockSign) {
            return Item.sign.itemID;
        }
        if (block instanceof BlockFlowerPot) {
            int md = world.getBlockMetadata(x, y, z);
            ItemStack itemstack = BlockFlowerPot.getPlantForMeta(md);
            return itemstack == null ? Item.flowerPot.itemID : itemstack.itemID;
        }
        if (block instanceof BlockDoor) {
            return block.blockMaterial == Material.iron ? Item.doorIron.itemID : Item.doorWood.itemID;
        }
        if (block instanceof BlockRedstoneWire) {
            return Item.redstone.itemID;
        }
        if (block instanceof BlockBed) {
            return Item.bed.itemID;
        }
        if (block instanceof BlockBrewingStand) {
            return Item.brewingStand.itemID;
        }
        if (block instanceof BlockReed) {
            return Item.reed.itemID;
        }
        if (block instanceof BlockEndPortal) {
            return 0;
        }
        if (block instanceof BlockTripWire) {
            return Item.silk.itemID;
        }
        if (block instanceof BlockCauldron) {
            return Item.cauldron.itemID;
        }
        if (block instanceof BlockRedstoneRepeater) {
            return Item.redstoneRepeater.itemID;
        }
        //BlockDragonEgg: No. Return yourself! (haxx)
        if (block instanceof BlockCake) { //Only return cake if it's a full cake
            int md = world.getBlockMetadata(x, y, z);
            if (md == 0) {
                return Item.cake.itemID;
            }
            return 0;
        }
        if (block instanceof BlockMushroomCap) {
            if (block == Block.mushroomCapBrown) {
                return Block.mushroomBrown.blockID;
            }
            return Block.mushroomRed.blockID;
        }
        if (block instanceof BlockComparator) {
            return Item.comparator.itemID;
        }
        if (block instanceof BlockNetherStalk) {
            return Item.netherStalkSeeds.itemID;
        }
        if (block instanceof BlockSkull) {
            return Item.skull.itemID; //NORELEASE: Bad!
        }
        if (block instanceof BlockRedstoneTorch) {
            return Block.torchRedstoneActive.blockID;
        }
        if (block instanceof BlockPistonExtension) {
            int l = world.getBlockMetadata(x, y, z);
            return (l & 8) != 0 ? Block.pistonStickyBase.blockID : Block.pistonBase.blockID;
        }
        if (block instanceof BlockFarmland) {
            return Block.dirt.blockID;
        }
        if (block instanceof BlockCrops) {
            if (block == Block.crops) {
                return Item.seeds.itemID;
            }
            return 0;
        }
        if (block instanceof BlockPistonMoving) {
            return 0;
        }
        if (block instanceof BlockPortal) {
            return 0;
        }
        if (block instanceof BlockFurnace) {
            return Block.furnaceIdle.blockID;
        }
        if (block instanceof BlockMobSpawner) {
            return 0;
        }
        
        return block.blockID;
    }
}
