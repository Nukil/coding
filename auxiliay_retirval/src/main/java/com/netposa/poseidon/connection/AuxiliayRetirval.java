package com.netposa.poseidon.connection;

import java.util.ArrayList;
import java.util.List;

import com.netposa.poseidon.bean.ResponseResult;
import com.netposa.poseidon.bean.SearchImgResult;

public class AuxiliayRetirval 
{
	static List<SearchImgResult> srcResults = new ArrayList<SearchImgResult>();
    static List<SearchImgResult> positiveResults = new ArrayList<SearchImgResult>();
	public static ResponseResult auxiliayRetirval(List<SearchImgResult> srcResults,List<SearchImgResult> positiveResults)
	{
		AuxiliayRetirvalOrgImpl auxiliay=new AuxiliayRetirvalOrgImpl();
		return auxiliay.auxiliayRetirvalorg(srcResults, positiveResults);
	}
}
