package factorization.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import net.minecraft.src.EntityPlayer;
import net.minecraft.src.InventoryPlayer;
import net.minecraft.src.ItemStack;
import factorization.api.ExoStateShader;
import factorization.api.ExoStateType;

public enum Command {
    bagShuffle(1), craftClear(2), craftMove(3), craftBalance(4), craftOpen(5, true),
    bagShuffleReverse(6), exoKeyOn(7, true), exoKeyOff(8, true), exoModLeftClick(9),
    exoModRightClick(10);

    static class name {
        static HashMap<Byte, Command> map = new HashMap();
    }

    public byte id;
    boolean executeLocally = false;

    Command(int id) {
        this.id = (byte) id;
        name.map.put(this.id, this);
    }

    Command(int id, boolean executeLocally) {
        this(id);
        this.executeLocally = executeLocally;
    }

    static void fromNetwork(EntityPlayer player, byte s, byte arg) {
        Command c = name.map.get(s);
        if (c == null) {
            System.err.println("Received invalid command #" + s);
            return;
        }
        c.call(player, arg);
    }

    public void call(EntityPlayer player) {
        call(player, (byte) 0);
    }

    public void call(EntityPlayer player, byte arg) {
        if (player == null) {
            return;
        }
        if (player.worldObj.isRemote) {
//			if (this == craftOpen && player.inventoryContainer != null) {
//				((EntityClientPlayerMP)player).closeScreen();
//			}
            Core.network.sendCommand(player, this, arg);
            if (!executeLocally) {
                return;
            }
        }
        switch (this) {
        case bagShuffle:
            Core.registry.bag_of_holding.useBag(player, false);
            break;
        case bagShuffleReverse:
            Core.registry.bag_of_holding.useBag(player, true);
            break;
        case craftClear:
            // move items from pocket crafting area into rest of inventory, or into a bag
            craftClear(player);
            break;
        case craftMove:
            // do something smart with items in crafting area
            craftMove(player);
            break;
        case craftBalance:
            // move as many items as we can to fill in template in crafting area
            craftBalance(player);
            break;
        case craftOpen:
            Core.registry.pocket_table.tryOpen(player);
            break;
        case exoKeyOff:
        case exoKeyOn:
            Core.exoCore.buttonPressed(player, arg, this == exoKeyOn);
            break;
        case exoModLeftClick:
        case exoModRightClick:
            if (player.openContainer instanceof ContainerExoModder) {
                ContainerExoModder cont = (ContainerExoModder) player.openContainer;
                ItemStack armor = cont.upgrader.armor;
                ExoArmor m = (ExoArmor) armor.getItem();
                int slot = arg / 2;
                boolean changeExoType = 0 == (arg % 2);
                int deltaDirection = this == exoModLeftClick ? 1 : -1;
                ExoStateType mst = m.getExoStateType(armor, slot);
                ExoStateShader mss = m.getExoStateShader(armor, slot);
                if (changeExoType) {
                    do {
                        mst = FactorizationUtil.shiftEnum(mst, ExoStateType.values(), deltaDirection);
                    } while (!mst.armorRestriction.canUse(m.armorType));
                    m.setExoStateType(armor, slot, mst);
                } else {
                    //changeExoShader
                    if (mst == ExoStateType.NEVER) {
                        //Only use the first two, as the others don't make sense for a constant.
                        if (mss != ExoStateShader.NORMAL) {
                            mss = ExoStateShader.NORMAL;
                        } else {
                            mss = ExoStateShader.INVERSE;
                        }
                    } else {
                        mss = FactorizationUtil.shiftEnum(mss, ExoStateShader.values(), deltaDirection);
                    }
                    m.setExoStateShader(armor, slot, mss);
                }
            }
            break;
        default:
            throw new RuntimeException("Command " + this + " is missing handler");
        }
    }
    
    void craftClear(EntityPlayer player) {
        if (!(player.openContainer instanceof ContainerPocket)) {
            return;
        }
        ContainerPocket pocket = (ContainerPocket) player.openContainer;
        for (int i : pocket.craftArea) {
            //transferStackInSlot
            pocket.transferStackInSlot(player, i);
        }
    }
    
    private boolean rotateAll(InventoryPlayer inv, int slots[]) {
        int empty = 0;
        for (int slot : slots) {
            if (FactorizationUtil.normalize(inv.getStackInSlot(slot)) == null) {
                empty++;
            }
        }
        if (empty >= 2) {
            return false;
        }
        ArrayList<ItemStack> buffer = new ArrayList(8);
        for (int slot : slots) {
            ItemStack toAdd = inv.getStackInSlot(slot);
            buffer.add(buffer.size(), toAdd);
        }
        buffer.add(0, buffer.remove(buffer.size() - 1));
        for (int slot : slots) {
            ItemStack toSet = buffer.remove(0);
            inv.setInventorySlotContents(slot, toSet);
        }
        return true;
    }
    
    private boolean smear(InventoryPlayer inv, int slots[]) {
        int stackSrcSlotIndex = 0;
        boolean foundNonEmpty = false;
        for (int slot : slots) {
            if (inv.getStackInSlot(slot) != null) {
                foundNonEmpty = true;
                continue;
            }
            if (!foundNonEmpty) {
                continue;
            }
            //loop around looking for something that's spreadable
            ItemStack toDrop = null;
            for (int count = 0; count < slots.length; count++) {
                ItemStack here = inv.getStackInSlot(slots[stackSrcSlotIndex]);
                stackSrcSlotIndex++;
                if (stackSrcSlotIndex == slots.length) {
                    stackSrcSlotIndex = 0;
                }
                if (here == null || here.stackSize <= 1) {
                    continue;
                }
                toDrop = here;
                break;
            }
            if (toDrop == null) {
                return true;
            }
            inv.setInventorySlotContents(slot, toDrop.splitStack(1));
        }
        return true;
    }
    
    void craftMove(EntityPlayer player) {
        InventoryPlayer inv = player.inventory;
        //spin the crafting grid
        int slots[] = {15, 16, 17, 26, 35, 34, 33, 24};
        try {
            if (rotateAll(inv, slots)) {
                return;
            }
            if (smear(inv, slots)) {
                return;
            }			
        } finally {
            if (player.openContainer instanceof ContainerPocket) {
                ((ContainerPocket) player.openContainer).updateMatrix();
            }
        }
    }
    
    void craftBalance(EntityPlayer player) {
        class Accumulator {
            ItemStack toMatch;
            int stackCount = 0;
            ArrayList<Integer> matchingSlots = new ArrayList(9);
            public Accumulator(ItemStack toMatch, int slot) {
                this.toMatch = toMatch;
                stackCount = toMatch.stackSize;
                toMatch.stackSize = 0;
                matchingSlots.add(slot);
            }
            
            boolean add(ItemStack ta, int slot) {
                if (toMatch.isItemEqual(ta)) {
                    stackCount += ta.stackSize;
                    ta.stackSize = 0;
                    matchingSlots.add(slot);
                    return true;
                }
                return false;
            }
        }
        InventoryPlayer inv = player.inventory;
        int slots[] = {15, 16, 17, 24, 25, 26, 33, 34, 35};
        ArrayList<Accumulator> list = new ArrayList(9);
        for (int slot : slots) {
            ItemStack here = inv.getStackInSlot(slot);
            if (here == null || here.stackSize == 0) {
                continue;
            }
            boolean found = false;
            for (Accumulator acc : list) {
                if (acc.add(here, slot)) {
                    found = true;
                }
            }
            if (!found) {
                list.add(new Accumulator(here, slot));
            }
        }
        
        for (Accumulator acc : list) {
            int delta = acc.stackCount/acc.matchingSlots.size(); //this should be incapable of being 0
            delta = Math.min(delta, 1); //...we'll make sure anyways.
            for (int slot : acc.matchingSlots) {
                if (acc.stackCount <= 0) {
                    break;
                }
                inv.getStackInSlot(slot).stackSize = delta;
                acc.stackCount -= delta;
            }
            //we now may have a few left over, which we'll distribute
            while (acc.stackCount > 0) {
                for (int slot : acc.matchingSlots) {
                    if (acc.stackCount <= 0) {
                        break;
                    }
                    inv.getStackInSlot(slot).stackSize++;
                    acc.stackCount--;
                }
            }
        }
        
        if (player.openContainer instanceof ContainerPocket) {
            ((ContainerPocket) player.openContainer).updateMatrix();
        }
    }
}