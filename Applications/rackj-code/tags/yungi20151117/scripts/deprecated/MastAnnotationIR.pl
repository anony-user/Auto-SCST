#!/usr/bin/perl -w
use Bio::DB::Fasta;

my $usage = "Usage: MastAnnotationIR.pl strandCGFF intronList w1 w2 w3 w4 overlapMerge(0/1) mastIn genomeFastaIn outFilename\n";
my $strandCGFF = shift or die $usage;
my $intronListFile = shift or die $usage;
my $w1 = $ARGV[0]; shift;
my $w2 = $ARGV[0]; shift;
my $w3 = $ARGV[0]; shift;
my $w4 = $ARGV[0]; shift;
my $overlapMerge = $ARGV[0]; shift;
my $mastIn = shift or die $usage;
my $genomeFastaIn = shift or die $usage;
my $outFilename = shift or die $usage;

my $debug = 0;

# read strandCGFF to build geneInfoHOA and cgffHOA
my %geneInfoHOA;
my %cgffHOA;
open(FILE,"<$strandCGFF");
while( $line = <FILE> ){
    my @token = split(/\t/, $line);
    if ( $line=~/^>/ ) {
        $geneID = substr $token[0],1;
        if (@token>4){
            # chr in array[0] and strand in array[1]
            push @{ $geneInfoHOA{$geneID} },trim($token[1]),trim($token[4]); 
        }else{
            die "$strandCGFF not a strand CGFF\n"
        }
    }else{
        push @{ $cgffHOA{$geneID} },trim($token[0]),trim($token[1]); 
    }
}
close(FILE);

# read introns in intronList file into intronHash
my %intronHash;
open(FILE,"<$intronListFile");
while( $line = <FILE> ) {
    my @token = split(/\s/, $line);
    $intronHash{$token[0],$token[1]}="";
}
close(FILE);

# for case insensitive seq names
my $genomeDB = Bio::DB::Fasta->new($genomeFastaIn);
my @seqIds = $genomeDB->ids;
my %seqNameMap = ();

for (my $idx = 0; $idx < @seqIds; $idx++) {
    $seqNameMap{uc($seqIds[$idx])} = $seqIds[$idx];
}

# build lookupHOA
my %lookupHOA;
for $intron ( sort byIntron keys %intronHash ) {
    # getting intron infor
    my @token = split($;, $intron);
    my $geneID = $token[0];
    my $intronNo = $token[1];
    # get 5' start and 3' stop of the intron
    my $intronStart = $cgffHOA{$geneID}[2*$intronNo-1]+1;
    my $intronStop = $cgffHOA{$geneID}[2*$intronNo]-1;
    # get chr and strand
    my $chr = $seqNameMap{uc($geneInfoHOA{$geneID}[0])};
    my $strand = $geneInfoHOA{$geneID}[1];

    # get lookupString
    # check if overlap in intron side
    my $intronSideOverlap = 0;
    
    $intronSideOverlap = 1 if $w2+$w3 > $intronStop-$intronStart+1;
    
    if (($intronSideOverlap && $overlapMerge) || ($w2==-1 && $w3==-1)) {
        if ($strand eq "+") {
            my $lookupString = "$geneID"."_"."$intronNo"."_"."forward";
            my $seq = $genomeDB->seq($chr,($intronStart-1-$w1+1) => ($intronStop+1+$w4-1));
            push(@{$lookupHOA{$lookupString}}, $geneID, $intronNo, $strand, 0, $intronStart, $intronStop, ($intronStart-1-$w1+1), ($intronStop+1+$w4-1), $seq);
        }else{
            my $lookupString = "$geneID"."_"."$intronNo"."_"."reverse";
            my $seq = $genomeDB->seq($chr,($intronStop+1+$w1-1) => ($intronStart-1-$w4+1));
            push(@{$lookupHOA{$lookupString}}, $geneID, $intronNo, $strand, 0, $intronStop, $intronStart, ($intronStop+1+$w1-1), ($intronStart-1-$w4+1), $seq);
        }
    }else{
        if ($strand eq "+") {
            if ($w1+$w2>0) {
                my $lookupString = "$geneID"."_"."$intronNo"."_"."forward"."_"."1";
                my $seq = $genomeDB->seq($chr,($intronStart-1-$w1+1) => ($intronStart+$w2-1));
                push(@{$lookupHOA{$lookupString}}, $geneID, $intronNo, $strand, 1, $intronStart, $intronStop, ($intronStart-1-$w1+1), ($intronStart+$w2-1), $seq);
            }
            if ($w3+$w4>0) {
                my $lookupString = "$geneID"."_"."$intronNo"."_"."forward"."_"."2";
                my $seq = $genomeDB->seq($chr,($intronStop-$w3+1) => ($intronStop+1+$w4-1));
                push(@{$lookupHOA{$lookupString}}, $geneID, $intronNo, $strand, 2, $intronStart, $intronStop, ($intronStop-$w3+1), ($intronStop+1+$w4-1), $seq);
            }
        }else{
            if ($w1+$w2>0){
                my $lookupString = "$geneID"."_"."$intronNo"."_"."reverse"."_"."1";
                my $seq = $genomeDB->seq($chr,($intronStop+1+$w1-1) => ($intronStop-$w2+1));
                push(@{$lookupHOA{$lookupString}}, $geneID, $intronNo, $strand, 1, $intronStop, $intronStart, ($intronStop+1+$w1-1), ($intronStop-$w2+1), $seq);
            }
            if ($w3+$w4>0){
                my $lookupString = "$geneID"."_"."$intronNo"."_"."reverse"."_"."2";
                my $seq = $genomeDB->seq($chr,($intronStart+$w3-1) => ($intronStart-1-$w4+1));
                push(@{$lookupHOA{$lookupString}}, $geneID, $intronNo, $strand, 2, $intronStop, $intronStart, ($intronStart+$w3-1), ($intronStart-1-$w4+1), $seq);
            }
        }
    }
}

# for each line in mastIn file
print "fasta read done\n" if $debug;
open(FILE,"<$mastIn");
open(OUT,">$outFilename");
#print OUT "sequence_name\tmotif\thit_start\thit_end\tscore\thit_p-value\thit_region\thitStart\thitStop\trelativePos\tintervals\n";
print OUT "sequence_name\tmotif\thit_start\thit_end\tscore\thit_p-value\thit_region\thit Start\thit Stop\tHit start to junction start\tHit stop to junction end\n";
while( $line = <FILE> ) {
    next if ($line =~ /^#/);
    my @token = split(/\s+/, $line);
    my $lookupString = $token[0];
    my $hitStart = $token[2];
    my $hitStop = $token[3];
    
    # get corresponding infor
    my ($geneID, $intronNo, $strand, $seqNo, $junctionStart, $junctionEnd, $seqStartGenome, $seqStopGenome, $seq) = @{ $lookupHOA{$lookupString} };
    #print "$geneID, $intronNo, $strand, $seqNo, $seqStartGenome, $seqStopGenome\n";
    if ($strand eq "+") {
        $intronStart = $cgffHOA{$geneID}[2*$intronNo-1]+1;
        $intronStop = $cgffHOA{$geneID}[2*$intronNo]-1;
    }else{
        $intronStop = $cgffHOA{$geneID}[2*$intronNo-1]+1;
        $intronStart = $cgffHOA{$geneID}[2*$intronNo]-1;
    }
    # position assert
    my $chr = $seqNameMap{uc($geneInfoHOA{$geneID}[0])};
    my $chrLength = $genomeDB->length($chr);
    $seqStartGenome = assertPosition($seqStartGenome,$chrLength);
    $seqStopGenome = assertPosition($seqStopGenome,$chrLength);
    
    my ($start_shift,$end_shift)=("","");
    my ($hitStartGenome,$hitStopGenome)=(-1,-1);
    # transfer hitStart & hitStop into hitStartGenome & hitStopGenome
    if ($strand eq "+") {
        $hitStartGenome = $seqStartGenome + $hitStart -1;
        $hitStopGenome = $seqStartGenome + $hitStop -1;
        $start_shift = $hitStartGenome - $junctionStart;
        $end_shift =  $hitStopGenome - $junctionEnd;
    }else{
        $hitStartGenome = $seqStartGenome - $hitStart +1;
        $hitStopGenome = $seqStartGenome - $hitStop +1;
        $start_shift = $junctionStart - $hitStartGenome;
        $end_shift = $junctionEnd - $hitStopGenome;
    }
    
    print "$lookupString, $geneID, $intronNo, $strand, $seqNo, $seqStartGenome, $seqStopGenome\n" if $debug;

    # find position of hitStart relative to the intron
    
    foreach (@token){
        print OUT trim($_)."\t";
    }
    print OUT substr($seq,$hitStart-1,$hitStop-$hitStart+1)."\t";
    print OUT "$hitStartGenome\t$hitStopGenome\t$start_shift\t$end_shift\n";
}
close(OUT);
close(FILE);

sub assertPosition {
    my $inPos = shift;
    my $max = shift;
    
    return 1 if $inPos<1;
    return $max if $inPos>$max;
    return $inPos;
}

sub byIntron {
    my @token1 = split($;, $a);
    my @token2 = split($;, $b);
    my $x_a = $token1[0];
    my $y_a = $token1[1];
    my $x_b = $token2[0];
    my $y_b = $token2[1];
    if (($x_a cmp $x_b) != 0){
        return ($x_a cmp $x_b);
    }else{
        return ($y_a cmp $y_b);
    }
}

sub trim {
    my $str=shift;
    $str =~ s/\s+$//g;
    $str =~ s/^\s+//g;
    return $str;
}
