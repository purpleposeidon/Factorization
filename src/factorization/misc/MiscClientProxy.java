package factorization.misc;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.EnumSet;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.stats.AchievementList;
import net.minecraft.stats.StatFileWriter;
import net.minecraft.util.EnumChatFormatting;
import cpw.mods.fml.client.GuiModList;
import cpw.mods.fml.client.registry.KeyBindingRegistry;
import cpw.mods.fml.client.registry.KeyBindingRegistry.KeyHandler;
import cpw.mods.fml.common.IScheduledTickHandler;
import cpw.mods.fml.common.TickType;
import cpw.mods.fml.common.registry.TickRegistry;
import cpw.mods.fml.relauncher.ReflectionHelper;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.common.Core;
import factorization.common.FzConfig;

public class MiscClientProxy extends MiscProxy {
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
    static @interface help {
        public String value();
    }
    
    public static class miscCommands { //NOTE: *not* SideOnly'd.
        static Minecraft mc;
        static EntityClientPlayerMP player;
        static String arg0, arg1;
        
        @alias({"date", "time"})
        @help("Show the real-world time")
        public static String now() {
            return Calendar.getInstance().getTime().toString();
        }
        
        @alias({"help", "?"})
        public static void about() {
            player.addChatMessage("Miscellaneous Client Commands; from Factorization, by neptunepink");
            player.addChatMessage("Use /f list go see the sub-commands.");
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
                if (arg1 == null || arg1.length() == 0 || msg.contains(arg1)) {
                    player.addChatMessage(msg);
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
                player.addChatMessage(msg);
            }
        }
        
        @alias({"cl"})
        @help("Erases the chat window")
        @SideOnly(Side.CLIENT)
        public static void clear() {
            List cp = new ArrayList();
            cp.addAll(mc.ingameGUI.getChatGUI().getSentMessages());
            mc.ingameGUI.getChatGUI().clearChatMessages(); 
            mc.ingameGUI.getChatGUI().getSentMessages().addAll(cp);
        }
        
        @sketchy
        @help("Reveals your coordinates in-chat")
        public static void saycoords() {
            player.sendChatMessage("/me is at " + ((int) player.posX) + ", " + ((int) player.posY) + ", " + ((int) player.posZ));
        }
        
        @alias({"ss"})
        @help("Saves game settings. (Vanilla seems to need help with this.)")
        @SideOnly(Side.CLIENT)
        public static String savesettings() {
            mc.gameSettings.saveOptions();
            return "Saved settings";
        }
        
        @alias({"render_everything_lagfest"})
        @help("Render a ton of terrain at once (may lock your game up for a while)")
        @SideOnly(Side.CLIENT)
        public static void render_above() {
            Object wr_list = ReflectionHelper.getPrivateValue(RenderGlobal.class, mc.renderGlobal, "sortedWorldRenderers", "sortedWorldRenderers");
            if (!(wr_list instanceof WorldRenderer[])) {
                mc.thePlayer.addChatMessage("Reflection failed");
                return;
            }
            WorldRenderer[] lizt = (WorldRenderer[]) wr_list;
            int did = 0;
            int total = 0;
            boolean lagfest = arg0.contains("lagfest");
            for (WorldRenderer wr : lizt) {
                total++;
                if (wr.needsUpdate) {
                    if (wr.posY - 16*3 > mc.thePlayer.posY && wr.posY < mc.thePlayer.posY + 16*8 || lagfest) {
                        wr.updateRenderer();
                        wr.needsUpdate = false;
                        did++;
                    }
                }
            }
            player.addChatMessage("Rendered " + did + " chunks out of " + total);
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
                player.sendChatMessage("/toggledownfall");
            }
            double angle =player.worldObj.getCelestialAngle(0) % 360;
            if (angle < 45 || angle > 90+45) {
                player.sendChatMessage("/time set " + 20*60);
            }
            player.sendChatMessage("/f cl");
        }
        
        @help("Shows the mods screen")
        @SideOnly(Side.CLIENT)
        public static void mods() {
            mc.displayGuiScreen(new GuiModList(null));
        }
        
        @sketchy
        @alias({"neo", "deneo", "deninja"})
        @help("Makes the world run slowly (single-player client-side only). Can specify custom timerSpeed.")
        @SideOnly(Side.CLIENT)
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
            if (MiscClientProxy.watch_dog == null) {
                return "Watchdog disabled. Enable in config, or use /f startwatchdog";
            }
            
            if (arg1 == null) {
                return "Usage: /f watchdog [waitInterval=" + watch_dog.sleep_time + "]";
            }
            watch_dog.sleep_time = Double.parseDouble(arg1);
            return "Set waitInterval to " + watch_dog.sleep_time;
        }
        
        @help("Starts the watchdog")
        public static String startwatchdog() {
            if (MiscClientProxy.watch_dog == null) {
                FzConfig.lagssie_watcher = true;
                MiscClientProxy.startLagWatchDog();
                return "Started watchdog.";
            } else {
                return "Watchdog already running.";
            }
        }
        
        @alias({"td"})
        @help("Sets the minimum time dilation (between 0.1 and 1), or disables it (0)")
        @SideOnly(Side.CLIENT)
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
        
        //Remember to include 'public' for anything added here.
        //Need to SideOnly(CLIENT) for things that access Minecraft.class, and add them to the list in the ICommand.
    }
    
    @Override
    void runCommand(List<String> args) {
        Minecraft mc = Minecraft.getMinecraft();
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
            int i = mc.gameSettings.renderDistance;
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
                if (!mc.isSingleplayer() || !FzConfig.enable_sketchy_client_commands) {
                    if (i < 0) {
                        i = 0;
                    }
                }
                if (i > 8) {
                    i = 8; //seems to have started crashing. Lame.
                }
                mc.gameSettings.renderDistance = i;
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
            mc.thePlayer.addChatMessage("Unknown command. Try /f list.");
        } catch (Exception e) {
            mc.thePlayer.addChatMessage("Command failed; see console");
            e.printStackTrace();
        }
    }
    
    static boolean commandAllowed(Method method) {
        if (method.getAnnotation(sketchy.class) != null && !FzConfig.enable_sketchy_client_commands) {
            return false;
        }
        if (method.getDeclaringClass() == Object.class || method.getParameterTypes().length != 0) {
            return false;
        }
        return true;
    }
    
    void tryCall(Method method, List<String> args) {
        Minecraft mc = Minecraft.getMinecraft();
        if (!commandAllowed(method)) {
            mc.thePlayer.addChatMessage("That command is disabled");
            return;
        }
        try {
            miscCommands.mc = mc;
            miscCommands.player = mc.thePlayer;
            miscCommands.arg0 = args.get(0);
            if (args.size() >= 2) {
                miscCommands.arg1 = args.get(1);
            }
            
            Object ret = method.invoke(null);
            if (ret != null) {
                mc.thePlayer.addChatMessage(ret.toString());
            }
        } catch (Exception e) {
            mc.thePlayer.addChatMessage("Caught an exception from command; see console");
            e.printStackTrace();
        } finally {
            miscCommands.mc = null;
            miscCommands.player = null;
            miscCommands.arg0 = miscCommands.arg1 = null;
        }
    }
    
    @Override
    void fixAchievements() {
        //give the first achievement, because it is stupid and nobody cares.
        //If you're using this mod, you've probably opened your inventory before anyways.
        StatFileWriter sfw = Minecraft.getMinecraft().statFileWriter;
        if (sfw != null && !sfw.hasAchievementUnlocked(AchievementList.openInventory) && !FzConfig.add_branding) {
            sfw.readStat(AchievementList.openInventory, 1);
            Core.logInfo("Achievement Get! You've opened your inventory hundreds of times already! Yes! You're welcome!");
        }
        Minecraft.memoryReserve = new byte[0]; //Consider it an experiment. Would this break anything? I've *never* seen the out of memory screen.
    }
    
    @Override
    void registerLoadAlert() {
        IScheduledTickHandler th = new IScheduledTickHandler() {
            boolean hit = false;
            int count = 0;
            @Override
            public EnumSet<TickType> ticks() {
                if (hit) {
                    return EnumSet.noneOf(TickType.class);
                }
                return EnumSet.of(TickType.CLIENT);
            }
            
            @Override
            public void tickStart(EnumSet<TickType> type, Object... tickData) {
                if (type.contains(TickType.CLIENT)) {
                    Minecraft mc = Minecraft.getMinecraft();
                    if (count == 40) {
                        //playing any earlier doesn't seem to work (sound is probably loaded in a separate thread?)
                        if (mc.currentScreen instanceof GuiMainMenu) {
                            mc.sndManager.playSoundFX("random.click", 1.0F, 1.0F);
                        }
                        hit = true;
                        startLagWatchDog();
                    }
                    count++;
                }
            }
            
            @Override
            public void tickEnd(EnumSet<TickType> type, Object... tickData) { }

            @Override
            public int nextTickSpacing() {
                if (hit) {
                    return 100000;
                }
                return 1;
            }
            
            @Override
            public String getLabel() {
                return "FZMisc waiting for Main Menu";
            }
        };
        TickRegistry.registerScheduledTickHandler(th, Side.CLIENT);
    }
    
    
    KeyBinding sprint = new KeyBinding("FZ vanilla sprint", 0);
    @Override
    void registerSprintKey() {
        KeyBindingRegistry.registerKeyBinding(new KeyHandler(new KeyBinding[] {sprint}, new boolean[] {true}) {
            @Override
            public String getLabel() {
                return "FZ Sprint (vanilla)";
            }
            
            @Override
            public EnumSet<TickType> ticks() {
                return EnumSet.of(TickType.CLIENT);
            }
            
            @Override
            public void keyUp(EnumSet<TickType> types, KeyBinding kb, boolean tickEnd) {
                sprint(false);
            }
            
            @Override
            public void keyDown(EnumSet<TickType> types, KeyBinding kb, boolean tickEnd, boolean isRepeat) {
                sprint(true);
            }
            
            void sprint(boolean state) {
                Minecraft mc = Minecraft.getMinecraft();
                if (mc.currentScreen != null) {
                    return;
                }
                if (mc.thePlayer == null) {
                    return;
                }
                if (sprint.keyCode == 0) {
                    return;
                }
                if (!mc.thePlayer.isSneaking() && mc.thePlayer.isSprinting() != state) {
                    mc.thePlayer.setSprinting(state);
                }
                mc.gameSettings.keyBindForward.pressed = state;
            }
        });
    }
    
    @Override
    void handleTpsReport(float newTps) {
        if (Float.isInfinite(newTps) || Float.isNaN(newTps)) {
            return;
        }
        if (!FzConfig.use_tps_reports) {
            return;
        }
        newTps = Math.min(1.5F, Math.max(FzConfig.lowest_dilation, newTps));
        Minecraft mc = Minecraft.getMinecraft();
        mc.timer.timerSpeed = newTps;
    }
    
    static LagssieWatchDog watch_dog = null;
    
    static void startLagWatchDog() {
        if (FzConfig.lagssie_watcher) {
            watch_dog = new LagssieWatchDog(Thread.currentThread(), FzConfig.lagssie_interval);
            Thread dog = new Thread(watch_dog);
            dog.setDaemon(true);
            dog.start();
        }
    }
}
