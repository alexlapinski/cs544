import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class SimpleFileWriter {

    private FileWriter _fout;

    public SimpleFileWriter(String filename) {
        File receivedFile = new File(filename);
        
        if( receivedFile.exists() ) {
            receivedFile.delete();
        } else {
            try {
                receivedFile.createNewFile();
            } catch(IOException ioe) {
                System.out.println(ioe);
            }
        }

        try {
            _fout = new FileWriter(receivedFile);
        } catch(IOException ioe) {
            System.out.println(ioe);
        }
    }

    public void appendToFile(String str) {
        try {
            _fout.write(str, 0, str.length());   
            _fout.flush();
        } catch(IOException ioe) {
            System.out.println(ioe);
        }
    }

    public void closeFile() {
        try {
            _fout.close();
        } catch(IOException ioe) {
            System.out.println(ioe);
        }
    }
}