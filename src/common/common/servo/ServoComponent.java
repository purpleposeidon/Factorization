package factorization.common.servo;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.ForgeDirection;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.reflect.ClassPath;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.api.Coord;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.DataInNBT;
import factorization.api.datahelpers.DataInPacket;
import factorization.api.datahelpers.DataOutNBT;
import factorization.api.datahelpers.DataOutPacket;
import factorization.common.Core;
import factorization.common.FactorizationUtil;

public abstract class ServoComponent {
    private static HashMap<String, Class<? extends ServoComponent>> componentMap = new HashMap<String, Class<? extends ServoComponent>>(50, 0.5F);
    final private static String componentTagKey = "SCId";

    public static void register(Class<? extends ServoComponent> componentClass) {
        String name;
        try {
            ServoComponent decor = componentClass.newInstance();
            name = decor.getName();
        } catch (Throwable e) {
            Core.logSevere("Unable to instantiate %s: %s", componentClass, e);
            e.printStackTrace();
            throw new IllegalArgumentException(e);
        }
        componentMap.put(name, componentClass);
    }

    public static Class<? extends ServoComponent> getComponent(String name) {
        return componentMap.get(name);
    }

    private static BiMap<Short, Class<? extends ServoComponent>> the_idMap = null;
    private static BiMap<Short, Class<? extends ServoComponent>> getPacketIdMap() {
        if (the_idMap == null) {
            ArrayList<String> names = new ArrayList(componentMap.keySet());
            Collections.sort(names);
            ImmutableBiMap.Builder<Short, Class<? extends ServoComponent>> builder = ImmutableBiMap.<Short, Class<? extends ServoComponent>>builder();
            for (short i = 0; i < names.size(); i++) {
                builder.put(i, componentMap.get(names.get(i)));
            }
            the_idMap = builder.build();
        }
        return the_idMap;
    }
    
    public static Iterable<Class<? extends ServoComponent>> getComponents() {
        return getPacketIdMap().values();
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
    
    void writeToPacket(DataOutputStream dos) throws IOException {
        short id = getPacketIdMap().inverse().get(getName());
        dos.writeInt(id);
        DataHelper data = new DataOutPacket(dos, Side.SERVER);
        putData(data);
    }
    
    static ServoComponent readFromPacket(DataInputStream dis) throws IOException {
        int id = dis.readInt();
        Class<? extends ServoComponent> componentClass = getPacketIdMap().get(id);
        if (componentClass == null) {
            Core.logWarning("Unknown servo component with #ID %s", id);
            return null;
        }
        try {
            ServoComponent decor = componentClass.newInstance();
            decor.putData(new DataInPacket(dis, Side.CLIENT));
            return decor;
        } catch (IOException e) {
            throw e;
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public ItemStack toItem() {
        ItemStack ret = new ItemStack(Core.registry.servo_component);
        NBTTagCompound tag = new NBTTagCompound();
        save(tag);
        ret.setTagCompound(tag);
        return ret;
    }
    
    public static ServoComponent fromItem(ItemStack is) {
        if (!is.hasTagCompound()) {
            return null;
        }
        return load(is.getTagCompound());
    }
    
    //return True if the item should be consumed by a survival-mode player
    abstract boolean onClick(EntityPlayer player, Coord block, ForgeDirection side);
    abstract boolean onClick(EntityPlayer player, ServoMotor motor);
    
    /**
     * @return a unique name, something like "modname.componentType.name"
     */
    public abstract String getName();
    
    /**
     * This function is called to do all serialization
     * 
     * @param data
     * @throws IOException
     */
    protected abstract void putData(DataHelper data) throws IOException;
    
    /**
     * Attempt to update the ServoComponent's configuration from stack. {@link deconfigure} will be called first.
     * @param stack
     */
    public abstract boolean configure(ServoStack stack);
    
    /**
     * Used to wipe everything clean, to emptiness. Important conditions:
     * <ul>
     * 	<li>Be sure to include the ItemStacks that were sucked up via configure() so that nothing gets destroyed.</li>
     *  <li>Calling deconfigure() and configure() (or in the other order) with the same stack should keep the same state,
     *  tho there are some cases where this is not feasible.</li>
     * </ul>
     * @param list to add state to.
     */
    public abstract void deconfigure(List<Object> stack);
    
    /**
     * Render to the Tessellator. This must be appropriate for a SimpleBlockRenderingHandler.
     * @param where to render it at in world. If null, render immediately at 0,0,0.
     * @param rb RenderBlocks
     */
    @SideOnly(Side.CLIENT)
    public abstract void renderStatic(Coord where, RenderBlocks rb);
    
    @SideOnly(Side.CLIENT)
    public void renderDynamic() {
        Tessellator tess = Tessellator.instance;
        tess.startDrawingQuads();
        renderStatic(null, FactorizationUtil.getRB());
        tess.draw();
    }
    
    @SideOnly(Side.CLIENT)
    public void addInformation(List info) { }
    
    public static void registerRecursivelyFromPackage(String packageName) {
        ClassLoader loader = ServoComponent.class.getClassLoader();
        ClassPath cp;
        try {
            cp = ClassPath.from(loader);
        } catch (IOException e1) {
            e1.printStackTrace();
            return;
        }
        for (ClassPath.ClassInfo ci : cp.getTopLevelClassesRecursive(packageName)) {
            Class<?> someClass;
            try {
                someClass = loader.loadClass(ci.getName());
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                continue;
            }
            if (ServoComponent.class.isAssignableFrom(someClass)) {
                if (someClass.isInterface() || Modifier.isAbstract(someClass.getModifiers())) {
                    continue;
                }
                Class<? extends ServoComponent> cl = (Class<? extends ServoComponent>) someClass;
                ServoComponent.register(cl);
            }
        }
    }
    
    static {
        registerRecursivelyFromPackage("factorization.common.servo");
    }
}
