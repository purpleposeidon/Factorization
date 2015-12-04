package factorization.notify;

import factorization.util.LangUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.EnumSet;

class ClientMessage {
    World world;
    Object locus;
    ItemStack item;
    String msg;
    EnumSet<Style> style;
    
    long creationTime;
    long lifeTime;
    boolean position_important = false;
    boolean show_item = false;
    
    static final int SHORT_TIME = 6, LONG_TIME = 11, VERY_LONG_TIME = 60;

    public ClientMessage(World world, Object locus, ItemStack item, String format, String... args) {
        this.world = world;
        this.locus = locus;
        this.item = item;
        this.msg = format;
        
        String[] parts = msg.split("\n", 2);
        style = NotifyImplementation.loadStyle(parts[0]);
        msg = parts[1];
        
        creationTime = System.currentTimeMillis();
        if (style.contains(Style.LONG)) {
            lifeTime = 1000 * LONG_TIME;
        } else {
            lifeTime = 1000 * SHORT_TIME;
        }
        position_important = style.contains(Style.EXACTPOSITION);
        show_item = style.contains(Style.DRAWITEM) && item != null;
        translate(args);
    }
    
    void translate(String... args) {
        msg = StatCollector.translateToLocal(msg);
        msg = msg.replace("\\n", "\n");
        
        String item_name = "null", item_info = "", item_info_newline = "";
        if (item != null) {
            item_name = item.getDisplayName();
            EntityPlayer player = Minecraft.getMinecraft().thePlayer;
            ArrayList<String> bits = new ArrayList();
            try {
                item.getItem().addInformation(item, player, bits, false);
            } catch (Throwable t) {
                t.printStackTrace();
                bits.add("" + EnumChatFormatting.RED + EnumChatFormatting.BOLD + "ERROR");
            }
            boolean tail = false;
            for (String s : bits) {
                if (tail) {
                    item_info += "\n";
                }
                tail = true;
                item_info += s;
            }
            item_info_newline = "\n" + item_info;
        }

        String[] cp = new String[args.length + 3];
        for (int i = 0; i < args.length; i++) {
            cp[i] = LangUtil.tryTranslate(args[i], args[i]);
        }
        cp[args.length] = item_name;
        cp[args.length + 1] = item_info;
        cp[args.length + 2] = item_info_newline;
        msg = msg.replace("{ITEM_NAME}", "%" + (args.length + 1) + "$s");
        msg = msg.replace("{ITEM_INFOS}", "%" + (args.length + 2) + "$s");
        msg = msg.replace("{ITEM_INFOS_NEWLINE}", "%" + (args.length + 3) + "$s");

        msg = String.format(msg, (Object[]) cp);
    }
    
    static double interp(double old, double new_, float partial) {
        return old*(1 - partial) + new_*partial;
    }

    Vec3 getPosition(float partial) {
        if (locus instanceof Vec3) {
            return (Vec3) locus;
        }
        if (locus instanceof Entity) {
            if (locus instanceof EntityMinecart) {
                partial = 1; // Wtf?
            }
            Entity e = ((Entity) locus);
            double w = e.width * -1.5;
            double eye_height = 4.0 / 16.0;
            if (e instanceof EntityLiving) {
                eye_height += e.getEyeHeight();
            }
            final double x = interp(e.prevPosX, e.posX, partial) + w / 2;
            final double y = interp(e.prevPosY, e.posY, partial) + eye_height;
            final double z = interp(e.prevPosZ, e.posZ, partial) + w / 2;
            return new Vec3(x, y, z);
        }
        if (locus instanceof TileEntity) {
            TileEntity te = ((TileEntity) locus);
            return new Vec3(te.pos.getX(), te.pos.getY(), te.pos.getZ());
        }
        if (locus instanceof ISaneCoord) {
            ISaneCoord c = (ISaneCoord) locus;
            return new Vec3(c.x(), c.y(), c.z());
        }
        return null;
    }

    boolean stillValid() {
        if (locus instanceof Entity) {
            Entity e = ((Entity) locus);
            return !e.isDead;
        }
        if (locus instanceof TileEntity) {
            TileEntity te = ((TileEntity) locus);
            return !te.isInvalid();
        }
        return true;
    }

    ISaneCoord asCoordMaybe() {
        if (locus instanceof ISaneCoord) {
            return (ISaneCoord) locus;
        }
        if (locus instanceof TileEntity) {
            TileEntity te = (TileEntity) locus;
            return new SimpleCoord(te.getWorld(), te.pos.getX(), te.pos.getY(), te.pos.getZ());
        }
        return null;
    }
}