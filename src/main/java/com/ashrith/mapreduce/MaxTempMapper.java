package org.csu.cptr5950.mapreduce;

import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;

import java.io.IOException;

/**
 * Parses data set and outputs max temperature
 *
 * @author ashrith
 */
public class MaxTempMapper extends Mapper<LongWritable, Text, DoubleWritable, IntWritable> {

  private static final double MISSING = 999.9;
  private final static IntWritable one = new IntWritable(1);

  @Override
  public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
    String line = value.toString();
    String[] parts = line.split("\\s+");
    String temp = parts[17];
    double maxTemp;
    if (temp.charAt(temp.length() - 1)=='*') {
      temp = temp.substring(0, temp.length() - 1);
      maxTemp = Double.parseDouble(temp);
    } else {
      maxTemp = Double.parseDouble(temp);
    }

    if (maxTemp != MISSING && maxTemp != 0.0) {
      context.write(new DoubleWritable(maxTemp), one);
    } else {
      context.getCounter(MaxTempFreqDriver.Temperature.MISSING).increment(1);
    }
  }
}
