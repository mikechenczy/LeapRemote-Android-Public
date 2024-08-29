package org.mj.leapremote.util;

import android.util.Log;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class ThreadUtils {


    private static final Map<Integer, Map<Integer, ExecutorService>> TYPE_PRIORITY_POOLS = new HashMap<>();

    private static final int   CPU_COUNT = Runtime.getRuntime().availableProcessors();

    private static final byte TYPE_SINGLE = -1;
    private static final byte TYPE_CACHED = -2;
    private static final byte TYPE_IO     = -4;
    private static final byte TYPE_CPU    = -8;

    public static ExecutorService getCachedPool() {
        return getPoolByTypeAndPriority(TYPE_CACHED);
    }

    private static ExecutorService getPoolByTypeAndPriority(final int type) {
        return getPoolByTypeAndPriority(type, Thread.NORM_PRIORITY);
    }
    private static ExecutorService getPoolByTypeAndPriority(final int type, final int priority) {
        synchronized (TYPE_PRIORITY_POOLS) {
            ExecutorService pool;
            Map<Integer, ExecutorService> priorityPools = TYPE_PRIORITY_POOLS.get(type);
            if (priorityPools == null) {
                priorityPools = new ConcurrentHashMap<>();
                pool = ThreadPoolExecutor4Util.createPool(type, priority);
                priorityPools.put(priority, pool);
                TYPE_PRIORITY_POOLS.put(type, priorityPools);
            } else {
                pool = priorityPools.get(priority);
                if (pool == null) {
                    pool = ThreadPoolExecutor4Util.createPool(type, priority);
                    priorityPools.put(priority, pool);
                }
            }
            return pool;
        }
    }
    static final class ThreadPoolExecutor4Util extends ThreadPoolExecutor {

        private static ExecutorService createPool(final int type, final int priority) {
            switch (type) {
                case TYPE_SINGLE:
                    return new ThreadPoolExecutor4Util(1, 1,
                            0L, TimeUnit.MILLISECONDS,
                            new LinkedBlockingQueue4Util(),
                            new UtilsThreadFactory("single", priority)
                    );
                case TYPE_CACHED:
                    return new ThreadPoolExecutor4Util(0, 128,
                            60L, TimeUnit.SECONDS,
                            new LinkedBlockingQueue4Util(true),
                            new UtilsThreadFactory("cached", priority)
                    );
                case TYPE_IO:
                    return new ThreadPoolExecutor4Util(2 * CPU_COUNT + 1, 2 * CPU_COUNT + 1,
                            30, TimeUnit.SECONDS,
                            new LinkedBlockingQueue4Util(),
                            new UtilsThreadFactory("io", priority)
                    );
                case TYPE_CPU:
                    return new ThreadPoolExecutor4Util(CPU_COUNT + 1, 2 * CPU_COUNT + 1,
                            30, TimeUnit.SECONDS,
                            new LinkedBlockingQueue4Util(true),
                            new UtilsThreadFactory("cpu", priority)
                    );
                default:
                    return new ThreadPoolExecutor4Util(type, type,
                            0L, TimeUnit.MILLISECONDS,
                            new LinkedBlockingQueue4Util(),
                            new UtilsThreadFactory("fixed(" + type + ")", priority)
                    );
            }
        }

        private final AtomicInteger mSubmittedCount = new AtomicInteger();

        private LinkedBlockingQueue4Util mWorkQueue;

        ThreadPoolExecutor4Util(int corePoolSize, int maximumPoolSize,
                                long keepAliveTime, TimeUnit unit,
                                LinkedBlockingQueue4Util workQueue,
                                ThreadFactory threadFactory) {
            super(corePoolSize, maximumPoolSize,
                    keepAliveTime, unit,
                    workQueue,
                    threadFactory
            );
            workQueue.mPool = this;
            mWorkQueue = workQueue;
        }

        private int getSubmittedCount() {
            return mSubmittedCount.get();
        }

        @Override
        protected void afterExecute(Runnable r, Throwable t) {
            mSubmittedCount.decrementAndGet();
            super.afterExecute(r, t);
        }

        @Override
        public void execute(@NonNull Runnable command) {
            if (this.isShutdown()) return;
            mSubmittedCount.incrementAndGet();
            try {
                super.execute(command);
            } catch (RejectedExecutionException ignore) {
                Log.e("ThreadUtils", "This will not happen!");
                mWorkQueue.offer(command);
            } catch (Throwable t) {
                mSubmittedCount.decrementAndGet();
            }
        }
    }
    private static final class LinkedBlockingQueue4Util extends LinkedBlockingQueue<Runnable> {

        private volatile ThreadPoolExecutor4Util mPool;

        private int mCapacity = Integer.MAX_VALUE;

        LinkedBlockingQueue4Util() {
            super();
        }

        LinkedBlockingQueue4Util(boolean isAddSubThreadFirstThenAddQueue) {
            super();
            if (isAddSubThreadFirstThenAddQueue) {
                mCapacity = 0;
            }
        }

        LinkedBlockingQueue4Util(int capacity) {
            super();
            mCapacity = capacity;
        }

        @Override
        public boolean offer(@NonNull Runnable runnable) {
            if (mCapacity <= size() &&
                    mPool != null && mPool.getPoolSize() < mPool.getMaximumPoolSize()) {
                // create a non-core thread
                return false;
            }
            return super.offer(runnable);
        }
    }

    static final class UtilsThreadFactory extends AtomicLong
            implements ThreadFactory {
        private static final AtomicInteger POOL_NUMBER      = new AtomicInteger(1);
        private static final long          serialVersionUID = -9209200509960368598L;
        private final        String        namePrefix;
        private final        int           priority;
        private final        boolean       isDaemon;

        UtilsThreadFactory(String prefix, int priority) {
            this(prefix, priority, false);
        }

        UtilsThreadFactory(String prefix, int priority, boolean isDaemon) {
            namePrefix = prefix + "-pool-" +
                    POOL_NUMBER.getAndIncrement() +
                    "-thread-";
            this.priority = priority;
            this.isDaemon = isDaemon;
        }

        @Override
        public Thread newThread(@NonNull Runnable r) {
            Thread t = new Thread(r, namePrefix + getAndIncrement()) {
                @Override
                public void run() {
                    try {
                        super.run();
                    } catch (Throwable t) {
                        Log.e("ThreadUtils", "Request threw uncaught throwable", t);
                    }
                }
            };
            t.setDaemon(isDaemon);
            t.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread t, Throwable e) {
                    System.out.println(e);
                }
            });
            t.setPriority(priority);
            return t;
        }
    }

}
