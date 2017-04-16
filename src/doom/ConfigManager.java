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

import i.Game;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.Objects;
import java.util.regex.Pattern;
import m.Settings;
import static m.Settings.NAME_COMPARATOR;
import static m.Settings.values;
import utils.OSValidator;
import utils.ParseString;
import utils.QuoteType;
import utils.ResourceIO;

/**
 * Loads and saves game cfg files
 * 
 * @author Good Sign
 */
public class ConfigManager {
    private static final Pattern splitter = Pattern.compile("[ \t\n\r\f]+");
    
    private boolean changed = false;
    private String configBase = null;
    private String configName = null;
    private final EnumMap<Settings, Object> configMap = new EnumMap<>(Settings.class);

    public ConfigManager() {
        if (OSValidator.isMac() || OSValidator.isUnix()) {
            configBase = System.getenv("HOME");
        } else if (OSValidator.isWindows()) {
            configBase = System.getenv("USERPROFILE");
        }
        
        if (configBase == null) {
            configBase = "";
        } else {
            configBase += System.getProperty("file.separator");
        }
        
        LoadDefaults();
    }
    
    public boolean update(final Settings setting, final String value) {
        if (setting.valueType == String.class) {
            return changed = !Objects.equals(configMap.put(setting, value), value) || changed;
        } else if (setting.valueType == Character.class
            || setting.valueType == Long.class
            || setting.valueType == Integer.class
            || setting.valueType == Boolean.class)
        {
            final Object parse = ParseString.parseString(value);
            if (setting.valueType.isInstance(parse)) {
                return changed = !Objects.equals(configMap.put(setting, parse), parse) || changed;
            }
        } else if (setting.valueType.getSuperclass() == Enum.class) {
            try {
                final Object enumerated = setting.valueType
                    .getDeclaredMethod("valueOf", String.class) // Enum search by name
                    .invoke(null, value); // null as 'this' object pointer, this is static method
                
                return changed = !Objects.equals(configMap.put(setting, enumerated), enumerated) || changed;
            } catch (NoSuchMethodException
                | SecurityException
                | IllegalAccessException
                | IllegalArgumentException
                | InvocationTargetException ex)
            {}
        }
        
        return false;
    }
    
    public boolean update(final Settings setting, final Object value) {
        if (setting.valueType == String.class) {
            return changed = !Objects.equals(configMap.put(setting, value.toString()), value.toString()) || changed;
        }
        
        return false;
    }
    
    public boolean update(final Settings setting, final int value) {
        if (setting.valueType == Integer.class) {
            return changed = !Objects.equals(configMap.put(setting, value), value) || changed;
        } else if (setting.valueType == String.class) {
            final String valStr = Integer.toString(value);
            return changed = !Objects.equals(configMap.put(setting, valStr), valStr) || changed;
        } else if (setting.valueType.getSuperclass() == Enum.class) {
            final Object[] enumValues = setting.valueType.getEnumConstants();
            if (value >= 0 && value < enumValues.length) {
                return changed = !Objects.equals(configMap.put(setting, enumValues[value]), enumValues[value]) || changed;
            }
        }
        
        return false;
    }
        
    public boolean update(final Settings setting, final long value) {
        if (setting.valueType == Long.class) {
            return changed = !Objects.equals(configMap.put(setting, value), value) || changed;
        } else if (setting.valueType == String.class) {
            final String valStr = Long.toString(value);
            return changed = !Objects.equals(configMap.put(setting, valStr), valStr) || changed;
        }
        
        return false;
    }
        
    public boolean update(final Settings setting, final double value) {
        if (setting.valueType == Double.class) {
            return changed = !Objects.equals(configMap.put(setting, value), value) || changed;
        } else if (setting.valueType == String.class) {
            final String valStr = Double.toString(value);
            return changed = !Objects.equals(configMap.put(setting, valStr), valStr) || changed;
        }
        
        return false;
    }
        
    public boolean update(final Settings setting, final char value) {
        if (setting.valueType == Character.class) {
            return changed = !Objects.equals(configMap.put(setting, value), value) || changed;
        } else if (setting.valueType == String.class) {
            final String valStr = Character.toString(value);
            return changed = !Objects.equals(configMap.put(setting, valStr), valStr) || changed;
        }
        
        return false;
    }

    public boolean update(final Settings setting, final boolean value) {
        if (setting.valueType == Boolean.class) {
            return changed = !Objects.equals(configMap.put(setting, value), value) || changed;
        } else if (setting.valueType == String.class) {
            final String valStr = Boolean.toString(value);
            return changed = !Objects.equals(configMap.put(setting, valStr), valStr) || changed;
        }
        
        return false;
    }

    private String export(final Settings setting) {
        return setting.quoteType().map(qt -> {
            return new StringBuilder()
                .append(setting.name())
                .append("\t\t")
                .append(qt.quoteChar)
                .append(configMap.get(setting))
                .append(qt.quoteChar)
                .toString();
        }).orElseGet(() -> {
            return new StringBuilder()
                .append(setting.name())
                .append("\t\t")
                .append(configMap.get(setting))
                .toString();
        });
    }
    
    public boolean equals(final Settings setting, final Object obj) {
        return obj.equals(configMap.get(setting));
    }
    
    @SuppressWarnings("unchecked")
    public <T> T getValue(final Settings setting, final Class<T> valueType) {
        if (setting.valueType == valueType) {
            return (T) configMap.get(setting);
        } else if (valueType == String.class) {
            return (T) configMap.get(setting).toString();
        } else if (setting.valueType == String.class) {
            if (valueType == Character.class
                || valueType == Long.class
                || valueType == Integer.class
                || valueType == Boolean.class)
            {
                final Object parse = ParseString.parseString(configMap.get(setting).toString());
                if (valueType.isInstance(parse)) {
                    return (T) parse;
                }
            }
        } else if (valueType == Integer.class && setting.valueType.getSuperclass() == Enum.class) {
            return (T) ((Integer) ((Enum) configMap.get(setting)).ordinal());
        }
        
        throw new IllegalArgumentException("Unsupported cast: " + setting.valueType + " to " + valueType);
    }
    
    public String getConfig() {
        // TODO: make this count
        return /* getCofigBase() + */ getConfigFilename();
    }
    
    public String getCofigBase() {
        return configBase;
    }
    
    public void setCofigBase(final String newBase) {
        configBase = newBase;
    }

    public String getConfigFilename() {
        if (configName != null)
            return configName;
        
        if (OSValidator.isMac() || OSValidator.isUnix())
            return configName = BaseDefault.UNIX.fileName;
        else
            return configName = BaseDefault.WINDOWS.fileName;
    }
    
    public static enum BaseDefault {
        WINDOWS("default.cfg"), UNIX(".doomrc");
        public final String fileName;

        BaseDefault(final String fileName) {
            this.fileName = fileName;
        }
    }
    
    public void SaveDefaults() {
        // do not write unless there is changes
        if (!changed) {
            return;
        }
        
        final String file = getConfig();
        final ResourceIO rio = new ResourceIO(file);
        final Iterator<Settings> it = Arrays.stream(values()).sorted(NAME_COMPARATOR).iterator();
        rio.writeLines(() -> {
            if (it.hasNext()) {
                return export(it.next());
            }
            
            return null;
        }, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
    }
    
    private void LoadDefaults() {
        Arrays.stream(Settings.values())
            .forEach(setting -> {
                configMap.put(setting, setting.defaultValue);
            });
        
        // Handles variables and settings from default.cfg
        // load before initing other systems, but don't apply them yet.          
        System.out.print("M_LoadDefaults: Load system defaults.\n");
        Game.getCVM().with(CommandVariable.CONFIG, 0, (String configName) -> {
            this.configName = configName;
            System.out.print(String.format("M_LoadDefaults: Using config %s.\n", configName));
        });
        final String file = getConfig();
        final ResourceIO rio = new ResourceIO(file);
        if (rio.readLines(line -> {
            final String[] split = splitter.split(line, 2);
            if (split.length < 2) {
                return;
            }
            
            final String name = split[0];
            try {
                final Settings setting = Settings.valueOf(name);
                final String value = setting.quoteType()
                    .filter(qt -> qt == QuoteType.DOUBLE)
                    .map(qt -> qt.unQuote(split[1]))
                    .orElse(split[1]);

                if (!update(setting, value)) {
                    System.err.printf("WARNING: invalid config value for: %s\n", name);
                }
            } catch (IllegalArgumentException ex) {}
        })) {
            // we just have loaded defaults and cfg file, should be considered no changes
            changed = false;
        } else {
            // This won't destroy successfully read values, though.
            System.err.printf("I just can't read the settings file %s, will use defaults.\n", file);
        }
    }
}