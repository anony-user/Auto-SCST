#!/usr/bin/perl 
use Bio::DB::Fasta;

no warnings qw(once);

my $usage = "MastWrapper.pl <MAST file> <fasta file>\n";
my $mast = shift or die $usage;
my $fasta = shift or die $usage;

# to redirect error/warning message
my $nullpath = File::Spec->devnull();
open(CPERR, ">&STDERR");
open(STDERR,">$nullpath");

my $db = Bio::DB::Fasta->new("$fasta");

my $num_seqIDs=0;
my $stream  = $db->get_PrimarySeq_stream;
while (my $seq = $stream->next_seq) {
	$num_seqIDs++;
}
# restore STDERR
open(STDERR, ">&CPERR");

print "#Number_of_sequence:$num_seqIDs\tformat:.mast\n";

open(FILE,"<$mast");
while(<FILE>) {
	s/^\s+|\s+$//g;
	if (/^# seq/) {
		my @t = split(/\s+/);
		shift @t;
		print "#".join("\t",@t)."\n";
	}elsif (/^#/) {
		print "$_\n";
	}else {
		my @t = split(/\s+/);
		print join("\t",@t)."\n";
	}
}
close(FILE);