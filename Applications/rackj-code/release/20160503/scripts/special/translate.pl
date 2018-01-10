#!/usr/bin/perl -w

my $usage = "Usage: translate.pl <inFASTA> <outFASTA>\n";
my $inFASTA = shift or die $usage; 
my $outFASTA = shift or die $usage;

my %genetic_code = getTranslateMap();

open(INFILE,"<$inFASTA");
open(OUTFILE,">$outFASTA");

my $orfSeq = "";
my $lastID = "";
while(my $line=<INFILE>){
	$line = trim($line);
	if($line=~/^>(.+)$/){
		print OUTFILE ">$lastID\n".seq2aa($orfSeq)."\n" if length($lastID)>0;
		$lastID = $1;
		$orfSeq = "";
	}else{
		$orfSeq .= $line;
	}
}
print OUTFILE ">$lastID\n".seq2aa($orfSeq)."\n" if length($lastID)>0;

close INFILE;
close OUTFILE;

# subroutines
sub trim {
	my $str=shift;
	$str =~ s/(^\s+|\s+$)//g;
	return $str;
}

sub getTranslateMap{
	my (%retuenMap) = (
		'TCA' => 'S',    #Serine
		'TCC' => 'S',    #Serine
		'TCG' => 'S',    #Serine
		'TCT' => 'S',    #Serine
		'TCN' => 'S',    #Serine
		'TTC' => 'F',    #Phenylalanine
		'TTT' => 'F',    #Phenylalanine
		'TTA' => 'L',    #Leucine
		'TTG' => 'L',    #Leucine
		'TAC' => 'Y',    #Tyrosine
		'TAT' => 'Y',    #Tyrosine
		'TAA' => '*',    #Stop
		'TAG' => '*',    #Stop
		'TGC' => 'C',    #Cysteine
		'TGT' => 'C',    #Cysteine
		'TGA' => '*',    #Stop
		'TGG' => 'W',    #Tryptophan
		'CTA' => 'L',    #Leucine
		'CTC' => 'L',    #Leucine
		'CTG' => 'L',    #Leucine
		'CTT' => 'L',    #Leucine
		'CTN' => 'L',    #Leucine
		'CAT' => 'H',    #Histidine
		'CAC' => 'H',    #Histidine
		'CAA' => 'Q',    #Glutamine
		'CAG' => 'Q',    #Glutamine
		'CGA' => 'R',    #Arginine
		'CGC' => 'R',    #Arginine
		'CGG' => 'R',    #Arginine
		'CGT' => 'R',    #Arginine
		'CGN' => 'R',    #Arginine
		'ATA' => 'I',    #Isoleucine
		'ATC' => 'I',    #Isoleucine
		'ATT' => 'I',    #Isoleucine
		'ATG' => 'M',    #Methionine
		'ACA' => 'T',    #Threonine
		'ACC' => 'T',    #Threonine
		'ACG' => 'T',    #Threonine
		'ACT' => 'T',    #Threonine
		'ACN' => 'T',    #Threonine
		'AAC' => 'N',    #Asparagine
		'AAT' => 'N',    #Asparagine
		'AAA' => 'K',    #Lysine
		'AAG' => 'K',    #Lysine
		'AGC' => 'S',    #Serine
		'AGT' => 'S',    #Serine
		'AGA' => 'R',    #Arginine
		'AGG' => 'R',    #Arginine
		'CCA' => 'P',    #Proline
		'CCC' => 'P',    #Proline
		'CCG' => 'P',    #Proline
		'CCT' => 'P',    #Proline
		'CCN' => 'P',    #Proline
		'GTA' => 'V',    #Valine
		'GTC' => 'V',    #Valine
		'GTG' => 'V',    #Valine
		'GTT' => 'V',    #Valine
		'GTN' => 'V',    #Valine
		'GCA' => 'A',    #Alanine
		'GCC' => 'A',    #Alanine
		'GCG' => 'A',    #Alanine
		'GCT' => 'A',    #Alanine
		'GCN' => 'A',    #Alanine
		'GAC' => 'D',    #Aspartic Acid
		'GAT' => 'D',    #Aspartic Acid
		'GAA' => 'E',    #Glutamic Acid
		'GAG' => 'E',    #Glutamic Acid
		'GGA' => 'G',    #Glycine
		'GGC' => 'G',    #Glycine
		'GGG' => 'G',    #Glycine
		'GGT' => 'G',    #Glycine
		'GGN' => 'G',    #Glycine
	);
	return %retuenMap;
}

sub seq2aa{
	my $seq=uc $_[0];
	my $Ns = 3-(length($seq)%3);
	$seq .= 'N' x $Ns if $Ns < 3;  # complement Sequence to 3mer
	
	my $AA_seq='';
	for $k (0 .. length($seq)/3-1) {
		my $codon = substr($seq,3*$k,3);
		
		if(exists $genetic_code{$codon}) {
			$AA_seq.=$genetic_code{$codon};
		}else{
			$AA_seq.="x";
		}
	}
	return $AA_seq;
}
