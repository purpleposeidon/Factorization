package factorization.beauty;

import factorization.algos.FastBag;
import factorization.api.Coord;
import factorization.api.ICoordFunction;
import factorization.shared.Core;
import factorization.util.DataUtil;
import factorization.util.ItemUtil;
import factorization.util.PlayerUtil;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.NoiseGeneratorOctaves;

import java.util.HashSet;

public class EntityLeafBomb extends EntityThrowable {
    ItemStack stack, origStack;

    public EntityLeafBomb(World world) {
        super(world);
    }

    public EntityLeafBomb(World world, EntityLivingBase thrower, ItemStack stack) {
        super(world, thrower);
        this.origStack = stack;
    }

    public EntityLeafBomb(World world, double motionX, double motionY, double motionZ) {
        super(world, motionX, motionY, motionZ);
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        if (origStack != null) {
            tag.setTag("leafType", DataUtil.item2tag(origStack));
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        origStack = DataUtil.tag2item(tag.getCompoundTag("leafType"), null);
    }

    EntityPlayer shooter = null;

    protected void onImpact(MovingObjectPosition mop) {
        if (worldObj.isRemote) return;
        if (origStack == null) return;
        stack = Core.registry.leafBomb.getLeaves(origStack);

        Coord center = Coord.fromMop(worldObj, mop);
        if (mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            center.adjust(SpaceUtil.getOrientation(mop.sideHit));
        }
        shooter = PlayerUtil.makePlayer(center, "LeafBomb");
        int id = DataUtil.getId(stack);
        if (id == 0) id = DataUtil.getId(Blocks.leaves);
        particle = "blockcrack_" + id + "_" + stack.getItemDamage();
        boolean firstSpotSet = set(center);
        stack.stackSize += rand.nextInt(2);
        int fortune = 1 + EnchantmentHelper.getEnchantmentLevel(Enchantment.fortune.effectId, origStack);
        stack.stackSize *= Math.pow(3, fortune);
        if (firstSpotSet) {
            spawnGentleLeafBlob(center);
        } else {
            spamLeavesStupidly(center);
        }
        PlayerUtil.recycleFakePlayer(shooter);
        this.setDead();
    }

    private void spamLeavesStupidly(Coord center) {
        boolean firstSpotSet = false;
        double r = 1;
        double desparation = 0.25;
        int limit = 128;
        while (stack.stackSize > 0 && limit-- > 0) {
            int dx = (int) (rand.nextGaussian() * r);
            int dy = (int) (rand.nextGaussian() * r);
            int dz = (int) (rand.nextGaussian() * r);
            Coord hit = center.add(dx, dy, dz);
            if (!firstSpotSet) {
                firstSpotSet = set(hit);
                if (!firstSpotSet) r += desparation;
                continue;
            }
            boolean foundAdj = false;
            if (!isAdj(hit)) {
                r += desparation;
                continue;
            }
            set(hit);
        }
    }

    int dim;
    double[] noise;
    Coord start, end;
    private void initNoise(Coord at) {
        dim = 32;
        int half = dim / 2;
        start = at.add(-half, -half, -half);
        end = at.add(half, half, half);
        int octaves = 3;
        octaves -= Math.log10(stack.stackSize / 5);
        if (octaves < 1) octaves = 1;
        noise = new NoiseGeneratorOctaves(rand, octaves).generateNoiseOctaves(null,
                start.x, start.y, start.z,
                dim, dim, dim,
                end.x, end.y, end.z);
    }

    double sample(Coord at) {
        if (!at.inside(start, end)) return 0;
        int index = (at.x - start.x) + (at.y - start.y) * dim + (at.z - start.z) * dim * dim;
        if (index < 0 || index >= noise.length) return 0;
        return noise[index] + 1;
    }

    private void spawnGentleLeafBlob(Coord at) {
        initNoise(at);
        final HashSet<Coord> visited = new HashSet<Coord>(stack.stackSize);
        final FastBag<Coord> frontier = new FastBag<Coord>();
        frontier.add(at);
        visited.add(at);
        Coord min = at.copy(), max = at.copy();
        while (stack.stackSize > 0 && !frontier.isEmpty()) {
            Coord highest = null;
            double best = -1;
            for (Coord c : frontier) {
                double s = sample(c) / at.distanceSq(c);
                if (s > best) {
                    best = s;
                    highest = c;
                }
            }
            if (highest == null) break;
            set(highest);
            frontier.remove(highest);
            for (Coord n : highest.getNeighborsAdjacent()) {
                if (visited.add(n) && n.isReplacable()) {
                    frontier.add(n);
                }
            }
            Coord.sort(min, at);
            Coord.sort(at, max);
        }
        Coord.iterateCube(min, max, new ICoordFunction() {
            @Override
            public void handle(Coord here) {
                if (!here.isReplacable()) return;
                int accounted = 0;
                for (Coord n : here.getNeighborsAdjacent()) {
                    if (visited.contains(n)) {
                        accounted++;
                    } else if (n.isNormalCube()) {
                        accounted++;
                    } else if (n.getBlock().getMaterial() == Material.leaves) {
                        accounted++;
                    } else {
                        return;
                    }
                }
                if (accounted == 6) {
                    stack.stackSize++;
                    set(here);
                }
            }
        });
    }

    boolean isAdj(Coord at) {
        Block shouldBe = DataUtil.getBlock(stack);
        if (shouldBe != null) {
            for (Coord n : at.getNeighborsAdjacent()) {
                if (n.getBlock() == shouldBe) return true;
            }
        } else {
            for (Coord n : at.getNeighborsAdjacent()) {
                if (n.getBlock().getMaterial() == Material.leaves) return true;
            }
        }
        return false;
    }

    String particle = "blockcrack_19_0";

    boolean set(Coord at) {
        if (ItemUtil.normalize(stack) == null) return false;
        if (!at.isReplacable()) return false;
        if (stack.tryPlaceItemIntoWorld(shooter, at.w, at.x, at.y, at.z, 0, 0, 0, 0)) {
            stack.stackSize--;
        }
        ((WorldServer) worldObj).func_147487_a(particle, at.x + 0.5, at.y + 0.5, at.z + 0.5, 4, 0, 0, 0, 1);
        return true;
    }
}
