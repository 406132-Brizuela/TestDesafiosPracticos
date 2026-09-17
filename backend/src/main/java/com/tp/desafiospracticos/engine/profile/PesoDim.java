package com.tp.desafiospracticos.engine.profile;

import java.util.Map;

/**
 * Peso de una dimension dentro de un perfil, junto con los parametros que ajustan sus
 * umbrales (p. ej. anidamientoMax/complejidadMax para complexity, limiteMs para performance).
 */
public record PesoDim(int weight, Map<String, Object> params) {
}
