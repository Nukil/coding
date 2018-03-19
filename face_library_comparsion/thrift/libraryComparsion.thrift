namespace java com.netposa.poseidon.library.rpc
enum StatusCode
{
  OK = 1,                      //操作成功
  ERROR_PARAM = 2,   	         //参数错误
  ERROR_NOT_EXIST_LIBRARY = 3, //库不存在
  ERROR_OTHER = 10                   //其他错误
}
enum OperationType
{
	INSERT = 1,
	UPDATE = 2,
	DELETE = 3,
	SELECT = 4
}
//库操作
struct LibraryOperation{
	1: OperationType operationType,  //库操作类型
	2: string libraryId,             //库id
	3: i16 libraryType               //库类型，1表示大库，2表示小库。库创建时传入，创建完成后不支持修改库类型。
}
struct LibraryOperationResponse{
  1: StatusCode rCode,              //请求返回错误码
  2: string message          //错误码对应错误信息
}

//数据操作参数
struct ImgInfo{
  1: string imgId,
  2: binary feature
}
struct StorageInfo{
  1: string libraryId,
  2: string id,                    //编号
  3: list<ImgInfo> imgInfos,    //特征数据，插入语句时不能为空
  4: string ext                    //扩展检索字段
}
struct StorageRequest{
  1: list<StorageInfo> storageInfos,
  2: OperationType type 
}

struct StorageResponse{
  1: StatusCode rCode,
  2: string message
}
//库检索请求
struct QueryRequest{
  1: list<string> libraryIds,
  2: i16 similarity,               //相似度阈值,取值(0,100)
  3: i32 rCount,                   //返回结果数量,similarity与rCount不能同时为空
  4: binary feature,
  5: string requestId
}
//大库检索响应接口
struct ImgScore{
  1: string imgId,
  2: double score
}
struct RecordInfo{
  1: string id, //人员ID
  2: list<ImgScore> imgScore,    //相似度分数
  3: string ext,   // 其它信息
  4: double highestScore, //  最高分数（用于按照此属性降序排序）
  5: string highestScoreImgId // 最高分数对应的照片ID（业务层可以直接拿过来展现）
}
struct FeatureVerifyRecord{
  1:binary feature1,
  2:binary feature2
}

struct QueryResponse{
  1: StatusCode rCode,
  2: string message,
  3: list<RecordInfo> recordInfos   //记录结果信息
}
//接口
service LibraryDataAnalyzeRpcService {
  //库操作接口
  LibraryOperationResponse libraryOperate(1:LibraryOperation request)
  //数据操作接口
  StorageResponse dataOperate(1:StorageRequest request)
  //检索接口
  QueryResponse dataQuery(1:QueryRequest request)
  //批量检索接口
  map<string,QueryResponse> dataBatchQuery(1:list<QueryRequest> queryRequests)
  //批量检索接口
  double featureVerify(1:FeatureVerifyRecord record)
}
