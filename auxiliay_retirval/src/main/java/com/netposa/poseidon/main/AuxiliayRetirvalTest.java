package com.netposa.poseidon.main;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import com.netposa.poseidon.bean.ResponseResult;
import com.netposa.poseidon.bean.SearchImgResult;
import com.netposa.poseidon.connection.AuxiliayRetirval;

import java.util.Iterator;


public class AuxiliayRetirvalTest
{
	public static void main(String[] args) throws Exception
	{
		//long startTime = System.currentTimeMillis();    //获取开始时间
		//初始化图集A、图集B，用于测试
		List<SearchImgResult> srcResults = new ArrayList<SearchImgResult>();
	    List<SearchImgResult> positiveResults = new ArrayList<SearchImgResult>();
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
        //int n=1000;
		//String a="123";
		//srcResults.add(new SearchImgResult(a, "12",(long)20170915, 99.87));
		//positiveResults.add(new SearchImgResult(a, "12",(long)20170915, 99.87));
        for(int i=0;i<count;i++)
        {
            String q=String.valueOf((int)12);
        	String s1 = String.valueOf((int) i);
        	String s2 = String.valueOf((int) (i+21));
        	srcResults.add(new SearchImgResult(s1, q, (long) 20170915, num1[i] ));
        	positiveResults.add(new SearchImgResult(s2, q, (long) 20170915,num2[i] ));
        }
        //positiveResults.addAll(srcResults);
        //positiveResults=null;
        //srcResults.clear();
        //调用多图检索服务
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
		try ( PrintWriter w = new PrintWriter("positiveResults.txt"))
	    {
	        for(SearchImgResult line : positiveResults)
	        {
	            w.println(line);
	        }
	    } catch(IOException e)
	    {
	        // handle exception
	    }
		
        long startTime = System.currentTimeMillis();    //获取开始时间
		ResponseResult result;
		//AuxiliayRetirvalImpl auxiliay=new AuxiliayRetirvalImpl();
		//result=auxiliay.auxiliayRetirval(srcResults, positiveResults);
		result=AuxiliayRetirval.auxiliayRetirval(srcResults, positiveResults);
		//输出结果
		myprint(result.list);
		System.out.println(result.rCode+"     "+result.message); 
		long endTime = System.currentTimeMillis();    //获取结束时间
		System.out.println("程序运行时间：" + (endTime - startTime) + "ms");    //输出程序运行时间
		System.out.println((result.list).size()); 		
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