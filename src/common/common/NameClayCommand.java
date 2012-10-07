package factorization.common;

import net.minecraft.src.CommandBase;
import net.minecraft.src.EntityPlayer;
import net.minecraft.src.ICommandSender;
import net.minecraft.src.ItemStack;
import net.minecraft.src.NBTTagCompound;

public class NameClayCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "nameclay";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (!(sender instanceof EntityPlayer)) {
            sender.sendChatToPlayer("You are not logged in");
            return;
        }
        String name = "";
        boolean first = true;
        for (String s : args) {
            if (!first) {
                name += " ";
            }
            first = false;
            name += s;
        }
        EntityPlayer player = (EntityPlayer) sender;
        ItemStack is = player.getCurrentEquippedItem();
        if (is != null && is.isItemEqual(Core.registry.greenware_item)) {
            NBTTagCompound tag = is.getTagCompound();
            if (tag != null && tag.hasKey("parts")) {
                tag.setString("sculptureName", name);
                return;
            }
        }
        player.sendChatToPlayer("That item can not be named.");
    }

    @Override
    public String getCommandUsage(ICommandSender par1iCommandSender) {
        return super.getCommandUsage(par1iCommandSender) + " name for held sculpture";
    }
}
