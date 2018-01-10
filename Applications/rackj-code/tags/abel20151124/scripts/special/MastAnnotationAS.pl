#!/usr/bin/perl -w
use Bio::DB::Fasta;

my $usage = "Usage: MastAnnotationAS.pl [-exonES] <spliceList> <w1> <w2> <w3> <w4> <overlapMerge(0/1)> <mastIn> <strandCGFF> <genomeFasta> <outFilename>\n";

my $exonES = 0;

#Retrieve parameter
my @arg_idx=(0..@ARGV-1);
for my $i (0..@ARGV-1) {
	if ($ARGV[$i] eq '-exonES') {
		$exonES = 1;
		delete $arg_idx[$i];
	}
}
my @new_arg;
for (@arg_idx) { push(@new_arg,$ARGV[$_]) if (defined($_)); }
@ARGV=@new_arg;

# rest parameters
my $ListFile = shift or die $usage;
my $w1 = $ARGV[0]; shift;
my $w2 = $ARGV[0]; shift;
my $w3 = $ARGV[0]; shift;
my $w4 = $ARGV[0]; shift;
my $overlapMerge = $ARGV[0]; shift;
my $mastIn = shift or die $usage;
my $strandCGFF = shift or die $usage;
my $genomeFastaIn = shift or die $usage;
my $outFilename = shift or die $usage;

my $debug = 0;

# read strandCGFF to build geneInfoHOA and cgffHOA
my %geneInfoHOA;
my %cgffHOA;
open(FILE,"<$strandCGFF") or die "strandCGFF file error\n";
while( $line = <FILE> ) {
    my @token = split(/\s/, $line);
    if ($line=~/^>/) {
        $geneID = substr $token[0],1;
        if (@token>4) {
            # chr in array[0] and strand in array[1]
            push(@{$geneInfoHOA{$geneID}}, trim($token[1]), trim($token[4])); 
        }else{
            die "$strandCGFF not a strand CGFF\n";
        }
    }else{
        push(@{$cgffHOA{$geneID}}, trim($token[0]), trim($token[1])); 
    }
}
close(FILE);

# for case insensitive seq names
my $genomeDB = Bio::DB::Fasta->new($genomeFastaIn);
my @seqIds = $genomeDB->ids;
my %seqNameMap;

for (my $idx = 0; $idx < @seqIds; $idx++) {
    $seqNameMap{uc($seqIds[$idx])} = $seqIds[$idx];
}

# read pattern in List file into geneSpliceHash
my %geneSpliceSet;
open(FILE,"<$ListFile") or die "List file error\n";
while(<FILE>) {
    next if /^\#/;
    my @token = split(/\s/);
    my $geneID = $token[0];
    my $pattern = $token[1];
    
    if( $exonES && ( $pattern =~ /(\d+)<=>(\d+)/ ) ){
    	my ($startExon,$stopExon) = ($1,$2);
    	for(my $skippedExon = $startExon+1; $skippedExon < $stopExon; $skippedExon++){
            $geneSpliceSet{$geneID}{">$skippedExon<"}="";
        }
    }else{
        $geneSpliceSet{$geneID}{$pattern}="";
    }
}

my %lookupHOA;
for $geneID (sort {$a cmp $b} keys %geneSpliceSet) {
    for $splice (sort {$a cmp $b} keys %{$geneSpliceSet{$geneID}}) {
        my $chr = $seqNameMap{uc($geneInfoHOA{$geneID}[0])};
        my $strand = $geneInfoHOA{$geneID}[1];
        my ($genomeStart,$genomeEnd)=(0,0);
        my ($exon1,$exon2)=(0,0);
        my $FastaTag = bracketTransformat($splice);
        my $Overlap = 0;
        
        # get genomic positions of start/end, in genome direction
        # note that start(end) is exactly the point in front(back) of the junction start(end)
        if ($splice =~ /(\d+)\[(\d+)-(\d+)\]/) {
            $exon1 = $1;
            $exon2 = $1;
            $genomeStart = $cgffHOA{$geneID}[2*($exon1-1)]+$2;
            $genomeEnd = $cgffHOA{$geneID}[2*($exon1-1)]+$3;
        }elsif ($splice =~ /(\d+)\(([+|-]?\d+)\)-(\d+)\(([+|-]?\d+)\)/) {
            $exon1 = $1;
            $exon2 = $3;
            $genomeStart = $cgffHOA{$geneID}[2*($exon1-1)+1]+$2;
            $genomeEnd = $cgffHOA{$geneID}[2*($exon2-1)]-$4;
        }elsif ($splice =~ /(\d+)-(\d+)\(([+|-]?\d+)\)/) {
            $exon2 = $2;
            $genomeStart = $1;
            $genomeEnd = $cgffHOA{$geneID}[2*($exon2-1)]-$3;
        }elsif ($splice =~ /(\d+)\(([+|-]?\d+)\)-(\d+)/) {
            $exon1 = $1;
            $genomeStart = $cgffHOA{$geneID}[2*($exon1-1)+1]+$2;
            $genomeEnd = $3;
        }elsif ($splice =~ /(\d+)-(\d+)/) {
            $genomeStart = $1;
            $genomeEnd = $2;
        }elsif ($splice =~ /(\d+)<=>(\d+)/ ) {
            $exon1 = $1;
            $exon2 = $2;
            $genomeStart = $cgffHOA{$geneID}[2*($exon1-1)+1];
            $genomeEnd = $cgffHOA{$geneID}[2*($exon2-1)];
        }elsif ($splice=~ />(\d+)</) {
            $exon1 = $1;
            $genomeStart = $cgffHOA{$geneID}[2*($exon1-1)]-1;
            $genomeEnd = $cgffHOA{$geneID}[2*($exon1-1)+1]+1;
        }elsif ($splice=~ /^(\d+)$/) {
            $exon1 = $1;
            $exon2 = $1 + 1;
            $genomeStart = $cgffHOA{$geneID}[2*($exon1-1)+1];
            $genomeEnd = $cgffHOA{$geneID}[2*($exon2-1)];
        }
        
        my $width = $genomeEnd - $genomeStart + 1;
        
        my ($start,$end)=($genomeStart,$genomeEnd);
        ($start,$end)=($genomeEnd,$genomeStart) if $strand eq "-";
        $Overlap = 1 if $w2+$w3 > $width;
        
        if (($Overlap && $overlapMerge) || ($w2==-1 && $w3==-1)){
            if ($strand eq "+"){
                my $lookupString = "$geneID\_$FastaTag\_Overlap_forward";
                my $originString = "$geneID\_$splice\_Overlap_forward";
                my $seq = $genomeDB->seq($chr,($start-$w1+1) => ($end+$w4-1));
                push(@{$lookupHOA{$lookupString}}, $originString, $geneID, $start, $end, $strand, 0, ($start-$w1+1), ($end+$w4-1), $seq);
            }else{
                my $lookupString = "$geneID\_$FastaTag\_Overlap_reverse";
                my $originString = "$geneID\_$splice\_Overlap_reverse";
                my $seq = $genomeDB->seq($chr,($start+$w1-1) => ($end-$w4+1));
                push(@{$lookupHOA{$lookupString}}, $originString, $geneID, $start, $end, $strand, 0, ($start+$w1-1), ($end-$w4+1), $seq);
            }
        }else{
            if ($strand eq "+"){
                if ($w1+$w2>0){
                    my $lookupString = "$geneID\_$FastaTag\_forward_1";
                    my $originString = "$geneID\_$splice\_forward_1";
                    my $seq = $genomeDB->seq($chr,($start-$w1+1) => ($start+$w2));
                    push(@{$lookupHOA{$lookupString}}, $originString, $geneID, $start, $end, $strand, 1, ($start-$w1+1), ($start+$w2), $seq);
                }
                if ($w3+$w4>0){
                    my $lookupString = "$geneID\_$FastaTag\_forward_2";
                    my $originString = "$geneID\_$splice\_forward_2";
                    my $seq = $genomeDB->seq($chr,($end-$w3) => ($end+$w4-1));
                    push(@{$lookupHOA{$lookupString}}, $originString, $geneID, $start, $end, $strand, 2, ($end-$w3), ($end+$w4-1), $seq);
                }
            }else{
                if ($w1+$w2>0){
                    my $lookupString = "$geneID\_$FastaTag\_reverse_1";
                    my $originString = "$geneID\_$splice\_reverse_1";
                    my $seq = $genomeDB->seq($chr,($start+$w1-1) => ($start-$w2));
                    push(@{$lookupHOA{$lookupString}}, $originString, $geneID, $start, $end, $strand, 1, ($start+$w1-1), ($start-$w2), $seq);
                }
                if ($w3+$w4>0){
                    my $lookupString = "$geneID\_$FastaTag\_reverse_2";
                    my $originString = "$geneID\_$splice\_reverse_2";
                    my $seq = $genomeDB->seq($chr,($end+$w3) => ($end-$w4+1));
                    push(@{$lookupHOA{$lookupString}}, $originString, $geneID, $start, $end, $strand, 2, ($end+$w3), ($end-$w4+1), $seq);
                }
            }
        }
    }
}

# for each line in mastIn file
print "fasta read done\n" if $debug;
open(FILE,"<$mastIn") or die "Mast file error\n";
open(OUT,">$outFilename");
print OUT "sequence_name\tmotif\thit_start\thit_end\tscore\thit_p-value\thit_region\thit Start\thit Stop\tto junction start\tto junction end\n";
while( $line = <FILE> ) {
    next if ($line =~ /^#/);
    my @token = split(/\s+/, $line);
    my $lookupString = $token[0];
    my $motif = $token[1];
    my $hitStart = $token[2];
    my $hitStop = $token[3];
    
    # get corresponding infor
    my ($sequenceName, $geneID, $junctionStart, $junctionEnd, $strand, $seqNo, $seqStartGenome, $seqStopGenome, $seq) = @{ $lookupHOA{$lookupString} };
    
    # position assert
    my $chr = $seqNameMap{uc($geneInfoHOA{$geneID}[0])};
    my $chrLength = $genomeDB->length($chr);
    $seqStartGenome = assertPosition($seqStartGenome,$chrLength);
    $seqStopGenome = assertPosition($seqStopGenome,$chrLength);
    
    my ($start_shift,$end_shift)=("","");
    my ($hitStartGenome,$hitStopGenome)=(-1,-1);
    # transfer hitStart & hitStop into hitStartGenome & hitStopGenome
    # and find position of hitStart relative to the junction
    if ($strand eq "+") {
        $hitStartGenome = $seqStartGenome + $hitStart -1;
        $hitStopGenome = $seqStartGenome + $hitStop -1;
        $start_shift = $hitStartGenome - $junctionStart;
        $end_shift =  $hitStopGenome - $junctionEnd;
    }elsif ($strand eq "-") {
        $hitStartGenome = $seqStartGenome - $hitStart +1;
        $hitStopGenome = $seqStartGenome - $hitStop +1;
        $start_shift = $junctionStart - $hitStartGenome;
        $end_shift = $junctionEnd - $hitStopGenome;
    }
    
    print "$lookupString, $geneID, $junctionStart, $junctionEnd, $strand, $seqNo, $seqStartGenome, $seqStopGenome\n" if $debug;
    
    # output 
    shift(@token);
    print OUT "$sequenceName\t";
    foreach (@token){
        print OUT trim($_)."\t";
    }

    my $subSeq = substr($seq,$hitStart-1,$hitStop-$hitStart+1);
    if($motif=~/^-/){ #reversed hit
        $subSeq = reverse $subSeq;
        $subSeq =~ tr/ACGTNacgtn/TGCANtgcan/;
    }
    print OUT $subSeq."\t";

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

sub trim {
    my $str=shift;
    $str =~ s/\s+$//g;
    $str =~ s/^\s+//g;
    return $str;
}

sub bracketTransformat {
    my $str=shift;
    $str =~ tr/[]()<>=/qpdbL7Z/;
    return $str;
}
