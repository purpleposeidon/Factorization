package factorization.notify;

import java.util.ArrayList;
import java.util.EnumSet;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.StatCollector;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import factorization.api.Coord;
import factorization.notify.Notify.Style;

class Message {
    World world;
    Object locus;
    ItemStack item;
    String msg;
    EnumSet<Style> style;
    
    long creationTime;
    long lifeTime;
    boolean position_important = false;
    boolean show_item = false;

    public Message(World world, Object locus, ItemStack item, String format, String... args) {
        this.world = world;
        this.locus = locus;
        this.item = item;
        this.msg = format;
        
        String[] parts = msg.split("\n", 2);
        style = NotifyImplementation.loadStyle(parts[0]);
        msg = parts[1];
        
        creationTime = System.currentTimeMillis();
        lifeTime = 1000 * 6;
        if (style.contains(Style.LONG)) {
            lifeTime += 1000 * 5;
        }
        position_important = style.contains(Style.EXACTPOSITION);
        show_item = style.contains(Style.DRAWITEM) && item != null;
        translate(args);
    }
    
    void translate(String... args) {
        msg = StatCollector.translateToLocal(msg);
        for (int i = 0; i < args.length; i++) {
            String translated = StatCollector.translateToLocal(args[i]);
            args[i] = translated;
        }
        
        String item_name = "null", item_info = "", item_info_newline = "";
        if (item != null) {
            item_name = item.getDisplayName();
            EntityPlayer player = Minecraft.getMinecraft().thePlayer;
            ArrayList<String> bits = new ArrayList();
            item.getItem().addInformation(item, player, bits, false);
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
            cp[i] = args[i];
        }
        cp[args.length] = item_name;
        cp[args.length + 1] = item_info;
        cp[args.length + 2] = item_info_newline;
        msg = msg.replace("{ITEM_NAME}", "%" + (args.length + 1) + "$s");
        msg = msg.replace("{ITEM_INFOS}", "%" + (args.length + 2) + "$s");
        msg = msg.replace("{ITEM_INFOS_NEWLINE}", "%" + (args.length + 3) + "$s");

        try {
            msg = String.format(msg, (Object[]) cp);
        } catch (Exception e) {
            e.printStackTrace();
            msg = "FORMAT ERROR\n" + msg;
        }
    }
    
    static double interp(double old, double new_, float partial) {
        return old*(1 - partial) + new_*partial;
    }

    Vec3 getPosition(float partial) {
        if (locus instanceof Vec3) {
            return (Vec3) locus;
        }
        if (locus instanceof Entity) {
            Entity e = ((Entity) locus);
            double w = e.width*-1.5;
            double eye_height = e.getEyeHeight() + 4.0/16.0;
            return Vec3.createVectorHelper(
                    interp(e.prevPosX, e.posX, partial) + w/2,
                    interp(e.prevPosY, e.posY, partial) + eye_height,
                    interp(e.prevPosZ, e.posZ, partial) + w/2);
        }
        if (locus instanceof TileEntity) {
            TileEntity te = ((TileEntity) locus);
            return Vec3.createVectorHelper(te.xCoord, te.yCoord, te.zCoord);
        }
        if (locus instanceof Coord) {
            return ((Coord) locus).createVector();
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

    Coord asCoordMaybe() {
        if (locus instanceof Coord) {
            return (Coord) locus;
        }
        if (locus instanceof TileEntity) {
            return new Coord((TileEntity) locus);
        }
        return null;
    }
}