#!/usr/bin/perl

my $usage="SSEScompare.pl <outputFilename> <readLength> <prefix_1> [<prefix_i>]+\n";
die $usage if @ARGV<4;

my $OutputFilename = shift;
my $readLength = shift;

my @parameterArray = ();
while(@ARGV>0){
	my $prefix = shift;
	my @parameters = ("$prefix.spliceCount","$prefix.geneRPKM",$prefix);
	push @parameterArray, @parameters;
}

my $cmd="SSEScompare_ex.pl $OutputFilename $readLength ".join(" ",@parameterArray);
#print "$cmd\n";
system($cmd);