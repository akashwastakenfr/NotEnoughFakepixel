package org.ginafro.notenoughfakepixel.features.duels;
import com.google.gson.annotations.Expose;
import org.ginafro.notenoughfakepixel.config.features.ChocolateFactory;
import org.ginafro.notenoughfakepixel.config.gui.core.config.annotations.*;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class Duels {
    @Expose
    private Map<String, Object> staticFieldValues = new HashMap<>();
    // Duels K/D Counter (subcategoryId = 1)
    @Expose
    @ConfigOption(name = "K/D Counter", desc = "Enable the K/D counter in Duels.", subcategoryId = 1)
    @ConfigEditorBoolean
    public static boolean kdCounterEnabled = true;

    @Expose
    @ConfigOption(name = "K/D Counter Offset X", desc = "Horizontal offset of the K/D counter.", subcategoryId = 1)
    @ConfigEditorSlider(minValue = 0.0f, maxValue = 1800.0f, minStep = 1.0f)
    public static float kdCounterOffsetX = 10.0f;

    @Expose
    @ConfigOption(name = "K/D Counter Offset Y", desc = "Vertical offset of the K/D counter.", subcategoryId = 1)
    @ConfigEditorSlider(minValue = 0.0f, maxValue = 1250.0f, minStep = 1.0f)
    public static float kdCounterOffsetY = 10.0f;

    @Expose
    @ConfigOption(name = "K/D Counter Scale", desc = "Scale of the K/D counter text.", subcategoryId = 1)
    @ConfigEditorSlider(minValue = 0.5f, maxValue = 5.0f, minStep = 0.1f)
    public static float kdCounterScale = 1.0f;

    @Expose
    @ConfigOption(name = "Edit K/D Counter Position", desc = "Adjust the K/D counter position visually", subcategoryId = 1)
    @ConfigEditorButton(runnableId = "editKdCounterPosition", buttonText = "Edit Position")
    public static String editKdCounterPositionButton = "";

    // Sync static fields to the map before saving
    public void saveStaticFields() {
        try {
            staticFieldValues.clear();
            for (Field field : Duels.class.getDeclaredFields()) {
                if (field.isAnnotationPresent(Expose.class) && field.isAnnotationPresent(ConfigOption.class)) {
                    field.setAccessible(true);
                    staticFieldValues.put(field.getName(), field.get(null)); // null because static
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Load static fields from the map after deserialization
    public void loadStaticFields() {
        try {
            for (Field field : Duels.class.getDeclaredFields()) {
                if (field.isAnnotationPresent(Expose.class) && field.isAnnotationPresent(ConfigOption.class)) {
                    field.setAccessible(true);
                    Object value = staticFieldValues.get(field.getName());
                    if (value != null) {
                        if (field.getType() == int.class && value instanceof Number) {
                            field.setInt(null, ((Number) value).intValue());
                        } else if (field.getType() == boolean.class && value instanceof Boolean) {
                            field.setBoolean(null, (Boolean) value);
                        } else {
                            field.set(null, value);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

