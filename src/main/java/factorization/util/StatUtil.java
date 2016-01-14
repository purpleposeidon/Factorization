package factorization.util;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.S37PacketStatistics;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.stats.StatBase;
import net.minecraft.stats.StatFileWriter;
import net.minecraft.stats.StatisticsFile;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.HashMap;

public class StatUtil {
    public static StatisticsFile getStatsFile(EntityPlayer player) {
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null) return null;
        ServerConfigurationManager cm = server.getConfigurationManager();
        if (cm == null) return null;
        return cm.getPlayerStatsFile(player);
    }

    public interface IFzStat {
        int get();
        void set(int val);
        int add(int val);
        void sync();
    }

    public static IFzStat load(EntityPlayer player, StatBase field) {
        if (player.worldObj.isRemote) return loadForClient(player, field);
        return new ServerStat(player, field);
    }

    public static IFzStat loadWithBackup(EntityPlayer player, StatBase field, String backupKeyName) {
        return new ServerStatBackedup(player, field, backupKeyName);
    }

    public static IFzStat loadForClient(EntityPlayer player, StatBase field) {
        if (player.worldObj.isRemote) {
            return doClientLoad(player, field);
        }
        return null;
    }

    private static IFzStat doClientLoad(EntityPlayer player, StatBase field) {
        if (player instanceof EntityPlayerSP) {
            return new ClientSideStat((EntityPlayerSP) player, field);
        }
        return null;
    }

    private static class ServerStat implements IFzStat {
        final EntityPlayer player;
        final StatBase stat;
        final StatisticsFile statsFile;

        public ServerStat(EntityPlayer player, StatBase stat) {
            this.player = player;
            this.stat = stat;
            statsFile = getStatsFile(player);
        }

        @Override
        public int get() {
            if (statsFile == null) return 0;
            return statsFile.readStat(stat);
        }

        @Override
        public void set(int val) {
            if (statsFile == null) return;
            statsFile.unlockAchievement(player, stat, val);
        }

        @Override
        public int add(int val) {
            set(get() + val);
            return get();
        }

        @Override
        public void sync() {
            if (!(player instanceof EntityPlayerMP)) return;
            EntityPlayerMP player = (EntityPlayerMP) this.player;
            int value = get();
            HashMap<StatBase, Integer> statInfo = new HashMap<StatBase, Integer>();
            statInfo.put(stat, value);
            player.playerNetServerHandler.sendPacket(new S37PacketStatistics(statInfo));
        }
    }

    private static class ServerStatBackedup extends ServerStat {
        final String backupName;

        public ServerStatBackedup(EntityPlayer player, StatBase stat, String backupName) {
            super(player, stat);
            this.backupName = backupName;
        }

        @Override
        public int get() {
            int ret = super.get();
            if (ret == 0) {
                if (player.getEntityData().hasKey(backupName)) {
                    ret = player.getEntityData().getInteger(backupName);
                    set(ret);
                }
            }
            return ret;
        }

        @Override
        public void set(int val) {
            super.set(val);
            player.getEntityData().setInteger(backupName, val);
        }
    }


    @SideOnly(Side.CLIENT)
    private static class ClientSideStat implements IFzStat {
        private final EntityPlayerSP player;
        private final StatFileWriter statsFile;
        private final StatBase field;

        public ClientSideStat(EntityPlayerSP player, StatBase field) {
            this.player = player;
            this.statsFile = player.getStatFileWriter();
            this.field = field;
        }

        @Override
        public int get() {
            return statsFile.readStat(field);
        }

        @Override
        public void set(int val) {
            statsFile.unlockAchievement(player, field, val);
        }

        @Override
        public int add(int val) {
            return 0;
        }

        @Override
        public void sync() {

        }
    }


}
