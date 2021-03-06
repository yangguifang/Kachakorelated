package kachako.ml.crf_old;

import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.MapWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;
import kachako.ml.crf_old.MapToText.SequenceFileMapper;

public class IterativeParameterMixingDriver {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		Configuration conf = new Configuration();
		String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
		if(otherArgs.length<2)
		{
				System.err.println("Usage:hadoop <input file> <output file> 8");
				System.exit(2);
			
		}
		int maxIteration = conf.getInt("iteration.num", 20);
		
		Path input = new Path(otherArgs[0]);
	
		int iteration = 1;
		Path outputLast = new Path(otherArgs[1], "weight-"+maxIteration);
		Path modelFiles = new Path(otherArgs[1], "Model");
		while(maxIteration >= iteration){
				Path output = new Path(otherArgs[1], "weight-"+iteration);
				if(iteration>1){
					Path prevWeight = new Path(otherArgs[1],"weight-"+(iteration-1)+"/part-r-00000");
					conf.set("weight.file", prevWeight.toString());
					conf.setBoolean("itrNum", false);
				}
				runIteration(conf, input, output);

				
				iteration++;
				
			
		}
		
		mapToText(conf,outputLast,modelFiles);
		
	}
		private static void runIteration (Configuration conf, Path input, Path output) throws IOException, InterruptedException, ClassNotFoundException
		{
			@SuppressWarnings("deprecation")
			Job job = new Job(conf, "least squares:" + output);
			job.setJarByClass(IterativeParameterMixingDriver.class);
			job.setMapperClass(LeastSquaresMapper.class);
			job.setReducerClass(RegressionReducer.class);
			job.setInputFormatClass(SequenceFileInputFormat.class);
			job.setOutputFormatClass(SequenceFileOutputFormat.class);
			job.setOutputKeyClass(NullWritable.class);
			job.setOutputValueClass(MapWritable.class);
			//job.setOutputValueClass(Text.class);
			FileInputFormat.addInputPath(job, input);
			FileOutputFormat.setOutputPath(job, output);
			if(!job.waitForCompletion(true)){
					throw new InterruptedException("job failed:"+output);
					
		}
		
		}
		private static void mapToText(Configuration conf, Path input, Path output)throws IOException, InterruptedException, ClassNotFoundException{
			Job job = new Job(conf, "MapToText");

		    job.setJarByClass(MapToText.class);

		    job.setMapperClass(MapToText.SequenceFileMapper.class);
		    //job.setReducerClass(IdentityReducer.class);

		    job.setInputFormatClass(SequenceFileInputFormat.class);
		    job.setOutputFormatClass(TextOutputFormat.class);

		    job.setOutputKeyClass(Text.class);
		    job.setOutputValueClass(Text.class);
		    FileInputFormat.addInputPath(job, input);
		    FileOutputFormat.setOutputPath(job, output);

		    if(!job.waitForCompletion(true)){
				throw new InterruptedException("job failed:"+output);
						}
		}
}
