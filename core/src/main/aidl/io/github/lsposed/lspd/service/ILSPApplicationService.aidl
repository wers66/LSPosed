package io.github.lsposed.lspd.service;

interface ILSPApplicationService {
    void registerHeartBeat(IBinder handle) = 1;

    IBinder requestModuleBinder() = 2;

    IBinder requestManagerBinder() = 3;

    int getVariant() = 4;

    boolean isResourcesHookEnabled() = 5;

    List<String> getModulesList() = 6;

    String getPrefsPath(String packageName) = 7;

    String getCachePath(String fileName) = 8;
}
