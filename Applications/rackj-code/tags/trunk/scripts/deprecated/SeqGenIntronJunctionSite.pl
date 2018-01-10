#!/usr/bin/perl
use Bio::DB::Fasta;

$usage = "Usage: SeqGenIntronJunctionSite.pl strandCGFF intronList w1 w2 w3 w4 overlapMerge(0/1) genomeFasta outFilename\n";
$strandCGFF = shift or die $usage;
$intronListFile = shift or die $usage;
$w1 = $ARGV[0]; shift;
$w2 = $ARGV[0]; shift;
$w3 = $ARGV[0]; shift;
$w4 = $ARGV[0]; shift;
$overlapMerge = $ARGV[0]; shift;
$genomeFasta = shift or die $usage;
$outFilename = shift or die $usage;

# read strandCGFF to build geneInfoHOA and cgffHOA
%geneInfoHOA = ();
%cgffHOA = ();
open(FILE,"<$strandCGFF");
while ($line = <FILE>) {
    @token = split(/\t/, $line);
    $geneID;
    
    if ($line=~/^>/) {
        $geneID = substr $token[0],1;
        if (@token>4){
            # chr in array[0] and strand in array[1]
            push @{ $geneInfoHOA{$geneID} },trim($token[1]),trim($token[4]); 
        }else{
            die "$strandCGFF not a strand CGFF\n"
        }
    }else{
        push(@{$cgffHOA{$geneID}}, trim($token[0]), trim($token[1])); 
    }
}
close FILE;

# for case insensitive seq names
my $genomeDB = Bio::DB::Fasta->new($genomeFasta);
my @seqIds = $genomeDB->ids;
my %seqNameMap = ();

for (my $idx = 0; $idx < @seqIds; $idx++) {
    $seqNameMap{uc($seqIds[$idx])} = $seqIds[$idx];
}

# read introns in intronList file into intronHash
%intronHash = ();
open(FILE,"<$intronListFile");
while( $line = <FILE> ) {
    @token = split(/\s/, $line);
    $intronHash{$token[0],$token[1]}="";
}
close FILE;

# process each intron
open(OUT,">$outFilename");
for $intron ( sort byIntron keys %intronHash ) {
    # getting intron infor
    @token = split($;, $intron);
    $geneID = $token[0];
    $intronNo = $token[1];
    # get 5' start and 3' stop of the intron
    $intronStart = $cgffHOA{$geneID}[2*$intronNo-1]+1;
    $intronStop = $cgffHOA{$geneID}[2*$intronNo]-1;
    # get chr and strand
    $chr = $seqNameMap{uc($geneInfoHOA{$geneID}[0])};
    $strand = $geneInfoHOA{$geneID}[1];
    
    # computation of ranges of intron junction site
    # check if overlap in intron side
    $intronSideOverlap = 0;
    if ($w2+$w3 > $intronStop-$intronStart+1) {
        $intronSideOverlap = 1;
    }
    
    if (($intronSideOverlap && $overlapMerge) || ($w2==-1 && $w3==-1)) {
        if ($strand eq "+") {
            print OUT ">$geneID"."_"."$intronNo"."_"."forward\n";
            my $seq = $genomeDB->seq($chr,($intronStart-1-$w1+1) => ($intronStop+1+$w4-1));
            print OUT $seq."\n";
        }else{
            print OUT ">$geneID"."_"."$intronNo"."_"."reverse\n";
            my $seq = $genomeDB->seq($chr,($intronStop+1+$w1-1) => ($intronStart-1-$w4+1));
            print OUT $seq."\n";
        }
    }else{
        if ($strand eq "+") {
            if ($w1+$w2>0) {
                print OUT ">$geneID"."_"."$intronNo"."_"."forward"."_"."1\n";
                my $seq = $genomeDB->seq($chr,($intronStart-1-$w1+1) => ($intronStart+$w2-1));
                print OUT $seq."\n";
            }
            if ($w3+$w4>0) {
                print OUT ">$geneID"."_"."$intronNo"."_"."forward"."_"."2\n";
                my $seq = $genomeDB->seq($chr,($intronStop-$w3+1) => ($intronStop+1+$w4-1));
                print OUT $seq."\n";
            }
        }else{
            if ($w1+$w2>0) {
                print OUT ">$geneID"."_"."$intronNo"."_"."reverse"."_"."1\n";
                my $seq = $genomeDB->seq($chr,($intronStop+1+$w1-1) => ($intronStop-$w2+1));
                print OUT $seq."\n";
            }
            if ($w3+$w4>0) {
                print OUT ">$geneID"."_"."$intronNo"."_"."reverse"."_"."2\n";
                my $seq = $genomeDB->seq($chr,($intronStart+$w3-1) => ($intronStart-1-$w4+1));
                print OUT $seq."\n";
            }
        }
    }
}
close OUT;

sub byIntron {
    my @token1 = split($;, $a);
    my @token2 = split($;, $b);
    my $x_a = $token1[0];
    my $y_a = $token1[1];
    my $x_b = $token2[0];
    my $y_b = $token2[1];
    if (($x_a cmp $x_b) != 0) {
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
