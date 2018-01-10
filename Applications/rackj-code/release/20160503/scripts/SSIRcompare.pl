#!/usr/bin/perl

my $usage="SSIRcompare.pl [-byDepth] [-appendCoverRatio] [-delta <delta>] <outputFilename> <intronPrefix_1> <exonPrefix_1> [<intronPrefix_i> <exonPrefix_i>]+\n";

my $delta = 0.1;
my $byDepth = 0;
my $appendCoverRatio = 0;

#Retrieve parameter
my @arg_idx=(0..@ARGV-1);
for my $i (0..@ARGV-1) {
    if ($ARGV[$i] eq '-byDepth') {
        $byDepth = 1;
        delete $arg_idx[$i];
    }elsif ($ARGV[$i] eq '-appendCoverRatio') {
        $appendCoverRatio = 1;
        delete $arg_idx[$i];
    }elsif ($ARGV[$i] eq '-delta') {
        $delta=$ARGV[$i+1];
        delete @arg_idx[$i,$i+1];
    }
}
my @new_arg;
for (@arg_idx) { push(@new_arg,$ARGV[$_]) if (defined($_)); }
@ARGV=@new_arg;

die $usage if @ARGV < 5;

my $OutputFilename = shift;
my $numSamples = @ARGV / 2;
my @intronPrefix = ();
my @exonPrefix   = ();
for(my $idx=0;$idx<@ARGV;$idx+=2){
	push @intronPrefix, $ARGV[$idx];
	push @exonPrefix, $ARGV[$idx+1];
}

#my $cmd="SSIRcompare_ex.pl $OutputFilename $paraLineA $exonPrefix[0] $paraLineB $exonPrefix[1]";
my $cmd = "SSIRcompare_ex.pl ";
$cmd .= "-byDepth " if $byDepth;
$cmd .= "-appendCoverRatio " if $appendCoverRatio;
$cmd .= "-delta $delta ";
$cmd .= "$OutputFilename";

for(my $idx=0;$idx<@intronPrefix;$idx++){
  my @parameters;
  if(not $byDepth){ # data from ExonCounter (by or not by RPKMComputer)
  	if(not $appendCoverRatio){ # without additional covered ratios
  		push @parameters, "$intronPrefix[$idx].intronCount",
  				"$exonPrefix[$idx].exonCount",
  				"$exonPrefix[$idx].geneRPKM",
  				"$exonPrefix[$idx].spliceCount",
  				$exonPrefix[$idx];
	}else{ # with additional covered ratios from intronExonAvgDepth.pl
  		push @parameters, "$intronPrefix[$idx].intronCount",
  				"$intronPrefix[$idx].depth.intronCount",
  				"$exonPrefix[$idx].exonCount",
  				"$exonPrefix[$idx].geneRPKM",
  				"$exonPrefix[$idx].spliceCount",
  				$exonPrefix[$idx];
	}
  }else{ # data from GeneCoverageArray -> intronExonAvgDepth.pl
    push @parameters, "$intronPrefix[$idx].depth.intronCount",
    			"$exonPrefix[$idx].depth.exonCount",
    			"$exonPrefix[$idx].geneRPKM",
    			"$exonPrefix[$idx].spliceCount",
    			$exonPrefix[$idx];
  }
  $cmd .= " ";
  $cmd .= join(" ",@parameters);
}

#print "$cmd\n";
system($cmd);
