#!/usr/bin/perl

# assuming input join file contains no ends been used more than once

use Bio::DB::Fasta;

my $minimumN = 2;
my $joinThreshold = 5;

my @joinSrc;

# Retrieve parameter
my @arg_idx=(0..@ARGV-1);
for my $i (0..@ARGV-1) {
	if ($ARGV[$i] eq '-join') {
		my $joinFile = $ARGV[$i+1];
		my $avgGapLength = $ARGV[$i+2];
		push @joinSrc, [$joinFile,$avgGapLength];
		delete @arg_idx[$i,$i+1,$i+2];
	}elsif($ARGV[$i] eq '-min') {
		$joinThreshold = $ARGV[$i+1];
		delete @arg_idx[$i,$i+1];
	}
}
my @new_arg;
for (@arg_idx) { push(@new_arg,$ARGV[$_]) if (defined($_)); }
@ARGV=@new_arg;

my $usage = "Usage: PairJointer.pl [-min <minPair>] [-join <joinFile> <avgGapSize>]+ <contigFasta> <outPrefix>\n";
my $fastaFile = shift or die $usage;
my $outPrefix = shift or die $usage;

# parameter check
die $usage if @joinSrc<1;


# get sequence lengths of original sequences
my %seqLength=();	# record lengths of host sequences
my %location=();	# $location{inputSeq}=(host,start,stop,strand);

my $nullpath = File::Spec->devnull();
open(CPERR, ">&STDERR");
open(STDERR,">$nullpath");
my $stream  = Bio::DB::Fasta->new($fastaFile)->get_PrimarySeq_stream;
while (my $seq = $stream->next_seq) {
    my $id  = $seq->id;
    my $length  = $seq->length;
    $seqLength{$id}=$length;
    push @{$location{$id}}, $id,1,$length,"+";
}
open(STDERR, ">&CPERR");


# join
for(my $idx1=0;$idx1<@joinSrc;$idx1++){
	my ($joinFile,$avgGapLength) = @{$joinSrc[$idx1]};
	
	# read join file
	my $joinHashRef = readJoinFile($joinFile);
	
	# translate join records
	my $translatedJoinHashRef = translateJoin($joinHashRef,$avgGapLength);
	
	# decompost into lists
	my ($decomposeRes,$decomposeRetRef) = decompose($translatedJoinHashRef);

	if($decomposeRes){ # contain circle, STOP
		my $nodeSetRef = $decomposeRetRef;
		print "contain circle\n";
		for my $node (keys %{$nodeSetRef}){
			print " $node"
		}
		print "\n";
		exit 0;
	}else{ # contain no circle
		my $componentListsRef = $decomposeRetRef;
	        for(my $idx2=0;$idx2<@{$componentListsRef};$idx2++){
	        	my $listLen = 0;
	                for(my $idx3=0;$idx3<@{$$componentListsRef[$idx2]};$idx3++){
	                        $listLen += $seqLength{${$$componentListsRef[$idx2]}[$idx3]};
	                }
	        }
	}
	
	# join lists
	joinLists($decomposeRetRef,$translatedJoinHashRef,$avgGapLength);

}

# output host info
my %hostScaffoldMap;
my $scId = 1;
open(FILE, ">$outPrefix\.host");
for $host (sort keys %seqLength){
	my $scName = "scaffold_$scId";
	$scId++;
	$hostScaffoldMap{$host}=$scName;

	print FILE "HOST: $scName LEN: $seqLength{$host}\n";
	for $seq (sort {$location{$a}[1] <=> $location{$b}[1] || 
			$location{$a}[2] <=> $location{$b}[2] || 
			$a cmp $b} keys %location){
		print FILE "$seq(".join(",",@{$location{$seq}}[1,2,3]).") " if $location{$seq}[0] eq $host;
	}
	print FILE "\n";
}
close FILE;

# output scaffold
my $fastaDB = Bio::DB::Fasta->new($fastaFile);
open(FILE, ">$outPrefix\.fasta");
for $host (sort keys %seqLength){
	my $scName = $hostScaffoldMap{$host};
	my $seq = "";

	for $sourceID (sort {$location{$a}[1] <=> $location{$b}[1] || 
			$location{$a}[2] <=> $location{$b}[2] || 
			$a cmp $b} keys %location){
		next if $location{$sourceID}[0] ne $host;
		
		my ($start,$stop,$strand) = @{$location{$sourceID}}[1,2,3];
		
		# insert N's
		$seq .= ("N"x($start-length($seq)-1));

		# append sequence
		my $sourceSeqLen = $fastaDB->get_Seq_by_id($sourceID)->length;
		if($strand eq "+"){
			$seq .= uc($fastaDB->seq($sourceID, 1 => $sourceSeqLen));
		}else{
			$seq .= uc($fastaDB->seq($sourceID, $sourceSeqLen => 1));
		}
	}
	
	print FILE ">$scName\n";
	for(my $idx=0;$idx<length($seq);$idx+=60){
		print FILE substr($seq,$idx,60)."\n";
	}
}
close FILE;

############ subrutines

sub printHosts {

	my $fileHandle;
	
	if(@_){
		$fileHandle = shift;
	}else{
		$fileHandle = STDIN;
	}

	for $host (sort keys %seqLength){
		print $fileHandle "HOST: $host LEN: $seqLength{$host}\n";
		for $seq (sort {$location{$a}[1] <=> $location{$b}[1] || 
				$location{$a}[2] <=> $location{$b}[2] || 
				$a cmp $b} keys %location){
			print $fileHandle "$seq(".join(",",@{$location{$seq}}[1,2,3]).") " if $location{$seq}[0] eq $host;
		}
		print $fileHandle "\n";
	}
}

sub joinLists {
	my $componentListsRef = shift;
	my $joinHashRef = shift;
	my $avgGapLength = shift;

	# for every list, build a new host named by first node
        for(my $idx2=0;$idx2<@{$componentListsRef};$idx2++){
        
        	# initial
        	my $newHostID = ${$$componentListsRef[$idx2]}[0]; # first node as new host ID
        	my $newHostLen = 0;
        
        	# first node
        	my $node = ${$$componentListsRef[$idx2]}[0];
        	my $edge = ${$$componentListsRef[$idx2]}[1];
        	my $connectingEnd = findConnectingEnd($node,$edge);

        	my $reverseFlag;
        	if($connectingEnd eq "3"){
        		$reverseFlag = 0;
        	}elsif($connectingEnd eq "5"){
        		$reverseFlag = 1;
        	}
        	
        	updateHost($node,$newHostID,$newHostLen,$reverseFlag);
        	$newHostLen += $seqLength{$node};
        	
        	# rest nodes
                for(my $idx3=2;$idx3<@{$$componentListsRef[$idx2]};$idx3+=2){
                	$node = ${$$componentListsRef[$idx2]}[$idx3];
                	$edge = ${$$componentListsRef[$idx2]}[$idx3-1];
                	$connectingEnd = findConnectingEnd($node,$edge);
                	
                	# edge
			$newHostLen += computeNs($edge,$avgGapLength,$joinHashRef);
                	
                	# node

                	undef $reverseFlag;
                	if($connectingEnd eq "3"){
                		$reverseFlag = 1;
			}elsif($connectingEnd eq "5"){
				$reverseFlag = 0;
			}
			
			updateHost($node,$newHostID,$newHostLen,$reverseFlag);
			$newHostLen += $seqLength{$node};
                }
                
                # update %seqLength: remove nodes from %seqLength and add the new one
                for(my $idx3=0;$idx3<@{$$componentListsRef[$idx2]};$idx3+=2){
                	$node = ${$$componentListsRef[$idx2]}[$idx3];
                	delete $seqLength{$node};
                }
                $seqLength{$newHostID} = $newHostLen;
        }
}

# output host

# output scaffold

sub computeNs { # may be changed by someday
	my $edge = shift;
	
	my $avgGapLength = shift;
	my $joinHashRef = shift;
	
	my $n1=0;
	my $n2=0;
	for(my $idx=0;$idx<@{$joinHashRef->{$edge}};$idx++){
		$n1 += ${$joinHashRef->{$edge}[$idx]}[0];
		$n2 += ${$joinHashRef->{$edge}[$idx]}[1];
	}
	$n1 = $n1 / @{$joinHashRef->{$edge}};
	$n2 = $n2 / @{$joinHashRef->{$edge}};
	my $ans = sprintf("%.0f",$avgGapLength - $n1 - $n2);
	
	$ans = $minimumN if $ans<$minimumN;

	return $ans;
}

# to update %location
sub updateHost {
	my $host = shift;
	my $newHost = shift;
	my $lastEnd = shift;
	my $reverseFlag = shift;
	
	if($reverseFlag){ # reverse
		for $key (keys %location){
			my ($oldHost,$oldStart,$oldStop,$strand) = @{$location{$key}};
			next if $oldHost ne $host;
			
			my $hostLen = $seqLength{$oldHost};
			my $start = $lastEnd + ($hostLen-$oldStop+1);
			my $stop = $lastEnd + ($hostLen-$oldStart+1);
			
			if($strand eq "+"){
				$strand = "-";
			}else{
				$strand = "+";
			}
			
			@{$location{$key}} = ($newHost,$start,$stop,$strand);
		}
	}else{ # no reverse
		for $key (keys %location){
			my ($oldHost,$start,$stop,$strand) = @{$location{$key}};
			next if $oldHost ne $host;

			$start += $lastEnd;
			$stop += $lastEnd;
			
			@{$location{$key}} = ($newHost,$start,$stop,$strand);
		}
	}
}

sub findConnectingEnd {
	my $node = shift;
	my $edge = shift;
	
	my (@id,@end);
	($id[0],$id[1],$end[0],$end[1])=split(/:/,$edge);
	
	for(my $i=0;$i<@id;$i++){
		return $end[$i] if $id[$i] eq $node;
	}
	
	return "";
}

sub translateJoin {
        my $joinHashRef = shift;
        my $maxGap = shift;

	my %translatedJoinHash = ();
	for my $keyStr (keys %{$joinHashRef}){
		my (@id,@end,@newId,@newEnd,@endGap);
		($id[0],$id[1],$end[0],$end[1])=split(/:/,$keyStr);
		
		for(my $idx2=0;$idx2<2;$idx2++){
		        my ($hostID,$start,$stop,$strand) = @{$location{$id[$idx2]}};
		        my $hostLen = $seqLength{$hostID};
		        
		        $newId[$idx2] = $hostID;
		        if( ($end[$idx2] eq "5" && $strand eq "+") || ($end[$idx2] eq "3" && $strand eq "-") ){
		                $newEnd[$idx2] = "5";
		                $endGap[$idx2] = $start-1;
		        }else{
		                $newEnd[$idx2] = "3";
		                $endGap[$idx2] = $hostLen-$stop;
		        }
		}
		
		# skip if the two seq already in the same host
		next if $newId[0] eq $newId[1];

		my $newKeyStr = "$newId[0]:$newId[1]:$newEnd[0]:$newEnd[1]";
       		for(my $idx2=0;$idx2<@{$joinHashRef->{$keyStr}};$idx2++){
       			my $gap1 = ${$joinHashRef->{$keyStr}[$idx2]}[0]+$endGap[0];
       			my $gap2 = ${$joinHashRef->{$keyStr}[$idx2]}[1]+$endGap[1];
       			
       			push @{$translatedJoinHash{$newKeyStr}}, [$gap1, $gap2] if $gap1<=$maxGap && $gap2<=$maxGap;
       		}
	}
	
	return \%translatedJoinHash;
}

sub printJoin {
        my $joinHashRef = shift;

	for my $keyStr (sort keys %{$joinHashRef}){
	        print "$keyStr = ";
       		for(my $idx2=0;$idx2<@{$joinHashRef->{$keyStr}};$idx2++){
       			print "\t${$joinHashRef->{$keyStr}[$idx2]}[0]:${$joinHashRef->{$keyStr}[$idx2]}[1]"
       		}
       		print "\n";
	}
}

sub readJoinFile {
	my $filename = shift;
	
	my %hash;
	
	open(FILE,"<$filename");
	while(<FILE>){
		next if /^#/;
		
		my @t=split(/\t/,$_);
		
		next if $t[1] < $joinThreshold;
		
		my @locPairStr = split(/[\[\],\s]+/,$t[2]);
		shift @locPairStr; #remove first
		for my $tmpStr (@locPairStr){
			my ($pos1,$pos2) = split(/:/,$tmpStr);
			push @{$hash{$t[0]}}, [$pos1,$pos2];
		}
	}
	close FILE;
	
	return \%hash;
}

# return two values: 1. containCircle and 2. a hash of nodes (a component) if a circle
# return an array of lists if no circle
sub decompose {
	my $joinHashRef = shift;
	
	# build node-neighbors map
	my %nodeNeighborEdge;
	my %nodes;
	for my $keyStr (sort keys %{$joinHashRef}){
		my (@id,@end);
		($id[0],$id[1],$end[0],$end[1])=split(/:/,$keyStr);
		$nodeNeighborEdge{$id[0]}{$id[1]}=$keyStr;
		$nodeNeighborEdge{$id[1]}{$id[0]}=$keyStr;
		$nodes{$id[0]}=1;
		$nodes{$id[1]}=1;
	}

	# compute components
	my %visitedEdges;
	my @components;
	while((keys %nodes)>0){ # when there are some nodes unvisited
	        my %nodeSet;
	        my @queue;
	        $queue[0] = (keys %nodes)[0]; # pick one
	        delete $nodes{$queue[0]};
	        $nodeSet{$queue[0]}=1;
	        
	        while(@queue>0){
	                my $n0 = shift @queue; # for every first node in queue
	                
	                # for every neighbor connected by an unvisited edge
	                for my $n1 (keys %{$nodeNeighborEdge{$n0}}){
	                        next if exists $visitedEdges{$nodeNeighborEdge{$n0}{$n1}};
	                        if(not exists $nodes{$n1}){ # unvisited edge connect a visited node
	                                return 1,\%nodeSet;
	                        }
	                        
	                        push @queue, $n1;
	                        delete $nodes{$n1}; # set node visited
	                        $nodeSet{$n1}=1;
	                        $visitedEdges{$nodeNeighborEdge{$n0}{$n1}} = 1; # set edge visited
	                }
	        }
	        push @components, \%nodeSet;
	}

	# make components into lists
	my @componentLists;
	for(my $idx=0;$idx<@components;$idx++){
	        my $n0;
	        # fine a node with degree 1
	        for $node (keys %{$components[$idx]}){
	                my $degree = (keys %{$nodeNeighborEdge{$node}});
	                $n0 = $node;
	                last if (keys %{$nodeNeighborEdge{$node}})==1;
	        }
	        
	        # generate list
	        my @list;
	        my $lastNode = "";
	        my $lastEdge = "";
                while($n0 ne ""){
                	push @list,"$lastEdge" if $lastEdge ne "";
                        push @list, $n0;
                        my $nextNode;
                        for my $node (keys %{$nodeNeighborEdge{$n0}}){
                                $nextNode = $node;
                                last if $nextNode ne $lastNode;
                        }
                        $nextNode = "" if $nextNode eq $lastNode;
                        
                        $lastEdge = $nodeNeighborEdge{$n0}{$nextNode};
                        $lastNode = $n0;
                        $n0 = $nextNode;
                }
                
                $componentLists[$idx] = \@list;
	}
	
	return 0,\@componentLists;
}
