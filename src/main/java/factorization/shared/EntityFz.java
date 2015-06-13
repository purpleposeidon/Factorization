package factorization.shared;

import cpw.mods.fml.common.network.internal.FMLNetworkHandler;
import io.netty.buffer.ByteBuf;

import java.io.IOException;

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

public abstract class EntityFz extends Entity implements IEntityAdditionalSpawnData {

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

    protected abstract void putData(DataHelper data) throws IOException;

}
