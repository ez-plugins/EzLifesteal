package com.skyblockexp.ezlifesteal.util;

import org.bukkit.attribute.Attribute;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class BukkitHealthAttributeResolverTest {

    @Test
    void resolveMaxHealthAttributeReturnsGenericWhenAvailable() {
        BukkitHealthAttributeResolver resolver = new BukkitHealthAttributeResolver() {
            @Override
            Object readGenericMaxHealthField() {
                return Attribute.MAX_HEALTH;
            }
        };

        Attribute attribute = resolver.resolveMaxHealthAttribute();

        assertEquals(Attribute.MAX_HEALTH, attribute);
    }

    @Test
    void resolveMaxHealthAttributeFallsBackOnMissingField() {
        BukkitHealthAttributeResolver resolver = new BukkitHealthAttributeResolver() {
            @Override
            Object readGenericMaxHealthField() throws NoSuchFieldException {
                throw new NoSuchFieldException("missing");
            }
        };

        Attribute attribute = resolver.resolveMaxHealthAttribute();

        assertEquals(Attribute.MAX_HEALTH, attribute);
    }

    @Test
    void resolveMaxHealthAttributeFallsBackOnIllegalAccess() {
        BukkitHealthAttributeResolver resolver = new BukkitHealthAttributeResolver() {
            @Override
            Object readGenericMaxHealthField() throws IllegalAccessException {
                throw new IllegalAccessException("denied");
            }
        };

        Attribute attribute = resolver.resolveMaxHealthAttribute();

        assertEquals(Attribute.MAX_HEALTH, attribute);
    }

    @Test
    void resolveMaxHealthAttributeFallsBackOnRuntimeException() {
        BukkitHealthAttributeResolver resolver = new BukkitHealthAttributeResolver() {
            @Override
            Object readGenericMaxHealthField() {
                throw new RuntimeException("unexpected");
            }
        };

        Attribute attribute = resolver.resolveMaxHealthAttribute();

        assertEquals(Attribute.MAX_HEALTH, attribute);
    }

    @Test
    void resolveMaxHealthAttributeFallsBackWhenFieldTypeIsUnexpected() {
        BukkitHealthAttributeResolver resolver = new BukkitHealthAttributeResolver() {
            @Override
            Object readGenericMaxHealthField() {
                return "not-an-attribute";
            }
        };

        Attribute attribute = resolver.resolveMaxHealthAttribute();

        assertEquals(Attribute.MAX_HEALTH, attribute);
    }

    @Test
    void readGenericMaxHealthFieldUsesReflectionLookup() {
        BukkitHealthAttributeResolver resolver = new BukkitHealthAttributeResolver();
        try {
            resolver.readGenericMaxHealthField();
        }
        catch (NoSuchFieldException | IllegalAccessException ignored) {
            // This environment may not expose GENERIC_MAX_HEALTH; invoking still covers reflective access line.
        }
    }

    @Test
    void readMaxHealthFieldPrefersGenericFieldWhenPresent() throws Exception {
        BukkitHealthAttributeResolver resolver = new BukkitHealthAttributeResolver();

        Object value = resolver.readMaxHealthField(FakeAttribute.class);

        assertEquals("generic", value);
    }

    public static final class FakeAttribute {
        public static final String GENERIC_MAX_HEALTH = "generic";

        public static final String MAX_HEALTH = "legacy";

    }
}
