package com.netposa.poseidon.connection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.netposa.poseidon.bean.ResponseResult;
import com.netposa.poseidon.bean.SearchImgResult;

public class ReverseAuxiliaryReImpl implements ReverseAuxiliaryRe 
{
	@Override
	public ResponseResult reverseAuxiliaryRe(List<SearchImgResult> srcResults, List<SearchImgResult> negativeResults) 
	{
		//判断集合A、B是否存在
		if (srcResults == null || negativeResults == null) 
		{
			ResponseResult result = new ResponseResult();
			result.setRCode(0);//result.rCode = rCode.stand;
			result.setMessage("Input List is Null");//result.message = "Input List Error";
			result.setList(null);//result.list = null;
			return result;
		}
		else 
		{
			int sizeSrcRe = srcResults.size();
			int sizeNegRe = negativeResults.size();
			//判断集合A是否为空
			if (sizeSrcRe == 0) 
			{
				ResponseResult result = new ResponseResult();
				result.setRCode(1);//result.rCode = rCode.error;
				result.setMessage("Input srcList is Empty");//result.message = "Input List Empty";
				result.setList(null);//result.list = null;
				return result;
			}
			else 
			{
				ResponseResult result = new ResponseResult();
				List<SearchImgResult> sortAgain = new ArrayList<SearchImgResult>();// 多图检索结果集合C
				List<SearchImgResult> srcThreshold = new ArrayList<SearchImgResult>();// A大于阈值集合
				double sortmax=0;//图集A、B最大值
				double sortmin;//图集A、B最小值
				sortmin=srcResults.get(sizeSrcRe-1).score;
				//取图集A、B共有元素计算，同时记录共有元素在B中的位置
				double threshold = 99;// 阈值
				int sign=0;
				for(int i=0; i<sizeSrcRe; i++)
				{
					if(srcResults.get(i).score>= threshold)
					{
						//判断图集A中元素是否为原图，若为原图不操作，保存该图信息
						srcThreshold.add(srcResults.get(i));
						continue;
					}
					if (srcResults.get(i).score > sortmax && srcResults.get(i).score < threshold) 
					{
						//取图集A中小于threshold值最大分数为归一化上限
						sortmax = srcResults.get(i).score;// 提取A中<threshold元素最大值
					}
					for(int j=0; j<sizeNegRe; j++)
					{
						if(srcResults.get(i).recordId.equals(negativeResults.get(j).recordId))
						{
							String recordId = srcResults.get(i).recordId;
							String number = srcResults.get(i).number;
							Long gatherTime = srcResults.get(i).gatherTime;
							Double Score = srcResults.get(i).score - Math.exp(-(j+1) / 50)* negativeResults.get(j).score 
									+ 0.01* Math.exp(-(i+j+2) / 50)* srcResults.get(i).score* negativeResults.get(j).score;
							if (negativeResults.get(j).score<99 && Score>0)
							    sortAgain.add(new SearchImgResult(recordId, number, gatherTime, Score));
							sign=1;
							break;
						}
				    }
					if(sign==0)
					{
						sortAgain.add(srcResults.get(i));
					}
					sign=0;
			    }
				//计算归一化上限值
				if(sortmax==0)
					sortmax=threshold;
				//多图检索结果归一化
				/*
				Collections.sort(sortAgain, new ScoreComparator());// 倒序排序检索结果
				if(sortAgain.size() != 0)
				{
					double nowmax = sortAgain.get(0).score;// 获取多图检索后结果上限值
					double nowmin = sortAgain.get(sortAgain.size() - 1).score;// 获取多图检索后结果下限值
					for(int i=0; i< sortAgain.size(); i++)
					{
						if (nowmax==nowmin)
							break;
						else
						{
							String recordId = sortAgain.get(i).recordId;
							String number = sortAgain.get(i).number;
							Long gatherTime = sortAgain.get(i).gatherTime;
							Double Score = (sortmax - sortmin) / (nowmax - nowmin) * (sortAgain.get(i).score - nowmin)
									+ sortmin;// 归一化检索结果值
							sortAgain.set(i, new SearchImgResult(recordId, number, gatherTime, Score));
						}
					}
				} 
				*/
				sortAgain.addAll(srcThreshold);
				Collections.sort(sortAgain, new ScoreComparator());
				result.setRCode(2);//result.rCode = rCode.OK;
				result.setMessage("SUCESS");//result.message = "SUCESS";
				result.setList(sortAgain);//result.list = sortAgain;
				return result;
		    }
		}
	}
	// 自定义比较器：按相似度分数排序
		class ScoreComparator implements Comparator<Object> 
		{
			public int compare(Object object1, Object object2) 
			{// 实现接口中的方法
				SearchImgResult p1 = (SearchImgResult) object1; // 强制转换
				SearchImgResult p2 = (SearchImgResult) object2;
				return new Double(p2.score).compareTo(new Double(p1.score));
			}
		}

}
