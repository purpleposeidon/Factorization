package factorization.common.servo;

import java.io.IOException;
import java.util.HashMap;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.ForgeDirection;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.DataInNBT;
import factorization.api.datahelpers.DataOutNBT;
import factorization.common.Core;

public abstract class ServoComponent {
    private static HashMap<Integer, Class<? extends ServoComponent>> idMap = new HashMap<Integer, Class<? extends ServoComponent>>(50);
    
    public static void register(Class<? extends ServoComponent> decorClass) {
        int id;
        try {
            ServoComponent decor = decorClass.newInstance();
            id = decor.getUniqueId();
        } catch (Throwable e) {
            throw new IllegalArgumentException(e);
        }
        idMap.put(id, decorClass);
    }
    
    
    final private static String id_index = "SCId";
    
    static ServoComponent load(NBTTagCompound tag) {
        if (tag == null || !tag.hasKey(id_index)) {
            return null;
        }
        int id = tag.getInteger(id_index);
        Class<? extends ServoComponent> decorClass = idMap.get(id);
        if (decorClass == null) {
            Core.logWarning("Unknown servo component with ID %i", id);
            return null;
        }
        try {
            ServoComponent decor = decorClass.newInstance();
            decor.putData(new DataInNBT(tag));
            return decor;
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }
    
    protected final NBTTagCompound save() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger(id_index, getUniqueId());
        try {
            putData(new DataOutNBT(tag));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return tag;
    }
    
    private static int range = 10000;
    protected final static int ACTUATOR_BASE = 0*range, CONTROLLER_BASE = 1*range, DECORATOR_BASE = 2*range, INSTRUCTION_BASE = 3*range, OTHER_BASE = 4*range;
    
    public abstract String getName();
    public abstract int getUniqueId();
    protected abstract void putData(DataHelper data) throws IOException;
    abstract void onClick(EntityPlayer player, ForgeDirection side);
    abstract void dropItems();
    @SideOnly(Side.CLIENT)
    abstract void renderDynamic();
    @SideOnly(Side.CLIENT)
    abstract void renderStatic();
    
}
