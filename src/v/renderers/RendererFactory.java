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

import java.util.Objects;
import v.DoomGraphicSystem;
import v.graphics.Palettes;
import v.scale.VideoScale;

/**
 * Renderer choice that depends on selected (or provided through command line) BppMode
 * It also ensures you create it in right order and with right components.
 * 
 * And see - no package interface shared to public
 * @author Good Sign
 */
public class RendererFactory {
    private RendererFactory() {}
    
    public static <T, V> Clear<T, V> newBuilder() {
        return new Builder<>();
    }

    private static class Builder<T, V>
        implements Clear<T, V>, WithVideoScale<T, V>, WithBppMode<T, V>, WithPalette<T, V>, WithColormap<T, V>
    {
        private VideoScale videoScale;
        private BppMode bppMode;
        private byte[] palette;
        private byte[][] colormap;
        
        @Override
        public WithVideoScale<T, V> setVideoScale(VideoScale videoScale) {
            this.videoScale = Objects.requireNonNull(videoScale);
            return this;
        }

        @Override
        public WithBppMode<T, V> setBppMode(BppMode bppMode) {
            this.bppMode = Objects.requireNonNull(bppMode);
            return this;
        }

        @Override
        public WithPalette<T, V> setPlaypal(byte[] palette) {
            this.palette = Objects.requireNonNull(palette);
            final int minLength = Palettes.PAL_NUM_COLORS * Palettes.PAL_NUM_STRIDES;
            if (palette.length < minLength) {
                throw new IllegalArgumentException("Invalid PLAYPAL: has " + palette.length + " entries instead of " + minLength);
            }
            return this;
        }

        @Override
        public WithColormap<T, V> setColormap(byte[][] colormap) {
            this.colormap = colormap;
            // TODO: sanity check
            return this;
        }

        @Override
        @SuppressWarnings("unchecked")
        public DoomGraphicSystem<T, V> build() {
            final SoftwareGraphicsSystem ret;
            switch (bppMode) {
                case HiColor:
                    ret = new BufferedRenderer16(videoScale, palette);
                    break;
                case TrueColor:
                    ret = new BufferedRenderer32(videoScale, palette);
                    break;
                case Indexed:
                default:
                    ret = new BufferedRenderer(videoScale, palette, colormap);
            }

            return ret;
        }
    }
    
    public interface Clear<T, V> {
        WithVideoScale<T, V> setVideoScale(VideoScale videoScale);
    }

    public interface WithVideoScale<T, V> {
        WithBppMode<T, V> setBppMode(BppMode bppMode);
    }
    
    public interface WithBppMode<T, V> {
        WithPalette<T, V> setPlaypal(byte[] palette);
    }
    
    public interface WithPalette<T, V> {
        WithColormap<T, V> setColormap(byte[][] colormap);
    }
    
    public interface WithColormap<T, V> {
        DoomGraphicSystem<T, V> build();
    }

}
