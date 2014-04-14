package org.csu.cptr5950.mapreduce;

import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import java.io.IOException;

/**
 * Description goes here
 *
 * @author ashrith
 */
public class SortMapper extends Mapper<LongWritable, Text, LongWritable, DoubleWritable> {

  private LongWritable count = new LongWritable();
  private DoubleWritable temp = new DoubleWritable();

  @Override
  public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
    String line = value.toString();
    String[] parts = line.split("\\s+");
    temp.set(Double.parseDouble(parts[0]));
    count.set(Long.parseLong(parts[1]));
    context.write(count, temp);
  }
}
