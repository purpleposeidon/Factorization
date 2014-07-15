package factorization.notify;

import java.util.EnumSet;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class Notice {
    final Object where;
    private String message;
    private String[] messageParameters;
    private ItemStack item;
    private EnumSet<Style> style = EnumSet.noneOf(Style.class);
    private NoticeUpdater updater;

    private boolean isUpdating = false;
    private int age = 0;
    private boolean changed = false;
    private boolean changedItem = false;
    private boolean addedToRecurList = false;
    EntityPlayer targetPlayer = null;
    World world;

    private static final String[] emptyStringArray = new String[0];

    /**
     * Creates an in-world notification message, which is sent using
     * {@link sendTo} or {@link sendToEveryone}. Additional options can be
     * provided using {@link withItem}, {@link withStyle}, {@link withWorld},
     * and {@link withUpdater}. Don't forget to send the Notice!<br>
     * <code><pre>
     * Notice msg = new Notice(oldManEntity, "%s", "It's dangerous to go alone!\nTake this!");
     * msg.withItem(new ItemStack(Items.iron_sword)).sendTo(player); 
     * </pre></code>
     * 
     * @param where
     *            An {@link Entity}, {@link TileEntity}, {@link Vec3}, or
     *            {@link ISaneCoord} (eg {@link SimpleCoord}, or FZ's Coord)
     * @param message
     *            The message to be sent.
     * 
     *            <p>
     *            The message will be translated, and then the translated
     *            message and the messageParameters will be passed through
     *            {@link String.format}. All translations happen client-side.
     *            </p>
     *            <p>
     *            Newlines work as expected.
     *            </p>
     * @param messageParameters
     *            The format parameters.
     * 
     */

    public Notice(Object where, String message, String... messageParameters) {
        this.where = where;
        this.message = message;
        this.messageParameters = messageParameters;
        if (where instanceof ISaneCoord) {
            world = ((ISaneCoord) where).w();
        } else if (where instanceof Entity) {
            world = ((Entity) where).worldObj;
        } else if (where instanceof TileEntity) {
            world = ((TileEntity) where).getWorldObj();
        }
    }
    
    /**
     * Creates a new Notice.
     * 
     * <pre>
     * <code>
     * new Notice(somewhere, new NoticeUpdater() {
     *     void update(Notice msg) {
     *         msg.setMessage("%s", System.currentTimeMillis()/1000);
     *     }
     * }).sendTo(someone);
     * </code>
     * </pre>
     * 
     * @param where
     *            An {@link Entity}, {@link TileEntity}, {@link Vec3}, or
     *            {@link ISaneCoord} (eg {@link SimpleCoord}, or FZ's Coord)
     * 
     */
    public Notice(Object where, NoticeUpdater updater) {
        this.where = where;
        withUpdater(updater);
        updater.update(this);
    }
    
    /**
     * <p>
     * Sets a single item to be sent along with the message. It can be used in
     * two ways.
     * </p>
     * 
     * <p>
     * If {@link withStyle}({@link Style.DRAWITEM}) is used, then the item will
     * be drawn in the notification.
     * </p>
     * 
     * <p>
     * The item's name can be inserted in the message's text using the format
     * codes <code>{ITEM_NAME}</code>, <code>{ITEM_INFOS}</code>, or <code>{ITEM_INFOS_NEWLINE}</code>.
     * </p>
     * <p>
     * <code>{ITEM_NAME}</code>
     * will be replaced with the name of the item, and is gotten by calling
     * {@link ItemStack.getDisplayName}. <code>{ITEM_INFOS}</code> and <code>{ITEM_INFOS_NEWLINE</code> are
     * gotten via {@link Items.addInformation}. ITEM_INFOS_NEWLINE is prefixed
     * with a newline, unless the information list is empty.
     * </p>
     */
    public Notice withItem(ItemStack item) {
        if (isUpdating && !changed) {
            cmpIs(this.item, item);
            changedItem |= changed;
        }
        this.item = item == null ? null : item.copy();
        return this;
    }

    /**
     * Sets the {@link Style}s for the message. See {@link Style} for details.
     */
    public Notice withStyle(Style... styles) {
        boolean addedStyle = false;
        for (Style s : styles) {
            addedStyle |= style.add(s);
        }
        if (addedStyle && isUpdating) {
            this.changed = true;
        }
        return this;
    }

    /**
     * Sets the world of the Notice. (This is only needed for sending a
     * notification at a Vec3 position to everyone;)
     */
    public Notice withWorld(World world) {
        this.world = world;
        return this;
    }

    /**
     * Schedules a recurring notification.
     * <code>{@link NoticeUpdater.update}(this)</code> will be called until no
     * longer necessary.
     */
    public Notice withUpdater(NoticeUpdater updater) {
        this.updater = updater;
        return this;
    }
    
    /**
     * Changes the message. This goes with the {@link NOticeUpdater} constructor.
     */
    public void setMessage(String newMessage, String... newMessageParameters) {
        cmp(this.message, newMessage);
        if (!changed && this.messageParameters != null && newMessageParameters != null) {
            if (this.messageParameters.length != newMessageParameters.length) {
                changed = true;
            } else {
                for (int i = 0; i < newMessageParameters.length; i++) {
                    cmp(newMessageParameters[i], this.messageParameters[i]);
                    if (changed) break;
                }
            }
        } else {
            cmp(messageParameters, newMessageParameters);
        }
        this.message = newMessage;
        this.messageParameters = newMessageParameters;
    }
    
    private void cmp(Object a, Object b) {
        if (a == b) return;
        if (a != null && b != null) {
            changed |= !a.equals(b);
        } else {
            changed |= a == b;
        }
    }
    
    private void cmpIs(ItemStack a, ItemStack b) {
        if (a == b) return;
        if (a != null && b != null) {
            changed |= !a.isItemEqual(b);
        } else {
            changed |= a == b;
        }
    }
    
    boolean isInvalid() {
        int maxAge = 20 * (style.contains(Style.LONG) ? ClientMessage.LONG_TIME : ClientMessage.SHORT_TIME);
        if (age++ > maxAge) {
            return true;
        }
        if (where instanceof Entity) {
            Entity ent = (Entity) where;
            if (ent.isDead) {
                return false;
            }
        } else if (where instanceof TileEntity) {
            TileEntity te = (TileEntity) where;
            if (te.isInvalid()) {
                return false;
            }
        } else if (where instanceof ISaneCoord) {
            ISaneCoord coord = (ISaneCoord) where;
            if (!coord.w().blockExists(coord.x(), coord.y(), coord.z())) {
                return false;
            }
        } else if (where instanceof Vec3 && world != null) {
            Vec3 vec = (Vec3) where;
            if (!world.blockExists((int) vec.xCoord, (int) vec.yCoord, (int) vec.zCoord)) {
                return false;
            }
        }
        if (targetPlayer != null) {
            return targetPlayer.isDead;
        }
        return false;

    }

    /**
     * Dispatches the Notice to the player. If the player is null, then all
     * players in the world will see it.
     */
    public void send(EntityPlayer player) {
        if (isUpdating) {
            // In this case, it is our responsibility. Shouldn't be called.
            return;
        }
        if (world == null && player != null) {
            world = player.worldObj;
        }
        NotifyImplementation.instance.doSend(player, where, world, style, item, message, messageParameters);
        changed = false;
        changedItem = false;
        if (updater != null && !addedToRecurList) {
            NotifyImplementation.instance.addRecuringNotification(this);
            targetPlayer = player;
            addedToRecurList = true;
        }
    }

    /**
     * Sends the Notice to everyone in the world.
     * @see sendTo
     */
    public void sendToAll() {
        send(null);
    }

    /**
     * Erases all Notifications a player has.
     */
    public static void clear(EntityPlayer player) {
        SimpleCoord at = new SimpleCoord(player.worldObj, (int) player.posX, (int) player.posY, (int) player.posZ);
        NotifyImplementation.instance.doSend(player, at, player.worldObj, EnumSet.of(Style.CLEAR), null, "", emptyStringArray);
    }

    /**
     * Sends an on-screen message, using Vanilla's mechanism for displaying the
     * minecart's "Press SHIFT to dismount" message.
     * (Unfortunately it kind of looks like crap because the text isn't shadowed. Oh well.)
     * 
     * @param player
     *            The player to be notified
     * @param message
     *            A string. The client will localize this message prior to
     *            displaying it.
     * @param formatArguments
     *            Optional string arguments for a format parameter.
     */
    public static void onscreen(EntityPlayer player, String message, String... formatArguments) {
        NotifyImplementation.instance.doSendOnscreenMessage(player, message, formatArguments);
    }

    boolean updateNotice() {
        if (updater == null)
            return false;
        if (targetPlayer != null && targetPlayer.isDead)
            return false;
        if (isUpdating)
            return false;
        isUpdating = true;
        updater.update(this);
        isUpdating = false;
        if (changed) {
            if (changedItem) {
                style.add(Style.UPDATE);
                send(targetPlayer);
                style.remove(Style.UPDATE);
            } else {
                style.add(Style.UPDATE_SAME_ITEM);
                send(targetPlayer);
                style.remove(Style.UPDATE_SAME_ITEM);
            }
            changed = changedItem = false;
        }
        return true;
    }

}
