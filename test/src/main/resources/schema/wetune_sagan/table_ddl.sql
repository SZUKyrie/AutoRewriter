


CREATE TABLE IF NOT EXISTS sagan."member_profile" (
  "id" bigint NOT NULL,
  "avatar_url" varchar(255) DEFAULT NULL,
  "bio" varchar(255) DEFAULT NULL,
  "latitude" double precision DEFAULT NULL,
  "longitude" double precision DEFAULT NULL,
  "github_id" bigint DEFAULT NULL,
  "github_username" varchar(255) DEFAULT NULL,
  "gravatar_email" varchar(255) DEFAULT NULL,
  "hidden" boolean DEFAULT NULL,
  "lanyrd_username" varchar(255) DEFAULT NULL,
  "location" varchar(255) DEFAULT NULL,
  "name" varchar(255) DEFAULT NULL,
  "speakerdeck_username" varchar(255) DEFAULT NULL,
  "twitter_username" varchar(255) DEFAULT NULL,
  "username" varchar(255) NOT NULL,
  "video_embeds" varchar(255) DEFAULT NULL,
  "job_title" varchar(255) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS sagan."post" (
  "id" bigint NOT NULL,
  "broadcast" boolean NOT NULL,
  "category" varchar(255) NOT NULL,
  "created_at" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "draft" boolean NOT NULL,
  "format" varchar(255) DEFAULT NULL,
  "public_slug" varchar(255) DEFAULT NULL,
  "publish_at" timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  "raw_content" varchar(255) NOT NULL,
  "rendered_content" varchar(255) NOT NULL,
  "rendered_summary" varchar(255) NOT NULL,
  "title" varchar(255) NOT NULL,
  "author_id" integer NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS sagan."post_public_slug_aliases" (
  "post_id" integer NOT NULL,
  "public_slug_aliases" varchar(255) NOT NULL,
  PRIMARY KEY (post_id,public_slug_aliases)
);


CREATE TABLE IF NOT EXISTS sagan."project" (
  "id" varchar(255) NOT NULL,
  "name" varchar(255) DEFAULT NULL,
  "repo_url" varchar(255) DEFAULT NULL,
  "category" varchar(255) DEFAULT NULL,
  "site_url" varchar(255) DEFAULT NULL,
  "stack_overflow_tags" varchar(255) DEFAULT NULL,
  "raw_boot_config" varchar(255) DEFAULT NULL,
  "rendered_boot_config" varchar(255) DEFAULT NULL,
  "raw_overview" varchar(255) DEFAULT '',
  "rendered_overview" varchar(255) DEFAULT '',
  "parent_project_id" varchar(255) DEFAULT NULL,
  "display_order" integer NOT NULL DEFAULT '255',
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS sagan."project_release_list" (
  "project_id" varchar(255) NOT NULL,
  "repository_id" varchar(255) DEFAULT NULL,
  "api_doc_url" varchar(255) DEFAULT NULL,
  "artifact_id" varchar(255) DEFAULT NULL,
  "group_id" varchar(255) DEFAULT NULL,
  "is_current" boolean DEFAULT NULL,
  "ref_doc_url" varchar(255) DEFAULT NULL,
  "release_status" integer DEFAULT NULL,
  "version_name" varchar(255) NOT NULL,
  PRIMARY KEY (project_id,version_name)
);


CREATE TABLE IF NOT EXISTS sagan."project_repository" (
  "id" varchar(255) NOT NULL,
  "name" varchar(255) DEFAULT NULL,
  "url" varchar(255) DEFAULT NULL,
  "snapshots_enabled" boolean DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS sagan."project_sample_list" (
  "title" varchar(255) DEFAULT NULL,
  "description" varchar(255) DEFAULT NULL,
  "url" varchar(255) DEFAULT NULL,
  "display_order" integer NOT NULL,
  "project_id" varchar(255) NOT NULL,
  PRIMARY KEY (project_id,display_order)
);


CREATE TABLE IF NOT EXISTS sagan."schema_version" (
  "version_rank" integer NOT NULL,
  "installed_rank" integer NOT NULL,
  "version" varchar(50) NOT NULL,
  "description" varchar(200) NOT NULL,
  "type" varchar(20) NOT NULL,
  "script" varchar(1000) NOT NULL,
  "checksum" integer DEFAULT NULL,
  "installed_by" varchar(100) NOT NULL,
  "installed_on" timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  "execution_time" integer NOT NULL,
  "success" boolean NOT NULL,
  PRIMARY KEY (version)
);


CREATE TABLE IF NOT EXISTS sagan."spring_tools_platform" (
  "id" varchar(255) NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS sagan."spring_tools_platform_downloads" (
  "spring_tools_platform_id" varchar(255) NOT NULL,
  "download_url" varchar(255) NOT NULL,
  "variant" varchar(255) NOT NULL,
  "label" varchar(255) NOT NULL,
  PRIMARY KEY (spring_tools_platform_id,variant)
);

