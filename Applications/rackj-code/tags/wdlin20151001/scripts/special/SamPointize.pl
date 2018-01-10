#!/usr/bin/perl -w

# authors: Peter Tsai & Wendar Lin. 20120522

use Getopt::Std;
my %opts = (s=>15);
getopts('s:', \%opts);
my $usage = "Usage: SamPointize.pl <-s pointlize position> SAM Files";
die $usage if @ARGV==0;

while(<>) {
	if (/^@/) {  # sam header
		print;
	}else{	   # sam record
		my @s	  = split;
		my $CIGAR  = $s[5];
		my $Tstart = $s[3];
		my $Tend = $Tstart-1;
		my $strand = "+";
		$strand = "-" if $s[1] & 16;

		# Q: query side position
		# match: number of matches
		# Tshift: target side shift
		my ($Q,$match,$Tshift)=(0,0,0);
		my @CC = $CIGAR =~ /\d+[M|I|H|S|D]/g;
		
		my $flag = 0; # got position or not
		for $i (0 ... @CC-1) {
			$i = @CC - $i-1 if $strand eq "-";
			my $nOP = $CC[$i];
			my ($Csize,$op) = $nOP =~ /(\d+)(.)/;
			if ($op =~ /[H|S|I]/) {
				$Q += $Csize if $match < $opts{s};
			}elsif ($op eq "M") {
				if ($match < $opts{s} && $match+$Csize >= $opts{s}) {
					$Tshift += $opts{s} - $match;
					$Q += $opts{s} - $match;
					$flag = 1;
				}elsif ($match<$opts{s}){
					$Tshift += $Csize;
					$Q += $Csize;
				}
				$match += $Csize;
				$Tend += $Csize;
			}elsif ($op eq "D") {
				$Tshift += $Csize if $match<$opts{s};
				$Tend += $Csize;
			}
		}
		
		$s[5] = "1M";
		if ($strand eq "+") {
			$s[3] = $Tstart + $Tshift - 1;
			$s[9] = substr($s[9],$Q-1,1);
			$s[10] = substr($s[10],$Q-1,1) if $s[10] ne "*";
		}elsif ($strand eq "-") {
			$s[3] = $Tend +1 - $Tshift;
			$s[9] = substr($s[9],0-$Q,1);
			$s[10] = substr($s[10],0-$Q,1) if $s[10] ne "*";
		}
		
		print "$s[0]\t$s[1]\t$s[2]\t$s[3]\t$s[4]\t$s[5]\t$s[6]\t$s[7]\t$s[8]\t$s[9]\t$s[10]\n";
	}
}
