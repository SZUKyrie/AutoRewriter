










CREATE
    EXTENSION IF NOT EXISTS hstore WITH SCHEMA public;

CREATE
    EXTENSION IF NOT EXISTS pg_trgm WITH SCHEMA public;



CREATE TABLE discourse.anonymous_users
(
    id             bigint                      NOT NULL,
    user_id        integer                     NOT NULL,
    master_user_id integer                     NOT NULL,
    active         boolean                     NOT NULL,
    created_at     timestamp without time zone NOT NULL,
    updated_at     timestamp without time zone NOT NULL
);

CREATE
    SEQUENCE discourse.anonymous_users_id_seq
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.anonymous_users_id_seq OWNED BY discourse.anonymous_users.id;

CREATE TABLE discourse.api_keys
(
    id            integer                     NOT NULL,
    user_id       integer,
    created_by_id integer,
    created_at    timestamp without time zone NOT NULL,
    updated_at    timestamp without time zone NOT NULL,
    allowed_ips   inet[],
    hidden        boolean DEFAULT false       NOT NULL,
    last_used_at  timestamp without time zone,
    revoked_at    timestamp without time zone,
    description   text,
    key_hash      character varying           NOT NULL,
    truncated_key character varying           NOT NULL
);

CREATE
    SEQUENCE discourse.api_keys_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.api_keys_id_seq OWNED BY discourse.api_keys.id;

CREATE TABLE discourse.application_requests
(
    id       integer           NOT NULL,
    date     date              NOT NULL,
    req_type integer           NOT NULL,
    count    integer DEFAULT 0 NOT NULL
);

CREATE
    SEQUENCE discourse.application_requests_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.application_requests_id_seq OWNED BY discourse.application_requests.id;

CREATE TABLE discourse.ar_internal_metadata
(
    key        character varying              NOT NULL,
    value      character varying,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL
);

CREATE TABLE discourse.backup_draft_posts
(
    id         bigint                         NOT NULL,
    user_id    integer                        NOT NULL,
    post_id    integer                        NOT NULL,
    key        character varying              NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL
);

CREATE
    SEQUENCE discourse.backup_draft_posts_id_seq
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.backup_draft_posts_id_seq OWNED BY discourse.backup_draft_posts.id;

CREATE TABLE discourse.backup_draft_topics
(
    id         bigint                         NOT NULL,
    user_id    integer                        NOT NULL,
    topic_id   integer                        NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL
);

CREATE
    SEQUENCE discourse.backup_draft_topics_id_seq
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.backup_draft_topics_id_seq OWNED BY discourse.backup_draft_topics.id;

CREATE TABLE discourse.backup_metadata
(
    id    bigint            NOT NULL,
    name  character varying NOT NULL,
    value character varying
);

CREATE
    SEQUENCE discourse.backup_metadata_id_seq
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.backup_metadata_id_seq OWNED BY discourse.backup_metadata.id;

CREATE TABLE discourse.badge_groupings
(
    id          integer                     NOT NULL,
    name        character varying           NOT NULL,
    description text,
    "position"  integer                     NOT NULL,
    created_at  timestamp without time zone NOT NULL,
    updated_at  timestamp without time zone NOT NULL
);

CREATE
    SEQUENCE discourse.badge_groupings_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.badge_groupings_id_seq OWNED BY discourse.badge_groupings.id;

CREATE TABLE discourse.categories
(
    id                                integer                                                    NOT NULL,
    name                              character varying(50)                                      NOT NULL,
    color                             character varying(6)  DEFAULT '0088CC':: character varying NOT NULL,
    topic_id                          integer,
    topic_count                       integer               DEFAULT 0                            NOT NULL,
    created_at                        timestamp without time zone                                NOT NULL,
    updated_at                        timestamp without time zone                                NOT NULL,
    user_id                           integer                                                    NOT NULL,
    topics_year                       integer               DEFAULT 0,
    topics_month                      integer               DEFAULT 0,
    topics_week                       integer               DEFAULT 0,
    slug                              character varying                                          NOT NULL,
    description                       text,
    text_color                        character varying(6)  DEFAULT 'FFFFFF':: character varying NOT NULL,
    read_restricted                   boolean               DEFAULT false                        NOT NULL,
    auto_close_hours                  double precision,
    post_count                        integer               DEFAULT 0                            NOT NULL,
    latest_post_id                    integer,
    latest_topic_id                   integer,
    "position"                        integer,
    parent_category_id                integer,
    posts_year                        integer               DEFAULT 0,
    posts_month                       integer               DEFAULT 0,
    posts_week                        integer               DEFAULT 0,
    email_in                          character varying,
    email_in_allow_strangers          boolean               DEFAULT false,
    topics_day                        integer               DEFAULT 0,
    posts_day                         integer               DEFAULT 0,
    allow_badges                      boolean               DEFAULT true                         NOT NULL,
    name_lower                        character varying(50)                                      NOT NULL,
    auto_close_based_on_last_post     boolean               DEFAULT false,
    topic_template                    text,
    contains_messages                 boolean,
    sort_order                        character varying,
    sort_ascending                    boolean,
    uploaded_logo_id                  integer,
    uploaded_background_id            integer,
    topic_featured_link_allowed       boolean               DEFAULT true,
    all_topics_wiki                   boolean               DEFAULT false                        NOT NULL,
    show_subcategory_list             boolean               DEFAULT false,
    num_featured_topics               integer               DEFAULT 3,
    default_view                      character varying(50),
    subcategory_list_style            character varying(50) DEFAULT 'rows_with_featured_topics':: character varying,
    default_top_period                character varying(20) DEFAULT 'all':: character varying,
    mailinglist_mirror                boolean               DEFAULT false                        NOT NULL,
    minimum_required_tags             integer               DEFAULT 0                            NOT NULL,
    navigate_to_first_post_after_read boolean               DEFAULT false                        NOT NULL,
    search_priority                   integer               DEFAULT 0,
    allow_global_tags                 boolean               DEFAULT false                        NOT NULL,
    reviewable_by_group_id            integer,
    required_tag_group_id             integer,
    min_tags_from_required_group      integer               DEFAULT 1                            NOT NULL
);

CREATE TABLE discourse.posts
(
    id                      integer                        NOT NULL,
    user_id                 integer,
    topic_id                integer                        NOT NULL,
    post_number             integer                        NOT NULL,
    raw                     text                           NOT NULL,
    cooked                  text                           NOT NULL,
    created_at              timestamp without time zone    NOT NULL,
    updated_at              timestamp without time zone    NOT NULL,
    reply_to_post_number    integer,
    reply_count             integer          DEFAULT 0     NOT NULL,
    quote_count             integer          DEFAULT 0     NOT NULL,
    deleted_at              timestamp without time zone,
    off_topic_count         integer          DEFAULT 0     NOT NULL,
    like_count              integer          DEFAULT 0     NOT NULL,
    incoming_link_count     integer          DEFAULT 0     NOT NULL,
    bookmark_count          integer          DEFAULT 0     NOT NULL,
    avg_time                integer,
    score                   double precision,
    reads                   integer          DEFAULT 0     NOT NULL,
    post_type               integer          DEFAULT 1     NOT NULL,
    sort_order              integer,
    last_editor_id          integer,
    hidden                  boolean          DEFAULT false NOT NULL,
    hidden_reason_id        integer,
    notify_moderators_count integer          DEFAULT 0     NOT NULL,
    spam_count              integer          DEFAULT 0     NOT NULL,
    illegal_count           integer          DEFAULT 0     NOT NULL,
    inappropriate_count     integer          DEFAULT 0     NOT NULL,
    last_version_at         timestamp without time zone    NOT NULL,
    user_deleted            boolean          DEFAULT false NOT NULL,
    reply_to_user_id        integer,
    percent_rank            double precision DEFAULT 1.0,
    notify_user_count       integer          DEFAULT 0     NOT NULL,
    like_score              integer          DEFAULT 0     NOT NULL,
    deleted_by_id           integer,
    edit_reason             character varying,
    word_count              integer,
    version                 integer          DEFAULT 1     NOT NULL,
    cook_method             integer          DEFAULT 1     NOT NULL,
    wiki                    boolean          DEFAULT false NOT NULL,
    baked_at                timestamp without time zone,
    baked_version           integer,
    hidden_at               timestamp without time zone,
    self_edits              integer          DEFAULT 0     NOT NULL,
    reply_quoted            boolean          DEFAULT false NOT NULL,
    via_email               boolean          DEFAULT false NOT NULL,
    raw_email               text,
    public_version          integer          DEFAULT 1     NOT NULL,
    action_code             character varying,
    image_url               character varying,
    locked_by_id            integer
);

CREATE TABLE discourse.topics
(
    id                        integer                                                 NOT NULL,
    title                     character varying                                       NOT NULL,
    last_posted_at            timestamp without time zone,
    created_at                timestamp without time zone                             NOT NULL,
    updated_at                timestamp without time zone                             NOT NULL,
    views                     integer           DEFAULT 0                             NOT NULL,
    posts_count               integer           DEFAULT 0                             NOT NULL,
    user_id                   integer,
    last_post_user_id         integer                                                 NOT NULL,
    reply_count               integer           DEFAULT 0                             NOT NULL,
    featured_user1_id         integer,
    featured_user2_id         integer,
    featured_user3_id         integer,
    avg_time                  integer,
    deleted_at                timestamp without time zone,
    highest_post_number       integer           DEFAULT 0                             NOT NULL,
    image_url                 character varying,
    like_count                integer           DEFAULT 0                             NOT NULL,
    incoming_link_count       integer           DEFAULT 0                             NOT NULL,
    category_id               integer,
    visible                   boolean           DEFAULT true                          NOT NULL,
    moderator_posts_count     integer           DEFAULT 0                             NOT NULL,
    closed                    boolean           DEFAULT false                         NOT NULL,
    archived                  boolean           DEFAULT false                         NOT NULL,
    bumped_at                 timestamp without time zone                             NOT NULL,
    has_summary               boolean           DEFAULT false                         NOT NULL,
    archetype                 character varying DEFAULT 'regular':: character varying NOT NULL,
    featured_user4_id         integer,
    notify_moderators_count   integer           DEFAULT 0                             NOT NULL,
    spam_count                integer           DEFAULT 0                             NOT NULL,
    pinned_at                 timestamp without time zone,
    score                     double precision,
    percent_rank              double precision  DEFAULT 1.0                           NOT NULL,
    subtype                   character varying,
    slug                      character varying,
    deleted_by_id             integer,
    participant_count         integer           DEFAULT 1,
    word_count                integer,
    excerpt                   character varying(1000),
    pinned_globally           boolean           DEFAULT false                         NOT NULL,
    pinned_until              timestamp without time zone,
    fancy_title               character varying(400),
    highest_staff_post_number integer           DEFAULT 0                             NOT NULL,
    featured_link             character varying,
    reviewable_score          double precision  DEFAULT 0.0                           NOT NULL,
    CONSTRAINT has_category_id CHECK (((category_id IS NOT NULL) OR
                                       ((archetype):: text <> 'regular':: text))),
    CONSTRAINT pm_has_no_category CHECK (((category_id IS NULL) OR
                                          ((archetype):: text <> 'private_message':: text)))
);

CREATE VIEW discourse.badge_posts AS
SELECT p.id,
       p.user_id,
       p.topic_id,
       p.post_number,
       p.raw,
       p.cooked,
       p.created_at,
       p.updated_at,
       p.reply_to_post_number,
       p.reply_count,
       p.quote_count,
       p.deleted_at,
       p.off_topic_count,
       p.like_count,
       p.incoming_link_count,
       p.bookmark_count,
       p.avg_time,
       p.score,
       p.reads,
       p.post_type,
       p.sort_order,
       p.last_editor_id,
       p.hidden,
       p.hidden_reason_id,
       p.notify_moderators_count,
       p.spam_count,
       p.illegal_count,
       p.inappropriate_count,
       p.last_version_at,
       p.user_deleted,
       p.reply_to_user_id,
       p.percent_rank,
       p.notify_user_count,
       p.like_score,
       p.deleted_by_id,
       p.edit_reason,
       p.word_count,
       p.version,
       p.cook_method,
       p.wiki,
       p.baked_at,
       p.baked_version,
       p.hidden_at,
       p.self_edits,
       p.reply_quoted,
       p.via_email,
       p.raw_email,
       p.public_version,
       p.action_code,
       p.image_url,
       p.locked_by_id
FROM ((discourse.posts p
    JOIN discourse.topics t ON ((t.id = p.topic_id)))
         JOIN discourse.categories c ON ((c.id = t.category_id)))
WHERE (c.allow_badges AND (p.deleted_at IS NULL) AND (t.deleted_at IS NULL) AND
       (NOT c.read_restricted) AND t.visible AND (p.post_type = ANY (ARRAY [1, 2, 3])));

CREATE TABLE discourse.badge_types
(
    id         integer                     NOT NULL,
    name       character varying           NOT NULL,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL
);

CREATE
    SEQUENCE discourse.badge_types_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.badge_types_id_seq OWNED BY discourse.badge_types.id;

CREATE TABLE discourse.badges
(
    id                integer                         NOT NULL,
    name              character varying               NOT NULL,
    description       text,
    badge_type_id     integer                         NOT NULL,
    grant_count       integer           DEFAULT 0     NOT NULL,
    created_at        timestamp without time zone     NOT NULL,
    updated_at        timestamp without time zone     NOT NULL,
    allow_title       boolean           DEFAULT false NOT NULL,
    multiple_grant    boolean           DEFAULT false NOT NULL,
    icon              character varying DEFAULT 'fa-certificate':: character varying,
    listable          boolean           DEFAULT true,
    target_posts      boolean           DEFAULT false,
    query             text,
    enabled           boolean           DEFAULT true  NOT NULL,
    auto_revoke       boolean           DEFAULT true  NOT NULL,
    badge_grouping_id integer           DEFAULT 5     NOT NULL,
    trigger           integer,
    show_posts        boolean           DEFAULT false NOT NULL,
    system            boolean           DEFAULT false NOT NULL,
    image             character varying(255),
    long_description  text
);

CREATE
    SEQUENCE discourse.badges_id_seq
    AS integer
    START
        WITH 100
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.badges_id_seq OWNED BY discourse.badges.id;

CREATE TABLE discourse.bookmarks
(
    id                    bigint                         NOT NULL,
    user_id               bigint                         NOT NULL,
    topic_id              bigint                         NOT NULL,
    post_id               bigint                         NOT NULL,
    name                  character varying,
    reminder_type         integer,
    reminder_at           timestamp without time zone,
    created_at            timestamp(6) without time zone NOT NULL,
    updated_at            timestamp(6) without time zone NOT NULL,
    reminder_last_sent_at timestamp without time zone,
    reminder_set_at       timestamp without time zone
);

CREATE
    SEQUENCE discourse.bookmarks_id_seq
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.bookmarks_id_seq OWNED BY discourse.bookmarks.id;

CREATE
    SEQUENCE discourse.categories_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.categories_id_seq OWNED BY discourse.categories.id;

CREATE TABLE discourse.categories_web_hooks
(
    web_hook_id integer NOT NULL,
    category_id integer NOT NULL
);

CREATE TABLE discourse.category_custom_fields
(
    id          integer                     NOT NULL,
    category_id integer                     NOT NULL,
    name        character varying(256)      NOT NULL,
    value       text,
    created_at  timestamp without time zone NOT NULL,
    updated_at  timestamp without time zone NOT NULL
);

CREATE
    SEQUENCE discourse.category_custom_fields_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.category_custom_fields_id_seq OWNED BY discourse.category_custom_fields.id;

CREATE TABLE discourse.category_featured_topics
(
    category_id integer                     NOT NULL,
    topic_id    integer                     NOT NULL,
    created_at  timestamp without time zone NOT NULL,
    updated_at  timestamp without time zone NOT NULL,
    rank        integer DEFAULT 0           NOT NULL,
    id          integer                     NOT NULL
);

CREATE
    SEQUENCE discourse.category_featured_topics_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.category_featured_topics_id_seq OWNED BY discourse.category_featured_topics.id;

CREATE TABLE discourse.category_groups
(
    id              integer                     NOT NULL,
    category_id     integer                     NOT NULL,
    group_id        integer                     NOT NULL,
    created_at      timestamp without time zone NOT NULL,
    updated_at      timestamp without time zone NOT NULL,
    permission_type integer DEFAULT 1
);

CREATE
    SEQUENCE discourse.category_groups_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.category_groups_id_seq OWNED BY discourse.category_groups.id;

CREATE TABLE discourse.category_search_data
(
    category_id integer NOT NULL,
    search_data tsvector,
    raw_data    text,
    locale      text,
    version     integer DEFAULT 0
);

CREATE TABLE discourse.category_tag_groups
(
    id           integer                     NOT NULL,
    category_id  integer                     NOT NULL,
    tag_group_id integer                     NOT NULL,
    created_at   timestamp without time zone NOT NULL,
    updated_at   timestamp without time zone NOT NULL
);

CREATE
    SEQUENCE discourse.category_tag_groups_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.category_tag_groups_id_seq OWNED BY discourse.category_tag_groups.id;

CREATE TABLE discourse.category_tag_stats
(
    id          bigint            NOT NULL,
    category_id bigint            NOT NULL,
    tag_id      bigint            NOT NULL,
    topic_count integer DEFAULT 0 NOT NULL
);

CREATE
    SEQUENCE discourse.category_tag_stats_id_seq
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.category_tag_stats_id_seq OWNED BY discourse.category_tag_stats.id;

CREATE TABLE discourse.category_tags
(
    id          integer                     NOT NULL,
    category_id integer                     NOT NULL,
    tag_id      integer                     NOT NULL,
    created_at  timestamp without time zone NOT NULL,
    updated_at  timestamp without time zone NOT NULL
);

CREATE
    SEQUENCE discourse.category_tags_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.category_tags_id_seq OWNED BY discourse.category_tags.id;

CREATE TABLE discourse.category_users
(
    id                 integer NOT NULL,
    category_id        integer NOT NULL,
    user_id            integer NOT NULL,
    notification_level integer,
    last_seen_at       timestamp without time zone
);

CREATE
    SEQUENCE discourse.category_users_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.category_users_id_seq OWNED BY discourse.category_users.id;

CREATE TABLE discourse.child_themes
(
    id              integer                     NOT NULL,
    parent_theme_id integer,
    child_theme_id  integer,
    created_at      timestamp without time zone NOT NULL,
    updated_at      timestamp without time zone NOT NULL
);

CREATE
    SEQUENCE discourse.child_themes_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.child_themes_id_seq OWNED BY discourse.child_themes.id;

CREATE TABLE discourse.color_scheme_colors
(
    id              integer                     NOT NULL,
    name            character varying           NOT NULL,
    hex             character varying           NOT NULL,
    color_scheme_id integer                     NOT NULL,
    created_at      timestamp without time zone NOT NULL,
    updated_at      timestamp without time zone NOT NULL
);

CREATE
    SEQUENCE discourse.color_scheme_colors_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.color_scheme_colors_id_seq OWNED BY discourse.color_scheme_colors.id;

CREATE TABLE discourse.color_schemes
(
    id             integer                     NOT NULL,
    name           character varying           NOT NULL,
    version        integer DEFAULT 1           NOT NULL,
    created_at     timestamp without time zone NOT NULL,
    updated_at     timestamp without time zone NOT NULL,
    via_wizard     boolean DEFAULT false       NOT NULL,
    base_scheme_id character varying,
    theme_id       integer
);

CREATE
    SEQUENCE discourse.color_schemes_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.color_schemes_id_seq OWNED BY discourse.color_schemes.id;

CREATE TABLE discourse.custom_emojis
(
    id         integer                     NOT NULL,
    name       character varying           NOT NULL,
    upload_id  integer                     NOT NULL,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL
);

CREATE
    SEQUENCE discourse.custom_emojis_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.custom_emojis_id_seq OWNED BY discourse.custom_emojis.id;

CREATE TABLE discourse.developers
(
    id      integer NOT NULL,
    user_id integer NOT NULL
);

CREATE
    SEQUENCE discourse.developers_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.developers_id_seq OWNED BY discourse.developers.id;

CREATE TABLE discourse.directory_items
(
    id             integer           NOT NULL,
    period_type    integer           NOT NULL,
    user_id        integer           NOT NULL,
    likes_received integer           NOT NULL,
    likes_given    integer           NOT NULL,
    topics_entered integer           NOT NULL,
    topic_count    integer           NOT NULL,
    post_count     integer           NOT NULL,
    created_at     timestamp without time zone,
    updated_at     timestamp without time zone,
    days_visited   integer DEFAULT 0 NOT NULL,
    posts_read     integer DEFAULT 0 NOT NULL
);

CREATE
    SEQUENCE discourse.directory_items_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.directory_items_id_seq OWNED BY discourse.directory_items.id;

CREATE TABLE discourse.draft_sequences
(
    id        integer           NOT NULL,
    user_id   integer           NOT NULL,
    draft_key character varying NOT NULL,
    sequence  integer           NOT NULL
);

CREATE
    SEQUENCE discourse.draft_sequences_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.draft_sequences_id_seq OWNED BY discourse.draft_sequences.id;

CREATE TABLE discourse.drafts
(
    id         integer                     NOT NULL,
    user_id    integer                     NOT NULL,
    draft_key  character varying           NOT NULL,
    data       text                        NOT NULL,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL,
    sequence   integer DEFAULT 0           NOT NULL,
    revisions  integer DEFAULT 1           NOT NULL,
    owner      character varying
);

CREATE
    SEQUENCE discourse.drafts_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.drafts_id_seq OWNED BY discourse.drafts.id;

CREATE TABLE discourse.email_change_requests
(
    id                 integer                     NOT NULL,
    user_id            integer                     NOT NULL,
    old_email          character varying           NOT NULL,
    new_email          character varying           NOT NULL,
    old_email_token_id integer,
    new_email_token_id integer,
    change_state       integer                     NOT NULL,
    created_at         timestamp without time zone NOT NULL,
    updated_at         timestamp without time zone NOT NULL
);

CREATE
    SEQUENCE discourse.email_change_requests_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.email_change_requests_id_seq OWNED BY discourse.email_change_requests.id;

CREATE TABLE discourse.email_logs
(
    id         integer                     NOT NULL,
    to_address character varying           NOT NULL,
    email_type character varying           NOT NULL,
    user_id    integer,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL,
    post_id    integer,
    bounce_key uuid,
    bounced    boolean DEFAULT false       NOT NULL,
    message_id character varying
);

CREATE
    SEQUENCE discourse.email_logs_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.email_logs_id_seq OWNED BY discourse.email_logs.id;

CREATE TABLE discourse.email_tokens
(
    id         integer                     NOT NULL,
    user_id    integer                     NOT NULL,
    email      character varying           NOT NULL,
    token      character varying           NOT NULL,
    confirmed  boolean DEFAULT false       NOT NULL,
    expired    boolean DEFAULT false       NOT NULL,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL
);

CREATE
    SEQUENCE discourse.email_tokens_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.email_tokens_id_seq OWNED BY discourse.email_tokens.id;

CREATE TABLE discourse.embeddable_hosts
(
    id             integer                     NOT NULL,
    host           character varying           NOT NULL,
    category_id    integer                     NOT NULL,
    created_at     timestamp without time zone NOT NULL,
    updated_at     timestamp without time zone NOT NULL,
    path_whitelist character varying,
    class_name     character varying
);

CREATE
    SEQUENCE discourse.embeddable_hosts_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.embeddable_hosts_id_seq OWNED BY discourse.embeddable_hosts.id;

CREATE TABLE discourse.github_user_infos
(
    id             integer                     NOT NULL,
    user_id        integer                     NOT NULL,
    screen_name    character varying           NOT NULL,
    github_user_id integer                     NOT NULL,
    created_at     timestamp without time zone NOT NULL,
    updated_at     timestamp without time zone NOT NULL
);

CREATE
    SEQUENCE discourse.github_user_infos_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.github_user_infos_id_seq OWNED BY discourse.github_user_infos.id;

CREATE TABLE discourse.given_daily_likes
(
    user_id       integer               NOT NULL,
    likes_given   integer               NOT NULL,
    given_date    date                  NOT NULL,
    limit_reached boolean DEFAULT false NOT NULL
);

CREATE TABLE discourse.group_archived_messages
(
    id         integer                     NOT NULL,
    group_id   integer                     NOT NULL,
    topic_id   integer                     NOT NULL,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL
);

CREATE
    SEQUENCE discourse.group_archived_messages_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.group_archived_messages_id_seq OWNED BY discourse.group_archived_messages.id;

CREATE TABLE discourse.group_custom_fields
(
    id         integer                     NOT NULL,
    group_id   integer                     NOT NULL,
    name       character varying(256)      NOT NULL,
    value      text,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL
);

CREATE
    SEQUENCE discourse.group_custom_fields_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.group_custom_fields_id_seq OWNED BY discourse.group_custom_fields.id;

CREATE TABLE discourse.group_histories
(
    id             integer                     NOT NULL,
    group_id       integer                     NOT NULL,
    acting_user_id integer                     NOT NULL,
    target_user_id integer,
    action         integer                     NOT NULL,
    subject        character varying,
    prev_value     text,
    new_value      text,
    created_at     timestamp without time zone NOT NULL,
    updated_at     timestamp without time zone NOT NULL
);

CREATE
    SEQUENCE discourse.group_histories_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.group_histories_id_seq OWNED BY discourse.group_histories.id;

CREATE TABLE discourse.group_mentions
(
    id         integer                     NOT NULL,
    post_id    integer,
    group_id   integer,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL
);

CREATE
    SEQUENCE discourse.group_mentions_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.group_mentions_id_seq OWNED BY discourse.group_mentions.id;

CREATE TABLE discourse.group_requests
(
    id         bigint                      NOT NULL,
    group_id   integer,
    user_id    integer,
    reason     text,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL
);

CREATE
    SEQUENCE discourse.group_requests_id_seq
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.group_requests_id_seq OWNED BY discourse.group_requests.id;

CREATE TABLE discourse.group_users
(
    id                 integer                     NOT NULL,
    group_id           integer                     NOT NULL,
    user_id            integer                     NOT NULL,
    created_at         timestamp without time zone NOT NULL,
    updated_at         timestamp without time zone NOT NULL,
    owner              boolean DEFAULT false       NOT NULL,
    notification_level integer DEFAULT 2           NOT NULL
);

CREATE
    SEQUENCE discourse.group_users_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.group_users_id_seq OWNED BY discourse.group_users.id;

CREATE TABLE discourse.groups
(
    id                                 integer                     NOT NULL,
    name                               character varying           NOT NULL,
    created_at                         timestamp without time zone NOT NULL,
    updated_at                         timestamp without time zone NOT NULL,
    automatic                          boolean DEFAULT false       NOT NULL,
    user_count                         integer DEFAULT 0           NOT NULL,
    automatic_membership_email_domains text,
    automatic_membership_retroactive   boolean DEFAULT false,
    primary_group                      boolean DEFAULT false       NOT NULL,
    title                              character varying,
    grant_trust_level                  integer,
    incoming_email                     character varying,
    has_messages                       boolean DEFAULT false       NOT NULL,
    flair_url                          character varying,
    flair_bg_color                     character varying,
    flair_color                        character varying,
    bio_raw                            text,
    bio_cooked                         text,
    allow_membership_requests          boolean DEFAULT false       NOT NULL,
    full_name                          character varying,
    default_notification_level         integer DEFAULT 3           NOT NULL,
    visibility_level                   integer DEFAULT 0           NOT NULL,
    public_exit                        boolean DEFAULT false       NOT NULL,
    public_admission                   boolean DEFAULT false       NOT NULL,
    membership_request_template        text,
    messageable_level                  integer DEFAULT 0,
    mentionable_level                  integer DEFAULT 0,
    publish_read_state                 boolean DEFAULT false       NOT NULL,
    members_visibility_level           integer DEFAULT 0           NOT NULL
);

CREATE
    SEQUENCE discourse.groups_id_seq
    AS integer
    START
        WITH 100
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.groups_id_seq OWNED BY discourse.groups.id;

CREATE TABLE discourse.groups_web_hooks
(
    web_hook_id integer NOT NULL,
    group_id    integer NOT NULL
);

CREATE TABLE discourse.ignored_users
(
    id              bigint                      NOT NULL,
    user_id         integer                     NOT NULL,
    ignored_user_id integer                     NOT NULL,
    created_at      timestamp without time zone NOT NULL,
    updated_at      timestamp without time zone NOT NULL,
    summarized_at   timestamp without time zone,
    expiring_at     timestamp without time zone
);

CREATE
    SEQUENCE discourse.ignored_users_id_seq
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.ignored_users_id_seq OWNED BY discourse.ignored_users.id;

CREATE TABLE discourse.incoming_domains
(
    id    integer                NOT NULL,
    name  character varying(100) NOT NULL,
    https boolean DEFAULT false  NOT NULL,
    port  integer                NOT NULL
);

CREATE
    SEQUENCE discourse.incoming_domains_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.incoming_domains_id_seq OWNED BY discourse.incoming_domains.id;

CREATE TABLE discourse.incoming_emails
(
    id                integer                     NOT NULL,
    user_id           integer,
    topic_id          integer,
    post_id           integer,
    raw               text,
    error             text,
    message_id        text,
    from_address      text,
    to_addresses      text,
    cc_addresses      text,
    subject           text,
    created_at        timestamp without time zone NOT NULL,
    updated_at        timestamp without time zone NOT NULL,
    rejection_message text,
    is_auto_generated boolean DEFAULT false,
    is_bounce         boolean DEFAULT false       NOT NULL
);

CREATE
    SEQUENCE discourse.incoming_emails_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.incoming_emails_id_seq OWNED BY discourse.incoming_emails.id;

CREATE TABLE discourse.incoming_links
(
    id                  integer                     NOT NULL,
    created_at          timestamp without time zone NOT NULL,
    user_id             integer,
    ip_address          inet,
    current_user_id     integer,
    post_id             integer                     NOT NULL,
    incoming_referer_id integer
);

CREATE
    SEQUENCE discourse.incoming_links_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.incoming_links_id_seq OWNED BY discourse.incoming_links.id;

CREATE TABLE discourse.incoming_referers
(
    id                 integer                 NOT NULL,
    path               character varying(1000) NOT NULL,
    incoming_domain_id integer                 NOT NULL
);

CREATE
    SEQUENCE discourse.incoming_referers_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.incoming_referers_id_seq OWNED BY discourse.incoming_referers.id;

CREATE TABLE discourse.invited_groups
(
    id         integer                     NOT NULL,
    group_id   integer,
    invite_id  integer,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL
);

CREATE
    SEQUENCE discourse.invited_groups_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.invited_groups_id_seq OWNED BY discourse.invited_groups.id;

CREATE TABLE discourse.invites
(
    id             integer                     NOT NULL,
    invite_key     character varying(32)       NOT NULL,
    email          character varying,
    invited_by_id  integer                     NOT NULL,
    user_id        integer,
    redeemed_at    timestamp without time zone,
    created_at     timestamp without time zone NOT NULL,
    updated_at     timestamp without time zone NOT NULL,
    deleted_at     timestamp without time zone,
    deleted_by_id  integer,
    invalidated_at timestamp without time zone,
    moderator      boolean DEFAULT false       NOT NULL,
    custom_message text,
    emailed_status integer
);

CREATE
    SEQUENCE discourse.invites_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.invites_id_seq OWNED BY discourse.invites.id;

CREATE TABLE discourse.javascript_caches
(
    id             bigint                      NOT NULL,
    theme_field_id bigint,
    digest         character varying,
    content        text                        NOT NULL,
    created_at     timestamp without time zone NOT NULL,
    updated_at     timestamp without time zone NOT NULL,
    theme_id       bigint
);
-- , CONSTRAINT enforce_theme_or_theme_field CHECK ((((theme_id IS NOT NULL) AND (theme_field_id IS NULL)) OR ((theme_id IS NULL) AND (theme_field_id IS NOT NULL))))

CREATE
    SEQUENCE discourse.javascript_caches_id_seq
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.javascript_caches_id_seq OWNED BY discourse.javascript_caches.id;

CREATE TABLE discourse.message_bus
(
    id         integer                     NOT NULL,
    name       character varying,
    context    character varying,
    data       text,
    created_at timestamp without time zone NOT NULL
);

CREATE
    SEQUENCE discourse.message_bus_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.message_bus_id_seq OWNED BY discourse.message_bus.id;

CREATE TABLE discourse.muted_users
(
    id            integer                     NOT NULL,
    user_id       integer                     NOT NULL,
    muted_user_id integer                     NOT NULL,
    created_at    timestamp without time zone NOT NULL,
    updated_at    timestamp without time zone NOT NULL
);

CREATE
    SEQUENCE discourse.muted_users_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.muted_users_id_seq OWNED BY discourse.muted_users.id;

CREATE TABLE discourse.notifications
(
    id                integer                     NOT NULL,
    notification_type integer                     NOT NULL,
    user_id           integer                     NOT NULL,
    data              character varying(1000)     NOT NULL,
    read              boolean DEFAULT false       NOT NULL,
    created_at        timestamp without time zone NOT NULL,
    updated_at        timestamp without time zone NOT NULL,
    topic_id          integer,
    post_number       integer,
    post_action_id    integer
);

CREATE
    SEQUENCE discourse.notifications_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.notifications_id_seq OWNED BY discourse.notifications.id;

CREATE TABLE discourse.oauth2_user_infos
(
    id         integer                     NOT NULL,
    user_id    integer                     NOT NULL,
    uid        character varying           NOT NULL,
    provider   character varying           NOT NULL,
    email      character varying,
    name       character varying,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL
);

CREATE
    SEQUENCE discourse.oauth2_user_infos_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.oauth2_user_infos_id_seq OWNED BY discourse.oauth2_user_infos.id;

CREATE TABLE discourse.onceoff_logs
(
    id         integer                     NOT NULL,
    job_name   character varying,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL
);

CREATE
    SEQUENCE discourse.onceoff_logs_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.onceoff_logs_id_seq OWNED BY discourse.onceoff_logs.id;

CREATE TABLE discourse.optimized_images
(
    id        integer               NOT NULL,
    sha1      character varying(40) NOT NULL,
    extension character varying(10) NOT NULL,
    width     integer               NOT NULL,
    height    integer               NOT NULL,
    upload_id integer               NOT NULL,
    url       character varying     NOT NULL,
    filesize  integer,
    etag      character varying,
    version   integer
);

CREATE
    SEQUENCE discourse.optimized_images_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.optimized_images_id_seq OWNED BY discourse.optimized_images.id;

CREATE TABLE discourse.permalinks
(
    id           integer                     NOT NULL,
    url          character varying(1000)     NOT NULL,
    topic_id     integer,
    post_id      integer,
    category_id  integer,
    created_at   timestamp without time zone NOT NULL,
    updated_at   timestamp without time zone NOT NULL,
    external_url character varying(1000)
);

CREATE
    SEQUENCE discourse.permalinks_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.permalinks_id_seq OWNED BY discourse.permalinks.id;

CREATE TABLE discourse.plugin_store_rows
(
    id          integer           NOT NULL,
    plugin_name character varying NOT NULL,
    key         character varying NOT NULL,
    type_name   character varying NOT NULL,
    value       text
);

CREATE
    SEQUENCE discourse.plugin_store_rows_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.plugin_store_rows_id_seq OWNED BY discourse.plugin_store_rows.id;

CREATE TABLE discourse.poll_options
(
    id              bigint                      NOT NULL,
    poll_id         bigint,
    digest          character varying           NOT NULL,
    html            text                        NOT NULL,
    anonymous_votes integer,
    created_at      timestamp without time zone NOT NULL,
    updated_at      timestamp without time zone NOT NULL
);

CREATE
    SEQUENCE discourse.poll_options_id_seq
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.poll_options_id_seq OWNED BY discourse.poll_options.id;

CREATE TABLE discourse.poll_votes
(
    poll_id        bigint,
    poll_option_id bigint,
    user_id        bigint,
    created_at     timestamp without time zone NOT NULL,
    updated_at     timestamp without time zone NOT NULL
);

CREATE TABLE discourse.polls
(
    id               bigint                                               NOT NULL,
    post_id          bigint,
    name             character varying DEFAULT 'poll':: character varying NOT NULL,
    close_at         timestamp without time zone,
    type             integer           DEFAULT 0                          NOT NULL,
    status           integer           DEFAULT 0                          NOT NULL,
    results          integer           DEFAULT 0                          NOT NULL,
    visibility       integer           DEFAULT 0                          NOT NULL,
    min              integer,
    max              integer,
    step             integer,
    anonymous_voters integer,
    created_at       timestamp without time zone                          NOT NULL,
    updated_at       timestamp without time zone                          NOT NULL,
    chart_type       integer           DEFAULT 0                          NOT NULL,
    groups           character varying
);

CREATE
    SEQUENCE discourse.polls_id_seq
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.polls_id_seq OWNED BY discourse.polls.id;

CREATE TABLE discourse.post_action_types
(
    name_key            character varying(50)          NOT NULL,
    is_flag             boolean          DEFAULT false NOT NULL,
    icon                character varying(20),
    created_at          timestamp without time zone    NOT NULL,
    updated_at          timestamp without time zone    NOT NULL,
    id                  integer                        NOT NULL,
    "position"          integer          DEFAULT 0     NOT NULL,
    score_bonus         double precision DEFAULT 0.0   NOT NULL,
    reviewable_priority integer          DEFAULT 0     NOT NULL
);

CREATE
    SEQUENCE discourse.post_action_types_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.post_action_types_id_seq OWNED BY discourse.post_action_types.id;

CREATE TABLE discourse.post_actions
(
    id                  integer                     NOT NULL,
    post_id             integer                     NOT NULL,
    user_id             integer                     NOT NULL,
    post_action_type_id integer                     NOT NULL,
    deleted_at          timestamp without time zone,
    created_at          timestamp without time zone NOT NULL,
    updated_at          timestamp without time zone NOT NULL,
    deleted_by_id       integer,
    related_post_id     integer,
    staff_took_action   boolean DEFAULT false       NOT NULL,
    deferred_by_id      integer,
    targets_topic       boolean DEFAULT false       NOT NULL,
    agreed_at           timestamp without time zone,
    agreed_by_id        integer,
    deferred_at         timestamp without time zone,
    disagreed_at        timestamp without time zone,
    disagreed_by_id     integer
);

CREATE
    SEQUENCE discourse.post_actions_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.post_actions_id_seq OWNED BY discourse.post_actions.id;

CREATE TABLE discourse.post_custom_fields
(
    id         integer                     NOT NULL,
    post_id    integer                     NOT NULL,
    name       character varying(256)      NOT NULL,
    value      text,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL
);

CREATE
    SEQUENCE discourse.post_custom_fields_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.post_custom_fields_id_seq OWNED BY discourse.post_custom_fields.id;

CREATE TABLE discourse.post_details
(
    id         integer                     NOT NULL,
    post_id    integer,
    key        character varying,
    value      character varying,
    extra      text,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL
);

CREATE
    SEQUENCE discourse.post_details_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.post_details_id_seq OWNED BY discourse.post_details.id;

CREATE TABLE discourse.post_replies
(
    post_id       integer,
    created_at    timestamp without time zone NOT NULL,
    updated_at    timestamp without time zone NOT NULL,
    reply_post_id integer
);

CREATE TABLE discourse.post_reply_keys
(
    id         bigint                      NOT NULL,
    user_id    integer                     NOT NULL,
    post_id    integer                     NOT NULL,
    reply_key  uuid                        NOT NULL,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL
);

CREATE
    SEQUENCE discourse.post_reply_keys_id_seq
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.post_reply_keys_id_seq OWNED BY discourse.post_reply_keys.id;

CREATE TABLE discourse.post_revisions
(
    id            integer                     NOT NULL,
    user_id       integer,
    post_id       integer,
    modifications text,
    number        integer,
    created_at    timestamp without time zone NOT NULL,
    updated_at    timestamp without time zone NOT NULL,
    hidden        boolean DEFAULT false       NOT NULL
);

CREATE
    SEQUENCE discourse.post_revisions_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.post_revisions_id_seq OWNED BY discourse.post_revisions.id;

CREATE TABLE discourse.post_search_data
(
    post_id     integer NOT NULL,
    search_data tsvector,
    raw_data    text,
    locale      character varying,
    version     integer DEFAULT 0
);

CREATE TABLE discourse.post_stats
(
    id                           integer                     NOT NULL,
    post_id                      integer,
    drafts_saved                 integer,
    typing_duration_msecs        integer,
    composer_open_duration_msecs integer,
    created_at                   timestamp without time zone NOT NULL,
    updated_at                   timestamp without time zone NOT NULL
);

CREATE
    SEQUENCE discourse.post_stats_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.post_stats_id_seq OWNED BY discourse.post_stats.id;

CREATE TABLE discourse.post_timings
(
    topic_id    integer NOT NULL,
    post_number integer NOT NULL,
    user_id     integer NOT NULL,
    msecs       integer NOT NULL
);

CREATE TABLE discourse.post_uploads
(
    id        integer NOT NULL,
    post_id   integer NOT NULL,
    upload_id integer NOT NULL
);

CREATE
    SEQUENCE discourse.post_uploads_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.post_uploads_id_seq OWNED BY discourse.post_uploads.id;

CREATE
    SEQUENCE discourse.posts_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.posts_id_seq OWNED BY discourse.posts.id;

CREATE TABLE discourse.push_subscriptions
(
    id         bigint                      NOT NULL,
    user_id    integer                     NOT NULL,
    data       character varying           NOT NULL,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL
);

CREATE
    SEQUENCE discourse.push_subscriptions_id_seq
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.push_subscriptions_id_seq OWNED BY discourse.push_subscriptions.id;

CREATE TABLE discourse.quoted_posts
(
    id             integer                     NOT NULL,
    post_id        integer                     NOT NULL,
    quoted_post_id integer                     NOT NULL,
    created_at     timestamp without time zone NOT NULL,
    updated_at     timestamp without time zone NOT NULL
);

CREATE
    SEQUENCE discourse.quoted_posts_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.quoted_posts_id_seq OWNED BY discourse.quoted_posts.id;

CREATE TABLE discourse.remote_themes
(
    id                        integer                     NOT NULL,
    remote_url                character varying           NOT NULL,
    remote_version            character varying,
    local_version             character varying,
    about_url                 character varying,
    license_url               character varying,
    commits_behind            integer,
    remote_updated_at         timestamp without time zone,
    created_at                timestamp without time zone NOT NULL,
    updated_at                timestamp without time zone NOT NULL,
    private_key               text,
    branch                    character varying,
    last_error_text           text,
    authors                   character varying,
    theme_version             character varying,
    minimum_discourse_version character varying,
    maximum_discourse_version character varying
);

CREATE
    SEQUENCE discourse.remote_themes_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.remote_themes_id_seq OWNED BY discourse.remote_themes.id;

CREATE TABLE discourse.reviewable_claimed_topics
(
    id         bigint                      NOT NULL,
    user_id    integer                     NOT NULL,
    topic_id   integer                     NOT NULL,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL
);

CREATE
    SEQUENCE discourse.reviewable_claimed_topics_id_seq
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.reviewable_claimed_topics_id_seq OWNED BY discourse.reviewable_claimed_topics.id;

CREATE TABLE discourse.reviewable_histories
(
    id                      bigint                      NOT NULL,
    reviewable_id           integer                     NOT NULL,
    reviewable_history_type integer                     NOT NULL,
    status                  integer                     NOT NULL,
    created_by_id           integer                     NOT NULL,
    edited                  json,
    created_at              timestamp without time zone NOT NULL,
    updated_at              timestamp without time zone NOT NULL
);

CREATE
    SEQUENCE discourse.reviewable_histories_id_seq
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.reviewable_histories_id_seq OWNED BY discourse.reviewable_histories.id;

CREATE TABLE discourse.reviewable_scores
(
    id                    bigint                       NOT NULL,
    reviewable_id         integer                      NOT NULL,
    user_id               integer                      NOT NULL,
    reviewable_score_type integer                      NOT NULL,
    status                integer                      NOT NULL,
    score                 double precision DEFAULT 0.0 NOT NULL,
    take_action_bonus     double precision DEFAULT 0.0 NOT NULL,
    reviewed_by_id        integer,
    reviewed_at           timestamp without time zone,
    meta_topic_id         integer,
    created_at            timestamp without time zone  NOT NULL,
    updated_at            timestamp without time zone  NOT NULL,
    reason                character varying,
    user_accuracy_bonus   double precision DEFAULT 0.0 NOT NULL
);

CREATE
    SEQUENCE discourse.reviewable_scores_id_seq
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.reviewable_scores_id_seq OWNED BY discourse.reviewable_scores.id;

CREATE TABLE discourse.reviewables
(
    id                      bigint                         NOT NULL,
    type                    character varying              NOT NULL,
    status                  integer          DEFAULT 0     NOT NULL,
    created_by_id           integer                        NOT NULL,
    reviewable_by_moderator boolean          DEFAULT false NOT NULL,
    reviewable_by_group_id  integer,
    category_id             integer,
    topic_id                integer,
    score                   double precision DEFAULT 0.0   NOT NULL,
    potential_spam          boolean          DEFAULT false NOT NULL,
    target_id               integer,
    target_type             character varying,
    target_created_by_id    integer,
    payload                 json,
    version                 integer          DEFAULT 0     NOT NULL,
    latest_score            timestamp without time zone,
    created_at              timestamp without time zone    NOT NULL,
    updated_at              timestamp without time zone    NOT NULL
);

CREATE
    SEQUENCE discourse.reviewables_id_seq
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.reviewables_id_seq OWNED BY discourse.reviewables.id;

CREATE TABLE discourse.scheduler_stats
(
    id                integer                     NOT NULL,
    name              character varying           NOT NULL,
    hostname          character varying           NOT NULL,
    pid               integer                     NOT NULL,
    duration_ms       integer,
    live_slots_start  integer,
    live_slots_finish integer,
    started_at        timestamp without time zone NOT NULL,
    success           boolean,
    error             text
);

CREATE
    SEQUENCE discourse.scheduler_stats_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.scheduler_stats_id_seq OWNED BY discourse.scheduler_stats.id;

CREATE TABLE discourse.schema_migration_details
(
    id            integer                     NOT NULL,
    version       character varying           NOT NULL,
    name          character varying,
    hostname      character varying,
    git_version   character varying,
    rails_version character varying,
    duration      integer,
    direction     character varying,
    created_at    timestamp without time zone NOT NULL
);

CREATE
    SEQUENCE discourse.schema_migration_details_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.schema_migration_details_id_seq OWNED BY discourse.schema_migration_details.id;

CREATE TABLE discourse.schema_migrations
(
    version character varying NOT NULL
);

CREATE TABLE discourse.screened_emails
(
    id            integer                     NOT NULL,
    email         character varying           NOT NULL,
    action_type   integer                     NOT NULL,
    match_count   integer DEFAULT 0           NOT NULL,
    last_match_at timestamp without time zone,
    created_at    timestamp without time zone NOT NULL,
    updated_at    timestamp without time zone NOT NULL,
    ip_address    inet
);

CREATE
    SEQUENCE discourse.screened_emails_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.screened_emails_id_seq OWNED BY discourse.screened_emails.id;

CREATE TABLE discourse.screened_ip_addresses
(
    id            integer                     NOT NULL,
    ip_address    inet                        NOT NULL,
    action_type   integer                     NOT NULL,
    match_count   integer DEFAULT 0           NOT NULL,
    last_match_at timestamp without time zone,
    created_at    timestamp without time zone NOT NULL,
    updated_at    timestamp without time zone NOT NULL
);

CREATE
    SEQUENCE discourse.screened_ip_addresses_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.screened_ip_addresses_id_seq OWNED BY discourse.screened_ip_addresses.id;

CREATE TABLE discourse.screened_urls
(
    id            integer                     NOT NULL,
    url           character varying           NOT NULL,
    domain        character varying           NOT NULL,
    action_type   integer                     NOT NULL,
    match_count   integer DEFAULT 0           NOT NULL,
    last_match_at timestamp without time zone,
    created_at    timestamp without time zone NOT NULL,
    updated_at    timestamp without time zone NOT NULL,
    ip_address    inet
);

CREATE
    SEQUENCE discourse.screened_urls_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.screened_urls_id_seq OWNED BY discourse.screened_urls.id;

CREATE TABLE discourse.search_logs
(
    id                 integer                     NOT NULL,
    term               character varying           NOT NULL,
    user_id            integer,
    ip_address         inet,
    search_result_id   integer,
    search_type        integer                     NOT NULL,
    created_at         timestamp without time zone NOT NULL,
    search_result_type integer
);

CREATE
    SEQUENCE discourse.search_logs_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.search_logs_id_seq OWNED BY discourse.search_logs.id;

CREATE TABLE discourse.shared_drafts
(
    topic_id    integer                     NOT NULL,
    category_id integer                     NOT NULL,
    created_at  timestamp without time zone NOT NULL,
    updated_at  timestamp without time zone NOT NULL,
    id          bigint                      NOT NULL
);

CREATE
    SEQUENCE discourse.shared_drafts_id_seq
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.shared_drafts_id_seq OWNED BY discourse.shared_drafts.id;

CREATE TABLE discourse.single_sign_on_records
(
    id                              integer                     NOT NULL,
    user_id                         integer                     NOT NULL,
    external_id                     character varying           NOT NULL,
    last_payload                    text                        NOT NULL,
    created_at                      timestamp without time zone NOT NULL,
    updated_at                      timestamp without time zone NOT NULL,
    external_username               character varying,
    external_email                  character varying,
    external_name                   character varying,
    external_avatar_url             character varying(1000),
    external_profile_background_url character varying,
    external_card_background_url    character varying
);

CREATE
    SEQUENCE discourse.single_sign_on_records_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.single_sign_on_records_id_seq OWNED BY discourse.single_sign_on_records.id;

CREATE TABLE discourse.site_settings
(
    id         integer                     NOT NULL,
    name       character varying           NOT NULL,
    data_type  integer                     NOT NULL,
    value      text,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL
);

CREATE
    SEQUENCE discourse.site_settings_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.site_settings_id_seq OWNED BY discourse.site_settings.id;

CREATE TABLE discourse.skipped_email_logs
(
    id            bigint                      NOT NULL,
    email_type    character varying           NOT NULL,
    to_address    character varying           NOT NULL,
    user_id       integer,
    post_id       integer,
    reason_type   integer                     NOT NULL,
    custom_reason text,
    created_at    timestamp without time zone NOT NULL,
    updated_at    timestamp without time zone NOT NULL
);

CREATE
    SEQUENCE discourse.skipped_email_logs_id_seq
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.skipped_email_logs_id_seq OWNED BY discourse.skipped_email_logs.id;

CREATE TABLE discourse.stylesheet_cache
(
    id         integer                        NOT NULL,
    target     character varying              NOT NULL,
    digest     character varying              NOT NULL,
    content    text                           NOT NULL,
    created_at timestamp without time zone    NOT NULL,
    updated_at timestamp without time zone    NOT NULL,
    theme_id   integer DEFAULT '-1':: integer NOT NULL,
    source_map text
);

CREATE
    SEQUENCE discourse.stylesheet_cache_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.stylesheet_cache_id_seq OWNED BY discourse.stylesheet_cache.id;

CREATE TABLE discourse.tag_group_memberships
(
    id           integer                     NOT NULL,
    tag_id       integer                     NOT NULL,
    tag_group_id integer                     NOT NULL,
    created_at   timestamp without time zone NOT NULL,
    updated_at   timestamp without time zone NOT NULL
);

CREATE
    SEQUENCE discourse.tag_group_memberships_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.tag_group_memberships_id_seq OWNED BY discourse.tag_group_memberships.id;

CREATE TABLE discourse.tag_group_permissions
(
    id              bigint                      NOT NULL,
    tag_group_id    bigint                      NOT NULL,
    group_id        bigint                      NOT NULL,
    permission_type integer DEFAULT 1           NOT NULL,
    created_at      timestamp without time zone NOT NULL,
    updated_at      timestamp without time zone NOT NULL
);

CREATE
    SEQUENCE discourse.tag_group_permissions_id_seq
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.tag_group_permissions_id_seq OWNED BY discourse.tag_group_permissions.id;

CREATE TABLE discourse.tag_groups
(
    id            integer                     NOT NULL,
    name          character varying           NOT NULL,
    created_at    timestamp without time zone NOT NULL,
    updated_at    timestamp without time zone NOT NULL,
    parent_tag_id integer,
    one_per_topic boolean DEFAULT false
);

CREATE
    SEQUENCE discourse.tag_groups_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.tag_groups_id_seq OWNED BY discourse.tag_groups.id;

CREATE TABLE discourse.tag_search_data
(
    tag_id      integer NOT NULL,
    search_data tsvector,
    raw_data    text,
    locale      text,
    version     integer DEFAULT 0
);

CREATE
    SEQUENCE discourse.tag_search_data_tag_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.tag_search_data_tag_id_seq OWNED BY discourse.tag_search_data.tag_id;

CREATE TABLE discourse.tag_users
(
    id                 integer                     NOT NULL,
    tag_id             integer                     NOT NULL,
    user_id            integer                     NOT NULL,
    notification_level integer                     NOT NULL,
    created_at         timestamp without time zone NOT NULL,
    updated_at         timestamp without time zone NOT NULL
);

CREATE
    SEQUENCE discourse.tag_users_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.tag_users_id_seq OWNED BY discourse.tag_users.id;

CREATE TABLE discourse.tags
(
    id             integer                     NOT NULL,
    name           character varying           NOT NULL,
    topic_count    integer DEFAULT 0           NOT NULL,
    created_at     timestamp without time zone NOT NULL,
    updated_at     timestamp without time zone NOT NULL,
    pm_topic_count integer DEFAULT 0           NOT NULL,
    target_tag_id  integer
);

CREATE
    SEQUENCE discourse.tags_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.tags_id_seq OWNED BY discourse.tags.id;

CREATE TABLE discourse.tags_web_hooks
(
    web_hook_id bigint NOT NULL,
    tag_id      bigint NOT NULL
);

CREATE TABLE discourse.theme_fields
(
    id               integer                         NOT NULL,
    theme_id         integer                         NOT NULL,
    target_id        integer                         NOT NULL,
    name             character varying(255)          NOT NULL,
    value            text                            NOT NULL,
    value_baked      text,
    created_at       timestamp without time zone     NOT NULL,
    updated_at       timestamp without time zone     NOT NULL,
    compiler_version character varying(50) DEFAULT 0 NOT NULL,
    error            character varying,
    upload_id        integer,
    type_id          integer               DEFAULT 0 NOT NULL
);

CREATE
    SEQUENCE discourse.theme_fields_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.theme_fields_id_seq OWNED BY discourse.theme_fields.id;

CREATE TABLE discourse.theme_modifier_sets
(
    id                       bigint NOT NULL,
    theme_id                 bigint NOT NULL,
    serialize_topic_excerpts boolean,
    csp_extensions           character varying[],
    svg_icons                character varying[]
);

CREATE
    SEQUENCE discourse.theme_modifier_sets_id_seq
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.theme_modifier_sets_id_seq OWNED BY discourse.theme_modifier_sets.id;

CREATE TABLE discourse.theme_settings
(
    id         bigint                      NOT NULL,
    name       character varying(255)      NOT NULL,
    data_type  integer                     NOT NULL,
    value      text,
    theme_id   integer                     NOT NULL,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL
);

CREATE
    SEQUENCE discourse.theme_settings_id_seq
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.theme_settings_id_seq OWNED BY discourse.theme_settings.id;

CREATE TABLE discourse.theme_translation_overrides
(
    id              bigint                      NOT NULL,
    theme_id        integer                     NOT NULL,
    locale          character varying           NOT NULL,
    translation_key character varying           NOT NULL,
    value           character varying           NOT NULL,
    created_at      timestamp without time zone NOT NULL,
    updated_at      timestamp without time zone NOT NULL
);

CREATE
    SEQUENCE discourse.theme_translation_overrides_id_seq
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.theme_translation_overrides_id_seq OWNED BY discourse.theme_translation_overrides.id;

CREATE TABLE discourse.themes
(
    id               integer                     NOT NULL,
    name             character varying           NOT NULL,
    user_id          integer                     NOT NULL,
    created_at       timestamp without time zone NOT NULL,
    updated_at       timestamp without time zone NOT NULL,
    compiler_version integer DEFAULT 0           NOT NULL,
    user_selectable  boolean DEFAULT false       NOT NULL,
    hidden           boolean DEFAULT false       NOT NULL,
    color_scheme_id  integer,
    remote_theme_id  integer,
    component        boolean DEFAULT false       NOT NULL,
    enabled          boolean DEFAULT true        NOT NULL
);

CREATE
    SEQUENCE discourse.themes_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.themes_id_seq OWNED BY discourse.themes.id;

CREATE TABLE discourse.top_topics
(
    id                       integer                    NOT NULL,
    topic_id                 integer,
    yearly_posts_count       integer          DEFAULT 0 NOT NULL,
    yearly_views_count       integer          DEFAULT 0 NOT NULL,
    yearly_likes_count       integer          DEFAULT 0 NOT NULL,
    monthly_posts_count      integer          DEFAULT 0 NOT NULL,
    monthly_views_count      integer          DEFAULT 0 NOT NULL,
    monthly_likes_count      integer          DEFAULT 0 NOT NULL,
    weekly_posts_count       integer          DEFAULT 0 NOT NULL,
    weekly_views_count       integer          DEFAULT 0 NOT NULL,
    weekly_likes_count       integer          DEFAULT 0 NOT NULL,
    daily_posts_count        integer          DEFAULT 0 NOT NULL,
    daily_views_count        integer          DEFAULT 0 NOT NULL,
    daily_likes_count        integer          DEFAULT 0 NOT NULL,
    daily_score              double precision DEFAULT 0.0,
    weekly_score             double precision DEFAULT 0.0,
    monthly_score            double precision DEFAULT 0.0,
    yearly_score             double precision DEFAULT 0.0,
    all_score                double precision DEFAULT 0.0,
    daily_op_likes_count     integer          DEFAULT 0 NOT NULL,
    weekly_op_likes_count    integer          DEFAULT 0 NOT NULL,
    monthly_op_likes_count   integer          DEFAULT 0 NOT NULL,
    yearly_op_likes_count    integer          DEFAULT 0 NOT NULL,
    quarterly_posts_count    integer          DEFAULT 0 NOT NULL,
    quarterly_views_count    integer          DEFAULT 0 NOT NULL,
    quarterly_likes_count    integer          DEFAULT 0 NOT NULL,
    quarterly_score          double precision DEFAULT 0.0,
    quarterly_op_likes_count integer          DEFAULT 0 NOT NULL
);

CREATE
    SEQUENCE discourse.top_topics_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.top_topics_id_seq OWNED BY discourse.top_topics.id;

CREATE TABLE discourse.topic_allowed_groups
(
    id       integer NOT NULL,
    group_id integer NOT NULL,
    topic_id integer NOT NULL
);

CREATE
    SEQUENCE discourse.topic_allowed_groups_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.topic_allowed_groups_id_seq OWNED BY discourse.topic_allowed_groups.id;

CREATE TABLE discourse.topic_allowed_users
(
    id         integer                     NOT NULL,
    user_id    integer                     NOT NULL,
    topic_id   integer                     NOT NULL,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL
);

CREATE
    SEQUENCE discourse.topic_allowed_users_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.topic_allowed_users_id_seq OWNED BY discourse.topic_allowed_users.id;

CREATE TABLE discourse.topic_custom_fields
(
    id         integer                     NOT NULL,
    topic_id   integer                     NOT NULL,
    name       character varying(256)      NOT NULL,
    value      text,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL
);

CREATE
    SEQUENCE discourse.topic_custom_fields_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.topic_custom_fields_id_seq OWNED BY discourse.topic_custom_fields.id;

CREATE TABLE discourse.topic_embeds
(
    id            integer                     NOT NULL,
    topic_id      integer                     NOT NULL,
    post_id       integer                     NOT NULL,
    embed_url     character varying(1000)     NOT NULL,
    content_sha1  character varying(40),
    created_at    timestamp without time zone NOT NULL,
    updated_at    timestamp without time zone NOT NULL,
    deleted_at    timestamp without time zone,
    deleted_by_id integer
);

CREATE
    SEQUENCE discourse.topic_embeds_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.topic_embeds_id_seq OWNED BY discourse.topic_embeds.id;

CREATE TABLE discourse.topic_groups
(
    id                    bigint                      NOT NULL,
    group_id              integer                     NOT NULL,
    topic_id              integer                     NOT NULL,
    last_read_post_number integer DEFAULT 0           NOT NULL,
    created_at            timestamp without time zone NOT NULL,
    updated_at            timestamp without time zone NOT NULL
);

CREATE
    SEQUENCE discourse.topic_groups_id_seq
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.topic_groups_id_seq OWNED BY discourse.topic_groups.id;

CREATE TABLE discourse.topic_invites
(
    id         integer                     NOT NULL,
    topic_id   integer                     NOT NULL,
    invite_id  integer                     NOT NULL,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL
);

CREATE
    SEQUENCE discourse.topic_invites_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.topic_invites_id_seq OWNED BY discourse.topic_invites.id;

CREATE TABLE discourse.topic_link_clicks
(
    id            integer                     NOT NULL,
    topic_link_id integer                     NOT NULL,
    user_id       integer,
    created_at    timestamp without time zone NOT NULL,
    updated_at    timestamp without time zone NOT NULL,
    ip_address    inet
);

CREATE
    SEQUENCE discourse.topic_link_clicks_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.topic_link_clicks_id_seq OWNED BY discourse.topic_link_clicks.id;

CREATE TABLE discourse.topic_links
(
    id            integer                     NOT NULL,
    topic_id      integer                     NOT NULL,
    post_id       integer,
    user_id       integer                     NOT NULL,
    url           character varying(500)      NOT NULL,
    domain        character varying(100)      NOT NULL,
    internal      boolean DEFAULT false       NOT NULL,
    link_topic_id integer,
    created_at    timestamp without time zone NOT NULL,
    updated_at    timestamp without time zone NOT NULL,
    reflection    boolean DEFAULT false,
    clicks        integer DEFAULT 0           NOT NULL,
    link_post_id  integer,
    title         character varying,
    crawled_at    timestamp without time zone,
    quote         boolean DEFAULT false       NOT NULL,
    extension     character varying(10)
);

CREATE
    SEQUENCE discourse.topic_links_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.topic_links_id_seq OWNED BY discourse.topic_links.id;

CREATE TABLE discourse.topic_search_data
(
    topic_id    integer           NOT NULL,
    raw_data    text,
    locale      character varying NOT NULL,
    search_data tsvector,
    version     integer DEFAULT 0
);

CREATE
    SEQUENCE discourse.topic_search_data_topic_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.topic_search_data_topic_id_seq OWNED BY discourse.topic_search_data.topic_id;

CREATE TABLE discourse.topic_tags
(
    id         integer                     NOT NULL,
    topic_id   integer                     NOT NULL,
    tag_id     integer                     NOT NULL,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL
);

CREATE
    SEQUENCE discourse.topic_tags_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.topic_tags_id_seq OWNED BY discourse.topic_tags.id;

CREATE TABLE discourse.topic_timers
(
    id                 integer                     NOT NULL,
    execute_at         timestamp without time zone NOT NULL,
    status_type        integer                     NOT NULL,
    user_id            integer                     NOT NULL,
    topic_id           integer                     NOT NULL,
    based_on_last_post boolean DEFAULT false       NOT NULL,
    deleted_at         timestamp without time zone,
    deleted_by_id      integer,
    created_at         timestamp without time zone NOT NULL,
    updated_at         timestamp without time zone NOT NULL,
    category_id        integer,
    public_type        boolean DEFAULT true,
    duration           integer
);

CREATE
    SEQUENCE discourse.topic_timers_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.topic_timers_id_seq OWNED BY discourse.topic_timers.id;

CREATE TABLE discourse.topic_users
(
    user_id                  integer               NOT NULL,
    topic_id                 integer               NOT NULL,
    posted                   boolean DEFAULT false NOT NULL,
    last_read_post_number    integer,
    highest_seen_post_number integer,
    last_visited_at          timestamp without time zone,
    first_visited_at         timestamp without time zone,
    notification_level       integer DEFAULT 1     NOT NULL,
    notifications_changed_at timestamp without time zone,
    notifications_reason_id  integer,
    total_msecs_viewed       integer DEFAULT 0     NOT NULL,
    cleared_pinned_at        timestamp without time zone,
    id                       integer               NOT NULL,
    last_emailed_post_number integer,
    liked                    boolean DEFAULT false,
    bookmarked               boolean DEFAULT false
);

CREATE
    SEQUENCE discourse.topic_users_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.topic_users_id_seq OWNED BY discourse.topic_users.id;

CREATE TABLE discourse.topic_views
(
    topic_id   integer NOT NULL,
    viewed_at  date    NOT NULL,
    user_id    integer,
    ip_address inet
);

CREATE
    SEQUENCE discourse.topics_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.topics_id_seq OWNED BY discourse.topics.id;

CREATE TABLE discourse.translation_overrides
(
    id              integer                     NOT NULL,
    locale          character varying           NOT NULL,
    translation_key character varying           NOT NULL,
    value           character varying           NOT NULL,
    created_at      timestamp without time zone NOT NULL,
    updated_at      timestamp without time zone NOT NULL,
    compiled_js     text
);

CREATE
    SEQUENCE discourse.translation_overrides_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.translation_overrides_id_seq OWNED BY discourse.translation_overrides.id;

CREATE TABLE discourse.unsubscribe_keys
(
    key                  character varying(64)       NOT NULL,
    user_id              integer                     NOT NULL,
    created_at           timestamp without time zone NOT NULL,
    updated_at           timestamp without time zone NOT NULL,
    unsubscribe_key_type character varying,
    topic_id             integer,
    post_id              integer
);

CREATE TABLE discourse.uploads
(
    id                     integer                     NOT NULL,
    user_id                integer                     NOT NULL,
    original_filename      character varying           NOT NULL,
    filesize               integer                     NOT NULL,
    width                  integer,
    height                 integer,
    url                    character varying           NOT NULL,
    created_at             timestamp without time zone NOT NULL,
    updated_at             timestamp without time zone NOT NULL,
    sha1                   character varying(40),
    origin                 character varying(1000),
    retain_hours           integer,
    extension              character varying(10),
    thumbnail_width        integer,
    thumbnail_height       integer,
    etag                   character varying,
    secure                 boolean DEFAULT false       NOT NULL,
    access_control_post_id bigint,
    original_sha1          character varying
);

CREATE
    SEQUENCE discourse.uploads_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.uploads_id_seq OWNED BY discourse.uploads.id;

CREATE TABLE discourse.user_actions
(
    id              integer                     NOT NULL,
    action_type     integer                     NOT NULL,
    user_id         integer                     NOT NULL,
    target_topic_id integer,
    target_post_id  integer,
    target_user_id  integer,
    acting_user_id  integer,
    created_at      timestamp without time zone NOT NULL,
    updated_at      timestamp without time zone NOT NULL
);

CREATE
    SEQUENCE discourse.user_actions_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.user_actions_id_seq OWNED BY discourse.user_actions.id;

CREATE TABLE discourse.user_api_keys
(
    id               integer                                               NOT NULL,
    user_id          integer                                               NOT NULL,
    client_id        character varying                                     NOT NULL,
    key              character varying                                     NOT NULL,
    application_name character varying                                     NOT NULL,
    push_url         character varying,
    created_at       timestamp without time zone                           NOT NULL,
    updated_at       timestamp without time zone                           NOT NULL,
    revoked_at       timestamp without time zone,
    scopes           text[]                      DEFAULT '{}':: text[]     NOT NULL,
    last_used_at     timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE
    SEQUENCE discourse.user_api_keys_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.user_api_keys_id_seq OWNED BY discourse.user_api_keys.id;

CREATE TABLE discourse.user_archived_messages
(
    id         integer                     NOT NULL,
    user_id    integer                     NOT NULL,
    topic_id   integer                     NOT NULL,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL
);

CREATE
    SEQUENCE discourse.user_archived_messages_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.user_archived_messages_id_seq OWNED BY discourse.user_archived_messages.id;

CREATE TABLE discourse.user_associated_accounts
(
    id            bigint                                                NOT NULL,
    provider_name character varying                                     NOT NULL,
    provider_uid  character varying                                     NOT NULL,
    user_id       integer,
    last_used     timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    info          jsonb                       DEFAULT '{}'::jsonb       NOT NULL,
    credentials   jsonb                       DEFAULT '{}'::jsonb       NOT NULL,
    extra         jsonb                       DEFAULT '{}'::jsonb       NOT NULL,
    created_at    timestamp without time zone                           NOT NULL,
    updated_at    timestamp without time zone                           NOT NULL
);

CREATE
    SEQUENCE discourse.user_associated_accounts_id_seq
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.user_associated_accounts_id_seq OWNED BY discourse.user_associated_accounts.id;

CREATE TABLE discourse.user_auth_token_logs
(
    id                 integer           NOT NULL,
    action             character varying NOT NULL,
    user_auth_token_id integer,
    user_id            integer,
    client_ip          inet,
    user_agent         character varying,
    auth_token         character varying,
    created_at         timestamp without time zone,
    path               character varying
);

CREATE
    SEQUENCE discourse.user_auth_token_logs_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.user_auth_token_logs_id_seq OWNED BY discourse.user_auth_token_logs.id;

CREATE TABLE discourse.user_auth_tokens
(
    id              integer                     NOT NULL,
    user_id         integer                     NOT NULL,
    auth_token      character varying           NOT NULL,
    prev_auth_token character varying           NOT NULL,
    user_agent      character varying,
    auth_token_seen boolean DEFAULT false       NOT NULL,
    client_ip       inet,
    rotated_at      timestamp without time zone NOT NULL,
    created_at      timestamp without time zone NOT NULL,
    updated_at      timestamp without time zone NOT NULL,
    seen_at         timestamp without time zone
);

CREATE
    SEQUENCE discourse.user_auth_tokens_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.user_auth_tokens_id_seq OWNED BY discourse.user_auth_tokens.id;

CREATE TABLE discourse.user_avatars
(
    id                             integer                     NOT NULL,
    user_id                        integer                     NOT NULL,
    custom_upload_id               integer,
    gravatar_upload_id             integer,
    last_gravatar_download_attempt timestamp without time zone,
    created_at                     timestamp without time zone NOT NULL,
    updated_at                     timestamp without time zone NOT NULL
);

CREATE
    SEQUENCE discourse.user_avatars_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.user_avatars_id_seq OWNED BY discourse.user_avatars.id;

CREATE TABLE discourse.user_badges
(
    id              integer                     NOT NULL,
    badge_id        integer                     NOT NULL,
    user_id         integer                     NOT NULL,
    granted_at      timestamp without time zone NOT NULL,
    granted_by_id   integer                     NOT NULL,
    post_id         integer,
    notification_id integer,
    seq             integer DEFAULT 0           NOT NULL,
    featured_rank   integer
);

CREATE
    SEQUENCE discourse.user_badges_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.user_badges_id_seq OWNED BY discourse.user_badges.id;

CREATE TABLE discourse.user_custom_fields
(
    id         integer                     NOT NULL,
    user_id    integer                     NOT NULL,
    name       character varying(256)      NOT NULL,
    value      text,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL
);

CREATE
    SEQUENCE discourse.user_custom_fields_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.user_custom_fields_id_seq OWNED BY discourse.user_custom_fields.id;

CREATE TABLE discourse.user_emails
(
    id         integer                     NOT NULL,
    user_id    integer                     NOT NULL,
    email      character varying(513)      NOT NULL,
    "primary"  boolean DEFAULT false       NOT NULL,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL
);

CREATE
    SEQUENCE discourse.user_emails_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.user_emails_id_seq OWNED BY discourse.user_emails.id;

CREATE TABLE discourse.user_exports
(
    id         integer                     NOT NULL,
    file_name  character varying           NOT NULL,
    user_id    integer                     NOT NULL,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL,
    upload_id  integer,
    topic_id   integer
);

CREATE
    SEQUENCE discourse.user_exports_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.user_exports_id_seq OWNED BY discourse.user_exports.id;

CREATE TABLE discourse.user_field_options
(
    id            integer                     NOT NULL,
    user_field_id integer                     NOT NULL,
    value         character varying           NOT NULL,
    created_at    timestamp without time zone NOT NULL,
    updated_at    timestamp without time zone NOT NULL
);

CREATE
    SEQUENCE discourse.user_field_options_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.user_field_options_id_seq OWNED BY discourse.user_field_options.id;

CREATE TABLE discourse.user_fields
(
    id                integer                     NOT NULL,
    name              character varying           NOT NULL,
    field_type        character varying           NOT NULL,
    created_at        timestamp without time zone NOT NULL,
    updated_at        timestamp without time zone NOT NULL,
    editable          boolean DEFAULT false       NOT NULL,
    description       character varying           NOT NULL,
    required          boolean DEFAULT true        NOT NULL,
    show_on_profile   boolean DEFAULT false       NOT NULL,
    "position"        integer DEFAULT 0,
    show_on_user_card boolean DEFAULT false       NOT NULL,
    external_name     character varying,
    external_type     character varying
);

CREATE
    SEQUENCE discourse.user_fields_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.user_fields_id_seq OWNED BY discourse.user_fields.id;

CREATE TABLE discourse.user_histories
(
    id             integer                     NOT NULL,
    action         integer                     NOT NULL,
    acting_user_id integer,
    target_user_id integer,
    details        text,
    created_at     timestamp without time zone NOT NULL,
    updated_at     timestamp without time zone NOT NULL,
    context        character varying,
    ip_address     character varying,
    email          character varying,
    subject        text,
    previous_value text,
    new_value      text,
    topic_id       integer,
    admin_only     boolean DEFAULT false,
    post_id        integer,
    custom_type    character varying,
    category_id    integer
);

CREATE
    SEQUENCE discourse.user_histories_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.user_histories_id_seq OWNED BY discourse.user_histories.id;

CREATE TABLE discourse.user_open_ids
(
    id         integer                     NOT NULL,
    user_id    integer                     NOT NULL,
    email      character varying           NOT NULL,
    url        character varying           NOT NULL,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL,
    active     boolean                     NOT NULL
);

CREATE
    SEQUENCE discourse.user_open_ids_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.user_open_ids_id_seq OWNED BY discourse.user_open_ids.id;

CREATE TABLE discourse.user_options
(
    user_id                          integer                            NOT NULL,
    mailing_list_mode                boolean   DEFAULT false            NOT NULL,
    email_digests                    boolean,
    external_links_in_new_tab        boolean   DEFAULT false            NOT NULL,
    enable_quoting                   boolean   DEFAULT true             NOT NULL,
    dynamic_favicon                  boolean   DEFAULT false            NOT NULL,
    disable_jump_reply               boolean   DEFAULT false            NOT NULL,
    automatically_unpin_topics       boolean   DEFAULT true             NOT NULL,
    digest_after_minutes             integer,
    auto_track_topics_after_msecs    integer,
    new_topic_duration_minutes       integer,
    last_redirected_to_top_at        timestamp without time zone,
    email_previous_replies           integer   DEFAULT 2                NOT NULL,
    email_in_reply_to                boolean   DEFAULT true             NOT NULL,
    like_notification_frequency      integer   DEFAULT 1                NOT NULL,
    mailing_list_mode_frequency      integer   DEFAULT 1                NOT NULL,
    include_tl0_in_digests           boolean   DEFAULT false,
    notification_level_when_replying integer,
    theme_key_seq                    integer   DEFAULT 0                NOT NULL,
    allow_private_messages           boolean   DEFAULT true             NOT NULL,
    homepage_id                      integer,
    theme_ids                        integer[] DEFAULT '{}':: integer[] NOT NULL,
    hide_profile_and_presence        boolean   DEFAULT false            NOT NULL,
    text_size_key                    integer   DEFAULT 0                NOT NULL,
    text_size_seq                    integer   DEFAULT 0                NOT NULL,
    email_level                      integer   DEFAULT 1                NOT NULL,
    email_messages_level             integer   DEFAULT 0                NOT NULL,
    title_count_mode_key             integer   DEFAULT 0                NOT NULL,
    enable_defer                     boolean   DEFAULT false            NOT NULL,
    timezone                         character varying
);

CREATE TABLE discourse.user_profile_views
(
    id              integer                     NOT NULL,
    user_profile_id integer                     NOT NULL,
    viewed_at       timestamp without time zone NOT NULL,
    ip_address      inet,
    user_id         integer
);

CREATE
    SEQUENCE discourse.user_profile_views_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.user_profile_views_id_seq OWNED BY discourse.user_profile_views.id;

CREATE TABLE discourse.user_profiles
(
    user_id                      integer           NOT NULL,
    location                     character varying,
    website                      character varying,
    bio_raw                      text,
    bio_cooked                   text,
    dismissed_banner_key         integer,
    bio_cooked_version           integer,
    badge_granted_title          boolean DEFAULT false,
    views                        integer DEFAULT 0 NOT NULL,
    profile_background_upload_id integer,
    card_background_upload_id    integer,
    granted_title_badge_id       bigint,
    featured_topic_id            integer
);

CREATE TABLE discourse.user_search_data
(
    user_id     integer NOT NULL,
    search_data tsvector,
    raw_data    text,
    locale      text,
    version     integer DEFAULT 0
);

CREATE TABLE discourse.user_second_factors
(
    id         bigint                      NOT NULL,
    user_id    integer                     NOT NULL,
    method     integer                     NOT NULL,
    data       character varying           NOT NULL,
    enabled    boolean DEFAULT false       NOT NULL,
    last_used  timestamp without time zone,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL,
    name       character varying
);

CREATE
    SEQUENCE discourse.user_second_factors_id_seq
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.user_second_factors_id_seq OWNED BY discourse.user_second_factors.id;

CREATE TABLE discourse.user_security_keys
(
    id            bigint                      NOT NULL,
    user_id       bigint                      NOT NULL,
    credential_id character varying           NOT NULL,
    public_key    character varying           NOT NULL,
    factor_type   integer DEFAULT 0           NOT NULL,
    enabled       boolean DEFAULT true        NOT NULL,
    name          character varying           NOT NULL,
    last_used     timestamp without time zone,
    created_at    timestamp without time zone NOT NULL,
    updated_at    timestamp without time zone NOT NULL
);

CREATE
    SEQUENCE discourse.user_security_keys_id_seq
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.user_security_keys_id_seq OWNED BY discourse.user_security_keys.id;

CREATE TABLE discourse.user_stats
(
    user_id                  integer                                               NOT NULL,
    topics_entered           integer                     DEFAULT 0                 NOT NULL,
    time_read                integer                     DEFAULT 0                 NOT NULL,
    days_visited             integer                     DEFAULT 0                 NOT NULL,
    posts_read_count         integer                     DEFAULT 0                 NOT NULL,
    likes_given              integer                     DEFAULT 0                 NOT NULL,
    likes_received           integer                     DEFAULT 0                 NOT NULL,
    topic_reply_count        integer                     DEFAULT 0                 NOT NULL,
    new_since                timestamp without time zone                           NOT NULL,
    read_faq                 timestamp without time zone,
    first_post_created_at    timestamp without time zone,
    post_count               integer                     DEFAULT 0                 NOT NULL,
    topic_count              integer                     DEFAULT 0                 NOT NULL,
    bounce_score             double precision            DEFAULT 0                 NOT NULL,
    reset_bounce_score_after timestamp without time zone,
    flags_agreed             integer                     DEFAULT 0                 NOT NULL,
    flags_disagreed          integer                     DEFAULT 0                 NOT NULL,
    flags_ignored            integer                     DEFAULT 0                 NOT NULL,
    first_unread_at          timestamp without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    distinct_badge_count     integer                     DEFAULT 0                 NOT NULL
);

CREATE TABLE discourse.user_uploads
(
    id         bigint                      NOT NULL,
    upload_id  integer                     NOT NULL,
    user_id    integer                     NOT NULL,
    created_at timestamp without time zone NOT NULL
);

CREATE
    SEQUENCE discourse.user_uploads_id_seq
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.user_uploads_id_seq OWNED BY discourse.user_uploads.id;

CREATE TABLE discourse.user_visits
(
    id         integer           NOT NULL,
    user_id    integer           NOT NULL,
    visited_at date              NOT NULL,
    posts_read integer DEFAULT 0,
    mobile     boolean DEFAULT false,
    time_read  integer DEFAULT 0 NOT NULL
);

CREATE
    SEQUENCE discourse.user_visits_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.user_visits_id_seq OWNED BY discourse.user_visits.id;

CREATE TABLE discourse.user_warnings
(
    id            integer                     NOT NULL,
    topic_id      integer                     NOT NULL,
    user_id       integer                     NOT NULL,
    created_by_id integer                     NOT NULL,
    created_at    timestamp without time zone NOT NULL,
    updated_at    timestamp without time zone NOT NULL
);

CREATE
    SEQUENCE discourse.user_warnings_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.user_warnings_id_seq OWNED BY discourse.user_warnings.id;

CREATE TABLE discourse.users
(
    id                        integer                     NOT NULL,
    username                  character varying(60)       NOT NULL,
    created_at                timestamp without time zone NOT NULL,
    updated_at                timestamp without time zone NOT NULL,
    name                      character varying,
    seen_notification_id      integer DEFAULT 0           NOT NULL,
    last_posted_at            timestamp without time zone,
    password_hash             character varying(64),
    salt                      character varying(32),
    active                    boolean DEFAULT false       NOT NULL,
    username_lower            character varying(60)       NOT NULL,
    last_seen_at              timestamp without time zone,
    admin                     boolean DEFAULT false       NOT NULL,
    last_emailed_at           timestamp without time zone,
    trust_level               integer                     NOT NULL,
    approved                  boolean DEFAULT false       NOT NULL,
    approved_by_id            integer,
    approved_at               timestamp without time zone,
    previous_visit_at         timestamp without time zone,
    suspended_at              timestamp without time zone,
    suspended_till            timestamp without time zone,
    date_of_birth             date,
    views                     integer DEFAULT 0           NOT NULL,
    flag_level                integer DEFAULT 0           NOT NULL,
    ip_address                inet,
    moderator                 boolean DEFAULT false,
    title                     character varying,
    uploaded_avatar_id        integer,
    locale                    character varying(10),
    primary_group_id          integer,
    registration_ip_address   inet,
    staged                    boolean DEFAULT false       NOT NULL,
    first_seen_at             timestamp without time zone,
    silenced_till             timestamp without time zone,
    group_locked_trust_level  integer,
    manual_locked_trust_level integer,
    secure_identifier         character varying
);

CREATE
    SEQUENCE discourse.users_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.users_id_seq OWNED BY discourse.users.id;

CREATE TABLE discourse.watched_words
(
    id         integer                     NOT NULL,
    word       character varying           NOT NULL,
    action     integer                     NOT NULL,
    created_at timestamp without time zone NOT NULL,
    updated_at timestamp without time zone NOT NULL
);

CREATE
    SEQUENCE discourse.watched_words_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.watched_words_id_seq OWNED BY discourse.watched_words.id;

CREATE TABLE discourse.web_crawler_requests
(
    id         bigint            NOT NULL,
    date       date              NOT NULL,
    user_agent character varying NOT NULL,
    count      integer DEFAULT 0 NOT NULL
);

CREATE
    SEQUENCE discourse.web_crawler_requests_id_seq
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.web_crawler_requests_id_seq OWNED BY discourse.web_crawler_requests.id;

CREATE TABLE discourse.web_hook_event_types
(
    id   integer           NOT NULL,
    name character varying NOT NULL
);

CREATE TABLE discourse.web_hook_event_types_hooks
(
    web_hook_id            integer NOT NULL,
    web_hook_event_type_id integer NOT NULL
);

CREATE
    SEQUENCE discourse.web_hook_event_types_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.web_hook_event_types_id_seq OWNED BY discourse.web_hook_event_types.id;

CREATE TABLE discourse.web_hook_events
(
    id               integer                     NOT NULL,
    web_hook_id      integer                     NOT NULL,
    headers          character varying,
    payload          text,
    status           integer DEFAULT 0,
    response_headers character varying,
    response_body    text,
    duration         integer DEFAULT 0,
    created_at       timestamp without time zone NOT NULL,
    updated_at       timestamp without time zone NOT NULL
);

CREATE
    SEQUENCE discourse.web_hook_events_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.web_hook_events_id_seq OWNED BY discourse.web_hook_events.id;

CREATE TABLE discourse.web_hooks
(
    id                   integer                         NOT NULL,
    payload_url          character varying               NOT NULL,
    content_type         integer           DEFAULT 1     NOT NULL,
    last_delivery_status integer           DEFAULT 1     NOT NULL,
    status               integer           DEFAULT 1     NOT NULL,
    secret               character varying DEFAULT '':: character varying,
    wildcard_web_hook    boolean           DEFAULT false NOT NULL,
    verify_certificate   boolean           DEFAULT true  NOT NULL,
    active               boolean           DEFAULT false NOT NULL,
    created_at           timestamp without time zone     NOT NULL,
    updated_at           timestamp without time zone     NOT NULL
);

CREATE
    SEQUENCE discourse.web_hooks_id_seq
    AS integer
    START
        WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE
        1;

ALTER
    SEQUENCE discourse.web_hooks_id_seq OWNED BY discourse.web_hooks.id;










































































































































    ADD CONSTRAINT anonymous_users_pkey PRIMARY KEY (id);

    ADD CONSTRAINT api_keys_pkey PRIMARY KEY (id);

    ADD CONSTRAINT application_requests_pkey PRIMARY KEY (id);

    ADD CONSTRAINT ar_internal_metadata_pkey PRIMARY KEY (key);

    ADD CONSTRAINT backup_draft_posts_pkey PRIMARY KEY (id);

    ADD CONSTRAINT backup_draft_topics_pkey PRIMARY KEY (id);

    ADD CONSTRAINT backup_metadata_pkey PRIMARY KEY (id);

    ADD CONSTRAINT badge_groupings_pkey PRIMARY KEY (id);

    ADD CONSTRAINT badge_types_pkey PRIMARY KEY (id);

    ADD CONSTRAINT badges_pkey PRIMARY KEY (id);

    ADD CONSTRAINT bookmarks_pkey PRIMARY KEY (id);

    ADD CONSTRAINT categories_pkey PRIMARY KEY (id);

    ADD CONSTRAINT categories_search_pkey PRIMARY KEY (category_id);

    ADD CONSTRAINT category_custom_fields_pkey PRIMARY KEY (id);

    ADD CONSTRAINT category_featured_topics_pkey PRIMARY KEY (id);

    ADD CONSTRAINT category_groups_pkey PRIMARY KEY (id);

    ADD CONSTRAINT category_tag_groups_pkey PRIMARY KEY (id);

    ADD CONSTRAINT category_tag_stats_pkey PRIMARY KEY (id);

    ADD CONSTRAINT category_tags_pkey PRIMARY KEY (id);

    ADD CONSTRAINT category_users_pkey PRIMARY KEY (id);

    ADD CONSTRAINT child_themes_pkey PRIMARY KEY (id);

    ADD CONSTRAINT color_scheme_colors_pkey PRIMARY KEY (id);

    ADD CONSTRAINT color_schemes_pkey PRIMARY KEY (id);

    ADD CONSTRAINT custom_emojis_pkey PRIMARY KEY (id);

    ADD CONSTRAINT developers_pkey PRIMARY KEY (id);

    ADD CONSTRAINT digest_unsubscribe_keys_pkey PRIMARY KEY (key);

    ADD CONSTRAINT directory_items_pkey PRIMARY KEY (id);

    ADD CONSTRAINT draft_sequences_pkey PRIMARY KEY (id);

    ADD CONSTRAINT drafts_pkey PRIMARY KEY (id);

    ADD CONSTRAINT email_change_requests_pkey PRIMARY KEY (id);

    ADD CONSTRAINT email_logs_pkey PRIMARY KEY (id);

    ADD CONSTRAINT email_tokens_pkey PRIMARY KEY (id);

    ADD CONSTRAINT embeddable_hosts_pkey PRIMARY KEY (id);

    ADD CONSTRAINT github_user_infos_pkey PRIMARY KEY (id);

    ADD CONSTRAINT group_archived_messages_pkey PRIMARY KEY (id);

    ADD CONSTRAINT group_custom_fields_pkey PRIMARY KEY (id);

    ADD CONSTRAINT group_histories_pkey PRIMARY KEY (id);

    ADD CONSTRAINT group_mentions_pkey PRIMARY KEY (id);

    ADD CONSTRAINT group_requests_pkey PRIMARY KEY (id);

    ADD CONSTRAINT group_users_pkey PRIMARY KEY (id);

    ADD CONSTRAINT groups_pkey PRIMARY KEY (id);

    ADD CONSTRAINT ignored_users_pkey PRIMARY KEY (id);

    ADD CONSTRAINT incoming_domains_pkey PRIMARY KEY (id);

    ADD CONSTRAINT incoming_emails_pkey PRIMARY KEY (id);

    ADD CONSTRAINT incoming_links_pkey PRIMARY KEY (id);

    ADD CONSTRAINT incoming_referers_pkey PRIMARY KEY (id);

    ADD CONSTRAINT invited_groups_pkey PRIMARY KEY (id);

    ADD CONSTRAINT invites_pkey PRIMARY KEY (id);

    ADD CONSTRAINT javascript_caches_pkey PRIMARY KEY (id);

    ADD CONSTRAINT message_bus_pkey PRIMARY KEY (id);

    ADD CONSTRAINT muted_users_pkey PRIMARY KEY (id);

    ADD CONSTRAINT notifications_pkey PRIMARY KEY (id);

    ADD CONSTRAINT oauth2_user_infos_pkey PRIMARY KEY (id);

    ADD CONSTRAINT onceoff_logs_pkey PRIMARY KEY (id);

    ADD CONSTRAINT optimized_images_pkey PRIMARY KEY (id);

    ADD CONSTRAINT permalinks_pkey PRIMARY KEY (id);

    ADD CONSTRAINT plugin_store_rows_pkey PRIMARY KEY (id);

    ADD CONSTRAINT poll_options_pkey PRIMARY KEY (id);

    ADD CONSTRAINT polls_pkey PRIMARY KEY (id);

    ADD CONSTRAINT post_action_types_pkey PRIMARY KEY (id);

    ADD CONSTRAINT post_actions_pkey PRIMARY KEY (id);

    ADD CONSTRAINT post_custom_fields_pkey PRIMARY KEY (id);

    ADD CONSTRAINT post_details_pkey PRIMARY KEY (id);

    ADD CONSTRAINT post_reply_keys_pkey PRIMARY KEY (id);

    ADD CONSTRAINT post_revisions_pkey PRIMARY KEY (id);

    ADD CONSTRAINT post_stats_pkey PRIMARY KEY (id);

    ADD CONSTRAINT post_uploads_pkey PRIMARY KEY (id);

    ADD CONSTRAINT posts_pkey PRIMARY KEY (id);

    ADD CONSTRAINT posts_search_pkey PRIMARY KEY (post_id);

    ADD CONSTRAINT push_subscriptions_pkey PRIMARY KEY (id);

    ADD CONSTRAINT quoted_posts_pkey PRIMARY KEY (id);

    ADD CONSTRAINT remote_themes_pkey PRIMARY KEY (id);

    ADD CONSTRAINT reviewable_claimed_topics_pkey PRIMARY KEY (id);

    ADD CONSTRAINT reviewable_histories_pkey PRIMARY KEY (id);

    ADD CONSTRAINT reviewable_scores_pkey PRIMARY KEY (id);

    ADD CONSTRAINT reviewables_pkey PRIMARY KEY (id);

    ADD CONSTRAINT scheduler_stats_pkey PRIMARY KEY (id);

    ADD CONSTRAINT schema_migration_details_pkey PRIMARY KEY (id);

    ADD CONSTRAINT schema_migrations_pkey PRIMARY KEY (version);

    ADD CONSTRAINT screened_emails_pkey PRIMARY KEY (id);

    ADD CONSTRAINT screened_ip_addresses_pkey PRIMARY KEY (id);

    ADD CONSTRAINT screened_urls_pkey PRIMARY KEY (id);

    ADD CONSTRAINT search_logs_pkey PRIMARY KEY (id);

    ADD CONSTRAINT shared_drafts_pkey PRIMARY KEY (id);

    ADD CONSTRAINT single_sign_on_records_pkey PRIMARY KEY (id);

    ADD CONSTRAINT site_settings_pkey PRIMARY KEY (id);

    ADD CONSTRAINT skipped_email_logs_pkey PRIMARY KEY (id);

    ADD CONSTRAINT stylesheet_cache_pkey PRIMARY KEY (id);

    ADD CONSTRAINT tag_group_memberships_pkey PRIMARY KEY (id);

    ADD CONSTRAINT tag_group_permissions_pkey PRIMARY KEY (id);

    ADD CONSTRAINT tag_groups_pkey PRIMARY KEY (id);

    ADD CONSTRAINT tag_search_data_pkey PRIMARY KEY (tag_id);

    ADD CONSTRAINT tag_users_pkey PRIMARY KEY (id);

    ADD CONSTRAINT tags_pkey PRIMARY KEY (id);

    ADD CONSTRAINT theme_fields_pkey PRIMARY KEY (id);

    ADD CONSTRAINT theme_modifier_sets_pkey PRIMARY KEY (id);

    ADD CONSTRAINT theme_settings_pkey PRIMARY KEY (id);

    ADD CONSTRAINT theme_translation_overrides_pkey PRIMARY KEY (id);

    ADD CONSTRAINT themes_pkey PRIMARY KEY (id);

    ADD CONSTRAINT top_topics_pkey PRIMARY KEY (id);

    ADD CONSTRAINT topic_allowed_groups_pkey PRIMARY KEY (id);

    ADD CONSTRAINT topic_allowed_users_pkey PRIMARY KEY (id);

    ADD CONSTRAINT topic_custom_fields_pkey PRIMARY KEY (id);

    ADD CONSTRAINT topic_embeds_pkey PRIMARY KEY (id);

    ADD CONSTRAINT topic_groups_pkey PRIMARY KEY (id);

    ADD CONSTRAINT topic_invites_pkey PRIMARY KEY (id);

    ADD CONSTRAINT topic_link_clicks_pkey PRIMARY KEY (id);

    ADD CONSTRAINT topic_links_pkey PRIMARY KEY (id);

    ADD CONSTRAINT topic_search_data_pkey PRIMARY KEY (topic_id);

    ADD CONSTRAINT topic_tags_pkey PRIMARY KEY (id);

    ADD CONSTRAINT topic_timers_pkey PRIMARY KEY (id);

    ADD CONSTRAINT topic_users_pkey PRIMARY KEY (id);

    ADD CONSTRAINT topics_pkey PRIMARY KEY (id);

    ADD CONSTRAINT translation_overrides_pkey PRIMARY KEY (id);

    ADD CONSTRAINT uploads_pkey PRIMARY KEY (id);

    ADD CONSTRAINT user_actions_pkey PRIMARY KEY (id);

    ADD CONSTRAINT user_api_keys_pkey PRIMARY KEY (id);

    ADD CONSTRAINT user_archived_messages_pkey PRIMARY KEY (id);

    ADD CONSTRAINT user_associated_accounts_pkey PRIMARY KEY (id);

    ADD CONSTRAINT user_auth_token_logs_pkey PRIMARY KEY (id);

    ADD CONSTRAINT user_auth_tokens_pkey PRIMARY KEY (id);

    ADD CONSTRAINT user_avatars_pkey PRIMARY KEY (id);

    ADD CONSTRAINT user_badges_pkey PRIMARY KEY (id);

    ADD CONSTRAINT user_custom_fields_pkey PRIMARY KEY (id);

    ADD CONSTRAINT user_emails_pkey PRIMARY KEY (id);

    ADD CONSTRAINT user_exports_pkey PRIMARY KEY (id);

    ADD CONSTRAINT user_field_options_pkey PRIMARY KEY (id);

    ADD CONSTRAINT user_fields_pkey PRIMARY KEY (id);

    ADD CONSTRAINT user_histories_pkey PRIMARY KEY (id);

    ADD CONSTRAINT user_open_ids_pkey PRIMARY KEY (id);

    ADD CONSTRAINT user_profile_views_pkey PRIMARY KEY (id);

    ADD CONSTRAINT user_profiles_pkey PRIMARY KEY (user_id);

    ADD CONSTRAINT user_second_factors_pkey PRIMARY KEY (id);

    ADD CONSTRAINT user_security_keys_pkey PRIMARY KEY (id);

    ADD CONSTRAINT user_stats_pkey PRIMARY KEY (user_id);

    ADD CONSTRAINT user_uploads_pkey PRIMARY KEY (id);

    ADD CONSTRAINT user_visits_pkey PRIMARY KEY (id);

    ADD CONSTRAINT user_warnings_pkey PRIMARY KEY (id);

    ADD CONSTRAINT users_pkey PRIMARY KEY (id);

    ADD CONSTRAINT users_search_pkey PRIMARY KEY (user_id);

    ADD CONSTRAINT watched_words_pkey PRIMARY KEY (id);

    ADD CONSTRAINT web_crawler_requests_pkey PRIMARY KEY (id);

    ADD CONSTRAINT web_hook_event_types_pkey PRIMARY KEY (id);

    ADD CONSTRAINT web_hook_events_pkey PRIMARY KEY (id);

    ADD CONSTRAINT web_hooks_pkey PRIMARY KEY (id);

CREATE INDEX associated_accounts_provider_uid ON discourse.user_associated_accounts USING btree (provider_name, provider_uid);

CREATE UNIQUE INDEX associated_accounts_provider_user ON discourse.user_associated_accounts USING btree (provider_name, user_id);

CREATE INDEX by_link ON discourse.topic_link_clicks USING btree (topic_link_id);

CREATE UNIQUE INDEX cat_featured_threads ON discourse.category_featured_topics USING btree (category_id, topic_id);

CREATE UNIQUE INDEX idx_category_tag_groups_ix1 ON discourse.category_tag_groups USING btree (category_id, tag_group_id);

CREATE UNIQUE INDEX idx_category_tags_ix1 ON discourse.category_tags USING btree (category_id, tag_id);

CREATE UNIQUE INDEX idx_category_tags_ix2 ON discourse.category_tags USING btree (tag_id, category_id);

CREATE UNIQUE INDEX idx_category_users_category_id_user_id ON discourse.category_users USING btree (category_id, user_id);

CREATE UNIQUE INDEX idx_category_users_user_id_category_id ON discourse.category_users USING btree (user_id, category_id);

CREATE INDEX idx_notifications_speedup_unread_count ON discourse.notifications USING btree (user_id, notification_type) WHERE (NOT read);

CREATE INDEX idx_post_custom_fields_akismet ON discourse.post_custom_fields USING btree (post_id) WHERE (
        ((name):: text = 'AKISMET_STATE':: text) AND (value = 'needs_review':: text));

CREATE INDEX idx_posts_created_at_topic_id ON discourse.posts USING btree (created_at, topic_id) WHERE (deleted_at IS NULL);

CREATE INDEX idx_posts_deleted_posts ON discourse.posts USING btree (topic_id, post_number) WHERE (deleted_at IS NOT NULL);

CREATE INDEX idx_posts_user_id_deleted_at ON discourse.posts USING btree (user_id) WHERE (deleted_at IS NULL);

CREATE INDEX idx_search_category ON discourse.category_search_data USING gin (search_data);

CREATE INDEX idx_search_post ON discourse.post_search_data USING gin (search_data);

CREATE INDEX idx_search_tag ON discourse.tag_search_data USING gin (search_data);

CREATE INDEX idx_search_topic ON discourse.topic_search_data USING gin (search_data);

CREATE INDEX idx_search_user ON discourse.user_search_data USING gin (search_data);

CREATE UNIQUE INDEX idx_tag_users_ix1 ON discourse.tag_users USING btree (user_id, tag_id, notification_level);

CREATE UNIQUE INDEX idx_tag_users_ix2 ON discourse.tag_users USING btree (tag_id, user_id, notification_level);

CREATE UNIQUE INDEX idx_topic_id_public_type_deleted_at ON discourse.topic_timers USING btree (topic_id) WHERE ((public_type = true) AND (deleted_at IS NULL));

CREATE INDEX idx_topics_front_page ON discourse.topics USING btree (deleted_at, visible, archetype, category_id, id);

CREATE INDEX idx_topics_user_id_deleted_at ON discourse.topics USING btree (user_id) WHERE (deleted_at IS NULL);

CREATE UNIQUE INDEX idx_unique_actions ON discourse.post_actions USING btree (user_id, post_action_type_id, post_id, targets_topic) WHERE (
        (deleted_at IS NULL) AND (disagreed_at IS NULL) AND (deferred_at IS NULL));

CREATE UNIQUE INDEX idx_unique_flags ON discourse.post_actions USING btree (user_id, post_id, targets_topic) WHERE (
        (deleted_at IS NULL) AND (disagreed_at IS NULL) AND (deferred_at IS NULL) AND
        (post_action_type_id = ANY (ARRAY [3, 4, 7, 8])));

CREATE UNIQUE INDEX idx_unique_post_uploads ON discourse.post_uploads USING btree (post_id, upload_id);

CREATE UNIQUE INDEX idx_unique_rows ON discourse.user_actions USING btree (action_type, user_id,
                                                                        target_topic_id,
                                                                        target_post_id,
                                                                        acting_user_id);

CREATE INDEX idx_user_actions_speed_up_user_all ON discourse.user_actions USING btree (user_id, created_at, action_type);

CREATE INDEX idx_users_admin ON discourse.users USING btree (id) WHERE admin;

CREATE INDEX idx_users_moderator ON discourse.users USING btree (id) WHERE moderator;

CREATE UNIQUE INDEX idx_web_hook_event_types_hooks_on_ids ON discourse.web_hook_event_types_hooks USING btree (web_hook_event_type_id, web_hook_id);

CREATE INDEX idxtopicslug ON discourse.topics USING btree (slug) WHERE ((deleted_at IS NULL) AND (slug IS NOT NULL));

CREATE UNIQUE INDEX index_anonymous_users_on_master_user_id ON discourse.anonymous_users USING btree (master_user_id) WHERE active;

CREATE UNIQUE INDEX index_anonymous_users_on_user_id ON discourse.anonymous_users USING btree (user_id);

CREATE INDEX index_api_keys_on_key_hash ON discourse.api_keys USING btree (key_hash);

CREATE INDEX index_api_keys_on_user_id ON discourse.api_keys USING btree (user_id);

CREATE UNIQUE INDEX index_application_requests_on_date_and_req_type ON discourse.application_requests USING btree (date, req_type);

CREATE UNIQUE INDEX index_backup_draft_posts_on_post_id ON discourse.backup_draft_posts USING btree (post_id);

CREATE UNIQUE INDEX index_backup_draft_posts_on_user_id_and_key ON discourse.backup_draft_posts USING btree (user_id, key);

CREATE UNIQUE INDEX index_backup_draft_topics_on_topic_id ON discourse.backup_draft_topics USING btree (topic_id);

CREATE UNIQUE INDEX index_backup_draft_topics_on_user_id ON discourse.backup_draft_topics USING btree (user_id);

CREATE UNIQUE INDEX index_badge_types_on_name ON discourse.badge_types USING btree (name);

CREATE INDEX index_badges_on_badge_type_id ON discourse.badges USING btree (badge_type_id);

CREATE UNIQUE INDEX index_badges_on_name ON discourse.badges USING btree (name);

CREATE INDEX index_bookmarks_on_post_id ON discourse.bookmarks USING btree (post_id);

CREATE INDEX index_bookmarks_on_reminder_at ON discourse.bookmarks USING btree (reminder_at);

CREATE INDEX index_bookmarks_on_reminder_set_at ON discourse.bookmarks USING btree (reminder_set_at);

CREATE INDEX index_bookmarks_on_reminder_type ON discourse.bookmarks USING btree (reminder_type);

CREATE INDEX index_bookmarks_on_topic_id ON discourse.bookmarks USING btree (topic_id);

CREATE INDEX index_bookmarks_on_user_id ON discourse.bookmarks USING btree (user_id);

CREATE UNIQUE INDEX index_bookmarks_on_user_id_and_post_id ON discourse.bookmarks USING btree (user_id, post_id);

CREATE UNIQUE INDEX index_categories_on_email_in ON discourse.categories USING btree (email_in);

CREATE INDEX index_categories_on_reviewable_by_group_id ON discourse.categories USING btree (reviewable_by_group_id);

CREATE INDEX index_categories_on_search_priority ON discourse.categories USING btree (search_priority);

CREATE INDEX index_categories_on_topic_count ON discourse.categories USING btree (topic_count);

CREATE UNIQUE INDEX index_categories_web_hooks_on_web_hook_id_and_category_id ON discourse.categories_web_hooks USING btree (web_hook_id, category_id);

CREATE INDEX index_category_custom_fields_on_category_id_and_name ON discourse.category_custom_fields USING btree (category_id, name);

CREATE INDEX index_category_featured_topics_on_category_id_and_rank ON discourse.category_featured_topics USING btree (category_id, rank);

-- CREATE INDEX index_category_groups_on_group_id ON discourse.category_groups USING btree (group_id);
CREATE INDEX index_category_groups_on_category_id_and_group_id ON discourse.category_groups USING btree (category_id, group_id);

CREATE INDEX index_category_groups_on_group_id_and_category_id ON discourse.category_groups USING btree (group_id, category_id);

CREATE INDEX index_category_tag_stats_on_category_id ON discourse.category_tag_stats USING btree (category_id);

CREATE UNIQUE INDEX index_category_tag_stats_on_category_id_and_tag_id ON discourse.category_tag_stats USING btree (category_id, tag_id);

CREATE INDEX index_category_tag_stats_on_category_id_and_topic_count ON discourse.category_tag_stats USING btree (category_id, topic_count);

CREATE INDEX index_category_tag_stats_on_tag_id ON discourse.category_tag_stats USING btree (tag_id);

CREATE INDEX index_category_users_on_user_id_and_last_seen_at ON discourse.category_users USING btree (user_id, last_seen_at);

CREATE UNIQUE INDEX index_child_themes_on_child_theme_id_and_parent_theme_id ON discourse.child_themes USING btree (child_theme_id, parent_theme_id);

CREATE UNIQUE INDEX index_child_themes_on_parent_theme_id_and_child_theme_id ON discourse.child_themes USING btree (parent_theme_id, child_theme_id);

CREATE INDEX index_color_scheme_colors_on_color_scheme_id ON discourse.color_scheme_colors USING btree (color_scheme_id);

CREATE UNIQUE INDEX index_custom_emojis_on_name ON discourse.custom_emojis USING btree (name);

CREATE UNIQUE INDEX index_developers_on_user_id ON discourse.developers USING btree (user_id);

CREATE INDEX index_directory_items_on_days_visited ON discourse.directory_items USING btree (days_visited);

CREATE INDEX index_directory_items_on_likes_given ON discourse.directory_items USING btree (likes_given);

CREATE INDEX index_directory_items_on_likes_received ON discourse.directory_items USING btree (likes_received);

CREATE UNIQUE INDEX index_directory_items_on_period_type_and_user_id ON discourse.directory_items USING btree (period_type, user_id);

CREATE INDEX index_directory_items_on_post_count ON discourse.directory_items USING btree (post_count);

CREATE INDEX index_directory_items_on_posts_read ON discourse.directory_items USING btree (posts_read);

CREATE INDEX index_directory_items_on_topic_count ON discourse.directory_items USING btree (topic_count);

CREATE INDEX index_directory_items_on_topics_entered ON discourse.directory_items USING btree (topics_entered);

CREATE UNIQUE INDEX index_draft_sequences_on_user_id_and_draft_key ON discourse.draft_sequences USING btree (user_id, draft_key);

CREATE UNIQUE INDEX index_drafts_on_user_id_and_draft_key ON discourse.drafts USING btree (user_id, draft_key);

CREATE INDEX index_email_change_requests_on_user_id ON discourse.email_change_requests USING btree (user_id);

CREATE UNIQUE INDEX index_email_logs_on_bounce_key ON discourse.email_logs USING btree (bounce_key) WHERE (bounce_key IS NOT NULL);

CREATE INDEX index_email_logs_on_bounced ON discourse.email_logs USING btree (bounced);

CREATE INDEX index_email_logs_on_created_at ON discourse.email_logs USING btree (created_at DESC);

CREATE INDEX index_email_logs_on_message_id ON discourse.email_logs USING btree (message_id);

CREATE INDEX index_email_logs_on_post_id ON discourse.email_logs USING btree (post_id);

CREATE INDEX index_email_logs_on_user_id ON discourse.email_logs USING btree (user_id);

CREATE UNIQUE INDEX index_email_tokens_on_token ON discourse.email_tokens USING btree (token);

CREATE INDEX index_email_tokens_on_user_id ON discourse.email_tokens USING btree (user_id);

CREATE INDEX index_for_rebake_old ON discourse.posts USING btree (id DESC) WHERE (
        ((baked_version IS NULL) OR (baked_version < 2)) AND (deleted_at IS NULL));

CREATE UNIQUE INDEX index_github_user_infos_on_github_user_id ON discourse.github_user_infos USING btree (github_user_id);

CREATE UNIQUE INDEX index_github_user_infos_on_user_id ON discourse.github_user_infos USING btree (user_id);

CREATE INDEX index_given_daily_likes_on_limit_reached_and_user_id ON discourse.given_daily_likes USING btree (limit_reached, user_id);

CREATE UNIQUE INDEX index_given_daily_likes_on_user_id_and_given_date ON discourse.given_daily_likes USING btree (user_id, given_date);

CREATE UNIQUE INDEX index_group_archived_messages_on_group_id_and_topic_id ON discourse.group_archived_messages USING btree (group_id, topic_id);

CREATE INDEX index_group_custom_fields_on_group_id_and_name ON discourse.group_custom_fields USING btree (group_id, name);

CREATE INDEX index_group_histories_on_acting_user_id ON discourse.group_histories USING btree (acting_user_id);

CREATE INDEX index_group_histories_on_action ON discourse.group_histories USING btree (action);

CREATE INDEX index_group_histories_on_group_id ON discourse.group_histories USING btree (group_id);

CREATE INDEX index_group_histories_on_target_user_id ON discourse.group_histories USING btree (target_user_id);

CREATE UNIQUE INDEX index_group_mentions_on_group_id_and_post_id ON discourse.group_mentions USING btree (group_id, post_id);

CREATE UNIQUE INDEX index_group_mentions_on_post_id_and_group_id ON discourse.group_mentions USING btree (post_id, group_id);

CREATE INDEX index_group_requests_on_group_id ON discourse.group_requests USING btree (group_id);

CREATE UNIQUE INDEX index_group_requests_on_group_id_and_user_id ON discourse.group_requests USING btree (group_id, user_id);

CREATE INDEX index_group_requests_on_user_id ON discourse.group_requests USING btree (user_id);

CREATE UNIQUE INDEX index_group_users_on_group_id_and_user_id ON discourse.group_users USING btree (group_id, user_id);

CREATE UNIQUE INDEX index_group_users_on_user_id_and_group_id ON discourse.group_users USING btree (user_id, group_id);

CREATE UNIQUE INDEX index_groups_on_incoming_email ON discourse.groups USING btree (incoming_email);

CREATE UNIQUE INDEX index_groups_on_name ON discourse.groups USING btree (name);

CREATE UNIQUE INDEX index_groups_web_hooks_on_web_hook_id_and_group_id ON discourse.groups_web_hooks USING btree (web_hook_id, group_id);

CREATE UNIQUE INDEX index_ignored_users_on_ignored_user_id_and_user_id ON discourse.ignored_users USING btree (ignored_user_id, user_id);

CREATE UNIQUE INDEX index_ignored_users_on_user_id_and_ignored_user_id ON discourse.ignored_users USING btree (user_id, ignored_user_id);

CREATE UNIQUE INDEX index_incoming_domains_on_name_and_https_and_port ON discourse.incoming_domains USING btree (name, https, port);

CREATE INDEX index_incoming_emails_on_created_at ON discourse.incoming_emails USING btree (created_at);

CREATE INDEX index_incoming_emails_on_error ON discourse.incoming_emails USING btree (error);

CREATE INDEX index_incoming_emails_on_message_id ON discourse.incoming_emails USING btree (message_id);

CREATE INDEX index_incoming_emails_on_post_id ON discourse.incoming_emails USING btree (post_id);

CREATE INDEX index_incoming_emails_on_user_id ON discourse.incoming_emails USING btree (user_id) WHERE (user_id IS NOT NULL);

CREATE INDEX index_incoming_links_on_created_at_and_user_id ON discourse.incoming_links USING btree (created_at, user_id);

CREATE INDEX index_incoming_links_on_post_id ON discourse.incoming_links USING btree (post_id);

CREATE UNIQUE INDEX index_incoming_referers_on_path_and_incoming_domain_id ON discourse.incoming_referers USING btree (path, incoming_domain_id);

CREATE INDEX index_invites_on_email_and_invited_by_id ON discourse.invites USING btree (email, invited_by_id);

CREATE INDEX index_invites_on_emailed_status ON discourse.invites USING btree (emailed_status);

CREATE UNIQUE INDEX index_invites_on_invite_key ON discourse.invites USING btree (invite_key);

CREATE INDEX index_invites_on_invited_by_id ON discourse.invites USING btree (invited_by_id);

CREATE INDEX index_javascript_caches_on_digest ON discourse.javascript_caches USING btree (digest);

CREATE INDEX index_javascript_caches_on_theme_field_id ON discourse.javascript_caches USING btree (theme_field_id);

CREATE INDEX index_javascript_caches_on_theme_id ON discourse.javascript_caches USING btree (theme_id);

CREATE INDEX index_message_bus_on_created_at ON discourse.message_bus USING btree (created_at);

CREATE UNIQUE INDEX index_muted_users_on_muted_user_id_and_user_id ON discourse.muted_users USING btree (muted_user_id, user_id);

CREATE UNIQUE INDEX index_muted_users_on_user_id_and_muted_user_id ON discourse.muted_users USING btree (user_id, muted_user_id);

CREATE INDEX index_notifications_on_post_action_id ON discourse.notifications USING btree (post_action_id);

CREATE UNIQUE INDEX index_notifications_on_read_or_n_type ON discourse.notifications USING btree (user_id, id DESC, read, topic_id) WHERE (read OR (notification_type <> 6));

CREATE INDEX index_notifications_on_topic_id_and_post_number ON discourse.notifications USING btree (topic_id, post_number);

CREATE INDEX index_notifications_on_user_id_and_created_at ON discourse.notifications USING btree (user_id, created_at);

CREATE UNIQUE INDEX index_notifications_on_user_id_and_id ON discourse.notifications USING btree (user_id, id) WHERE ((notification_type = 6) AND (NOT read));

CREATE INDEX index_notifications_on_user_id_and_topic_id_and_post_number ON discourse.notifications USING btree (user_id, topic_id, post_number);

CREATE UNIQUE INDEX index_oauth2_user_infos_on_uid_and_provider ON discourse.oauth2_user_infos USING btree (uid, provider);

CREATE INDEX index_oauth2_user_infos_on_user_id_and_provider ON discourse.oauth2_user_infos USING btree (user_id, provider);

CREATE INDEX index_onceoff_logs_on_job_name ON discourse.onceoff_logs USING btree (job_name);

CREATE INDEX index_optimized_images_on_etag ON discourse.optimized_images USING btree (etag);

CREATE INDEX index_optimized_images_on_upload_id ON discourse.optimized_images USING btree (upload_id);

CREATE UNIQUE INDEX index_optimized_images_on_upload_id_and_width_and_height ON discourse.optimized_images USING btree (upload_id, width, height);

CREATE UNIQUE INDEX index_permalinks_on_url ON discourse.permalinks USING btree (url);

CREATE UNIQUE INDEX index_plugin_store_rows_on_plugin_name_and_key ON discourse.plugin_store_rows USING btree (plugin_name, key);

CREATE INDEX index_poll_options_on_poll_id ON discourse.poll_options USING btree (poll_id);

CREATE UNIQUE INDEX index_poll_options_on_poll_id_and_digest ON discourse.poll_options USING btree (poll_id, digest);

CREATE INDEX index_poll_votes_on_poll_id ON discourse.poll_votes USING btree (poll_id);

CREATE UNIQUE INDEX index_poll_votes_on_poll_id_and_poll_option_id_and_user_id ON discourse.poll_votes USING btree (poll_id, poll_option_id, user_id);

CREATE INDEX index_poll_votes_on_poll_option_id ON discourse.poll_votes USING btree (poll_option_id);

CREATE INDEX index_poll_votes_on_user_id ON discourse.poll_votes USING btree (user_id);

CREATE INDEX index_polls_on_post_id ON discourse.polls USING btree (post_id);

CREATE UNIQUE INDEX index_polls_on_post_id_and_name ON discourse.polls USING btree (post_id, name);

CREATE INDEX index_post_actions_on_post_action_type_id_and_disagreed_at ON discourse.post_actions USING btree (post_action_type_id, disagreed_at) WHERE (disagreed_at IS NULL);

CREATE INDEX index_post_actions_on_post_id ON discourse.post_actions USING btree (post_id);

CREATE INDEX index_post_actions_on_user_id ON discourse.post_actions USING btree (user_id);

CREATE INDEX index_post_actions_on_user_id_and_post_action_type_id ON discourse.post_actions USING btree (user_id, post_action_type_id) WHERE (deleted_at IS NULL);

CREATE INDEX index_post_custom_fields_on_name_and_value ON discourse.post_custom_fields USING btree (name, "left"(value, 200));

CREATE UNIQUE INDEX index_post_custom_fields_on_notice_args ON discourse.post_custom_fields USING btree (post_id) WHERE ((name):: text = 'notice_args':: text);

CREATE UNIQUE INDEX index_post_custom_fields_on_notice_type ON discourse.post_custom_fields USING btree (post_id) WHERE ((name):: text = 'notice_type':: text);

CREATE UNIQUE INDEX index_post_custom_fields_on_post_id ON discourse.post_custom_fields USING btree (post_id) WHERE ((name):: text = 'missing uploads':: text);

CREATE INDEX index_post_custom_fields_on_post_id_and_name ON discourse.post_custom_fields USING btree (post_id, name);

CREATE UNIQUE INDEX index_post_details_on_post_id_and_key ON discourse.post_details USING btree (post_id, key);

CREATE UNIQUE INDEX index_post_id_where_missing_uploads_ignored ON discourse.post_custom_fields USING btree (post_id) WHERE ((name):: text = 'missing uploads ignored':: text);

CREATE UNIQUE INDEX index_post_replies_on_post_id_and_reply_post_id ON discourse.post_replies USING btree (post_id, reply_post_id);

CREATE INDEX index_post_replies_on_reply_post_id ON discourse.post_replies USING btree (reply_post_id);

CREATE UNIQUE INDEX index_post_reply_keys_on_reply_key ON discourse.post_reply_keys USING btree (reply_key);

CREATE UNIQUE INDEX index_post_reply_keys_on_user_id_and_post_id ON discourse.post_reply_keys USING btree (user_id, post_id);

CREATE INDEX index_post_revisions_on_post_id ON discourse.post_revisions USING btree (post_id);

CREATE INDEX index_post_revisions_on_post_id_and_number ON discourse.post_revisions USING btree (post_id, number);

CREATE INDEX index_post_search_data_on_post_id_and_version_and_locale ON discourse.post_search_data USING btree (post_id, version, locale);

CREATE INDEX index_post_stats_on_post_id ON discourse.post_stats USING btree (post_id);

CREATE INDEX index_post_timings_on_user_id ON discourse.post_timings USING btree (user_id);

CREATE INDEX index_post_uploads_on_post_id ON discourse.post_uploads USING btree (post_id);

CREATE INDEX index_post_uploads_on_upload_id ON discourse.post_uploads USING btree (upload_id);

CREATE INDEX index_posts_on_id_and_baked_version ON discourse.posts USING btree (id DESC, baked_version) WHERE (deleted_at IS NULL);

CREATE INDEX index_posts_on_reply_to_post_number ON discourse.posts USING btree (reply_to_post_number);

CREATE INDEX index_posts_on_topic_id_and_percent_rank ON discourse.posts USING btree (topic_id, percent_rank);

CREATE UNIQUE INDEX index_posts_on_topic_id_and_post_number ON discourse.posts USING btree (topic_id, post_number);

CREATE INDEX index_posts_on_topic_id_and_sort_order ON discourse.posts USING btree (topic_id, sort_order);

CREATE INDEX index_posts_on_user_id_and_created_at ON discourse.posts USING btree (user_id, created_at);

CREATE UNIQUE INDEX index_quoted_posts_on_post_id_and_quoted_post_id ON discourse.quoted_posts USING btree (post_id, quoted_post_id);

CREATE UNIQUE INDEX index_quoted_posts_on_quoted_post_id_and_post_id ON discourse.quoted_posts USING btree (quoted_post_id, post_id);

CREATE UNIQUE INDEX index_reviewable_claimed_topics_on_topic_id ON discourse.reviewable_claimed_topics USING btree (topic_id);

CREATE INDEX index_reviewable_histories_on_created_by_id ON discourse.reviewable_histories USING btree (created_by_id);

CREATE INDEX index_reviewable_histories_on_reviewable_id ON discourse.reviewable_histories USING btree (reviewable_id);

CREATE INDEX index_reviewable_scores_on_reviewable_id ON discourse.reviewable_scores USING btree (reviewable_id);

CREATE INDEX index_reviewable_scores_on_user_id ON discourse.reviewable_scores USING btree (user_id);

CREATE INDEX index_reviewables_on_reviewable_by_group_id ON discourse.reviewables USING btree (reviewable_by_group_id);

CREATE INDEX index_reviewables_on_status_and_created_at ON discourse.reviewables USING btree (status, created_at);

CREATE INDEX index_reviewables_on_status_and_score ON discourse.reviewables USING btree (status, score);

CREATE INDEX index_reviewables_on_status_and_type ON discourse.reviewables USING btree (status, type);

CREATE INDEX index_reviewables_on_topic_id_and_status_and_created_by_id ON discourse.reviewables USING btree (topic_id, status, created_by_id);

CREATE UNIQUE INDEX index_reviewables_on_type_and_target_id ON discourse.reviewables USING btree (type, target_id);

CREATE INDEX index_schema_migration_details_on_version ON discourse.schema_migration_details USING btree (version);

CREATE UNIQUE INDEX index_screened_emails_on_email ON discourse.screened_emails USING btree (email);

CREATE INDEX index_screened_emails_on_last_match_at ON discourse.screened_emails USING btree (last_match_at);

CREATE UNIQUE INDEX index_screened_ip_addresses_on_ip_address ON discourse.screened_ip_addresses USING btree (ip_address);

CREATE INDEX index_screened_ip_addresses_on_last_match_at ON discourse.screened_ip_addresses USING btree (last_match_at);

CREATE INDEX index_screened_urls_on_last_match_at ON discourse.screened_urls USING btree (last_match_at);

CREATE UNIQUE INDEX index_screened_urls_on_url ON discourse.screened_urls USING btree (url);

CREATE INDEX index_search_logs_on_created_at ON discourse.search_logs USING btree (created_at);

CREATE INDEX index_shared_drafts_on_category_id ON discourse.shared_drafts USING btree (category_id);

CREATE UNIQUE INDEX index_shared_drafts_on_topic_id ON discourse.shared_drafts USING btree (topic_id);

CREATE UNIQUE INDEX index_single_sign_on_records_on_external_id ON discourse.single_sign_on_records USING btree (external_id);

CREATE INDEX index_single_sign_on_records_on_user_id ON discourse.single_sign_on_records USING btree (user_id);

CREATE UNIQUE INDEX index_site_settings_on_name ON discourse.site_settings USING btree (name);

CREATE INDEX index_skipped_email_logs_on_created_at ON discourse.skipped_email_logs USING btree (created_at);

CREATE INDEX index_skipped_email_logs_on_post_id ON discourse.skipped_email_logs USING btree (post_id);

CREATE INDEX index_skipped_email_logs_on_reason_type ON discourse.skipped_email_logs USING btree (reason_type);

CREATE INDEX index_skipped_email_logs_on_user_id ON discourse.skipped_email_logs USING btree (user_id);

CREATE UNIQUE INDEX index_stylesheet_cache_on_target_and_digest ON discourse.stylesheet_cache USING btree (target, digest);

CREATE UNIQUE INDEX index_tag_group_memberships_on_tag_group_id_and_tag_id ON discourse.tag_group_memberships USING btree (tag_group_id, tag_id);

CREATE INDEX index_tag_group_permissions_on_group_id ON discourse.tag_group_permissions USING btree (group_id);

CREATE INDEX index_tag_group_permissions_on_tag_group_id ON discourse.tag_group_permissions USING btree (tag_group_id);

CREATE UNIQUE INDEX index_tags_on_lower_name ON discourse.tags USING btree (lower((name):: text));

CREATE UNIQUE INDEX index_tags_on_name ON discourse.tags USING btree (name);

CREATE UNIQUE INDEX index_theme_modifier_sets_on_theme_id ON discourse.theme_modifier_sets USING btree (theme_id);

CREATE INDEX index_theme_translation_overrides_on_theme_id ON discourse.theme_translation_overrides USING btree (theme_id);

CREATE UNIQUE INDEX index_themes_on_remote_theme_id ON discourse.themes USING btree (remote_theme_id);

CREATE INDEX index_top_topics_on_all_score ON discourse.top_topics USING btree (all_score);

CREATE INDEX index_top_topics_on_daily_likes_count ON discourse.top_topics USING btree (daily_likes_count DESC);

CREATE INDEX index_top_topics_on_daily_op_likes_count ON discourse.top_topics USING btree (daily_op_likes_count);

CREATE INDEX index_top_topics_on_daily_posts_count ON discourse.top_topics USING btree (daily_posts_count DESC);

CREATE INDEX index_top_topics_on_daily_score ON discourse.top_topics USING btree (daily_score);

CREATE INDEX index_top_topics_on_daily_views_count ON discourse.top_topics USING btree (daily_views_count DESC);

CREATE INDEX index_top_topics_on_monthly_likes_count ON discourse.top_topics USING btree (monthly_likes_count DESC);

CREATE INDEX index_top_topics_on_monthly_op_likes_count ON discourse.top_topics USING btree (monthly_op_likes_count);

CREATE INDEX index_top_topics_on_monthly_posts_count ON discourse.top_topics USING btree (monthly_posts_count DESC);

CREATE INDEX index_top_topics_on_monthly_score ON discourse.top_topics USING btree (monthly_score);

CREATE INDEX index_top_topics_on_monthly_views_count ON discourse.top_topics USING btree (monthly_views_count DESC);

CREATE INDEX index_top_topics_on_quarterly_likes_count ON discourse.top_topics USING btree (quarterly_likes_count);

CREATE INDEX index_top_topics_on_quarterly_op_likes_count ON discourse.top_topics USING btree (quarterly_op_likes_count);

CREATE INDEX index_top_topics_on_quarterly_posts_count ON discourse.top_topics USING btree (quarterly_posts_count);

CREATE INDEX index_top_topics_on_quarterly_views_count ON discourse.top_topics USING btree (quarterly_views_count);

CREATE UNIQUE INDEX index_top_topics_on_topic_id ON discourse.top_topics USING btree (topic_id);

CREATE INDEX index_top_topics_on_weekly_likes_count ON discourse.top_topics USING btree (weekly_likes_count DESC);

CREATE INDEX index_top_topics_on_weekly_op_likes_count ON discourse.top_topics USING btree (weekly_op_likes_count);

CREATE INDEX index_top_topics_on_weekly_posts_count ON discourse.top_topics USING btree (weekly_posts_count DESC);

CREATE INDEX index_top_topics_on_weekly_score ON discourse.top_topics USING btree (weekly_score);

CREATE INDEX index_top_topics_on_weekly_views_count ON discourse.top_topics USING btree (weekly_views_count DESC);

CREATE INDEX index_top_topics_on_yearly_likes_count ON discourse.top_topics USING btree (yearly_likes_count DESC);

CREATE INDEX index_top_topics_on_yearly_op_likes_count ON discourse.top_topics USING btree (yearly_op_likes_count);

CREATE INDEX index_top_topics_on_yearly_posts_count ON discourse.top_topics USING btree (yearly_posts_count DESC);

CREATE INDEX index_top_topics_on_yearly_score ON discourse.top_topics USING btree (yearly_score);

CREATE INDEX index_top_topics_on_yearly_views_count ON discourse.top_topics USING btree (yearly_views_count DESC);

CREATE UNIQUE INDEX index_topic_allowed_groups_on_group_id_and_topic_id ON discourse.topic_allowed_groups USING btree (group_id, topic_id);

CREATE UNIQUE INDEX index_topic_allowed_groups_on_topic_id_and_group_id ON discourse.topic_allowed_groups USING btree (topic_id, group_id);

CREATE UNIQUE INDEX index_topic_allowed_users_on_topic_id_and_user_id ON discourse.topic_allowed_users USING btree (topic_id, user_id);

CREATE UNIQUE INDEX index_topic_allowed_users_on_user_id_and_topic_id ON discourse.topic_allowed_users USING btree (user_id, topic_id);

CREATE INDEX index_topic_custom_fields_on_topic_id_and_name ON discourse.topic_custom_fields USING btree (topic_id, name);

CREATE UNIQUE INDEX index_topic_embeds_on_embed_url ON discourse.topic_embeds USING btree (embed_url);

CREATE UNIQUE INDEX index_topic_groups_on_group_id_and_topic_id ON discourse.topic_groups USING btree (group_id, topic_id);

CREATE INDEX index_topic_invites_on_invite_id ON discourse.topic_invites USING btree (invite_id);

CREATE UNIQUE INDEX index_topic_invites_on_topic_id_and_invite_id ON discourse.topic_invites USING btree (topic_id, invite_id);

CREATE INDEX index_topic_links_on_extension ON discourse.topic_links USING btree (extension);

CREATE INDEX index_topic_links_on_link_post_id_and_reflection ON discourse.topic_links USING btree (link_post_id, reflection);

CREATE INDEX index_topic_links_on_post_id ON discourse.topic_links USING btree (post_id);

CREATE INDEX index_topic_links_on_topic_id ON discourse.topic_links USING btree (topic_id);

CREATE INDEX index_topic_links_on_user_id ON discourse.topic_links USING btree (user_id);

CREATE INDEX index_topic_search_data_on_topic_id_and_version_and_locale ON discourse.topic_search_data USING btree (topic_id, version, locale);

CREATE UNIQUE INDEX index_topic_tags_on_topic_id_and_tag_id ON discourse.topic_tags USING btree (topic_id, tag_id);

CREATE INDEX index_topic_timers_on_user_id ON discourse.topic_timers USING btree (user_id);

CREATE UNIQUE INDEX index_topic_users_on_topic_id_and_user_id ON discourse.topic_users USING btree (topic_id, user_id);

CREATE UNIQUE INDEX index_topic_users_on_user_id_and_topic_id ON discourse.topic_users USING btree (user_id, topic_id);

CREATE INDEX index_topic_views_on_topic_id_and_viewed_at ON discourse.topic_views USING btree (topic_id, viewed_at);

CREATE INDEX index_topic_views_on_user_id_and_viewed_at ON discourse.topic_views USING btree (user_id, viewed_at);

CREATE INDEX index_topic_views_on_viewed_at_and_topic_id ON discourse.topic_views USING btree (viewed_at, topic_id);

CREATE INDEX index_topics_on_bumped_at ON discourse.topics USING btree (bumped_at DESC);

CREATE INDEX index_topics_on_created_at_and_visible ON discourse.topics USING btree (created_at, visible) WHERE (
        (deleted_at IS NULL) AND ((archetype):: text <> 'private_message':: text));

CREATE INDEX index_topics_on_id_and_deleted_at ON discourse.topics USING btree (id, deleted_at);

CREATE UNIQUE INDEX index_topics_on_id_filtered_banner ON discourse.topics USING btree (id) WHERE (
        ((archetype):: text = 'banner':: text) AND (deleted_at IS NULL));

CREATE INDEX index_topics_on_lower_title ON discourse.topics USING btree (lower((title):: text));

CREATE INDEX index_topics_on_pinned_at ON discourse.topics USING btree (pinned_at) WHERE (pinned_at IS NOT NULL);

CREATE INDEX index_topics_on_pinned_globally ON discourse.topics USING btree (pinned_globally) WHERE pinned_globally;

CREATE INDEX index_topics_on_updated_at_public ON discourse.topics USING btree (updated_at, visible,
                                                                             highest_staff_post_number,
                                                                             highest_post_number,
                                                                             category_id,
                                                                             created_at, id) WHERE (
        ((archetype):: text <> 'private_message':: text) AND (deleted_at IS NULL));

CREATE UNIQUE INDEX index_translation_overrides_on_locale_and_translation_key ON discourse.translation_overrides USING btree (locale, translation_key);

CREATE INDEX index_unsubscribe_keys_on_created_at ON discourse.unsubscribe_keys USING btree (created_at);

CREATE INDEX index_uploads_on_access_control_post_id ON discourse.uploads USING btree (access_control_post_id);

CREATE INDEX index_uploads_on_etag ON discourse.uploads USING btree (etag);

CREATE INDEX index_uploads_on_extension ON discourse.uploads USING btree (lower((extension):: text));

CREATE INDEX index_uploads_on_id_and_url ON discourse.uploads USING btree (id, url);

CREATE INDEX index_uploads_on_original_sha1 ON discourse.uploads USING btree (original_sha1);

CREATE UNIQUE INDEX index_uploads_on_sha1 ON discourse.uploads USING btree (sha1);

CREATE INDEX index_uploads_on_url ON discourse.uploads USING btree (url);

CREATE INDEX index_uploads_on_user_id ON discourse.uploads USING btree (user_id);

CREATE INDEX index_user_actions_on_acting_user_id ON discourse.user_actions USING btree (acting_user_id);

CREATE INDEX index_user_actions_on_action_type_and_created_at ON discourse.user_actions USING btree (action_type, created_at);

CREATE INDEX index_user_actions_on_target_post_id ON discourse.user_actions USING btree (target_post_id);

CREATE INDEX index_user_actions_on_target_user_id ON discourse.user_actions USING btree (target_user_id) WHERE (target_user_id IS NOT NULL);

CREATE INDEX index_user_actions_on_user_id_and_action_type ON discourse.user_actions USING btree (user_id, action_type);

CREATE UNIQUE INDEX index_user_api_keys_on_client_id ON discourse.user_api_keys USING btree (client_id);

CREATE UNIQUE INDEX index_user_api_keys_on_key ON discourse.user_api_keys USING btree (key);

CREATE INDEX index_user_api_keys_on_user_id ON discourse.user_api_keys USING btree (user_id);

CREATE UNIQUE INDEX index_user_archived_messages_on_user_id_and_topic_id ON discourse.user_archived_messages USING btree (user_id, topic_id);

CREATE INDEX index_user_auth_token_logs_on_user_id ON discourse.user_auth_token_logs USING btree (user_id);

CREATE UNIQUE INDEX index_user_auth_tokens_on_auth_token ON discourse.user_auth_tokens USING btree (auth_token);

CREATE UNIQUE INDEX index_user_auth_tokens_on_prev_auth_token ON discourse.user_auth_tokens USING btree (prev_auth_token);

CREATE INDEX index_user_auth_tokens_on_user_id ON discourse.user_auth_tokens USING btree (user_id);

CREATE INDEX index_user_avatars_on_custom_upload_id ON discourse.user_avatars USING btree (custom_upload_id);

CREATE INDEX index_user_avatars_on_gravatar_upload_id ON discourse.user_avatars USING btree (gravatar_upload_id);

CREATE INDEX index_user_avatars_on_user_id ON discourse.user_avatars USING btree (user_id);

CREATE INDEX index_user_badges_on_badge_id_and_user_id ON discourse.user_badges USING btree (badge_id, user_id);

CREATE UNIQUE INDEX index_user_badges_on_badge_id_and_user_id_and_post_id ON discourse.user_badges USING btree (badge_id, user_id, post_id) WHERE (post_id IS NOT NULL);

CREATE UNIQUE INDEX index_user_badges_on_badge_id_and_user_id_and_seq ON discourse.user_badges USING btree (badge_id, user_id, seq) WHERE (post_id IS NULL);

CREATE INDEX index_user_badges_on_user_id ON discourse.user_badges USING btree (user_id);

CREATE INDEX index_user_custom_fields_on_user_id_and_name ON discourse.user_custom_fields USING btree (user_id, name);

-- CREATE UNIQUE INDEX index_user_emails_on_email ON discourse.user_emails USING btree (lower((email):: text));
CREATE UNIQUE INDEX index_user_emails_on_email ON discourse.user_emails USING btree (email);

CREATE INDEX index_user_emails_on_user_id ON discourse.user_emails USING btree (user_id);

-- CREATE UNIQUE INDEX index_user_emails_on_user_id_and_primary ON discourse.user_emails USING btree (user_id, "primary") WHERE "primary";
CREATE INDEX index_user_emails_on_user_id_and_primary ON discourse.user_emails USING btree (user_id, "primary") WHERE "primary";

CREATE INDEX index_user_histories_on_acting_user_id_and_action_and_id ON discourse.user_histories USING btree (acting_user_id, action, id);

CREATE INDEX index_user_histories_on_action_and_id ON discourse.user_histories USING btree (action, id);

CREATE INDEX index_user_histories_on_category_id ON discourse.user_histories USING btree (category_id);

CREATE INDEX index_user_histories_on_subject_and_id ON discourse.user_histories USING btree (subject, id);

CREATE INDEX index_user_histories_on_target_user_id_and_id ON discourse.user_histories USING btree (target_user_id, id);

CREATE INDEX index_user_histories_on_topic_id_and_target_user_id_and_action ON discourse.user_histories USING btree (topic_id, target_user_id, action);

CREATE INDEX index_user_open_ids_on_url ON discourse.user_open_ids USING btree (url);

CREATE UNIQUE INDEX index_user_options_on_user_id ON discourse.user_options USING btree (user_id);

CREATE INDEX index_user_profile_views_on_user_id ON discourse.user_profile_views USING btree (user_id);

CREATE INDEX index_user_profile_views_on_user_profile_id ON discourse.user_profile_views USING btree (user_profile_id);

CREATE INDEX index_user_profiles_on_bio_cooked_version ON discourse.user_profiles USING btree (bio_cooked_version);

CREATE INDEX index_user_profiles_on_card_background_upload_id ON discourse.user_profiles USING btree (card_background_upload_id);

CREATE INDEX index_user_profiles_on_granted_title_badge_id ON discourse.user_profiles USING btree (granted_title_badge_id);

CREATE INDEX index_user_profiles_on_profile_background_upload_id ON discourse.user_profiles USING btree (profile_background_upload_id);

CREATE INDEX index_user_second_factors_on_method_and_enabled ON discourse.user_second_factors USING btree (method, enabled);

CREATE INDEX index_user_second_factors_on_user_id ON discourse.user_second_factors USING btree (user_id);

CREATE UNIQUE INDEX index_user_security_keys_on_credential_id ON discourse.user_security_keys USING btree (credential_id);

CREATE INDEX index_user_security_keys_on_factor_type ON discourse.user_security_keys USING btree (factor_type);

CREATE INDEX index_user_security_keys_on_factor_type_and_enabled ON discourse.user_security_keys USING btree (factor_type, enabled);

CREATE INDEX index_user_security_keys_on_last_used ON discourse.user_security_keys USING btree (last_used);

CREATE INDEX index_user_security_keys_on_public_key ON discourse.user_security_keys USING btree (public_key);

CREATE INDEX index_user_security_keys_on_user_id ON discourse.user_security_keys USING btree (user_id);

CREATE UNIQUE INDEX index_user_uploads_on_upload_id_and_user_id ON discourse.user_uploads USING btree (upload_id, user_id);

CREATE INDEX index_user_uploads_on_user_id_and_upload_id ON discourse.user_uploads USING btree (user_id, upload_id);

CREATE UNIQUE INDEX index_user_visits_on_user_id_and_visited_at ON discourse.user_visits USING btree (user_id, visited_at);

CREATE INDEX index_user_visits_on_user_id_and_visited_at_and_time_read ON discourse.user_visits USING btree (user_id, visited_at, time_read);

CREATE INDEX index_user_visits_on_visited_at_and_mobile ON discourse.user_visits USING btree (visited_at, mobile);

CREATE UNIQUE INDEX index_user_warnings_on_topic_id ON discourse.user_warnings USING btree (topic_id);

CREATE INDEX index_user_warnings_on_user_id ON discourse.user_warnings USING btree (user_id);

CREATE INDEX index_users_on_last_posted_at ON discourse.users USING btree (last_posted_at);

CREATE INDEX index_users_on_last_seen_at ON discourse.users USING btree (last_seen_at);

CREATE UNIQUE INDEX index_users_on_secure_identifier ON discourse.users USING btree (secure_identifier);

CREATE INDEX index_users_on_uploaded_avatar_id ON discourse.users USING btree (uploaded_avatar_id);

CREATE UNIQUE INDEX index_users_on_username ON discourse.users USING btree (username);

CREATE UNIQUE INDEX index_users_on_username_lower ON discourse.users USING btree (username_lower);

CREATE UNIQUE INDEX index_watched_words_on_action_and_word ON discourse.watched_words USING btree (action, word);

CREATE UNIQUE INDEX index_web_crawler_requests_on_date_and_user_agent ON discourse.web_crawler_requests USING btree (date, user_agent);

CREATE INDEX index_web_hook_events_on_web_hook_id ON discourse.web_hook_events USING btree (web_hook_id);

CREATE UNIQUE INDEX post_custom_field_broken_images_idx ON discourse.post_custom_fields USING btree (post_id) WHERE ((name):: text = 'broken_images':: text);

CREATE UNIQUE INDEX post_custom_field_downloaded_images_idx ON discourse.post_custom_fields USING btree (post_id) WHERE ((name):: text = 'downloaded_images':: text);

CREATE UNIQUE INDEX post_custom_field_large_images_idx ON discourse.post_custom_fields USING btree (post_id) WHERE ((name):: text = 'large_images':: text);

CREATE INDEX post_timings_summary ON discourse.post_timings USING btree (topic_id, post_number);

CREATE UNIQUE INDEX post_timings_unique ON discourse.post_timings USING btree (topic_id, post_number, user_id);

CREATE UNIQUE INDEX theme_field_unique_index ON discourse.theme_fields USING btree (theme_id, target_id, type_id, name);

CREATE UNIQUE INDEX theme_translation_overrides_unique ON discourse.theme_translation_overrides USING btree (theme_id, locale, translation_key);

CREATE INDEX topic_custom_fields_value_key_idx ON discourse.topic_custom_fields USING btree (value, name) WHERE ((value IS NOT NULL) AND (char_length(value) < 400));

CREATE UNIQUE INDEX uniq_ip_or_user_id_topic_views ON discourse.topic_views USING btree (user_id, ip_address, topic_id);

CREATE UNIQUE INDEX unique_index_categories_on_name ON discourse.categories USING btree (COALESCE(parent_category_id, '-1':: integer), name);

CREATE UNIQUE INDEX unique_index_categories_on_slug ON discourse.categories USING btree (COALESCE(parent_category_id, '-1':: integer), slug) WHERE ((slug):: text <> '':: text);

CREATE UNIQUE INDEX unique_post_links ON discourse.topic_links USING btree (topic_id, post_id, url);

CREATE UNIQUE INDEX unique_profile_view_user_or_ip ON discourse.user_profile_views USING btree (viewed_at, user_id, ip_address, user_profile_id);

CREATE UNIQUE INDEX web_hooks_tags ON discourse.tags_web_hooks USING btree (web_hook_id, tag_id);

    ADD CONSTRAINT fk_rails_1d362f2e97 FOREIGN KEY (profile_background_upload_id) REFERENCES discourse.uploads (id);

    ADD CONSTRAINT fk_rails_272c56774b FOREIGN KEY (topic_id) REFERENCES discourse.topics (id);

    ADD CONSTRAINT fk_rails_38ea484ed4 FOREIGN KEY (granted_title_badge_id) REFERENCES discourse.badges (id);

    ADD CONSTRAINT fk_rails_58f94aecc4 FOREIGN KEY (theme_id) REFERENCES discourse.themes (id) ON
        DELETE CASCADE;

    ADD CONSTRAINT fk_rails_848ece0184 FOREIGN KEY (poll_option_id) REFERENCES discourse.poll_options (id);

    ADD CONSTRAINT fk_rails_8b89adf296 FOREIGN KEY (access_control_post_id) REFERENCES discourse.posts (id);

    ADD CONSTRAINT fk_rails_90999b0454 FOREIGN KEY (user_id) REFERENCES discourse.users (id);

    ADD CONSTRAINT fk_rails_a6e6974b7e FOREIGN KEY (poll_id) REFERENCES discourse.polls (id);

    ADD CONSTRAINT fk_rails_aa85becb42 FOREIGN KEY (poll_id) REFERENCES discourse.polls (id);

    ADD CONSTRAINT fk_rails_b50b782d08 FOREIGN KEY (post_id) REFERENCES discourse.posts (id);

    ADD CONSTRAINT fk_rails_b64de9b025 FOREIGN KEY (user_id) REFERENCES discourse.users (id);

    ADD CONSTRAINT fk_rails_c1ff6fa4ac FOREIGN KEY (user_id) REFERENCES discourse.users (id);

    ADD CONSTRAINT fk_rails_ca64aa462b FOREIGN KEY (card_background_upload_id) REFERENCES discourse.uploads (id);

    ADD CONSTRAINT fk_rails_d8b54790ff FOREIGN KEY (post_id) REFERENCES discourse.posts (id);

    ADD CONSTRAINT fk_rails_ed33506dbd FOREIGN KEY (theme_field_id) REFERENCES discourse.theme_fields (id) ON
        DELETE CASCADE;
