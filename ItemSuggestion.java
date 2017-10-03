import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class ItemSuggestion {

    public static class ItemMapper
            extends Mapper<LongWritable, Text, Text, Text>{

        private Text word = new Text();
        private Text values = new Text();

        public void map(LongWritable key, Text value, Context context
        ) throws IOException, InterruptedException {
            String line = value.toString();
            String[] stringList = line.split(" ");
            for (String str : stringList){
                word.set(str);
                String suggestion = new String();
                for (String s : stringList) {
                    if (s != str) {
                        suggestion = suggestion + " " + s;
                    }
                }
                values.set(suggestion);
                context.write(word, values);
            }

        }
    }
    public static class ItemReducer
            extends Reducer<Text,Text,Text,Text> {
        private Text result = new Text();

        public void reduce(Text key, Iterable<Text> values,
                           Context context
        ) throws IOException, InterruptedException {
            String finalList = new String();
            for (Text val : values) {
                String[] valueList = (val.toString()).split(" ");
                for (String val2 : valueList){
                    finalList = finalList + " " + val2;
                }
            }
            List<String> stringList = new ArrayList<String>(Arrays.asList(finalList.split(" ")));
            Map<String, Integer> mapOfItemOccurency = new HashMap<String , Integer>();
            for (String item : stringList) {
                if (mapOfItemOccurency.containsKey(item)) {
                    Integer entryValue = mapOfItemOccurency.get(item);
                    mapOfItemOccurency.put(item, (entryValue + 1));
                }
                else {
                    mapOfItemOccurency.put(item, 1);
                }
            }
            List<Integer> listOfValues = new ArrayList<Integer>();
            List<String> listOfKeys = new ArrayList<String>();
            for (Map.Entry<String, Integer> entry : mapOfItemOccurency.entrySet()){
                listOfKeys.add(entry.getKey());
                listOfValues.add(entry.getValue());
            }
            for (int i=0; i < (listOfKeys.size()-1); i++) {
                for (int j=i+1; j < (listOfValues.size()); j++){
                    if (listOfValues.get(i) < listOfValues.get(j)){
                        int temp = listOfValues.get(i);
                        listOfValues.set(i,listOfValues.get(j));
                        listOfValues.set(j, temp);
                        String tempString = listOfKeys.get(i);
                        listOfKeys.set(i,listOfKeys.get(j));
                        listOfKeys.set(j, tempString);
                    }
                }

            }
            String suggestions = new String();
            for (String word : listOfKeys){
                suggestions = suggestions + " " + word;
            }
            result.set(suggestions);
            context.write(key, result);
        }
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf, "item suggestion");
        job.setJarByClass(ItemSuggestion.class);
        job.setMapperClass(ItemMapper.class);
        job.setCombinerClass(ItemReducer.class);
        job.setReducerClass(ItemReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);
        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}
