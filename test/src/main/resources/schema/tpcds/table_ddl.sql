
create table if not exists tpcds.customer_daily_sales (
	customer_id bigint,
	sales_date date,
	channel varchar,
	gross_sales decimal(12,2),
	discount_amt decimal(12,2),
	net_sales decimal(12,2)
);

create table if not exists tpcds.store_sales (
	ss_sold_date_sk int,
	ss_store_sk int,
	ss_customer_sk int,
	ss_quantity int,
	ss_net_paid decimal(10,2),
	ss_net_loss decimal(10,2)
);

create table if not exists tpcds.store (
	s_store_sk int,
	s_store_name varchar,
	s_closed_date_sk int
);

create table if not exists tpcds.date_dim (
	d_date_sk int,
	d_date date,
	d_year int,
	d_month_seq int
);

create table if not exists tpcds.catalog_sales (
	cs_sold_date_sk int,
	cs_bill_customer_sk int,
	cs_quantity int,
	cs_net_paid decimal(10,2),
	cs_net_loss decimal(10,2)
);

create table if not exists tpcds.web_sales (
	ws_sold_date_sk int,
	ws_warehouse_sk int,
	ws_quantity int,
	ws_net_paid decimal(10,2),
	ws_net_loss decimal(10,2)
);

create table if not exists tpcds.warehouse (
	w_warehouse_sk int,
	w_warehouse_name varchar
);

create table if not exists tpcds.store_sales_ext (
	sale_id varchar,
	sold_date_sk int,
	store_sk int,
	item_sk int,
	customer_sk int,
	quantity int,
	sales_price decimal(9,2),
	coupon_amt decimal(9,2),
	net_paid decimal(10,2)
);

create table if not exists tpcds.catalog_sales_ext (
	sale_id varchar,
	sold_date_sk int,
	call_center_sk int,
	item_sk int,
	bill_customer_sk int,
	quantity int,
	net_paid decimal(10,2),
	net_loss decimal(10,2)
);

create table if not exists tpcds.channel_sales_metrics (
	sales_date date,
	channel varchar,
	gross_after_discount decimal(12,2),
	net_revenue decimal(12,2),
	as_of_date timestamp
);
