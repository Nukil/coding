namespace java com.netposa.poseidon.face.rpc.outrpc

enum StatusCode {
  OK = 1,                           //操作成功
  ERROR_PARAM = 2,   	            //参数错误
  ERROR_NOT_EXIST_LIBRARY = 3,        //库不存在
  ERROR_OTHER = 10                   //其他错误
}

//每个区域的筛选条件对象
struct RegionRecord {
  1: list<string> cameraIds, //每个区域包含的相机编号集合
  2: i64 startTime,
  3: i64 endTime
}
//碰撞分析输入条件
struct CollisionAyalyzeInputRecord {
  1: list<RegionRecord> regionRecords, //区域对象集合
  2: i32 regionCount, //至少出现的区域个数阈值
  3: i32 distence //相似度阈值
}
//碰撞分析输出结果
struct CollisionAyalyzeResult {
  1: list<list<string>> logNums, //多个人员集合，集合内的每个元素又代表这个人对应的多张照片日志编号
  2: i32 regionCount //该多个人员出现的区域个数
}
//伴随人员分析输入条件
struct AccompanyAyalyzeInputRecord  {
  1: i64 startTime,
  2: i64 endTime,
  3: binary feature,
  4: i32 distence, //相似度阈值ֵ
  5: i32 accompanyCount, //伴随频次阈值
  6: i32 accompanyTime,	//伴随时间阈值,即通过前后多久认定为伴随（单位：s）
  7: string cameraId //默认传全部cameraId,多个Id之间用逗号隔开
}
//伴随人员分析输出结果
struct AccompanyAyalyzeResult{
  1: list<string> logNums, //该人员对应的多张照片的人脸日志编号
  2: i32 accompanyCount //伴随频次
}
//频繁出没输入条件
struct FrequentPassInputRecord {
  1: i64 startTime,
  2: i64 endTime,
  3: string cameraId,
  4: i32 passCount,	//经过次数阈值
  5: i32 distence //相似度阈值
}
//行为跟踪返回结果
struct FaceTrackingResult {
  1: list<SearchImgResult> faceImgs,
  2: string cameraId,
  3: i32 occurrencesCount //出现次数
}
//轨迹找人输入条件
struct SearchHumanByTrackInputRecord {
  1: i64 startTime,
  2: i64 endTime,
  3: string cameraId,
  4: i32 distence
}

//以人脸图搜图输入条件
struct SearchImgByImgInputRecord {
  1: i64 startTime,
  2: i64 endTime,
  3: string cameraId,
  4: binary feature,
  5: i32 distance,     //相似度阈值ֵ
  6: i32 count         //返回结果个数(相似度降序排序TopN)
}
//日志编号找人输入参数
struct SearchImgByLogInputRecord {
  1: i64 startTime,
  2: i64 endTime,
  3: string cameraId,
  4: string logNum,
  5: string date,      //absTime yyyyMMdd
  6: i32 distance,     //相似度阈值ֵ
  7: i32 count         //返回结果个数(相似度降序排序TopN)
}
//以图搜图输出结果
struct SearchImgResult{
  1: string logNum,   //日志编号
  2: string cameraId, //相机编号
  3: i64 gatherTime,  //相片采集时间
  4: double score     //相似度
}
/**
 * 人脸特征1:1
**/
struct FeatureVerifyRecord{
  1:binary feature1,
  2:binary feature2
}

/**
* 人脸辅助检索输入
**/
struct SearchImgByAssistInputRecord {
    1: list<SearchImgResult> resultList, //上次计算的结果集
    2: list<binary> assist, //辅助目标特征集合
    3: list<binary> exclude, //排除目标特征集合
    4: string cameraID, //相机编号
    5: i64 startTime, //开始时间
    6: i64 endTime, //结束时间
    7: i32 assistDistance, //辅助目标阈值
    8: i32 excludeDistance, //排除目标阈值
    9: i32 count //返回结果数量
}
struct SearchImgByAssistResponse {
    1: StatusCode rCode,
    2: string message,
    3: list<SearchImgResult> result
}

/**
* 人脸日志编号查特征输入
**/
struct SearchFeatureByLogInputRecord {
    1: string logNum, //日志编号
    2: i64 gatherTime //采集时间
}
struct SearchFeatureByLogResponse {
    1: StatusCode rCode,
    2: string message,
    3: binary feature
}

service FaceFeatureDataAnalyzeRpcService {
    //碰撞分析接口
    list<CollisionAyalyzeResult> collisionAyalyze(1:CollisionAyalyzeInputRecord record)
    //伴随人员分析接口
    list<AccompanyAyalyzeResult> accompanyAyalyze(1:AccompanyAyalyzeInputRecord record)
    //频繁出没接口
    list<AccompanyAyalyzeResult> frequentPass(1:FrequentPassInputRecord record)
    //行为跟踪接口
    list<FaceTrackingResult> faceTracking(1:SearchImgByImgInputRecord record)
    //轨迹找人接口
    list<AccompanyAyalyzeResult> searchHumanByTrack(1:SearchHumanByTrackInputRecord record)

    //以图搜图接口
    list<SearchImgResult> searchFaceImgByImg(1:SearchImgByImgInputRecord record)
    //日志编号查人接口
    list<SearchImgResult> searchFaceImgByLog(1:SearchImgByLogInputRecord record)
    //批量检索接口
    double featureVerify(1:FeatureVerifyRecord record)
    //人脸辅助检索
    SearchImgByAssistResponse searchFaceImgByAssist(1: SearchImgByAssistInputRecord record)
    //根据记录编号查特征
    SearchFeatureByLogResponse searchFeatureByLog(1: SearchFeatureByLogInputRecord record)
}
