package org.archive.hadoop.jobs;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.NLineInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.archive.hadoop.mapreduce.ZipNumAllOutputFormat;
import org.archive.hadoop.pig.ZipNumPartitioner;
import org.archive.hadoop.pig.ZipNumRecordReader;

public class MergeCluster implements Tool {
	
	public static final String TOOL_NAME = "cdx-sort-merge";
	public static final String TOOL_DESCRIPTION = "map/reduce program that merges existing CDX zipnum clusters";

	Configuration conf = null;
	
	
	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		int res = ToolRunner.run(new Configuration(), new MergeCluster(), args);
		System.exit(res);
	}

	public Configuration getConf() {
		return conf;
	}

	public void setConf(Configuration conf) {
		this.conf = conf;
	}
	
	
	public static class ZipNumInputFormat extends NLineInputFormat
	{
		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
		public RecordReader createRecordReader(
				InputSplit genericSplit, TaskAttemptContext context)
				throws IOException {
			
			return new ZipNumRecordReader();
		}
	}
	
	protected int runMerge(String inputPath, String outputPath, int numOutputParts, int numLinesPerInputSplit) throws IOException, ClassNotFoundException, InterruptedException
	{		
		Job job = new Job(getConf(), "cdx-sort-merge");
		
		Configuration conf = job.getConfiguration();
		
		FileInputFormat.setInputPaths(job, inputPath);
		
		if (numLinesPerInputSplit > 0) {
			NLineInputFormat.setNumLinesPerSplit(job, numLinesPerInputSplit);
		}
		
		Path[] parsedPaths = FileInputFormat.getInputPaths(job);
		conf.set(ZipNumPartitioner.ZIPNUM_PARTITIONER_CLUSTER, parsedPaths[0].toString());
 
		FileOutputFormat.setOutputPath(job, new Path(outputPath));
 
		job.setInputFormatClass(ZipNumInputFormat.class);
		job.setOutputFormatClass(ZipNumAllOutputFormat.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		job.setPartitionerClass(ZipNumPartitioner.class);

		job.setNumReduceTasks(numOutputParts);
		
		job.setJarByClass(MergeCluster.class);
		return (job.waitForCompletion(true) ? 0 : 1);
	}

	@Override
	public int run(String[] args) throws Exception {
		return runMerge(args[0], args[1], 10, 500);
	}
}
