package factorization.shared;

import factorization.api.IEntityMessage;
import factorization.api.datahelpers.*;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.Packet;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.relauncher.Side;

import java.io.IOException;

public abstract class EntityFz extends Entity implements IEntityAdditionalSpawnData, IEntityMessage {

    public EntityFz(World w) {
        super(w);
    }

    @Override
    protected final void readEntityFromNBT(NBTTagCompound tag) {
        //super.readEntityFromNBT(tag);
        DataHelper data = new DataInNBT(tag);
        try {
            putData(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected final void writeEntityToNBT(NBTTagCompound tag) {
        //super.writeEntityToNBT(tag);
        DataHelper data = new DataOutNBT(tag);
        try {
            putData(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public final void writeSpawnData(ByteBuf buffer) {
        DataHelper data = new DataOutByteBuf(buffer, Side.SERVER);
        try {
            putData(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public final void readSpawnData(ByteBuf buffer) {
        DataHelper data = new DataInByteBuf(buffer, Side.CLIENT);
        try {
            putData(data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void syncData() {
        // Rember 'syncWithSpawnPacket'? Broken. In a subtle-ish way. Don't use it. Use this.
        ByteBuf output = Unpooled.buffer();
        try {
            Core.network.prefixEntityPacket(output, this, NetworkFactorization.MessageType.entity_sync);
            writeSpawnData(output);
            Packet p = Core.network.entityPacket(output);
            FzNetDispatch.addPacketFrom(p, this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected abstract void putData(DataHelper data) throws IOException;

    @Override
    public boolean handleMessageFromClient(NetworkFactorization.MessageType messageType, ByteBuf input) throws IOException {
        return false;
    }

    @Override
    public boolean handleMessageFromServer(NetworkFactorization.MessageType messageType, ByteBuf input) throws IOException {
        if (messageType == NetworkFactorization.MessageType.entity_sync) {
            putData(new DataInByteBuf(input, Side.CLIENT));
            return true;
        }
        return false;
    }
}
