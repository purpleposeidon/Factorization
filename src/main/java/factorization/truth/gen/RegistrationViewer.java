package factorization.truth.gen;

import com.google.common.collect.BiMap;
import factorization.truth.api.IDocGenerator;
import factorization.truth.api.ITypesetter;
import factorization.truth.api.TruthError;
import net.minecraft.entity.Entity;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

public class RegistrationViewer implements IDocGenerator {
    @Override
    public void process(ITypesetter out, String arg) throws TruthError {
        if (arg.equalsIgnoreCase("entity")) {
            BiMap<Class<? extends Entity>, EntityRegistry.EntityRegistration> registrationMap;
            registrationMap = ReflectionHelper.getPrivateValue(EntityRegistry.class, EntityRegistry.instance(), "entityClassRegistrations");
            for (EntityRegistry.EntityRegistration reg : registrationMap.values()) {
                out.write("\\u{" + reg.getEntityName() + "}");
                out.write("\\nlRegistered by: " + reg.getContainer().getDisplayVersion());
                out.write("\\nTracking range: " + reg.getTrackingRange());
                out.write("\\nlLocation sync frequency: " + reg.getUpdateFrequency());
                out.write("\\nlSends velocity updates: " + reg.sendsVelocityUpdates());
                out.write("\\nl");
            }
        } else if (arg.equalsIgnoreCase("tileentity")) {
            //See: GameRegistry.registerTileEntity();
            out.write("TODO: too lazy to add an AT just for this...");
        } else {
            for (String s : new String[] {
                    "entity",
                    "tileentity"
            }) {
                out.write("\\{cgi/registry/" + s + "}{" + s +"}\n\n");
            }
        }
    }
}
