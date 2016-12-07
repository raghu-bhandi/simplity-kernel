import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class Util {
	public static void main(String[] args) {
		File fl = new File("D:/workspace/simplity/AFE2016/src/main/webapp/public/js/roles.json");
		File fo = new File("D:/workspace/simplity/AFE2016/src/main/webapp/public/js/roles2.json");
		try {
			BufferedReader br = new BufferedReader(new FileReader(fl));
			BufferedWriter bw = new BufferedWriter(new FileWriter(fo));
			String line;
			while((line=br.readLine())!=null){
				bw.write( line.replaceAll("\\s",""));
			}
			bw.flush();
			bw.close();
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
				

	}

	private static void RewriteFile() {
		File fl = new File("D:/workspace/simplity/AFE2016/src/main/webapp/public/js/roles.json");
		File fo = new File("D:/workspace/simplity/AFE2016/src/main/webapp/public/js/roles2.json");
		try {
			BufferedReader br = new BufferedReader(new FileReader(fl));
			BufferedWriter bw = new BufferedWriter(new FileWriter(fo));
			String line;
			while((line=br.readLine())!=null){
				bw.write("\""+line+"\",");
			}
			bw.flush();
			bw.close();
			br.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}