package factorization.coremod;

import java.util.Iterator;

import net.minecraft.block.*;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import cpw.mods.fml.relauncher.SideOnly;

public class StripSideOnly implements IClassTransformer {

    private static final String LSideOnly = "L" + SideOnly.class.getCanonicalName().replace('.', '/') + ";";

    public static class BlockNewBaseClass {
        public int idPicked(World world, int x, int y, int z) {
            Block me = (Block) (Object) this;
            Block block = me;
            if (me instanceof BlockSign || me instanceof BlockFlowerPot || me instanceof BlockRedstoneWire || me instanceof BlockBrewingStand
                    || me instanceof BlockReed || me instanceof BlockTripWire || me instanceof BlockCauldron || me instanceof BlockRedstoneRepeater
                    || me instanceof BlockComparator || me instanceof BlockRedstoneTorch || me instanceof BlockFarmland || me instanceof BlockFurnace
                    || me instanceof BlockMushroomCap || me instanceof BlockRedstoneLight) {
                int md = world.getBlockMetadata(x, y, z);
                return me.idDropped(md, world.rand, 0);
            }
            if (me instanceof BlockCocoa || me instanceof BlockNetherStalk) {
                return 0;
            }
            if (block instanceof BlockPistonMoving || block instanceof BlockPortal || block instanceof BlockEndPortal || block instanceof BlockSilverfish
                    || block instanceof BlockMobSpawner) {
                return 0;
            }
            return me.blockID;
        }

        int tryCallIdPicked(Block me, World world, int x, int y, int z) {
            try {
                return me.idPicked(world, x, y, z);
            } catch (NoSuchMethodError e) {
                return me.blockID;
            }
        }

        public ItemStack getPickBlock(MovingObjectPosition target, World world, int x, int y, int z) {
            Block me = (Block) (Object) this;
            int id = me.blockID;
            try {
                id = tryCallIdPicked(me, world, x, y, z);
            } catch (NoSuchMethodError e) {
            }

            if (id == 0) {
                return null;
            }

            Item item = Item.itemsList[id];
            if (item == null) {
                return null;
            }

            return new ItemStack(id, 1, me.getDamageValue(world, x, y, z));
        }
    }

    @Override
    public byte[] transform(String name, String transformedName, byte[] original_bytes) {
        if (original_bytes == null) {
            return null;
        }
        // TODO: Rewrite to use the visitor interface instead; it's much faster
        ClassNode classNode = new ClassNode();
        ClassReader classReader = new ClassReader(original_bytes);
        classReader.accept(classNode, 0);
        if (transformedName.equals("net.minecraft.block.Block")) {
            ClassWriter writer = new ClassWriter(0);
            classNode.accept(writer);
            String superName = BlockNewBaseClass.class.getName();
            classNode.superName = superName;
            for (Iterator<MethodNode> iterator = classNode.methods.iterator(); iterator.hasNext();) {
                MethodNode methodNode = iterator.next();
                if (methodNode.name.equals("getPickBlock")) {
                    iterator.remove();
                    break;
                }
            }
            return writer.toByteArray();
        }
        boolean changes = false;

        // Remove @SideOnly
        for (MethodNode methodNode : classNode.methods) {
            final String mname = methodNode.name;
            final boolean target_method = mname.equals("idPicked") || mname.equals("func_71922_a") || mname.equals("getPickBlock") /*
                                                                                                                                     * unlikely
                                                                                                                                     * ,
                                                                                                                                     * but
                                                                                                                                     * just
                                                                                                                                     * in
                                                                                                                                     * case
                                                                                                                                     * (
                                                                                                                                     * forge
                                                                                                                                     * method
                                                                                                                                     * ,
                                                                                                                                     * so
                                                                                                                                     * no
                                                                                                                                     * srg
                                                                                                                                     * name
                                                                                                                                     * )
                                                                                                                                     */;
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
                    log("Removing @SideOnly from " + transformedName + "." + mname);
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

    static void log(String msg) {
        System.out.println(msg);
    }

}
