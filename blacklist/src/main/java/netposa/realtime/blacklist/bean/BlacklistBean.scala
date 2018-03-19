package netposa.realtime.blacklist.bean

import scala.collection.mutable.ListBuffer
import org.apache.commons.lang.StringUtils

class BlacklistBean(val id: Long,
                    val platenumber: Array[Byte],
                    val platecolor: Array[Byte],
                    val vehiclebrand: Array[Byte],
                    val vehiclecolor: Array[Byte],
                    val vehicletype: Array[Byte],
                    val orgid: String,
                    val monitorid: String,
                    val begintime: Long,
                    val endtime: Long,
                    val alarmstatus: String,
                    val alarmtype: String,
                    val controllevel: String,
                    val controluser: String,
                    val disable_platecolor: Boolean,
                    val disable_vehiclebrand: Boolean,
                    val disable_vehiclecolor: Boolean,
                    val disable_vehicletype: Boolean,
                    val disable_orgid: Boolean,
                    val disable_monitorid: Boolean,
                    val enable_alarm: Boolean) {

    val orgids = init(orgid)
    val monitorids = init(monitorid)

    private def init(ids: String): Array[Array[Byte]] = {
        if (null != ids) {
            val buffer = new ListBuffer[Array[Byte]]()
            val ids_arr = ids.split(',')
            ids_arr.foreach(id => {
                val value = StringUtils.trimToNull(id)
                if (null != value) {
                    buffer += value.getBytes()
                }
            })
            buffer.toArray
        } else {
            null
        }
    }
}
