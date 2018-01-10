package misc;

/**
 * To store necessary information of a read.
 * @author wdlin
 * @version 0.5
 */
public class ReadInfo {
	private String readID = null;
	private int readLength = 0;
	private int readSn = 0;
	
	public ReadInfo(String readID, int readLength){
		this.readID = readID;
		this.readLength = readLength;
	}
	
	public ReadInfo(String readID, int readLength, int readSn){
		this.readID = readID;
		this.readLength = readLength;
		this.readSn = readSn;
	}
	
	public String getReadID(){
		return readID;
	}
	
	public int getReadLength(){
		return readLength;
	}
	
	public int getReadSn(){
		return readSn;
	}
	
	public int hashCode(){
		int result = 0;

		result += readID.hashCode();
		result += readLength;
		result += readSn;
		
		return result;
	}

	public boolean equals(Object obj){
		if(obj==null){
			throw(new NullPointerException());
		}

		if (obj instanceof ReadInfo) {
			ReadInfo other = (ReadInfo) obj;
			if (readID.equals(other.readID) &&
					readLength == other.readLength &&
					readSn == other.readSn) {
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}
	
	static public boolean idPaired(String readA, String readB){
		int tailTrimming;

		// detect tailing identical part
		for (tailTrimming = 1; tailTrimming < readA.length() && tailTrimming < readB.length(); tailTrimming++) {
			char tailA = readA.charAt(readA.length()-tailTrimming);
			char tailB = readB.charAt(readB.length()-tailTrimming);
			if(tailA!=tailB){
				break;
			}
		}
		
		return idPaired(readA,readB,tailTrimming);
	}

	static public boolean idPaired(String readA, String readB, int tailTrimming){
		
        String strA = readA.substring(0,readA.length()-tailTrimming);
        String strB = readB.substring(0,readB.length()-tailTrimming);

        if(strA.equals(strB)){
        	return true;
        }else{
        	return false;
        }
	}
	
	static public boolean idPaired(String readA, String readB, char trimmingChar){
		
        String strA = readA.substring(0,readA.lastIndexOf((int)trimmingChar));
        String strB = readB.substring(0,readB.lastIndexOf((int)trimmingChar));

        if(strA.equals(strB)){
        	return true;
        }else{
        	return false;
        }
	}

	static IdPairedChecker getIdPairedChecker(final Object chkParameter){
		
		if(chkParameter == null){
			return new IdPairedChecker(){
				public boolean idPaired(String readA, String readB) {
					return false;
				}
			};
		}else if(chkParameter instanceof Integer){
			return new IdPairedChecker(){
				public boolean idPaired(String readA, String readB) {
					return ReadInfo.idPaired(readA,readB,((Integer)chkParameter).intValue());
				}
			};
		}else if(chkParameter instanceof String){
			if(((String)chkParameter).length()==1){
				return new IdPairedChecker(){
					public boolean idPaired(String readA, String readB) {
						return ReadInfo.idPaired(readA,readB,((String)chkParameter).charAt(0));
					}
				};
			}else if(((String)chkParameter).equals("SAM")){
				return new IdPairedChecker(){
					public boolean idPaired(String readA, String readB) {
						return readA.equals(readB);
					}
				};
			}
		}
		
		return null;
	}
}

interface IdPairedChecker {
	
	public boolean idPaired(String readA,String readB);
}
