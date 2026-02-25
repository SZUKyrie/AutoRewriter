select
	d.d_date as sales_date,
	s.s_store_name as store_name,
	sum(ss_quantity) as total_units,
	sum(coalesce(ss_net_paid, 0) - coalesce(ss_net_loss, 0)) as net_revenue
from tpcds.store_sales ss
inner join tpcds.store s on ss.ss_store_sk = s.s_store_sk
inner join tpcds.date_dim d on ss.ss_sold_date_sk = d.d_date_sk
where d.d_year = 2001
group by d.d_date, s.s_store_name
order by sales_date, store_name
limit 100;
