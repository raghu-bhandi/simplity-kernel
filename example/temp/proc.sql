create or replace PROCEDURE RC_FETCH_PRE_NPI_LIST (
      filter_column_name     IN     VARCHAR2,
      filter_list            IN     RC_NEW_FILTER_OBJ_LIST,
      txt   OUT varchar2) as 
	  BEGIN
		txt := filter_list(2).COL_VALUE(1).FILTER_DATA ;
	  END