#!/usr/bin/perl

# this program extracts from a given GFF file for records with the specified feature with
# specified attribute values

my $usage = "Usage: GFFExtractor.pl <GffFilename> <outputFilename> [<feature> [<attr>]]\n";

$numArgs = @ARGV;
if($numArgs < 2)
{
	print $usage;
	exit;
}

$InputFilename = $ARGV[0]; 
$OutputFilename = $ARGV[1]; 

if($numArgs < 3)
{
	$feature = "gene";
}
else
{
	# change it if feature is given
	$feature = $ARGV[2];
}

if($numArgs < 4)
{
	$attrTag = "ID";
}
else
{
	# change it if attr is given
	$attrTag = $ARGV[3];
}

open(GFF,"<$InputFilename");
open(STDOUT,">$OutputFilename");

while( $line = <GFF> )
{	
	@token=split(/\t/, $line);
	if( @token[2] eq $feature )
	{
		#@token = split(/\t/, $line);
		$direction = @token[6];
		@attrFieldArray  = split(/;/, @token[8]);
		$attrFieldNum = @attrFieldArray;
		%attrHash = ();
		for($i = 0; $i < $attrFieldNum; $i++)
		{
			@attrToken = split(/=/, @attrFieldArray[$i]);
			$key = @attrToken[0];
			$valueStr = @attrToken[1];
			@valueToken = split(/\,/, $valueStr);
			$value = @valueToken[0];
			$attrHash{$key} = $value;
		}
		
		if( exists $attrHash{$attrTag} )
		{
			print trim(@token[2])."\t".trim(@token[0])."\t".trim(@token[3])."\t".trim(@token[4])."\t".trim($direction)."\t".trim($attrHash{$attrTag})."\n";
		}
	}
}
close GFF;
close STDOUT;

sub trim {
    my $str=shift;
    $str =~ s/\s+$//g;
    $str =~ s/^\s+//g;
    return $str;
}


