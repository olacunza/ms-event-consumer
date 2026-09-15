SET XACT_ABORT ON;
BEGIN TRANSACTION;
DECLARE @lockResult INT;
EXEC @lockResult = sp_getapplock @Resource = 'ms-event-consumer-schema', @LockMode = 'Exclusive', @LockOwner = 'Transaction', @LockTimeout = 60000;
IF @lockResult < 0 THROW 50002, 'No se pudo bloquear el esquema B', 1;
IF OBJECT_ID(N'dbo.batch_record', N'U') IS NULL
 THROW 50003, 'Inicializar servicio A antes de B', 1;
IF OBJECT_ID(N'dbo.consumed_event', N'U') IS NULL
BEGIN
 CREATE TABLE dbo.consumed_event (
  event_id VARCHAR(36) NOT NULL PRIMARY KEY,
  record_id BIGINT NOT NULL REFERENCES dbo.batch_record(id),
  completed_at DATETIME2 NOT NULL
 );
 CREATE INDEX ix_consumed_record ON dbo.consumed_event(record_id);
END;
IF OBJECT_ID(N'dbo.rejected_event', N'U') IS NULL
BEGIN
 CREATE TABLE dbo.rejected_event (
  id VARCHAR(64) NOT NULL PRIMARY KEY,
  topic VARCHAR(249) NOT NULL,
  partition_number INT NOT NULL,
  record_offset BIGINT NOT NULL,
  payload NVARCHAR(MAX) NULL,
  reason VARCHAR(2000) NOT NULL,
  rejected_at DATETIME2 NOT NULL
 );
 CREATE INDEX ix_rejected_at ON dbo.rejected_event(rejected_at);
END;
COMMIT TRANSACTION;
^^^
