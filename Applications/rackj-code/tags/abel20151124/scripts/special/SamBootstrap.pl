#!/usr/bin/perl -w

my $LEVEL_ALIGNMENT=0;
my $LEVEL_READ=1;
my $LEVEL_PAIR=2;

my $idTrimLen=0; #
my $level=$LEVEL_ALIGNMENT; # default level
my $rndSeed;

# Retrieve parameter
my @arg_idx=(0..@ARGV-1);
for my $i (0..@ARGV-1) {
	if ($ARGV[$i] eq '-alignment') {
		$level=$LEVEL_ALIGNMENT;
		delete $arg_idx[$i];
	}elsif ($ARGV[$i] eq '-read') {
		$level=$LEVEL_READ;
		delete $arg_idx[$i];
	}elsif ($ARGV[$i] eq '-pair') {
		$level=$LEVEL_PAIR;
		$idTrimLen = $ARGV[$i+1];
		delete @arg_idx[$i,$i+1];
	}elsif ($ARGV[$i] eq '-seed') {
		$rndSeed=$ARGV[$i+1];
		delete @arg_idx[$i,$i+1];
	}
}
my @new_arg;
for (@arg_idx) { push(@new_arg,$ARGV[$_]) if (defined($_)); }
@ARGV=@new_arg;

my $usage = "Usage: SamBootstrap.pl [-alignment] [-read] [-pair <x>] [-seed <y>] <fraction>\n";
my $fraction = shift or die $usage;

# set random number seed
if(defined $rndSeed){
	srand($rndSeed);
}

# LEVEL: alignment
if($level==$LEVEL_ALIGNMENT){
	while(<>){
		if(/^@/){ # header
			print;
		}else{
			print if rand(1)<$fraction;
		}
	}
}

# LEVEL: read
if($level==$LEVEL_READ){
	my $flag;
	my $lastID="";
	while(<>){
		if(/^@/){ # header
			print;
		}else{
			@t=split;
			# make decisiion if new read
			if($t[0] ne $lastID){
				if(rand(1)<$fraction){
					$flag=1;
				}else{
					$flag=0;
				}
				$lastID=$t[0];
			}
			print if $flag;
		}
	}
}

# LEVEL: pair
if($level==$LEVEL_PAIR){
	my $flag;
	my $lastTrimID="";
	while(<>){
		if(/^@/){ # header
			print;
		}else{
			@t=split;
			# make decisiion if new pair
			if(substr($t[0],0,length($t[0])-$idTrimLen) ne $lastTrimID){
				if(rand(1)<$fraction){
					$flag=1;
				}else{
					$flag=0;
				}
				$lastTrimID=substr($t[0],0,length($t[0])-$idTrimLen);
			}
			print if $flag;
		}
	}
}
