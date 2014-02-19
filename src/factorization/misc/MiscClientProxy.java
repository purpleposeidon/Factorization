package factorization.misc;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.io.File;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.EnumSet;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.network.INetworkManager;
import net.minecraft.network.NetLoginHandler;
import net.minecraft.network.packet.NetHandler;
import net.minecraft.network.packet.Packet1Login;
import net.minecraft.server.MinecraftServer;
import net.minecraft.stats.AchievementList;
import net.minecraft.stats.StatFileWriter;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.EnumMovingObjectType;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraft.world.WorldProviderHell;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import cpw.mods.fml.client.GuiModList;
import cpw.mods.fml.client.registry.KeyBindingRegistry;
import cpw.mods.fml.client.registry.KeyBindingRegistry.KeyHandler;
import cpw.mods.fml.common.IScheduledTickHandler;
import cpw.mods.fml.common.TickType;
import cpw.mods.fml.common.network.IConnectionHandler;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.Player;
import cpw.mods.fml.common.registry.TickRegistry;
import cpw.mods.fml.relauncher.ReflectionHelper;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import factorization.api.Coord;
import factorization.common.FzConfig;
import factorization.shared.Core;
import factorization.shared.FzUtil;

public class MiscClientProxy extends MiscProxy {
    static final Minecraft mc = Minecraft.getMinecraft();
    
    @Override
    void initializeClient() {
        //give the first achievement, because it is stupid and nobody cares.
        //If you're using this mod, you've probably opened your inventory before anyways.
        StatFileWriter sfw = Minecraft.getMinecraft().statFileWriter;
        if (sfw != null && !sfw.hasAchievementUnlocked(AchievementList.openInventory) && !FzConfig.add_branding) {
            sfw.readStat(AchievementList.openInventory, 1);
            Core.logInfo("Achievement Get! You've opened your inventory hundreds of times already! Yes! You're welcome!");
        }
        Minecraft.memoryReserve = new byte[0]; //Consider it an experiment. Would this break anything? I've *never* seen the out of memory screen.
        MinecraftForge.EVENT_BUS.register(this);
        ClientCommandHandler.instance.registerCommand(new MiscClientCommands());
    }
    
    static boolean setFinalField(Field field, Object instance, Object newValue) {
        try {
            field.setAccessible(true);
            Field modifiersField = Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
            field.set(instance, newValue);
        } catch (Throwable t) {
            t.printStackTrace();
            return false;
        }
        return true;
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
        th = new IScheduledTickHandler() {
            @Override
            public EnumSet<TickType> ticks() {
                return EnumSet.of(TickType.CLIENT);
            }
            
            boolean wasClicked = false;
            
            @Override
            public void tickStart(EnumSet<TickType> type, Object... tickData) {
                MiscClientCommands.tick();
                if (!mc.gameSettings.keyBindPickBlocks.pressed) {
                    wasClicked = false;
                    return;
                }
                EntityPlayer player = mc.thePlayer;
                if (player == null) {
                    return;
                }
                if (player.capabilities.isCreativeMode) {
                    return;
                }
                if (mc.currentScreen != null) {
                    return;
                }
                if (wasClicked) {
                    return;
                }
                wasClicked = true;
                MovingObjectPosition mop = mc.objectMouseOver;
                if (mop == null || mop.typeOfHit != EnumMovingObjectType.TILE) {
                    return;
                }
                Coord here = new Coord(player.worldObj, mop);
                if (vanillaSatisfied(mop, here, player)) {
                    return;
                }
                // Search the inventory for the exact block. Failing that, search for the broken version
                List<ItemStack> validItems = Arrays.asList(here.getPickBlock(mop), here.getBrokenBlock(), new ItemStack(here.getId(), 1, here.getMd()));
                int firstEmpty = -1;
                if (player.getHeldItem() == null) {
                    firstEmpty = player.inventory.currentItem;
                }
                for (int i = 0; i < player.inventory.mainInventory.length; i++) {
                    for (ItemStack needle : validItems) {
                        if (needle == null) {
                            continue;
                        }
                        ItemStack is = player.inventory.mainInventory[i];
                        if (is == null && firstEmpty == -1 && i < 9) {
                            firstEmpty = i;
                        }
                        if (is == null || !FzUtil.couldMerge(needle, is)) {
                            continue;
                        }
                        if (i < 9) {
                            player.inventory.currentItem = i;
                            return;
                        }
                        if (firstEmpty != -1) {
                            player.inventory.currentItem = firstEmpty;
                        }
                        int targetSlot = player.inventory.currentItem;
                        mc.playerController.windowClick(player.inventoryContainer.windowId, i, targetSlot, 2, player);
                        return;
                    }
                }
            }
            
            private Minecraft mc = Minecraft.getMinecraft();
            @Override
            public void tickEnd(EnumSet<TickType> type, Object... tickData) {
                
            }
            
            private boolean vanillaSatisfied(MovingObjectPosition mop, Coord here, EntityPlayer player) {
                ItemStack held = player.inventory.getStackInSlot(player.inventory.currentItem);
                if (held == null) {
                    return false;
                }
                if (FzUtil.couldMerge(held, here.getPickBlock(mop))) {
                    return true;
                }
                if (FzUtil.couldMerge(held, here.getBrokenBlock())) {
                    return true;
                }
                return false;
            }
            
            @Override
            public String getLabel() {
                return "FZMisc PickBlock helper";
            }
            
            @Override
            public int nextTickSpacing() {
                return 1;
            }
        };
        if (FzConfig.fix_middle_click) {
            TickRegistry.registerTickHandler(th, Side.CLIENT);
        }
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
