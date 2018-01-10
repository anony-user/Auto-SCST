#!/usr/bin/perl -w

no warnings 'once';

$|=1;

my $debug=0;

my $delayTime = 120;
my @watchList = ();

while(@ARGV > 0){
  my $option = shift;
  if($option eq "-d"){
    $option = shift or die $usage;
    $delayTime = $option;
    next;
  }
  if($option eq "-w"){
    push @watchList, @ARGV; # take all rest tokens as watch list
    last;
  }
}

# get watch list from current qstat if no watch list assigned
my $qstatOut = `qstat`;
my $runningIdHash = getQstatJobs($qstatOut,"R","Q");
if(@watchList==0){
  for my $key (sort {$a cmp $b} keys %{$runningIdHash}){
    push @watchList, $key;
  }
  
  if(@watchList==0){
    print "empty watch list and no running jobs\n";
    exit;
  }
  
  print "get watch list: @watchList\n";
}

# for every delayTime seconds
my $dotCnt=0;
while(not allWatchListNotRunning(\@watchList,$runningIdHash)){
  sleep $delayTime;

  $dotCnt++;
  print ".";
  print "\n" if ($dotCnt%10)==0;

  $qstatOut = `qstat`;
  $runningIdHash = getQstatJobs($qstatOut,"R","Q");
}
print "all watch jobs done.\n";

# subroutine
sub allWatchListNotRunning {
  $listRef = shift;
  $hashRef = shift;
  
  for my $key (@{$listRef}){
    return 0 if exists $hashRef->{$key};
  }
  return 1;
}

sub getQstatJobs {
  my $qstatStr = shift;
  my @lines = split(/^/m,$qstatStr);
  my $hash={};

  my $gotStartingDash=0;
  
  for my $line (@lines){
    $gotStartingDash = 1 if $line=~/^-/;
    next if (not $gotStartingDash) || $line=~/^-/;
    my @token=split(/\s+/,$line);

    # see if a job in specified state
    my $flag=0;
    for(my $idx=0;$idx<@_;$idx++){
      $flag=1 if $token[4] eq $_[$idx];
    }
    $hash->{$token[0]}=$token[4] if $flag;
    
    print $line if $debug;
  }
  
  return $hash;
}
