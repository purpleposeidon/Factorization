package factorization.common;

import java.util.Arrays;

import net.minecraft.src.Block;
import net.minecraft.src.Entity;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import net.minecraft.src.World;
import net.minecraft.src.forge.ITextureProvider;

public class ItemWandOfCooling extends Item implements ITextureProvider {
    static int changeArray[] = new int[0xFF];
    final int max_change = 70;
    final int radius_tries = 50;
    final int max_radius = 5;
    int change_count = 0, noise_count = 0;

    public ItemWandOfCooling(int par1) {
        super(par1);
        setMaxStackSize(1);
        setFull3D();
        setMaxDamage(1024 * 15);

        Arrays.fill(changeArray, -1);
        remove(Block.fire);
    }

    @Override
    public String getItemName() {
        return "item.wandofcooling";
    }

    @Override
    public String getItemNameIS(ItemStack is) {
        return getItemName();
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
        if (Core.instance.isCannonical(world)) {
            world.setBlockWithNotify(x, y, z, id);
        }
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
        if (id == Core.lightair_id && md == Core.registry.lightair_block.fire_md) {
            newid = 0;
            cost = 2;
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
    public boolean onItemUse(ItemStack is, EntityPlayer player, World world, int x, int y, int z,
            int side) {
        if (!Core.instance.isCannonical(world)) {
            return true;
        }
        reset();
        safetyFirst(player, 0);
        safetyFirst(player, 1);
        changeArea(world, x, y, z, 1);
        int real_max_radius = max_radius;
        if (player.isSneaking()) {
            real_max_radius *= 2;
        }
        for (int r = 2; r < real_max_radius; r++) {
            changeRadius(world, x, y, z, r);
        }

        int damage = Math.min(max_change - change_count, 1);
        if (damage > 0 && Core.instance.isCannonical(player.worldObj)) {
            is.damageItem(damage, player);
        }

        return true;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack is, World world, EntityPlayer player) {
        onItemUse(is, player, world, (int) player.posX, (int) player.posY, (int) player.posZ, 0);
        return is;
    }

    @Override
    public String getTextureFile() {
        return Core.texture_file_item;
    }

    // @Override -- can't override due to the stupidly typical reason.
    public int getIconFromDamage(int par1) {
        return (2 * 16) + 2;
    }
}
