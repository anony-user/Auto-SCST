#!/usr/bin/perl 

my $usage = "SSEScompare_ex.pl [-delta <delta>] <outputFilename> <readLength> <spliceCount_1> <geneRPKM_1> <name_1> [<spliceCount_i> <geneRPKM_i> <name_i>]+\n";

my $delta = 0.1;

#Retrieve parameter
my @arg_idx=(0..@ARGV-1);
for my $i (0..@ARGV-1) {
    if ($ARGV[$i] eq '-delta') {
        $delta=$ARGV[$i+1];
        delete @arg_idx[$i,$i+1];
    }
}
my @new_arg;
for (@arg_idx) { push(@new_arg,$ARGV[$_]) if (defined($_)); }
@ARGV=@new_arg;

my $OutputFilename = shift or die $usage;
my $readLength = shift or die $usage;
my @spliceCountInputFile,@RPKMInputFile;
die "Existing output file: $OutputFilename\n" if (-e "$OutputFilename");

my $numPrefix = @ARGV/3;
my @spliceCountInputFile,@RPKMInputFile,@Prefixes;

die $usage if @ARGV%3>0 || @ARGV==0;

for $i (0..$numPrefix-1) {
    $spliceCountInputFile[$i] = shift;
    $RPKMInputFile[$i] = shift;
    $Prefixes[$i]=shift;
}

my @FileNameArr;
push(@FileNameArr, @spliceCountInputFile);
push(@FileNameArr, @RPKMInputFile);

for($i = 0; $i < @FileNameArr; $i++) {
    die  "Absent data file: $FileNameArr[$i]\n" if (!-e "$FileNameArr[$i]") ;
}

# establish keys (geneID - exonPair)
# create a hash table
my %geneExonpairsMap;
my @spliceCountAoH;
for($j = 0; $j < @spliceCountInputFile; $j++) {
    my %rec;
    open(file,"<$spliceCountInputFile[$j]");
    while(<file>) {
        next if /^\#/; 
        my @token= split(/\t/);
        $geneExonpairsMap{$token[0]}{$token[1]} = "" if $token[3] eq "V" ; #Novel gene hash {GeneID}{exonPair} initialize
        $rec{$token[0]}{$token[1]} = $token[2];    # %rec {GeneID}{exonPair} = reads
    }
    push(@spliceCountAoH,\%rec);
    close(file);
}

my @RPKMAoH;
for($j = 0; $j < @RPKMInputFile; $j++) {
    my $rec = {};
    open(file,"< $RPKMInputFile[$j]");
    while (<file>) {
        next if /^\#/;
        my @token = split(/\t/);
        # rec {GeneID} = coverage depth of uniq-reads
        $rec->{$token[0]} = ($token[2]*(1-$token[4]) * $readLength) / ($token[1] * 1000);
    }
    push(@RPKMAoH,$rec);
    close(file);
}

my @resultAr;
for $gene (keys %geneExonpairsMap) {   #key {GeneID}{exonPair}
    for $exonPair ( keys %{$geneExonpairsMap{$gene}}) {
        my @readset; # number of x-splice reads, for computation
        my @splitExonPair = split(/<=>/, $exonPair);
        my $u = @splitExonPair[0];
        my $v = @splitExonPair[1];
        
        my @numRead;
        my @numRead_disp; #for display
        my @sumReadArr; # number of x-splice reads
        my @depth;
        for ($idx = 0; $idx < @spliceCountAoH; $idx++) {
            if(exists $spliceCountAoH[$idx]{$gene}{$exonPair}) {
                $numRead[$idx] = $spliceCountAoH[$idx]{$gene}{$exonPair};  # get $ReadNum from @spliceCountAoH
                $numRead_disp[$idx] = $spliceCountAoH[$idx]{$gene}{$exonPair};
            }else{
                $numRead[$idx] = $delta;
                $numRead_disp[$idx] = 0;
            }
            
            my $sumread = 0;
            for($j = 1; $j < ($v-$u); $j++) {
                if($u+$j < $v) {
                    $I = $u + $j;
                    my $subExonPair = "$u<=>$I";
                    $sumread += lookup($gene,$subExonPair,$idx);
                }
                
                if($v-$j > $u) {
                    $I = $v - $j;
                    my $subExonPair = "$I<=>$v";
                    $sumread += lookup($gene,$subExonPair,$idx);
                }
            }
            
            if($sumread == 0) {
                $readset[$idx] = $delta;
            }else{
                $readset[$idx] = $sumread;
            }
            $sumReadArr[$idx] = $sumread; # array ($sumRead)
        }
        
        my $RPKMAoHSize = @RPKMAoH;
        for ($idx = 0; $idx < $RPKMAoHSize; $idx++) {
            if(exists $RPKMAoH[$idx]{$gene}) {
                $depth[$idx] = $RPKMAoH[$idx]{$gene};
            }else{
                $depth[$idx] = 0;
            }
        }

        # Do chiSquared test
        my $chiSquared = chisquared(\@numRead,\@depth);
        my $XchiSquared1 = chisquared(\@numRead, \@readset);
        my $XchiSquared2 = chisquared(\@readset, \@numRead);
        
        if($XchiSquared1 < $XchiSquared2) {
            $XchiSquaredMin = $XchiSquared1;
        }else{
            $XchiSquaredMin = $XchiSquared2;
        }
        
        if($XchiSquaredMin eq "N/A" && $XchiSquared1 eq "N/A" && $XchiSquared2 eq "N/A") {
            $XchiSquaredMin = "N/A";
        }elsif($XchiSquaredMin eq "N/A" && $XchiSquared1 eq "N/A" && $XchiSquared2 ne "N/A") {
            $XchiSquaredMin = $XchiSquared2;
        }elsif($XchiSquaredMin eq "N/A" && $XchiSquared2 eq "N/A" && $XchiSquared1 ne "N/A") {
            $XchiSquaredMin = $XchiSquared1;
        }
        
        my $readNumAr = join("\t",@numRead_disp);
        my $depthAr = join("\t",@depth);
        my $sumReadAr = join("\t",@sumReadArr);
        
        push(@resultAr,[$gene,$exonPair,$readNumAr,$depthAr,$chiSquared,$sumReadAr,$XchiSquaredMin]);
    }
}

open(STDOUT,">$OutputFilename");
# Headers
print "#GeneID\texonPair\t";
for($i = 0; $i < $numPrefix; $i++) {print $Prefixes[$i]."\t";}
for($i = 0; $i < $numPrefix; $i++) {print "depth".$Prefixes[$i]."\t";}
print "chiSquared\t";
for($i = 0; $i < $numPrefix; $i++) {print "x".$Prefixes[$i]."\t";}
print "xChiSquared\n";


@resultAr = sort {$b->[6]<=>$a->[6] || $a->[4]<=>$b->[4]} @resultAr ;

for $Ar (@resultAr) {
   my $line = (join"\t",@{$Ar});
   print "$line\n";
}

close STDOUT;

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


sub lookup {
    my $gene = shift;
    my $exonPair = shift;
    my $indx = shift;
    my $reads;
    
    if(exists $spliceCountAoH[$indx]{$gene}{$exonPair}) {
        $reads = $spliceCountAoH[$indx]{$gene}{$exonPair};
    }else{
        $reads = 0;
    }
    return $reads;
}