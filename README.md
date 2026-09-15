# Servicio B — consumidor de eventos

Java 21, Spring Boot 3.3.5, Spring Kafka, JPA/Hibernate y SQL Server.

Consume `record-ready-topic-v2` como parte del grupo `record-processor-v2`.
Procesa lotes de hasta 25 mensajes, consulta XML en batch_record y guarda PROCESSED/FAILED junto con consumed_event en una transacción.
El offset se confirma después del commit SQL.
Los duplicados no repiten el efecto persistido.

Los mensajes malformados, incompatibles o huérfanos se conservan en rejected_event (payload, tópico, partición, offset, motivo y fecha).
No bloquean mensajes válidos.
Los errores técnicos revierten el lote y se reintentan cada 2 segundos; si no se puede guardar cuarentena tampoco se confirma el offset.

Este servicio B expone Actuator para salud/métricas. Health UP no equivale a lag = 0: revisar también offsets y tablas.

## Docker

Coloca este proyecto junto a ms-batch-producer.
El Compose de A construye y levanta los dos servicios, Kafka y SQL Server:

```sh
cd ../ms-batch-producer
cp .env.example .env
# Configura tu contraseña SQL y puertos.
docker compose up -d --build --wait
```

B se publica en localhost:8081 por defecto.
Su puerto interno Docker es 8080.
La imagen se construye con su Dockerfile multietapa y ejecuta pruebas antes de empaquetar.

## IntelliJ / terminal

SQL Server y A deben haber inicializado reto_db.
Copia config/sqlserver-local.properties.example al archivo privado equivalente y configura la contraseña.
Ejecuta MsEventConsumerApplication con este proyecto como directorio de trabajo.
Puerto local actual: 8082, configurable con SERVER_PORT.
El grupo puede consumir mensajes históricos si no tiene offsets válidos.

```sh
./mvnw verify
./mvnw spring-boot:run
```

Variables: DB_URL, DB_USERNAME, MSSQL_SA_PASSWORD, KAFKA_BOOTSTRAP_SERVERS, KAFKA_GROUP_ID, KAFKA_CONCURRENCY, SERVER_PORT, PROCESSING_DELAY_MS y KAFKA_AUTO_STARTUP.
No apuntar B a otra base distinta de A.
Hibernate usa validate; los scripts crean inbox/cuarentena sin eliminar tablas.

