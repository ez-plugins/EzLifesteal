package com.skyblockexp.ezlifesteal.test;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

public final class MockBukkitTestHelper {

    private MockBukkitTestHelper() { }

    public static ServerMock startServer() {
        return MockBukkit.mock();
    }

    public static void stopServer() {
        MockBukkit.unmock();
    }
}
