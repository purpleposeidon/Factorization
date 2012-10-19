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
import factorization.api.MechaStateActivation;
import factorization.api.MechaStateShader;
import factorization.api.MechaStateType;

public class MechaCore implements ITickHandler {
    public MechaCore() {
        for (Side side : Side.values()) {
            //We need this for everything. Except for clients that aren't running servers, but there's no distinguishing that.
            TickRegistry.registerTickHandler(this, side);
        }
    }
    
    private static HashMap<String, MechaPlayerState> playerMap = new HashMap();
    public static class MechaPlayerState {
        private MechaStateActivation state[] = new MechaStateActivation[MechaStateType.values().length];
        private boolean toggle[] = new boolean[MechaStateType.values().length];
        private EntityPlayer player;
        private MechaPlayerState(EntityPlayer player) {
            this.player = player;
            for (int i = 0; i < state.length; i++) {
                state[i] = MechaStateActivation.OFF;
            }
            playerMap.put(player.username, this);
        }
        
        private void update() {
            for (int i = 0; i < state.length; i++) {
                if (state[i] == MechaStateActivation.FIRSTON) {
                    state[i] = MechaStateActivation.ON;
                } else if (state[i] == MechaStateActivation.FIRSTOFF) {
                    state[i] = MechaStateActivation.OFF;
                }
            }
            touchState(MechaStateType.EATING, player.isEating());
            touchState(MechaStateType.HURT, player.hurtResistantTime > 0);
            touchState(MechaStateType.WOUNDED, player.getHealth() / (float) player.getMaxHealth() <= 0.333F);
            touchState(MechaStateType.MOVING, Math.abs(player.motionX) + Math.abs(player.motionY) + Math.abs(player.motionZ) > 0.1);
            touchState(MechaStateType.ONFIRE, player.isBurning());
            touchState(MechaStateType.SNEAKING, player.isSneaking());
            touchState(MechaStateType.RIDING, player.isRiding());
            touchState(MechaStateType.SPRINTING, player.isSneaking());
            touchState(MechaStateType.ONGROUND, player.onGround);
            touchState(MechaStateType.INWATER, player.isInWater());
        }
        
        private void touchState(MechaStateType type, boolean isOn) {
            final int i = type.ordinal();
            MechaStateActivation origState = state[i];
            if (isOn != origState.on) {
                if (isOn && !origState.on) {
                    state[i] = MechaStateActivation.FIRSTON; //turn it on
                    toggle[i] = !toggle[i];
                } else if (!isOn && origState.on) {
                    state[i] = MechaStateActivation.FIRSTOFF; //turn it off
                }
            }
        }
        
        public MechaStateActivation getStateActivation(MechaStateType type) {
            return state[type.ordinal()];
        }
        
        public boolean getIsActive(MechaStateType mst, MechaStateShader mss) {
            MechaStateActivation msa = getStateActivation(mst);
            switch (mss) {
            default:
            case NORMAL: return msa.on;
            case INVERSE: return !msa.on;
            case RISINGEDGE: return msa == MechaStateActivation.FIRSTON;
            case FALLINGEDGE: return msa == MechaStateActivation.FIRSTOFF;
            case TOGGLE: return toggle[mst.ordinal()];
            case INVTOGGLE: return !toggle[mst.ordinal()];
            }
        }
    }
    
    private static MechaPlayerState getPlayerState(EntityPlayer player) {
        MechaPlayerState mps = playerMap.get(player.username);
        if (mps == null) {
            mps = new MechaPlayerState(player);
            playerMap.put(player.username, mps);
        }
        return mps;
    }
    
    private static ArrayList<String> toRemove = new ArrayList(2);
    public static void updatePlayerStates() {
        for (String playerName : playerMap.keySet()) {
            MechaPlayerState playerState = playerMap.get(playerName);
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
        MechaPlayerState mps = getPlayerState(player);
        mps.update();
        MechaArmor.onTickPlayer(player, mps);
    }
    
    public static void buttonPressed(EntityPlayer player, int button, boolean isOn) {
        MechaStateType mst = null;
        switch (button) {
        case 0: mst = MechaStateType.BUTTON1; break;
        case 1: mst = MechaStateType.BUTTON2; break;
        case 2: mst = MechaStateType.BUTTON3; break;
        }
        if (mst != null) {
            getPlayerState(player).touchState(mst, isOn);
        }
    }
    
    @Override
    public void tickStart(EnumSet<TickType> type, Object... tickData) {
        if (type.contains(TickType.PLAYER)) {
            EntityPlayer player = (EntityPlayer) tickData[0];
            MechaCore.tickPlayer(player);
        }
        if (type.contains(TickType.WORLD)) {
            MechaCore.updatePlayerStates();
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
        return "mecha";
    }
}
