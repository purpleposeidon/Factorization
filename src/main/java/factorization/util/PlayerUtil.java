package factorization.util;

import com.mojang.authlib.GameProfile;
import factorization.api.Coord;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.rcon.RConConsoleSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.stats.StatisticsFile;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.StringUtils;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;
import java.util.WeakHashMap;

public final class PlayerUtil {
    private static final UUID FZ_UUID = null; //UUID.fromString("f979c78a-f80d-46b1-9c49-0121ea8850e6");
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
        found.where = where;
        where.setAsEntityLocation(found);
        Arrays.fill(found.inventory.armorInventory, null);
        Arrays.fill(found.inventory.mainInventory, null);
        found.isDead = false;
        return found;
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
        return server.getConfigurationManager().func_152596_g(player.getGameProfile());
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
        return cm.func_152602_a(player);
    }

    private static class FakeNetManager extends NetworkManager {
        public FakeNetManager() {
            super(false);
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
        public ChunkCoordinates getPlayerCoordinates() {
            return new ChunkCoordinates(where.x, where.y, where.z);
        }

        @Override
        public boolean isEntityInvulnerable() {
            return true;
        }
    }
}
