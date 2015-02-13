package factorization.misc;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import factorization.common.FzConfig;
import factorization.shared.Core;
import factorization.util.FzUtil;
import factorization.weird.NeptuneCape;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.EnumDifficulty;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.event.GuiScreenEvent.ActionPerformedEvent;
import net.minecraftforge.client.event.GuiScreenEvent.InitGuiEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent17;

public class MiscClientProxy extends MiscProxy {
    static final Minecraft mc = Minecraft.getMinecraft();
    MiscClientTickHandler cth = new MiscClientTickHandler();
    
    @Override
    void initializeClient() {
        Minecraft.memoryReserve = new byte[0]; // Free up this unused memory. The OOM screen *never* happens.
        Core.loadBus(this);
        ClientCommandHandler.instance.registerCommand(new MiscClientCommands());
        FMLCommonHandler.instance().bus().register(cth);
        new NeptuneCape();
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
        mc.timer.timerSpeed = newTps;
    }
    
    private GuiButton difficulty_button = null;
    
    @SubscribeEvent
    public void addDifficultyInfo(InitGuiEvent.Post event) {
        difficulty_button = null;
        if (!(event.gui instanceof GuiSelectWorld)) return;
        if (getDifficulty() != EnumDifficulty.PEACEFUL) return;
        event.buttonList.add(difficulty_button = new GuiButton(-237, 0, 0, ""));
        updateDifficultyString();
    }
    
    @SubscribeEvent
    public void changeDifficulty(ActionPerformedEvent.Pre event) {
        if (event.button != difficulty_button || difficulty_button == null) return;
        GameSettings gs = Minecraft.getMinecraft().gameSettings;
        gs.difficulty = FzUtil.shiftEnum(gs.difficulty, EnumDifficulty.values(), 1);
        updateDifficultyString();
    }
    
    void updateDifficultyString() {
        EnumDifficulty ed = getDifficulty();
        String color = (ed == EnumDifficulty.PEACEFUL) ? ("" + EnumChatFormatting.RED) : "";
        difficulty_button.displayString = color + "Difficulty: " + ed;
    }
    
    EnumDifficulty getDifficulty() {
        return Minecraft.getMinecraft().gameSettings.difficulty;
    }
    
    @SubscribeEvent
    public void patchupTheStupidSecretButton(InitGuiEvent.Post event) {
        if (!(event.gui instanceof GuiOptions)) return;
        
        for (Object obj : event.buttonList) {
            if (obj instanceof GuiButton) {
                GuiButton button = (GuiButton) obj;
                if (button.id == 8675309) {
                    button.displayString = "Shaders; press F4 to reset";
                    button.xPosition = 0;
                    button.yPosition = 0;
                    return;
                }
            }
        }
    }
    
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void addDirectionInfoToDebugScreen(RenderGameOverlayEvent.Text event) {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.thePlayer;
        float t = 360;
        double yaw = ((player.rotationYaw % t) + t) % t;
        yaw = Math.toRadians(yaw);
        double x = -Math.sin(yaw);
        double z = Math.cos(yaw);
        
        for (int i = 0; i < event.left.size(); i++) {
            String line = event.left.get(i);
            if (line == null) continue;
            if (line.startsWith("f:")) {
                line += " (" + displ(x) + ", " + displ(z) + ")";
                event.left.set(i, line);
                break;
            }
        }
    }
    
    private String displ(double r) {
        int n = (int) Math.abs(r * 3);
        if (n == 0) {
            return "=";
        }
        String s = r > 0 ? "+" : "-";
        String ret = "";
        for (int i = 0; i < n; i++) {
            ret += s;
        }
        return ret;
    }


    long present_tick = -100;
    int event_count = 0;
    static final int max_event = 4;
    static final double logMax = Math.log(max_event);

    @SubscribeEvent(priority = EventPriority.LOW)
    public void supressExcessiveSound(PlaySoundEvent17 event) {
        // Basically, divide the volume by the number of events minus some threshold
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null) {
            present_tick = -100;
            return;
        }
        if (event.result == null) return;
        if (event.isCanceled()) return;
        long now = mc.theWorld.getTotalWorldTime();
        if (now != present_tick) {
            present_tick = now;
            event_count = 0;
        }
        if (event_count++ < max_event) return;
        final double origVolume = event.result.getVolume();
        final float newVolume = (float)(origVolume / Math.log(event_count) * logMax);
        event.result = new ProxiedSound(event.result) {
            @Override
            public float getVolume() {
                return newVolume;
            }
        };
    }
}
