import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Relation {

    public static class RelMapper extends Mapper<Object, Text, Text, Text> {
        @Override
        protected void map(Object key, Text value, Context context) throws IOException, InterruptedException {
            String line = value.toString().trim();
            //跳过表头
            if(line.startsWith("child")) return;
            String[] parts = line.split("\\s+");
            if(parts.length!=2) return;
            String child = parts[0];
            String parent = parts[1];
            //标记A：key=child  value=P:+parent  表示child的父辈是parent
            context.write(new Text(child), new Text("P:"+parent));
            //标记B：key=parent value=C:+child  表示parent的孩子是child
            context.write(new Text(parent), new Text("C:"+child));
        }
    }

    public static class RelReducer extends Reducer<Text,Text,Text,Text> {
        @Override
        protected void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
            List<String> parents = new ArrayList<>();
            List<String> children = new ArrayList<>();
            for(Text v : values){
                String s = v.toString();
                if(s.startsWith("P:")){
                    parents.add(s.substring(2));
                }else if(s.startsWith("C:")){
                    children.add(s.substring(2));
                }
            }
            //组合：孩子(children) 和祖辈(parents)，输出祖孙关系
            for(String grandChild : children){
                for(String grandParent : parents){
                    context.write(new Text(grandChild), new Text(grandParent));
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf,"relation");
        job.setJarByClass(Relation.class);

        job.setMapperClass(RelMapper.class);
        job.setReducerClass(RelReducer.class);

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        FileInputFormat.addInputPath(job,new Path(args[0]));
        FileOutputFormat.setOutputPath(job,new Path(args[1]));

        System.exit(job.waitForCompletion(true)?0:1);
    }
}

