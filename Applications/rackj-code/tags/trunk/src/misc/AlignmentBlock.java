package misc;

public class AlignmentBlock implements Comparable {
  public String strand;
  public Interval readBlock;
  public GenomeInterval refBlock;
  public String chrOriginal=null;

  public AlignmentBlock(String strand,Interval readBlock,GenomeInterval refBlock,String chrOriginal){
    this(strand,readBlock,refBlock);
    this.chrOriginal = chrOriginal;
  }

  public AlignmentBlock(String strand,Interval readBlock,GenomeInterval refBlock) {
    this.strand = strand.intern();
    this.readBlock = readBlock;
    this.refBlock = refBlock;
  }

  public String getStrand(){
      return strand;
  }

  /**
   * Return an Interval that is coordinated according read direction
   * @param readLength read length
   * @return Interval
   */
  public Interval getCanonicalReadBlock(int readLength){
    if(strand.equals("+")){
      return readBlock;
    }else{
      return new Interval(readLength-readBlock.getStart()+1,readLength-readBlock.getStop()+1,true);
    }
  }

  public int compareTo(Object object) {
      AlignmentBlock otherBlock;

      if(object==null){
        throw(new NullPointerException());
      }
      otherBlock = (AlignmentBlock) object;

      if (this.refBlock.equals(otherBlock.refBlock) == false ) {
        return this.refBlock.compareTo(otherBlock.refBlock);
      }
      else if(this.readBlock.equals(otherBlock.readBlock) == false) {
        return this.readBlock.compareTo(otherBlock.readBlock);
      }
      else if (this.getStrand().equals(otherBlock.getStrand()) == false ) {
          if (otherBlock.getStrand().equals("-")) {
              return 1;
          }else{
              return -1;
          }
      }
      else{
          return 0;
      }

  }

  public int hashCode() {
      int hashcode;
      hashcode=this.refBlock.hashCode()+this.readBlock.hashCode();
      if (this.strand.equals("-")) {
        hashcode *= -1;
      }
      return hashcode;
  }

  public boolean equals(Object obj){
      AlignmentBlock otherAb;

      if(obj==null){
        throw(new NullPointerException());
      }

      if(obj instanceof AlignmentBlock){
        otherAb = (AlignmentBlock) obj;
        if (otherAb.refBlock.equals(this.refBlock) && otherAb.readBlock.equals(this.readBlock) && otherAb.strand.equals(this.strand)){
          return true;
        }else{
          return false;
        }
      }
      return false;
    }
}
