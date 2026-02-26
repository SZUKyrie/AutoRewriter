select  distinct(i_product_name)
 from tpcds.item i1
 where i_manufact_id between 787 and 787+40
   and (select count(*) as item_cnt
        from tpcds.item
        where (i_manufact = i1.i_manufact and
        ((i_category = 'Women' and
        (i_color = 'mint' or i_color = 'khaki') and
        (i_units = 'Ounce' or i_units = 'Cup') and
        (i_size = 'small' or i_size = 'large')
        ) or
        (i_category = 'Women' and
        (i_color = 'coral' or i_color = 'dodger') and
        (i_units = 'Unknown' or i_units = 'Lb') and
        (i_size = 'petite' or i_size = 'N/A')
        ) or
        (i_category = 'Men' and
        (i_color = 'yellow' or i_color = 'moccasin') and
        (i_units = 'Carton' or i_units = 'Box') and
        (i_size = 'medium' or i_size = 'economy')
        ) or
        (i_category = 'Men' and
        (i_color = 'cornsilk' or i_color = 'hot') and
        (i_units = 'Tbl' or i_units = 'Dozen') and
        (i_size = 'small' or i_size = 'large')
        ))) or
       (i_manufact = i1.i_manufact and
        ((i_category = 'Women' and
        (i_color = 'magenta' or i_color = 'blush') and
        (i_units = 'Tsp' or i_units = 'Pallet') and
        (i_size = 'small' or i_size = 'large')
        ) or
        (i_category = 'Women' and
        (i_color = 'dark' or i_color = 'chiffon') and
        (i_units = 'N/A' or i_units = 'Gram') and
        (i_size = 'petite' or i_size = 'N/A')
        ) or
        (i_category = 'Men' and
        (i_color = 'maroon' or i_color = 'gainsboro') and
        (i_units = 'Dram' or i_units = 'Pound') and
        (i_size = 'medium' or i_size = 'economy')
        ) or
        (i_category = 'Men' and
        (i_color = 'midnight' or i_color = 'steel') and
        (i_units = 'Each' or i_units = 'Ton') and
        (i_size = 'small' or i_size = 'large')
        )))) > 0
 order by i_product_name
 LIMIT 100;