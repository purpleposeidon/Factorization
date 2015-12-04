package factorization.notify;

import com.google.common.base.Joiner;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

import java.util.EnumSet;

public class MutterCommand extends CommandBase {

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (!(sender instanceof TileEntity || sender instanceof Entity)) {
            return;
        }
        EnumSet theStyle = EnumSet.noneOf(Style.class);
        ItemStack heldItem = null;
        if (sender instanceof EntityLivingBase) {
            heldItem = ((EntityLivingBase) sender).getHeldItem();
        }
        ItemStack sendItem = null;
        for (int i = 0; i < args.length; i++) {
            String s = args[i];
            if (s.equalsIgnoreCase("--long")) {
                theStyle.add(Style.LONG);
            } else if (s.equalsIgnoreCase("--show-item") && heldItem != null) {
                theStyle.add(Style.DRAWITEM);
                sendItem = heldItem;
            } else {
                break;
            }
            args[i] = null;
        }
        String msg = Joiner.on(" ").skipNulls().join(args);
        msg = msg.replace("\\n", "\n");
        new Notice(sender, "%s", msg).withItem(sendItem).sendToAll();
    }
    
    @Override
    public String getCommandUsage(ICommandSender icommandsender) {
        return "/mutter [--long] [--show-item] some text. Clears if empty";
    }
    
    @Override
    public String getCommandName() {
        return "mutter";
    }
    
    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return sender instanceof Entity;
    }
    
    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }
    
    // o_ó eclipse has no trouble compiling without these two methods...
    public int compareTo(ICommand otherCmd) {
        return this.getCommandName().compareTo(otherCmd.getCommandName());
    }

    public int compareTo(Object obj) {
        return this.compareTo((ICommand)obj);
    }
}
