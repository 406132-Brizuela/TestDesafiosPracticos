package com.tp.desafiospracticos.engine.dimension;

import java.util.Map;

/**
 * Lectura tolerante de parametros de perfil: llegan como Map&lt;String,Object&gt; (JSON-like),
 * los numeros pueden deserializar como Integer o Long segun el origen.
 */
final class ParamUtils {

    private ParamUtils() {
    }

    static int intParam(Map<String, Object> params, String key, int defaultValue) {
        Object value = params.get(key);
        return value instanceof Number number ? number.intValue() : defaultValue;
    }

    static long longParam(Map<String, Object> params, String key, long defaultValue) {
        Object value = params.get(key);
        return value instanceof Number number ? number.longValue() : defaultValue;
    }
}
