package org.csu.cptr5950.mapreduce;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.io.WritableComparator;

/**
 * Reverse sorts keys of type LongWritable
 *
 * @author ashrith
 */
public class ReverseComparator extends WritableComparator {
  private static final LongWritable.Comparator LONG_COMPARATOR = new LongWritable.Comparator();

  public ReverseComparator() {
    super(LongWritable.class);
  }

  @Override
  public int compare(byte[] b1, int s1, int l1, byte[] b2, int s2, int l2) {
    return (-1)* LONG_COMPARATOR.compare(b1, s1, l1, b2, s2, l2);
  }

  @Override
  public int compare(WritableComparable a, WritableComparable b) {
    if (a instanceof LongWritable && b instanceof LongWritable) {
      return (-1)*(((LongWritable) a).compareTo((LongWritable) b));
    }
    return super.compare(a, b);
  }

}
