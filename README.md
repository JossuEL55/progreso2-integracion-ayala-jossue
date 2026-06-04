# Progreso 2 - Integración de Sistemas - Salud360

## 1. Nombre del estudiante

**Jossue Ayala**

---

## 2. Descripción breve de la solución

Este proyecto implementa una solución mínima de integración para la organización ficticia **Salud360**, una red de centros médicos que necesita automatizar el flujo de registro de citas confirmadas entre varios sistemas.

La solución permite registrar una cita médica mediante una **API REST**, validar los datos recibidos, procesar la solicitud mediante **Apache Camel**, enviar mensajes a **RabbitMQ**, generar un archivo **CSV** para un sistema legado de auditoría y registrar solicitudes inválidas en un archivo de errores.

---

## 3. Tecnologías utilizadas

* Java 17
* Spring Boot
* Apache Camel
* RabbitMQ
* Docker / Docker Compose
* Swagger / OpenAPI
* Maven
* GitHub

---

## 4. Instrucciones para levantar RabbitMQ

El proyecto incluye un archivo `docker-compose.yml` para levantar RabbitMQ localmente.

Ejecutar:

```bash
docker compose up -d
```

Verificar que el contenedor esté activo:

```bash
docker ps
```

Acceder al panel web de RabbitMQ:

```text
http://localhost:15672
```

Credenciales:

```text
Usuario: guest
Contraseña: guest
```

---

## 5. Instrucciones para ejecutar la aplicación

Compilar el proyecto:

```bash
mvn package
```

Ejecutar la aplicación:

```bash
mvn spring-boot:run
```

La aplicación se ejecuta en el puerto:

```text
8081
```

Swagger está disponible en:

```text
http://localhost:8081/swagger-ui/index.html
```

---

## 6. Endpoint disponible

### Registrar cita médica

```http
POST /api/citas
```

URL completa:

```text
http://localhost:8081/api/citas
```

---

## 7. Ejemplo de request válido

```json
{
  "idCita": "CITA-1001",
  "paciente": "Ana Torres",
  "correo": "ana.torres@email.com",
  "especialidad": "Cardiologia",
  "fechaCita": "2026-06-15",
  "sede": "Centro Norte",
  "valor": 45.50
}
```

Respuesta esperada:

```text
Cita recibida y enviada al flujo de integración: CITA-1001
```

Cuando la cita es válida, el sistema realiza las siguientes acciones:

1. Envía un comando de facturación a `billing.queue`.
2. Publica un evento de cita confirmada a `notifications.queue` y `analytics.queue`.
3. Agrega una línea al archivo `data/outbox/auditoria-citas.csv`.

---

## 8. Ejemplo de request inválido

```json
{
  "idCita": "",
  "paciente": "",
  "correo": "",
  "especialidad": "",
  "fechaCita": "",
  "sede": "",
  "valor": 0
}
```

Respuesta esperada:

```text
Solicitud rechazada: El campo idCita es obligatorio
```

Cuando la cita es inválida, no se envía a RabbitMQ ni se registra en el CSV de auditoría. En su lugar, se registra el rechazo en:

```text
data/errors/citas-rechazadas.log
```

---

## 9. Explicación de patrones y estilos de integración

### API REST

La API REST se utiliza como punto de entrada para que el Sistema de Agenda Médica registre solicitudes de cita. Este estilo permite recibir datos de forma estructurada mediante HTTP y JSON.

Endpoint utilizado:

```text
POST /api/citas
```

---

### Point-to-Point Channel

El patrón **Point-to-Point** se aplica en la integración con el Sistema de Facturación.

Cola utilizada:

```text
billing.queue
```

Este patrón es adecuado porque la solicitud de facturación debe ser procesada por un único consumidor. No sería correcto que varios sistemas generen cobros duplicados para la misma cita.

Mensaje enviado:

```json
{
  "idCita": "CITA-1001",
  "paciente": "Ana Torres",
  "especialidad": "Cardiologia",
  "valor": 45.50,
  "tipoMensaje": "COMANDO_FACTURAR_CITA"
}
```

---

### Publish/Subscribe Channel

El patrón **Publish/Subscribe** se aplica para distribuir el evento de cita confirmada a varios sistemas interesados.

Exchange utilizado:

```text
appointments.events
```

Colas receptoras:

```text
notifications.queue
analytics.queue
```

Este patrón es adecuado porque tanto el Sistema de Notificaciones como el Sistema de Analítica necesitan recibir el mismo evento, pero cada uno lo procesa con un propósito diferente.

Evento publicado:

```json
{
  "idCita": "CITA-1001",
  "paciente": "Ana Torres",
  "correo": "ana.torres@email.com",
  "especialidad": "Cardiologia",
  "fechaCita": "2026-06-15",
  "sede": "Centro Norte",
  "tipoEvento": "CITA_CONFIRMADA"
}
```

---

### Transferencia de archivos

La transferencia de archivos se aplica para integrar el Sistema Legado de Auditoría, debido a que este sistema no cuenta con API ni conexión directa a mensajería.

Archivo generado:

```text
data/outbox/auditoria-citas.csv
```

Formato:

```csv
idCita,paciente,correo,especialidad,fechaCita,sede,valor
CITA-1001,Ana Torres,ana.torres@email.com,Cardiologia,2026-06-15,Centro Norte,45.50
```

---

### Manejo de errores

El sistema valida los campos obligatorios antes de iniciar el flujo de integración.

Campos validados:

* idCita obligatorio
* paciente obligatorio
* correo obligatorio
* especialidad obligatoria
* fechaCita obligatoria
* sede obligatoria
* valor mayor a 0

Si la solicitud es inválida:

1. La API responde con error controlado.
2. No se envía ningún mensaje a RabbitMQ.
3. No se escribe en el CSV de auditoría.
4. Se registra el rechazo en:

```text
data/errors/citas-rechazadas.log
```

---

## 10. Evidencia esperada para verificar el funcionamiento

Las capturas de evidencia se encuentran en:

```text
docs/capturas/
```

Evidencias incluidas:

1. `01-estructura-proyecto.png`
   Estructura del proyecto creada según lo solicitado.

2. `02-build-success.png`
   Compilación exitosa con Maven.

3. `03-rabbitmq-docker.png`
   RabbitMQ ejecutándose con Docker.

4. `04-api-ejecutandose.png`
   API ejecutándose correctamente en Spring Boot.

5. `05-swagger-request-valido-respuesta-exitosa.png`
   Request válido enviado desde Swagger y respuesta exitosa de la API.

6. `06-rabbitmq-colas-generadas.png`
   Colas generadas en RabbitMQ.

7. `07-billing-queue-mensaje.png`
   Mensaje de facturación en `billing.queue`.

8. `08-notifications-queue-mensaje.png`
   Evento recibido por `notifications.queue`.

9. `09-analytics-queue-mensaje.png`
   Evento recibido por `analytics.queue`.

10. `10-csv-generado.png`
    Archivo CSV generado para el sistema legado.

11. `11-request-invalido-swagger.png`
    Solicitud inválida enviada desde Swagger.

12. `12-error-registrado-log.png`
    Registro de error generado en `citas-rechazadas.log`.

---

## Flujo general de la solución

```text
Swagger / Cliente REST
        |
POST /api/citas
        |
CitaController
        |
CitaValidationService
        |
        |--- Solicitud inválida ---> data/errors/citas-rechazadas.log
        |
Apache Camel - direct:procesarCita
        |
        |--- Point-to-Point ---> billing.queue
        |
        |--- Publish/Subscribe ---> appointments.events
        |                              |--> notifications.queue
        |                              |--> analytics.queue
        |
        |--- Archivo CSV ---> data/outbox/auditoria-citas.csv
```

---

## Resultado esperado

Al ejecutar la solución, una cita válida debe generar:

* Un mensaje de facturación en `billing.queue`.
* Un evento de cita confirmada en `notifications.queue`.
* Un evento de cita confirmada en `analytics.queue`.
* Una línea nueva en `data/outbox/auditoria-citas.csv`.

Una cita inválida debe generar:

* Una respuesta HTTP de error.
* Un registro en `data/errors/citas-rechazadas.log`.
