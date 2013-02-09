package factorization.misc;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
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
        } else if (n.equalsIgnoreCase("clear")) {
            List cp = new ArrayList();
            cp.addAll(mc.ingameGUI.getChatGUI().getSentMessages());
            mc.ingameGUI.getChatGUI().func_73761_a(); //above printChatMessage; clears chatLines and sentMessages 
            mc.ingameGUI.getChatGUI().getSentMessages().addAll(cp);
        } else if (n.equalsIgnoreCase("saycoords") && Core.enable_sketchy_client_commands) {
            EntityClientPlayerMP player = mc.thePlayer;
            player.sendChatMessage("/me is at " + ((int) player.posX) + ", " + ((int) player.posY) + ", " + ((int) player.posZ));
        }
    }
}
