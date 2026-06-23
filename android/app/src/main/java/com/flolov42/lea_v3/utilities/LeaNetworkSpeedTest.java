package com.flolov42.lea_v3.utilities;

import com.flolov42.lea_v3.ui.*;
import com.flolov42.lea_v3.agents.*;
import com.flolov42.lea_v3.modes.*;
import com.flolov42.lea_v3.plus.gamification.*;
import com.flolov42.lea_v3.plus.lifestyle.*;
import com.flolov42.lea_v3.plus.learning.*;
import com.flolov42.lea_v3.plus.premium.*;
import com.flolov42.lea_v3.plus.connect.*;
import com.flolov42.lea_v3.bixby.*;
import com.flolov42.lea_v3.routines.*;
import com.flolov42.lea_v3.telephony.*;
import com.flolov42.lea_v3.code.*;
import com.flolov42.lea_v3.core.*;
import com.flolov42.lea_v3.database.*;
import com.flolov42.lea_v3.notifications.*;
import com.flolov42.lea_v3.utilities.*;

import android.content.Context;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Runs a TCP-based latency test against both local and Cloudflare endpoints.
 * Uses 3 back-to-back pings and returns the median.
 */
public class LeaNetworkSpeedTest {

    public static class SpeedResult {
        public final String target;
        public final long   medianMs;
        public final long   minMs;
        public final long   maxMs;
        public final boolean reachable;
        SpeedResult(String target, long median, long min, long max, boolean reachable) {
            this.target = target; this.medianMs = median;
            this.minMs = min; this.maxMs = max; this.reachable = reachable;
        }
    }

    public interface SpeedTestCallback {
        void onLocalResult(SpeedResult result);
        void onCloudflareResult(SpeedResult result);
        void onDone();
    }

    private static final ExecutorService executor = Executors.newFixedThreadPool(2);

    public static void runBothAsync(Context ctx, SpeedTestCallback cb) {
        executor.submit(() -> {
            SpeedResult local = ping(LeaNetworkConfig.LOCAL_HOST, LeaNetworkConfig.LOCAL_PORT);
            LeaNetworkLogger.get(ctx).latency(
                LeaNetworkDetector.TYPE_LOCAL, LeaNetworkConfig.LOCAL_HTTP,
                "SPEED_TEST", local.medianMs);
            cb.onLocalResult(local);

            SpeedResult cf = ping(LeaNetworkConfig.CLOUDFLARE_HOST, LeaNetworkConfig.CLOUDFLARE_PORT);
            LeaNetworkLogger.get(ctx).latency(
                LeaNetworkDetector.TYPE_CLOUDFLARE, LeaNetworkConfig.CLOUDFLARE_HTTP,
                "SPEED_TEST", cf.medianMs);
            cb.onCloudflareResult(cf);
            cb.onDone();
        });
    }

    private static SpeedResult ping(String host, int port) {
        long[] samples = new long[3];
        int successes = 0;
        for (int i = 0; i < 3; i++) {
            try (Socket s = new Socket()) {
                long t0 = System.currentTimeMillis();
                s.connect(new InetSocketAddress(host, port), 3_000);
                samples[i] = System.currentTimeMillis() - t0;
                successes++;
            } catch (Exception e) {
                samples[i] = Long.MAX_VALUE;
            }
        }
        if (successes == 0) return new SpeedResult(host + ":" + port, -1, -1, -1, false);
        java.util.Arrays.sort(samples);
        long min = Long.MAX_VALUE, max = 0;
        for (long v : samples) if (v != Long.MAX_VALUE) { if (v < min) min = v; if (v > max) max = v; }
        long median = samples[1] == Long.MAX_VALUE ? samples[0] : samples[1];
        return new SpeedResult(host + ":" + port, median, min, max, true);
    }
}
