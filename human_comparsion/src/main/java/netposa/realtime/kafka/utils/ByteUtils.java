package netposa.realtime.kafka.utils;

import sun.util.calendar.*;

import java.util.HashMap;
import java.util.Map;

public final class ByteUtils {
  private static final Gregorian gcal = CalendarSystem.getGregorianCalendar();
  private static JulianCalendar jcal;
  private static final long DEFAULT_GREGORIAN_CUTOVER = -12219292800000L;
  private static final int EPOCH_OFFSET = 719163; // Fixed date of January 1,
                                                  // 1970 (Gregorian)
  private static final int ONE_SECOND = 1000;
  private static final int ONE_MINUTE = 60 * ONE_SECOND;
  private static final int ONE_HOUR = 60 * ONE_MINUTE;
  private static final long ONE_DAY = 24 * ONE_HOUR;
  private static final long gregorianCutoverDate = (((DEFAULT_GREGORIAN_CUTOVER + 1) / ONE_DAY) - 1)
      + EPOCH_OFFSET;

  private static final byte YEAR_BATE = 'y';
  private static final byte MONTH_BATE = 'M';
  private static final byte DAY_BATE = 'd';
  private static final byte HOUR_BATE = 'H';
  private static final byte MINUTE_BATE = 'm';
  private static final byte SECOND_BATE = 's';
  private static final byte MS_BATE = 'S';
  private static final byte[] NOW_TIME = new byte[] { 'N', 'O', 'W', '(', ')' };
  
  private static final byte ZERO = '0';
  private static final byte NEGATIVE = '-';
  private static final byte NOT_NEGATIVE = '+';

  private static final Map<DateFormatDefine, int[]> date_pattern = buildDatePattern("yyyyMMddHHmmssSSS");

  /**
   * 把一个指定的short类型的值转换成2byte并从buffer数组中offset的位置开始写入到buffer中
   * 
   * @param val
   * @param buffer
   * @param offset
   */
  public static void toBytes(short val, byte[] buffer, int offset) {
    buffer[offset + 1] = (byte) val;
    val >>= 8;
    buffer[offset + 0] = (byte) val;
  }

  /**
   * 把一个指定的float类型的值转换成4byte并从buffer数组中offset的位置开始写入到buffer中
   * 
   * @param f
   * @param buffer
   * @param offset
   */
  public static void toBytes(final float f, byte[] buffer, int offset) {
    // Encode it as int
    toBytes(Float.floatToRawIntBits(f), buffer, offset);
  }

  /**
   * 把一个指定的int类型的值转换成4byte并从buffer数组中offset的位置开始写入到buffer中
   * 
   * @param val
   * @param buffer
   * @param offset
   */
  public static void toBytes(int val, byte[] buffer, int offset) {
    for (int i = 3; i > 0; i--) {
      buffer[offset + i] = (byte) val;
      val >>>= 8;
    }
    buffer[offset] = (byte) val;
    /*
     * for (int i = 0; i < 3; i++) { buffer[offset + i] = (byte) val; val >>>=
     * 8; } buffer[offset+3] = (byte) val;
     */
  }

  /**
   * 把一个指定的double类型的值转换成8byte并从buffer数组中offset的位置开始写入到buffer中
   * 
   * @param d
   * @param buffer
   * @param offset
   */
  public static void toBytes(final double d, byte[] buffer, int offset) {
    // Encode it as a long
    toBytes(Double.doubleToRawLongBits(d), buffer, offset);
  }

  /**
   * 把一个指定的long类型的值转换成8byte并从buffer数组中offset的位置开始写入到buffer中
   * 
   * @param val
   * @param buffer
   * @param offset
   */
  public static void toBytes(long val, byte[] buffer, int offset) {
    for (int i = 7; i > 0; i--) {
      buffer[offset + i] = (byte) val;
      val >>>= 8;
    }
    buffer[offset] = (byte) val;
  }

  public static long toLong(byte[] bytes, int offset, final int length) {
    if (length != 8 || offset + length > bytes.length) {
      throw explainWrongLengthOrOffset(bytes, offset, length, 8);
    }
    long l = 0;
    for (int i = offset; i < offset + length; i++) {
      l <<= 8;
      l ^= bytes[i] & 0xFF;
    }
    return l;
  }

  public static long toLong(byte[] bytes, int offset) {
    return toLong(bytes, offset, 8);
  }

  public static double toDouble(final byte[] bytes, final int offset) {
    return Double.longBitsToDouble(toLong(bytes, offset, 8));
  }

  public static int toInt(byte[] bytes, int offset, final int length) {
    if (length != 4 || offset + length > bytes.length) {
      throw explainWrongLengthOrOffset(bytes, offset, length, 4);
    }
    int n = 0;
    for (int i = offset; i < (offset + length); i++) {
      n <<= 8;
      n ^= bytes[i] & 0xFF;
    }
    /*
     * int n = 0; for (int i = (offset + length-1); i >= offset; i--) { n <<= 8;
     * n ^= bytes[i] & 0xFF; }
     */
    return n;
  }

  public static int toInt(byte[] bytes, int offset) {
    return toInt(bytes, offset, 4);
  }

  public static float toFloat(byte[] bytes, int offset) {
    return Float.intBitsToFloat(toInt(bytes, offset, 4));
  }

  private static IllegalArgumentException explainWrongLengthOrOffset(
      final byte[] bytes, final int offset, final int length,
      final int expectedLength) {
    String reason;
    if (length != expectedLength) {
      reason = "Wrong length: " + length + ", expected " + expectedLength;
    } else {
      reason = "offset (" + offset + ") + length (" + length + ") exceed the"
          + " capacity of the array: " + bytes.length;
    }
    return new IllegalArgumentException(reason);
  }

  public static int indexOf(byte[] array, byte[] target) {
    if (target.length == 0) {
      return 0;
    }

    outer: for (int i = 0; i < array.length - target.length + 1; i++) {
      for (int j = 0; j < target.length; j++) {
        if (array[i + j] != target[j]) {
          continue outer;
        }
      }
      return i;
    }
    return -1;
  }

  public static boolean contains(byte[] array, byte[] target) {
    return indexOf(array, target) > -1;
  }

  public static boolean exists(byte[][] list, byte[] target) {
    boolean flag = false;
    if (list == null || list.length == 0) {
      return flag;
    }
    for (int i = 0; i < list.length; i++) {
      flag = equals(list[i], target);
      if (flag) {
        return true;
      }
    }
    return false;
  }

  public static boolean equals(final byte[] left, final byte[] right) {
    // Could use Arrays.equals?
    // noinspection SimplifiableConditionalExpression
    if (left == right)
      return true;
    if (left == null || right == null)
      return false;
    if (left.length != right.length)
      return false;
    if (left.length == 0)
      return true;

    // Since we're often comparing adjacent sorted data,
    // it's usual to have equal arrays except for the very last byte
    // so check that first
    if (left[left.length - 1] != right[right.length - 1])
      return false;

    return compareTo(left, 0, left.length, right, 0, right.length) == 0;
  }

  public static boolean startsWith(byte[] bytes, byte[] prefix) {
    return bytes != null && prefix != null && bytes.length >= prefix.length
        && compareTo(bytes, 0, prefix.length, prefix, 0, prefix.length) == 0;
  }

  public static boolean endsWith(byte[] bytes, byte[] prefix) {
    return bytes != null
        && prefix != null
        && bytes.length >= prefix.length
        && compareTo(bytes, bytes.length - prefix.length, prefix.length,
            prefix, 0, prefix.length) == 0;
  }

  public static int compareTo(byte[] buffer1, int offset1, int length1,
      byte[] buffer2, int offset2, int length2) {
    // Short circuit equal case
    if (buffer1 == buffer2 && offset1 == offset2 && length1 == length2) {
      return 0;
    }
    // Bring WritableComparator code local
    int end1 = offset1 + length1;
    int end2 = offset2 + length2;
    for (int i = offset1, j = offset2; i < end1 && j < end2; i++, j++) {
      int a = (buffer1[i] & 0xff);
      int b = (buffer2[j] & 0xff);
      if (a != b) {
        return a - b;
      }
    }
    return length1 - length2;
  }

  /**
   * 通过传入一个指定的日期时间格式字符串,解析中此格式中每个日期字段所在的位置<br/>
   * 响应参数中,map的key为日期中各个字段的表示定义,<br/>
   * value为一个长度为２的数组,数组第0个下标为字段的开始位置,数组第1个下标为字段的长度.
   * 
   * @param pattern
   *          在pattern的表示中,yyyy表示年,MM表示月,dd表示日期,HH表示小时,mm表示分钟,ss表示秒,SSS表示毫秒
   * @return
   */
  public static Map<DateFormatDefine, int[]> buildDatePattern(String pattern) {
    Map<DateFormatDefine, int[]> date_time = new HashMap<DateFormatDefine, int[]>();

    int year_offset = -1, year_length = 0;
    int month_offset = -1, month_length = 0;
    int day_offset = -1, day_length = 0;
    int hour_offset = -1, hour_length = 0;
    int minute_offset = -1, minute_length = 0;
    int second_offset = -1, second_length = 0;
    int ms_offset = -1, ms_length = 0;

    pattern = pattern == null ? "" : pattern.trim();
    char[] format_arr = pattern.toCharArray();
    for (int i = 0; i < format_arr.length; i++) {
      switch (format_arr[i]) {
      case YEAR_BATE:
        if (year_offset == -1) {
          year_offset = i;
        }
        year_length++;
        break;
      case MONTH_BATE:
        if (month_offset == -1) {
          month_offset = i;
        }
        month_length++;
        break;
      case DAY_BATE:
        if (day_offset == -1) {
          day_offset = i;
        }
        day_length++;
        break;
      case HOUR_BATE:
        if (hour_offset == -1) {
          hour_offset = i;
        }
        hour_length++;
        break;
      case MINUTE_BATE:
        if (minute_offset == -1) {
          minute_offset = i;
        }
        minute_length++;
        break;
      case SECOND_BATE:
        if (second_offset == -1) {
          second_offset = i;
        }
        second_length++;
        break;
      case MS_BATE:
        if (ms_offset == -1) {
          ms_offset = i;
        }
        ms_length++;
        break;

      default:
        break;
      }
    }

    date_time
        .put(DateFormatDefine.YEAR, new int[] { year_offset, year_length });
    date_time.put(DateFormatDefine.MONTH, new int[] { month_offset,
        month_length });
    date_time.put(DateFormatDefine.DATE, new int[] { day_offset, day_length });
    date_time
        .put(DateFormatDefine.HOUR, new int[] { hour_offset, hour_length });
    date_time.put(DateFormatDefine.MINUTE, new int[] { minute_offset,
        minute_length });
    date_time.put(DateFormatDefine.SECOND, new int[] { second_offset,
        second_length });
    date_time.put(DateFormatDefine.MILLISECOND, new int[] { ms_offset,
        ms_length });
    return date_time;
  }

  synchronized private static final BaseCalendar getJulianCalendarSystem() {
    if (jcal == null) {
      jcal = (JulianCalendar) CalendarSystem.forName("julian");
    }
    return jcal;
  }

  private static long getFixedDate(BaseCalendar cal, int year, int month,
      int day) {
    if (month > java.util.Calendar.DECEMBER) {
      year += month / 12;
      month %= 12;
    } else if (month < java.util.Calendar.JANUARY) {
      int[] rem = new int[1];
      year += CalendarUtils.floorDivide(month, 12, rem);
      month = rem[0];
    }

    long fixedDate = cal.getFixedDate(year, month + 1, 1, null);
    fixedDate += day;
    fixedDate--;

    return fixedDate;
  }

  /**
   * 把一个指定的年/月/日/时(24小时类型)/分/秒/毫秒转换成一个long类型的时间值<br/>
   * 
   * @param year
   * @param month
   * @param day
   * @param hour
   * @param minute
   * @param second
   * @param millisecond
   * @return
   */
  public static long parse2LongTime(int year, int month, int day, int hour,
      int minute, int second, int millisecond) {
    long timeOfDay = 0;
    timeOfDay += hour;
    timeOfDay *= 60;
    timeOfDay += minute;
    timeOfDay *= 60;
    timeOfDay += second;
    timeOfDay *= 1000;
    timeOfDay += millisecond;

    long fixedDate = timeOfDay / ONE_DAY;
    timeOfDay %= ONE_DAY;
    while (timeOfDay < 0) {
      timeOfDay += ONE_DAY;
      --fixedDate;
    }

    if (month > 11) {
      year += month / 12;
      month %= 12;
    } else if (month < 0) {
      int[] rem = new int[1];
      year += CalendarUtils.floorDivide(month, 12, rem);
      month = rem[0];
    }
    calculateFixedDate: {
      long gfd, jfd;
      if (year > 1582) {
        gfd = fixedDate + getFixedDate(gcal, year, month, day);
        if (gfd >= gregorianCutoverDate) {
          fixedDate = gfd;
          break calculateFixedDate;
        }
        jfd = fixedDate
            + getFixedDate(getJulianCalendarSystem(), year, month, day);
      } else if (year < 1582) {
        jfd = fixedDate
            + getFixedDate(getJulianCalendarSystem(), year, month, day);
        if (jfd < gregorianCutoverDate) {
          fixedDate = jfd;
          break calculateFixedDate;
        }
        gfd = jfd;
      } else {
        jfd = fixedDate
            + getFixedDate(getJulianCalendarSystem(), year, month, day);
        gfd = fixedDate + getFixedDate(gcal, year, month, day);
      }

      if (gfd >= gregorianCutoverDate) {
        fixedDate = gfd;
        /*
         * if (jfd >= gregorianCutoverDate) { fixedDate = gfd; } else { if
         * (calsys == gcal || calsys == null) { fixedDate = gfd; } else {
         * fixedDate = jfd; } }
         */
      } else {
        if (jfd < gregorianCutoverDate) {
          fixedDate = jfd;
        } else {
          fixedDate = jfd;
        }
      }
    }
    long millis = (fixedDate - EPOCH_OFFSET) * ONE_DAY + timeOfDay;
    // 上海的时区,减去28800000加上0
    millis -= 28800000;
    return millis;
  }

  /**
   * 通过指定的日期格式把按此格式进行存储的一个byte array数组的值转换成一个long 类型的时间值
   * 
   * @param array
   * @param pattern
   * @return
   */
  public static long parseBytesToLongtime(byte[] array) {
    if (equalsIgnoreCase(array, 0, array.length, NOW_TIME, 0, NOW_TIME.length)) {
      return System.currentTimeMillis();
    }

    int year = 0, month = 0, date = 0, hour = 0, minute = 0, second = 0, ms = 0;
    int[] arr = null;
    for (DateFormatDefine define : date_pattern.keySet()) {
      switch (define) {
      case YEAR:
        arr = date_pattern.get(define);
        if (arr[0] != -1 && array.length >= (arr[0] + arr[1])) {
          year = parseInt(array, arr[0], arr[1]);
        }
        break;
      case MONTH:
        arr = date_pattern.get(define);
        if (arr[0] != -1 && array.length >= (arr[0] + arr[1])) {
          month = parseInt(array, arr[0], arr[1]);
          month = month - 1;
        }
        break;
      case DATE:
        arr = date_pattern.get(define);
        if (arr[0] != -1 && array.length >= (arr[0] + arr[1])) {
          date = parseInt(array, arr[0], arr[1]);
        }
        break;
      case HOUR:
        arr = date_pattern.get(define);
        if (arr[0] != -1 && array.length >= (arr[0] + arr[1])) {
          hour = parseInt(array, arr[0], arr[1]);
        }
        break;
      case MINUTE:
        arr = date_pattern.get(define);
        if (arr[0] != -1 && array.length >= (arr[0] + arr[1])) {
          minute = parseInt(array, arr[0], arr[1]);
        }
        break;
      case SECOND:
        arr = date_pattern.get(define);
        if (arr[0] != -1 && array.length >= (arr[0] + arr[1])) {
          second = parseInt(array, arr[0], arr[1]);
        }
        break;
      case MILLISECOND:
        arr = date_pattern.get(define);
        if (arr[0] != -1 && array.length >= (arr[0] + arr[1])) {
          ms = parseInt(array, arr[0], arr[1]);
        }
        break;
      }
    }
    long time = parse2LongTime(year, month, date, hour, minute, second, ms);
    return time;
  }

  private static int parseInt(byte[] array, int offset, int length) {
    int result = 0;
    boolean negative = false;
    int i = offset, len = length;
    int limit = -Integer.MAX_VALUE;
    int multmin;
    int digit;

    if (array == null || array.length < 1 || array.length < (offset + length)) {
      throw new NumberFormatException(
          "input array is null or array.length less than 1");
    }

    if (len > 0) {
      byte firstByte = array[offset];
      if (firstByte < ZERO) { // Possible leading "+" or "-"
        if (firstByte == NEGATIVE) {
          negative = true;
          limit = Integer.MIN_VALUE;
        } else if (firstByte != NOT_NEGATIVE)
          throw new NumberFormatException("input array can not be parse to int");

        if (len == 1) // Cannot have lone "+" or "-"
          throw new NumberFormatException("input array can not be parse to int");
        i++;
      }
      multmin = limit / 10;
      int l_l = offset + len;
      while (i < l_l) {
        // Accumulating negatively avoids surprises near MAX_VALUE
        digit = Character.digit(array[i++], 10);
        if (digit < 0) {
          throw new NumberFormatException("input array can not be parse to int");
        }
        if (result < multmin) {
          throw new NumberFormatException("input array can not be parse to int");
        }
        result *= 10;
        if (result < limit + digit) {
          throw new NumberFormatException("input array can not be parse to int");
        }
        result -= digit;
      }
    } else {
      throw new NumberFormatException("input array can not be parse to int");
    }
    return negative ? result : -result;
  }

  public static boolean equalsIgnoreCase(final byte[] left, int l_offset,
      int l_length, final byte[] right, int r_offset, int r_length) {
    if (left == right)
      return true;
    if (left == null || right == null)
      return false;
    if (l_length != r_length)
      return false;
    if (l_length == 0 || left.length == 0)
      return true;
    if ((left[(l_offset + l_length) - 1] ^ right[(r_offset + r_length) - 1]) != 0x00
        && (left[(l_offset + l_length) - 1] ^ (right[(r_offset + r_length) - 1] ^ (2 << 4))) != 0x00) {
      return false;
    }
    return compareToIgnoreCase(left, l_offset, l_length, right, r_offset,
        r_length) == 0;
  }

  private static int compareToIgnoreCase(byte[] buffer1, int offset1,
      int length1, byte[] buffer2, int offset2, int length2) {
    // Short circuit equal case
    if (buffer1 == buffer2 && offset1 == offset2 && length1 == length2) {
      return 0;
    }
    // Bring WritableComparator code local
    int end1 = offset1 + length1;
    int end2 = offset2 + length2;
    for (int i = offset1, j = offset2; i < end1 && j < end2; i++, j++) {
      int a = (buffer1[i] & 0xff);
      int b = (buffer2[j] & 0xff);
      if (a != b) {
        if ((a ^ (b ^ (2 << 4))) != 0x00) {
          return a - b;
        }
      }
    }
    return length1 - length2;
  }

}
