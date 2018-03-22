package netposa.firstincity.bloom.util;
/**
 * This is a very fast, non-cryptographic hash suitable for general hash-based
 * lookup.  See http://code.google.com/p/smhasher/wiki/MurmurHash3 for details.
 *
 * <p>MurmurHash3 is the successor to MurmurHash2. It comes in 3 variants, and
 * the 32-bit version targets low latency for hash table use.</p>
 */
public class MurmurHash3 extends Hash {
  private static MurmurHash3 _instance = new MurmurHash3();

  public static Hash getInstance() {
    return _instance;
  }

  @Override
  public int hash(byte[] bytes, int offset, int length, int initval) {
    final int c1 = 0xcc9e2d51;
    final int c2 = 0x1b873593;

    int h1 = initval;
    int roundedEnd = offset + (length & 0xfffffffc); // round down to 4 byte block

    for (int i = offset; i < roundedEnd; i += 4) {
      // little endian load order
      int k1 = (bytes[i] & 0xff) | ((bytes[i + 1] & 0xff) << 8) | ((bytes[i + 2] & 0xff) << 16)
          | (bytes[i + 3] << 24);
      k1 *= c1;
      k1 = (k1 << 15) | (k1 >>> 17); // ROTL32(k1,15);
      k1 *= c2;

      h1 ^= k1;
      h1 = (h1 << 13) | (h1 >>> 19); // ROTL32(h1,13);
      h1 = h1 * 5 + 0xe6546b64;
    }

    // tail
    int k1 = 0;

    switch (length & 0x03) {
    case 3:
      k1 = (bytes[roundedEnd + 2] & 0xff) << 16;
      // FindBugs SF_SWITCH_FALLTHROUGH
    case 2:
      k1 |= (bytes[roundedEnd + 1] & 0xff) << 8;
      // FindBugs SF_SWITCH_FALLTHROUGH
    case 1:
      k1 |= (bytes[roundedEnd] & 0xff);
      k1 *= c1;
      k1 = (k1 << 15) | (k1 >>> 17); // ROTL32(k1,15);
      k1 *= c2;
      h1 ^= k1;
    }

    // finalization
    h1 ^= length;

    // fmix(h1);
    h1 ^= h1 >>> 16;
    h1 *= 0x85ebca6b;
    h1 ^= h1 >>> 13;
    h1 *= 0xc2b2ae35;
    h1 ^= h1 >>> 16;

    return h1;
  }

}
