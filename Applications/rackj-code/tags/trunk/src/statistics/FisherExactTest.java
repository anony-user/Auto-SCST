package statistics;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.LinkedList;
import java.util.List;

/**
 * This class is for Fisher's exact test computation. This class is based on
 * Oyvind Langsrud's Java script (permission granted), plus some necessary
 * modifications into Java, and some wrapping into a java class.
 *
 * Please contact Oyvind Langsrud if you want to use the java script.
 */

public class FisherExactTest {
  private double sn11;
  private double sn;
  private double sn1_;
  private double sn_1;
  private double sprob;
  private double sleft;
  private double sright;
  private double sless;
  private double slarg;
  private double left;
  private double right;

  public FisherExactTest(int a, int b, int c, int d) {
          try {
                  if(a < 0 || b < 0 || c < 0 || d < 0)
                          throw new Exception("Parameters of FisherExactTest constructor less than 0 found; parameters must equal or greater than 0");
                  exact22(a,b,c,d);
          } catch(Exception e) {
                  e.printStackTrace();
          }
  }

/******* The following code is from Oyvind Langsrud's Java script, plus some necessary modifications into Java *******/

  static public double lngamm(double z)
// Reference: "Lanczos, C. 'A precision approximation
// of the gamma function', J. SIAM Numer. Anal., B, 1, 86-96, 1964."
// Translation of  Alan Miller's FORTRAN-implementation
// See http://lib.stat.cmu.edu/apstat/245
  {
    double x = 0;
    x += 0.1659470187408462e-06/(z+7);
    x += 0.9934937113930748e-05/(z+6);
    x -= 0.1385710331296526    /(z+5);
    x += 12.50734324009056     /(z+4);
    x -= 176.6150291498386     /(z+3);
    x += 771.3234287757674     /(z+2);
    x -= 1259.139216722289     /(z+1);
    x += 676.5203681218835     /(z);
    x += 0.9999999999995183;
    return(Math.log(x)-5.58106146679532777-z+(z-0.5)*Math.log(z+6.5));
  }

  static public double lnfact(double n)
  {
    if(n<=1) return(0);
    return(lngamm(n+1));
  }

  static public double lnbico(double n,double k)
  {
    return(lnfact(n)-lnfact(k)-lnfact(n-k));
  }

  public double hyper_323(double n11,double n1_,double n_1,double n)
  {
    return(Math.exp(lnbico(n1_,n11)+lnbico(n-n1_,n_1-n11)-lnbico(n,n_1)));
  }

  public double hyper0(double n11i,double n1_i,double n_1i,double ni)
  {
    if(n1_i==0 && n_1i==0 && ni==0)
    {
      if(!(n11i % 10 == 0))
      {
        if(n11i==sn11+1)
        {
                sprob *= ((sn1_-sn11)/(n11i))*((sn_1-sn11)/(n11i+sn-sn1_-sn_1));
                sn11 = n11i;
                return sprob;
        }
        if(n11i==sn11-1)
        {
                sprob *= ((sn11)/(sn1_-n11i))*((sn11+sn-sn1_-sn_1)/(sn_1-n11i));
                sn11 = n11i;
                return sprob;
        }
      }
      sn11 = n11i;
    }
    else
    {
      sn11 = n11i;
      sn1_=n1_i;
      sn_1=n_1i;
      sn=ni;
    }
    sprob = hyper_323(sn11,sn1_,sn_1,sn);
    return sprob;
  }

  public double hyper(double n11)
  {
    return(hyper0(n11,0,0,0));
  }


  public double exact(double n11, double n1_,double n_1,double n)
  {
        double i,j,p,prob;
    double max=n1_;
    if(n_1<max) max=n_1;
    double min = n1_+n_1-n;
    if(min<0) min=0;
    if(min==max)
    {
      sless = 1;
      sright= 1;
      sleft = 1;
      slarg = 1;
      return 1;
    }
    prob = hyper0(n11,n1_,n_1,n);
    sleft=0;
    p = hyper(min);
    for(i=min+1; p<0.99999999*prob; i++)
    {
      sleft += p;
      p=hyper(i);
    }
    i--;
    if(p<1.00000001*prob) sleft += p;
    else i--;
    sright=0;
    p=hyper(max);
    for(j=max-1; p<0.99999999*prob; j--)
    {
      sright += p;
      p=hyper(j);
    }
    j++;
    if(p<1.00000001*prob) sright += p;
    else j++;

    // workaround
    if(new Double(prob).equals(new Double(0.0))){
	    if( (n11/n1_) < ((n_1-n11)/(n-n1_)) )
	    {
	      sless = sleft;
	      slarg = 1 - sleft + prob;
	    }
	    else
	    {
	      sless = 1 - sright + prob;
	      slarg = sright;
	    }
    }else{
	    if(Math.abs(i-n11)<Math.abs(j-n11))
	    {
	      sless = sleft;
	      slarg = 1 - sleft + prob;
	    }
	    else
	    {
	      sless = 1 - sright + prob;
	      slarg = sright;
	    }
    }
    
    if(debug){
        System.out.print("min: "+min+"\n");
        System.out.print("max: "+max+"\n");
        
        System.out.print("prob: "+prob+"\n");
    	
        System.out.print("sright: "+sright+"\n");
        System.out.print("sleft: "+sleft+"\n");
        System.out.print("sless: "+sless+"\n");
        System.out.print("slarg: "+slarg+"\n");
    }
    
    return prob;
  }

  private void exact22(double n11,double n12,double n21,double n22)
  {
    double n1_ = n11+n12;
    double n_1 = n11+n21;
    double n   = n11 +n12 +n21 +n22;
    double prob = exact(n11,n1_,n_1,n);
    left    = sless;
    right   = slarg;
    double twotail = sleft + sright;
    if(twotail>1) twotail=1;
  }

/******* The above code is from Oyvind Langsrud's Java script, plus some necessary modifications into Java *******/


  public Double getLeft(){
    return new Double(left);
  }

  public Double getRight(){
    return new Double(right);
  }

  public Double getTwoTail(){
    if(sleft+sright>1) return new Double(1);
    else return new Double(sleft+sright);
  }
  
  private static boolean debug=false;

  public static void main(String argv[]){
    if(argv.length==4){
      FisherExactTest fet = new FisherExactTest(Integer.parseInt(argv[0]),
                                                Integer.parseInt(argv[1]),
                                                Integer.parseInt(argv[2]),
                                                Integer.parseInt(argv[3]));
      System.out.println(fet.getLeft());
      System.out.println(fet.getRight());
      System.out.println(fet.getTwoTail());
    }else if(argv.length==2){
      String inFilename = argv[0];
      String outFilename = argv[1];
      try {
        FileWriter fw = new FileWriter(outFilename);
        BufferedReader fr = new BufferedReader(new FileReader(inFilename));
        String line;
        while ( (line = fr.readLine()) != null) {
          String token[] = line.split("\\s+");
          FisherExactTest fet = new FisherExactTest(Integer.parseInt(token[0]),
                                                    Integer.parseInt(token[1]),
                                                    Integer.parseInt(token[2]),
                                                    Integer.parseInt(token[3]));
          fw.write(fet.getLeft() + "\t" +
                   fet.getRight() + "\t" +
                   fet.getTwoTail() + "\n");
          fw.flush();
        }
        fw.close();
        fr.close();
      }
      catch (IOException ex) {
        ex.printStackTrace();
        System.exit(1);
      }
    }else if(argv.length==0 || (argv.length==1 && argv[0].equals("-debug"))){
    	
    	if(argv.length==1 && argv[0].equals("-debug")){
    		debug = true;
    	}
    	
        try {
          BufferedReader fr = new BufferedReader(new InputStreamReader(System.in));
          BufferedWriter fw = new BufferedWriter(new OutputStreamWriter(System.out));
          String line;
          List<Integer> numList = new LinkedList<Integer>();
          while ( (line = fr.readLine()) != null) {
        	line = line.trim();
            String token[] = line.split("\\s+");
            
            for(int i=0 ; i<token.length ; i++){
            	try {
            		Integer value = Integer.parseInt(token[i]);
            		numList.add(value);
            	}
            	catch(NumberFormatException ex){
            		// do nothing
            	}
            }

            while(numList.size()>=4){
	            FisherExactTest fet = new FisherExactTest(numList.remove(0),
	            		numList.remove(0),
	            		numList.remove(0),
	            		numList.remove(0));
	            fw.write(fet.getLeft() + "\t" +
	                     fet.getRight() + "\t" +
	                     fet.getTwoTail() + "\n");
	            fw.flush();
            }
          }
          fw.close();
          fr.close();
        }
        catch (IOException ex) {
          ex.printStackTrace();
          System.exit(1);
        }
      }
  }
}
