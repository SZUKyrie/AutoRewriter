


CREATE TABLE IF NOT EXISTS halo."attachments" (
  "id" integer NOT NULL,
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" smallint(4) DEFAULT '0',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "file_key" varchar(2047) DEFAULT '',
  "height" integer DEFAULT '0',
  "media_type" varchar(50) NOT NULL,
  "name" varchar(255) NOT NULL,
  "path" varchar(1023) NOT NULL,
  "size" bigint NOT NULL,
  "suffix" varchar(50) DEFAULT '',
  "thumb_path" varchar(1023) DEFAULT '',
  "type" integer DEFAULT '0',
  "width" integer DEFAULT '0',
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS halo."categories" (
  "id" integer NOT NULL,
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" smallint(4) DEFAULT '0',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "description" varchar(100) DEFAULT '',
  "name" varchar(50) NOT NULL,
  "parent_id" integer DEFAULT '0',
  "slug_name" varchar(50) NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS halo."comments" (
  "type" integer NOT NULL DEFAULT '0',
  "id" bigint NOT NULL,
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" smallint(4) DEFAULT '0',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "author" varchar(50) NOT NULL,
  "author_url" varchar(512) DEFAULT '',
  "content" varchar(1023) NOT NULL,
  "email" varchar(255) NOT NULL,
  "gravatar_md5" varchar(128) DEFAULT '',
  "ip_address" varchar(127) DEFAULT '',
  "is_admin" smallint(4) DEFAULT '0',
  "parent_id" bigint DEFAULT '0',
  "post_id" integer NOT NULL,
  "status" integer DEFAULT '1',
  "top_priority" integer DEFAULT '0',
  "user_agent" varchar(512) DEFAULT '',
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS halo."journals" (
  "id" integer NOT NULL,
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" smallint(4) DEFAULT '0',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "content" varchar(1023) NOT NULL,
  "likes" bigint DEFAULT '0',
  "type" integer DEFAULT '1',
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS halo."links" (
  "id" integer NOT NULL,
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" smallint(4) DEFAULT '0',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "description" varchar(255) DEFAULT '',
  "logo" varchar(1023) DEFAULT '',
  "name" varchar(255) NOT NULL,
  "priority" integer DEFAULT '0',
  "team" varchar(255) DEFAULT '',
  "url" varchar(1023) NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS halo."logs" (
  "id" bigint NOT NULL,
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" smallint(4) DEFAULT '0',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "content" varchar(1023) NOT NULL,
  "ip_address" varchar(127) DEFAULT '',
  "log_key" varchar(1023) DEFAULT '',
  "type" integer NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS halo."menus" (
  "id" integer NOT NULL,
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" smallint(4) DEFAULT '0',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "icon" varchar(50) DEFAULT '',
  "name" varchar(50) NOT NULL,
  "parent_id" integer DEFAULT '0',
  "priority" integer DEFAULT '0',
  "target" varchar(20) DEFAULT '_self',
  "team" varchar(255) DEFAULT '',
  "url" varchar(1023) NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS halo."metas" (
  "type" integer NOT NULL DEFAULT '0',
  "id" bigint NOT NULL,
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" smallint(4) DEFAULT '0',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "meta_key" varchar(100) NOT NULL,
  "post_id" integer NOT NULL,
  "meta_value" varchar(1023) NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS halo."options" (
  "id" integer NOT NULL,
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" smallint(4) DEFAULT '0',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "option_key" varchar(100) NOT NULL,
  "option_value" varchar(1023) NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS halo."photos" (
  "id" integer NOT NULL,
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" smallint(4) DEFAULT '0',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "description" varchar(255) DEFAULT '',
  "location" varchar(255) DEFAULT '',
  "name" varchar(255) NOT NULL,
  "take_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "team" varchar(255) DEFAULT '',
  "thumbnail" varchar(1023) DEFAULT '',
  "url" varchar(1023) NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS halo."post_categories" (
  "id" integer NOT NULL,
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" smallint(4) DEFAULT '0',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "category_id" integer DEFAULT NULL,
  "post_id" integer DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS halo."post_tags" (
  "id" integer NOT NULL,
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" smallint(4) DEFAULT '0',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "post_id" integer NOT NULL,
  "tag_id" integer NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS halo."posts" (
  "type" integer NOT NULL DEFAULT '0',
  "id" integer NOT NULL,
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" smallint(4) DEFAULT '0',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "create_from" integer DEFAULT '0',
  "disallow_comment" integer DEFAULT '0',
  "edit_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "format_content" text NOT NULL,
  "likes" bigint DEFAULT '0',
  "original_content" text NOT NULL,
  "password" varchar(255) DEFAULT '',
  "status" integer DEFAULT '1',
  "summary" varchar(500) DEFAULT '',
  "template" varchar(255) DEFAULT '',
  "thumbnail" varchar(1023) DEFAULT '',
  "title" varchar(100) NOT NULL,
  "top_priority" integer DEFAULT '0',
  "url" varchar(255) NOT NULL,
  "visits" bigint DEFAULT '0',
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS halo."tags" (
  "id" integer NOT NULL,
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" smallint(4) DEFAULT '0',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "name" varchar(255) NOT NULL,
  "slug_name" varchar(255) NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS halo."theme_settings" (
  "id" integer NOT NULL,
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" smallint(4) DEFAULT '0',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "setting_key" varchar(255) NOT NULL,
  "theme_id" varchar(255) NOT NULL,
  "setting_value" varchar(10239) NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS halo."users" (
  "id" integer NOT NULL,
  "create_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "deleted" smallint(4) DEFAULT '0',
  "update_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "avatar" varchar(1023) DEFAULT '',
  "description" varchar(1023) DEFAULT '',
  "email" varchar(127) DEFAULT '',
  "expire_time" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "nickname" varchar(255) NOT NULL,
  "password" varchar(255) NOT NULL,
  "username" varchar(50) NOT NULL,
  PRIMARY KEY (id)
);

