


CREATE TABLE IF NOT EXISTS diaspora."account_deletions" (
  "id" int NOT NULL,
  "person_id" int DEFAULT NULL,
  "completed_at" timestamp DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS diaspora."account_migrations" (
  "id" bigint NOT NULL,
  "old_person_id" int NOT NULL,
  "new_person_id" int NOT NULL,
  "completed_at" timestamp DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS diaspora."ar_internal_metadata" (
  "value" varchar(255) DEFAULT NULL,
  "created_at" timestamp NOT NULL,
  "updated_at" timestamp NOT NULL,
  PRIMARY KEY (key)
);


CREATE TABLE IF NOT EXISTS diaspora."aspect_memberships" (
  "id" int NOT NULL,
  "aspect_id" int NOT NULL,
  "contact_id" int NOT NULL,
  "created_at" timestamp NOT NULL,
  "updated_at" timestamp NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS diaspora."aspect_visibilities" (
  "id" int NOT NULL,
  "shareable_id" int NOT NULL,
  "aspect_id" int NOT NULL,
  "shareable_type" varchar(255) NOT NULL DEFAULT 'Post',
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS diaspora."aspects" (
  "id" int NOT NULL,
  "name" varchar(255) NOT NULL,
  "user_id" int NOT NULL,
  "created_at" timestamp NOT NULL,
  "updated_at" timestamp NOT NULL,
  "order_id" int DEFAULT NULL,
  "post_default" boolean DEFAULT '1',
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS diaspora."authorizations" (
  "id" int NOT NULL,
  "user_id" int DEFAULT NULL,
  "o_auth_application_id" int DEFAULT NULL,
  "refresh_token" varchar(255) DEFAULT NULL,
  "code" varchar(255) DEFAULT NULL,
  "redirect_uri" varchar(255) DEFAULT NULL,
  "nonce" varchar(255) DEFAULT NULL,
  "scopes" text,
  "code_used" boolean DEFAULT '0',
  "created_at" timestamp NOT NULL,
  "updated_at" timestamp NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS diaspora."blocks" (
  "id" int NOT NULL,
  "user_id" int DEFAULT NULL,
  "person_id" int DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS diaspora."comment_signatures" (
  "comment_id" int NOT NULL,
  "author_signature" text NOT NULL,
  "signature_order_id" int NOT NULL,
  "additional_data" text
);


CREATE TABLE IF NOT EXISTS diaspora."comments" (
  "id" int NOT NULL,
  "text" text NOT NULL,
  "commentable_id" int NOT NULL,
  "author_id" int NOT NULL,
  "guid" varchar(255) NOT NULL,
  "created_at" timestamp NOT NULL,
  "updated_at" timestamp NOT NULL,
  "likes_count" int NOT NULL DEFAULT '0',
  "commentable_type" varchar(60) NOT NULL DEFAULT 'Post',
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS diaspora."contacts" (
  "id" int NOT NULL,
  "user_id" int NOT NULL,
  "person_id" int NOT NULL,
  "created_at" timestamp NOT NULL,
  "updated_at" timestamp NOT NULL,
  "sharing" boolean NOT NULL DEFAULT '0',
  "receiving" boolean NOT NULL DEFAULT '0',
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS diaspora."conversation_visibilities" (
  "id" int NOT NULL,
  "conversation_id" int NOT NULL,
  "person_id" int NOT NULL,
  "unread" int NOT NULL DEFAULT '0',
  "created_at" timestamp NOT NULL,
  "updated_at" timestamp NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS diaspora."conversations" (
  "id" int NOT NULL,
  "subject" varchar(255) DEFAULT NULL,
  "guid" varchar(255) NOT NULL,
  "author_id" int NOT NULL,
  "created_at" timestamp NOT NULL,
  "updated_at" timestamp NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS diaspora."invitation_codes" (
  "id" int NOT NULL,
  "token" varchar(255) DEFAULT NULL,
  "user_id" int DEFAULT NULL,
  "count" int DEFAULT NULL,
  "created_at" timestamp NOT NULL,
  "updated_at" timestamp NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS diaspora."like_signatures" (
  "like_id" int NOT NULL,
  "author_signature" text NOT NULL,
  "signature_order_id" int NOT NULL,
  "additional_data" text
);


CREATE TABLE IF NOT EXISTS diaspora."likes" (
  "id" int NOT NULL,
  "positive" boolean DEFAULT '1',
  "target_id" int DEFAULT NULL,
  "author_id" int DEFAULT NULL,
  "guid" varchar(255) DEFAULT NULL,
  "created_at" timestamp NOT NULL,
  "updated_at" timestamp NOT NULL,
  "target_type" varchar(60) NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS diaspora."locations" (
  "id" int NOT NULL,
  "address" varchar(255) DEFAULT NULL,
  "lat" varchar(255) DEFAULT NULL,
  "lng" varchar(255) DEFAULT NULL,
  "status_message_id" int DEFAULT NULL,
  "created_at" timestamp NOT NULL,
  "updated_at" timestamp NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS diaspora."mentions" (
  "id" int NOT NULL,
  "mentions_container_id" int NOT NULL,
  "person_id" int NOT NULL,
  "mentions_container_type" varchar(255) NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS diaspora."messages" (
  "id" int NOT NULL,
  "conversation_id" int NOT NULL,
  "author_id" int NOT NULL,
  "guid" varchar(255) NOT NULL,
  "text" text NOT NULL,
  "created_at" timestamp NOT NULL,
  "updated_at" timestamp NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS diaspora."notification_actors" (
  "id" int NOT NULL,
  "notification_id" int DEFAULT NULL,
  "person_id" int DEFAULT NULL,
  "created_at" timestamp NOT NULL,
  "updated_at" timestamp NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS diaspora."notifications" (
  "id" int NOT NULL,
  "target_type" varchar(255) DEFAULT NULL,
  "target_id" int DEFAULT NULL,
  "recipient_id" int NOT NULL,
  "unread" boolean NOT NULL DEFAULT '1',
  "created_at" timestamp NOT NULL,
  "updated_at" timestamp NOT NULL,
  "type" varchar(255) DEFAULT NULL,
  "guid" varchar(255) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS diaspora."o_auth_access_tokens" (
  "id" int NOT NULL,
  "authorization_id" int DEFAULT NULL,
  "token" varchar(255) DEFAULT NULL,
  "expires_at" timestamp DEFAULT NULL,
  "created_at" timestamp NOT NULL,
  "updated_at" timestamp NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS diaspora."o_auth_applications" (
  "id" int NOT NULL,
  "user_id" int DEFAULT NULL,
  "client_id" varchar(255) DEFAULT NULL,
  "client_secret" varchar(255) DEFAULT NULL,
  "client_name" varchar(255) DEFAULT NULL,
  "redirect_uris" text,
  "response_types" varchar(255) DEFAULT NULL,
  "grant_types" varchar(255) DEFAULT NULL,
  "application_type" varchar(255) DEFAULT 'web',
  "contacts" varchar(255) DEFAULT NULL,
  "logo_uri" varchar(255) DEFAULT NULL,
  "client_uri" varchar(255) DEFAULT NULL,
  "policy_uri" varchar(255) DEFAULT NULL,
  "tos_uri" varchar(255) DEFAULT NULL,
  "sector_identifier_uri" varchar(255) DEFAULT NULL,
  "token_endpoint_auth_method" varchar(255) DEFAULT NULL,
  "jwks" text,
  "jwks_uri" varchar(255) DEFAULT NULL,
  "ppid" boolean DEFAULT '0',
  "created_at" timestamp NOT NULL,
  "updated_at" timestamp NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS diaspora."o_embed_caches" (
  "id" int NOT NULL,
  "url" varchar(1024) NOT NULL,
  "data" text NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS diaspora."open_graph_caches" (
  "id" int NOT NULL,
  "title" varchar(255) DEFAULT NULL,
  "ob_type" varchar(255) DEFAULT NULL,
  "image" text,
  "url" text,
  "description" text,
  "video_url" text,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS diaspora."participations" (
  "id" int NOT NULL,
  "guid" varchar(255) DEFAULT NULL,
  "target_id" int DEFAULT NULL,
  "target_type" varchar(60) NOT NULL,
  "author_id" int DEFAULT NULL,
  "created_at" timestamp NOT NULL,
  "updated_at" timestamp NOT NULL,
  "count" int NOT NULL DEFAULT '1',
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS diaspora."people" (
  "id" int NOT NULL,
  "guid" varchar(255) NOT NULL,
  "diaspora_handle" varchar(255) NOT NULL,
  "serialized_public_key" text NOT NULL,
  "owner_id" int DEFAULT NULL,
  "created_at" timestamp NOT NULL,
  "updated_at" timestamp NOT NULL,
  "closed_account" boolean DEFAULT '0',
  "fetch_status" int DEFAULT '0',
  "pod_id" int DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS diaspora."photos" (
  "id" int NOT NULL,
  "author_id" int NOT NULL,
  "public" boolean NOT NULL DEFAULT '0',
  "guid" varchar(255) NOT NULL,
  "pending" boolean NOT NULL DEFAULT '0',
  "text" text,
  "remote_photo_path" text,
  "remote_photo_name" varchar(255) DEFAULT NULL,
  "random_string" varchar(255) DEFAULT NULL,
  "processed_image" varchar(255) DEFAULT NULL,
  "created_at" timestamp DEFAULT NULL,
  "updated_at" timestamp DEFAULT NULL,
  "unprocessed_image" varchar(255) DEFAULT NULL,
  "status_message_guid" varchar(255) DEFAULT NULL,
  "height" int DEFAULT NULL,
  "width" int DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS diaspora."pods" (
  "id" int NOT NULL,
  "host" varchar(255) NOT NULL,
  "ssl" boolean DEFAULT NULL,
  "created_at" timestamp NOT NULL,
  "updated_at" timestamp NOT NULL,
  "status" int DEFAULT '0',
  "checked_at" timestamp DEFAULT '1970-01-01 00:00:00',
  "offline_since" timestamp DEFAULT NULL,
  "response_time" int DEFAULT '-1',
  "software" varchar(255) DEFAULT NULL,
  "error" varchar(255) DEFAULT NULL,
  "port" int DEFAULT NULL,
  "blocked" boolean DEFAULT '0',
  "scheduled_check" boolean NOT NULL DEFAULT '0',
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS diaspora."poll_answers" (
  "id" int NOT NULL,
  "answer" varchar(255) NOT NULL,
  "poll_id" int NOT NULL,
  "guid" varchar(255) DEFAULT NULL,
  "vote_count" int DEFAULT '0',
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS diaspora."poll_participation_signatures" (
  "poll_participation_id" int NOT NULL,
  "author_signature" text NOT NULL,
  "signature_order_id" int NOT NULL,
  "additional_data" text
);


CREATE TABLE IF NOT EXISTS diaspora."poll_participations" (
  "id" int NOT NULL,
  "poll_answer_id" int NOT NULL,
  "author_id" int NOT NULL,
  "poll_id" int NOT NULL,
  "guid" varchar(255) DEFAULT NULL,
  "created_at" timestamp DEFAULT NULL,
  "updated_at" timestamp DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS diaspora."polls" (
  "id" int NOT NULL,
  "question" varchar(255) NOT NULL,
  "status_message_id" int NOT NULL,
  "status" boolean DEFAULT NULL,
  "guid" varchar(255) DEFAULT NULL,
  "created_at" timestamp DEFAULT NULL,
  "updated_at" timestamp DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS diaspora."posts" (
  "id" int NOT NULL,
  "author_id" int NOT NULL,
  "public" boolean NOT NULL DEFAULT '0',
  "guid" varchar(255) NOT NULL,
  "type" varchar(40) NOT NULL,
  "text" text,
  "created_at" timestamp NOT NULL,
  "updated_at" timestamp NOT NULL,
  "provider_display_name" varchar(255) DEFAULT NULL,
  "root_guid" varchar(255) DEFAULT NULL,
  "likes_count" int DEFAULT '0',
  "comments_count" int DEFAULT '0',
  "o_embed_cache_id" int DEFAULT NULL,
  "reshares_count" int DEFAULT '0',
  "interacted_at" timestamp DEFAULT NULL,
  "tweet_id" varchar(255) DEFAULT NULL,
  "open_graph_cache_id" int DEFAULT NULL,
  "tumblr_ids" text,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS diaspora."ppid" (
  "id" int NOT NULL,
  "o_auth_application_id" int DEFAULT NULL,
  "user_id" int DEFAULT NULL,
  "guid" varchar(32) DEFAULT NULL,
  "identifier" varchar(255) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS diaspora."profiles" (
  "id" int NOT NULL,
  "diaspora_handle" varchar(255) DEFAULT NULL,
  "first_name" varchar(127) DEFAULT NULL,
  "last_name" varchar(127) DEFAULT NULL,
  "image_url" varchar(255) DEFAULT NULL,
  "image_url_small" varchar(255) DEFAULT NULL,
  "image_url_medium" varchar(255) DEFAULT NULL,
  "birthday" date DEFAULT NULL,
  "gender" varchar(255) DEFAULT NULL,
  "bio" text,
  "searchable" boolean NOT NULL DEFAULT '1',
  "person_id" int NOT NULL,
  "created_at" timestamp NOT NULL,
  "updated_at" timestamp NOT NULL,
  "location" varchar(255) DEFAULT NULL,
  "full_name" varchar(70) DEFAULT NULL,
  "nsfw" boolean DEFAULT '0',
  "public_details" boolean DEFAULT '0',
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS diaspora."references" (
  "id" bigint NOT NULL,
  "source_id" int NOT NULL,
  "source_type" varchar(60) NOT NULL,
  "target_id" int NOT NULL,
  "target_type" varchar(60) NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS diaspora."reports" (
  "id" int NOT NULL,
  "item_id" int NOT NULL,
  "item_type" varchar(255) NOT NULL,
  "reviewed" boolean DEFAULT '0',
  "text" text,
  "created_at" timestamp DEFAULT NULL,
  "updated_at" timestamp DEFAULT NULL,
  "user_id" int NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS diaspora."roles" (
  "id" int NOT NULL,
  "person_id" int DEFAULT NULL,
  "name" varchar(255) DEFAULT NULL,
  "created_at" timestamp NOT NULL,
  "updated_at" timestamp NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS diaspora."schema_migrations" (
  "version" varchar(255) NOT NULL,
  PRIMARY KEY (version)
);


CREATE TABLE IF NOT EXISTS diaspora."services" (
  "id" int NOT NULL,
  "type" varchar(127) NOT NULL,
  "user_id" int NOT NULL,
  "uid" varchar(127) DEFAULT NULL,
  "access_token" varchar(255) DEFAULT NULL,
  "access_secret" varchar(255) DEFAULT NULL,
  "nickname" varchar(255) DEFAULT NULL,
  "created_at" timestamp NOT NULL,
  "updated_at" timestamp NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS diaspora."share_visibilities" (
  "id" int NOT NULL,
  "shareable_id" int NOT NULL,
  "hidden" boolean NOT NULL DEFAULT '0',
  "shareable_type" varchar(60) NOT NULL DEFAULT 'Post',
  "user_id" int NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS diaspora."signature_orders" (
  "id" int NOT NULL,
  "order" varchar(255) NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS diaspora."simple_captcha_data" (
  "id" int NOT NULL,
  "value" varchar(12) DEFAULT NULL,
  "created_at" timestamp DEFAULT NULL,
  "updated_at" timestamp DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS diaspora."tag_followings" (
  "id" int NOT NULL,
  "tag_id" int NOT NULL,
  "user_id" int NOT NULL,
  "created_at" timestamp NOT NULL,
  "updated_at" timestamp NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS diaspora."taggings" (
  "id" int NOT NULL,
  "tag_id" int DEFAULT NULL,
  "taggable_id" int DEFAULT NULL,
  "taggable_type" varchar(127) DEFAULT NULL,
  "tagger_id" int DEFAULT NULL,
  "tagger_type" varchar(127) DEFAULT NULL,
  "context" varchar(127) DEFAULT NULL,
  "created_at" timestamp DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS diaspora."tags" (
  "id" int NOT NULL,
  "name" varchar(255) DEFAULT NULL,
  "taggings_count" int DEFAULT '0',
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS diaspora."user_preferences" (
  "id" int NOT NULL,
  "email_type" varchar(255) DEFAULT NULL,
  "user_id" int DEFAULT NULL,
  "created_at" timestamp NOT NULL,
  "updated_at" timestamp NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS diaspora."users" (
  "id" int NOT NULL,
  "username" varchar(255) NOT NULL,
  "serialized_private_key" text,
  "getting_started" boolean NOT NULL DEFAULT '1',
  "disable_mail" boolean NOT NULL DEFAULT '0',
  "language" varchar(255) DEFAULT NULL,
  "email" varchar(255) NOT NULL DEFAULT '',
  "encrypted_password" varchar(255) NOT NULL DEFAULT '',
  "reset_password_token" varchar(255) DEFAULT NULL,
  "remember_created_at" timestamp DEFAULT NULL,
  "sign_in_count" int DEFAULT '0',
  "current_sign_in_at" timestamp DEFAULT NULL,
  "last_sign_in_at" timestamp DEFAULT NULL,
  "current_sign_in_ip" varchar(255) DEFAULT NULL,
  "last_sign_in_ip" varchar(255) DEFAULT NULL,
  "created_at" timestamp NOT NULL,
  "updated_at" timestamp NOT NULL,
  "invited_by_id" int DEFAULT NULL,
  "authentication_token" varchar(30) DEFAULT NULL,
  "unconfirmed_email" varchar(255) DEFAULT NULL,
  "confirm_email_token" varchar(30) DEFAULT NULL,
  "locked_at" timestamp DEFAULT NULL,
  "show_community_spotlight_in_stream" boolean NOT NULL DEFAULT '1',
  "auto_follow_back" boolean DEFAULT '0',
  "auto_follow_back_aspect_id" int DEFAULT NULL,
  "hidden_shareables" text,
  "reset_password_sent_at" timestamp DEFAULT NULL,
  "last_seen" timestamp DEFAULT NULL,
  "remove_after" timestamp DEFAULT NULL,
  "export" varchar(255) DEFAULT NULL,
  "exported_at" timestamp DEFAULT NULL,
  "exporting" boolean DEFAULT '0',
  "strip_exif" boolean DEFAULT '1',
  "exported_photos_file" varchar(255) DEFAULT NULL,
  "exported_photos_at" timestamp DEFAULT NULL,
  "exporting_photos" boolean DEFAULT '0',
  "color_theme" varchar(255) DEFAULT NULL,
  "post_default_public" boolean DEFAULT '0',
  "consumed_timestep" int DEFAULT NULL,
  "otp_required_for_login" boolean DEFAULT NULL,
  "otp_backup_codes" text,
  "plain_otp_secret" varchar(255) DEFAULT NULL,
  PRIMARY KEY (id)
);

