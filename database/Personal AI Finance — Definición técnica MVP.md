# Personal AI Finance — Definición técnica MVP

Oct 9, 2026 · @Andrés Castro

## 1. Visión

Personal AI Finance registra gastos en lenguaje natural (texto o foto de recibo), los estructura con IA local y devuelve métricas, alertas y recomendaciones. Es el proyecto 1 del roadmap de system design: el objetivo es aprender a tomar decisiones de arquitectura, a diseñar sistemas event-driven y a usar IA de forma responsable.

**Problema.** La gente no registra gastos por fricción, no sabe en qué se le va el dinero y no recibe retroalimentación sobre sus hábitos.

**Público.** Jóvenes profesionales, universitarios y personas que quieren ahorrar pero no llevan control.

**Objetivos de aprendizaje del proyecto**

- Arquitectura event-driven con Kafka: contratos, idempotencia, reintentos, DLQ.
- Backend reactivo con Spring WebFlux y R2DBC.
- IA local (Ollama, OCR) con salidas estructuradas, evaluación medible y trazabilidad.
- Manejo de binarios grandes fuera del bus de mensajes (claim check con MinIO).
- Documentar decisiones con ADRs antes de codear.

**Métricas de éxito**

| Métrica | Objetivo |
| --- | --- |
| Gastos de texto estructurados correctamente | > 85% sobre el dataset de evaluación |
| Gastos de imagen estructurados correctamente | > 70% sobre el dataset de evaluación |
| Tiempo de procesamiento IA texto (p95) | < 10 s |
| Tiempo de procesamiento IA imagen (p95) | < 60 s |
| Gastos que terminan en NEEDS\_REVIEW | Medido por versión de prompt/modelo |

## 2. Alcance

El MVP incluye texto, imágenes de recibos, alertas y metas de ahorro. Audio y modelos predictivos quedan para después.

| Área | MVP | Después |
| --- | --- | --- |
| Entrada | Texto libre, foto de recibo | Audio (faster-whisper → reusa el pipeline de texto) |
| IA | Estructuración por LLM local, OCR | Insights generados por LLM, modelos predictivos |
| Recomendaciones | Reglas simples | Recomendaciones personalizadas por LLM |
| Alertas | Presupuesto 80/100%, gasto inusual, tendencia | Notificaciones push/correo |
| Metas | Presupuesto mensual total y por categoría, meta de ahorro | Metas multi-mes, proyecciones |
| Usuarios | Registro, login JWT | OAuth, recuperación de contraseña |

**Orden de construcción dentro del MVP.** Cada fase termina funcionando end-to-end antes de empezar la siguiente.

1. Fase A — Esqueleto: auth, CRUD de gastos manual, PostgreSQL, docker-compose.
2. Fase B — Pipeline de texto: Kafka, servicio IA, estados PROCESSING/CONFIRMED/NEEDS\_REVIEW, revisión manual.
3. Fase C — Dashboard: métricas mensuales, distribución por categoría, recomendaciones por reglas.
4. Fase D — Imágenes: MinIO, OCR/visión, comparación de enfoques.
5. Fase E — Metas y alertas: presupuestos, motor de alertas, notificación en tiempo real (SSE).

## 3. Requerimientos funcionales

33 requerimientos agrupados por módulo. La columna Fase indica cuándo se construye.

| ID | Requerimiento | Fase |
| --- | --- | --- |
| RF-MVP-01 | Registro de usuario con correo y contraseña | A |
| RF-MVP-02 | Login que devuelve un JWT válido | A |
| RF-MVP-03 | Cada usuario solo ve y gestiona sus propios datos | A |
| RF-MVP-04 | Registrar un gasto por texto libre ("Ayer pagué 45.000 en almuerzo con tarjeta") | B |
| RF-MVP-05 | Crear el gasto en PROCESSING guardando el texto original | B |
| RF-MVP-06 | Enviar el contenido al servicio IA | B |
| RF-MVP-07 | Extraer monto, categoría, fecha, descripción; método de pago opcional | B |
| RF-MVP-08 | Estados: PROCESSING, CONFIRMED, NEEDS\_REVIEW, FAILED | B |
| RF-MVP-09 | Editar manualmente un gasto en NEEDS\_REVIEW y confirmarlo | B |
| RF-MVP-10 | Listar gastos por fecha descendente, paginado | C |
| RF-MVP-11 | Filtrar por rango de fechas, categoría y rango de monto | C |
| RF-MVP-12 | Resumen: total mes actual, mes anterior y variación % | C |
| RF-MVP-13 | Total por categoría del mes actual | C |
| RF-MVP-14 | Actualizar métricas al pasar un gasto a CONFIRMED | C |
| RF-MVP-15 | Generar recomendaciones por reglas (aumento > X%, categoría > Y%, microgastos) | C |
| RF-MVP-16 | Persistir recomendaciones | C |
| RF-MVP-17 | Consultar recomendaciones activas al cargar el dashboard | C |
| RF-MVP-18 | Publicar el gasto en Kafka para procesamiento | B |
| RF-MVP-19 | Consumir el resultado estructurado desde Kafka | B |
| RF-MVP-20 | NEEDS\_REVIEW cuando la confianza sea menor al umbral (0.75) | B |
| RF-MVP-21 | Subir foto de recibo (JPG/PNG, máx. 5 MB) | D |
| RF-MVP-22 | Guardar la imagen en MinIO y crear el gasto en PROCESSING con imagen\_ref | D |
| RF-MVP-23 | El evento Kafka lleva solo la referencia a la imagen | D |
| RF-MVP-24 | Extraer de la imagen los mismos campos que en texto, más el comercio | D |
| RF-MVP-25 | Ver la imagen original al revisar un gasto | D |
| RF-MVP-26 | Definir presupuesto mensual total y por categoría | E |
| RF-MVP-27 | Definir ingreso mensual y meta de ahorro | E |
| RF-MVP-28 | Ver % consumido por presupuesto y progreso de la meta | E |
| RF-MVP-29 | Alerta al superar 80% y 100% de un presupuesto | E |
| RF-MVP-30 | Alerta de gasto inusual (monto > 3× promedio de su categoría) | E |
| RF-MVP-31 | Alerta de tendencia: gasto mensual creciente 3 meses seguidos | E |
| RF-MVP-32 | No repetir la misma alerta para el mismo presupuesto y periodo | E |
| RF-MVP-33 | Marcar alertas como leídas; recibirlas en tiempo real | E |

Cambios respecto a la definición original: se agrega el estado FAILED (la IA no pudo procesar tras los reintentos) y se fija el umbral de confianza en 0.75, ajustable con el dataset de evaluación.

### 3.1 Ciclo de vida del gasto

&#91;embedded content: estados del gasto · 4 estados, 7 transiciones\]

Cualquier transición fuera de este diagrama devuelve 409. Un gasto en PROCESSING no se edita ni se elimina (RN-07).

### 3.2 Criterios de aceptación

Cada criterio es un test. Formato: dado / cuando / entonces.

**Usuarios (RF-01 a 03)**

- CA-01: Dado un correo nuevo y contraseña válida, cuando me registro, entonces recibo 201 y puedo hacer login.
- CA-02: Dado un correo ya registrado, cuando me registro, entonces recibo 409 y no se crea otro usuario.
- CA-03: Dada una contraseña incorrecta, cuando hago login, entonces recibo 401 sin indicar si el correo existe.
- CA-04: Dado un JWT del usuario A, cuando pido `GET /gastos/{id}` de un gasto de B, entonces recibo 404 (no 403, para no revelar que existe).
- CA-05: Dado un JWT expirado, cuando llamo cualquier endpoint protegido, entonces recibo 401.

**Registro por texto (RF-04 a 09, 18 a 20)**

- CA-06: Cuando envío "Hoy pagué 45.000 en almuerzo con tarjeta", entonces recibo 202 con estado PROCESSING en menos de 300 ms, sin esperar a la IA.
- CA-07: Dado ese gasto, en menos de 10 s pasa a CONFIRMED con monto 45000, categoría comida, fecha hoy, método tarjeta.
- CA-08: Dado "ayer" en el texto, la fecha extraída es la fecha de referencia menos un día en la zona horaria del usuario.
- CA-09: Dado un texto sin monto ("almorcé con Juan"), el gasto termina en NEEDS\_REVIEW, no en CONFIRMED.
- CA-10: Dado un gasto en NEEDS\_REVIEW, cuando edito monto y categoría y confirmo, entonces pasa a CONFIRMED con `confirmadoPor` = USUARIO y se emite `gastos.confirmed`.
- CA-11: Dado un gasto CONFIRMED o PROCESSING, cuando intento confirmarlo como si estuviera en revisión, entonces recibo 409.
- CA-12: Dado que el mismo evento de `gastos.processed` llega dos veces, el gasto se actualiza una sola vez y las métricas no se duplican.
- CA-13: Dado que el servicio IA está apagado, el gasto pasa a FAILED a los 5 min y al reintentar vuelve a PROCESSING.
- CA-14: Dado un texto de más de 500 caracteres, recibo 400.

**Imágenes (RF-21 a 25)**

- CA-15: Cuando subo un JPG de 2 MB, recibo 202 con PROCESSING y la imagen existe en MinIO en `recibos/{usuarioId}/{gastoId}.jpg`.
- CA-16: Cuando subo un PDF o un archivo de 6 MB, recibo 400 o 413 y no se crea gasto ni objeto en MinIO.
- CA-17: El mensaje en `gastos.input` no contiene bytes de la imagen, solo la referencia.
- CA-18: Dado un gasto de imagen, `GET /gastos/{id}/imagen` devuelve una URL que deja de funcionar a los 5 min.
- CA-19: Dada una foto ilegible, el gasto termina en NEEDS\_REVIEW con los campos que se pudieron extraer.

**Dashboard y métricas (RF-10 a 14)**

- CA-20: Dados gastos en varios estados, el resumen solo suma los CONFIRMED.
- CA-21: Dado un gasto confirmado de 100.000 que edito a 80.000, el total del mes baja exactamente 20.000.
- CA-22: Dado un gasto con fecha del mes anterior, suma al mes anterior aunque se haya registrado hoy.
- CA-23: Dado mes anterior en 0, la variación % se muestra como "sin datos", no como infinito ni error.
- CA-24: La lista de gastos viene ordenada por fecha del gasto descendente, 20 por página por defecto.

**Recomendaciones (RF-15 a 17)**

- CA-25: Dado un mes con 25% más gasto que el anterior, existe una recomendación AUMENTO\_MENSUAL activa.
- CA-26: Dado que la regla se dispara 10 veces en el mismo mes, existe una sola recomendación de ese tipo para ese mes.

**Metas y alertas (RF-26 a 33)**

- CA-27: Dado un presupuesto de comida de 500.000, cuando el consumido llega a 400.000, se genera una alerta PRESUPUESTO\_80.
- CA-28: Dado que ya existe PRESUPUESTO\_80 para ese presupuesto y mes, un nuevo gasto no genera otra.
- CA-29: Dado un gasto que salta de 70% a 105% de un presupuesto, se generan PRESUPUESTO\_80 y PRESUPUESTO\_100.
- CA-30: Dada una alerta nueva y el frontend conectado a `/stream`, la alerta aparece sin recargar la página.
- CA-31: Dado ingreso de 3.000.000 y meta de 500.000, el progreso muestra ingreso − gastos confirmados del mes contra la meta.
- CA-32: Dada una categoría con menos de 5 gastos históricos, no se evalúa GASTO\_INUSUAL.

### 3.3 Reglas de negocio y validaciones

**Reglas de negocio**

| ID | Regla |
| --- | --- |
| RN-01 | Solo los gastos CONFIRMED cuentan en métricas, presupuestos, metas, recomendaciones y alertas |
| RN-02 | Un gasto pertenece al mes de su fecha de gasto, no al de su registro |
| RN-03 | La fecha del gasto no puede ser futura ni anterior a 1 año desde hoy |
| RN-04 | Si la IA no encuentra fecha, usa la fecha de referencia del evento |
| RN-05 | Si la IA no encuentra categoría válida, usa "otros" y baja la confianza |
| RN-06 | El usuario puede editar un gasto CONFIRMED; eso reemite `gastos.confirmed` y recalcula |
| RN-07 | Un gasto en PROCESSING no se puede editar ni eliminar |
| RN-08 | Eliminar un gasto CONFIRMED resta su valor de las métricas |
| RN-09 | Si llega un resultado de IA para un gasto que el usuario ya editó o eliminó, se descarta |
| RN-10 | Un usuario tiene como máximo un presupuesto total y uno por categoría por mes |
| RN-11 | Un usuario tiene como máximo una meta de ahorro por mes |
| RN-12 | Las alertas y recomendaciones no se repiten para la misma regla, referencia y mes |
| RN-13 | Las categorías son fijas en el MVP: comida, transporte, entretenimiento, servicios, salud, educación, hogar, otros |
| RN-14 | Todos los montos son COP, sin decimales en la UI, NUMERIC en base de datos (nunca float) |
| RN-15 | Los periodos se calculan en la zona horaria del usuario |

**Validaciones de campos**

| Campo | Regla |
| --- | --- |
| email | Formato válido, máx. 255, único, se guarda en minúsculas |
| contraseña | Mínimo 8 caracteres, al menos una letra y un número |
| texto del gasto | 3 a 500 caracteres |
| imagen | JPG o PNG (validado por magic bytes, no por extensión), máx. 5 MB |
| monto | Mayor a 0, máx. 50.000.000 |
| descripción | Máx. 255 caracteres |
| método de pago | efectivo, tarjeta\_debito, tarjeta\_credito, transferencia, otro, o vacío |
| periodo | Formato YYYY-MM |
| presupuesto / meta / ingreso | Mayor a 0 |

## 4. Requerimientos no funcionales

Los datos financieros nunca salen de la infraestructura propia y ningún gasto queda atascado en PROCESSING.

| ID | Área | Requerimiento |
| --- | --- | --- |
| RNF-01 | Privacidad | Toda la inferencia IA corre local (Ollama). Ningún texto ni imagen va a APIs externas |
| RNF-02 | Seguridad | Contraseñas con BCrypt; JWT con expiración de 1 h; todas las consultas filtran por usuario\_id del token |
| RNF-03 | Seguridad | Imágenes en MinIO con bucket privado; el frontend las ve solo con URL prefirmada de 5 min |
| RNF-04 | Resiliencia | Gasto en PROCESSING más de 5 min pasa a FAILED (job programado); el usuario puede reintentar |
| RNF-05 | Resiliencia | 3 reintentos con backoff en el consumidor IA; luego el mensaje va a gastos.input.dlq |
| RNF-06 | Consistencia | Consumidores idempotentes por eventId; un evento repetido no duplica efectos |
| RNF-07 | Consistencia | Publicación a Kafka con patrón outbox: gasto y evento se guardan en la misma transacción |
| RNF-08 | Rendimiento | API p95 < 300 ms en lecturas; procesamiento IA según métricas de la sección 1 |
| RNF-09 | Trazabilidad | Cada gasto guarda modelo, versión de prompt, confianza y salida cruda de la IA |
| RNF-10 | Observabilidad | Logs JSON con correlationId = gastoId a través de backend, Kafka e IA; métricas con Micrometer/Prometheus |
| RNF-11 | Localización | Moneda COP, punto como separador de miles, zona horaria America/Bogota |
| RNF-12 | Despliegue | Todo levanta con docker-compose en local; manifiestos para OKD como paso posterior |

### 4.1 Estimación de carga

Con 1.000 usuarios activos el cuello de botella es la inferencia de imágenes, no la base de datos ni Kafka. Supuestos para practicar back-of-the-envelope; ajusta los números y recalcula.

| Supuesto | Valor |
| --- | --- |
| Usuarios activos diarios | 1.000 |
| Gastos por usuario al día | 5 (80% texto, 20% imagen) |
| Hora pico | 3× el promedio |
| Tamaño promedio de foto | 1,5 MB |
| Tiempo IA texto / imagen (CPU, 1 worker) | \~4 s / \~30 s |

| Resultado | Cálculo | Valor |
| --- | --- | --- |
| Gastos al día | 1.000 × 5 | 5.000 |
| Escrituras por segundo en pico | 5.000 / 86.400 × 3 | \~0,2/s |
| Imágenes al día | 5.000 × 20% | 1.000 |
| Almacenamiento de imágenes al año | 1.000 × 1,5 MB × 365 | \~550 GB |
| Filas de gastos al año | 5.000 × 365 | \~1,8 millones |
| Segundos de IA al día | 4.000 × 4 + 1.000 × 30 | \~46.000 s (\~13 h de CPU) |
| Workers IA necesarios en pico | 46.000 / 86.400 × 3 | \~2 |

Conclusiones para el diseño:

- PostgreSQL con los índices correctos aguanta esto sin esfuerzo; no hace falta particionar.
- El almacenamiento de imágenes crece más rápido que la base de datos: define política de retención o compresión.
- La IA es el cuello de botella. Las particiones de `gastos.input` (3) son el límite de workers en paralelo dentro del mismo consumer group.
- Una imagen lenta no debe bloquear textos rápidos: evalúa separar topics o consumers por tipo de entrada (pregunta abierta en la sección 12).

## 5. Arquitectura y flujos

Un backend Java (monolito modular) y un worker Python de IA, desacoplados por Kafka. Las imágenes viajan por MinIO, nunca por Kafka.

&#91;embedded content: arquitectura · 7 componentes\]

El frontend solo habla con el backend; el servicio IA no tiene API pública ni acceso a PostgreSQL.

**Flujo 1 — Gasto por texto**

1. `POST /gastos/texto`: el backend guarda el gasto en PROCESSING y una fila en `outbox`, en la misma transacción. Responde 202.
2. El publisher de outbox envía el evento a `gastos.input` y marca la fila como publicada.
3. El worker IA consume, estructura con Ollama, calcula la confianza y publica en `gastos.processed`.
4. El backend consume: confianza ≥ 0.75 → CONFIRMED y outbox a `gastos.confirmed`; menor → NEEDS\_REVIEW; ERROR → FAILED.
5. El backend envía `gasto.actualizado` por SSE al frontend.

**Flujo 2 — Gasto por imagen**

1. `POST /gastos/imagen`: el backend sube la foto a MinIO (`recibos/{usuarioId}/{gastoId}.jpg`) y sigue igual que el flujo 1, con `imagenRef` en el evento.
2. El worker descarga la imagen de MinIO, aplica OCR + LLM o modelo de visión, y publica el resultado.

**Flujo 3 — Confirmación, métricas y alertas**

1. El backend consume `gastos.confirmed` (consumer group `metricas`).
2. Upsert incremental en `monthly_metrics`.
3. El motor de reglas evalúa recomendaciones y alertas.
4. Si hay alerta nueva: se guarda, se publica en `alertas.generated` y llega al frontend por SSE.

**Flujo 4 — Fallos**

1. El worker reintenta 3 veces con backoff; si sigue fallando publica en `gastos.input.dlq` y un `gastos.processed` con ERROR.
2. Un job cada minuto pasa a FAILED los gastos en PROCESSING por más de 5 min.
3. El usuario puede reintentar un gasto FAILED desde la UI.

## 6. Contratos de eventos Kafka

Cinco topics, todos con key = usuarioId para mantener el orden de eventos por usuario.

| Topic | Productor | Consumidor | Particiones | Propósito |
| --- | --- | --- | --- | --- |
| gastos.input | Backend (outbox) | Servicio IA | 3 | Gasto pendiente de estructurar |
| gastos.processed | Servicio IA | Backend | 3 | Resultado de la IA |
| gastos.input.dlq | Servicio IA | Manual / backend | 1 | Mensajes que fallaron tras reintentos |
| gastos.confirmed | Backend (outbox) | Backend: métricas, alertas, recomendaciones | 3 | Gasto confirmado (por IA o por usuario) |
| alertas.generated | Backend | Backend: SSE al frontend | 3 | Alerta lista para notificar |

**gastos.input**

```json
{
  "eventId": "8f1c...",
  "eventType": "GASTO_CREADO",
  "occurredAt": "2026-10-09T14:32:10-05:00",
  "gastoId": "2b7e...",
  "usuarioId": "a91d...",
  "tipoEntrada": "TEXTO",
  "texto": "Ayer pagué 45.000 en almuerzo con tarjeta",
  "imagenRef": null,
  "fechaReferencia": "2026-10-09",
  "zonaHoraria": "America/Bogota",
  "moneda": "COP",
  "categoriasValidas": ["comida", "transporte", "entretenimiento", "servicios", "salud", "educacion", "hogar", "otros"]
}
```

Para imagen: `tipoEntrada` = `IMAGEN`, `texto` = null, `imagenRef` = `recibos/a91d.../2b7e....jpg`.

**gastos.processed**

```json
{
  "eventId": "c44a...",
  "eventType": "GASTO_PROCESADO",
  "occurredAt": "2026-10-09T14:32:14-05:00",
  "gastoId": "2b7e...",
  "usuarioId": "a91d...",
  "resultado": "OK",
  "confianza": 0.75,
  "datos": {
    "monto": 45000,
    "categoria": "comida",
    "fecha": "2026-10-08",
    "descripcion": "almuerzo",
    "metodoPago": "tarjeta",
    "comercio": null
  },
  "textoExtraido": null,
  "modelo": "qwen2.5:7b",
  "promptVersion": "texto-v1",
  "duracionMs": 3820,
  "error": null
}
```

`resultado` = `OK` | `ERROR`. Con ERROR el backend marca el gasto FAILED. `textoExtraido` guarda la salida del OCR en imágenes.

**gastos.confirmed**

```json
{
  "eventId": "e02b...",
  "eventType": "GASTO_CONFIRMADO",
  "occurredAt": "2026-10-09T14:32:15-05:00",
  "gastoId": "2b7e...",
  "usuarioId": "a91d...",
  "monto": 45000,
  "categoria": "comida",
  "fecha": "2026-10-08",
  "periodo": "2026-10",
  "confirmadoPor": "IA"
}
```

`confirmadoPor` = `IA` | `USUARIO`. También se emite cuando el usuario edita un gasto ya confirmado, para recalcular.

## 7. Modelo de datos (PostgreSQL)

Modelo final diseñado en dbdiagram: 12 tablas, PostgreSQL 15+, zona horaria única America/Bogota. Las reglas se validan en código (mensajes claros) y en la base de datos (garantía ante concurrencia).

**Decisiones del modelo**

| Decisión | Por qué |
| --- | --- |
| `expense_entries` separado de `expenses` | El registro y su procesamiento IA viven en entries; `expenses` solo tiene gastos confirmados, así las métricas no filtran por estado (RN-01) |
| `ai_output` nunca se modifica | Al corregir un gasto solo cambia `expenses`; la diferencia IA vs usuario alimenta el dataset de evaluación |
| Login con email o username | Username generado automáticamente, sin arroba, enviado por correo; si el identificador tiene arroba se busca por email |
| Sin `dni` ni `cellphone` | Ningún requerimiento los usa; minimización de datos personales |
| Auditoría `created_*` / `updated_*` | En tablas de negocio; `created_by` guarda el username o el actor del sistema (`ai-service`, `rules-engine`) |
| `outbox` y `processed_events` sin auditoría | Tablas técnicas que solo toca el sistema |
| `processed_events` con PK `(event_id, consumer)` | Un mismo evento lo consumen métricas y alertas |
| Zona horaria fija | Solo se usa en Bogotá; Postgres, backend e IA configurados con America/Bogota |

**Diagrama (DBML para dbdiagram.io)**

```dbml
Project personal_ai_finance {
  database_type: 'PostgreSQL'
  Note: '''
  Personal AI Finance - MVP
  - PostgreSQL 15+ (requerido por UNIQUE NULLS NOT DISTINCT)
  - Zona horaria unica: America/Bogota
  - Montos en COP con decimal(14,2)
  - Periodos en formato YYYY-MM
  - Validaciones en codigo y en base de datos
  '''
}

Table users {
  id uuid [pk]
  email varchar(255) [not null, unique, note: 'Minusculas']
  username varchar(30) [not null, unique, note: 'Generado, minusculas, sin arroba']
  password_hash varchar(100) [not null]
  full_name varchar(200) [not null]
  monthly_income decimal(14,2)
  created_at timestamp [not null]
  created_by varchar(100) [not null]
  updated_at timestamp
  updated_by varchar(100)

  Note: 'Login con email o username'
}

Table categories {
  id uuid [pk]
  code varchar(30) [not null, unique]
  name varchar(60) [not null]
  created_at timestamp [not null]
  created_by varchar(100) [not null]
  updated_at timestamp
  updated_by varchar(100)

  Note: 'Catalogo fijo: food, transport, entertainment, utilities, health, education, home, other'
}

Table images {
  id uuid [pk]
  user_id uuid [not null, ref: > users.id]
  object_key varchar(255) [not null, unique, note: 'Key del objeto en MinIO']
  content_type varchar(20) [not null, note: 'image/jpeg | image/png']
  created_at timestamp [not null]
  created_by varchar(100) [not null]
  updated_at timestamp
  updated_by varchar(100)
}

Table expense_entries {
  id uuid [pk]
  user_id uuid [not null, ref: > users.id]
  image_id uuid [unique, ref: - images.id]
  entry_type varchar(10) [not null, note: 'TEXT | IMAGE']
  raw_text varchar(500)
  extracted_text text [note: 'Salida del OCR']
  error_message varchar(500)
  status varchar(15) [not null, note: 'PROCESSING | NEEDS_REVIEW | CONFIRMED | FAILED']
  confidence decimal(3,2)
  ai_output jsonb [note: 'Respuesta cruda de la IA, no se modifica']
  created_at timestamp [not null]
  created_by varchar(100) [not null]
  updated_at timestamp
  updated_by varchar(100)
}

Table expenses {
  id uuid [pk]
  user_id uuid [not null, ref: > users.id]
  entry_id uuid [not null, unique, ref: - expense_entries.id]
  category_id uuid [not null, ref: > categories.id]
  amount decimal(14,2) [not null]
  expense_date date [not null]
  description varchar(255)
  payment_method varchar(20) [note: 'CASH | DEBIT_CARD | CREDIT_CARD | TRANSFER | OTHER']
  created_at timestamp [not null]
  created_by varchar(100) [not null]
  updated_at timestamp
  updated_by varchar(100)
}

Table monthly_metrics {
  id uuid [pk]
  user_id uuid [not null, ref: > users.id]
  category_id uuid [ref: > categories.id, note: 'NULL = total del mes']
  period char(7) [not null, note: 'YYYY-MM']
  total decimal(14,2) [not null]
  quantity int [not null]
  created_at timestamp [not null]
  created_by varchar(100) [not null]
  updated_at timestamp
  updated_by varchar(100)

  indexes {
    (user_id, period, category_id) [unique, note: 'NULLS NOT DISTINCT']
  }
}

Table budgets {
  id uuid [pk]
  user_id uuid [not null, ref: > users.id]
  category_id uuid [ref: > categories.id, note: 'NULL = presupuesto total']
  period char(7) [not null, note: 'YYYY-MM']
  amount decimal(14,2) [not null]
  created_at timestamp [not null]
  created_by varchar(100) [not null]
  updated_at timestamp
  updated_by varchar(100)

  indexes {
    (user_id, period, category_id) [unique, note: 'NULLS NOT DISTINCT - RN-10']
  }
}

Table savings_goals {
  id uuid [pk]
  user_id uuid [not null, ref: > users.id]
  period char(7) [not null, note: 'YYYY-MM']
  target_amount decimal(14,2) [not null]
  created_at timestamp [not null]
  created_by varchar(100) [not null]
  updated_at timestamp
  updated_by varchar(100)

  indexes {
    (user_id, period) [unique, note: 'RN-11']
  }
}

Table recommendations {
  id uuid [pk]
  user_id uuid [not null, ref: > users.id]
  type varchar(20) [not null, note: 'SAVING | WARNING | COMPARATIVE | TREND | PROJECTION']
  rule varchar(40) [not null, note: 'MONTHLY_INCREASE | DOMINANT_CATEGORY | MICRO_EXPENSES | SAVINGS_PROJECTION']
  period char(7) [not null, note: 'YYYY-MM']
  message text [not null]
  active boolean [not null]
  data jsonb
  created_at timestamp [not null]
  created_by varchar(100) [not null]
  updated_at timestamp
  updated_by varchar(100)

  indexes {
    (user_id, rule, period) [unique, note: 'RN-12']
  }
}

Table alerts {
  id uuid [pk]
  user_id uuid [not null, ref: > users.id]
  type varchar(30) [not null, note: 'BUDGET_80 | BUDGET_100 | UNUSUAL_EXPENSE | TREND']
  reference_id uuid [note: 'budget_id o expense_id segun type']
  period char(7) [note: 'YYYY-MM']
  message text [not null]
  is_read boolean [not null]
  created_at timestamp [not null]
  created_by varchar(100) [not null]
  updated_at timestamp
  updated_by varchar(100)

  indexes {
    (user_id, type, reference_id, period) [unique, note: 'NULLS NOT DISTINCT - RN-12']
  }
}

Table outbox {
  id uuid [pk]
  topic varchar(60) [not null]
  message_key varchar(60) [not null]
  payload jsonb [not null]
  created_at timestamp [not null]
  published_at timestamp [note: 'NULL = pendiente de publicar']
}

Table processed_events {
  event_id uuid [not null]
  consumer varchar(60) [not null]
  processed_at timestamp [not null]

  indexes {
    (event_id, consumer) [pk]
  }
}
```

**Restricciones que se agregan a mano en las migraciones**

dbdiagram exporta los índices únicos sin `NULLS NOT DISTINCT` y no exporta los `CHECK`.

```sql
-- En budgets, monthly_metrics y alerts reemplaza el indice unico que exporta dbdiagram por estas restricciones
ALTER TABLE budgets ADD CONSTRAINT ux_budgets_user_period_cat
  UNIQUE NULLS NOT DISTINCT (user_id, period, category_id);
ALTER TABLE budgets ADD CONSTRAINT ck_budgets_amount CHECK (amount > 0);

ALTER TABLE savings_goals ADD CONSTRAINT ck_goals_amount CHECK (target_amount > 0);

ALTER TABLE monthly_metrics ADD CONSTRAINT ux_metrics_user_period_cat
  UNIQUE NULLS NOT DISTINCT (user_id, period, category_id);

ALTER TABLE alerts ADD CONSTRAINT ux_alerts_user_type_ref_period
  UNIQUE NULLS NOT DISTINCT (user_id, type, reference_id, period);

ALTER TABLE expenses ADD CONSTRAINT ck_expenses_amount
  CHECK (amount > 0 AND amount <= 50000000);

ALTER TABLE expense_entries ADD CONSTRAINT ck_entries_type
  CHECK (entry_type IN ('TEXT', 'IMAGE'));
ALTER TABLE expense_entries ADD CONSTRAINT ck_entries_status
  CHECK (status IN ('PROCESSING', 'NEEDS_REVIEW', 'CONFIRMED', 'FAILED'));
ALTER TABLE expense_entries ADD CONSTRAINT ck_entries_input
  CHECK ((entry_type = 'TEXT' AND raw_text IS NOT NULL)
      OR (entry_type = 'IMAGE' AND image_id IS NOT NULL));

CREATE INDEX ix_outbox_pending ON outbox (created_at) WHERE published_at IS NULL;
```

**Plan de migraciones**

```
V1__users_categories.sql                 -- Fase A
V2__seed_categories.sql                  -- Fase A
V3__images_entries_expenses.sql          -- Fase A/B
V4__outbox_processed_events.sql          -- Fase B
V5__monthly_metrics_recommendations.sql  -- Fase C
V6__budgets_goals_alerts.sql             -- Fase E
```

Nunca edites una migración ya aplicada; si algo cambia, crea la siguiente.

## 8. API REST

Base `/api/v1`, JSON, JWT en `Authorization: Bearer`. Todo endpoint salvo auth filtra por el usuario del token.

| Método | Ruta | Descripción | RF |
| --- | --- | --- | --- |
| POST | /auth/registro | Crear usuario | 01 |
| POST | /auth/login | Devuelve JWT | 02 |
| POST | /gastos/texto | Body `{ "texto": "..." }` → 202 con el gasto en PROCESSING | 04, 05 |
| POST | /gastos/imagen | multipart `archivo` → 202 con el gasto en PROCESSING | 21, 22 |
| GET | /gastos | Filtros `desde`, `hasta`, `categoria`, `montoMin`, `montoMax`, `page`, `size` | 10, 11 |
| GET | /gastos/{id} | Detalle con campos IA | 09 |
| GET | /gastos/{id}/imagen | Redirige a URL prefirmada de MinIO | 25 |
| PUT | /gastos/{id} | Editar campos; confirma si estaba en NEEDS\_REVIEW | 09 |
| POST | /gastos/{id}/reintentar | Vuelve a PROCESSING un gasto FAILED | RNF-04 |
| DELETE | /gastos/{id} | Eliminar gasto | — |
| GET | /dashboard/resumen?periodo= | Total actual, anterior, variación %, por categoría | 12, 13 |
| GET | /dashboard/tendencia?meses=6 | Totales por mes | 12 |
| GET | /recomendaciones | Activas del usuario | 17 |
| PUT | /presupuestos | Upsert de presupuestos del periodo | 26 |
| GET | /presupuestos?periodo= | Presupuestos con % consumido | 28 |
| PUT | /metas | Ingreso mensual y meta de ahorro | 27 |
| GET | /metas?periodo= | Progreso de la meta | 28 |
| GET | /alertas?leida=false | Alertas del usuario | 33 |
| PATCH | /alertas/{id} | Marcar como leída | 33 |
| GET | /stream | SSE: eventos `gasto.actualizado` y `alerta.nueva` | 33 |

El SSE también le avisa al frontend cuando un gasto sale de PROCESSING, así no hace polling.

Ejemplo de respuesta de `POST /gastos/texto`:

```json
{
  "id": "2b7e...",
  "estado": "PROCESSING",
  "tipoEntrada": "TEXTO",
  "textoOriginal": "Ayer pagué 45.000 en almuerzo con tarjeta",
  "creadoEn": "2026-10-09T14:32:10-05:00"
}
```

### 8.1 Errores

Todos los errores usan el formato Problem Details (RFC 9457), que Spring Boot 3 trae con `ProblemDetail`.

```json
{
  "type": "https://finance.local/errores/estado-invalido",
  "title": "Estado inválido",
  "status": 409,
  "detail": "El gasto está en PROCESSING y no se puede editar",
  "instance": "/api/v1/gastos/2b7e...",
  "codigo": "GASTO_ESTADO_INVALIDO"
}
```

| HTTP | Código | Cuándo |
| --- | --- | --- |
| 400 | VALIDACION | Campo inválido (sección 3.3); incluye la lista de campos con error |
| 400 | ARCHIVO\_TIPO\_INVALIDO | Imagen que no es JPG ni PNG |
| 401 | NO\_AUTENTICADO | Sin token, token inválido o expirado, credenciales incorrectas |
| 404 | NO\_ENCONTRADO | Recurso inexistente o de otro usuario (CA-04) |
| 409 | EMAIL\_REGISTRADO | Registro con correo existente |
| 409 | GASTO\_ESTADO\_INVALIDO | Transición no permitida (RN-07, CA-11) |
| 409 | CONFLICTO\_VERSION | Optimistic locking: el gasto cambió mientras lo editabas |
| 413 | ARCHIVO\_MUY\_GRANDE | Imagen > 5 MB |
| 429 | DEMASIADAS\_SOLICITUDES | Más de 30 gastos por minuto por usuario |
| 503 | ALMACENAMIENTO\_NO\_DISPONIBLE | MinIO caído al subir imagen |

## 9. Servicio IA (Python)

Un worker Python consume `gastos.input`, estructura con un LLM local y publica en `gastos.processed`. La confianza la calcula el código con validaciones, no el modelo.

**Stack:** Python 3.12, `confluent-kafka`, `pydantic` v2, `ollama`, `paddleocr`, `minio`, `structlog`. Modelos: `qwen2.5:7b` (texto), `qwen2.5vl:7b` o `moondream` (visión).

**Pipeline de texto**

1. Consumir evento; si `eventId` ya se procesó, ignorar.
2. Llamar a Ollama con `format` = JSON Schema del modelo Pydantic y `temperature` 0.
3. Validar con Pydantic; si falla, reintentar una vez con el error en el prompt.
4. Calcular confianza con reglas.
5. Publicar en `gastos.processed`.

**Pipeline de imagen.** Se implementan los dos enfoques y se comparan con el mismo dataset (ver ADR-05):

- Enfoque A: PaddleOCR extrae texto → el mismo LLM de texto estructura. `textoExtraido` se guarda.
- Enfoque B: modelo de visión recibe la imagen y devuelve el JSON directamente.

```python
from datetime import date
from pydantic import BaseModel, Field
import ollama

PROMPT_VERSION = "texto-v1"
MODELO = "qwen2.5:7b"

class GastoIA(BaseModel):
    monto: float = Field(gt=0)
    categoria: str
    fecha: date
    descripcion: str = Field(max_length=255)
    metodo_pago: str | None = None
    comercio: str | None = None

def system_prompt(fecha_ref: date, categorias: list[str]) -> str:
    return (
        f"Extraes gastos de texto en español de Colombia. Hoy es {fecha_ref.isoformat()}. "
        "Moneda COP; el punto es separador de miles (45.000 = 45000). "
        f"Categorías válidas: {', '.join(categorias)}. Si no estás seguro usa 'otros'. "
        "Resuelve fechas relativas (hoy, ayer, el lunes) contra la fecha de hoy."
    )

def estructurar(texto: str, fecha_ref: date, categorias: list[str]) -> GastoIA:
    resp = ollama.chat(
        model=MODELO,
        format=GastoIA.model_json_schema(),
        options={"temperature": 0},
        messages=[
            {"role": "system", "content": system_prompt(fecha_ref, categorias)},
            {"role": "user", "content": texto},
        ],
    )
    return GastoIA.model_validate_json(resp.message.content)

def confianza(g: GastoIA, texto: str, fecha_ref: date, categorias: list[str]) -> float:
    digitos = "".join(c for c in texto if c.isdigit())
    checks = [
        g.categoria in categorias and g.categoria != "otros",
        g.fecha <= fecha_ref and (fecha_ref - g.fecha).days <= 60,
        str(int(g.monto)) in digitos,        # el monto aparece en el texto
        g.monto < 50_000_000,                # cota de cordura
    ]
    return round(sum(checks) / len(checks), 2)
```

**IA responsable**

- Salida forzada con schema; nunca se parsea texto libre.
- El humano tiene la última palabra: todo gasto de baja confianza pasa por revisión manual.
- Trazabilidad: modelo, versión de prompt, salida cruda y confianza quedan guardados por gasto.
- Dataset de evaluación versionado en el repo: `eval/texto.jsonl` (100 frases) y `eval/recibos/` (30 fotos) con la respuesta esperada. Un script `eval.py` mide exactitud por campo, latencia y % NEEDS\_REVIEW por cada combinación modelo + prompt.
- Cambiar de modelo o prompt solo se hace si `eval.py` no empeora.
- Las correcciones manuales del usuario se guardan y alimentan el dataset de evaluación.
- Privacidad: inferencia 100% local; los logs no incluyen texto ni montos, solo ids.

## 10. Reglas de recomendaciones y alertas

Todas se evalúan en el backend al consumir `gastos.confirmed`, sobre `monthly_metrics` ya actualizadas. Los umbrales viven en `application.yml` para ajustarlos sin tocar código.

| Regla | Tipo | Condición | Mensaje ejemplo |
| --- | --- | --- | --- |
| AUMENTO\_MENSUAL | Recomendación · Comparativo | Total del mes > total del mes anterior × 1.20 | Llevas 30% más que el mes pasado |
| CATEGORIA\_DOMINANTE | Recomendación · Advertencia | Una categoría > 40% del total del mes | Transporte es el 42% de tu gasto |
| MICROGASTOS | Recomendación · Ahorro | ≥ 10 gastos < 15.000 en la misma categoría en el mes | 12 gastos pequeños en café suman 95.000 |
| PROYECCION\_AHORRO | Recomendación · Proyección | Hay meta y el ritmo actual no la alcanza | Si reduces entretenimiento 15% llegas a tu meta |
| PRESUPUESTO\_80 | Alerta | Consumido ≥ 80% de un presupuesto | Usaste el 80% del presupuesto de comida |
| PRESUPUESTO\_100 | Alerta | Consumido ≥ 100% | Superaste el presupuesto de comida |
| GASTO\_INUSUAL | Alerta | Monto > 3× promedio de la categoría (últimos 3 meses, mínimo 5 gastos) | Este gasto de 600.000 es inusual para hogar |
| TENDENCIA | Alerta | Total mensual creciente 3 meses seguidos | Tu gasto lleva 3 meses subiendo |

La proyección de ahorro usa: ahorro proyectado = ingreso mensual − (gasto a la fecha / días transcurridos × días del mes).

Diseño del motor como interfaz para agregar reglas sin tocar el consumidor:

```java
public interface Regla {
    String codigo();
    Mono<Optional<Hallazgo>> evaluar(ContextoUsuario ctx);
}

@Component
public class MotorReglas {
    private final List<Regla> reglas;
    private final HallazgoRepository repo;

    public Mono<Void> evaluar(ContextoUsuario ctx) {
        return Flux.fromIterable(reglas)
            .flatMap(r -> r.evaluar(ctx))
            .flatMap(Mono::justOrEmpty)
            .flatMap(repo::guardarSiNoExiste)   // ON CONFLICT DO NOTHING
            .then();
    }
}
```

`guardarSiNoExiste` usa las restricciones UNIQUE de `recommendations` y `alerts`; si inserta una alerta, publica en `alertas.generated`.

## 11. Decisiones, estructura y plan

**ADRs.** Cada uno va como archivo en `docs/adr/` con contexto, decisión, alternativas y consecuencias.

| ADR | Decisión | Alternativa descartada | Por qué |
| --- | --- | --- | --- |
| 01 | Kafka entre backend e IA | Llamada HTTP síncrona | La IA es lenta y puede caerse; el objetivo del proyecto es aprender event-driven, aunque un usuario no lo necesite |
| 02 | IA local con Ollama | API externa (OpenAI, Claude) | Privacidad de datos financieros; contraste con el proyecto 2, que usará API |
| 03 | Patrón outbox para publicar | Publicar directo tras el commit | Evita gastos guardados sin evento o eventos sin gasto |
| 04 | Imágenes en MinIO, referencia en el evento | Imagen en base64 dentro del mensaje | Kafka no es para binarios grandes; claim check |
| 05 | Comparar OCR+LLM vs modelo de visión | Elegir uno a priori | La decisión se toma con datos de `eval.py` |
| 06 | Confianza calculada por reglas | Pedirle la confianza al LLM | Los LLM no dan confianza calibrada |
| 07 | Recomendaciones por reglas en el backend | LLM generando consejos | Determinista, explicable y testeable para el MVP |
| 08 | WebFlux + R2DBC | Spring MVC + JPA | Objetivo de aprendizaje: reactivo end-to-end |
| 09 | SSE para notificaciones | WebSockets | Solo hace falta servidor → cliente; WebSockets queda para el proyecto 3 |
| 10 | Backend como monolito modular | Microservicio por dominio | Un solo servicio Java con módulos; el único servicio aparte es IA (otro lenguaje y otro perfil de carga) |

**Estructura del repositorio (monorepo)**

```
personal-ai-finance/
├── docker-compose.yml          # postgres, kafka (KRaft), minio, ollama, kafka-ui
├── docs/
│   └── adr/
├── backend/                    # Spring Boot 3, WebFlux, R2DBC, Flyway
│   └── src/main/java/.../finance/
│       ├── auth/
│       ├── gastos/             # API, servicio, outbox, consumidor processed
│       ├── metricas/
│       ├── reglas/             # recomendaciones + alertas
│       ├── presupuestos/
│       ├── notificaciones/     # SSE
│       └── shared/             # eventos, kafka config, seguridad
├── ia-service/                 # Python worker
│   ├── app/
│   │   ├── consumer.py
│   │   ├── texto.py
│   │   ├── imagen.py
│   │   └── confianza.py
│   ├── eval/
│   │   ├── texto.jsonl
│   │   ├── recibos/
│   │   └── eval.py
│   └── tests/
└── frontend/                   # Angular
```

**Plan de construcción**

- [ ] Escribir ADR-01 a ADR-10 en `docs/adr/`
- [ ] docker-compose con Postgres, Kafka KRaft, MinIO, Ollama y kafka-ui levantando
- [ ] Fase A: migraciones Flyway, registro y login JWT, CRUD manual de gastos
- [ ] Crear `eval/texto.jsonl` con 100 frases etiquetadas antes de escribir el prompt
- [ ] Fase B: outbox + publisher, worker IA de texto, consumidor processed, idempotencia, DLQ, job de timeout
- [ ] Correr `eval.py` y fijar el umbral de confianza
- [ ] Fase C: métricas incrementales, endpoints de dashboard, reglas de recomendación, Angular dashboard
- [ ] Fase D: subida a MinIO, enfoques A y B, `eval/recibos/` con 30 fotos, decidir con datos
- [ ] Fase E: presupuestos, metas, motor de alertas, SSE
- [ ] Pruebas de carga simples (k6) y análisis de cuellos de botella: ¿dónde se forma la cola?

## 12. Preguntas abiertas

Decisiones que no están tomadas. Cada una debería terminar en un ADR.

- [ ] ¿Un solo topic `gastos.input` o separar `gastos.input.texto` y `gastos.input.imagen` para que las imágenes lentas no retrasen los textos?
- [ ] ¿El backend sigue como monolito modular o se separa métricas/alertas en su propio servicio?
- [ ] ¿Outbox con polling desde Java o Debezium (CDC)?
- [ ] ¿R2DBC puro o jOOQ/Spring Data R2DBC para las consultas de métricas?
- [ ] ¿Qué enfoque de imagen gana: OCR + LLM o modelo de visión? Se decide con `eval.py`.
- [ ] ¿Umbral de confianza 0.75 o el que salga del dataset de evaluación?
- [ ] ¿Retención de imágenes: para siempre, 1 año, o comprimir tras confirmar?
- [ ] ¿La IA también genera el texto de las recomendaciones en una iteración futura, y cómo se evalúa?
- [ ] ¿Cómo se versionan los contratos de eventos (campo `version`, schema registry)?
- [ ] ¿Auth propio con JWT o Keycloak para practicar OAuth2?
