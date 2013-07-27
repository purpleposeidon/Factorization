package factorization.notify;

import java.util.EnumSet;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Vec3;
import factorization.api.Coord;

public abstract class Notify {
    public static enum Style {
        /**
         * This message will not cause other nearby messages to be removed
         */
        FORCE,
        /**
         * This message will stay around for an extra 5 seconds
         */
        LONG,
        /**
         * This message will cause all messages to be removed (including itself)
         */
        CLEAR,
        /**
         * If the message is for a {@link Coord} or {@link TileEntity}, then it will not take the block's bounding box into consideration
         */
        EXACTPOSITION,
        /**
         * This message will have its item drawn on the right side.
         */
        DRAWITEM
    }
    
    /**
     * @return if Factorization Notify is installed. Users will have to use
     *         some other means of notification, such as {@link EntityPlayer.addChatMessage},
     *         if this is not installed.
     */
    public static boolean enabled() {
        return instance != null;
    }
    
    
    /**
     * Sends an in-world notification message.
     * 
     * @param player
     *            The player to send the notification to. If null, it will be
     *            sent to everyone nearby.
     * @param where
     *            An {@link Entity}, {@link TileEntity}, {@link Vec3}, or
     *            {@link Coord}
     * @param style
     *            An EnumSet of {@link Style}. May be null, or empty.
     * @param item
     *            The item used by {@link Style.DRAWITEM}, or with a
     *            format substitution. Can be null.
     * @param format
     *            The message to be sent.
     * 
     *            <p>
     *            format and args are passed to String.format after being
     *            translated. All translations happen client-side.
     *            </p>
     *            <p>
     *            The item's name can be inserted here using "{ITEM_NAME}",
     *            "{ITEM_INFOS}", or "{ITEM_INFOS_NEWLINE}". ITEM_NAME will be
     *            replaced with the name of the item, and is gotten by calling
     *            {@link ItemStack.getDisplayName}. ITEM_INFOS and
     *            ITEM_INFOS_NEWLINE are gotten via {@link Item.addInformation}.
     *            ITEM_INFOS_NEWLINE is prefixed with a newline, unless the
     *            information list is empty.
     *            </p>
     * @param args
     * @return 
     */
    public static boolean send(EntityPlayer player, Object where, EnumSet<Style> style, ItemStack item, String format, String... args) {
        if (instance == null) {
            return false;
        }
        instance.doSend(player, where, style, item, format, args);
        style = noStyle;
        item = null;
        return true;
    }
    
    
    public static boolean send(Object where, String msg, String... args) {
        if (instance == null) {
            return false;
        }
        instance.doSend(null, where, style, item, msg, args);
        style = noStyle;
        item = null;
        return true;
    }
    
    public static boolean send(EntityPlayer player, Object where, String msg, String... args) {
        if (instance == null) {
            return false;
        }
        instance.doSend(player, where, style, item, msg, args);
        style = noStyle;
        item = null;
        return true;
    }
    
    public static void withItem(ItemStack is) {
        item = is;
    }
    
    public static void withStyle(Style...args) {
        style = EnumSet.noneOf(Style.class);
        for (int i = 0; i < args.length; i++) {
            style.add(args[i]);
        }
    }
    
    public static void clear(EntityPlayer player) {
        instance.doSend(player, new Coord(player), EnumSet.of(Style.CLEAR), null, "", emptyArray);
    }
    
    
    
    
    protected static Notify instance;
    protected abstract void doSend(EntityPlayer player, Object where, EnumSet<Style> style, ItemStack item, String format, String[] args);
    
    private static EnumSet<Style> noStyle = EnumSet.noneOf(Style.class);
    private static String[] emptyArray = new String[0];
    
    private static ItemStack item = null;
    private static EnumSet<Style> style = noStyle;
}
