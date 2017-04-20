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

import awt.DoomVideoInterface.DoomListener;
import static awt.MochaDoomInputEvent.EV_CONFIGURE_NOTIFY;
import i.Game;
import java.util.Spliterators.AbstractSpliterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.stream.StreamSupport;
import m.Settings;

/**
 * Reworked intermediate class for storing events being already heard by listener
 * by not yet processed by the game event queue
 * 
 * Supports more robust concurrency then its predecessors and provides unique parallelism
 * @author Good Sign
 */
abstract class ConcurrentEvents extends AbstractSpliterator<MochaDoomInputEvent> implements DoomListener {
    /**
     * Parallelism
     */
    private static final int INPUT_THREADS = Game.getConfig().getValue(Settings.parallelism_input, Integer.class);
    private static final ForkJoinPool pool = INPUT_THREADS > 0 ? new ForkJoinPool(INPUT_THREADS) : null;

    /**
     * modifications of eventQueue must be thread safe!
     * But do not invent a wheel. Use one already existent.
     */
    protected final ConcurrentLinkedQueue<MochaDoomInputEvent> eventQueue = new ConcurrentLinkedQueue<>();
    protected static final boolean D = false;
    protected volatile boolean we_are_moving = false;

    /**
     * We have absolutely no idea about "how many events do we have total", so
     * we report max value of long. It is normal for Java! If I remember correctly, this exact value
     * is a special case and will cause StreamSupport framework to assume infinity
     */
    ConcurrentEvents() {
        super(Long.MAX_VALUE, ORDERED|CONCURRENT);
        eventQueue.add(EV_CONFIGURE_NOTIFY);
    }

    @Override
    public void setMouseIsMoving(boolean set) {
        this.we_are_moving = set;
    }

    @Override
    public boolean getMouseWasMoving() {
        return this.we_are_moving;
    }

    /**
     * There lies parallelism. He is the Good with the Fists!
     * As we implement Spliterator interface, we easily create... parallel stream.
     * And place it into a custom thread pool.
     * 
     * @param action whoever will eat events
     */
    @Override
    public void processAllPending(Consumer<? super MochaDoomInputEvent> action) {
        if (INPUT_THREADS > 0) {
            pool.submit(() -> StreamSupport.stream(this, true).forEach(action::accept));
        } else {
            StreamSupport.stream(this, false).forEach(action::accept);
        }
    }
    
    /**
     * A part of spliterator interface implementation, nothing special there
     */
    @Override
    public boolean tryAdvance(Consumer<? super MochaDoomInputEvent> action) {
        if (!eventQueue.isEmpty()) {
            action.accept(eventQueue.remove());
            return true;
        }
        
        return false;
    }
}
