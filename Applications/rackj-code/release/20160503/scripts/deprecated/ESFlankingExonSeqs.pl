#!/usr/bin/perl
use Bio::DB::Fasta;

$usage = "Usage: ESFlankingExonSeqs.pl strandCGFF esList genomeFasta outFilename\n";
$strandCGFF = shift or die $usage;
$esListFile = shift or die $usage;
$genomeFasta = shift or die $usage;
$outFilename = shift or die $usage;

# read strandCGFF to build geneInfoHOA and cgffHOA
%geneInfoHOA = ();
%cgffHOA = ();
open(FILE,"<$strandCGFF");
while( $line = <FILE> )
{
	@token = split(/\t/, $line);
	$geneID;

	if( $line=~/^>/ )
	{
		$geneID = substr $token[0],1;
		if(@token>4){
			# chr in array[0] and strand in array[1]
			push @{ $geneInfoHOA{$geneID} },trim($token[1]),trim($token[4]); 
		}else{
			die "$strandCGFF not a strand CGFF\n"
		}
	}else{
		push @{ $cgffHOA{$geneID} },trim($token[0]),trim($token[1]); 
	}
}
close FILE;

# read exons in eaList file into exonHash
%esHash = ();
%orderHash = ();
$order = 1;
open(FILE,"<$esListFile");
while( $line = <FILE> )
{
	@token = split(/\s/, $line);
	@exonPair = split(/<=>/, $token[1]);
	push @{$esHash{$token[0],$token[1]}}, @exonPair;
	$orderHash{$token[0],$token[1]} = $order;
	$order++;
}
close FILE;

my $genomeDB = Bio::DB::Fasta->new($genomeFasta);
open(OUT,">$outFilename");
print OUT "geneID\tstrand\texonPair\tpreExon\tpostExon\tpreExonSeq\tpostExonSeq\tbrowseStr\n";
for $key ( sort byOrder keys %esHash ){
	@token = split($;, $key);
	$geneID = $token[0];
	$chr = $geneInfoHOA{$geneID}[0];
	$strand = $geneInfoHOA{$geneID}[1];
	$preExon = $esHash{$token[0],$token[1]}[0];
	$postExon = $esHash{$token[0],$token[1]}[1];
	
	$preExonStart = $cgffHOA{$geneID}[2*($preExon-1)];
	$preExonStop = $cgffHOA{$geneID}[2*($preExon-1)+1];
	$postExonStart = $cgffHOA{$geneID}[2*($postExon-1)];
	$postExonStop = $cgffHOA{$geneID}[2*($postExon-1)+1];
	
	print OUT "$geneID\t$strand\t$token[1]\t$preExon\t$postExon\t";
	print OUT $genomeDB->seq($chr,$preExonStart => $preExonStop)."\t";
	print OUT $genomeDB->seq($chr,$postExonStart => $postExonStop)."\t";
	print OUT $chr.":".($preExonStart-20)."-".($postExonStop+20)."\n";
}
close OUT;

sub byOrder 
{
	my @token1 = split($;, $a);
	my @token2 = split($;, $b);
	my $x_a = $token1[0];
	my $y_a = $token1[1];
	my $x_b = $token2[0];
	my $y_b = $token2[1];
	return ($orderHash{$x_a,$y_a} <=> $orderHash{$x_b,$y_b});
}

sub trim {
    my $str=shift;
    $str =~ s/\s+$//g;
    $str =~ s/^\s+//g;
    return $str;
}
