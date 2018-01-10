#!/usr/bin/perl -w 
use strict;
use File::Basename;
use File::Path qw(make_path);
use Cwd;
no warnings qw(once);

my $usage  = "Usage: RNAseq_auto.pl\n";

my %srcHash = ();
my %bamHash = ();
my %filterHash = ();
my %computeHash = ();
my %designHash=();
my %compareHash = ();


############################################
#              User Set Up                 #
############################################
my $usePBS = 1;
my $logfile = "process.log";
my $stdoutDir = "logs";
my $pbsWorkDir = "";

my $javaPath = "java";

my $jarPath = "/RAID1/tmp/yungi/software/rackj/rackj.jar";
my $samJarPath = "/RAID1/tmp/yungi/software/rackj/lib/sam-1.89.jar";

my $CGFF = "DB/tair10.strand.cgff";
my $model = "DB/tair10.strand.model";

# If the sample has only ONE repeat, make repeat with the same name as sample
# To skip the mapping step, sets the value to .sam or .bam  
$srcHash{"5t03"}{"5t03"} = "5t03.merged.bam";
$srcHash{"axe"}{"axe"} = "axe.merged.bam";
$srcHash{"Col"}{"Col"} = "Col.merged.bam";
$srcHash{"fld"}{"fld"} = "fld.merged.bam";
$srcHash{"fve"}{"fve"} = "fve.merged.bam";
$srcHash{"hda9"}{"hda9"} = "hda9.merged.bam";



# (Step1) MappingBlat 
my $mappingTargetString = "-target DB/TAIR10_chr_all.fas"; # the value can be the target name listing on TargetTableFile, or -target yourPathTo/target.fas 
my $mappingBlatOptions = "-ID 0.85 -q=rna -t=dna -tileSize=9 -stepSize=3 -maxIntron=2000";
my $splitAmount = 20;



# (Step2) Filter
#$filterHash{"j2mb8"} = "-J 2 -minB 8";
$filterHash{"ID95Top"} = "-ID 0.95 -top true";



# (Step3) Computation #TODO: $CGFF $model
$computeHash{"sort"} = ["ID95Top", ""];
$computeHash{"RPKMComputer"} = ["ID95Top", "-min 2"];
$computeHash{"IntronCounter"} = ["ID95Top", ""];
$computeHash{"FineSpliceCounter"} = ["ID95Top", "-min 2"];
$computeHash{"GeneCoverageArray"} = ["ID95Top", ""];



# (Step4) Comparison
my $readLength=51;

my $chiSqrTh = 10;
my $PvalTh = 0.5;

$designHash{"Col","5t03"} = "5t03";
$designHash{"Col","axe"} = "axe";
$designHash{"Col","fld"} = "fld";
$designHash{"Col","fve"} = "fve";
$designHash{"Col","hda9"} = "hda9";

$compareHash{"SSES"} = "";
$compareHash{"SSIR"} = "";
#$compareHash{"SSDA"} = "-minSplice 3"; 
#$compareHash{"SSGE"} = "-min 3.3";
$compareHash{"SSDA"} = ""; 
$compareHash{"SSGE"} = "";

$compareHash{"RPKMcorrelation"}{"log"} = "-log";
$compareHash{"RPKMcorrelation"}{""} = "";


############################################
#              Steps          	           #
############################################
my %CmdHoH;
my %CmdHash;
my $cmd;

my $logfileFH;
$|=1;

my $reference;
if($mappingTargetString =~ /-target\s+(\S+)/){
	$reference = $1;
}else{
	require("MappingTargetTable.pl");
	$reference = MappingTargetTable($mappingTargetString, "blat");
}

make_path($stdoutDir) if ($usePBS && ($stdoutDir ne ""));

$pbsWorkDir = getcwd() if ($pbsWorkDir eq "");
$stdoutDir = "." if($stdoutDir eq "");


#---------------------------------
# (Step1) MappingBlat 	
#---------------------------------
die "Duplicate repeats name on srcHash.\n" if( !validateSrcRepeatName() );

for my $sample (sort keys %srcHash){
	for my $repeat (sort keys $srcHash{$sample}){
		die "Absent data file: $srcHash{$sample}{$repeat}\n" if (!-e $srcHash{$sample}{$repeat});
	}
}

%CmdHash = ();
my $cmdTemplete = "Mapping.pl ";
$cmdTemplete .= "-split $splitAmount " if ($splitAmount>0);

if($mappingTargetString =~ /-target/){
	$cmdTemplete .= "x %s %s MappingBlat.pl $mappingBlatOptions $mappingTargetString";
}else{
	$cmdTemplete .= "$mappingTargetString %s %s MappingBlat.pl $mappingBlatOptions"; 
}

for my $sample (sort keys %srcHash){
	for my $repeat (sort keys %{$srcHash{$sample}}){

		my $input = $srcHash{$sample}{$repeat};
		
		if($input =~ /\.[b|s]am$/i){
			
			$bamHash{$sample}{$repeat}{""} = $input;
			
		}else{
			my $output = "$repeat.blat.bam";
			
			my $cmd = sprintf $cmdTemplete, $input, $output;
			
			$CmdHash{$cmd} = "$stdoutDir/$repeat.log";
			
			$bamHash{$sample}{$repeat}{""} = $output;
		}
	}
}

if( %CmdHash ){
	open($logfileFH, ">>$logfile");
	print $logfileFH "Mapping:\n\n";
	close($logfileFH);
	submitCommand(\%CmdHash, $usePBS, $pbsWorkDir, $logfile);
}



#---------------------------------
# (Step2) Alignment filter
#---------------------------------
my $filterNameErr = validateFilterName();
die "The following parent filter cannot be found : ".join(", ", @{$filterNameErr})."\n" if( @{$filterNameErr} > 0 );

my %queueHash = (); 

my %todoHash = ();
for my $filter (keys %filterHash){
	if( ref( $filterHash{$filter} ) eq 'ARRAY' ){
		$todoHash{$filter} = $filterHash{$filter}[0];
	}else{
		push @{$queueHash{0}}, $filter;	
	}
}

my $current=0; 
while( (keys %todoHash)>0 ){
	
	if(!exists $queueHash{$current}){
		last;
	}

	for my $filter (keys %todoHash){
		my $parent = $todoHash{$filter};

		if( grep {$parent eq $_} @{$queueHash{$current}} ){
			my $myLevel = ($current+1);
			push @{$queueHash{$myLevel}}, $filter;
			
			delete $todoHash{$filter};
		}
	}
	
	$current++;
}

my @rest = (keys %todoHash);
die "Some of the following filters have invalid parent : ".join(", ", @rest)."\n" if( @rest >0 );

for my $level (sort keys %queueHash){
	
	%CmdHash = ();	
	for my $myName ( @{$queueHash{$level}} ){
		
		my $myParent = ( ref($filterHash{$myName}) eq 'ARRAY' )? $filterHash{$myName}[0] : "";
		my $myParameter = ( ref($filterHash{$myName}) eq 'ARRAY' )? $filterHash{$myName}[1] : $filterHash{$myName};
		
		for my $sample (sort keys %bamHash){
			for my $repeat (sort keys %{$bamHash{$sample}}){
				$cmd = "$javaPath -classpath $jarPath:$samJarPath rnaseq.AlignmentFilter ";
				
				my $inBam = $bamHash{$sample}{$repeat}{$myParent};
				die "Absent data file: $inBam\n" if (!-e $inBam);
				
				my $fname = fileparse($inBam, qr/\.[b|s]am$/i);
				my $outputName = "$fname.$myName.sam";
				
				$cmd .= "-M SAM $inBam ";
				$cmd .= $myParameter;
				$cmd .= " -O $outputName";
				
				$CmdHash{$cmd} = "$stdoutDir/$repeat.log";
				
				$bamHash{$sample}{$repeat}{$myName} = $outputName;
			}
		}
	}

	open($logfileFH, ">>$logfile");
	print $logfileFH "\nAlignment Filter:\n\n";
	close($logfileFH);
	submitCommand(\%CmdHash, $usePBS, $pbsWorkDir, $logfile);
}


#---------------------------------
# (Step3) Compute
#---------------------------------
my $computeNameErr = validateComputeFilter();
die "No such filter name : ".join(", ", @{$computeNameErr})."\n" if( @{$computeNameErr} > 0 );

%CmdHoH = ();
%CmdHash = ();
my %cmdIndexHash = ();

my @modelRequiredTasks = ('RPKMComputer','IntronCounter','FineSpliceCounter'); # -model is required in these programs

for my $task (sort keys %computeHash){
	my ($filterName, $parameter) = @{$computeHash{$task}};
	
	if($task eq "sort"){
		
		for my $sample (sort keys %bamHash){
			for my $repeat (sort keys %{$bamHash{$sample}}){
				my $inSam = $bamHash{$sample}{$repeat}{$filterName};
				my $name = fileparse($inSam, qr/\.[b|s]am$/i);
				
				my $cmdSort;
				if ($inSam =~ /.sam$/i){
					die "Unable to convert SAM to BAM : mappingTargetString is incorrect\n" if (!-e $reference);
					$cmdSort = "samtools view -bS -T $reference $inSam | samtools sort -m 2000000000 /dev/stdin sorted.$name";	
				}else{
					$cmdSort = "samtools sort -m 2000000000 $inSam sorted.$name";
				}
				
				$CmdHash{$cmdSort} = "$stdoutDir/$repeat.log";
				$cmdIndexHash{"samtools index sorted.$name.bam"} = "$stdoutDir/$repeat.log";
			}
		}
		
	}else{
		
		if($task eq "IntronCounter"){
			$cmd = "$javaPath -classpath $jarPath:$samJarPath rnaseq.ReadCounterMainAdaptor rnaseq.$task ";
		}else{
			$cmd = "$javaPath -classpath $jarPath:$samJarPath rnaseq.$task "; 	
		}
		
		for my $sample (sort keys %bamHash){
			my $pooledIn = "";
			
			for my $repeat (sort keys %{$bamHash{$sample}}){ 
				
				my $in = "-M SAM $bamHash{$sample}{$repeat}{$filterName}";
				
				$pooledIn .= $in;
				
				$in .= " $parameter" if($parameter ne "");
				$in .= " -GFF $CGFF";
				$in .= " -model $model" if( grep {$task eq $_} @modelRequiredTasks );
				$in .= " -O $repeat";
				
				$CmdHoH{$task}{'separate'}{"$cmd"."$in"} = "$stdoutDir/$repeat.log";
			}
			
			$pooledIn .= " $parameter" if($parameter ne ""); 
			$pooledIn .= " -GFF $CGFF";
			$pooledIn .= " -model $model" if( grep {$task eq $_} @modelRequiredTasks );
			$pooledIn .= " -O $sample";
			
			$CmdHoH{$task}{'pooled'}{"$cmd"."$pooledIn"} = "$stdoutDir/$sample.log" if( (keys %{$bamHash{$sample}})>1 );
		}
		
	}

}

if( %CmdHash && %cmdIndexHash ){
	open($logfileFH, ">>$logfile");
	print $logfileFH "\nSamtools sort,index:\n\n";
	close($logfileFH);
	submitCommand(\%CmdHash, $usePBS, $pbsWorkDir, $logfile);
	submitCommand(\%cmdIndexHash, $usePBS, $pbsWorkDir, $logfile);
}

if( %CmdHoH ) {
	for my $key (sort keys %CmdHoH){
		open($logfileFH, ">>$logfile");
		print $logfileFH "\n".$key.":\n\n";
		close($logfileFH);
		for my $type (sort keys $CmdHoH{$key}){
			submitCommand(\%{$CmdHoH{$key}{$type}}, $usePBS, $pbsWorkDir, $logfile);
		}
	}	
}



#---------------------------------
# (Step4) Comparison
#---------------------------------
%CmdHash = ();
%CmdHoH = ();

# correlation
if(exists $compareHash{"RPKMcorrelation"}){
	my @inputFileNames;
	for my $sample (sort keys %bamHash){
		for my $repeat (sort keys %{$bamHash{$sample}}){
			push @inputFileNames, $repeat;
		}
	}
	checkInputFiles(\@inputFileNames, [".geneRPKM"]);
	
	my $fileStr = "";
	for(@inputFileNames){
		$fileStr .= "$_.geneRPKM ";
	}
	
	for my $key (keys %{$compareHash{"RPKMcorrelation"}}){
		my $parameter = $compareHash{"RPKMcorrelation"}{$key};
		
		my $outName = "";
		$outName = "_$key" if (length($key)>0);
		
		$cmd = "RPKMcorrelation.pl";
		$cmd .= " $parameter" unless($parameter eq "");
		$cmd .= " -o correlation$outName.xls $fileStr";
		
		$CmdHash{$cmd} = "$stdoutDir/corr$outName.log";
	}
}
 
# SSAS compare
for my $key (keys %designHash){
	my $prefix = $designHash{$key};	
	my ($ctrl, $expt) = split($;, $key);
	
	my $pooled = 0;
	my $ctrlCnt = (keys %{$bamHash{$ctrl}});
	my $exptCnt = (keys %{$bamHash{$expt}});
	$pooled = 1 if( $ctrlCnt>1 && $exptCnt>1 );
	
	my @inputFileNames;
	if( $pooled ){
		@inputFileNames = ( (keys %{$bamHash{$ctrl}}), (keys %{$bamHash{$expt}}), $ctrl, $expt );
	}else{
		@inputFileNames = ( (keys %{$bamHash{$ctrl}}), (keys %{$bamHash{$expt}}) );	
	}
	
	my $separatedOut = "$stdoutDir/$ctrl\_$expt.log";
	my $pooledOut = "$stdoutDir/$ctrl\_$expt\_pooled.log";
	
	for my $task (sort keys %compareHash){
		my $parameter = $compareHash{$task};
		
		my $cmdPooled;
		my $cmdSeparate;
		
		if($task =~ /SSES/){
			checkInputFiles(\@inputFileNames, [".spliceCount", ".geneRPKM"]);
			
			# separate
			$cmdSeparate = "SSEScompare.pl SSES_$prefix\_separate.xls $readLength";
			for my $ctrlRepeat (sort keys %{$bamHash{$ctrl}}){
				$cmdSeparate .= " $ctrlRepeat";
			}
			for my $exptRepeat (sort keys %{$bamHash{$expt}}){
				$cmdSeparate .= " $exptRepeat";
			}
			$CmdHoH{$task}{'separate'}{$cmdSeparate} = $separatedOut;
			
			# pooled
			if( $pooled ){
				$cmdPooled = "SSEScompare.pl SSES_$prefix\_pooled.xls $readLength $ctrl $expt";
				$CmdHoH{$task}{'pooled'}{$cmdPooled} = $pooledOut;
			}
			
		}elsif($task =~ /SSIR/){
			checkInputFiles(\@inputFileNames, [".intronCount", ".exonCount", ".geneRPKM", ".spliceCount"]);
			
			# separate
			$cmdSeparate = "SSIRcompare.pl SSIR_$prefix\_separate.xls";
			for my $ctrlRepeat (sort keys %{$bamHash{$ctrl}}){
				$cmdSeparate .= " $ctrlRepeat $ctrlRepeat";
			}
			for my $exptRepeat (sort keys %{$bamHash{$expt}}){
				$cmdSeparate .= " $exptRepeat $exptRepeat";
			}
			$CmdHoH{$task}{'separate'}{$cmdSeparate} = $separatedOut;
			
			# pooled
			if( $pooled ){
				$cmdPooled = "SSIRcompare.pl SSIR_$prefix\_pooled.xls $ctrl $ctrl $expt $expt";
				$CmdHoH{$task}{'pooled'}{$cmdPooled} = $pooledOut;
			}
			
		}elsif($task =~ /SSDA/){
			checkInputFiles(\@inputFileNames, [".fineSplice", ".geneRPKM"]);
			
			# separate
			$cmdSeparate = "SSDAcompare_chisqr.pl SSDA_$prefix\_separate.xls $chiSqrTh";
			$cmdSeparate .= " $parameter" unless($parameter eq "");
			$cmdSeparate .= " $CGFF";
			for my $ctrlRepeat (sort keys %{$bamHash{$ctrl}}){
				$cmdSeparate .= " $ctrlRepeat.fineSplice $ctrlRepeat.geneRPKM $ctrlRepeat";
			}
			for my $exptRepeat (sort keys %{$bamHash{$expt}}){
				$cmdSeparate .= " $exptRepeat.fineSplice $exptRepeat.geneRPKM $exptRepeat";
			}
			$CmdHoH{$task}{'separate'}{$cmdSeparate} = $separatedOut;
			
			# pooled
			if( $pooled ){
				$cmdPooled = "SSDAcompare.pl SSDA_$prefix\_pooled.xls $ctrl.fineSplice $ctrl.geneRPKM $ctrl $expt.fineSplice $expt.geneRPKM $expt $jarPath $PvalTh";
				$cmdPooled .= " $parameter" unless($parameter eq "");
				$cmdPooled .= " $CGFF";
				$CmdHoH{$task}{'pooled'}{$cmdPooled} = $pooledOut;
			}
			
		}elsif($task =~ /SSGE/){
			checkInputFiles(\@inputFileNames, [".fineSplice", ".geneCoverage"]);
			
			# separate
			$cmdSeparate = "SSGEcompare.pl SSGE_$prefix\_separate.xls";
			$cmdSeparate .= " $parameter" unless($parameter eq "");
			$cmdSeparate .= " $CGFF";
			for my $ctrlRepeat (sort keys %{$bamHash{$ctrl}}){
				$cmdSeparate .= " $ctrlRepeat.fineSplice $ctrlRepeat.geneCoverage $readLength $ctrlRepeat";
			}
			for my $exptRepeat (sort keys %{$bamHash{$expt}}){
				$cmdSeparate .= " $exptRepeat.fineSplice $exptRepeat.geneCoverage $readLength $exptRepeat";
			}
			$CmdHoH{$task}{'separate'}{$cmdSeparate} = $separatedOut;
			
			# pooled
			if( $pooled ){
				$cmdPooled = "SSGEcompare.pl SSGE_$prefix\_pooled.xls";
				$cmdPooled .= " $parameter" unless($parameter eq "");
				$cmdPooled .= " $CGFF"; 
				$cmdPooled .= " $ctrl.fineSplice $ctrl.geneCoverage $readLength $ctrl $expt.fineSplice $expt.geneCoverage $readLength $expt";
				$CmdHoH{$task}{'pooled'}{$cmdPooled} = $pooledOut;
			}
			
		}
	}
}


if( %CmdHash ){
	open($logfileFH, ">>$logfile");
	print $logfileFH "\nRPKMcorrelation:\n\n";
	close($logfileFH);
	submitCommand(\%CmdHash, $usePBS, $pbsWorkDir, $logfile);
}

if( %CmdHoH ) {
	for my $key (sort keys %CmdHoH){
		open($logfileFH, ">>$logfile");
		print $logfileFH "\n".$key.":\n\n";
		close($logfileFH);
		for my $type (keys $CmdHoH{$key}){
			submitCommand(\%{$CmdHoH{$key}{$type}}, $usePBS, $pbsWorkDir, $logfile);
		}
	}
}


#---------------------------------
# subroutine
#---------------------------------
sub validateSrcRepeatName {
	my %names;
	my $count=0;
	for my $sample (keys %srcHash){
		for my $repeat (keys %{$srcHash{$sample}}){
			$names{$repeat}=1;
			$count++;
		}
	}
	
	if( (keys %names) != $count ){
		return 0;
	}
	return 1;
}


sub validateFilterName {
	my @msg=();
	for my $filter (keys %filterHash){
		if(ref($filterHash{$filter}) eq 'ARRAY'){
			my $parent = $filterHash{$filter}[0];
			
			if ( (!exists $filterHash{$parent}) || ($filter eq $parent) ){
				push @msg, "$parent";
			}
		}
	}
	
	return \@msg;
}


sub validateComputeFilter {
	my @msg = ();
	
	for my $key (keys %computeHash){
		my $filter = ${computeHash}{$key}[0];
		next if $filter eq "";
		if(!exists $filterHash{$filter}){
			push @msg, "$filter";
		}
	}
	
	return \@msg;
}


sub checkInputFiles {
	my @names = @{$_[0]};
	my @suffixes = @{$_[1]};
	
	my @msg = ();
	for my $name (@names){
		for my $suffix (@suffixes){
			push @msg, "$name$suffix" if (!-e $name.$suffix);
		}
	}
	
	if(@msg>0){
		die "Absent data file: ".join(", ", @msg)."\n";	
	}
}


sub submitCommand {
	my ($comdHash, $pbs, $workDIR, $logfileName) = @_;
	
	if($pbs){
		open(my $fh, ">>$logfileName");
		
		my $qMonitorCmd = "qMonitor.pl";
		
		for my $comd (sort keys %{$comdHash}){
			
			my $ncpu = 0;
			if($comd =~ /-split\s+(\S+)/){
				$ncpu = $1;
			}else{
				$ncpu = split(/\|/, $comd);
			}
			
			my $str = "echo \\\"$comd\\\" >>${$comdHash}{$comd}";
			my $pbsCmd = "echo \"$str; $comd >>${$comdHash}{$comd} 2>&1\" | qsub -d $workDIR -l ncpus=$ncpu";
			
			my $jobID;
			eval{	
				local *STDOUT = $fh; 
				print "$pbsCmd\n";
				open(CPERR, ">&STDERR");
				open(STDERR,">&STDOUT");
				$jobID = `$pbsCmd`;
				close(STDERR);
				open(STDERR, ">&CPERR");
				close(CPERR);
				die "[FAIL] Fail to qsub: $pbsCmd\n" if (not defined $jobID);	
			};
			if($@){
				print $fh "$@\n";
				print "$@\n";
				exit;
			}
			
			print $fh trim($jobID)."\n";
			$qMonitorCmd .= " -w ".trim($jobID);
		}
		
		# monitor
		print $fh "$qMonitorCmd\n";
		print "$qMonitorCmd\n";
		system($qMonitorCmd)==0 or die "qMonitor failed: $?";
		
		close($fh);
	}else{
		
		for my $comd (sort keys %{$comdHash}){
			my $echo = "echo \"$comd\" >>$logfileName";
			my $sysCmd = "$echo; $comd >>$logfileName 2>&1";
			
			eval{
				system($sysCmd)==0 or die "[FAIL] $sysCmd failed: $?";
			};
			if($@){
				open(my $fh, ">>$logfileName");
				print $fh "$@\n";
				close($fh);
				print "$@\n";
				exit;
			}
		}
		
	}
		
}


sub trim {
	my $str=shift;
	$str =~ s/\s+$//g;
	$str =~ s/^\s+//g;
	return $str;
}