package org.csu.cptr5950.mapreduce;

import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;

/**
 * Counts the occurrences of the max temp
 *
 * @author ashrith
 */
public class MaxTempReducer extends Reducer<DoubleWritable, IntWritable, DoubleWritable, LongWritable> {

  private LongWritable result = new LongWritable();

  @Override
  public void reduce(DoubleWritable key, Iterable<IntWritable> values, Context context)
      throws IOException, InterruptedException {
    long sum = 0;
    for (IntWritable val : values) {
      sum += val.get();
    }
    result.set(sum);
    context.write(key, result);
  }
}
