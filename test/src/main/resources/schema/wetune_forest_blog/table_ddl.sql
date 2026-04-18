


CREATE TABLE IF NOT EXISTS forest_blog."article" (
  "article_id" integer NOT NULL,
  "article_user_id" integer DEFAULT NULL,
  "article_title" varchar(255) DEFAULT NULL,
  "article_content" text,
  "article_view_count" integer DEFAULT '0',
  "article_comment_count" integer DEFAULT '0',
  "article_like_count" integer DEFAULT '0',
  "article_is_comment" integer DEFAULT NULL,
  "article_status" integer DEFAULT '1',
  "article_order" integer DEFAULT NULL,
  "article_update_time" timestamp DEFAULT NULL,
  "article_create_time" timestamp DEFAULT NULL,
  "article_summary" text,
  PRIMARY KEY (article_id)
);


CREATE TABLE IF NOT EXISTS forest_blog."article_category_ref" (
  "article_id" integer DEFAULT NULL,
  "category_id" integer DEFAULT NULL
);


CREATE TABLE IF NOT EXISTS forest_blog."article_tag_ref" (
  "article_id" integer NOT NULL,
  "tag_id" integer NOT NULL,
  PRIMARY KEY (article_id,tag_id)
);


CREATE TABLE IF NOT EXISTS forest_blog."category" (
  "category_id" integer NOT NULL,
  "category_pid" integer DEFAULT NULL,
  "category_name" varchar(50) DEFAULT NULL,
  "category_description" varchar(255) DEFAULT NULL,
  "category_order" integer DEFAULT '1',
  "category_icon" varchar(20) DEFAULT NULL,
  PRIMARY KEY (category_id)
);


CREATE TABLE IF NOT EXISTS forest_blog."comment" (
  "comment_id" integer NOT NULL,
  "comment_pid" integer DEFAULT '0',
  "comment_pname" varchar(255) DEFAULT NULL,
  "comment_article_id" integer DEFAULT NULL,
  "comment_author_name" varchar(50) DEFAULT NULL,
  "comment_author_email" varchar(50) DEFAULT NULL,
  "comment_author_url" varchar(50) DEFAULT NULL,
  "comment_author_avatar" varchar(100) DEFAULT NULL,
  "comment_content" varchar(1000) DEFAULT NULL,
  "comment_agent" varchar(200) DEFAULT NULL,
  "comment_ip" varchar(50) DEFAULT NULL,
  "comment_create_time" timestamp DEFAULT NULL,
  "comment_role" integer DEFAULT NULL,
  PRIMARY KEY (comment_id)
);


CREATE TABLE IF NOT EXISTS forest_blog."link" (
  "link_id" integer NOT NULL,
  "link_url" varchar(255) DEFAULT NULL,
  "link_name" varchar(255) DEFAULT NULL,
  "link_image" varchar(255) DEFAULT NULL,
  "link_description" varchar(255) DEFAULT NULL,
  "link_owner_nickname" varchar(40) DEFAULT NULL,
  "link_owner_contact" varchar(255) DEFAULT NULL,
  "link_update_time" timestamp DEFAULT NULL,
  "link_create_time" timestamp DEFAULT NULL,
  "link_order" integer DEFAULT '1',
  "link_status" integer DEFAULT '1',
  PRIMARY KEY (link_id)
);


CREATE TABLE IF NOT EXISTS forest_blog."menu" (
  "menu_id" integer NOT NULL,
  "menu_name" varchar(255) DEFAULT NULL,
  "menu_url" varchar(255) DEFAULT NULL,
  "menu_level" integer DEFAULT NULL,
  "menu_icon" varchar(255) DEFAULT NULL,
  "menu_order" integer DEFAULT NULL,
  PRIMARY KEY (menu_id)
);


CREATE TABLE IF NOT EXISTS forest_blog."notice" (
  "notice_id" integer NOT NULL,
  "notice_title" varchar(255) DEFAULT NULL,
  "notice_content" varchar(10000) DEFAULT NULL,
  "notice_create_time" timestamp DEFAULT NULL,
  "notice_update_time" timestamp DEFAULT NULL,
  "notice_status" integer DEFAULT '1',
  "notice_order" integer DEFAULT NULL,
  PRIMARY KEY (notice_id)
);


CREATE TABLE IF NOT EXISTS forest_blog."options" (
  "option_id" integer NOT NULL,
  "option_site_title" varchar(255) DEFAULT NULL,
  "option_site_descrption" varchar(255) DEFAULT NULL,
  "option_meta_descrption" varchar(255) DEFAULT NULL,
  "option_meta_keyword" varchar(255) DEFAULT NULL,
  "option_aboutsite_avatar" varchar(255) DEFAULT NULL,
  "option_aboutsite_title" varchar(255) DEFAULT NULL,
  "option_aboutsite_content" varchar(255) DEFAULT NULL,
  "option_aboutsite_wechat" varchar(255) DEFAULT NULL,
  "option_aboutsite_qq" varchar(255) DEFAULT NULL,
  "option_aboutsite_github" varchar(255) DEFAULT NULL,
  "option_aboutsite_weibo" varchar(255) DEFAULT NULL,
  "option_tongji" varchar(255) DEFAULT NULL,
  "option_status" integer DEFAULT '1',
  PRIMARY KEY (option_id)
);


CREATE TABLE IF NOT EXISTS forest_blog."page" (
  "page_id" integer NOT NULL,
  "page_key" varchar(50) DEFAULT NULL,
  "page_title" varchar(50) DEFAULT NULL,
  "page_content" text,
  "page_create_time" timestamp DEFAULT NULL,
  "page_update_time" timestamp DEFAULT NULL,
  "page_view_count" integer DEFAULT '0',
  "page_comment_count" integer DEFAULT '0',
  "page_status" integer DEFAULT '1',
  PRIMARY KEY (page_id)
);


CREATE TABLE IF NOT EXISTS forest_blog."tag" (
  "tag_id" integer NOT NULL,
  "tag_name" varchar(50) DEFAULT NULL,
  "tag_description" varchar(255) DEFAULT NULL,
  PRIMARY KEY (tag_id)
);


CREATE TABLE IF NOT EXISTS forest_blog."user" (
  "user_id" integer NOT NULL,
  "user_name" varchar(255) NOT NULL DEFAULT '',
  "user_pass" varchar(255) NOT NULL DEFAULT '',
  "user_nickname" varchar(255) NOT NULL DEFAULT '',
  "user_email" varchar(100) DEFAULT '',
  "user_url" varchar(100) DEFAULT '',
  "user_avatar" varchar(255) DEFAULT NULL,
  "user_last_login_ip" varchar(255) DEFAULT NULL,
  "user_register_time" timestamp DEFAULT NULL,
  "user_last_login_time" timestamp DEFAULT NULL,
  "user_status" integer DEFAULT '1',
  PRIMARY KEY (user_id)
);

