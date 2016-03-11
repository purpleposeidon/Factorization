package factorization.misc;

import com.google.common.collect.Lists;
import factorization.api.Coord;
import factorization.common.FzConfig;
import factorization.util.FzUtil;
import factorization.util.ItemUtil;
import factorization.util.PlayerUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.world.World;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.text.DateFormat;
import java.util.*;

public class MiscClientTickHandler {
    private final Minecraft mc = Minecraft.getMinecraft();
    
    @SubscribeEvent
    public void clientTicks(ClientTickEvent event) {
        if (event.phase != Phase.START) return;
        if (event.type != TickEvent.Type.CLIENT) return;
        emitLoadAlert();
        checkPickBlockKey();
        checkSprintKey();
        MiscClientCommands.tick();
        notifyTimeOnFullScreen();
    }
    
    int count = 0;
    boolean hit = false;
    private void emitLoadAlert() {
        if (hit) return;
        if (count == 40) {
            //playing any earlier doesn't seem to work (sound is probably loaded in a separate thread?)
            if (mc.currentScreen instanceof GuiMainMenu) {
                mc.getSoundHandler().playSound(PositionedSoundRecord.create(new ResourceLocation("gui.button.press"), 1.0F));
            }
            hit = true;
            LagssieWatchDog.start();
            setupPickBlockKey();
        }
        count++;
    }

    private ItemStack[] swaps = new ItemStack[9];

    boolean was_activated = true;
    private void checkPickBlockKey() {
        if (!FzConfig.fix_middle_click) return;
        EntityPlayer player = mc.thePlayer;
        if (player == null) {
            return;
        }
        boolean keyPressed = pickBlock.isKeyDown();
        if (!keyPressed) {
            was_activated = false;
            return;
        }
        if (was_activated) return;
        was_activated = true;
        if (mc.currentScreen != null) {
            return;
        }
        if (PlayerUtil.isPlayerCreative(player)) {
            // I suppose we could try pulling from the player's inventory.
            // And creative mode inventories tend to get super-ugly cluttered with duplicate crap...
            // But it doesn't work. :(
            mc.middleClickMouse();
            return;
        }
        MovingObjectPosition mop = mc.objectMouseOver;
        if (mop == null) return;
        List<ItemStack> validItems = new ArrayList<ItemStack>();
        int slot = player.inventory.currentItem;
        if (swaps.length != InventoryPlayer.getHotbarSize()) {
            swaps = new ItemStack[InventoryPlayer.getHotbarSize()];
        }
        if (slot >= 0 && 0 < swaps.length && swaps[slot] != null) {
            ItemStack origSwap = swaps[slot];
            swaps[slot] = null;
            boolean movedDown = false;
            for (int i = 0; i < InventoryPlayer.getHotbarSize(); i++) {
                if (ItemUtil.identical(origSwap, player.inventory.getStackInSlot(i))) {
                    movedDown = true;
                    break;
                }
            }
            if (!movedDown) validItems.add(origSwap);
            // If the NBT changes (eg, drill recharging) it's still the same ItemStack object, right? Not sure.
            // If the object changes, then this code'll have difficulties.
        }
        if (mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            Coord here = Coord.fromMop(player.worldObj, mop);
            validItems.add(here.getPickBlock(mop, player));
            validItems.add(here.getBrokenBlock());
            validItems.add(new ItemStack(here.getBlock(), 1, here.getMd()));
            ItemStack b = FzUtil.getReifiedBarrel(here);
            if (b != null) validItems.add(b);
        } else if (mop.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY) {
            validItems.add(mop.entityHit.getPickedResult(mop));
        }

        final ItemStack held = player.getHeldItem();
        for (Iterator<ItemStack> it = validItems.iterator(); it.hasNext(); ) {
            // Don't match items that you're holding.
            if (ItemUtil.identical(it.next(), held)) it.remove();
        }
        if (validItems.isEmpty()) return;

        int firstEmpty = -1;
        if (held == null) {
            firstEmpty = player.inventory.currentItem;
        }
        // Find a valid stack. If it's on the hotbar, change the hotbar index; else move it to an appropriate space.
        for (int invSlot = 0; invSlot < player.inventory.mainInventory.length; invSlot++) {
            for (ItemStack needle : validItems) {
                if (needle == null) continue;
                ItemStack hay = player.inventory.mainInventory[invSlot];
                if (hay == null && firstEmpty == -1 && invSlot < 9) {
                    firstEmpty = invSlot;
                }
                if (hay == null || !ItemUtil.couldMerge(needle, hay)) {
                    continue;
                }
                if (invSlot < 9) {
                    player.inventory.currentItem = invSlot;
                    return;
                }
                if (firstEmpty != -1) {
                    player.inventory.currentItem = firstEmpty;
                }
                swapify(player, held, invSlot);
                return;
            }
        }
    }

    private void swapify(EntityPlayer player, ItemStack held, int invSlot) {
        int targetSlot = player.inventory.currentItem;
        mc.playerController.windowClick(player.inventoryContainer.windowId, invSlot, targetSlot, 2, player);
        if (held == null) return;
        if (swaps[targetSlot] == null) {
            swaps[targetSlot] = held;
            return;
        }
        boolean canReplace = false;
        for (int barSlot = 0; barSlot < 9; barSlot++) {
            ItemStack barItem = player.inventory.getStackInSlot(barSlot);
            if (barItem == null) continue;
            if (barItem == swaps[targetSlot]) {
                canReplace = true;
                break;
            }
        }
        if (canReplace) {
            swaps[targetSlot] = held;
        }
    }

    static KeyBinding sprint = new KeyBinding("Sprint (FZ)", 0, "key.categories.movement");
    static KeyBinding pickBlock = new KeyBinding("Pick Block (FZ)", 0, "key.categories.gameplay");
    static {
        ClientRegistry.registerKeyBinding(sprint);
        if (FzConfig.fix_middle_click) {
            ClientRegistry.registerKeyBinding(pickBlock);
        }
    }

    static void setupPickBlockKey() {
        if (!FzConfig.fix_middle_click) return;
        GameSettings gs = Minecraft.getMinecraft().gameSettings;
        if (gs.keyBindPickBlock.getKeyCode() != 0 && pickBlock.getKeyCode() == 0) {
            pickBlock.setKeyCode(gs.keyBindPickBlock.getKeyCode());
            gs.keyBindPickBlock.setKeyCode(0);
            MiscClientCommands.miscCommands.savesettings();
        }
    }

    boolean prevState = false;
    private void checkSprintKey() {
        if (mc.currentScreen != null) {
            return;
        }
        if (mc.thePlayer == null) {
            return;
        }
        if (sprint.getKeyCode() == 0) {
            return;
        }
        final boolean state = sprint.isKeyDown();
        final int forwardCode = mc.gameSettings.keyBindForward.getKeyCode();
        if (state) {
            if (!mc.thePlayer.isSneaking()) {
                KeyBinding.setKeyBindState(forwardCode, true);
                mc.thePlayer.setSprinting(true);
            }
        } else if (prevState) {
            KeyBinding.setKeyBindState(forwardCode, false);
            mc.thePlayer.setSprinting(false);
        }
        prevState = state;
    }
    
    long old_now = -1;
    long interval = 30;
    long getNow() {
        World world = Minecraft.getMinecraft().theWorld;
        if (world == null) return -1;
        Calendar cal = world.getCurrentDate();
        return cal.get(Calendar.MINUTE) / interval;
    }
    
    String last_msg = null;
    boolean mentioned_disabling = false;
    
    public void notifyTimeOnFullScreen() {
        if (!FzConfig.show_time_on_fullscreen) return;
        if (interval <= 0) return;
        long now = getNow();
        if (now == old_now || now == -1) return;
        old_now = now;
        Minecraft mc = Minecraft.getMinecraft();
        if (!mc.isFullScreen()) {
            DisplayMode desktop = Display.getDesktopDisplayMode();
            if (desktop.getWidth() != mc.displayWidth || desktop.getHeight() != mc.displayHeight) {
                return;
            }
        }
        DateFormat df = DateFormat.getTimeInstance(DateFormat.SHORT);
        String msg = df.format(new Date());
        if (!mentioned_disabling) {
            msg += " (via FZ)";
            mentioned_disabling = true;
        }
        ChatStyle style = new ChatStyle().setItalic(true).setColor(EnumChatFormatting.GRAY);
        mc.ingameGUI.getChatGUI().printChatMessageWithOptionalDeletion(new ChatComponentText(msg).setChatStyle(style), 20392);
        last_msg = msg;
    }

    int last_chunk_x = Integer.MAX_VALUE, last_chunk_z = Integer.MAX_VALUE;
    boolean chunkChanged() {
        EntityPlayer player = Minecraft.getMinecraft().thePlayer;
        if (player == null) return false;
        if (last_chunk_x == player.chunkCoordX && last_chunk_z == player.chunkCoordZ) return false;
        last_chunk_x = player.chunkCoordX;
        last_chunk_z = player.chunkCoordZ;
        return true;
    }

    /* Used to have a fix for MC-2713 (being unable to interact with entities if you walk a medium ways away from a
    chunk and come back. It seems to be fixed in 1.8.
     */
}
