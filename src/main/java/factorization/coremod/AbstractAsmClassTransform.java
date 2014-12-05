package factorization.coremod;

import java.util.ListIterator;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodNode;

public abstract class AbstractAsmClassTransform {
    public abstract void apply(ClassNode cn);
    
    public static class Mixin extends AbstractAsmClassTransform {
        ClassNode mixin;
        String mixinName;
        String slashedMixinName;
        String mangledMixinName;
        
        public Mixin(String mixinName, String mangledMixinName) {
            this.mixinName = mixinName;
            this.slashedMixinName = mixinName.replace(".", "/");
            this.mangledMixinName = mangledMixinName;
            mixin = ASMTransformer.readClass(mixinName);
        }

        @Override
        public void apply(ClassNode parent) {
            // TODO: Doesn't copy field initializers
            if (mixin.fields != null) {
                for (FieldNode field : mixin.fields) {
                    parent.fields.add(field);
                }
            }
            if (mixin.methods != null) {
                for (MethodNode method : mixin.methods) {
                    if (method.name.equals("<init>")) continue;
                    parent.methods.add(method);
                    for (LocalVariableNode var : method.localVariables) {
                        if (var.desc.equals(mangledMixinName)) {
                            var.desc = "L" + parent.name + ";";
                        }
                    }
                    ListIterator<AbstractInsnNode> it = method.instructions.iterator();
                    while (it.hasNext()) {
                        AbstractInsnNode insn = it.next();
                        if (insn instanceof FieldInsnNode) {
                            FieldInsnNode field = (FieldInsnNode) insn;
                            if (field.desc.equals(mangledMixinName)) {
                                field.desc = parent.name;
                            }
                            if (field.owner.equals(slashedMixinName)) {
                                field.owner = parent.name;
                            }
                        }
                    }
                }
            }
            if (mixin.interfaces != null) {
                for (String interf : mixin.interfaces) {
                    parent.interfaces.add(interf);
                }
            }
            if (mixin.visibleAnnotations != null) {
                for (AnnotationNode annotation : mixin.visibleAnnotations) {
                    parent.visibleAnnotations.add(annotation);
                }
            }
            if (mixin.invisibleAnnotations != null) {
                for (AnnotationNode annotation : mixin.invisibleAnnotations) {
                    parent.invisibleAnnotations.add(annotation);
                }
            }
            
        }
    }
}
