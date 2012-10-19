package factorization.common;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;

import net.minecraft.src.EntityPlayer;
import net.minecraft.src.EntityPlayerSP;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.ITickHandler;
import cpw.mods.fml.common.Side;
import cpw.mods.fml.common.TickType;
import cpw.mods.fml.common.registry.TickRegistry;
import factorization.api.ExoStateActivation;
import factorization.api.ExoStateShader;
import factorization.api.ExoStateType;

public class ExoCore implements ITickHandler {
    public ExoCore() {
        for (Side side : Side.values()) {
            //We need this for everything. Except for clients that aren't running servers, but there's no distinguishing that.
            TickRegistry.registerTickHandler(this, side);
        }
    }
    
    private static HashMap<String, ExoPlayerState> playerMap = new HashMap();
    public static class ExoPlayerState {
        private ExoStateActivation state[] = new ExoStateActivation[ExoStateType.values().length];
        private boolean toggle[] = new boolean[ExoStateType.values().length];
        private EntityPlayer player;
        private ExoPlayerState(EntityPlayer player) {
            this.player = player;
            for (int i = 0; i < state.length; i++) {
                state[i] = ExoStateActivation.OFF;
            }
            playerMap.put(player.username, this);
        }
        
        private void update() {
            for (int i = 0; i < state.length; i++) {
                if (state[i] == ExoStateActivation.FIRSTON) {
                    state[i] = ExoStateActivation.ON;
                } else if (state[i] == ExoStateActivation.FIRSTOFF) {
                    state[i] = ExoStateActivation.OFF;
                }
            }
            touchState(ExoStateType.EATING, player.isEating());
            touchState(ExoStateType.HURT, player.hurtResistantTime > 0);
            touchState(ExoStateType.WOUNDED, player.getHealth() / (float) player.getMaxHealth() <= 0.333F);
            touchState(ExoStateType.MOVING, Math.abs(player.motionX) + Math.abs(player.motionY) + Math.abs(player.motionZ) > 0.1);
            touchState(ExoStateType.ONFIRE, player.isBurning());
            touchState(ExoStateType.SNEAKING, player.isSneaking());
            touchState(ExoStateType.RIDING, player.isRiding());
            touchState(ExoStateType.SPRINTING, player.isSneaking());
            touchState(ExoStateType.ONGROUND, player.onGround);
            touchState(ExoStateType.INWATER, player.isInWater());
        }
        
        private void touchState(ExoStateType type, boolean isOn) {
            final int i = type.ordinal();
            ExoStateActivation origState = state[i];
            if (isOn != origState.on) {
                if (isOn && !origState.on) {
                    state[i] = ExoStateActivation.FIRSTON; //turn it on
                    toggle[i] = !toggle[i];
                } else if (!isOn && origState.on) {
                    state[i] = ExoStateActivation.FIRSTOFF; //turn it off
                }
            }
        }
        
        public ExoStateActivation getStateActivation(ExoStateType type) {
            return state[type.ordinal()];
        }
        
        public boolean getIsActive(ExoStateType mst, ExoStateShader mss) {
            ExoStateActivation msa = getStateActivation(mst);
            switch (mss) {
            default:
            case NORMAL: return msa.on;
            case INVERSE: return !msa.on;
            case RISINGEDGE: return msa == ExoStateActivation.FIRSTON;
            case FALLINGEDGE: return msa == ExoStateActivation.FIRSTOFF;
            case TOGGLE: return toggle[mst.ordinal()];
            case INVTOGGLE: return !toggle[mst.ordinal()];
            }
        }
    }
    
    private static ExoPlayerState getPlayerState(EntityPlayer player) {
        ExoPlayerState mps = playerMap.get(player.username);
        if (mps == null) {
            mps = new ExoPlayerState(player);
            playerMap.put(player.username, mps);
        }
        return mps;
    }
    
    private static ArrayList<String> toRemove = new ArrayList(2);
    public static void updatePlayerStates() {
        for (String playerName : playerMap.keySet()) {
            ExoPlayerState playerState = playerMap.get(playerName);
            if (playerState.player.isDead) {
                //this also handles logouts, ServerConfigurationManager.playerLoggedOut():207
                //We'll want the state to be reset in those cases.
                toRemove.add(playerName);
            }
        }
        for (String playerName : toRemove) {
            playerMap.remove(playerName);
        }
    }
    
    public static void tickPlayer(EntityPlayer player) {
        if (player.isDead) {
            playerMap.remove(player.username);
            return;
        }
        if (!(player instanceof EntityPlayerSP) && FMLCommonHandler.instance().getSide() != Side.CLIENT) {
            playerMap.remove(player.username);
        }
        ExoPlayerState mps = getPlayerState(player);
        mps.update();
        ExoArmor.onTickPlayer(player, mps);
    }
    
    public static void buttonPressed(EntityPlayer player, int button, boolean isOn) {
        ExoStateType mst = null;
        switch (button) {
        case 0: mst = ExoStateType.BUTTON1; break;
        case 1: mst = ExoStateType.BUTTON2; break;
        case 2: mst = ExoStateType.BUTTON3; break;
        }
        if (mst != null) {
            getPlayerState(player).touchState(mst, isOn);
        }
    }
    
    @Override
    public void tickStart(EnumSet<TickType> type, Object... tickData) {
        if (type.contains(TickType.PLAYER)) {
            EntityPlayer player = (EntityPlayer) tickData[0];
            ExoCore.tickPlayer(player);
        }
        if (type.contains(TickType.WORLD)) {
            ExoCore.updatePlayerStates();
        }
    }

    @Override
    public void tickEnd(EnumSet<TickType> type, Object... tickData) {
    }

    private static EnumSet<TickType> myTicks = EnumSet.of(TickType.PLAYER, TickType.SERVER);
    @Override
    public EnumSet<TickType> ticks() {
        return myTicks;
    }

    @Override
    public String getLabel() {
        return "exo";
    }
}
