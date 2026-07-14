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
        catch (NoSuchFieldException | IllegalAccessException | RuntimeException ignored) {
            // Fall back to legacy field names across server variants.
        }
        final Object fallbackValue = findAttributeByFieldNames("MAX_HEALTH", "GENERIC_MAX_HEALTH");
        if (fallbackValue instanceof Attribute attribute) {
            return attribute;
        }
        throw new IllegalStateException("Unable to resolve max-health attribute from Bukkit API.");
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

    Object findAttributeByFieldNames(String... fieldNames) {
        for (String fieldName : fieldNames) {
            try {
                return Attribute.class.getField(fieldName).get(null);
            }
            catch (NoSuchFieldException | IllegalAccessException ignored) {
            }
        }
        return null;
    }
}
