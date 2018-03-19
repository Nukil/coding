package com.netposa.poseidon.bean;

import java.text.DecimalFormat;


public class SearchImgResult implements Comparable<Object>
{ // 定义类，默认继承自Object类  
    public String recordId;// 记录编号  
    public String number;// 人脸为相机编号车辆为号牌号码
    public Long gatherTime; // 采集时间
    public Double score;// 分数 
    
  
    public SearchImgResult() 
    {  
        this("X","X", (long) 0, 0.0); 
    	//this("X","X",0.0);
    }

	public SearchImgResult(String recordId, String number, Long gathertime, Double score) 
	{
		this.recordId=recordId;
		this.number=number;
		this.gatherTime=gathertime;
		this.score=score;
		// TODO Auto-generated constructor stub
	}
	//构造get（），set（）方法
		public String getRecordId()
		{
			return recordId;
		}
		public void setLogNum(String recordId) 
		{
			  this.recordId = recordId;
		}
		public String getNumber()
		{
			return number;
		}
		public void setNumber(String number) 
		{
			  this.number = number;
		}
		public Long getGatherTime()
		{
			return gatherTime;
		}
		public void setGatherTime(Long gatherTime) 
		{
			  this.gatherTime = gatherTime;
		}
		public Double getScore()
		{
			return score;
		}
		public void setScore(Double score) 
		{
			  this.score = score;
		}
	// 重写继承自父类Object的方法，满足SearchImgResult类信息描述的要求  
	public String toString() 
    {  
        String showStr = "recordId" + recordId + "\t"+ "number" + number + "\t"; // 定义显示类信息的字符串  
        DecimalFormat formatscore = new DecimalFormat("0.00");// 格式化分数到小数点后两位
        //DecimalFormat formattime = new DecimalFormat("0");// 格式化时间到小数点前
        //showStr += formattime.format(gatherTime) + "\t";// 格式化时间
        showStr += formatscore.format(score); // 格式化分数
        return showStr; // 返回类信息字符串  
    }		
	//@Override
	public int compareTo(Object obj) 
	{
		 //TODO Auto-generated method stub
		SearchImgResult b = (SearchImgResult) obj;  
        return (int)(this.score -b.score); // 按score排序，用于默认排序  
	}		
} 
