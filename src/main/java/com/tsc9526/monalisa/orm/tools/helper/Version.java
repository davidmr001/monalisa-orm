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
package com.tsc9526.monalisa.orm.tools.helper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 
 * @author zzg.zhou(11039850@qq.com)
 */
public class Version {
	private static String version;
	
	public static String getVersion(){
		if(version==null){
			InputStream in=null;
			try{
				in=Version.class.getResourceAsStream("/META-INF/maven/com.tsc9526/monalisa-orm/pom.properties");
				if(in!=null){
					Properties p=new Properties();
					p.load(in);
					version=p.getProperty("version");
				}else{
					File pom=new File("pom.xml");
					
					String xml=FileHelper.readToString(pom,"utf-8");
					
					int p1=xml.indexOf("<version>");
					int p2=xml.indexOf("</version>");
					if(p2>p1 && p1>0){
						version=xml.substring(p1+"<version>".length(),p2).trim();
					}
				}
			}catch(IOException e){
				e.printStackTrace();
			}finally{
				CloseQuietly.close(in);
			}
		}
		
		return version==null?"unknow":version;
	}
}
