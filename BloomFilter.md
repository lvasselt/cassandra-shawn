一种技术的产生总是有其原因的，那么当要处理什么问题的时候使用bloom filter呢？

Q: 判断一个元素是否在一个集合中， 你如何做呢？

A: http://www.google.com.hk/ggblog/googlechinablog/2007/07/bloom-filter_7469.html

在日常生活中，包括在设计计算机软件时，我们经常要判断一个元素是否在一个集合中。比如在字处理软件中，需要检查一个英语单词是否拼写正确（也就是要判断它是否在已知的字典中）；在 FBI，一个嫌疑人的名字是否已经在嫌疑名单上；在网络爬虫里，一个网址是否被访问过等等。最直接的方法就是将集合中全部的元素存在计算机中，遇到一个新元素时，将它和集合中的元素直接比较即可。一般来讲，计算机中的集合是用哈希表（hash table）来存储的。它的好处是快速准确，缺点是费存储空间。当集合比较小时，这个问题不显著，但是当集合巨大时，哈希表存储效率低的问题就显现出来了。比如说，一个象 Yahoo,Hotmail 和 Gmai 那样的公众电子邮件（email）提供商，总是需要过滤来自发送垃圾邮件的人（spamer）的垃圾邮件。一个办法就是记录下那些发垃圾邮件的 email 地址。由于那些发送者不停地在注册新的地址，全世界少说也有几十亿个发垃圾邮件的地址，将他们都存起来则需要大量的网络服务器。如果用哈希表，每存储一亿个 email 地址， 就需要 1.6GB 的内存（用哈希表实现的具体办法是将每一个 email 地址对应成一个八字节的信息指纹 googlechinablog.com/2006/08/blog-post.html ，然后将这些信息指纹存入哈希表，由于哈希表的存储效率一般只有 50%，因此一个 email 地址需要占用十六个字节。一亿个地址大约要 1.6GB， 即十六亿字节的内存）。因此存贮几十亿个邮件地址可能需要上百 GB 的内存。除非是超级计算机，一般服务器是无法存储的。

今天，我们介绍一种称作布隆过滤器的数学工具，它只需要哈希表 1/8 到 1/4 的大小就能解决同样的问题。

布隆过滤器是由巴顿.布隆于一九七零年提出的。它实际上是一个很长的二进制向量和一系列随机映射函数。我们通过上面的例子来说明起工作原理。

假定我们存储一亿个电子邮件地址，我们先建立一个十六亿二进制（比特），即两亿字节的向量，然后将这十六亿个二进制全部设置为零。对于每一个电子邮件地址 X，我们用八个不同的随机数产生器（F1,F2, ...,F8） 产生八个信息指纹（f1, f2, ..., f8）。再用一个随机数产生器 G 把这八个信息指纹映射到 1 到十六亿中的八个自然数 g1, g2, ...,g8。现在我们把这八个位置的二进制全部设置为一。当我们对这一亿个 email 地址都进行这样的处理后。一个针对这些 email 地址的布隆过滤器就建成了。（见下图）
![http://cassandra-shawn.googlecode.com/files/bloomfilter.jpg](http://cassandra-shawn.googlecode.com/files/bloomfilter.jpg)
现在，让我们看看如何用布隆过滤器来检测一个可疑的电子邮件地址 Y 是否在黑名单中。我们用相同的八个随机数产生器（F1, F2, ..., F8）对这个地址产生八个信息指纹 s1,s2,...,s8，然后将这八个指纹对应到布隆过滤器的八个二进制位，分别是 t1,t2,...,t8。如果 Y 在黑名单中，显然，t1,t2,..,t8 对应的八个二进制一定是一。这样在遇到任何在黑名单中的电子邮件地址，我们都能准确地发现。

布隆过滤器决不会漏掉任何一个在黑名单中的可疑地址。但是，它有一条不足之处。也就是它有极小的可能将一个不在黑名单中的电子邮件地址判定为在黑名单中，因为有可能某个好的邮件地址正巧对应个八个都被设置成一的二进制位。好在这种可能性很小。我们把它称为误识概率。在上面的例子中，误识概率在万分之一以下。

布隆过滤器的好处在于快速，省空间。但是有一定的误识别率。常见的补救办法是在建立一个小的白名单，存储那些可能别误判的邮件地址。

也可以看一下：

http://www.hellodba.net/2009/04/bloom_filter.html

http://pages.cs.wisc.edu/~cao/papers/summary-cache/node8.html


---


**In Cassandra, the bloom filter was implemented in the following four java files which packages is "org.apache.cassandra.utils".**


  * BitSetSerializer.java
```
主要提供了serialize/deserialize两个方法
```

  * BloomCalculations.java
```
在读这个文件的源代码之前先要阅读下面说明中的paper，那里面主要是根据一个表选择bloom filter相关的两个参数，一个是每一个element有多少个bucket，另外一个就是要使用多少个hash函数。
/**
 * The following calculations are taken from:
 * http://www.cs.wisc.edu/~cao/papers/summary-cache/node8.html
 * "Bloom Filters - the math"
 *
 * This class's static methods are meant to facilitate the use of the Bloom
 * Filter class by helping to choose correct values of 'bits per element' and
 * 'number of hash functions, k'.
 */

    /**
     * In the following table, the row 'i' shows false positive rates if i buckets
     * per element are used.  Column 'j' shows false positive rates if j hash
     * functions are used.  The first row is 'i=0', the first column is 'j=0'.
     * Each cell (i,j) the false positive rate determined by using i buckets per
     * element and j hash functions.
     */
    static final double[][] probs = new double[][]{
        {1.0}, // dummy row representing 0 buckets per element
        {1.0, 1.0}, // dummy row representing 1 buckets per element
        {1.0, 0.393,  0.400},
        {1.0, 0.283,  0.237,   0.253},
        {1.0, 0.221,  0.155,   0.147,   0.160},
        {1.0, 0.181,  0.109,   0.092,   0.092,   0.101}, // 5
        {1.0, 0.154,  0.0804,  0.0609,  0.0561,  0.0578,   0.0638},
        {1.0, 0.133,  0.0618,  0.0423,  0.0359,  0.0347,   0.0364},
        {1.0, 0.118,  0.0489,  0.0306,  0.024,   0.0217,   0.0216,   0.0229},
        {1.0, 0.105,  0.0397,  0.0228,  0.0166,  0.0141,   0.0133,   0.0135,   0.0145},
        {1.0, 0.0952, 0.0329,  0.0174,  0.0118,  0.00943,  0.00844,  0.00819,  0.00846}, // 10
        {1.0, 0.0869, 0.0276,  0.0136,  0.00864, 0.0065,   0.00552,  0.00513,  0.00509},
        {1.0, 0.08,   0.0236,  0.0108,  0.00646, 0.00459,  0.00371,  0.00329,  0.00314},
        {1.0, 0.074,  0.0203,  0.00875, 0.00492, 0.00332,  0.00255,  0.00217,  0.00199,  0.00194},
        {1.0, 0.0689, 0.0177,  0.00718, 0.00381, 0.00244,  0.00179,  0.00146,  0.00129,  0.00121,  0.0012},
        {1.0, 0.0645, 0.0156,  0.00596, 0.003,   0.00183,  0.00128,  0.001,    0.000852, 0.000775, 0.000744}, // 15
	{1.0, 0.0606, 0.0138,  0.005,   0.00239, 0.00139,  0.000935, 0.000702, 0.000574, 0.000505, 0.00047,  0.000459},
	{1.0, 0.0571, 0.0123,  0.00423, 0.00193, 0.00107,  0.000692, 0.000499, 0.000394, 0.000335, 0.000302, 0.000287, 0.000284},
	{1.0, 0.054,  0.0111,  0.00362, 0.00158, 0.000839, 0.000519, 0.00036,  0.000275, 0.000226, 0.000198, 0.000183, 0.000176},
	{1.0, 0.0513, 0.00998, 0.00312, 0.0013,  0.000663, 0.000394, 0.000264, 0.000194, 0.000155, 0.000132, 0.000118, 0.000111, 0.000109},
	{1.0, 0.0488, 0.00906, 0.0027,  0.00108, 0.00053,  0.000303, 0.000196, 0.00014,  0.000108, 8.89e-05, 7.77e-05, 7.12e-05, 6.79e-05, 6.71e-05} // 20
    };  // the first column is a dummy column representing K=0.

/**
 * Given the number of buckets that can be used per element, return a
 * specification that minimizes the false positive rate.
 *
 * @param bucketsPerElement The number of buckets per element for the filter.
 * @return A spec that minimizes the false positive rate.
 */

// bucketsPerElement对应probs的行()，然后在该行寻找false positive最小的列K
// 然后构造class BloomSpecification
public static BloomSpecification computeBloomSpec(int bucketsPerElement);

/**
 * Given a maximum tolerable false positive probability, compute a Bloom
 * specification which will give less than the specified false positive rate,
 * but minimize the number of buckets per element and the number of hash
 * functions used.  Because bandwidth (and therefore total bitvector size)
 * is considered more expensive than computing power, preference is given
 * to minimizing buckets per element rather than number of hash functions.
 *
 * @param maxBucketsPerElement The maximum number of buckets available for the filter.
 * @param maxFalsePosProb The maximum tolerable false positive rate.
 * @return A Bloom Specification which would result in a false positive rate
 * less than specified by the function call
 * @throws UnsupportedOperationException if a filter satisfying the parameters cannot be
 * met
 */
// 如果给定了一个最大可容忍的false positive probability(计算错误的最大可能性)，找到一个满足// 这个要求的最小的buckets/element，最少的hash function，考虑到带宽问题，在所有满足这个要求// 的组合里面选择buckets/element最小的那个。    
public static BloomSpecification computeBloomSpec(int maxBucketsPerElement, double maxFalsePosProb)
```

  * Filter.java
```
这个是一个抽象类，里面包含如下方法：
int getHashCount(); // bloom filter 使用了多少个hash 函数
public int[] getHashBuckets(byte[] key);// 计算key对应其在filter_的buckets
abstract int buckets(); // 返回filter_一共有多少个buckets
public abstract void add(byte[] key); // 添加一个key到filter_中
public abstract boolean isPresent(byte[] key); // 判断一个key是否在filter_中

// Murmur is faster than an SHA-based approach and provides as-good collision
// resistance.  The combinatorial generation approach described in
// http://www.eecs.harvard.edu/~kirsch/pubs/bbbf/esa06.pdf
// does prove to work in actual tests, and is obviously faster
// than performing further iterations of murmur.
static int[] getHashBuckets(byte[] b, int hashCount, int max)
{
    // 计算key对应其在filter_对应哪些buckets
    int[] result = new int[hashCount];
    int hash1 = hasher.hash(b, b.length, 0);
    int hash2 = hasher.hash(b, b.length, hash1);
    for (int i = 0; i < hashCount; i++)
    {
        result[i] = Math.abs((hash1 + i * hash2) % max);
    }
    return result;
}
```

  * BloomFilter.java
```
这类实现了上面的Filter，所以需要实现其没有实现的抽象方法
private BitSet filter_;


/**
 * Calculates the maximum number of buckets per element that this implementation
 * can support.  Crucially, it will lower the bucket count if necessary to meet
 * BitSet's size restrictions.
 */
private static int maxBucketsPerElement(long numElements)

// 两种方法create BloomFilter，其实就是构建其两个fields，一个是hashCount(计算bucket的时候决定一个key对应filter_t多少个buckets)，另外一个就是filter_t（位数组）
/**
 * @return A BloomFilter with the lowest practical false positive probability
 * for the given number of elements.
 */
public static BloomFilter getFilter(long numElements, int targetBucketsPerElem)

/**
 * @return The smallest BloomFilter that can provide the given false positive
 * probability rate for the given number of elements.
 *
 * Asserts that the given probability can be satisfied using this filter.
 */
public static BloomFilter getFilter(long numElements, double maxFalsePosProbability)

// If all of key's buckets in filter were set to true, then the key exists in filter_t, otherwise not
public boolean isPresent(byte[] key)
{
    for (int bucketIndex : getHashBuckets(key))
    {
        if (!filter_.get(bucketIndex))
        {
            return false;
        }
    }
    return true;
}

/*
 * @param key -- value whose hash is used to fill
 * the filter_.
 * This is a general purpose API.
 * 
 * Set the key's buckets in filter_t to true
 */
public void add(byte[] key)
{
    for (int bucketIndex : getHashBuckets(key))
    {
        filter_.set(bucketIndex);
    }
}

```