package com.skyblockexp.ezlifesteal.util;

import org.bukkit.attribute.Attribute;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BukkitHealthAttributeResolverTest {

    @Test
    void readMaxHealthFieldPrefersGenericFieldWhenPresent() throws Exception {
        BukkitHealthAttributeResolver resolver = new BukkitHealthAttributeResolver() {
        };

        Object value = resolver.readMaxHealthField(FakeAttribute.class);

        assertEquals("generic", value);
    }

    @Test
    void readMaxHealthFieldFallsBackToLegacyField() throws Exception {
        BukkitHealthAttributeResolver resolver = new BukkitHealthAttributeResolver();

        Object value = resolver.readMaxHealthField(FakeLegacyOnlyAttribute.class);

        assertEquals("legacy", value);
    }

    @Test
    void readMaxHealthFieldThrowsWhenNoExpectedFieldsExist() {
        BukkitHealthAttributeResolver resolver = new BukkitHealthAttributeResolver();

        assertThrows(NoSuchFieldException.class, () -> resolver.readMaxHealthField(FakeNoHealthAttribute.class));
    }

    @Test
    void resolveMaxHealthAttributeUsesPrimaryReadPath() {
        Attribute expected = resolveAnyAttribute();
        BukkitHealthAttributeResolver resolver = new BukkitHealthAttributeResolver() {
            @Override
            Object readGenericMaxHealthField() {
                return expected;
            }
        };

        assertEquals(expected, resolver.resolveMaxHealthAttribute());
    }

    @Test
    void resolveMaxHealthAttributeFallsBackWhenPrimaryReadFails() {
        Attribute expected = resolveAnyAttribute();
        BukkitHealthAttributeResolver resolver = new BukkitHealthAttributeResolver() {
            @Override
            Object readGenericMaxHealthField() throws NoSuchFieldException {
                throw new NoSuchFieldException("missing primary field");
            }

            @Override
            Object findAttributeByFieldNames(String... fieldNames) {
                return expected;
            }
        };

        assertEquals(expected, resolver.resolveMaxHealthAttribute());
    }

    @Test
    void resolveMaxHealthAttributeThrowsWhenNothingResolvable() {
        BukkitHealthAttributeResolver resolver = new BukkitHealthAttributeResolver() {
            @Override
            Object readGenericMaxHealthField() throws NoSuchFieldException {
                throw new NoSuchFieldException("missing primary field");
            }

            @Override
            Object findAttributeByFieldNames(String... fieldNames) {
                return null;
            }
        };

        assertThrows(IllegalStateException.class, resolver::resolveMaxHealthAttribute);
    }

    @Test
    void findAttributeByFieldNamesReturnsNullForUnknownNames() {
        BukkitHealthAttributeResolver resolver = new BukkitHealthAttributeResolver();

        Object value = resolver.findAttributeByFieldNames("__MISSING_ONE__", "__MISSING_TWO__");

        assertEquals(null, value);
    }

    private Attribute resolveAnyAttribute() {
        try {
            Object maxHealth = Attribute.class.getField("MAX_HEALTH").get(null);
            if (maxHealth instanceof Attribute attribute) {
                return attribute;
            }
        }
        catch (ReflectiveOperationException ignored) {
        }

        try {
            Object genericMaxHealth = Attribute.class.getField("GENERIC_MAX_HEALTH").get(null);
            if (genericMaxHealth instanceof Attribute attribute) {
                return attribute;
            }
        }
        catch (ReflectiveOperationException ignored) {
        }

        throw new IllegalStateException("No Bukkit attributes available for test");
    }

    static final class FakeAttribute {
        public static final String GENERIC_MAX_HEALTH = "generic";

        public static final String MAX_HEALTH = "legacy";
    }

    static final class FakeLegacyOnlyAttribute {
        public static final String MAX_HEALTH = "legacy";
    }

    static final class FakeNoHealthAttribute {
        public static final String SOMETHING_ELSE = "value";
    }
}
