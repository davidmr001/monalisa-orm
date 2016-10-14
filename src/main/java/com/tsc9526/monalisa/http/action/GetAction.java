/*******************************************************************************************
 *	Copyright (c) 2016, zzg.zhou(11039850@qq.com)
 * 
 *  Monalisa is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU Lesser General Public License as published by
 *	the Free Software Foundation, either version 3 of the License, or
 *	(at your option) any later version.

 *	This program is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU Lesser General Public License for more details.

 *	You should have received a copy of the GNU Lesser General Public License
 *	along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************************/
package com.tsc9526.monalisa.http.action;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.tsc9526.monalisa.http.Response;
import com.tsc9526.monalisa.orm.Query;
import com.tsc9526.monalisa.orm.annotation.Column;
import com.tsc9526.monalisa.orm.datasource.DbProp;
import com.tsc9526.monalisa.orm.datatable.DataMap;
import com.tsc9526.monalisa.orm.datatable.DataTable;
import com.tsc9526.monalisa.orm.datatable.Page;
import com.tsc9526.monalisa.orm.dialect.Dialect;
import com.tsc9526.monalisa.orm.model.Record;
import com.tsc9526.monalisa.orm.tools.helper.ClassHelper.FGS;

/**
 * 
 * @author zzg.zhou(11039850@qq.com)
 */
public class GetAction extends Action{
  
	public GetAction(ActionArgs args) {
		super(args);
	}

	public Response getResponse() {
		String qid=args.getQueryId();
		if(qid!=null){
			return doQueryById(qid);
		}else if(args.getTables()!=null){
			return getMultiTableRows();
		}else{
			if(args.getTable()==null){
				return getTables();
			}else{
				if(args.getSinglePK()!=null){
					return getTableRowBySinglePK();
				}else if(args.getMultiKeys()!=null){
					return getTableRowByMultiKeys();
				}else{
					return getTableRows();
				}
			}
		}
	}
	 
	public Response doQueryById(String queryId){
		int p=queryId.lastIndexOf(".");
		if(p>0){
			String clazz =queryId.substring(0,p);
			String name  =queryId.substring(p+1);
			try{
				Class<?> c=Class.forName(clazz);
				Method x=null;
				for(Method m:c.getDeclaredMethods()){
					if(m.getName().equals(name)){
						x=m;
						 
						Annotation[][] ass=m.getParameterAnnotations();
						if(ass!=null && ass.length>0){
							for(Annotation[] xs:ass){
								if(xs!=null && xs.length>0){
									
								}
							}
						}
					}
				}
				
				return (Response)x.invoke(c.newInstance(), args);
			}catch(Exception e){
				throw new RuntimeException(e);
			}
		}else{
			return new Response(Response.REQUEST_BAD_PARAMETER, "Error QueryId: "+queryId+", should be a class method. e.g: your_class_package_name.findById");
		}
	}

	/**
	 * Get data using query multi-tables
	 * @return rows
	 */
	public Response getMultiTableRows(){
		Query query=createQuery();
		
		query.add("SELECT ");
		List<String> ics=args.getIncludeColumns();
		if(ics.size()>0){
			for(int i=0;i<ics.size();i++){
				String c=ics.get(i);
				if(i>0){
					query.add(", "+c);
				}else{
					query.add( c);
				}
			}
		}else{
			query.add("*");
		}
		
		query.add(" FROM ");
		String[] tables=args.getTables();
		for(int i=0;i<tables.length;i++){
			if(i>0){
				query.add(", "+tables[i]);
			}else{
				query.add( tables[i] );
			}
		}
		
		query.add(" WHERE ");
		String[] joins =args.getJoins();
		if(joins!=null){
			for(int i=0;i<joins.length;i++){
				if(i>0){
					query.add(" AND "+joins[i]);
				}else{
					query.add( joins[i] );
				}
			}
		}else{
			//TODO: find table joins
			query.add("1=1 ");
		}
		
		List<String[]> filters=args.getFilters();
		for(int i=0;i<filters.size();i++){
			String[] fs=filters.get(i);
			query.add(" AND "+fs[0]+" "+fs[1]+" ?",fs[2]);
		}
		
		List<String[]> orderBy=args.getOrderBy();
		if(orderBy.size()>0){
			query.add(" ORDER BY ");
			for(int i=0;i<orderBy.size();i++){
				String[] fs=orderBy.get(i);
				if(i>0){
					query.add(", ");
				}
				query.add(fs[0]+" "+fs[1]);
			}
		}
		
		int limit =args.getLimit();
		int offset=args.getOffset();
		
		int maxLimit=getMaxLimit();
		if(limit>maxLimit){
			return new Response(Response.REQUEST_BAD_PARAMETER, "Error: limit = "+limit+", it is too bigger than the max value: "+maxLimit+". (OR increase the max value: \"DB.cfg.dbs.max.rows\")");
		}
		
		if(args.isPaging()){
			Page<DataMap> page=query.getPage(limit,offset);

			return doGetPage(page);
		}else{
			DataTable<DataMap> table=query.getList(limit, offset);
			
			return doGetTable(table);
		}
	}
	

	/**
	 * Get the database tables
	 * 
	 * @return list tables of the database 
	 */
	public Response getTables(){
		DataTable<DataMap> table=new DataTable<DataMap>();
		for(String t:db.getTables()){
			DataMap m=new DataMap();
			
			m.put("table_name",t);
			
			table.add(m);
		}
		return doGetTable(table );
	}
	  
	/**
	 * Get the table rows
	 * 
	 * @return list rows of the table
	 */
	public Response getTableRows(){
		Query query=createQuery();
		
		query.add("SELECT ");
		
		List<String> ics=args.getIncludeColumns();
		List<String> exs=args.getExcludeColumns();
		if(ics.size()==0 && exs.size()>0){
			Set<String> es=new HashSet<String>();
			for(String n:exs){
				es.add(Dialect.getRealname(n).toLowerCase());
			}
			
			Record record=createRecord();
			
			for(FGS fgs:record.fields()){
				Column c=fgs.getAnnotation(Column.class);
				
				String cname=c.name();
				if(!es.contains(cname.toLowerCase())){
					ics.add(cname);
				}		
			} 
		}
		
		if(ics.size()>0){
			for(int i=0;i<ics.size();i++){
				String c=ics.get(i);
				if(i>0){
					query.add(", "+c);
				}else{
					query.add( c);
				}
			}
		}else{
			query.add("*");
		}
		query.add(" FROM "+args.getTable());
		
		List<String[]> filters=args.getFilters();
		if(filters.size()>0){
			query.add(" WHERE ");
			for(int i=0;i<filters.size();i++){
				String[] fs=filters.get(i);
				if(i>0){
					query.add(" AND ");
				}
				query.add(fs[0]+" "+fs[1]+" ?",fs[2]);
			}
		}
		
		List<String[]> orderBy=args.getOrderBy();
		if(orderBy.size()>0){
			query.add(" ORDER BY ");
			for(int i=0;i<orderBy.size();i++){
				String[] fs=orderBy.get(i);
				if(i>0){
					query.add(", ");
				}
				query.add(fs[0]+" "+fs[1]);
			}
		}
		
		int limit =args.getLimit();
		int offset=args.getOffset();
		
		int maxLimit=DbProp.PROP_TABLE_DBS_MAX_ROWS.getIntValue(db,args.getTable(),10000);
		if(limit>maxLimit){
			return new Response(Response.REQUEST_BAD_PARAMETER, "Error parameter: limit = "+limit+", it is too bigger than the max value: "+maxLimit+". (OR increase the max value: \"DB.cfg.dbs.max.rows\")");
		}
		
		if(args.isPaging()){
			Page<DataMap> page=query.getPage(limit,offset);
		
			return doGetPage(page);
		}else{
			DataTable<DataMap> table=query.getList(limit, offset);
			
			return doGetTable(table);
		}
		
	}
	 
	/**
	 * Get row of the table by single primary key
	 * @return 200: one row,  404: NOT found
	 */
	public Response getTableRowBySinglePK(){
		Record record=createRecord();
		List<FGS> pks=record.pkFields();
		if(pks.size()==1){
			for(String c:args.getExcludeColumns()){
				record.exclude(c);
			}
	
			for(String c:args.getIncludeColumns()){
				record.include(c);
			}
			
			record.set(pks.get(0).getFieldName(),args.getSinglePK()).load();
			if(record.entity()){
				return doGetRecord(record);
			}else{
				return new Response(Response.REQUEST_NOT_FOUND,"Primary key not found: /"+args.getPathDatabases()+"/"+args.getPathTables()+"/"+args.getSinglePK());
			}
		}else{
			StringBuilder sb=new StringBuilder();
			sb.append(args.getActionName()+" error, table: "+args.getTable()+" primary key has more than one columns");
			sb.append(", change the request's path to: /"+args.getPathDatabases()+"/"+args.getPathTables());
			for(FGS fgs:pks){
				sb.append("/").append(fgs.getFieldName()).append("=xxx");
			}
			return new Response(Response.REQUEST_BAD_PARAMETER,sb.toString());
		}
	}
	
	public Response getTableRowByMultiKeys(){
		Record record=createRecord();
		
		for(String c:args.getExcludeColumns()){
			record.exclude(c);
		}

		for(String c:args.getIncludeColumns()){
			record.include(c);
		}
		
		Record.Criteria c=record.WHERE();
		for(String[] nv:args.getMultiKeys()){
			if(record.field(nv[0])!=null){
				c.field(nv[0]).eq(nv[1]);
			}else{
				return new Response(Response.REQUEST_BAD_PARAMETER,args.getActionName()+" error, column not found: "+nv[0]+" in the table: "+args.getTable());
			}
		}
		
		Record x=c.SELECT().selectOne();
		if(x!=null){
			return doGetRecord(x);
		}else{
			return new Response(Response.REQUEST_NOT_FOUND,args.getActionName()+" error, multi keys not found: "+args.getPathRequest());
		}
	}
	
	protected int getMaxLimit(){
		int maxLimit=DbProp.PROP_TABLE_DBS_MAX_ROWS.getIntValue(db, 10000);
		return maxLimit;
	}
	
	protected Response doGetRecord(Record r){
		return new Response(r);
	}
	
	protected Response doGetTable(DataTable<DataMap> table){
		return new Response(table).setDetail(""+table.size());
	}
	
	protected Response doGetPage(Page<DataMap> page){
		args.getResp().setHeader("X-Total-Count", ""+page.getTotalRow());
		args.getResp().setHeader("X-Total-Page" , ""+page.getTotalPage());
		
		return new Response(page.getList()).setDetail(""+page.getTotalRow());
	}
}