#!/usr/bin/perl -w
sub MappingTargetTable {
	my $TargetName = $_[0];
	my $database = $_[1];
	my $TableFile;
	$TableFile= $ENV{'TargetTableFile'} if defined $ENV{'TargetTableFile'};
	my $colNo=0;
	my $genome;
	return $genome if !(defined $TableFile);
	open(TargetStream,"<$TableFile") or die "Can't open target table \"$TableFile\"";
	while(<TargetStream>)
	{
		if (/^\#/) {
			my @t = split(/\t/,$_);
			for my $i (0..@t-1) {
				$colNo=$i if $t[$i] =~ /$database/i;
			}
			next;
		}
		my @t = split(/\t/,$_);
		if ($t[0] =~ /$TargetName/i) {
		    $genome = $t[$colNo];
		    last;
		}
	}
	close(TargetStream);
	$genome =~ s/\s+$// if defined $genome;
	return $genome;
}

return 1;