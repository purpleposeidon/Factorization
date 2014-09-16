package factorization.coremod;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodNode;

import cpw.mods.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;

abstract class AbstractAsmMethodTransform {
    protected final String obfClassName;
    final String srgName;
    final String mcpName;
    boolean satisfied = false;
    AbstractAsmMethodTransform(String obfClassName, String srgClassName, String srgName, String mcpName) {
        this.obfClassName = obfClassName;
        this.srgName = srgName;
        this.mcpName = mcpName;
    }
    
    boolean applies(MethodNode method) {
        if (LoadingPlugin.deobfuscatedEnvironment) {
            return method.name.equals(mcpName);
        } else {
            String method_as_srg = FMLDeobfuscatingRemapper.INSTANCE.mapMethodName(obfClassName, method.name, method.desc);
            return srgName.equals(method_as_srg);
        }
    }
    
    abstract void apply(MethodNode base, MethodNode addition);
    
    static boolean isReturn(int op) {
        return op == Opcodes.IRETURN
                || op == Opcodes.LRETURN
                || op == Opcodes.FRETURN
                || op == Opcodes.DRETURN
                || op == Opcodes.ARETURN
                || op == Opcodes.RETURN;
    }
    
    static class Append extends AbstractAsmMethodTransform {
        Append(String obfClassName, String srgClassName, String srgName, String mcpName) {
            super(obfClassName, srgClassName, srgName, mcpName);
        }

        @Override
        void apply(MethodNode base, MethodNode addition) {
            AbstractInsnNode base_end = base.instructions.getLast();
            while (!isReturn(base_end.getOpcode())) {
                base_end = base_end.getPrevious();
            }
            AbstractInsnNode ins = addition.instructions.getFirst();
            while (ins != null) {
                AbstractInsnNode next = ins.getNext();
                if (!(ins instanceof LineNumberNode)) {
                    base.instructions.insertBefore(base_end, ins);
                }
                ins = next;
            }
            base.instructions.remove(base_end);
        }
    }
    
    static class Prepend extends AbstractAsmMethodTransform {
        Prepend(String obfClassName, String srgClassName, String srgName, String mcpName) {
            super(obfClassName, srgClassName, srgName, mcpName);
        }

        @Override
        void apply(MethodNode base, MethodNode addition) {
            AbstractInsnNode head = base.instructions.getFirst();
            AbstractInsnNode addition_end = addition.instructions.getLast();
            // Seek to just before the return insn
            while (!isReturn(addition_end.getOpcode())) {
                addition_end = addition_end.getPrevious();
            }
            while (isReturn(addition_end.getOpcode())) {
                addition_end = addition_end.getPrevious();
            }
            AbstractInsnNode cleaner = addition_end.getNext();
            while (cleaner != null) {
                AbstractInsnNode next = cleaner.getNext();
                addition.instructions.remove(cleaner);
                cleaner = next;
            }
            base.instructions.insertBefore(head, addition.instructions);
        }
        
    }
}