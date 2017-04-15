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
package v.graphics;

import i.Game;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import m.Settings;
import rr.column_t;
import rr.patch_t;

/**
 * Patch columns drawing.
 * The whole class is my custom hand-crafted code
 *  - Good Sign 2017/04/03
 * 
 * @author Good Sign
 */
public interface Columns<V, E extends Enum<E>> extends Blocks<V, E> {
    /**
     * We have to draw columns to the screen, not rows and is ineffective performance-wise because
     * System.arraycopy only speeds a lot row copying, where it only have to be called once
     */
    default void DrawColumn(V screen, column_t col, Horizontal row, V data, int scrWidth, int dupy) {
        final int fullRowShift = scrWidth * dupy;
        /**
         * For each post, j is the index of post.
         * 
         * A delta is a number of transparent rows to skip, if it is 0xFF then the whole column
         * is transparent, so if we have delta 0xFF, then we've done with column drawing.
         */
        for (int j = 0, delta = 0;
             j < col.posts && col.postdeltas[j] != 0xFF;
             ++j
        ) {
            // shift a row down by difference of current and previous delta with respect to scaling
            row.shift(point(0, (-delta + (delta = col.postdeltas[j])) * dupy, scrWidth));
            final int saveRowStart = row.start;

            /**
             * For each pixel in the post: p is a position of pixel in the column's data,
             * column.postlen[j] is how many pixels tall is the post (a vertical string of pixels)
             */
            for (int p = 0; p < col.postlen[j]; ++p, row.shift(fullRowShift)) {
                // Fill first line of rect
                screenSet(data, col.postofs[j] + p, screen, row);
                // Fill the rest of the rect
                RepeatRow(screen, row, dupy - 1);
            }
            row.start = saveRowStart;
        }
    }

    /**
     * Accepts patch columns drawing arguments (usually from Patches::DrawPatch method) 
     * and submits the task to the local ForkJoinPool. The task iterates over patch columns in parallel.
     * We need to only iterate through real patch.width and perform scale in-loop
     */
    default void DrawPatchColumns(V screen, patch_t patch, int x, int y, int dupx, int dupy, boolean flip) {
        final int scrWidth = getScreenWidth();
        try {
            U.pool.submit(() -> IntStream.range(0, patch.width).parallel().forEach(i -> {
                final int startPoint = point(x + i * dupx, y, scrWidth);
                final column_t column = flip ? patch.columns[patch.width - 1 - i] : patch.columns[i];
                DrawColumn(screen, column, new Horizontal(startPoint, dupx),
                    convertPalettedBlock(column.data), scrWidth, dupy);
            })).get();
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(Columns.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    class U {
        static final int COLUMN_THREADS = Game.getConfig().getValue(Settings.parallelism_patch_columns, Integer.class);
        private static final ForkJoinPool pool = new ForkJoinPool(COLUMN_THREADS);
        private U() {}
    }
}
