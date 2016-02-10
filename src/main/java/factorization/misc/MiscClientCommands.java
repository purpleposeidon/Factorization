package factorization.misc;

import com.google.common.base.Joiner;
import factorization.aabbdebug.AabbDebugger;
import factorization.api.Coord;
import factorization.common.FzConfig;
import factorization.notify.Notice;
import factorization.notify.Style;
import factorization.shared.Core;
import factorization.util.DataUtil;
import factorization.util.FzUtil;
import factorization.util.NORELEASE;
import factorization.util.RenderUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EnumPlayerModelParts;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.IWorldAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.client.GuiModList;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.nio.IntBuffer;
import java.text.DateFormat;
import java.util.*;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

public class MiscClientCommands implements ICommand {
    static final Minecraft mc = Minecraft.getMinecraft();
    static int queued_action = 0;
    static int queue_delay = 0;
    static final int SHOW_MODS_LIST = 1, CLEAR_CHAT = 2;
    
    public int compareTo(ICommand other) {
        return this.getCommandName().compareTo(other.getCommandName());
    }

    @Override
    public String getCommandName() {
        return FzConfig.f;
    }

    @Override
    public List<String> getCommandAliases() {
        return new ArrayList<String>();
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
        static EntityPlayerSP player;
        static String arg0, arg1;
        static List<String> args;
        
        @alias({"date", "time"})
        @help("Show the real-world time. Notes can be added.")
        public static String now() {
            DateFormat df = DateFormat.getTimeInstance(DateFormat.SHORT);
            String ret = "[" + df.format(new Date()) + "]";
            args.remove(0);
            if (!args.isEmpty()) {
                ret += " " + Joiner.on(" ").join(args);
            }
            return ret;
        }
        
        @alias({"help", "?"})
        public static void about() {
            player.addChatMessage(new ChatComponentText("Miscellaneous Client Commands; from Factorization, by neptunepink"));
            player.addChatMessage(new ChatComponentText("Use /" + FzConfig.f + " list go see the sub-commands."));
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
            queue_delay = 10;
        }
        
        @sketchy
        @help("Reveals your coordinates in-chat")
        public static void saycoords() {
            String append = "";
            if (args.size() > 1) {
                append = ": ";
                for (int i = 1; i < args.size(); i++) {
                    append += args.get(i) + " ";
                }
            }
            player.sendChatMessage("/me is at " + ((int) player.posX) + ", "
                    + ((int) player.posY) + ", " + ((int) player.posZ) + " in dimension "
                    + player.worldObj.provider.getDimensionId() + append);
        }
        
        @alias({"ss"})
        @help("Saves game settings. (Vanilla seems to need help with this.)")
        public static String savesettings() {
            mc.gameSettings.saveOptions();
            return "Saved settings";
        }
        
        @alias("c")
        @help("Switch between creative and survival mode")
        public static void creative() {
            //Not sketchy since you wouldn't be able to run it anyways.
            player.sendChatMessage("/gamemode " + (player.capabilities.isCreativeMode ? 0 : 1));
        }

        @alias("sp")
        @help("Switch betweeen spectator and creative mode")
        public static void spectate() {
            //Not sketchy since you wouldn't be able to run it anyways.
            player.sendChatMessage("/gamemode " + (player.capabilities.isCreativeMode ? 3 : 1));
        }

        @alias({"n", "makenice"})
        @help("Makes it a sunny morning")
        public static void nice() {
            if (player.worldObj.isRaining()) {
                player.sendChatMessage("/weather clear");
            }
            double angle = player.worldObj.getCelestialAngle(0) % 360;
            if (angle < 45 || angle > 90+45) {
                player.sendChatMessage("/time set " + 20*60);
            }
            clear();
        }

        @help("Makes it day")
        public static void day() {
            player.sendChatMessage("/time set " + 20*60);
        }

        @help("Makes it night")
        public static void night() {
            player.sendChatMessage("/time set 18000");
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
                return "Watchdog disabled. Enable in config, or use /" + FzConfig.f + " startwatchdog";
            }
            
            if (arg1 == null) {
                return "Usage: /" + FzConfig.f + " watchdog [waitInterval=" + LagssieWatchDog.instance.sleep_time + "]";
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
            player.sendChatMessage("/gamerule keepInventory true");
            player.sendChatMessage("/weather clear 999999");
            player.sendChatMessage("/time set " + 20*60);
            NORELEASE.fixme("There's commands to do this now.");
            NORELEASE.fixme("Also add stuff for per-entity murder in docbook?");
            MinecraftServer ms = MinecraftServer.getServer();
            if (ms == null) {
                return;
            }
            if (ms.worldServers == null) {
                return;
            }
            for (World w : ms.worldServers) {
                for (Entity ent : (Iterable<Entity>) w.loadedEntityList) {
                    if (ent instanceof EntityLiving) {
                        ent.setDead();
                    } else if (ent instanceof EntityItem) {
                        ent.setDead();
                    }
                }
            }
        }
        
        @help("Pass an /f command to the server (for Factions; but see FZ's config)")
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

        @help("Copy the internal name of the held item to the clipboard")
        public static String copyname() {
            ItemStack is = mc.thePlayer.getHeldItem();
            if (is == null) {
                return "Not holding anything";
            }
            String name = DataUtil.getName(is);
            FzUtil.copyStringToClipboard(name);
            return "Copied to clipboard: " + name;
        }
        
        @help("Marks nearby fake air blocks")
        public static void checkFakeAir() {
            Coord at = new Coord(mc.thePlayer);
            int d = 3;
            for (int dz = -d; dz <= d; dz++) {
                for (int dy = -d; dy <= d; dy++) {
                    for (int dx = -d; dx <= d; dx++) {
                        Coord here = at.add(dx, dy, dz);
                        if (here.isAir() && here.getBlock() != Blocks.air) {
                            new Notice(here, "X").withStyle(Style.FORCE).send(mc.thePlayer);
                        }
                    }
                }
            }
        }

        @help("Generate random values")
        public static String rng() {
            String coin = "TF";
            String dirs = "⬅⬆⬇➡";
            String blocks = "░▀▄█";
            String[] woods = new String[] { "Oak", "Spruce", "Birch", "Jungle", "Dark Oak", "Acacia" };
            String[] tc4 = new String[] { "Earth", "Air", "Fire", "Water", "Order", "Chaos" };
            String iching = "䷀䷁䷂䷃䷄䷅䷆䷇䷈䷉䷊䷋䷌䷍䷎䷏䷐䷑䷒䷓䷔䷕䷖䷗䷘䷙䷚䷛䷜䷝䷞䷟䷠䷡䷢䷣䷤䷥䷦䷧䷨䷩䷪䷫䷬䷭䷮䷯䷰䷱䷲䷳䷴䷵䷶䷷䷸䷹䷺䷻䷼䷽䷾䷿";
            Random rng;
            if (arg1 == null) {
                rng = new Random();
            } else {
                long seed;
                try {
                    seed = Long.parseLong(arg1);
                } catch (NumberFormatException e) {
                    seed = arg1.hashCode();
                }
                rng = new Random(seed);
            }
            return pick(coin, rng) + " " + pick(dirs, rng) + " " + pick(blocks, rng, 8) + " " + pick(rng, woods) + " " + rng.nextFloat() + " " + pick(iching, rng) + " " + pick(rng, tc4);
        }

        private static String pick(String s, Random rng) {
            int i = rng.nextInt(s.length());
            return s.substring(i, i + 1);
        }

        private static String pick(String s, Random rng, int n) {
            String bs = "";
            for (int i = 0; i < n; i++) {
                bs += pick(s, rng);
            }
            return bs;
        }

        private static String pick(Random rng, String... args) {
            return args[rng.nextInt(args.length)];
        }

        
        @help("Turns your cape on or off")
        public static void cape() {
            boolean inv = !mc.gameSettings.getModelParts().contains(EnumPlayerModelParts.CAPE);
            mc.gameSettings.setModelPartEnabled(EnumPlayerModelParts.CAPE, inv);
        }

        private static int active_world_hash = 0;
        private static IWorldAccess debugger = null;

        @help("shows block render update ranges; run again to disable")
        public static String debugBlockUpdates() {
            if (debugger == null) {
                debugger = new BlockUpdateDebugger();
                AabbDebugger.freeze = true;
            }
            World w = mc.theWorld;
            int hash = w.hashCode();
            if (hash == active_world_hash) {
                w.removeWorldAccess(debugger);
                active_world_hash = 0;
                return "Render update tracing disabled. To remove boxes run: /boxdbg clean";
            } else {
                w.addWorldAccess(debugger);
                active_world_hash = hash;
                return "Render update tracing enabled. Run this command again to disable.";
            }
        }

        @help("Saves block & item textures to a file, primarily for exportWorld & exportChunk; probably goes into .minecraft")
        public static void exportTextures() {
            // See ScreenShotHelper
            doSave(Core.blockAtlas, "./fz_block_atlas.png");
            doSave(Core.itemAtlas, "./fz_item_atlas.png");
        }

        private static void doSave(ResourceLocation texture, String filename) {
            mc.getTextureManager().bindTexture(texture);
            RenderUtil.checkGLError("Before save texture");
            int width = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH);
            int height = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT);
            //int format = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_INTERNAL_FORMAT);
            int bufferSize = width * height;
            RenderUtil.checkGLError("After get texture info");
            IntBuffer pixelBuffer = BufferUtils.createIntBuffer(bufferSize);
            int[] pixelValues = new int[bufferSize];

            GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
            GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 1);

            GL11.glGetTexImage(GL11.GL_TEXTURE_2D, 0, GL12.GL_BGRA, GL12.GL_UNSIGNED_INT_8_8_8_8_REV, pixelBuffer);
            pixelBuffer.get(pixelValues);
            BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
            img.setRGB(0, 0, width, height, pixelValues, 0, width);
            try {
                ImageIO.write(img, "png", new File(filename));
            } catch (IOException e) {
                e.printStackTrace();
            }
            RenderUtil.checkGLError("After save texture");
        }


        /*
        @help("Change the FOV")
        public static String fov() {
            // 70 + fov*40
            float origFov = 70F + mc.gameSettings.fovSetting*40F;
            float fov = Float.parseFloat(arg1);
            mc.gameSettings.fovSetting = (fov - 70F)/40F;
            return "FOV changed: " + origFov + " -> " + fov;
        }
        */
        
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
        return "/" + FzConfig.f + " <subcommand, such as 'help' or 'list'>";
    }

    @Override
    public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
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
                if (i < 1) { // 0 crashes
                    i = 1;
                }
                if (i > 16) { // and 17 crashes as well
                    i = 16;
                }
                mc.gameSettings.renderDistanceChunks = i;
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
            mc.thePlayer.addChatMessage(new ChatComponentText("Unknown command. Try /" + FzConfig.f + " list."));
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
            new Notice(mc.thePlayer, "").withStyle(Style.CLEAR).send(mc.thePlayer);
            break;
        case SHOW_MODS_LIST:
            mc.displayGuiScreen(new GuiModList(null));
            break;
        }
        queued_action = 0;
    }
}
