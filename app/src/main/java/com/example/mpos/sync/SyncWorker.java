package com.example.mpos.sync;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.mpos.auth.SessionManager;

import java.util.concurrent.TimeUnit;

/** WorkManager job that syncs SQLite → Firestore every 15 minutes when online. */
public class SyncWorker extends Worker {

    public SyncWorker(@NonNull Context ctx, @NonNull WorkerParameters params) {
        super(ctx, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            SessionManager session = new SessionManager(getApplicationContext());
            if (!session.isLoggedIn() || !session.hasShopSelected()) return Result.success();

            RtdbSync sync = new RtdbSync(getApplicationContext(), session.getShopId());
            sync.syncPendingOrders();
            return Result.success();
        } catch (Exception e) {
            return Result.retry();
        }
    }

    /** Schedule the periodic sync job — call once from Application.onCreate(). */
    public static void schedule(Context ctx) {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                SyncWorker.class, 15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .addTag("firestore_sync")
                .build();

        WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
                "firestore_sync",
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                request);
    }
}
