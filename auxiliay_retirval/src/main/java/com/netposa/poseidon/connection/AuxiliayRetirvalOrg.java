package com.netposa.poseidon.connection;

import java.util.List;

import com.netposa.poseidon.bean.ResponseResult;
import com.netposa.poseidon.bean.SearchImgResult;

public interface AuxiliayRetirvalOrg 
{
	public ResponseResult auxiliayRetirvalorg(List<SearchImgResult> srcResults, List<SearchImgResult> positiveResults);
}
