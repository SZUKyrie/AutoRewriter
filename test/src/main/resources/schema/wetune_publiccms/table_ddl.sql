


CREATE TABLE IF NOT EXISTS publiccms."cms_category" (
  "id" integer NOT NULL,
  "site_id" smallint(6) NOT NULL,
  "name" varchar(50) NOT NULL,
  "parent_id" integer DEFAULT NULL,
  "type_id" integer DEFAULT NULL,
  "child_ids" text,
  "tag_type_ids" text,
  "code" varchar(50) NOT NULL,
  "template_path" varchar(255) DEFAULT NULL,
  "path" varchar(1000) DEFAULT NULL,
  "only_url" boolean NOT NULL,
  "has_static" boolean NOT NULL,
  "url" varchar(1000) DEFAULT NULL,
  "content_path" varchar(1000) DEFAULT NULL,
  "contain_child" boolean NOT NULL DEFAULT '1',
  "page_size" integer DEFAULT NULL,
  "allow_contribute" boolean NOT NULL,
  "sort" integer NOT NULL DEFAULT '0',
  "hidden" boolean NOT NULL,
  "disabled" boolean NOT NULL,
  "extend_id" integer DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS publiccms."cms_category_attribute" (
  "category_id" integer NOT NULL,
  "title" varchar(80) DEFAULT NULL,
  "description" varchar(300) DEFAULT NULL,
  "data" text,
  PRIMARY KEY (category_id)
);


CREATE TABLE IF NOT EXISTS publiccms."cms_category_model" (
  "category_id" integer NOT NULL,
  "model_id" varchar(20) NOT NULL,
  "template_path" varchar(200) DEFAULT NULL,
  PRIMARY KEY (category_id,model_id)
);


CREATE TABLE IF NOT EXISTS publiccms."cms_category_type" (
  "id" integer NOT NULL,
  "site_id" smallint(6) NOT NULL,
  "name" varchar(50) NOT NULL,
  "sort" integer NOT NULL,
  "extend_id" integer DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS publiccms."cms_comment" (
  "id" bigint NOT NULL,
  "site_id" smallint(6) NOT NULL,
  "user_id" bigint NOT NULL,
  "reply_id" bigint DEFAULT NULL,
  "reply_user_id" bigint DEFAULT NULL,
  "content_id" bigint NOT NULL,
  "check_user_id" bigint DEFAULT NULL,
  "check_date" timestamp DEFAULT NULL,
  "update_date" timestamp DEFAULT NULL,
  "create_date" timestamp NOT NULL,
  "status" integer NOT NULL,
  "disabled" boolean NOT NULL,
  "text" text,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS publiccms."cms_content" (
  "id" bigint NOT NULL,
  "site_id" smallint(6) NOT NULL,
  "title" varchar(255) NOT NULL,
  "user_id" bigint NOT NULL,
  "check_user_id" bigint DEFAULT NULL,
  "category_id" integer NOT NULL,
  "model_id" varchar(20) NOT NULL,
  "parent_id" bigint DEFAULT NULL,
  "quote_content_id" bigint DEFAULT NULL,
  "copied" boolean NOT NULL,
  "author" varchar(50) DEFAULT NULL,
  "editor" varchar(50) DEFAULT NULL,
  "only_url" boolean NOT NULL,
  "has_images" boolean NOT NULL,
  "has_files" boolean NOT NULL,
  "has_static" boolean NOT NULL,
  "url" varchar(1000) DEFAULT NULL,
  "description" varchar(300) DEFAULT NULL,
  "tag_ids" text,
  "dictionar_values" text,
  "cover" varchar(255) DEFAULT NULL,
  "childs" integer NOT NULL,
  "scores" integer NOT NULL,
  "comments" integer NOT NULL,
  "clicks" integer NOT NULL,
  "publish_date" timestamp NOT NULL,
  "expiry_date" timestamp DEFAULT NULL,
  "check_date" timestamp DEFAULT NULL,
  "update_date" timestamp DEFAULT NULL,
  "create_date" timestamp NOT NULL,
  "sort" integer NOT NULL DEFAULT '0',
  "status" integer NOT NULL,
  "disabled" boolean NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS publiccms."cms_content_attribute" (
  "content_id" bigint NOT NULL,
  "source" varchar(50) DEFAULT NULL,
  "source_url" varchar(1000) DEFAULT NULL,
  "data" text,
  "search_text" text,
  "text" text,
  "word_count" integer NOT NULL,
  PRIMARY KEY (content_id)
);


CREATE TABLE IF NOT EXISTS publiccms."cms_content_file" (
  "id" bigint NOT NULL,
  "content_id" bigint NOT NULL,
  "user_id" bigint NOT NULL,
  "file_path" varchar(255) NOT NULL,
  "file_type" varchar(20) NOT NULL,
  "file_size" bigint NOT NULL,
  "clicks" integer NOT NULL,
  "sort" integer NOT NULL,
  "description" varchar(300) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS publiccms."cms_content_related" (
  "id" bigint NOT NULL,
  "content_id" bigint NOT NULL,
  "related_content_id" bigint DEFAULT NULL,
  "user_id" bigint NOT NULL,
  "url" varchar(1000) DEFAULT NULL,
  "title" varchar(255) DEFAULT NULL,
  "description" varchar(300) DEFAULT NULL,
  "clicks" integer NOT NULL,
  "sort" integer NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS publiccms."cms_dictionary" (
  "id" varchar(20) NOT NULL,
  "site_id" smallint(6) NOT NULL,
  "name" varchar(100) NOT NULL,
  "multiple" boolean NOT NULL,
  PRIMARY KEY (id,site_id)
);


CREATE TABLE IF NOT EXISTS publiccms."cms_dictionary_data" (
  "dictionary_id" varchar(20) NOT NULL,
  "site_id" smallint(6) NOT NULL,
  "value" varchar(50) NOT NULL,
  "text" varchar(100) NOT NULL,
  PRIMARY KEY (dictionary_id,site_id,value)
);


CREATE TABLE IF NOT EXISTS publiccms."cms_place" (
  "id" bigint NOT NULL,
  "site_id" smallint(6) NOT NULL,
  "path" varchar(100) NOT NULL,
  "user_id" bigint DEFAULT NULL,
  "check_user_id" bigint DEFAULT NULL,
  "item_type" varchar(50) DEFAULT NULL,
  "item_id" bigint DEFAULT NULL,
  "title" varchar(255) NOT NULL,
  "url" varchar(1000) DEFAULT NULL,
  "cover" varchar(255) DEFAULT NULL,
  "create_date" timestamp NOT NULL,
  "publish_date" timestamp NOT NULL,
  "expiry_date" timestamp DEFAULT NULL,
  "status" integer NOT NULL,
  "clicks" integer NOT NULL,
  "disabled" boolean NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS publiccms."cms_place_attribute" (
  "place_id" bigint NOT NULL,
  "data" text,
  PRIMARY KEY (place_id)
);


CREATE TABLE IF NOT EXISTS publiccms."cms_tag" (
  "id" bigint NOT NULL,
  "site_id" smallint(6) NOT NULL,
  "name" varchar(50) NOT NULL,
  "type_id" integer DEFAULT NULL,
  "search_count" integer NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS publiccms."cms_tag_type" (
  "id" integer NOT NULL,
  "site_id" smallint(6) NOT NULL,
  "name" varchar(50) NOT NULL,
  "count" integer NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS publiccms."cms_word" (
  "id" bigint NOT NULL,
  "site_id" smallint(6) NOT NULL,
  "name" varchar(100) NOT NULL,
  "search_count" integer NOT NULL,
  "hidden" boolean NOT NULL,
  "create_date" timestamp NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS publiccms."log_login" (
  "id" bigint NOT NULL,
  "site_id" smallint(6) NOT NULL,
  "name" varchar(50) NOT NULL,
  "user_id" bigint DEFAULT NULL,
  "ip" varchar(64) NOT NULL,
  "channel" varchar(50) NOT NULL,
  "result" boolean NOT NULL,
  "create_date" timestamp NOT NULL,
  "error_password" varchar(100) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS publiccms."log_operate" (
  "id" bigint NOT NULL,
  "site_id" smallint(6) NOT NULL,
  "user_id" bigint DEFAULT NULL,
  "channel" varchar(50) NOT NULL,
  "operate" varchar(40) NOT NULL,
  "ip" varchar(64) DEFAULT NULL,
  "create_date" timestamp NOT NULL,
  "content" text NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS publiccms."log_task" (
  "id" bigint NOT NULL,
  "site_id" smallint(6) NOT NULL,
  "task_id" integer NOT NULL,
  "begintime" timestamp NOT NULL,
  "endtime" timestamp DEFAULT NULL,
  "success" boolean NOT NULL,
  "result" text,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS publiccms."log_upload" (
  "id" bigint NOT NULL,
  "site_id" smallint(6) NOT NULL,
  "user_id" bigint NOT NULL,
  "channel" varchar(50) NOT NULL,
  "original_name" varchar(255) DEFAULT NULL,
  "file_type" varchar(20) NOT NULL,
  "file_size" bigint NOT NULL,
  "ip" varchar(64) DEFAULT NULL,
  "create_date" timestamp NOT NULL,
  "file_path" varchar(500) NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS publiccms."sys_app" (
  "id" integer NOT NULL,
  "site_id" smallint(6) NOT NULL,
  "channel" varchar(50) NOT NULL,
  "app_key" varchar(50) NOT NULL,
  "app_secret" varchar(50) NOT NULL,
  "authorized_apis" text,
  "expiry_minutes" integer DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS publiccms."sys_app_client" (
  "id" bigint NOT NULL,
  "site_id" smallint(6) NOT NULL,
  "channel" varchar(20) NOT NULL,
  "uuid" varchar(50) NOT NULL,
  "user_id" bigint DEFAULT NULL,
  "client_version" varchar(50) DEFAULT NULL,
  "last_login_date" timestamp DEFAULT NULL,
  "last_login_ip" varchar(64) DEFAULT NULL,
  "create_date" timestamp NOT NULL,
  "disabled" boolean NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS publiccms."sys_app_token" (
  "auth_token" varchar(40) NOT NULL,
  "app_id" integer NOT NULL,
  "create_date" timestamp NOT NULL,
  "expiry_date" timestamp DEFAULT NULL,
  PRIMARY KEY (auth_token)
);


CREATE TABLE IF NOT EXISTS publiccms."sys_cluster" (
  "uuid" varchar(40) NOT NULL,
  "create_date" timestamp NOT NULL,
  "heartbeat_date" timestamp NOT NULL,
  "master" boolean NOT NULL,
  "cms_version" varchar(20) DEFAULT NULL,
  PRIMARY KEY (uuid)
);


CREATE TABLE IF NOT EXISTS publiccms."sys_config_data" (
  "site_id" smallint(6) NOT NULL,
  "code" varchar(50) NOT NULL,
  "data" text NOT NULL,
  PRIMARY KEY (site_id,code)
);


CREATE TABLE IF NOT EXISTS publiccms."sys_dept" (
  "id" integer NOT NULL,
  "site_id" smallint(6) NOT NULL,
  "name" varchar(50) NOT NULL,
  "parent_id" integer DEFAULT NULL,
  "description" varchar(300) DEFAULT NULL,
  "user_id" bigint DEFAULT NULL,
  "max_sort" integer NOT NULL DEFAULT '1000',
  "owns_all_category" boolean NOT NULL,
  "owns_all_page" boolean NOT NULL,
  "owns_all_config" boolean NOT NULL DEFAULT '1',
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS publiccms."sys_dept_category" (
  "dept_id" integer NOT NULL,
  "category_id" integer NOT NULL,
  PRIMARY KEY (dept_id,category_id)
);


CREATE TABLE IF NOT EXISTS publiccms."sys_dept_config" (
  "dept_id" integer NOT NULL,
  "config" varchar(100) NOT NULL,
  PRIMARY KEY (dept_id,config)
);


CREATE TABLE IF NOT EXISTS publiccms."sys_dept_page" (
  "dept_id" integer NOT NULL,
  "page" varchar(100) NOT NULL,
  PRIMARY KEY (dept_id,page)
);


CREATE TABLE IF NOT EXISTS publiccms."sys_domain" (
  "name" varchar(100) NOT NULL,
  "site_id" smallint(6) NOT NULL,
  "wild" boolean NOT NULL,
  "path" varchar(100) DEFAULT NULL,
  PRIMARY KEY (name)
);


CREATE TABLE IF NOT EXISTS publiccms."sys_email_token" (
  "auth_token" varchar(40) NOT NULL,
  "user_id" bigint NOT NULL,
  "email" varchar(100) NOT NULL,
  "create_date" timestamp NOT NULL,
  "expiry_date" timestamp NOT NULL,
  PRIMARY KEY (auth_token)
);


CREATE TABLE IF NOT EXISTS publiccms."sys_extend" (
  "id" integer NOT NULL,
  "item_type" varchar(20) NOT NULL,
  "item_id" integer NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS publiccms."sys_extend_field" (
  "extend_id" integer NOT NULL,
  "code" varchar(20) NOT NULL,
  "required" boolean NOT NULL,
  "searchable" boolean NOT NULL,
  "maxlength" integer DEFAULT NULL,
  "name" varchar(20) NOT NULL,
  "description" varchar(100) DEFAULT NULL,
  "input_type" varchar(20) NOT NULL,
  "default_value" varchar(50) DEFAULT NULL,
  "dictionary_id" varchar(20) DEFAULT NULL,
  "sort" integer NOT NULL DEFAULT '0',
  PRIMARY KEY (extend_id,code)
);


CREATE TABLE IF NOT EXISTS publiccms."sys_module" (
  "id" varchar(30) NOT NULL,
  "url" varchar(255) DEFAULT NULL,
  "authorized_url" text,
  "attached" varchar(50) DEFAULT NULL,
  "parent_id" varchar(30) DEFAULT NULL,
  "menu" boolean NOT NULL DEFAULT '1',
  "sort" integer NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS publiccms."sys_module_lang" (
  "module_id" varchar(30) NOT NULL,
  "lang" varchar(20) NOT NULL,
  "value" varchar(100) DEFAULT NULL,
  PRIMARY KEY (module_id,lang)
);


CREATE TABLE IF NOT EXISTS publiccms."sys_role" (
  "id" integer NOT NULL,
  "site_id" smallint(6) NOT NULL,
  "name" varchar(50) NOT NULL,
  "owns_all_right" boolean NOT NULL,
  "show_all_module" boolean NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS publiccms."sys_role_authorized" (
  "role_id" integer NOT NULL,
  "url" varchar(100) NOT NULL,
  PRIMARY KEY (role_id,url)
);


CREATE TABLE IF NOT EXISTS publiccms."sys_role_module" (
  "role_id" integer NOT NULL,
  "module_id" varchar(30) NOT NULL,
  PRIMARY KEY (role_id,module_id)
);


CREATE TABLE IF NOT EXISTS publiccms."sys_role_user" (
  "role_id" integer NOT NULL,
  "user_id" bigint NOT NULL,
  PRIMARY KEY (role_id,user_id)
);


CREATE TABLE IF NOT EXISTS publiccms."sys_site" (
  "id" smallint(6) NOT NULL,
  "parent_id" smallint(6) DEFAULT NULL,
  "name" varchar(50) NOT NULL,
  "use_static" boolean NOT NULL,
  "site_path" varchar(255) NOT NULL,
  "use_ssi" boolean NOT NULL,
  "dynamic_path" varchar(255) NOT NULL,
  "disabled" boolean NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS publiccms."sys_task" (
  "id" integer NOT NULL,
  "site_id" smallint(6) NOT NULL,
  "name" varchar(50) NOT NULL,
  "status" integer NOT NULL,
  "cron_expression" varchar(50) NOT NULL,
  "description" varchar(300) DEFAULT NULL,
  "file_path" varchar(255) DEFAULT NULL,
  "update_date" timestamp DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS publiccms."sys_user" (
  "id" bigint NOT NULL,
  "site_id" smallint(6) NOT NULL,
  "name" varchar(50) NOT NULL,
  "password" varchar(128) NOT NULL,
  "salt" varchar(20) DEFAULT NULL,
  "weak_password" boolean NOT NULL DEFAULT '0',
  "nick_name" varchar(45) NOT NULL,
  "dept_id" integer DEFAULT NULL,
  "owns_all_content" boolean NOT NULL DEFAULT '1',
  "roles" text,
  "email" varchar(100) DEFAULT NULL,
  "email_checked" boolean NOT NULL,
  "superuser_access" boolean NOT NULL,
  "disabled" boolean NOT NULL,
  "last_login_date" timestamp DEFAULT NULL,
  "last_login_ip" varchar(64) DEFAULT NULL,
  "login_count" integer NOT NULL,
  "registered_date" timestamp DEFAULT NULL,
  PRIMARY KEY (id),
#   UNIQUE KEY nick_name (nick_name,site_id)
);


CREATE TABLE IF NOT EXISTS publiccms."sys_user_token" (
  "auth_token" varchar(40) NOT NULL,
  "site_id" smallint(6) NOT NULL,
  "user_id" bigint NOT NULL,
  "channel" varchar(50) NOT NULL,
  "create_date" timestamp NOT NULL,
  "expiry_date" timestamp DEFAULT NULL,
  "login_ip" varchar(64) NOT NULL,
  PRIMARY KEY (auth_token)
);

