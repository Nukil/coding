package com.netposa.poseidon.bean;

import java.util.List;

public class ResponseResult 
{
	public class rCode
	{ // 定义类，默认继承自Object类  
	    public final static int stand=0;
	    public final static int error=1;
	    public final static int OK=2;
	}

		public int rCode;
		public List<SearchImgResult> list;
		public String message;
		public List<SearchImgResult> getList()
		{
			return list;
		}
		public void setList(List<SearchImgResult> list)
		{
			this.list=list;
		}
		public int getRCode()
		{
			return rCode;
		}
		public void setRCode(int rCode)
		{
			this.rCode=rCode;
		}
		public String getMessage()
		{
			return message;
		}
		public void setMessage(String message)
		{
			this.message=message;
		}
}

