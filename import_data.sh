#!/bin/bash
#
source uri.sh

function usage {
	echo "Missing file name, use $0 <file_name>"
}

file=$1
if [ -z "$file" ];
then
	usage
   	exit 1
fi
/opt/mongodb-database-tools-macos-x86_64-100.6.0/bin/mongoimport --uri "$URI" -d mongoDBAnalysis -c logs $1 

mongosh "$URI" update_log.js
