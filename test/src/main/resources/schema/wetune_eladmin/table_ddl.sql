


CREATE TABLE IF NOT EXISTS eladmin."alipay_config" (
  "id" bigint NOT NULL,
  "app_id" varchar(255) DEFAULT NULL,
  "charset" varchar(255) DEFAULT NULL,
  "format" varchar(255) DEFAULT NULL,
  "gateway_url" varchar(255) DEFAULT NULL,
  "notify_url" varchar(255) DEFAULT NULL,
  "private_key" text,
  "public_key" text,
  "return_url" varchar(255) DEFAULT NULL,
  "sign_type" varchar(255) DEFAULT NULL,
  "sys_service_provider_id" varchar(255) DEFAULT NULL,
  PRIMARY KEY (id) USING BTREE
);


CREATE TABLE IF NOT EXISTS eladmin."dept" (
  "id" bigint NOT NULL,
  "name" varchar(255) NOT NULL,
  "pid" bigint NOT NULL,
  "create_time" timestamp DEFAULT NULL,
  "enabled" bit(1) NOT NULL,
  PRIMARY KEY (id) USING BTREE
);


CREATE TABLE IF NOT EXISTS eladmin."dict" (
  "id" bigint NOT NULL,
  "name" varchar(255) NOT NULL,
  "remark" varchar(255) DEFAULT NULL,
  PRIMARY KEY (id) USING BTREE
);


CREATE TABLE IF NOT EXISTS eladmin."dict_detail" (
  "id" bigint NOT NULL,
  "label" varchar(255) NOT NULL,
  "value" varchar(255) NOT NULL,
  "sort" varchar(255) DEFAULT NULL,
  "dict_id" bigint DEFAULT NULL,
  PRIMARY KEY (id) USING BTREE
);


CREATE TABLE IF NOT EXISTS eladmin."email_config" (
  "id" bigint NOT NULL,
  "from_user" varchar(255) DEFAULT NULL,
  "host" varchar(255) DEFAULT NULL,
  "pass" varchar(255) DEFAULT NULL,
  "port" varchar(255) DEFAULT NULL,
  "user" varchar(255) DEFAULT NULL,
  PRIMARY KEY (id) USING BTREE
);


CREATE TABLE IF NOT EXISTS eladmin."gen_config" (
  "id" bigint NOT NULL,
  "author" varchar(255) DEFAULT NULL,
  "cover" bit(1) DEFAULT NULL,
  "module_name" varchar(255) DEFAULT NULL,
  "pack" varchar(255) DEFAULT NULL,
  "path" varchar(255) DEFAULT NULL,
  "api_path" varchar(255) DEFAULT NULL,
  "prefix" varchar(255) DEFAULT NULL,
  PRIMARY KEY (id) USING BTREE
);


CREATE TABLE IF NOT EXISTS eladmin."job" (
  "id" bigint NOT NULL,
  "name" varchar(255) NOT NULL,
  "enabled" bit(1) NOT NULL,
  "create_time" timestamp DEFAULT NULL,
  "sort" bigint NOT NULL,
  "dept_id" bigint DEFAULT NULL,
  PRIMARY KEY (id) USING BTREE
);


CREATE TABLE IF NOT EXISTS eladmin."local_storage" (
  "id" bigint NOT NULL,
  "real_name" varchar(255) DEFAULT NULL,
  "name" varchar(255) DEFAULT NULL,
  "suffix" varchar(255) DEFAULT NULL,
  "path" varchar(255) DEFAULT NULL,
  "type" varchar(255) DEFAULT NULL,
  "size" varchar(100) DEFAULT NULL,
  "operate" varchar(255) DEFAULT NULL,
  "create_time" timestamp DEFAULT NULL,
  "update_time" timestamp DEFAULT NULL,
  PRIMARY KEY (id) USING BTREE
);


CREATE TABLE IF NOT EXISTS eladmin."log" (
  "id" bigint NOT NULL,
  "create_time" timestamp DEFAULT NULL,
  "description" varchar(255) DEFAULT NULL,
  "exception_detail" text,
  "log_type" varchar(255) DEFAULT NULL,
  "method" varchar(255) DEFAULT NULL,
  "params" text,
  "request_ip" varchar(255) DEFAULT NULL,
  "time" bigint DEFAULT NULL,
  "username" varchar(255) DEFAULT NULL,
  "address" varchar(255) DEFAULT NULL,
  PRIMARY KEY (id) USING BTREE
);


CREATE TABLE IF NOT EXISTS eladmin."menu" (
  "id" bigint NOT NULL,
  "create_time" timestamp DEFAULT NULL,
  "i_frame" bit(1) DEFAULT NULL,
  "name" varchar(255) DEFAULT NULL,
  "component" varchar(255) DEFAULT NULL,
  "pid" bigint NOT NULL,
  "sort" bigint NOT NULL,
  "icon" varchar(255) DEFAULT NULL,
  "path" varchar(255) DEFAULT NULL,
  "cache" bit(1) DEFAULT b'0',
  "hidden" bit(1) DEFAULT b'0',
  "component_name" varchar(20) DEFAULT '-',
  PRIMARY KEY (id) USING BTREE
);


CREATE TABLE IF NOT EXISTS eladmin."permission" (
  "id" bigint NOT NULL,
  "alias" varchar(255) DEFAULT NULL,
  "create_time" timestamp DEFAULT NULL,
  "name" varchar(255) DEFAULT NULL,
  "pid" integer NOT NULL,
  PRIMARY KEY (id) USING BTREE
);


CREATE TABLE IF NOT EXISTS eladmin."picture" (
  "id" bigint NOT NULL,
  "create_time" timestamp DEFAULT NULL,
  "delete_url" varchar(255) DEFAULT NULL,
  "filename" varchar(255) DEFAULT NULL,
  "height" varchar(255) DEFAULT NULL,
  "size" varchar(255) DEFAULT NULL,
  "url" varchar(255) DEFAULT NULL,
  "username" varchar(255) DEFAULT NULL,
  "width" varchar(255) DEFAULT NULL,
  PRIMARY KEY (id) USING BTREE
);


CREATE TABLE IF NOT EXISTS eladmin."qiniu_config" (
  "id" bigint NOT NULL,
  "access_key" text,
  "bucket" varchar(255) DEFAULT NULL,
  "host" varchar(255) NOT NULL,
  "secret_key" text,
  "type" varchar(255) DEFAULT NULL,
  "zone" varchar(255) DEFAULT NULL,
  PRIMARY KEY (id) USING BTREE
);


CREATE TABLE IF NOT EXISTS eladmin."qiniu_content" (
  "id" bigint NOT NULL,
  "bucket" varchar(255) DEFAULT NULL,
  "name" varchar(255) DEFAULT NULL,
  "size" varchar(255) DEFAULT NULL,
  "type" varchar(255) DEFAULT NULL,
  "update_time" timestamp DEFAULT NULL,
  "url" varchar(255) DEFAULT NULL,
  "suffix" varchar(255) DEFAULT NULL,
  PRIMARY KEY (id) USING BTREE
);


CREATE TABLE IF NOT EXISTS eladmin."quartz_job" (
  "id" bigint NOT NULL,
  "bean_name" varchar(255) DEFAULT NULL,
  "cron_expression" varchar(255) DEFAULT NULL,
  "is_pause" bit(1) DEFAULT NULL,
  "job_name" varchar(255) DEFAULT NULL,
  "method_name" varchar(255) DEFAULT NULL,
  "params" varchar(255) DEFAULT NULL,
  "remark" varchar(255) DEFAULT NULL,
  "update_time" timestamp DEFAULT NULL,
  PRIMARY KEY (id) USING BTREE
);


CREATE TABLE IF NOT EXISTS eladmin."quartz_log" (
  "id" bigint NOT NULL,
  "baen_name" varchar(255) DEFAULT NULL,
  "create_time" timestamp DEFAULT NULL,
  "cron_expression" varchar(255) DEFAULT NULL,
  "exception_detail" text,
  "is_success" bit(1) DEFAULT NULL,
  "job_name" varchar(255) DEFAULT NULL,
  "method_name" varchar(255) DEFAULT NULL,
  "params" varchar(255) DEFAULT NULL,
  "time" bigint DEFAULT NULL,
  PRIMARY KEY (id) USING BTREE
);


CREATE TABLE IF NOT EXISTS eladmin."role" (
  "id" bigint NOT NULL,
  "create_time" timestamp DEFAULT NULL,
  "name" varchar(255) NOT NULL,
  "remark" varchar(255) DEFAULT NULL,
  "data_scope" varchar(255) DEFAULT NULL,
  "level" integer DEFAULT NULL,
  PRIMARY KEY (id) USING BTREE
);


CREATE TABLE IF NOT EXISTS eladmin."roles_depts" (
  "role_id" bigint NOT NULL,
  "dept_id" bigint NOT NULL,
  PRIMARY KEY (role_id,dept_id) USING BTREE
);


CREATE TABLE IF NOT EXISTS eladmin."roles_menus" (
  "menu_id" bigint NOT NULL,
  "role_id" bigint NOT NULL,
  PRIMARY KEY (menu_id,role_id) USING BTREE
);


CREATE TABLE IF NOT EXISTS eladmin."roles_permissions" (
  "role_id" bigint NOT NULL,
  "permission_id" bigint NOT NULL,
  PRIMARY KEY (role_id,permission_id) USING BTREE
);


CREATE TABLE IF NOT EXISTS eladmin."user" (
  "id" bigint NOT NULL,
  "avatar_id" bigint DEFAULT NULL,
  "create_time" timestamp DEFAULT NULL,
  "email" varchar(255) DEFAULT NULL,
  "enabled" bigint DEFAULT NULL,
  "password" varchar(255) DEFAULT NULL,
  "username" varchar(255) DEFAULT NULL,
  "last_password_reset_time" timestamp DEFAULT NULL,
  "dept_id" bigint DEFAULT NULL,
  "phone" varchar(255) DEFAULT NULL,
  "job_id" bigint DEFAULT NULL,
  PRIMARY KEY (id) USING BTREE
);


CREATE TABLE IF NOT EXISTS eladmin."user_avatar" (
  "id" bigint NOT NULL,
  "real_name" varchar(255) DEFAULT NULL,
  "path" varchar(255) DEFAULT NULL,
  "size" varchar(255) DEFAULT NULL,
  PRIMARY KEY (id) USING BTREE
);


CREATE TABLE IF NOT EXISTS eladmin."users_roles" (
  "user_id" bigint NOT NULL,
  "role_id" bigint NOT NULL,
  PRIMARY KEY (user_id,role_id) USING BTREE
);


CREATE TABLE IF NOT EXISTS eladmin."verification_code" (
  "id" bigint NOT NULL,
  "code" varchar(255) DEFAULT NULL,
  "create_time" timestamp DEFAULT NULL,
  "status" bit(1) DEFAULT NULL,
  "type" varchar(255) DEFAULT NULL,
  "value" varchar(255) DEFAULT NULL,
  "scenes" varchar(255) DEFAULT NULL,
  PRIMARY KEY (id) USING BTREE
);


CREATE TABLE IF NOT EXISTS eladmin."visits" (
  "id" bigint NOT NULL,
  "create_time" timestamp DEFAULT NULL,
  "date" varchar(255) DEFAULT NULL,
  "ip_counts" bigint DEFAULT NULL,
  "pv_counts" bigint DEFAULT NULL,
  "week_day" varchar(255) DEFAULT NULL,
  PRIMARY KEY (id) USING BTREE
);

