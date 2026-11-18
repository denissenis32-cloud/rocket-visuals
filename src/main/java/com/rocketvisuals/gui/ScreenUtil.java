package com.rocketvisuals.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Opens screens in a way that survives the 26.x API shuffle
 * (Minecraft#setScreen -> Minecraft#setScreenAndShow -> Gui#setScreen).
 */
public final class ScreenUtil {
    private ScreenUtil() {}

    public static void open(Screen screen) {
        Minecraft mc = Minecraft.getInstance();
        Object gui = readField(mc, "gui");
        if (gui != null && invoke(gui, "setScreen", screen)) return;
        if (invoke(mc, "setScreenAndShow", screen)) return;
        invoke(mc, "setScreen", screen);
    }

    /** Opens vanilla Options > Controls > Key Binds (resolved reflectively), returning to {@code parent}. */
    public static void openKeyBinds(Screen parent) {
        Minecraft mc = Minecraft.getInstance();
        String[] names = {
                "net.minecraft.client.gui.screens.options.controls.KeyBindsScreen",
                "net.minecraft.client.gui.screens.controls.KeyBindsScreen"
        };
        for (String n : names) {
            try {
                Class<?> cls = Class.forName(n);
                for (Constructor<?> k : cls.getConstructors()) {
                    Class<?>[] pt = k.getParameterTypes();
                    if (pt.length == 2 && pt[0].isAssignableFrom(Screen.class) && pt[1].isInstance(mc.options)) {
                        open((Screen) k.newInstance(parent, mc.options));
                        return;
                    }
                }
            } catch (ReflectiveOperationException ignored) { }
        }
    }

    private static Object readField(Object o, String name) {
        try {
            Field f = o.getClass().getDeclaredField(name);
            f.setAccessible(true);
            return f.get(o);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    private static boolean invoke(Object target, String name, Screen arg) {
        for (Method m : target.getClass().getMethods()) {
            if (!m.getName().equals(name) || m.getParameterCount() != 1) continue;
            if (!m.getParameterTypes()[0].isAssignableFrom(Screen.class)) continue;
            try {
                m.invoke(target, arg);
                return true;
            } catch (ReflectiveOperationException | RuntimeException ignored) { }
        }
        return false;
    }
}
