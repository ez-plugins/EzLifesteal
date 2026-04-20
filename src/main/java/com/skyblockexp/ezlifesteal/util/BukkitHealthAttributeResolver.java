package com.skyblockexp.ezlifesteal.util;

import com.skyblockexp.ezlifesteal.service.HealthAttributeResolver;
import java.lang.reflect.Field;
import org.bukkit.attribute.Attribute;

/**
 * Resolves max-health attribute constants across Bukkit/Paper versions.
 */
public class BukkitHealthAttributeResolver implements HealthAttributeResolver {

    @Override
    public Attribute resolveMaxHealthAttribute() {
        try {
            final Object value = readGenericMaxHealthField();
            if (value instanceof Attribute attribute) {
                return attribute;
            }
        }
        catch (NoSuchFieldException | IllegalAccessException exception) {
            return Attribute.MAX_HEALTH;
        }
        catch (RuntimeException exception) {
            return Attribute.MAX_HEALTH;
        }
        return Attribute.MAX_HEALTH;
    }

    Object readGenericMaxHealthField() throws NoSuchFieldException, IllegalAccessException {
        return readMaxHealthField(Attribute.class);
    }

    Object readMaxHealthField(Class<?> attributeClass) throws NoSuchFieldException, IllegalAccessException {
        for (Field field : attributeClass.getFields()) {
            if ("GENERIC_MAX_HEALTH".equals(field.getName())) {
                return field.get(null);
            }
        }
        return attributeClass.getField("MAX_HEALTH").get(null);
    }
}
