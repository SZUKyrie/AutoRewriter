


CREATE TABLE IF NOT EXISTS spree."action_mailbox_inbound_emails" (
  "id" bigint NOT NULL,
  "status" integer NOT NULL DEFAULT '0',
  "message_id" varchar(255) NOT NULL,
  "message_checksum" varchar(255) NOT NULL,
  "created_at" timestamp(6) NOT NULL,
  "updated_at" timestamp(6) NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."action_text_rich_texts" (
  "id" bigint NOT NULL,
  "name" varchar(255) NOT NULL,
  "body" text,
  "record_type" varchar(255) NOT NULL,
  "record_id" bigint NOT NULL,
  "created_at" timestamp(6) NOT NULL,
  "updated_at" timestamp(6) NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."active_storage_attachments" (
  "id" bigint NOT NULL,
  "name" varchar(255) NOT NULL,
  "record_type" varchar(255) NOT NULL,
  "record_id" bigint NOT NULL,
  "blob_id" bigint NOT NULL,
  "created_at" timestamp NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."active_storage_blobs" (
  "id" bigint NOT NULL,
  "filename" varchar(255) NOT NULL,
  "content_type" varchar(255) DEFAULT NULL,
  "metadata" text,
  "byte_size" bigint NOT NULL,
  "checksum" varchar(255) NOT NULL,
  "created_at" timestamp NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."ar_internal_metadata" (
  "value" varchar(255) DEFAULT NULL,
  "created_at" timestamp(6) NOT NULL,
  "updated_at" timestamp(6) NOT NULL,
  PRIMARY KEY (key)
);


CREATE TABLE IF NOT EXISTS spree."friendly_id_slugs" (
  "id" integer NOT NULL,
  "slug" varchar(255) NOT NULL,
  "sluggable_id" integer NOT NULL,
  "sluggable_type" varchar(50) DEFAULT NULL,
  "scope" varchar(255) DEFAULT NULL,
  "created_at" timestamp DEFAULT NULL,
  "deleted_at" timestamp DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."schema_migrations" (
  "version" varchar(255) NOT NULL,
  PRIMARY KEY (version)
);


CREATE TABLE IF NOT EXISTS spree."spree_addresses" (
  "id" integer NOT NULL,
  "firstname" varchar(255) DEFAULT NULL,
  "lastname" varchar(255) DEFAULT NULL,
  "address1" varchar(255) DEFAULT NULL,
  "address2" varchar(255) DEFAULT NULL,
  "city" varchar(255) DEFAULT NULL,
  "zipcode" varchar(255) DEFAULT NULL,
  "phone" varchar(255) DEFAULT NULL,
  "state_name" varchar(255) DEFAULT NULL,
  "alternative_phone" varchar(255) DEFAULT NULL,
  "company" varchar(255) DEFAULT NULL,
  "state_id" integer DEFAULT NULL,
  "country_id" integer DEFAULT NULL,
  "created_at" timestamp(6) NOT NULL,
  "updated_at" timestamp(6) NOT NULL,
  "user_id" integer DEFAULT NULL,
  "deleted_at" timestamp DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."spree_adjustments" (
  "id" integer NOT NULL,
  "source_type" varchar(255) DEFAULT NULL,
  "source_id" integer DEFAULT NULL,
  "adjustable_type" varchar(255) DEFAULT NULL,
  "adjustable_id" integer DEFAULT NULL,
  "amount" decimal(10,2) DEFAULT NULL,
  "label" varchar(255) DEFAULT NULL,
  "mandatory" boolean DEFAULT NULL,
  "eligible" boolean DEFAULT '1',
  "created_at" timestamp(6) NOT NULL,
  "updated_at" timestamp(6) NOT NULL,
  "state" varchar(255) DEFAULT NULL,
  "order_id" integer NOT NULL,
  "included" boolean DEFAULT '0',
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."spree_assets" (
  "id" integer NOT NULL,
  "viewable_type" varchar(255) DEFAULT NULL,
  "viewable_id" integer DEFAULT NULL,
  "attachment_width" integer DEFAULT NULL,
  "attachment_height" integer DEFAULT NULL,
  "attachment_file_size" integer DEFAULT NULL,
  "position" integer DEFAULT NULL,
  "attachment_content_type" varchar(255) DEFAULT NULL,
  "attachment_file_name" varchar(255) DEFAULT NULL,
  "type" varchar(75) DEFAULT NULL,
  "attachment_updated_at" timestamp DEFAULT NULL,
  "alt" text,
  "created_at" timestamp DEFAULT NULL,
  "updated_at" timestamp DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."spree_calculators" (
  "id" integer NOT NULL,
  "type" varchar(255) DEFAULT NULL,
  "calculable_type" varchar(255) DEFAULT NULL,
  "calculable_id" integer DEFAULT NULL,
  "created_at" timestamp(6) NOT NULL,
  "updated_at" timestamp(6) NOT NULL,
  "preferences" text,
  "deleted_at" timestamp DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."spree_countries" (
  "id" integer NOT NULL,
  "iso_name" varchar(255) DEFAULT NULL,
  "iso" varchar(255) NOT NULL,
  "iso3" varchar(255) NOT NULL,
  "name" varchar(255) DEFAULT NULL,
  "numcode" integer DEFAULT NULL,
  "states_required" boolean DEFAULT '0',
  "updated_at" timestamp DEFAULT NULL,
  "zipcode_required" boolean DEFAULT '1',
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."spree_credit_cards" (
  "id" integer NOT NULL,
  "month" varchar(255) DEFAULT NULL,
  "year" varchar(255) DEFAULT NULL,
  "cc_type" varchar(255) DEFAULT NULL,
  "last_digits" varchar(255) DEFAULT NULL,
  "address_id" integer DEFAULT NULL,
  "gateway_customer_profile_id" varchar(255) DEFAULT NULL,
  "gateway_payment_profile_id" varchar(255) DEFAULT NULL,
  "created_at" timestamp(6) NOT NULL,
  "updated_at" timestamp(6) NOT NULL,
  "name" varchar(255) DEFAULT NULL,
  "user_id" integer DEFAULT NULL,
  "payment_method_id" integer DEFAULT NULL,
  "default" boolean NOT NULL DEFAULT '0',
  "deleted_at" timestamp DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."spree_customer_returns" (
  "id" integer NOT NULL,
  "number" varchar(255) DEFAULT NULL,
  "stock_location_id" integer DEFAULT NULL,
  "created_at" timestamp(6) NOT NULL,
  "updated_at" timestamp(6) NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."spree_dummy_models" (
  "id" bigint NOT NULL,
  "name" varchar(255) DEFAULT NULL,
  "position" integer DEFAULT NULL,
  "created_at" timestamp NOT NULL,
  "updated_at" timestamp NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."spree_gateways" (
  "id" integer NOT NULL,
  "type" varchar(255) DEFAULT NULL,
  "name" varchar(255) DEFAULT NULL,
  "description" text,
  "active" boolean DEFAULT '1',
  "environment" varchar(255) DEFAULT 'development',
  "server" varchar(255) DEFAULT 'test',
  "test_mode" boolean DEFAULT '1',
  "created_at" timestamp(6) NOT NULL,
  "updated_at" timestamp(6) NOT NULL,
  "preferences" text,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."spree_inventory_units" (
  "id" integer NOT NULL,
  "state" varchar(255) DEFAULT NULL,
  "variant_id" integer DEFAULT NULL,
  "order_id" integer DEFAULT NULL,
  "shipment_id" integer DEFAULT NULL,
  "created_at" timestamp(6) NOT NULL,
  "updated_at" timestamp(6) NOT NULL,
  "pending" boolean DEFAULT '1',
  "line_item_id" integer DEFAULT NULL,
  "quantity" integer DEFAULT '1',
  "original_return_item_id" integer DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."spree_line_items" (
  "id" integer NOT NULL,
  "variant_id" integer DEFAULT NULL,
  "order_id" integer DEFAULT NULL,
  "quantity" integer NOT NULL,
  "price" decimal(10,2) NOT NULL,
  "created_at" timestamp(6) NOT NULL,
  "updated_at" timestamp(6) NOT NULL,
  "currency" varchar(255) DEFAULT NULL,
  "cost_price" decimal(10,2) DEFAULT NULL,
  "tax_category_id" integer DEFAULT NULL,
  "adjustment_total" decimal(10,2) DEFAULT '0.00',
  "additional_tax_total" decimal(10,2) DEFAULT '0.00',
  "promo_total" decimal(10,2) DEFAULT '0.00',
  "included_tax_total" decimal(10,2) NOT NULL DEFAULT '0.00',
  "pre_tax_amount" decimal(12,4) NOT NULL DEFAULT '0.0000',
  "taxable_adjustment_total" decimal(10,2) NOT NULL DEFAULT '0.00',
  "non_taxable_adjustment_total" decimal(10,2) NOT NULL DEFAULT '0.00',
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."spree_log_entries" (
  "id" integer NOT NULL,
  "source_type" varchar(255) DEFAULT NULL,
  "source_id" integer DEFAULT NULL,
  "details" text,
  "created_at" timestamp(6) NOT NULL,
  "updated_at" timestamp(6) NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."spree_oauth_access_grants" (
  "id" bigint NOT NULL,
  "resource_owner_id" integer NOT NULL,
  "application_id" bigint NOT NULL,
  "token" varchar(255) NOT NULL,
  "expires_in" integer NOT NULL,
  "redirect_uri" text NOT NULL,
  "created_at" timestamp NOT NULL,
  "revoked_at" timestamp DEFAULT NULL,
  "scopes" varchar(255) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."spree_oauth_access_tokens" (
  "id" bigint NOT NULL,
  "resource_owner_id" integer DEFAULT NULL,
  "application_id" bigint DEFAULT NULL,
  "token" varchar(255) NOT NULL,
  "refresh_token" varchar(255) DEFAULT NULL,
  "expires_in" integer DEFAULT NULL,
  "revoked_at" timestamp DEFAULT NULL,
  "created_at" timestamp NOT NULL,
  "scopes" varchar(255) DEFAULT NULL,
  "previous_refresh_token" varchar(255) NOT NULL DEFAULT '',
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."spree_oauth_applications" (
  "id" bigint NOT NULL,
  "name" varchar(255) NOT NULL,
  "uid" varchar(255) NOT NULL,
  "secret" varchar(255) NOT NULL,
  "redirect_uri" text NOT NULL,
  "scopes" varchar(255) NOT NULL DEFAULT '',
  "confidential" boolean NOT NULL DEFAULT '1',
  "created_at" timestamp NOT NULL,
  "updated_at" timestamp NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."spree_option_type_prototypes" (
  "prototype_id" integer DEFAULT NULL,
  "option_type_id" integer DEFAULT NULL,
  "id" integer NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."spree_option_types" (
  "id" integer NOT NULL,
  "name" varchar(100) DEFAULT NULL,
  "presentation" varchar(100) DEFAULT NULL,
  "position" integer NOT NULL DEFAULT '0',
  "created_at" timestamp(6) NOT NULL,
  "updated_at" timestamp(6) NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."spree_option_value_variants" (
  "variant_id" integer DEFAULT NULL,
  "option_value_id" integer DEFAULT NULL,
  "id" integer NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."spree_option_values" (
  "id" integer NOT NULL,
  "position" integer DEFAULT NULL,
  "name" varchar(255) DEFAULT NULL,
  "presentation" varchar(255) DEFAULT NULL,
  "option_type_id" integer DEFAULT NULL,
  "created_at" timestamp(6) NOT NULL,
  "updated_at" timestamp(6) NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."spree_order_promotions" (
  "order_id" integer DEFAULT NULL,
  "promotion_id" integer DEFAULT NULL,
  "id" integer NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."spree_orders" (
  "id" integer NOT NULL,
  "number" varchar(32) DEFAULT NULL,
  "item_total" decimal(10,2) NOT NULL DEFAULT '0.00',
  "total" decimal(10,2) NOT NULL DEFAULT '0.00',
  "state" varchar(255) DEFAULT NULL,
  "adjustment_total" decimal(10,2) NOT NULL DEFAULT '0.00',
  "user_id" integer DEFAULT NULL,
  "completed_at" timestamp DEFAULT NULL,
  "bill_address_id" integer DEFAULT NULL,
  "ship_address_id" integer DEFAULT NULL,
  "payment_total" decimal(10,2) DEFAULT '0.00',
  "shipment_state" varchar(255) DEFAULT NULL,
  "payment_state" varchar(255) DEFAULT NULL,
  "email" varchar(255) DEFAULT NULL,
  "special_instructions" text,
  "created_at" timestamp(6) NOT NULL,
  "updated_at" timestamp(6) NOT NULL,
  "currency" varchar(255) DEFAULT NULL,
  "last_ip_address" varchar(255) DEFAULT NULL,
  "created_by_id" integer DEFAULT NULL,
  "shipment_total" decimal(10,2) NOT NULL DEFAULT '0.00',
  "additional_tax_total" decimal(10,2) DEFAULT '0.00',
  "promo_total" decimal(10,2) DEFAULT '0.00',
  "channel" varchar(255) DEFAULT 'spree',
  "included_tax_total" decimal(10,2) NOT NULL DEFAULT '0.00',
  "item_count" integer DEFAULT '0',
  "approver_id" integer DEFAULT NULL,
  "approved_at" timestamp DEFAULT NULL,
  "confirmation_delivered" boolean DEFAULT '0',
  "considered_risky" boolean DEFAULT '0',
  "token" varchar(255) DEFAULT NULL,
  "canceled_at" timestamp DEFAULT NULL,
  "canceler_id" integer DEFAULT NULL,
  "store_id" integer DEFAULT NULL,
  "state_lock_version" integer NOT NULL DEFAULT '0',
  "taxable_adjustment_total" decimal(10,2) NOT NULL DEFAULT '0.00',
  "non_taxable_adjustment_total" decimal(10,2) NOT NULL DEFAULT '0.00',
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."spree_payment_capture_events" (
  "id" integer NOT NULL,
  "amount" decimal(10,2) DEFAULT '0.00',
  "payment_id" integer DEFAULT NULL,
  "created_at" timestamp(6) NOT NULL,
  "updated_at" timestamp(6) NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."spree_payment_methods" (
  "id" integer NOT NULL,
  "type" varchar(255) DEFAULT NULL,
  "name" varchar(255) DEFAULT NULL,
  "description" text,
  "active" boolean DEFAULT '1',
  "deleted_at" timestamp DEFAULT NULL,
  "created_at" timestamp(6) NOT NULL,
  "updated_at" timestamp(6) NOT NULL,
  "display_on" varchar(255) DEFAULT 'both',
  "auto_capture" boolean DEFAULT NULL,
  "preferences" text,
  "position" integer DEFAULT '0',
  "store_id" bigint DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."spree_payments" (
  "id" integer NOT NULL,
  "amount" decimal(10,2) NOT NULL DEFAULT '0.00',
  "order_id" integer DEFAULT NULL,
  "source_type" varchar(255) DEFAULT NULL,
  "source_id" integer DEFAULT NULL,
  "payment_method_id" integer DEFAULT NULL,
  "state" varchar(255) DEFAULT NULL,
  "response_code" varchar(255) DEFAULT NULL,
  "avs_response" varchar(255) DEFAULT NULL,
  "created_at" timestamp(6) NOT NULL,
  "updated_at" timestamp(6) NOT NULL,
  "number" varchar(255) DEFAULT NULL,
  "cvv_response_code" varchar(255) DEFAULT NULL,
  "cvv_response_message" varchar(255) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."spree_preferences" (
  "id" integer NOT NULL,
  "value" text,
  "created_at" timestamp(6) NOT NULL,
  "updated_at" timestamp(6) NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."spree_prices" (
  "id" integer NOT NULL,
  "variant_id" integer NOT NULL,
  "amount" decimal(10,2) DEFAULT NULL,
  "currency" varchar(255) DEFAULT NULL,
  "deleted_at" timestamp DEFAULT NULL,
  "created_at" timestamp NOT NULL,
  "updated_at" timestamp NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."spree_product_option_types" (
  "id" integer NOT NULL,
  "position" integer DEFAULT NULL,
  "product_id" integer DEFAULT NULL,
  "option_type_id" integer DEFAULT NULL,
  "created_at" timestamp(6) NOT NULL,
  "updated_at" timestamp(6) NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."spree_product_promotion_rules" (
  "product_id" integer DEFAULT NULL,
  "promotion_rule_id" integer DEFAULT NULL,
  "id" integer NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."spree_product_properties" (
  "id" integer NOT NULL,
  "value" varchar(255) DEFAULT NULL,
  "product_id" integer DEFAULT NULL,
  "property_id" integer DEFAULT NULL,
  "created_at" timestamp(6) NOT NULL,
  "updated_at" timestamp(6) NOT NULL,
  "position" integer DEFAULT '0',
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."spree_products" (
  "id" integer NOT NULL,
  "name" varchar(255) NOT NULL DEFAULT '',
  "description" text,
  "available_on" timestamp DEFAULT NULL,
  "discontinue_on" timestamp DEFAULT NULL,
  "deleted_at" timestamp DEFAULT NULL,
  "slug" varchar(255) DEFAULT NULL,
  "meta_description" text,
  "meta_keywords" varchar(255) DEFAULT NULL,
  "tax_category_id" integer DEFAULT NULL,
  "shipping_category_id" integer DEFAULT NULL,
  "created_at" timestamp(6) NOT NULL,
  "updated_at" timestamp(6) NOT NULL,
  "promotionable" boolean DEFAULT '1',
  "meta_title" varchar(255) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."spree_products_taxons" (
  "product_id" integer DEFAULT NULL,
  "taxon_id" integer DEFAULT NULL,
  "id" integer NOT NULL,
  "position" integer DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."spree_promotion_action_line_items" (
  "id" integer NOT NULL,
  "promotion_action_id" integer DEFAULT NULL,
  "variant_id" integer DEFAULT NULL,
  "quantity" integer DEFAULT '1',
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."spree_promotion_actions" (
  "id" integer NOT NULL,
  "promotion_id" integer DEFAULT NULL,
  "position" integer DEFAULT NULL,
  "type" varchar(255) DEFAULT NULL,
  "deleted_at" timestamp DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."spree_promotion_categories" (
  "id" integer NOT NULL,
  "name" varchar(255) DEFAULT NULL,
  "created_at" timestamp(6) NOT NULL,
  "updated_at" timestamp(6) NOT NULL,
  "code" varchar(255) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."spree_promotion_rule_taxons" (
  "id" integer NOT NULL,
  "taxon_id" integer DEFAULT NULL,
  "promotion_rule_id" integer DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."spree_promotion_rule_users" (
  "user_id" integer DEFAULT NULL,
  "promotion_rule_id" integer DEFAULT NULL,
  "id" integer NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."spree_promotion_rules" (
  "id" integer NOT NULL,
  "promotion_id" integer DEFAULT NULL,
  "user_id" integer DEFAULT NULL,
  "product_group_id" integer DEFAULT NULL,
  "type" varchar(255) DEFAULT NULL,
  "created_at" timestamp(6) NOT NULL,
  "updated_at" timestamp(6) NOT NULL,
  "code" varchar(255) DEFAULT NULL,
  "preferences" text,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."spree_promotions" (
  "id" integer NOT NULL,
  "description" varchar(255) DEFAULT NULL,
  "expires_at" timestamp DEFAULT NULL,
  "starts_at" timestamp DEFAULT NULL,
  "name" varchar(255) DEFAULT NULL,
  "type" varchar(255) DEFAULT NULL,
  "usage_limit" integer DEFAULT NULL,
  "match_policy" varchar(255) DEFAULT 'all',
  "code" varchar(255) DEFAULT NULL,
  "advertise" boolean DEFAULT '0',
  "path" varchar(255) DEFAULT NULL,
  "created_at" timestamp(6) NOT NULL,
  "updated_at" timestamp(6) NOT NULL,
  "promotion_category_id" integer DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."spree_properties" (
  "id" integer NOT NULL,
  "name" varchar(255) DEFAULT NULL,
  "presentation" varchar(255) NOT NULL,
  "created_at" timestamp(6) NOT NULL,
  "updated_at" timestamp(6) NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."spree_property_prototypes" (
  "prototype_id" integer DEFAULT NULL,
  "property_id" integer DEFAULT NULL,
  "id" integer NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."spree_prototype_taxons" (
  "id" integer NOT NULL,
  "taxon_id" integer DEFAULT NULL,
  "prototype_id" integer DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."spree_prototypes" (
  "id" integer NOT NULL,
  "name" varchar(255) DEFAULT NULL,
  "created_at" timestamp(6) NOT NULL,
  "updated_at" timestamp(6) NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."spree_refund_reasons" (
  "id" integer NOT NULL,
  "name" varchar(255) DEFAULT NULL,
  "active" boolean DEFAULT '1',
  "mutable" boolean DEFAULT '1',
  "created_at" timestamp(6) NOT NULL,
  "updated_at" timestamp(6) NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."spree_refunds" (
  "id" integer NOT NULL,
  "payment_id" integer DEFAULT NULL,
  "amount" decimal(10,2) NOT NULL DEFAULT '0.00',
  "transaction_id" varchar(255) DEFAULT NULL,
  "created_at" timestamp(6) NOT NULL,
  "updated_at" timestamp(6) NOT NULL,
  "refund_reason_id" integer DEFAULT NULL,
  "reimbursement_id" integer DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."spree_reimbursement_credits" (
  "id" integer NOT NULL,
  "amount" decimal(10,2) NOT NULL DEFAULT '0.00',
  "reimbursement_id" integer DEFAULT NULL,
  "creditable_id" integer DEFAULT NULL,
  "creditable_type" varchar(255) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."spree_reimbursement_types" (
  "id" integer NOT NULL,
  "name" varchar(255) DEFAULT NULL,
  "active" boolean DEFAULT '1',
  "mutable" boolean DEFAULT '1',
  "created_at" timestamp(6) NOT NULL,
  "updated_at" timestamp(6) NOT NULL,
  "type" varchar(255) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."spree_reimbursements" (
  "id" integer NOT NULL,
  "number" varchar(255) DEFAULT NULL,
  "reimbursement_status" varchar(255) DEFAULT NULL,
  "customer_return_id" integer DEFAULT NULL,
  "order_id" integer DEFAULT NULL,
  "total" decimal(10,2) DEFAULT NULL,
  "created_at" timestamp(6) NOT NULL,
  "updated_at" timestamp(6) NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."spree_return_authorization_reasons" (
  "id" integer NOT NULL,
  "name" varchar(255) DEFAULT NULL,
  "active" boolean DEFAULT '1',
  "mutable" boolean DEFAULT '1',
  "created_at" timestamp(6) NOT NULL,
  "updated_at" timestamp(6) NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."spree_return_authorizations" (
  "id" integer NOT NULL,
  "number" varchar(255) DEFAULT NULL,
  "state" varchar(255) DEFAULT NULL,
  "order_id" integer DEFAULT NULL,
  "memo" text,
  "created_at" timestamp DEFAULT NULL,
  "updated_at" timestamp DEFAULT NULL,
  "stock_location_id" integer DEFAULT NULL,
  "return_authorization_reason_id" integer DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."spree_return_items" (
  "id" integer NOT NULL,
  "return_authorization_id" integer DEFAULT NULL,
  "inventory_unit_id" integer DEFAULT NULL,
  "exchange_variant_id" integer DEFAULT NULL,
  "created_at" timestamp(6) NOT NULL,
  "updated_at" timestamp(6) NOT NULL,
  "pre_tax_amount" decimal(12,4) NOT NULL DEFAULT '0.0000',
  "included_tax_total" decimal(12,4) NOT NULL DEFAULT '0.0000',
  "additional_tax_total" decimal(12,4) NOT NULL DEFAULT '0.0000',
  "reception_status" varchar(255) DEFAULT NULL,
  "acceptance_status" varchar(255) DEFAULT NULL,
  "customer_return_id" integer DEFAULT NULL,
  "reimbursement_id" integer DEFAULT NULL,
  "acceptance_status_errors" text,
  "preferred_reimbursement_type_id" integer DEFAULT NULL,
  "override_reimbursement_type_id" integer DEFAULT NULL,
  "resellable" boolean NOT NULL DEFAULT '1',
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."spree_role_users" (
  "role_id" integer DEFAULT NULL,
  "user_id" integer DEFAULT NULL,
  "id" integer NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."spree_roles" (
  "id" integer NOT NULL,
  "name" varchar(255) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."spree_shipments" (
  "id" integer NOT NULL,
  "tracking" varchar(255) DEFAULT NULL,
  "number" varchar(255) DEFAULT NULL,
  "cost" decimal(10,2) DEFAULT '0.00',
  "shipped_at" timestamp DEFAULT NULL,
  "order_id" integer DEFAULT NULL,
  "address_id" integer DEFAULT NULL,
  "state" varchar(255) DEFAULT NULL,
  "created_at" timestamp(6) NOT NULL,
  "updated_at" timestamp(6) NOT NULL,
  "stock_location_id" integer DEFAULT NULL,
  "adjustment_total" decimal(10,2) DEFAULT '0.00',
  "additional_tax_total" decimal(10,2) DEFAULT '0.00',
  "promo_total" decimal(10,2) DEFAULT '0.00',
  "included_tax_total" decimal(10,2) NOT NULL DEFAULT '0.00',
  "pre_tax_amount" decimal(12,4) NOT NULL DEFAULT '0.0000',
  "taxable_adjustment_total" decimal(10,2) NOT NULL DEFAULT '0.00',
  "non_taxable_adjustment_total" decimal(10,2) NOT NULL DEFAULT '0.00',
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."spree_shipping_categories" (
  "id" integer NOT NULL,
  "name" varchar(255) DEFAULT NULL,
  "created_at" timestamp(6) NOT NULL,
  "updated_at" timestamp(6) NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."spree_shipping_method_categories" (
  "id" integer NOT NULL,
  "shipping_method_id" integer NOT NULL,
  "shipping_category_id" integer NOT NULL,
  "created_at" timestamp(6) NOT NULL,
  "updated_at" timestamp(6) NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."spree_shipping_method_zones" (
  "shipping_method_id" integer DEFAULT NULL,
  "zone_id" integer DEFAULT NULL,
  "id" integer NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."spree_shipping_methods" (
  "id" integer NOT NULL,
  "name" varchar(255) DEFAULT NULL,
  "display_on" varchar(255) DEFAULT NULL,
  "deleted_at" timestamp DEFAULT NULL,
  "created_at" timestamp(6) NOT NULL,
  "updated_at" timestamp(6) NOT NULL,
  "tracking_url" varchar(255) DEFAULT NULL,
  "admin_name" varchar(255) DEFAULT NULL,
  "tax_category_id" integer DEFAULT NULL,
  "code" varchar(255) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."spree_shipping_rates" (
  "id" integer NOT NULL,
  "shipment_id" integer DEFAULT NULL,
  "shipping_method_id" integer DEFAULT NULL,
  "selected" boolean DEFAULT '0',
  "cost" decimal(8,2) DEFAULT '0.00',
  "created_at" timestamp(6) NOT NULL,
  "updated_at" timestamp(6) NOT NULL,
  "tax_rate_id" integer DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."spree_state_changes" (
  "id" integer NOT NULL,
  "name" varchar(255) DEFAULT NULL,
  "previous_state" varchar(255) DEFAULT NULL,
  "stateful_id" integer DEFAULT NULL,
  "user_id" integer DEFAULT NULL,
  "stateful_type" varchar(255) DEFAULT NULL,
  "next_state" varchar(255) DEFAULT NULL,
  "created_at" timestamp(6) NOT NULL,
  "updated_at" timestamp(6) NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."spree_states" (
  "id" integer NOT NULL,
  "name" varchar(255) DEFAULT NULL,
  "abbr" varchar(255) DEFAULT NULL,
  "country_id" integer DEFAULT NULL,
  "updated_at" timestamp DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."spree_stock_items" (
  "id" integer NOT NULL,
  "stock_location_id" integer DEFAULT NULL,
  "variant_id" integer DEFAULT NULL,
  "count_on_hand" integer NOT NULL DEFAULT '0',
  "created_at" timestamp(6) NOT NULL,
  "updated_at" timestamp(6) NOT NULL,
  "backorderable" boolean DEFAULT '0',
  "deleted_at" timestamp DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."spree_stock_locations" (
  "id" integer NOT NULL,
  "name" varchar(255) DEFAULT NULL,
  "created_at" timestamp(6) NOT NULL,
  "updated_at" timestamp(6) NOT NULL,
  "default" boolean NOT NULL DEFAULT '0',
  "address1" varchar(255) DEFAULT NULL,
  "address2" varchar(255) DEFAULT NULL,
  "city" varchar(255) DEFAULT NULL,
  "state_id" integer DEFAULT NULL,
  "state_name" varchar(255) DEFAULT NULL,
  "country_id" integer DEFAULT NULL,
  "zipcode" varchar(255) DEFAULT NULL,
  "phone" varchar(255) DEFAULT NULL,
  "active" boolean DEFAULT '1',
  "backorderable_default" boolean DEFAULT '0',
  "propagate_all_variants" boolean DEFAULT '1',
  "admin_name" varchar(255) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."spree_stock_movements" (
  "id" integer NOT NULL,
  "stock_item_id" integer DEFAULT NULL,
  "quantity" integer DEFAULT '0',
  "action" varchar(255) DEFAULT NULL,
  "created_at" timestamp(6) NOT NULL,
  "updated_at" timestamp(6) NOT NULL,
  "originator_type" varchar(255) DEFAULT NULL,
  "originator_id" integer DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."spree_stock_transfers" (
  "id" integer NOT NULL,
  "type" varchar(255) DEFAULT NULL,
  "reference" varchar(255) DEFAULT NULL,
  "source_location_id" integer DEFAULT NULL,
  "destination_location_id" integer DEFAULT NULL,
  "created_at" timestamp(6) NOT NULL,
  "updated_at" timestamp(6) NOT NULL,
  "number" varchar(255) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."spree_store_credit_categories" (
  "id" integer NOT NULL,
  "name" varchar(255) DEFAULT NULL,
  "created_at" timestamp(6) NOT NULL,
  "updated_at" timestamp(6) NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."spree_store_credit_events" (
  "id" integer NOT NULL,
  "store_credit_id" integer NOT NULL,
  "action" varchar(255) NOT NULL,
  "amount" decimal(8,2) DEFAULT NULL,
  "authorization_code" varchar(255) NOT NULL,
  "user_total_amount" decimal(8,2) NOT NULL DEFAULT '0.00',
  "originator_id" integer DEFAULT NULL,
  "originator_type" varchar(255) DEFAULT NULL,
  "deleted_at" timestamp DEFAULT NULL,
  "created_at" timestamp(6) NOT NULL,
  "updated_at" timestamp(6) NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."spree_store_credit_types" (
  "id" integer NOT NULL,
  "name" varchar(255) DEFAULT NULL,
  "priority" integer DEFAULT NULL,
  "created_at" timestamp(6) NOT NULL,
  "updated_at" timestamp(6) NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."spree_store_credits" (
  "id" integer NOT NULL,
  "user_id" integer DEFAULT NULL,
  "category_id" integer DEFAULT NULL,
  "created_by_id" integer DEFAULT NULL,
  "amount" decimal(8,2) NOT NULL DEFAULT '0.00',
  "amount_used" decimal(8,2) NOT NULL DEFAULT '0.00',
  "memo" text,
  "deleted_at" timestamp DEFAULT NULL,
  "currency" varchar(255) DEFAULT NULL,
  "amount_authorized" decimal(8,2) NOT NULL DEFAULT '0.00',
  "originator_id" integer DEFAULT NULL,
  "originator_type" varchar(255) DEFAULT NULL,
  "type_id" integer DEFAULT NULL,
  "created_at" timestamp(6) NOT NULL,
  "updated_at" timestamp(6) NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."spree_stores" (
  "id" integer NOT NULL,
  "name" varchar(255) DEFAULT NULL,
  "url" varchar(255) DEFAULT NULL,
  "meta_description" text,
  "meta_keywords" text,
  "seo_title" varchar(255) DEFAULT NULL,
  "mail_from_address" varchar(255) DEFAULT NULL,
  "default_currency" varchar(255) DEFAULT NULL,
  "code" varchar(255) DEFAULT NULL,
  "default" boolean NOT NULL DEFAULT '0',
  "created_at" timestamp(6) NOT NULL,
  "updated_at" timestamp(6) NOT NULL,
  "facebook" varchar(255) DEFAULT NULL,
  "twitter" varchar(255) DEFAULT NULL,
  "instagram" varchar(255) DEFAULT NULL,
  "default_locale" varchar(255) DEFAULT NULL,
  "customer_support_email" varchar(255) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."spree_tax_categories" (
  "id" integer NOT NULL,
  "name" varchar(255) DEFAULT NULL,
  "description" varchar(255) DEFAULT NULL,
  "is_default" boolean DEFAULT '0',
  "deleted_at" timestamp DEFAULT NULL,
  "created_at" timestamp(6) NOT NULL,
  "updated_at" timestamp(6) NOT NULL,
  "tax_code" varchar(255) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."spree_tax_rates" (
  "id" integer NOT NULL,
  "amount" decimal(8,5) DEFAULT NULL,
  "zone_id" integer DEFAULT NULL,
  "tax_category_id" integer DEFAULT NULL,
  "included_in_price" boolean DEFAULT '0',
  "created_at" timestamp(6) NOT NULL,
  "updated_at" timestamp(6) NOT NULL,
  "name" varchar(255) DEFAULT NULL,
  "show_rate_in_label" boolean DEFAULT '1',
  "deleted_at" timestamp DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."spree_taxonomies" (
  "id" integer NOT NULL,
  "name" varchar(255) NOT NULL,
  "created_at" timestamp(6) NOT NULL,
  "updated_at" timestamp(6) NOT NULL,
  "position" integer DEFAULT '0',
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."spree_taxons" (
  "id" integer NOT NULL,
  "parent_id" integer DEFAULT NULL,
  "position" integer DEFAULT '0',
  "name" varchar(255) NOT NULL,
  "permalink" varchar(255) DEFAULT NULL,
  "taxonomy_id" integer DEFAULT NULL,
  "lft" integer DEFAULT NULL,
  "rgt" integer DEFAULT NULL,
  "description" text,
  "created_at" timestamp(6) NOT NULL,
  "updated_at" timestamp(6) NOT NULL,
  "meta_title" varchar(255) DEFAULT NULL,
  "meta_description" varchar(255) DEFAULT NULL,
  "meta_keywords" varchar(255) DEFAULT NULL,
  "depth" integer DEFAULT NULL,
  "hide_from_nav" boolean DEFAULT '0',
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."spree_trackers" (
  "id" integer NOT NULL,
  "analytics_id" varchar(255) DEFAULT NULL,
  "active" boolean DEFAULT '1',
  "created_at" timestamp(6) NOT NULL,
  "updated_at" timestamp(6) NOT NULL,
  "engine" integer NOT NULL DEFAULT '0',
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."spree_users" (
  "id" integer NOT NULL,
  "encrypted_password" varchar(128) DEFAULT NULL,
  "password_salt" varchar(128) DEFAULT NULL,
  "email" varchar(255) DEFAULT NULL,
  "remember_token" varchar(255) DEFAULT NULL,
  "persistence_token" varchar(255) DEFAULT NULL,
  "reset_password_token" varchar(255) DEFAULT NULL,
  "perishable_token" varchar(255) DEFAULT NULL,
  "sign_in_count" integer NOT NULL DEFAULT '0',
  "failed_attempts" integer NOT NULL DEFAULT '0',
  "last_request_at" timestamp DEFAULT NULL,
  "current_sign_in_at" timestamp DEFAULT NULL,
  "last_sign_in_at" timestamp DEFAULT NULL,
  "current_sign_in_ip" varchar(255) DEFAULT NULL,
  "last_sign_in_ip" varchar(255) DEFAULT NULL,
  "login" varchar(255) DEFAULT NULL,
  "ship_address_id" integer DEFAULT NULL,
  "bill_address_id" integer DEFAULT NULL,
  "authentication_token" varchar(255) DEFAULT NULL,
  "unlock_token" varchar(255) DEFAULT NULL,
  "locked_at" timestamp DEFAULT NULL,
  "remember_created_at" timestamp DEFAULT NULL,
  "reset_password_sent_at" timestamp DEFAULT NULL,
  "created_at" timestamp(6) NOT NULL,
  "updated_at" timestamp(6) NOT NULL,
  "spree_api_key" varchar(48) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."spree_variants" (
  "id" integer NOT NULL,
  "sku" varchar(255) NOT NULL DEFAULT '',
  "weight" decimal(8,2) DEFAULT '0.00',
  "height" decimal(8,2) DEFAULT NULL,
  "width" decimal(8,2) DEFAULT NULL,
  "depth" decimal(8,2) DEFAULT NULL,
  "deleted_at" timestamp DEFAULT NULL,
  "discontinue_on" timestamp DEFAULT NULL,
  "is_master" boolean DEFAULT '0',
  "product_id" integer DEFAULT NULL,
  "cost_price" decimal(10,2) DEFAULT NULL,
  "cost_currency" varchar(255) DEFAULT NULL,
  "position" integer DEFAULT NULL,
  "track_inventory" boolean DEFAULT '1',
  "tax_category_id" integer DEFAULT NULL,
  "updated_at" timestamp NOT NULL,
  "created_at" timestamp NOT NULL,
  "count_on_hand" int NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."spree_zone_members" (
  "id" integer NOT NULL,
  "zoneable_type" varchar(255) DEFAULT NULL,
  "zoneable_id" integer DEFAULT NULL,
  "zone_id" integer DEFAULT NULL,
  "created_at" timestamp(6) NOT NULL,
  "updated_at" timestamp(6) NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS spree."spree_zones" (
  "id" integer NOT NULL,
  "name" varchar(255) DEFAULT NULL,
  "description" varchar(255) DEFAULT NULL,
  "default_tax" boolean DEFAULT '0',
  "zone_members_count" integer DEFAULT '0',
  "created_at" timestamp(6) NOT NULL,
  "updated_at" timestamp(6) NOT NULL,
  "kind" varchar(255) DEFAULT NULL,
  PRIMARY KEY (id)
);

