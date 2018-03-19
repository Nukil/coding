package com.netposa.poseidon.main;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.netposa.poseidon.bean.ResponseResult;
import com.netposa.poseidon.bean.SearchImgResult;
import com.netposa.poseidon.connection.ReverseAuxiliary;

public class ReverseAuxiliaryRetirval 
{
	public static void main(String[] args)
	{
		List<SearchImgResult> srcResults = new ArrayList<SearchImgResult>();
	    List<SearchImgResult> negativeResults = new ArrayList<SearchImgResult>();
	    double[] num1= new double[10000];
	    double[] num2= new double[10000];
	    int count=0;
		while(count<10000)
		{
			num1[count]= (double) (Math.random()*50+50);
			num2[count]= (double) (Math.random()*50+50);
			count++;
		}
		sortdown(num1);
		sortdown(num2);
		/////////////////////////////////////////////////////////////////////////////////////////////
		for(int i=0; i<count; i++)
        {
            String q=String.valueOf((int)56);
        	String s1 = String.valueOf((int) i);
        	String s2 = String.valueOf((int) (i+10));
        	srcResults.add(new SearchImgResult(s1, q, (long) 20170915, num1[i] ));
        	negativeResults.add(new SearchImgResult(s2, q, (long) 20170915,num2[i] ));
        }
		//////////////////////////////////////////////////////////////////////////////////////////////
		//srcResults.add(new SearchImgResult("01", "12",(long)20170915, 99.24));
		//srcResults.add(new SearchImgResult("02", "12",(long)20170915, 95.32));
		//srcResults.add(new SearchImgResult("03", "12",(long)20170915, 83.26));
		/*srcResults.add(new SearchImgResult("04", "12",(long)20170915, 60.34));
		srcResults.add(new SearchImgResult("05", "12",(long)20170915, 45.26));
		srcResults.add(new SearchImgResult("06", "12",(long)20170915, 24.87));*/
		//negativeResults.addAll(srcResults);
		///////////////////////////////////////////////////////////////////////////////////////////////
		//String a=String.valueOf((int)12);
		//String b=String.valueOf((int)13);
		//srcResults.add(new SearchImgResult(a, "12",(long)20170915, 50.00));
		//negativeResults.add(new SearchImgResult(b, "12",(long)20170915, 50.00));
		//////////////////////////////////////////////////////////////////////////////////////////////
		//存储原图集
        try ( PrintWriter w = new PrintWriter("srcResults.txt"))
	    {
	        for(SearchImgResult line : srcResults)
	        {
	            w.println(line);
	        }
	    } catch(IOException e)
	    {
	        // handle exception
	    }
		try ( PrintWriter w = new PrintWriter("negativeResults.txt"))
	    {
	        for(SearchImgResult line : negativeResults)
	        {
	            w.println(line);
	        }
	    } catch(IOException e)
	    {
	        // handle exception
	    }
		//调用多图检索
		long startTime = System.currentTimeMillis();    //获取开始时间
		ResponseResult result;
		result=ReverseAuxiliary.reverseRetirval(srcResults, negativeResults);
		long endTime = System.currentTimeMillis();    //获取结束时间
		//System.out.println("程序运行时间：" + (endTime - startTime) + "ms");    //输出程序运行时间
		myprint(result.list);
		System.out.println(result.rCode+"     "+result.message); 
		System.out.println((result.list).size()); 		
		System.out.println("程序运行时间：" + (endTime - startTime) + "ms");    //输出程序运行时间
		try ( PrintWriter w = new PrintWriter("sortAgain.txt"))
	    {
	        for(SearchImgResult line : result.list)
	        {
	            w.println(line);
	        }
	    } catch(IOException e)
	    {
	        // handle exception
	    }
	}
	//输出list子函数
	public static void myprint(List<SearchImgResult> list) 
	{  
		Iterator<SearchImgResult> it = list.iterator(); // 得到迭代器，用于遍历list中的所有元素  
		while (it.hasNext()) 
		{// 如果迭代器中有元素，则返回true  
			   System.out.println("\t" + it.next());// 显 示该元素  
		}
	}
	//降序排列数组，用于排列测试数据图集score值		
	private static void sortdown(double[] a) 
	{
	     for (int i = 0; i < a.length-1; i++) 
	     {
	         for (int j = i+1; j < a.length; j++) 
	         {
	             if(a[j]>a[i])
	             {
	                 double t=a[j];
	                 a[j]=a[i];
	                 a[i]=t;
	             }
	         }
	     }
     }
}
