


CREATE TABLE IF NOT EXISTS redmine."ar_internal_metadata" (
  "value" varchar(255) DEFAULT NULL,
  "created_at" timestamp NOT NULL,
  "updated_at" timestamp NOT NULL,
  PRIMARY KEY (key)
);


CREATE TABLE IF NOT EXISTS redmine."attachments" (
  "id" integer NOT NULL,
  "container_id" integer DEFAULT NULL,
  "container_type" varchar(30) DEFAULT NULL,
  "filename" varchar(255) NOT NULL DEFAULT '',
  "disk_filename" varchar(255) NOT NULL DEFAULT '',
  "filesize" bigint NOT NULL DEFAULT '0',
  "content_type" varchar(255) DEFAULT '',
  "digest" varchar(64) NOT NULL DEFAULT '',
  "downloads" integer NOT NULL DEFAULT '0',
  "author_id" integer NOT NULL DEFAULT '0',
  "created_on" timestamp NULL DEFAULT NULL,
  "description" varchar(255) DEFAULT NULL,
  "disk_directory" varchar(255) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS redmine."auth_sources" (
  "id" integer NOT NULL,
  "type" varchar(30) NOT NULL DEFAULT '',
  "name" varchar(60) NOT NULL DEFAULT '',
  "host" varchar(60) DEFAULT NULL,
  "port" integer DEFAULT NULL,
  "account" varchar(255) DEFAULT NULL,
  "account_password" varchar(255) DEFAULT '',
  "base_dn" varchar(255) DEFAULT NULL,
  "attr_login" varchar(30) DEFAULT NULL,
  "attr_firstname" varchar(30) DEFAULT NULL,
  "attr_lastname" varchar(30) DEFAULT NULL,
  "attr_mail" varchar(30) DEFAULT NULL,
  "onthefly_register" boolean NOT NULL DEFAULT '0',
  "tls" boolean NOT NULL DEFAULT '0',
  "filter" text,
  "timeout" integer DEFAULT NULL,
  "verify_peer" boolean NOT NULL DEFAULT '1',
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS redmine."boards" (
  "id" integer NOT NULL,
  "project_id" integer NOT NULL,
  "name" varchar(255) NOT NULL DEFAULT '',
  "description" varchar(255) DEFAULT NULL,
  "position" integer DEFAULT NULL,
  "topics_count" integer NOT NULL DEFAULT '0',
  "messages_count" integer NOT NULL DEFAULT '0',
  "last_message_id" integer DEFAULT NULL,
  "parent_id" integer DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS redmine."changes" (
  "id" integer NOT NULL,
  "changeset_id" integer NOT NULL,
  "action" varchar(1) NOT NULL DEFAULT '',
  "path" text NOT NULL,
  "from_path" text,
  "from_revision" varchar(255) DEFAULT NULL,
  "revision" varchar(255) DEFAULT NULL,
  "branch" varchar(255) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS redmine."changeset_parents" (
  "changeset_id" integer NOT NULL,
  "parent_id" integer NOT NULL
);


CREATE TABLE IF NOT EXISTS redmine."changesets" (
  "id" integer NOT NULL,
  "repository_id" integer NOT NULL,
  "revision" varchar(255) NOT NULL,
  "committer" varchar(255) DEFAULT NULL,
  "committed_on" timestamp NOT NULL,
  "comments" text,
  "commit_date" date DEFAULT NULL,
  "scmid" varchar(255) DEFAULT NULL,
  "user_id" integer DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS redmine."changesets_issues" (
  "changeset_id" integer NOT NULL,
  "issue_id" integer NOT NULL
);


CREATE TABLE IF NOT EXISTS redmine."comments" (
  "id" integer NOT NULL,
  "commented_type" varchar(30) NOT NULL DEFAULT '',
  "commented_id" integer NOT NULL DEFAULT '0',
  "author_id" integer NOT NULL DEFAULT '0',
  "content" text,
  "created_on" timestamp NOT NULL,
  "updated_on" timestamp NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS redmine."custom_field_enumerations" (
  "id" integer NOT NULL,
  "custom_field_id" integer NOT NULL,
  "name" varchar(255) NOT NULL,
  "active" boolean NOT NULL DEFAULT '1',
  "position" integer NOT NULL DEFAULT '1',
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS redmine."custom_fields" (
  "id" integer NOT NULL,
  "type" varchar(30) NOT NULL DEFAULT '',
  "name" varchar(30) NOT NULL DEFAULT '',
  "field_format" varchar(30) NOT NULL DEFAULT '',
  "possible_values" text,
  "regexp" varchar(255) DEFAULT '',
  "min_length" integer DEFAULT NULL,
  "max_length" integer DEFAULT NULL,
  "is_required" boolean NOT NULL DEFAULT '0',
  "is_for_all" boolean NOT NULL DEFAULT '0',
  "is_filter" boolean NOT NULL DEFAULT '0',
  "position" integer DEFAULT NULL,
  "searchable" boolean DEFAULT '0',
  "default_value" text,
  "editable" boolean DEFAULT '1',
  "visible" boolean NOT NULL DEFAULT '1',
  "multiple" boolean DEFAULT '0',
  "format_store" text,
  "description" text,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS redmine."custom_fields_projects" (
  "custom_field_id" integer NOT NULL DEFAULT '0',
  "project_id" integer NOT NULL DEFAULT '0'
);


CREATE TABLE IF NOT EXISTS redmine."custom_fields_roles" (
  "custom_field_id" integer NOT NULL,
  "role_id" integer NOT NULL
);


CREATE TABLE IF NOT EXISTS redmine."custom_fields_trackers" (
  "custom_field_id" integer NOT NULL DEFAULT '0',
  "tracker_id" integer NOT NULL DEFAULT '0'
);


CREATE TABLE IF NOT EXISTS redmine."custom_values" (
  "id" integer NOT NULL,
  "customized_type" varchar(30) NOT NULL DEFAULT '',
  "customized_id" integer NOT NULL DEFAULT '0',
  "custom_field_id" integer NOT NULL DEFAULT '0',
  "value" text,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS redmine."documents" (
  "id" integer NOT NULL,
  "project_id" integer NOT NULL DEFAULT '0',
  "category_id" integer NOT NULL DEFAULT '0',
  "title" varchar(255) NOT NULL DEFAULT '',
  "description" text,
  "created_on" timestamp NULL DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS redmine."email_addresses" (
  "id" integer NOT NULL,
  "user_id" integer NOT NULL,
  "address" varchar(255) NOT NULL,
  "is_default" boolean NOT NULL DEFAULT '0',
  "notify" boolean NOT NULL DEFAULT '1',
  "created_on" timestamp NOT NULL,
  "updated_on" timestamp NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS redmine."enabled_modules" (
  "id" integer NOT NULL,
  "project_id" integer DEFAULT NULL,
  "name" varchar(255) NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS redmine."enumerations" (
  "id" integer NOT NULL,
  "name" varchar(30) NOT NULL DEFAULT '',
  "position" integer DEFAULT NULL,
  "is_default" boolean NOT NULL DEFAULT '0',
  "type" varchar(255) DEFAULT NULL,
  "active" boolean NOT NULL DEFAULT '1',
  "project_id" integer DEFAULT NULL,
  "parent_id" integer DEFAULT NULL,
  "position_name" varchar(30) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS redmine."groups_users" (
  "group_id" integer NOT NULL,
  "user_id" integer NOT NULL
);


CREATE TABLE IF NOT EXISTS redmine."import_items" (
  "id" integer NOT NULL,
  "import_id" integer NOT NULL,
  "position" integer NOT NULL,
  "obj_id" integer DEFAULT NULL,
  "message" text,
  "unique_id" varchar(255) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS redmine."imports" (
  "id" integer NOT NULL,
  "type" varchar(255) DEFAULT NULL,
  "user_id" integer NOT NULL,
  "filename" varchar(255) DEFAULT NULL,
  "settings" text,
  "total_items" integer DEFAULT NULL,
  "finished" boolean NOT NULL DEFAULT '0',
  "created_at" timestamp NOT NULL,
  "updated_at" timestamp NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS redmine."issue_categories" (
  "id" integer NOT NULL,
  "project_id" integer NOT NULL DEFAULT '0',
  "name" varchar(60) NOT NULL DEFAULT '',
  "assigned_to_id" integer DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS redmine."issue_relations" (
  "id" integer NOT NULL,
  "issue_from_id" integer NOT NULL,
  "issue_to_id" integer NOT NULL,
  "relation_type" varchar(255) NOT NULL DEFAULT '',
  "delay" integer DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS redmine."issue_statuses" (
  "id" integer NOT NULL,
  "name" varchar(30) NOT NULL DEFAULT '',
  "is_closed" boolean NOT NULL DEFAULT '0',
  "position" integer DEFAULT NULL,
  "default_done_ratio" integer DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS redmine."issues" (
  "id" integer NOT NULL,
  "tracker_id" integer NOT NULL,
  "project_id" integer NOT NULL,
  "subject" varchar(255) NOT NULL DEFAULT '',
  "description" text,
  "due_date" date DEFAULT NULL,
  "category_id" integer DEFAULT NULL,
  "status_id" integer NOT NULL,
  "assigned_to_id" integer DEFAULT NULL,
  "priority_id" integer NOT NULL,
  "fixed_version_id" integer DEFAULT NULL,
  "author_id" integer NOT NULL,
  "lock_version" integer NOT NULL DEFAULT '0',
  "created_on" timestamp NULL DEFAULT NULL,
  "updated_on" timestamp NULL DEFAULT NULL,
  "start_date" date DEFAULT NULL,
  "done_ratio" integer NOT NULL DEFAULT '0',
  "estimated_hours" float DEFAULT NULL,
  "parent_id" integer DEFAULT NULL,
  "root_id" integer DEFAULT NULL,
  "lft" integer DEFAULT NULL,
  "rgt" integer DEFAULT NULL,
  "is_private" boolean NOT NULL DEFAULT '0',
  "closed_on" timestamp DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS redmine."journal_details" (
  "id" integer NOT NULL,
  "journal_id" integer NOT NULL DEFAULT '0',
  "property" varchar(30) NOT NULL DEFAULT '',
  "prop_key" varchar(30) NOT NULL DEFAULT '',
  "old_value" text,
  "value" text,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS redmine."journals" (
  "id" integer NOT NULL,
  "journalized_id" integer NOT NULL DEFAULT '0',
  "journalized_type" varchar(30) NOT NULL DEFAULT '',
  "user_id" integer NOT NULL DEFAULT '0',
  "notes" text,
  "created_on" timestamp NOT NULL,
  "private_notes" boolean NOT NULL DEFAULT '0',
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS redmine."member_roles" (
  "id" integer NOT NULL,
  "member_id" integer NOT NULL,
  "role_id" integer NOT NULL,
  "inherited_from" integer DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS redmine."members" (
  "id" integer NOT NULL,
  "user_id" integer NOT NULL DEFAULT '0',
  "project_id" integer NOT NULL DEFAULT '0',
  "created_on" timestamp NULL DEFAULT NULL,
  "mail_notification" boolean NOT NULL DEFAULT '0',
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS redmine."messages" (
  "id" integer NOT NULL,
  "board_id" integer NOT NULL,
  "parent_id" integer DEFAULT NULL,
  "subject" varchar(255) NOT NULL DEFAULT '',
  "content" text,
  "author_id" integer DEFAULT NULL,
  "replies_count" integer NOT NULL DEFAULT '0',
  "last_reply_id" integer DEFAULT NULL,
  "created_on" timestamp NOT NULL,
  "updated_on" timestamp NOT NULL,
  "locked" boolean DEFAULT '0',
  "sticky" integer DEFAULT '0',
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS redmine."news" (
  "id" integer NOT NULL,
  "project_id" integer DEFAULT NULL,
  "title" varchar(60) NOT NULL DEFAULT '',
  "summary" varchar(255) DEFAULT '',
  "description" text,
  "author_id" integer NOT NULL DEFAULT '0',
  "created_on" timestamp NULL DEFAULT NULL,
  "comments_count" integer NOT NULL DEFAULT '0',
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS redmine."open_id_authentication_associations" (
  "id" integer NOT NULL,
  "issued" integer DEFAULT NULL,
  "lifetime" integer DEFAULT NULL,
  "handle" varchar(255) DEFAULT NULL,
  "assoc_type" varchar(255) DEFAULT NULL,
  "server_url" bytea,
  "secret" bytea,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS redmine."open_id_authentication_nonces" (
  "id" integer NOT NULL,
  "timestamp" integer NOT NULL,
  "server_url" varchar(255) DEFAULT NULL,
  "salt" varchar(255) NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS redmine."projects" (
  "id" integer NOT NULL,
  "name" varchar(255) NOT NULL DEFAULT '',
  "description" text,
  "homepage" varchar(255) DEFAULT '',
  "is_public" boolean NOT NULL DEFAULT '1',
  "parent_id" integer DEFAULT NULL,
  "created_on" timestamp NULL DEFAULT NULL,
  "updated_on" timestamp NULL DEFAULT NULL,
  "identifier" varchar(255) DEFAULT NULL,
  "status" integer NOT NULL DEFAULT '1',
  "lft" integer DEFAULT NULL,
  "rgt" integer DEFAULT NULL,
  "inherit_members" boolean NOT NULL DEFAULT '0',
  "default_version_id" integer DEFAULT NULL,
  "default_assigned_to_id" integer DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS redmine."projects_trackers" (
  "project_id" integer NOT NULL DEFAULT '0',
  "tracker_id" integer NOT NULL DEFAULT '0'
);


CREATE TABLE IF NOT EXISTS redmine."queries" (
  "id" integer NOT NULL,
  "project_id" integer DEFAULT NULL,
  "name" varchar(255) NOT NULL DEFAULT '',
  "filters" text,
  "user_id" integer NOT NULL DEFAULT '0',
  "column_names" text,
  "sort_criteria" text,
  "group_by" varchar(255) DEFAULT NULL,
  "type" varchar(255) DEFAULT NULL,
  "visibility" integer DEFAULT '0',
  "options" text,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS redmine."queries_roles" (
  "query_id" integer NOT NULL,
  "role_id" integer NOT NULL
);


CREATE TABLE IF NOT EXISTS redmine."repositories" (
  "id" integer NOT NULL,
  "project_id" integer NOT NULL DEFAULT '0',
  "url" varchar(255) NOT NULL DEFAULT '',
  "login" varchar(60) DEFAULT '',
  "password" varchar(255) DEFAULT '',
  "root_url" varchar(255) DEFAULT '',
  "type" varchar(255) DEFAULT NULL,
  "path_encoding" varchar(64) DEFAULT NULL,
  "log_encoding" varchar(64) DEFAULT NULL,
  "extra_info" text,
  "identifier" varchar(255) DEFAULT NULL,
  "is_default" boolean DEFAULT '0',
  "created_on" timestamp NULL DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS redmine."roles" (
  "id" integer NOT NULL,
  "name" varchar(255) NOT NULL DEFAULT '',
  "position" integer DEFAULT NULL,
  "assignable" boolean DEFAULT '1',
  "builtin" integer NOT NULL DEFAULT '0',
  "permissions" text,
  "issues_visibility" varchar(30) NOT NULL DEFAULT 'default',
  "users_visibility" varchar(30) NOT NULL DEFAULT 'all',
  "time_entries_visibility" varchar(30) NOT NULL DEFAULT 'all',
  "all_roles_managed" boolean NOT NULL DEFAULT '1',
  "settings" text,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS redmine."roles_managed_roles" (
  "role_id" integer NOT NULL,
  "managed_role_id" integer NOT NULL
);


CREATE TABLE IF NOT EXISTS redmine."schema_migrations" (
  "version" varchar(255) NOT NULL,
  PRIMARY KEY (version)
);


CREATE TABLE IF NOT EXISTS redmine."settings" (
  "id" integer NOT NULL,
  "name" varchar(255) NOT NULL DEFAULT '',
  "value" text,
  "updated_on" timestamp NULL DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS redmine."time_entries" (
  "id" integer NOT NULL,
  "project_id" integer NOT NULL,
  "author_id" integer DEFAULT NULL,
  "user_id" integer NOT NULL,
  "issue_id" integer DEFAULT NULL,
  "hours" float NOT NULL,
  "comments" varchar(1024) DEFAULT NULL,
  "activity_id" integer NOT NULL,
  "spent_on" date NOT NULL,
  "tyear" integer NOT NULL,
  "tmonth" integer NOT NULL,
  "tweek" integer NOT NULL,
  "created_on" timestamp NOT NULL,
  "updated_on" timestamp NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS redmine."tokens" (
  "id" integer NOT NULL,
  "user_id" integer NOT NULL DEFAULT '0',
  "action" varchar(30) NOT NULL DEFAULT '',
  "value" varchar(40) NOT NULL DEFAULT '',
  "created_on" timestamp NOT NULL,
  "updated_on" timestamp NULL DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS redmine."trackers" (
  "id" integer NOT NULL,
  "name" varchar(30) NOT NULL DEFAULT '',
  "description" varchar(255) DEFAULT NULL,
  "is_in_chlog" boolean NOT NULL DEFAULT '0',
  "position" integer DEFAULT NULL,
  "is_in_roadmap" boolean NOT NULL DEFAULT '1',
  "fields_bits" integer DEFAULT '0',
  "default_status_id" integer DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS redmine."user_preferences" (
  "id" integer NOT NULL,
  "user_id" integer NOT NULL DEFAULT '0',
  "others" text,
  "hide_mail" boolean DEFAULT '1',
  "time_zone" varchar(255) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS redmine."users" (
  "id" integer NOT NULL,
  "login" varchar(255) NOT NULL DEFAULT '',
  "hashed_password" varchar(40) NOT NULL DEFAULT '',
  "firstname" varchar(30) NOT NULL DEFAULT '',
  "lastname" varchar(255) NOT NULL DEFAULT '',
  "admin" boolean NOT NULL DEFAULT '0',
  "status" integer NOT NULL DEFAULT '1',
  "last_login_on" timestamp DEFAULT NULL,
  "language" varchar(5) DEFAULT '',
  "auth_source_id" integer DEFAULT NULL,
  "created_on" timestamp NULL DEFAULT NULL,
  "updated_on" timestamp NULL DEFAULT NULL,
  "type" varchar(255) DEFAULT NULL,
  "identity_url" varchar(255) DEFAULT NULL,
  "mail_notification" varchar(255) NOT NULL DEFAULT '',
  "salt" varchar(64) DEFAULT NULL,
  "must_change_passwd" boolean NOT NULL DEFAULT '0',
  "passwd_changed_on" timestamp DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS redmine."versions" (
  "id" integer NOT NULL,
  "project_id" integer NOT NULL DEFAULT '0',
  "name" varchar(255) NOT NULL DEFAULT '',
  "description" varchar(255) DEFAULT '',
  "effective_date" date DEFAULT NULL,
  "created_on" timestamp NULL DEFAULT NULL,
  "updated_on" timestamp NULL DEFAULT NULL,
  "wiki_page_title" varchar(255) DEFAULT NULL,
  "status" varchar(255) DEFAULT 'open',
  "sharing" varchar(255) NOT NULL DEFAULT 'none',
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS redmine."watchers" (
  "id" integer NOT NULL,
  "watchable_type" varchar(255) NOT NULL DEFAULT '',
  "watchable_id" integer NOT NULL DEFAULT '0',
  "user_id" integer DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS redmine."wiki_content_versions" (
  "id" integer NOT NULL,
  "wiki_content_id" integer NOT NULL,
  "page_id" integer NOT NULL,
  "author_id" integer DEFAULT NULL,
  "data" bytea,
  "compression" varchar(6) DEFAULT '',
  "comments" varchar(1024) DEFAULT '',
  "updated_on" timestamp NOT NULL,
  "version" integer NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS redmine."wiki_contents" (
  "id" integer NOT NULL,
  "page_id" integer NOT NULL,
  "author_id" integer DEFAULT NULL,
  "text" text,
  "comments" varchar(1024) DEFAULT '',
  "updated_on" timestamp NOT NULL,
  "version" integer NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS redmine."wiki_pages" (
  "id" integer NOT NULL,
  "wiki_id" integer NOT NULL,
  "title" varchar(255) NOT NULL,
  "created_on" timestamp NOT NULL,
  "protected" boolean NOT NULL DEFAULT '0',
  "parent_id" integer DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS redmine."wiki_redirects" (
  "id" integer NOT NULL,
  "wiki_id" integer NOT NULL,
  "title" varchar(255) DEFAULT NULL,
  "redirects_to" varchar(255) DEFAULT NULL,
  "created_on" timestamp NOT NULL,
  "redirects_to_wiki_id" integer NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS redmine."wikis" (
  "id" integer NOT NULL,
  "project_id" integer NOT NULL,
  "start_page" varchar(255) NOT NULL,
  "status" integer NOT NULL DEFAULT '1',
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS redmine."workflows" (
  "id" integer NOT NULL,
  "tracker_id" integer NOT NULL DEFAULT '0',
  "old_status_id" integer NOT NULL DEFAULT '0',
  "new_status_id" integer NOT NULL DEFAULT '0',
  "role_id" integer NOT NULL DEFAULT '0',
  "assignee" boolean NOT NULL DEFAULT '0',
  "author" boolean NOT NULL DEFAULT '0',
  "type" varchar(30) DEFAULT NULL,
  "field_name" varchar(30) DEFAULT NULL,
  "rule" varchar(30) DEFAULT NULL,
  PRIMARY KEY (id)
);

