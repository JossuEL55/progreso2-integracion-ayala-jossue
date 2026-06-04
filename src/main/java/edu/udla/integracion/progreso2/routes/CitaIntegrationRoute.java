package edu.udla.integracion.progreso2.routes;

import java.time.LocalDateTime;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import edu.udla.integracion.progreso2.model.CitaRequest;

@Component
public class CitaIntegrationRoute extends RouteBuilder {

    private final RabbitTemplate rabbitTemplate;
    private final AmqpAdmin amqpAdmin;

    public CitaIntegrationRoute(RabbitTemplate rabbitTemplate, AmqpAdmin amqpAdmin) {
        this.rabbitTemplate = rabbitTemplate;
        this.amqpAdmin = amqpAdmin;
    }

    @Override
    public void configure() {

        from("direct:procesarCita")
                .routeId("ruta-procesar-cita")
                .log("Procesando cita confirmada: ${body.idCita}")
                .multicast()
                    .to("direct:enviarFacturacion",
                        "direct:publicarEvento",
                        "direct:generarCsv")
                .end();

        from("direct:enviarFacturacion")
                .routeId("ruta-facturacion-point-to-point")
                .process(exchange -> {
                    CitaRequest cita = exchange.getMessage().getBody(CitaRequest.class);

                    Queue billingQueue = new Queue("billing.queue", true);
                    DirectExchange billingExchange = new DirectExchange("billing.exchange", true, false);
                    Binding billingBinding = BindingBuilder
                            .bind(billingQueue)
                            .to(billingExchange)
                            .with("billing.queue");

                    amqpAdmin.declareQueue(billingQueue);
                    amqpAdmin.declareExchange(billingExchange);
                    amqpAdmin.declareBinding(billingBinding);

                    String mensajeFacturacion = String.format(
                            "{\"idCita\":\"%s\",\"paciente\":\"%s\",\"especialidad\":\"%s\",\"valor\":%.2f,\"tipoMensaje\":\"COMANDO_FACTURAR_CITA\"}",
                            cita.getIdCita(),
                            cita.getPaciente(),
                            cita.getEspecialidad(),
                            cita.getValor()
                    );

                    rabbitTemplate.convertAndSend("billing.exchange", "billing.queue", mensajeFacturacion);
                })
                .log("Mensaje enviado a billing.queue");

        from("direct:publicarEvento")
                .routeId("ruta-evento-publish-subscribe")
                .process(exchange -> {
                    CitaRequest cita = exchange.getMessage().getBody(CitaRequest.class);

                    Queue notificationsQueue = new Queue("notifications.queue", true);
                    Queue analyticsQueue = new Queue("analytics.queue", true);
                    FanoutExchange appointmentsExchange = new FanoutExchange("appointments.events", true, false);

                    Binding notificationsBinding = BindingBuilder
                            .bind(notificationsQueue)
                            .to(appointmentsExchange);

                    Binding analyticsBinding = BindingBuilder
                            .bind(analyticsQueue)
                            .to(appointmentsExchange);

                    amqpAdmin.declareQueue(notificationsQueue);
                    amqpAdmin.declareQueue(analyticsQueue);
                    amqpAdmin.declareExchange(appointmentsExchange);
                    amqpAdmin.declareBinding(notificationsBinding);
                    amqpAdmin.declareBinding(analyticsBinding);

                    String evento = String.format(
                            "{\"idCita\":\"%s\",\"paciente\":\"%s\",\"correo\":\"%s\",\"especialidad\":\"%s\",\"fechaCita\":\"%s\",\"sede\":\"%s\",\"tipoEvento\":\"CITA_CONFIRMADA\"}",
                            cita.getIdCita(),
                            cita.getPaciente(),
                            cita.getCorreo(),
                            cita.getEspecialidad(),
                            cita.getFechaCita(),
                            cita.getSede()
                    );

                    rabbitTemplate.convertAndSend("appointments.events", "", evento);
                })
                .log("Evento publicado a notifications.queue y analytics.queue");

        from("direct:generarCsv")
                .routeId("ruta-archivo-csv-legado")
                .process(exchange -> {
                    CitaRequest cita = exchange.getMessage().getBody(CitaRequest.class);

                    String lineaCsv = String.format(
                            "%s,%s,%s,%s,%s,%s,%.2f",
                            cita.getIdCita(),
                            cita.getPaciente(),
                            cita.getCorreo(),
                            cita.getEspecialidad(),
                            cita.getFechaCita(),
                            cita.getSede(),
                            cita.getValor()
                    );

                    exchange.getMessage().setBody(lineaCsv);
                })
                .to("file:data/outbox?fileName=auditoria-citas.csv&fileExist=Append")
                .log("Línea agregada al archivo CSV de auditoría");

        from("direct:citaRechazada")
                .routeId("ruta-cita-rechazada")
                .process(exchange -> {
                    CitaRequest cita = exchange.getMessage().getBody(CitaRequest.class);

                    String idCita = cita != null && cita.getIdCita() != null
                            ? cita.getIdCita()
                            : "SIN_ID";

                    String logError = String.format(
                            "%s | idCita=%s | Solicitud inválida o incompleta | Payload=%s",
                            LocalDateTime.now(),
                            idCita,
                            cita
                    );

                    exchange.getMessage().setBody(logError);
                })
                .to("file:data/errors?fileName=citas-rechazadas.log&fileExist=Append")
                .log("Cita rechazada registrada en archivo de errores");
    }
}