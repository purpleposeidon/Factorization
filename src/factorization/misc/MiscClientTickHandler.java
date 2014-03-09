package factorization.misc;

import java.util.Arrays;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.ResourceLocation;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import factorization.api.Coord;
import factorization.shared.Core;
import factorization.shared.FzUtil;

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
    }
    
    int count = 0;
    boolean hit = false;
    private void emitLoadAlert() {
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
        if (!mc.gameSettings.keyBindPickBlock.isPressed()) {
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
                if (is == null || !FzUtil.couldMerge(needle, is)) {
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
        if (FzUtil.couldMerge(held, here.getPickBlock(mop))) {
            return true;
        }
        if (FzUtil.couldMerge(held, here.getBrokenBlock())) {
            return true;
        }
        return false;
    }
    
    static KeyBinding sprint = new KeyBinding("Sprint (FZ)", 0, "key.categories.gameplay");
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
}
