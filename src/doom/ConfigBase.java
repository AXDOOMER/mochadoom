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
package doom;

import data.dstrings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import m.Settings;
import mochadoom.Engine;
import utils.OSValidator;
import utils.ResourceIO;

/**
 * Manages loading different config files from different places
 * (the part about different places is still unfinished)
 * 
 * @author Good Sign
 */
public enum ConfigBase {
    WINDOWS("default.cfg", "USERPROFILE"),
    UNIX(".doomrc", "HOME");

    /**
     * Early detection of the system and setting this is important to define global config Files
     */
    public static final ConfigBase CURRENT = OSValidator.isMac() || OSValidator.isUnix() ? UNIX : WINDOWS;

    /**
     * Reference these in Settings.java to set which file they will go on by default
     */
    public static final Files FILE_DOOM = new Files(CURRENT.defaultConfigName, Enum::compareTo),
            FILE_MOCHADOOM = new Files("mochadoom.cfg");

    public final String defaultConfigName;
    public final String env;

    ConfigBase(final String fileName, final String env) {
        this.defaultConfigName = fileName;
        this.env = env;
    }

    public static class Files {

        private static String folder;

        public final Comparator<Settings> comparator;
        public final String fileName;
        // flags that configuration is provided by the -config argument
        public final boolean alternate;

        public boolean changed = true;
        private String[] paths;

        public Files(String fileName) {
            this(fileName, false);
        }

        public Files(String fileName, boolean alternate) {
            this(fileName, Comparator.comparing(Enum::name, String::compareTo), alternate);
        }

        public Files(String fileName, Comparator<Settings> comparator) {
            this(fileName, comparator, false);
        }

        public Files(String fileName, Comparator<Settings> comparator, boolean alternate) {
            this.fileName = fileName;
            this.comparator = comparator;
            this.alternate = alternate;
        }

        public Optional<ResourceIO> firstValidPathIO() {
            return Arrays.stream(getPaths())
                    .map(ResourceIO::new)
                    .filter(ResourceIO::exists)
                    .findFirst();
        }

        public ResourceIO workDirIO() {
            return new ResourceIO(getFolder() + fileName);
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 53 * hash + Objects.hashCode(this.fileName);
            hash = 53 * hash + (this.alternate ? 1 : 0);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Files other = (Files) obj;
            if (this.alternate != other.alternate) {
                return false;
            }
            return Objects.equals(this.fileName, other.fileName);
        }

        /**
         * Get file / paths combinations
         * 
         * @return a one or more path to the file
         */
        private String[] getPaths() {
            if (paths != null) {
                return paths;
            }

            String getPath = null;

            try { // get it if have rights to do, otherwise ignore and use only current folder
                getPath = System.getenv(CURRENT.env);
            } catch (SecurityException ex) {
            }

            if (getPath == null || "".equals(getPath)) {
                return new String[]{folder};
            }

            getPath += System.getProperty("file.separator");
            return paths = new String[]{
                /**
                 * Uncomment the next line and it will load default.cfg and mochadoom.cfg from user home dir
                 * I find it undesirable - it can load some unrelated file and even write it at exit
                 *  - Good Sign 2017/04/19
                 */
                //getPath + folder + fileName,
                getFolder() + fileName
            };
        }

        private static String getFolder() {
            return folder != null ? folder : (folder
                    = Engine.getCVM().bool(CommandVariable.SHDEV)
                    || Engine.getCVM().bool(CommandVariable.REGDEV)
                    || Engine.getCVM().bool(CommandVariable.FR1DEV)
                    || Engine.getCVM().bool(CommandVariable.FRDMDEV)
                    || Engine.getCVM().bool(CommandVariable.FR2DEV)
                    || Engine.getCVM().bool(CommandVariable.COMDEV)
                    ? dstrings.DEVDATA + System.getProperty("file.separator")
                    : "");
        }
    }

    /**
     * To be able to look for config in several places
     * Still unfinished
     */
    public static List<Files> getFiles() {
        final List<Files> ret = new ArrayList<>();

        /**
         * If user supplied -config argument, it will only use the values from these files instead of defaults
         */
        if (!Engine.getCVM()
                .with(CommandVariable.CONFIG, 0, (String[] fileNames)
                        -> Arrays.stream(fileNames).map(fileName -> new Files(fileName, true)).forEach(ret::add)) /**
                 * If there is no such argument, load default.cfg (or .doomrc) and mochadoom.cfg
                 */
                ) {
            ret.add(FILE_DOOM);
            ret.add(FILE_MOCHADOOM);
        }

        return ret;
    }
}
