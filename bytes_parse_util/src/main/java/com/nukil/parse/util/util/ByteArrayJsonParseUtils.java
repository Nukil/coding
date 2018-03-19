package com.nukil.parse.util.util;

import org.apache.log4j.Logger;

import java.nio.charset.Charset;
import java.util.ArrayList;

/**
 * Created by hongs on 15-11-10.
 * 字节数据转化为json的工具类
 */
public class ByteArrayJsonParseUtils {

    private static final Logger LOG = Logger.getLogger(ByteArrayJsonParseUtils.class);

    public static final Charset charset = Charset.forName("UTF-8");

    public static final byte NEW_LINE = '\n';
    public static final byte RETURN = '\r';
    public static final byte SPACE = ' ';
    public static final byte TAB = '\t';
    public static final byte BELL = '\b';
    public static final byte FORM_FEED = '\f';//换页
    public static final byte OPEN_CURLY = '{';
    public static final byte OPEN_BRACKET = '[';
    public static final byte COLON = ':';
    public static final byte COMMA = ',';
    public static final byte CLOSED_CURLY = '}';
    public static final byte CLOSED_BRACKET = ']';
    public static final byte DOUBLE_QUOTE = '"';
    public static final byte ESCAPE = '\\';

    /**
     * 通过指定的关键字解析json字节数组
     *
     * @param source 源json字节数组
     * @param key    关键字字节数据
     * @param begin  解析开始位置
     * @param end    解析结束位置
     * @return
     */
    public ArrayList<int[]> parseJsonBytesByKey(byte[] source, byte[] key, int begin, int end) {

        if (source == null) {
            return null;
        }
        if (begin >= end || end > source.length) {
            return null;
        }
        ArrayList<int[]> list = new ArrayList<int[]>();
        byte currentByte;
        done:
        for (; begin < end; begin++) {
            currentByte = source[begin];
            switch (currentByte) {
                case NEW_LINE:
                    break;
                case RETURN:
                case SPACE:
                case TAB:
                case BELL:
                case FORM_FEED:
                    break;
                case OPEN_CURLY:
                    findJsonObjectIndex(source, key, begin, end, list);
                    break done;
                case OPEN_BRACKET:
                    findJsonArrayIndex(source, key, begin, end, list);
                    break done;
            }
        }
        return list;
    }

    /**
     * 查找json数组
     * @param source
     * @param key
     * @param begin
     * @param end
     * @param results
     */
    private final void findJsonArrayIndex(byte[] source, byte[] key, int begin, int end, ArrayList<int[]> results) {
        try {
            if (source[begin] == OPEN_BRACKET) {
                begin++;
            }
            //先移出空白部分
            if (begin < end && source[begin] <= 32) {
                begin = skipWhiteSpaceFast(source, begin, end);
            }

            // the list might be empty
            if (source[begin] == CLOSED_BRACKET) {
                begin++;
                return;
            }

            loop:
            while (begin < end) {
                int endIndex = findJsonObjectIndex(source, key, begin, end, results);
                if (endIndex > begin) {
                    begin = endIndex - 1;
                }
                while (true && begin < end) {
                    if (source[begin] == COMMA) {
                        begin++;
                        continue loop;
                    } else if (source[begin] == CLOSED_BRACKET) {
                        begin++;
                        break loop;
                    } else if (source[begin] <= 32) {
                        begin++;
                        continue;
                    } else {
                        break;
                    }
                }

                if (begin < end) {
                    if (source[begin] == COMMA) {
                        begin++;
                        continue;
                    } else if (source[begin] == CLOSED_BRACKET) {
                        begin++;
                        break;
                    } else {
                        begin++;
                    }
                }
            }
        } catch (Exception ex) {
            LOG.warn("begin = " + begin + " , end = " + end);
            LOG.warn("issue parsing JSON array" + ex.getMessage());
            throw new RuntimeException("issue parsing JSON array", ex);
        }
    }

    /**
     * 查找json对象
     * @return
     */
    private final int findJsonObjectIndex(byte[] source, byte[] key, int begin, int end, ArrayList<int[]> results) {
        if (source[begin] == OPEN_CURLY) {
            begin++;
        }

        int beginIndex;
        int endIndex;
        int opened = 1;
        while (opened > 0 && begin < end) {
            // 先移出空白部分
            if (begin < end && source[begin] <= 32) {
                begin = skipWhiteSpaceFast(source, begin, end);
            }
            if (source[begin] == OPEN_CURLY || source[begin] == OPEN_BRACKET) {
                opened += 1;
            } else if (source[begin] == CLOSED_CURLY || source[begin] == CLOSED_BRACKET) {
                opened -= 1;
            } else if (source[begin] == DOUBLE_QUOTE) {
                if (begin < end && source[begin] == DOUBLE_QUOTE) {
                    begin++;
                }
                final int start_index = begin;
                begin = hasEscapeCharUTF8(source, begin, end);
                boolean flag = equalsIgnoreCase(key, 0, key.length, source, start_index, begin - start_index);
                if (begin < end) {
                    begin++;
                }

                // 先移出空白部分
                if (begin < end && source[begin] <= 32) {
                    begin = skipWhiteSpaceFast(source, begin, end);
                }

                if (begin < end && source[begin] != COLON) {
                    begin++;
                    continue;
                }

                begin++;
                // 先移出空白部分
                if (begin < end && source[begin] <= 32) {
                    begin = skipWhiteSpaceFast(source, begin, end);
                }

                beginIndex = begin;
                begin = decodeValueIndex(source, begin, end);
                endIndex = begin + 1;

                if (flag && opened == 1) {
                    if (source[beginIndex] == OPEN_BRACKET) {
                        beginIndex += 1;
                        while (beginIndex < endIndex && source[beginIndex] == OPEN_CURLY) {
                            // 先移出空白部分
                            if (beginIndex < end && source[beginIndex] <= 32) {
                                beginIndex = skipWhiteSpaceFast(source, beginIndex, endIndex);
                            }
                            int stopIndex = decodeValueIndex(source, beginIndex, endIndex) + 1;
                            results.add(new int[]{beginIndex, stopIndex});

                            beginIndex = stopIndex;

                            // 先移出空白部分
                            if (beginIndex < end && source[beginIndex] <= 32) {
                                beginIndex = skipWhiteSpaceFast(source, beginIndex, endIndex);
                            }

                            if (source[beginIndex] != COLON) {
                                beginIndex++;
                                continue;
                            }

                            // 先移出空白部分
                            if (beginIndex < end && source[beginIndex] <= 32) {
                                beginIndex = skipWhiteSpaceFast(source, beginIndex, endIndex);
                            }
                        }
                    } else {
                        results.add(new int[]{beginIndex, endIndex});
                    }
                    return endIndex;
                }
            }
            begin++;
        }
        return -1;
    }

    private final int decodeValueIndex(byte[] source, int begin, int end) {
        int opened = 0;
        byte currentByte;

        done:
        for (; begin < end; begin++) {
            currentByte = source[begin];
            switch (currentByte) {
                case NEW_LINE:
                case RETURN:
                case SPACE:
                case TAB:
                case BELL:
                case FORM_FEED:
                    break;
                case COMMA:
                    if (opened == 0) {
                        break done;
                    } else {
                        break;
                    }
                case OPEN_BRACKET:
                case OPEN_CURLY:
                    opened += 1;
                    break;
                case CLOSED_BRACKET:
                case CLOSED_CURLY:
                    if (opened > 0)
                        opened -= 1;
                    if (opened == 0) {
                        break done;
                    } else {
                        break;
                    }
                default:
            }
        }

        return begin;
    }

    public static int hasEscapeCharUTF8(byte[] array, int index, int end) {
        byte currentChar;
        for (; index < end; index++) {
            currentChar = array[index];
            if (currentChar >= 0) {
                if (DOUBLE_QUOTE == currentChar) {
                    return index;
                } else if (ESCAPE == currentChar) {
                    return index;
                }
            } else {
                index = skipUTF8NonCharOrLongChar(currentChar, index);
            }
        }
        return index;
    }

    private static int skipUTF8NonCharOrLongChar(final byte c, int index) {
        if ((c >> 5) == -2) {
            index++;
        } else if ((c >> 4) == -2) {
            index += 2;
        } else if ((c >> 3) == -2) {
            index += 3;
        }
        return index;
    }

    /**
     * 跳过空白字符部分
     * @param array
     * @param index
     * @param end
     * @return
     */
    public static int skipWhiteSpaceFast(byte[] array, int index, int end) {
        int c;
        for (; index < array.length; index++) {
            c = array[index];
            if (c > 32) {

                return index;
            }
        }
        return index - 1;
    }

    public static boolean equalsIgnoreCase(final byte[] left, int l_offset, int l_length, final byte[] right, int r_offset, int r_length) {
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
        return compareToIgnoreCase(left, l_offset, l_length, right, r_offset, r_length) == 0;
    }

    private static int compareToIgnoreCase(byte[] buffer1, int offset1, int length1, byte[] buffer2, int offset2, int length2) {
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
