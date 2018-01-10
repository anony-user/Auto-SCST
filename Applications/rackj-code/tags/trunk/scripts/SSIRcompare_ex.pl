#!/usr/bin/perl 
use Data::Dumper;

my $usage = "Usage: SSIRcompare_ex.pl [-byDepth] [-appendCoverRatio] [-delta <delta>] <outputFilename> <intronCount_1> [<intronCntWithCoverRatio_1>] <exonCount_1> <geneRPKM_1> <spliceCount_1> <name_1> [<intronCount_i> [<intronCntWithCoverRatio_i>] <exonCount_i> <geneRPKM_i> <spliceCount_i> <name_i>]+\n";

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

my $OutputFilename = shift or die $usage;

die "Existing output file: $OutputFilename\n" if (-e "$OutputFilename");
my $numPrefix;
if($appendCoverRatio){
	die $usage if @ARGV%6>0 || @ARGV==0;
	$numPrefix = @ARGV/6;
}else{
	die $usage if @ARGV%5>0 || @ARGV==0;
	$numPrefix = @ARGV/5;
}

my @intronCountFilename,@intronCoveredFilename,@ExonCountFilename,@GeneRPKMFilename,@spliceCountFilename,@Prefixes;
for $i (0..$numPrefix-1) {
    $intronCountFilename[$i] = shift;
    
    # include one-more file for covered ratio
    if( (not $byDepth) && $appendCoverRatio ){
    	$intronCoveredFilename[$i] = shift;
    }
    
    $ExonCountFilename[$i] = shift;
    $GeneRPKMFilename[$i] = shift;
    $spliceCountFilename[$i] = shift;
    $Prefixes[$i]=shift;
}

my $intronCountArrSize = @intronCountFilename;
my $intronCoveredArrSize = @intronCoveredFilename;
my $ExonCountArrSize = @ExonCountFilename;
my $GeneRPKMArrSize = @GeneRPKMFilename;
my $spliceCountArrSize = @spliceCountFilename;

my @FileNameArr;
push(@FileNameArr, @intronCountFilename);
push(@FileNameArr, @intronCoveredFilename);
push(@FileNameArr, @ExonCountFilename);
push(@FileNameArr, @GeneRPKMFilename);
push(@FileNameArr, @spliceCountFilename);

for($i = 0; $i < @FileNameArr; $i++) {
    die "Absent data file: $FileNameArr[$i]\n" if (!-e "$FileNameArr[$i]");
}

# 1st File "intronCount"
my @intronReadAoH;
my @intronCoveredAoH;
my %intronLengthMap;
for($i = 0; $i < $intronCountArrSize ; $i++) {
    open(file,"<$intronCountFilename[$i]");
    my %intronReadHashSet;
    my %intronCoveredMap;
    while(<file>) {
        next if /^\#/;
        my @token= split(/\s+/);
        $intronLengthMap{$token[0]}{$token[1]} = $token[3]; #$intronLengthMap{GeneID}{intronNo}=intronLength;
        my $uniqRead = $token[2]*(1-$token[4]);
        if($byDepth){ # no rounding
            $intronReadHashSet{$token[0]}{$token[1]} = $uniqRead; #$intronReadHashSet{GeneID}{intronNo) = UniqRead;
            # coveredRatio
            $intronCoveredMap{$token[0]}{$token[1]} = $token[5];
        }else{ # rounding
            $intronReadHashSet{$token[0]}{$token[1]} = sprintf("%.0f", $uniqRead); #$intronReadHashSet{GeneID}{intronNo) = UniqRead;
        }
    }
    push(@intronReadAoH,\%intronReadHashSet);
    push(@intronCoveredAoH,\%intronCoveredMap) if $byDepth;
    close(file);
}

# 1-sub-1 File "intronCovered"
if( (not $byDepth) && $appendCoverRatio ){
	for($i = 0; $i < $intronCoveredArrSize ; $i++) {
		open(file,"<$intronCoveredFilename[$i]");
		my %intronCoveredMap;
		while(<file>) {
			next if /^\#/;
			my @token= split(/\s+/);
			$intronCoveredMap{$token[0]}{$token[1]} = $token[5];
		}
		push(@intronCoveredAoH,\%intronCoveredMap);
		close(file);
	}
}

#2nd File "exonCount"
my @exonReadAoH;
for($i = 0; $i < $ExonCountArrSize; $i++) {
    open(file,"<$ExonCountFilename[$i]");
    my %exonReadHashSet;
    while(<file>) {
        next if /^\#/;
        my @token= split(/\t/);
        my $uniqRead = $token[2]*(1-$token[4]);
        if($byDepth){ # no rounding
            $exonReadHashSet{$token[0]}{$token[1]} =  $uniqRead; #$exonReadHashSet{GeneID}{ExonNo} =  UniqRead;
        }else{ # rounding
            $exonReadHashSet{$token[0]}{$token[1]} =  sprintf("%.0f", $uniqRead); #$exonReadHashSet{GeneID}{ExonNo} =  UniqRead;
        }
    }
    push(@exonReadAoH, \%exonReadHashSet);
    close(file);
}

#3rd File "geneRPKM"
my @geneRPKMAoH;
for($i = 0; $i < $GeneRPKMArrSize; $i++) {
     open(file,"<$GeneRPKMFilename[$i]");
     my %rpkmReadHashSet;
     while(<file>) {
         next if /^\#/;
         @token= split(/\t/);
         my $uniqRead = $token[2]*(1-$token[4]);
         $rpkmReadHashSet{$token[0]} = sprintf("%.0f", $uniqRead); #$rpkmReadHashSet{GeneID} = UniqRead;
     }
     push(@geneRPKMAoH, \%rpkmReadHashSet);
     close file;
}

#4th File "spliceCount"
my @spliceCountAoH;
for($i = 0; $i < $spliceCountArrSize; $i++) {
    open(file,"<$spliceCountFilename[$i]");
    my %spliceReadHashSet;
    while(<file>) {
        next if /^\#/;
        my @token = split(/\t/);
        $spliceReadHashSet{$token[0]}{$token[1]} = $token[2]; #$spliceReadHashSet{GeneID}{exonPair} = ReadCount;
    }
    push(@spliceCountAoH,\%spliceReadHashSet);
    close(file);
}

my @resultAr;
for $GeneID (keys %intronLengthMap) {
    for $intronNo (keys %{$intronLengthMap{$GeneID}}) {
        my $intronLength = $intronLengthMap{$GeneID}{$intronNo};
        
        my @intronReadAr;
        my @intronCoveredAr;
        my @exonReadAr;
        my @rpkmReadAr;
        my @spliceReadAr;
        my @Chi_spliceReadAr; # adjusted @spliceReadAr
        
        # skips those introns without any counts
        my $intronReadSum=0;
        for ( $i = 0 ; $i < $numPrefix; $i++ ) {
           $intronReadSum += $intronReadAoH[$i]{$GeneID}{$intronNo} if(exists $intronReadAoH[$i]{$GeneID}{$intronNo});
        }
        next if $intronReadSum==0;

        # for each sample        
        for ( $i = 0 ; $i < $numPrefix; $i++ ) {
            
            if(exists $intronReadAoH[$i]{$GeneID}{$intronNo}) {
                $intronReadAr[$i] = $intronReadAoH[$i]{$GeneID}{$intronNo};
            }else{
                $intronReadAr[$i] = 0;
            }

            if($byDepth || ( (not $byDepth) && $appendCoverRatio ) ){
                if(exists $intronCoveredAoH[$i]{$GeneID}{$intronNo}) {
                    $intronCoveredAr[$i] = $intronCoveredAoH[$i]{$GeneID}{$intronNo};
                }else{
                    $intronCoveredAr[$i] = 0;
                }
            }
            
            my $exonNo1 = $intronNo;
            my $exonNo2 = $exonNo1+1;
            my $sumExonRead = 0;
            $sumExonRead += $exonReadAoH[$i]{$GeneID}{$exonNo1} if(exists $exonReadAoH[$i]{$GeneID}{$exonNo1}); 
            $sumExonRead += $exonReadAoH[$i]{$GeneID}{$exonNo2} if(exists $exonReadAoH[$i]{$GeneID}{$exonNo2}); 
            
            my $exonPair = "$exonNo1<=>$exonNo2";
            # remove duplicate counts of spliced reads if not byDepth
            $sumExonRead = $sumExonRead-$spliceCountAoH[$i]{$GeneID}{$exonPair} if (exists $spliceCountAoH[$i]{$GeneID}{$exonPair} && not $byDepth);
            $exonReadAr[$i] = $sumExonRead;
            
            if(exists $geneRPKMAoH[$i]{$GeneID}) {
                $rpkmReadAr[$i] = $geneRPKMAoH[$i]{$GeneID};
            }else{
                $rpkmReadAr[$i] = 0;
            }
            
            my $sumSpliceCounRead=0;
            for $spliceCounExonP (keys %{$spliceCountAoH[$i]{$GeneID}}) {
                my ($u,$v) = $spliceCounExonP =~ /(\d+)<=>(\d+)/;
                if($u <= $intronNo && $intronNo < $v) {
                    $sumSpliceCounRead += $spliceCountAoH[$i]{$GeneID}{$spliceCounExonP};
                }
            }
            
            if($sumSpliceCounRead>0) {
                $spliceReadAr[$i] = $sumSpliceCounRead;
                $Chi_spliceReadAr[$i] = $sumSpliceCounRead;
            }else{
                $spliceReadAr[$i] = 0;
                $Chi_spliceReadAr[$i] = $delta;
            }
        }
        
        $chisqu1 = chisquared(\@intronReadAr ,\@exonReadAr);
        $chisqu2 = chisquared(\@intronReadAr, \@rpkmReadAr);
        $chisqu3 = chisquared(\@intronReadAr, \@Chi_spliceReadAr);
        
        # do rounding if byDepth
        if($byDepth){
            my @roundIntronRead;
            push @roundIntronRead, sprintf("%.2f", $_) for(@intronReadAr);
            @intronReadAr = @roundIntronRead;
        }
        my $intronRead_Str=join("\t",@intronReadAr);

        # do rounding if byDepth
        if($byDepth){
            my @roundExonRead;
            push @roundExonRead, sprintf("%.2f", $_) for(@exonReadAr);
            @exonReadAr = @roundExonRead;
        }
        my $exonRead_Str=join("\t",@exonReadAr);

        my $rpkmRead_Str=join("\t",@rpkmReadAr);
        my $spliceRead_Str=join("\t",@spliceReadAr);

        my $covered_Str = join("\t",@intronCoveredAr);
        
        if($byDepth || ( (not $byDepth) && $appendCoverRatio ) ){
            # add covered ratios
            push(@resultAr,[$GeneID,$intronNo,$intronLength,$intronRead_Str,$exonRead_Str,$chisqu1,$rpkmRead_Str,$chisqu2,$spliceRead_Str,$chisqu3,$covered_Str]);
        }else{
            push(@resultAr,[$GeneID,$intronNo,$intronLength,$intronRead_Str,$exonRead_Str,$chisqu1,$rpkmRead_Str,$chisqu2,$spliceRead_Str,$chisqu3]);
        }
    }
}


open(STDOUT,">$OutputFilename");
print "#GeneID\tintronNo\tintronLength\t";
for($i = 0; $i < $numPrefix; $i++) {print "intron$Prefixes[$i]\t";}
for($i = 0; $i < $numPrefix; $i++) {print "exon$Prefixes[$i]\t";}
print "chiSquared\t";
for($i = 0; $i < $numPrefix; $i++) {print "uniq$Prefixes[$i]\t";}
print "uniChiSquared\t";
for($i = 0; $i < $numPrefix; $i++) {print "splice$Prefixes[$i]\t";}
print "spliceChiSquared";

if($byDepth || ( (not $byDepth) && $appendCoverRatio ) ){
    for($i = 0; $i < $numPrefix; $i++) {print "\tcovered$Prefixes[$i]";}
}

print "\n";

@resultAr = sort {$b->[5]<=>$a->[5]} @resultAr;
for $Ar (@resultAr) {
    my $line = join("\t",@{$Ar});
    print "$line\n"
}
close(STDOUT);

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

    for $val (@Xn) {
         $sum1 += $val;
    }
    
    for $val (@Yn) {
         $sum2 += $val;
    }
    
    for $idx (0 .. @Yn-1) {
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