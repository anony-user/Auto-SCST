#!/usr/bin/perl -w

my %userTimeHash;
print "User email\tCPU time\tWalltime\tJob Usage\n";
while(<>) {
	my @t=split(/;/);
	next unless ($t[1] eq "E") && /jobname=\d+_\S+@/;
	my ($job_id,$tool,$user) = $t[3] =~ /jobname=(\d+)_(.+)_([A-Za-z0-9\.]+@\S+)/;
	my ($cpu_hr,$cpu_min,$cpu_sec) = $t[3] =~ /used\.cput=(\d+):(\d+):(\d+)/;
	my ($wall_hr,$wall_min,$wall_sec) = $t[3] =~ /used\.walltime=(\d+):(\d+):(\d+)/;
	$cpu_min += ($cpu_hr*60); 
	$cpu_sec += ($cpu_min*60);
	$wall_min += ($wall_hr*60); 
	$wall_sec += ($wall_min*60);
	$userTimeHash{$user}{'cputime'}+=$cpu_sec;
	$userTimeHash{$user}{'walltime'}+=$wall_sec;
	$userTimeHash{$user}{'jobtime'}{$tool}+=$cpu_sec;
}

for $user (sort keys %userTimeHash) {
	my $cpu_sec = $userTimeHash{$user}{'cputime'};
	my $wall_sec = $userTimeHash{$user}{'walltime'};
	my @jobUsage;
	
	my @jobnames = sort keys %{$userTimeHash{$user}{'jobtime'}};
	for my $jobname (@jobnames) {
		my $job_sec = $userTimeHash{$user}{'jobtime'}{$jobname};
		my $percentage = sprintf("%.1f",($job_sec/$cpu_sec)*100);
		$jobname =~ s/^_+|_+$//g;
		push(@jobUsage,"$jobname:$percentage%");
	}
	
	my $cpu_min = int($cpu_sec/60);
	$cpu_sec %= 60;
	my $cpu_hr = int($cpu_min/60);
	$cpu_min %= 60;
	
	my $wall_min = int($wall_sec/60);
	$wall_sec %= 60;
	my $wall_hr = int($wall_min/60);
	$wall_min %= 60;
	
	$cpu_hr = sprintf("%02d",$cpu_hr);
	$cpu_min = sprintf("%02d",$cpu_min);
	$cpu_sec = sprintf("%02d",$cpu_sec);
	
	$wall_hr = sprintf("%02d",$wall_hr);
	$wall_min = sprintf("%02d",$wall_min);
	$wall_sec = sprintf("%02d",$wall_sec);
	
	my $job_percentages = join("; ",@jobUsage);
	
	print "$user\t$cpu_hr:$cpu_min:$cpu_sec\t$wall_hr:$wall_min:$wall_sec\t$job_percentages\n";
}