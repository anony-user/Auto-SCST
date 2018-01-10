#!/usr/bin/perl -w
use Bio::SeqIO;
my $minLength=1;

my $usage = "Usage: fixFasta.pl [-min minimum sequence length] <inFasta> <outFasta>\n";

my @arg_idx=(0..@ARGV-1);
for my $i (0..@ARGV-1) {
  if ($ARGV[$i] eq '-min') {
    $minLength=$ARGV[$i+1];
    delete @arg_idx[$i,$i+1];
  }
}
my @new_arg;
for (@arg_idx) { push(@new_arg,$ARGV[$_]) if (defined($_)); }
@ARGV=@new_arg;

my $inFilename = shift or die $usage;
my $outFilename = shift or die $usage;

my $in  = Bio::SeqIO->new(-file => "$inFilename",-format => 'Fasta');
my $out = Bio::SeqIO->new(-file => ">$outFilename",-format => 'Fasta');

while ( my $seq = $in->next_seq() ) {
  next unless $seq->length() >= $minLength;
  $out->write_seq($seq);
}
