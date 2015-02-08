package factorization.docs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import factorization.util.PlayerUtil;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;

public class ExportHtml implements ICommand {
    @Override
    public int compareTo(Object arg0) {
        if (arg0 instanceof ICommand) {
            ICommand other = (ICommand) arg0;
            return getCommandName().compareTo(other.getCommandName());
        }
        return 0;
    }

    @Override
    public String getCommandName() {
        return "html-fzdoc-export";
    }

    @Override
    public String getCommandUsage(ICommandSender icommandsender) {
        return "/html-fzdoc-export";
    }

    @Override
    public List getCommandAliases() { return null; }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender icommandsender) {
        return icommandsender instanceof EntityPlayer && PlayerUtil.isCommandSenderOpped(icommandsender);
    }

    @Override
    public List addTabCompletionOptions(ICommandSender icommandsender, String[] astring) { return null; }

    @Override
    public boolean isUsernameIndex(String[] astring, int i) { return false; }

    @Override
    public void processCommand(ICommandSender player, String[] args) {
        resetLinks();
        while (!frontier.isEmpty()) {
            String f = frontier.remove(0);
            try {
                processFile(f);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    void processFile(String filename) throws IOException {
        File outfile = new File("/tmp/fzdoc-html/" + filename + ".html");
        outfile.getParentFile().mkdirs();
        outfile.delete();
        OutputStream os = new FileOutputStream(outfile);
        PrintStream out = new PrintStream(os);
        out.println("<html>");
        out.println("<head>");
        out.println();
        out.println("</head>");
        out.println("<body>");
        try {
            HtmlConversionTypesetter conv = new HtmlConversionTypesetter(os);
            String text = DocumentationModule.readDocument(filename);
            conv.processText(text);
            out.println("</body>");
            out.println("</html>");
        } finally {
            if (os != null) {
                os.close();
            }
        }
    }
    
    static HashSet<String> visited = new HashSet();
    static ArrayList<String> frontier = new ArrayList();

    public static void visitLink(String newLink) {
        if (visited.contains(newLink)) {
            return;
        }
        frontier.add(newLink);
        visited.add(newLink);
    }
    
    public static void resetLinks() {
        visited.clear();
        frontier.clear();
        visitLink("index");
    }
}
