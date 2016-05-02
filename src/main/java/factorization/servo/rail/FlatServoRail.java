package factorization.servo.rail;

import factorization.api.Coord;
import factorization.api.FzColor;
import factorization.api.datahelpers.DataHelper;
import factorization.api.datahelpers.IDataSerializable;
import factorization.api.datahelpers.Share;
import factorization.flat.AbstractFlatWire;
import factorization.flat.api.Flat;
import factorization.flat.api.IFlatModel;
import factorization.flat.api.IModelMaker;
import factorization.servo.ServoFeature;
import factorization.servo.instructions.GenericPlaceholder;
import factorization.shared.FzModel;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Locale;

public class FlatServoRail extends AbstractFlatWire {
    public FzColor color = FzColor.NO_COLOR;
    public ServoComponent component = GenericPlaceholder.INSTANCE;

    public ServoComponent getComponent() {
        return component;
    }

    @Override
    public IDataSerializable serialize(String prefix, DataHelper data) throws IOException {
        color = data.as(Share.VISIBLE, "color").putEnum(color);
        component = data.as(Share.VISIBLE, "component").putIDS(component);
        return this;
    }

    protected static transient int SPECIES = Flat.nextSpeciesId();
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
}
