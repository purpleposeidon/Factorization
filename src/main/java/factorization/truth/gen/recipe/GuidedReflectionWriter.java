package factorization.truth.gen.recipe;

import com.google.common.base.Splitter;
import factorization.shared.NORELEASE;
import factorization.truth.api.IObjectWriter;
import factorization.truth.word.LocalizedWord;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.util.StringUtils;
import net.minecraftforge.common.util.Constants;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** NBT tag syntax:
 * {
 *     "category": "Same syntax as AddRecipeCategory",
 *     "output": ["ReflectionExpression"],
 *     "input": ["ReflectionExpression"],
 *     "catalyst": ["ReflectionExpression"],
 *     "text": "a string; goes w/ the recipe. For special instructions. Uses FzDoc formatting."
 * }
 * The recipe will be printed in the order shown. output, input, catalyst, and text do not have to be specified.
 * A ReflectionExpression is a series of actions separated by periods, which can be one of:
 * <ul>
 *     <li>field access: none of the below symbols</li>
 *     <li>method invokation: ends with "()"; no parameters may be passed in, and the function may not return void</li>
 *     <li>NBTTagCompound access: wrapped 'single quotes'. </li>
 * </ul>
 *
 * If a null is returned anywhere, 'null' will be the result of the expression.
 * Fields and parameters are neither obfuscated nor deobfuscated; use only your own mod's stuff & don't go hambone.
 * If you need something more complicated, register an IObjectWriter.
 *
 * If the expression ends with a '#', then everything after is used as a localization key.
 *
 * @see IObjectWriter
 *
 */
public class GuidedReflectionWriter<T> implements IObjectWriter<T> {
    final ReflectionExpression[] input, catalyst, output;
    final String text;

    public static void register(NBTTagCompound tag) throws ClassNotFoundException, NoSuchMethodException, NoSuchFieldException {
        GuidedReflectionWriter<Object> writer = new GuidedReflectionWriter<Object>(tag);
        String label = tag.getString("category").split("\\|")[0];
        RecipeViewer.instance.guiders.put(label, writer);
    }

    private GuidedReflectionWriter(NBTTagCompound tag) throws NoSuchMethodException, NoSuchFieldException {
        input = read(tag, "input");
        catalyst = read(tag, "catalyst");
        output = read(tag, "output");
        text = tag.getString("text");
    }

    ReflectionExpression[] read(NBTTagCompound tag, String keyName) throws NoSuchMethodException, NoSuchFieldException {
        ArrayList<ReflectionExpression> out = new ArrayList<ReflectionExpression>();
        NBTTagList list = tag.getTagList(keyName, Constants.NBT.TAG_STRING);
        for (int i = 0; i < list.tagCount(); i++) {
            String s = list.getStringTagAt(i);
            out.add(new ReflectionExpression(s));
        }
        return out.toArray(new ReflectionExpression[out.size()]);
    }

    @Override
    public void writeObject(List out, T val, IObjectWriter<Object> generic) {
        write(output, out, val, generic);
        write(input, out, val, generic);
        write(catalyst, out, val, generic);
        if (!StringUtils.isNullOrEmpty(text)) {
            out.add(text);
        }
        out.add(NORELEASE.just("\\nl")); // This shouldn't be required.
    }

    private void write(ReflectionExpression[] exprList, List out, T val, IObjectWriter<Object> generic) {
        boolean maybeGeneric = exprList == output && output.length == 1;
        for (ReflectionExpression expr : exprList) {
            Object v;
            try {
                v = expr.get(val);
            } catch (Throwable t) {
                t.printStackTrace();
                out.add("<ERR>");
                continue;
            }
            if (expr.prefix != null) {
                out.add(new LocalizedWord(expr.prefix));
                out.add(": ");
            }
            if (v == null) v = "null";
            if (v instanceof String) {
                out.add(v);
                out.add("\\nl");
            } else {
                ItemStack is = null;
                if (maybeGeneric) {
                    if (v instanceof ItemStack) {
                        is = (ItemStack) v;
                    } else if (v instanceof ItemStack[]) {
                        ItemStack[] array = (ItemStack[]) v;
                        if (array.length > 0) {
                            is = array[0];
                        }
                    } else if (v instanceof Collection) {
                        Collection c = (Collection) v;
                        if (c.size() == 1) {
                            Object next = c.iterator().next();
                            if (next instanceof ItemStack) {
                                is = (ItemStack) next;
                            }
                        }
                    }
                }
                if (is != null) {
                    RecipeViewer.genericRecipePrefix(out, is);
                } else {
                    generic.writeObject(out, v, generic);
                    out.add("\\nl");
                }
            }
        }
    }


}

class ReflectionExpression {
    String exprBody, prefix;
    ReflectionComponent[] parts;

    ReflectionExpression(String exprBody) {
        if (exprBody == null) throw new NullPointerException();
        this.exprBody = exprBody;
    }

    void parse(Object walker, String exprBody) throws NoSuchMethodException, NoSuchFieldException, InvocationTargetException, IllegalAccessException {
        Class<?> head = walker.getClass();
        if (exprBody.contains("#")) {
            String parts[] = exprBody.split("#");
            exprBody = parts[0];
            prefix = parts[1];
        } else {
            prefix = null;
        }
        ArrayList<ReflectionComponent> found = new ArrayList<ReflectionComponent>();
        for (String part : Splitter.on(".").split(exprBody)) {
            ReflectionComponent rc = new ReflectionComponent(head, part);
            walker = rc.get(walker);
            head = walker.getClass();
            found.add(rc);
        }
        this.parts = found.toArray(new ReflectionComponent[found.size()]);
    }

    Object get(Object val) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException, NoSuchFieldException {
        if (parts == null) {
            parse(val, exprBody);
        }
        for (ReflectionComponent rc : parts) {
            val = rc.get(val);
        }
        return val;
    }
}

class ReflectionComponent {
    // So the format is, like...
    // Well, we've got to be given objects from an iterable. So then for each object...
    // "someField.and.aMethod().andAnother.field"
    // But: "Class#thingABoo"
    final Method method;
    final Field field;
    final String nbtKey;

    ReflectionComponent(Class head, String part) throws NoSuchMethodException, NoSuchFieldException {
        if (part.startsWith("'")) {
            this.method = null;
            this.field = null;
            this.nbtKey = part.replaceAll("'", "");
        } else if (part.endsWith("()")) {
            this.field = null;
            this.nbtKey = null;
            this.method = getInterfaceMethod(head, part.replace("()", ""));
            this.method.setAccessible(true);
        } else {
            this.method = null;
            this.nbtKey = null;
            this.field = head.getField(part);
            this.field.setAccessible(true);
        }
    }

    Method getInterfaceMethod(Class head, String part) throws NoSuchMethodException {
        for (Class<?> i : head.getInterfaces()) {
            try {
                return i.getMethod(part);
            } catch (NoSuchMethodException e) {
                // ignored
            }
        }
        return head.getMethod(part);
    }

    Class<?> getType() {
        if (nbtKey != null) return NBTBase.class; // Shouldn't be called? I guess?
        if (field != null) return field.getType();
        return method.getReturnType();
    }

    Object get(Object src) throws IllegalAccessException, InvocationTargetException {
        if (src == null) return null;
        if (nbtKey != null) {
            NBTTagCompound tag = (NBTTagCompound) src;
            NBTBase value = tag.getTag(nbtKey);
            switch (value.getId()) {
                case Constants.NBT.TAG_END: return null;
                case Constants.NBT.TAG_BYTE: return "" + ((NBTTagByte) value).func_150290_f();
                case Constants.NBT.TAG_SHORT: return "" + ((NBTTagShort) value).func_150289_e();
                case Constants.NBT.TAG_INT: return "" + ((NBTTagInt) value).func_150287_d();
                case Constants.NBT.TAG_LONG: return "" + ((NBTTagLong) value).func_150291_c();
                case Constants.NBT.TAG_FLOAT: return "" + ((NBTTagFloat) value).func_150288_h();
                case Constants.NBT.TAG_DOUBLE: return "" + ((NBTTagDouble) value).func_150286_g();
                case Constants.NBT.TAG_BYTE_ARRAY: return null; // No way to handle
                case Constants.NBT.TAG_STRING: return ((NBTTagString) value).func_150285_a_();
                case Constants.NBT.TAG_LIST: return null; // No way to handle
                case Constants.NBT.TAG_COMPOUND: return value;
                case Constants.NBT.TAG_INT_ARRAY: return null; // No way to handle
                default: return null;
            }
        }
        if (field != null) return field.get(src);
        return method.invoke(src);
    }
}
