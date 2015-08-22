package factorization.truth.export;

import factorization.shared.Core;
import factorization.truth.DocumentationModule;
import factorization.util.PlayerUtil;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

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
        Core.logInfo("Processing: " + filename);
        String root = System.getProperty("fzdoc.webroot", "/FzDocs/");
        String outDir = System.getProperty("fzdoc.out", "/tmp/fzdoc-html/");
        File outfile = new File(outDir + filename + ".html");
        if (!outfile.getParentFile().mkdirs()) {
            throw new IOException("Failed to create output directory");
        }
        if (outfile.exists()) {
            if (!outfile.delete()) {
                throw new IOException("Unable to delete old file!");
            }
        }
        OutputStream os = new FileOutputStream(outfile);
        PrintStream out = new PrintStream(os);
        out.println("<!doctype html>");
        out.println("<html>");
        out.println("<head>");
        out.println("<meta charset=\"utf-8\"/>");
        out.println("<title>The Factorization Manual</title>");
        out.println("<link rel=\"stylesheet\" type=\"text/css\" href=\"" + root + "man.css\">");
        out.println();
        out.println("</head>");
        out.println("<body>");
        try {
            HtmlConversionTypesetter conv = new HtmlConversionTypesetter("factorization", os, root);
            String text = DocumentationModule.readDocument("factorization", filename);
            conv.processText(text);
        } finally {
            out.println("</body>");
            out.println("</html>");
            os.close();
        }
    }
    
    static HashSet<String> visited = new HashSet<String>();
    static ArrayList<String> frontier = new ArrayList<String>();

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
