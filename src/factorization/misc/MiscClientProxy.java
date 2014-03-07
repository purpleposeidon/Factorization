package factorization.misc;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.AchievementList;
import net.minecraft.stats.StatFileWriter;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.relauncher.Side;
import factorization.api.Coord;
import factorization.common.FzConfig;
import factorization.shared.Core;
import factorization.shared.FzUtil;

public class MiscClientProxy extends MiscProxy {
    static final Minecraft mc = Minecraft.getMinecraft();
    
    @Override
    void initializeClient() {
        Minecraft.memoryReserve = new byte[0]; // Free up this unused memory. The OOM screen *never* happens.
        FMLCommonHandler.instance().bus().register(this);
        ClientCommandHandler.instance.registerCommand(new MiscClientCommands());
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
}
