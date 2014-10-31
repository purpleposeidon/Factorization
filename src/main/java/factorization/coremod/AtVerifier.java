package factorization.coremod;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.Type;

import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.io.CharSource;
import com.google.common.io.Resources;

import cpw.mods.fml.relauncher.ReflectionHelper;

public class AtVerifier {
    private static String currentLine;
    
    public static void verify() {
        //InputStream at = AtVerifier.class.getResourceAsStream("factorization_at.cfg");
        CharSource at = Resources.asCharSource(Resources.getResource("factorization_at.cfg"), Charsets.UTF_8);
        if (at == null) {
            throw new IllegalArgumentException("AT is missing!");
        }
        try {
            Splitter bannana = Splitter.on(" ").trimResults();
            for (String line : at.readLines()) {
                int rem = line.indexOf("#");
                if (rem != -1) {
                    line = line.substring(0,  rem);
                }
                if (line.equals("")) continue;
                currentLine = line;
                List<String> parts = bannana.splitToList(line);
                if (parts == null || parts.size() == 0) continue;
                String access, className, member;
                if (parts.size() == 3) {
                    access = parts.get(0);
                    className = parts.get(1);
                    member = parts.get(2);
                } else if (parts.size() == 2) {
                    access = parts.get(0);
                    className = parts.get(1);
                    member = null;
                } else {
                    throw new IllegalArgumentException("Malformed AT?");
                }
                try {
                    validate(access, className, member);
                } catch (Throwable t) {
                    throw new IllegalArgumentException(t);
                }
            }
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
        }
    }
    
    protected static void validate(String access, String className, String member) throws Throwable {
        Class<?> theClass = AtVerifier.class.forName(className);
        if (member == null) {
            validateClass(access, theClass);
            return;
        }
        if (member.startsWith("*")) return;
        if (member.contains("(")) {
            validateMethod(access, theClass, member);
        } else {
            validateField(access, theClass, member);
        }
    }

    private static void validateClass(String access, Class<?> theClass) {
        int mod = theClass.getModifiers();
        checkModifiers(access, mod);
    }

    private static void validateField(String access, Class<?> theClass, String member) throws Throwable {
        Field field = theClass.getDeclaredField(member);
        int mod = field.getModifiers();
        checkModifiers(access, mod);
    }

    private static void validateMethod(String access, Class<?> theClass, String member) throws Throwable {
        int paren = member.indexOf("(");
        String methodName = member.substring(0, paren);
        String descr = member.substring(paren, member.length());
        Type methType = Type.getType(descr);
        Type[] argTypes = methType.getArgumentTypes();
        ArrayList<Class<?>> params = new ArrayList();
        for (Type t : argTypes) {
            switch (t.getSort()) {
            case Type.OBJECT:
                params.add(AtVerifier.class.forName(t.getClassName()));
                break;
            case Type.INT:
                params.add(int.class);
                break;
            case Type.SHORT:
                params.add(short.class);
                break;
            case Type.BOOLEAN:
                params.add(boolean.class);
                break;
            case Type.LONG:
                params.add(long.class);
                break;
            case Type.BYTE:
                params.add(byte.class);
                break;
            case Type.CHAR:
                params.add(char.class);
                break;
            case Type.FLOAT:
                params.add(float.class);
                break;
            case Type.DOUBLE:
                params.add(double.class);
                break;
            case Type.ARRAY:
                params.add(Object[].class);
                break;
            }
        }
        Method m = theClass.getDeclaredMethod(methodName, params.toArray(new Class<?>[0]));
        checkModifiers(access, m.getModifiers());
    }
    
    private static void checkModifiers(String access, int mod) {
        if (access.endsWith("-f") && Modifier.isFinal(mod)) crash();
        if (access.endsWith("+f") && !Modifier.isFinal(mod)) crash();
        if (access.startsWith("public") && !Modifier.isPublic(mod)) crash();
        if (access.startsWith("protected") && !(Modifier.isPublic(mod) || Modifier.isProtected(mod))) crash();
    }
    
    private static void crash() {
        throw new IllegalArgumentException("AT failed: " + currentLine);
    }
}
