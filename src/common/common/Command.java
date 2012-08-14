package factorization.common;

import java.util.HashMap;

import cpw.mods.fml.common.FMLCommonHandler;

import net.minecraft.src.EntityPlayer;
import net.minecraft.src.ItemStack;
import factorization.common.MechaArmor.MechaMode;

public enum Command {
    bagShuffle(1), craftClear(2), craftMove(3), craftBalance(4), craftOpen(5, true),
    bagShuffleReverse(6), mechaKeyOn(7, true), mechaKeyOff(8, true), mechaModLeftClick(9),
    mechaModRightClick(10);

    static class name {
        static HashMap<Byte, Command> map = new HashMap();
    }

    public byte id;
    boolean shareCommand = false;

    Command(int id) {
        this.id = (byte) id;
        name.map.put(this.id, this);
    }

    Command(int id, boolean shareCommand) {
        this(id);
        this.shareCommand = shareCommand;
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
            Core.network.sendCommand(player, this, arg);
            if (!shareCommand) {
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
            break;
        case craftMove:
            // do something smart with items in crafting area
            break;
        case craftBalance:
            // move as many items as we can to fill in template in crafting area
            break;
        case craftOpen:
            Core.registry.pocket_table.tryOpen(player);
            break;
        case mechaKeyOff:
        case mechaKeyOn:
            Core.instance.putPlayerKey(player, arg, this == mechaKeyOn);
            break;
        case mechaModLeftClick:
        case mechaModRightClick:
            if (player.craftingInventory instanceof ContainerMechaModder) {
                ContainerMechaModder cont = (ContainerMechaModder) player.craftingInventory;
                ItemStack armor = cont.upgrader.armor;
                MechaArmor m = (MechaArmor) armor.getItem();
                int slot = arg / 2;
                boolean activationButton = 0 == (arg % 2);
                MechaMode mode = m.getSlotMechaMode(armor, slot);
                if (activationButton) {
                    if (this == mechaModLeftClick) {
                        mode.nextActivationMode(1);
                    }
                    else {
                        mode.nextActivationMode(-1);
                    }
                }
                else {
                    if (this == mechaModLeftClick) {
                        mode.nextKey(1);
                    }
                    else {
                        mode.nextKey(-1);
                    }
                }
                m.setSlotMechaMode(armor, slot, mode);
            }
            break;
        default:
            throw new RuntimeException("Command " + this + " is missing handler");
        }
    }
}