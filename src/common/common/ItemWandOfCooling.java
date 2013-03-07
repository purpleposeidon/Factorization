package factorization.common;

import java.util.Arrays;
import java.util.List;

import factorization.api.Coord;
import factorization.common.Core.TabType;

import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class ItemWandOfCooling extends Item {
    static int changeArray[] = new int[Block.blocksList.length];
    final int max_change = 70;
    final int radius_tries = 50;
    final int max_radius = 3;
    int change_count = 0, noise_count = 0;

    public ItemWandOfCooling(int par1) {
        super(par1);
        setMaxStackSize(1);
        setFull3D();
        setMaxDamage(1024 * 13);

        Arrays.fill(changeArray, -1);
        remove(Block.fire);
        Core.tab(this, TabType.TOOLS);
        setUnlocalizedName("factorization.tool.wand_of_cooling");
    }

    public void cool(Block src, Block dest) {
        changeArray[src.blockID] = dest.blockID;
    }

    public void remove(Block src) {
        changeArray[src.blockID] = 0;
    }

    int getRadius(ItemStack is) {
        return 4;
    }

    void setBlock(World world, int x, int y, int z, int id) {
        world.setBlockAndMetadataWithNotify(x, y, z, id, 0, Coord.NOTIFY_NEIGHBORS);
        soundCool(world, x, y, z);
    }

    int makeSafe(World world, int x, int y, int z) {
        // destroy lava. This is used in the blocks the player's standing in
        int id = world.getBlockId(x, y, z);
        if (id == Block.lavaMoving.blockID || id == Block.lavaStill.blockID) {
            int md = world.getBlockMetadata(x, y, z);
            if (md == 0) {
                setBlock(world, x, y, z, 0);
                return 41;
            }
            setBlock(world, x, y, z, 0);
            return 10;
        }
        if (id == Block.fire.blockID) {
            setBlock(world, x, y, z, 0);
            return 10;
        }
        return 0;
    }

    void safetyFirst(Entity player, int d) {
        int hit = 0;
        for (int x = (int) (player.posX - d); x <= player.posX + d; x++) {
            for (int y = (int) (player.posY - d); y <= player.posY + d; y++) {
                for (int z = (int) (player.posZ - d); z <= player.posZ + d; z++) {
                    change_count -= makeSafe(player.worldObj, x, y, z);
                    if (change_count <= 0) {
                        return;
                    }
                }
            }
        }
        if (player.isBurning()) {
            change_count -= 20;
            player.extinguish();
        }
        return;
    }

    int transformBlock(World world, int x, int y, int z) {
        int id = world.getBlockId(x, y, z);
        int md = world.getBlockMetadata(x, y, z);
        if (id > 0xFF) {
            return 0;
        }
        if (id == 0) {
            return 0;
        }
        int newid = changeArray[id];
        boolean water = id == Block.waterMoving.blockID || id == Block.waterStill.blockID;
        boolean lava = id == Block.lavaMoving.blockID || id == Block.lavaStill.blockID;
        int cost = 10;
        if (water || lava) {
            if (md == 0) {
                // a source block. Freeze it.
                if (water) {
                    newid = Block.ice.blockID;
                }
                if (lava) {
                    newid = Block.obsidian.blockID;
                    cost = 15;
                }
            } else {
                // a flowing block. Destroy it.
                newid = 0;
                cost = 1; // make destroying derpfluid real cheap
            }
        }
        if (id == Core.registry.lightair_block.blockID && md == Core.registry.lightair_block.fire_md) {
            newid = 0;
            cost = 1;
            world.removeBlockTileEntity(x, y, z);
        }
        if (newid == -1) {
            return 0;
        }
        setBlock(world, x, y, z, newid);
        return cost;
    }

    void changeRadius(World world, int x, int y, int z, int r) {
        if (change_count <= 0) {
            return;
        }
        int tries = radius_tries;
        do {
            if (change_count < 0) {
                break;
            }
            int dx = (int) (itemRand.nextGaussian() * r);
            int dy = (int) (itemRand.nextGaussian() * r);
            int dz = (int) (itemRand.nextGaussian() * r);
            int delta = transformBlock(world, x + dx, y + dy, z + dz);
            if (delta != 0) {
                change_count -= delta;
            } else {
                tries -= 1;
            }
        } while (tries > 0);
    }

    void changeArea(World world, int x, int y, int z, int r) {
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (change_count < 0) {
                        return;
                    }
                    change_count -= transformBlock(world, x + dx, y + dy, z + dz);
                }
            }
        }
    }

    void reset() {
        change_count = max_change;
        noise_count = 0;
    }

    void soundCool(World world, int x, int y, int z) {
        // SO SUGOI
        noise_count += 1;
        if (noise_count > 1) {
            return;
        }
        Sound.wandCool.playAt(world, x, y, z);
    }
    
    @Override
    public boolean onItemUse(ItemStack par1ItemStack,
            EntityPlayer par2EntityPlayer, World par3World, int par4, int par5,
            int par6, int par7, float par8, float par9, float par10) {
        return tryPlaceIntoWorld(par1ItemStack, par2EntityPlayer, par3World, par4, par5,
                par6, par7, par8, par9, par10);
    }

    public boolean tryPlaceIntoWorld(ItemStack is, EntityPlayer player, World w, int x, int y,
            int z, int side, float vecx, float vecy, float vecz) {
        if (w.isRemote) {
            return true;
        }
        reset();
        safetyFirst(player, 0);
        safetyFirst(player, 1);
        changeArea(w, x, y, z, 1);
        int real_max_radius = max_radius;
        if (player.isSneaking()) {
            real_max_radius++;
        }
        for (int r = 2; r < real_max_radius; r++) {
            for (int i = 0; i < 3; i++) {
                changeRadius(w, x, y, z, r);
            }
        }

        int damage = Math.min(max_change - change_count, 1);
        if (damage > 0) {
            is.damageItem(damage, player);
        }

        return true;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack is, World world, EntityPlayer player) {
        tryPlaceIntoWorld(is, player, world, (int) player.posX, (int) player.posY, (int) player.posZ, 0, 0, 0, 0);
        return is;
    }

    @Override
    public void addInformation(ItemStack is, EntityPlayer player, List infoList, boolean verbose) {
        Core.brand(infoList);
    }
}
