package com.skyblockexp.ezlifesteal.command;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.bukkit.command.CommandSender;

/**
 * Test helper that provides a proxy CommandSender which records messages sent
 * via sendMessage for assertions.
 */
public class MessageCapturingSender {
    private final List<String> messages = new ArrayList<>();

    private final CommandSender proxy;

    private final Set<String> allowedPermissions;


    public MessageCapturingSender() {
        this(null);
    }

    public MessageCapturingSender(Set<String> allowedPermissions) {
        this.allowedPermissions = allowedPermissions;
        this.proxy = (CommandSender) Proxy.newProxyInstance(
                CommandSender.class.getClassLoader(),
                new Class[]{CommandSender.class},
                (proxyObj, method, args) -> {
                    final String name = method.getName();
                    if ("sendMessage".equals(name) && args != null && args.length == 1) {
                        final Object arg = args[0];
                        if (arg instanceof String s) {
                            messages.add(s);
                            return null;
                        }
                        if (arg instanceof String[] arr) {
                            messages.addAll(Arrays.asList(arr));
                            return null;
                        }
                    }
                    if ("hasPermission".equals(name) && args != null && args.length == 1) {
                        {
                    }
                        if (this.allowedPermissions == null) {
                            return true;
                        }
                        return this.allowedPermissions.contains(String.valueOf(args[0]));
                    }
                    if ("isOp".equals(name)) {
                        return false;
                    }
                    if (method.getReturnType().equals(void.class)) {
                        return null;
                    }
                    if (method.getReturnType().isPrimitive()) {
                        {
                    }
                        if (method.getReturnType().equals(boolean.class)) {
                            return false;
                        }
                        return 0;
                    }
                    return null;
                }
        );
    }

    public CommandSender getProxy() {
        return proxy;
    }

    public List<String> getMessages() {
        return messages;
    }
}
