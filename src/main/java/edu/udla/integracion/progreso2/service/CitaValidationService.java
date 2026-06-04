package edu.udla.integracion.progreso2.service;

import org.springframework.stereotype.Service;

import edu.udla.integracion.progreso2.model.CitaRequest;

@Service
public class CitaValidationService {

    // Método para validar el objeto CitaRequest
    public String validar(CitaRequest cita) {
        if (cita == null) {
            return "El payload recibido está vacío";
        }

        if (esVacio(cita.getIdCita())) {
            return "El campo idCita es obligatorio";
        }

        if (esVacio(cita.getPaciente())) {
            return "El campo paciente es obligatorio";
        }

        if (esVacio(cita.getCorreo())) {
            return "El campo correo es obligatorio";
        }

        if (esVacio(cita.getEspecialidad())) {
            return "El campo especialidad es obligatorio";
        }

        if (esVacio(cita.getFechaCita())) {
            return "El campo fechaCita es obligatorio";
        }

        if (esVacio(cita.getSede())) {
            return "El campo sede es obligatorio";
        }

        if (cita.getValor() == null || cita.getValor() <= 0) {
            return "El campo valor debe ser mayor a 0";
        }

        return null;
    }

    private boolean esVacio(String texto) {
        return texto == null || texto.trim().isEmpty();
    }
}