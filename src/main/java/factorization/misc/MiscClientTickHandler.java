package factorization.misc;

import java.text.DateFormat;
import java.util.*;

import factorization.api.ICoordFunction;
import factorization.util.FzUtil;
import factorization.util.ItemUtil;
import factorization.util.PlayerUtil;
import factorization.weird.TileEntityDayBarrel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.world.World;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import factorization.api.Coord;
import factorization.common.FzConfig;
import net.minecraft.world.chunk.Chunk;

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
        fix_mc2713();
    }
    
    int count = 0;
    boolean hit = false;
    private void emitLoadAlert() {
        if (hit) return;
        if (count == 40) {
            //playing any earlier doesn't seem to work (sound is probably loaded in a separate thread?)
            if (mc.currentScreen instanceof GuiMainMenu) {
                mc.getSoundHandler().playSound(PositionedSoundRecord.func_147674_a(new ResourceLocation("gui.button.press"), 1.0F));
            }
            hit = true;
            LagssieWatchDog.start();
        }
        count++;
    }

    private ItemStack[] swaps = new ItemStack[9];
    
    boolean wasClicked = false;
    private void checkPickBlockKey() {
        if (!mc.gameSettings.keyBindPickBlock.getIsKeyPressed()) {
            wasClicked = false;
            return;
        }
        if (wasClicked) {
            return;
        }
        wasClicked = true;
        EntityPlayer player = mc.thePlayer;
        if (player == null) {
            return;
        }
        if (PlayerUtil.isPlayerCreative(player)) {
            // I suppose we could try pulling from the player's inventory.
            // And creative mode inventories tend to get super-ugly cluttered with duplicate crap...
            // But it doesn't work. Oh well.
            return;
        }
        if (mc.currentScreen != null) {
            return;
        }
        MovingObjectPosition mop = mc.objectMouseOver;
        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            return;
        }
        Coord here = new Coord(player.worldObj, mop);
        List<ItemStack> validItems = new ArrayList<ItemStack>();
        final ItemStack held = player.getHeldItem();
        if (vanillaSatisfied(mop, here, player)) {
            if (held == null) return;
            ItemStack replace = swaps[player.inventory.currentItem];
            if (replace == null) return;
            boolean already_found = false;
            for (int i = 0; i < 9; i++) {
                ItemStack is = player.inventory.getStackInSlot(i);
                if (is == null) continue;
                if (ItemUtil.couldMerge(is, replace)) {
                    already_found = true;
                    break;
                }
            }
            if (!already_found) {
                validItems.add(replace);
            }
        }
        // Search the inventory for the exact block. Failing that, search for the broken version
        if (validItems.isEmpty()) {
            validItems.add(here.getPickBlock(mop));
            validItems.add(here.getBrokenBlock());
            validItems.add(new ItemStack(here.getId(), 1, here.getMd()));
            ItemStack b = FzUtil.getReifiedBarrel(here);
            if (b != null) validItems.add(b);
        }
        int firstEmpty = -1;
        if (held == null) {
            firstEmpty = player.inventory.currentItem;
        }
        for (int i = 0; i < player.inventory.mainInventory.length; i++) {
            for (ItemStack needle : validItems) {
                if (needle == null) {
                    continue;
                }
                ItemStack hay = player.inventory.mainInventory[i];
                if (hay == null && firstEmpty == -1 && i < 9) {
                    firstEmpty = i;
                }
                if (hay == null || !ItemUtil.couldMerge(needle, hay)) {
                    continue;
                }
                if (i < 9) {
                    player.inventory.currentItem = i;
                    return;
                }
                if (firstEmpty != -1) {
                    player.inventory.currentItem = firstEmpty;
                }
                int targetSlot = player.inventory.currentItem;
                mc.playerController.windowClick(player.inventoryContainer.windowId, i, targetSlot, 2, player);
                if (held != null) {
                    if (swaps[player.inventory.currentItem] == null) {
                        swaps[player.inventory.currentItem] = held;
                    } else {
                        boolean canReplace = false;
                        for (int bar = 0; bar < 9; bar++) {
                            ItemStack barItem = player.inventory.getStackInSlot(bar);
                            if (barItem == null) continue;
                            if (barItem == swaps[player.inventory.currentItem]) {
                                canReplace = true;
                                break;
                            }
                        }
                        if (canReplace) {
                            swaps[player.inventory.currentItem] = held;
                        }
                    }
                }
                return;
            }
        }
    }
    
    private boolean vanillaSatisfied(MovingObjectPosition mop, Coord here, EntityPlayer player) {
        ItemStack held = player.inventory.getStackInSlot(player.inventory.currentItem);
        if (held == null) return false;
        final ItemStack pickBlock = here.getPickBlock(mop);
        if (pickBlock != null && ItemUtil.couldMerge(held, pickBlock)) {
            return true;
        }
        final ItemStack brokenBlock = here.getBrokenBlock();
        if (brokenBlock != null && ItemUtil.couldMerge(held, brokenBlock)) {
            return true;
        }
        return false;
    }
    
    static KeyBinding sprint = new KeyBinding("Sprint (FZ)", 0, "key.categories.movement");
    static {
        ClientRegistry.registerKeyBinding(sprint);
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
        final boolean state = sprint.getIsKeyPressed();
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
        if (!mc.isFullScreen()) return;
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

    void fix_mc2713() { // NORELEASE: Seems to be fixed in 1.8. Are we in 1.8?
        World world = mc.theWorld;
        if (world == null || world.loadedEntityList == null) return;
        if (!chunkChanged()) return;
        int d = 16 * 3 / 2;
        final HashSet<Entity> properly_known_entities = new HashSet<Entity>();
        Coord at = new Coord(mc.thePlayer);
        Coord.iterateChunks(at.add(-d, -d, -d), at.add(d, d, d), new ICoordFunction() {
            @Override
            public void handle(Coord here) {
                final Chunk chunk = here.getChunk();
                if (chunk == null) return;
                for (List l : chunk.entityLists) {
                    if (l == null) continue;
                    properly_known_entities.addAll((Collection<Entity>) l);
                }
            }
        });
        nextEntity:
        for (Entity ent : (Iterable<Entity>) world.loadedEntityList) {
            int ecx = MathHelper.floor_double(ent.posX / 16.0D);
            int ecz = MathHelper.floor_double(ent.posZ / 16.0D);
            int dx = (last_chunk_x - ecx);
            int dz = (last_chunk_z - ecz);
            int dSq = dx * dx + dz * dz;
            boolean near = dSq <= 4;
            if (!near) continue nextEntity;

            Chunk chunk = world.getChunkFromChunkCoords(ecx, ecz);
            if (chunk.entityLists[ent.chunkCoordY].size() < 16) {
                for (Entity e : (Iterable<Entity>) chunk.entityLists[ent.chunkCoordY]) {
                    if (e == ent) continue nextEntity;
                }
            } else if (properly_known_entities.contains(ent)) {
                continue nextEntity;
            }
            chunk.addEntity(ent);
        }
    }
}
