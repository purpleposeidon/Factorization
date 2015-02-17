package factorization.coremod;

import cpw.mods.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

import java.lang.reflect.Method;

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
    
    static class MutateCall extends AbstractAsmMethodTransform {
        MutateCall(String obfClassName, String srgClassName, String srgName, String mcpName) {
            super(obfClassName, srgClassName, srgName, mcpName);
        }

        private String find_owner, find_mcp_name, find_srg_name, find_notch_name, find_notch_desc, find_mcp_desc;
        
        public MutateCall find(String owner, String srg_name, String mcp_name, String notch_name, String find_notch_desc) {
            if (!ASMTransformer.dev_environ) {
                owner = FMLDeobfuscatingRemapper.INSTANCE.unmap(owner.replace(".", "/"));
                find_srg_name = FMLDeobfuscatingRemapper.INSTANCE.unmap(srg_name);
            }
            this.find_owner = owner.replace(".", "/");
            this.find_mcp_name = mcp_name;
            this.find_srg_name = srg_name;
            this.find_notch_name = notch_name;
            this.find_notch_desc = find_notch_desc;


            return this;
        }
        
        @Override
        void apply(MethodNode base, MethodNode addition) {
            find_mcp_desc = addition.desc; // Not used. Would have to convert desc from MCP to Notch, and there's no actual bug yet
            String find_desc = ASMTransformer.dev_environ ? find_mcp_desc : find_notch_desc;
            boolean any = false;
            InsnList instructions = base.instructions;
            for (AbstractInsnNode insn = instructions.getFirst(); insn != null; insn = insn.getNext()) {
                int op = insn.getOpcode();
                if (op != Opcodes.INVOKEVIRTUAL && op != Opcodes.INVOKESTATIC) {
                    continue;
                }
                MethodInsnNode meth = (MethodInsnNode) insn;
                String name = meth.name;
                if (!meth.desc.equals(find_desc)) continue;
                boolean match = meth.owner.equals(find_owner) && (name.equals(find_mcp_name) || name.equals(find_srg_name) || name.equals(find_notch_name));
                if (!match) {
                    continue;
                }
                meth.setOpcode(Opcodes.INVOKESTATIC);
                meth.owner = "factorization/coremod/MethodSplices";
                meth.name = addition.name;
                meth.desc = addition.desc;
                any = true;
            }
            if (!any) {
                throw new RuntimeException("Method mutation failed: did not find " + find_owner + " " + find_desc + " " + find_mcp_name + "/" + find_srg_name);
            }
        }
        
    }
}