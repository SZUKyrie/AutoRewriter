select
	w.w_warehouse_name as warehouse_name,
	d.d_month_seq,
	sum(ws_quantity) as total_units,
	sum(coalesce(ws_net_paid, 0) - coalesce(ws_net_loss, 0)) as net_revenue
from tpcds.web_sales ws
inner join tpcds.warehouse w on ws.ws_warehouse_sk = w.w_warehouse_sk
inner join tpcds.date_dim d on ws.ws_sold_date_sk = d.d_date_sk
where d.d_year = 2001
group by w.w_warehouse_name, d.d_month_seq
order by d_month_seq, warehouse_name;

