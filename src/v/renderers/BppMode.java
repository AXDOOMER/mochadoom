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

import doom.CVarManager;
import doom.CommandVariable;
import doom.DoomMain;
import i.Game;
import java.awt.Transparency;
import java.util.function.Function;
import m.Settings;
import rr.SceneRenderer;

/**
 * This class helps to choose proper components for bit depth
 * selected in config or through use of command line arguments
 */
public enum BppMode {
    Indexed(5, BufferedRenderer::new, BppMode::SceneGen_8, Transparency.OPAQUE),
    HiColor(5, BufferedRenderer16::new, BppMode::SceneGen_16, Transparency.OPAQUE),
    TrueColor(8, BufferedRenderer32::new, BppMode::SceneGen_32, Transparency.OPAQUE),
    AlphaTrueColor(8, BufferedRenderer32::new, BppMode::SceneGen_32, Transparency.TRANSLUCENT);
    
    public final int transparency;
    public final int lightBits;
    final RenderGen renderGen;
    final ScenerGen scenerGen;

    private BppMode(int lightBits, RenderGen renderGen, ScenerGen scenerGen, int transparency) {
        this.lightBits = lightBits;
        this.renderGen = renderGen;
        this.scenerGen = scenerGen;
        this.transparency = transparency;
    }
    
    @SuppressWarnings("unchecked")
    public <T, V> SceneRenderer<T, V> sceneRenderer(DoomMain<T, V> DOOM) {
        return scenerGen.apply(DOOM);
    }

    public static BppMode chooseBppMode(CVarManager CVM) {
        if (CVM.bool(CommandVariable.TRUECOLOR)) {
            return TrueColor;
        } else if (CVM.bool(CommandVariable.HICOLOR)) {
            return HiColor;
        } else if (CVM.bool(CommandVariable.INDEXED)) {
            return Indexed;
        } else if (CVM.bool(CommandVariable.ALPHATRUECOLOR)) {
            return AlphaTrueColor;
        } else {
            return Game.getConfig().getValue(Settings.color_depth, BppMode.class);
        }
    }
    
    private static SceneRenderer SceneGen_8(DoomMain DOOM) {
        return SceneRendererMode.getMode().indexedGen.apply(DOOM);
    }
    
    private static SceneRenderer SceneGen_16(DoomMain DOOM) {
        return SceneRendererMode.getMode().hicolorGen.apply(DOOM);
    }
    
    private static SceneRenderer SceneGen_32(DoomMain DOOM) {
        return SceneRendererMode.getMode().truecolorGen.apply(DOOM);
    }
    
    interface ScenerGen extends Function<DoomMain, SceneRenderer> {}
    interface RenderGen extends Function<RendererFactory.WithColormap, SoftwareGraphicsSystem> {}
}
