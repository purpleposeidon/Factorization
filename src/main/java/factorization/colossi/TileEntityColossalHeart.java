package factorization.colossi;

import java.io.IOException;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.DataInNBT;
import factorization.api.datahelpers.DataOutNBT;
import factorization.api.datahelpers.Share;

public class TileEntityColossalHeart extends TileEntity {
    public static final UUID noController = UUID.fromString("00000000-0000-0000-0000-000000000000");
    int seed = -1;
    UUID controllerUuid = noController;
    
    void loadInfoFromBuilder(ColossalBuilder builder) {
        seed = builder.seed;
    }
    
    void showInfo(EntityPlayer player) {
        player.addChatMessage(new ChatComponentText("Seed: " + seed));
    }
    
    void putData(DataHelper data) throws IOException {
        seed = data.as(Share.PRIVATE, "gen_seed").putInt(seed);
        controllerUuid = data.as(Share.PRIVATE, "controller").putUUID(controllerUuid);
    }
    
    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        try {
            putData(new DataOutNBT(tag));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        try {
            putData(new DataInNBT(tag));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public boolean canUpdate() {
        return false;
    }
}
