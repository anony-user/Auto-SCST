#!/usr/bin/perl 
use Bio::DB::Fasta;

my $usage = "Usage: MastCompareWrapper.pl <targetMast> <backgroundMast> <backgroundContainingTarget(0/1)> <rackjJARpath>\n";
my $targetMast = shift or die $usage;
my $backgroundMast = shift or die $usage;
my $containingTarget = shift;
my $rackjJarPath = shift or die $usage;

my $targetNum;
my $backgroundNum;

open(FILE,"<$targetMast");
$_=<FILE>;
s/^\s+|\s+$//g;
my ($targetNum) = /Number_of_sequence:(\d+)/;
close(FILE);

open(FILE,"<$backgroundMast");
$_=<FILE>;
s/^\s+|\s+$//g;
my ($backgroundNum) = /Number_of_sequence:(\d+)/;
close(FILE);

die "Input MAST files not wrapped.\n" if !defined $targetNum || !defined $backgroundNum;

my $cmd = "MastCompare.pl $targetMast $backgroundMast $targetNum $backgroundNum $containingTarget $rackjJarPath";
exec($cmd) or die "Cannot execute MastCompareWapper.\n";
