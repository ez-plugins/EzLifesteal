package com.skyblockexp.ezlifesteal.util;

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
