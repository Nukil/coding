package com.netposa.poseidon.connection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import com.netposa.poseidon.bean.ResponseResult;
import com.netposa.poseidon.bean.ResponseResult.rCode;
import com.netposa.poseidon.bean.SearchImgResult;

public class AuxiliayRetirvalOrgImpl implements AuxiliayRetirvalOrg {
	// private static final ResponseResult ResponseResult = null;
	public ResponseResult auxiliayRetirvalorg(List<SearchImgResult> srcResults, List<SearchImgResult> positiveResults) {
		if (srcResults == null || positiveResults == null) {
			ResponseResult result = new ResponseResult();
			result.rCode = rCode.stand;
			result.message = "Input List Error";
			result.list = null;
			return result;
		} else {
			int sizeSrcRe = srcResults.size();
			int sizeProRe = positiveResults.size();
			//如果检索结合有空 则返回非空集合
			if (sizeSrcRe == 0 || sizeProRe ==0) {
				ResponseResult result = new ResponseResult();
				result.rCode = rCode.OK;
				result.message = "Input List Empty";
				result.list = sizeProRe > sizeSrcRe ? positiveResults : srcResults;
				return result;
			} else {
/*				if (sizeProRe == 0) {
					ResponseResult result = new ResponseResult();
					result.rCode = rCode.OK;
					result.message = "Sucess But PositiveResults is Empty";
					result.list = srcResults;
					return result;
				} else {*/
					ResponseResult result = new ResponseResult();
					// 计算准备工作
					List<SearchImgResult> sortAgain = new ArrayList<SearchImgResult>();// 多图检索结果集合C
					List<SearchImgResult> srcListsign = new ArrayList<SearchImgResult>();// A集合中大于threshold值存放集合
					List<SearchImgResult> posListsign = new ArrayList<SearchImgResult>();// B集合中大于threshold值存放集合
					List<Integer> srcSign = new ArrayList<Integer>();// 记录A图集中共有元素、大于阈值元素位置
					List<Integer> posSign = new ArrayList<Integer>();// 记录B图集中共有元素、大于阈值元素位置
					double threshold = 99;// 阈值
					double maxsrc = 0;// A小于阈值最大值
					double maxpos = 0;// B小于阈值最大值
					String recordId;
					String number;
					long gatherTime;
					double Score;
					// 图集A与图集B相同图片进行计算，A有B没有的复制A（当A>threshold时不复制）
					for (int i = 0; i < sizeSrcRe; i++) {
						if (srcResults.get(i).score >= threshold) {
							srcListsign.add(srcResults.get(i));// A中>threshold元素存储
							srcSign.add(i);
							continue;
						}
						if (srcResults.get(i).score > maxsrc && srcResults.get(i).score < 99.9) {
							maxsrc = srcResults.get(i).score;// 提取A中<threshold元素最大值
						}
						for (int j = 0; j < sizeProRe; j++) {
							if ((srcResults.get(i).recordId).equals(positiveResults.get(j).recordId)) {
								if (positiveResults.get(j).score >= threshold) {
									srcSign.add(i);// 当元素在A中<threshold而在B中>threshold时不计算
									break;
								}
								recordId = srcResults.get(i).recordId;
								number = srcResults.get(i).number;
								gatherTime = srcResults.get(i).gatherTime;
								Score = (double) (srcResults.get(i).score
										+ Math.exp(-(2 * j + i + 3) / 100) * positiveResults.get(j).score);
								sortAgain.add(new SearchImgResult(recordId, number, gatherTime, Score));
								srcSign.add(i);// 记录A中共有元素位置
								posSign.add(j);// 记录B中共有元素位置
								break;
							}
						}
					}
					// B中>threshold记录，取B最大值
					int sign = 0;
					for (int i = 0; i < sizeProRe; i++) {
						if (positiveResults.get(i).score >= threshold) {
							for (int j = 0; j < srcListsign.size(); j++) {
								if (positiveResults.get(i).recordId.equals(srcListsign.get(j).recordId)) {
									// 若B中>threshold元素A中也>threshold，则以A为准，不存储
									sign = 1;
									posSign.add(i);
									break;
								}
							}
							if (sign == 0) {
								// 否则存储元素
								posListsign.add(positiveResults.get(i));
								posSign.add(i);
								continue;
							}
							sign = 0;
						} else {
							// 记录B中<threshold元素最大值
							if (positiveResults.get(i).score > maxpos)
								maxpos = positiveResults.get(i).score;
							for (int j = 0; j < srcListsign.size(); j++) {
								if (positiveResults.get(i).recordId.equals(srcListsign.get(j).recordId)) {
									posSign.add(i);
									break;
								}
							}
						}
					}
					// 计算多图检索前输入数据值得上下限，便于做数值归一化
					double sortmax = Math.max(maxsrc, maxpos);
					if (sortmax == 0)
						sortmax = threshold;
					double sortmin = Math.min(srcResults.get(sizeSrcRe - 1).score,
							positiveResults.get(sizeProRe - 1).score);
					if (sortmin > threshold)
						sortmin = threshold;
					// 添加A、B图集中剩余图片
					srcSign = new ArrayList<Integer>(new LinkedHashSet<>(srcSign));// 去除重复
					posSign = new ArrayList<Integer>(new LinkedHashSet<>(posSign));// 去除重复
					Collections.sort(srcSign);
					Collections.reverse(srcSign);
					Collections.sort(posSign);
					Collections.reverse(posSign);
					for (int i = 0; i < srcSign.size(); i++) {
						int flag = srcSign.get(i);
						srcResults.remove(flag);
					}
					for (int j = 0; j < posSign.size(); j++) {
						int flag = posSign.get(j);
						positiveResults.remove(flag);
					}
					sortAgain.addAll(srcResults);
					sortAgain.addAll(positiveResults);
					if (sortAgain.size() != 0) {
						// 结果归一化
						Collections.sort(sortAgain, new ScoreComparator());// 倒序排序检索结果
						double nowmax = sortAgain.get(0).score;// 获取多图检索后结果上限值
						double nowmin = sortAgain.get(sortAgain.size() - 1).score;// 获取多图检索后结果下限值
						for (int i = 0; i < sortAgain.size(); i++) {
							if (nowmax == nowmin) {
								recordId = sortAgain.get(i).recordId;
								number = sortAgain.get(i).number;
								gatherTime = sortAgain.get(i).gatherTime;
								Score = sortmax - ((sortmax - sortmin) * (200 - nowmax) / (200 - sortmin));
								sortAgain.set(i, new SearchImgResult(recordId, number, gatherTime, Score));
							} else {
								recordId = sortAgain.get(i).recordId;
								number = sortAgain.get(i).number;
								gatherTime = sortAgain.get(i).gatherTime;
								Score = (sortmax - sortmin) / (nowmax - nowmin) * (sortAgain.get(i).score - nowmin)
										+ sortmin;// 归一化检索结果值
								sortAgain.set(i, new SearchImgResult(recordId, number, gatherTime, Score));
							}
						}
					}
					// 添加输入集合中大于阈值元素
					sortAgain.addAll(srcListsign);
					sortAgain.addAll(posListsign);
					// 排序图集C
					Collections.sort(sortAgain, new ScoreComparator());
					// Collections.reverse(sortAgain);
					result.rCode = rCode.OK;
					result.message = "SUCESS";
					result.list = sortAgain;
					// TODO Auto-generated method stub
					return result;
				}
			//}
		}
	}

	// 自定义比较器：按相似度分数排序
	class ScoreComparator implements Comparator<Object> {
		public int compare(Object object1, Object object2) {// 实现接口中的方法
			SearchImgResult p1 = (SearchImgResult) object1; // 强制转换
			SearchImgResult p2 = (SearchImgResult) object2;
			return new Double(p2.score).compareTo(new Double(p1.score));
		}
	}

}
