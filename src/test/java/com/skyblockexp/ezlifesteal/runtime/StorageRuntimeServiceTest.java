package com.skyblockexp.ezlifesteal.runtime;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

class StorageRuntimeServiceTest {

    @Test
    void startInvokesRuntimeMethodsInExpectedOrder() {
        DefaultPluginRuntimeServices runtime = mock(DefaultPluginRuntimeServices.class);
        StorageRuntimeService service = new StorageRuntimeService(runtime);

        service.start();

        InOrder inOrder = inOrder(runtime);
        inOrder.verify(runtime).initializeStorageExecutor();
        inOrder.verify(runtime).setupStorage();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    void reloadInvokesRuntimeMethodsInExpectedOrder() {
        DefaultPluginRuntimeServices runtime = mock(DefaultPluginRuntimeServices.class);
        StorageRuntimeService service = new StorageRuntimeService(runtime);

        service.reload();

        InOrder inOrder = inOrder(runtime);
        inOrder.verify(runtime).initializeStorageExecutor();
        inOrder.verify(runtime).setupStorage();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    void stopInvokesRuntimeMethodsInExpectedOrder() {
        DefaultPluginRuntimeServices runtime = mock(DefaultPluginRuntimeServices.class);
        StorageRuntimeService service = new StorageRuntimeService(runtime);

        service.stop();

        InOrder inOrder = inOrder(runtime);
        inOrder.verify(runtime).closeStorage();
        inOrder.verify(runtime).shutdownStorageExecutor();
        inOrder.verifyNoMoreInteractions();
    }
}
