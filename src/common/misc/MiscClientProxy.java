package factorization.misc;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.EnumSet;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.stats.AchievementList;
import net.minecraft.stats.StatFileWriter;
import cpw.mods.fml.client.registry.KeyBindingRegistry;
import cpw.mods.fml.client.registry.KeyBindingRegistry.KeyHandler;
import cpw.mods.fml.common.IScheduledTickHandler;
import cpw.mods.fml.common.TickType;
import cpw.mods.fml.common.registry.TickRegistry;
import cpw.mods.fml.relauncher.Side;
import factorization.common.Core;

public class MiscClientProxy extends MiscProxy {
    @Override
    void runCommand(List<String> args) {
        Minecraft mc = Minecraft.getMinecraft();
        String n;
        if (args.size() == 1) {
            n = args.get(0);
            int i = mc.gameSettings.renderDistance;
            if (n.equalsIgnoreCase("far")) {
                i = 0;
            } else if (n.equalsIgnoreCase("normal")) {
                i = 1;
            } else if (n.equalsIgnoreCase("short")) {
                i = 2;
            } else if (n.equalsIgnoreCase("tiny")) {
                i = 3;
            } else if (n.equalsIgnoreCase("micro")) {
                i = 4;
            } else if (n.equalsIgnoreCase("microfog")) {
                i = 5;
            } else {
                try {
                    i = Integer.parseInt(n);
                } catch (NumberFormatException e) { }
            }
            if (!mc.isSingleplayer() || !Core.enable_sketchy_client_commands) {
                if (i < 0) {
                    i = 0;
                }
                if (i > 8) {
                    i = 8;
                }
            }
            mc.gameSettings.renderDistance = i;
        } else if (args.size() == 0) {
            n = "about";
        } else {
            n = args.get(1);
        }
        if (n.equalsIgnoreCase("other")) {
            String txt = "";
            for (String s : MiscellaneousNonsense.FogCommand.otherCommands) {
                txt += s + " ";
            }
            mc.thePlayer.addChatMessage("Other fog-unrelated subcommands; use \"/fog <command\":");
            mc.thePlayer.addChatMessage(txt);
        } else if (n.equalsIgnoreCase("pauserender")) {
            mc.skipRenderWorld = !mc.skipRenderWorld;
        } else if (n.equalsIgnoreCase("gc")) {
            System.gc(); //probably doesn't help the lag
            mc.thePlayer.addChatMessage("JVM Garbage Collection requested");
        } else if (n.equalsIgnoreCase("now") || n.equalsIgnoreCase("date") || n.equalsIgnoreCase("time")) {
            mc.thePlayer.addChatMessage(Calendar.getInstance().getTime().toString());
        } else if (n.equalsIgnoreCase("about") || n.equalsIgnoreCase("?") || n.equalsIgnoreCase("help")) {
            mc.thePlayer.addChatMessage("Misc client-side commands; from Factorization by neptunepink");
        } else if (n.equalsIgnoreCase("clear") || n.equalsIgnoreCase("cl")) {
            List cp = new ArrayList();
            cp.addAll(mc.ingameGUI.getChatGUI().getSentMessages());
            mc.ingameGUI.getChatGUI().func_73761_a(); //above printChatMessage; clears chatLines and sentMessages 
            mc.ingameGUI.getChatGUI().getSentMessages().addAll(cp);
        } else if (n.equalsIgnoreCase("saycoords") && Core.enable_sketchy_client_commands) {
            EntityClientPlayerMP player = mc.thePlayer;
            player.sendChatMessage("/me is at " + ((int) player.posX) + ", " + ((int) player.posY) + ", " + ((int) player.posZ));
        }
    }
    
    @Override
    void fixAchievements() {
        //give the first achievement, because it is stupid and nobody cares.
        //If you're using this mod, you've probably opened your inventory before anyways.
        StatFileWriter sfw = Minecraft.getMinecraft().statFileWriter;
        if (sfw != null && !sfw.hasAchievementUnlocked(AchievementList.openInventory) && !Core.add_branding) {
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
                            Core.logInfo("Click!");
                        }
                        hit = true;
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
                return "FZ sprint (vanilla)";
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
                if (mc.thePlayer == null) {
                    return;
                }
                mc.thePlayer.setSprinting(state);
                mc.gameSettings.keyBindForward.pressed = state;
            }
        });
    }
}
