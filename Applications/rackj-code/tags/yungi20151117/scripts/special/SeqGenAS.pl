#!/usr/bin/perl
use Bio::DB::Fasta;

my $usage = "Usage: SeqGenAS.pl [-exonES] <spliceList> <w1> <w2> <w3> <w4> <overlapMerge(0/1)> <strandCGFF> <genomeFasta> <outFilename>\n";

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
my $strandCGFF = shift or die $usage;
my $genomeFasta = shift or die $usage;
my $outFilename = shift or die $usage;

# read strandCGFF to build geneInfoHOA and cgffHOA
my %geneInfoHOA;
my %cgffHOA;
open(FILE,"<$strandCGFF") or die "strandCGFF file error\n";
while(<FILE>) {
    my @token = split(/\s/);
    if (/^>/) {
        $geneID = substr($token[0],1);
        if (@token>4) {
            # chr in array[0] and strand in array[1]
            push(@{$geneInfoHOA{$geneID}}, trim($token[1]), trim($token[4])); 
        }else{
            die "$strandCGFF not a strand CGFF\n";
        }
    }else{
        push(@{$cgffHOA{$geneID}},trim($token[0]),trim($token[1])); 
    }
}
close(FILE);

my $genomeDB = Bio::DB::Fasta->new($genomeFasta);

# for case insensitive seq names
my @seqIds = $genomeDB->ids;
my %seqNameMap;

for (my $idx = 0; $idx < @seqIds; $idx++) {
    $seqNameMap{uc($seqIds[$idx])} = $seqIds[$idx];
}

my %geneSpliceSet;
# read pattern in List file into geneSpliceHash
open(FILE,"<$ListFile") or die "List file error\n";
while(<FILE>) {
    next if /^\#/;
    my @token = split(/\s/);
    my $geneID = $token[0];
    my $splice = $token[1];

    if( $exonES && ( $splice =~ /(\d+)<=>(\d+)/ ) ){
    	my ($startExon,$stopExon) = ($1,$2);
    	for(my $skippedExon = $startExon+1; $skippedExon < $stopExon; $skippedExon++){
            $geneSpliceSet{$geneID}{">$skippedExon<"}="";
        }
    }else{
        $geneSpliceSet{$geneID}{$splice}="";
    }
}

# process each splice
open(OUT,">$outFilename");
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
                print OUT ">$geneID"."_$FastaTag"."_Overlap_"."forward\n";
                my $seq = $genomeDB->seq($chr,($start-$w1+1) => ($end+$w4-1));
                print OUT $seq."\n";
            }else{
                print OUT ">$geneID"."_$FastaTag"."_Overlap_"."reverse\n";
                my $seq = $genomeDB->seq($chr,($start+$w1-1) => ($end-$w4+1));
                print OUT $seq."\n";
            }
        }else{
            if ($strand eq "+"){
                if ($w1+$w2>0){
                    print OUT ">$geneID"."_$FastaTag"."_"."forward"."_"."1\n";
                    my $seq = $genomeDB->seq($chr,($start-$w1+1) => ($start+$w2));
                    print OUT $seq."\n";
                }
                if ($w3+$w4>0){
                    print OUT ">$geneID"."_$FastaTag"."_"."forward"."_"."2\n";
                    my $seq = $genomeDB->seq($chr,($end-$w3) => ($end+$w4-1));
                    print OUT $seq."\n";
                }
            }else{
                if ($w1+$w2>0){
                    print OUT ">$geneID"."_$FastaTag"."_"."reverse"."_"."1\n";
                    my $seq = $genomeDB->seq($chr,($start+$w1-1) => ($start-$w2));
                    print OUT $seq."\n";
                }
                if ($w3+$w4>0){
                    print OUT ">$geneID"."_$FastaTag"."_"."reverse"."_"."2\n";
                    my $seq = $genomeDB->seq($chr,($end+$w3) => ($end-$w4+1));
                    print OUT $seq."\n";
                }
            }
        }
    }
}
close(OUT);


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