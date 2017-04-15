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
package utils;

public class GenericCopy {
    @SuppressWarnings("unchecked")
    public static <T> void memset(T array, int start, int length, long... value) {
        memset(array, start, length, (T) value, 0, value.length);
    }

    @SuppressWarnings("unchecked")
    public static <T> void memset(T array, int start, int length, int... value) {
        memset(array, start, length, (T) value, 0, value.length);
    }

    @SuppressWarnings("unchecked")
    public static <T> void memset(T array, int start, int length, short... value) {
        memset(array, start, length, (T) value, 0, value.length);
    }

    @SuppressWarnings("unchecked")
    public static <T> void memset(T array, int start, int length, char... value) {
        memset(array, start, length, (T) value, 0, value.length);
    }

    @SuppressWarnings("unchecked")
    public static <T> void memset(T array, int start, int length, byte... value) {
        memset(array, start, length, (T) value, 0, value.length);
    }

    @SuppressWarnings("unchecked")
    public static <T> void memset(T array, int start, int length, double... value) {
        memset(array, start, length, (T) value, 0, value.length);
    }

    @SuppressWarnings("unchecked")
    public static <T> void memset(T array, int start, int length, float... value) {
        memset(array, start, length, (T) value, 0, value.length);
    }

    @SuppressWarnings("unchecked")
    public static <T> void memset(T array, int start, int length, boolean... value) {
        memset(array, start, length, (T) value, 0, value.length);
    }

    @SuppressWarnings("SuspiciousSystemArraycopy")
    public static <T> void memset(T array, int start, int length, T value, int valueStart, int valueLength) {
        if (length > 0 && valueLength > 0) {
            System.arraycopy(value, valueStart, array, start, valueLength);
        
            for (int i = valueLength; i < length; i += i) {
                System.arraycopy(array, start, array, start + i, ((length - i) < i) ? (length - i) : i);
            }
        }
    }
    
    @SuppressWarnings("SuspiciousSystemArraycopy")
    public static <T> void memcpy(T srcArray, int srcStart, T dstArray, int dstStart, int length) {
        System.arraycopy(srcArray, srcStart, dstArray, dstStart, length);
    }
    
    private GenericCopy() {}
}
