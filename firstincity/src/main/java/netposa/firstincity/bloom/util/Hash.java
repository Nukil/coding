package netposa.firstincity.bloom.util;

public abstract class Hash {
  /** 无效的hash类型. */
  public static final int INVALID_HASH = -1;
  /** jenkins hash实例 {@link JenkinsHash}. */
  public static final int JENKINS_HASH = 0;
  /** MurmurHash实例 {@link MurmurHash}. */
  public static final int MURMUR_HASH  = 1;
  /** MurmurHash3的实例 {@link MurmurHash3}. */
  public static final int MURMUR_HASH3 = 2;
  
  
  
  /**
   * 这种方法将哈希函数名称的字符串表示的符号常量返回,目前支持如下三种类型:<br/>
   * "jenkins", "murmur" and "murmur3".
   * @param name hash 函数名称
   * @return 返回hash函数名称对应的HASH实例的常数
   */
  public static int parseHashType(String name) {
    if ("jenkins".equalsIgnoreCase(name)) {
      return JENKINS_HASH;
    } else if ("murmur".equalsIgnoreCase(name)) {
      return MURMUR_HASH;
    } else if ("murmur3".equalsIgnoreCase(name)) {
      return MURMUR_HASH3;
    } else {
      return INVALID_HASH;
    }
  }

  /**
   * 通过一个给定的hash函数名称,返回hash函数对应的常数
   * @param conf string
   * @return one of the predefined constants
   */
  public static int getHashType(String conf) {
    String name = "murmur";
    if (conf != null) {
      name = conf;
    }
    return parseHashType(name);
  }

  /**
   * 通过一个指定的常数,返回一个全局的hash函数实例.
   * @param type hash type
   * @return hash function instance, or null if type is invalid
   */
  public static Hash getInstance(int type) {
    switch(type) {
    case JENKINS_HASH:
      return JenkinsHash.getInstance();
    case MURMUR_HASH:
      return MurmurHash.getInstance();
    case MURMUR_HASH3:
      return MurmurHash3.getInstance();
    default:
      return null;
    }
  }

  /**
   * 通过指定的HASH函数名称,返回HASH函数的实现实例
   * @param conf hash函数的名称
   * @return defined hash type, or null if type is invalid
   */
  public static Hash getInstance(String conf) {
    int type = getHashType(conf);
    return getInstance(type);
  }

  /**
   * 把一个指定的byte array进行HASH操作,返回HASH CODE
   * @param bytes input bytes
   * @return hash value
   */
  public int hash(byte[] bytes) {
    return hash(bytes, bytes.length, -1);
  }

  /**
   * 计算一个byte array的hash值,通过一个给定的byte array与seed值,这个值最好是一个质数
   * @param bytes input bytes
   * @param initval seed value
   * @return hash value
   */
  public int hash(byte[] bytes, int initval) {
    return hash(bytes, 0, bytes.length, initval);
  }

  /**
   * 计算给定的byte array中从0到指定长度的值的hash值,此函数需要给定一个seed值,
   * @param bytes input bytes
   * @param length length of the valid bytes after offset to consider
   * @param initval seed value
   * @return hash value
   */
  public int hash(byte[] bytes, int length, int initval) {
    return hash(bytes, 0, length, initval);
  }

  /**
   * 计算一个byte array中从给定的位置到指定长度的值的hash值,此函数需要给定一个seed值,
   * @param bytes input bytes
   * @param offset the offset into the array to start consideration
   * @param length length of the valid bytes after offset to consider
   * @param initval seed value
   * @return hash value
   */
  public abstract int hash(byte[] bytes, int offset, int length, int initval);

}
