package com.fastasyncworldedit.bukkit.util;

import com.fastasyncworldedit.core.util.FoliaSupport;
import com.fastasyncworldedit.core.util.TaskManager;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import com.sk89q.worldedit.entity.Player;
import com.sk89q.worldedit.util.Location;
import com.sk89q.worldedit.world.World;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

import java.lang.invoke.MethodHandle;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.lang.invoke.MethodHandles.lookup;
import static java.lang.invoke.MethodType.methodType;

public class FoliaTaskManager extends TaskManager {

    private final static MethodHandle IS_GLOBAL_TICK_THREAD;

    static {
        try {
            IS_GLOBAL_TICK_THREAD = lookup().findStatic(Bukkit.class, "isGlobalTickThread", methodType(boolean.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
          throw new AssertionError("Incompatile Folia version", e);
        }
    }

    private final AtomicInteger idCounter = new AtomicInteger();

    @Override
    public int repeatAsync(@NotNull final Runnable runnable, final int interval) {
        // TODO (folia) return some kind of own ScheduledTask instead of int
        Bukkit.getAsyncScheduler().runAtFixedRate(
                WorldEditPlugin.getInstance(),
                asConsumer(runnable),
                0,
                ticksToMs(interval),
                TimeUnit.MILLISECONDS
        );
        return idCounter.getAndIncrement();
    }

    @Override
    public void async(@NotNull final Runnable runnable) {
        Bukkit.getAsyncScheduler().runNow(WorldEditPlugin.getInstance(), asConsumer(runnable));
    }

    @Override
    public void task(@NotNull final Runnable runnable) {
        Bukkit.getGlobalRegionScheduler().run(WorldEditPlugin.getInstance(), asConsumer(runnable));
    }

    @Override
    public int repeat(@NotNull final Runnable runnable, final int interval) {
        Bukkit.getGlobalRegionScheduler().runAtFixedRate(
                WorldEditPlugin.getInstance(),
                asConsumer(runnable),
                1,
                interval
        );
        return idCounter.getAndIncrement();
    }

    @Override
    public void later(@NotNull final Runnable runnable, final int delay) {
        Bukkit.getGlobalRegionScheduler().runDelayed(
                WorldEditPlugin.getInstance(),
                asConsumer(runnable),
                Math.max(1, delay)
        );
    }

    @Override
    public void laterAsync(@NotNull final Runnable runnable, final int delay) {
        Bukkit.getAsyncScheduler().runDelayed(
                WorldEditPlugin.getInstance(),
                asConsumer(runnable),
                ticksToMs(delay),
                TimeUnit.MILLISECONDS
        );
    }

    @Override
    public void cancel(final int task) {
    }

    private <R> Consumer<R> asConsumer(Runnable runnable) {
        return __ -> runnable.run();
    }

    private int ticksToMs(int ticks) {
        return ticks * 50;
    }

}
