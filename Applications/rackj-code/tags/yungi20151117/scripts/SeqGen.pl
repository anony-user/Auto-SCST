#!/usr/bin/perl
use Getopt::Std;
use Bio::DB::Fasta;

# Given a (strand) CGFF file and genome sequence file (in FASTA format), this program
# extracts sequences from genomeFasta for the CGFF. A strandFile could be given for
# indicating sequence directions of a certain gene list.
#
# This program requires BioPerl.

# get parameters
my $usage = "Usage: SeqGen.pl [-n <lineLength>] <outputFasta> <genomeFasta> <CGFF> [<strandFile>]\n";

my %opts = (n=>'60');
getopts('n:', \%opts);
my $lineWidth = $opts{n};

$numArgs = @ARGV;
if($numArgs < 3)
{
	print $usage;
	exit;
}

$output = $ARGV[0]; 
$genome = $ARGV[1]; 
$cgff = $ARGV[2];
if($numArgs < 4)
{
	$strandFile = "";
}
else
{
	$strandFile = $ARGV[3];
}

open(strandFile,"<$strandFile");
%strandFilehash = ();
while( $line = <strandFile> )
{
	@token= split(/\t/, $line);
	$strandFilehash{$token[0]} = trim($token[1]);
	#@keys = keys(%strandFilehash);
}
close strandFile;

# create database from directory of fasta files
my $db = Bio::DB::Fasta->new($genome);

open(CGFF,"<$cgff");
open(report,">$output");

%notFindOfId = ();
%chromosome = ();
my @ids = $db->ids;
$idarrSize = @ids;

for($i = 0; $i < $idarrSize; $i++)
{
	$chromosome{uc(@ids[$i])} = @ids[$i];
}

@token = ();
while( $line = <CGFF> )
{	
	@token = split(/\t/, $line);

	if( $token[0]=~/^>/ )
	{	
		if( $processedGene > 0 && length($sequences) > 0)
		{
			print report ">$gene\n";
			if($lineWidth ne 'null'){
				my $idx;
				for($idx=0;$idx+$lineWidth-1 <= length($sequences)-1;$idx+=$lineWidth){
					print report substr($sequences,$idx,$lineWidth)."\n";
				}
				print report substr($sequences,$idx)."\n" if $idx <= length($sequences)-1;
			}else{
				print report $sequences."\n";
			}
			$sequences = "";
			$processedGene = 0;
		}
		$processedGene++;
#		print $gene."\n";
		@splitGeneid = split(/>/, $token[0]);
		$gene = @splitGeneid[1];
		if(@token>4){
			#print "$gene\t".trim($token[4])."\n";
			$strandFilehash{$gene} = trim($token[4]);
		}
		$id = $token[1];
		$ID = uc($id);

		if(!exists $chromosome{$ID}){
			$notFindOfId{$id} = "";
		}
	}
	else
	{
		$start = $token[0];
		$stop = $token[1];

		next if(!exists $chromosome{$ID});
		
		if( exists $strandFilehash{$gene} )
		{
			if( $strandFilehash{$gene}=~/\+/ )
			{
				my $seq = $db->seq($chromosome{$ID},$start => $stop);
				if( length($sequences) > 0 )
				{
					if($lineWidth ne 'null'){
						$sequences=$sequences.$seq;
					}else{
						$sequences=$sequences."\n".$seq;
					}
				}
				else
				{
					$sequences=$seq;
				}
			}
			else
			{
				my $revseq = $db->seq($chromosome{$ID},$stop => $start);
				if( length($sequences) > 0 )
				{
					if($lineWidth ne 'null'){
						$sequences=$revseq.$sequences;
					}else{
						$sequences=$revseq."\n".$sequences;
					}
				}
				else
				{
					$sequences=$revseq;
				}				
			}
		}
		else
		{	
			my $seq = $db->seq($chromosome{$ID},$start => $stop);
			if( length($sequences) > 0 )
			{
				if($lineWidth ne 'null'){
					$sequences=$sequences.$seq;
				}else{
					$sequences=$sequences."\n".$seq;
				}
			}
			else
			{
				$sequences=$seq;
			}
		}
	}
}

print report ">$gene\n";
if($lineWidth ne 'null'){
	my $idx;
	for($idx=0;$idx+$lineWidth-1 <= length($sequences)-1;$idx+=$lineWidth){
		print report substr($sequences,$idx,$lineWidth)."\n";
	}
	print report substr($sequences,$idx)."\n" if $idx <= length($sequences)-1;
}else{
	print report $sequences."\n";
}

@keys = keys(%notFindOfId);
if(@keys > 0)
{
	print "the following chromosome not found:\n";
	foreach $key (keys %notFindOfId)
	{ 
		print "$key\n";
	}
}

close CGFF;
close report;

sub trim {
    my $str=shift;
    $str =~ s/\s+$//g;
    $str =~ s/^\s+//g;
    return $str;
}
