package edu.udla.integracion.progreso2.controller;

import org.apache.camel.ProducerTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.udla.integracion.progreso2.model.CitaRequest;
import edu.udla.integracion.progreso2.service.CitaValidationService;

@RestController
@RequestMapping("/api/citas")

// Controlador para manejar las solicitudes de registro de citasmvn package
public class CitaController {
    private final ProducerTemplate producerTemplate;
    private final CitaValidationService validationService;

    public CitaController(ProducerTemplate producerTemplate, CitaValidationService validationService) {
        this.producerTemplate = producerTemplate;
        this.validationService = validationService;
    }

    @PostMapping
    public ResponseEntity<?> registrarCita(@RequestBody CitaRequest cita) {

        String error = validationService.validar(cita);

        if (error != null) {
            producerTemplate.sendBody("direct:citaRechazada", cita);
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body("Solicitud rechazada: " + error);
        }

        producerTemplate.sendBody("direct:procesarCita", cita);

        return ResponseEntity.ok("Cita recibida y enviada al flujo de integración: " + cita.getIdCita());
    }
}