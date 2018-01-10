#!/usr/bin/perl
use Bio::DB::Fasta;

$usage = "Usage: IRFlankingExonSeqs.pl strandCGFF irList genomeFasta outFilename\n";
$strandCGFF = shift or die $usage;
$irListFile = shift or die $usage;
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

# read introns in irList file into exonHash
%irHash = ();
%orderHash = ();
$order = 1;
open(FILE,"<$irListFile");
while( $line = <FILE> )
{
	@token = split(/\s/, $line);
	$esHash{$token[0],$token[1]}="";
	$orderHash{$token[0],$token[1]} = $order;
	$order++;
}
close FILE;

my $genomeDB = Bio::DB::Fasta->new($genomeFasta);
open(OUT,">$outFilename");
print OUT "geneID\tstrand\tintronNo\tintronLen\tstart\tstop\tpreExonSeq\tpostExonSeq\tbrowseStr\n";
for $key ( sort byOrder keys %esHash ){
	@token = split($;, $key);
	$geneID = $token[0];
	$intronNo = $token[1];
	$chr = $geneInfoHOA{$geneID}[0];
	$strand = $geneInfoHOA{$geneID}[1];
	$preExon = $intronNo;
	$postExon = $intronNo+1;
	
	$preExonStart = $cgffHOA{$geneID}[2*($intronNo-1)];
	$preExonStop = $cgffHOA{$geneID}[2*($intronNo-1)+1];
	$postExonStart = $cgffHOA{$geneID}[2*$intronNo];
	$postExonStop = $cgffHOA{$geneID}[2*$intronNo+1];
	$intronStart = $preExonStop+1;
	$intronStop = $postExonStart-1;
	
	print OUT "$geneID\t$strand\t$intronNo\t".($intronStop-$intronStart+1)."\t$intronStart\t$intronStop\t";
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
