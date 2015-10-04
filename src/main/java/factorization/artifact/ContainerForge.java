package factorization.artifact;

import factorization.api.Coord;
import factorization.shared.Core;
import factorization.shared.NetworkFactorization;
import factorization.util.InvUtil;
import factorization.util.ItemUtil;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StringUtils;

import java.util.ArrayList;

public class ContainerForge extends Container {
    final Coord orig;
    public final InventoryForge forge;
    final EntityPlayer player;

    int player_slot_start, player_slot_end;
    public int invdx, invdy;
    int slot_start, slot_end;
    ArrayList<Slot> playerSlots = new ArrayList<Slot>();
    ArrayList<Slot> toolSlots = new ArrayList<Slot>();
    ArrayList<Slot> potencySlots = new ArrayList<Slot>();
    ArrayList<Slot> enchantSlots = new ArrayList<Slot>();
    ArrayList<Slot> dyeSlots = new ArrayList<Slot>();

    @Override
    public ItemStack transferStackInSlot(EntityPlayer player, int slotIndex) {
        Slot slot = getSlot(slotIndex);
        if (playerSlots.contains(slot)) {
            ItemStack is = slot.getStack();
            if (ItemUtil.is(is, Core.registry.item_potency)) {
                return InvUtil.transferSlotToSlots(player, slot, potencySlots);
            }
            if (TileEntityLegendarium.isTool(is)) {
                return InvUtil.transferSlotToSlots(player, slot, toolSlots);
            }
            if (InventoryForge.getDyeIndex(is) >= 0) {
                return InvUtil.transferSlotToSlots(player, slot, dyeSlots);
            }
            return InvUtil.transferSlotToSlots(player, slot, enchantSlots);
        }
        return InvUtil.transferSlotToSlots(player, slot, playerSlots);
    }

    public ContainerForge(Coord orig, EntityPlayer player) {
        this.orig = orig;
        this.forge = new InventoryForge(player, this);
        this.player = player;
        invdx = -4;
        invdy = 10;
        addPlayerSlots(player.inventory);
        int dx = -4 - 15, dy = -3;
        s(new ArrayList(), new SlotSane(forge, InventoryForge.SLOT_OUT, 80 + dx, 71 + dy));
        s(toolSlots, new SlotSane(forge, InventoryForge.SLOT_FIRST, 52 + dx, 71 + dy));
        s(toolSlots, new SlotSane(forge, InventoryForge.SLOT_SECOND, 108 + dx, 71 + dy));
        s(potencySlots, new SlotSane(forge, InventoryForge.SLOT_POTENT_START + 0, 80 + dx, 38 + dy));
        s(potencySlots, new SlotSane(forge, InventoryForge.SLOT_POTENT_START + 1, 67 + dx, 102 + dy));
        s(potencySlots, new SlotSane(forge, InventoryForge.SLOT_POTENT_START + 2, 94 + dx, 102 + dy));
        for (int i = InventoryForge.SLOT_ENCHANT_START; i < InventoryForge.SLOT_ENCHANT_END; i++) {
            s(enchantSlots, new SlotSane(forge, i, 0, 20 * (i - InventoryForge.SLOT_ENCHANT_START)));
        }
        s(dyeSlots, new SlotSane(forge, InventoryForge.SLOT_DYE_1, 152 + 15 + dx, 134 + dy));
    }

    void s(ArrayList<Slot> list, Slot slot) {
        list.add(slot);
        addSlotToContainer(slot);
    }

    @Override
    public boolean canInteractWith(EntityPlayer player) {
        return orig.distanceSq(new Coord(player)) < 36;
    }


    void addPlayerSlots(InventoryPlayer inventoryplayer) {
        int d = 18 * 3 + 4;
        invdy += d;
        player_slot_start = inventorySlots.size();
        for (int row = 0; row < 3; row++) {
            for (int c = 0; c < 9; c++) {
                final Slot slot = new Slot(inventoryplayer, c + row * 9 + 9, invdx + 8 + c * 18, invdy + 84 + row * 18);
                s(playerSlots, slot);
            }
        }

        for (int c = 0; c < 9; c++) {
            final Slot slot = new Slot(inventoryplayer, c, invdx + 8 + c * 18, invdy + 142);
            playerSlots.add(slot);
            addSlotToContainer(slot);
        }
        player_slot_end = inventorySlots.size();
        slot_start = 0;
        slot_end = player_slot_start;
        invdy -= d;
    }

    public void onContainerClosed(EntityPlayer player) {
        super.onContainerClosed(player);

        if (player.worldObj.isRemote) return;
        for (int i = 0; i < InventoryForge.SIZE; ++i) {
            if (i == InventoryForge.SLOT_OUT) continue;
            ItemStack is = forge.getStackInSlot(i);
            if (is != null) {
                player.dropPlayerItemWithRandomChoice(is, false);
            }
        }
    }

    @Override
    public void putStackInSlot(int slot, ItemStack stack) {
        super.putStackInSlot(slot, stack);
        detectAndSendChanges();
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        if (player.worldObj.isRemote) return;
        String new_err = forge.error_message == null ? "" : forge.error_message;
        Core.network.sendPlayerMessage(player, NetworkFactorization.MessageType.ArtifactForgeError, new_err, forge.warnings);
    }
}
