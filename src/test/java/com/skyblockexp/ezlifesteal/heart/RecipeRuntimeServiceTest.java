package com.skyblockexp.ezlifesteal.heart;

import com.skyblockexp.ezlifesteal.runtime.DefaultPluginRuntimeServices;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecipeRuntimeServiceTest {

    @Test
    void startRegistersRecipesOnlyWhenEnabled() {
        DefaultPluginRuntimeServices runtime = mock(DefaultPluginRuntimeServices.class);
        RecipeRuntimeService service = new RecipeRuntimeService(runtime);

        when(runtime.isHeartRecipesEnabled()).thenReturn(false);
        service.start();
        verify(runtime, never()).registerHeartRecipes();

        when(runtime.isHeartRecipesEnabled()).thenReturn(true);
        service.start();

        verify(runtime, times(1)).registerHeartRecipes();
    }

    @Test
    void stopClearsRegisteredRecipesOnlyAfterSuccessfulStart() {
        DefaultPluginRuntimeServices runtime = mock(DefaultPluginRuntimeServices.class);
        RecipeRuntimeService service = new RecipeRuntimeService(runtime);
        when(runtime.isHeartRecipesEnabled()).thenReturn(true);

        service.start();
        service.stop();
        service.stop();

        verify(runtime, times(1)).registerHeartRecipes();
        verify(runtime, times(1)).clearHeartRecipesSafely();
    }

    @Test
    void reloadPerformsStopThenStartWhenRunning() {
        DefaultPluginRuntimeServices runtime = mock(DefaultPluginRuntimeServices.class);
        RecipeRuntimeService service = new RecipeRuntimeService(runtime);
        when(runtime.isHeartRecipesEnabled()).thenReturn(true);

        service.start();
        service.reload();

        InOrder inOrder = inOrder(runtime);
        inOrder.verify(runtime).isHeartRecipesEnabled();
        inOrder.verify(runtime).registerHeartRecipes();
        inOrder.verify(runtime).clearHeartRecipesSafely();
        inOrder.verify(runtime).isHeartRecipesEnabled();
        inOrder.verify(runtime).registerHeartRecipes();
    }

    @Test
    void reloadFromStoppedStateStartsSafelyWhenEnabled() {
        DefaultPluginRuntimeServices runtime = mock(DefaultPluginRuntimeServices.class);
        RecipeRuntimeService service = new RecipeRuntimeService(runtime);
        when(runtime.isHeartRecipesEnabled()).thenReturn(true);

        service.reload();

        verify(runtime, never()).clearHeartRecipesSafely();
        verify(runtime, times(1)).registerHeartRecipes();
    }

    @Test
    void repeatedStartAndStopDoNotDuplicateCallsOrThrow() {
        DefaultPluginRuntimeServices runtime = mock(DefaultPluginRuntimeServices.class);
        RecipeRuntimeService service = new RecipeRuntimeService(runtime);
        when(runtime.isHeartRecipesEnabled()).thenReturn(true);

        service.start();
        service.start();
        service.stop();
        service.stop();

        verify(runtime, times(1)).registerHeartRecipes();
        verify(runtime, times(1)).clearHeartRecipesSafely();
    }
}
