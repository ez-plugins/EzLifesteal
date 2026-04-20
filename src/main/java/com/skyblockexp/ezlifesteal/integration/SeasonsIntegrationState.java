package com.skyblockexp.ezlifesteal.integration;

import com.skyblockexp.ezlifesteal.listener.SeasonsServiceListener;

public final class SeasonsIntegrationState {

    private Class<?> apiClass;

    private Class<?> integrationClass;

    private Class<?> profileClass;

    private Object apiInstance;

    private Object integrationProxy;

    private SeasonsServiceListener serviceListener;

    private SeasonResetListener seasonResetListener;

    public Class<?> getApiClass() {
        return apiClass;
    }

    public void setApiClass(Class<?> apiClass) {
        this.apiClass = apiClass;
    }

    public Class<?> getIntegrationClass() {
        return integrationClass;
    }

    public void setIntegrationClass(Class<?> integrationClass) {
        this.integrationClass = integrationClass;
    }

    public Class<?> getProfileClass() {
        return profileClass;
    }

    public void setProfileClass(Class<?> profileClass) {
        this.profileClass = profileClass;
    }

    public Object getApiInstance() {
        return apiInstance;
    }

    public void setApiInstance(Object apiInstance) {
        this.apiInstance = apiInstance;
    }

    public Object getIntegrationProxy() {
        return integrationProxy;
    }

    public void setIntegrationProxy(Object integrationProxy) {
        this.integrationProxy = integrationProxy;
    }

    public void clearIntegrationRegistration() {
        this.apiInstance = null;
        this.integrationProxy = null;
    }

    public void clearLoadedClasses() {
        this.apiClass = null;
        this.integrationClass = null;
        this.profileClass = null;
    }

    public SeasonsServiceListener getServiceListener() {
        return serviceListener;
    }

    public void setServiceListener(SeasonsServiceListener serviceListener) {
        this.serviceListener = serviceListener;
    }

    public SeasonResetListener getSeasonResetListener() {
        return seasonResetListener;
    }

    public void setSeasonResetListener(SeasonResetListener seasonResetListener) {
        this.seasonResetListener = seasonResetListener;
    }
}
