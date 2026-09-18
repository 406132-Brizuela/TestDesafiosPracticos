package com.tp.desafiospracticos.motor;

import java.util.Collection;
import java.util.List;

/** Puerto HTTP de lectura hacia Motor. G05 nunca crea ni modifica estos datos. */
public interface MotorDesafioClient {

    MotorChallenge findById(String desafioId);

    List<MotorChallenge> findAll();

    List<MotorChallenge> findAllByIds(Collection<String> desafioIds);
}
