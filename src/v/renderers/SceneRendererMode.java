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
package v.renderers;

import doom.CommandVariable;
import doom.DoomMain;
import i.Game;
import java.util.function.Function;
import m.Settings;
import rr.SceneRenderer;
import rr.UnifiedRenderer;
import rr.parallel.ParallelRenderer;
import rr.parallel.ParallelRenderer2;

/**
 * This class helps to choose between scene renderers
 */
public enum SceneRendererMode {
    Serial(SceneRendererMode::Serial_8, SceneRendererMode::Serial_16, SceneRendererMode::Serial_32),
    Parallel(SceneRendererMode::Parallel_8, SceneRendererMode::Parallel_16, SceneRendererMode::Parallel_32),
    Parallel2(SceneRendererMode::Parallel2_8, SceneRendererMode::Parallel2_16, SceneRendererMode::Parallel2_32);
    
    private static final boolean cVarSerial = Game.getCVM().bool(CommandVariable.SERIALRENDERER);
    private static final boolean cVarParallel = Game.getCVM().present(CommandVariable.PARALLELRENDERER);
    private static final boolean cVarParallel2 = Game.getCVM().present(CommandVariable.PARALLELRENDERER2);
    private static final int[] threads = cVarSerial ? null : cVarParallel
        ? parseSwitchConfig(CommandVariable.PARALLELRENDERER)
        : cVarParallel2
            ? parseSwitchConfig(CommandVariable.PARALLELRENDERER2)
            : new int[]{2, 2, 2};
            
    final SG indexedGen, hicolorGen, truecolorGen;

    private SceneRendererMode(SG indexed, SG hi, SG truecolor) {
        this.indexedGen = indexed;
        this.hicolorGen = hi;
        this.truecolorGen = truecolor;
    }
    
    static int[] parseSwitchConfig(CommandVariable sw) {
        // Try parsing walls, or default to 1
        final int walls = Game.getCVM().get(sw, Integer.class, 0).orElse(1);
        // Try parsing floors. If wall succeeded, but floors not, it will default to 1.
        final int floors = Game.getCVM().get(sw, Integer.class, 1).orElse(1);
        // In the worst case, we will use the defaults.
        final int masked = Game.getCVM().get(sw, Integer.class, 2).orElse(2);
        return new int[]{walls, floors, masked};
    }
    
    static SceneRendererMode getMode() {
        if (cVarSerial) {
            /**
             * Serial renderer in command line argument will override everything else
             */
            return Serial;
        } else if (cVarParallel) {
            /**
             * The second-top priority switch is parallelrenderer (not 2) command line argument
             */
            return Parallel;
        } else if (cVarParallel2) {
            /**
             * If we have parallelrenderer2 on command line, it will still override config setting
             */
            return Parallel2;
        }

        /**
         * We dont have overrides on command line - get mode from default.cfg (or whatever)
         * Set default parallelism config in this case
         * TODO: make able to choose in config, but on ONE line along with scene_renderer_mode, should be tricky!
         */
        return Game.getConfig().getValue(Settings.scene_renderer_mode, SceneRendererMode.class);
    }
    
    @SuppressWarnings("unchecked")
    private static SceneRenderer Serial_8(DoomMain DOOM) {
        return new UnifiedRenderer.Indexed(DOOM);
    }
    
    @SuppressWarnings("unchecked")
    private static SceneRenderer Serial_16(DoomMain DOOM) {
        return new UnifiedRenderer.HiColor(DOOM);
    }
    
    @SuppressWarnings("unchecked")
    private static SceneRenderer Serial_32(DoomMain DOOM) {
        return new UnifiedRenderer.TrueColor(DOOM);
    }
    
    @SuppressWarnings("unchecked")
    private static SceneRenderer Parallel_8(DoomMain DOOM) {
        return new ParallelRenderer.Indexed(DOOM, threads[0], threads[1], threads[2]);
    }
    
    @SuppressWarnings("unchecked")
    private static SceneRenderer Parallel_16(DoomMain DOOM) {
        return new ParallelRenderer.HiColor(DOOM, threads[0], threads[1], threads[2]);
    }
    
    @SuppressWarnings("unchecked")
    private static SceneRenderer Parallel_32(DoomMain DOOM) {
        return new ParallelRenderer.TrueColor(DOOM, threads[0], threads[1], threads[2]);
    }
    
    @SuppressWarnings("unchecked")
    private static SceneRenderer Parallel2_8(DoomMain DOOM) {
        return new ParallelRenderer2.Indexed(DOOM, threads[0], threads[1], threads[2]);
    }
    
    @SuppressWarnings("unchecked")
    private static SceneRenderer Parallel2_16(DoomMain DOOM) {
        return new ParallelRenderer2.HiColor(DOOM, threads[0], threads[1], threads[2]);
    }
    
    @SuppressWarnings("unchecked")
    private static SceneRenderer Parallel2_32(DoomMain DOOM) {
        return new ParallelRenderer2.TrueColor(DOOM, threads[0], threads[1], threads[2]);
    }
    
    interface SG extends Function<DoomMain, SceneRenderer> {}
}
