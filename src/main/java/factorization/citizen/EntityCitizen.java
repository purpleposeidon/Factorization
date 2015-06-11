package factorization.citizen;

import cpw.mods.fml.client.IModGuiFactory;
import factorization.api.Coord;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.Share;
import factorization.servo.ItemMatrixProgrammer;
import factorization.shared.Core;
import factorization.shared.EntityFz;
import factorization.util.InvUtil;
import factorization.util.ItemUtil;
import factorization.util.SpaceUtil;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.EnderTeleportEvent;

import java.io.IOException;
import java.util.Random;

import static factorization.citizen.EntityCitizen.ScriptKinds.*;

public class EntityCitizen extends EntityFz {
    public EntityCitizen(World w) {
        super(w);
    }

    private ItemStack held = new ItemStack(Blocks.stone, 0);
    private int ticks = 0;
    boolean spinning = false, visible = false;
    public transient int spin_ticks = 0, visible_ticks = 0;

    private static boolean isLmp(ItemStack is) {
        if (is == null) return false;
        return is.getItem() == Core.registry.logicMatrixProgrammer;
    }

    boolean takeLmp(EntityPlayer player) {
        if (isLmp(player.getHeldItem())) {
            held = player.getHeldItem();
            player.setCurrentItemOrArmor(0, null);
            return true;
        }
        InvUtil.FzInv inv = InvUtil.openInventory(player, true);
        ItemStack gotten = inv.pull(ItemUtil.makeWildcard(Core.registry.logicMatrixProgrammer), 1, false);
        if (gotten == null) return false;
        held = gotten;
        return true;
    }

    public static boolean spawnOn(EntityPlayerMP player) {
        if (player.worldObj.isRemote) return false;
        if (ItemMatrixProgrammer.isUserAuthenticated(player)) return false;
        EntityCitizen citizen = new EntityCitizen(player.worldObj);
        new Coord(player).setAsEntityLocation(citizen);
        if (!citizen.worldObj.spawnEntityInWorld(citizen)) return false;
        if (!citizen.takeLmp(player)) {
            citizen.setDead();
            return false;
        }
        citizen.mountEntity(player);
        return true;
    }

    @Override
    protected void putData(DataHelper data) throws IOException {
        held = data.as(Share.VISIBLE, "heldItem").putItemStack(held);
        ticks = data.as(Share.PRIVATE, "citizenTicks").putInt(ticks);
        spinning = data.as(Share.VISIBLE, "citizenSpin").putBoolean(spinning);
        visible = data.as(Share.VISIBLE, "visible").putBoolean(visible);
    }

    @Override
    protected void entityInit() {

    }

    private static class ScriptEvent {
        final int duration;
        final ScriptKinds kind;
        final String arg;

        private ScriptEvent(int duration, ScriptKinds kind, String arg) {
            this.duration = duration;
            this.kind = kind;
            this.arg = arg;
        }
    }

    private static ScriptEvent s(int duration, ScriptKinds kind, String arg) {
        return new ScriptEvent(duration, kind, arg);
    }

    private static ScriptEvent s(int duration, ScriptKinds kind) {
        script_duration += duration;
        return new ScriptEvent(duration, kind, "");
    }

    static enum ScriptKinds {
        potions, reveal, say, spin, unspin, clap, authenticate, leave
    }

    private static final ScriptEvent[] script = new ScriptEvent[] {
            s(0, potions),
            s(20, reveal),
            s(80, say, "intruder"),
            s(40+45, say, "gotcha"),
            s(40, say, "young"),
            s(40, say, "age"),
            s(30, spin),
            s(80, say, "lmp"),
            s(40, say, "garbage"),
            s(80, say, "cold"),
            s(80, say, "lift"),
            s(80, say, "mind"),
            s(0, unspin),
            s(0, clap),
            s(10, say, "okay"),
            s(0, authenticate),
            s(80, say, "authed"),
            s(80, say, "power"),
            s(80, say, "bedrock"),
            s(0, say, "bye"),
            s(0, leave)
    };
    private static int script_duration = 0;

    private void doEvent(ScriptEvent se, EntityPlayer player) {
        final String arg = se.arg;
        switch (se.kind) {
            case potions:
                int potion_duration = script_duration - ticks;
                pot(player, Potion.blindness, 1, 25);
                pot(player, Potion.confusion, 1, 25);
                pot(player, Potion.jump, 2, 60);
                pot(player, Potion.regeneration, 2, potion_duration);
                pot(player, Potion.resistance, 2, potion_duration);
                pot(player, Potion.fireResistance, 1, potion_duration);
                pot(player, Potion.waterBreathing, 1, potion_duration);
                pot(player, Potion.weakness, 10, potion_duration);
                pot(player, Potion.moveSlowdown, 10, potion_duration);
                pot(player, Potion.digSlowdown, 10, potion_duration);
                break;
            case reveal:
                break;
            case say:
                break;
            case spin:
                break;
            case unspin:
                break;
            case clap:
                break;
            case authenticate:
                break;
            case leave:
                break;
        }
    }

    private static void pot(EntityPlayer player, Potion potion, int power, int duration) {
        PotionEffect effect = new PotionEffect(potion.getId(), duration, power, true);
        effect.getCurativeItems().clear();
        player.addPotionEffect(effect);
    }

    @Override
    public void onEntityUpdate() {
        super.onEntityUpdate();

        if (!(ridingEntity instanceof EntityPlayer)) {
            setDead();
            return;
        }
        final EntityPlayer player = (EntityPlayer) ridingEntity;
        int acc = 0;
        for (ScriptEvent s : script) {
            if (ticks == acc) {
                doEvent(s, player);
            }
            acc += s.duration;
        }
        ticks++;
        if (ticks % 5 == 0) {
            remove_nearby_hostiles();
        }
        if (ticks > acc * 2) setDead();
    }

    private void remove_nearby_hostiles() {
        int r = 12;
        AxisAlignedBB range = getBoundingBox().expand(r, r, r);
        Vec3 me = SpaceUtil.fromEntPos(this);
        for (Entity ent : (Iterable<Entity>) worldObj.selectEntitiesWithinAABB(null, range, IMob.mobSelector)) {
            Vec3 you = SpaceUtil.fromEntPos(ent);
            Vec3 delta = SpaceUtil.subtract(you, me).normalize();
            SpaceUtil.incrScale(delta, r * 2 + Math.abs(rand.nextGaussian()));
            Vec3 target = SpaceUtil.add(you, delta);
            enderport(ent, target.xCoord, target.yCoord, target.zCoord);
        }
    }

    protected static boolean enderport(Entity ent, double x, double y, double z) {
        double d3 = ent.posX;
        double d4 = ent.posY;
        double d5 = ent.posZ;
        ent.posX = x;
        ent.posY = y;
        ent.posZ = z;
        boolean flag = false;
        int i = MathHelper.floor_double(ent.posX);
        int j = MathHelper.floor_double(ent.posY);
        int k = MathHelper.floor_double(ent.posZ);

        if (ent.worldObj.blockExists(i, j, k)) {
            boolean flag1 = false;

            while (!flag1 && j > 0) {
                Block block = ent.worldObj.getBlock(i, j - 1, k);

                if (block.getMaterial().blocksMovement()) {
                    flag1 = true;
                } else {
                    --ent.posY;
                    --j;
                }
            }

            if (flag1) {
                ent.setPosition(ent.posX, ent.posY, ent.posZ);

                if (ent.worldObj.getCollidingBoundingBoxes(ent, ent.boundingBox).isEmpty() && !ent.worldObj.isAnyLiquid(ent.boundingBox)) {
                    flag = true;
                }
            }
        }

        if (!flag) {
            ent.setPosition(d3, d4, d5);
            return false;
        } else {
            short short1 = 128;
            Random rand = ent.worldObj.rand;

            for (int l = 0; l < short1; ++l) {
                double d6 = (double) l / ((double) short1 - 1.0D);
                float f = (rand.nextFloat() - 0.5F) * 0.2F;
                float f1 = (rand.nextFloat() - 0.5F) * 0.2F;
                float f2 = (rand.nextFloat() - 0.5F) * 0.2F;
                double d7 = d3 + (ent.posX - d3) * d6 + (rand.nextDouble() - 0.5D) * (double) ent.width * 2.0D;
                double d8 = d4 + (ent.posY - d4) * d6 + rand.nextDouble() * (double) ent.height;
                double d9 = d5 + (ent.posZ - d5) * d6 + (rand.nextDouble() - 0.5D) * (double) ent.width * 2.0D;
                ent.worldObj.spawnParticle("portal", d7, d8, d9, (double) f, (double) f1, (double) f2);
            }

            ent.worldObj.playSoundEffect(d3, d4, d5, "mob.endermen.portal", 1.0F, 1.0F);
            ent.playSound("mob.endermen.portal", 1.0F, 1.0F);
            return true;
        }
    }
}
