SELECT
  DISTINCT people.id AS id,
  people.guid AS guid,
  people.diaspora_handle AS diaspora_handle,
  people.serialized_public_key AS serialized_public_key,
  people.owner_id AS owner_id,
  people.created_at AS created_at,
  people.updated_at AS updated_at,
  people.closed_account AS closed_account,
  people.fetch_status AS fetch_status,
  people.pod_id AS pod_id
FROM
  contacts AS contacts
  INNER JOIN aspect_memberships AS aspect_memberships ON aspect_memberships.contact_id = contacts.id
  INNER JOIN people AS people ON contacts.person_id = people.id
WHERE
  contacts.user_id = 488
  AND aspect_memberships.aspect_id = 322
LIMIT
  15
OFFSET
  0