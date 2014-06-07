// Author: Maxwell Young
// Date: April 28, 2014


public class packet implements
java.io.Serializable
{
    
    private int type;      // 0 if an ACK, 1 if a data packet
	private int seqnum;    // sequence number
	private int length;    // number of characters carried in data field 
	private String data;   // should be 0 for ACK packets  
    
	public packet(int t, int s, int l, String d){
	    type = t;
	    seqnum = s;
	    length = l;
	    data = d;
	}
	
	public int getType(){
	    return type;
	}
	
	public int getSeqNum(){
	     return seqnum;   
	}
	
	public int getLength(){
	     return length;   
	}
	
	public String getData(){
	     return data;   
	}
	
	public void printContents(){
	     System.out.println("type: " + type + "  seqnum: " + seqnum + " length: " + length);
	     System.out.println("data: " + data);
	     System.out.println();
	}
	
} // end of class
