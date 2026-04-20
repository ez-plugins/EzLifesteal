package com.skyblockexp.ezlifesteal.command;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import org.bukkit.command.CommandSender;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LifestealCommandSubcommandRequirementReflectionTest {

    @Test
    void childrenAccessAndGetChildBehavior() throws Exception {
        Class<?> clazz = Class.forName("com.skyblockexp.ezlifesteal.command.LifestealCommand$SubcommandRequirement");
        Constructor<?> ctor = clazz.getDeclaredConstructor(boolean.class, java.util.List.class, java.util.Map.class);
        ctor.setAccessible(true);

        // create a child requirement that allows execution for players
        Object childReq = ctor.newInstance(false, java.util.List.of(), java.util.Map.of());
        // create parent with the child
        Object parentReq = ctor.newInstance(false, java.util.List.of("some.permission"), Map.of("child", childReq));

        // make sure canExecute uses permissions: stub sender
        CommandSender sender = Mockito.mock(CommandSender.class);
        Mockito.when(sender.hasPermission(Mockito.anyString())).thenReturn(true);

        Method canExecute = clazz.getDeclaredMethod("canExecute", CommandSender.class);
        canExecute.setAccessible(true);
        boolean parentCan = (Boolean) canExecute.invoke(parentReq, sender);
        assertTrue(parentCan, "Parent with permission should be executable");

        // test getAccessibleChildren
        Method getAccessibleChildren = clazz.getDeclaredMethod("getAccessibleChildren", CommandSender.class);
        getAccessibleChildren.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<String> children = (List<String>) getAccessibleChildren.invoke(parentReq, sender);
        assertNotNull(children);
        assertTrue(children.contains("child"));

        // test getChild
        Method getChild = clazz.getDeclaredMethod("getChild", String.class);
        getChild.setAccessible(true);
        Object fetched = getChild.invoke(parentReq, "child");
        assertNotNull(fetched);
        assertEquals(childReq.getClass(), fetched.getClass());
    }
}
