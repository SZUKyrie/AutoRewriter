


CREATE TABLE IF NOT EXISTS refinerycms."ar_internal_metadata" (
  "value" varchar(255) DEFAULT NULL,
  "created_at" timestamp(6) NOT NULL,
  "updated_at" timestamp(6) NOT NULL,
  PRIMARY KEY (key)
);


CREATE TABLE IF NOT EXISTS refinerycms."refinery_crud_dummies" (
  "id" bigint NOT NULL,
  "parent_id" int DEFAULT NULL,
  "lft" int DEFAULT NULL,
  "rgt" int DEFAULT NULL,
  "depth" int DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS refinerycms."refinery_image_translations" (
  "id" int NOT NULL,
  "image_alt" varchar(255) DEFAULT NULL,
  "image_title" varchar(255) DEFAULT NULL,
  "locale" varchar(255) NOT NULL,
  "refinery_image_id" int NOT NULL,
  "created_at" timestamp NOT NULL,
  "updated_at" timestamp NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS refinerycms."refinery_images" (
  "id" int NOT NULL,
  "image_mime_type" varchar(255) DEFAULT NULL,
  "image_name" varchar(255) DEFAULT NULL,
  "image_size" int DEFAULT NULL,
  "image_width" int DEFAULT NULL,
  "image_height" int DEFAULT NULL,
  "image_uid" varchar(255) DEFAULT NULL,
  "created_at" timestamp DEFAULT NULL,
  "updated_at" timestamp DEFAULT NULL,
  "parent_id" int DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS refinerycms."refinery_page_part_translations" (
  "id" int NOT NULL,
  "body" text,
  "locale" varchar(255) NOT NULL,
  "refinery_page_part_id" int NOT NULL,
  "created_at" timestamp NOT NULL,
  "updated_at" timestamp NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS refinerycms."refinery_page_parts" (
  "id" int NOT NULL,
  "refinery_page_id" int DEFAULT NULL,
  "slug" varchar(255) DEFAULT NULL,
  "position" int DEFAULT NULL,
  "created_at" timestamp DEFAULT NULL,
  "updated_at" timestamp DEFAULT NULL,
  "title" varchar(255) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS refinerycms."refinery_page_translations" (
  "id" int NOT NULL,
  "title" varchar(255) DEFAULT NULL,
  "custom_slug" varchar(255) DEFAULT NULL,
  "menu_title" varchar(255) DEFAULT NULL,
  "slug" varchar(255) DEFAULT NULL,
  "locale" varchar(255) NOT NULL,
  "refinery_page_id" int NOT NULL,
  "created_at" timestamp NOT NULL,
  "updated_at" timestamp NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS refinerycms."refinery_pages" (
  "id" int NOT NULL,
  "parent_id" int DEFAULT NULL,
  "path" varchar(255) DEFAULT NULL,
  "show_in_menu" boolean DEFAULT '1',
  "link_url" varchar(255) DEFAULT NULL,
  "menu_match" varchar(255) DEFAULT NULL,
  "deletable" boolean DEFAULT '1',
  "draft" boolean DEFAULT '0',
  "skip_to_first_child" boolean DEFAULT '0',
  "lft" int DEFAULT NULL,
  "rgt" int DEFAULT NULL,
  "depth" int DEFAULT NULL,
  "view_template" varchar(255) DEFAULT NULL,
  "layout_template" varchar(255) DEFAULT NULL,
  "created_at" timestamp DEFAULT NULL,
  "updated_at" timestamp DEFAULT NULL,
  "children_count" int NOT NULL DEFAULT '0',
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS refinerycms."refinery_resource_translations" (
  "id" int NOT NULL,
  "resource_title" varchar(255) DEFAULT NULL,
  "locale" varchar(255) NOT NULL,
  "refinery_resource_id" int NOT NULL,
  "created_at" timestamp NOT NULL,
  "updated_at" timestamp NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS refinerycms."refinery_resources" (
  "id" int NOT NULL,
  "file_mime_type" varchar(255) DEFAULT NULL,
  "file_name" varchar(255) DEFAULT NULL,
  "file_size" int DEFAULT NULL,
  "file_uid" varchar(255) DEFAULT NULL,
  "file_ext" varchar(255) DEFAULT NULL,
  "created_at" timestamp DEFAULT NULL,
  "updated_at" timestamp DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS refinerycms."schema_migrations" (
  "version" varchar(255) NOT NULL,
  PRIMARY KEY (version)
);


CREATE TABLE IF NOT EXISTS refinerycms."seo_meta" (
  "id" int NOT NULL,
  "seo_meta_id" int DEFAULT NULL,
  "seo_meta_type" varchar(255) DEFAULT NULL,
  "browser_title" varchar(255) DEFAULT NULL,
  "meta_description" text,
  "created_at" timestamp NOT NULL,
  "updated_at" timestamp NOT NULL,
  PRIMARY KEY (id)
);

