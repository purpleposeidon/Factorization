package factorization.common;

import factorization.api.Coord;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;

public class GeyserTest extends CommandBase {
    @Override
    public String getCommandName() {
        return "geyser";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/geyser";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        Coord at = new Coord(sender.getEntityWorld(), sender.getPosition());
        String arg = "help";
        if (args.length == 1) {
            arg = args[0];
        }
        if (arg.equals("spawn")) {
            new CopperGeyserGen().generateAt(at.w.rand, at);
        } else {
            sender.addChatMessage(new ChatComponentText("Usage: /geyser spawn"));
        }
    }
}
