#!/usr/bin/perl
use Bio::DB::Fasta;

$usage = "Usage: SeqGenExonSkippingJunctionSite.pl strandCGFF esList width genomeFasta outFilename\n";
$strandCGFF = shift or die $usage;
$esListFile = shift or die $usage;
$width = shift or die $usage;
$genomeFasta = shift or die $usage;
$outFilename = shift or die $usage;

# read strandCGFF to build geneInfoHOA and cgffHOA
%geneInfoHOA = ();
%cgffHOA = ();
open(FILE,"<$strandCGFF");
while( $line = <FILE> ) {
    @token = split(/\t/, $line);
    $geneID;
    
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
close FILE;

# read exons in eaList file into exonHash
%exonHash = ();
open(FILE,"<$esListFile");
while( $line = <FILE> ) {
    @token = split(/\s/, $line);
    $exonHash{$token[0],$token[1]}="";
}
close FILE;

# for case insensitive seq names
my $genomeDB = Bio::DB::Fasta->new($genomeFasta);
my @seqIds = $genomeDB->ids;
my %seqNameMap;

for (my $idx = 0; $idx < @seqIds; $idx++) {
    $seqNameMap{uc($seqIds[$idx])} = $seqIds[$idx];
}


# process each exon
open(OUT,">$outFilename");
for $exon (sort byIntron keys %exonHash) {
    # getting exon infor
    @token = split($;, $exon);
    $geneID = $token[0];
    $exonPair = $token[1];
    @splitExon = ();
    @splitExon = split(/<=>/, $exonPair);
    # get 5' start and 3' stop of the intron
    $startPosition = $cgffHOA{$geneID}[2*($splitExon[0]-1)+1];
    $stopPosition = $cgffHOA{$geneID}[2*$splitExon[1]-2];
    # get chr and strand
    $chr = $seqNameMap{uc($geneInfoHOA{$geneID}[0])};
    $strand = $geneInfoHOA{$geneID}[1];
    
    
    if ($strand eq "+") {
        print OUT ">$geneID"."_".$splitExon[0]."_".$splitExon[1]."_"."forward\n";
        my $seq = $genomeDB->seq($chr,($startPosition-$width) => ($stopPosition+$width));
        print OUT $seq."\n";
    }else{
        print OUT ">$geneID"."_".$splitExon[0]."_".$splitExon[1]."_"."reverse\n";
        my $seq = $genomeDB->seq($chr,($stopPosition+$width) => ($startPosition-$width));
        print OUT $seq."\n";
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
