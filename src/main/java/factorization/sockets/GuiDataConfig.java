package factorization.sockets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;

import org.lwjgl.input.Keyboard;

import factorization.api.Coord;
import factorization.api.datahelpers.DataBackup;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.DataOutPacket;
import factorization.api.datahelpers.DataOutByteBufEdited;
import factorization.api.datahelpers.DataValidator;
import factorization.api.datahelpers.IDataSerializable;
import factorization.api.datahelpers.Share;
import factorization.shared.Core;
import factorization.shared.FzNetDispatch;
import factorization.shared.NetworkFactorization.MessageType;

public class GuiDataConfig extends GuiScreen {
    IDataSerializable ids;
    TileEntity te;
    Entity containingEntity;
    
    ArrayList<Field> fields = new ArrayList();
    int posLabel, posControl;
    boolean changed;
    boolean orig_f1_state;;
    
    static class UsefulButton extends GuiButton {
        static FontRenderer fontRenderer = Minecraft.getMinecraft().fontRenderer;
        public UsefulButton(int id, int xPos, int yPos, String text) {
            super(id, xPos, yPos - 4, fontRenderer.getStringWidth(text) + 8, 20, text);
        }

        public boolean isHovered() { return field_146123_n; }
    }
    
    class Field {
        String name;
        Object object;
        Class objectType;
        int posY;
        String label;
        int color = 0xFFFFFF;
        
        ArrayList<UsefulButton> buttons = new ArrayList();
        int buttonPos = 0;
        
        public Field(String name, Object object, int posY) {
            this.name = name;
            this.object = object;
            this.objectType = object.getClass();
            this.posY = posY;
            this.label = Core.translate(getTrans());
            if (this.getClass() == Field.class) {
                color = 0xAAAAAA;
            }
        }
        
        String getTrans() {
            return "data.label." + ids.getClass().getSimpleName() + "." + name;
        }
        
        void initGui() {
            buttonPos = posControl;
            buttons.clear();
        }
        
        UsefulButton button(int delta, String text) {
            UsefulButton button = new UsefulButton(delta, buttonPos, posY - 2, text);
            buttons.add(button);
            buttonPos += fontRendererObj.getStringWidth(button.displayString + 10);
            return button;
        }
        
        void render(int mouseX, int mouseY) {
            renderLabel();
            renderControl(mouseX, mouseY);
            for (UsefulButton button : buttons) {
                button.drawButton(mc, mouseX, mouseY);
            }
        }
        
        void renderLabel() {
            drawString(fontRendererObj, label, posLabel, posY, color);
        }
        
        void renderControl(int mouseX, int mouseY) { }
        
        int getLabelWidth() {
            return fontRendererObj.getStringWidth(label) + 80;
        }
        
        void mouseClick(int mouseX, int mouseY, boolean rightClick) {
            for (UsefulButton button : buttons) {
                if (button.isHovered()) {
                    buttonPressed(button, rightClick);
                    break;
                }
            }
        }
        
        void put(Map<String, Object> map) {
            map.put(name, object);
        }
        
        void keyTyped(int keysym, char ch) {}
        void buttonPressed(UsefulButton button, boolean rightClick) {}
    }
    
    class BooleanField extends Field {
        boolean val;
        UsefulButton button;
        public BooleanField(String name, Object object, int posY) {
            super(name, object, posY);
            val = (Boolean) object;
        }
        
        @Override
        void initGui() {
            super.initGui();
            button = button(0, Core.tryTranslate(getTrans() + "." + val, "" + val));
        }
        
        @Override
        void buttonPressed(UsefulButton button, boolean rightClick) {
            val = !val;
            object = val;
            initGui();
        }
    }
    
    class NumberField extends Field {
        long val;
        int labelPos;
        
        public NumberField(String name, Object object, int posY) {
            super(name, object, posY);
            Number n = (Number) object;
            val = n.intValue();
        }
        
        String transVal() {
            return Core.tryTranslate(getTrans() + "." + val, "" + val);
        }
        
        @Override
        void initGui() {
            super.initGui();
            button(-10, "-10");
            button(-1, "-");
            labelPos = buttonPos;
            buttonPos += fontRendererObj.getStringWidth(transVal()) + 5;
            button(1, "+");
            button(10, "+10");
        }
        
        @Override
        void renderControl(int mouseX, int mouseY) {
            drawString(fontRendererObj, transVal(), this.labelPos, posY, color);
        }
        
        @Override
        void keyTyped(int keysym, char ch) {
            if (keysym == Keyboard.KEY_BACK) {
                val /= 10;
            } else {
                try {
                    int digit = Integer.parseInt(Character.toString(ch));
                    val = val*10 + digit;
                } catch (NumberFormatException e) {}
            }
            object = val;
            initGui();
        }
        
        @Override
        void buttonPressed(UsefulButton button, boolean rightClick) {
            val += button.id;
            object = val;
            initGui();
        }
        
        @Override
        void put(Map<String, Object> map) {
            Object obj = object;
            if (objectType == Long.class) {
                obj = new Long((long) val);
            } else if (objectType == Integer.class) {
                obj = new Integer((int) val);
            } else if (objectType == Short.class) {
                obj = new Short((short) val);
            } else if (objectType == Byte.class) {
                obj = new Byte((byte) val);
            } //else: gonna crash, uh-oh...
            map.put(name, obj);
        }
    }
    
    class EnumField<E extends Enum> extends Field {
        E val;
        UsefulButton button;
        public EnumField(String name, E object, int posY) {
            super(name, object, posY);
            val = object;
        }
        
        @Override
        void initGui() {
            super.initGui();
            button = button(0, Core.tryTranslate(getTrans() + "." + val, "" + val));
        }
        
        @Override
        void buttonPressed(UsefulButton button, boolean rightClick) {
            int ord = val.ordinal();
            E[] family = (E[]) val.getClass().getEnumConstants();
            if (rightClick) {
                ord = ord == 0 ? family.length - 1 : ord - 1;
            } else {
                ord = ord + 1 == family.length ? 0 : ord + 1;
            }
            object = val = family[ord];
            initGui();
        }
        
        @Override
        void put(Map<String, Object> map) {
            map.put(name, val.ordinal());
        }
    }
    
    public GuiDataConfig(IDataSerializable ids) {
        this.ids = ids;
        this.te = (TileEntity) ids;
        mc = Minecraft.getMinecraft();
        orig_f1_state = mc.gameSettings.hideGUI;
        mc.gameSettings.hideGUI = true;
    }
    
    public GuiDataConfig(IDataSerializable ids, Entity container) {
        this.ids = ids;
        this.te = (TileEntity) ids;
        this.containingEntity = container;
        mc = Minecraft.getMinecraft();
        orig_f1_state = mc.gameSettings.hideGUI;
        mc.gameSettings.hideGUI = true;
    }
    
    void closeScreen() {
        keyTyped('\0', 1);
    }
    
    
    boolean fields_initialized = false;
    boolean fields_valid = false;
    
    @Override
    public void initGui() {
        super.initGui();
        if (!fields_initialized) {
            //This can't happen in the constructor because we can't close the GUI from the constructor.
            fields_initialized = true;
            try {
                initFields();
            } catch (IOException e) {
                e.printStackTrace();
                closeScreen();
            }
            fields_valid = true;
        }
        posLabel = 40;
        posControl = posLabel + 20;
        for (Field field : fields) {
            posControl = Math.max(posControl, field.getLabelWidth() + 20);
        }
        
        for (Field field : fields) {
            field.initGui();
        }
    }
    
    void initFields() throws IOException {
        fields.clear();
        ids.serialize("", new DataHelper() {
            int count = 0;
            
            @Override
            protected boolean shouldStore(Share share) {
                return share.is_public && share.client_can_edit;
            }
            
            @Override
            public <E> E put(E o) throws IOException {
                if (!valid) {
                    return o;
                }
                if (o instanceof Enum) {
                    return (E) putImplementation(o);
                }
                return super.put(o);
            }
            
            @Override
            protected <E> E putImplementation(E o) throws IOException {
                if (!valid) {
                    return o;
                }
                int fieldStart = 100;
                int fieldHeight = 24;
                int posY = count*fieldHeight + fieldStart;
                if (o instanceof Boolean) {
                    fields.add(new BooleanField(name, o, posY));
                } else if (o instanceof Number) {
                    fields.add(new NumberField(name, o, posY));
                } else if (o instanceof Enum) {
                    fields.add(new EnumField<Enum>(name, (Enum) o, posY));
                } else {
                    fields.add(new Field(name, o, posY));
                }
                count++;
                return o;
            }
            
            @Override
            public boolean isReader() {
                return true;
            }

            @Override
            public ItemStack[] putItemArray(ItemStack[] value) throws IOException {
                return value;
            }
        });
    }
    
    boolean validate(ArrayList<Field> fields) {
        HashMap<String, Object> data = new HashMap();
        for (Field f : fields) {
            f.put(data);
        }
        DataValidator dv = new DataValidator(data);
        try {
            ids.serialize("", dv);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return dv.isValid();
    }
    
    @Override
    public void drawScreen(int mouseX, int mouseY, float partial) {
        //drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partial);
        
        TextureManager tex = Minecraft.getMinecraft().renderEngine;
        tex.bindTexture(Core.itemAtlas);
        
        IIcon lmp = Core.registry.logicMatrixProgrammer.getIconFromDamage(0);
        
        int w = 256;
        int xSize = w, ySize = xSize;
        int left = (width - 6) / 2;
        int top = (height - ySize) / 2;
        
        drawTexturedModelRectFromIcon(left, top, lmp, xSize, ySize);
        
        for (Field field : fields) {
            field.render(mouseX, mouseY);
        }
    }
    
    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        for (Field field : fields) {
            field.mouseClick(mouseX, mouseY, button == 1);
        }
        valueChanged();
        super.mouseClicked(mouseX, mouseY, button);
    }
    
    void valueChanged() {
        DataBackup origValues = new DataBackup();
        try {
            ids.serialize("", origValues);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        
        applyChangesToEntity();
        
        origValues.restoring();
        try {
            ids.serialize("", origValues);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
    }
    
    void applyChangesToEntity() {
        if (validate(fields)) {
            fields_valid = true;
        } else {
            fields_valid = false;
        }
        
        fields_initialized = false;
        initGui();
        changed = true;
    }
    
    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
    
    @Override
    protected void keyTyped(char chr, int keySym) {
        super.keyTyped(chr, keySym);
        if (keySym == 1) {
            return;
        }
    }
    
    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        mc.gameSettings.hideGUI = orig_f1_state;
        if (!fields_valid || !changed) {
            return;
        }
        applyChangesToEntity();
        try {
            sendPacket();
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
    }
    
    void sendPacket() throws IOException {
        ByteBuf buf = Unpooled.buffer();
        DataHelper dop = new DataOutByteBufEdited(buf);
        Coord here = new Coord((TileEntity) ids);
        if (containingEntity == null) {
            Core.network.prefixTePacket(buf, here, MessageType.DataHelperEdit);
            ids.serialize("", dop);
            Core.network.broadcastPacket(mc.thePlayer, here, FzNetDispatch.generate(buf));
        } else {
            Core.network.prefixEntityPacket(buf, containingEntity, MessageType.DataHelperEditOnEntity);
            ids.serialize("", dop);
            Core.network.broadcastPacket(mc.thePlayer, here, Core.network.entityPacket(buf));
        }
    }
}
