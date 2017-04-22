/*
 * Copyright (C) 2017 Good Sign
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package awt;

import i.Game;
import java.awt.AWTEvent;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import m.Settings;

/**
 * Reworked intermediate class for storing events being already heard by listener
 * by not yet processed by the game event queue.
 * Supports more robust concurrency then its predecessors and provides unique parallelism.
 * 
 * The purpose of this very abstract and very reusable class is to provide way of performant, concurrent input.
 * If the main loop is stuck by rendering complex frames, with low FPS, the difference between
 * plain and concurrent and concurrent approaches will be boldly visible.
 * 
 * However, it will also provide improvement for smoothness over very-high FPS.
 * 
 * It took some effort to separate event handling into three different classes: one for listening the input,
 * one for queuing it and one for sending to underlying DOOM engine. But it best for them to be separated and
 * abstracted for each other for portability reason. For example, if you'll have to rewrite Mocha Doom for
 * new OS of year 2030, which do not have AWT, but have some other input framework (maybe even working with
 * think-reader helmet instead of keyboard) you will only have to put much effort on rewriting DoomFrame and MochaEvents
 * @author Good Sign
 */
public abstract class ConcurrentEvents {
    /**
     * Parallelism
     */
    private static final int INPUT_THREADS = Game.getConfig().getValue(Settings.parallelism_input, Integer.class);

    /**
     * modifications of eventQueue must be thread safe!
     * But do not invent a wheel. Use one already existent.
     */
    protected final ArrayBlockingQueue<AWTEvent> eventQueue = new ArrayBlockingQueue<>(Math.min(4, INPUT_THREADS << 3), false);
    protected final Executor executor = INPUT_THREADS > 0 ? Executors.newFixedThreadPool(INPUT_THREADS) : null;
    protected final Consumer<? super AWTEvent> action;
    protected static final boolean D = false;

    /**
     * We have absolutely no idea about "how many events do we have total", so
     * we report max value of long. It is normal for Java! If I remember correctly, this exact value
     * is a special case and will cause StreamSupport framework to assume infinity
     */
    ConcurrentEvents(Consumer<? super AWTEvent> action) {
        if (INPUT_THREADS > 0) {
            IntStream.range(0, INPUT_THREADS).forEach(i -> {
                executor.execute(() -> {
                    for(;;) {
                        try {
                            action.accept(eventQueue.take());
                        } catch (InterruptedException ex) {
                            Logger.getLogger(ConcurrentEvents.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                });
            });
        }
        this.action = action;
    }

    /**
     * There lies parallelism. He is the Good with the Fists!
     * As we implement Spliterator interface, we easily create... parallel stream.
     * And place it into a custom thread pool.
     * 
     * @param action whoever will eat events
     */
    public void processAllPending() {
        if (INPUT_THREADS <= 0) {
            eventQueue.forEach(action::accept);
        }
    }
    
    abstract void setMouseLoose();
    abstract void setMouseCaptured();
}
