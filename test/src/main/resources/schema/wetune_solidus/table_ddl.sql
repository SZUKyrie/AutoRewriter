


CREATE TABLE IF NOT EXISTS solidus."action_mailbox_inbound_emails" (
  "id" bigint NOT NULL,
  "status" integer NOT NULL DEFAULT '0',
  "message_id" varchar(255) NOT NULL,
  "message_checksum" varchar(255) NOT NULL,
  "created_at" timestamp(6) NOT NULL,
  "updated_at" timestamp(6) NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."action_text_rich_texts" (
  "id" bigint NOT NULL,
  "name" varchar(255) NOT NULL,
  "body" text,
  "record_type" varchar(255) NOT NULL,
  "record_id" bigint NOT NULL,
  "created_at" timestamp(6) NOT NULL,
  "updated_at" timestamp(6) NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."active_storage_attachments" (
  "id" bigint NOT NULL,
  "name" varchar(255) NOT NULL,
  "record_type" varchar(255) NOT NULL,
  "record_id" bigint NOT NULL,
  "blob_id" bigint NOT NULL,
  "created_at" timestamp NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."active_storage_blobs" (
  "id" bigint NOT NULL,
  "filename" varchar(255) NOT NULL,
  "content_type" varchar(255) DEFAULT NULL,
  "metadata" text,
  "byte_size" bigint NOT NULL,
  "checksum" varchar(255) NOT NULL,
  "created_at" timestamp NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."ar_internal_metadata" (
  "value" varchar(255) DEFAULT NULL,
  "created_at" timestamp(6) NOT NULL,
  "updated_at" timestamp(6) NOT NULL,
  PRIMARY KEY (key)
);


CREATE TABLE IF NOT EXISTS solidus."friendly_id_slugs" (
  "id" integer NOT NULL,
  "slug" varchar(255) NOT NULL,
  "sluggable_id" integer NOT NULL,
  "sluggable_type" varchar(50) DEFAULT NULL,
  "scope" varchar(255) DEFAULT NULL,
  "created_at" timestamp(6) DEFAULT NULL,
  "updated_at" timestamp(6) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."schema_migrations" (
  "version" varchar(255) NOT NULL,
  PRIMARY KEY (version)
);


CREATE TABLE IF NOT EXISTS solidus."spree_addresses" (
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
  "created_at" timestamp(6) DEFAULT NULL,
  "updated_at" timestamp(6) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_adjustment_reasons" (
  "id" integer NOT NULL,
  "name" varchar(255) DEFAULT NULL,
  "code" varchar(255) DEFAULT NULL,
  "active" boolean DEFAULT '1',
  "created_at" timestamp(6) DEFAULT NULL,
  "updated_at" timestamp(6) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_adjustments" (
  "id" integer NOT NULL,
  "source_type" varchar(255) DEFAULT NULL,
  "source_id" integer DEFAULT NULL,
  "adjustable_type" varchar(255) DEFAULT NULL,
  "adjustable_id" integer NOT NULL,
  "amount" decimal(10,2) DEFAULT NULL,
  "label" varchar(255) DEFAULT NULL,
  "eligible" boolean DEFAULT '1',
  "created_at" timestamp(6) DEFAULT NULL,
  "updated_at" timestamp(6) DEFAULT NULL,
  "order_id" integer NOT NULL,
  "included" boolean DEFAULT '0',
  "promotion_code_id" integer DEFAULT NULL,
  "adjustment_reason_id" integer DEFAULT NULL,
  "finalized" boolean NOT NULL DEFAULT '0',
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_assets" (
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
  "created_at" timestamp(6) DEFAULT NULL,
  "updated_at" timestamp(6) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_calculators" (
  "id" integer NOT NULL,
  "type" varchar(255) DEFAULT NULL,
  "calculable_type" varchar(255) DEFAULT NULL,
  "calculable_id" integer DEFAULT NULL,
  "created_at" timestamp(6) DEFAULT NULL,
  "updated_at" timestamp(6) DEFAULT NULL,
  "preferences" text,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_cartons" (
  "id" integer NOT NULL,
  "number" varchar(255) DEFAULT NULL,
  "external_number" varchar(255) DEFAULT NULL,
  "stock_location_id" integer DEFAULT NULL,
  "address_id" integer DEFAULT NULL,
  "shipping_method_id" integer DEFAULT NULL,
  "tracking" varchar(255) DEFAULT NULL,
  "shipped_at" timestamp DEFAULT NULL,
  "created_at" timestamp(6) DEFAULT NULL,
  "updated_at" timestamp(6) DEFAULT NULL,
  "imported_from_shipment_id" integer DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_countries" (
  "id" integer NOT NULL,
  "iso_name" varchar(255) DEFAULT NULL,
  "iso" varchar(255) DEFAULT NULL,
  "iso3" varchar(255) DEFAULT NULL,
  "name" varchar(255) DEFAULT NULL,
  "numcode" integer DEFAULT NULL,
  "states_required" boolean DEFAULT '0',
  "updated_at" timestamp(6) DEFAULT NULL,
  "created_at" timestamp(6) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_credit_cards" (
  "id" integer NOT NULL,
  "month" varchar(255) DEFAULT NULL,
  "year" varchar(255) DEFAULT NULL,
  "cc_type" varchar(255) DEFAULT NULL,
  "last_digits" varchar(255) DEFAULT NULL,
  "gateway_customer_profile_id" varchar(255) DEFAULT NULL,
  "gateway_payment_profile_id" varchar(255) DEFAULT NULL,
  "created_at" timestamp(6) DEFAULT NULL,
  "updated_at" timestamp(6) DEFAULT NULL,
  "name" varchar(255) DEFAULT NULL,
  "user_id" integer DEFAULT NULL,
  "payment_method_id" integer DEFAULT NULL,
  "default" boolean NOT NULL DEFAULT '0',
  "address_id" integer DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_customer_returns" (
  "id" integer NOT NULL,
  "number" varchar(255) DEFAULT NULL,
  "stock_location_id" integer DEFAULT NULL,
  "created_at" timestamp(6) DEFAULT NULL,
  "updated_at" timestamp(6) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_inventory_units" (
  "id" integer NOT NULL,
  "state" varchar(255) DEFAULT NULL,
  "variant_id" integer DEFAULT NULL,
  "shipment_id" integer DEFAULT NULL,
  "created_at" timestamp(6) DEFAULT NULL,
  "updated_at" timestamp(6) DEFAULT NULL,
  "pending" boolean DEFAULT '1',
  "line_item_id" integer DEFAULT NULL,
  "carton_id" integer DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_line_item_actions" (
  "id" integer NOT NULL,
  "line_item_id" integer NOT NULL,
  "action_id" integer NOT NULL,
  "quantity" integer DEFAULT '0',
  "created_at" timestamp(6) DEFAULT NULL,
  "updated_at" timestamp(6) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_line_items" (
  "id" integer NOT NULL,
  "variant_id" integer DEFAULT NULL,
  "order_id" integer DEFAULT NULL,
  "quantity" integer NOT NULL,
  "price" decimal(10,2) NOT NULL,
  "created_at" timestamp(6) DEFAULT NULL,
  "updated_at" timestamp(6) DEFAULT NULL,
  "cost_price" decimal(10,2) DEFAULT NULL,
  "tax_category_id" integer DEFAULT NULL,
  "adjustment_total" decimal(10,2) DEFAULT '0.00',
  "additional_tax_total" decimal(10,2) DEFAULT '0.00',
  "promo_total" decimal(10,2) DEFAULT '0.00',
  "included_tax_total" decimal(10,2) NOT NULL DEFAULT '0.00',
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_log_entries" (
  "id" integer NOT NULL,
  "source_type" varchar(255) DEFAULT NULL,
  "source_id" integer DEFAULT NULL,
  "details" text,
  "created_at" timestamp(6) DEFAULT NULL,
  "updated_at" timestamp(6) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_option_type_prototypes" (
  "id" integer NOT NULL,
  "prototype_id" integer DEFAULT NULL,
  "option_type_id" integer DEFAULT NULL,
  "created_at" timestamp(6) DEFAULT NULL,
  "updated_at" timestamp(6) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_option_types" (
  "id" integer NOT NULL,
  "name" varchar(100) DEFAULT NULL,
  "presentation" varchar(100) DEFAULT NULL,
  "position" integer NOT NULL DEFAULT '0',
  "created_at" timestamp(6) DEFAULT NULL,
  "updated_at" timestamp(6) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_option_values" (
  "id" integer NOT NULL,
  "position" integer DEFAULT NULL,
  "name" varchar(255) DEFAULT NULL,
  "presentation" varchar(255) DEFAULT NULL,
  "option_type_id" integer DEFAULT NULL,
  "created_at" timestamp(6) DEFAULT NULL,
  "updated_at" timestamp(6) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_option_values_variants" (
  "id" integer NOT NULL,
  "variant_id" integer DEFAULT NULL,
  "option_value_id" integer DEFAULT NULL,
  "created_at" timestamp(6) DEFAULT NULL,
  "updated_at" timestamp(6) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_order_mutexes" (
  "id" integer NOT NULL,
  "order_id" integer NOT NULL,
  "created_at" timestamp(6) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_orders" (
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
  "created_at" timestamp(6) DEFAULT NULL,
  "updated_at" timestamp(6) DEFAULT NULL,
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
  "guest_token" varchar(255) DEFAULT NULL,
  "canceled_at" timestamp DEFAULT NULL,
  "canceler_id" integer DEFAULT NULL,
  "store_id" integer DEFAULT NULL,
  "approver_name" varchar(255) DEFAULT NULL,
  "frontend_viewable" boolean NOT NULL DEFAULT '1',
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_orders_promotions" (
  "id" integer NOT NULL,
  "order_id" integer DEFAULT NULL,
  "promotion_id" integer DEFAULT NULL,
  "promotion_code_id" integer DEFAULT NULL,
  "created_at" timestamp(6) DEFAULT NULL,
  "updated_at" timestamp(6) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_payment_capture_events" (
  "id" integer NOT NULL,
  "amount" decimal(10,2) DEFAULT '0.00',
  "payment_id" integer DEFAULT NULL,
  "created_at" timestamp(6) DEFAULT NULL,
  "updated_at" timestamp(6) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_payment_methods" (
  "id" integer NOT NULL,
  "type" varchar(255) DEFAULT NULL,
  "name" varchar(255) DEFAULT NULL,
  "description" text,
  "active" boolean DEFAULT '1',
  "deleted_at" timestamp DEFAULT NULL,
  "created_at" timestamp(6) DEFAULT NULL,
  "updated_at" timestamp(6) DEFAULT NULL,
  "auto_capture" boolean DEFAULT NULL,
  "preferences" text,
  "preference_source" varchar(255) DEFAULT NULL,
  "position" integer DEFAULT '0',
  "available_to_users" boolean DEFAULT '1',
  "available_to_admin" boolean DEFAULT '1',
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_payments" (
  "id" integer NOT NULL,
  "amount" decimal(10,2) NOT NULL DEFAULT '0.00',
  "order_id" integer DEFAULT NULL,
  "source_type" varchar(255) DEFAULT NULL,
  "source_id" integer DEFAULT NULL,
  "payment_method_id" integer DEFAULT NULL,
  "state" varchar(255) DEFAULT NULL,
  "response_code" varchar(255) DEFAULT NULL,
  "avs_response" varchar(255) DEFAULT NULL,
  "created_at" timestamp(6) DEFAULT NULL,
  "updated_at" timestamp(6) DEFAULT NULL,
  "number" varchar(255) DEFAULT NULL,
  "cvv_response_code" varchar(255) DEFAULT NULL,
  "cvv_response_message" varchar(255) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_preferences" (
  "id" integer NOT NULL,
  "value" text,
  "created_at" timestamp(6) DEFAULT NULL,
  "updated_at" timestamp(6) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_prices" (
  "id" integer NOT NULL,
  "variant_id" integer NOT NULL,
  "amount" decimal(10,2) DEFAULT NULL,
  "currency" varchar(255) DEFAULT NULL,
  "deleted_at" timestamp DEFAULT NULL,
  "created_at" timestamp(6) DEFAULT NULL,
  "updated_at" timestamp(6) DEFAULT NULL,
  "country_iso" varchar(2) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_product_option_types" (
  "id" integer NOT NULL,
  "position" integer DEFAULT NULL,
  "product_id" integer DEFAULT NULL,
  "option_type_id" integer DEFAULT NULL,
  "created_at" timestamp(6) DEFAULT NULL,
  "updated_at" timestamp(6) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_product_promotion_rules" (
  "id" integer NOT NULL,
  "product_id" integer DEFAULT NULL,
  "promotion_rule_id" integer DEFAULT NULL,
  "created_at" timestamp(6) DEFAULT NULL,
  "updated_at" timestamp(6) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_product_properties" (
  "id" integer NOT NULL,
  "value" varchar(255) DEFAULT NULL,
  "product_id" integer DEFAULT NULL,
  "property_id" integer DEFAULT NULL,
  "created_at" timestamp(6) DEFAULT NULL,
  "updated_at" timestamp(6) DEFAULT NULL,
  "position" integer DEFAULT '0',
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_products" (
  "id" integer NOT NULL,
  "name" varchar(255) NOT NULL DEFAULT '',
  "description" text,
  "available_on" timestamp DEFAULT NULL,
  "deleted_at" timestamp DEFAULT NULL,
  "slug" varchar(255) DEFAULT NULL,
  "meta_description" text,
  "meta_keywords" varchar(255) DEFAULT NULL,
  "tax_category_id" integer DEFAULT NULL,
  "shipping_category_id" integer DEFAULT NULL,
  "created_at" timestamp(6) DEFAULT NULL,
  "updated_at" timestamp(6) DEFAULT NULL,
  "promotionable" boolean DEFAULT '1',
  "meta_title" varchar(255) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_products_taxons" (
  "id" integer NOT NULL,
  "product_id" integer DEFAULT NULL,
  "taxon_id" integer DEFAULT NULL,
  "position" integer DEFAULT NULL,
  "created_at" timestamp(6) DEFAULT NULL,
  "updated_at" timestamp(6) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_promotion_action_line_items" (
  "id" integer NOT NULL,
  "promotion_action_id" integer DEFAULT NULL,
  "variant_id" integer DEFAULT NULL,
  "quantity" integer DEFAULT '1',
  "created_at" timestamp(6) DEFAULT NULL,
  "updated_at" timestamp(6) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_promotion_actions" (
  "id" integer NOT NULL,
  "promotion_id" integer DEFAULT NULL,
  "position" integer DEFAULT NULL,
  "type" varchar(255) DEFAULT NULL,
  "deleted_at" timestamp DEFAULT NULL,
  "preferences" text,
  "created_at" timestamp(6) DEFAULT NULL,
  "updated_at" timestamp(6) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_promotion_categories" (
  "id" integer NOT NULL,
  "name" varchar(255) DEFAULT NULL,
  "created_at" timestamp(6) DEFAULT NULL,
  "updated_at" timestamp(6) DEFAULT NULL,
  "code" varchar(255) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_promotion_code_batches" (
  "id" integer NOT NULL,
  "promotion_id" integer NOT NULL,
  "base_code" varchar(255) NOT NULL,
  "number_of_codes" integer NOT NULL,
  "email" varchar(255) DEFAULT NULL,
  "error" varchar(255) DEFAULT NULL,
  "state" varchar(255) DEFAULT 'pending',
  "created_at" timestamp(6) NOT NULL,
  "updated_at" timestamp(6) NOT NULL,
  "join_characters" varchar(255) NOT NULL DEFAULT '_',
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_promotion_codes" (
  "id" integer NOT NULL,
  "promotion_id" integer NOT NULL,
  "value" varchar(255) NOT NULL,
  "created_at" timestamp(6) DEFAULT NULL,
  "updated_at" timestamp(6) DEFAULT NULL,
  "promotion_code_batch_id" integer DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_promotion_rule_taxons" (
  "id" integer NOT NULL,
  "taxon_id" integer DEFAULT NULL,
  "promotion_rule_id" integer DEFAULT NULL,
  "created_at" timestamp(6) DEFAULT NULL,
  "updated_at" timestamp(6) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_promotion_rules" (
  "id" integer NOT NULL,
  "promotion_id" integer DEFAULT NULL,
  "product_group_id" integer DEFAULT NULL,
  "type" varchar(255) DEFAULT NULL,
  "created_at" timestamp(6) DEFAULT NULL,
  "updated_at" timestamp(6) DEFAULT NULL,
  "code" varchar(255) DEFAULT NULL,
  "preferences" text,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_promotion_rules_stores" (
  "id" bigint NOT NULL,
  "store_id" bigint NOT NULL,
  "promotion_rule_id" bigint NOT NULL,
  "created_at" timestamp NOT NULL,
  "updated_at" timestamp NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_promotion_rules_users" (
  "id" integer NOT NULL,
  "user_id" integer DEFAULT NULL,
  "promotion_rule_id" integer DEFAULT NULL,
  "created_at" timestamp(6) DEFAULT NULL,
  "updated_at" timestamp(6) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_promotions" (
  "id" integer NOT NULL,
  "description" varchar(255) DEFAULT NULL,
  "expires_at" timestamp DEFAULT NULL,
  "starts_at" timestamp DEFAULT NULL,
  "name" varchar(255) DEFAULT NULL,
  "type" varchar(255) DEFAULT NULL,
  "usage_limit" integer DEFAULT NULL,
  "match_policy" varchar(255) DEFAULT 'all',
  "advertise" boolean DEFAULT '0',
  "path" varchar(255) DEFAULT NULL,
  "created_at" timestamp(6) DEFAULT NULL,
  "updated_at" timestamp(6) DEFAULT NULL,
  "promotion_category_id" integer DEFAULT NULL,
  "per_code_usage_limit" integer DEFAULT NULL,
  "apply_automatically" boolean DEFAULT '0',
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_properties" (
  "id" integer NOT NULL,
  "name" varchar(255) DEFAULT NULL,
  "presentation" varchar(255) NOT NULL,
  "created_at" timestamp(6) DEFAULT NULL,
  "updated_at" timestamp(6) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_property_prototypes" (
  "id" integer NOT NULL,
  "prototype_id" integer DEFAULT NULL,
  "property_id" integer DEFAULT NULL,
  "created_at" timestamp(6) DEFAULT NULL,
  "updated_at" timestamp(6) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_prototype_taxons" (
  "id" integer NOT NULL,
  "taxon_id" integer DEFAULT NULL,
  "prototype_id" integer DEFAULT NULL,
  "created_at" timestamp(6) DEFAULT NULL,
  "updated_at" timestamp(6) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_prototypes" (
  "id" integer NOT NULL,
  "name" varchar(255) DEFAULT NULL,
  "created_at" timestamp(6) DEFAULT NULL,
  "updated_at" timestamp(6) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_refund_reasons" (
  "id" integer NOT NULL,
  "name" varchar(255) DEFAULT NULL,
  "active" boolean DEFAULT '1',
  "mutable" boolean DEFAULT '1',
  "created_at" timestamp(6) DEFAULT NULL,
  "updated_at" timestamp(6) DEFAULT NULL,
  "code" varchar(255) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_refunds" (
  "id" integer NOT NULL,
  "payment_id" integer DEFAULT NULL,
  "amount" decimal(10,2) NOT NULL DEFAULT '0.00',
  "transaction_id" varchar(255) DEFAULT NULL,
  "created_at" timestamp(6) DEFAULT NULL,
  "updated_at" timestamp(6) DEFAULT NULL,
  "refund_reason_id" integer DEFAULT NULL,
  "reimbursement_id" integer DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_reimbursement_credits" (
  "id" integer NOT NULL,
  "amount" decimal(10,2) NOT NULL DEFAULT '0.00',
  "reimbursement_id" integer DEFAULT NULL,
  "creditable_id" integer DEFAULT NULL,
  "creditable_type" varchar(255) DEFAULT NULL,
  "created_at" timestamp(6) DEFAULT NULL,
  "updated_at" timestamp(6) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_reimbursement_types" (
  "id" integer NOT NULL,
  "name" varchar(255) DEFAULT NULL,
  "active" boolean DEFAULT '1',
  "mutable" boolean DEFAULT '1',
  "created_at" timestamp(6) DEFAULT NULL,
  "updated_at" timestamp(6) DEFAULT NULL,
  "type" varchar(255) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_reimbursements" (
  "id" integer NOT NULL,
  "number" varchar(255) DEFAULT NULL,
  "reimbursement_status" varchar(255) DEFAULT NULL,
  "customer_return_id" integer DEFAULT NULL,
  "order_id" integer DEFAULT NULL,
  "total" decimal(10,2) DEFAULT NULL,
  "created_at" timestamp(6) DEFAULT NULL,
  "updated_at" timestamp(6) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_return_authorizations" (
  "id" integer NOT NULL,
  "number" varchar(255) DEFAULT NULL,
  "state" varchar(255) DEFAULT NULL,
  "order_id" integer DEFAULT NULL,
  "memo" text,
  "created_at" timestamp(6) DEFAULT NULL,
  "updated_at" timestamp(6) DEFAULT NULL,
  "stock_location_id" integer DEFAULT NULL,
  "return_reason_id" integer DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_return_items" (
  "id" integer NOT NULL,
  "return_authorization_id" integer DEFAULT NULL,
  "inventory_unit_id" integer DEFAULT NULL,
  "exchange_variant_id" integer DEFAULT NULL,
  "created_at" timestamp(6) DEFAULT NULL,
  "updated_at" timestamp(6) DEFAULT NULL,
  "amount" decimal(12,4) NOT NULL DEFAULT '0.0000',
  "included_tax_total" decimal(12,4) NOT NULL DEFAULT '0.0000',
  "additional_tax_total" decimal(12,4) NOT NULL DEFAULT '0.0000',
  "reception_status" varchar(255) DEFAULT NULL,
  "acceptance_status" varchar(255) DEFAULT NULL,
  "customer_return_id" integer DEFAULT NULL,
  "reimbursement_id" integer DEFAULT NULL,
  "exchange_inventory_unit_id" integer DEFAULT NULL,
  "acceptance_status_errors" text,
  "preferred_reimbursement_type_id" integer DEFAULT NULL,
  "override_reimbursement_type_id" integer DEFAULT NULL,
  "resellable" boolean NOT NULL DEFAULT '1',
  "return_reason_id" integer DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_return_reasons" (
  "id" integer NOT NULL,
  "name" varchar(255) DEFAULT NULL,
  "active" boolean DEFAULT '1',
  "mutable" boolean DEFAULT '1',
  "created_at" timestamp(6) DEFAULT NULL,
  "updated_at" timestamp(6) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_roles" (
  "id" integer NOT NULL,
  "name" varchar(255) DEFAULT NULL,
  "created_at" timestamp(6) DEFAULT NULL,
  "updated_at" timestamp(6) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_roles_users" (
  "id" integer NOT NULL,
  "role_id" integer DEFAULT NULL,
  "user_id" integer DEFAULT NULL,
  "created_at" timestamp(6) DEFAULT NULL,
  "updated_at" timestamp(6) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_shipments" (
  "id" integer NOT NULL,
  "tracking" varchar(255) DEFAULT NULL,
  "number" varchar(255) DEFAULT NULL,
  "cost" decimal(10,2) DEFAULT '0.00',
  "shipped_at" timestamp DEFAULT NULL,
  "order_id" integer DEFAULT NULL,
  "deprecated_address_id" integer DEFAULT NULL,
  "state" varchar(255) DEFAULT NULL,
  "created_at" timestamp(6) DEFAULT NULL,
  "updated_at" timestamp(6) DEFAULT NULL,
  "stock_location_id" integer DEFAULT NULL,
  "adjustment_total" decimal(10,2) DEFAULT '0.00',
  "additional_tax_total" decimal(10,2) DEFAULT '0.00',
  "promo_total" decimal(10,2) DEFAULT '0.00',
  "included_tax_total" decimal(10,2) NOT NULL DEFAULT '0.00',
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_shipping_categories" (
  "id" integer NOT NULL,
  "name" varchar(255) DEFAULT NULL,
  "created_at" timestamp(6) DEFAULT NULL,
  "updated_at" timestamp(6) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_shipping_method_categories" (
  "id" integer NOT NULL,
  "shipping_method_id" integer NOT NULL,
  "shipping_category_id" integer NOT NULL,
  "created_at" timestamp(6) DEFAULT NULL,
  "updated_at" timestamp(6) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_shipping_method_stock_locations" (
  "id" integer NOT NULL,
  "shipping_method_id" integer DEFAULT NULL,
  "stock_location_id" integer DEFAULT NULL,
  "created_at" timestamp(6) DEFAULT NULL,
  "updated_at" timestamp(6) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_shipping_method_zones" (
  "id" integer NOT NULL,
  "shipping_method_id" integer DEFAULT NULL,
  "zone_id" integer DEFAULT NULL,
  "created_at" timestamp(6) DEFAULT NULL,
  "updated_at" timestamp(6) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_shipping_methods" (
  "id" integer NOT NULL,
  "name" varchar(255) DEFAULT NULL,
  "deleted_at" timestamp DEFAULT NULL,
  "created_at" timestamp(6) DEFAULT NULL,
  "updated_at" timestamp(6) DEFAULT NULL,
  "tracking_url" varchar(255) DEFAULT NULL,
  "admin_name" varchar(255) DEFAULT NULL,
  "tax_category_id" integer DEFAULT NULL,
  "code" varchar(255) DEFAULT NULL,
  "available_to_all" boolean DEFAULT '1',
  "carrier" varchar(255) DEFAULT NULL,
  "service_level" varchar(255) DEFAULT NULL,
  "available_to_users" boolean DEFAULT '1',
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_shipping_rate_taxes" (
  "id" integer NOT NULL,
  "amount" decimal(8,2) NOT NULL DEFAULT '0.00',
  "tax_rate_id" integer DEFAULT NULL,
  "shipping_rate_id" integer DEFAULT NULL,
  "created_at" timestamp(6) NOT NULL,
  "updated_at" timestamp(6) NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_shipping_rates" (
  "id" integer NOT NULL,
  "shipment_id" integer DEFAULT NULL,
  "shipping_method_id" integer DEFAULT NULL,
  "selected" boolean DEFAULT '0',
  "cost" decimal(8,2) DEFAULT '0.00',
  "created_at" timestamp(6) DEFAULT NULL,
  "updated_at" timestamp(6) DEFAULT NULL,
  "tax_rate_id" integer DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_state_changes" (
  "id" integer NOT NULL,
  "name" varchar(255) DEFAULT NULL,
  "previous_state" varchar(255) DEFAULT NULL,
  "stateful_id" integer DEFAULT NULL,
  "user_id" integer DEFAULT NULL,
  "stateful_type" varchar(255) DEFAULT NULL,
  "next_state" varchar(255) DEFAULT NULL,
  "created_at" timestamp(6) DEFAULT NULL,
  "updated_at" timestamp(6) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_states" (
  "id" integer NOT NULL,
  "name" varchar(255) DEFAULT NULL,
  "abbr" varchar(255) DEFAULT NULL,
  "country_id" integer DEFAULT NULL,
  "updated_at" timestamp(6) DEFAULT NULL,
  "created_at" timestamp(6) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_stock_items" (
  "id" integer NOT NULL,
  "stock_location_id" integer DEFAULT NULL,
  "variant_id" integer DEFAULT NULL,
  "count_on_hand" integer NOT NULL DEFAULT '0',
  "created_at" timestamp(6) DEFAULT NULL,
  "updated_at" timestamp(6) DEFAULT NULL,
  "backorderable" boolean DEFAULT '0',
  "deleted_at" timestamp DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_stock_locations" (
  "id" integer NOT NULL,
  "name" varchar(255) DEFAULT NULL,
  "created_at" timestamp(6) DEFAULT NULL,
  "updated_at" timestamp(6) DEFAULT NULL,
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
  "position" integer DEFAULT '0',
  "restock_inventory" boolean NOT NULL DEFAULT '1',
  "fulfillable" boolean NOT NULL DEFAULT '1',
  "code" varchar(255) DEFAULT NULL,
  "check_stock_on_transfer" boolean DEFAULT '1',
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_stock_movements" (
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


CREATE TABLE IF NOT EXISTS solidus."spree_store_credit_categories" (
  "id" integer NOT NULL,
  "name" varchar(255) DEFAULT NULL,
  "created_at" timestamp(6) DEFAULT NULL,
  "updated_at" timestamp(6) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_store_credit_events" (
  "id" integer NOT NULL,
  "store_credit_id" integer NOT NULL,
  "action" varchar(255) NOT NULL,
  "amount" decimal(8,2) DEFAULT NULL,
  "user_total_amount" decimal(8,2) NOT NULL DEFAULT '0.00',
  "authorization_code" varchar(255) NOT NULL,
  "deleted_at" timestamp DEFAULT NULL,
  "originator_type" varchar(255) DEFAULT NULL,
  "originator_id" integer DEFAULT NULL,
  "created_at" timestamp(6) DEFAULT NULL,
  "updated_at" timestamp(6) DEFAULT NULL,
  "amount_remaining" decimal(8,2) DEFAULT NULL,
  "store_credit_reason_id" integer DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_store_credit_reasons" (
  "id" bigint NOT NULL,
  "name" varchar(255) DEFAULT NULL,
  "active" boolean DEFAULT '1',
  "created_at" timestamp NOT NULL,
  "updated_at" timestamp NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_store_credit_types" (
  "id" integer NOT NULL,
  "name" varchar(255) DEFAULT NULL,
  "priority" integer DEFAULT NULL,
  "created_at" timestamp(6) DEFAULT NULL,
  "updated_at" timestamp(6) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_store_credits" (
  "id" integer NOT NULL,
  "user_id" integer DEFAULT NULL,
  "category_id" integer DEFAULT NULL,
  "created_by_id" integer DEFAULT NULL,
  "amount" decimal(8,2) NOT NULL DEFAULT '0.00',
  "amount_used" decimal(8,2) NOT NULL DEFAULT '0.00',
  "amount_authorized" decimal(8,2) NOT NULL DEFAULT '0.00',
  "currency" varchar(255) DEFAULT NULL,
  "memo" text,
  "deleted_at" timestamp DEFAULT NULL,
  "created_at" timestamp(6) DEFAULT NULL,
  "updated_at" timestamp(6) DEFAULT NULL,
  "type_id" integer DEFAULT NULL,
  "invalidated_at" timestamp DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_store_payment_methods" (
  "id" integer NOT NULL,
  "store_id" integer NOT NULL,
  "payment_method_id" integer NOT NULL,
  "created_at" timestamp(6) NOT NULL,
  "updated_at" timestamp(6) NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_store_shipping_methods" (
  "id" bigint NOT NULL,
  "store_id" bigint NOT NULL,
  "shipping_method_id" bigint NOT NULL,
  "created_at" timestamp(6) NOT NULL,
  "updated_at" timestamp(6) NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_stores" (
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
  "created_at" timestamp(6) DEFAULT NULL,
  "updated_at" timestamp(6) DEFAULT NULL,
  "cart_tax_country_iso" varchar(255) DEFAULT NULL,
  "available_locales" varchar(255) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_tax_categories" (
  "id" integer NOT NULL,
  "name" varchar(255) DEFAULT NULL,
  "description" varchar(255) DEFAULT NULL,
  "is_default" boolean DEFAULT '0',
  "deleted_at" timestamp DEFAULT NULL,
  "created_at" timestamp(6) DEFAULT NULL,
  "updated_at" timestamp(6) DEFAULT NULL,
  "tax_code" varchar(255) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_tax_rate_tax_categories" (
  "id" integer NOT NULL,
  "tax_category_id" integer NOT NULL,
  "tax_rate_id" integer NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_tax_rates" (
  "id" integer NOT NULL,
  "amount" decimal(8,5) DEFAULT NULL,
  "zone_id" integer DEFAULT NULL,
  "included_in_price" boolean DEFAULT '0',
  "created_at" timestamp(6) DEFAULT NULL,
  "updated_at" timestamp(6) DEFAULT NULL,
  "name" varchar(255) DEFAULT NULL,
  "show_rate_in_label" boolean DEFAULT '1',
  "deleted_at" timestamp DEFAULT NULL,
  "starts_at" timestamp DEFAULT NULL,
  "expires_at" timestamp DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_taxonomies" (
  "id" integer NOT NULL,
  "name" varchar(255) NOT NULL,
  "created_at" timestamp(6) DEFAULT NULL,
  "updated_at" timestamp(6) DEFAULT NULL,
  "position" integer DEFAULT '0',
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_taxons" (
  "id" integer NOT NULL,
  "parent_id" integer DEFAULT NULL,
  "position" integer DEFAULT '0',
  "name" varchar(255) NOT NULL,
  "permalink" varchar(255) DEFAULT NULL,
  "taxonomy_id" integer DEFAULT NULL,
  "lft" integer DEFAULT NULL,
  "rgt" integer DEFAULT NULL,
  "icon_file_name" varchar(255) DEFAULT NULL,
  "icon_content_type" varchar(255) DEFAULT NULL,
  "icon_file_size" integer DEFAULT NULL,
  "icon_updated_at" timestamp DEFAULT NULL,
  "description" text,
  "created_at" timestamp(6) DEFAULT NULL,
  "updated_at" timestamp(6) DEFAULT NULL,
  "meta_title" varchar(255) DEFAULT NULL,
  "meta_description" varchar(255) DEFAULT NULL,
  "meta_keywords" varchar(255) DEFAULT NULL,
  "depth" integer DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_unit_cancels" (
  "id" integer NOT NULL,
  "inventory_unit_id" integer NOT NULL,
  "reason" varchar(255) DEFAULT NULL,
  "created_by" varchar(255) DEFAULT NULL,
  "created_at" timestamp(6) DEFAULT NULL,
  "updated_at" timestamp(6) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_user_addresses" (
  "id" integer NOT NULL,
  "user_id" integer NOT NULL,
  "address_id" integer NOT NULL,
  "default" boolean DEFAULT '0',
  "archived" boolean DEFAULT '0',
  "created_at" timestamp(6) NOT NULL,
  "updated_at" timestamp(6) NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_user_stock_locations" (
  "id" integer NOT NULL,
  "user_id" integer DEFAULT NULL,
  "stock_location_id" integer DEFAULT NULL,
  "created_at" timestamp(6) DEFAULT NULL,
  "updated_at" timestamp(6) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_users" (
  "id" integer NOT NULL,
  "crypted_password" varchar(128) DEFAULT NULL,
  "salt" varchar(128) DEFAULT NULL,
  "email" varchar(255) DEFAULT NULL,
  "remember_token" varchar(255) DEFAULT NULL,
  "remember_token_expires_at" varchar(255) DEFAULT NULL,
  "persistence_token" varchar(255) DEFAULT NULL,
  "single_access_token" varchar(255) DEFAULT NULL,
  "perishable_token" varchar(255) DEFAULT NULL,
  "login_count" integer NOT NULL DEFAULT '0',
  "failed_login_count" integer NOT NULL DEFAULT '0',
  "last_request_at" timestamp DEFAULT NULL,
  "current_login_at" timestamp DEFAULT NULL,
  "last_login_at" timestamp DEFAULT NULL,
  "current_login_ip" varchar(255) DEFAULT NULL,
  "last_login_ip" varchar(255) DEFAULT NULL,
  "login" varchar(255) DEFAULT NULL,
  "ship_address_id" integer DEFAULT NULL,
  "bill_address_id" integer DEFAULT NULL,
  "created_at" timestamp(6) NOT NULL,
  "updated_at" timestamp(6) NOT NULL,
  "openid_identifier" varchar(255) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_variant_property_rule_conditions" (
  "id" integer NOT NULL,
  "option_value_id" integer DEFAULT NULL,
  "variant_property_rule_id" integer DEFAULT NULL,
  "created_at" timestamp(6) NOT NULL,
  "updated_at" timestamp(6) NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_variant_property_rule_values" (
  "id" integer NOT NULL,
  "value" text,
  "position" integer DEFAULT '0',
  "property_id" integer DEFAULT NULL,
  "variant_property_rule_id" integer DEFAULT NULL,
  "created_at" timestamp(6) DEFAULT NULL,
  "updated_at" timestamp(6) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_variant_property_rules" (
  "id" integer NOT NULL,
  "product_id" integer DEFAULT NULL,
  "created_at" timestamp(6) NOT NULL,
  "updated_at" timestamp(6) NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_variants" (
  "id" integer NOT NULL,
  "sku" varchar(255) NOT NULL DEFAULT '',
  "weight" decimal(8,2) DEFAULT '0.00',
  "height" decimal(8,2) DEFAULT NULL,
  "width" decimal(8,2) DEFAULT NULL,
  "depth" decimal(8,2) DEFAULT NULL,
  "deleted_at" timestamp DEFAULT NULL,
  "is_master" boolean DEFAULT '0',
  "product_id" integer DEFAULT NULL,
  "cost_price" decimal(10,2) DEFAULT NULL,
  "position" integer DEFAULT NULL,
  "cost_currency" varchar(255) DEFAULT NULL,
  "track_inventory" boolean DEFAULT '1',
  "tax_category_id" integer DEFAULT NULL,
  "updated_at" timestamp(6) DEFAULT NULL,
  "created_at" timestamp(6) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_wallet_payment_sources" (
  "id" integer NOT NULL,
  "user_id" integer NOT NULL,
  "payment_source_type" varchar(255) NOT NULL,
  "payment_source_id" integer NOT NULL,
  "default" boolean NOT NULL DEFAULT '0',
  "created_at" timestamp(6) NOT NULL,
  "updated_at" timestamp(6) NOT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_zone_members" (
  "id" integer NOT NULL,
  "zoneable_type" varchar(255) DEFAULT NULL,
  "zoneable_id" integer DEFAULT NULL,
  "zone_id" integer DEFAULT NULL,
  "created_at" timestamp(6) DEFAULT NULL,
  "updated_at" timestamp(6) DEFAULT NULL,
  PRIMARY KEY (id)
);


CREATE TABLE IF NOT EXISTS solidus."spree_zones" (
  "id" integer NOT NULL,
  "name" varchar(255) DEFAULT NULL,
  "description" varchar(255) DEFAULT NULL,
  "zone_members_count" integer DEFAULT '0',
  "created_at" timestamp(6) DEFAULT NULL,
  "updated_at" timestamp(6) DEFAULT NULL,
  PRIMARY KEY (id)
);

