


CREATE TABLE IF NOT EXISTS guns."sys_dept" (
  "dept_id" bigint NOT NULL,
  "pid" bigint DEFAULT '0',
  "pids" varchar(512) DEFAULT '',
  "simple_name" varchar(45) DEFAULT NULL,
  "full_name" varchar(255) DEFAULT NULL,
  "description" varchar(255) DEFAULT NULL,
  "version" integer DEFAULT NULL,
  "sort" integer DEFAULT NULL,
  "create_time" timestamp DEFAULT NULL,
  "update_time" timestamp DEFAULT NULL,
  "create_user" bigint DEFAULT NULL,
  "update_user" bigint DEFAULT NULL,
  PRIMARY KEY (dept_id) USING BTREE
);


CREATE TABLE IF NOT EXISTS guns."sys_dict" (
  "dict_id" bigint NOT NULL,
  "dict_type_id" bigint NOT NULL,
  "code" varchar(50) NOT NULL,
  "name" varchar(255) NOT NULL,
  "parent_id" bigint NOT NULL,
  "parent_ids" varchar(255) DEFAULT NULL,
  "status" varchar(10) NOT NULL DEFAULT 'ENABLE',
  "sort" integer DEFAULT NULL,
  "description" varchar(1000) DEFAULT NULL,
  "create_time" timestamp DEFAULT NULL,
  "update_time" timestamp DEFAULT NULL,
  "create_user" bigint DEFAULT NULL,
  "update_user" bigint DEFAULT NULL,
  PRIMARY KEY (dict_id) USING BTREE
);


CREATE TABLE IF NOT EXISTS guns."sys_dict_type" (
  "dict_type_id" bigint NOT NULL,
  "code" varchar(255) NOT NULL,
  "name" varchar(255) NOT NULL,
  "description" varchar(1000) DEFAULT NULL,
  "system_flag" char(1) NOT NULL,
  "status" varchar(10) NOT NULL DEFAULT 'ENABLE',
  "sort" integer DEFAULT NULL,
  "create_time" timestamp DEFAULT NULL,
  "create_user" bigint DEFAULT NULL,
  "update_time" timestamp DEFAULT NULL,
  "update_user" bigint DEFAULT NULL,
  PRIMARY KEY (dict_type_id) USING BTREE
);


CREATE TABLE IF NOT EXISTS guns."sys_file_info" (
  "file_id" varchar(50) NOT NULL,
  "file_data" text,
  "create_time" timestamp DEFAULT NULL,
  "update_time" timestamp DEFAULT NULL,
  "create_user" bigint DEFAULT NULL,
  "update_user" bigint DEFAULT NULL,
  PRIMARY KEY (file_id) USING BTREE
);


CREATE TABLE IF NOT EXISTS guns."sys_login_log" (
  "login_log_id" bigint NOT NULL,
  "log_name" varchar(255) DEFAULT NULL,
  "user_id" bigint DEFAULT NULL,
  "create_time" timestamp DEFAULT NULL,
  "succeed" varchar(255) DEFAULT NULL,
  "message" text,
  "ip_address" varchar(255) DEFAULT NULL,
  PRIMARY KEY (login_log_id) USING BTREE
);


CREATE TABLE IF NOT EXISTS guns."sys_menu" (
  "menu_id" bigint NOT NULL,
  "code" varchar(255) DEFAULT NULL,
  "pcode" varchar(255) DEFAULT NULL,
  "pcodes" varchar(255) DEFAULT NULL,
  "name" varchar(255) DEFAULT NULL,
  "icon" varchar(255) DEFAULT NULL,
  "url" varchar(255) DEFAULT NULL,
  "sort" integer DEFAULT NULL,
  "levels" integer DEFAULT NULL,
  "menu_flag" varchar(32) DEFAULT NULL,
  "description" varchar(255) DEFAULT NULL,
  "status" varchar(32) DEFAULT 'ENABLE',
  "new_page_flag" varchar(32) DEFAULT NULL,
  "open_flag" varchar(32) DEFAULT NULL,
  "create_time" timestamp DEFAULT NULL,
  "update_time" timestamp DEFAULT NULL,
  "create_user" bigint DEFAULT NULL,
  "update_user" bigint DEFAULT NULL,
  PRIMARY KEY (menu_id) USING BTREE
);


CREATE TABLE IF NOT EXISTS guns."sys_notice" (
  "notice_id" bigint NOT NULL,
  "title" varchar(255) DEFAULT NULL,
  "content" text,
  "create_time" timestamp DEFAULT NULL,
  "create_user" bigint DEFAULT NULL,
  "update_time" timestamp DEFAULT NULL,
  "update_user" bigint DEFAULT NULL,
  PRIMARY KEY (notice_id) USING BTREE
);


CREATE TABLE IF NOT EXISTS guns."sys_operation_log" (
  "operation_log_id" bigint NOT NULL,
  "log_type" varchar(32) DEFAULT NULL,
  "log_name" varchar(255) DEFAULT NULL,
  "user_id" bigint DEFAULT NULL,
  "class_name" varchar(255) DEFAULT NULL,
  "method" text,
  "create_time" timestamp DEFAULT NULL,
  "succeed" varchar(32) DEFAULT NULL,
  "message" text,
  PRIMARY KEY (operation_log_id) USING BTREE
);


CREATE TABLE IF NOT EXISTS guns."sys_relation" (
  "relation_id" bigint NOT NULL,
  "menu_id" bigint DEFAULT NULL,
  "role_id" bigint DEFAULT NULL,
  PRIMARY KEY (relation_id) USING BTREE
);


CREATE TABLE IF NOT EXISTS guns."sys_role" (
  "role_id" bigint NOT NULL,
  "pid" bigint DEFAULT NULL,
  "name" varchar(255) DEFAULT NULL,
  "description" varchar(255) DEFAULT NULL,
  "sort" integer DEFAULT NULL,
  "version" integer DEFAULT NULL,
  "create_time" timestamp DEFAULT NULL,
  "update_time" timestamp DEFAULT NULL,
  "create_user" bigint DEFAULT NULL,
  "update_user" bigint DEFAULT NULL,
  PRIMARY KEY (role_id) USING BTREE
);


CREATE TABLE IF NOT EXISTS guns."sys_user" (
  "user_id" bigint NOT NULL,
  "avatar" varchar(255) DEFAULT NULL,
  "account" varchar(45) DEFAULT NULL,
  "password" varchar(45) DEFAULT NULL,
  "salt" varchar(45) DEFAULT NULL,
  "name" varchar(45) DEFAULT NULL,
  "birthday" timestamp DEFAULT NULL,
  "sex" varchar(32) DEFAULT NULL,
  "email" varchar(45) DEFAULT NULL,
  "phone" varchar(45) DEFAULT NULL,
  "role_id" varchar(255) DEFAULT NULL,
  "dept_id" bigint DEFAULT NULL,
  "status" varchar(32) DEFAULT NULL,
  "create_time" timestamp DEFAULT NULL,
  "create_user" bigint DEFAULT NULL,
  "update_time" timestamp DEFAULT NULL,
  "update_user" bigint DEFAULT NULL,
  "version" integer DEFAULT NULL,
  PRIMARY KEY (user_id) USING BTREE
);

