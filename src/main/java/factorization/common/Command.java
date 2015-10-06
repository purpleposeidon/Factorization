package factorization.common;

import java.util.HashMap;

import factorization.misc.ItemMover;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import factorization.shared.Core;
import factorization.weird.ContainerPocket;

public enum Command {
    craftClear(2, true), craftSwirl(3, true), craftBalance(4, true), craftOpen(5, true), craftFill(11, true),
    gooRightClick(12, false), gooLeftClick(13, false), gooSelectNone(14, false),
    itemTransferUp(15, true), itemTransferDown(16, true), itemTransferLeft(17, true), itemTransferRight(18, true),
    itemTransferUpShift(19, true), itemTransferDownShift(20, true), itemTransferLeftShift(21, true), itemTransferRightShift(22, true);

    static class Names {
        static HashMap<Byte, Command> map = new HashMap<Byte, Command>();
    }

    public byte id;
    boolean executeLocally = false;
    public Command reverse = this;

    Command(int id, boolean executeLocally) {
        this.id = (byte) id;
        this.executeLocally = executeLocally;
        if (Names.map.put(this.id, this) != null) {
            throw new RuntimeException("Duplicate command IDs for " + this.id);
        }
    }
    
    void setReverse(Command rev) {
        rev.reverse = this;
        this.reverse = rev;
    }

    public static void fromNetwork(EntityPlayer player, byte s, int arg) {
        Command c = Names.map.get(s);
        if (c == null) {
            Core.logWarning("Received invalid command #" + s);
            return;
        }
        c.call(player, arg);
    }

    public void call(EntityPlayer player) {
        call(player, 0);
    }

    public void call(EntityPlayer player, int arg) {
        if (player == null) {
            return;
        }
        if (player.worldObj.isRemote) {
            Core.network.sendCommand(player, this, arg);
            if (!executeLocally) {
                return;
            }
        }
        switch (this) {
        case craftClear:
        case craftSwirl:
        case craftBalance:
        case craftFill:
            if (player.openContainer instanceof ContainerPocket) {
                ((ContainerPocket) player.openContainer).executeCommand(this, arg);
            }
            break;
        case craftOpen:
            Core.registry.pocket_table.tryOpen(player);
            break;
        case gooLeftClick:
        case gooRightClick:
        case gooSelectNone:
            if (player instanceof EntityPlayerMP) {
                Core.registry.utiligoo.executeCommand(this, (EntityPlayerMP) player);
            }
            break;
        case itemTransferDown:
            ItemMover.moveItems(player, arg, -1);
            break;
        case itemTransferUp:
            ItemMover.moveItems(player, arg, +1);
            break;
        case itemTransferRight:
            ItemMover.moveItems(player, arg, -4);
            break;
        case itemTransferLeft:
            ItemMover.moveItems(player, arg, +4);
            break;
        case itemTransferDownShift:
            ItemMover.moveItems(player, arg, -10);
            break;
        case itemTransferUpShift:
            ItemMover.moveItems(player, arg, +10);
            break;
        case itemTransferRightShift:
            ItemMover.moveItems(player, arg, -16);
            break;
        case itemTransferLeftShift:
            ItemMover.moveItems(player, arg, +16);
            break;
        default:
            Core.logWarning("Command " + this + " is missing handler");
        }
    }
    
}
