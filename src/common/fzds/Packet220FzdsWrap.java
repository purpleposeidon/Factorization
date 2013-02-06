package factorization.fzds;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

import net.minecraft.client.multiplayer.NetClientHandler;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.packet.NetHandler;
import net.minecraft.network.packet.Packet;
import net.minecraft.world.World;

public class Packet220FzdsWrap extends Packet {
    Packet wrapped = null;
    
    public Packet220FzdsWrap() {
        
    }
    
    public Packet220FzdsWrap(Packet toWrap) {
        this.wrapped = toWrap;
        this.isChunkDataPacket = toWrap.isChunkDataPacket;
    }
    
    private static Socket fakeSocket = new Socket();
    @Override
    public void readPacketData(DataInputStream dis) throws IOException {
        wrapped = Packet.readPacket(dis, false, fakeSocket);
    }

    @Override
    public void writePacketData(DataOutputStream dos) throws IOException {
        dos.write(wrapped.getPacketId());
        wrapped.writePacketData(dos);
    }

    @Override
    public void processPacket(NetHandler netHandler) {
        Hammer.proxy.setClientWorld(Hammer.proxy.getOppositeWorld());
        try {
            wrapped.processPacket(netHandler);
        } finally {
            Hammer.proxy.restoreClientWorld();
        }
    }

    @Override
    public int getPacketSize() {
        //We need to exclude our own header, but include the wrapped packet's header
        return 1 + wrapped.getPacketSize();
    }
    
    @Override
    public boolean isRealPacket() {
        return true;
    }
    
    @Override
    public String toString() {
        return super.toString() + " of " + wrapped;
    }

}
