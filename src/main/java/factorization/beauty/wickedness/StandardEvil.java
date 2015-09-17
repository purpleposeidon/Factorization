package factorization.beauty.wickedness;

import factorization.api.Coord;
import factorization.beauty.wickedness.api.EvilBit;
import factorization.beauty.wickedness.api.EvilRegistry;
import factorization.util.EvilUtil;
import factorization.beauty.wickedness.api.IEvil;
import net.minecraft.entity.monster.EntityCaveSpider;
import net.minecraft.entity.monster.EntityGhast;
import net.minecraft.entity.monster.EntitySilverfish;
import net.minecraft.entity.passive.EntityBat;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import java.util.Random;

public class StandardEvil {
    static boolean loaded = false;
    public static void load() {
        if (loaded) return;
        loaded = true;
        EvilRegistry.register(new IEvil() {
            @Override
            public String getId() {
                return "neptunepink:annabel-lee";
            }

            @Override
            public int getMainColor() {
                return 0x000000;
            }

            @Override
            public int getSecondColor() {
                return 0xFF0000;
            }

            @Override
            public void setup(TileEntity egg, EvilBit evilBit) {
                evilBit.tickSpeed = 4;
                evilBit.cost = 4;
            }

            @Override
            public boolean act(TileEntity egg, EvilBit evilBit, EntityPlayer nearbyPlayer, Random rand) {
                EntityGhast annabel = new EntityGhast(egg.getWorldObj());
                annabel.setCustomNameTag("Annabel Lee");
                EvilUtil.givePotion(annabel, Potion.invisibility, 1, false);
                EvilUtil.givePotion(annabel, Potion.wither, 2, false);
                return EvilUtil.spawn(egg, rand, evilBit.playerRange, annabel);
            }
        });
        EvilRegistry.register(EvilRegistry.ON_BREAK, new IEvil() {
            @Override
            public String getId() {
                return "neptunepink:spider-egg";
            }

            @Override
            public int getMainColor() {
                return 0x000000;
            }

            @Override
            public int getSecondColor() {
                return 0xFF2020;
            }

            @Override
            public void setup(TileEntity egg, EvilBit evilBit) {
                evilBit.cost = 8;
            }

            @Override
            public boolean act(TileEntity egg, EvilBit evilBit, EntityPlayer nearbyPlayer, Random rand) {
                int count = 4 + rand.nextInt(2);
                for (int N = 0; N < count; N++) {
                    final World world = egg.getWorldObj();
                    EntityCaveSpider spider = new EntityCaveSpider(world);
                    EvilUtil.positionEntity(egg, rand, 0, spider);
                    EvilUtil.givePotion(spider, Potion.jump, 2, true);
                    EvilUtil.givePotion(spider, Potion.regeneration, 1, false);
                    if (rand.nextInt(10) == 0) {
                        EvilUtil.givePotion(spider, Potion.damageBoost, 1, false);
                    }
                    /*spider.motionX = (int) (rand.nextGaussian() * 100) / 1000F;
                    spider.motionY = Math.abs((int) (rand.nextGaussian() * 100) / 1000F);
                    spider.motionZ = (int) (rand.nextGaussian() * 100) / 1000F;*/
                    EvilUtil.forceSpawn(egg, spider);
                }
                return true;
            }
        });
        EvilRegistry.register(new IEvil() {
            @Override
            public String getId() {
                return "neptunepink:flying-fish";
            }

            @Override
            public int getMainColor() {
                return 0x000000;
            }

            @Override
            public int getSecondColor() {
                return 0x808080;
            }

            @Override
            public void setup(TileEntity egg, EvilBit evilBit) {

            }

            @Override
            public boolean act(TileEntity egg, EvilBit evilBit, EntityPlayer nearbyPlayer, Random rand) {
                final World w = egg.getWorldObj();
                EntityBat bat = new EntityBat(w);
                EvilUtil.positionEntity(egg, rand, evilBit.playerRange, bat);
                if (!EvilUtil.canSpawn(bat)) return false;
                EntitySilverfish feesh = new EntitySilverfish(w);
                feesh.mountEntity(bat);
                w.spawnEntityInWorld(bat);
                w.spawnEntityInWorld(feesh);
                return true;
            }
        });
    }
}
