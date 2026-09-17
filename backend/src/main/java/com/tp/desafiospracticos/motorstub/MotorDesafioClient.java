package com.tp.desafiospracticos.motorstub;

import com.tp.desafiospracticos.practicalchallenge.Difficulty;

/**
 * Puerto que representa el registro local de lo que Motor (Tema 03, dueño
 * real del desafío: nombre, dificultad, fechas, tipo, etc. — ver decisión de
 * arquitectura en CLAUDE.md) ya decidió y nos hizo llegar.
 *
 * <p>Importante: Motor crea el desafío en su propia base y genera el
 * {@code desafioId} sin llamarnos — el profesor llega a nuestra pantalla de
 * autoría vía un redirect de browser que trae ese {@code desafioId} como
 * parámetro de URL. G05 nunca genera ni pide un id propio acá; este puerto
 * no le pide nada a Motor, solo registra/ecoa localmente lo que ya llegó
 * como dato de entrada (title, difficulty) para que nuestras propias
 * pantallas (listado/detalle) lo puedan seguir mostrando sin duplicar esos
 * campos en la entidad de contenido.
 *
 * <p>A diferencia de otros puertos de este mismo paquete/patrón (que el día
 * de mañana se reemplazan por un adaptador real, p. ej. HTTP vía Gateway),
 * este puerto desaparece por completo cuando Motor exista: en ese momento
 * el {@code desafioId}, title y difficulty ya no llegan más como parámetro
 * de URL para "eco" local — Motor pasa a ser la fuente de verdad que se
 * consulta directamente, y este stub y su tabla dejan de tener sentido.
 */
public interface MotorDesafioClient {

    void registrarDesafioRecibido(String desafioId, String title, Difficulty difficulty);
}
