package factorization.servo;

import com.google.common.collect.BiMap;
import com.google.common.collect.ImmutableBiMap;
import factorization.api.Coord;
import factorization.api.datahelpers.*;
import factorization.flat.api.IFlatModel;
import factorization.flat.api.IModelMaker;
import factorization.servo.instructions.*;
import factorization.shared.Core;
import factorization.util.NORELEASE;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public abstract class ServoComponent implements IDataSerializable {
    private static HashMap<String, Class<? extends ServoComponent>> componentMap = new HashMap<String, Class<? extends ServoComponent>>(50, 0.5F);
    final private static String componentTagKey = "SCId";

    public static void register(Class<? extends ServoComponent> componentClass, ArrayList<ItemStack> sortedList) {
        String name;
        ServoComponent decor;
        try {
            decor = componentClass.getConstructor().newInstance();
        } catch (/*ReflectiveOperationException Java 7 */ Throwable e) {
            Core.logSevere("Unable to instantiate %s: %s", componentClass, e);
            e.printStackTrace();
            throw new IllegalArgumentException(e);
        }
        name = decor.getName();
        componentMap.put(name, componentClass);
        sortedList.add(decor.toItem());
    }

    public static Class<? extends ServoComponent> getComponent(String name) {
        return componentMap.get(name);
    }

    private static BiMap<Short, Class<? extends ServoComponent>> the_idMap = null;
    private static BiMap<Short, Class<? extends ServoComponent>> getPacketIdMap() {
        if (the_idMap == null) {
            ArrayList<String> names = new ArrayList<String>(componentMap.keySet());
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

    @Override
    public final IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        if (data.isReader()) {
            String componentName = data.asSameShare(prefix + componentTagKey).putString(getName());
            Class<? extends ServoComponent> componentClass = getComponent(componentName);
            ServoComponent sc;
            try {
                sc = componentClass.newInstance();
            } catch (Throwable e) {
                e.printStackTrace();
                return this;
            }
            return sc.putData(prefix, data);
        } else {
            data.asSameShare(prefix + componentTagKey).putString(getName());
            return putData(prefix, data);
        }
    }

    protected abstract IDataSerializable putData(String prefix, DataHelper data) throws IOException;

    protected static ServoComponent load(NBTTagCompound tag) {
        if (tag == null || !tag.hasKey(componentTagKey)) {
            return null;
        }
        String componentName = tag.getString(componentTagKey);
        Class<? extends ServoComponent> componentClass = getComponent(componentName);
        if (componentClass == null) {
            Core.logWarning("Unknown servo component with ID %s. Removing tag info!", componentName);
            Core.logWarning("The tag: %s", tag);
            Thread.dumpStack();
            tag.removeTag(componentTagKey);
            return null;
        }
        try {
            ServoComponent decor = componentClass.newInstance();
            return new DataInNBT(tag).as(Share.VISIBLE, "sc").putIDS(decor);
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }

    protected final void save(NBTTagCompound tag) {
        tag.setString(componentTagKey, getName());
        try {
            (new DataOutNBT(tag)).as(Share.VISIBLE, "sc").putIDS(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    void writeToPacket(DataOutputStream dos) throws IOException {
        dos.writeShort(getNetworkId());
        (new DataOutPacket(dos, Side.SERVER)).as(Share.VISIBLE, "sc").putIDS(this);
    }
    
    static ServoComponent readFromPacket(ByteBuf dis) throws IOException {
        short id = dis.readShort();
        Class<? extends ServoComponent> componentClass = getPacketIdMap().get(id);
        if (componentClass == null) {
            Core.logWarning("Unknown servo component with #ID %s", id);
            return null;
        }
        try {
            ServoComponent decor = componentClass.newInstance();
            (new DataInByteBuf(dis, Side.CLIENT)).as(Share.VISIBLE, "sc").putIDS(decor);
            return decor;
        } catch (IOException e) {
            throw e;
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public ItemStack toItem() {
        ItemStack ret;
        if (this instanceof Instruction) {
            ret = new ItemStack(Core.registry.servo_widget_instruction);
        } else {
            ret = new ItemStack(Core.registry.servo_widget_decor);
        }
        NBTTagCompound tag = new NBTTagCompound();
        save(tag);
        ret.setTagCompound(tag);
        String name = getName();
        int dmg = Math.abs(name.hashCode()) % (Short.MAX_VALUE);
        ret.setItemDamage(dmg);
        return ret;
    }
    
    public ServoComponent copyComponent() {
        NBTTagCompound tag = new NBTTagCompound();
        save(tag);
        return load(tag);
    }
    
    public static ServoComponent fromItem(ItemStack is) {
        if (!is.hasTagCompound()) {
            return null;
        }
        return load(is.getTagCompound());
    }
    
    //return True if the item should be consumed by a survival-mode player
    public abstract boolean onClick(EntityPlayer player, Coord block, EnumFacing side);
    public abstract boolean onClick(EntityPlayer player, ServoMotor motor);
    
    /**
     * @return a unique name, something like "modname.componentType.name"
     */
    public abstract String getName();

    @SideOnly(Side.CLIENT)
    public void addInformation(List info) {
        NORELEASE.fixme("Generics!!! >:|");
        //info.add("Servo Component");
    }

    static ArrayList<ItemStack> sorted_instructions = new ArrayList<ItemStack>();
    static ArrayList<ItemStack> sorted_decors = new ArrayList<ItemStack>();

    static {
        //registerRecursivelyFromPackage("factorization.common.servo.actuators");
        //registerRecursivelyFromPackage("factorization.common.servo.instructions");
        Class<? extends ServoComponent>[] decorations = (Class<? extends ServoComponent>[])new Class[] {
                ScanColor.class,
        };
        Class<? extends ServoComponent>[] instructions = (Class<? extends ServoComponent>[])new Class[] {
                // Color by class, sort by color
                // Cyan: Motion instructions
                EntryControl.class,
                SetDirection.class,
                SetSpeed.class,
                Trap.class,

                // Red: Redstone-ish instructions
                RedstonePulse.class,
                SocketCtrl.class,
                ReadRedstone.class,
                CountItems.class,
                ShifterControl.class,

                // Yellow: Math instructions
                Drop.class,
                Dup.class,
                IntegerValue.class,
                Sum.class,
                Product.class,
                BooleanValue.class,
                Compare.class,

                // White: Computation instructions
                Jump.class,
                SetEntryAction.class,
                SetRepeatedInstruction.class,
                InstructionGroup.class,
        };

        for (Class<? extends ServoComponent> cl : decorations) register(cl, sorted_decors);
        for (Class<? extends ServoComponent> cl : instructions) register(cl, sorted_instructions);
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

    public void onItemUse(Coord here, EntityPlayer player) { }

    @SideOnly(Side.CLIENT)
    public abstract IFlatModel getModel(Coord at, EnumFacing side);

    @SideOnly(Side.CLIENT)
    protected abstract void loadModels(IModelMaker maker);

    protected static IFlatModel reg(IModelMaker maker, String name) {
        return maker.getModel(new ResourceLocation("factorization:flat/servo/instruction/" + name));
    }

    public byte getPriority() {
        return 0;
    }
}
