package factorization.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.EnumDifficulty;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.event.GuiScreenEvent.ActionPerformedEvent;
import net.minecraftforge.client.event.GuiScreenEvent.InitGuiEvent;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import factorization.common.FzConfig;
import factorization.shared.Core;
import factorization.shared.FzUtil;
import factorization.weird.NeptuneCape;

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
}
