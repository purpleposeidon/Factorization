package factorization.misc;

import factorization.algos.ReservoirSampler;
import factorization.common.Command;
import factorization.common.FzConfig;
import factorization.coremodhooks.UnhandledGuiKeyEvent;
import factorization.shared.Core;
import factorization.truth.DocumentationModule;
import factorization.util.FzUtil;
import factorization.weird.NeptuneCape;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.EnumDifficulty;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.event.GuiScreenEvent.ActionPerformedEvent;
import net.minecraftforge.client.event.GuiScreenEvent.InitGuiEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.commons.io.Charsets;
import org.lwjgl.input.Keyboard;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

public class MiscClientProxy extends MiscProxy {
    static final Minecraft mc = Minecraft.getMinecraft();
    MiscClientTickHandler cth = new MiscClientTickHandler();
    
    @Override
    void initializeClient() {
        Minecraft.memoryReserve = new byte[0]; // Frees 10MB. Used for OOM screen, but that *never* happens.
        Core.loadBus(this);
        ClientCommandHandler.instance.registerCommand(new MiscClientCommands());
        Core.loadBus(cth);
        new NeptuneCape();

        GameSettings gameSettings = Minecraft.getMinecraft().gameSettings;
        gameSettings.setOptionValue(GameSettings.Options.REALMS_NOTIFICATIONS, 0);
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
        if (!(event.gui instanceof GuiSelectWorld)) {
            difficulty_button = null;
            return;
        }
        if (difficulty_button != null) return;
        if (getDifficulty() != EnumDifficulty.PEACEFUL) return;

        GuiButton first = (GuiButton) event.buttonList.get(0);
        first.displayString = "    " + EnumChatFormatting.BOLD + EnumChatFormatting.RED + first.displayString;
        event.buttonList.add(difficulty_button = new GuiButton(-237, 0, 0, 150, 18, ""));
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
    public void supressExcessiveSound(PlaySoundEvent event) {
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

    int last_hash = 0;
    @SubscribeEvent
    public void customSplash(InitGuiEvent.Pre event) {
        if (!(event.gui instanceof GuiMainMenu)) return;
        int hash = event.gui.hashCode();
        if (hash == last_hash) return;
        last_hash = hash;
        GuiMainMenu menu = (GuiMainMenu) event.gui;
        ReservoirSampler<String> sampler = new ReservoirSampler<String>(1, new Random());
        sampler.give(menu.splashText);
        sampler.preGive(359); // NORELEASE: Verify this number each MC version, including minor versions. (Or we could just count it. Hmm.)
        // Err, should that be <number of lines> - 1? Or maybe even -2 for the hashCode thing?
        sampler.give(""); // !!!! The secret EMPTY splash text! :O
        HashSet<String> alreadySeen = new HashSet<String>();
        int dupes = 0;
        try {
            @SuppressWarnings("unchecked")
            List<IResource> resources = mc.getResourceManager().getAllResources(new ResourceLocation("minecraft:texts/extra_splashes.txt"));
            for (IResource res : resources) {
                InputStream is = null;
                try {
                    is = res.getInputStream();
                    if (is == null) continue;
                    BufferedReader bufferedreader = new BufferedReader(new InputStreamReader(is, Charsets.UTF_8));
                    String s;

                    while ((s = bufferedreader.readLine()) != null) {
                        s = s.trim();
                        if (s.isEmpty()) continue;
                        if (s.hashCode() == 125780783) continue; // Probably "This message will never appear on the splash screen, isn't that weird?".hashCode()
                        if (s.startsWith("#")) continue;
                        if (!alreadySeen.add(s)) {
                            dupes++;
                            continue;
                        }
                        sampler.give(s);
                    }
                } finally {
                    FzUtil.closeNoisily("Closing " + res, is);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (dupes > 0) {
            Core.logWarning("Extra splashes had duplicated lines: " + dupes);
        }
        if (sampler.size() < 1) return;
        menu.splashText = sampler.getSamples().get(0);
    }

    @SubscribeEvent
    public void transferItems(UnhandledGuiKeyEvent event) {
        if (!(event.gui instanceof GuiContainer)) return;
        if (event.keysym == 0) return;
        Slot slot = DocumentationModule.getSlotUnderMouse();
        if (slot == null) return;
        ItemStack search = slot.getStack();
        if (search == null) return;
        GameSettings gm = Minecraft.getMinecraft().gameSettings;
        Command command;
        boolean sneak = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
        // Ugly!
        if (event.keysym == gm.keyBindLeft.getKeyCode()) {
            command = sneak ? Command.itemTransferLeftShift : Command.itemTransferLeft;
        } else if (event.keysym == gm.keyBindRight.getKeyCode()) {
            command = sneak ? Command.itemTransferRightShift : Command.itemTransferRight;
        } else if (event.keysym == gm.keyBindForward.getKeyCode()) {
            command = sneak ? Command.itemTransferUpShift : Command.itemTransferUp;
        } else if (event.keysym == gm.keyBindBack.getKeyCode()) {
            command = sneak ? Command.itemTransferDownShift : Command.itemTransferDown;
        } else {
            return;
        }
        command.call(event.player, slot.slotNumber);
    }
}
