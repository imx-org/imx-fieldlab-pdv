package nl.geostandaarden.imx.fieldlab.source.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import java.util.Map;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RestUtils {

    public static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    @SuppressWarnings("unchecked")
    public static <T> T cast(Object value) {
        return (T) value;
    }
}
