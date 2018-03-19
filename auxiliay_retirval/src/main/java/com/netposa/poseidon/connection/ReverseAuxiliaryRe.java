package com.netposa.poseidon.connection;

import java.util.List;

import com.netposa.poseidon.bean.ResponseResult;
import com.netposa.poseidon.bean.SearchImgResult;

public interface ReverseAuxiliaryRe
{
	public ResponseResult reverseAuxiliaryRe(List<SearchImgResult> srcResults, List<SearchImgResult> negativeResults);
}