package rnaseq;

import misc.CanonicalGFF;

/**
 * @deprecated This subclass of CanonicalGFF will be removed soon. Replace it with CanonicalGFF.
 * @author Wen-Dar Lin
 * @version 0.5
 */
public class GeneModel extends CanonicalGFF {

    public GeneModel(String filename,CanonicalGFF cgff){
        super(filename);
    }

    public static void main(String[] args) {
        System.out.println("nothing happening");
    }
}
