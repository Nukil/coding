namespace java com.netposa.poseidon.face.rpc.inrpc

enum StatusCode {
  OK = 1,                           //操作成功
  ERROR_PARAM = 2,   	            //参数错误
  ERROR_NOT_EXIST_LIBRARY = 3,      //库不存在
  ERROR_SERVICE_EXCEPTION = 4,      // 服务异常
  ERROR_OTHER = 10                  //其他错误
}

//以人脸图搜图输入条件
struct SearchImgByImgInputRecord {
  1: i64 startTime,
  2: i64 endTime,
  3: string cameraId,
  4: list<double> feature,
  5: i32 distance,     //相似度阈值ֵ
  6: i32 count         //返回结果个数(相似度降序排序TopN)
}
//以图搜图输出结果
struct SearchImgResult{
  1: string logNum,   //日志编号
  2: string cameraId, //相机编号
  3: i64 gatherTime,  //相片采集时间
  4: double score     //相似度
}
struct SearchImgByImgResponse {
  1: StatusCode rCode,
  2: string message,
  3: list<SearchImgResult> results
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

/**
* 热数据数据结构
**/
struct RecentDataBean {
    1: string logNum,
    2: string cameraId,
    3: i64 gatherTime,
    4: list<double> feature
}
/**
* 热数据存储输入
**/
struct SetRecentDataInputRecord {
    1: list<RecentDataBean> dataList
}
/**
* 热数据存储返回值
**/
struct SetRecentDataResponse {
    1: StatusCode rCode,
    2: string message
}

/*
 * ping返回值
 */
struct PingResponse {
    1: string message
}

service FaceFeatureDataAnalyzeRpcService {
    //人体以图搜图接口
    SearchImgByImgResponse searchFaceImgByImg(1:SearchImgByImgInputRecord record, 2: string uuid)
    //热数据插入
    SetRecentDataResponse setRecentData(1:SetRecentDataInputRecord record, 2: string uuid)
    //检测server心跳
    PingResponse ping()
}