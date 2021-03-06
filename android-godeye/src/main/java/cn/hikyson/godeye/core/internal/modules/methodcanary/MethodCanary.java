package cn.hikyson.godeye.core.internal.modules.methodcanary;

import cn.hikyson.godeye.core.internal.Install;
import cn.hikyson.godeye.core.internal.ProduceableSubject;
import cn.hikyson.godeye.core.utils.L;
import cn.hikyson.methodcanary.lib.MethodCanaryConfig;

public class MethodCanary extends ProduceableSubject<MethodsRecordInfo> implements Install<MethodCanaryContext> {
    private boolean mInstalled = false;
    private MethodCanaryContext mMethodCanaryContext;

    @Override
    public synchronized void install(final MethodCanaryContext methodCanaryContext) {
        if (this.mInstalled) {
            L.d("MethodCanary already installed, ignore.");
            return;
        }
        this.mMethodCanaryContext = methodCanaryContext;
        this.mInstalled = true;
        L.d("MethodCanary installed.");
    }

    @Override
    public synchronized void uninstall() {
        if (!this.mInstalled) {
            L.d("MethodCanary already uninstalled, ignore.");
            return;
        }
        this.mMethodCanaryContext = null;
        this.mInstalled = false;
        L.d("MethodCanary uninstalled.");
    }

    @Override
    public synchronized boolean isInstalled() {
        return this.mInstalled;
    }

    @Override
    public MethodCanaryContext config() {
        return mMethodCanaryContext;
    }

    public synchronized void startMonitor(String tag) {
        try {
            if (!isInstalled()) {
                L.d("MethodCanary start monitor fail, not installed.");
                return;
            }
            cn.hikyson.methodcanary.lib.MethodCanary.get().start(tag);
            L.d("MethodCanary start monitor success.");
        } catch (Exception e) {
            L.d("MethodCanary start monitor fail:" + e);
        }
    }

    public synchronized void stopMonitor(String tag) {
        try {
            if (!isInstalled()) {
                L.d("MethodCanary stop monitor fail, not installed.");
                return;
            }
            cn.hikyson.methodcanary.lib.MethodCanary.get().stop(tag
                    , new MethodCanaryConfig(this.mMethodCanaryContext.lowCostMethodThresholdMillis() * 1000000), (sessionTag, startNanoTime, stopNanoTime, methodEventMap) -> {
                        long start0 = System.currentTimeMillis();
                        MethodsRecordInfo methodsRecordInfo = MethodCanaryConverter.convertToMethodsRecordInfo(startNanoTime, stopNanoTime, methodEventMap);
                        long start1 = System.currentTimeMillis();
                        MethodCanaryConverter.filter(methodsRecordInfo, this.mMethodCanaryContext);
                        long end = System.currentTimeMillis();
                        L.d(String.format("MethodCanary output converter cost %s ms, filter cost %s ms", end - start0, end - start1));
                        produce(methodsRecordInfo);
                    });
            L.d("MethodCanary stop monitor success.");
        } catch (Exception e) {
            L.d("MethodCanary stop monitor fail:" + e);
        }
    }

    public synchronized boolean isRunning(String tag) {
        return cn.hikyson.methodcanary.lib.MethodCanary.get().isRunning(tag);
    }
}
