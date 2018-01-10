#!/usr/bin/perl 

my $usage = "Usage: SplitFasta.pl <inFasta> <numSeq>\n";

$inputFilename = shift or die $usage;
$numSeq = shift or die $usage;

open(INFILE,"<$inputFilename");
$outFileNum = 0;
while( $line = <INFILE> )
{
  if($line =~/^>/)
  {
    $readNum++;
    if(($readNum % $numSeq) == 1){
      if($outFileNum > 0){
      	close OUTFILE;
      	print "close $inputFilename"."_$outFileNum, with #reads: ".($readNum-1)."\n";
      }
      $readNum = 1;
      $outFileNum++;
      open(OUTFILE,">$inputFilename"."_$outFileNum");
      print "open $inputFilename"."_$outFileNum\n";
    }
  }
  
  print OUTFILE $line;
} 
close INFILE;
close OUTFILE;
print "close $inputFilename"."_$outFileNum, with #reads: ".($readNum)."\n";
