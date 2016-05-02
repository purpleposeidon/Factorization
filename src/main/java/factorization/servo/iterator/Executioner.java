package factorization.servo.iterator;

import factorization.api.Coord;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.Share;
import factorization.notify.Notice;
import factorization.servo.instructions.IntegerValue;
import factorization.servo.rail.Instruction;
import factorization.servo.rail.TileEntityServoRail;

import java.io.IOException;

public class Executioner {
    private transient final ServoMotor motor;
    
    public static final byte JMP_NONE = 0, JMP_NEXT_INSTRUCTION = 1, JMP_NEXT_TILE = 2;
    
    protected ServoStack argumentStack = new ServoStack(this);
    protected ServoStack pendingInstructions = new ServoStack(this);
    protected ServoStack enterBlockInstructions = new ServoStack(this);
    
    public byte jmp = JMP_NONE;
    public boolean cpu_blocked = false;
    public EntryAction entry_action = EntryAction.ENTRY_EXECUTE;
    
    transient boolean stacks_changed = false;
    
    
    public Executioner(ServoMotor motor) {
        this.motor = motor;
    }
    
    public void tick() {
        if (cpu_blocked) return;
        boolean found_blocking_instruction = false;
        while (pendingInstructions.getSize() > 0) {
            Object obj = pendingInstructions.peek();
            if (!(obj instanceof Instruction)) {
                argumentStack.push(pendingInstructions.pop());
                continue;
            }
            Instruction insn = (Instruction) obj;
            switch (insn.getBlockingBehavior()) {
            case BLOCK_UNTIL_NEXT_ENTRY:
                cpu_blocked = true; //$FALL-THROUGH$
            case BLOCK_FOR_TICK:
                if (found_blocking_instruction) {
                    return;
                }
                found_blocking_instruction = true;
                break;
            default:
            case NO_BLOCKING:
                break;
            }
            pendingInstructions.pop();
            insn.motorHit(motor);
        }
    }
    
    public void onEnterNewBlock(TileEntityServoRail rail) {
        cpu_blocked = false;
        switch (entry_action) {
        default:
        case ENTRY_EXECUTE:
            if (rail == null /* :| */ || rail.decoration == null) {
                if (jmp == JMP_NEXT_TILE) {
                    jmp = JMP_NONE;
                }
                break;
            }
            if (motor.getCurrentPos().isWeaklyPowered()) {
                if (jmp == JMP_NEXT_TILE) {
                    jmp = JMP_NONE;
                }
                break;
            }
            if (jmp != JMP_NONE) {
                jmp = JMP_NONE;
                break;
            }
            rail.decoration.motorHit(motor);
            break;
        case ENTRY_LOAD:
            if (rail.decoration instanceof Instruction) {
                argumentStack.push(rail.decoration.copyComponent());
            }
            break;
        case ENTRY_WRITE:
            ServoStack ss = argumentStack;
            if (ss.getSize() == 0) {
                motor.putError("IO stack is emtpy!");
                break;
            }
            Object o = ss.pop();
            if (o instanceof Instruction) {
                rail.decoration = (Instruction) ((Instruction) o).copyComponent();
                new Coord(rail).markBlockForUpdate();
            } else if (o instanceof Integer) {
                int val = (Integer) o;
                IntegerValue iv = new IntegerValue();
                iv.setVal(val);
                rail.decoration = iv;
                new Coord(rail).markBlockForUpdate();
            } else {
                motor.putError("Can't write " + o + ", sorry!");
            }
            if (ss.getSize() == 0) {
                entry_action = EntryAction.ENTRY_IGNORE;
            }
            break;
        case ENTRY_IGNORE:
            break;
        }
        if (enterBlockInstructions.getSize() > 0) {
            for (Object obj : enterBlockInstructions) {
                pendingInstructions.push(obj);
            }
        }
    }
    
    void putData(DataHelper data) throws IOException {
        argumentStack = data.as(Share.VISIBLE, "stack4" /* 4 is for compatibility */).putIDS(argumentStack);
        pendingInstructions = data.as(Share.VISIBLE, "pendingInstructions").putIDS(pendingInstructions);
        enterBlockInstructions = data.as(Share.VISIBLE, "enterBlockInstructions").putIDS(enterBlockInstructions);
        jmp = data.as(Share.VISIBLE, "jmp").putByte(jmp);
        cpu_blocked = data.as(Share.VISIBLE, "cpuBlock").putBoolean(cpu_blocked);
        entry_action = data.as(Share.VISIBLE, "entryAction").putEnum(entry_action);
    }
    
    public ServoStack getArgStack() {
        return argumentStack;
    }
    
    public ServoStack getInstructionStack() {
        return pendingInstructions;
    }
    
    public ServoStack getEntryInstructionStack() {
        return enterBlockInstructions;
    }
    
    public void putError(Object error) {
        if (!motor.worldObj.isRemote) {
            new Notice(motor, "%s", error.toString()).sendToAll();
        }
    }
    
    public void markDirty() {
        stacks_changed = true;
    }
    
    public void setEntryInstruction(Object insn) {
        enterBlockInstructions.clear();
        enterBlockInstructions.push(insn);
    }
}
