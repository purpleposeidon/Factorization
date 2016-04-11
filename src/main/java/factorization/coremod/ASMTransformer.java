package factorization.coremod;

import cpw.mods.fml.relauncher.FMLRelaunchLog;
import factorization.fzds.HammerEnabled;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;

public class ASMTransformer implements IClassTransformer {
    public static boolean dev_environ = Launch.blackboard != null ? (Boolean) Launch.blackboard.get("fml.deobfuscatedEnvironment") : false;

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        // Sorted by beauty!
        {
            // Why-isn't-this-in-Forge hooks stuff for unspecific FZ features

            // Add an event for unhandled key presses
            if (transformedName.equals("net.minecraft.client.gui.inventory.GuiContainer")) {
                return applyTransform(basicClass,
                        new AbstractAsmMethodTransform.Append(name, transformedName, "func_73869_a", "keyTyped")
                );
            }

            // Hack in a method to make diamond blocks explode into shards
            if (transformedName.equals("net.minecraft.block.Block")) {
                return applyTransform(basicClass,
                        new AbstractAsmMethodTransform.Append(name, transformedName, "func_149723_a", "onBlockDestroyedByExplosion"),
                        new AbstractAsmMethodTransform.Append(name, transformedName, "func_149659_a", "canDropFromExplosion")
                );
            }
        }

        {
            // FZDS-related hooks

            // Add an event to cancel attack/use key presses before anything else
            // (Should go in Forge)
            if (transformedName.equals("net.minecraft.client.Minecraft") && HammerEnabled.ENABLED) {
                return applyTransform(basicClass,
                        new AbstractAsmMethodTransform.Prepend(name, transformedName, "func_147116_af", "func_147116_af"), // "attack key pressed" function (first handler), MCPBot name clickMouse
                        new AbstractAsmMethodTransform.Prepend(name, transformedName, "func_147121_ag", "func_147121_ag") // "use key pressed" function, MCPBot name rightClickMouse
                );
            }
            // Make the camera take UCs into account in 3rd-person view
            // (Could go in forge, but is implemented specifically for FZDS)
            if (transformedName.equals("net.minecraft.client.renderer.EntityRenderer") && HammerEnabled.ENABLED) {
                try {
                    return applyTransform(basicClass,
                            new AbstractAsmMethodTransform.MutateCall(name, transformedName, "func_78467_g", "orientCamera")
                                    .setOwner("net.minecraft.client.multiplayer.WorldClient")
                                    .setName("rayTraceBlocks", "func_72933_a", "a")
                                    .setDescr("(Lnet/minecraft/util/Vec3;Lnet/minecraft/util/Vec3;)Lnet/minecraft/util/MovingObjectPosition;", "(Lazw;Lazw;)Lazu;")
                    );
                } catch (RuntimeException e) {
                    log("WARNING: Transformation to " + transformedName + " failed!");
                    log("This is lame, but ignorable: the camera will pass through solid FZDS blocks in 3rd-person view.");
                    e.printStackTrace();
                }
            }
            // Add "Universal Colliders". Adds a list of entities to the chunk that are added to every collision query.
            // The alternative to this is setting World.MAX_ENTITY_RADIUS to a large value, and putting collodier-entities everywhere, which is slow & ugly
            if (transformedName.equals("net.minecraft.world.chunk.Chunk") && HammerEnabled.ENABLED) {
                return applyTransform(basicClass,
                        new AbstractAsmClassTransform.Mixin("factorization.coremodhooks.MixinExtraChunkData", "Lfactorization/coremodhooks/MixinExtraChunkData;"),
                        new AbstractAsmMethodTransform.Append(name, transformedName, "func_76588_a", "getEntitiesWithinAABBForEntity")
                );
            }
            // Allow the player to stand on a UC without getting kicked from the server
            if (transformedName.equals("net.minecraft.world.World") && HammerEnabled.ENABLED) {
                try {
                    return applyTransform(basicClass,
                            new AbstractAsmMethodTransform.Append(name, transformedName, "func_72829_c", "checkBlockCollision"));
                } catch (RuntimeException e) {
                    log("WARNING: Transformation to " + transformedName + " failed!");
                    log("This is lame, but (kind-of?) ignorable: you may be kicked for flying when standing on FZDS blocks ");
                    e.printStackTrace();
                }
            }
            // (This... might not be entirely necessary. I didn't test this problem enough!)
            // Something about limiting the velocity of an entity when it's being influenced by multiple IDCs moving in the same direction...
            // Is this just vestigial? Shouldn't cause too much trouble tho.
            if (transformedName.equals("net.minecraft.entity.Entity") && HammerEnabled.ENABLED) {
                return applyTransform(basicClass,
                        new AbstractAsmClassTransform.Mixin("factorization.coremodhooks.MixinEntityKinematicsTracker", "Lfactorization/coremodhooks/MixinEntityKinematicsTracker;"));
            }
            // Don't let IDCs be knocked backwards; fixes a vanilla bug
            if (transformedName.equals("net.minecraft.world.Explosion") && HammerEnabled.ENABLED) {
                return applyTransform(basicClass,
                        new AbstractAsmMethodTransform.MutateCall(name, transformedName, "func_77278_a", "doExplosionA")
                                // find(String owner, String srg_name, String mcp_name, String notch_name, String find_notch_desc)
                                .setOwner("net.minecraft.enchantment.EnchantmentProtection")
                                .setName("func_92092_a", "func_92092_a", "a")
                                .setDescr("(Lnet/minecraft/entity/Entity;D)D", "(Lsa;D)D")
                );
            }
            // Sigh. :/ needed to keep minimap mods happy...
            if (transformedName.equals("net.minecraft.client.multiplayer.WorldClient") && HammerEnabled.ENABLED) {
                return applyTransform(basicClass,
                        new AbstractAsmMethodTransform.MutateCall(name, transformedName, "<init>", "<init>")
                                .setOwner("cpw.mods.fml.common.eventhandler.EventBus")
                                .setName("post", "post", "post")
                                .setDescr("(Lcpw/mods/fml/common/eventhandler/Event;)Z", "(Lcpw/mods/fml/common/eventhandler/Event;)Z")
                );
            }
        }
        {
            // Just for funnsies
            if (transformedName.equals("net.minecraft.block.BlockRailBase") && !Boolean.parseBoolean(System.getProperty("fz.disableRailStairs"))) {
                return applyTransform(basicClass,
                        new AbstractAsmClassTransform.Mixin("factorization.coremodhooks.MixinRailStairs", "Lfactorization/coremodhooks/MixinRailStairs;"));
            }
        }
        {
            // Skyboy special request
            // cofh/tweak/asmhooks/HooksCore#CoFHTweaks_FZ_Hook(chunk, entity, bb, collidingBoundingBoxes)
            if (transformedName.equals("cofh.tweak.asmhooks.HooksCore")) {
                String n = "CoFHTweaks_FZ_Hook";
                return applyTransform(basicClass, new AbstractAsmMethodTransform.Append(name, transformedName, n, n));
            }

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

    static String renameMethod(String name, String className) {
        if (!name.equals("<init>")) return name;
        return className.replace(".", "$") + "$init";
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
                    MethodNode method = getMethod(renameMethod(change.srgName, change.obfClassName));
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
        int flags = 0; // FIXME: Troubles running with intellij? Different JVM or something?
        if (dev_environ) flags |= ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES;
        ClassWriter cw = new ClassWriter(cr, flags);
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
