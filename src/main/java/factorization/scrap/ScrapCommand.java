package factorization.scrap;

import com.google.common.base.Joiner;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ScrapCommand extends CommandBase {
    @Override
    public String getCommandName() {
        return "scrap";
    }

    @Override
    public String getCommandUsage(ICommandSender user) {
        return "/scrap (list | reload | undo | reset | Action …)";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        String action;
        if (args.length > 0) {
            action = args[0];
        } else {
            action = "list";
        }
        if (action.equalsIgnoreCase("undo")) {
            sender.addChatMessage(new ChatComponentText(ScrapManager.undo()));
        } else if (action.equalsIgnoreCase("reload")) {
            sender.addChatMessage(new ChatComponentText(ScrapManager.reload()));
        } else if (action.equalsIgnoreCase("reset")) {
            sender.addChatMessage(new ChatComponentText(ScrapManager.reset()));
        } else if (action.equalsIgnoreCase("list")) {
            for (Map.Entry<String, Class<? extends IRevertible>> e : ScrapManager.actionClasses.entrySet()) {
                String name = e.getKey();
                Class<? extends IRevertible> val = e.getValue();
                Help help = val.getAnnotation(Help.class);
                String[] lines;
                if (help == null) {
                    lines = new String[] { "(no help)" };
                } else {
                    lines = help.value();
                }
                boolean first = true;
                for (String line : lines) {
                    if (first) {
                        sender.addChatMessage(new ChatComponentText(name + ": " + line));
                        first = false;
                    } else {
                        sender.addChatMessage(new ChatComponentText("  " + line));
                    }
                }
            }
        } else {
            sender.addChatMessage(new ChatComponentText(ScrapManager.call(Joiner.on(" ").join(args))));
        }
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
        if (args.length <= 0) return null;
        if (args.length > 1) return null;
        ArrayList<String> all = new ArrayList<String>(ScrapManager.actionClasses.keySet());
        all.addAll(Arrays.asList("undo", "reload", "reset", "list"));
        String head = args[0];
        ArrayList<String> ret = new ArrayList<String>();
        for (String string : all) {
            if (string.startsWith(head)) ret.add(string);
        }
        return ret;
    }
}
