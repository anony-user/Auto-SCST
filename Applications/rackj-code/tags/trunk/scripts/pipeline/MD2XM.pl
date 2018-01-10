#!/usr/bin/perl -w

my $debug=0;

while (<>) {
	if (/^\@/) {  # SAM header lines
		# do nothing
	}else{
		chomp;
		my @t=split(/\t/,$_);
		
		my $computedXM;
		my $gotXM=0;
		for(my $idx=11;$idx<@t;$idx++){
			if($t[$idx]=~/^MD:Z:(.+)/){ # if a MD tag
				my $mdStr = $1;
				my $mismatch = 0;
				while($mdStr=~/([ACGTacgt]+)/g){
					my $prechr;
					$prechr = substr($mdStr,$-[0]-1,1) if $-[0]>=1;
					$mismatch+=length($1) if $prechr ne "^";
					
					print "$prechr $1\n" if $debug;
				}
				$computedXM = "XM:i:$mismatch";
			}
			if($t[$idx]=~/^XM:i:\d+$/){ # if a XM tag
				$gotXM=1;
			}
		}
		
		if( (not $gotXM) && (defined $computedXM) ){
			push @t,$computedXM;
		}
		print join("\t",@t)."\n";
	}
}
