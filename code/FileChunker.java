import java.io.*;
import java.util.Arrays;
import java.util.ArrayList;

public class FileChunker {

    private int _currentChunk;
    private final int MAX_CHUNK_SIZE;
    private char[] _fileData;
    private int _remainingCharacters;

    public FileChunker(String filename, int maxChunkSize) {
        MAX_CHUNK_SIZE = maxChunkSize;

        _fileData = _readFile(filename);
        _remainingCharacters = _fileData.length;
    }

    private char[] _readFile(String filename) {
        ArrayList<Character> buffer = new ArrayList<Character>();
        
        // Check validity of filename
        if( filename == null || filename.length() == 0 ) {
            return null;
        }

        // setup reader
        FileInputStream fin = null;
        InputStreamReader inStream = null;
        BufferedReader reader = null;
        
        try {
            fin = new FileInputStream(filename);
        } catch(FileNotFoundException fnfe) {
            System.out.println(fnfe);
        }

        if( fin == null ) {
            return null;
        }
        inStream = new InputStreamReader(fin);
        reader = new BufferedReader(inStream);
        
        // read the file
        try {
            int c;

            while((c = reader.read()) != -1) {
                buffer.add(new Character((char)c));
            }
            
            fin.close();

        } catch(IOException ioe) {
            System.out.println(ioe); // Lazy Error Handling
        }

        // Convert to simple byte array
        char[] fileContents = new char[buffer.size()];
        for(int i = 0; i < buffer.size(); i++ ) {
            fileContents[i] = buffer.get(i).charValue();
        }

        return fileContents;
    }

    public boolean hasMoreChunks() {
        return _remainingCharacters > 0;
    }

    public String getNextChunk() {
        int startIndex = (_currentChunk * MAX_CHUNK_SIZE);
        int endIndex = (_currentChunk+1) * MAX_CHUNK_SIZE;

        char[] chunkOfData = Arrays.copyOfRange(_fileData, startIndex, endIndex);

        _remainingCharacters -= chunkOfData.length;

        return new String(chunkOfData);       
    }
}