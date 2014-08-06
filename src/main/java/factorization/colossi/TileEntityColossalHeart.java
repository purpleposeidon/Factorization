package factorization.colossi;

import java.io.IOException;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.DataInNBT;
import factorization.api.datahelpers.DataOutNBT;
import factorization.api.datahelpers.Share;
import factorization.notify.Notice;

public class TileEntityColossalHeart extends TileEntity {
    int seed = -1;
    
    void loadInfoFromBuilder(ColossalBuilder builder) {
        seed = builder.seed;
    }
    
    void showInfo(EntityPlayer player) {
        new Notice(this, "Seed: " + seed).send(player);
    }
    
    void putData(DataHelper data) throws IOException {
        seed = data.as(Share.PRIVATE, "gen_seed").putInt(seed);
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
