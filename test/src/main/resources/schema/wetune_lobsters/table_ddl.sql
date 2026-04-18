


CREATE TABLE IF NOT EXISTS lobsters."ar_internal_metadata" (
  "value" varchar(255) DEFAULT NULL,
  "created_at" timestamp NOT NULL,
  "updated_at" timestamp NOT NULL,
  PRIMARY KEY (key)
);


CREATE TABLE IF NOT EXISTS lobsters."comments" (
  "id" bigint NOT NULL,
  "created_at" timestamp NOT NULL,
  "updated_at" timestamp DEFAULT NULL,
  "short_id" varchar(10) NOT NULL DEFAULT '',
  "story_id" bigint NOT NULL,
  "user_id" bigint NOT NULL,
  "parent_comment_id" bigint DEFAULT NULL,
  "thread_id" bigint DEFAULT NULL,
  "comment" text NOT NULL,
  "upvotes" integer NOT NULL DEFAULT '0',
  "downvotes" integer NOT NULL DEFAULT '0',
  "confidence" decimal(20,19) NOT NULL DEFAULT '0.0000000000000000000',
  "markeddown_comment" text,
  "is_deleted" boolean DEFAULT '0',
  "is_moderated" boolean DEFAULT '0',
  "is_from_email" boolean DEFAULT '0',
  "hat_id" bigint DEFAULT NULL,
  PRIMARY KEY (id),
  "FULLTEXT" KEY index_comments_on_comment (comment)
);


CREATE TABLE IF NOT EXISTS lobsters."domains" (
  "id" bigint NOT NULL,
  "domain" varchar(255) DEFAULT NULL,
  "is_tracker" boolean NOT NULL DEFAULT '0',
  "created_at" timestamp NOT NULL,
  "updated_at" timestamp NOT NULL,
  "banned_at" timestamp DEFAULT NULL,
  "banned_by_user_id" integer DEFAULT NULL,
  "banned_reason" varchar(200) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS lobsters."hat_requests" (
  "id" bigint NOT NULL,
  "created_at" timestamp DEFAULT NULL,
  "updated_at" timestamp DEFAULT NULL,
  "user_id" bigint NOT NULL,
  "hat" varchar(255) NOT NULL,
  "link" varchar(255) NOT NULL,
  "comment" text NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS lobsters."hats" (
  "id" bigint NOT NULL,
  "created_at" timestamp DEFAULT NULL,
  "updated_at" timestamp DEFAULT NULL,
  "user_id" bigint NOT NULL,
  "granted_by_user_id" bigint NOT NULL,
  "hat" varchar(255) NOT NULL,
  "link" varchar(255) DEFAULT NULL,
  "modlog_use" boolean DEFAULT '0',
  "doffed_at" timestamp DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS lobsters."hidden_stories" (
  "id" bigint NOT NULL,
  "user_id" bigint NOT NULL,
  "story_id" bigint NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS lobsters."invitation_requests" (
  "id" bigint NOT NULL,
  "code" varchar(255) DEFAULT NULL,
  "is_verified" boolean DEFAULT '0',
  "email" varchar(255) NOT NULL,
  "name" varchar(255) NOT NULL,
  "memo" text,
  "ip_address" varchar(255) DEFAULT NULL,
  "created_at" timestamp NOT NULL,
  "updated_at" timestamp NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS lobsters."invitations" (
  "id" bigint NOT NULL,
  "user_id" bigint NOT NULL,
  "email" varchar(255) DEFAULT NULL,
  "code" varchar(255) DEFAULT NULL,
  "created_at" timestamp NOT NULL,
  "updated_at" timestamp NOT NULL,
  "memo" text,
  "used_at" timestamp DEFAULT NULL,
  "new_user_id" bigint DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS lobsters."keystores" (
  "value" bigint DEFAULT NULL
);


CREATE TABLE IF NOT EXISTS lobsters."messages" (
  "id" bigint NOT NULL,
  "created_at" timestamp DEFAULT NULL,
  "author_user_id" bigint NOT NULL,
  "recipient_user_id" bigint NOT NULL,
  "has_been_read" boolean DEFAULT '0',
  "subject" varchar(100) DEFAULT NULL,
  "body" text,
  "short_id" varchar(30) DEFAULT NULL,
  "deleted_by_author" boolean DEFAULT '0',
  "deleted_by_recipient" boolean DEFAULT '0',
  "hat_id" bigint DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS lobsters."mod_notes" (
  "id" bigint NOT NULL,
  "moderator_user_id" bigint NOT NULL,
  "user_id" bigint NOT NULL,
  "note" text NOT NULL,
  "markeddown_note" text NOT NULL,
  "created_at" timestamp NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS lobsters."moderations" (
  "id" bigint NOT NULL,
  "created_at" timestamp NOT NULL,
  "updated_at" timestamp NOT NULL,
  "moderator_user_id" bigint DEFAULT NULL,
  "story_id" bigint DEFAULT NULL,
  "comment_id" bigint DEFAULT NULL,
  "user_id" bigint DEFAULT NULL,
  "action" text,
  "reason" text,
  "is_from_suggestions" boolean DEFAULT '0',
  "tag_id" bigint DEFAULT NULL,
  "domain_id" integer DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS lobsters."read_ribbons" (
  "id" bigint NOT NULL,
  "is_following" boolean DEFAULT '1',
  "created_at" timestamp NOT NULL,
  "updated_at" timestamp NOT NULL,
  "user_id" bigint NOT NULL,
  "story_id" bigint NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS lobsters."saved_stories" (
  "id" bigint NOT NULL,
  "created_at" timestamp NOT NULL,
  "updated_at" timestamp NOT NULL,
  "user_id" bigint NOT NULL,
  "story_id" bigint NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS lobsters."schema_migrations" (
  "version" varchar(255) NOT NULL,
  PRIMARY KEY (version)
);


CREATE TABLE IF NOT EXISTS lobsters."stories" (
  "id" bigint NOT NULL,
  "created_at" timestamp DEFAULT NULL,
  "user_id" bigint NOT NULL,
  "url" varchar(250) DEFAULT '',
  "title" varchar(150) NOT NULL DEFAULT '',
  "description" text,
  "short_id" varchar(6) NOT NULL DEFAULT '',
  "is_expired" boolean NOT NULL DEFAULT '0',
  "upvotes" integer NOT NULL DEFAULT '0',
  "downvotes" integer NOT NULL DEFAULT '0',
  "is_moderated" boolean NOT NULL DEFAULT '0',
  "hotness" decimal(20,10) NOT NULL DEFAULT '0.0000000000',
  "markeddown_description" text,
  "story_cache" text,
  "comments_count" integer NOT NULL DEFAULT '0',
  "merged_story_id" bigint DEFAULT NULL,
  "unavailable_at" timestamp DEFAULT NULL,
  "twitter_id" varchar(20) DEFAULT NULL,
  "user_is_author" boolean DEFAULT '0',
  "user_is_following" boolean NOT NULL DEFAULT '0',
  "domain_id" bigint DEFAULT NULL,
  PRIMARY KEY (id),
  "FULLTEXT" KEY index_stories_on_description (description),
  "FULLTEXT" KEY index_stories_on_story_cache (story_cache),
  "FULLTEXT" KEY stories_story_cache (story_cache),
  "FULLTEXT" KEY index_stories_on_title (title)
);


CREATE TABLE IF NOT EXISTS lobsters."suggested_taggings" (
  "id" bigint NOT NULL,
  "story_id" bigint NOT NULL,
  "tag_id" bigint NOT NULL,
  "user_id" bigint NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS lobsters."suggested_titles" (
  "id" bigint NOT NULL,
  "story_id" bigint NOT NULL,
  "user_id" bigint NOT NULL,
  "title" varchar(150) NOT NULL DEFAULT '',
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS lobsters."tag_filters" (
  "id" bigint NOT NULL,
  "created_at" timestamp NOT NULL,
  "updated_at" timestamp NOT NULL,
  "user_id" bigint NOT NULL,
  "tag_id" bigint NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS lobsters."taggings" (
  "id" bigint NOT NULL,
  "story_id" bigint NOT NULL,
  "tag_id" bigint NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS lobsters."tags" (
  "id" bigint NOT NULL,
  "tag" varchar(25) NOT NULL,
  "description" varchar(100) DEFAULT NULL,
  "privileged" boolean DEFAULT '0',
  "is_media" boolean DEFAULT '0',
  "inactive" boolean DEFAULT '0',
  "hotness_mod" float DEFAULT '0',
  "permit_by_new_users" boolean NOT NULL DEFAULT '1',
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS lobsters."users" (
  "id" bigint NOT NULL,
  "username" varchar(50) DEFAULT NULL,
  "email" varchar(100) DEFAULT NULL,
  "password_digest" varchar(75) DEFAULT NULL,
  "created_at" timestamp DEFAULT NULL,
  "is_admin" boolean DEFAULT '0',
  "password_reset_token" varchar(75) DEFAULT NULL,
  "session_token" varchar(75) NOT NULL DEFAULT '',
  "about" text,
  "invited_by_user_id" bigint DEFAULT NULL,
  "is_moderator" boolean DEFAULT '0',
  "pushover_mentions" boolean DEFAULT '0',
  "rss_token" varchar(75) DEFAULT NULL,
  "mailing_list_token" varchar(75) DEFAULT NULL,
  "mailing_list_mode" integer DEFAULT '0',
  "karma" integer NOT NULL DEFAULT '0',
  "banned_at" timestamp DEFAULT NULL,
  "banned_by_user_id" bigint DEFAULT NULL,
  "banned_reason" varchar(200) DEFAULT NULL,
  "deleted_at" timestamp DEFAULT NULL,
  "disabled_invite_at" timestamp DEFAULT NULL,
  "disabled_invite_by_user_id" bigint DEFAULT NULL,
  "disabled_invite_reason" varchar(200) DEFAULT NULL,
  "settings" text,
  PRIMARY KEY (id)
);


