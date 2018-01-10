#!/usr/bin/perl
use Bio::DB::Fasta;

$usage = "Usage: MastAnnotation.pl <mastIn> <fastaIn> <outFilename>\n";
$mastIn = shift or die $usage;
$fastaIn = shift or die $usage;
$outFilename = shift or die $usage;

# for each line in mastIn file
$fastaDB = Bio::DB::Fasta->new($fastaIn);

open(FILE,"<$mastIn") or die "Cant't not open $mastIn:$!\n";
open(OUT,">$outFilename");
print OUT "sequence_name\tmotif\thit_start\thit_end\tscore\thit_p-value\ttoSeqEnd\thit_region\n";
while( $line = <FILE> )
{	
        next if ($line =~ /^#/);
	@token = split(/\s+/, $line);
	$lookupString = $token[0];
	$motif = $token[1];
	$hitStart = $token[2];
	$hitStop = $token[3];
	
	foreach (@token){
	  print OUT trim($_)."\t";
	}
	
	# distance to sequence end
	$len = length($fastaDB->get_Seq_by_id($lookupString)->seq);
	print OUT ($len-$hitStop)."\t";
	
	# hit region
	if($motif=~/^-/){ # reversed hit
	  print OUT $fastaDB->seq($lookupString, $hitStop => $hitStart)."\n";
        }else{
	  print OUT $fastaDB->seq($lookupString, $hitStart => $hitStop)."\n";
        }
}
close OUT;
close FILE;

sub trim
{
	my $str = shift;
	$str =~ s/\s+$//g;
	$str =~ s/\s+$//g;
	return $str;
}
