#!/usr/bin/perl -w
use strict;
use IPC::Open2;
use Data::Dumper;
$|=1;

my $usage = "Usage: Interval2igv.pl <inFile> <rackjJARpath> <trackName>\n";
my $inFile = shift or die $usage;
my $jarPath = shift or die $usage;
my $trackName = shift or die $usage;

my %recordDeepHash;

open(igvIN,"<$inFile") or die "input file error\n";
my $pid = open2( *queryOUT, *queryIN,"java -classpath $jarPath special.IntervalQuery");
while(my $line = <igvIN>) {
	$line =~ s/^\s+|\s+$//g;
	my @t = split("\t",$line);
	my $itemID=$t[0];
	my $Chr=$t[1];
	my $start=$t[2];
	my $stop=$t[3];
	print queryIN "query\t$Chr\t$start\t$stop\n";
	my $msg = <queryOUT>;
	$msg =~ s/^\s+|\s+$//g;
	
		# if query is null
		#	insert this interval into java
		#	insert this item into perl hash with height 1
		# else
		#	if this interval is in java
		#		insert this item into perl hash
		#	else
		#		insert this interval into java
		#		insert this item into perl hash with one more height than other overlapping intervals
		
	if(length($msg)==0){ # no overlaps
		print queryIN "insert\t$Chr\t$start\t$stop\n";
		push( @{$recordDeepHash{$Chr}{$start}{$stop}{1}}, $itemID );
	}else{ # some overlaps
		my $hkey = 0; # for height
		my $isExist = 0;
		my @overlaps = split(",",$msg);
		foreach my $interval (@overlaps) {
			my @region = split("\t",$interval);
			if($start==$region[0] && $stop==$region[1]){ # some other item with exactly the same interval
				$hkey = (keys %{$recordDeepHash{$Chr}{$start}{$stop}})[0];
				$isExist = 1;
				last;
			}else{
				my $height = getHeight($Chr, \@region);
				$hkey = $height if($height>$hkey);
			}
		}
		
		if($isExist){
			push( @{$recordDeepHash{$Chr}{$start}{$stop}{$hkey}}, $itemID );
		}else{
			print queryIN "insert\t$Chr\t$start\t$stop\n";
			push( @{$recordDeepHash{$Chr}{$start}{$stop}{$hkey+1}}, $itemID );
		}
	}
}
close(igvIN);

# output
print "Chromosome\tStart\tEnd\tFeature\t$trackName\n";
for my $seqId ( sort keys %recordDeepHash ) {
	for my $igvStart ( sort {$a <=> $b} keys %{$recordDeepHash{$seqId}} ) {
		for my $igvEnd ( keys %{$recordDeepHash{$seqId}{$igvStart}} ) {
			for my $height ( keys %{$recordDeepHash{$seqId}{$igvStart}{$igvEnd}} ) {
				my $itemStr = join( ",", sort @{$recordDeepHash{$seqId}{$igvStart}{$igvEnd}{$height}} );
				print "$seqId\t".($igvStart-1)."\t$igvEnd\t$itemStr\t$height\n";			
			}
		}
	}
}

#----------------------
# subroutine
#----------------------
sub getHeight{
	my ($sid, $regions) = @_;
	return (keys %{$recordDeepHash{$sid}{$$regions[0]}{$$regions[1]}})[0];
}
