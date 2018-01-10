#!/usr/bin/perl

my $usage = "Usage: GFF3checker.pl\n";

my $debug = 0;

my $modeREDUND = 1; # check redundancy
my $modeRESORT = 2; # resort GFF3
my $modeREMOVE = 3; # remove records with roots in the given list
my $modeMERGE  = 4; # merge

my $mode = $modeREDUND; # default mode
my $redundTh = 0.5;     # redundancy threshold
my $removeFile;
my $mergeFile;
my $idAttr = "ID";

my @parentAttrs = ("Parent","Derives_from");
my $parentAttrsAssigned = 0;

my @features = ("gene");
my $featuresAssigned = 0;

#Retrieve parameter
my @arg_idx=(0..@ARGV-1);
for my $i (0..@ARGV-1) {
	if ($ARGV[$i] eq '-redundancy') {
		$mode = $modeREDUND;
		delete $arg_idx[$i];
	}elsif ($ARGV[$i] eq '-redundTh') {
		$redundTh = $ARGV[$i+1];
		delete @arg_idx[$i,$i+1];
	}elsif ($ARGV[$i] eq '-resort') {
		$mode = $modeRESORT;
		delete $arg_idx[$i];
	}elsif ($ARGV[$i] eq '-remove') {
		$mode = $modeREMOVE;
		$removeFile = $ARGV[$i+1];
		delete @arg_idx[$i,$i+1];
	}elsif ($ARGV[$i] eq '-merge') {
		$mode = $modeMERGE;
		$mergeFile = $ARGV[$i+1];
		delete @arg_idx[$i,$i+1];
	}elsif ($ARGV[$i] eq '-idAttr') {
		$idAttr = $ARGV[$i+1];
		delete @arg_idx[$i,$i+1];
	}elsif ($ARGV[$i] eq '-parentAttr') {
		if(not $parentAttrsAssigned){
			@parentAttrs = ();
			$parentAttrsAssigned = 1;
		}
		push @parentAttrs, $ARGV[$i+1];
		delete @arg_idx[$i,$i+1];
	}elsif ($ARGV[$i] eq '-feature') {
		if(not $featuresAssigned){
			@features = ();
			$featuresAssigned = 1;
		}
		push @features, $ARGV[$i+1];
		delete @arg_idx[$i,$i+1];
	}
}

my @new_arg;
for (@arg_idx) { push(@new_arg,$ARGV[$_]) if (defined($_)); }
@ARGV=@new_arg;

my %chrStrandRegions;	# chr+strand -> intervals
my %parentMap;		# record -> parent ID
my %idRec;		# ID -> record
my %mergeKey;		# for modeMERGE, will merge keys into the value

# read inputs
while(<>){
	chomp;
	next if /^#/;  # skip comment lines

	$_.=";";
	my @t=split;

	# get ID
	my ($ID)=$t[8]=~/$idAttr=(.+?);/;
	# get parent
	my $parent;
	for(my $idx=0;$idx<@parentAttrs;$idx++){
		my $parentAttr = $parentAttrs[$idx];
		if($t[8]=~/$parentAttr=(.+?);/){
			$parent = $1;
			last;
		}
	}
	$parent=$ID if not defined $parent;
	$parentMap{$_}=$parent;

	# ID-record map
	$idRec{$ID}=$_ if defined $ID;

	if($mode == $modeREDUND){ # mode REDUND
		
		my $flag=0;
		for(my $idx=0;$idx<@features;$idx++){
			$flag=1 if $t[2] eq $features[$idx];
		}
		if($flag){ # if this record is of an assigned feature (type)
			push @{$chrStrandRegions{"$t[0] $t[6]"}}, [$t[3], $t[4]];
		}

	}
}

# mode REDUND
if($mode == $modeREDUND){

	# merge overlap gene intervals
	for $key (keys %chrStrandRegions){ # for every chr-strand pair
		@{$chrStrandRegions{$key}} = overlappingMerge( \@{$chrStrandRegions{$key}}, $redundTh );
	}

	# collect IDs in those intervals
	my %redundantIdDigest;
	my %redundancy;
	for $ID (keys %idRec){
		next if $ID ne $parentMap{$idRec{$ID}}; # skip non-root IDs
		my $rec = $idRec{$ID};
		my @t = split(/\s+/,$rec);
		my @query = ($t[3],$t[4]);
		my $queryLen = overlapLen( \@query,\@query );
		for $interval (@{$chrStrandRegions{"$t[0] $t[6]"}}){ # check intervals of the same chr-strand
			if( overlapLen(\@query, $interval) >= $queryLen ){         # if contain
				$redundantIdDigest{$ID} = "";
				push @{$redundancy{join("_",($t[0],$$interval[0],$$interval[1]),$t[6])}}, $ID;
				last;
			}
		}
	}
	
	# form digests of records of redundant IDs
	for $rec (sort {(getRootID($a) cmp getRootID($b)) || featureSort($a,$b)} keys %parentMap){
		my $rootID = getRootID($rec);
		if(exists $redundantIdDigest{$rootID}){
			my @token = split(/\s+/,$rec);
			$redundantIdDigest{$rootID} .= " : " if length($redundantIdDigest{$rootID})>0;
			$redundantIdDigest{$rootID} .= join("_",@token[2..4]);
		}
	}
	
	# compare digests of redundant IDs for each key
	for $key (sort keys %redundancy){
		next if @{$redundancy{$key}}<=1; # skip non-redundant keys
		
		print "$key\t";
		my @redundIDs = sort { ($redundantIdDigest{$a} cmp $redundantIdDigest{$b}) || ($a cmp $b) } @{$redundancy{$key}};
		for(my $idx=0; $idx<@redundIDs ; $idx++){
			if($idx>0){
				if($redundantIdDigest{$redundIDs[$idx-1]} cmp $redundantIdDigest{$redundIDs[$idx]}){
					print " ! ";
				}else{
					print " = ";
				}
			}
			print $redundIDs[$idx];
		}
		print "\n";
	}

}elsif($mode == $modeRESORT){ # mode RESORT

	for $rec (sort {(getRootID($a) cmp getRootID($b)) || featureSort($a,$b)} keys %parentMap){
		print "$rec\n";
	}

}elsif($mode == $modeREMOVE){ # mode REMOVE

	# get remove list
	my %remove;
	open(FILE,"<$removeFile");
	while($line=<FILE>){
		chomp $line;
		$remove{$line}=1;
	}
	close FILE;
	
	# output a record if its root not in the remove list
	for $rec (sort {(getRootID($a) cmp getRootID($b)) || featureSort($a,$b)} keys %parentMap){
		print "$rec\n" if not exists $remove{getRootID($rec)};
	}

}elsif($mode == $modeMERGE){ # mode MERGE

	# get merge key
	open(FILE,"<$mergeFile");
	while($line=<FILE>){
		@t=split(/\s+/,$line);
		my $idx=0;
		my $mainKey;
		for $token (@t){
			# if this token is an ID and itself is a root
			if( (exists $idRec{$token}) && ($token eq getRootID($idRec{$token})) ){
				if($idx==0){ # first such token in this line
					$mainKey = $token;
				}
				$mergeKey{$token} = $mainKey;
				$idx++;
			}
		}
	}
	close FILE;

	my %root2ndDigest;
	# form digests of 2nd root related with merge
	for $rec (sort { mergeKeySort(getRootID($a),getRootID($b)) || 
			 (getRootID($a) cmp getRootID($b)) || 
			 featureSort($a,$b)
		       } keys %parentMap){
		# if related with merge and have 2nd root
		if( (exists $mergeKey{getRootID($rec)}) && (defined get2ndRootRec($rec)) ){
			my $root2nd = get2ndRootRec($rec);
			my @t = split(/\s+/,$rec);
			$root2ndDigest{$root2nd} .= " : " if length($root2ndDigest{$root2nd})>0;
			$root2ndDigest{$root2nd} .= join("_",@t[2..4]);
		}
	}
	
	# compute 2nd-root to be removed
	# based on root2ndDigest & getRootID & mergeKey
	my %root2ndRemove;
	my $lastRec;
	for $rec (sort { (getRootID($a) cmp getRootID($b)) ||
			 ($root2ndDigest{$a} cmp $root2ndDigest{$b}) ||
			 (mergeKeySort(getRootID($a),getRootID($b))) } keys %root2ndDigest){
		# if the same merge key and equal to last one
		if( defined $lastRec ){
			if( ($mergeKey{$getRootID{$lastRec}} eq $mergeKey{$getRootID{$rec}}) &&
			    ($root2ndDigest{$lastRec} eq $root2ndDigest{$rec}) ){
				$root2ndRemove{$rec} = 1;
			}
		}
		$lastRec = $rec;
	}

	# for every record, output if necessary
	for $rec (sort { mergeKeySort(getRootID($a),getRootID($b)) || 
			 (getRootID($a) cmp getRootID($b)) || 
			 featureSort($a,$b)
		       } keys %parentMap){
		       
		my $rootID = getRootID($rec);
		print "rootID: $rootID\n" if $debug;
		print "mergeKEY: $mergeKey{$rootID}\n" if $debug;

		# if related with merge
		if( exists $mergeKey{$rootID} ){

			print "merge related: $rootID => $mergeKey{$rootID}\n" if $debug;

			# a root record
			if( $rec eq $idRec{$rootID} ){
				print "merge related ROOT: $rootID => $mergeKey{$rootID}\n" if $debug;
				# only care merge key
				if( $rootID eq $mergeKey{$rootID} ){
					print "merge related ROOT KEY: $rootID => $mergeKey{$rootID}\n" if $debug;
					my @intervals;
					# for all root record with this merge key
					for my $testID (keys %mergeKey){
						next if $mergeKey{$testID} ne $rootID;
						my $testRec = $idRec{$testID};
						my @t=split(/\s+/,$testRec);
						push @intervals,[$t[3],$t[4]];
					}
					@intervals = mergeOverlap( \@intervals );
					
					# modify to merged range
					my @t=split(/\s+/,$rec);
					$t[3] = ${$intervals[0]}[0];
					$t[4] = ${$intervals[-1]}[1];
					print join("\t",@t)."\n";
				}
			}elsif( exists $root2ndRemove{get2ndRootRec($rec)} ){
				# skip
				print "merge related REMOVE: $rec\n" if $debug;
			}else{ # other record
				my @t=split(/\s+/,$rec);
				# get parent
				my $parent;
				my $parentAttr;
				for(my $idx=0;$idx<@parentAttrs;$idx++){
					$parentAttr = $parentAttrs[$idx];
					if($t[8]=~/$parentAttr=(.+?);/){
						$parent = $1;
						last;
					}
				}
				# if parent to be merged
				if( (exists $mergeKey{$parent}) && ($mergeKey{$parent} ne $parent) ){
					print "merge related other change: $rec\n" if $debug;
					# modify parent
					$t[8]=~s/$parentAttr=$parent;/$parentAttr=$mergeKey{$parent};/;
					print join("\t",@t)."\n";
				}else{
					print "merge related other\n" if $debug;
					print "$rec\n";
				}
			}
		}else{ # not related with merge
			print "$rec\n";
		}
	}

}

# get root ID from parentMap
sub getRootID {
	my $queryRec = shift;
	
	while( $parentMap{$queryRec} ne $parentMap{$idRec{$parentMap{$queryRec}}} ){
		$queryRec = $idRec{$parentMap{$queryRec}};
	}
	
	return $parentMap{$queryRec};
}

# return undef if 1st root, 2nd root otherwise
sub get2ndRootRec {
	my $queryRec = shift;
	
	my $ansRec;
	# if 1st root
	if($queryRec eq $idRec{$parentMap{$queryRec}}){
		return $ansRec;
	}
	
	# otherwise
	$ansRec=$queryRec;
	while( $parentMap{$ansRec} ne $parentMap{$idRec{$parentMap{$ansRec}}} ){
		$ansRec = $idRec{$parentMap{$ansRec}};
	}
	
	return $ansRec;
}

# compare two IDs based on mergeKey map
sub mergeKeySort {
	my $idA = shift;
	my $idB = shift;
	
	my $valueA = "";
	my $valueB = "";
	
	$valueA = $mergeKey{$idA} if exists $mergeKey{$idA};
	$valueB = $mergeKey{$idB} if exists $mergeKey{$idB};
	
	# suffixes for sorting merge key in front of others
	if($idA eq $valueA){
		$valueA .= " 0";
	}else{
		$valueA .= " 1";
	}
	
	if($idB eq $valueB){
		$valueB .= " 0";
	}else{
		$valueB .= " 1";
	}

	return $valueA cmp $valueB;
}

# compare two GFF3 lines
sub featureSort {
	my $recA = shift;
	my $recB = shift;
	
	my @tokenA = split(/\s+/,$recA);
	my @tokenB = split(/\s+/,$recB);
	
	my $featureA = featureCode($tokenA[2]);
	my $featureB = featureCode($tokenB[2]);
	
	return ($featureA <=> $featureB) || ($recA cmp $recB);
}

# modify if needed
sub featureCode {
	my $feature = shift;
	
	if($feature eq "gene"){
		return 1;
	}elsif($feature eq "mRNA"){
		return 2;
	}elsif($feature eq "exon"){
		return 3;
	}elsif($feature eq "five_prime_UTR"){
		return 4;
	}elsif($feature eq "CDS"){
		return 5;
	}elsif($feature eq "three_prime_UTR"){
		return 6;
	}else{
		return 7;
	}
}

# given an array of intervals, return an array of merged intervals
# of intervals overlapping each other geater than a certain threshold
# see mergeOverlap for the prototype
sub overlappingMerge {
	my @intervalSet = @{$_[0]};
	my $threshold = $_[1];
	
	# collect indexes of overlapping intervals
	my %overlappingIndexes;
	@intervalSet = sort { $a->[0] <=> $b->[0] || $a->[1] <=> $b->[1] } @intervalSet; 

	$cnt = @intervalSet;

	my $chkPoint = ${$intervalSet[0]}[1]; # stop point of the first interval
	for(my $idx=1; $idx<@intervalSet; $idx++){
		my $curr = $intervalSet[$idx];
		if($$curr[0] <= $chkPoint){ # if current interval intersects the merged interval
			$chkPoint = $$curr[1] if $$curr[1] > $chkPoint; # extend the merged interval if needed
			# save this interval, and last interval if it was not saved
			$overlappingIndexes{$idx-1}=1;
			$overlappingIndexes{$idx}=1;
		}else{
			$chkPoint = $$curr[1];
		}
	}
	
	# check these overlapping
	my %qualifiedIndexes;
	my @sortedOverlappingIndexes = sort {$a <=> $b} keys %overlappingIndexes;
	for my $idx0 (@sortedOverlappingIndexes){
		my $len0 = overlapLen( $intervalSet[$idx0],$intervalSet[$idx0] );
		for my $idx1 (@sortedOverlappingIndexes){
			next if $idx1 <= $idx0; # so that only $idx0<$idx1 computed
			my $len1 = overlapLen( $intervalSet[$idx1],$intervalSet[$idx1] );
			
			my $overlap = overlapLen( $intervalSet[$idx0],$intervalSet[$idx1] );
			if( $overlap >= $len0*$threshold || $overlap >= $len1*$threshold ){
				$qualifiedIndexes{$idx0} = 1;
				$qualifiedIndexes{$idx1} = 1;
			}
		}
	}
	
	# get intervals
	my @newSet;
	for $idx (keys %qualifiedIndexes){
		push @newSet, $intervalSet[$idx];
	}
	@newSet = mergeOverlap( \@newSet );

	return @newSet;
}

sub mergeOverlap {
	my @intervalSet = @{$_[0]};
	
	@intervalSet = sort { $a->[0] <=> $b->[0] || $a->[1] <=> $b->[1] } @intervalSet; 
	my @newSet=();
	my $prev = shift @intervalSet; # get first as the merged interval
	for my $curr (@intervalSet){
		if($$curr[0] <= $$prev[1]){ # if current interval intersects the merged interval
			$$prev[1] = $$curr[1] if $$curr[1] > $$prev[1]; # extend the merged interval if needed
		}else{
			push @newSet, [$$prev[0],$$prev[1]]; # save the merged interval if not intersecting
			$prev = $curr;	
		}
	}
	push @newSet, [$$prev[0],$$prev[1]] if defined $prev;
	
	return @newSet;
}

# return length of overlap
sub overlapLen {
	my @Ia = @{$_[0]};
	my @Ib = @{$_[1]};
	
	my $overlapStart = $Ia[0];
	$overlapStart = $Ib[0] if $Ib[0]>$Ia[0];

	my $overlapStop = $Ia[1];
	$overlapStop = $Ib[1] if $Ib[1]<$Ia[1];
	
	if($overlapStart > $overlapStop){
		return 0;
	}else{
		return $overlapStop-$overlapStart+1;
	}
}
