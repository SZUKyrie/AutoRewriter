select
	cs.cs_bill_customer_sk as customer_sk,
	sum(cs_quantity) as total_units,
	sum(coalesce(cs_net_paid, 0) - coalesce(cs_net_loss, 0)) as net_revenue
from tpcds.catalog_sales cs
where cs.cs_sold_date_sk is not null
group by cs.cs_bill_customer_sk
having sum(coalesce(cs_net_paid, 0) - coalesce(cs_net_loss, 0)) > 1000
order by net_revenue desc
limit 100;
