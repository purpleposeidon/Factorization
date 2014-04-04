package factorization.misc;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import cpw.mods.fml.client.GuiModList;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.common.FzConfig;
import factorization.shared.Core;
import factorization.shared.FzUtil;

public class MiscClientCommands implements ICommand {
    static final Minecraft mc = Minecraft.getMinecraft();
    static int queued_action = 0;
    static int queue_delay = 0;
    static final int SHOW_MODS_LIST = 1, CLEAR_CHAT = 2;
    
    public int compareTo(ICommand other) {
        return this.getCommandName().compareTo(other.getCommandName());
    }

    @Override
    public int compareTo(Object obj) {
        return this.compareTo((ICommand) obj);
    }

    @Override
    public String getCommandName() {
        return "f";
    }

    @Override
    public List getCommandAliases() {
        return null;
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender player) {
        return true;
    }

    
    @Override
    public boolean isUsernameIndex(String[] astring, int i) {
        return false;
    }
    
    
    
    
    @Retention(value = RUNTIME)
    @Target(value = METHOD)
    static @interface alias {
        public String[] value();
    }
    
    @Retention(value = RUNTIME)
    @Target(value = METHOD)
    static @interface sketchy { }
    
    @Retention(value = RUNTIME)
    @Target(value = METHOD)
    static @interface cheaty { }
    
    @Retention(value = RUNTIME)
    @Target(value = METHOD)
    static @interface help {
        public String value();
    }
    
    public static class miscCommands {
        static EntityClientPlayerMP player;
        static String arg0, arg1;
        static List<String> args;
        
        @alias({"date", "time"})
        @help("Show the real-world time")
        public static String now() {
            return Calendar.getInstance().getTime().toString();
        }
        
        @alias({"help", "?"})
        public static void about() {
            player.addChatMessage(new ChatComponentText("Miscellaneous Client Commands; from Factorization, by neptunepink"));
            player.addChatMessage(new ChatComponentText("Use /f list go see the sub-commands."));
        }
        
        @help("Lists available subcommands. Can also search the list.")
        public static void list() {
            String em = "" + EnumChatFormatting.GREEN;
            for (Method method : miscCommands.class.getMethods()) {
                if (!commandAllowed(method)) {
                    continue;
                }
                
                String msg = em + method.getName() + EnumChatFormatting.RESET;
                alias a = method.getAnnotation(alias.class);
                if (a != null) {
                    for (String v : a.value()) {
                        msg += ", " + em + v + EnumChatFormatting.RESET;
                    }
                }
                help h = method.getAnnotation(help.class);
                if (h != null) {
                    msg += ": " + h.value();
                }
                if (method.getAnnotation(sketchy.class) != null) {
                    msg += EnumChatFormatting.DARK_GRAY + " [SKETCHY]";
                }
                if (method.getAnnotation(cheaty.class) != null) {
                    msg += EnumChatFormatting.RED + " [CHEATY]";
                }
                if (arg1 == null || arg1.length() == 0 || msg.contains(arg1)) {
                    player.addChatMessage(new ChatComponentText(msg));
                }
            }
            String msg = "";
            boolean first = true;
            for (String v : new String[] {"0", "1", "2", "3", "4", "+", "-"}) {
                if (!first) {
                    msg += ", ";
                }
                first = false;
                msg += em + v + EnumChatFormatting.RESET;
            }
            msg += ": " + "Changes the fog";
            if (arg1 == null || arg1.length() == 0 || msg.contains(arg1)) {
                player.addChatMessage(new ChatComponentText(msg));
            }
        }
        
        @alias({"cl"})
        @help("Erases the chat window")
        public static void clear() {
            queued_action = CLEAR_CHAT;
        }
        
        @sketchy
        @help("Reveals your coordinates in-chat")
        public static void saycoords() {
            player.sendChatMessage("/me is at " + ((int) player.posX) + ", "
                    + ((int) player.posY) + ", " + ((int) player.posZ) + " in dimension "
                    + player.worldObj.provider.dimensionId);
        }
        
        @alias({"ss"})
        @help("Saves game settings. (Vanilla seems to need help with this.)")
        public static String savesettings() {
            mc.gameSettings.saveOptions();
            return "Saved settings";
        }
        
        @alias({"render_everything_lagfest"})
        @help("Render a ton of terrain at once (may lock your game up for a while)")
        public static void render_above() {
            WorldRenderer[] lizt = mc.renderGlobal.sortedWorldRenderers;
            int did = 0;
            int total = 0;
            boolean lagfest = arg0.contains("lagfest");
            for (WorldRenderer wr : lizt) {
                total++;
                if (wr.needsUpdate) {
                    if (wr.posY - 16*3 > mc.thePlayer.posY && wr.posY < mc.thePlayer.posY + 16*8 || lagfest) {
                        wr.updateRenderer(mc.thePlayer);
                        wr.needsUpdate = false;
                        did++;
                    }
                }
            }
            player.addChatMessage(new ChatComponentText("Rendered " + did + " chunks out of " + total));
        }
        
        @alias("c")
        @help("Switch between creative and survival mode")
        public static void creative() {
            //Not sketchy since you wouldn't be able to run it anyways.
            player.sendChatMessage("/gamemode " + (player.capabilities.isCreativeMode ? 0 : 1));
        }
        
        @alias({"n", "makenice"})
        @help("Makes it a sunny morning")
        public static void nice() {
            if (player.worldObj.isRaining()) {
                player.sendChatMessage("/weather clear");
            }
            double angle =player.worldObj.getCelestialAngle(0) % 360;
            if (angle < 45 || angle > 90+45) {
                player.sendChatMessage("/time set " + 20*60);
            }
            clear();
            queue_delay = 10;
        }
        
        @help("Shows the mods screen")
        @SideOnly(Side.CLIENT)
        public static void mods() {
            queued_action = SHOW_MODS_LIST;
        }
        
        @cheaty
        @alias({"neo", "deneo", "deninja"})
        @help("Makes the world run slowly (single-player client-side only). Can specify custom timerSpeed.")
        public static String ninja() {
            if (mc.isSingleplayer()) {
                float tps;
                if (arg0.startsWith("de")) {
                    tps = 1F;
                } else {
                    tps = 0.5F;
                    try {
                        tps = Float.parseFloat(arg1);
                    } catch (Throwable t) {}
                }
                tps = Math.max(0.1F, tps);
                tps = Math.min(1, tps);
                mc.timer.timerSpeed = tps;
                if (arg0.contains("neo")) {
                    if (tps == 1) {
                        return "Go back to sleep, Neo.";
                    } else {
                        return "Wake up, Neo.";
                    }
                }
            }
            return null;
        }
        
        @help("Sets the watchdog waitInterval")
        public static String watchdog() {
            if (LagssieWatchDog.instance == null) {
                return "Watchdog disabled. Enable in config, or use /f startwatchdog";
            }
            
            if (arg1 == null) {
                return "Usage: /f watchdog [waitInterval=" + LagssieWatchDog.instance.sleep_time + "]";
            }
            LagssieWatchDog.instance.sleep_time = Double.parseDouble(arg1);
            return "Set waitInterval to " + LagssieWatchDog.instance.sleep_time;
        }
        
        @help("Starts the watchdog")
        public static String startwatchdog() {
            if (LagssieWatchDog.instance == null) {
                FzConfig.lagssie_watcher = true;
                LagssieWatchDog.start();
                return "Started watchdog.";
            } else {
                return "Watchdog already running.";
            }
        }
        
        @alias({"td"})
        @help("Sets the minimum time dilation (between 0.1 and 1), or disables it (0)")
        public static String timedilation() {
            if (arg1 == null) {
                String msg = "Current time dilation: " + mc.timer.timerSpeed;
                if (!FzConfig.use_tps_reports) {
                    msg += " ";
                    msg += "(Disabled)";
                }
                return msg;
            }
            float dilation = Float.parseFloat(arg1);
            if (dilation <= 0) {
                FzConfig.use_tps_reports = false;
                return "Time dilation disabled";
            }
            dilation = Math.max(0.1F, dilation);
            dilation = Math.min(1F, dilation);
            FzConfig.lowest_dilation = dilation;
            if (!FzConfig.use_tps_reports) {
                FzConfig.use_tps_reports = true;
                return "Enabled time dilation at " + dilation;
            } else {
                return "Set minimum time dilation to " + dilation;
            }
        }
        
        @cheaty
        @SideOnly(Side.CLIENT)
        @help("Dump chunk to .obj")
        public static String exportChunk() {
            WorldRenderer[] lizt = mc.renderGlobal.sortedWorldRenderers;
            double px = mc.thePlayer.posX, py = mc.thePlayer.posY, pz = mc.thePlayer.posZ;
            for (WorldRenderer wr : lizt) {
                if (wr.posXMinus < px && px < wr.posXPlus
                        && wr.posYMinus < py && py < wr.posYPlus
                        && wr.posZMinus < pz && pz < wr.posZPlus) {
                    Tessellator real_tess = Tessellator.instance;
                    File output = new File("./chunkExport.obj");
                    try {
                        ExporterTessellator ex = new ExporterTessellator(output);
                        Tessellator.instance = ex;
                        wr.markDirty();
                        wr.updateRenderer(mc.thePlayer);
                        ex.doneDumping();
                    } finally {
                        Tessellator.instance = real_tess;
                    }
                    return "Written to " + output;
                }
            }
            return "You aren't in a rendering chunk. Remarkable.";
        }
        
        @cheaty
        @help("Dump all terrain to a .obj. This can take a while! Watch the console.")
        public static String exportWorld() {
            double maxDist = 256;
            if (arg1 != null && arg1 != "") {
                maxDist = Double.parseDouble(arg1);
            }
            WorldRenderer[] lizt = mc.renderGlobal.sortedWorldRenderers;
            Tessellator real_tess = Tessellator.instance;
            File output = new File("./worldExport.obj");
            
            try {
                ExporterTessellator ex = new ExporterTessellator(output);
                Tessellator.instance = ex;
                int total = lizt.length;
                double px = mc.thePlayer.posX, py = mc.thePlayer.posY, pz = mc.thePlayer.posZ;
                int skipped = 0;
                for (int i = 0; i < lizt.length; i++) {
                    WorldRenderer wr = lizt[i];
                    double dx = wr.posX - px;
                    double dy = wr.posY - py;
                    double dz = wr.posZ - pz;
                    double dist = Math.sqrt(dx*dx + dy*dy + dz*dz);
                    if (dist > maxDist) {
                        skipped++;
                        continue;
                    }
                    System.out.println("Writing chunk " + i + "/" + total + " at " + wr.posX + " " + wr.posY + " " + wr.posZ);
                    wr.markDirty();
                    wr.updateRenderer(mc.thePlayer);
                }
                System.out.println("Skipped " + skipped + " chunks");
                ex.doneDumping();
            } finally {
                Tessellator.instance = real_tess;
            }
            return "Done!";
        }
        
        @cheaty
        @help("Re-renders the chunk as a wireframe")
        public static String wireframe() {
            WorldRenderer[] lizt = mc.renderGlobal.sortedWorldRenderers;
            double px = mc.thePlayer.posX, py = mc.thePlayer.posY, pz = mc.thePlayer.posZ;
            for (WorldRenderer wr : lizt) {
                if (wr.posXMinus < px && px < wr.posXPlus
                        && wr.posYMinus < py && py < wr.posYPlus
                        && wr.posZMinus < pz && pz < wr.posZPlus) {
                    Tessellator real_tess = Tessellator.instance;
                    Tessellator.instance = new WireframeTessellator();
                    wr.markDirty();
                    wr.updateRenderer(mc.thePlayer);
                    Tessellator.instance = real_tess;
                    return null;
                }
            }
            return "You aren't in a rendering chunk. Remarkable.";
        }
        
        static Tessellator orig = null;
        @cheaty
        @help("Render all the things with a wireframe")
        public static String wireframeGlobal() {
            if (orig == null) {
                orig = Tessellator.instance;
                Tessellator.instance = new WireframeTessellator();
                return "Run the command again to disable. Note that some effects may persist until MC is restarted.";
            } else {
                Tessellator.instance = orig;
                orig = null;
                return "Restored normal Tessellator";
            }
        }
        
        static Map backup = null, empty = new HashMap();
        
        @help("Disable or enable TileEntity special renderers")
        public static String tesrtoggle() {
            if (backup == null) {
                if (TileEntityRendererDispatcher.instance.mapSpecialRenderers == null) {
                    return "no TESRs!";
                }
                backup = TileEntityRendererDispatcher.instance.mapSpecialRenderers;
                TileEntityRendererDispatcher.instance.mapSpecialRenderers = empty;
                return "TESRs disabled";
            } else {
                empty.clear();
                TileEntityRendererDispatcher.instance.mapSpecialRenderers = backup;
                backup = null;
                return "TESRs enabled; requires chunk update to restart drawing";
            }
        }
        
        @help("Change how large servo instructions are rendered. (This also has a config option.)")
        public static String servoInstructionSize() {
            FzConfig.large_servo_instructions = !FzConfig.large_servo_instructions;
            return "Servo instruction size toggled; requires a chunk update to redraw.";
        }
        
        @help("Sets doDaylightCycle, doMobSpawning, weather, time")
        public static void setupSterileTestWorld() {
            player.sendChatMessage("/gamerule doDaylightCycle false");
            player.sendChatMessage("/gamerule doMobSpawning false");
            player.sendChatMessage("/weather clear 999999");
            player.sendChatMessage("/time set " + 20*60);
        }
        
        @help("Pass an /f command to the server (for Factions)")
        @alias("/f")
        public static void factions() {
            String cmd = "";
            boolean first = true;
            for (String c : args) {
                if (first) {
                    first = false;
                    cmd = c;
                } else {
                    cmd += " " + c;
                }
            }
            player.sendChatMessage(cmd);
        }
        
        @help("Copy the unlocalized name of the held item to the clipboard")
        public static String copylocalkey() {
            ItemStack is = mc.thePlayer.getHeldItem();
            if (is == null) {
                return "Not holding anything";
            }
            String name = is.getUnlocalizedName();
            if (name == null) {
                return "Item has no localization key!";
            }
            if (name.isEmpty()) {
                return "Item's localization key is empty!";
            }
            FzUtil.copyStringToClipboard(name);
            return "Copied to clipboard: " + name;
        }
        
        // Remember to include 'public static' for anything added here.
        // And also to put the command in this nested class, not the wrong one. :P
    }
    
    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args == null || args.length == 0) {
            args = new String[] { "help" };
        }
        ArrayList<String> better = new ArrayList();
        for (String arg : args) {
            if (arg == null || arg.length() == 0) continue;
            better.add(arg);
        }
        runCommand(better);
    }
    
    @Override
    public String getCommandUsage(ICommandSender icommandsender) {
        return "/f <subcommand, such as 'help' or 'list'>";
    }
    
    @Override
    public List addTabCompletionOptions(ICommandSender sender, String[] args) {
        try {
            if (args.length == 1) {
                String arg0 = args[0];
                List<String> availCommands = new ArrayList<String>(50);
                availCommands.addAll(Arrays.asList("0", "1", "2", "3", "4", "+", ""));
                for (Method method : miscCommands.class.getMethods()) {
                    if (method.getDeclaringClass() == Object.class || method.getParameterTypes().length != 0) {
                        continue;
                    }
                    availCommands.add(method.getName());
                    MiscClientCommands.alias a = method.getAnnotation(MiscClientCommands.alias.class);
                    if (a == null) {
                        continue;
                    }
                    for (String name : a.value()) {
                        availCommands.add(name);
                    }
                }
                
                List<String> ret = new LinkedList();
                for (String name : availCommands) {
                    if (name.startsWith(arg0)) {
                        ret.add(name);
                    }
                }
                return ret;
            }
        } catch (Throwable e) {
            Core.logWarning("Wasn't able to do tab completion!");
            e.printStackTrace();
            return Arrays.asList("tab_completion_failed");
        }
        return new LinkedList();
    }
    
    void runCommand(List<String> args) {
        try {
            if (args == null) {
                args = new ArrayList<String>();
            }
            String n;
            if (args.size() == 0) {
                args = Arrays.asList("help");
                n = "help";
            } else {
                n = args.get(0);
            }
            int i = mc.gameSettings.renderDistanceChunks;
            boolean found_number = true;
            if (n.equalsIgnoreCase("+")) {
                i++;
            } else if (n.equalsIgnoreCase("-")) {
                i--;
            } else {
                try {
                    i = Integer.parseInt(n);
                } catch (NumberFormatException e) {
                    found_number = false;
                }
            }
            if (found_number) {
                if (i < 1) {
                    i = 1;
                }
                if (i > 16) {
                    i = 16; //seems to have started crashing. Lame.
                }
                mc.gameSettings.renderDistanceChunks = i; //NORELEASE: Test new ranges
                return;
            }
            if (n.equalsIgnoreCase("bug")) {
                addBugReport(args);
                return;
            }
            for (Method method : miscCommands.class.getMethods()) {
                if (method.getDeclaringClass() == Object.class || method.getParameterTypes().length != 0) {
                    continue;
                }
                if (method.getName().equals(n)) {
                    tryCall(method, args);
                    return;
                }
                alias a = method.getAnnotation(alias.class);
                if (a == null) {
                    continue;
                }
                for (String an : a.value()) {
                    if (an.equals(n)) {
                        tryCall(method, args);
                        return;
                    }
                }
            }
            mc.thePlayer.addChatMessage(new ChatComponentText("Unknown command. Try /f list."));
        } catch (Exception e) {
            mc.thePlayer.addChatMessage(new ChatComponentText("Command failed; see console"));
            e.printStackTrace();
        }
    }
    
    private void addBugReport(List<String> args) {
        String msg = "[" + Calendar.getInstance().getTime().toString() + "]";
        for (String arg : args) {
            if (arg == args.get(0)) continue;
            msg += " " + arg;
        }
        if (msg.isEmpty()) return;
        try {
            File target = new File("/media/media/fbugs");
            Writer out = new BufferedWriter(new FileWriter(target, true));
            out.append(msg + "\n");
            out.flush();
            out.close();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
    
    static boolean commandAllowed(Method method) {
        if (method.getDeclaringClass() == Object.class || method.getParameterTypes().length != 0) {
            return false;
        }
        boolean canCheat = mc.thePlayer.capabilities.isCreativeMode && mc.isSingleplayer();
        if (canCheat) {
            return true;
        }
        if (method.getAnnotation(sketchy.class) != null && !FzConfig.enable_sketchy_client_commands) {
            return false;
        }
        if (method.getAnnotation(cheaty.class) != null) {
            return false;
        }
        return true;
    }
    
    void tryCall(Method method, List<String> args) {
        if (!commandAllowed(method)) {
            mc.thePlayer.addChatMessage(new ChatComponentText("That command is disabled"));
            return;
        }
        try {
            miscCommands.player = mc.thePlayer;
            miscCommands.arg0 = args.get(0);
            if (args.size() >= 2) {
                miscCommands.arg1 = args.get(1);
            }
            miscCommands.args = args;
            
            Object ret = method.invoke(null);
            if (ret != null) {
                mc.thePlayer.addChatMessage(new ChatComponentText(ret.toString()));
            }
        } catch (Exception e) {
            mc.thePlayer.addChatMessage(new ChatComponentText("Caught an exception from command; see console"));
            e.printStackTrace();
        } finally {
            miscCommands.player = null;
            miscCommands.arg0 = miscCommands.arg1 = null;
        }
    }
    
    static void tick() {
        if (queued_action == 0) {
            return;
        }
        if (queue_delay > 0) {
            queue_delay--;
            return;
        }
        switch (queued_action) {
        case CLEAR_CHAT:
            List cp = new ArrayList();
            cp.addAll(mc.ingameGUI.getChatGUI().getSentMessages());
            mc.ingameGUI.getChatGUI().clearChatMessages(); 
            mc.ingameGUI.getChatGUI().getSentMessages().addAll(cp);
            break;
        case SHOW_MODS_LIST:
            mc.displayGuiScreen(new GuiModList(null));
            break;
        }
        queued_action = 0;
    }
}
