


CREATE TABLE IF NOT EXISTS febs."qrtz_blob_triggers" (
  "SCHED_NAME" varchar(120) NOT NULL,
  "TRIGGER_NAME" varchar(200) NOT NULL,
  "TRIGGER_GROUP" varchar(200) NOT NULL,
  "BLOB_DATA" bytea,
  PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
);


CREATE TABLE IF NOT EXISTS febs."qrtz_calendars" (
  "SCHED_NAME" varchar(120) NOT NULL,
  "CALENDAR_NAME" varchar(200) NOT NULL,
  "CALENDAR" bytea NOT NULL,
  PRIMARY KEY (SCHED_NAME,CALENDAR_NAME)
);


CREATE TABLE IF NOT EXISTS febs."qrtz_cron_triggers" (
  "SCHED_NAME" varchar(120) NOT NULL,
  "TRIGGER_NAME" varchar(200) NOT NULL,
  "TRIGGER_GROUP" varchar(200) NOT NULL,
  "CRON_EXPRESSION" varchar(200) NOT NULL,
  "TIME_ZONE_ID" varchar(80) DEFAULT NULL,
  PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
);


CREATE TABLE IF NOT EXISTS febs."qrtz_fired_triggers" (
  "SCHED_NAME" varchar(120) NOT NULL,
  "ENTRY_ID" varchar(95) NOT NULL,
  "TRIGGER_NAME" varchar(200) NOT NULL,
  "TRIGGER_GROUP" varchar(200) NOT NULL,
  "INSTANCE_NAME" varchar(200) NOT NULL,
  "FIRED_TIME" bigint NOT NULL,
  "SCHED_TIME" bigint NOT NULL,
  "PRIORITY" integer NOT NULL,
  "STATE" varchar(16) NOT NULL,
  "JOB_NAME" varchar(200) DEFAULT NULL,
  "JOB_GROUP" varchar(200) DEFAULT NULL,
  "IS_NONCONCURRENT" varchar(1) DEFAULT NULL,
  "REQUESTS_RECOVERY" varchar(1) DEFAULT NULL,
  PRIMARY KEY (SCHED_NAME,ENTRY_ID)
);


CREATE TABLE IF NOT EXISTS febs."qrtz_job_details" (
  "SCHED_NAME" varchar(120) NOT NULL,
  "JOB_NAME" varchar(200) NOT NULL,
  "JOB_GROUP" varchar(200) NOT NULL,
  "DESCRIPTION" varchar(250) DEFAULT NULL,
  "JOB_CLASS_NAME" varchar(250) NOT NULL,
  "IS_DURABLE" varchar(1) NOT NULL,
  "IS_NONCONCURRENT" varchar(1) NOT NULL,
  "IS_UPDATE_DATA" varchar(1) NOT NULL,
  "REQUESTS_RECOVERY" varchar(1) NOT NULL,
  "JOB_DATA" bytea,
  PRIMARY KEY (SCHED_NAME,JOB_NAME,JOB_GROUP)
);


CREATE TABLE IF NOT EXISTS febs."qrtz_locks" (
  "SCHED_NAME" varchar(120) NOT NULL,
  "LOCK_NAME" varchar(40) NOT NULL,
  PRIMARY KEY (SCHED_NAME,LOCK_NAME)
);


CREATE TABLE IF NOT EXISTS febs."qrtz_paused_trigger_grps" (
  "SCHED_NAME" varchar(120) NOT NULL,
  "TRIGGER_GROUP" varchar(200) NOT NULL,
  PRIMARY KEY (SCHED_NAME,TRIGGER_GROUP)
);


CREATE TABLE IF NOT EXISTS febs."qrtz_scheduler_state" (
  "SCHED_NAME" varchar(120) NOT NULL,
  "INSTANCE_NAME" varchar(200) NOT NULL,
  "LAST_CHECKIN_TIME" bigint NOT NULL,
  "CHECKIN_INTERVAL" bigint NOT NULL,
  PRIMARY KEY (SCHED_NAME,INSTANCE_NAME)
);


CREATE TABLE IF NOT EXISTS febs."qrtz_simple_triggers" (
  "SCHED_NAME" varchar(120) NOT NULL,
  "TRIGGER_NAME" varchar(200) NOT NULL,
  "TRIGGER_GROUP" varchar(200) NOT NULL,
  "REPEAT_COUNT" bigint NOT NULL,
  "REPEAT_INTERVAL" bigint NOT NULL,
  "TIMES_TRIGGERED" bigint NOT NULL,
  PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
);


CREATE TABLE IF NOT EXISTS febs."qrtz_simprop_triggers" (
  "SCHED_NAME" varchar(120) NOT NULL,
  "TRIGGER_NAME" varchar(200) NOT NULL,
  "TRIGGER_GROUP" varchar(200) NOT NULL,
  "STR_PROP_1" varchar(512) DEFAULT NULL,
  "STR_PROP_2" varchar(512) DEFAULT NULL,
  "STR_PROP_3" varchar(512) DEFAULT NULL,
  "INT_PROP_1" integer DEFAULT NULL,
  "INT_PROP_2" integer DEFAULT NULL,
  "LONG_PROP_1" bigint DEFAULT NULL,
  "LONG_PROP_2" bigint DEFAULT NULL,
  "DEC_PROP_1" decimal(13,4) DEFAULT NULL,
  "DEC_PROP_2" decimal(13,4) DEFAULT NULL,
  "BOOL_PROP_1" varchar(1) DEFAULT NULL,
  "BOOL_PROP_2" varchar(1) DEFAULT NULL,
  PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
);


CREATE TABLE IF NOT EXISTS febs."qrtz_triggers" (
  "SCHED_NAME" varchar(120) NOT NULL,
  "TRIGGER_NAME" varchar(200) NOT NULL,
  "TRIGGER_GROUP" varchar(200) NOT NULL,
  "JOB_NAME" varchar(200) NOT NULL,
  "JOB_GROUP" varchar(200) NOT NULL,
  "DESCRIPTION" varchar(250) DEFAULT NULL,
  "NEXT_FIRE_TIME" bigint DEFAULT NULL,
  "PREV_FIRE_TIME" bigint DEFAULT NULL,
  "PRIORITY" integer DEFAULT NULL,
  "TRIGGER_STATE" varchar(16) NOT NULL,
  "TRIGGER_TYPE" varchar(8) NOT NULL,
  "START_TIME" bigint NOT NULL,
  "END_TIME" bigint DEFAULT NULL,
  "CALENDAR_NAME" varchar(200) DEFAULT NULL,
  "MISFIRE_INSTR" smallint(2) DEFAULT NULL,
  "JOB_DATA" bytea,
  PRIMARY KEY (SCHED_NAME,TRIGGER_NAME,TRIGGER_GROUP)
);


CREATE TABLE IF NOT EXISTS febs."t_dept" (
  "DEPT_ID" bigint NOT NULL,
  "PARENT_ID" bigint NOT NULL,
  "DEPT_NAME" varchar(100) NOT NULL,
  "ORDER_NUM" bigint DEFAULT NULL,
  "CREATE_TIME" timestamp DEFAULT NULL,
  "MODIFY_TIME" timestamp DEFAULT NULL,
  PRIMARY KEY (DEPT_ID) USING BTREE
);


CREATE TABLE IF NOT EXISTS febs."t_eximport" (
  "FIELD1" varchar(20) NOT NULL,
  "FIELD2" integer NOT NULL,
  "FIELD3" varchar(100) NOT NULL,
  "CREATE_TIME" timestamp NOT NULL
);


CREATE TABLE IF NOT EXISTS febs."t_generator_config" (
  "id" integer NOT NULL,
  "author" varchar(20) NOT NULL,
  "base_package" varchar(50) NOT NULL,
  "entity_package" varchar(20) NOT NULL,
  "mapper_package" varchar(20) NOT NULL,
  "mapper_xml_package" varchar(20) NOT NULL,
  "service_package" varchar(20) NOT NULL,
  "service_impl_package" varchar(20) NOT NULL,
  "controller_package" varchar(20) NOT NULL,
  "is_trim" char(1) NOT NULL,
  "trim_value" varchar(10) DEFAULT NULL,
  PRIMARY KEY (id) USING BTREE
);


CREATE TABLE IF NOT EXISTS febs."t_job" (
  "JOB_ID" bigint NOT NULL,
  "BEAN_NAME" varchar(50) NOT NULL,
  "METHOD_NAME" varchar(50) NOT NULL,
  "PARAMS" varchar(50) DEFAULT NULL,
  "CRON_EXPRESSION" varchar(20) NOT NULL,
  "STATUS" char(2) NOT NULL,
  "REMARK" varchar(50) DEFAULT NULL,
  "CREATE_TIME" timestamp DEFAULT NULL,
  PRIMARY KEY (JOB_ID) USING BTREE
);


CREATE TABLE IF NOT EXISTS febs."t_job_log" (
  "LOG_ID" bigint NOT NULL,
  "JOB_ID" bigint NOT NULL,
  "BEAN_NAME" varchar(100) NOT NULL,
  "METHOD_NAME" varchar(100) NOT NULL,
  "PARAMS" varchar(200) DEFAULT NULL,
  "STATUS" char(2) NOT NULL,
  "ERROR" text,
  "TIMES" decimal(11,0) DEFAULT NULL,
  "CREATE_TIME" timestamp DEFAULT NULL,
  PRIMARY KEY (LOG_ID) USING BTREE
);


CREATE TABLE IF NOT EXISTS febs."t_log" (
  "ID" bigint NOT NULL,
  "USERNAME" varchar(50) DEFAULT NULL,
  "OPERATION" text,
  "TIME" decimal(11,0) DEFAULT NULL,
  "METHOD" text,
  "PARAMS" text,
  "IP" varchar(64) DEFAULT NULL,
  "CREATE_TIME" timestamp DEFAULT NULL,
  "location" varchar(50) DEFAULT NULL,
  PRIMARY KEY (ID) USING BTREE
);


CREATE TABLE IF NOT EXISTS febs."t_login_log" (
  "ID" bigint NOT NULL,
  "USERNAME" varchar(50) NOT NULL,
  "LOGIN_TIME" timestamp NOT NULL,
  "LOCATION" varchar(50) DEFAULT NULL,
  "IP" varchar(50) DEFAULT NULL,
  "SYSTEM" varchar(50) DEFAULT NULL,
  "BROWSER" varchar(50) DEFAULT NULL,
  PRIMARY KEY (ID) USING BTREE
);


CREATE TABLE IF NOT EXISTS febs."t_menu" (
  "MENU_ID" bigint NOT NULL,
  "PARENT_ID" bigint NOT NULL,
  "MENU_NAME" varchar(50) NOT NULL,
  "URL" varchar(50) DEFAULT NULL,
  "PERMS" text,
  "ICON" varchar(50) DEFAULT NULL,
  "TYPE" char(2) NOT NULL,
  "ORDER_NUM" bigint DEFAULT NULL,
  "CREATE_TIME" timestamp NOT NULL,
  "MODIFY_TIME" timestamp DEFAULT NULL,
  PRIMARY KEY (MENU_ID) USING BTREE
);


CREATE TABLE IF NOT EXISTS febs."t_role" (
  "ROLE_ID" bigint NOT NULL,
  "ROLE_NAME" varchar(100) NOT NULL,
  "REMARK" varchar(100) DEFAULT NULL,
  "CREATE_TIME" timestamp NOT NULL,
  "MODIFY_TIME" timestamp DEFAULT NULL,
  PRIMARY KEY (ROLE_ID) USING BTREE
);


CREATE TABLE IF NOT EXISTS febs."t_role_menu" (
  "ROLE_ID" bigint NOT NULL,
  "MENU_ID" bigint NOT NULL
);


CREATE TABLE IF NOT EXISTS febs."t_user" (
  "USER_ID" bigint NOT NULL,
  "USERNAME" varchar(50) NOT NULL,
  "PASSWORD" varchar(128) NOT NULL,
  "DEPT_ID" bigint DEFAULT NULL,
  "EMAIL" varchar(128) DEFAULT NULL,
  "MOBILE" varchar(20) DEFAULT NULL,
  "STATUS" char(1) NOT NULL,
  "CREATE_TIME" timestamp NOT NULL,
  "MODIFY_TIME" timestamp DEFAULT NULL,
  "LAST_LOGIN_TIME" timestamp DEFAULT NULL,
  "SSEX" char(1) DEFAULT NULL,
  "IS_TAB" char(1) DEFAULT NULL,
  "THEME" varchar(10) DEFAULT NULL,
  "AVATAR" varchar(100) DEFAULT NULL,
  "DESCRIPTION" varchar(100) DEFAULT NULL,
  PRIMARY KEY (USER_ID) USING BTREE
);


CREATE TABLE IF NOT EXISTS febs."t_user_role" (
  "USER_ID" bigint NOT NULL,
  "ROLE_ID" bigint NOT NULL
);

