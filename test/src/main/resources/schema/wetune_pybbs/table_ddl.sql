


CREATE TABLE IF NOT EXISTS pybbs."admin_user" (
  "id" integer NOT NULL,
  "username" varchar(255) NOT NULL DEFAULT '',
  "password" varchar(255) NOT NULL DEFAULT '',
  "in_time" timestamp NOT NULL,
  "role_id" integer NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS pybbs."code" (
  "id" integer NOT NULL,
  "user_id" integer DEFAULT NULL,
  "code" varchar(255) NOT NULL DEFAULT '',
  "in_time" timestamp NOT NULL,
  "expire_time" timestamp NOT NULL,
  "email" varchar(255) DEFAULT NULL,
  "mobile" varchar(255) DEFAULT NULL,
  "used" bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS pybbs."collect" (
  "topic_id" integer NOT NULL,
  "user_id" integer NOT NULL,
  "in_time" timestamp NOT NULL
);


CREATE TABLE IF NOT EXISTS pybbs."comment" (
  "id" integer NOT NULL,
  "content" text NOT NULL,
  "topic_id" integer NOT NULL,
  "user_id" integer NOT NULL,
  "in_time" timestamp NOT NULL,
  "comment_id" integer DEFAULT NULL,
  "up_ids" text,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS pybbs."flyway_schema_history" (
  "installed_rank" integer NOT NULL,
  "version" varchar(50) DEFAULT NULL,
  "description" varchar(200) NOT NULL,
  "type" varchar(20) NOT NULL,
  "script" varchar(1000) NOT NULL,
  "checksum" integer DEFAULT NULL,
  "installed_by" varchar(100) NOT NULL,
  "installed_on" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "execution_time" integer NOT NULL,
  "success" boolean NOT NULL,
  PRIMARY KEY (installed_rank)
);


CREATE TABLE IF NOT EXISTS pybbs."notification" (
  "id" integer NOT NULL,
  "topic_id" integer NOT NULL,
  "user_id" integer NOT NULL,
  "target_user_id" integer NOT NULL,
  "action" varchar(255) NOT NULL DEFAULT '',
  "in_time" timestamp NOT NULL,
  "read" bit(1) NOT NULL DEFAULT b'0',
  "content" text,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS pybbs."oauth_user" (
  "id" integer NOT NULL,
  "oauth_id" integer DEFAULT NULL,
  "type" varchar(255) NOT NULL DEFAULT '',
  "login" varchar(255) NOT NULL DEFAULT '',
  "access_token" varchar(255) NOT NULL DEFAULT '',
  "in_time" timestamp NOT NULL,
  "bio" text,
  "email" varchar(255) DEFAULT NULL,
  "user_id" integer NOT NULL,
  "refresh_token" varchar(255) DEFAULT NULL,
  "union_id" varchar(255) DEFAULT NULL,
  "expires_in" varchar(255) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS pybbs."permission" (
  "id" integer NOT NULL,
  "name" varchar(255) NOT NULL DEFAULT '',
  "value" varchar(255) NOT NULL DEFAULT '',
  "pid" integer NOT NULL DEFAULT '0',
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS pybbs."role" (
  "id" integer NOT NULL,
  "name" varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS pybbs."role_permission" (
  "role_id" integer NOT NULL,
  "permission_id" integer NOT NULL
);


CREATE TABLE IF NOT EXISTS pybbs."sensitive_word" (
  "id" integer NOT NULL,
  "word" varchar(255) NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS pybbs."system_config" (
  "id" integer NOT NULL,
  "value" varchar(255) DEFAULT '',
  "description" varchar(1000) NOT NULL,
  "pid" integer NOT NULL DEFAULT '0',
  "type" varchar(255) DEFAULT NULL,
  "option" varchar(255) DEFAULT NULL,
  "reboot" integer NOT NULL DEFAULT '0',
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS pybbs."tag" (
  "id" integer NOT NULL,
  "name" varchar(255) NOT NULL DEFAULT '',
  "description" varchar(1000) DEFAULT NULL,
  "icon" varchar(255) DEFAULT NULL,
  "topic_count" integer NOT NULL DEFAULT '0',
  "in_time" timestamp NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS pybbs."topic" (
  "id" integer NOT NULL,
  "title" varchar(255) NOT NULL DEFAULT '',
  "content" text,
  "in_time" timestamp NOT NULL,
  "modify_time" timestamp DEFAULT NULL,
  "user_id" integer NOT NULL,
  "comment_count" integer NOT NULL DEFAULT '0',
  "collect_count" integer NOT NULL DEFAULT '0',
  "view" integer NOT NULL DEFAULT '0',
  "top" bit(1) NOT NULL DEFAULT b'0',
  "good" bit(1) NOT NULL DEFAULT b'0',
  "up_ids" text,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS pybbs."topic_tag" (
  "tag_id" integer NOT NULL,
  "topic_id" integer NOT NULL
);


CREATE TABLE IF NOT EXISTS pybbs."user" (
  "id" integer NOT NULL,
  "username" varchar(255) NOT NULL DEFAULT '',
  "password" varchar(255) DEFAULT '',
  "avatar" varchar(1000) DEFAULT NULL,
  "email" varchar(255) DEFAULT NULL,
  "mobile" varchar(255) DEFAULT NULL,
  "website" varchar(255) DEFAULT NULL,
  "bio" varchar(1000) DEFAULT NULL,
  "score" integer NOT NULL DEFAULT '0',
  "in_time" timestamp NOT NULL,
  "token" varchar(255) NOT NULL DEFAULT '',
  "telegram_name" varchar(255) DEFAULT NULL,
  "email_notification" bit(1) NOT NULL DEFAULT b'0',
  "active" bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (id)
);

