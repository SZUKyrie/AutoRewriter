select
	cds.customer_id,
	cds.sales_date,
	cds.channel,
	cds.gross_sales - cds.discount_amt as gross_after_discount,
	cds.net_sales
from tpcds.customer_daily_sales cds
where cds.sales_date between DATE '2025-09-01' and DATE '2025-09-07'
order by cds.sales_date, cds.customer_id;
