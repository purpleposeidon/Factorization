package factorization.shared;

import cpw.mods.fml.common.network.internal.FMLNetworkHandler;
import factorization.api.IEntityMessage;
import io.netty.buffer.ByteBuf;

import java.io.IOException;

import io.netty.buffer.Unpooled;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.Packet;
import net.minecraft.world.World;
import cpw.mods.fml.common.registry.IEntityAdditionalSpawnData;
import cpw.mods.fml.relauncher.Side;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.DataInByteBuf;
import factorization.api.datahelpers.DataInNBT;
import factorization.api.datahelpers.DataOutByteBuf;
import factorization.api.datahelpers.DataOutNBT;

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

    public void syncWithSpawnPacket() {
        if (worldObj.isRemote) return;
        Packet p = FMLNetworkHandler.getEntitySpawningPacket(this);
        FzNetDispatch.addPacketFrom(p, this);
    }

    public void syncData() {
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
