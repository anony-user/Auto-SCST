#!/usr/bin/perl 

my $usage = "Usage: IntronicCGFF.pl <inputCGFF> <outputCGFF>\n";
my $InputFilename = shift or die $usage;
my $OutputFilename = shift or die $usage;

open(file,"<$InputFilename");
open(STDOUT,">$OutputFilename");

while($line = <file>)
{	
	@token = ();
	if( $line =~ /^\>/ ) 
	{
		$intronArrsize = @intronArr;
#		if($intronArrsize >= 1)
		{
			print "$id";
		}
		
		#print "$line";
		for($num = 0; $num < $intronArrsize; $num++)
		{
			print "@intronArr[$num]";	
		}
		$id = $line;
		$counter2 = 0;	
		@intronArr = ();
		$indx = 0;
	}
	
	else
	{
		$counter2++;
		@token = split(/\t/, $line);
		if( $counter2 == 1 )
		{
			$start = @token[1] + 1;
		}
	
		if( $counter2 > 1)
		{
			#print "$start\t";
			@intronArr[$indx] =  "$start\t";
			$indx++;
			$ends  = @token[0] - 1;
			$start = @token[1] + 1;
			@intronArr[$indx] =  "$ends\n";
			$indx++;
			#print "$ends\n";
		}
	}
}

$intronArrsize = @intronArr;
#if($intronArrsize >= 1)
{
	print "$id";
}

#print "$line";
for($num = 0; $num < $intronArrsize; $num++)
{
	print "@intronArr[$num]";	
}

close file;
close STDOUT;