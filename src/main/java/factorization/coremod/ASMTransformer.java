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
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.TraceClassVisitor;

import cpw.mods.fml.relauncher.FMLRelaunchLog;

public class ASMTransformer implements IClassTransformer {
    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (transformedName.equals("net.minecraft.block.Block")) {
            return applyTransform(basicClass,
                    new AbstractAsmMethodTransform.Append(name, transformedName, "func_149723_a", "onBlockDestroyedByExplosion"),
                    new AbstractAsmMethodTransform.Append(name, transformedName, "func_149659_a", "canDropFromExplosion")
            );
        }
        if (transformedName.equals("net.minecraft.client.gui.inventory.GuiContainer")) {
            return applyTransform(basicClass,
                    new AbstractAsmMethodTransform.Append(name, transformedName, "func_73869_a", "keyTyped")
            );
        }
        if (transformedName.equals("net.minecraft.client.Minecraft")) {
            return applyTransform(basicClass,
                    new AbstractAsmMethodTransform.Prepend(name, transformedName, "func_147116_af", "func_147116_af"), // "attack key pressed" function (first handler), MCPBot name clickMouse
                    new AbstractAsmMethodTransform.Prepend(name, transformedName, "func_147121_ag", "func_147121_ag") // "use key pressed" function, MCPBot name rightClickMouse
            );
        }
        if (transformedName.equals("net.minecraft.world.chunk.Chunk")) {
            return applyTransform(basicClass,
                    new AbstractAsmClassTransform.Mixin("factorization.coremodhooks.MixinExtraChunkData", "Lfactorization/coremodhooks/MixinExtraChunkData;"),
                    new AbstractAsmMethodTransform.Append(name, transformedName, "func_76588_a", "getEntitiesWithinAABBForEntity")
            );
        }
        if (transformedName.equals("net.minecraft.entity.Entity")) {
            return applyTransform(basicClass,
                    new AbstractAsmClassTransform.Mixin("factorization.coremodhooks.MixinEntityKinematicsTracker", "Lfactorization/coremodhooks/MixinEntityKinematicsTracker;"));
        }
        if (transformedName.equals("net.minecraft.client.renderer.EntityRenderer")) {
            final String vec_vec_mop = "(Lnet/minecraft/util/Vec3;Lnet/minecraft/util/Vec3;)Lnet/minecraft/util/MovingObjectPosition;";
            return applyTransform(basicClass,
                    new AbstractAsmMethodTransform.MutateCall(name, transformedName, "func_78467_g", "orientCamera")
                        .find("net.minecraft.client.multiplayer.WorldClient", "func_72933_a", "rayTraceBlocks", vec_vec_mop)
            );
        }
        return basicClass;
    }
    
    static AbstractAsmClassTransform[] EMPTY = new AbstractAsmClassTransform[0];
    
    byte[] applyTransform(byte[] basicClass, AbstractAsmMethodTransform... changes) {
        return applyTransform(basicClass, EMPTY, changes);
    }
    
    byte[] applyTransform(byte[] basicClass, AbstractAsmClassTransform ct, AbstractAsmMethodTransform... changes) {
        return applyTransform(basicClass, new AbstractAsmClassTransform[] { ct }, changes);
    }
    
    byte[] applyTransform(byte[] basicClass, AbstractAsmClassTransform[] classChanges, AbstractAsmMethodTransform... changes) {
        ClassReader cr = new ClassReader(basicClass);
        ClassNode cn = new ClassNode();
        cr.accept(cn, 0);
        
        for (AbstractAsmClassTransform classTransformer : classChanges) {
            classTransformer.apply(cn);
        }

        for (MethodNode m : cn.methods) {
            for (AbstractAsmMethodTransform change : changes) {
                if (change.applies(m)) {
                    MethodNode method = getMethod(change.srgName);
                    change.apply(m, method);
                    change.satisfied = true;
                }
            }
        }
        
        for (AbstractAsmMethodTransform change : changes) {
            if (!change.satisfied) {
                throw new RuntimeException("Unable to find method " + cn.name + "." + change.srgName + " (" + change.mcpName + ")");
            }
        }
        
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        return cw.toByteArray();
    }
    
    List<MethodNode> apendeeMethods = null;
    
    public static ClassNode readClass(String classname) {
        LaunchClassLoader rcl = (LaunchClassLoader) ASMTransformer.class.getClassLoader();
        try {
            byte[] classBytes = rcl.getClassBytes(classname);
            ClassReader cr = new ClassReader(classBytes);
            ClassNode cn = new ClassNode();
            cr.accept(cn, 0);
            return cn;
        } catch (IOException e) {
            e.printStackTrace();
        }   
        return null;
    }
    
    MethodNode getMethod(String methodName) {
        if (apendeeMethods == null) {
            ClassNode cn = readClass(MethodSplices.class.getCanonicalName());
            apendeeMethods = cn.methods;
        }
        for (MethodNode m : apendeeMethods) {
            if (m.name.equals(methodName)) {
                return m;
            }
        }
        throw new RuntimeException("Couldn't find method " + methodName);
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
