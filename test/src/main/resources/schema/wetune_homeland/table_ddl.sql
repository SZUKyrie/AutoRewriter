
CREATE TABLE homeland.actions (
    id integer NOT NULL,
    action_type character varying NOT NULL,
    action_option character varying,
    target_type character varying,
    target_id integer,
    user_type character varying,
    user_id integer,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL
);

CREATE SEQUENCE homeland.actions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


CREATE TABLE homeland.ar_internal_metadata (
    key character varying NOT NULL,
    value character varying,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL
);

CREATE TABLE homeland.authorizations (
    id integer NOT NULL,
    provider character varying NOT NULL,
    uid character varying(1000) NOT NULL,
    user_id integer NOT NULL,
    created_at timestamp without time zone,
    updated_at timestamp without time zone
);

CREATE SEQUENCE homeland.authorizations_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


CREATE TABLE homeland.commentable_pages (
    id bigint NOT NULL,
    name character varying,
    user_id integer,
    comments_count integer DEFAULT 0 NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL
);

CREATE SEQUENCE homeland.commentable_pages_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


CREATE TABLE homeland.comments (
    id integer NOT NULL,
    body text NOT NULL,
    user_id integer NOT NULL,
    commentable_type character varying,
    commentable_id integer,
    deleted_at timestamp without time zone,
    created_at timestamp without time zone,
    updated_at timestamp without time zone
);

CREATE SEQUENCE homeland.comments_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


CREATE TABLE homeland.devices (
    id integer NOT NULL,
    platform integer NOT NULL,
    user_id integer NOT NULL,
    token character varying NOT NULL,
    last_actived_at timestamp without time zone,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL
);

CREATE SEQUENCE homeland.devices_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


CREATE TABLE homeland.exception_tracks (
    id integer NOT NULL,
    title character varying,
    body text,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL
);

CREATE SEQUENCE homeland.exception_tracks_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


CREATE TABLE homeland.locations (
    id integer NOT NULL,
    name character varying NOT NULL,
    users_count integer DEFAULT 0 NOT NULL,
    created_at timestamp without time zone,
    updated_at timestamp without time zone
);

CREATE SEQUENCE homeland.locations_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


CREATE TABLE homeland.monkeys (
    id bigint NOT NULL,
    name character varying,
    user_id integer,
    comments_count integer,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL
);

CREATE SEQUENCE homeland.monkeys_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


CREATE TABLE homeland.nodes (
    id integer NOT NULL,
    name character varying NOT NULL,
    summary character varying,
    section_id integer NOT NULL,
    sort integer DEFAULT 0 NOT NULL,
    topics_count integer DEFAULT 0 NOT NULL,
    created_at timestamp without time zone,
    updated_at timestamp without time zone
);

CREATE SEQUENCE homeland.nodes_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


CREATE TABLE homeland.notes (
    id integer NOT NULL,
    title character varying NOT NULL,
    body text NOT NULL,
    user_id integer NOT NULL,
    word_count integer DEFAULT 0 NOT NULL,
    changes_count integer DEFAULT 0 NOT NULL,
    publish boolean DEFAULT false,
    created_at timestamp without time zone,
    updated_at timestamp without time zone
);

CREATE SEQUENCE homeland.notes_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


CREATE TABLE homeland.notifications (
    id integer NOT NULL,
    user_id integer NOT NULL,
    actor_id integer,
    notify_type character varying NOT NULL,
    target_type character varying,
    target_id integer,
    second_target_type character varying,
    second_target_id integer,
    third_target_type character varying,
    third_target_id integer,
    read_at timestamp without time zone,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL
);

CREATE SEQUENCE homeland.notifications_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


CREATE TABLE homeland.oauth_access_grants (
    id integer NOT NULL,
    resource_owner_id integer NOT NULL,
    application_id integer NOT NULL,
    token character varying NOT NULL,
    expires_in bigint,
    redirect_uri text NOT NULL,
    created_at timestamp without time zone NOT NULL,
    revoked_at timestamp without time zone,
    scopes character varying
);

CREATE SEQUENCE homeland.oauth_access_grants_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


CREATE TABLE homeland.oauth_access_tokens (
    id integer NOT NULL,
    resource_owner_id integer,
    application_id integer,
    token character varying NOT NULL,
    refresh_token character varying,
    expires_in bigint,
    revoked_at timestamp without time zone,
    created_at timestamp without time zone NOT NULL,
    scopes character varying
);

CREATE SEQUENCE homeland.oauth_access_tokens_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


CREATE TABLE homeland.oauth_applications (
    id integer NOT NULL,
    name character varying NOT NULL,
    uid character varying NOT NULL,
    secret character varying NOT NULL,
    redirect_uri text NOT NULL,
    scopes character varying DEFAULT ''::character varying NOT NULL,
    created_at timestamp without time zone,
    updated_at timestamp without time zone,
    owner_id integer,
    owner_type character varying,
    level integer DEFAULT 0 NOT NULL,
    confidential boolean DEFAULT true NOT NULL
);

CREATE SEQUENCE homeland.oauth_applications_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


CREATE TABLE homeland.page_versions (
    id integer NOT NULL,
    user_id integer NOT NULL,
    page_id integer NOT NULL,
    version integer DEFAULT 0 NOT NULL,
    slug character varying NOT NULL,
    title character varying NOT NULL,
    "desc" text NOT NULL,
    body text NOT NULL,
    created_at timestamp without time zone,
    updated_at timestamp without time zone
);

CREATE SEQUENCE homeland.page_versions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


CREATE TABLE homeland.photos (
    id integer NOT NULL,
    user_id integer,
    image character varying NOT NULL,
    created_at timestamp without time zone,
    updated_at timestamp without time zone
);

CREATE SEQUENCE homeland.photos_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


CREATE TABLE homeland.replies (
    id integer NOT NULL,
    user_id integer NOT NULL,
    topic_id integer NOT NULL,
    body text NOT NULL,
    state integer DEFAULT 1 NOT NULL,
    likes_count integer DEFAULT 0,
    mentioned_user_ids integer[] DEFAULT '{}'::integer[],
    deleted_at timestamp without time zone,
    created_at timestamp without time zone,
    updated_at timestamp without time zone,
    action character varying,
    target_type character varying,
    target_id character varying,
    reply_to_id integer
);

CREATE SEQUENCE homeland.replies_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


CREATE TABLE homeland.schema_migrations (
    version character varying NOT NULL
);

CREATE TABLE homeland.sections (
    id integer NOT NULL,
    name character varying NOT NULL,
    sort integer DEFAULT 0 NOT NULL,
    created_at timestamp without time zone,
    updated_at timestamp without time zone
);

CREATE SEQUENCE homeland.sections_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


CREATE TABLE homeland.settings (
    id integer NOT NULL,
    var character varying NOT NULL,
    value text,
    thing_id integer,
    thing_type character varying(30),
    created_at timestamp without time zone,
    updated_at timestamp without time zone
);

CREATE SEQUENCE homeland.settings_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


CREATE TABLE homeland.team_users (
    id integer NOT NULL,
    team_id integer NOT NULL,
    user_id integer NOT NULL,
    role integer,
    status integer,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL
);

CREATE SEQUENCE homeland.team_users_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


CREATE TABLE homeland.test_documents (
    id bigint NOT NULL,
    user_id integer,
    reply_to_id integer,
    mentioned_user_ids integer[] DEFAULT '{}'::integer[],
    body text,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL
);

CREATE SEQUENCE homeland.test_documents_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


CREATE TABLE homeland.topics (
    id integer NOT NULL,
    user_id integer NOT NULL,
    node_id integer NOT NULL,
    title character varying NOT NULL,
    body text NOT NULL,
    last_reply_id integer,
    last_reply_user_id integer,
    last_reply_user_login character varying,
    node_name character varying,
    who_deleted character varying,
    last_active_mark integer,
    lock_node boolean DEFAULT false,
    suggested_at timestamp without time zone,
    grade integer DEFAULT 0,
    replied_at timestamp without time zone,
    replies_count integer DEFAULT 0 NOT NULL,
    likes_count integer DEFAULT 0,
    mentioned_user_ids integer[] DEFAULT '{}'::integer[],
    deleted_at timestamp without time zone,
    created_at timestamp without time zone,
    updated_at timestamp without time zone,
    closed_at timestamp without time zone,
    team_id integer
);

CREATE SEQUENCE homeland.topics_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


CREATE TABLE homeland.user_ssos (
    id integer NOT NULL,
    user_id integer NOT NULL,
    uid character varying NOT NULL,
    username character varying,
    email character varying,
    name character varying,
    avatar_url character varying,
    last_payload text NOT NULL,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL
);

CREATE SEQUENCE homeland.user_ssos_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


CREATE TABLE homeland.users (
    id integer NOT NULL,
    login character varying(100) NOT NULL,
    name character varying(100),
    email character varying NOT NULL,
    email_md5 character varying NOT NULL,
    email_public boolean DEFAULT false NOT NULL,
    location character varying,
    location_id integer,
    bio character varying,
    website character varying,
    company character varying,
    github character varying,
    twitter character varying,
    avatar character varying,
    state integer DEFAULT 1 NOT NULL,
    tagline character varying,
    created_at timestamp without time zone,
    updated_at timestamp without time zone,
    encrypted_password character varying DEFAULT ''::character varying NOT NULL,
    reset_password_token character varying,
    reset_password_sent_at timestamp without time zone,
    remember_created_at timestamp without time zone,
    sign_in_count integer DEFAULT 0 NOT NULL,
    current_sign_in_at timestamp without time zone,
    last_sign_in_at timestamp without time zone,
    current_sign_in_ip character varying,
    last_sign_in_ip character varying,
    password_salt character varying DEFAULT ''::character varying NOT NULL,
    persistence_token character varying DEFAULT ''::character varying NOT NULL,
    single_access_token character varying DEFAULT ''::character varying NOT NULL,
    perishable_token character varying DEFAULT ''::character varying NOT NULL,
    topics_count integer DEFAULT 0 NOT NULL,
    replies_count integer DEFAULT 0 NOT NULL,
    follower_ids integer[] DEFAULT '{}'::integer[],
    type character varying(20),
    failed_attempts integer DEFAULT 0 NOT NULL,
    unlock_token character varying,
    locked_at timestamp without time zone,
    team_users_count integer,
    followers_count integer DEFAULT 0,
    following_count integer DEFAULT 0
);

CREATE SEQUENCE homeland.users_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


CREATE TABLE homeland.walking_deads (
    id bigint NOT NULL,
    name character varying,
    tag character varying,
    deleted_at timestamp without time zone,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL
);

CREATE SEQUENCE homeland.walking_deads_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;



























    ADD CONSTRAINT actions_pkey PRIMARY KEY (id);

    ADD CONSTRAINT ar_internal_metadata_pkey PRIMARY KEY (key);

    ADD CONSTRAINT authorizations_pkey PRIMARY KEY (id);

    ADD CONSTRAINT commentable_pages_pkey PRIMARY KEY (id);

    ADD CONSTRAINT comments_pkey PRIMARY KEY (id);

    ADD CONSTRAINT devices_pkey PRIMARY KEY (id);

    ADD CONSTRAINT exception_tracks_pkey PRIMARY KEY (id);

    ADD CONSTRAINT locations_pkey PRIMARY KEY (id);

    ADD CONSTRAINT monkeys_pkey PRIMARY KEY (id);

    ADD CONSTRAINT nodes_pkey PRIMARY KEY (id);

    ADD CONSTRAINT notes_pkey PRIMARY KEY (id);

    ADD CONSTRAINT notifications_pkey PRIMARY KEY (id);

    ADD CONSTRAINT oauth_access_grants_pkey PRIMARY KEY (id);

    ADD CONSTRAINT oauth_access_tokens_pkey PRIMARY KEY (id);

    ADD CONSTRAINT oauth_applications_pkey PRIMARY KEY (id);

    ADD CONSTRAINT page_versions_pkey PRIMARY KEY (id);

    ADD CONSTRAINT photos_pkey PRIMARY KEY (id);

    ADD CONSTRAINT replies_pkey PRIMARY KEY (id);

    ADD CONSTRAINT schema_migrations_pkey PRIMARY KEY (version);

    ADD CONSTRAINT sections_pkey PRIMARY KEY (id);

    ADD CONSTRAINT settings_pkey PRIMARY KEY (id);

    ADD CONSTRAINT team_users_pkey PRIMARY KEY (id);

    ADD CONSTRAINT test_documents_pkey PRIMARY KEY (id);

    ADD CONSTRAINT topics_pkey PRIMARY KEY (id);

    ADD CONSTRAINT user_ssos_pkey PRIMARY KEY (id);

    ADD CONSTRAINT users_pkey PRIMARY KEY (id);

    ADD CONSTRAINT walking_deads_pkey PRIMARY KEY (id);

CREATE INDEX index_actions_on_target_type_and_target_id_and_action_type ON homeland.actions USING btree (target_type, target_id, action_type);

CREATE INDEX index_actions_on_user_type_and_user_id_and_action_type ON homeland.actions USING btree (user_type, user_id, action_type);

CREATE INDEX index_authorizations_on_provider_and_uid ON homeland.authorizations USING btree (provider, uid);

CREATE INDEX index_comments_on_commentable_id ON homeland.comments USING btree (commentable_id);

CREATE INDEX index_comments_on_commentable_type ON homeland.comments USING btree (commentable_type);

CREATE INDEX index_comments_on_user_id ON homeland.comments USING btree (user_id);

CREATE INDEX index_devices_on_user_id ON homeland.devices USING btree (user_id);

CREATE INDEX index_locations_on_name ON homeland.locations USING btree (name);

CREATE INDEX index_nodes_on_section_id ON homeland.nodes USING btree (section_id);

CREATE INDEX index_nodes_on_sort ON homeland.nodes USING btree (sort);

CREATE INDEX index_notes_on_user_id ON homeland.notes USING btree (user_id);

CREATE INDEX index_notifications_on_user_id ON homeland.notifications USING btree (user_id);

CREATE UNIQUE INDEX index_oauth_access_grants_on_token ON homeland.oauth_access_grants USING btree (token);

CREATE UNIQUE INDEX index_oauth_access_tokens_on_refresh_token ON homeland.oauth_access_tokens USING btree (refresh_token);

CREATE INDEX index_oauth_access_tokens_on_resource_owner_id ON homeland.oauth_access_tokens USING btree (resource_owner_id);

CREATE UNIQUE INDEX index_oauth_access_tokens_on_token ON homeland.oauth_access_tokens USING btree (token);

CREATE INDEX index_oauth_applications_on_owner_id_and_owner_type ON homeland.oauth_applications USING btree (owner_id, owner_type);

CREATE UNIQUE INDEX index_oauth_applications_on_uid ON homeland.oauth_applications USING btree (uid);

CREATE INDEX index_page_versions_on_page_id ON homeland.page_versions USING btree (page_id);

CREATE INDEX index_photos_on_user_id ON homeland.photos USING btree (user_id);

CREATE INDEX index_replies_on_deleted_at ON homeland.replies USING btree (deleted_at);

CREATE INDEX index_replies_on_topic_id ON homeland.replies USING btree (topic_id);

CREATE INDEX index_replies_on_user_id ON homeland.replies USING btree (user_id);

CREATE INDEX index_sections_on_sort ON homeland.sections USING btree (sort);

CREATE UNIQUE INDEX index_settings_on_thing_type_and_thing_id_and_var ON homeland.settings USING btree (thing_type, thing_id, var);

CREATE INDEX index_team_users_on_team_id ON homeland.team_users USING btree (team_id);

CREATE INDEX index_team_users_on_user_id ON homeland.team_users USING btree (user_id);

CREATE INDEX index_topics_on_deleted_at ON homeland.topics USING btree (deleted_at);

CREATE INDEX index_topics_on_grade ON homeland.topics USING btree (grade);

CREATE INDEX index_topics_on_last_active_mark ON homeland.topics USING btree (last_active_mark);

CREATE INDEX index_topics_on_last_reply_id ON homeland.topics USING btree (last_reply_id);

CREATE INDEX index_topics_on_likes_count ON homeland.topics USING btree (likes_count);

CREATE INDEX index_topics_on_node_id_and_deleted_at ON homeland.topics USING btree (node_id, deleted_at);

CREATE INDEX index_topics_on_suggested_at ON homeland.topics USING btree (suggested_at);

CREATE INDEX index_topics_on_team_id ON homeland.topics USING btree (team_id);

CREATE INDEX index_topics_on_user_id ON homeland.topics USING btree (user_id);

CREATE UNIQUE INDEX index_user_ssos_on_uid ON homeland.user_ssos USING btree (uid);

CREATE UNIQUE INDEX index_users_on_email ON homeland.users USING btree (email);

CREATE INDEX index_users_on_location ON homeland.users USING btree (location);

CREATE UNIQUE INDEX index_users_on_login ON homeland.users USING btree (login);

CREATE INDEX index_users_on_lower_login_varchar_pattern_ops ON homeland.users USING btree (lower((login)::text) varchar_pattern_ops);

CREATE INDEX index_users_on_lower_name_varchar_pattern_ops ON homeland.users USING btree (lower((name)::text) varchar_pattern_ops);

CREATE UNIQUE INDEX index_users_on_unlock_token ON homeland.users USING btree (unlock_token);
