package factorization.misc;

import factorization.common.FzConfig;
import factorization.shared.Core;

import java.util.Random;

public class LagssieWatchDog implements Runnable {
    static int ticks = 0;
    
    Thread watch_thread;
    double sleep_time;
    private final boolean be_cute = true;
    
    public LagssieWatchDog(Thread watch_thread, double sleep_time) {
        this.watch_thread = watch_thread;
        this.sleep_time = sleep_time;
    }
    
    private static final String[] sickeningly_cute_lag_detected_messages = new String[] {
        "Woof! Something is being slow!",
        "Bark Bark! There's some lag! Bark bark!",
        "Pant Pant! Lag! Whiiinnee",
        "Whiine! Lag!",
        "Bark! Lag! Bark! Lag!",
        "Woof! Why is minecraft being so laggy!",
        "Yip! Lagfest!",
        "Bark! Minecraft is running slowly! It must be your computer! Bark!",
        "Arf! Why's it freezing up!",
        "Arf arf! Minecraft fell down the well!",
        "Bark! *I'm* the one who's supposed to play dead!"
    }; // This is what makes this really useful.
    
    @Override
    public void run() {
        log("Woof! I am Lagssie the Lag Watch Dog! (from Factorization's Miscellaneous Nonsense)");
        log("You can change how often I check for freezups with the command /f watchdog <waitInterval>; I also have configuration options!");
        int last_tick = 0;
        boolean had_good_tick = true;
        Random rng = new Random();
        while (true) {
            try {
                Thread.sleep((int)(sleep_time*1000));
            } catch (InterruptedException e) {
                continue;
            }
            if (ticks == last_tick) {
                if (had_good_tick) {
                    if (be_cute) {
                        log(sickeningly_cute_lag_detected_messages[rng.nextInt(sickeningly_cute_lag_detected_messages.length)]);
                    }
                    had_good_tick = false;
                } else {
                    log("");
                }
                for (StackTraceElement ste : watch_thread.getStackTrace()) {
                    log("   " + ste.toString());
                }
            } else {
                had_good_tick = true;
            }
            last_tick = ticks;
        }
    }

    void log(String msg) {
        Core.logInfo("[LAG] " + msg);
    }
    
    
    static LagssieWatchDog instance;
    static void start() {
        if (FzConfig.lagssie_watcher) {
            instance = new LagssieWatchDog(Thread.currentThread(), FzConfig.lagssie_interval);
            Thread dog = new Thread(instance);
            dog.setDaemon(true);
            dog.start();
        }
    }
}
