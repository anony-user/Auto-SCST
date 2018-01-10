#!/usr/bin/perl -w
use Bio::DB::Fasta;

my $usage = "Usage: MastAnnotationDA.pl strandCGFF spliceList w1 w2 w3 w4 overlapMerge(0/1) mastIn genomeFastaIn outFilename\n";
my $strandCGFF = shift or die $usage;
my $spliceListFile = shift or die $usage;
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
while( $line = <FILE> ) {
    my @token = split(/\t/, $line);
    if ($line=~/^>/) {
        $geneID = substr $token[0],1;
        if (@token>4) {
            # chr in array[0] and strand in array[1]
            push @{ $geneInfoHOA{$geneID} },trim($token[1]),trim($token[4]); 
        }else{
            die "$strandCGFF not a strand CGFF\n"
        }
    }else{
        push(@{$cgffHOA{$geneID}}, trim($token[0]), trim($token[1])); 
    }
}
close(FILE);

# for case insensitive seq names
my $genomeDB = Bio::DB::Fasta->new($genomeFastaIn);
my @seqIds = $genomeDB->ids;
my %seqNameMap = ();

for (my $idx = 0; $idx < @seqIds; $idx++) {
    $seqNameMap{uc($seqIds[$idx])} = $seqIds[$idx];
}

# read introns in intronList file into intronHash
my %geneSpliceSet;
open(FILE,"<$spliceListFile");
while(<FILE>) {
    next if /^\#/;
    my @token = split(/\s/);
    my $geneID = $token[0];
    my $splice = $token[1];
    $geneSpliceSet{$geneID}{$splice}="";
}

# build lookupHOA

my %lookupHOA;
for $geneID (sort {$a cmp $b} keys %geneSpliceSet) {
    for $splice (sort {$a cmp $b} keys %{$geneSpliceSet{$geneID}}) {
        my $chr = $seqNameMap{uc($geneInfoHOA{$geneID}[0])};
        my $strand = $geneInfoHOA{$geneID}[1];
        my ($start,$end)=(0,0);
        my ($exon1,$exon2)=(0,0);
        my $FastaTag = bracketTransformat($splice);
        my $Overlap = 0;
        my $width = 0;
        
        if ($splice =~ /(\d+)\[(\d+)-(\d+)\]/) {
            $exon1 = $1;
            $exon2 = $1;
            if ($strand eq "+") {
                $start = $cgffHOA{$geneID}[2*($exon1-1)]+$2;
                $end = $cgffHOA{$geneID}[2*($exon1-1)]+$3;
                $width = $end - $start + 1;
            }elsif ($strand eq "-") {
                $start = $cgffHOA{$geneID}[2*($exon1-1)]+$3;
                $end = $cgffHOA{$geneID}[2*($exon1-1)]+$2;
                $width = $start - $end + 1;
            }
        }elsif ($splice =~ /(\d+)\(([+|-]?\d+)\)-(\d+)\(([+|-]?\d+)\)/) {
            $exon1 = $1;
            $exon2 = $3;
            if ($strand eq "+") {
                $start = $cgffHOA{$geneID}[2*($exon1-1)+1]+$2;
                $end = $cgffHOA{$geneID}[2*($exon2-1)]-$4;
                $width = $end - $start + 1;
            }elsif ($strand eq "-") {
                $start = $cgffHOA{$geneID}[2*($exon2-1)]-$4;
                $end = $cgffHOA{$geneID}[2*($exon1-1)+1]+$2;
                $width = $start - $end + 1;
            }
        }elsif ($splice =~ /(\d+)-(\d+)\(([+|-]?\d+)\)/) {
            $exon1 = $2;
            if ($strand eq "+") {
                $start = $1;
                $end = $cgffHOA{$geneID}[2*($exon1-1)]-$3;
                $width = $end - $start + 1;
            }elsif ($strand eq "-") {
                $start = $cgffHOA{$geneID}[2*($exon1-1)]-$3;
                $end = $1;
                $width = $start - $end + 1;
            }
        }elsif ($splice =~ /(\d+)\(([+|-]?\d+)\)-(\d+)/) {
            $exon1 = $1;
            if ($strand eq "+") {
                $start = $cgffHOA{$geneID}[2*($exon1-1)+1]+$2;
                $end = $3;
                $width = $end - $start + 1;
            }elsif ($strand eq "-") {
                $start = $3;
                $end = $cgffHOA{$geneID}[2*($exon1-1)+1]+$2;
                $width = $start - $end + 1;
            }
        }elsif ($splice =~ /(\d+)-(\d+)/) {
            if ($strand eq "+") {
                $start =$1;
                $end = $2;
                $width = $end - $start + 1;
            }elsif ($strand eq "-") {
                $start = $2;
                $end = $1;
                $width = $start - $end + 1;
            }
        }
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
open(FILE,"<$mastIn");
open(OUT,">$outFilename");
print OUT "sequence_name\tmotif\thit_start\thit_end\tscore\thit_p-value\thit_region\thit Start\thit Stop\tHit start to junction start\tHit stop to junction end\n";
while( $line = <FILE> ) {
    next if ($line =~ /^#/);
    my @token = split(/\s+/, $line);
    my $lookupString = $token[0];
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

sub trim {
    my $str=shift;
    $str =~ s/\s+$//g;
    $str =~ s/^\s+//g;
    return $str;
}

sub bracketTransformat {
    my $str=shift;
    $str =~ tr/[]()/qpdb/;
    return $str;
}