package factorization.common.servo;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.ForgeDirection;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.DataInNBT;
import factorization.api.datahelpers.DataOutNBT;
import factorization.common.Core;

public abstract class ServoComponent {
    private static HashMap<String, Class<? extends ServoComponent>> componentMap = new HashMap<String, Class<? extends ServoComponent>>(50, 0.5F);
    final private static String componentTagKey = "SCId";
    
    public static void register(Class<? extends ServoComponent> componentClass) {
        String name;
        try {
            ServoComponent decor = componentClass.newInstance();
            name = decor.getName();
        } catch (Throwable e) {
            throw new IllegalArgumentException(e);
        }
        componentMap.put(name, componentClass);
    }
    
    public static Class<? extends ServoComponent> getComponent(String name) {
        return componentMap.get(name);
    }
    
    static ServoComponent load(NBTTagCompound tag) {
        if (tag == null || !tag.hasKey(componentTagKey)) {
            return null;
        }
        String componentName = tag.getString(componentTagKey);
        Class<? extends ServoComponent> componentClass = getComponent(componentName);
        if (componentClass == null) {
            Core.logWarning("Unknown servo component with ID %s", componentName);
            return null;
        }
        try {
            ServoComponent decor = componentClass.newInstance();
            decor.putData(new DataInNBT(tag));
            return decor;
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }
    
    protected final void save(NBTTagCompound tag) {
        tag.setString(componentTagKey, getName());
        try {
            putData(new DataOutNBT(tag));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * @return a unique name, something like "modname.componentType.name"
     */
    public abstract String getName();
    
    /**
     * This function is called to do all serialization
     * @param data
     * @throws IOException
     */
    protected abstract void putData(DataHelper data) throws IOException;
    public abstract void onClick(EntityPlayer player, ForgeDirection side);
    public abstract List<ItemStack> dropItems();
    
    /**
     * Render to the Tessellator. This must be appropriate for a SimpleBlockRenderingHandler.
     */
    @SideOnly(Side.CLIENT)
    public abstract void renderStatic();
    
    @SideOnly(Side.CLIENT)
    public void renderDynamic() {
        Tessellator tess = Tessellator.instance;
        tess.startDrawingQuads();
        renderStatic();
        tess.draw();
    }
    
    public abstract void configure(ServoStack stack);
}
