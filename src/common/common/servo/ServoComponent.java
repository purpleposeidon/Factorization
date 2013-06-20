package factorization.common.servo;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
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
import factorization.api.datahelpers.DataInNBT;
import factorization.api.datahelpers.DataInPacket;
import factorization.api.datahelpers.DataOutNBT;
import factorization.api.datahelpers.DataOutPacket;
import factorization.api.datahelpers.IDataSerializable;
import factorization.api.datahelpers.Share;
import factorization.common.Core;
import factorization.common.FactorizationUtil;

public abstract class ServoComponent implements IDataSerializable {
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
    
    public short getNetworkId() {
        BiMap<Class<? extends ServoComponent>, Short> map = getPacketIdMap().inverse();
        Short o = map.get(getClass());
        if (o == null) {
            throw new IllegalArgumentException(getClass() + " is not a registered ServoComponent");
        }
        return o;
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
            (new DataInNBT(tag)).as(Share.VISIBLE, "sc").put(decor);
            return decor;
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }

    protected final void save(NBTTagCompound tag) {
        tag.setString(componentTagKey, getName());
        try {
            (new DataOutNBT(tag)).as(Share.VISIBLE, "sc").put(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    void writeToPacket(DataOutputStream dos) throws IOException {
        dos.writeShort(getNetworkId());
        (new DataOutPacket(dos, Side.SERVER)).as(Share.VISIBLE, "sc").put(this);
    }
    
    static ServoComponent readFromPacket(DataInputStream dis) throws IOException {
        short id = dis.readShort();
        Class<? extends ServoComponent> componentClass = getPacketIdMap().get(id);
        if (componentClass == null) {
            Core.logWarning("Unknown servo component with #ID %s", id);
            return null;
        }
        try {
            ServoComponent decor = componentClass.newInstance();
            (new DataInPacket(dis, Side.CLIENT)).as(Share.VISIBLE, "sc").put(decor);
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
    public abstract boolean onClick(EntityPlayer player, Coord block, ForgeDirection side);
    public abstract boolean onClick(EntityPlayer player, ServoMotor motor);
    
    /**
     * @return a unique name, something like "modname.componentType.name"
     */
    public abstract String getName();
    
    /**
     * Attempt to update the ServoComponent's configuration from stack. {@link deconfigure} will be called first.
     * @param stack
     */
    public final boolean configure(ServoStack stack) {
        stack.setConfiguring(true);
        stack.setReader(true);
        try {
            stack.put(this);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * Used to wipe everything clean, to emptiness. Important conditions:
     * <ul>
     * 	<li>Be sure to include the ItemStacks that were sucked up via configure() so that nothing gets destroyed.</li>
     *  <li>Calling deconfigure() and configure() (or in the other order) with the same stack should keep the same state,
     *  tho there are some cases where this is not feasible.</li>
     * </ul>
     * @param list to add state to.
     */
    public final void deconfigure(LinkedList<Object> stack) {
        ServoStack out = new ServoStack();
        out.setConfiguring(true);
        out.setReader(false);
        out.setContentsList(stack);
        try {
            out.put(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Render to the Tessellator. This must be appropriate for a SimpleBlockRenderingHandler.
     * @param where to render it at in world. If null, it is being rendered in an inventory (or so). Render to 0,0,0.
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
    public void addInformation(List info) {
        info.add("Servo Component");
    }
    
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
    
    public static void setupRecipes() {
        for (Class<? extends ServoComponent> klazz : componentMap.values()) {
            try {
                klazz.newInstance().addRecipes();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    protected void addRecipes() {}
}
