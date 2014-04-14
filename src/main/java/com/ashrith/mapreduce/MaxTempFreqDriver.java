package org.csu.cptr5950.mapreduce;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import java.util.Arrays;

/**
 * Counts the frequency of max temperature and sorts from most frequent items to least frequent
 *
 * @author ashrith
 */
public class MaxTempFreqDriver extends Configured implements Tool {

  // Custom counter to keep track of malformed and missing records
  enum Temperature {
    MISSING,
    MALFORMED
  }

  public int run(String[] args) throws Exception {
    if (args.length < 3) {
      System.out.println("MaxTempFreqDriver <inDir> <countsOutDir> <sortOutDir>");
      ToolRunner.printGenericCommandUsage(System.out);
      System.out.println("");
      return -1;
    }
    System.out.println(Arrays.toString(args));

    Job job = new Job(getConf(), "Max Temp Frequency Counter");
    job.setJarByClass(MaxTempFreqDriver.class);
    job.setMapperClass(MaxTempMapper.class);
    job.setReducerClass(MaxTempReducer.class);
    job.setMapOutputKeyClass(DoubleWritable.class);
    job.setMapOutputValueClass(IntWritable.class);
    job.setOutputKeyClass(DoubleWritable.class);
    job.setOutputValueClass(LongWritable.class);
    FileInputFormat.addInputPath(job, new Path(args[0]));
    FileOutputFormat.setOutputPath(job, new Path(args[1]));
    job.waitForCompletion(true);

    Job job2 = new Job(getConf(), "Sort Frequencies");
    job2.setJarByClass(MaxTempFreqDriver.class);
    FileInputFormat.addInputPath(job2, new Path(args[1]));
    FileOutputFormat.setOutputPath(job2, new Path(args[2]));
    job2.setMapperClass(SortMapper.class);
    job2.setSortComparatorClass(ReverseComparator.class); // reverse sort to get most frequent to least frequent items
    job2.setReducerClass(Reducer.class);
    job2.setOutputKeyClass(LongWritable.class);
    job2.setOutputValueClass(DoubleWritable.class);
    job2.setNumReduceTasks(1);
    job2.waitForCompletion(true);

    return 0;
  }

  public static void main(String[] args) throws Exception {
    int res = ToolRunner.run(new Configuration(), new MaxTempFreqDriver(), args);
    System.exit(res);
  }
}
