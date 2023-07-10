package edu.uic.bitslab.callgraph.config;

import org.apache.commons.text.similarity.LevenshteinDistance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


abstract public class PropertyAnnotation {
    private static final Logger LOGGER = LoggerFactory.getLogger(ManualOptions.class);
    private static final Set<String> _propertyNames = new HashSet<>();

    protected void set() {
        set(System.getProperties());
    }

    protected void set(Properties properties) {
        boolean valid = true;
        Set<String> notFoundProps = new HashSet<>(properties.stringPropertyNames());

        for (Field field : this.getClass().getFields()) {
            if (field.isAnnotationPresent(FromProperty.class)) {
                FromProperty fromProperty = field.getAnnotation(FromProperty.class);
                String propertyName = fromProperty.name();

                if (propertyName == null) {
                    LOGGER.warn("Property {} has invalid name value.", propertyName);
                    continue;
                }

                String propertyValue = properties.getProperty(fromProperty.name(), null);

                // add property name
                _propertyNames.add(propertyName);

                // if value is null, then it wasn't provided to us
                if (propertyValue == null) {
                    if (fromProperty.required()) {
                        LOGGER.warn("Required property {} was not provided.", propertyName);
                        valid = false;
                    } else {
                        LOGGER.info("Property {} was not provided.", propertyName);
                    }
                    continue;
                } else {
                    LOGGER.info("Property {} provided with value {}.", propertyName, propertyValue);
                }

                // remove from not found props, since we found this one.
                notFoundProps.remove(propertyName);

                try {
                    setField(field, propertyValue);
                } catch (Exception e) {
                    LOGGER.warn("Unable to set field for property {}", propertyName);
                    LOGGER.warn("{}: {}", e.getClass().getName(), e.getMessage());
                    LOGGER.warn(Arrays.toString(e.getStackTrace()));
                    e.printStackTrace();
                }
            }
        }

        // given properties that didn't map
        notFoundProps.forEach( name -> LOGGER.warn("Property {} was not found.  Perhaps you meant {}", name, getSuggested(name)));

        if (!valid) {
            System.exit(1);
        }
    }

    private void setField(Field field, String propertyValue) throws IllegalAccessException, InstantiationException, UnsupportedOperationException, NoSuchMethodException, InvocationTargetException {
        Class<?> fieldTypeClass = field.getType();

        if (fieldTypeClass.isArray()) {
            throw new UnsupportedOperationException("Array fields are not supported. Use collection instead.");
        }

        if (fieldTypeClass.isPrimitive()) {
            if (int.class.isAssignableFrom(fieldTypeClass)) {
                field.setInt(this, Integer.parseInt(propertyValue));
            } else if (byte.class.isAssignableFrom(fieldTypeClass)) {
                field.setByte(this, Byte.parseByte(propertyValue));
            } else if (short.class.isAssignableFrom(fieldTypeClass)) {
                field.setShort(this, Short.parseShort(propertyValue));
            } else if (long.class.isAssignableFrom(fieldTypeClass)) {
                field.setLong(this, Long.parseLong(propertyValue));
            } else if (float.class.isAssignableFrom(fieldTypeClass)) {
                field.setFloat(this, Float.parseFloat(propertyValue));
            } else if (double.class.isAssignableFrom(fieldTypeClass)) {
                field.setDouble(this, Double.parseDouble(propertyValue));
            } else if (boolean.class.isAssignableFrom(fieldTypeClass)) {
                field.setBoolean(this, Boolean.parseBoolean(propertyValue));
            } else if (char.class.isAssignableFrom(fieldTypeClass)) {
                if (propertyValue.length() == 0) {
                    field.setChar(this, '\0');
                } else if (propertyValue.length() == 1) {
                    field.setChar(this, propertyValue.charAt(0));
                } else {
                    throw new UnsupportedOperationException("Property requires 0 or 1 characters.");
                }
            } else {
                // never should happen
                throw new UnsupportedOperationException(
                        String.format("Field with primitive %s is not supported.", fieldTypeClass.getName()));
            }

            return;
        }

        if (String.class.isAssignableFrom(fieldTypeClass)) {
            field.set(this, propertyValue);
            return;
        }

        if (Collection.class.isAssignableFrom(fieldTypeClass) || Map.class.isAssignableFrom(fieldTypeClass)) {
            if (!fieldTypeClass.isInterface()) {
                throw new UnsupportedOperationException(
                        String.format("Field %s must be declared using an interface. Gson will assign the concrete type based on provided data.", fieldTypeClass.getName()));
            }

            // expecting json value
            Gson gson = (new GsonBuilder()).create();
            field.set(this, gson.fromJson(propertyValue, fieldTypeClass));
            return;
        }

        throw new UnsupportedOperationException(
            String.format("Field with class %s is not supported.", fieldTypeClass.getName()));
    }

    private static String getSuggested(String name) {
        String suggestedName = null;
        int min = Integer.MAX_VALUE;
        for (String possibleName : _propertyNames) {
            int dist = LevenshteinDistance.getDefaultInstance().apply(name, possibleName);
            if (dist <= min) {
                min = dist;
                suggestedName = possibleName;
            }
        }

        // expecting name not found...
        assert(min > 0);

        String confidence;
        switch (min) {
            case 1:
            case 2:
                confidence = "?";
                break;

            case 3:
            case 4:
                confidence = "??";
                break;

            default:
                confidence = "??? \uD83E\uDD37"; // idk
                break;
        }

        return suggestedName + confidence;
    }
}
