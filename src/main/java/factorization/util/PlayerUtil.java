package factorization.util;

import com.mojang.authlib.GameProfile;
import factorization.api.Coord;
import factorization.shared.Core;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.command.ICommandSender;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.rcon.RConConsoleSource;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.stats.StatisticsFile;
import net.minecraft.util.*;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;
import java.util.WeakHashMap;

public final class PlayerUtil {
    private static final UUID FZ_UUID = UUID.fromString("f979c78a-f80d-46b1-9c49-0121ea8850e6");
    private static HashMap<String, WeakHashMap<World, FzFakePlayer>> usedPlayerCache = new HashMap();

    public static EntityPlayer makePlayer(final Coord where, String use) {
        WeakHashMap<World, FzFakePlayer> fakePlayerCache = usedPlayerCache.get(use);
        if (fakePlayerCache == null) {
            fakePlayerCache = new WeakHashMap<World, FzFakePlayer>();
            usedPlayerCache.put(use, fakePlayerCache);
        }
        FzFakePlayer found = fakePlayerCache.get(where.w);
        if (found == null) {
            if (where.w instanceof WorldServer) {
                found = new FzFakePlayer((WorldServer) where.w, "[FZ." + use + "]", where);
            } else {
                throw new IllegalArgumentException("Can't construct fake players on the client");
            }
            fakePlayerCache.put(where.w, found);
        }
        found.setHealth(20);
        found.isDead = false;
        found.worldObj = where.w;
        found.where = where;
        where.setAsEntityLocation(found);
        Arrays.fill(found.inventory.armorInventory, null);
        Arrays.fill(found.inventory.mainInventory, null);
        return found;
    }

    public static void recycleFakePlayer(EntityPlayer player) {
        //player.worldObj = null; causes NPE with Thaumcraft vampire bats. Stored behind a weakref anyways.
        player.isDead = true; // Avoid mob retribution
        player.setHealth(0);
        if (player instanceof FzFakePlayer) {
            FzFakePlayer p = (FzFakePlayer) player;
            p.where = null;
        }
    }

    public static class PlayerRecycler {
        @SubscribeEvent
        public void clearOldPlayers(WorldEvent.Unload event) {
            for (WeakHashMap<World, FzFakePlayer> map : usedPlayerCache.values()) {
                map.remove(event.world);
            }
        }
    }

    static {
        Core.loadBus(new PlayerRecycler());
    }

    private static GameProfile makeProfile(String name) {
        if (StringUtils.isNullOrEmpty(name)) return new GameProfile(FZ_UUID, "[FZ]");
        return new GameProfile(FZ_UUID, "[FZ:" + name + "]");
    }

    public static EntityPlayer fakeplayerToNull(EntityPlayer player) {
        if (player instanceof EntityPlayerMP) {
            if (player instanceof FakePlayer) {
                return null;
            }
            return player;
        }
        return null;
    }

    public static boolean isPlayerOpped(EntityPlayer player) {
        player = fakeplayerToNull(player);
        if (player == null) return false;
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null) return false;
        return server.getConfigurationManager().canSendCommands(player.getGameProfile());
    }

    public static boolean isCommandSenderOpped(ICommandSender player) {
        if (player instanceof EntityPlayer) {
            return isPlayerOpped((EntityPlayer) player);
        }
        return player instanceof MinecraftServer || player instanceof RConConsoleSource;
    }

    public static boolean isPlayerCreative(EntityPlayer player) {
        return player.capabilities.isCreativeMode;
    }

    public static StatisticsFile getStatsFile(EntityPlayer player) {
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null) return null;
        ServerConfigurationManager cm = server.getConfigurationManager();
        return cm.getPlayerStatsFile(player);
    }

    public static int getPuntStrengthOrWeakness(EntityPlayer player) {
        if (player == null) return 1;
        //strength * knocback
        int strength = 0;
        PotionEffect p_str = player.getActivePotionEffect(Potion.damageBoost);
        PotionEffect p_wea = player.getActivePotionEffect(Potion.weakness);
        if (p_str != null) {
            strength += p_str.getAmplifier() + 1;
        }
        if (p_wea != null) {
            strength -= p_wea.getAmplifier() + 1;
        }
        int knockback = EnchantmentHelper.getKnockbackModifier(player);
        return strength * knockback;
    }

    public static int getPuntStrengthInt(EntityPlayer player) {
        int str = getPuntStrengthOrWeakness(player);
        return Math.min(1, str);
    }

    public static double getPuntStrengthMultiplier(EntityPlayer player) {
        int str = getPuntStrengthOrWeakness(player);
        if (str == 0) return 1;
        if (str < 1) return 1.0 / -str;
        return str;
    }

    private static class FakeNetManager extends NetworkManager {
        public FakeNetManager() {
            super(EnumPacketDirection.CLIENTBOUND);
            this.channel = new EmbeddedChannel(new ChannelHandler() {
                @Override public void handlerAdded(ChannelHandlerContext ctx) throws Exception { }
                @Override public void handlerRemoved(ChannelHandlerContext ctx) throws Exception { }
                @Override @Deprecated public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception { }
            });
            this.channel.pipeline().addFirst("fz:null", new ChannelOutboundHandlerAdapter() {
                @Override public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception { }
            });
        }

    }

    private static class FakeNetHandler extends NetHandlerPlayServer {
        public FakeNetHandler(EntityPlayerMP player) {
            super(MinecraftServer.getServer(), new FakeNetManager(), player);
        }

        @Override public void sendPacket(Packet ignored) { }
    }

    private static class FzFakePlayer extends FakePlayer {
        Coord where;

        private FzFakePlayer(WorldServer world, String name, Coord where) {
            super(world, makeProfile(name));
            this.where = where;
            playerNetServerHandler = new FakeNetHandler(this);
        }

        @Override
        public BlockPos getPosition() {
            return where.toBlockPos();
        }

        @Override
        public boolean isEntityInvulnerable(DamageSource source) {
            return true;
        }
    }

    public static MovingObjectPosition rayTrace(EntityPlayer player, double dist, float partial) {
        Vec3 pos = new Vec3(player.posX, player.posY + (player.getEyeHeight() - player.getDefaultEyeHeight()), player.posZ);
        Vec3 look = player.getLook(partial);
        Vec3 ray = pos.addVector(look.xCoord * dist, look.yCoord * dist, look.zCoord * dist);
        return player.worldObj.rayTraceBlocks(pos, ray, false, false, true);
    }

    public static ItemStack cheatDecr(EntityPlayer player, ItemStack stack) {
        if (stack == null) return null;
        if (!isPlayerCreative(player)) {
            stack.stackSize--;
        }
        return ItemUtil.normalize(stack);
    }

    public static void consumeHeldItem(EntityPlayer player) {
        if (isPlayerCreative(player)) return;
        ItemStack held = player.getHeldItem();
        if (held == null) return;
        held.stackSize--;
        if (held.stackSize <= 0) {
            player.setCurrentItemOrArmor(0, null);
        }
    }
}
