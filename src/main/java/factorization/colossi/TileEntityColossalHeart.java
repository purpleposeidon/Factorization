package factorization.colossi;

import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.DataInNBT;
import factorization.api.datahelpers.DataOutNBT;
import factorization.api.datahelpers.Share;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;

import java.io.IOException;
import java.util.UUID;

public class TileEntityColossalHeart extends TileEntity {
    public static final UUID noController = UUID.fromString("00000000-0000-0000-0000-000000000000");
    int seed = -1;
    UUID controllerUuid = noController;
    String lore1, lore2;
    
    void loadInfoFromBuilder(ColossalBuilder builder) {
        seed = builder.seed;
        if (builder.mask1 != null) {
            lore1 = builder.mask1.lore;
        }
        if (builder.mask2 != null) {
            lore2 = builder.mask2.lore;
        }
    }
    
    void showInfo(EntityPlayer player) {
        player.addChatMessage(new ChatComponentText("Seed: " + seed));
    }
    
    void putData(DataHelper data) throws IOException {
        seed = data.as(Share.PRIVATE, "gen_seed").putInt(seed);
        controllerUuid = data.as(Share.PRIVATE, "controller").putUUID(controllerUuid);
        lore1 = data.as(Share.PRIVATE, "lore1").putString(lore1);
        lore2 = data.as(Share.PRIVATE, "lore2").putString(lore2);
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
}
