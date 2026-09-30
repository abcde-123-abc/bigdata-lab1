import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.*;
import java.io.*;
import java.util.Scanner;

public class HdfsOperate {
    private static FileSystem fs;
    static {
        try {
            Configuration conf = new Configuration();
            fs = FileSystem.get(conf);
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    //1 上传，存在则选择覆盖/追加
    public static void uploadWithChoice(Path local, Path dst)throws IOException{
        if(fs.exists(dst)){
            Scanner sc = new Scanner(System.in);
            System.out.println("文件已存在：1覆盖  2追加末尾");
            int opt = sc.nextInt();
            if(opt ==1){
                fs.copyFromLocalFile(false,true,local,dst);
                System.out.println("已覆盖上传");
            }else if(opt ==2){
                FSDataOutputStream out = fs.append(dst);
                BufferedReader br = new BufferedReader(new FileReader(local.toString()));
                String line;
                while((line=br.readLine())!=null){
                    out.write((line+"\n").getBytes());
                }
                br.close();
                out.close();
                System.out.println("已追加到末尾");
            }
        }else{
            fs.copyFromLocalFile(local,dst);
            System.out.println("上传成功");
        }
    }

    //2 下载，同名自动重命名
    public static void downloadAutoRename(Path hdfsSrc, Path localDst)throws IOException{
        File f = new File(localDst.toString());
        String base = localDst.getParent()+"/"+localDst.getName().split("\\.")[0];
        String suf = "";
        if(localDst.getName().contains(".")) suf="."+localDst.getName().split("\\.")[1];
        int num=1;
        while(f.exists()){
            f=new File(base+"_"+num+suf);
            num++;
        }
        Path realLocal = new Path(f.getAbsolutePath());
        fs.copyToLocalFile(false,hdfsSrc,realLocal);
        System.out.println("下载至："+realLocal);
    }

    //3 输出文件内容到控制台
    public static void catHdfsFile(Path path)throws IOException{
        FSDataInputStream in = fs.open(path);
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        String line;
        while((line=br.readLine())!=null){
            System.out.println(line);
        }
        br.close();
        in.close();
    }

    //4 打印文件元信息
    public static void printFileStatus(Path path)throws IOException{
        FileStatus st = fs.getFileStatus(path);
        System.out.println("路径:"+st.getPath());
        System.out.println("权限:"+st.getPermission());
        System.out.println("大小:"+st.getLen()+" bytes");
        System.out.println("修改时间:"+st.getModificationTime());
        System.out.println("所有者:"+st.getOwner());
    }

    //5 递归打印目录元信息
    public static void listRecursive(Path path)throws IOException{
        FileStatus[] arr = fs.listStatus(path);
        for(FileStatus s:arr){
            printFileStatus(s.getPath());
            if(s.isDirectory()){
                listRecursive(s.getPath());
            }
        }
    }

    //6 创建删除文件，父目录自动创建
    public static void createAndDelFile(Path filePath)throws IOException{
        Path parent = filePath.getParent();
        if(!fs.exists(parent)){
            fs.mkdirs(parent);
            System.out.println("自动创建父目录");
        }
        FSDataOutputStream out = fs.create(filePath);
        out.close();
        System.out.println("文件已创建");
        fs.delete(filePath,false);
        System.out.println("文件已删除");
    }

    //7 创建删除目录：仅空目录允许删除
    public static void createDelDir(Path dir)throws IOException{
        fs.mkdirs(dir);
        System.out.println("目录创建完成");
        FileStatus[] arr = fs.listStatus(dir);
        if(arr.length ==0){
            fs.delete(dir,false);
            System.out.println("空目录，已删除");
        }else{
            System.out.println("目录非空，不执行删除");
        }
    }

    //8 追加开头/末尾
    public static void appendSelect(Path hdfsFile,String addText,int mode)throws IOException{
        if(mode ==2){
            FSDataOutputStream out = fs.append(hdfsFile);
            out.write((addText+"\n").getBytes());
            out.close();
            System.out.println("追加到末尾完成");
        }else{
            //追加开头：读全部，重写
            FSDataInputStream in = fs.open(hdfsFile);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int len;
            while((len=in.read(buf))!=-1) baos.write(buf,0,len);
            in.close();
            Path tmp = new Path(hdfsFile.toString()+".tmp");
            FSDataOutputStream out = fs.create(tmp);
            out.write((addText+"\n").getBytes());
            out.write(baos.toByteArray());
            out.close();
            fs.delete(hdfsFile,false);
            fs.rename(tmp,hdfsFile);
            System.out.println("追加到文件开头完成");
        }
    }

    //9 删除文件
    public static void deleteFile(Path p)throws IOException{
        fs.delete(p,false);
        System.out.println("文件已删除");
    }

    //10 HDFS移动
    public static void moveHdfs(Path src,Path dst)throws IOException{
        fs.rename(src,dst);
        System.out.println("移动完成");
    }

    public static void main(String[] args) throws IOException {
        Path testHdfs = new Path("/user/hadoop/hdfs_api_test/local_test.txt");
        Path local = new Path(System.getProperty("user.home")+"/local_test.txt");

        System.out.println("====功能1====");
        uploadWithChoice(local,new Path("/user/hadoop/hdfs_api_test/upload.txt"));

        System.out.println("\n====功能2====");
        downloadAutoRename(testHdfs,new Path(System.getProperty("user.home")+"/local_test.txt"));

        System.out.println("\n====功能3====");
        catHdfsFile(testHdfs);

        System.out.println("\n====功能4====");
        printFileStatus(testHdfs);

        System.out.println("\n====功能5====");
        listRecursive(new Path("/user/hadoop/hdfs_api_test"));

        System.out.println("\n====功能6====");
        createAndDelFile(new Path("/user/hadoop/hdfs_api_test/aa/bb/cc.txt"));

        System.out.println("\n====功能7 空目录测试====");
        createDelDir(new Path("/user/hadoop/hdfs_api_test/empty_dir"));

        System.out.println("\n====功能8 追加末尾====");
        appendSelect(testHdfs,"test append content",2);

        System.out.println("\n====功能10 移动演示====");
        moveHdfs(new Path("/user/hadoop/hdfs_api_test/upload.txt"),new Path("/user/hadoop/hdfs_api_test/upload_mv.txt"));

        fs.close();
    }
}

