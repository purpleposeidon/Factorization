package factorization.scrap;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.*;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.common.MinecraftForge;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

@Help({"Deregisters an event handler from the EventBusses",
        "BusRemove EventClassname EventHandlerClassname"})
public class BusRemove implements IRevertible {
    static final List<EventBus> busses = Arrays.asList(MinecraftForge.EVENT_BUS,
            MinecraftForge.ORE_GEN_BUS,
            MinecraftForge.TERRAIN_GEN_BUS,
            FMLCommonHandler.instance().bus());

    final Class<?> handlerClass;
    final Class<? extends Event> eventClass;
    final Multimap<EventBus, Object> map = ArrayListMultimap.create();

    public BusRemove(Scanner in) throws ClassNotFoundException {
        handlerClass = (Class<? extends Event>) ScannerHelper.nextClass(in);
        String methodName = in.next();
        Class<? extends Event> foundEvent = null;
        for (Method method : handlerClass.getMethods()) {
            if (method.getName().equals(methodName)) {
                Class<?>[] params = method.getParameterTypes();
                if (params.length != 1) continue;
                if (Event.class.isAssignableFrom(params[0])) {
                    foundEvent = (Class<? extends Event>) params[0];
                    break;
                }
            }
        }

        if (foundEvent == null) {
            throw new CompileError("Didn't find method in class: " + handlerClass.getCanonicalName() + "::" + methodName);
        }

        eventClass = foundEvent;

        try {
            setup();
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    void setup() throws Throwable {
        Constructor<? extends Event> eventConstructor = eventClass.getConstructor();
        eventConstructor.setAccessible(true);
        Event testEvent = eventConstructor.newInstance();
        ListenerList listeners = testEvent.getListenerList();
        for (EventBus bus : busses) {
            int busID = ReflectionHelper.getPrivateValue(EventBus.class, bus, "busID");
            for (IEventListener listener : listeners.getListeners(busID)) {
                if (listener instanceof ASMEventHandler) {
                    Object handler = ReflectionHelper.getPrivateValue(ASMEventHandler.class, (ASMEventHandler) listener, "handler");
                    Field instanceField = handler.getClass().getField("instance");
                    instanceField.setAccessible(true);
                    Object instance = instanceField.get(handler);
                    if (instance.getClass() == handlerClass) {
                        map.put(bus, instance);
                    }
                }
            }
        }
    }

    @Override
    public void apply() {
        for (Map.Entry<EventBus, Object> entry : map.entries()) {
            EventBus bus = entry.getKey();
            Object handler = entry.getValue();
            bus.unregister(handler);
        }
    }

    @Override
    public void revert() {
        for (Map.Entry<EventBus, Object> entry : map.entries()) {
            EventBus bus = entry.getKey();
            Object handler = entry.getValue();
            bus.register(handler);
        }
    }

    @Override
    public String info() {
        int reg = 0;
        for (Object x : map.values()) {
            reg++;
        }
        return "BusRemove " + eventClass.getCanonicalName() + " " + handlerClass.getCanonicalName()
                + " # "  + reg + " registered on " + map.size() + " busses";
    }
}
