package com.netposa.poseidon.connection;

import java.util.ArrayList;
import java.util.List;

import com.netposa.poseidon.bean.ResponseResult;
import com.netposa.poseidon.bean.SearchImgResult;

public class ReverseAuxiliary 
{
	static List<SearchImgResult> srcResults = new ArrayList<SearchImgResult>();
    static List<SearchImgResult> negativeResults = new ArrayList<SearchImgResult>();
	public static ResponseResult reverseRetirval(List<SearchImgResult> srcResults,List<SearchImgResult> negativeResults)
	{
		ReverseAuxiliaryReImpl reverse=new ReverseAuxiliaryReImpl();
		return reverse.reverseAuxiliaryRe(srcResults, negativeResults);
	}
}