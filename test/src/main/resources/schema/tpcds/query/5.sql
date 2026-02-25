with daily as (
	select
		cds.sales_date,
		cds.channel,
		sum(cds.gross_sales - cds.discount_amt) as gross_after_discount,
		sum(cds.net_sales) as net_sales
	from tpcds.customer_daily_sales cds
	group by cds.sales_date, cds.channel
),
web as (
	select
		d.d_date as sales_date,
		'web' as channel,
		sum(coalesce(ws_net_paid, 0) - coalesce(ws_net_loss, 0)) as net_revenue
	from tpcds.web_sales ws
	inner join tpcds.date_dim d on ws.ws_sold_date_sk = d.d_date_sk
	where d.d_year = 2001
	group by d.d_date
)
select
	coalesce(daily.sales_date, web.sales_date) as sales_date,
	coalesce(daily.channel, web.channel) as channel,
	coalesce(daily.gross_after_discount, 0) as gross_after_discount,
	coalesce(daily.net_sales, 0) as net_sales,
	coalesce(web.net_revenue, 0) as net_revenue
from daily
full outer join web
	on daily.sales_date = web.sales_date
 and daily.channel = web.channel
order by sales_date, channel
limit 200;
