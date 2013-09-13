package factorization.coremod;

import java.util.Iterator;

import net.minecraft.launchwrapper.IClassTransformer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import cpw.mods.fml.relauncher.SideOnly;

public class StripSideOnly implements IClassTransformer {
    
    private static final String LSideOnly = "L" + SideOnly.class.getCanonicalName().replace('.', '/') + ";";
    
    @Override
    public byte[] transform(String name, String transformedName, byte[] original_bytes) {
        if (original_bytes == null) {
            return null;
        }
        ClassNode classNode = new ClassNode();
        ClassReader classReader = new ClassReader(original_bytes);
        classReader.accept(classNode, 0);
        boolean changes = false;
        for (MethodNode methodNode : classNode.methods) {
            final String mname = methodNode.name;
            final boolean target_method = mname.equals("idPicked") || mname.equals("func_71922_a")
                    || mname.equals("getPickBlock") /* unlikely, but just in case (forge method, so no srg name) */;
            if (!target_method) {
                continue;
            }
            if (methodNode.visibleAnnotations == null) {
                continue;
            }
            for (Iterator<AnnotationNode> iterator = methodNode.visibleAnnotations.iterator(); iterator.hasNext();) {
                AnnotationNode an = iterator.next();
                if (LSideOnly.equals(an.desc)) {
                    changes = true;
                    iterator.remove();
                }
            }
        }
        if (!changes) {
            return original_bytes;
        }
        ClassWriter writer = new ClassWriter(0);
        classNode.accept(writer);
        return writer.toByteArray();
    }

}
