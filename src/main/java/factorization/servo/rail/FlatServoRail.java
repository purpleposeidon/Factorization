package factorization.servo.rail;

import factorization.api.Coord;
import factorization.api.FzColor;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.api.datahelpers.Share;
import factorization.charge.enet.WireCharge;
import factorization.flat.AbstractFlatWire;
import factorization.flat.api.Flat;
import factorization.flat.api.FlatFace;
import factorization.flat.api.IFlatModel;
import factorization.flat.api.IModelMaker;
import factorization.notify.Notice;
import factorization.servo.ServoFeature;
import factorization.servo.instructions.GenericPlaceholder;
import factorization.shared.Core;
import factorization.shared.FzModel;
import factorization.util.ItemUtil;
import factorization.util.PlayerUtil;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Locale;

public class FlatServoRail extends AbstractFlatWire {
    public FzColor color = FzColor.NO_COLOR;
    @Nonnull
    public Decorator component = GenericPlaceholder.INSTANCE;

    public Decorator getComponent() {
        return component;
    }

    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        color = data.as(Share.VISIBLE, "color").putEnum(color);
        component = data.as(Share.VISIBLE, "component").putIDS(component);
        return this;
    }

    public static transient int SPECIES = Flat.nextSpeciesId();
    @Override
    public int getSpecies() {
        return SPECIES;
    }

    class RailModel implements IFlatModel {
        final IFlatModel parent;

        RailModel(IFlatModel parent) {
            this.parent = parent;
        }

        @Override
        @SideOnly(Side.CLIENT)
        public IBakedModel[] getModel(Coord at, EnumFacing side) {
            IBakedModel[] parentModels = parent.getModel(at, side);
            IBakedModel[] componentModels = null;
            int retLen;
            {
                retLen = color == FzColor.NO_COLOR ? 0 : 1;
                boolean nullComponent = component instanceof GenericPlaceholder;
                if (!nullComponent) {
                    IFlatModel cm = component.getModel(at, side);
                    if (cm != null) {
                        componentModels = cm.getModel(at, side);
                        retLen += componentModels.length;
                    }
                }
                if (retLen == 0) return parentModels;
                retLen += parentModels.length;
            }
            IBakedModel[] ret = new IBakedModel[retLen];
            int i = 0;
            for (IBakedModel m : parentModels) {
                ret[i++] = m;
            }
            if (color != FzColor.NO_COLOR) {
                ret[i++] = stains[color.toVanillaColorIndex()].model;
            }
            if (componentModels != null) {
                for (IBakedModel m : componentModels) {
                    ret[i++] = m;
                }
            }
            return ret;
        }
    }

    static FzModel[] stains = new FzModel[FzColor.VALID_COLORS.length];
    @Override
    public void loadModels(IModelMaker maker) {
        super.loadModels(maker);
        if (this != ServoFeature.static_rail) return;
        for (FzColor color : FzColor.VALID_COLORS) {
            stains[color.toVanillaColorIndex()] = new FzModel(new ResourceLocation("factorization:flat/servorail/stain_" + color.name().toLowerCase(Locale.ROOT)));
        }
    }

    @Nullable
    @Override
    public IFlatModel getModel(Coord at, EnumFacing side) {
        return new RailModel(super.getModel(at, side));
    }

    @Override
    protected String getModelGroupName() {
        return "factorization:flat/servorail/m";
    }

    @Override
    public boolean shouldBeConnectedToBy(FlatFace wire) {
        return super.shouldBeConnectedToBy(wire) || (component.connectWires() && wire.getSpecies() == WireCharge.SPECIES);
    }

    @Override
    public boolean canInteract(Coord at, EnumFacing side, EntityPlayer player) {
        ItemStack heldItem = player.getHeldItem();
        return super.canInteract(at, side, player)
                || ItemUtil.is(heldItem, Core.registry.logicMatrixProgrammer)
                || FzColor.fromItem(heldItem) != FzColor.NO_COLOR
                || (heldItem != null && heldItem.getItem() instanceof ItemServoRailWidget);
    }

    /** Convert a static instance into a dynamic one */
    private FlatServoRail expand() {
        if (isDynamic()) return this;
        FlatServoRail ret = new FlatServoRail();
        ret.component = this.component;
        ret.color = this.color;
        return ret;
    }

    /** Convert a dynamic instance into a static one */
    private FlatServoRail flatten() {
        // TODO: Should this use serialize() instead?
        if (isStatic()) {
            return this;
        }
        if (color == FzColor.NO_COLOR && component instanceof GenericPlaceholder) {
            return ServoFeature.static_rail;
        }
        return this;
    }

    @Override
    public void onActivate(Coord at, EnumFacing side, EntityPlayer player) {
        FlatServoRail me = this.expand();
        me.onActivateUnpacked(at, side, player);
        Flat.set(at, side, me.flatten());
    }

    protected void onActivateUnpacked(Coord at, EnumFacing side, EntityPlayer player) {
        ItemStack held = player.getHeldItem();
        FzColor heldColor = FzColor.fromItem(held);
        boolean hasLmp = ItemUtil.is(held, Core.registry.logicMatrixProgrammer);
        if (!hasLmp && heldColor != FzColor.NO_COLOR) {
            color = heldColor;
            return;
        }
        boolean componentIsNull = component instanceof GenericPlaceholder;
        if (player.isSneaking() && !componentIsNull && hasLmp) {
            if (!component.isFreeToPlace() && !PlayerUtil.isPlayerCreative(player)) {
                at.spawnItem(component.toItem());
            }
            component = GenericPlaceholder.INSTANCE;
            return;
        }
        if (!componentIsNull || held == null) return;
        if (held.getItem() instanceof ItemServoRailWidget) {
            ServoComponent sc = ((ItemServoRailWidget) held.getItem()).get(held);
            if (sc != null) {
                component = (Decorator) sc;
                if (!(component.isFreeToPlace())) {
                    PlayerUtil.cheatDecr(player, held);
                }
            }
        }
        String info = component.getInfo();
        if (info != null) {
            new Notice(at, info).sendTo(player);
        }
    }
}
