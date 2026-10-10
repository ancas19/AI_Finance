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

Note: '''
CHECK entry_type IN (TEXT, IMAGE)
CHECK status IN (PROCESSING, NEEDS_REVIEW, CONFIRMED, FAILED)
CHECK (entry_type = TEXT AND raw_text IS NOT NULL) OR (entry_type = IMAGE AND image_id IS NOT NULL)
'''
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

Note: 'CHECK amount > 0 AND amount <= 50000000. Solo gastos confirmados.'
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

Note: 'CHECK amount > 0'
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

Note: 'CHECK target_amount > 0'
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

Note: 'Evento guardado en la misma transaccion del cambio de negocio'
}

Table processed_events {
event_id uuid [not null]
consumer varchar(60) [not null]
processed_at timestamp [not null]

indexes {
(event_id, consumer) [pk]
}

Note: 'Idempotencia: un evento se aplica una sola vez por consumidor'
}