package factorization.scrap;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.relauncher.Side;
import factorization.shared.Core;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

@Mod(
        modid = "fz.scrap",
        version = Core.version,
        name = "Scrap"
)
public class ScrapManager {
    public static Logger log;

    @Mod.EventHandler
    public void registerActions(FMLPreInitializationEvent event) {
        log = event.getModLog();
        actionClasses.put("SetMaxDamage", SetMaxDamage.class);
        actionClasses.put("SetMaxSize", SetMaxSize.class);
        actionClasses.put("BusRemove", BusRemove.class);
        actionClasses.put("Script", Script.class);
        if (FMLCommonHandler.instance().getSide() == Side.CLIENT) {
            actionClasses.put("DeregisterTesr", DeregisterTesr.class);
        }
    }

    @Mod.EventHandler
    public void registerCommands(FMLServerStartingEvent event) {
        event.registerServerCommand(new ScrapCommand());
    }

    @Mod.EventHandler
    public void runScript(FMLServerStartedEvent event) {
        try {
            runScript("user", false);
        } catch (CompileError e) {
            // ignored
        }
    }

    public static void runScript(String filename, boolean createIfMissing) {
        run(new Script(filename, createIfMissing));
    }

    public static void run(IRevertible action) {
        actions.add(action);
        action.apply();
    }

    public static String call(String line) {
        IRevertible action = compile(line);
        run(action);
        return action.info();
    }

    public static String undo() {
        if (actions.isEmpty()) return "nothing to undo";
        IRevertible action = actions.remove(actions.size() - 1);
        action.revert();
        return "Undone: " + action.info();
    }

    public static String reset() {
        for (int i = actions.size() - 1; i >= 0; i--) {
            final IRevertible action = actions.get(i);
            action.revert();
        }
        actions.clear();
        return "Reset";
    }

    public static String reload() {
        reset();
        runScript("user", false);
        return "Reloaded";
    }


    static final ArrayList<IRevertible> actions = new ArrayList<IRevertible>();
    public static final HashMap<String, Class<? extends IRevertible>> actionClasses = new HashMap();

    public static IRevertible compile(String src) throws CompileError{
        Scanner scanner = new Scanner(src);
        String actionName = scanner.next();
        Class<? extends IRevertible> actionClass = actionClasses.get(actionName);
        if (actionClass == null) {
            throw new CompileError("Unknown action: " + actionName);
        }
        Constructor<? extends IRevertible> csn;
        try {
            csn = actionClass.getConstructor(Scanner.class);
            csn.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new CompileError("Invalid action class: " + actionName);
        }
        try {
            return csn.newInstance(scanner);
        } catch (CompileError t) {
            throw t;
        } catch (Throwable t) {
            throw new CompileError("Load failed", t);
        }
    }
}
