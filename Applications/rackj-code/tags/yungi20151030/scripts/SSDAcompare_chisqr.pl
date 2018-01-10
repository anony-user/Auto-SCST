#!/usr/bin/perl -w

my $usage = "Usage: SSDAcompare_chisqr.pl [-minSplice <minSpliceReads>] [-minDB <minDBmatch>] <outputFilename> <chiSqrTh> <strandCGFF> <fineSplice_1> <geneRPKM_1> <name_1> [<fineSplice_i> <geneRPKM_i> <name_i>]+\n";

my $debug=0;

#Retrieve parameter
my $minSplice=0;
my $minDB=0;
my $mirror=0;
my $minSplicing=0;
my $includeDB=0;

my $delta=0.1;

my @arg_idx=(0..@ARGV-1);
for my $i (0..@ARGV-1) {
	if ($ARGV[$i] eq '-minSplice') {
		$minSplice=$ARGV[$i+1];
		delete @arg_idx[$i,$i+1];
	}elsif ($ARGV[$i] eq '-minDB') {
		$minDB=$ARGV[$i+1];
		delete @arg_idx[$i,$i+1];
	}elsif ($ARGV[$i] eq '-mirror') {
		$mirror=1;
		delete $arg_idx[$i];
	}elsif ($ARGV[$i] eq '-minSplicing') {
		$minSplicing=$ARGV[$i+1];
		delete @arg_idx[$i,$i+1];
	}elsif ($ARGV[$i] eq '-includeDB') {
		$includeDB=1;
		delete $arg_idx[$i];
	}
}
my @new_arg;
for (@arg_idx) { push(@new_arg,$ARGV[$_]) if (defined($_)); }
@ARGV=@new_arg;

my $output = shift or die $usage;
my $chiThreshold = shift;
my $cgff = shift or die $usage;

die "Existing output file: $output\n" if (-e "$output");

my $numGroups = @ARGV/3;
my @fineSpliceFile;
my @RPKMFile;
my @Names;

die $usage if @ARGV%3>0 or @ARGV==0;

for my $i (0..$numGroups-1) {
	$fineSpliceFile[$i] = shift;
	$RPKMFile[$i] = shift;
	$Names[$i]=shift;
}

my @FileNameArr;
push(@FileNameArr, @fineSpliceFile);
push(@FileNameArr, @RPKMFile);

for(my $i = 0; $i < @FileNameArr; $i++) {
	die  "Absent data file: $FileNameArr[$i]\n" if (!-e "$FileNameArr[$i]") ;
}

my @spliceCountAoH;
my @rpkmAoH;
my %combineMap;
my %NovelSplice;
my %geneStrand;

my $thisFileWithNovel;
my $withNovel=0;
for(my $j = 0; $j < @fineSpliceFile; $j++) {
	my %spliceMap;
	open(FILE,"<$fineSpliceFile[$j]");
	$thisFileWithNovel=0;
	while (<FILE>) {
		if(/^#/){
			$thisFileWithNovel = 1 if /novel/i;
			$withNovel=1 if $thisFileWithNovel;
			next;
		}

		my @token=split(/\t/);
		next until $token[1] =~ /(\d+)\([+|-]?\d+\)-(\d+)\([+|-]?\d+\)/;

		# filtering
		my $pass=1;
		if($minSplicing>0){
			my $freqIdx=3;
			$freqIdx=4 if $thisFileWithNovel;
			my $splicingPosCnt = () = $token[$freqIdx] =~ /\=/g;
			$pass=0 if $splicingPosCnt<$minSplicing;
		}
		$pass=1 if $thisFileWithNovel && ($token[3] ne "V") && $includeDB;
		next if not $pass;

		$spliceMap{$token[0]}{$1."-".$2}{$token[1]}=$token[2]+0; # $spliceMap{gene}{exon pair}{splice event}=read count;
		$combineMap{$token[0]}{$1."-".$2}{$token[1]}=1;
		$NovelSplice{$token[0]}{$token[1]}=1 if $token[3] eq "V";
	}
	push(@spliceCountAoH,\%spliceMap);
	close(FILE);
}

if($debug){
	my $cnt = keys %combineMap;
	print "size of combineMap: $cnt\n";
}

for(my $j = 0; $j < @RPKMFile; $j++) {
	my %rpkmMap;
	open(File,"<$RPKMFile[$j]");
	while(<File>) {
		next if /\#/;
		my @token = split;
		my $uniqRead = $token[2]*(1-$token[4]);
		$rpkmMap{$token[0]} = sprintf("%.0f", $uniqRead); #$rpkmMap{GeneID} = UniqRead count;
	}
	push(@rpkmAoH,\%rpkmMap);
	close(File);
}

open(CGFF,"<$cgff");
while (<CGFF>) {
	next until /^\>/;
	my @token=split(/\s+/);
	my $gene = substr($token[0],1);
	$geneStrand{$gene}=$token[4];
}
close(CGFF);


my @resultArr;

for my $gene (sort {$a cmp $b} (keys %combineMap) ) {
	for my $exonpair (sort exonsort (keys %{$combineMap{$gene}}) ) {
		my @events = sort {$a cmp $b} keys %{$combineMap{$gene}{$exonpair}};
		next if @events < 2;
		for my $x (0..@events-2) {
			for my $y ($x+1 .. @events-1) {
				makeOneRow($gene,$exonpair,\@events,$x,$y,\@resultArr);
				makeOneRow($gene,$exonpair,\@events,$y,$x,\@resultArr) if $mirror;
			}
		}
	}
}
print "result count: ".scalar @resultArr."\n" if $debug;

open(OFILE, ">$output");
my $DA_Head="Donor/Acceptor Change\t";
my $dbHead= ($withNovel==1) ? "Database\t" : "";
my $Header="#Gene\tSplice1\t".$dbHead."Splice2\t$dbHead$DA_Head";
for my $i (0..$numGroups-1){
	$Header.="$Names[$i] Splice1\t";
}
for my $i (0..$numGroups-1){
	$Header.="$Names[$i] Splice2\t";
}
$Header.="chiSquared\t";
for my $i (0..$numGroups-1){
	$Header.="$Names[$i] Uniq\t";
}
$Header.="chiSquared";
print OFILE "$Header\n";

@resultArr = sort {$b->[1] <=> $a->[1]} @resultArr;
for (@resultArr) {
	my @array= @{$_};
	print OFILE "$array[0]\n";
}
close(OFILE);


sub chisquared {
	my @Xn=@{$_[0]};
	my @Yn=@{$_[1]};
	my $i;
	my $j;
	my $sum1;
	my $sum2;
	my $expect;
	my $chi;
	my $chisqure;

	for my $val (@Xn) {
		$sum1 += $val;
	}

	for my $val (@Yn) {
		$sum2 += $val;
	}

	for my $idx (0 .. @Yn-1) {
		if($sum2 > 0) {
			$expect = $sum1*($Yn[$idx]/$sum2);
		}else{
			$expect = 0;
		}

		if($expect > 0) {
			$chi = ($Xn[$idx]-$expect)*($Xn[$idx]-$expect) / $expect;
			$chisqure += $chi;
		}else{
			$chisqure = "N/A";
			last;
		}
	}
	return $chisqure;
}

sub exonsort {
	my ($a1, $a2) = $a =~ /(\d+)-(\d+)/;
	my ($b1, $b2) = $b =~ /(\d+)-(\d+)/;
	$a1 <=> $b1 or $a2 <=> $b2;
}


sub totalSpliceReads{
	my $gene = shift;
	my $exonPair = shift;
	my $spliceEvent = shift;
	my $sum=0;

	for (my $i = 0; $i < @spliceCountAoH; $i++) {
		if(exists $spliceCountAoH[$i]{$gene}{$exonPair}{$spliceEvent}) {
			$sum += $spliceCountAoH[$i]{$gene}{$exonPair}{$spliceEvent};
		}
	}
	return $sum;
}

sub makeOneRow{
	my $gene = shift;
	my $exonpair = shift;
	my $eventArrRef = shift;
	my $x = shift;
	my $y = shift;
	my $resultArrRef = shift;
	
	print "processing: $gene $exonpair $x $y\n" if $debug;

	my @event1ReadCounts = (0) x @spliceCountAoH;
	my @event2ReadCounts = (0) x @spliceCountAoH;
		
	my $splice1=$$eventArrRef[$x];
	my $splice2=$$eventArrRef[$y];

	for ($i=0;$i<@spliceCountAoH;$i++) {
		if(exists $spliceCountAoH[$i]{$gene}{$exonpair}{$splice1}) {
			$event1ReadCounts[$i] = $spliceCountAoH[$i]{$gene}{$exonpair}{$splice1};
		}else{
			$event1ReadCounts[$i] = $delta;
		}
		
		if(exists $spliceCountAoH[$i]{$gene}{$exonpair}{$splice2}) {
			$event2ReadCounts[$i] = $spliceCountAoH[$i]{$gene}{$exonpair}{$splice2};
		}else{
			$event2ReadCounts[$i] = $delta;
		}
	}

	my $chiSquared_cmp1 = chisquared(\@event1ReadCounts,\@event2ReadCounts);
	my $chiSquared_cmp2 = chisquared(\@event2ReadCounts,\@event1ReadCounts);
	my $chiSquared_readcount = ($chiSquared_cmp1>$chiSquared_cmp2)? $chiSquared_cmp2:$chiSquared_cmp1;
	
	# D/A notation
	my @spliceDA;
	my $Donor1=0;
	my $Acceptor1=0;
	my $Donor2=0;
	my $Acceptor2=0;
	if ($geneStrand{$gene} eq "+") {
		$splice1 =~ /\d+\(([+|-]?\d+)\)-\d+\(([+|-]?\d+)\)/;
		$Donor1=$1; $Acceptor1=$2;
		$splice2 =~ /\d+\(([+|-]?\d+)\)-\d+\(([+|-]?\d+)\)/;
		$Donor2=$1; $Acceptor2=$2;
	}elsif ($geneStrand{$gene} eq "-"){
		$splice1 =~ /\d+\(([+|-]?\d+)\)-\d+\(([+|-]?\d+)\)/;
		$Donor1=$2; $Acceptor1=$1;
		$splice2 =~ /\d+\(([+|-]?\d+)\)-\d+\(([+|-]?\d+)\)/;
		$Donor2=$2; $Acceptor2=$1;
	}
	push(@spliceDA,"D") if $Donor1!=$Donor2;
	push(@spliceDA,"A") if $Acceptor1!=$Acceptor2;
	my $DA = join("/",@spliceDA);

			
	my @uniqReads = (0) x @rpkmAoH;
	my @subReads;
	
	for ($j=0;$j<@rpkmAoH;$j++) {
		if(exists $rpkmAoH[$j]{$gene}) {
			$uniqReads[$j] = $rpkmAoH[$j]{$gene};
		}
	}

	my $splice1Str="$splice1";
	my $splice2Str="$splice2";
	my @spliceInDB=(0,0);
	if ($withNovel) {
		if (exists $NovelSplice{$gene}{$splice1}) {
			$splice1Str .= "\t";
		}else{
			$splice1Str .= "\tV";
			$spliceInDB[0]=1;
		}
		if (exists $NovelSplice{$gene}{$splice2}) {
			$splice2Str .= "\t";
		}else{
			$splice2Str .= "\tV";
			$spliceInDB[1]=1;
		}
	}

	if ($spliceInDB[0]+$spliceInDB[1]==1) {
		if ($spliceInDB[0]) {
			@subReads = @event2ReadCounts;
		} elsif ($spliceInDB[1]) {
			@subReads = @event1ReadCounts;
		}
	}else{
		my $totalReadCntSplice_1 = totalSpliceReads($gene,$exonpair,$splice1);
		my $totalReadCntSplice_2 = totalSpliceReads($gene,$exonpair,$splice2);
	
		if ($totalReadCntSplice_1<$totalReadCntSplice_2) {
			@subReads = @event1ReadCounts;
		}else {
			@subReads = @event2ReadCounts;
		}
	}
	
	my $chiSquared_rpkm = chisquared(\@subReads,\@uniqReads);
		
	# output filter
	my $report_flag=1;
	if(totalSpliceReads($gene,$exonpair,$splice1)<$minSplice || totalSpliceReads($gene,$exonpair,$splice2)<$minSplice){
		$report_flag=0;
	}
	return if not $report_flag;
	
	# output filter
	if($minDB>0){
		if(($spliceInDB[0]+$spliceInDB[1])<$minDB){
			$report_flag=0;
		}
	}
	return if not $report_flag;
	
	# output filter
	return if $chiSquared_readcount < $chiThreshold;

	# output string			
	foreach my $idx (0..@event1ReadCounts-1){
		$event1ReadCounts[$idx]=0 if ($event1ReadCounts[$idx]<1);
	}
	foreach my $idx (0..@event2ReadCounts-1){
		$event2ReadCounts[$idx]=0 if ($event2ReadCounts[$idx]<1);
	}

	my $result_msg = "$gene\t$splice1Str\t$splice2Str\t$DA\t";
	$result_msg.=join("\t",@event1ReadCounts);
	$result_msg.="\t";
	$result_msg.=join("\t",@event2ReadCounts);
	$result_msg.="\t";
	$result_msg.="$chiSquared_readcount\t";
	$result_msg.=join("\t",@uniqReads);
	$result_msg.="\t$chiSquared_rpkm";
	push @{$resultArrRef}, [$result_msg,$chiSquared_readcount];
}
