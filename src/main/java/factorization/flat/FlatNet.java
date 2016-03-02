package factorization.flat;

import factorization.api.Coord;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLEventChannel;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;

public class FlatNet {
    static final String channelName = "fzFlat";
    static FMLEventChannel channel;

    public FlatNet() {
        channel = NetworkRegistry.INSTANCE.newEventDrivenChannel(channelName);
        channel.register(this);
    }

    @SubscribeEvent
    public void packetToServer(FMLNetworkEvent.ServerCustomPacketEvent event) {

    }

    @SubscribeEvent
    public void packetToClient(FMLNetworkEvent.ClientCustomPacketEvent event) {
        
    }

    static byte INTERACT_HIT = 1, INTERACT_USE = 2, SYNC_CHUNK = 3, SYNC_SINGLE = 4;

    static ByteBuf prepare(byte packetType) {
        ByteBuf ret = Unpooled.buffer();
        ret.writeByte(packetType);
        return ret;
    }

    static void playerInteract(EntityPlayer player, Coord at, EnumFacing side, boolean useElseHit) {
        ByteBuf buff = prepare(useElseHit ? INTERACT_USE : INTERACT_HIT);
        at.writeToStream(buff);
        buff.writeByte(side.ordinal());
        send(player, buff);
    }

    static void send(EntityPlayer player, ByteBuf buff) {
        FMLProxyPacket toSend = new FMLProxyPacket(new PacketBuffer(buff), channelName);
        if (player.worldObj.isRemote) {
            channel.sendToServer(toSend);
        } else {
            channel.sendTo(toSend, (EntityPlayerMP) player);
        }
    }
}
