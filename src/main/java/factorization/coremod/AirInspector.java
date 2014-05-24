package factorization.coremod;

import net.minecraft.launchwrapper.IClassTransformer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class AirInspector implements IClassTransformer {
    boolean prefix = false;
    
    void log(String msg) {
        System.err.println("[AirInspector] " + msg);
    }

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        ClassReader cr = new ClassReader(basicClass);
        ClassNode cn = new ClassNode();
        cr.accept(cn, 0);
        for (MethodNode method : cn.methods) {
            AbstractInsnNode insn = method.instructions.getFirst();
            while (insn != null) {
                if (checkInstructions(insn)) {
                    if (!prefix) {
                        prefix = true;
                        log("Brought to you by Factorization! \"Please use block.isAir() rather than block == Blocks.air\"");
                        log("And by...");
                        log("Powdermilk biscuits! In the big blue box with the picture of the biscuit on the cover in the bag with the brown stains that indicate freshness.");
                        log("Heavens! They're tasty and expeditious!");
                    }
                    log(name + "." + method.name + "()");
                    break;
                }
                insn = insn.getNext();
            }
        }
        return basicClass;
    }
    
    static final String dev_block = "Lnet/minecraft/block/Block;", obf_block = "Lahu;";
    
    boolean checkInstructions(AbstractInsnNode the_instruction) {
        if (the_instruction.getOpcode() != Opcodes.GETSTATIC) return false;
        FieldInsnNode insn = (FieldInsnNode) the_instruction;
        if (insn.desc.equals(dev_block)) {
            if (insn.name.equals("field_150350_a") || insn.name.equals("air")) {
                return true;
            }
        }
        return false;
    }

}
