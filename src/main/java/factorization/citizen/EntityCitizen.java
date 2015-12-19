package factorization.citizen;

import java.io.IOException;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.IMob;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import factorization.api.Coord;
import factorization.api.FzOrientation;
import factorization.api.Quaternion;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.Share;
import factorization.fzds.interfaces.Interpolation;
import factorization.notify.Notice;
import factorization.servo.ItemMatrixProgrammer;
import factorization.shared.Core;
import factorization.shared.EntityFz;
import factorization.shared.EntityReference;
import factorization.util.InvUtil;
import factorization.util.ItemUtil;
import factorization.util.LangUtil;
import factorization.util.SpaceUtil;
import static factorization.citizen.EntityCitizen.ScriptKinds.authenticate;
import static factorization.citizen.EntityCitizen.ScriptKinds.give;
import static factorization.citizen.EntityCitizen.ScriptKinds.leave;
import static factorization.citizen.EntityCitizen.ScriptKinds.potions;
import static factorization.citizen.EntityCitizen.ScriptKinds.reveal;
import static factorization.citizen.EntityCitizen.ScriptKinds.say;
import static factorization.citizen.EntityCitizen.ScriptKinds.spin;
import static factorization.citizen.EntityCitizen.ScriptKinds.unspin;
import static factorization.citizen.EntityCitizen.ScriptKinds.wait;

public class EntityCitizen extends EntityFz {

    public ItemStack held = new ItemStack(Blocks.stone, 0);
    private int ticks = 0;
    boolean spinning = false, visible = false, player_lost_visibility_state = false;
    public transient int spinning_ticks = 0;

    public static final float TICKS_PER_SPIN = 90;
    private static final Quaternion NORMAL = new Quaternion(),
            POINT1 = Quaternion.fromOrientation(FzOrientation.FACE_UP_POINT_EAST),
            POINT2 = Quaternion.fromOrientation(FzOrientation.FACE_NORTH_POINT_DOWN).multiply(Quaternion.getRotationQuaternionRadians(Math.PI * 9, 0, 1, 0)),
            POINT3 = Quaternion.fromOrientation(FzOrientation.FACE_UP_POINT_SOUTH).multiply(Quaternion.getRotationQuaternionRadians(Math.PI * -9, 0, 0, -1));

    Quaternion rotation_start = NORMAL, rotation_target = NORMAL;

    final EntityReference<EntityPlayer> playerRef = new EntityReference<EntityPlayer>();

    public EntityCitizen(World w) {
        super(w);
        setSize(1, 1);
        playerRef.setWorld(w);
    }

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
        citizen.playerRef.trackEntity(player);
        return true;
    }

    @Override
    protected void putData(DataHelper data) throws IOException {
        held = data.as(Share.VISIBLE, "heldItem").putItemStack(held);
        ticks = data.as(Share.PRIVATE, "citizenTicks").putInt(ticks);
        spinning = data.as(Share.VISIBLE, "citizenSpin").putBoolean(spinning);
        visible = data.as(Share.VISIBLE, "visible").putBoolean(visible);
        player_lost_visibility_state = data.as(Share.PRIVATE, "playerLostVis").putBoolean(player_lost_visibility_state);
        data.as(Share.PRIVATE, "playerRef").putIDS(playerRef);
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
        script_duration += duration;
        return new ScriptEvent(duration, kind, arg);
    }

    private static ScriptEvent s(int duration, ScriptKinds kind) {
        script_duration += duration;
        return new ScriptEvent(duration, kind, "");
    }

    static enum ScriptKinds {
        potions, reveal, say, spin, unspin, authenticate, give, leave, wait, restart
    }

    public static final int WAIT_TIME = 20 * 5;
    public static final int SURRENDER_TIME = 20 * 2;
    public static final int EXPLODE_TIME = WAIT_TIME - SURRENDER_TIME;

    private static int script_duration = 0;
    private static final ScriptEvent[] script = new ScriptEvent[] {
            s(WAIT_TIME + 40, wait), // NOTE: Initial wait time is coordinated with the colossus
            s(20, potions),
            s(0, reveal),
            s(80, say, "intruder"),
            s(0, spin),
            s(40, say, "gotcha"),
            s(45, wait),
            s(40, say, "young"),
            s(60, say, "age"),
            s(80, say, "lmp"),
            s(40, say, "garbage"),
            s(0, unspin),
            s(80, say, "cold"),
            s(80, say, "lift"),
            s(80, say, "mind"),
            s(20, wait),
            s(10, say, "okay"),
            s(40, authenticate),
            s(100, say, "authed"),
            s(0, give),
            s(80, say, "bedrock"),
            s(80, say, "power"),
            s(80, say, "bye"),
            s(0, leave)
    };

    String current_text = null;
    int text_time = 0;
    int max_text_time = 0;
    int text_msg_index = 0;

    private void doEvent(ScriptEvent se, EntityPlayer player) {
        final String arg = se.arg;
        //NORELEASE.println(se.kind, arg);
        current_text = null;
        switch (se.kind) {
            case restart:
                ticks = 0;
                break;
            case wait:
                // Don't need to do anything
                break;
            case potions:
                int potion_duration = script_duration - ticks;
                pot(player, Potion.blindness, 10, 60);
                pot(player, Potion.confusion, 1, 60);
                pot(player, Potion.jump, 2, 60);
                pot(player, Potion.regeneration, 2, potion_duration);
                pot(player, Potion.resistance, 2, potion_duration);
                pot(player, Potion.fireResistance, 1, potion_duration);
                pot(player, Potion.waterBreathing, 1, potion_duration);
                if (!Core.dev_environ) {
                    pot(player, Potion.weakness, 64, potion_duration);
                    pot(player, Potion.moveSlowdown, 64, potion_duration);
                    pot(player, Potion.digSlowdown, 64, potion_duration);
                }
                break;
            case reveal:
                visible = true;
                syncData();
                break;
            case say:
                current_text = LangUtil.translateExact("fz.ent.citizen.say." + arg);
                text_time = 0;
                max_text_time = se.duration;
                text_msg_index = ticks;
                break;
            case spin:
                spinning = true;
                syncData();
                break;
            case unspin:
                spinning = false;
                syncData();
                break;
            case authenticate:
                if (held != null) {
                    Core.registry.logicMatrixProgrammer.setAuthenticated(held);
                }
                // Play a cool upgrade noise
                break;
            case give:
            {
                ItemStack old = player.getHeldItem();
                player.setCurrentItemOrArmor(0, held);
                held = null;
                if (old != null) {
                    InvUtil.FzInv inv = InvUtil.openInventory(player, true);
                    old = inv.push(old);
                    inv.onInvChanged();
                    if (old != null) {
                        new Coord(this).spawnItem(held).onCollideWithPlayer(player);
                    }
                }
                syncData();
                break;
            }
            case leave:
                setDead();
                break;
        }
    }

    private static void pot(EntityPlayer player, Potion potion, int power, int duration) {
        boolean ambient = potion != Potion.blindness;
        PotionEffect effect = new PotionEffect(potion.getId(), duration, power, ambient, ambient);
        effect.getCurativeItems().clear();
        player.addPotionEffect(effect);
    }

    void lockdownClient() {
        if (isDead) return;
        final EntityPlayer player = (EntityPlayer) ridingEntity;
        player.motionX = player.motionZ = 0;
        if (player.motionY > 0) player.motionY = 0;
        player.setJumping(false);
        if (player.rotationPitch > -80) {
            int n = 20;
            player.rotationPitch = (player.rotationPitch * n + -90) / (n + 1);
        }
        // This is fine, right? Right?
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.currentScreen != null) {
            if (!mc.currentScreen.doesGuiPauseGame()) {
                player.closeScreen();
            }
        }
        final GameSettings settings = mc.gameSettings;
        settings.smoothCamera = true;
        settings.thirdPersonView = 0;
        cinema_player = player;
    }

    EntityPlayer cinema_player = null;

    void decinema() {
        final Minecraft mc = Minecraft.getMinecraft();
        final GameSettings settings = mc.gameSettings;
        if (mc.thePlayer == cinema_player) {
            cinema_player = null;
            settings.smoothCamera = false;
        }
    }

    @Override
    public void setDead() {
        super.setDead();
        if (worldObj.isRemote) {
            decinema();
        }
    }

    @Override
    public void onEntityUpdate() {
        super.onEntityUpdate();
        if (worldObj.isRemote) {
            if (!(ridingEntity instanceof EntityPlayer)) {
                return;
            }

            lockdownClient();

            if (spinning_ticks++ > TICKS_PER_SPIN) {
                rotation_start = rotation_target;
                if (!spinning) {
                    rotation_target = NORMAL;
                } if (rotation_target == NORMAL) {
                    rotation_target = POINT1;
                } else if (rotation_target == POINT1) {
                    rotation_target = POINT2;
                } else if (rotation_target == POINT2) {
                    rotation_target = POINT3;
                } else if (rotation_target == POINT3) {
                    rotation_target = POINT1;
                }
                spinning_ticks = 0;
            }
            return;
        }

        if (!(ridingEntity instanceof EntityPlayer)) {
            if (!playerRef.trackingEntity()) {
                setDead();
                return;
            }
            if (!playerRef.entityFound()) {
                EntityPlayer player = playerRef.getEntity();
                if (player == null) {
                    if (visible) {
                        player_lost_visibility_state = true;
                        visible = false;
                        syncData();
                    }
                    return;
                }
                if (player.worldObj != worldObj || !player.isEntityAlive()) {
                    // You're trying far too hard to run away. :P
                    setDead();
                    return;
                }
                this.mountEntity(player);
                visible = player_lost_visibility_state;
                syncData();
            }
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
        if (current_text != null) {
            text_time++;
            float f = text_time / (float) max_text_time;
            double interp = Interpolation.SMOOTHER.scale(f);
            final int clen = current_text.length();
            int boundary = (int) (interp * clen);

            if (boundary > clen) boundary = clen;

            String head = current_text.substring(0, boundary);
            String tail = current_text.substring(boundary, clen);

            final ChatComponentText headText = new ChatComponentText(head);
            final ChatStyle tailFormat = new ChatStyle().setObfuscated(true).setColor(EnumChatFormatting.AQUA);
            final IChatComponent tailText = new ChatComponentText(tail).setChatStyle(tailFormat);
            final IChatComponent toSend = new ChatComponentTranslation("fz.ent.citizen.name").appendSibling(headText).appendSibling(tailText);
            Notice.chat(player, 90 + text_msg_index, toSend);
            if (text_time == max_text_time) {
                current_text = null;
            }
        }
        ticks++;
        if (ticks % 5 == 0) {
            remove_nearby_hostiles();
        }
        if (ticks > acc * 2) setDead();
    }

    private void remove_nearby_hostiles() {
        int r = 12;
        AxisAlignedBB range = getEntityBoundingBox().expand(r, r, r);
        Vec3 me = SpaceUtil.fromEntPos(this);
        for (Entity ent : worldObj.getEntitiesInAABBexcluding(this, range, IMob.mobSelector)) {
            Vec3 you = SpaceUtil.fromEntPos(ent);
            Vec3 delta = SpaceUtil.subtract(you, me).normalize();
            delta = SpaceUtil.scale(delta, r * 2 + Math.abs(rand.nextGaussian()));
            Vec3 target = you.add(delta);
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

        BlockPos pos = new BlockPos(i, j, k);
        if (ent.worldObj.isBlockLoaded(pos)) {
            boolean flag1 = false;

            while (!flag1 && j > 0) {
                Block block = ent.worldObj.getBlockState(pos.down()).getBlock();

                if (block.getMaterial().blocksMovement()) {
                    flag1 = true;
                } else {
                    --ent.posY;
                    --j;
                }
            }

            if (flag1) {
                ent.setPosition(ent.posX, ent.posY, ent.posZ);

                if (ent.worldObj.getCollidingBoundingBoxes(ent, ent.getCollisionBoundingBox()).isEmpty() && !ent.worldObj.isAnyLiquid(ent.getCollisionBoundingBox())) {
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
                ent.worldObj.spawnParticle(EnumParticleTypes.PORTAL, d7, d8, d9, (double) f, (double) f1, (double) f2);
            }

            ent.worldObj.playSoundEffect(d3, d4, d5, "mob.endermen.portal", 1.0F, 1.0F);
            ent.playSound("mob.endermen.portal", 1.0F, 1.0F);
            return true;
        }
    }
}
