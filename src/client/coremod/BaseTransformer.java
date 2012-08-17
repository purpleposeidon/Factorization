package factorization.client.coremod;

import cpw.mods.fml.relauncher.IClassTransformer;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin;
import cpw.mods.fml.relauncher.RelaunchClassLoader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.EventBus;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.MethodNode;

public class BaseTransformer implements IClassTransformer, IFMLLoadingPlugin {
    public byte[] transform(String name, byte[] bytes) {
        if ((!name.equals("net.minecraft.src.GuiContainer")) && (!name.equals("aqg"))) {
            return bytes;
        }
        ClassReader cr = new ClassReader(bytes);
        ClassNode cn = new ClassNode();
        cr.accept(cn, 0);

        boolean found = false;
        for (MethodNode m : (List<MethodNode>)cn.methods) {
            if (((m.name.equals("keyTyped")) || (m.name.equals("a")))
                    && (m.desc.equals("(CI)V"))) {
                appendMethod(m, getMyMethod());
                found = true;
                break;
            }
        }

        if (!found) {
            throw new RuntimeException("Unable to find GuiScreen.keyTyped! Did the obfuscations change?");
        }

        ClassWriter cw = new ClassWriter(3);
        cn.accept(cw);
        return cw.toByteArray();
    }

    void appendMethod(MethodNode base, MethodNode toAppend) {
        AbstractInsnNode base_end = base.instructions.getLast();
        while (base_end.getOpcode() != 177) {
            base_end = base_end.getPrevious();
        }
        AbstractInsnNode ins = toAppend.instructions.getFirst();
        while (ins != null) {
            AbstractInsnNode next = ins.getNext();
            if (!(ins instanceof LineNumberNode)) {
                base.instructions.insertBefore(base_end, ins);
            }
            ins = next;
        }
        base.instructions.remove(base_end);
    }

    void printInstructions(AbstractInsnNode ins) {
        while (ins != null) {
            System.out.print(ins.getOpcode() + "  ");
            if ((ins instanceof JumpInsnNode)) {
                System.out.println("Jump to " + ((JumpInsnNode) ins).label.getLabel());
            } else if ((ins instanceof LabelNode)) {
                System.out.println("Label: " + ((LabelNode) ins).getLabel());
            } else {
                System.out.println(ins);
            }
            ins = ins.getNext();
        }
    }

    MethodNode getMyMethod() {
        ClassReader cr = new ClassReader(getMyBytecode());
        ClassNode cn = new ClassNode();
        cr.accept(cn, 0);
        for (MethodNode m : (List<MethodNode>)cn.methods) {
            if (m.name.equals("append__keyTyped")) {
                return m;
            }
        }
        return null;
    }

    byte[] getMyBytecode() {
        RelaunchClassLoader rcl = (RelaunchClassLoader) getClass()
                .getClassLoader();
        try {
            return rcl.getClassBytes(getClass().getCanonicalName());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    void append__keyTyped(char par1, int par2) {
        if (MinecraftForge.EVENT_BUS.post(new GuiKeyEvent(par1, par2)))
            return;
    }

    public String[] getLibraryRequestClass() {
        return new String[0];
    }

    public String[] getASMTransformerClass() {
        return new String[] { getClass().getCanonicalName() };
    }

    public String getModContainerClass() {
        return null;
    }

    public String getSetupClass() {
        return null;
    }

    public void injectData(Map data) {
    }
}
