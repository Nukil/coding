package netposa.realtime.kafka.utils

import kafka.common.TopicAndPartition

/**
 * 此class用于得到KAFKA的消息,并存储消息对应的TOPIC/PARTITION与当前消息的offset
 */
class RowMessage_bak(val tp:TopicAndPartition, val message:Array[Byte], val offset:Long) {
}