package factorization.coremod;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.LaunchClassLoader;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.util.TraceClassVisitor;

import cpw.mods.fml.relauncher.FMLRelaunchLog;

public class ASMTransformer implements IClassTransformer {
    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (transformedName.equals("net.minecraft.block.Block")) {
            return applyTransform(basicClass,
                    new AbstractAsmTransform.Append(name, transformedName, "func_149723_a", "onBlockDestroyedByExplosion"),
                    new AbstractAsmTransform.Append(name, transformedName, "func_149659_a", "canDropFromExplosion")
            );
        }
        if (transformedName.equals("net.minecraft.client.gui.inventory.GuiContainer")) {
            return applyTransform(basicClass,
                    new AbstractAsmTransform.Append(name, transformedName, "func_73869_a", "keyTyped")
            );
        }
        if (transformedName.equals("net.minecraft.client.Minecraft")) {
            return applyTransform(basicClass,
                    new AbstractAsmTransform.Prepend(name, transformedName, "func_147116_af", "func_147116_af"), // "attack key pressed" function (first handler), MCPBot name clickMouse
                    new AbstractAsmTransform.Prepend(name, transformedName, "func_147121_ag", "func_147121_ag") // "use key pressed" function, MCPBot name rightClickMouse
            );
        }
        return basicClass;
    }
    
    byte[] applyTransform(byte[] basicClass, AbstractAsmTransform... changes) {
        ClassReader cr = new ClassReader(basicClass);
        ClassNode cn = new ClassNode();
        cr.accept(cn, 0);

        for (MethodNode m : cn.methods) {
            for (AbstractAsmTransform change : changes) {
                if (change.applies(m)) {
                    MethodNode method = getMethod(change.srgName);
                    change.apply(m, method);
                    change.satisfied = true;
                }
            }
        }
        
        for (AbstractAsmTransform change : changes) {
            if (!change.satisfied) {
                throw new RuntimeException("Unable to find method " + cn.name + "." + change.srgName + " (" + change.mcpName + ")");
            }
        }

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        return cw.toByteArray();
    }
    
    List<MethodNode> apendeeMethods = null;
    
    MethodNode getMethod(String methodName) {
        if (apendeeMethods == null) {
            ClassReader cr = new ClassReader(getAppendeeBytecodeBytecode());
            ClassNode cn = new ClassNode();
            cr.accept(cn, 0);
            apendeeMethods = cn.methods;
        }
        for (MethodNode m : apendeeMethods) {
            if (m.name.equals(methodName)) {
                return m;
            }
        }
        throw new RuntimeException("Couldn't find method " + methodName);
    }

    
    byte[] getAppendeeBytecodeBytecode() {
        LaunchClassLoader rcl = (LaunchClassLoader) getClass().getClassLoader();
        try {
            return rcl.getClassBytes(MethodSplices.class.getCanonicalName());
        } catch (IOException e) {
            e.printStackTrace();
        }   
        return null;
    }
    
    static HashMap<Integer, String> opcodeNameMap = new HashMap();
    static {
        boolean started = false;
        for (Field f : Opcodes.class.getFields()) {
            if (!started) {
                if (f.getName() != "NOP") {
                    continue;
                }
                started = true;
            }
            try {
                opcodeNameMap.put(f.getInt(null), f.getName());
            } catch (IllegalArgumentException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
    
    static void printInstructions(AbstractInsnNode ins) {
        while (ins != null) {
            int code = ins.getOpcode();
            if (code != -1) {
                Object o = opcodeNameMap.get(code);
                if (o == null) {
                    o = code;
                }
                System.out.println(o + "  " + ins);
            } else {
                if ((ins instanceof JumpInsnNode)) {
                    System.out.println("Jump to " + ((JumpInsnNode) ins).label.getLabel());
                } else if ((ins instanceof LabelNode)) {
                    System.out.println("Label: " + ((LabelNode) ins).getLabel());
                } else {
                    System.out.println(ins);
                }
            }
            ins = ins.getNext();
        }
    }
    
    static void dumpASM(byte[] basicClass) {
        ClassReader cr = new ClassReader(basicClass);
        ClassNode cn = new ClassNode();
        cr.accept(cn, 0);
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        File f = new File("/tmp/ASM/" + cn.name.replace("/", "_"));
        try {
            FileOutputStream os = new FileOutputStream(f);
            cr.accept(new TraceClassVisitor(new PrintWriter(os)), 0);
            os.close();
        } catch (Throwable e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    void log(Object msg) {
        FMLRelaunchLog.info(msg.toString());
    }
}
