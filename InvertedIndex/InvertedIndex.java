import java.io.IOException;
import java.util.*;
import java.util.regex.*;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.io.*;
import org.apache.hadoop.fs.*;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.apache.hadoop.conf.Configuration;

public class InvertedIndex {
	public static void main(String[] args) throws IOException, ClassNotFoundException, InterruptedException{
		if (args.length != 2) {
			System.err.println("Usage: Inverted Index <input path> <output path>");
			System.exit(-1);
		}

		Job job = new Job();

		job.setJarByClass(InvertedIndex.class);
		job.setJobName("Inverted Index");

		FileInputFormat.addInputPath(job, new Path(args[0]));
		FileOutputFormat.setOutputPath(job, new Path(args[1]));

		job.setMapperClass(InvertedIndexMapper.class);
		job.setReducerClass(InvertedIndexReducer.class);

		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(Text.class);
		job.waitForCompletion(true);
	}
}

class InvertedIndexMapper extends Mapper<LongWritable, Text, Text, Text> {
	private Text word = new Text();

	public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException{
		String line = value.toString();
		StringTokenizer tokenizer = new StringTokenizer(line);
		Text docID = new Text(((FileSplit) context.getInputSplit()).getPath()
				.toString().replace("gs://dataproc-staging-us-west1-45037165631-p7rrglgv/DataInput/", ""));
		Pattern pattern = Pattern.compile("[\\w].*[\\w]");
		Matcher matcher;
		
		while (tokenizer.hasMoreTokens()) {
			matcher = pattern.matcher(tokenizer.nextToken());
			if (matcher.find()) {	
				word.set(matcher.group(0).toLowerCase().trim());
				context.write(word, docID);
			}
		}
	}
}

class InvertedIndexReducer extends Reducer<Text, Text, Text, Text>{
	public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
		Iterator<Text> iteration = values.iterator();
		HashMap<String, Integer> docIDnum = new HashMap<String, Integer>();
		int total = 0;
		while (iteration.hasNext()) {
			String docID = iteration.next().toString();
			if (!docIDnum.containsKey(docID)){
				docIDnum.put(docID, 1 );
				total++;
			} else {
				docIDnum.put(docID, 1 + docIDnum.get(docID));
				total++;
			}
		}

		StringBuilder conVal = new StringBuilder();
		Iterator newIteration = docIDnum.entrySet().iterator();
		conVal.append(total);

		while (newIteration.hasNext()) {
			Map.Entry keyval = (Map.Entry)newIteration.next();
			conVal.append("," + keyval.getKey() + "--" + keyval.getValue());
		}
		context.write(key, new Text(conVal.toString()));
	}
}









