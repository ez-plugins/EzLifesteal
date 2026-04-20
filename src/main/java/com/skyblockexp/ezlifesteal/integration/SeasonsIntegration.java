package com.skyblockexp.ezlifesteal.integration;

import com.skyblockexp.ezlifesteal.EzLifestealPlugin;
import com.skyblockexp.ezlifesteal.model.LifestealProfile;
import com.skyblockexp.ezlifesteal.service.LifestealManager;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.entity.Player;

public final class SeasonsIntegration {

    private final EzLifestealPlugin plugin;

    public SeasonsIntegration(EzLifestealPlugin plugin) {
        this.plugin = plugin;
    }

    public Object createProxy(Class<?> integrationInterface, Class<?> profileInterface) {
        final IntegrationHandler handler = new IntegrationHandler(profileInterface);
        return Proxy.newProxyInstance(
                integrationInterface.getClassLoader(),
                new Class<?>[]{integrationInterface},
                handler
        );
    }

    public Object createLifecycleProxy(Class<?> integrationInterface) {
        final LifecycleHandler handler = new LifecycleHandler();
        return Proxy.newProxyInstance(
                integrationInterface.getClassLoader(),
                new Class<?>[]{integrationInterface},
                handler
        );
    }

    private final class IntegrationHandler implements InvocationHandler {

        private final Class<?> profileInterface;

        private IntegrationHandler(Class<?> profileInterface) {
            this.profileInterface = profileInterface;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            final String name = method.getName();
            if (method.getParameterCount() == 0) {
                switch (name) {
                    case "resetAllHeartsAsync":
                        return resetAllHeartsAsync();
                    case "requestTopHologramUpdate":
                        plugin.requestTopHologramUpdate();
                        return null;
                    case "toString":
                        return "EzLifestealSeasonsIntegration";
                    case "hashCode":
                        return System.identityHashCode(proxy);
                }
            }
            if ("equals".equals(name) && args != null && args.length == 1) {
                return proxy == args[0];
            }
            if (args == null) {
                args = new Object[0];
            }
            switch (name) {
                case "getLoadedProfile":
                    return getLoadedProfile((UUID) args[0]);
                case "applyHearts":
                    applyHearts((Player) args[0], args[1]);
                    return null;
                case "sendHeartStatus":
                    plugin.sendHeartStatus((Player) args[0], ((Number) args[1]).doubleValue());
                    return null;
                default:
                    throw new UnsupportedOperationException("Unsupported EzSeasons API call: " + name);
            }
        }

        private CompletableFuture<Void> resetAllHeartsAsync() {
            final LifestealManager manager = plugin.getLifestealManager();
            if (manager == null) {
                return CompletableFuture.failedFuture(new IllegalStateException("Lifesteal manager is not ready"));
            }
            return manager.resetAllHeartsAsync();
        }

        private Optional<Object> getLoadedProfile(UUID uniqueId) {
            final LifestealManager manager = plugin.getLifestealManager();
            if (manager == null) {
                return Optional.empty();
            }
            return manager.getLoadedProfile(uniqueId).map(this::wrapProfile);
        }

        private void applyHearts(Player player, Object profile) {
            final LifestealManager manager = plugin.getLifestealManager();
            if (manager == null) {
                return;
            }
            final LifestealProfile lifestealProfile = unwrapProfile(profile);
            manager.applyHearts(player, lifestealProfile);
        }

        private Object wrapProfile(LifestealProfile profile) {
            final ProfileHandler handler = new ProfileHandler(profile);
            return Proxy.newProxyInstance(
                    profileInterface.getClassLoader(),
                    new Class<?>[]{profileInterface},
                    handler
            );
        }

        private LifestealProfile unwrapProfile(Object profile) {
            if (profile == null || !Proxy.isProxyClass(profile.getClass())) {
                throw new IllegalArgumentException("Profile was not provided by EzLifesteal");
            }
            final InvocationHandler handler = Proxy.getInvocationHandler(profile);
            if (handler instanceof ProfileHandler profileHandler) {
                return profileHandler.profile();
            }
            throw new IllegalArgumentException("Profile was not provided by EzLifesteal");
        }
    }

    private static final class LifecycleHandler implements InvocationHandler {

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            final String name = method.getName();
            if (method.getParameterCount() == 0) {
                switch (name) {
                    case "onUnregister":
                        return null;
                    case "toString":
                        return "EzLifestealSeasonsLifecycleIntegration";
                    case "hashCode":
                        return System.identityHashCode(proxy);
                }
            }
            if ("equals".equals(name) && args != null && args.length == 1) {
                return proxy == args[0];
            }
            if ("onRegister".equals(name) && method.getParameterCount() == 1) {
                return null;
            }
            throw new UnsupportedOperationException("Unsupported lifecycle method: " + name);
        }
    }

    private final class ProfileHandler implements InvocationHandler {

        private final LifestealProfile profile;

        private ProfileHandler(LifestealProfile profile) {
            this.profile = profile;
        }

        private LifestealProfile profile() {
            return profile;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            final String name = method.getName();
            if ("getHearts".equals(name) && method.getParameterCount() == 0) {
                return profile.getHearts();
            }
            if ("toString".equals(name) && method.getParameterCount() == 0) {
                return "EzLifestealProfileProxy{" + profile.getUniqueId() + "}";
            }
            if ("hashCode".equals(name) && method.getParameterCount() == 0) {
                return Objects.hashCode(profile.getUniqueId());
            }
            if ("equals".equals(name) && args != null && args.length == 1) {
                if (proxy == args[0]) {
                    return true;
                }
                if (args[0] == null || !Proxy.isProxyClass(args[0].getClass())) {
                    return false;
                }
                final InvocationHandler handler = Proxy.getInvocationHandler(args[0]);
                if (handler instanceof ProfileHandler other) {
                    return Objects.equals(profile.getUniqueId(), other.profile.getUniqueId());
                }
                return false;
            }
            throw new UnsupportedOperationException("Unsupported profile method: " + name);
        }
    }
}
