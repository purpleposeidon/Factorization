package factorization.misc;

import java.text.DateFormat;
import java.util.*;

import factorization.api.ICoordFunction;
import factorization.util.ItemUtil;
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
    
    boolean wasClicked = false;
    private void checkPickBlockKey() {
        if (!mc.gameSettings.keyBindPickBlock.getIsKeyPressed()) {
            wasClicked = false;
            return;
        }
        EntityPlayer player = mc.thePlayer;
        if (player == null) {
            return;
        }
        if (player.capabilities.isCreativeMode) {
            return;
        }
        if (mc.currentScreen != null) {
            return;
        }
        if (wasClicked) {
            return;
        }
        wasClicked = true;
        MovingObjectPosition mop = mc.objectMouseOver;
        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            return;
        }
        Coord here = new Coord(player.worldObj, mop);
        if (vanillaSatisfied(mop, here, player)) {
            return;
        }
        // Search the inventory for the exact block. Failing that, search for the broken version
        List<ItemStack> validItems = Arrays.asList(here.getPickBlock(mop), here.getBrokenBlock(), new ItemStack(here.getId(), 1, here.getMd()));
        int firstEmpty = -1;
        if (player.getHeldItem() == null) {
            firstEmpty = player.inventory.currentItem;
        }
        for (int i = 0; i < player.inventory.mainInventory.length; i++) {
            for (ItemStack needle : validItems) {
                if (needle == null) {
                    continue;
                }
                ItemStack is = player.inventory.mainInventory[i];
                if (is == null && firstEmpty == -1 && i < 9) {
                    firstEmpty = i;
                }
                if (is == null || !ItemUtil.couldMerge(needle, is)) {
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
                return;
            }
        }
    }
    
    private boolean vanillaSatisfied(MovingObjectPosition mop, Coord here, EntityPlayer player) {
        ItemStack held = player.inventory.getStackInSlot(player.inventory.currentItem);
        if (held == null) {
            return false;
        }
        if (ItemUtil.couldMerge(held, here.getPickBlock(mop))) {
            return true;
        }
        if (ItemUtil.couldMerge(held, here.getBrokenBlock())) {
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
        if (!chunkChanged()) return;
        World world = mc.theWorld;
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
