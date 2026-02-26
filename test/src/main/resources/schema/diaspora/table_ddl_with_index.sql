CREATE TABLE IF NOT EXISTS diaspora.account_deletions (
  id SERIAL,
  person_id integer DEFAULT NULL,
  completed_at timestamp DEFAULT NULL,
  PRIMARY KEY (id),
  CONSTRAINT index_account_deletions_on_person_id UNIQUE (person_id)
);

CREATE TABLE IF NOT EXISTS  diaspora.account_migrations (
  id BIGSERIAL,
  old_person_id integer NOT NULL,
  new_person_id integer NOT NULL,
  completed_at timestamp DEFAULT NULL,
  PRIMARY KEY (id),
  CONSTRAINT index_account_migrations_on_old_person_id_and_new_person_id UNIQUE (old_person_id,new_person_id),
  CONSTRAINT index_account_migrations_on_old_person_id UNIQUE (old_person_id),
  CONSTRAINT fk_rails_610fe19943 FOREIGN KEY (new_person_id) REFERENCES people (id),
  CONSTRAINT fk_rails_ddbe553eee FOREIGN KEY (old_person_id) REFERENCES people (id)
);
CREATE INDEX fk_rails_610fe19943 ON diaspora.account_migrations (new_person_id);

CREATE TABLE IF NOT EXISTS  diaspora.ar_internal_metadata (
  key varchar(255) NOT NULL,
  value varchar(255) DEFAULT NULL,
  created_at timestamp NOT NULL,
  updated_at timestamp NOT NULL,
  PRIMARY KEY (key)
);


CREATE TABLE IF NOT EXISTS  diaspora.aspect_memberships (
  id SERIAL,
  aspect_id integer NOT NULL,
  contact_id integer NOT NULL,
  created_at timestamp NOT NULL,
  updated_at timestamp NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT index_aspect_memberships_on_aspect_id_and_contact_id UNIQUE (aspect_id,contact_id),
  CONSTRAINT aspect_memberships_aspect_id_fk FOREIGN KEY (aspect_id) REFERENCES aspects (id) ON DELETE CASCADE,
  CONSTRAINT aspect_memberships_contact_id_fk FOREIGN KEY (contact_id) REFERENCES contacts (id) ON DELETE CASCADE
);
CREATE INDEX index_aspect_memberships_on_aspect_id ON aspect_memberships (aspect_id);
CREATE INDEX index_aspect_memberships_on_contact_id ON aspect_memberships (contact_id);

CREATE TABLE IF NOT EXISTS diaspora.aspect_visibilities (
  id SERIAL,
  shareable_id integer NOT NULL,
  aspect_id integer NOT NULL,
  shareable_type varchar(255) NOT NULL DEFAULT 'Post',
  PRIMARY KEY (id),
  CONSTRAINT index_aspect_visibilities_on_shareable_and_aspect_id UNIQUE (shareable_id,shareable_type,aspect_id),
  CONSTRAINT aspect_visibilities_aspect_id_fk FOREIGN KEY (aspect_id) REFERENCES aspects (id) ON DELETE CASCADE
);
CREATE INDEX index_aspect_visibilities_on_aspect_id ON aspect_visibilities (aspect_id);
CREATE INDEX index_aspect_visibilities_on_shareable_id_and_shareable_type ON aspect_visibilities (shareable_id,shareable_type);

CREATE TABLE IF NOT EXISTS  diaspora.aspects (
  id SERIAL,
  name varchar(255) NOT NULL,
  user_id integer NOT NULL,
  created_at timestamp NOT NULL,
  updated_at timestamp NOT NULL,
  order_id integer DEFAULT NULL,
  post_default boolean DEFAULT true,
  PRIMARY KEY (id),
  CONSTRAINT index_aspects_on_user_id_and_name UNIQUE (user_id,name)
);
CREATE INDEX index_aspects_on_user_id ON aspects (user_id);

CREATE TABLE IF NOT EXISTS diaspora.authorizations (
  id SERIAL,
  user_id integer DEFAULT NULL,
  o_auth_application_id integer DEFAULT NULL,
  refresh_token varchar(255) DEFAULT NULL,
  code varchar(255) DEFAULT NULL,
  redirect_uri varchar(255) DEFAULT NULL,
  nonce varchar(255) DEFAULT NULL,
  scopes text,
  code_used boolean DEFAULT false,
  created_at timestamp NOT NULL,
  updated_at timestamp NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_rails_4ecef5b8c5 FOREIGN KEY (user_id) REFERENCES users (id),
  CONSTRAINT fk_rails_e166644de5 FOREIGN KEY (o_auth_application_id) REFERENCES o_auth_applications (id)
);
CREATE INDEX index_authorizations_on_o_auth_application_id ON authorizations (o_auth_application_id);
CREATE INDEX index_authorizations_on_user_id ON authorizations (user_id);

CREATE TABLE IF NOT EXISTS  diaspora.blocks (
  id SERIAL,
  user_id integer DEFAULT NULL,
  person_id integer DEFAULT NULL,
  PRIMARY KEY (id),
  CONSTRAINT index_blocks_on_user_id_and_person_id UNIQUE (user_id,person_id)
);

CREATE TABLE IF NOT EXISTS  diaspora.comment_signatures (
  comment_id integer NOT NULL,
  author_signature text NOT NULL,
  signature_order_id integer NOT NULL,
  additional_data text,
  CONSTRAINT index_comment_signatures_on_comment_id UNIQUE (comment_id),
  CONSTRAINT comment_signatures_comment_id_fk FOREIGN KEY (comment_id) REFERENCES comments (id) ON DELETE CASCADE,
  CONSTRAINT comment_signatures_signature_orders_id_fk FOREIGN KEY (signature_order_id) REFERENCES signature_orders (id)
);
CREATE INDEX comment_signatures_signature_orders_id_fk ON comment_signatures (signature_order_id);

CREATE TABLE IF NOT EXISTS diaspora.comments (
  id SERIAL,
  text text NOT NULL,
  commentable_id integer NOT NULL,
  author_id integer NOT NULL,
  guid varchar(255) NOT NULL,
  created_at timestamp NOT NULL,
  updated_at timestamp NOT NULL,
  likes_count integer NOT NULL DEFAULT 0,
  commentable_type varchar(60) NOT NULL DEFAULT 'Post',
  PRIMARY KEY (id),
  CONSTRAINT index_comments_on_guid UNIQUE (guid),
  CONSTRAINT comments_author_id_fk FOREIGN KEY (author_id) REFERENCES people (id) ON DELETE CASCADE
);
CREATE INDEX index_comments_on_person_id ON comments (author_id);
CREATE INDEX index_comments_on_commentable_id_and_commentable_type ON comments (commentable_id,commentable_type);

CREATE TABLE IF NOT EXISTS  diaspora.contacts (
  id SERIAL,
  user_id integer NOT NULL,
  person_id integer NOT NULL,
  created_at timestamp NOT NULL,
  updated_at timestamp NOT NULL,
  sharing boolean NOT NULL DEFAULT false,
  receiving boolean NOT NULL DEFAULT false,
  PRIMARY KEY (id),
  CONSTRAINT index_contacts_on_user_id_and_person_id UNIQUE (user_id,person_id),
  CONSTRAINT contacts_person_id_fk FOREIGN KEY (person_id) REFERENCES people (id) ON DELETE CASCADE
);
CREATE INDEX index_contacts_on_person_id ON contacts (person_id);

CREATE TABLE IF NOT EXISTS  diaspora.conversation_visibilities (
  id SERIAL,
  conversation_id integer NOT NULL,
  person_id integer NOT NULL,
  unread integer NOT NULL DEFAULT 0,
  created_at timestamp NOT NULL,
  updated_at timestamp NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT index_conversation_visibilities_usefully UNIQUE (conversation_id,person_id),
  CONSTRAINT conversation_visibilities_conversation_id_fk FOREIGN KEY (conversation_id) REFERENCES conversations (id) ON DELETE CASCADE,
  CONSTRAINT conversation_visibilities_person_id_fk FOREIGN KEY (person_id) REFERENCES people (id) ON DELETE CASCADE
);
CREATE INDEX index_conversation_visibilities_on_conversation_id ON conversation_visibilities (conversation_id);
CREATE INDEX index_conversation_visibilities_on_person_id ON conversation_visibilities (person_id);

CREATE TABLE IF NOT EXISTS  diaspora.conversations (
  id SERIAL,
  subject varchar(255) DEFAULT NULL,
  guid varchar(255) NOT NULL,
  author_id integer NOT NULL,
  created_at timestamp NOT NULL,
  updated_at timestamp NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT index_conversations_on_guid UNIQUE (guid),
  CONSTRAINT conversations_author_id_fk FOREIGN KEY (author_id) REFERENCES people (id) ON DELETE CASCADE
);
CREATE INDEX conversations_author_id_fk ON conversations (author_id);

CREATE TABLE IF NOT EXISTS  diaspora.invitation_codes (
  id SERIAL,
  token varchar(255) DEFAULT NULL,
  user_id integer DEFAULT NULL,
  count integer DEFAULT NULL,
  created_at timestamp NOT NULL,
  updated_at timestamp NOT NULL,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS  diaspora.like_signatures (
  like_id integer NOT NULL,
  author_signature text NOT NULL,
  signature_order_id integer NOT NULL,
  additional_data text,
  CONSTRAINT index_like_signatures_on_like_id UNIQUE (like_id),
  CONSTRAINT like_signatures_like_id_fk FOREIGN KEY (like_id) REFERENCES likes (id) ON DELETE CASCADE,
  CONSTRAINT like_signatures_signature_orders_id_fk FOREIGN KEY (signature_order_id) REFERENCES signature_orders (id)
);
CREATE INDEX like_signatures_signature_orders_id_fk ON like_signatures (signature_order_id);

CREATE TABLE IF NOT EXISTS  diaspora.likes (
  id SERIAL,
  positive boolean DEFAULT true,
  target_id integer DEFAULT NULL,
  author_id integer DEFAULT NULL,
  guid varchar(255) DEFAULT NULL,
  created_at timestamp NOT NULL,
  updated_at timestamp NOT NULL,
  target_type varchar(60) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT index_likes_on_target_id_and_author_id_and_target_type UNIQUE (target_id,author_id,target_type),
  CONSTRAINT index_likes_on_guid UNIQUE (guid),
  CONSTRAINT likes_author_id_fk FOREIGN KEY (author_id) REFERENCES people (id) ON DELETE CASCADE
);
CREATE INDEX likes_author_id_fk ON likes (author_id);
CREATE INDEX index_likes_on_post_id ON likes (target_id);

CREATE TABLE IF NOT EXISTS  diaspora.locations (
  id SERIAL,
  address varchar(255) DEFAULT NULL,
  lat varchar(255) DEFAULT NULL,
  lng varchar(255) DEFAULT NULL,
  status_message_id integer DEFAULT NULL,
  created_at timestamp NOT NULL,
  updated_at timestamp NOT NULL,
  PRIMARY KEY (id)
);
CREATE INDEX index_locations_on_status_message_id ON locations (status_message_id);

CREATE TABLE IF NOT EXISTS  diaspora.mentions (
  id SERIAL,
  mentions_container_id integer NOT NULL,
  person_id integer NOT NULL,
  mentions_container_type varchar(255) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT index_mentions_on_person_and_mc_id_and_mc_type UNIQUE (person_id,mentions_container_id,mentions_container_type)
);
CREATE INDEX index_mentions_on_mc_id_and_mc_type ON mentions (mentions_container_id,mentions_container_type);
CREATE INDEX index_mentions_on_person_id ON mentions (person_id);

CREATE TABLE IF NOT EXISTS  diaspora.messages (
  id SERIAL,
  conversation_id integer NOT NULL,
  author_id integer NOT NULL,
  guid varchar(255) NOT NULL,
  text text NOT NULL,
  created_at timestamp NOT NULL,
  updated_at timestamp NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT index_messages_on_guid UNIQUE (guid),
  CONSTRAINT messages_author_id_fk FOREIGN KEY (author_id) REFERENCES people (id) ON DELETE CASCADE,
  CONSTRAINT messages_conversation_id_fk FOREIGN KEY (conversation_id) REFERENCES conversations (id) ON DELETE CASCADE
);
CREATE INDEX index_messages_on_author_id ON messages (author_id);
CREATE INDEX messages_conversation_id_fk ON messages (conversation_id);

CREATE TABLE IF NOT EXISTS  diaspora.notification_actors (
  id SERIAL,
  notification_id integer DEFAULT NULL,
  person_id integer DEFAULT NULL,
  created_at timestamp NOT NULL,
  updated_at timestamp NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT index_notification_actors_on_notification_id_and_person_id UNIQUE (notification_id,person_id),
  CONSTRAINT notification_actors_notification_id_fk FOREIGN KEY (notification_id) REFERENCES notifications (id) ON DELETE CASCADE
);
CREATE INDEX index_notification_actors_on_notification_id ON notification_actors (notification_id);
CREATE INDEX index_notification_actors_on_person_id ON notification_actors (person_id);

CREATE TABLE IF NOT EXISTS  diaspora.notifications (
  id SERIAL,
  target_type varchar(255) DEFAULT NULL,
  target_id integer DEFAULT NULL,
  recipient_id integer NOT NULL,
  unread boolean NOT NULL DEFAULT true,
  created_at timestamp NOT NULL,
  updated_at timestamp NOT NULL,
  type varchar(255) DEFAULT NULL,
  guid varchar(255) DEFAULT NULL,
  PRIMARY KEY (id),
  CONSTRAINT index_notifications_on_guid UNIQUE (guid)
);
CREATE INDEX index_notifications_on_recipient_id ON notifications (recipient_id);
CREATE INDEX index_notifications_on_target_id ON notifications (target_id);
CREATE INDEX index_notifications_on_target_type_and_target_id ON notifications (target_type,target_id);

CREATE TABLE IF NOT EXISTS  diaspora.o_auth_access_tokens (
  id SERIAL,
  authorization_id integer DEFAULT NULL,
  token varchar(255) DEFAULT NULL,
  expires_at timestamp DEFAULT NULL,
  created_at timestamp NOT NULL,
  updated_at timestamp NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT index_o_auth_access_tokens_on_token UNIQUE (token),
  CONSTRAINT fk_rails_5debabcff3 FOREIGN KEY (authorization_id) REFERENCES authorizations (id)
);
CREATE INDEX index_o_auth_access_tokens_on_authorization_id ON o_auth_access_tokens (authorization_id);

CREATE TABLE IF NOT EXISTS diaspora.o_auth_applications (
  id SERIAL,
  user_id integer DEFAULT NULL,
  client_id varchar(255) DEFAULT NULL,
  client_secret varchar(255) DEFAULT NULL,
  client_name varchar(255) DEFAULT NULL,
  redirect_uris text,
  response_types varchar(255) DEFAULT NULL,
  grant_types varchar(255) DEFAULT NULL,
  application_type varchar(255) DEFAULT 'web',
  contacts varchar(255) DEFAULT NULL,
  logo_uri varchar(255) DEFAULT NULL,
  client_uri varchar(255) DEFAULT NULL,
  policy_uri varchar(255) DEFAULT NULL,
  tos_uri varchar(255) DEFAULT NULL,
  sector_identifier_uri varchar(255) DEFAULT NULL,
  token_endpoint_auth_method varchar(255) DEFAULT NULL,
  jwks text,
  jwks_uri varchar(255) DEFAULT NULL,
  ppid boolean DEFAULT false,
  created_at timestamp NOT NULL,
  updated_at timestamp NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT index_o_auth_applications_on_client_id UNIQUE (client_id),
  CONSTRAINT fk_rails_ad75323da2 FOREIGN KEY (user_id) REFERENCES users (id)
);
CREATE INDEX index_o_auth_applications_on_user_id ON o_auth_applications (user_id);

CREATE TABLE IF NOT EXISTS  diaspora.o_embed_caches (
  id SERIAL,
  url varchar(1024) NOT NULL,
  data text NOT NULL,
  PRIMARY KEY (id)
);
CREATE INDEX index_o_embed_caches_on_url ON o_embed_caches (url);


CREATE TABLE IF NOT EXISTS diaspora.open_graph_caches (
  id SERIAL,
  title varchar(255) DEFAULT NULL,
  ob_type varchar(255) DEFAULT NULL,
  image text,
  url text,
  description text,
  video_url text,
  PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS  diaspora.participations (
  id SERIAL,
  guid varchar(255) DEFAULT NULL,
  target_id integer DEFAULT NULL,
  target_type varchar(60) NOT NULL,
  author_id integer DEFAULT NULL,
  created_at timestamp NOT NULL,
  updated_at timestamp NOT NULL,
  count integer NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  CONSTRAINT index_participations_on_target_id_and_target_type_and_author_id UNIQUE (target_id,target_type,author_id)
);
CREATE INDEX index_participations_on_author_id ON participations (author_id);
CREATE INDEX index_participations_on_guid ON participations (guid);

CREATE TABLE IF NOT EXISTS diaspora.people (
  id SERIAL,
  guid varchar(255) NOT NULL,
  diaspora_handle varchar(255) NOT NULL,
  serialized_public_key text NOT NULL,
  owner_id integer DEFAULT NULL,
  created_at timestamp NOT NULL,
  updated_at timestamp NOT NULL,
  closed_account boolean DEFAULT false,
  fetch_status integer DEFAULT 0,
  pod_id integer DEFAULT NULL,
  PRIMARY KEY (id),
  CONSTRAINT index_people_on_diaspora_handle UNIQUE (diaspora_handle),
  CONSTRAINT index_people_on_guid UNIQUE (guid),
  CONSTRAINT index_people_on_owner_id UNIQUE (owner_id),
  CONSTRAINT people_pod_id_fk FOREIGN KEY (pod_id) REFERENCES pods (id) ON DELETE CASCADE
);
CREATE INDEX people_pod_id_fk ON people (pod_id);

CREATE TABLE IF NOT EXISTS diaspora.photos (
  id SERIAL,
  author_id integer NOT NULL,
  public boolean NOT NULL DEFAULT false,
  guid varchar(255) NOT NULL,
  pending boolean NOT NULL DEFAULT false,
  text text,
  remote_photo_path text,
  remote_photo_name varchar(255) DEFAULT NULL,
  random_string varchar(255) DEFAULT NULL,
  processed_image varchar(255) DEFAULT NULL,
  created_at timestamp DEFAULT NULL,
  updated_at timestamp DEFAULT NULL,
  unprocessed_image varchar(255) DEFAULT NULL,
  status_message_guid varchar(255) DEFAULT NULL,
  height integer DEFAULT NULL,
  width integer DEFAULT NULL,
  PRIMARY KEY (id),
  CONSTRAINT index_photos_on_guid UNIQUE (guid)
);
CREATE INDEX index_photos_on_author_id ON photos (author_id);
CREATE INDEX index_photos_on_status_message_guid ON photos (status_message_guid);

CREATE TABLE IF NOT EXISTS diaspora.pods (
  id SERIAL,
  host varchar(255) NOT NULL,
  ssl boolean DEFAULT NULL,
  created_at timestamp NOT NULL,
  updated_at timestamp NOT NULL,
  status integer DEFAULT 0,
  checked_at timestamp DEFAULT '1970-01-01 00:00:00',
  offline_since timestamp DEFAULT NULL,
  response_time integer DEFAULT '-1',
  software varchar(255) DEFAULT NULL,
  error varchar(255) DEFAULT NULL,
  port integer DEFAULT NULL,
  blocked boolean DEFAULT false,
  scheduled_check boolean NOT NULL DEFAULT false,
  PRIMARY KEY (id),
  CONSTRAINT index_pods_on_host_and_port UNIQUE (host,port)
);
CREATE INDEX index_pods_on_checked_at ON pods (checked_at);
CREATE INDEX index_pods_on_offline_since ON pods (offline_since);
CREATE INDEX index_pods_on_status ON pods (status);

CREATE TABLE IF NOT EXISTS diaspora.poll_answers (
  id SERIAL,
  answer varchar(255) NOT NULL,
  poll_id integer NOT NULL,
  guid varchar(255) DEFAULT NULL,
  vote_count integer DEFAULT 0,
  PRIMARY KEY (id),
  CONSTRAINT index_poll_answers_on_guid UNIQUE (guid)
);
CREATE INDEX index_poll_answers_on_poll_id ON poll_answers (poll_id);

CREATE TABLE IF NOT EXISTS diaspora.poll_participation_signatures (
  poll_participation_id integer NOT NULL,
  author_signature text NOT NULL,
  signature_order_id integer NOT NULL,
  additional_data text,
  CONSTRAINT index_poll_participation_signatures_on_poll_participation_id UNIQUE (poll_participation_id),
  CONSTRAINT poll_participation_signatures_poll_participation_id_fk FOREIGN KEY (poll_participation_id) REFERENCES poll_participations (id) ON DELETE CASCADE,
  CONSTRAINT poll_participation_signatures_signature_orders_id_fk FOREIGN KEY (signature_order_id) REFERENCES signature_orders (id)
);
CREATE INDEX poll_participation_signatures_signature_orders_id_fk ON poll_participation_signatures (signature_order_id);

CREATE TABLE IF NOT EXISTS diaspora.poll_participations (
  id SERIAL,
  poll_answer_id integer NOT NULL,
  author_id integer NOT NULL,
  poll_id integer NOT NULL,
  guid varchar(255) DEFAULT NULL,
  created_at timestamp DEFAULT NULL,
  updated_at timestamp DEFAULT NULL,
  PRIMARY KEY (id),
  CONSTRAINT index_poll_participations_on_poll_id_and_author_id UNIQUE (poll_id,author_id),
  CONSTRAINT index_poll_participations_on_guid UNIQUE (guid)
);

CREATE TABLE IF NOT EXISTS diaspora.polls (
  id SERIAL,
  question varchar(255) NOT NULL,
  status_message_id integer NOT NULL,
  status boolean DEFAULT NULL,
  guid varchar(255) DEFAULT NULL,
  created_at timestamp DEFAULT NULL,
  updated_at timestamp DEFAULT NULL,
  PRIMARY KEY (id),
  CONSTRAINT index_polls_on_guid UNIQUE (guid)
);
CREATE INDEX index_polls_on_status_message_id ON polls (status_message_id);

CREATE TABLE IF NOT EXISTS diaspora.posts (
  id SERIAL,
  author_id integer NOT NULL,
  public boolean NOT NULL DEFAULT false,
  guid varchar(255) NOT NULL,
  type varchar(40) NOT NULL,
  text text,
  created_at timestamp NOT NULL,
  updated_at timestamp NOT NULL,
  provider_display_name varchar(255) DEFAULT NULL,
  root_guid varchar(255) DEFAULT NULL,
  likes_count integer DEFAULT 0,
  comments_count integer DEFAULT 0,
  o_embed_cache_id integer DEFAULT NULL,
  reshares_count integer DEFAULT 0,
  interacted_at timestamp DEFAULT NULL,
  tweet_id varchar(255) DEFAULT NULL,
  open_graph_cache_id integer DEFAULT NULL,
  tumblr_ids text,
  PRIMARY KEY (id),
  CONSTRAINT index_posts_on_guid UNIQUE (guid),
  CONSTRAINT index_posts_on_author_id_and_root_guid UNIQUE (author_id,root_guid),
  CONSTRAINT posts_author_id_fk FOREIGN KEY (author_id) REFERENCES people (id) ON DELETE CASCADE
);
CREATE INDEX index_posts_on_person_id ON posts (author_id);
CREATE INDEX index_posts_on_created_at_and_id ON posts (created_at,id);
CREATE INDEX index_posts_on_id_and_type ON posts (id,type);
CREATE INDEX index_posts_on_root_guid ON posts (root_guid);

CREATE TABLE IF NOT EXISTS diaspora.ppid (
  id SERIAL,
  o_auth_application_id integer DEFAULT NULL,
  user_id integer DEFAULT NULL,
  guid varchar(32) DEFAULT NULL,
  identifier varchar(255) DEFAULT NULL,
  PRIMARY KEY (id),
  CONSTRAINT fk_rails_150457f962 FOREIGN KEY (o_auth_application_id) REFERENCES o_auth_applications (id),
  CONSTRAINT fk_rails_e6b8e5264f FOREIGN KEY (user_id) REFERENCES users (id)
);
CREATE INDEX index_ppid_on_o_auth_application_id ON ppid (o_auth_application_id);
CREATE INDEX index_ppid_on_user_id ON ppid (user_id);

CREATE TABLE IF NOT EXISTS diaspora.profiles (
  id SERIAL,
  diaspora_handle varchar(255) DEFAULT NULL,
  first_name varchar(127) DEFAULT NULL,
  last_name varchar(127) DEFAULT NULL,
  image_url varchar(255) DEFAULT NULL,
  image_url_small varchar(255) DEFAULT NULL,
  image_url_medium varchar(255) DEFAULT NULL,
  birthday date DEFAULT NULL,
  gender varchar(255) DEFAULT NULL,
  bio text,
  searchable boolean NOT NULL DEFAULT true,
  person_id integer NOT NULL,
  created_at timestamp NOT NULL,
  updated_at timestamp NOT NULL,
  location varchar(255) DEFAULT NULL,
  full_name varchar(70) DEFAULT NULL,
  nsfw boolean DEFAULT false,
  public_details boolean DEFAULT false,
  PRIMARY KEY (id),
  CONSTRAINT profiles_person_id_fk FOREIGN KEY (person_id) REFERENCES people (id) ON DELETE CASCADE
);
CREATE INDEX index_profiles_on_full_name_and_searchable ON profiles (full_name,searchable);
CREATE INDEX index_profiles_on_full_name ON profiles (full_name);
CREATE INDEX index_profiles_on_person_id ON profiles (person_id);

CREATE TABLE IF NOT EXISTS diaspora.references (
  id BIGSERIAL,
  source_id integer NOT NULL,
  source_type varchar(60) NOT NULL,
  target_id integer NOT NULL,
  target_type varchar(60) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT index_references_on_source_and_target UNIQUE (source_id,source_type,target_id,target_type)
);
CREATE INDEX index_references_on_source_id_and_source_type ON references (source_id,source_type);

CREATE TABLE IF NOT EXISTS diaspora.reports (
  id SERIAL,
  item_id integer NOT NULL,
  item_type varchar(255) NOT NULL,
  reviewed boolean DEFAULT false,
  text text,
  created_at timestamp DEFAULT NULL,
  updated_at timestamp DEFAULT NULL,
  user_id integer NOT NULL,
  PRIMARY KEY (id)
);
CREATE INDEX index_reports_on_item_id ON reports (item_id);

CREATE TABLE IF NOT EXISTS diaspora.roles (
  id SERIAL,
  person_id integer DEFAULT NULL,
  name varchar(255) DEFAULT NULL,
  created_at timestamp NOT NULL,
  updated_at timestamp NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT index_roles_on_person_id_and_name UNIQUE (person_id,name)
);

CREATE TABLE IF NOT EXISTS diaspora.schema_migrations (
  version varchar(255) NOT NULL,
  PRIMARY KEY (version)
);

CREATE TABLE IF NOT EXISTS diaspora.services (
  id SERIAL,
  type varchar(127) NOT NULL,
  user_id integer NOT NULL,
  uid varchar(127) DEFAULT NULL,
  access_token varchar(255) DEFAULT NULL,
  access_secret varchar(255) DEFAULT NULL,
  nickname varchar(255) DEFAULT NULL,
  created_at timestamp NOT NULL,
  updated_at timestamp NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT services_user_id_fk FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
CREATE INDEX index_services_on_type_and_uid ON services (type,uid);
CREATE INDEX index_services_on_user_id ON services (user_id);

CREATE TABLE IF NOT EXISTS diaspora.share_visibilities (
  id SERIAL,
  shareable_id integer NOT NULL,
  hidden boolean NOT NULL DEFAULT false,
  shareable_type varchar(60) NOT NULL DEFAULT 'Post',
  user_id integer NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT shareable_and_user_id UNIQUE (shareable_id,shareable_type,user_id),
  CONSTRAINT share_visibilities_user_id_fk FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
CREATE INDEX shareable_and_hidden_and_user_id ON share_visibilities (shareable_id,shareable_type,hidden,user_id);
CREATE INDEX index_post_visibilities_on_post_id ON share_visibilities (shareable_id);
CREATE INDEX index_share_visibilities_on_user_id ON share_visibilities (user_id);

CREATE TABLE IF NOT EXISTS diaspora.signature_orders (
  id SERIAL,
  order varchar(255) NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT index_signature_orders_on_order UNIQUE (order)
);

CREATE TABLE IF NOT EXISTS diaspora.simple_captcha_data (
  id SERIAL,
  key varchar(40) DEFAULT NULL,
  value varchar(12) DEFAULT NULL,
  created_at timestamp DEFAULT NULL,
  updated_at timestamp DEFAULT NULL,
  PRIMARY KEY (id)
);
CREATE INDEX idx_key ON simple_captcha_data (key);

CREATE TABLE IF NOT EXISTS diaspora.tag_followings (
  id SERIAL,
  tag_id integer NOT NULL,
  user_id integer NOT NULL,
  created_at timestamp NOT NULL,
  updated_at timestamp NOT NULL,
  PRIMARY KEY (id),
  CONSTRAINT index_tag_followings_on_tag_id_and_user_id UNIQUE (tag_id,user_id)
);
CREATE INDEX index_tag_followings_on_tag_id ON tag_followings (tag_id);
CREATE INDEX index_tag_followings_on_user_id ON tag_followings (user_id);

CREATE TABLE IF NOT EXISTS diaspora.taggings (
  id SERIAL,
  tag_id integer DEFAULT NULL,
  taggable_id integer DEFAULT NULL,
  taggable_type varchar(127) DEFAULT NULL,
  tagger_id integer DEFAULT NULL,
  tagger_type varchar(127) DEFAULT NULL,
  context varchar(127) DEFAULT NULL,
  created_at timestamp DEFAULT NULL,
  PRIMARY KEY (id),
  CONSTRAINT index_taggings_uniquely UNIQUE (taggable_id,taggable_type,tag_id)
);
CREATE INDEX index_taggings_on_created_at ON taggings (created_at);
CREATE INDEX index_taggings_on_tag_id ON taggings (tag_id);
CREATE INDEX index_taggings_on_taggable_id_and_taggable_type_and_context ON taggings (taggable_id,taggable_type,context);

CREATE TABLE IF NOT EXISTS diaspora.tags (
  id SERIAL,
  name varchar(255) DEFAULT NULL,
  taggings_count integer DEFAULT 0,
  PRIMARY KEY (id),
  CONSTRAINT index_tags_on_name UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS diaspora.user_preferences (
  id SERIAL,
  email_type varchar(255) DEFAULT NULL,
  user_id integer DEFAULT NULL,
  created_at timestamp NOT NULL,
  updated_at timestamp NOT NULL,
  PRIMARY KEY (id)
);
CREATE INDEX index_user_preferences_on_user_id_and_email_type ON user_preferences (user_id,email_type);

CREATE TABLE IF NOT EXISTS diaspora.users (
  id SERIAL,
  username varchar(255) NOT NULL,
  serialized_private_key text,
  getting_started boolean NOT NULL DEFAULT true,
  disable_mail boolean NOT NULL DEFAULT false,
  language varchar(255) DEFAULT NULL,
  email varchar(255) NOT NULL DEFAULT '',
  encrypted_password varchar(255) NOT NULL DEFAULT '',
  reset_password_token varchar(255) DEFAULT NULL,
  remember_created_at timestamp DEFAULT NULL,
  sign_in_count integer DEFAULT 0,
  current_sign_in_at timestamp DEFAULT NULL,
  last_sign_in_at timestamp DEFAULT NULL,
  current_sign_in_ip varchar(255) DEFAULT NULL,
  last_sign_in_ip varchar(255) DEFAULT NULL,
  created_at timestamp NOT NULL,
  updated_at timestamp NOT NULL,
  invited_by_id integer DEFAULT NULL,
  authentication_token varchar(30) DEFAULT NULL,
  unconfirmed_email varchar(255) DEFAULT NULL,
  confirm_email_token varchar(30) DEFAULT NULL,
  locked_at timestamp DEFAULT NULL,
  show_community_spotlight_in_stream boolean NOT NULL DEFAULT true,
  auto_follow_back boolean DEFAULT false,
  auto_follow_back_aspect_id integer DEFAULT NULL,
  hidden_shareables text,
  reset_password_sent_at timestamp DEFAULT NULL,
  last_seen timestamp DEFAULT NULL,
  remove_after timestamp DEFAULT NULL,
  export varchar(255) DEFAULT NULL,
  exported_at timestamp DEFAULT NULL,
  exporting boolean DEFAULT false,
  strip_exif boolean DEFAULT true,
  exported_photos_file varchar(255) DEFAULT NULL,
  exported_photos_at timestamp DEFAULT NULL,
  exporting_photos boolean DEFAULT false,
  color_theme varchar(255) DEFAULT NULL,
  post_default_public boolean DEFAULT false,
  consumed_timestep integer DEFAULT NULL,
  otp_required_for_login boolean DEFAULT NULL,
  otp_backup_codes text,
  plain_otp_secret varchar(255) DEFAULT NULL,
  PRIMARY KEY (id),
  CONSTRAINT index_users_on_email UNIQUE (email),
  CONSTRAINT index_users_on_username UNIQUE (username),
  CONSTRAINT index_users_on_authentication_token UNIQUE (authentication_token)
);
