package factorization.misc;

import net.minecraft.command.CommandException;
import net.minecraft.command.CommandHelp;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SafeCommandHelp extends CommandHelp {

    @Override
    public String getCommandName() {
        return "safe-help";
    }

    @Override
    public String getCommandUsage(ICommandSender player) {
        return "/safe-help -- Crash resistant /help";
    }

    @Override
    public List getCommandAliases() {
        return null;
    }

    @Override
    protected List getSortedPossibleCommands(ICommandSender player) {
        List b = super.getSortedPossibleCommands(player);
        ArrayList<SafetyWrap> ret = new ArrayList<SafetyWrap>(b.size());
        for (Object c : b) {
            ret.add(new SafetyWrap((ICommand) c));
        }
        return ret;
    }

    @Override
    protected Map getCommands() {
        Map<String, ICommand> b = super.getCommands();
        HashMap<String, SafetyWrap> ret = new HashMap<String, SafetyWrap>(b.size());
        for (Map.Entry<String, ICommand> c : b.entrySet()) {
            ret.put(c.getKey(), new SafetyWrap(c.getValue()));
        }
        return ret;
    }

    @Override
    public void processCommand(ICommandSender player, String[] args) throws CommandException{
        try {
            super.processCommand(player, args);
        } catch (CommandException t) {
            throw t;
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    static class SafetyWrap implements ICommand {
        final ICommand base;

        SafetyWrap(ICommand base) {
            this.base = base;
        }

        @Override
        public String getCommandName() {
            try {
                String name = base.getCommandName();
                if (name == null || name.length() == 0) {
                    return "<Unnamed command: " + base.getClass() + ">";
                }
                return base.getCommandName();
            } catch (Throwable t) {
                t.printStackTrace();
                return "<Command with erroring name: " + base.getClass() + ">";
            }
        }

        @Override
        public String getCommandUsage(ICommandSender player) {
            try {
                String usage = base.getCommandUsage(player);
                if (usage == null || usage.length() == 0) {
                    return "<Command with no usage:" + base.getClass() + ">";
                }
                return usage;
            } catch (Throwable t) {
                return "<Command with erroring usage: " + base.getClass() + ">";
            }
        }

        @Override
        public List getCommandAliases() {
            return base.getCommandAliases();
        }

        @Override
        public void processCommand(ICommandSender sender, String[] args) throws CommandException {
            // Doesn't happen.
            // Even if it did happen, it isn't reasonable to catch & ignore any exceptiosn it might throw.
            base.processCommand(sender, args);
        }

        @Override
        public boolean canCommandSenderUseCommand(ICommandSender player) {
            try {
                return base.canCommandSenderUseCommand(player);
            } catch (Throwable t) {
                t.printStackTrace();
                return false;
            }
        }

        @Override
        public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
            try {
                return base.addTabCompletionOptions(sender, args, pos);
            } catch (Throwable t) {
                // Uh. Yeah, not sure.
                t.printStackTrace();
                return null;
            }
        }

        @Override
        public boolean isUsernameIndex(String[] args, int id) {
            try {
                return base.isUsernameIndex(args, id);
            } catch (Throwable t) {
                t.printStackTrace();
                return false;
            }
        }

        @Override
        public int compareTo(ICommand obj) {
            try {
                return base.getClass().getName().compareTo(obj.getClass().getName());
            } catch (Throwable t) {
                final int a = System.identityHashCode(base);
                final int b = System.identityHashCode(obj);
                if (a == b) return 0;
                if (a > b) return +1;
                return -1;
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof SafetyWrap) {
                obj = ((SafetyWrap) obj).base;
            }
            return base.equals(obj);
        }
    }
}
