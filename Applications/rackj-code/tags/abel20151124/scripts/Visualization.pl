#!/usr/bin/perl 

# This script is for helping automation of making plots

############################################
#              User Set Up                 #
############################################

# path to java executable file, plus something like -Xmx1400M if there is
# an out-of-memory problem
$javaPath = "java";
# path to rnaseq.jar of RackJ. For WINDOWS system, remember to use double anti-slash \\
# in quotes
$jarPath = "YourPathTo/rackJ/rackj.jar";

# ref: http://rackj.sourceforge.net/Manual/index.html#visualization
$CGFF = "tair10.strand.cgff";
$model = "tair10.strand.model";
$traceFile = "geneList.txt";
# output dir of data files and image files. Ending with path separator (/, or \\ for WINDOWS)
# would let all output files in a subdir.
$outputDir = "Plots/";

# In the following example setting, we got two samples (AAA & BBB), each with 3 .better files
# Just add as more samples as you like and add as more mapping result files as you like
# ref: http://rackj.sourceforge.net/Manual/index.html#mappingFormats
$HoA{"AAA"} = ["BLAT AAA.1.better","BLAT AAA.2.better","BLAT AAA.3.better"];
$HoA{"BBB"} = ["BLAT BBB.1.better","BLAT BBB.2.better","BLAT BBB.3.better"];

# Intermediate data generation
# set this to 0 if data generation already done.
$dataGeneration = 1;
# RegionTracer or GeneTracer
# ref: http://rackj.sourceforge.net/Manual/index.html#visualization
$TracerClass = "rnaseq.RegionTracer";
$TracerOption = "-J 2 -ID 0.95 -exon true -contain false -min 8 -ALL true";

# Plot generation
$ReadVizClass = "graphics.ReadViz";
# Sometime the ReadViz program may spend too much time on unsuccessful painting of a plot,
# simply changing options may help this. With $timeOutSec, this script controls the
# maximum execution time of ReadViz. This script trys the first option set, after $timeOutSec
# seconds, it trys the second option set, and so on ...
# NOTE: Be sure to check $traceFile.status for UNSUCCESS executions
# ref: http://rackj.sourceforge.net/Manual/index.html#graphics.ReadViz
@ReadVizOption = ("",
		"-multi false",
		"-multi false -uniq false");
$timeOutSec = 120;
# NOTE: we didn't implement this time-out control for WINDOWS system. Set $Windows = 1
# if you are using WINDOWS or you don't want to use the time-out control.
$Windows = 0;

############################################
#              Program                     #
############################################
if($dataGeneration){
	for $key (sort keys %HoA ) 
	{ 
		$mappingOptStr = "";
		for $i ( 0 .. $#{ $HoA{$key} } ) 
		{ 
			$mappingOptStr = $mappingOptStr." -M ".$HoA{$key}[$i];
		}
		my $cmd = "$javaPath -classpath $jarPath $TracerClass $TracerOption -GFF $CGFF -model $model $mappingOptStr -O $outputDir$key -traceFile $traceFile";
		print "\n\nRunning RackJ program: ".$cmd."\n\n";
		system($cmd);
	} 
}

%statusHash = ();
$ReadVizOptionArrSize = @ReadVizOption;
open(file,"<$traceFile");
while( $line = <file> )
{
	$line = trim($line);
	next if length($line)==0;

	@token = split(/\s/, $line);
	if(3==@token){
		$gene = $token[0]."_".$token[1]."_".$token[2];
	}else{
		$gene = trim($token[0]);
	}
	
	for $key (sort keys %HoA ) 
	{
		for($i = 0; $i < $ReadVizOptionArrSize; $i++)
		{  
			my $cmd = "$javaPath -classpath $jarPath $ReadVizClass $ReadVizOption[$i] -I $outputDir$key.$gene -O $outputDir$gene.$key.jpg";
			print "\n\nRunning RackJ program: ".$cmd."\n\n";
			$overTime = alarmProcess($cmd);
			$fileSize = -s "$outputDir$gene.$key.jpg";
			if($fileSize > 0 && $overTime == 0)
			{
				#print "$gene\t$key\t$ReadVizOption[$i]\n";
				$statusHash{$gene, $key} = "$ReadVizOption[$i]";
				last; 
			}
			
		}
		if($fileSize <= 0 || $overTime == 1)
		{
			#print "$gene\t$key\tUNSUCCESS\n";
			$statusHash{$gene, $key} = "UNSUCCESS";
		}
	}
}

open(OUTFILE,">$traceFile.status");
for $gene_sample (sort byGeneSample keys %statusHash)
{
	@token = split($;, $gene_sample);
	print OUTFILE "$token[0]\t$token[1]\t$statusHash{$gene_sample}\n";	
}
close OUTFILE;

$unsuccessFlag = 0;
for $gene_sample ( keys %statusHash)
{
	if($statusHash{$gene_sample} eq "UNSUCCESS"){
		$unsuccessFlag = 1;
		last;
	}
}
if($unsuccessFlag){
	print "\nThe folloing gene-sample pairs didn't success, check $traceFile.status\n";
	for $gene_sample ( keys %statusHash)
	{
		if($statusHash{$gene_sample} eq "UNSUCCESS"){
			@token = split($;, $gene_sample);
			print "$token[0]\t$token[1]\t$statusHash{$gene_sample}\n";	
		}
	}
}else{
	print "\nAll gene-sample pairs succeeded, check $traceFile.status\n";
}

sub alarmProcess($)
{
	my $cmd = shift;

	if($Windows){
		system($cmd);
		return 0;
	}
	
	$timeout = 0;
	$childPid = fork();

	if($childPid == 0)
	{
		#print "PERL: executing: $cmd\n";
		exec split(/ /,$cmd);
	}
	else
	{
		local $SIG{ALRM} = sub {
			alarm 0;
			kill 1,$childPid;
			#print "PERL: kill child pid: $childPid\n";
			$timeout = 1;
			}; # NB: \n required
		alarm $timeOutSec;
#		print "PERL: child pid: $childPid\n";
		wait;
		alarm 0;
	}
	#print "PERL: $childPid : $timeout\n";
	return $timeout;
}

sub byGeneSample {
	my @split1 = split($;, $a);
	my @split2 = split($;, $b);
	my $x_a = $split1[0];
	my $y_a = $split1[1];
	my $x_b = $split2[0];
	my $y_b = $split2[1];
	
	if(($x_a cmp $x_b) != 0){
		return ($x_a cmp $x_b);
	}else{
		return ($y_a cmp $y_b);
	}
}

sub trim {
	my $str=shift;
	$str =~ s/\s+$//g;
	$str =~ s/^\s+//g;
	return $str;
}
